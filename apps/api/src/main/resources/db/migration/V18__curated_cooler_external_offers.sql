WITH selected_offers (
  public_id,
  search_query,
  title,
  image_url,
  supplier_name,
  offer_url,
  low_price,
  source_product_key,
  maker,
  brand,
  product_type
) AS (
  VALUES
    ('00000000-0000-4000-8000-000000011701'::uuid, 'ARCTIC Liquid Freezer III 360 A-RGB', 'ARCTIC Liquid Freezer III PRO 360 A-RGB 서린씨앤아이, 블랙', 'https://shopping-phinf.pstatic.net/main_5694280/56942800609.20250926121415.jpg', '네이버', 'https://search.shopping.naver.com/catalog/56942800609', 187000, '56942800609', 'ARCTIC', 'ARCTIC', '1'),
    ('00000000-0000-4000-8000-000000011702'::uuid, 'DeepCool ASSASSIN IV', '(국내배송)DEEPCOOL ASSASSIN IV Premium CPU Air Cooler / 딥쿨 어쎄신 4 플레그쉽 하이 퍼포먼스 CPU 공랭 쿨러 / 병행', 'https://shopping-phinf.pstatic.net/main_9110501/91105011686.jpg', '퍼브컴', 'https://smartstore.naver.com/main/products/13560501319', 148000, '91105011686', 'DEEPCOOL', 'DEEPCOOL', '2'),
    ('00000000-0000-4000-8000-000000011703'::uuid, 'Noctua NH-D15 G2', '녹투아 NH-D15 G2 브라운', 'https://shopping-phinf.pstatic.net/main_5226636/52266362625.20250103105351.jpg', '네이버', 'https://search.shopping.naver.com/catalog/52266362625', 248000, '52266362625', '녹투아', '', '1'),
    ('00000000-0000-4000-8000-000000011704'::uuid, 'Corsair iCUE LINK TITAN 360 RX RGB', '커세어 iCUE LINK TITAN 360 RX RGB 화이트', 'https://shopping-phinf.pstatic.net/main_5825924/58259241143.20251224172832.jpg', '네이버', 'https://search.shopping.naver.com/catalog/58259241143', 262350, '58259241143', '커세어', '커세어', '1'),
    ('00000000-0000-4000-8000-000000011705'::uuid, 'NZXT Kraken Elite 360 RGB', 'NZXT KRAKEN ELITE 360 RGB 블랙', 'https://shopping-phinf.pstatic.net/main_5692780/56927804356.20250925165905.jpg', '네이버', 'https://search.shopping.naver.com/catalog/56927804356', 399000, '56927804356', 'NZXT', 'NZXT', '1'),
    ('00000000-0000-4000-8000-000000011706'::uuid, 'Lian Li HydroShift LCD 360R', '리안리 Hydroshift II LCD-C 360N 블랙', 'https://shopping-phinf.pstatic.net/main_5731347/57313474622.20260416095737.jpg', '네이버', 'https://search.shopping.naver.com/catalog/57313474622', 247990, '57313474622', '리안리', '리안리', '1'),
    ('00000000-0000-4000-8000-000000011707'::uuid, 'be quiet Dark Rock Pro 5', 'BE QUIET DARK ROCK PRO 5 블랙', 'https://shopping-phinf.pstatic.net/main_5259370/52593709157.20250121094608.jpg', '네이버', 'https://search.shopping.naver.com/catalog/52593709157', 139000, '52593709157', 'BE QUIET', 'BE QUIET', '1'),
    ('00000000-0000-4000-8000-000000011708'::uuid, 'Thermalright Phantom Spirit 120 EVO', '써멀라이트 Phantom Spirit 120 EVO 블랙', 'https://shopping-phinf.pstatic.net/main_5261332/52613322659.20250122160146.jpg', '네이버', 'https://search.shopping.naver.com/catalog/52613322659', 73840, '52613322659', '써멀라이트', '써멀라이트', '1'),
    ('00000000-0000-4000-8000-000000011709'::uuid, 'Cooler Master MasterLiquid 360 Atmos', '쿨러마스터 MASTERLIQUID 360 ATMOS II VRM Fan 블랙', 'https://shopping-phinf.pstatic.net/main_5692535/56925358625.20260417092052.jpg', '네이버', 'https://search.shopping.naver.com/catalog/56925358625', 172600, '56925358625', '쿨러마스터', '쿨러마스터', '1'),
    ('00000000-0000-4000-8000-000000011710'::uuid, 'ASUS ROG Ryujin III 360 ARGB', 'ASUS ROG RYUJIN III 360 ARGB EXTREME 블랙', 'https://shopping-phinf.pstatic.net/main_5824549/58245496317.20251223170738.jpg', '네이버', 'https://search.shopping.naver.com/catalog/58245496317', 293020, '58245496317', 'ASUS', 'ASUS', '1')
),
matched_parts AS (
  SELECT p.id AS part_id,
         p.public_id,
         s.search_query,
         s.title,
         s.image_url,
         s.supplier_name,
         s.offer_url,
         s.low_price,
         s.source_product_key,
         s.maker,
         s.brand,
         s.product_type
  FROM parts p
  JOIN selected_offers s ON s.public_id = p.public_id
  WHERE p.deleted_at IS NULL
),
updated_parts AS (
  UPDATE parts p
  SET price = m.low_price,
      attributes = jsonb_set(
        jsonb_set(
          jsonb_set(
            coalesce(p.attributes, '{}'::jsonb),
            '{imageUrl}',
            to_jsonb(m.image_url),
            true
          ),
          '{externalSources,naver}',
          coalesce(p.attributes #> '{externalSources,naver}', '{}'::jsonb)
            || jsonb_build_object(
              'keyword', m.search_query,
              'catalogRefresh', true,
              'sourceProductKey', m.source_product_key
            ),
          true
        ),
        '{externalSources,naver,lastManualReview}',
        to_jsonb('2026-06-29'::text),
        true
      ),
      updated_at = now()
  FROM matched_parts m
  WHERE p.id = m.part_id
  RETURNING p.id
),
upserted_offers AS (
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
  SELECT m.part_id,
         'NAVER_SHOPPING_SEARCH',
         m.search_query,
         m.title,
         m.image_url,
         m.supplier_name,
         m.offer_url,
         m.low_price,
         jsonb_build_object(
           'source', 'manual-curated-naver-offer',
           'sourceProductKey', m.source_product_key,
           'maker', m.maker,
           'brand', m.brand,
           'productType', m.product_type,
           'selectedFor', m.public_id::text,
           'selectedAt', '2026-06-29'
         ),
         '2026-06-29T17:20:00Z',
         now(),
         now()
  FROM matched_parts m
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
       '2026-06-29T17:20:00Z',
       raw_payload
FROM upserted_offers;

SELECT setval(pg_get_serial_sequence('price_snapshots', 'id'), (SELECT max(id) FROM price_snapshots));
