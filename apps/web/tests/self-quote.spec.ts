import { expect, test } from '@playwright/test';

test('filters internal assets by sidebar category on self quote page', async ({ page }) => {
  const requestedCategories: string[] = [];

  await page.route('**/api/parts**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.includes('/price-history')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          partId: 'part-gpu-test',
          partName: 'RTX 4070 SUPER 테스트',
          currentPrice: 890000,
          days: 3650,
          source: 'NAVER_SHOPPING_SEARCH',
          items: [
            { price: 890000, source: 'NAVER_SHOPPING_SEARCH', collectedAt: '2026-06-29T00:00:00Z' }
          ],
          summary: {
            sampleCount: 1,
            currentPrice: 890000,
            minPrice: 890000,
            maxPrice: 890000,
            firstPrice: 890000,
            lastPrice: 890000,
            changeAmount: 0,
            changeRatePercent: 0
          }
        })
      });
      return;
    }
    const category = url.searchParams.get('category') ?? '';
    requestedCategories.push(category);

    const items = category === 'GPU'
      ? [
          {
            id: 'part-gpu-test',
            category: 'GPU',
            name: 'RTX 4070 SUPER 테스트',
            manufacturer: 'NVIDIA',
            price: 890000,
            status: 'ACTIVE',
            benchmarkSummary: { score: 92.4 },
            externalOffer: {
              imageUrl: 'https://example.test/rtx4070.png',
              supplierName: '테스트몰',
              offerUrl: 'https://example.test/rtx4070',
              lowPrice: 890000,
              source: 'NAVER_SHOPPING_SEARCH'
            }
          }
        ]
      : [
          {
            id: 'part-cpu-test',
            category: 'CPU',
            name: 'Ryzen 5 테스트',
            manufacturer: 'AMD',
            price: 260000,
            status: 'ACTIVE',
            benchmarkSummary: { score: 81.1 },
            externalOffer: {
              imageUrl: 'https://example.test/ryzen.png',
              supplierName: 'CPU테스트몰',
              offerUrl: 'https://example.test/ryzen',
              lowPrice: 260000,
              source: 'NAVER_SHOPPING_SEARCH'
            }
          }
        ];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items,
        page: 0,
        size: 50,
        total: items.length
      })
    });
  });

  await page.goto('/self-quote');
  await expect(page.getByText('셀프 견적 / 전체 부품 목록')).toBeVisible();
  await expect(page.getByText('Ryzen 5 테스트')).toBeVisible();
  await expect(page.getByRole('img', { name: 'Ryzen 5 테스트 제품 사진' })).toBeVisible();
  await expect(page.getByText('CPU테스트몰')).toBeVisible();

  await page.getByRole('button', { name: 'GPU' }).click();

  await expect(page.getByText('GPU 부품 목록')).toBeVisible();
  await expect(page.getByText('RTX 4070 SUPER 테스트')).toBeVisible();
  await expect(page.getByRole('img', { name: 'RTX 4070 SUPER 테스트 제품 사진' })).toBeVisible();
  await expect(page.getByText('테스트몰')).toBeVisible();
  await expect(page.getByText('ACTIVE')).toHaveCount(0);
  await expect(page.getByText('92.4')).toHaveCount(0);
  expect(requestedCategories).toContain('GPU');

  await page.getByRole('button', { name: 'RTX 4070 SUPER 테스트 견적 담기' }).click();
  await expect(page.getByText('견적 합계')).toBeVisible();
  await expect(page.getByText('890,000원')).toHaveCount(3);
  await expect(page.getByText('가격 기록 1개')).toBeVisible();

  await page.getByRole('button', { name: 'RTX 4070 SUPER 테스트 견적에서 제거' }).click();
  await expect(page.getByText('왼쪽 목록에서 부품을 담으면 이곳에 내 견적이 쌓입니다.')).toBeVisible();
});

