package com.jimrealty.listingagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS (Cross-Origin Resource Sharing) is a browser security policy that blocks
// requests from one origin (http://localhost:3000) to another (http://localhost:8080)
// by default. Without this config, your React frontend will get a CORS error
// the moment it tries to call this API — even though both are running on localhost.
//
// In production, replace "http://localhost:3000" with your actual frontend domain.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")          // apply to all /api/ routes
                .allowedOrigins(
                    "http://localhost:3000",    // React dev server (Create React App)
                    "http://localhost:5173"     // React dev server (Vite)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
