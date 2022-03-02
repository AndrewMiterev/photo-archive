package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FileMetaService {
    void storeMeta(Photo photo);
    Optional<Photo> getPhoto(String id);
    List<Photo> getAllPhoto();
    void delete(String id);
    List<Photo> getPhotosWithStatus(String nextStep);
    Integer getCount();
    Photo getPhotoOnIndex(Integer indexNumber);
	List<Photo> getPhotosWithNotStatus(String status);
	Map<LocalDate, Integer> getPhotosStatistics();
}
