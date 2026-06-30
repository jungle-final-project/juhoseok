import { Link } from 'react-router-dom';
import { Save } from 'lucide-react';
import { DataTable, Panel, Screen, StatusBadge } from '../../../components/ui';
import { Field } from '../components/Field';
import { builds } from '../mocks/quoteMock';

export function MyQuotesPage() {
  return (
    <Screen>
      <div className="grid grid-cols-[1fr_340px] gap-5">
        <Panel title="내 견적함 / 목표가 알림" subtitle="저장 Build와 부품별 목표가 도달 알림을 관리합니다.">
          <DataTable columns={['id', 'name', 'price', 'confidence', 'action']} rows={builds.map((build) => ({ id: build.id, name: build.name, price: `${build.totalPrice.toLocaleString()}원`, confidence: <StatusBadge status={build.confidence} />, action: <Link to={`/builds/${build.id}`} className="font-bold text-brand-blue">상세</Link> }))} />
        </Panel>
        <Panel title="목표가 알림 등록">
          <div className="space-y-3">
            <Field label="부품 ID" value="gpu-4070s" />
            <Field label="목표가" value="850,000" />
            <Field label="이메일" value="user@example.com" />
            <button className="w-full rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white"><Save className="mr-1 inline" size={15} /> 알림 등록</button>
          </div>
        </Panel>
      </div>
    </Screen>
  );
}
