import { useMutation, useQuery } from '@tanstack/react-query';
import { AdminShell, DataTable, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { runPriceJob } from '../adminApi';
import { listParts } from '../../parts/partsApi';
import type { PartRow } from '../../parts/types';

export function AdminPartsPage() {
  const { data, isError, isLoading, refetch } = useQuery({
    queryKey: ['admin-parts'],
    queryFn: () => listParts({ size: 100, sort: 'category' })
  });
  const priceJobMutation = useMutation({
    mutationFn: () => runPriceJob(),
    onSuccess: () => {
      void refetch();
    }
  });
  const parts = data?.items ?? [];

  return (
    <AdminShell title="부품 / 가격 관리자">
      <div className="grid grid-cols-[1fr_360px] gap-5">
        <Panel title="부품 DB" subtitle={`${data?.total ?? 0}개 내부 자산`}>
          {isLoading ? <StateMessage type="info" title="부품 DB 로딩 중" body="서버의 parts 테이블에서 내부 쇼핑몰 자산을 불러오고 있습니다." /> : null}
          {isError ? <StateMessage type="warn" title="부품 DB 조회 실패" body="GET /api/parts 응답을 확인해야 합니다." /> : null}
          {!isLoading && !isError ? (
            <DataTable columns={['category', 'name', 'manufacturer', 'price', 'status', 'source']} rows={partRows(parts)} />
          ) : null}
        </Panel>
        <Panel title="가격 Job 상태">
          <StateMessage type="info" title="다나와 백업 스냅샷 기준" body="현재 seed는 DANAWA_BACKUP source와 raw_payload로 외부 가격 수집 백업 구조를 남깁니다." />
          <button onClick={() => priceJobMutation.mutate()} disabled={priceJobMutation.isPending} className="mt-5 w-full rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white disabled:bg-slate-400">
            {priceJobMutation.isPending ? '가격 Job 실행 중' : '가격 Job 실행'}
          </button>
          {priceJobMutation.isSuccess ? <div className="mt-3 text-xs font-bold text-emerald-700">가격 Job 요청 완료</div> : null}
          {priceJobMutation.isError ? <div className="mt-3 text-xs font-bold text-orange-700">가격 Job 요청 실패</div> : null}
        </Panel>
      </div>
    </AdminShell>
  );
}

function partRows(parts: PartRow[]) {
  return parts.map((part) => ({
    category: part.category,
    name: part.name,
    manufacturer: part.manufacturer ?? '-',
    price: `${part.price.toLocaleString()}원`,
    status: <StatusBadge status={part.status} />,
    source: part.latestPriceSource ?? '-'
  }));
}
