/**
 * API Route: ALL /api/proxy/[...path]
 *
 * Generic passthrough proxy to the Spring Boot backend.
 * Forwards GET, POST, PUT, DELETE with headers and body intact.
 * Returns 503 (not 500) when the backend is unavailable, so the
 * client can distinguish "service down" from "internal error".
 *
 * Usage in the frontend: fetch('/api/proxy/products')
 *   → forwards to http://crud-api:8080/api/products
 */
import { NextRequest, NextResponse } from 'next/server';

const SUPPORTED_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

async function handler(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const { path } = await params;
  const apiBase = process.env.API_URL ?? 'http://localhost:8080';
  const targetPath = path.join('/');
  const search = request.nextUrl.search;
  const targetUrl = `${apiBase}/api/${targetPath}${search}`;

  // Forward headers — especially Authorization for secured endpoints
  const forwardHeaders: Record<string, string> = {};
  const auth = request.headers.get('authorization');
  const contentType = request.headers.get('content-type');
  if (auth) forwardHeaders['authorization'] = auth;
  if (contentType) forwardHeaders['content-type'] = contentType;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10_000);

  try {
    const res = await fetch(targetUrl, {
      method: request.method,
      headers: forwardHeaders,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : await request.text(),
      signal: controller.signal,
    });

    const body = await res.text();
    return new NextResponse(body, {
      status: res.status,
      headers: {
        'Content-Type': res.headers.get('content-type') ?? 'application/json',
      },
    });
  } catch {
    return NextResponse.json(
      { error: 'Backend unavailable. Please try again in a moment.' },
      { status: 503 },
    );
  } finally {
    clearTimeout(timeout);
  }
}

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;
