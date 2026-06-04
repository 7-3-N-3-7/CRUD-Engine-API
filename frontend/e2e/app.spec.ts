import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Base URL for the Spring Boot backend (must be running). */
const BACKEND = 'http://localhost:8080';

/** RSA private key in JWK format — matches the test public key in application.properties. */
const TEST_PRIVATE_KEY_JWK = {
  kty: 'RSA',
  n: 'tzBhHDmFE0O1nv4B2ZWnVmuvswS9Vrzz2swgQ49smMgTdCi-q_8Ka-bH99AiQab78dM1vk0Q8ot2_Ha-mgBeG2G0CTFDMLySjVxGiBcFI-Z-4t5_nPlFVTsjz5Sjbt2piFCEZqZzEEybb6nbLCXdzokCAvURPBwCAwSr1iLzFTxiHphPUYvG4mdQEJgvJSVbECP9YvXLJwHzvARsh3hWVE2LxTsa2xIZa2cukqi05S0G4oQxbFJHlDwoh5FyzHctFnArzVDESHaEeSOeI_oUNLyZHwQxxD1SRuZ1XsYr-hziEciv4Le5aOWs7At4bHh6vz25HX__fpDMUkB8c7Xcdw',
  e: 'AQAB',
  d: 'FbtTieROTn7AfmARGsg6Kx0TTDV1ELknD1Rv72kwWjTsueGrh6lLNIm9kenLETwctLojAgm6ckuWjf9i6noTWpmN9hk1_gN2OfbcbICZR4JXQyq0u4vcFxmw9zXhkuFmSe7jW29w7wS829OsAIzCx3b8GhsLNF-jjXVxvTGK4istXnjPq7yoUeEAnsrlYUzuQDInOMmbDhW3DNcrK_RltaTgA8Ga3dQo0ev94V93bnqv7oTegHLcwI_5pdPPt-PR_0CGmsK8e9Ew6CVg23W4i6D8IJNrLP90PugORKjP5NNuRojSe8K5uCOkxClq4z16UHzP9StWazH0RSYBxhv9IQ',
  p: '7qL1rioac5lsKwuo4DBCAzp8AG7TkMqGz4H8jDmE5B125PGmiluqYpnni12MzS0uPT6w8O3BlLydSp1YdvlLWzq8hgz7_7UnojjPK_if9zFN3RXclInSD_Eu2sUuXbAFkDG6gjdp3ZaqeLgqXhbkXPSg-VI724j0-j02SGepieU',
  q: 'xISbiNBIq0WDVoyGJ4ZlLguk2KL5I8Lhyz0H63DeRhyswdmhKodtd09BNq1yizHB0H43toHE52nWo5iO8Ha53133rikKSfURyl4nGtFw5iI_BQoXd0g0sLe-SE07SIzNapJrki4_UmW-y86cP_tNKVE0YF7vo-XhB6E8YFd1tys',
  dp: 'SjdILRhPDbCjYWfI56Bah2KC-id9iMRT1OlaP8oLuF4pgd5dqx4DCZNP3ZoEljL89HMw2F05HSbjzDbPMoEpnH_R7ebP4KDYaK0-UTCLn3cn_iA0b8XFHMwnhEZauyxpLoUouiK9u_qFnfG4y3ZXI0m5XpDiqM4ZUlIDNdV3drk',
  dq: 'eKFB7CCWivPXpDgMXZTE5Rfmr8iSkF4fRieHhgG5n2YYscHKiZWqH1O6HzsnFcSMSVRBFLnhyX-RbsjF7VujyzYeRH0SwMU7j3JuJKst10ZsUsaYEvNyzItttWobGvS7X1DT0V6sJgMotGh2R1wWSGd9dC6ygXQpxwo1SppFOxM',
  qi: 'x0h5WWp8TgBW1jY142PnpNQvquLhiznwx73kjtzfA3VqTg3e6bXlPuVInm2AGMvvhqRp7vab7rrq3ixWUWTEvpq4py0WFElLwVtmIrC9UpX0J8Q2YyvhJuZH1TZVWJYSDz3UTQRSKZxOaXENAD0qYSIL1AFraKYqRJi81vI8TUc',
};

