package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Builder
@ToString
@Getter
public class ExifData {
	private Geo geo;
	private LocalDateTime date;
	private String camera;
}
