package com.example.photoarchive.domain.repo;

import com.example.photoarchive.domain.entities.Protocol;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProtocolRepository extends MongoRepository<Protocol, UUID> {
}
