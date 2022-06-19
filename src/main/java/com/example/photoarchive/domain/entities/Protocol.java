package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document
public class Protocol {
	@Transient
	public static final String SEQUENCE_NAME = "protocols";

	@Id
	long id;
	private LocalDateTime date;
	private String type;
	private String message;
}
