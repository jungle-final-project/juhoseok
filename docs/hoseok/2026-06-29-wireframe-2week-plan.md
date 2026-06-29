# 2026-06-29 와이어프레임 담당 파트 및 2주 계획

## 목적

오늘 작업은 GitHub/프로젝트 분석 결과를 기준으로 각 담당자가 Figma 와이어프레임에서 어떤 화면을 맡아야 하는지 정리하고, 2주 실행 계획을 Notion에 올릴 수 있는 형태로 고정하는 것이다.

기준 문서:

- `docs/ROUTE_OWNERSHIP.md`
- `docs/role-workspaces.md`
- `docs/API_CONTRACT.md`
- `docs/sprint-1-start-checklist.md`
- `docs/architecture.md`

Figma 기준:

- 원본 섹션: `138:2` / `SHOP-SPEC Real Desktop Screens - Compuzone style PLAN(2)`
- 캡처: `docs/hoseok/figma-138-2-section-overview.png`
- 5번 직접 담당 캡처: `docs/hoseok/figma-153-1880-admin-dashboard-degraded.png`
- 5번 AdminShell 참고 캡처: `docs/hoseok/figma-153-2011-admin-as-ticket-shell-reference.png`

## 담당자별 와이어프레임 매핑

| 담당 | Figma에서 찾아야 할 화면 | 주 route | 주 파일/도메인 | 주의할 경계 |
| --- | --- | --- | --- | --- |
| 1번 | 쇼핑몰 홈, AI 견적 입력, 추천 결과, 부품 변경, 내 견적함, 로그인/회원가입 화면 | `/`, `/requirements/new`, `/builds/:buildId`, `/builds/:buildId/change-part`, `/my/quotes`, `/login`, `/signup`, `/auth/callback` | `apps/web/src/features/quote`, `apps/web/src/features/auth/pages`, `build`, `requirement` | Auth 화면은 1번이지만 Auth API, token, guard는 5번이다. |
| 2번 | 셀프 견적, 부품 목록/필터, Tool check 결과, 가격 알림, 관리자 부품/가격 Job 화면 | `/self-quote`, `/admin/parts`, `/my/quotes` 가격 알림 영역 | `features/parts`, `features/admin/parts`, `part`, `price`, `tool` | `price_jobs` 실행 환경은 5번과 협업한다. Tool 호출 이력 저장은 3번 영역이다. |
| 3번 | Agent 세션 상세, Tool 호출 상세, RAG 근거 상세, 추천 근거 추적 화면 | `/admin/agent-sessions/:id`, `/admin/tool-invocations/:id`, `/admin/rag-evidence/:id` | `features/admin/agent`, `features/admin/evidence`, `agent`, `rag` | 추천 결과 UI는 1번, Tool 판정 로직은 2번, AS 원인 후보는 4번과 경계가 있다. |
| 4번 | PC Agent 안내/로그 업로드, AS 접수, AS 티켓 상세, 관리자 AS 티켓 목록/상세 | `/support/new`, `/support/:ticketId`, `/admin/as-tickets`, `/admin/as-tickets/:ticketId` | `apps/pc-agent`, `features/support`, `features/admin/as-tickets`, `log`, `ticket` | 사용자 화면에는 AS 원인 후보를 노출하지 않는다. 관리자 AS 화면은 4번이고 shell/guard만 5번이다. |
| 5번 | 관리자 운영 대시보드, AdminShell/sidebar/topbar, 관리자 권한 guard, 공통 컴포넌트, 인프라/CI 화면 기준 | `/admin` | `features/admin/pages/AdminDashboardPage.tsx`, `components`, `features/admin/shell`, `lib/api.ts`, `user`, `auth`, `admin`, `common`, `infra`, `tools`, `.github/workflows` | 도메인별 관리자 내부 화면은 각 owner가 맡는다. 5번은 shell, guard, dashboard frame, auth 공통, CI/infra를 맡는다. |

## 5번 담당 화면 판정

5번이 직접 구현해야 하는 Figma 화면은 `153:1880 STATE-15 ADMIN-01 운영 대시보드 / degraded`다.

포함 범위:

- Admin sidebar: 대시보드, Agent 세션, Tool 이력, RAG 근거, 부품/가격, AS 티켓, 가격 Job, 부하 테스트
- Admin topbar: route title, admin search, 내보내기, 작업 실행
- Dashboard metric cards: LLM Queue, 비LLM API, AS OPEN, 가격 Job
- 운영 상태 alert: degraded, 큐 대기 p95 경고
- 관리자 할 일, 최근 Agent 세션, 운영 작업 summary frame

