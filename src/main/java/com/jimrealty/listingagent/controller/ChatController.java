package com.jimrealty.listingagent.controller;

import com.jimrealty.listingagent.model.ChatRequest;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    // ─────────────────────────────────────────────
    // RATE LIMITER
    // One bucket per IP address. Each bucket holds
    // 10 tokens and refills at 10 per hour.
    // A "token" is consumed on each request.
    // When the bucket is empty, the request is denied.
    // ConcurrentHashMap is thread-safe — multiple
    // requests can arrive simultaneously.
    // ─────────────────────────────────────────────
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket getBucketForIp(String ip) {
        return buckets.computeIfAbsent(ip, key -> {
            // Allow 10 requests per hour per IP
            Bandwidth limit = Bandwidth.classic(
                10,
                Refill.greedy(10, Duration.ofHours(1))
            );
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    private String getRealIp(HttpServletRequest request) {
        // Railway passes the real client IP in X-Forwarded-For
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping
    public ResponseEntity<?> chat(
            @RequestBody ChatRequest chatRequest,
            HttpServletRequest request) {

        // Check rate limit for this IP
        String ip = getRealIp(request);
        Bucket bucket = getBucketForIp(ip);

        if (!bucket.tryConsume(1)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Too many requests. Please wait before sending another message.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        RestTemplate restTemplate = new RestTemplate();

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

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages",
            entity,
            Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }
}