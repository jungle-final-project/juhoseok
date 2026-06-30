-- RAG v2 source chunks: policy + examples + counterexamples.
-- These rows are reusable knowledge chunks; Agent runs copy selected rows into session evidence.

WITH seed_rag(public_id, source_id, chunk_text, summary, score, metadata) AS (
  VALUES
    (
      '00000000-0000-4000-8000-000000015101',
      'requirement-counterexample-premium-with-user-budget',
      'Counterexample for premium wording: if the user says 최고사양, 끝판왕 느낌, 하이엔드 감성, or 제일 좋은 but also gives a concrete budget such as 200만원 or 300만원, keep budgetPolicy as USER_BUDGET. Interpret the request as best possible within that budget, not OPEN_BUDGET.',
      'Requirement parse counterexample: premium wording with a concrete budget must remain USER_BUDGET.',
      0.96500,
      '{"sourceType":"INTERNAL_RULE","purpose":"REQUIREMENT_PARSE","title":"Premium wording with explicit budget counterexample","relatedFields":["budget","budgetPolicy","performanceTier"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015102',
      'requirement-example-gaming-resolution-refresh',
      'Examples: 배그 QHD 144Hz, 로스트아크 4K, 고주사율 FPS, qhd 옵션 타협 없음, fhd 240hz are gaming and display-performance requirements. Extract usageTags GAMING, resolution when present, and ask a follow-up question if refresh rate or option level is missing.',
      'Requirement parse examples: Korean gaming, resolution, refresh-rate, and option-level wording.',
      0.94500,
      '{"sourceType":"BENCHMARK","purpose":"REQUIREMENT_PARSE","title":"Gaming resolution and refresh parse examples","relatedFields":["usageTags","resolution","questions"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015103',
      'requirement-example-workload-mixed-creator-ai',
      'Examples: 개발 IDE, Docker, 컴파일, 영상 편집, 프리미어, 다빈치, 블렌더, 3D 작업, 로컬 AI, LLM 실험, CUDA should map to DEVELOPMENT, VIDEO_EDIT, or AI_DEV as appropriate. Mixed workload requests should preserve multiple usageTags instead of choosing only gaming.',
      'Requirement parse examples: development, creator, 3D, and local AI workload wording.',
      0.94000,
      '{"sourceType":"GUIDE","purpose":"REQUIREMENT_PARSE","title":"Mixed workload parse examples","relatedFields":["usageTags","preferredVendors"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015104',
      'requirement-example-noise-upgrade-brand',
      'Examples: 조용한 PC, 저소음, 밤에 켜둘 PC, 업그레이드 여유, 오래 쓸 PC, NVIDIA 선호, 라데온 싫음, 인텔 선호 should become mustHave LOW_NOISE, priority or follow-up for upgrade headroom, and preferredVendors or excluded vendor notes when clear.',
      'Requirement parse examples: noise sensitivity, upgrade headroom, and brand preference wording.',
      0.93500,
      '{"sourceType":"INTERNAL_RULE","purpose":"REQUIREMENT_PARSE","title":"Noise upgrade and brand parse examples","relatedFields":["mustHave","preferredVendors","priority","questions"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015105',
      'build-rule-cpu-gpu-balance-and-bottleneck',
      'Build recommendation should avoid pairing a flagship GPU with a weak CPU for high refresh gaming or creator workloads. Use CPU class, GPU class, resolution, and workload to explain bottleneck risk; do not invent benchmark numbers that are not stored.',
      'Build recommendation rule: explain CPU/GPU balance and bottleneck risk from stored facts only.',
      0.93000,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_RECOMMEND","title":"CPU GPU balance rule","relatedCategories":["CPU","GPU"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015106',
      'build-rule-memory-storage-workload-floor',
      'For development, editing, 3D, and AI-adjacent workloads, prefer at least 32GB RAM and consider 64GB when multitasking, large projects, or local model wording appears. Prefer fast NVMe storage for editing scratch, build caches, and game loading.',
      'Build recommendation rule: RAM and NVMe floors for development, editing, 3D, and AI workloads.',
      0.92000,
      '{"sourceType":"BENCHMARK","purpose":"BUILD_RECOMMEND","title":"RAM storage workload floor","relatedCategories":["RAM","STORAGE"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015107',
      'build-rule-airflow-cooler-case-fit',
      'High power CPU and GPU builds should check case airflow, maxGpuLengthMm, slot width, maxCpuCoolerHeightMm, radiator support, and cooler socket support before recommending compact or silent cases.',
      'Build recommendation rule: airflow, cooler, case clearance, and socket support checks.',
      0.91500,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_RECOMMEND","title":"Airflow cooler case fit rule","relatedCategories":["CASE","COOLER","GPU","CPU"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015108',
      'build-rule-saved-price-and-psu-headroom',
      'Use saved current parts.price and price_snapshots during user recommendation. For PSU, compare estimated system draw, GPU requiredSystemPowerW, PSU capacityW, connector standard, and headroom; recommend WARN when the margin is narrow.',
      'Build recommendation rule: saved price first and PSU headroom validation.',
      0.91000,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_RECOMMEND","title":"Saved price and PSU headroom rule","relatedCategories":["PSU","GPU"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015109',
      'as-guide-gpu-thermal-frame-drop',
      'AS examples: 게임 20분 뒤 프레임 급락, GPU 온도 90도 이상, 팬 소음 증가, 프레임 타임 튐 often indicate thermal throttling, airflow restriction, dust, fan curve, or driver instability before hardware replacement.',
      'AS guide: GPU temperature, frame drop, fan noise, and frame-time spikes suggest thermal or driver causes.',
      0.95000,
      '{"sourceType":"TROUBLESHOOTING","purpose":"AS_ANALYZE","title":"GPU thermal frame drop AS guide","relatedCategories":["GPU","CASE","COOLER"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015110',
      'as-guide-driver-crash-event-log',
      'AS examples: 화면 멈춤, 블루스크린, 드라이버 오류, nvlddmkm, display driver stopped, 게임 튕김, 이벤트 로그 오류 should prioritize driver rollback/update, Windows Event Log, GPU stability, and power checks before replacement.',
      'AS guide: crashes and display driver errors should inspect drivers, event logs, GPU stability, and power.',
      0.94000,
      '{"sourceType":"TROUBLESHOOTING","purpose":"AS_ANALYZE","title":"Driver crash and event log AS guide","relatedCategories":["GPU","PSU"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015111',
      'as-guide-memory-storage-pressure',
      'AS examples: 렌더링 느림, IDE 멈춤, 크롬 탭 많음, RAM 90퍼센트, 디스크 100퍼센트, 게임 로딩 지연 should check memory pressure, SSD health, disk queue, background processes, and upgrade candidates for RAM or NVMe.',
      'AS guide: memory pressure and disk saturation can explain slow rendering, IDE stalls, and loading delays.',
      0.93000,
      '{"sourceType":"TROUBLESHOOTING","purpose":"AS_ANALYZE","title":"Memory and storage pressure AS guide","relatedCategories":["RAM","STORAGE"],"metadataVersion":2}'
    ),
    (
      '00000000-0000-4000-8000-000000015112',
      'as-guide-power-instability',
      'AS examples: 부하 걸면 재부팅, 전원이 꺼짐, 고사양 게임에서만 다운, 파워 부족 의심 should check PSU capacityW, connector, transient load, GPU requiredSystemPowerW, and event logs before recommending component replacement.',
      'AS guide: load-related reboot or shutdown should check PSU margin, connector, transient load, and logs.',
      0.92500,
      '{"sourceType":"TROUBLESHOOTING","purpose":"AS_ANALYZE","title":"Power instability AS guide","relatedCategories":["PSU","GPU"],"metadataVersion":2}'
    )
)
INSERT INTO rag_evidence (
  public_id,
  agent_session_id,
  source_id,
  chunk_text,
  summary,
  score,
  metadata,
  created_at
)
SELECT
  public_id::uuid,
  NULL,
  source_id,
  chunk_text,
  summary,
  score,
  metadata::jsonb,
  now()
FROM seed_rag
ON CONFLICT (public_id) DO UPDATE SET
  agent_session_id = NULL,
  source_id = EXCLUDED.source_id,
  chunk_text = EXCLUDED.chunk_text,
  summary = EXCLUDED.summary,
  score = EXCLUDED.score,
  metadata = EXCLUDED.metadata,
  created_at = rag_evidence.created_at;
