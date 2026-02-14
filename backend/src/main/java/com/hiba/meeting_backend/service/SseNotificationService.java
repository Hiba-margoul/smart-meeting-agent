package com.hiba.meeting_backend.service;

import com.hiba.meeting_backend.model.Meet;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class SseNotificationService {

    // ⚠️ CORRECTION MAJEURE ICI : .directBestEffort()
    // Cela empêche le Sink de "mourir" quand le dernier utilisateur se déconnecte.
    private final Sinks.Many<Object> meetSink = Sinks.many().multicast().directBestEffort();

    public Flux<ServerSentEvent<Object>> getMeetStream() {
        // Flux des données (Mises à jour de meeting)
        Flux<ServerSentEvent<Object>> dataFlux = meetSink.asFlux()
                .map(data -> ServerSentEvent.<Object>builder()
                        .data(data)
                        .build());

        // Flux Heartbeat (Ping toutes les 20s pour garder la connexion)
        Flux<ServerSentEvent<Object>> heartbeatFlux = Flux.interval(Duration.ofSeconds(20))
                .map(i -> ServerSentEvent.<Object>builder()
                        .event("heartbeat")
                        .data("ping")
                        .build());

        return Flux.merge(dataFlux, heartbeatFlux);
    }

    public void notifyUpdate(Meet meet) {
        System.out.println("📢 Envoi SSE pour : " + meet.getTitle());

        try {
            Map<String, Object> safeMeet = new HashMap<>();
            safeMeet.put("id", meet.getId());
            safeMeet.put("title", meet.getTitle());
            safeMeet.put("status", meet.getStatus());
            safeMeet.put("hostId", meet.getHostId());
            safeMeet.put("startedAt", meet.getStartedAt() != null ? meet.getStartedAt().toString() : null);
            safeMeet.put("endedAt", meet.getEndedAt() != null ? meet.getEndedAt().toString() : null);

            // ✅ CORRECTION 1 : Ajouter la liste des invités (avec une liste vide par défaut pour éviter le null)
            safeMeet.put("invitedUserIds", meet.getInvitedUserIds() != null ? meet.getInvitedUserIds() : new ArrayList<>());

            // ✅ CORRECTION 2 : Ajouter le flag reportGenerated (pour le bouton rapport)
            safeMeet.put("reportGenerated", meet.isReportGenerated());

            // ÉMISSION
            Sinks.EmitResult result = meetSink.tryEmitNext(safeMeet);

            if (result.isFailure()) {
                // Si ça échoue encore, c'est grave, mais directBestEffort ne renvoie quasi jamais d'erreur
                System.err.println("❌ Erreur SSE : " + result.name());
            } else {
                System.out.println("✅ Succès : Notification envoyée !");
            }
        } catch (Exception e) {
            System.err.println("❌ CRASH SSE : " + e.getMessage());
            e.printStackTrace();
        }
    }
}