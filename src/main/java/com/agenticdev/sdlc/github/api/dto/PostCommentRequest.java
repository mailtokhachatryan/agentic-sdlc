package com.agenticdev.sdlc.github.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCommentRequest(
        @NotBlank @Size(max = 65536) String body
) {}
