package com.mutsapifa.mcmmuse.closet.presentation;

import com.mutsapifa.mcmmuse.closet.application.ScanService;
import com.mutsapifa.mcmmuse.closet.domain.exception.InvalidImageException;
import com.mutsapifa.mcmmuse.closet.presentation.dto.ScanResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ScanController {

  private final ScanService scanService;

  public ScanController(ScanService scanService) {
    this.scanService = scanService;
  }

  /** 계약 §3-1 — 옷 스캔 (multipart, 미저장). '재스캔' = 재호출 */
  @PostMapping(value = "/api/v1/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ScanResponse scan(@RequestPart("image") MultipartFile image) {
    try {
      return ScanResponse.from(scanService.scan(image.getBytes(), image.getContentType()));
    } catch (IOException e) {
      throw new InvalidImageException();
    }
  }
}
