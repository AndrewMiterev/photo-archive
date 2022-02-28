package com.example.photoarchive.services;


import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.example.photoarchive.domain.entities.ExifData;
import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.entities.Photo;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Log4j2
@Service
public class FileServiceImpl implements FileService {
	private final ConfigProperties config;
	private final PredictService service;

	public FileServiceImpl(ConfigProperties config, PredictService service) {
		this.config = config;
		this.service = service;
	}

	private LocalDateTime convertToLocalDateTime(Date dateToConvert) {
		if (Objects.isNull(dateToConvert)) return null;
		return LocalDateTime.ofInstant(dateToConvert.toInstant(), ZoneId.systemDefault());
	}

	private boolean moveToWithPrefix(Photo photo, String toFolder, String prefix) {
		var filename = service.filenameWithHash(photo, prefix);
		return moveTo(photo, toFolder, filename);
	}

	private boolean moveTo(Photo photo, String toFolder) {
		return moveToWithPrefix(photo, toFolder, "");
	}

	private boolean moveTo(Photo photo, String toFolder, String newFileName) {
		if (moveTo(photo.getFolder(), photo.getName(), toFolder, newFileName)) {
			photo.setFolder(toFolder);
			photo.setName(newFileName);
			return true;
		}
		return false;
	}

	private boolean moveTo(String fromFolder, String fromFileName, String toFolder, String toFileName) {
		try {
			var sourceFile = Paths.get(fromFolder, fromFileName).toFile();
			var destinationFile = Paths.get(toFolder, toFileName).toFile();
			FileUtils.moveFile(
					sourceFile,
					destinationFile
			);
		} catch (IOException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	private Double getAltitude(GpsDirectory gpsDirectory) {
		Rational altitude = gpsDirectory.getRational(6);
		if (Objects.isNull(altitude)) return null;
		String altitudeRef = gpsDirectory.getString(5);
		if (Objects.isNull(altitudeRef)) return null;
		double decimal = Math.abs(altitude.doubleValue());
		if (Double.isNaN(decimal)) return null;
		if (!altitudeRef.equalsIgnoreCase("0")) decimal *= -1.0D;
		return decimal;
	}

	private Date getDateWithoutTime(GpsDirectory gpsDirectory) {
		String date = gpsDirectory.getString(29);
		if (Objects.isNull(date)) return null;
		try {
			DateFormat parser = new SimpleDateFormat("yyyy:MM:dd");
			return parser.parse(date);
		} catch (ParseException e) {
			return null;
		}
	}

	@Override
	public String getFolderForNewFiles() {
		return config.getInFolder();
	}

	@Override
	public String storeNewFile(InputStream stream) {
		try {
			File outDirectory = new File(getFolderForNewFiles());
			var created = outDirectory.mkdir();
			File out = File.createTempFile("downloaded-", "", outDirectory);
			FileUtils.copyInputStreamToFile(stream, out);
			log.info("stored new file {}", out.getPath());
			return out.getName();
		} catch (IOException e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public String calculateHash(Photo photo) {
		return calculateHash(photo.getFolder(), photo.getName());
	}

	@Override
	public String calculateHash(String folderName, String fileName) {
		try (InputStream is = Files.newInputStream(Paths.get(folderName, fileName))) {
			return DigestUtils.md5Hex(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getFolderForDuplicates() {
		return config.getDuplicateFolder();
	}

	@Override
	public void moveToDuplicates(Photo photo) {
		moveToWithPrefix(photo, getFolderForDuplicates(), service.getFormattedDateTime(photo.getOriginal().getDate()));
	}

	@Override
	public ExifData getExifData(Photo photo) {
		LocalDateTime date0 = null;
		LocalDateTime date1 = null;
		LocalDateTime date2 = null;
		LocalDateTime date3 = null;
		Geo geo = null;
		LocalDateTime dateGps = null;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(Paths.get(photo.getFolder(), photo.getName()).toFile());
//            for (Directory directory : metadata.getDirectories()) {
//                log.debug("   Directory {}", directory.getName());
//                for (Tag tag : directory.getTags()) {
//                    log.debug("      Tag {} {} {}", tag.getTagName(), tag.getDescription(), tag.toString());
//                }
//            }
			if (metadata == null) {
				log.debug("! metadata is null");
				return null;
			}
			ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (Objects.nonNull(exifIFD0Directory))
				date0 = convertToLocalDateTime(exifIFD0Directory.getDate(ExifIFD0Directory.TAG_DATETIME));

			ExifSubIFDDirectory exifSubIDDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			if (Objects.nonNull(exifSubIDDDirectory)) {
				date1 = convertToLocalDateTime(exifSubIDDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME));
				date2 = convertToLocalDateTime(exifSubIDDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
				date3 = convertToLocalDateTime(exifSubIDDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
			}

			GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
			if (Objects.nonNull(gpsDir)) {
				GeoLocation location = gpsDir.getGeoLocation();
				if (Objects.nonNull(location) && !location.isZero()) {
					geo = Geo.builder()
							.longitude(location.getLongitude())
							.latitude(location.getLatitude())
							.altitude(getAltitude(gpsDir))
							.build();
				}
				dateGps = convertToLocalDateTime(gpsDir.getGpsDate());
				if (Objects.isNull(dateGps))
					dateGps = convertToLocalDateTime(getDateWithoutTime(gpsDir));
			}

			LocalDateTime dateExif = ObjectUtils.firstNonNull(date0, date1, date2, date3);
			LocalDateTime photoDate = ObjectUtils.firstNonNull(dateGps, dateExif);

			if (Objects.nonNull(photoDate)) {
				if (Objects.nonNull(dateGps) && Objects.nonNull(dateExif) && dateGps.toLocalDate().compareTo(dateExif.toLocalDate()) != 0)
					log.warn("GPS Date and Exif date too different! {} and {}", dateGps, dateExif);
				if (photoDate.getHour() == 0 && photoDate.getMinute() == 0 && photoDate.getSecond() == 0) {
					log.warn("photo date is ZERO! {}. Try ro repair it", photoDate);
					if (Objects.nonNull(dateExif))
						photoDate = photoDate
								.withHour(dateExif.getHour())
								.withMinute(dateExif.getMinute())
								.withSecond(dateExif.getSecond());
				}
			}
			return ExifData.builder()
					.geo(geo)
					.date(photoDate)
					.build();
		} catch (ImageProcessingException e) {
			log.warn("the file is not a Photo {} {}", photo, e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void moveToProcessing(Photo photo) {
		moveTo(photo, getFolderForProcessing());
	}

	@Override
	public String getFolderForProcessing() {
		return config.getProcessingFolder();
	}

	@Override
	public void moveToManual(Photo photo) {
		moveTo(photo, getFolderForManual());
	}

	@Override
	public String getFolderForManual() {
		return config.getManualFolder();
	}

	@Override
	public boolean moveToPermanent(Photo photo) {
		return moveTo(photo, getPermanentFolderFor(photo), getPermanentNameFor(photo));
	}

	@Override
	public String getPermanentFolderFor(Photo photo) {
		File file = FileUtils.getFile(
				Stream.of(
								config.getPermanentFolder(),
								photo.getPredict().getFolder(),
								photo.getPredict().getSubFolder(),
								photo.getPredict().getSubSubFolder()
						)
						.filter(Objects::nonNull)
						.toArray(String[]::new));
		return file.getPath();
	}

	@Override
	public String getPermanentNameFor(Photo photo) {
		return photo.getPredict().getName();
	}

	@Override
	public CompletableFuture<byte[]> readPhotoDataAsync(Photo photo) {
		return CompletableFuture.supplyAsync(() -> {
			File file = Paths.get(photo.getFolder(), photo.getName()).toFile();
			try (FileInputStream inputStream = new FileInputStream(file)) {
				return IOUtils.toByteArray(inputStream);
			} catch (IOException e) {
				log.error("Can't read photo data {}", photo);
				return null;
			}
		});
	}

	@Override
	public void callConsumerOnLoad(BiConsumer<StreamResource, Photo> consumer, Photo photo, CompletableFuture<byte[]> data) {
		assert Objects.nonNull(consumer) && Objects.nonNull(photo) && Objects.nonNull(data);
		if (data.isDone()) {
			try {
				var buffer = data.get();
				consumer.accept(new StreamResource(photo.getName(), () -> new ByteArrayInputStream(buffer)), photo);
			} catch (InterruptedException | ExecutionException e) {
				log.error(e);
			}
		} else data.thenAcceptAsync(d ->
				consumer.accept(new StreamResource(photo.getName(), () -> new ByteArrayInputStream(d)), photo));
	}

	@Override
	public void moveToCorrupted(Photo photo) {
		moveToWithPrefix(photo, getFolderForCorrupted(), "corrupted-%s-".formatted(calculateHash(photo)));
	}

	@Override
	public void moveToUnprocessed(String folderName, String fileName) {
		moveTo(folderName, fileName, getFolderForUnprocessed(), "unprocessed-%s-%s".formatted(calculateHash(folderName, fileName), fileName));
	}

	private String getFolderForCorrupted() {
		return config.getCorruptedFolder();
	}

	@Override
	public void iterateByPermanentFolder(BiConsumer<String, String> fileConsumer) {
		Objects.requireNonNull(fileConsumer);
		Path permanentRoot = Path.of(config.getPermanentFolder());
		log.trace("start iterate walk by permanent folder {{}}", permanentRoot);
		try {
			Files.walk(permanentRoot)
					.parallel()
					.filter(Files::isRegularFile)
					.forEach(path -> fileConsumer.accept(path.getParent().toString(), path.getFileName().toString()));
		} catch (RuntimeException e) {
			log.warn("Walk on {{}}. {{}}", permanentRoot, e.getMessage());
			throw new RuntimeException(e);
		} catch (IOException e) {
			log.warn("IOException directory {{}}. {{}}", permanentRoot, e.getMessage());
			throw new RuntimeException(e);
		}
		log.trace("finish iterate walk {{}}", permanentRoot);
	}

	@Override
	public String getFolderForUnprocessed() {
		return config.getUnprocessedFolder();
	}
}
