package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.tools.RingRandomSequence;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
@Service
public class SlideshowServiceImpl implements SlideshowService {
	private final FileMetaService metaService;
	private final FileService fileService;
	private final CachePhotoService cacheService;

	private int photosCount = 0;
	private volatile Photo[] photos = null;
	private LocalDateTime stamp = LocalDateTime.now();
	private CompletableFuture<Void> processReload;

	public SlideshowServiceImpl(FileMetaService metaService, FileService fileService, CachePhotoService cacheService) {
		this.metaService = metaService;
		this.fileService = fileService;
		this.cacheService = cacheService;
	}

	@PostConstruct
	void postConstruct() {
		photosCount = metaService.getCount();
		if (photosCount < 1)
			log.warn("No ready photos in archive");
		stamp = LocalDateTime.now();
		processReload = reload();
	}

	@Override
	public CompletableFuture<Void> reload() {
		log.trace("Start photos reload");
		if (Objects.nonNull(processReload) && !processReload.isDone()) {
			log.debug("The old reload process has not been completed yet");
			return processReload;
		}
		processReload = CompletableFuture.supplyAsync(() -> {
			log.trace("Request list of all photos from database");
			var photos = metaService.getPhotosWithStatus(null)
					.stream()
					.filter(this::isaPhotoImage)
					.toArray(Photo[]::new);
			log.trace("Photos list received. Total in loop {{}} photos", photos.length);
			return photos;
		}).thenAcceptAsync(a -> {
			photos = a;
			photosCount = photos.length;
			stamp = LocalDateTime.now();
		});
		return processReload;
	}

	@Override
	public LocalDateTime directoryIsLoadedAt() {
		return stamp;
	}

	@Override
	public RingRandomSequence makeSequence() {
		if (Objects.isNull(photos) || !processReload.isDone())
			return new RingRandomSequence(photosCount);

		// on these operators can be raise condition by photos array, but to complexity fix it ...
		var mapDirectory2ListFileNumbers = IntStream.range(0, photos.length)
				.mapToObj(i -> Pair.of(photos[i].getFolder(), i))
				.collect(Collectors.groupingBy(Pair::getLeft, Collectors.collectingAndThen(Collectors.toList(),
						list -> list.stream().map(Pair::getRight).toList())));
		var directories = mapDirectory2ListFileNumbers.keySet().toArray(String[]::new);
		var directoriesSequence = new RingRandomSequence(directories.length);

		var numberForCommonSequence = IntStream.range(0, directories.length)
				.mapToObj(i0 -> {
					var filesInDirectory = mapDirectory2ListFileNumbers
							.get(directories[directoriesSequence.next()])
							.toArray(Integer[]::new);
					var sizeOfDirectory = filesInDirectory.length;
					var sequence = new RingRandomSequence(sizeOfDirectory);
					return IntStream.range(0, sizeOfDirectory)
							.mapToObj(i1 -> filesInDirectory[sequence.next()]);
				})
				.flatMap(s -> s)
				.mapToInt(Integer::intValue)
				.toArray();

		var result = new RingRandomSequence(numberForCommonSequence);

//		log.debug("Random Big Ring {}", result);
		if (result.size() != photos.length)
			throw new RuntimeException("Internal error! Random photos array size not equivalent to repository select");
		return result;
	}

	private CompletableFuture<byte[]> readCachedPhotoData(Photo photo) {
		if (cacheService.exists(photo))
			return cacheService.get(photo);
		return cacheService.save(photo, fileService.readPhotoDataAsync(photo));
	}

	/**
	 * heic
	 * <dependencies>
	 * <dependency>
	 * <groupId>com.idrsolutions</groupId>
	 * <artifactId>jdeli</artifactId>
	 * <version>1.0</version>
	 * </dependency>
	 * </dependencies>
	 * <p>
	 * https://support.idrsolutions.com/jdeli/tutorials/converting/all/heic/convert-heic-to-jpg
	 */
	// todo change in final version (also .HIEC e.t.s.)
	private boolean isaPhotoImage(Photo p) {
		return p.getName().toLowerCase().endsWith(".jpg");
	}

	private void preheatPhoto(RingRandomSequence sequence, int offset) {
		var photo = getPhoto(sequence, offset);
		readCachedPhotoData(photo);
	}

	private Photo findPhotoBySequence(Function<Integer, Integer> changer) {
		for (int i = 0; i < photosCount; i++) {
			var photo = metaService.getPhotoOnIndex(changer.apply(i));
			if (isaPhotoImage(photo) && Objects.isNull(photo.getStatus())) return photo;
		}
		throw new RuntimeException("Read pre-ahead error");
	}

	private Photo getPhoto(RingRandomSequence sequence, int offset) {
		if (Objects.nonNull(photos)) return photos[sequence.suppose(offset)];
		return findPhotoBySequence(r -> sequence.suppose(offset > 0 ? offset + r : offset - r));
	}

	private Photo getNextPhoto(RingRandomSequence sequence) {
		if (Objects.nonNull(photos)) return photos[sequence.next()];
		return findPhotoBySequence(o -> sequence.next());
	}

	private Photo getPreviousPhoto(RingRandomSequence sequence) {
		if (Objects.nonNull(photos)) return photos[sequence.previous()];
		return findPhotoBySequence(o -> sequence.previous());
	}

	private Photo getCurrentPhoto(RingRandomSequence sequence) {
		if (Objects.nonNull(photos)) return photos[sequence.current()];
		return findPhotoBySequence(sequence::suppose);
	}

	@Override
	public void current(RingRandomSequence sequence, BiConsumer<StreamResource, Photo> consumer) {
		Photo photo = getCurrentPhoto(sequence);
		fileService.callConsumerOnLoad(consumer, photo, readCachedPhotoData(photo));
		preheatPhoto(sequence, 1);
		preheatPhoto(sequence, -1);
	}

	@Override
	public void next(RingRandomSequence sequence, BiConsumer<StreamResource, Photo> consumer) {
		Photo photo = getNextPhoto(sequence);
		fileService.callConsumerOnLoad(consumer, photo, readCachedPhotoData(photo));
		preheatPhoto(sequence, 1);
		preheatPhoto(sequence, 2);
	}

	@Override
	public void previous(RingRandomSequence sequence, BiConsumer<StreamResource, Photo> consumer) {
		Photo photo = getPreviousPhoto(sequence);
		fileService.callConsumerOnLoad(consumer, photo, readCachedPhotoData(photo));
		preheatPhoto(sequence, -1);
		preheatPhoto(sequence, -2);
	}

	@Override
	public boolean isEmpty() {
		return photosCount == 0;
	}
}
