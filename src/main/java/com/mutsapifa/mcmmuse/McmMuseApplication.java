package com.mutsapifa.mcmmuse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MCM MUSE 백엔드 부트스트랩.
 *
 * <p>{@code @EnableAsync} 는 룩 이미지 비동기 생성(계약 D2)에 쓰인다. {@code POST /looks} 는 즉시 201을 반환하고, 이미지 생성은
 * 백그라운드에서 돌아 {@code generatedImageUrl} 을 나중에 채운다.
 */
@EnableAsync
@SpringBootApplication
public class McmMuseApplication {

  public static void main(String[] args) {
    SpringApplication.run(McmMuseApplication.class, args);
  }
}
