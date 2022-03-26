package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.util.List;
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
	List<Pair<LocalDate, Integer>> getPhotosCountByDate();
	List<Pair<String, Integer>> getPhotosCountByStatus();
	List<Pair<String, Integer>> getPhotosCountByMime();
	List<Photo> getPhotosWithStatusNotNullAndStatusNotManual(Integer maxPhotosForRobot);
}
