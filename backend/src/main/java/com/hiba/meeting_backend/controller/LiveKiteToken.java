package com.hiba.meeting_backend.controller;


import com.hiba.meeting_backend.Repository.MeetRepository;
import com.hiba.meeting_backend.model.Meet;
import com.hiba.meeting_backend.model.User;
import com.hiba.meeting_backend.service.UserService;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/livekit")
@CrossOrigin(origins = "http://localhost:4200")
public class LiveKiteToken {
    private final UserService userService;
    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    private final MeetRepository meetRepository;

    public LiveKiteToken(MeetRepository meetRepository, UserService userService) {
        this.meetRepository = meetRepository;
        this.userService = userService;
    }

    @PostMapping("/token")
    public ResponseEntity<String> createToken(@RequestBody Map<String, String> body) {
        Meet meet=  meetRepository.findByLiveKitRoomName(body.get("liveKitRoomName"));
        if(meet.getStatus() != Meet.MeetingStatus.ACTIVE) {
            return ResponseEntity.status(403).body("Réunion non active ou terminée");
        }
        String roomName = body.get("title");
        String participantName = body.get("participantName");
        String participantId = body.get("participantId");

        if (!meet.getHostId().equals(participantId) && !meet.getInvitedUserIds().contains(participantId)) {
            return ResponseEntity.status(403).body("Vous n'êtes pas invité à cette réunion !");
        }
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(participantName);
        token.setIdentity(participantId);
        token.addGrants(new RoomJoin(true), new RoomName(roomName));
        String jwt = token.toJwt();
        System.out.println(jwt);
        return ResponseEntity.ok(Map.of("token", jwt).toString());
    }
}
