import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CategorySidebar, DataTable, MetricCard, Panel, Screen, StatusBadge } from '../../../components/ui';
import { categories } from '../../quote/mocks/quoteMock';
import { listParts } from '../partsApi';
import type { PartRow } from '../types';

const partCategories = ['CPU', 'MOTHERBOARD', 'RAM', 'GPU', 'STORAGE', 'PSU', 'CASE', 'COOLER'];

export function SelfQuotePage() {
  const [category, setCategory] = useState<string>('');
  const [query, setQuery] = useState('');
  const { data, isError, isLoading } = useQuery({
    queryKey: ['parts', 'self-quote', category, query],
    queryFn: () => listParts({ category, q: query, size: 50, sort: 'category' })
  });
  const parts = data?.items ?? [];
  const selectedTotal = parts.slice(0, 8).reduce((sum, part) => sum + part.price, 0);

  return (
    <Screen>
      <div className="grid grid-cols-[216px_1fr_300px] gap-5">
        <CategorySidebar items={categories} />
        <Panel title="셀프 견적 / 부품 선택표" subtitle="내부 부품 DB 기준으로 견적 후보를 탐색합니다.">
          <div className="mb-4 grid grid-cols-[220px_1fr] gap-3">
            <select value={category} onChange={(event) => setCategory(event.target.value)} className="rounded border border-slate-300 px-3 py-2 text-sm">
              <option value="">전체 카테고리</option>
              {partCategories.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="부품명, 제조사, 사양 검색" className="rounded border border-slate-300 px-3 py-2 text-sm" />
          </div>
          {isLoading ? <div className="rounded border border-slate-200 p-5 text-sm text-slate-500">부품 목록을 불러오는 중입니다.</div> : null}
          {isError ? <div className="rounded border border-orange-200 bg-orange-50 p-5 text-sm text-orange-700">부품 목록 API를 불러오지 못했습니다.</div> : null}
          {!isLoading && !isError ? (
            <DataTable columns={['category', 'name', 'manufacturer', 'price', 'status', 'score']} rows={partRows(parts)} />
          ) : null}
        </Panel>
        <Panel title="검증 / 합계">
          <MetricCard label="표시 후보 합계" value={`${selectedTotal.toLocaleString()}원`} />
          <div className="mt-4 space-y-3">
            <button className="w-full rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white">Tool 검증하기</button>
            <Link to="/builds/00000000-0000-4000-8000-000000002001" className="block rounded border border-slate-300 px-4 py-3 text-center text-sm font-bold">추천 결과로 보기</Link>
          </div>
        </Panel>
      </div>
    </Screen>
  );
}

function partRows(parts: PartRow[]) {
  return parts.map((part) => ({
    category: part.category,
    name: part.name,
    manufacturer: part.manufacturer ?? '-',
    price: `${part.price.toLocaleString()}원`,
    status: <StatusBadge status={part.status} />,
    score: formatScore(part.benchmarkSummary?.score)
  }));
}

function formatScore(score?: number | string) {
  if (score === undefined || score === null) {
    return '-';
  }
  return typeof score === 'number' ? score.toFixed(1) : score;
}
