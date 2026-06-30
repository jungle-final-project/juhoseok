import type { BuildSummary } from '../types';

export const categories = ['AI 추천', '셀프 견적', 'CPU', '메인보드', 'RAM', 'GPU', 'SSD', '파워', '케이스', '쿨러', '목표가 알림', 'PC Agent', 'AS 접수'];

export const builds: BuildSummary[] = [
  {
    id: '00000000-0000-4000-8000-000000002001',
    name: 'QHD 게임 균형형',
    recommendedFor: '균형 우선',
    summary: 'QHD 게임과 개발을 함께 고려한 샘플 Build입니다.',
    totalPrice: 1980000,
    confidence: 'MEDIUM',
    warnings: [{ message: 'PSU 여유율 확인 필요', severity: 'WARN' }],
    items: [
      { category: 'CPU', name: 'Ryzen 7 class', price: 420000 },
      { category: 'GPU', name: 'RTX 5070 class', price: 920000 },
      { category: 'RAM', name: 'DDR5 32GB', price: 160000 }
    ]
  },
  {
    id: '00000000-0000-4000-8000-000000002002',
    name: '개발 + 게임 혼합형',
    recommendedFor: '작업 병행',
    summary: '개발 IDE와 게임을 함께 쓰는 기준의 샘플 Build입니다.',
    totalPrice: 2120000,
    confidence: 'HIGH',
    warnings: [{ message: 'RAM 32GB 이상 권장', severity: 'WARN' }],
    items: [
      { category: 'CPU', name: 'Core Ultra 7 class', price: 450000 },
      { category: 'GPU', name: 'RTX 5070 Ti class', price: 1050000 },
      { category: 'RAM', name: 'DDR5 64GB', price: 260000 }
    ]
  },
  {
    id: '00000000-0000-4000-8000-000000002003',
    name: 'AI 실습 입문형',
    recommendedFor: 'VRAM 우선',
    summary: 'AI 실습과 CUDA 사용 가능성을 고려한 샘플 Build입니다.',
    totalPrice: 1620000,
    confidence: 'MEDIUM',
    warnings: [{ message: 'VRAM 한계 가능성', severity: 'WARN' }],
    items: [
      { category: 'CPU', name: 'Ryzen 5 class', price: 260000 },
      { category: 'GPU', name: 'RTX 5060 Ti class', price: 650000 },
      { category: 'RAM', name: 'DDR5 32GB', price: 160000 }
    ]
  }
];
