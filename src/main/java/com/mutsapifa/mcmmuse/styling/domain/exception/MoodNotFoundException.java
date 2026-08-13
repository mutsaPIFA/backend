package com.mutsapifa.mcmmuse.styling.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MoodNotFoundException extends BusinessException {

  public MoodNotFoundException() {
    super(HttpStatus.NOT_FOUND, "무드를 찾을 수 없습니다");
  }
}
