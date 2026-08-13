package com.mutsapifa.mcmmuse.closet.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** 남의 옷장 아이템 접근 — 계약 에러표: 403 (없는 리소스는 404와 구분) */
public class ClosetAccessDeniedException extends BusinessException {

  public ClosetAccessDeniedException() {
    super(HttpStatus.FORBIDDEN, "권한이 없습니다");
  }
}
