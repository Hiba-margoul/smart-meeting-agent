package com.hiba.meeting_backend.Config;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    @Value("${livekit.api.key}")
    private String apiKey;


    @Value("${livekit.api.secret}")
    private String apiSecret;

    @Value("${livekit.url}")
    private String livekitUrl;


    @Bean
    public RoomServiceClient roomServiceClient() {
        // 2. Pass the URL to the client
        return RoomServiceClient.create(livekitUrl, apiKey, apiSecret);
    }
}
