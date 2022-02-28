package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;

import java.io.InputStream;
import java.time.LocalDateTime;

public interface PhotoArchiveProcessor {
    void processClearAllCollections();
    boolean storeNewFile(String source, InputStream stream, String name, String mime, Long size,  LocalDateTime date, String user);
    void processFileHash(String photoId);
    void processExtractExif(Photo photo);
    void processObtainGeocode(Photo photo);
    void processResolveGeocode(Photo photo);
    void processPredict(Photo photo);
    void processMove(Photo photo);
    void processCheckCollections();
	void processCheckPermanentFolder();
}