/**
 * Generate a signed RS256 JWT token usable against the test backend.
 * Uses the Web Crypto API available inside Playwright's browser context.
 *
 * IMPORTANT: The page MUST have navigated to a real URL (not about:blank)
 * before calling this, because crypto.subtle requires a secure context.
 */
async function mintJwt(
  page: import('@playwright/test').Page,
  username = 'admin-user',
  roles: string[] = ['ADMIN'],
  tenant = 'tenant-e2e',
): Promise<string> {
  return page.evaluate(
    async ({ privateKey, username, roles, tenant }) => {
      const header = { alg: 'RS256', typ: 'JWT', kid: 'test-key-id' };
      const payload = {
        sub: username,
        preferred_username: username,
        tenant,
        realm_access: { roles },
        iss: 'http://localhost:8081/realms/crud-realm',
        exp: Math.floor(Date.now() / 1000) + 3600,
      };

      const b64url = (arr: Uint8Array) =>
        btoa(String.fromCharCode(...arr))
          .replace(/=/g, '')
          .replace(/\+/g, '-')
          .replace(/\//g, '_');

      const enc = new TextEncoder();
      const headerStr = b64url(enc.encode(JSON.stringify(header)));
      const payloadStr = b64url(enc.encode(JSON.stringify(payload)));
      const input = `${headerStr}.${payloadStr}`;

      const key = await crypto.subtle.importKey(
        'jwk',
        privateKey,
        { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
        false,
        ['sign'],
      );

      const sig = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, enc.encode(input));
      return `${input}.${b64url(new Uint8Array(sig))}`;
    },
    { privateKey: TEST_PRIVATE_KEY_JWK, username, roles, tenant },
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe('Frontend loads correctly', () => {
  test('page renders with expected title', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Dynamic CRUD/i);
  });

  test('architecture diagram renders all nodes', async ({ page }) => {
    await page.goto('/');
    const nodes = page.locator('.arch-node');
    await expect(nodes).not.toHaveCount(0);
    // Verify at least the first and last nodes
    await expect(nodes.first()).toContainText('Client Webapp');
    await expect(nodes.last()).toContainText('RLS Database');
  });

  test('header badge shows WEBFLUX version', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.badge-version')).toContainText('WEBFLUX');
  });
});