직접 구현이 아닌 범위:

- `/admin/as-tickets` 내부 AS 티켓 큐와 상세 패널은 4번 담당
- `/admin/parts` 내부 부품/가격 Job 상세는 2번 담당
- `/admin/agent-sessions`, `/admin/tool-invocations`, `/admin/rag-evidence` 상세는 3번 담당
- `/login`, `/signup`, `/auth/callback` 화면 자체는 1번 담당

단, 위 모든 관리자 화면은 `RequireAdmin`과 `AdminShell`을 통과하므로 guard, layout slot, navigation contract는 5번 책임이다.

## 현재 코드와 Figma 차이

| 항목 | 현재 코드 | Figma/계약 기준 | 다음 조치 |
| --- | --- | --- | --- |
| Admin route guard | `/admin/*`가 `RequireAdmin`으로 감싸져 있음 | 관리자 API는 ADMIN 권한 필요 | 유지. 401/403 화면 분기를 더 명확히 한다. |
| AdminShell nav | Dashboard, Agent/RAG, Parts/Price, AS Tickets 4개 | 대시보드, Agent 세션, Tool 이력, RAG 근거, 부품/가격, AS 티켓, 가격 Job, 부하 테스트 | nav item을 세분화하되 각 상세 route owner와 합의한다. |
| Dashboard API 타입 | 프론트가 `llmQueueP95`, `apiP95`, `asOpen`, `recommendationSuccess` 기대 | OpenAPI는 `agentRunning`, `openTickets`, `priceJobsRunning`, `degraded`, `generatedAt` | 5번이 DTO 필드 정합성을 맞춘다. API 계약 변경 시 `openapi.yaml`도 같이 수정한다. |
| Dashboard 화면 | metric 4개와 최근 Agent/운영 경고 일부 | Figma는 degraded alert, 운영 작업, 관리자 할 일까지 포함 | 5번이 dashboard frame만 구현하고 도메인별 상세 데이터는 각 owner API에 맡긴다. |
| Notion 업로드 | 대상 페이지 미확정 | 검색상 `프로젝트 개요 & 기획`이 관련 있어 보임 | 사용자에게 Notion 페이지/DB 링크를 받아 업로드한다. |

## 2주 실행 계획

### 1주차: 2026-06-29 월요일 ~ 2026-07-05 일요일

목표: 담당 화면과 API 계약을 고정하고, 각자 첫 PR이 실제 route/API/state/service와 연결되게 만든다.

| 날짜 | 팀 공통 목표 | 1번 | 2번 | 3번 | 4번 | 5번 |
| --- | --- | --- | --- | --- | --- | --- |
| 06-29 월 | Figma 담당 파트 확정, Notion 업로드, PR 범위 고정 | quote/auth 화면 범위 확인 | self-quote/parts/admin parts 확인 | agent/rag/tool admin 확인 | support/as/admin ticket 확인 | `/admin` dashboard/AdminShell 범위 확정 |
| 06-30 화 | API/DB/route owner 재확인 | `quoteApi.ts`와 추천 DTO 비교 | parts/tool/price DTO 초안 | agent state와 evidence DTO 초안 | JSONL/upload/ticket DTO 초안 | auth/admin DTO, 401/403, dashboard DTO 정합성 확인 |
| 07-01 수 | 화면 state skeleton 연결 | 로그인/회원가입 state, 요구사항 입력 loading/error | 셀프 견적 표, Tool check state | Agent/RAG/Tool 상세 loading/error | AS 접수 동의, 티켓 상세 state | AdminShell nav, guard state, dashboard loading/error |
| 07-02 목 | 백엔드 skeleton과 contract test 시작 | requirement/build service 경계 | parts/price/tool service 경계 | agent/rag service 경계 | log/ticket service 경계 | user/auth/admin/common, security, health 경계 |
| 07-03 금 | 1차 통합 smoke | 추천 생성 route smoke | parts/tool/price smoke | admin evidence smoke | support/as smoke | web build/test, OpenAPI, compose config, bootJar |
| 07-04 토 | 버퍼/리뷰 | PR 피드백 반영 | PR 피드백 반영 | PR 피드백 반영 | PR 피드백 반영 | CI/infra 실패 원인 정리 |
| 07-05 일 | 1주차 동결 | 다음 주 통합 이슈 정리 | 다음 주 통합 이슈 정리 | 다음 주 통합 이슈 정리 | 다음 주 통합 이슈 정리 | merge 순서와 검증 명령 정리 |

