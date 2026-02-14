package com.hiba.meeting_backend.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {

        // C'est ce paramètre qui tue votre SSE au bout de 5 min par défaut
        configurer.setDefaultTimeout(-1);
    }
}
