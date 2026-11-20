package com.securelab.user.service;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.global.exception.BusinessException;
import com.securelab.global.exception.ErrorCode;
import com.securelab.user.dto.UserResponse;
import com.securelab.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getMe(AuthenticatedUser principal) {
		return this.userRepository.findById(principal.id())
			.map(UserResponse::from)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	/**
	 * 본인 또는 관리자만 조회할 수 있다. 권한 검사를 존재 여부 확인보다 먼저 해서, 남의 ID로 계정 존재 여부를 알아낼 수 없게 한다.
	 */
	@Transactional(readOnly = true)
	public UserResponse getUser(AuthenticatedUser principal, Long userId) {
		if (!principal.isAdmin() && !principal.id().equals(userId)) {
			log.warn("security.denied resource=user id={} actor={}", userId, principal.id());
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}
		return this.userRepository.findById(userId)
			.map(UserResponse::from)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

}
