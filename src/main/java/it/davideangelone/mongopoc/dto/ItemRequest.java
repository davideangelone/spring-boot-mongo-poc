package it.davideangelone.mongopoc.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record ItemRequest(
        @NotBlank String type,
        @NotBlank String title,
        Map<String, Object> data
) {
}
