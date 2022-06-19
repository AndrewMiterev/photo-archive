package com.example.photoarchive.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import java.util.Set;

@Document
@Data
@Builder
public class User {
	@Id
	@Email
	private String username;

	@JsonIgnore
	private String password;

	private Set<Role> roles;
}
