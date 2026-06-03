package com.jimrealty.listingagent.controller;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import com.jimrealty.listingagent.model.ChatRequest;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    // ─────────────────────────────────────────────
    // RATE LIMITER
    // ─────────────────────────────────────────────
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
    .expireAfterAccess(2, TimeUnit.HOURS)
    .maximumSize(10_000)
    .build();
    
    private final RestTemplate restTemplate;

    private static Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofHours(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket getBucketForIp(String ip) {
        return buckets.get(ip, key -> createBucket());
    }

    private String getRealIp(HttpServletRequest request) {
        // Railway passes the real client IP in X-Forwarded-For
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @SuppressWarnings("null")
    @PostMapping
    public ResponseEntity<?> chat(
            @RequestBody ChatRequest chatRequest,
            HttpServletRequest request) {

        String ip = getRealIp(request);
        Bucket bucket = getBucketForIp(ip);

        if (!bucket.tryConsume(1)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Too many requests. Please wait before sending another message.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-sonnet-4-5");
        body.put("max_tokens", 600);
        body.put("system", chatRequest.getSystemPrompt());
        body.put("messages", chatRequest.getMessages());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(  // ← exchange + PTR
            "https://api.anthropic.com/v1/messages",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        return ResponseEntity.ok(response.getBody());
    }
}