package com.hiba.meeting_backend.service;

import com.hiba.meeting_backend.DTO.CreateMeetRequest;
import com.hiba.meeting_backend.DTO.MeetingContextDTO;
import com.hiba.meeting_backend.DTO.SectionContextDTO;
import com.hiba.meeting_backend.DTO.TokenLiveKitDto;
import com.hiba.meeting_backend.Repository.MeetRepository;
import com.hiba.meeting_backend.Repository.UserRepository;
import com.hiba.meeting_backend.model.Meet;
import com.hiba.meeting_backend.model.ReportSection;
import com.hiba.meeting_backend.model.ReportTemplate;
import com.hiba.meeting_backend.model.User;
import io.livekit.server.*;
import livekit.LivekitModels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import retrofit2.Call;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetService {
    private  final MeetRepository meetRepository;
    private final UserRepository userRepository;
    private final RoomServiceClient roomService;
    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    public MeetService(MeetRepository meetRepository , UserRepository userRepository,RoomServiceClient roomService) {
        this.meetRepository = meetRepository;
        this.userRepository = userRepository;
        this.roomService= roomService;
    }
    public List<Meet> findByHostId(String hostId) {
        return meetRepository.findByHostId(hostId);
    }
    public Meet createMeet(CreateMeetRequest request,String hostId ) {
        Meet meet = new Meet();
        meet.setHostId(hostId);
        meet.setTitle(request.getTitle());
        meet.setInvitedUserIds(request.getInvitedUserIds());
        meet.setStatus(Meet.MeetingStatus.PLANNED);
        meet.setCreatedAt(new Date());
        meet.setReportTemplateId(request.getReportTemplateId());

        String uniqueRoomName = UUID.randomUUID().toString();
        meet.setLiveKitRoomName(uniqueRoomName);
        return meetRepository.save(meet);

    }

    public TokenLiveKitDto joinMeet(String email , String meetId ) {
        Meet meet = meetRepository.findById(meetId).get();
        User user = userRepository.findByEmail(email).get();
        System.out.println("Email: " + email);
        System.out.println("Invited: " + meet.getInvitedUserIds());
        System.out.println("Host: " + meet.getHostId());
        if(meet.getStatus().equals(Meet.MeetingStatus.FINISHED)) {
            throw new RuntimeException("Cette réunion est terminée");
        }
        if(! meet.getInvitedUserIds().contains(email) && !meet.getHostId().equals(email))   {
            throw new RuntimeException("Vous avez pas la permession de joindre cette réunion");
        }
        if(meet.getStatus().equals(Meet.MeetingStatus.PLANNED ) ) {
            meet.setStatus(Meet.MeetingStatus.ACTIVE);
            meet.setStartedAt(new Date());
            meetRepository.save(meet);
        }

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(user.getName());
        token.setIdentity(email);
        token.addGrants(new RoomJoin(true), new RoomName(meet.getLiveKitRoomName()));
        String jwt = token.toJwt();
        System.out.println(jwt);
        TokenLiveKitDto  response = new TokenLiveKitDto();
        response.setToken(jwt);
        return  response;

    }

    public void handleParticipantLeft(String roomName, String userIdLeft) {

        Meet meeting = meetRepository.findByLiveKitRoomName(roomName);


        if (meeting == null) {
            System.err.println("Erreur: Meeting introuvable pour la room " + roomName);
            return;
        }
        int remainingParticipants = countParticipants(roomName) - 1;

        if (remainingParticipants <= 1
                || meeting.getHostId().equals(userIdLeft)) {

            closeMeetingAndGenerateReport(meeting);
        }



    }

    private void closeMeetingAndGenerateReport(Meet meeting) {
        System.out.println(" Le manager (" + meeting.getHostId() + ") est parti. Fermeture de la salle...");

        meeting.setStatus(Meet.MeetingStatus.FINISHED);
        meeting.setEndedAt(new Date());
        meetRepository.save(meeting);
        try {
            if (roomService != null) {
                roomService.deleteRoom(meeting.getLiveKitRoomName()).execute();
                System.out.println("✅ Room LiveKit fermée avec succès.");
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la fermeture LiveKit: " + e.getMessage());
        }
        // C. Déclencher la génération de rapport (Agent IA)
        // On délègue ça à un autre service pour garder le code propre
        //reportService.triggerReportGeneration(meeting);
}

   public Meet getMeetById(String meetId)  {
        return meetRepository.findById(meetId).get();  }
    public Meet getMeetByName(String meetName){
        return meetRepository.findByLiveKitRoomName(meetName);
    }
    public Meet getMeetByTitle(String title) {
        return meetRepository.findByTitle(title);
    }
    public List<Meet> findAll(){
        return meetRepository.findAll();
    }
    public Meet save(Meet meet){
        return  meetRepository.save(meet);
    }

    public MeetingContextDTO buildContext(Meet meet, ReportTemplate template) {
        MeetingContextDTO dto = new MeetingContextDTO();

        // --- 1. Mapping Meeting (Données de base) ---
        dto.setMeetId(meet.getId());
        dto.setMeetingTitle(meet.getTitle());

        // Sécurité sur les dates (au cas où la réunion n'est pas "finie" proprement)
        if (meet.getStartedAt() != null) {
            dto.setMeetingDate(meet.getStartedAt().toString());

            if (meet.getEndedAt() != null) {
                long diff = meet.getEndedAt().getTime() - meet.getStartedAt().getTime();
                long minutes = diff / (60 * 1000);
                dto.setDuration(minutes + " minutes");
            } else {
                dto.setDuration("En cours / Inconnue");
            }
        } else {
            dto.setMeetingDate("Date inconnue");
            dto.setDuration("N/A");
        }

        // --- 2. Mapping Template (AVEC GESTION NULL) ---
        if (template != null) {
            // CAS NORMAL : On a un template
            dto.setTemplateName(template.getName());
            dto.setTemplateDescription(template.getDescription());

            // Mapping des sections actives et triées
            List<SectionContextDTO> sectionDTOs = template.getSections().stream()
                    .filter(ReportSection::isEnabled)
                    .sorted(Comparator.comparingInt(ReportSection::getOrder))
                    .map(sec -> new SectionContextDTO(
                            sec.getCode(),
                            sec.getTitle(),
                            sec.getGuidance()
                    ))
                    .collect(Collectors.toList());

            dto.setSections(sectionDTOs);

        } else {
            // ⚠️ CAS DE SECOURS (FALLBACK) : Pas de template associé
            // On crée une structure par défaut pour ne pas planter l'IA
            dto.setTemplateName("Standard (Fallback)");
            dto.setTemplateDescription("Structure par défaut générée automatiquement.");

            List<SectionContextDTO> defaultSections = new ArrayList<>();

            // Section 1 : Résumé
            defaultSections.add(new SectionContextDTO(
                    "SUMMARY",
                    "Résumé Exécutif",
                    "Faire une synthèse globale de la réunion."
            ));

            // Section 2 : Actions
            defaultSections.add(new SectionContextDTO(
                    "ACTIONS",
                    "Plan d'Action",
                    "Lister les tâches à faire, qui doit les faire et pour quand."
            ));

            dto.setSections(defaultSections);
        }

        return dto;
    }

    public int countParticipants(String roomName) {
        try {
            var response = roomService.listParticipants(roomName).execute();

            if (!response.isSuccessful() || response.body() == null) {
                return 0;
            }

            return response.body().size();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to count participants for room " + roomName, e
            );
        }
    }

}
