package com.example.photoarchive.domain.repo;

import com.example.photoarchive.domain.entities.Role;
import com.example.photoarchive.domain.entities.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Log4j2
public class FillUserRepositoryForDebug {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public FillUserRepositoryForDebug(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@PostConstruct
	private void fillDebugUsers() {
		var numberRecordsInUsersRepository = userRepository.count();
		log.warn("!!! to delete module! Number users in collection {}", numberRecordsInUsersRepository);
		if (numberRecordsInUsersRepository < 2) {
			var userAdmin = User.builder()
					.username("admin")
					.password(passwordEncoder.encode("admin"))
					.roles(Stream.of(Role.USER, Role.ADMIN).collect(Collectors.toSet()))
					.build();
			userRepository.save(userAdmin);
			userRepository.save(User.builder()
					.username("user")
					.password(passwordEncoder.encode("user"))
					.roles(Stream.of(Role.USER).collect(Collectors.toSet()))
					.build());
			log.warn("Added two new users: admin(admin) and user(user)");
		}
	}
}
