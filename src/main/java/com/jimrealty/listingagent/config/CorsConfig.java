package com.jimrealty.listingagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;
// CORS (Cross-Origin Resource Sharing) is a browser security policy that blocks
// requests from one origin (http://localhost:3000) to another (http://localhost:8080)
// by default. Without this config, your React frontend will get a CORS error
// the moment it tries to call this API — even though both are running on localhost.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://localhost:4321",                                           // Astro dev server
                    "https://listing-agent-api.vercel.app",                            // React frontend
                    "https://sawicki-group-web-43343667811.us-central1.run.app"        // Astro production
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
}