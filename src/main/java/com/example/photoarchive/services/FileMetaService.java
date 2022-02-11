package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetaService {
    void storeMeta(Photo photo);
    Optional<Photo> getPhoto(String id);
    List<Photo> getAllPhoto();
    void delete(String id);
    List<Photo> getPhotosWithStatus(String nextStep);
    Integer getCount();
    Photo getPhotoOnIndex(Integer indexNumber);
}
