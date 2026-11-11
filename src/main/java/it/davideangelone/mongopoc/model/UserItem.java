package it.davideangelone.mongopoc.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;

public record UserItem(
        String id,
        @Indexed
        String type,
        String title,
        Instant createdAt,
        Map<String, Object> data
) {
}