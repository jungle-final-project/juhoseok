-- Reusable RAG knowledge chunks for requirement parsing.
-- These rows are source chunks: agent_session_id stays NULL until an Agent run copies one.

WITH seed_rag(public_id, source_id, chunk_text, summary, score, metadata) AS (
  VALUES
    (
      '00000000-0000-4000-8000-000000015001',
      'guide-requirement-parse-budget-resolution-workload',
      'Before recommending parts, parse the user message into budget, usage tags, resolution, preferred vendors, required conditions, and missing follow-up questions. Do not infer a missing budget or resolution unless it appears in the text or optional inputs.',
      'Requirement parse guide: extract budget, workload, resolution, vendors, hard conditions, and missing questions without inventing absent values.',
      0.95000,
      '{"sourceType":"GUIDE","purpose":"REQUIREMENT_PARSE","title":"Requirement parse field guide","relatedFields":["budget","usageTags","resolution","preferredVendors","mustHave"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000015002',
      'benchmark-requirement-parse-gaming-development',
      'Gaming requests such as PUBG, QHD, high refresh rate, or RTX preference should be tagged as GAMING. Mentions of IDE, programming, compile, Docker, or workstation-like multitasking should add DEVELOPMENT. If both appear, ask the user to choose workload ratio before final recommendation.',
      'Requirement parse benchmark: gaming and development can both apply, and mixed workloads should produce a workload-ratio follow-up question.',
      0.93000,
      '{"sourceType":"BENCHMARK","purpose":"REQUIREMENT_PARSE","title":"Gaming and development workload parse guide","relatedFields":["usageTags","questions"],"metadataVersion":1}'
    ),
    (
      '00000000-0000-4000-8000-000000015003',
      'internal-rule-requirement-parse-noise-upgrade',
      'Noise-sensitive phrases such as quiet, low-noise, silent, 조용, or 저소음 should set LOW_NOISE. Upgrade-related phrases should not change the current budget by themselves; instead ask about upgrade headroom and let the recommendation stage decide PSU, case, and motherboard margin.',
      'Requirement parse internal rule: map quiet or low-noise wording to LOW_NOISE and ask upgrade headroom instead of silently increasing budget.',
      0.91000,
      '{"sourceType":"INTERNAL_RULE","purpose":"REQUIREMENT_PARSE","title":"Noise and upgrade parse rule","relatedFields":["mustHave","questions","priority"],"metadataVersion":1}'
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
