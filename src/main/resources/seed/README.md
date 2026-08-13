# 시드 데이터

## `mcm-products.json` (예정)

MCM 상품 카탈로그 시드 **150~200개**. `catalog` BC의 `SeedJsonProductSource`가 읽는다.

MCM 공식몰은 Cloudflare 관리형 챌린지로 **서버 크롤링이 막혀 있다**(브라우저 접속은 정상).
반면 이미지 CDN은 서버에서 접근 가능하므로, **SKU만 확보하면 이미지는 URL로 조립된다.**

```
https://images.mcmworldwide.com/i/mcmworldwide/{SKU}_01        # 원본
https://images.mcmworldwide.com/i/mcmworldwide/{SKU}_01?w=600  # 리사이즈도 동작
```

### 예상 형태

```json
[
  {
    "sku": "MWSGAXT03CO001",
    "name": "Tracy 비세토스 숄더백",
    "category": "가방",
    "color": "카멜",
    "material": "가죽",
    "price": 890000,
    "productUrl": "https://kr.mcmworldwide.com/ko_KR/.../MWSGAXT03CO001.html"
  }
]
```

`imageUrl`은 SKU에서 조립하고, `cutoutUrl`은 **적재 시 rembg로 생성**한다.

### 카테고리 분포를 반드시 맞출 것

MCM 공식몰은 가방 비중이 압도적이다. 그대로 담으면 `POST /outfits` 코디가
"내 옷 + MCM 가방" 한 패턴만 반복하고 **화면 16이 무의미해진다.**
상의·하의·아우터·원피스·신발 각각 최소 수량을 강제할 것.

## 수집 방식

`ProductSource` 인터페이스 뒤에 숨긴다 — 주최측 공식 피드를 받으면 구현체만 교체.

```
SeedJsonProductSource      ← v1 (이 파일)
PartnerFeedProductSource   ← 주최측 피드 확보 시
CrawlerProductSource       ← 파킹랏
```

상세 논의: `my/crawler-and-payment.md` (개인 노트, 레포 밖)
