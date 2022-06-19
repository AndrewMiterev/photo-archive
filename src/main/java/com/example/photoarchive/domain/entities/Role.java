package com.example.photoarchive.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Role {
	USER("user"), ADMIN("admin");
	private String roleName;
}
