import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { DataTable, MetricCard, Panel, Screen, StateMessage, StatusBadge } from '../../../components/ui';
import { QuoteCard } from '../components/QuoteCard';
import { getBuild } from '../quoteApi';
import type { ToolResult } from '../types';

export function BuildResultPage() {
  const { buildId = '00000000-0000-4000-8000-000000002001' } = useParams();
  const { data: build, isLoading, isError } = useQuery({
    queryKey: ['build', buildId],
    queryFn: () => getBuild(buildId)
  });

  if (isLoading) {
    return (
      <Screen>
        <Panel title="추천 Build 결과">
          <StateMessage type="info" title="Build 로딩 중" body="추천 build 상세와 Tool 검증 결과를 불러오고 있습니다." />
        </Panel>
      </Screen>
    );
  }

  if (isError || !build) {
    return (
      <Screen>
        <Panel title="추천 Build 결과">
          <StateMessage type="warn" title="Build 조회 실패" body="선택한 추천 build를 불러오지 못했습니다." />
        </Panel>
      </Screen>
    );
  }

  return (
    <Screen>
      <div className="grid grid-cols-[1fr_320px] gap-5">
        <div className="space-y-5">
          <Panel title={`추천 Build 결과 / ${build.name}`} subtitle={build.id}>
            <div className="flex gap-4">
              <QuoteCard build={build} selected />
            </div>
          </Panel>
          <Panel title="구성 부품">
            <DataTable columns={['category', 'name', 'manufacturer', 'price']} rows={build.items.map((item) => ({
              category: item.category,
              name: item.name,
              manufacturer: item.manufacturer ?? '-',
              price: `${item.price.toLocaleString()}원`
            }))} />
          </Panel>
          <Panel title="Tool 검증 결과">
            <DataTable columns={['tool', 'status', 'confidence', 'summary']} rows={toolRows(build.toolResults ?? [])} />
          </Panel>
        </div>
        <Panel title="견적 요약 / 액션">
          <div className="space-y-4">
            <MetricCard label="총액" value={`${build.totalPrice.toLocaleString()}원`} />
            <StateMessage
              type={build.warnings.length > 0 ? 'warn' : 'success'}
              title={build.warnings.length > 0 ? '확인 필요' : '주요 조건 충족'}
              body={build.warnings[0]?.message ?? '현재 구성은 저장된 내부 자산 기준 Tool 검증을 통과했습니다.'}
            />
            <Link to={`/builds/${build.id}/change-part`} className="block rounded bg-brand-blue px-4 py-3 text-center text-sm font-bold text-white">부품 변경 비교</Link>
            <Link to="/my/quotes" className="block rounded border border-slate-300 px-4 py-3 text-center text-sm font-bold">견적 저장</Link>
          </div>
        </Panel>
      </div>
    </Screen>
  );
}

function toolRows(results: ToolResult[]) {
  return results.map((row) => ({
    tool: row.tool,
    status: <StatusBadge status={row.status} />,
    confidence: <StatusBadge status={row.confidence} />,
    summary: row.summary
  }));
}
