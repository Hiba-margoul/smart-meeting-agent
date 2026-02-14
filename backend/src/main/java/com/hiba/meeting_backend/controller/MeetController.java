package com.hiba.meeting_backend.controller;

import com.hiba.meeting_backend.DTO.CreateMeetRequest;
import com.hiba.meeting_backend.DTO.JoinMeetRequest;
import com.hiba.meeting_backend.DTO.MeetingContextDTO;
import com.hiba.meeting_backend.DTO.TokenLiveKitDto;
import com.hiba.meeting_backend.Repository.MeetRepository;
import com.hiba.meeting_backend.model.Meet;
import com.hiba.meeting_backend.model.ReportTemplate;
import com.hiba.meeting_backend.model.User;
import com.hiba.meeting_backend.service.MeetService;
import com.hiba.meeting_backend.service.ReportTemplateService;
import com.hiba.meeting_backend.service.SseNotificationService;
import com.hiba.meeting_backend.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/meeting")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class MeetController {
    private final MeetService meetService;
    private final UserService userService;
    private final ReportTemplateService reportTemplateService;
    private final SseNotificationService sseService;


    public MeetController(MeetService meetService, UserService userService, MeetRepository meetRepository, ReportTemplateService reportTemplateService,SseNotificationService sseService) {
        this.meetService = meetService;
        this.userService = userService;
        this.reportTemplateService = reportTemplateService;
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
// ⚠️ CHANGEMENT DU TYPE DE RETOUR : <Meet> devient <Object> ou <?>
    public Flux<ServerSentEvent<Object>> streamMeets() {
        return sseService.getMeetStream(); //
    }


    @PostMapping("/create_meet")
    public ResponseEntity<Meet> createMeet(  @RequestBody CreateMeetRequest request,
                                             Authentication authentication ) {
        String email = authentication.getName();
        Meet meet = meetService.createMeet(request,email);
        sseService.notifyUpdate(meet);

        return ResponseEntity.ok(meet);
    }

    @PostMapping("/join_meet")
    public ResponseEntity<TokenLiveKitDto> joinMeet(@RequestBody JoinMeetRequest request ,Authentication authentication) {

        String email = authentication.getName();
        TokenLiveKitDto dto = meetService.joinMeet(email, request.getMeetId());
        Meet meet = meetService.getMeetById(request.getMeetId());
        sseService.notifyUpdate(meet);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/meets")
    public ResponseEntity<List<Meet>> getMeets(Authentication authentication) {
        List<Meet> meets = meetService.findAll();
        return ResponseEntity.ok(meets);
    }
    @GetMapping("/{roomName}")
    public ResponseEntity<?> getMeetByRoomName(@PathVariable String roomName) {
        try {
            // 1. Tenter de récupérer le meeting
            Meet meet = meetService.getMeetByName(roomName);

            // Si meet est null (dépend de votre Service), on renvoie 404 manuellement
            if (meet == null) {
                return ResponseEntity.status(404).body("{\"error\": \"Meeting not found\"}");
            }

            // 2. Gestion du Template (votre code existant)
            ReportTemplate template = null;
            if (meet.getReportTemplateId() != null) {
                try {
                    template = reportTemplateService.getReportTemplateById(meet.getReportTemplateId());
                } catch (Exception e) {
                    System.err.println("⚠️ Template introuvable pour l'ID : " + meet.getReportTemplateId());
                    // On continue sans template (template = null)
                }
            } else {
                System.out.println("ℹ️ Aucun template associé à ce meeting (ID null). Utilisation du défaut.");
            }

            // 3. Construction du DTO
            MeetingContextDTO contextDTO = meetService.buildContext(meet, template);
            return ResponseEntity.ok(contextDTO);

        } catch (Exception e) {
            // 4. C'est ICI que l'erreur Python est corrigée
            // Au lieu de laisser Spring renvoyer du HTML, on renvoie un JSON d'erreur 404
            System.err.println("❌ Erreur lors de la récupération du meeting : " + e.getMessage());
            return ResponseEntity.status(404).body("{\"error\": \"Meeting not found or internal error\"}");
        }
    }
    @GetMapping("/id/{meetId}")
    public ResponseEntity<Meet> getMeetById(@PathVariable String meetId) {

        Meet meet = meetService.getMeetById(meetId);
        return ResponseEntity.ok(meet);
    }


    @GetMapping("/close_meet/{roomName}")

    public ResponseEntity<String> closeMeet(Authentication authentication, @PathVariable String roomName) {
        String email = authentication.getName();
        Optional<User> manager = userService.findByEmail(email);
        String hostId = manager.get().getId();
        try{
         meetService.handleParticipantLeft( roomName,hostId);
         Meet meet = meetService.getMeetByName(roomName);
         sseService.notifyUpdate(meet);

        return ResponseEntity.ok("meet left successfully");}
        catch(Exception e){
            return ResponseEntity.status(500).body("meet left failed");
        }
    }

    }

