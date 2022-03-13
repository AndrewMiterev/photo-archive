package com.example.photoarchive.domain.repo;

import com.example.photoarchive.domain.entities.Photo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PhotoRepository extends MongoRepository<Photo, String> {
	List<Photo> findAllByStatus(String status);

	List<Photo> findAllByStatusNot(String status);

}
