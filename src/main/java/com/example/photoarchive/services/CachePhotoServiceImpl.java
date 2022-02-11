package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class CachePhotoServiceImpl implements CachePhotoService {
	private final ConfigProperties config;
	private Map<String, CompletableFuture<byte[]>> map;

	public CachePhotoServiceImpl(ConfigProperties config) {
		this.config = config;
	}

	private class LinkedHashMapWithMaxSize<K, V> extends LinkedHashMap<K, V> {
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > config.getCacheSizeNumberFiles();
		}
	}

	@PostConstruct
	private void postConstruct() {
		map = Collections.synchronizedMap(new LinkedHashMapWithMaxSize<String, CompletableFuture<byte[]>>());
	}

	@Override
	public CompletableFuture<byte[]> save(Photo photo, CompletableFuture<byte[]> data) {
		assert Objects.nonNull(map);
		map.put(photo.getHash(), data);
		return data;
	}

	@Override
	public boolean exists(Photo photo) {
		assert Objects.nonNull(map);
		return map.containsKey(photo.getHash());
	}

	@Override
	public CompletableFuture<byte[]> get(Photo photo) {
		assert Objects.nonNull(map);
		return map.get(photo.getHash());
	}
}
