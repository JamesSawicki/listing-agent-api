package com.jimrealty.listingagent.controller;

import com.jimrealty.listingagent.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    // @Value injects the property from application.properties
    // The key never leaves the server
    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @PostMapping
    public ResponseEntity<Map> chat(@RequestBody ChatRequest chatRequest) {
        RestTemplate restTemplate = new RestTemplate();

        // Build the request body Anthropic expects
        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-sonnet-4-5");
        body.put("max_tokens", 600);
        body.put("system", chatRequest.getSystemPrompt());
        body.put("messages", chatRequest.getMessages());

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Forward to Anthropic and return the response directly to React
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages",
            entity,
            Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }
}