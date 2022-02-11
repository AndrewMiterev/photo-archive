package com.example.photoarchive.domain.repo;

import com.example.photoarchive.domain.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
