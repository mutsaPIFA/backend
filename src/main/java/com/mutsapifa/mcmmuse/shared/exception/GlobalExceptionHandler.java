package com.mutsapifa.mcmmuse.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** 모든 에러를 계약 형식 {@code {status, message, code?}} 으로 변환한다. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> handleBusiness(BusinessException e) {
    return ResponseEntity.status(e.getStatus())
        .body(new ApiError(e.getStatus().value(), e.getMessage(), e.getCode()));
  }

  /** 어떤 필드가 왜 틀렸는지까지 내려준다 — "값이 올바르지 않습니다"만으로는 프론트가 디버깅 불가 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse("값이 올바르지 않습니다");
    return ResponseEntity.badRequest().body(ApiError.of(400, message));
  }

  /** body 파싱 실패 — vocabulary 밖 enum 값("빨강"), 형식 오류 등 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "값이 올바르지 않습니다"));
  }

  /** 쿼리 파라미터 타입 불일치 — ?source=WRONG 등 */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "값이 올바르지 않습니다"));
  }

  /** multipart 자체가 없거나 깨짐 / 15MB 초과 — 계약 §3-1: "이미지를 확인해 주세요" */
  @ExceptionHandler({
    MultipartException.class,
    MissingServletRequestPartException.class,
    MaxUploadSizeExceededException.class
  })
  public ResponseEntity<ApiError> handleMultipart(Exception e) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "이미지를 확인해 주세요"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception e) {
    log.error("unexpected error", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(500, "서버 오류가 발생했습니다"));
  }
}
