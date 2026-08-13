package com.mutsapifa.mcmmuse.styling.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class LookNotFoundException extends BusinessException {

  public LookNotFoundException() {
    super(HttpStatus.NOT_FOUND, "룩을 찾을 수 없습니다");
  }
}
