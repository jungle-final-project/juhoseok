-- RAG source chunk for premium/open-budget requirement interpretation.
-- BuildQueryService must not inspect raw user wording for this decision; the parse Agent
-- retrieves this policy and writes performanceTier/budgetPolicy into parsed_context.

WITH seed_rag(public_id, source_id, chunk_text, summary, score, metadata) AS (
  VALUES (
    '00000000-0000-4000-8000-000000015004',
    'internal-rule-requirement-parse-premium-open-budget',
    'If a user asks for the best possible PC without giving a concrete budget, interpret phrases such as 끝판왕, 최고사양, 최고급, 최상급, 하이엔드, 플래그십, RTX 5090급, 돈 상관 없음, 예산 무관, 가장 좋은, or 제일 좋은 as premium intent. Keep budget null, set performanceTier to ENTHUSIAST, and set budgetPolicy to OPEN_BUDGET. If the user provides a concrete budget, keep budgetPolicy as USER_BUDGET and do not ignore the budget.',
    'Requirement parse internal rule: premium intent without a concrete budget should become ENTHUSIAST and OPEN_BUDGET, while explicit user budgets remain authoritative.',
    0.97000,
    '{"sourceType":"INTERNAL_RULE","purpose":"REQUIREMENT_PARSE","title":"Premium and open-budget parse rule","relatedFields":["performanceTier","budgetPolicy","budget"],"metadataVersion":1}'
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
