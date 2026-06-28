import { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { Screen, StateMessage } from '../../components/ui';

function hasDemoAdminToken() {
  return localStorage.getItem('buildgraph.token')?.includes('demo-jwt-admin') ?? false;
}

export function RequireAdmin({ children }: { children: ReactNode }) {
  if (hasDemoAdminToken()) {
    return <>{children}</>;
  }

  return <AdminPermissionRequiredPage />;
}

function AdminPermissionRequiredPage() {
  return (
    <Screen>
      <div className="mx-auto mt-20 grid w-[760px] grid-cols-[88px_1fr] gap-6 panel p-8">
        <div className="grid h-20 w-20 place-items-center rounded bg-brand-pale text-brand-blue">
          <ShieldAlert size={34} />
        </div>
        <div>
          <div className="text-xs font-bold uppercase text-slate-500">Admin access</div>
          <h1 className="mt-2 text-2xl font-bold text-brand-navy">관리자 권한이 필요합니다</h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            이 화면은 운영 담당자에게만 열립니다. 관리자 계정으로 로그인한 뒤 다시 접근하세요.
          </p>
          <div className="mt-5">
            <StateMessage type="info" title="현재 세션 확인" body="브라우저에 관리자 권한이 확인되지 않아 관리자 화면을 표시하지 않았습니다." />
          </div>
          <div className="mt-6 flex gap-3">
            <Link to="/login" className="rounded bg-brand-blue px-5 py-3 text-sm font-bold text-white">로그인으로 이동</Link>
            <Link to="/" className="rounded border border-slate-300 px-5 py-3 text-sm font-bold">홈으로 이동</Link>
          </div>
        </div>
      </div>
    </Screen>
  );
}
