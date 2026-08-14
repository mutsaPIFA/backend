package com.mutsapifa.mcmmuse.catalog.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

/** List&lt;String&gt; ↔ 파이프 구분 text 컬럼 — URL에는 '|'가 없다(CSV 원천도 파이프 구분). */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    return attribute == null || attribute.isEmpty() ? null : String.join("|", attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank()
        ? List.of()
        : Arrays.stream(dbData.split("\\|")).map(String::trim).toList();
  }
}
