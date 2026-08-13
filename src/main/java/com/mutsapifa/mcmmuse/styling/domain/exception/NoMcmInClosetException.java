package com.mutsapifa.mcmmuse.styling.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** 계약 유일의 비즈니스 code — 프론트가 'MCM 담으러 가기' 유도 화면으로 분기한다. */
public class NoMcmInClosetException extends BusinessException {

  public NoMcmInClosetException() {
    super(HttpStatus.CONFLICT, "코디를 만들려면 옷장에 MCM 제품이 있어야 합니다.", "NO_MCM_IN_CLOSET");
  }
}
