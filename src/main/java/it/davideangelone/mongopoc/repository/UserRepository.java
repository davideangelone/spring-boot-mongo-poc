package it.davideangelone.mongopoc.repository;

import it.davideangelone.mongopoc.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> { }
