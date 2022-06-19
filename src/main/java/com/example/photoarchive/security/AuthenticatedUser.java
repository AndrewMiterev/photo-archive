package com.example.photoarchive.security;

import com.example.photoarchive.domain.entities.User;
import com.example.photoarchive.domain.repo.UserRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUser {
	private final UserRepository userRepository;

	public AuthenticatedUser(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	private Optional<Authentication> getAuthentication() {
		SecurityContext context = SecurityContextHolder.getContext();
		return Optional.ofNullable(context.getAuthentication())
				.filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken));
	}

	public Optional<User> get() {
		return getAuthentication().flatMap(authentication -> userRepository.findById(authentication.getName()));
	}

	public void logout() {
		UI.getCurrent().getPage().setLocation(SecurityConfiguration.LOGOUT_URL);
		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
	}
}
