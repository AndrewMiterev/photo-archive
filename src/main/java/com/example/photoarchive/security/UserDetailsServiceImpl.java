package com.example.photoarchive.security;

import com.example.photoarchive.domain.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var user = userRepository.findById(username)
				.orElseThrow(() -> new UsernameNotFoundException("No user present with username: %s".formatted(username)));
		return new User(user.getUsername(), user.getPassword(), user.getRoles()
				.stream()
				.map(r -> new SimpleGrantedAuthority("ROLE_%s".formatted(r.getRoleName())))
				.collect(Collectors.toList())
		);
	}
}
