package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.domain.repo.PhotoRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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

	private static class DateCount {
		LocalDate date;
		Integer count;
	}

	@Override
	public List<Pair<LocalDate, Integer>> getPhotosCountByDate() {
		ProjectionOperation toDateProjectOperation = Aggregation.project("date")
				.andExpression("toDate(toLong(date))")
				.as("formattedServerDate");

		ProjectionOperation dateToStringProjectOperation = Aggregation.project("date")
				.and("formattedServerDate")
				.dateAsFormattedString("%d-%m-%Y")
				.as("formattedServerDate");

		Aggregation agg = newAggregation(
				match(Criteria.where("date").exists(true))
				, project().and("date").dateAsFormattedString("%Y-%m-%d").as("date")
				, project("date").andExpression("toDate(date)").as("date")
				, group("date").count().as("count")
				, project("count").and("date").previousOperation()
				, sort(Sort.Direction.ASC, "date")
		);
		AggregationResults<DateCount> results = template.aggregate(agg, "photo", DateCount.class);
//		results.forEach(r->log.debug(" date {{}} count {{}}", r.date, r.count));
		return results.getMappedResults().stream().map(r -> Pair.of(r.date, r.count)).toList();
	}

	private static class StatusCount {
		String name;
		Integer count;
	}

	@Override
	public List<Pair<String, Integer>> getPhotosCountByStatus() {
		Aggregation agg = newAggregation(
				group("status").count().as("count")
				, project("count").and("status").previousOperation()
				, project("count").and("status").as("name")
		);
		AggregationResults<StatusCount> results = template.aggregate(agg, "photo", StatusCount.class);
//		results.forEach(r->log.debug("key {{}} value {{}}", r.status, r.count));
		return results.getMappedResults().stream().map(r -> Pair.of(r.name, r.count)).toList();
	}

	@Override
	public List<Pair<String, Integer>> getPhotosCountByMime() {
		Aggregation agg = newAggregation(
				group("original.mime").count().as("count")
				, project("count").and("original.mime").previousOperation()
				, project("count").and("original.mime").as("name")
		);
		AggregationResults<StatusCount> results = template.aggregate(agg, "photo", StatusCount.class);
		results.forEach(r -> log.debug("key {{}} value {{}}", r.name, r.count));
		return results.getMappedResults().stream().map(r -> Pair.of(r.name, r.count)).toList();
	}

	@Override
	public List<Photo> getPhotosWithStatusNotNullAndStatusNotManual(Integer maxPhotosForRobot) {
		Query query = Query.query(
						Criteria.where("status").not().in(null, "manual"))
				.limit(maxPhotosForRobot);
		return template.query(Photo.class)
				.matching(query)
				.all();
	}

//	private static class Ids {
//		String _id;
//	}
//
//	@Override
//	public List<String> getPhotosWithStatusNotNullAndStatusNotManual(long maxPhotosForRobot) {
//		Aggregation agg = newAggregation(
//				match(Criteria.where("status").not().in(null,"manual"))
//				, project("id")
//		);
//		AggregationResults<Ids> results = template.aggregate(agg, "photo", Ids.class);
//		results.forEach(r->log.debug("{{}}", r._id));
//
//		return results.getMappedResults().stream().map(r->r._id).collect(Collectors.toList());
//	}
}
