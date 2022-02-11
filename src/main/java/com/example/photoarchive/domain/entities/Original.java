package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;

@Builder
@ToString
@Getter
public class Original {
    private String name;
    private String folder;
    private String source;
    private String mime;
    private Long size;
    private LocalDateTime date;
}