test('opens cooler internal assets from home category link', async ({ page }) => {
  await page.route('**/api/parts**', async (route) => {
    const url = new URL(route.request().url());
    const category = url.searchParams.get('category') ?? '';
    const items = category === 'COOLER'
      ? [
          {
            id: 'part-cooler-home-test',
            category: 'COOLER',
            name: 'Liquid Freezer III 360 테스트',
            manufacturer: 'ARCTIC',
            price: 165000,
            status: 'ACTIVE',
            benchmarkSummary: { score: 77.7 },
            attributes: {
              shortSpec: '360mm AIO, AM5/LGA1851'
            },
            externalOffer: {
              imageUrl: 'https://example.test/cooler.png',
              supplierName: '쿨러테스트몰',
              offerUrl: 'https://example.test/cooler',
              lowPrice: 165000,
              source: 'NAVER_SHOPPING_SEARCH'
            }
          }
        ]
      : [];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items,
        page: 0,
        size: 50,
        total: items.length
      })
    });
  });

  await page.goto('/');
  await page.getByRole('link', { name: '쿨러' }).click();

  await expect(page).toHaveURL('/self-quote?category=COOLER');
  await expect(page.getByText('쿨러 부품 목록')).toBeVisible();
  await expect(page.getByText('Liquid Freezer III 360 테스트')).toBeVisible();
  await expect(page.getByText('쿨러테스트몰')).toBeVisible();
  await expect(page.getByText('ACTIVE')).toHaveCount(0);
  await expect(page.getByText('77.7')).toHaveCount(0);
});

test('opens GPU internal assets from home category link', async ({ page }) => {
  await page.route('**/api/parts**', async (route) => {
    const url = new URL(route.request().url());
    const category = url.searchParams.get('category') ?? '';
    const items = category === 'GPU'
      ? [
          {
            id: 'part-gpu-home-test',
            category: 'GPU',
            name: '홈에서 열린 RTX 테스트',
            manufacturer: 'NVIDIA',
            price: 890000,
            status: 'ACTIVE',
            benchmarkSummary: { score: 92.4 },
            externalOffer: {
              imageUrl: 'https://example.test/home-rtx.png',
              supplierName: '홈테스트몰',
              offerUrl: 'https://example.test/home-rtx',
              lowPrice: 890000,
              source: 'NAVER_SHOPPING_SEARCH'
            }
          }
        ]
      : [];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items,
        page: 0,
        size: 50,
        total: items.length
      })
    });
  });

  await page.goto('/');
  await page.getByRole('link', { name: 'GPU' }).click();

  await expect(page).toHaveURL('/self-quote?category=GPU');
  await expect(page.getByText('GPU 부품 목록')).toBeVisible();
  await expect(page.getByText('홈에서 열린 RTX 테스트')).toBeVisible();
  await expect(page.getByText('홈테스트몰')).toBeVisible();
});

test('paginates self quote assets in 20 item pages', async ({ page }) => {
  const requestedPages: string[] = [];
  const requestedSizes: string[] = [];

  await page.route('**/api/parts**', async (route) => {
    const url = new URL(route.request().url());
    const pageParam = url.searchParams.get('page') ?? '0';
    const sizeParam = url.searchParams.get('size') ?? '';
    requestedPages.push(pageParam);
    requestedSizes.push(sizeParam);

    const currentPage = Number.parseInt(pageParam, 10);
    const start = currentPage * 20;
    const items = Array.from({ length: 20 }, (_, index) => {
      const itemNumber = start + index + 1;
      return {
        id: `part-psu-page-${itemNumber}`,
        category: 'PSU',
        name: `페이징 파워 ${itemNumber}`,
        manufacturer: '테스트파워',
        price: 50000 + itemNumber,
        status: 'ACTIVE',
        attributes: {
          shortSpec: `${itemNumber}번 파워`
        },
        externalOffer: {
          imageUrl: `https://example.test/psu-${itemNumber}.png`,
          supplierName: '페이징몰',
          offerUrl: `https://example.test/psu-${itemNumber}`,
          lowPrice: 50000 + itemNumber,
          source: 'NAVER_SHOPPING_SEARCH'
        }
      };
    });

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items,
        page: currentPage,
        size: 20,
        total: 45
      })
    });
  });

  await page.goto('/self-quote?category=PSU');
  await expect(page.getByText('45개 중 1-20개 표시')).toBeVisible();
  await expect(page.getByText('페이지 1 / 3')).toBeVisible();
  await expect(page.getByText('페이징 파워 1', { exact: true })).toBeVisible();
  expect(requestedPages).toContain('0');
  expect(requestedSizes).toContain('20');

  await page.getByRole('button', { name: '다음' }).click();

  await expect(page).toHaveURL('/self-quote?category=PSU&page=1');
  await expect(page.getByText('45개 중 21-40개 표시')).toBeVisible();
  await expect(page.getByText('페이지 2 / 3')).toBeVisible();
  await expect(page.getByText('페이징 파워 21', { exact: true })).toBeVisible();
  expect(requestedPages).toContain('1');
});