test.describe('Backend connectivity', () => {
  test('metadata is fetched from live backend (no mock fallback)', async ({ page }) => {
    await page.goto('/');

    // Wait for metadata to load — the mock fallback banner should NOT appear
    // Give the app time to attempt the fetch and render
    await page.waitForTimeout(3000);

    // The mock fallback warning should not be visible
    const mockBanner = page.locator('text=Connection Failed: Running Mock Fallback');
    await expect(mockBanner).toHaveCount(0);

    // The resource tabs should be visible with real data
    const tabs = page.locator('.resource-tabs .tab');
    await expect(tabs).not.toHaveCount(0);
    await expect(tabs.first()).toContainText('PRODUCTS');
  });

  test('backend health endpoint returns UP', async ({ request }) => {
    const response = await request.get(`${BACKEND}/health/liveness`);
    expect(response.status()).toBe(200);
    expect(await response.text()).toBe('UP');
  });

  test('backend metadata endpoint returns JSON', async ({ request }) => {
    const response = await request.get(`${BACKEND}/api/metadata`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty('products');
    expect(body.products).toHaveProperty('basePath', 'products');
  });
});

test.describe('CRUD operations via backend API', () => {
  let createdId: number;

  test('POST creates a product', async ({ page, request }) => {
    // Navigate to a real page so crypto.subtle is available (secure context)
    await page.goto('/');
    const token = await mintJwt(page);

    const response = await request.post(`${BACKEND}/api/v1/products`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      data: {
        name: 'E2E Test Widget',
        description: 'Created by Playwright E2E test',
        price: 42.99,
        attributes: { origin: 'e2e' },
      },
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.name).toBe('E2E TEST WIDGET'); // ProductInterceptor uppercases
    expect(body.tenantId).toBe('tenant-e2e');
    createdId = body.id;
  });

  test('GET ALL returns the created product', async ({ page, request }) => {
    await page.goto('/');
    const token = await mintJwt(page);

    const response = await request.get(`${BACKEND}/api/v1/products?size=100`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(response.status()).toBe(200);
    const body = await response.text();
    expect(body).toContain('E2E TEST WIDGET');
  });

  test('GET by ID returns the created product', async ({ page, request }) => {
    test.skip(!createdId, 'No product was created — skipping');
    await page.goto('/');
    const token = await mintJwt(page);

    const response = await request.get(`${BACKEND}/api/v1/products/${createdId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.id).toBe(createdId);
  });

  test('PUT updates the product', async ({ page, request }) => {
    test.skip(!createdId, 'No product was created — skipping');
    await page.goto('/');
    const token = await mintJwt(page);

    const response = await request.put(`${BACKEND}/api/v1/products/${createdId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      data: {
        name: 'Updated Widget',
        description: 'Updated by E2E',
        price: 99.99,
      },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.name).toBe('UPDATED WIDGET');
    expect(body.price).toBe(99.99);
  });

  test('DELETE removes the product', async ({ page, request }) => {
    test.skip(!createdId, 'No product was created — skipping');
    await page.goto('/');
    const token = await mintJwt(page);

    const delResponse = await request.delete(`${BACKEND}/api/v1/products/${createdId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(delResponse.status()).toBe(204);

    // Confirm it's gone
    const getResponse = await request.get(`${BACKEND}/api/v1/products/${createdId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(getResponse.status()).toBe(404);
  });
});

test.describe('Security enforcement', () => {
  test('401 when no auth token is provided', async ({ request }) => {
    const response = await request.get(`${BACKEND}/api/v1/products`, {
      failOnStatusCode: false,
    });
    expect(response.status()).toBe(401);
  });

  test('403 when GUEST role tries to access products', async ({ page, request }) => {
    await page.goto('/');
    const token = await mintJwt(page, 'guest-user', ['GUEST']);

    const response = await request.get(`${BACKEND}/api/v1/products`, {
      headers: { Authorization: `Bearer ${token}` },
      failOnStatusCode: false,
    });
    expect(response.status()).toBe(403);
  });

  test('400 for invalid payload with unknown fields', async ({ page, request }) => {
    await page.goto('/');
    const token = await mintJwt(page);

    const response = await request.post(`${BACKEND}/api/v1/products`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      data: {
        name: 'Bad Product',
        description: 'Has unknown fields',
        price: 10.0,
        hackerField: 'should-be-rejected',
      },
      failOnStatusCode: false,
    });
    expect(response.status()).toBe(400);
  });

  test('400 for validation errors (negative price)', async ({ page, request }) => {
    await page.goto('/');
    const token = await mintJwt(page);

    const response = await request.post(`${BACKEND}/api/v1/products`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      data: {
        name: '',
        description: '',
        price: -10.0,
      },
      failOnStatusCode: false,
    });
    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.status).toBe(400);
    expect(body.message).toBe('Validation failed');
  });
});

test.describe('Multi-tenant isolation', () => {
  test('tenant-B cannot see tenant-A products', async ({ page, request }) => {
    // Navigate first so crypto.subtle is available
    await page.goto('/');

    // Create product as tenant-A
    const tokenA = await mintJwt(page, 'user-a', ['ADMIN'], 'tenant-alpha');
    const createRes = await request.post(`${BACKEND}/api/v1/products`, {
      headers: {
        Authorization: `Bearer ${tokenA}`,
        'Content-Type': 'application/json',
      },
      data: {
        name: 'Alpha Secret',
        description: 'Visible only to tenant-alpha',
        price: 1.0,
      },
    });
    expect(createRes.status()).toBe(201);

    // Attempt to list products as tenant-B
    const tokenB = await mintJwt(page, 'user-b', ['ADMIN'], 'tenant-beta');
    const listRes = await request.get(`${BACKEND}/api/v1/products?size=100`, {
      headers: { Authorization: `Bearer ${tokenB}` },
    });
    expect(listRes.status()).toBe(200);
    const body = await listRes.text();
    expect(body).not.toContain('ALPHA SECRET');
  });
});
