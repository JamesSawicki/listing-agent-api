package com.jimrealty.listingagent.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String systemPrompt;
    private List<Map<String, String>> messages;
}
