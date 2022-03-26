package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.entities.ReadableGeoInfo;

public interface GeocodeService {
    String get(Geo geo);
    ReadableGeoInfo resolve(String geocode);
    String status(String geocode);
	String update(Geo geo);
}
