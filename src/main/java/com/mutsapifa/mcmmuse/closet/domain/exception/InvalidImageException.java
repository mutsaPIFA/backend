package com.mutsapifa.mcmmuse.closet.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidImageException extends BusinessException {

  public InvalidImageException() {
    super(HttpStatus.BAD_REQUEST, "이미지를 확인해 주세요");
  }
}
