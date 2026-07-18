/**
 * API Route: GET /api/icons/[name]
 *
 * Proxies SVG icon files from MinIO's `frontend-assets/icons/` bucket.
 * If MinIO is unavailable or the file doesn't exist, returns a minimal
 * inline SVG placeholder — the browser never sees a 500 or broken icon.
 *
 * Example: /api/icons/logo  →  MinIO: frontend-assets/icons/logo.svg
 */
import { NextRequest, NextResponse } from 'next/server';

// Minimal fallback SVG — a neutral grey circle shown when MinIO is down
const FALLBACK_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none">
  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5" opacity="0.3"/>
</svg>`;

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ name: string }> },
) {
  const { name } = await params;
  const minioBase = process.env.NEXT_PUBLIC_MINIO_URL ?? 'http://localhost:9000';
  const iconUrl = `${minioBase}/frontend-assets/icons/${name}.svg`;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 3000);

  try {
    const res = await fetch(iconUrl, { signal: controller.signal });

    if (!res.ok) {
      return new NextResponse(FALLBACK_SVG, {
        status: 200,
        headers: { 'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store' },
      });
    }

    const svg = await res.text();
    return new NextResponse(svg, {
      status: 200,
      headers: {
        'Content-Type': 'image/svg+xml',
        // Cache for 5 min — icons rarely change
        'Cache-Control': 'public, max-age=300',
      },
    });
  } catch {
    // MinIO unreachable — return the fallback circle SVG
    return new NextResponse(FALLBACK_SVG, {
      status: 200,
      headers: { 'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store' },
    });
  } finally {
    clearTimeout(timeout);
  }
}
