package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Original;
import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.domain.entities.PredictName;
import com.example.photoarchive.domain.repo.PhotoRepository;
import com.example.photoarchive.domain.repo.ProtocolRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Objects;

@Log4j2
@Service
public class PhotoArchiveProcessorImpl implements PhotoArchiveProcessor {
	private final FileService fileService;
	private final FileMetaService metaService;
	private final ProtocolService protocolService;
	private final GeocodeService geoService;
	private final PredictService predictService;
	private final PhotoRepository photoRepository;
	private final ProtocolRepository protocolRepository;

	public PhotoArchiveProcessorImpl(FileService fileService, FileMetaService metaService,
									 ProtocolService protocolService, GeocodeService geoService,
									 PredictService predictService, PhotoRepository photoRepository, ProtocolRepository protocolRepository) {
		this.fileService = fileService;
		this.metaService = metaService;
		this.protocolService = protocolService;
		this.geoService = geoService;
		this.predictService = predictService;
		this.photoRepository = photoRepository;
		this.protocolRepository = protocolRepository;
	}

	@Override
	public void processClearAllCollections() {
		log.trace("clear all collections");
		photoRepository.deleteAll();
		protocolRepository.deleteAll();
//		metaService.getAllPhoto().forEach(p -> metaService.delete(p.getHash()));
//		protocolService.getAll().forEach(protocolService::delete);
	}

	@Override
	public boolean storeNewFile(String source, InputStream stream, String name, String mime, Long size, LocalDateTime date, String user) {
		log.trace("store new file. Source {{}} user {{}} original name {{}} date {{}} mime {{}} size {{}}", source, user, name, date, mime, size);
		var newName = fileService.storeNewFile(stream);
		if (newName == null) return false;
		log.debug("allocated name {{}}", newName);
		var photo = Photo.builder()
				.hash(newName)
				.name(newName)
				.folder(fileService.getFolderForNewFiles())
				.status("hash")
				.user(user)
				.original(Original.builder()
						.name(name)
						.source(source)
						.mime(mime)
						.size(size)
						.date(date)
						.build())
				.build();
		metaService.storeMeta(photo);
		log.debug("New photo stored {{}}", photo);
		protocolService.add("Info", "File {%s} loaded by {%s} on {%s}".formatted(name, user, source));
		return true;
	}

	@Override
	public void processFileHash(String id) {
		log.trace("hashing process for {{}}", id);
		Photo photo = metaService.getPhoto(id)
				.filter(p -> Objects.nonNull(p.getStatus()))
				.filter(p -> p.getStatus().equalsIgnoreCase("hash"))
				.orElseThrow();
		var hash = fileService.calculateHash(photo);
		metaService.delete(photo.getHash());
		photo.setHash(hash);
		log.debug("Photo hashed {}", photo);
		metaService.getPhoto(hash).ifPresentOrElse(p -> {
			fileService.moveToDuplicates(photo);
			protocolService.add("Warning",
					"Already loaded photo {%s}. Move to folder {%s}. Previous photo with same hash {%s}"
							.formatted(
									photo.getOriginal().getName()
									, fileService.getFolderForDuplicates()
									, p.getOriginal()
							)
			);
			log.warn("Already loaded photo {}. Move to folder {}. Previous photo with same hash {}",
					photo.getOriginal().getName(), fileService.getFolderForDuplicates(), p);
		}, () -> {
			fileService.moveToProcessing(photo);
			photo.setStatus("exif");
			metaService.storeMeta(photo);
			log.trace("Stored photo to collection {}", photo);
		});
	}

	@Override
	public void processExtractExif(Photo photo) {
		log.trace("the process of extracting Exif information for {}", photo);
		var exifData = fileService.getExifData(photo);
		log.debug("Exif data {}", exifData);
		if (Objects.isNull(exifData) || Objects.isNull(exifData.getGeo())) {
			fileService.moveToManual(photo);
			photo.setStatus("manual");
			protocolService.add("Warning", "Photo {%s} without Exif information moved to manual distribution".formatted(photo.getOriginal().getName()));
			log.info("Photo without Exif information moved to manual distribution folder {} {} ", fileService.getFolderForManual(), photo);
		} else {
			photo.setExifData(exifData);
			photo.setStatus("google");
			log.trace("Photo to reverse geocoding {}", photo);
		}
		metaService.storeMeta(photo);
	}

