package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.domain.entities.PredictName;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

@Log4j2
@Service
public class PredictServiceImpl implements PredictService {
	private final ConfigProperties config;

	public PredictServiceImpl(ConfigProperties config) {
		this.config = config;
	}

	private String concatBy(String space, String... strings) {
		StringBuilder builder = new StringBuilder();
		for (String string : strings)
			if (Objects.nonNull(string) && !string.isEmpty()) {
				if (!builder.isEmpty())
					builder.append(space);
				builder.append(string.replaceAll(config.getForbiddenFolderSymbolsRegex(), ""));
			}
		return builder.toString();
	}

	@Override
	public String getFormattedDateTime(LocalDateTime date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
		return date.format(formatter);
	}


	@Override
	public String filenameWithHash(Photo photo, String prefix) {
		var baseName = FilenameUtils.getBaseName(photo.getOriginal().getName());
		var extension = FilenameUtils.getExtension(photo.getOriginal().getName());
		return concatBy(".", concatBy("_", prefix, photo.getHash(), baseName), extension);
	}

	@Override
	public PredictName get(Photo photo) {
		var date = ObjectUtils.firstNonNull(
				Objects.nonNull(photo.getExifData()) ? photo.getExifData().getDate() : null,
				Objects.nonNull(photo.getOriginal()) ? photo.getOriginal().getDate() : null
		);
		if (Objects.isNull(date)) {
			log.error("Date for photo not defined {}", photo);
			return null;
		}
		var locale = Locale.forLanguageTag(config.getDefaultLanguage());
		String yearName = "%d".formatted(date.getYear());
		String monthName = date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale);
		String dateName = date.toLocalDate().toString();

		String dateTimeFilename = getFormattedDateTime(date);

		if (Objects.isNull(photo.getOriginal()) || Objects.isNull(photo.getOriginal().getName())) {
			log.error("Original/name for photo not defined {}", photo);
			return null;
		}

		String folder = yearName;
		String subFolder = monthName;
		String subSubFolder = dateName;
		String fileName = dateTimeFilename;

		var geocode = photo.getReadableGeocode();
		if (Objects.nonNull(geocode)) {
			if (!ObjectUtils.allNotNull(geocode.getCountry(), geocode.getLocality(), geocode.getAddress())) {
				log.error("Not all readable geocodes for photos are defined {}", photo);
				return null;
			}
			folder = concatBy(". ", folder, geocode.getCountry());
			subFolder = concatBy(". ", monthName, geocode.getLocality());
			subSubFolder = concatBy(". ", dateName, geocode.getAddress());
			fileName = concatBy("_", fileName, geocode.getAddress().replaceAll(" ", "_"));
		}

		fileName = filenameWithHash(photo, fileName);

		return PredictName.builder()
				.folder(folder)
				.subFolder(subFolder)
				.subSubFolder(config.getUseSubSubFolders() ? subSubFolder : null)
				.name(fileName)
				.build();
	}
}
