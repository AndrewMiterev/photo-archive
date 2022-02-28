package com.example.photoarchive.domain.repo;

import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.entities.GeoCache;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GeoCacheRepository extends MongoRepository<GeoCache, Geo> {
}
