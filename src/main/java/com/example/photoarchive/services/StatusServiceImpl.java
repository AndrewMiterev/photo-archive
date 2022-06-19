package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Log4j2
@Service
public class StatusServiceImpl implements StatusService {
	private Map<String, Consumer<Photo>> map = new HashMap<>();

	@Override
	public void register(String status, Consumer<Photo> processor) {
		map.put(status, processor);
	}

	@Override
	public void process(Photo photo) {
		map.getOrDefault(photo.getStatus(), p -> {
			log.info("Processing photo stopped. Status {}", p.getStatus());
		}).accept(photo);
	}
}