	@Override
	public void processObtainGeocode(Photo photo) {
		log.trace("the process of obtaining a geocode for {}", photo);
		var geocode = geoService.get(photo.getExifData().getGeo());
		log.debug("GEO code {}", geocode);
		if (Objects.nonNull(geocode)) {
			photo.setGeocode(geocode);
			var response = geoService.status(geocode);
			if (Objects.nonNull(response) && response.equalsIgnoreCase("ok")) {
				photo.setStatus("resolve");
				log.info("Data received from reverse geocode service for {}", photo);
			} else {
				log.warn("Response from reverse geocode service {} for {}", response, photo);
				protocolService.add("Warning", "Response from reverse geocode service {%s} for photo {%s}".formatted(response, photo.getOriginal().getName()));
			}
			metaService.storeMeta(photo);
		} else {
			log.error("Error request GEO code for {}", photo);
		}
	}

	@Override
	public void processResolveGeocode(Photo photo) {
		log.trace("the process of resolving a geographical name for {}", photo);
		var response = geoService.resolve(photo.getGeocode());
		log.debug("resolved GEO code {}", response);
		if (Objects.nonNull(response)) {
			photo.setGeoInfo(response);
			photo.setStatus("predict");
			log.trace("Photo to name prediction {}", photo);
		} else {
			fileService.moveToManual(photo);
			photo.setStatus("manual");
			protocolService.add("Warning", "Photo {%s} can't resolve geocode and moved to manual distribution".formatted(photo.getOriginal().getName()));
			log.info("Photo can't resolve geocode and moved to manual distribution {} {}", fileService.getFolderForManual(), photo);
		}
		metaService.storeMeta(photo);
	}

	@Override
	public void processPredict(Photo photo) {
		log.trace("the process predicts folder and file names for {}", photo);
		PredictName result = predictService.get(photo);
		log.debug("prediction {}", result);
		if (Objects.nonNull(result)) {
			photo.setPredict(result);
			photo.setStatus("move");
			log.trace("Photo to moving {}", photo);
		} else {
			fileService.moveToManual(photo);
			photo.setStatus("manual");
			protocolService.add("Warning", "Photo {%s} can't predicted and moved to manual distribution".formatted(photo.getOriginal().getName()));
			log.info("Photo can't predicted and moved to manual distribution {} {}", fileService.getFolderForManual(), photo);
		}
		metaService.storeMeta(photo);
	}

	@Override
	public void processMove(Photo photo) {
		log.trace("the process of moving to a permanent place {}", photo);
		if (fileService.moveToPermanent(photo)) {
			photo.setDate(photo.getPredict().getDate());
			photo.setTitle(photo.getPredict().getTitle());
			photo.setStatus(null);
			photo.setGeocode(null);
			photo.setPredict(null);
			metaService.storeMeta(photo);
		} else {
			log.debug("folder {}", fileService.getPermanentFolderFor(photo));
			log.debug("file {}", fileService.getPermanentNameFor(photo));
			protocolService.add("Error", "Photo {%s} can't me stored in {%s} with name {%s}".formatted(
					photo.getOriginal().getName(),
					fileService.getPermanentFolderFor(photo),
					fileService.getPermanentNameFor(photo)
			));
			log.error("Photo can't me stored to permanent place with name {} {} {}", photo,
					fileService.getPermanentFolderFor(photo), fileService.getPermanentNameFor(photo));
		}
	}

	public void processCheckCollections() {
		log.trace("validate all collections");
		metaService.getPhotosWithStatus("hash").forEach(p -> {
			log.debug("check availability file {{}}", p.getName());
			if (!fileService.fileExists(p.getFolder(), p.getName())) {
				protocolService.add("Warning", "Photo file not found {%s}, {%s}".formatted(p.getFolder(), p.getName()));
				log.warn("Photo file not found {}, {}", p.getFolder(), p.getName());
				metaService.delete(p.getHash());
			}
		});
		metaService.getPhotosWithNotStatus("hash").forEach(p -> {
			var hash = fileService.calculateHash(p);
			log.debug("calculated hash {} for {}", hash, p);
			if (!p.getHash().equals(hash)) {
				fileService.moveToCorrupted(p);
				protocolService.add("Warning", "Photo has another hash {%s}, {%s}".formatted(hash, p));
				log.warn("Photo has another hash {}, {}", hash, p);
				metaService.delete(p.getHash());
			}
		});
	}

	public void processCheckPermanentFolder() {
		log.trace("scan permanent folder for unlinked files");
		fileService.iteratePossibleFolders((folderName, fileName) -> {
//			log.debug("check consumer take folder {{}} and file {{}}", folderName, fileName);
			var hash = fileService.calculateHash(folderName, fileName);
//			log.debug("hash is {{}}", hash);
			var photo = metaService.getPhoto(hash);
//			log.debug("photo {{}}", photo);
			if (photo.isEmpty()) {
				fileService.moveToUnprocessed(folderName, fileName);
				protocolService.add("Warning", "File not presented in meta {%s} {%s}".formatted(folderName, fileName));
				log.warn("File not presented in meta moved to unprocessed {{}} {{}} {{}}", folderName, fileName, fileService.getFolderForUnprocessed());
			}
		});
	}
}
