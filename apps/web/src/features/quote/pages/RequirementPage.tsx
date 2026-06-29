import { FormEvent, useMemo, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { DataTable, Panel, Screen, StateMessage, StatusBadge } from '../../../components/ui';
import { QuoteCard } from '../components/QuoteCard';
import { parseRequirements, recommendBuild } from '../quoteApi';
import type { BuildSummary, ParsedRequirement, ToolResult } from '../types';

export function RequirementPage() {
  const [message, setMessage] = useState('');
  const [budget, setBudget] = useState('');
  const [usageTags, setUsageTags] = useState('');
  const [resolution, setResolution] = useState('');
  const [preferredVendors, setPreferredVendors] = useState('');
  const [priority, setPriority] = useState('');
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [selectedBuildId, setSelectedBuildId] = useState<string | null>(null);

  const parseMutation = useMutation({
    mutationFn: () => parseRequirements({
      message: message.trim(),
      budget: numberOrUndefined(budget),
      usageTags: splitText(usageTags),
      resolution: resolution.trim() || undefined,
      preferredVendors: splitText(preferredVendors),
      priority: priority.trim() || undefined
    }),
    onSuccess: () => {
      setAnswers({});
      setSelectedBuildId(null);
      recommendMutation.reset();
    }
  });

  const recommendMutation = useMutation({
    mutationFn: (requirement: ParsedRequirement) => recommendBuild(requirement.id, answers),
    onSuccess: (data) => {
      setSelectedBuildId(data.recommendations[0]?.id ?? null);
    }
  });

  const parsed = parseMutation.data;
  const recommendations = recommendMutation.data?.recommendations ?? [];
  const selectedBuild = useMemo(
    () => recommendations.find((build) => build.id === selectedBuildId) ?? recommendations[0],
    [recommendations, selectedBuildId]
  );
  const canParse = message.trim().length > 0 && !parseMutation.isPending;
  const canRecommend = Boolean(parsed) && !recommendMutation.isPending;

  function submitParse(event: FormEvent) {
    event.preventDefault();
    if (canParse) {
      parseMutation.mutate();
    }
  }

  function runRecommend() {
    if (parsed && canRecommend) {
      recommendMutation.mutate(parsed);
    }
  }

  return (
    <Screen>
      <div className="space-y-5">
        <div className="grid grid-cols-[520px_1fr_320px] gap-5">
          <Panel title="AI 견적 입력" subtitle="자연어 요구사항은 필수, 나머지는 추천 품질 보강용 선택 입력입니다.">
            <form onSubmit={submitParse} className="space-y-4">
              <textarea
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                className="h-36 w-full rounded border border-slate-300 p-4 text-sm outline-none focus:border-brand-blue"
                placeholder="예: 200만원 안에서 QHD 게임과 개발을 같이 할 PC 추천해줘. NVIDIA 선호."
              />
              <div className="grid grid-cols-2 gap-3">
                <Input label="예산" value={budget} onChange={setBudget} placeholder="2000000" />
                <Input label="주 용도" value={usageTags} onChange={setUsageTags} placeholder="게임, 개발" />
                <Input label="해상도" value={resolution} onChange={setResolution} placeholder="QHD" />
                <Input label="브랜드 선호" value={preferredVendors} onChange={setPreferredVendors} placeholder="NVIDIA" />
                <div className="col-span-2">
                  <Input label="우선순위" value={priority} onChange={setPriority} placeholder="성능 > 안정성 > 가격" />
                </div>
              </div>
              <div className="flex gap-3">
                <button disabled={!canParse} className="rounded bg-brand-blue px-5 py-2 text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-slate-300">
                  {parseMutation.isPending ? '분석 중' : '요구사항 분석'}
                </button>
                <button type="button" onClick={() => { setMessage(''); setBudget(''); setUsageTags(''); setResolution(''); setPreferredVendors(''); setPriority(''); }} className="rounded border border-slate-300 px-5 py-2 text-sm font-bold">
                  초기화
                </button>
              </div>
            </form>
          </Panel>

          <Panel title="파싱 결과" subtitle="분석 API가 저장한 requirement 기준">
            {parseMutation.isIdle ? <StateMessage type="info" title="아직 분석 전" body="왼쪽 자연어 요구사항을 입력하고 분석을 실행하면 구조화된 결과가 표시됩니다." /> : null}
            {parseMutation.isPending ? <StateMessage type="info" title="요구사항 분석 중" body="자연어에서 예산, 용도, 해상도, 선호 조건을 추출하고 있습니다." /> : null}
            {parseMutation.isError ? <StateMessage type="warn" title="요구사항 분석 실패" body="자연어 입력이 비어 있거나 API 응답을 불러오지 못했습니다." /> : null}
            {parsed ? <DataTable columns={['필드', '값', '확신도']} rows={parseRows(parsed)} /> : null}
          </Panel>

          <Panel title="추가 질문" subtitle="모호한 항목만 선택적으로 답변">
            {!parsed ? <StateMessage type="info" title="분석 후 활성화" body="추가 질문은 파싱 결과를 기반으로 생성됩니다." /> : null}
            {parsed ? (
              <div className="space-y-3">
                {parsed.questions.length === 0 ? <StateMessage type="success" title="추가 질문 없음" body="추천 생성에 필요한 기본 조건이 충족되었습니다." /> : null}
                {parsed.questions.map((question) => (
                  <label key={question.key} className="block rounded border border-slate-200 bg-white p-3 text-xs">
                    <span className="mb-2 block font-bold text-slate-700">{question.label}</span>
                    <select
                      value={answers[question.key] ?? ''}
                      onChange={(event) => setAnswers((current) => ({ ...current, [question.key]: event.target.value }))}
                      className="h-9 w-full rounded border border-slate-300 px-2"
                    >
                      <option value="">기본값 사용</option>
                      {question.options.map((option) => <option key={option} value={option}>{option}</option>)}
                    </select>
                  </label>
                ))}
                <button type="button" disabled={!canRecommend} onClick={runRecommend} className="w-full rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-slate-300">
                  {recommendMutation.isPending ? '추천 생성 중' : '추천 결과 보기'}
                </button>
              </div>
            ) : null}
          </Panel>
        </div>

        {recommendMutation.isPending ? (
          <Panel title="추천 결과 생성 중" subtitle="내부 자산 조합과 Tool 검증을 실행하고 있습니다.">
            <StateMessage type="info" title="Build 생성 중" body="parts DB에서 후보를 선택하고 compatibility, power, size, performance, price 검증을 수행합니다." />
          </Panel>
        ) : null}

        {recommendMutation.isError ? (
          <Panel title="추천 결과 오류">
            <StateMessage type="warn" title="추천 생성 실패" body="내부 자산 또는 Agent 실행 결과를 불러오지 못했습니다." />
          </Panel>
        ) : null}

        {recommendations.length > 0 ? (
          <div className="grid grid-cols-[1fr_420px] gap-5">
            <div className="space-y-5">
              <Panel title="추천 빌드 3개" subtitle="내부 자산, 저장된 현재가, Tool 검증 결과를 기반으로 생성">
                <div className="flex gap-4 overflow-x-auto pb-1">
                  {recommendations.map((build) => (
                    <QuoteCard key={build.id} build={build} selected={build.id === selectedBuild?.id} onSelect={(nextBuild) => setSelectedBuildId(nextBuild.id)} />
                  ))}
                </div>
              </Panel>
              <Panel title="Tool 검증 결과" subtitle={selectedBuild ? `${selectedBuild.name} 기준` : undefined}>
                <DataTable columns={['tool', 'status', 'confidence', 'summary']} rows={toolRows(selectedBuild?.toolResults ?? [])} />
              </Panel>
            </div>
            <Panel title="RAG / Agent 근거 요약">
              <div className="space-y-4">
                <StateMessage
                  type={selectedBuild?.agentSummary ? 'success' : 'info'}
                  title={selectedBuild?.agentSummary ? 'Agent 요약 생성됨' : 'Agent 요약 대기'}
                  body={selectedBuild?.agentSummary ?? '추천 build와 Tool 결과는 생성되었고, Agent summary는 실행 환경에 따라 비어 있을 수 있습니다.'}
                />
                <DataTable columns={['항목', '값']} rows={[
                  { 항목: 'agentSessionId', 값: selectedBuild?.agentSessionId ?? '-' },
                  { 항목: 'evidenceIds', 값: selectedBuild?.evidenceIds?.length ? `${selectedBuild.evidenceIds.length}개` : '-' },
                  { 항목: 'warnings', 값: selectedBuild?.warnings?.length ? `${selectedBuild.warnings.length}개` : '없음' }
                ]} />
              </div>
            </Panel>
          </div>
        ) : null}
      </div>
    </Screen>
  );
}

function Input({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return (
    <label className="block text-xs font-bold text-slate-600">
      <span className="mb-1 block">{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="h-10 w-full rounded border border-slate-300 px-3 text-sm font-normal text-slate-900 outline-none focus:border-brand-blue" />
    </label>
  );
}

function parseRows(parsed: ParsedRequirement) {
  const context = parsed.parsedContext ?? {};
  const confidence = context.confidence instanceof Object && !Array.isArray(context.confidence) ? context.confidence as Record<string, unknown> : {};
  return [
    { 필드: 'parseMode', 값: String(context.parseMode ?? '-'), 확신도: <StatusBadge status={String(context.parseMode).includes('FALLBACK') ? 'WARN' : 'HIGH'} /> },
    { 필드: 'parser', 값: String(context.parser ?? '-'), 확신도: <StatusBadge status={parsed.agentSessionId ? 'HIGH' : 'MEDIUM'} /> },
    { 필드: 'budget', 값: parsed.budget ? `${parsed.budget.toLocaleString()}원` : '-', 확신도: <StatusBadge status={String(confidence.budget ?? 'LOW')} /> },
    { 필드: 'usageTags', 값: parsed.usageTags.join(', '), 확신도: <StatusBadge status={String(confidence.usageTags ?? 'MEDIUM')} /> },
    { 필드: 'resolution', 값: String(context.resolution ?? '-'), 확신도: <StatusBadge status={String(confidence.resolution ?? 'LOW')} /> },
    { 필드: 'preferredVendors', 값: arrayText(context.preferredVendors), 확신도: <StatusBadge status={String(confidence.preferredVendors ?? 'LOW')} /> },
    { 필드: 'mustHave', 값: arrayText(context.mustHave), 확신도: <StatusBadge status="MEDIUM" /> },
    { 필드: 'parseEvidence', 값: parsed.evidenceIds?.length ? `${parsed.evidenceIds.length}개` : '-', 확신도: <StatusBadge status={parsed.evidenceIds?.length ? 'HIGH' : 'LOW'} /> },
    { 필드: 'agentSummary', 값: parsed.agentSummary ?? String(context.parseNotes ?? '-'), 확신도: <StatusBadge status={parsed.agentSummary ? 'HIGH' : 'MEDIUM'} /> }
  ];
}

function toolRows(results: ToolResult[]) {
  return results.map((row) => ({
    tool: row.tool,
    status: <StatusBadge status={row.status} />,
    confidence: <StatusBadge status={row.confidence} />,
    summary: row.summary
  }));
}

function splitText(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function numberOrUndefined(value: string) {
  const normalized = value.replace(/,/g, '').trim();
  if (!normalized) {
    return undefined;
  }
  const parsed = Number.parseInt(normalized, 10);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function arrayText(value: unknown) {
  return Array.isArray(value) && value.length > 0 ? value.join(', ') : '-';
}
