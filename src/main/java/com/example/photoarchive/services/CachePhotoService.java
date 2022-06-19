package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;

import java.util.concurrent.CompletableFuture;

public interface CachePhotoService {
	CompletableFuture<byte[]> save(Photo photo, CompletableFuture<byte[]> data);

	boolean exists(Photo photo);

	CompletableFuture<byte[]> get(Photo photo);
}
