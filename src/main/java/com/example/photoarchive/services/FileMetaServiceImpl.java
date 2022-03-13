package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.domain.repo.PhotoRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

@Log4j2
@Service
public class FileMetaServiceImpl implements FileMetaService {
	private final PhotoRepository repository;
	private final MongoTemplate template;

	public FileMetaServiceImpl(PhotoRepository repository, MongoTemplate template) {
		this.repository = repository;
		this.template = template;
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

	static class DateCount {
		public LocalDate date;
		public Integer count;
	}
	static class YearCount {
		public String year;
		public Integer count;
	}

	@Override
	public Map<LocalDate, Integer> getPhotosStatistics() {
		ProjectionOperation toDateProjectOperation = Aggregation.project("date")
				.andExpression("toDate(toLong(date))")
				.as("formattedServerDate");

		ProjectionOperation dateToStringProjectOperation = Aggregation.project("date")
				.and("formattedServerDate")
				.dateAsFormattedString("%d-%m-%Y")
				.as("formattedServerDate");

		Aggregation agg = newAggregation(
				 match(Criteria.where("date").exists(true))
				, project().and("date").dateAsFormattedString("%Y-%m-%d").as("year")
				, project("year").andExpression("toDate(year)").as("year")
				, group("year").count().as("count")
				, project("count").and("year").previousOperation()
				, sort(Sort.Direction.ASC, "year")
		);
		AggregationResults<YearCount> results = template.aggregate(agg, "photo", YearCount.class);
		var aaa = results.getMappedResults();
		HashMap<LocalDate, Integer> map = new HashMap<>();
		aaa.forEach(d -> {
			log.debug("{} {}", d.year, d.count);
//			map.put(d.date, d.count);
		});

		return null;
	}
}
