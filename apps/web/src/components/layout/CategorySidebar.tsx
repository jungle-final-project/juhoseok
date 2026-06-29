import { Link } from 'react-router-dom';

type CategorySidebarItem = string | {
  label: string;
  value: string;
};

const categoryRoutes: Record<string, string> = {
  'AI 추천': '/requirements/new',
  '셀프 견적': '/self-quote',
  CPU: '/self-quote?category=CPU',
  '메인보드': '/self-quote?category=MOTHERBOARD',
  RAM: '/self-quote?category=RAM',
  GPU: '/self-quote?category=GPU',
  SSD: '/self-quote?category=STORAGE',
  '파워': '/self-quote?category=PSU',
  '케이스': '/self-quote?category=CASE',
  '쿨러': '/self-quote?category=COOLER',
  '목표가 알림': '/my/quotes',
  'PC Agent': '/support/new',
  'AS 접수': '/support/new'
};

export function CategorySidebar({
  items,
  activeValue,
  onSelect
}: {
  items: CategorySidebarItem[];
  activeValue?: string;
  onSelect?: (value: string) => void;
}) {
  return (
    <aside className="panel w-[216px] p-4">
      <div className="mb-1 text-base font-bold">PC 카테고리</div>
      <div className="mb-4 text-xs text-slate-500">프로젝트 범위만 표시</div>
      <div className="space-y-2">
        {items.map((item, idx) => {
          const label = typeof item === 'string' ? item : item.label;
          const value = typeof item === 'string' ? item : item.value;
          const active = activeValue === value;

          if (onSelect) {
            return (
              <button
                key={label}
                type="button"
                onClick={() => onSelect(value)}
                className={`block w-full rounded border px-3 py-3 text-left text-sm hover:border-brand-blue hover:bg-brand-pale ${active ? 'border-brand-blue bg-brand-pale font-bold text-brand-blue' : 'border-slate-200 bg-slate-50 text-slate-900'}`}
              >
                {label}
              </button>
            );
          }

          return (
            <Link key={label} to={categoryRoutes[label] ?? (idx === 0 ? '/requirements/new' : idx === 1 ? '/self-quote' : '/')} className="block rounded border border-slate-200 bg-slate-50 px-3 py-2 text-sm hover:border-brand-blue hover:bg-brand-pale">
              {label}
            </Link>
          );
        })}
      </div>
    </aside>
  );
}
