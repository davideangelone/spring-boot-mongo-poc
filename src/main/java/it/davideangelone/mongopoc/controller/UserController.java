package it.davideangelone.mongopoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.davideangelone.mongopoc.dto.UserRequest;
import it.davideangelone.mongopoc.model.User;
import it.davideangelone.mongopoc.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "CRUD degli utenti")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Elenca tutti gli utenti con i rispettivi item")
    public List<User> findAll() { return service.findAllUsers(); }

    @GetMapping("/{userId}")
    @Operation(summary = "Recupera un utente")
    public User findById(@PathVariable String userId) { return service.findUser(userId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un utente")
    public User create(@Valid @RequestBody UserRequest request) { return service.createUser(request); }

    @PutMapping("/{userId}")
    @Operation(summary = "Aggiorna nome ed email di un utente")
    public User update(@PathVariable String userId, @Valid @RequestBody UserRequest request) {
        return service.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un utente e tutti i suoi item embedded")
    public void delete(@PathVariable String userId) { service.deleteUser(userId); }
}
