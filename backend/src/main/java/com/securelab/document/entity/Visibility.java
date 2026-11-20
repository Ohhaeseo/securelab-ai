package com.securelab.document.entity;

public enum Visibility {

	/** 소유자만 볼 수 있다 (기본값). */
	PRIVATE,

	/** 로그인한 모든 사용자가 볼 수 있다. 수정·삭제는 소유자만 할 수 있다. */
	PUBLIC

}
