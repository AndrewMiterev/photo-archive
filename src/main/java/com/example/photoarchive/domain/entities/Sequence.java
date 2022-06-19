package com.example.photoarchive.domain.entities;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
public class Sequence {
	@Id
	private String id;
	private long number;
}
