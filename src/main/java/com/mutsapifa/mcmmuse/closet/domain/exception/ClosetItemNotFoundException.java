package com.mutsapifa.mcmmuse.closet.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ClosetItemNotFoundException extends BusinessException {

  public ClosetItemNotFoundException() {
    super(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다");
  }
}
