package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;

import java.util.function.Consumer;

public interface StatusService {
    void register(String status, Consumer<Photo> processor);
    void process(Photo photo);
}
