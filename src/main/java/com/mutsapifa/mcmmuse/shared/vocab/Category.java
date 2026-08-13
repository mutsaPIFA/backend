package com.mutsapifa.mcmmuse.shared.vocab;

/**
 * controlled vocabulary — 계약 api-v1.md "공통 타입"과 1:1.
 *
 * <p>상수명 = 계약의 한국어 값 그대로. {@code @Enumerated(STRING)} 저장값·JSON 직렬화 값이 계약과 자동 일치한다. 값 추가 = 상수 추가 +
 * 계약 문서 갱신 (DB 마이그레이션 불필요 — varchar).
 */
public enum Category {
  상의,
  하의,
  아우터,
  원피스,
  신발,
  가방,
  악세서리
}
