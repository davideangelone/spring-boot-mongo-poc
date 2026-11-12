package it.davideangelone.mongopoc.model;

import java.time.Instant;
import java.util.Map;

public record UserItem(
        String id,
        String type,
        String title,
        Instant createdAt,
        Map<String, Object> data
) {
}