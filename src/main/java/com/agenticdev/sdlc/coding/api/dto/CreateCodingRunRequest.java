package com.agenticdev.sdlc.coding.api.dto;

import com.agenticdev.sdlc.llm.Provider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

public record CreateCodingRunRequest(
        @NotNull UUID planId,
        @URL @Size(max = 512) String repoUrl,
        @Size(max = 256) @Pattern(regexp = "^[\\w./\\-]+$", message = "invalid ref") String baseRef,
        Provider provider,
        @Size(max = 128) String model,
        @URL @Size(max = 1024) String webhookUrl,
        Boolean autoOpenPr,
        @Size(max = 512) String prTitle,
        @Size(max = 16384) String prBody
) {}
