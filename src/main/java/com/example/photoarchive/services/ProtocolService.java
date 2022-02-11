package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Protocol;

import java.util.List;

public interface ProtocolService {
    void add(String type, String message);
    List<Protocol> getAll();
    void delete(Protocol protocol);
}
