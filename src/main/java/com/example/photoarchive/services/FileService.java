package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.ExifData;
import com.example.photoarchive.domain.entities.Photo;
import com.vaadin.flow.server.StreamResource;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface FileService {
    String storeNewFile(InputStream stream);
    String getFolderForNewFiles();
    String calculateHash(Photo photo);
    String calculateHash(String folderName, String fileName);
    String getFolderForDuplicates();
    void moveToDuplicates(Photo photo);
    ExifData getExifData(Photo photo);
    void moveToProcessing(Photo photo);
    String getFolderForProcessing();
    void moveToManual(Photo photo);
    String getFolderForManual();
    boolean moveToPermanent(Photo photo);
    String getPermanentFolderFor(Photo photo);
    String getPermanentNameFor(Photo photo);
    CompletableFuture<byte[]> readPhotoDataAsync(Photo photo);
    void callConsumerOnLoad(BiConsumer<StreamResource, Photo> consumer, Photo photo, CompletableFuture<byte[]> data);
	void moveToCorrupted(Photo photo);
    void moveToUnprocessed(String folderName, String fileName);
    void iterateByPermanentFolder(BiConsumer<String, String> consumer);
}
