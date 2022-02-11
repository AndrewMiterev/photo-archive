package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Protocol;
import com.example.photoarchive.domain.repo.ProtocolRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
public class ProtocolServiceImpl implements ProtocolService {
    private final ProtocolRepository repository;

    public ProtocolServiceImpl(ProtocolRepository repository) {
        this.repository = repository;
    }

    @Override
    public void add(String type, String message) {
        repository.save(Protocol.builder()
                .date(LocalDateTime.now())
                .type(type)
                .message(message)
                .build());
    }

    @Override
    public List<Protocol> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Protocol protocol) {
        repository.delete(protocol);
    }
}
