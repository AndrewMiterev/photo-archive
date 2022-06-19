package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.tools.RingRandomSequence;
import com.vaadin.flow.server.StreamResource;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface SlideshowService {
	void next(RingRandomSequence sequence, BiConsumer<StreamResource, Photo> consumer);

	void current(RingRandomSequence sequence, BiConsumer<StreamResource, Photo> consumer);

	void previous(RingRandomSequence sequence, BiConsumer<StreamResource, Photo> consumer);

	LocalDateTime directoryIsLoadedAt();

	RingRandomSequence makeSequence(); // todo sorting parameters

	CompletableFuture<Void> reload();

	boolean isEmpty();
}
