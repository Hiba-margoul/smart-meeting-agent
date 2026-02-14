package com.hiba.meeting_backend.controller;


import com.hiba.meeting_backend.service.MeetService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private final MeetService meetService;
    private final WebhookReceiver webhookReceiver;

    public WebhookController(MeetService meetService,
                             @Value("${livekit.api.key}") String apiKey,
                             @Value("${livekit.api.secret}") String apiSecret) {
        this.meetService = meetService;
        // Le Receiver sert à valider que le message vient bien de LiveKit
        this.webhookReceiver = new WebhookReceiver(apiKey, apiSecret);
    }

    @PostMapping("/events")
    public ResponseEntity<String> receiveWebhook(@RequestBody String body,
                                                 @RequestHeader("Authorization") String authHeader) {
        try {
            // 1. Validation de sécurité (Signature JWT)
            // Si la signature est fausse, ça lance une exception
            LivekitWebhook.WebhookEvent event = webhookReceiver.receive(body, authHeader);

            System.out.println("🔔 Webhook reçu : " + event.getEvent() + " / Room: " + event.getRoom().getName());

            // 2. Traitement des événements
            if ("participant_left".equals(event.getEvent())) {
                String roomName = event.getRoom().getName();
                String userId = event.getParticipant().getIdentity();

                // Appel à ton service pour vérifier si c'est le manager qui part
                meetService.handleParticipantLeft(roomName, userId);
            }

            // Tu pourras ajouter d'autres cas ici (ex: "room_finished")

            return ResponseEntity.ok("ok");

        } catch (Exception e) {
            System.err.println("❌ Erreur Webhook : Signature invalide ou données corrompues.");
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid Signature");
        }
    }
}
