package it.davideangelone.mongopoc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import it.davideangelone.mongopoc.dto.ItemRequest;
import it.davideangelone.mongopoc.dto.UserRequest;
import it.davideangelone.mongopoc.exception.ResourceNotFoundException;
import it.davideangelone.mongopoc.model.User;
import it.davideangelone.mongopoc.model.UserItem;
import it.davideangelone.mongopoc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Test di integrazione contro un MongoDB reale avviato da Testcontainers (serve Docker).
 * Ogni test crea utenti con email univoche, quindi non dipende dal seed iniziale.
 */
@Testcontainers
@SpringBootTest
class UserServiceIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:9.0.8");

    @Autowired
    UserService service;

    @Autowired
    UserRepository repository;

    // Con N richieste concorrenti una richiesta fallisce al massimo una volta per ciascuna delle
    // altre N-1 (ogni conflitto corrisponde a una scrittura riuscita di un'altra richiesta).
    // Con 5 richieste servono quindi al massimo 4 ritentativi, e @Retryable ne consente 5:
    // l'esito è deterministico. Con molte più richieste simultanee il 409 sarebbe possibile.
    @Test
    void createItemsConcurrentlyKeepsAllOfThem() {
        User user = newUser();
        int total = 5;

        ExecutorService executor = Executors.newFixedThreadPool(total);
        try {
            List<CompletableFuture<UserItem>> futures = IntStream.range(0, total)
                    .mapToObj(i -> CompletableFuture.supplyAsync(
                            () -> service.createItem(user.getId(), item("NOTE", "nota " + i)), executor))
                    .toList();
            futures.forEach(CompletableFuture::join);
        } finally {
            executor.shutdown();
        }

        User reloaded = service.findUser(user.getId());
        assertThat(reloaded.getItems()).hasSize(total);
        assertThat(reloaded.getItems()).extracting(UserItem::id).doesNotHaveDuplicates();
    }

    // 1 updateUser + 4 createItem = 5 richieste concorrenti: come sopra, l'esito è deterministico.
    // Nessuna delle due scritture deve annullare l'altra.
    @Test
    void updateUserConcurrentWithItemCreationLosesNothing() {
        User user = newUser();
        String newName = "Nome aggiornato";
        int itemRequests = 4;

        ExecutorService executor = Executors.newFixedThreadPool(itemRequests + 1);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            futures.add(CompletableFuture.runAsync(
                    () -> service.updateUser(user.getId(), new UserRequest(newName, uniqueEmail())), executor));
            for (int i = 0; i < itemRequests; i++) {
                String title = "nota " + i;
                futures.add(CompletableFuture.runAsync(
                        () -> service.createItem(user.getId(), item("NOTE", title)), executor));
            }
            futures.forEach(CompletableFuture::join);
        } finally {
            executor.shutdown();
        }

        User reloaded = service.findUser(user.getId());
        assertThat(reloaded.getName()).isEqualTo(newName);
        assertThat(reloaded.getItems()).hasSize(itemRequests);
    }

    @Test
    void updateItemKeepsIdAndCreatedAt() {
        User user = newUser();
        UserItem first = service.createItem(user.getId(), item("TASK", "primo"));
        UserItem second = service.createItem(user.getId(), item("TASK", "secondo"));

        // createItem restituisce l'Instant in memoria (nanosecondi), MongoDB salva i millisecondi:
        // il confronto va fatto con l'item come persistito.
        UserItem stored = service.findItem(user.getId(), first.id());

        UserItem updated = service.updateItem(user.getId(), first.id(),
                new ItemRequest("NOTE", "primo modificato", Map.of("content", "x")));

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.createdAt()).isEqualTo(stored.createdAt());
        assertThat(updated.type()).isEqualTo("NOTE");
        assertThat(updated.title()).isEqualTo("primo modificato");

        List<UserItem> items = service.findItems(user.getId());
        assertThat(items).extracting(UserItem::id).containsExactlyInAnyOrder(first.id(), second.id());
        assertThat(items).extracting(UserItem::title).containsExactlyInAnyOrder("primo modificato", "secondo");
    }

    @Test
    void deleteItemRemovesOnlyThatItem() {
        User user = newUser();
        UserItem keep = service.createItem(user.getId(), item("TASK", "resta"));
        UserItem drop = service.createItem(user.getId(), item("TASK", "via"));

        service.deleteItem(user.getId(), drop.id());

        assertThat(service.findItems(user.getId())).extracting(UserItem::id).containsExactly(keep.id());
    }

    @Test
    void missingUserOrItemYieldsNotFound() {
        User user = newUser();
        String missing = "000000000000000000000000";

        assertThatThrownBy(() -> service.createItem(missing, item("TASK", "x")))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Utente");
        assertThatThrownBy(() -> service.updateItem(missing, "x", item("TASK", "x")))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Utente");
        assertThatThrownBy(() -> service.updateItem(user.getId(), "inesistente", item("TASK", "x")))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Item");
        assertThatThrownBy(() -> service.deleteItem(user.getId(), "inesistente"))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Item");
        assertThatThrownBy(() -> service.deleteUser(missing))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void staleSaveAfterItemOperationIsRejected() {
        User user = newUser();
        User stale = service.findUser(user.getId());

        service.createItem(user.getId(), item("TASK", "concorrente"));

        stale.setName("modifica basata su una lettura vecchia");
        assertThatThrownBy(() -> repository.save(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    // Verifica che repository.delete(entity) tenga conto di @Version: è ciò su cui si basa il
    // @Retryable di deleteUser. Se questo test fallisse, la delete ignorerebbe la versione e il
    // retry su deleteUser non avrebbe effetto.
    @Test
    void staleDeleteAfterItemOperationIsRejected() {
        User user = newUser();
        User stale = service.findUser(user.getId());

        service.createItem(user.getId(), item("TASK", "concorrente"));

        assertThatThrownBy(() -> repository.delete(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(repository.existsById(user.getId())).isTrue();
    }

    private User newUser() {
        return service.createUser(new UserRequest("Test User", uniqueEmail()));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static ItemRequest item(String type, String title) {
        return new ItemRequest(type, title, Map.of());
    }
}
