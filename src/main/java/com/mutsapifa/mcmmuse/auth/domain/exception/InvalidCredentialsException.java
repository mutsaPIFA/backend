package com.mutsapifa.mcmmuse.auth.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessException {

  public InvalidCredentialsException() {
    super(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다");
  }
}
