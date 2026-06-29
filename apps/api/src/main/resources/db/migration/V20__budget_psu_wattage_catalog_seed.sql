WITH seed_parts (
  public_id,
  category,
  name,
  manufacturer,
  price,
  attributes
) AS (
  VALUES
    ('00000000-0000-4000-8000-000000012001', 'PSU', 'AONE 컴퓨터 파워 ATX 300W 600T', 'AONE', 35500, $json$
{"capacityW":300,"wattage":300,"efficiency":"STANDARD","modular":false,"atxSpec":"ATX","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"300W, ATX, fixed cable","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"정격 300W 파워","sourceProductKey":"89364221385","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012002', 'PSU', '잘만테크 잘만 Wattbit II 400W 83+, 블랙', 'Zalman', 37500, $json$
{"capacityW":400,"wattage":400,"efficiency":"83PLUS","modular":false,"atxSpec":"ATX","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"400W, 83+, fixed cable","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"잘만 400W 파워","sourceProductKey":"60099791344","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012003', 'PSU', '마이크로닉스 Classic II 풀체인지 500W 80PLUS BRONZE ATX3.1, 블랙', 'Micronics', 57500, $json$
{"capacityW":500,"wattage":500,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"500W, Bronze, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"마이크로닉스 500W 파워","sourceProductKey":"59317557761","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012004', 'PSU', '잘만테크 잘만 EcoMax 500W 83+, 블랙', 'Zalman', 38000, $json$
{"capacityW":500,"wattage":500,"efficiency":"83PLUS","modular":false,"atxSpec":"ATX","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"500W, 83+, fixed cable","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"정격 500W 파워","sourceProductKey":"58863751097","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012005', 'PSU', '마이크로닉스 Classic II 풀체인지 600W 80PLUS BRONZE ATX 3.1, 화이트', 'Micronics', 66900, $json$
{"capacityW":600,"wattage":600,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"600W, Bronze, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"정격 600W 파워","sourceProductKey":"59317587813","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012006', 'PSU', 'FSP HYPER K PRO 600W Bronze EU ATX3.1 파워서플라이', 'FSP', 67000, $json$
{"capacityW":600,"wattage":600,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"600W, Bronze, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"FSP 600W 파워","sourceProductKey":"88611425805","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012007', 'PSU', 'MSI MAG A650BN', 'MSI', 56900, $json$
{"capacityW":650,"wattage":650,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"650W, Bronze, fixed cable","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"MSI 650W 파워","sourceProductKey":"29191213629","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012008', 'PSU', '마이크로닉스 Classic II 풀체인지 700W 80PLUS BRONZE ATX3.1 PCIE5.1 블랙, 블랙', 'Micronics', 86900, $json$
{"capacityW":700,"wattage":700,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"700W, Bronze, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"마이크로닉스 700W 파워","sourceProductKey":"59317622364","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012009', 'PSU', '잘만 MegaMax II 게임 조립용 ATX 정격 700W 브론즈 ATX3.1', 'Zalman', 86900, $json$
{"capacityW":700,"wattage":700,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"PCIe 6+2","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"700W, Bronze, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"정격 700W 파워","sourceProductKey":"91030029781","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012010', 'PSU', '마이크로닉스 Classic II 750W 80PLUS골드 풀모듈러 ATX3.1, 블랙', 'Micronics', 126280, $json$
{"capacityW":750,"wattage":750,"efficiency":"80PLUS_GOLD","modular":true,"atxSpec":"ATX 3.1","gpuConnector":"12V-2x6","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"750W, Gold, full modular, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"정격 750W ATX3.1 파워","sourceProductKey":"59304181768","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012011', 'PSU', 'FSP VITA GD 750W 80PLUS GOLD ATX3.1 (PCIe5.1) 파워서플라이', 'FSP', 94000, $json$
{"capacityW":750,"wattage":750,"efficiency":"80PLUS_GOLD","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"12V-2x6","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"750W, Gold, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"FSP 750W ATX3.1 파워","sourceProductKey":"88650020953","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012012', 'PSU', 'MSI MAG A750GLS PCIE5.1 80PLUS골드 풀모듈러 ATX3.1 750W, 블랙', 'MSI', 129000, $json$
{"capacityW":750,"wattage":750,"efficiency":"80PLUS_GOLD","modular":true,"atxSpec":"ATX 3.1","gpuConnector":"12V-2x6","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"750W, Gold, full modular, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"MSI 750W ATX3.1 파워","sourceProductKey":"60600380906","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012013', 'PSU', '마이크로닉스 Classic II 풀체인지 800W 80PLUS BRONZE ATX3.1 PCIE5.1, 블랙', 'Micronics', 97400, $json$
{"capacityW":800,"wattage":800,"efficiency":"80PLUS_BRONZE","modular":false,"atxSpec":"ATX 3.1","gpuConnector":"PCIe 5.1","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"800W, Bronze, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"정격 800W 파워","sourceProductKey":"59317644454","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012014', 'PSU', '커세어 RM750e ATX 3.1', 'Corsair', 164000, $json$
{"capacityW":750,"wattage":750,"efficiency":"80PLUS_GOLD","modular":true,"atxSpec":"ATX 3.1","gpuConnector":"12V-2x6","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"750W, Gold, full modular, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"Corsair RM750e ATX3.1","sourceProductKey":"91058568072","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012015', 'PSU', '시소닉 NEW FOCUS V4 GX-750 GOLD 풀모듈러 ATX3.1 750W, 블랙', 'Seasonic', 150000, $json$
{"capacityW":750,"wattage":750,"efficiency":"80PLUS_GOLD","modular":true,"atxSpec":"ATX 3.1","gpuConnector":"12V-2x6","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"750W, Gold, full modular, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"Seasonic 750W ATX3.1","sourceProductKey":"59588106955","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012016', 'PSU', '마이크로닉스 Classic II 850W 80PLUS골드 풀모듈러 ATX3.1, 블랙', 'Micronics', 141500, $json$
{"capacityW":850,"wattage":850,"efficiency":"80PLUS_GOLD","modular":true,"atxSpec":"ATX 3.1","gpuConnector":"12V-2x6","depthMm":140,"widthMm":150,"heightMm":86,"shortSpec":"850W, Gold, full modular, ATX 3.1","catalogGeneration":"CURRENT_2026_06_BUDGET_PSU","currentLineupOnly":true,"toolReady":true,"specSource":"CURATED_NAME_SPEC","specConfidence":"REVIEW_READY","externalSources":{"naver":{"keyword":"마이크로닉스 850W ATX3.1 파워","sourceProductKey":"59303926561","catalogRefresh":true}},"metadataVersion":5}$json$::jsonb)
)
INSERT INTO parts (
  public_id,
  category,
  name,
  manufacturer,
  price,
  status,
  attributes,
  created_at,
  updated_at
)
SELECT public_id::uuid,
       category,
       name,
       manufacturer,
       price,
       'ACTIVE',
       attributes,
       now(),
       now()
FROM seed_parts
ON CONFLICT (public_id) DO UPDATE SET
  category = EXCLUDED.category,
  name = EXCLUDED.name,
  manufacturer = EXCLUDED.manufacturer,
  price = EXCLUDED.price,
  status = 'ACTIVE',
  attributes = EXCLUDED.attributes,
  updated_at = now();

WITH seed_offers (
  public_id,
  search_query,
  title,
  image_url,
  supplier_name,
  offer_url,
  low_price,
  raw_payload
) AS (
  VALUES
    ('00000000-0000-4000-8000-000000012001', '정격 300W 파워', 'AONE 컴퓨터 파워 ATX 300W 600T', 'https://shopping-phinf.pstatic.net/main_8936422/89364221385.jpg', '정품파트너-오늘출발', 'https://smartstore.naver.com/main/products/11819710798', 35500, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012001","sourceProductKey":"89364221385","maker":"에이원","brand":"에이원","productType":"2","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012002', '잘만 400W 파워', '잘만테크 잘만 Wattbit II 400W 83+, 블랙', 'https://shopping-phinf.pstatic.net/main_6009979/60099791344.20260519183510.jpg', '네이버', 'https://search.shopping.naver.com/catalog/60099791344', 37500, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012002","sourceProductKey":"60099791344","maker":"잘만테크","brand":"잘만","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012003', '마이크로닉스 500W 파워', '마이크로닉스 Classic II 풀체인지 500W 80PLUS BRONZE ATX3.1, 블랙', 'https://shopping-phinf.pstatic.net/main_5931755/59317557761.20260319102111.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59317557761', 57500, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012003","sourceProductKey":"59317557761","maker":"마이크로닉스","brand":"마이크로닉스","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012004', '정격 500W 파워', '잘만테크 잘만 EcoMax 500W 83+, 블랙', 'https://shopping-phinf.pstatic.net/main_5886375/58863751097.20260209160145.jpg', '네이버', 'https://search.shopping.naver.com/catalog/58863751097', 38000, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012004","sourceProductKey":"58863751097","maker":"잘만테크","brand":"잘만","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012005', '정격 600W 파워', '마이크로닉스 Classic II 풀체인지 600W 80PLUS BRONZE ATX 3.1, 화이트', 'https://shopping-phinf.pstatic.net/main_5931758/59317587813.20260319103402.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59317587813', 66900, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012005","sourceProductKey":"59317587813","maker":"마이크로닉스","brand":"마이크로닉스","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012006', 'FSP 600W 파워', 'FSP HYPER K PRO 600W Bronze EU ATX3.1 파워서플라이', 'https://shopping-phinf.pstatic.net/main_8861142/88611425805.jpg', 'FSP스토어', 'https://smartstore.naver.com/main/products/11066919594', 67000, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012006","sourceProductKey":"88611425805","maker":"FSP","brand":"FSP","productType":"2","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012007', 'MSI 650W 파워', 'MSI MAG A650BN', 'https://shopping-phinf.pstatic.net/main_2919121/29191213629.20211011120951.jpg', '네이버', 'https://search.shopping.naver.com/catalog/29191213629', 56900, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012007","sourceProductKey":"29191213629","maker":"MSI","brand":"MSI","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012008', '마이크로닉스 700W 파워', '마이크로닉스 Classic II 풀체인지 700W 80PLUS BRONZE ATX3.1 PCIE5.1 블랙, 블랙', 'https://shopping-phinf.pstatic.net/main_5931762/59317622364.20260319103906.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59317622364', 86900, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012008","sourceProductKey":"59317622364","maker":"마이크로닉스","brand":"마이크로닉스","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012009', '정격 700W 파워', '잘만 MegaMax II 게임 조립용 ATX 정격 700W 브론즈 ATX3.1', 'https://shopping-phinf.pstatic.net/main_9103002/91030029781.jpg', '티돌이샵', 'https://smartstore.naver.com/main/products/13485519429', 86900, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012009","sourceProductKey":"91030029781","maker":"잘만테크","brand":"잘만","productType":"2","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012010', '정격 750W ATX3.1 파워', '마이크로닉스 Classic II 750W 80PLUS골드 풀모듈러 ATX3.1, 블랙', 'https://shopping-phinf.pstatic.net/main_5930418/59304181768.20260318111331.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59304181768', 126280, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012010","sourceProductKey":"59304181768","maker":"마이크로닉스","brand":"마이크로닉스","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012011', 'FSP 750W ATX3.1 파워', 'FSP VITA GD 750W 80PLUS GOLD ATX3.1 (PCIe5.1) 파워서플라이', 'https://shopping-phinf.pstatic.net/main_8865002/88650020953.jpg', 'FSP스토어', 'https://search.shopping.naver.com/catalog/88650020953', 94000, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012011","sourceProductKey":"88650020953","maker":"FSP","brand":"FSP","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012012', 'MSI 750W ATX3.1 파워', 'MSI MAG A750GLS PCIE5.1 80PLUS골드 풀모듈러 ATX3.1 750W, 블랙', 'https://shopping-phinf.pstatic.net/main_6060038/60600380906.jpg', '네이버', 'https://search.shopping.naver.com/catalog/60600380906', 129000, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012012","sourceProductKey":"60600380906","maker":"MSI","brand":"MSI","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012013', '정격 800W 파워', '마이크로닉스 Classic II 풀체인지 800W 80PLUS BRONZE ATX3.1 PCIE5.1, 블랙', 'https://shopping-phinf.pstatic.net/main_5931764/59317644454.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59317644454', 97400, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012013","sourceProductKey":"59317644454","maker":"마이크로닉스","brand":"마이크로닉스","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012014', 'Corsair RM750e ATX3.1', '커세어 RM750e ATX 3.1', 'https://shopping-phinf.pstatic.net/main_9105856/91058568072.jpg', '커세어 공식스토어', 'https://smartstore.naver.com/main/products/13514057720', 164000, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012014","sourceProductKey":"91058568072","maker":"커세어","brand":"커세어","productType":"2","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012015', 'Seasonic 750W ATX3.1', '시소닉 NEW FOCUS V4 GX-750 GOLD 풀모듈러 ATX3.1 750W, 블랙', 'https://shopping-phinf.pstatic.net/main_5958810/59588106955.20260408152209.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59588106955', 150000, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012015","sourceProductKey":"59588106955","maker":"시소닉","brand":"시소닉","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb),
    ('00000000-0000-4000-8000-000000012016', '마이크로닉스 850W ATX3.1 파워', '마이크로닉스 Classic II 850W 80PLUS골드 풀모듈러 ATX3.1, 블랙', 'https://shopping-phinf.pstatic.net/main_5930392/59303926561.20260318103746.jpg', '네이버', 'https://search.shopping.naver.com/catalog/59303926561', 141500, $json$
{"source":"curated-budget-psu-v20","selectedFor":"00000000-0000-4000-8000-000000012016","sourceProductKey":"59303926561","maker":"마이크로닉스","brand":"마이크로닉스","productType":"1","selectedAt":"2026-06-29"}$json$::jsonb)
), matched_parts AS (
  SELECT p.id AS part_id,
         s.search_query,
         s.title,
         s.image_url,
         s.supplier_name,
         s.offer_url,
         s.low_price,
         s.raw_payload
  FROM seed_offers s
  JOIN parts p ON p.public_id = s.public_id::uuid
  WHERE p.deleted_at IS NULL
), upserted_offers AS (
  INSERT INTO part_external_offers (
    part_id,
    source,
    search_query,
    title,
    image_url,
    supplier_name,
    offer_url,
    low_price,
    raw_payload,
    refreshed_at,
    created_at,
    updated_at
  )
  SELECT part_id,
         'NAVER_SHOPPING_SEARCH',
         search_query,
         title,
         image_url,
         supplier_name,
         offer_url,
         low_price,
         raw_payload,
         '2026-06-29T18:25:00Z',
         now(),
         now()
  FROM matched_parts
  ON CONFLICT (part_id, source) WHERE deleted_at IS NULL
  DO UPDATE SET
    search_query = EXCLUDED.search_query,
    title = EXCLUDED.title,
    image_url = EXCLUDED.image_url,
    supplier_name = EXCLUDED.supplier_name,
    offer_url = EXCLUDED.offer_url,
    low_price = EXCLUDED.low_price,
    raw_payload = EXCLUDED.raw_payload,
    refreshed_at = EXCLUDED.refreshed_at,
    updated_at = now()
  RETURNING part_id, low_price, raw_payload
)
INSERT INTO price_snapshots (
  part_id,
  price,
  source,
  collected_at,
  raw_payload
)
SELECT part_id,
       low_price,
       'NAVER_SHOPPING_SEARCH',
       '2026-06-29T18:25:00Z',
       raw_payload
FROM upserted_offers;

SELECT setval(pg_get_serial_sequence('parts', 'id'), (SELECT max(id) FROM parts));
SELECT setval(pg_get_serial_sequence('price_snapshots', 'id'), (SELECT max(id) FROM price_snapshots));
