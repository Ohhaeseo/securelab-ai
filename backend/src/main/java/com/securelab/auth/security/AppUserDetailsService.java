package com.securelab.auth.security;

import com.securelab.user.repository.UserRepository;
import com.securelab.user.service.EmailNormalizer;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public AppUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		return this.userRepository.findByEmail(EmailNormalizer.normalize(email))
			.map(AuthenticatedUser::from)
			.orElseThrow(() -> new UsernameNotFoundException("user not found"));
	}

}
