package com.example.photoarchive.experiment;

import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.repo.GeoCacheRepository;
import com.example.photoarchive.domain.repo.PhotoRepository;
import com.example.photoarchive.services.FileMetaService;
import com.example.photoarchive.services.GeocodeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

@Service
@Log4j2
public class GeoConvertor {
	private final GeoCacheRepository geoCacheRepository;
	private final PhotoRepository photoRepository;
	private final MongoTemplate template;
	private final GeocodeService service;
	private final FileMetaService metaService;

	public GeoConvertor(GeoCacheRepository repository, PhotoRepository photoRepository, MongoTemplate template, GeocodeService service, FileMetaService metaService) {
		this.geoCacheRepository = repository;
		this.photoRepository = photoRepository;
		this.template = template;
		this.service = service;
		this.metaService = metaService;
	}

	public List<Geo> getGeoList() {
		Aggregation agg = newAggregation(
				project("_id.latitude", "_id.longitude", "_id.altitude")
		);
		AggregationResults<Geo> results = template.aggregate(agg, "geo-cache", Geo.class);
//		results.forEach(r->log.debug("{{}}", r));
		return results.getMappedResults();
	}

	public void calculate() {
		metaService.getPhotosWithStatusNotNullAndStatusNotManual(2)
				.forEach(log::debug);
	}

	public void calculate_geo() {
		var list = getGeoList();
		for (var geo : list) {
			geoCacheRepository.findById(geo).ifPresent(c -> {
				var r1 = service.resolve(c.getGeoCode());
				if (containNumbers(r1.getCountry()) ||
						containNumbers(r1.getLocality()) ||
						containNumbers(r1.getAddress())
				)
					log.debug("{{}} {{}} {{}} {{}} {{}}", geo, c, r1.getCountry(), r1.getLocality(), r1.getAddress());
				if (!r1.getPoi().isEmpty())
					log.debug("{{}} {{}} {{}} {{}} {{}} {{}}", geo, c, r1.getCountry(), r1.getLocality(), r1.getAddress(), r1.getPoi());
			});
		}
	}

	private boolean containNumbers(String str) {
//		log.debug("{{}}", str);
		var numDigit = 0;
		for (int i = 0; i < str.length(); i++) {
			if ("0123456789".indexOf(str.charAt(i)) > 0)
				numDigit++;
			else numDigit = 0;
			if (numDigit > 4) return true;
		}
		return false;
	}

}
