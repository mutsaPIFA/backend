package com.mutsapifa.mcmmuse.catalog.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 시드 자동 적재 — upsert 멱등이라 매 부팅 안전. 팀원 누구나 docker compose up만으로 동일 데이터를 갖는다. 누끼 백필은 비동기로 이어서.
 */
@Slf4j
@Component
public class SeedIngestRunner implements ApplicationRunner {

  private final McmProductIngestService ingestService;
  private final CutoutBackfillService cutoutBackfillService;

  public SeedIngestRunner(
      McmProductIngestService ingestService, CutoutBackfillService cutoutBackfillService) {
    this.ingestService = ingestService;
    this.cutoutBackfillService = cutoutBackfillService;
  }

  @Override
  public void run(ApplicationArguments args) {
    ingestService.ingest();
    cutoutBackfillService.backfillAsync();
  }
}
