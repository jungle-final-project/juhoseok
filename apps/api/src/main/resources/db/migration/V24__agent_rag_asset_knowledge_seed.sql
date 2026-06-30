-- Reusable RAG knowledge chunks for Agent/RAG owner.
-- agent_session_id is NULL on purpose: these rows are searchable source chunks.
-- Agent runs copy the selected chunk into a session-bound rag_evidence row.

WITH seed_rag(public_id, source_id, chunk_text, summary, score, metadata) AS (
  VALUES
    (
      '00000000-0000-4000-8000-000000014001',
      'internal-rule-build-qhd-gaming-gpu-priority',
      'QHD gaming and game-development builds should prioritize the GPU class first, then keep CPU, RAM, PSU, and case airflow balanced. Use the saved internal parts catalog and current price snapshots before recommending an upgrade.',
      'QHD gaming recommendation rule: prioritize GPU class while checking CPU balance, memory, power margin, airflow, and current saved price.',
      0.94000,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_RECOMMEND","title":"QHD gaming build recommendation rule","relatedCategories":["GPU","CPU","RAM","PSU","CASE"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014002',
      'part-catalog-rtx50-tool-ready-dimensions',
      'The active internal GPU catalog contains RTX 50-series assets with lengthMm, widthMm, heightMm, slotWidth, wattage, requiredSystemPowerW, powerConnector, current Naver offer, and price snapshot fields. Size and power tools can use these fields without calling external APIs during user requests.',
      'RTX 50 GPU catalog evidence: dimensions, slot width, wattage, connector, current offer, and price snapshot are stored for Tool use.',
      0.93000,
      '{"sourceType":"PART_SPEC","purpose":"BUILD_RECOMMEND","title":"RTX 50 internal GPU catalog fields","relatedCategories":["GPU"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014003',
      'internal-rule-case-gpu-clearance',
      'For case compatibility, compare GPU lengthMm against case maxGpuLengthMm and leave assembly margin when possible. Also check GPU slotWidth, case airflow, and CPU cooler height before recommending compact cases.',
      'Case and GPU fit rule: compare GPU length with maxGpuLengthMm and consider slot width, airflow, and cooler height.',
      0.91000,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_EXPLAIN","title":"Case GPU clearance and cooler fit rule","relatedCategories":["GPU","CASE","COOLER"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014004',
      'internal-rule-psu-atx31-power-margin',
      'For RTX 50-series builds, power checks should compare total estimated wattage, GPU requiredSystemPowerW, PSU capacityW, efficiency, ATX 3.1 support, and 12V-2x6 or 8-pin connector compatibility. A WARN result is appropriate when the margin is narrow even if the build can boot.',
      'PSU power rule: compare capacity, required system power, efficiency, ATX 3.1 support, and GPU connector compatibility.',
      0.90000,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_RECOMMEND","title":"ATX 3.1 PSU margin rule","relatedCategories":["GPU","PSU"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014005',
      'benchmark-guide-ram-video-dev-floor',
      'For video editing, development, and AI-adjacent workloads, 32GB RAM is a practical minimum and 64GB or more can be recommended when logs or workload descriptions show memory pressure. DDR5 speed and module count should be considered with motherboard memory support.',
      'RAM workload evidence: 32GB is a practical floor for editing/development, and larger DDR5 kits are justified by memory pressure.',
      0.87000,
      '{"sourceType":"BENCHMARK","purpose":"BUILD_EXPLAIN","title":"RAM capacity workload guide","relatedCategories":["RAM","MOTHERBOARD"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014006',
      'support-guide-gpu-thermal-frame-drop',
      'If a PC shows sustained GPU temperature spikes together with frame-time drops after several minutes of gaming, the first AS hypothesis should be thermal throttling, airflow restriction, fan curve, driver instability, or case cooling limits before recommending a GPU replacement.',
      'AS troubleshooting evidence: GPU temperature spikes plus frame-time drops point to thermal or driver causes before replacement.',
      0.92000,
      '{"sourceType":"TROUBLESHOOTING","purpose":"AS_ANALYZE","title":"GPU thermal throttling and frame drop guide","relatedCategories":["GPU","CASE","COOLER"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014007',
      'support-guide-airflow-upgrade-before-gpu',
      'When support logs suggest high GPU temperature but the GPU class is otherwise adequate, recommend airflow checks, case fan direction, dust cleaning, and CPU/GPU cooler inspection before suggesting an expensive GPU upgrade.',
      'AS upgrade evidence: when GPU class is adequate, check airflow and cooling before recommending replacement.',
      0.88000,
      '{"sourceType":"GUIDE","purpose":"AS_ANALYZE","title":"Airflow-first AS upgrade guide","relatedCategories":["GPU","CASE","COOLER"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000014008',
      'price-guide-saved-snapshot-first',
      'User-facing part lists and Agent price reasoning must use saved parts.price, part_external_offers, and price_snapshots. Naver Shopping Search refresh is an admin or scheduler operation, not a user request-time lookup.',
      'Price reasoning rule: use saved current price and snapshots; external search refresh belongs to admin or scheduler flows.',
      0.89000,
      '{"sourceType":"INTERNAL_RULE","purpose":"BUILD_EXPLAIN","title":"Saved price snapshot first rule","relatedCategories":["GPU","CPU","PSU","STORAGE","RAM"],"metadataVersion":1}'
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
