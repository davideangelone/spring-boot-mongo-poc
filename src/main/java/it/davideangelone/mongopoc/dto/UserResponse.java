package it.davideangelone.mongopoc.dto;

import java.time.Instant;
import java.util.List;

import it.davideangelone.mongopoc.model.User;

/**
 * Rappresentazione di un utente esposta dall'API, separata dal documento MongoDB {@link User}.
 * Campi interni come {@code version} non vengono esposti.
 */
public record UserResponse(
        String id,
        String name,
        String email,
        Instant createdAt,
        Instant updatedAt,
        List<ItemResponse> items
) {

    public static UserResponse from(User user) {
        List<ItemResponse> items = user.getItems() == null
                ? List.of()
                : user.getItems().stream().map(ItemResponse::from).toList();

        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getCreatedAt(), user.getUpdatedAt(), items);
    }
}
