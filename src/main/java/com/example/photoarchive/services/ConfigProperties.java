package com.example.photoarchive.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@ConfigurationProperties(prefix = "photo-archive")
@Validated
public class ConfigProperties {
	@NotBlank
	private String inFolder;
	@NotBlank
	private String duplicateFolder;
	@NotBlank
	private String processingFolder;
	@NotBlank
	private String manualFolder;
	@NotBlank
	private String permanentFolder;
	@NotBlank
	private String googleApiKey;
	@NotBlank
	private String defaultLanguage;
	@NotBlank
	@Pattern(regexp = "\\[.+[^-_ ]\\]", message = "Regex: Forbidden characters for naming folders. All symbols, " +
			"filtered by regex, will be truncated. It is forbidden to define symbols: ' '(space), '-' and '_'")
	private String forbiddenFolderSymbolsRegex;
	@NotNull
	private Boolean useSubSubFolders;
	@Min(value = -1, message = "-1: Automatic photo change in the slideshow doesn't work. [0-...]: delay in seconds")
	private Integer slideshowDelayBetweenPhotos;
	@Min(value = -1, message = "-1: Automatic photo change in the slideshow doesn't work. [0-...]: delay in seconds")
	private Integer slideshowStartDelayAfterManual;
	@NotNull
	@Min(value = 3, message = "Minimum {value}. One for each others previous, current and next")
	@Max(value = 10000) // todo for debug. 100 on production
	private Integer cacheSizeNumberFiles;
	@NotBlank
	private String corruptedFolder;
	@NotBlank
	private String unprocessedFolder;

	@Min(value = 1, message = "Minimum {value} threads to processing")
	@Max(value = 64, message = "Maximum {value} threads to processing")
	private Integer processingThreads = 4;
	@Min(value = 1, message = "Minimum {value} photos per processing")
	@Max(value = 10000, message = "Maximum {value}. (Comment from programmer: I think no more loaded photos per load period, next portion on next iteration)")
	private Integer processingNumberPhotosAtSameTime = 1000;
	@Min(value = 1000, message = "Minimum {value} milliseconds for delay between loads")
	@Max(value = 24 * 60 * 60 * 1000, message = "Maximum {value} milliseconds for delay between loads. (one day)")
	private Integer processingLoadDelayInMilliseconds;
	@Min(value = 1000, message = "Minimum {value} milliseconds for initial delay between loads")
	@Max(value = 24 * 60 * 60 * 1000, message = "Maximum {value} milliseconds for initial delay between loads. (one day)")
	private Integer processingLoadInitialDelayInMilliseconds;
	@Min(value = 10, message = "Minimum {value} milliseconds on delay tick process scaner")
	private Integer processingTickDelayInMilliseconds;
	@Min(value = 10, message = "Minimum {value} milliseconds on initial delay tick process scaner")
	private Integer processingTickInitialDelayInMilliseconds;
}
