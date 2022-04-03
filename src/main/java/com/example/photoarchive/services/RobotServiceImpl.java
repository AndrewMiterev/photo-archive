package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Optional;
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
		var oneLoadProcess = CompletableFuture.runAsync(() -> {
			var maxPhotosForRobot = properties.getProcessingNumberPhotosAtSameTime();
			var photosList = service.getPhotosWithStatusNotNullAndStatusNotManual(maxPhotosForRobot)
					.stream()
					.map(Photo::getHash)
					.toList();
			// clear accumulated errors (throws e.t.s.)
			if (notReadyPhotos.size() == 0 && photosList.isEmpty()) workOnIt.clear();
//		log.debug("Number photos for processing {{}}", photosList.size());
			notReadyPhotos.addAll(photosList);
		}, executor);
	}

	@Scheduled(fixedDelayString = "${photo-archive.processing-tick-delay-in-milliseconds:0}",
			initialDelayString = "${photo-archive.processing-tick-initial-delay-in-milliseconds:0}")
	@Override
	public void tick() {
		var onePhotoProcess = CompletableFuture.runAsync(() ->
			notReadyPhotos.stream()
					.filter(hash -> !workOnIt.contains(hash))
					.findAny()
					.ifPresent(this::process)
		, executor);
	}

	private void process(String photoHash) {
		var chain = CompletableFuture.completedFuture(photoHash)
				.thenApplyAsync(this::lockHash, executor)
				.thenApplyAsync(service::getPhoto, executor)
				.thenApplyAsync(Optional::orElseThrow, executor)
				.thenApplyAsync(this::action, executor)
				.thenApplyAsync(photo -> {
					var hash = photo.getHash();
					service.getPhoto(hash).ifPresentOrElse(p -> {
						if (Objects.isNull(p.getStatus()) || p.getStatus().equalsIgnoreCase("manual"))
							notReadyPhotos.remove(hash);
					}, () -> notReadyPhotos.remove(hash));
					return hash;
				}, executor)
				.whenCompleteAsync((hash, throwable) -> {
					if (Objects.nonNull(hash))
						if (!workOnIt.remove(hash))
							log.error("!!! Unrecoverable error! lock free error!");
					if (Objects.nonNull(throwable)) {
						if (!(throwable instanceof LockException))
							log.error("exception {{}} error {{}}", throwable.getCause(), throwable);
					}
				}, executor);
	}

	private String lockHash(String hash) {
		if (!workOnIt.add(hash)) throw new LockException(hash);
		return hash;
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
