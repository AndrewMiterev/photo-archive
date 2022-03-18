package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.tools.RingRandomSequence;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
@EnableScheduling
public class RobotServiceImpl implements RobotService {
	private static final long MAX_PHOTOS_FOR_ROBOT = 10;
	private Queue<Photo> photos = new LinkedList<>();
	private final AtomicBoolean busy = new AtomicBoolean(false);

	private final FileMetaService service;
	private final PhotoArchiveProcessor processor;

	public RobotServiceImpl(FileMetaService service, PhotoArchiveProcessor processor) {
		this.service = service;
		this.processor = processor;
	}

	private void lockAndRun(Runnable action) {
		if (!busy.compareAndSet(false, true)) return;
		try {
			action.run();
		} finally {
			busy.set(false);
		}
	}

	@PostConstruct
	private void postConstructor() {
		load();
	}

	@Scheduled(fixedDelay = 600000, initialDelay = 60000)
	@Override
	public void load() {
		lockAndRun(() -> {
			if (!photos.isEmpty()) return;
			var photosList = service.getPhotosWithNotStatus(null)
					.stream()
					.filter(p -> !p.getStatus().equalsIgnoreCase("manual"))
					.toList();
			if (photosList.isEmpty()) return;
			var sequence = new RingRandomSequence(photosList.size());
			var photos = Stream
					.iterate(0, i -> sequence.next())
					.limit(MAX_PHOTOS_FOR_ROBOT)
					.map(photosList::get)
					.collect(Collectors.toCollection(LinkedList::new));
			photos.forEach(d -> log.debug("to process {{}}", d));
			this.photos = photos;
		});
	}

	@Scheduled(fixedDelay = 1000)
	@Override
	public void tick() {
		AtomicBoolean needReload = new AtomicBoolean(false);
		lockAndRun(() -> {
			if (photos.isEmpty()) return;
			var photo = photos.poll();
			action(photo);
			needReload.set(photos.isEmpty());
		});
		if (needReload.get()) load();
	}

	private void action(Photo photo) {
		log.debug("action {{}} photo {{}}", photo.getStatus(), photo);
		switch (photo.getStatus()) {
			case "hash" -> processor.processFileHash(photo.getHash());
			case "exif" -> processor.processExtractExif(photo);
			case "google" -> processor.processObtainGeocode(photo);
			case "resolve" -> processor.processResolveGeocode(photo);
			case "predict" -> processor.processPredict(photo);
			case "move" -> processor.processMove(photo);
			default -> {
				var error = "Undefined process %s".formatted(photo.getStatus());
				log.error(error);
				throw new RuntimeException(error);
			}
		}
	}
}
