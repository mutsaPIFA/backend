package com.mutsapifa.mcmmuse.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인 예외의 공통 부모. 각 BC의 domain/exception 이 이걸 상속한다.
 *
 * <p>{@code code}는 프론트 분기가 필요한 비즈니스 에러에만 넣는다 (예: NO_MCM_IN_CLOSET).
 */
public class BusinessException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public BusinessException(HttpStatus status, String message) {
    this(status, message, null);
  }

  public BusinessException(HttpStatus status, String message, String code) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }
}
