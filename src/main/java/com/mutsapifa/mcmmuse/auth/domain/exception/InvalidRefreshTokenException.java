package com.mutsapifa.mcmmuse.auth.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BusinessException {

  public InvalidRefreshTokenException() {
    super(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요");
  }
}
