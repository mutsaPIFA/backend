package com.mutsapifa.mcmmuse.closet.domain.exception;

import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** AI 처리(누끼·태깅) 실패 — 계약 §3-1: 409 */
public class ScanFailedException extends BusinessException {

  public ScanFailedException(Throwable cause) {
    super(HttpStatus.CONFLICT, "분석에 실패했어요. 다시 시도해 주세요");
    initCause(cause);
  }
}
