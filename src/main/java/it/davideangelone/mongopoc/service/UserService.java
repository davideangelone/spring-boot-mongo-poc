package it.davideangelone.mongopoc.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.davideangelone.mongopoc.dto.ItemRequest;
import it.davideangelone.mongopoc.dto.UserRequest;
import it.davideangelone.mongopoc.exception.ResourceNotFoundException;
import it.davideangelone.mongopoc.model.User;
import it.davideangelone.mongopoc.model.UserItem;
import it.davideangelone.mongopoc.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> findAllUsers() {
        return repository.findAll();
    }

    public User findUser(String userId) {
        return getUser(userId);
    }

    public User createUser(UserRequest request) {
        User user = new User(null, request.name(), request.email(), List.of());
        return repository.save(user);
    }

    public User updateUser(String userId, UserRequest request) {
        User user = getUser(userId);
        user.setName(request.name());
        user.setEmail(request.email());

        return repository.save(user);
    }

    public void deleteUser(String userId) {
        User user = getUser(userId);
        repository.delete(user);
    }

    public List<UserItem> findItems(String userId) {
        return getUser(userId).getItems();
    }

    public UserItem findItem(String userId, String itemId) {
        return getItem(getUser(userId), itemId);
    }

    public UserItem createItem(String userId, ItemRequest request) {
        User user = getUser(userId);

        UserItem item = new UserItem(UUID.randomUUID().toString(), request.type(), request.title(), Instant.now(), request.data());
        user.getItems().add(item);

        repository.save(user);
        return item;
    }

    public UserItem updateItem(String userId, String itemId, ItemRequest request) {
        User user = getUser(userId);
        UserItem existingItem = getItem(user, itemId);

        UserItem updatedItem = new UserItem(
                existingItem.id(),
                request.type(),
                request.title(),
                existingItem.createdAt(),
                request.data()
        );

        user.getItems().remove(existingItem);
        user.getItems().add(updatedItem);

        repository.save(user);

        return updatedItem;
    }

    public void deleteItem(String userId, String itemId) {
        User user = getUser(userId);
        UserItem item = getItem(user, itemId);
        user.getItems().remove(item);
        repository.save(user);
    }

    private User getUser(String userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato: " + userId));
    }

    private UserItem getItem(User user, String itemId) {
        return user.getItems()
                .stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item non trovato: " + itemId));
    }
}