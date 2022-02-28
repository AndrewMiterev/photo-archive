package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.entities.ReadableGeocode;

public interface GeocodeService {
    String get(Geo geo);
    ReadableGeocode resolve(String geocode);
    String status(String geocode);
}
