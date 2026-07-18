/**
 * API Route: GET /api/translations
 *
 * Proxies to the Spring Boot backend. If the backend is unavailable
 * (still starting, crashed, etc.) this returns an empty array []
 * instead of propagating a 500 — the frontend falls back to its
 * hardcoded English strings and never shows a broken page.
 */
import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const apiBase = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

  // Forward the ?lang= query param if present
  const lang = request.nextUrl.searchParams.get('lang') ?? 'en';

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 4000);

  try {
    const res = await fetch(`${apiBase}/api/translations?lang=${lang}`, {
      cache: 'no-store',
      signal: controller.signal,
      headers: {
        // Forward any Authorization header the client sent
        ...(request.headers.get('authorization')
          ? { authorization: request.headers.get('authorization')! }
          : {}),
      },
    });

    if (!res.ok) {
      // Backend returned an error — return empty array, not 500
      return NextResponse.json([], { status: 200 });
    }

    const data = await res.json();
    return NextResponse.json(data, { status: 200 });
  } catch {
    // Backend unreachable (ECONNREFUSED, timeout, etc.)
    // Always return 200 with empty array so the page renders cleanly.
    return NextResponse.json([], { status: 200 });
  } finally {
    clearTimeout(timeout);
  }
}
