package com.example.photoarchive.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

import javax.annotation.PostConstruct;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class MongoDBConfig implements InitializingBean {
	@Lazy
	private final MappingMongoConverter converter;

	@Override
	public void afterPropertiesSet() throws Exception {
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
	}

	@PostConstruct
	void postConstruct() {
		log.info("MongoDB configuration: saving without names of classes ...");
	}
}
