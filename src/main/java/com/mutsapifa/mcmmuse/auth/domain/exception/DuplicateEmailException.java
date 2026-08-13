package com.mutsapifa.mcmmuse.auth.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {

  public DuplicateEmailException() {
    super(HttpStatus.CONFLICT, "이미 가입된 이메일입니다");
  }
}
