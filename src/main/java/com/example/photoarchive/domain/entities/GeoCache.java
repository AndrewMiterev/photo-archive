package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document
public class GeoCache {
	@Id
	private Geo geo;
	private String geoCode;
}
