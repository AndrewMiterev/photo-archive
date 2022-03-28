package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

	private final Set<String> notReadyPhotos = new CopyOnWriteArraySet<>();
	private final Set<String> workOnIt = new CopyOnWriteArraySet<>();
	private ExecutorService executor;

	public RobotServiceImpl(FileMetaService service, PhotoArchiveProcessor processor, ConfigProperties properties) {
		this.service = service;
		this.processor = processor;
		this.properties = properties;
	}

	@PostConstruct
	public void postConstruct() {
		executor = Executors.newFixedThreadPool(properties.getProcessingThreads());
	}

	@Scheduled(fixedDelayString = "${photo-archive.processing-load-delay-in-milliseconds:0}",
			initialDelayString = "${photo-archive.processing-load-initial-delay-in-milliseconds:0}")
	@Override
	public void load() {
		if (notReadyPhotos.size() == 0) workOnIt.clear();        // clear accumulated errors (throws e.t.s.)
		var maxPhotosForRobot = properties.getProcessingNumberPhotosAtSameTime();
		var photosList = service.getPhotosWithStatusNotNullAndStatusNotManual(maxPhotosForRobot)
				.stream()
				.map(Photo::getHash)
				.toList();
//		log.debug("Number photos for processing {{}}", photosList.size());
		notReadyPhotos.addAll(photosList);
	}

	@Scheduled(fixedDelayString = "${photo-archive.processing-tick-delay-in-milliseconds:0}",
			initialDelayString = "${photo-archive.processing-tick-initial-delay-in-milliseconds:0}")
	@Override
	public void tick() {
		notReadyPhotos.stream()
				.filter(hash -> !workOnIt.contains(hash))
				.findAny()
				.flatMap(service::getPhoto)
				.ifPresent(this::process);
//				.ifPresentOrElse(this::process, () -> log.trace("no photo to processing"));
	}

	private void process(Photo photoToProcess) {
		var chain = CompletableFuture.completedFuture(photoToProcess)
				.thenApplyAsync(photo -> {
					if (!workOnIt.add(photo.getHash()))
						throw new RuntimeException("I can't lock photo with hash {%s}".formatted(photo.getHash()));
					return photo;
				}, executor)
				.thenApplyAsync(this::action, executor)
				.thenApplyAsync(photo -> {
					var hash = photo.getHash();
					service.getPhoto(hash).ifPresentOrElse(p -> {
						log.debug("new status of photo {{}} {{}}", p.getStatus(), p);
						if (Objects.isNull(p.getStatus()) || photo.getStatus().equalsIgnoreCase("manual"))
							notReadyPhotos.remove(hash);
					}, () -> {
//						log.trace("duplicate?");
						notReadyPhotos.remove(hash);
					});
					return photo;
				})
				.whenCompleteAsync((photo, throwable) -> {
					if (Objects.nonNull(photo))
						workOnIt.remove(photo.getHash());
					if (Objects.nonNull(throwable))
						log.error("{} {}", throwable.getCause(), throwable);
				}, executor);
	}

	private Photo action(Photo photo) {
		log.debug("action {{}} photo {{}}", photo.getStatus(), photo);
		switch (photo.getStatus()) {
			case "hash" -> processor.processFileHash(photo.getHash());
			case "exif" -> processor.processExtractExif(photo);
			case "google" -> processor.processObtainGeocode(photo);
			case "resolve" -> processor.processResolveGeocode(photo);
			case "predict" -> processor.processPredict(photo);
			case "move" -> processor.processMove(photo);
			default -> {
				var error = "Undefined process '%s'".formatted(photo.getStatus());
				log.error(error);
				throw new RuntimeException(error);
			}
		}
		return photo;
	}
}
