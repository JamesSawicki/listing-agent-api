package com.jimrealty.listingagent.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ChatRequest {
    @NotBlank @Size(max = 5000)
    private String systemPrompt;

    @NotNull @Size(max = 20)
    private List<@NotNull Map<String, String>> messages;
}
