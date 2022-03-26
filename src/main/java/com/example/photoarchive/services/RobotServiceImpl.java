package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Log4j2
@EnableScheduling
public class RobotServiceImpl implements RobotService {
	private final FileMetaService service;
	private final PhotoArchiveProcessor processor;
	private final ConfigProperties properties;

	private final Set<Photo> notReadyPhotos = new CopyOnWriteArraySet<>();
	private final Set<String> workOnIt = new CopyOnWriteArraySet<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(1);

	public RobotServiceImpl(FileMetaService service, PhotoArchiveProcessor processor, ConfigProperties properties) {
		this.service = service;
		this.processor = processor;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${photo-archive.processing-delay-in-milliseconds:600000}"
			, initialDelayString = "${photo-archive.processing-initial-delay-in-milliseconds:60000}")
	@Override
	public void load() {
		var maxPhotosForRobot = properties.getProcessingNumberPhotosAtSameTime();
		var photosList = service.getPhotosWithStatusNotNullAndStatusNotManual(maxPhotosForRobot);
		log.debug("Number photos for processing {{}}", photosList.size());
		notReadyPhotos.addAll(photosList);
	}

	//	@Scheduled(fixedDelay = 1000)
	@Override
	public void tick() {
		var chain = CompletableFuture
				.supplyAsync(() -> {
					var randomPhoto = notReadyPhotos.stream()
							.filter(p -> !workOnIt.contains(p.getHash()))
							.findAny()
							.orElse(null);
					if (Objects.isNull(randomPhoto)) {
						log.debug("list of 'not ready photos' is empty");
						throw new RuntimeException("Empty list 'not ready photos'");
					}
					if (!workOnIt.add(randomPhoto.getHash()))
						throw new RuntimeException("I can't lock hash {%s}".formatted(randomPhoto.getHash()));
					return randomPhoto;
				}, executor)
				.thenApplyAsync(this::action, executor)
				.thenApplyAsync(photo -> {
					if (photo.getStatus().isEmpty() || photo.getStatus().equalsIgnoreCase("manual"))
						notReadyPhotos.remove(photo);
					return photo;
				})
				.whenCompleteAsync((photo, throwable) -> {
					workOnIt.remove(photo.getHash());
					if (Objects.nonNull(throwable)) {
						log.error("{} {}", throwable.getCause(), throwable);
					}
				}, executor);
	}

	private Photo action(Photo photo) {
		log.debug("action {{}} photo {{}}", photo.getStatus(), photo);
//		switch (photo.getStatus()) {
//			case "hash" -> processor.processFileHash(photo.getHash());
//			case "exif" -> processor.processExtractExif(photo);
//			case "google" -> processor.processObtainGeocode(photo);
//			case "resolve" -> processor.processResolveGeocode(photo);
//			case "predict" -> processor.processPredict(photo);
//			case "move" -> processor.processMove(photo);
//			default -> {
//				var error = "Undefined process '%s'".formatted(photo.getStatus());
//				log.error(error);
//				throw new RuntimeException(error);
//			}
//		}
		return photo;
	}
}
