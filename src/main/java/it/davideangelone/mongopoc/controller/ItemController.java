package it.davideangelone.mongopoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.davideangelone.mongopoc.dto.ItemRequest;
import it.davideangelone.mongopoc.dto.ItemResponse;
import it.davideangelone.mongopoc.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/items")
@Tag(name = "User items", description = "CRUD degli item personalizzati appartenenti a un utente")
public class ItemController {
    private final UserService service;
    public ItemController(UserService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Elenca gli item dell'utente")
    public List<ItemResponse> findAll(@PathVariable String userId) {
        return service.findItems(userId)
                .stream()
                .map(ItemResponse::from)
                .toList();
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Recupera un item dell'utente")
    public ItemResponse findById(@PathVariable String userId, @PathVariable String itemId) {
        return ItemResponse.from(service.findItem(userId, itemId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un item per l'utente")
    public ItemResponse create(@PathVariable String userId, @Valid @RequestBody ItemRequest request) {
        return ItemResponse.from(service.createItem(userId, request));
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Aggiorna un item dell'utente")
    public ItemResponse update(@PathVariable String userId, @PathVariable String itemId,
                               @Valid @RequestBody ItemRequest request) {
        return ItemResponse.from(service.updateItem(userId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un item dell'utente")
    public void delete(@PathVariable String userId, @PathVariable String itemId) {
        service.deleteItem(userId, itemId);
    }
}
