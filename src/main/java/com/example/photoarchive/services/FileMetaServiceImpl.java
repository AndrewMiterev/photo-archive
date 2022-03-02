package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.domain.repo.PhotoRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class FileMetaServiceImpl implements FileMetaService {
    private final PhotoRepository repository;

    public FileMetaServiceImpl(PhotoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void storeMeta(Photo photo) {
        repository.save(photo);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Photo> getPhoto(String id) {
        return repository.findById(id);
    }

    @Override
    public List<Photo> getAllPhoto() {
        return repository.findAll();
    }

    @Override
    public List<Photo> getPhotosWithStatus(String nextStep) {
        return repository.findAllByStatus(nextStep);
    }

    @Override
    public Integer getCount() {
        return Math.toIntExact(repository.count());
    }

    @Override
    public Photo getPhotoOnIndex(Integer recordIndex) {
        Pageable pageable = PageRequest.of(recordIndex, 1);
        return repository.findAll(pageable).get().findFirst().orElseThrow();
    }

    @Override
    public List<Photo> getPhotosWithNotStatus(String status) {
        return repository.findAllByStatusNot(status);
    }

    @Override
    public Map<LocalDate, Integer> getPhotosStatistics() {
        return null;
    }
}
