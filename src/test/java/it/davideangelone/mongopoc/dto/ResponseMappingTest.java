package it.davideangelone.mongopoc.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import it.davideangelone.mongopoc.model.User;
import it.davideangelone.mongopoc.model.UserItem;
import org.junit.jupiter.api.Test;

/** Test unitari del mapping entity -> DTO di risposta: non richiedono Docker né il contesto Spring. */
class ResponseMappingTest {

    private static final Instant CREATED = Instant.parse("2026-11-12T08:35:56.522Z");

    @Test
    void itemResponseCopiesAllFields() {
        UserItem item = new UserItem("item-1", "TASK", "Titolo", CREATED, Map.of("priority", "HIGH"));

        ItemResponse response = ItemResponse.from(item);

        assertThat(response.id()).isEqualTo("item-1");
        assertThat(response.type()).isEqualTo("TASK");
        assertThat(response.title()).isEqualTo("Titolo");
        assertThat(response.createdAt()).isEqualTo(CREATED);
        assertThat(response.data()).containsEntry("priority", "HIGH");
    }

    @Test
    void userResponseCopiesFieldsAndMapsItems() {
        UserItem first = new UserItem("item-1", "TASK", "Primo", CREATED, Map.of());
        UserItem second = new UserItem("item-2", "NOTE", "Secondo", CREATED, Map.of("content", "x"));
        User user = new User("user-1", "Mario Rossi", "mario.rossi@example.com", List.of(first, second));

        UserResponse response = UserResponse.from(user);

        assertThat(response.id()).isEqualTo("user-1");
        assertThat(response.name()).isEqualTo("Mario Rossi");
        assertThat(response.email()).isEqualTo("mario.rossi@example.com");
        assertThat(response.items()).extracting(ItemResponse::id).containsExactly("item-1", "item-2");
        assertThat(response.items()).extracting(ItemResponse::title).containsExactly("Primo", "Secondo");
    }

    @Test
    void userResponseHandlesUserWithoutItems() {
        User user = new User("user-1", "Mario Rossi", "mario.rossi@example.com", null);

        assertThat(UserResponse.from(user).items()).isEmpty();
    }

    @Test
    void userResponseHandlesNullItemsList() {
        User user = new User("user-1", "Mario Rossi", "mario.rossi@example.com", List.of());
        user.setItems(null);

        assertThat(UserResponse.from(user).items()).isEmpty();
    }
}
