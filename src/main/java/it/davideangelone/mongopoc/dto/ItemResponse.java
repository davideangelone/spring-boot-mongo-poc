package it.davideangelone.mongopoc.dto;

import java.time.Instant;
import java.util.Map;

import it.davideangelone.mongopoc.model.UserItem;

/** Rappresentazione di un item esposta dall'API, separata dal documento embedded {@link UserItem}. */
public record ItemResponse(
        String id,
        String type,
        String title,
        Instant createdAt,
        Map<String, Object> data
) {

    public static ItemResponse from(UserItem item) {
        return new ItemResponse(item.id(), item.type(), item.title(), item.createdAt(), item.data());
    }
}
