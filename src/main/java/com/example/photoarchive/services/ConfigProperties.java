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
    @Min(value = 3, message = "Minimum 3. One for each others previous, current and next")
    @Max(value = 10000) // todo for debug. 100 on production
    private Integer cacheSizeNumberFiles;
    @NotBlank
    private String corruptedFolder;
    @NotBlank
    private String unprocessedFolder;
    @Min(value = 1, message = "Minimum 1 photos per 10 minutes")
    @Max(value = 1000, message = "Maximum 1'000. (Comment from programmer: I think no more loaded photos per scan period, next portion on next iteration)")
    private Integer processingNumberPhotosAtSameTime;
}