1주차 완료 기준:

- 각 담당자가 자기 Figma 화면과 route/API/file owner를 Notion에서 확인할 수 있다.
- API 요청/응답 변경이 있으면 `docs/API_CONTRACT.md`와 `docs/openapi.yaml`이 같이 수정된다.
- page component가 `api()`를 직접 호출하지 않고 담당 `*Api.ts` wrapper를 사용한다.
- 각 owner별 최소 route smoke 결과를 PR에 남긴다.

### 2주차: 2026-07-06 월요일 ~ 2026-07-12 일요일

목표: 핵심 E2E 흐름을 통합하고, 관리자 화면에서 각 도메인 상태를 볼 수 있게 만든다.

| 날짜 | 팀 공통 목표 | 1번 | 2번 | 3번 | 4번 | 5번 |
| --- | --- | --- | --- | --- | --- | --- |
| 07-06 월 | 1차 API 연결 확장 | 추천 결과와 build 상세 연결 | parts 목록/상세, price alert 연결 | agent session/run, RAG search 연결 | 로그 업로드, AS 티켓 생성 연결 | dashboard DTO 정합성, audit logs, AdminShell route 정리 |
| 07-07 화 | 도메인별 성공/실패 상태 구현 | change-part 409 처리 | Tool PASS/WARN/FAIL, price job 409 처리 | agent 상태 전이 409, fallback 표시 | JSONL validation, ticket 상태 전이 409 | 401/403 분기, auth/me, health DB smoke |
| 07-08 수 | 사용자 E2E 1차 | 자연어 입력 -> 추천 -> 상세 | 부품 변경 후보와 가격 알림 협업 | 추천 근거 trace 저장 협업 | AS 접수 진입 조건 확인 | 공통 API error normalization 점검 |
| 07-09 목 | 관리자 E2E 1차 | build/evidence link 확인 | admin parts/price job 확인 | admin agent/tool/rag 상세 확인 | admin AS ticket 상세 확인 | `/admin` dashboard와 각 admin route shell 통합 |
| 07-10 금 | 2주차 통합 PR 후보 정리 | quote/auth 마감 이슈 정리 | parts/price/tool 마감 이슈 정리 | agent/rag/tool 마감 이슈 정리 | support/as 마감 이슈 정리 | CI, Docker Compose, k6 smoke, 검증 명령 정리 |
| 07-11 토 | 버퍼/버그 수정 | E2E 깨진 흐름 수정 | E2E 깨진 흐름 수정 | E2E 깨진 흐름 수정 | E2E 깨진 흐름 수정 | infra/CI 안정화 |
| 07-12 일 | 다음 스프린트 준비 | 남은 기능 backlog 작성 | 남은 기능 backlog 작성 | 남은 기능 backlog 작성 | 남은 기능 backlog 작성 | 부하 테스트/운영 체크리스트 작성 |

2주차 완료 기준:

- 자연어 입력 -> 요구사항 파싱 -> 추천 결과 표시 -> 부품 변경 비교 -> 가격 알림 등록까지 최소 한 흐름이 연결된다.
- PC Agent 로그 export -> AS 접수 -> 티켓 생성 -> 관리자 AS 확인 흐름이 최소 mock/dev API 기준으로 연결된다.
- 관리자 `/admin`에서 운영 요약을 보고 각 도메인 admin route로 이동할 수 있다.
- `npm --prefix apps/web run build`, `npm --prefix apps/web run test`, `python tools/validate_openapi.py`, `docker compose config`, `./gradlew bootJar --no-daemon` 검증 기준을 유지한다.

## 오늘 바로 할 일

1. 이 문서를 Notion에 업로드한다.
2. Notion에서 각 담당자별 Figma 화면 이름과 route를 확인하도록 공유한다.
3. 5번은 `AdminDashboardPage.tsx`와 `adminApi.ts`의 DTO 불일치를 우선 이슈로 잡는다.
4. AdminShell navigation을 Figma 기준으로 세분화할지 팀에 확인한다.
5. Notion에는 "도메인별 관리자 내부 화면은 각 owner, shell/guard는 5번"이라는 경계를 굵게 남긴다.

## Notion 업로드 전 필요한 결정

업로드 대상이 필요하다.

- 특정 Notion 페이지 아래에 만들 경우: 페이지 URL 필요
- 특정 Notion 데이터베이스에 task/page로 만들 경우: 데이터베이스 URL 필요
- 대상이 없으면 private standalone page로 만들 수 있지만, 팀 페이지에 자동 배치되지는 않는다.

