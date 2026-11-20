package com.securelab.auth.service;

import com.securelab.auth.dto.SignupRequest;
import com.securelab.global.exception.BusinessException;
import com.securelab.global.exception.ErrorCode;
import com.securelab.user.dto.UserResponse;
import com.securelab.user.entity.Role;
import com.securelab.user.entity.User;
import com.securelab.user.repository.UserRepository;
import com.securelab.user.service.EmailNormalizer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 일반 사용자(USER)로 가입한다. 관리자 역할은 가입 API로 만들 수 없다.
	 */
	@Transactional
	public UserResponse signup(SignupRequest request) {
		String email = EmailNormalizer.normalize(request.email());
		if (this.userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
		User user = User.create(email, this.passwordEncoder.encode(request.password()), Role.USER);
		try {
			this.userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// 동시에 같은 이메일로 가입한 경우 (UNIQUE 제약)
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
		return UserResponse.from(user);
	}

}
