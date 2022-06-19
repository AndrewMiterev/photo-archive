package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class Geo {
	private double latitude;
	private double longitude;
	private Double altitude;
}
