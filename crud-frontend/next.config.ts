import type { NextConfig } from 'next';

/**
 * Next.js configuration.
 *
 * Rewrites proxy /api/* and /health/* to the Spring Boot backend,
 * mirroring the behaviour of the old Vite proxy config.
 * This means the browser always sees same-origin requests — no CORS needed.
 */
const nextConfig: NextConfig = {
  // Standalone output generates a minimal self-contained server.js for Docker.
  // This eliminates the need to copy node_modules into the production image.
  output: 'standalone',

  // Server-side rewrites (handled by Next.js Node server, not the browser)
  async rewrites() {
    // In Docker: NEXT_PUBLIC_API_URL=http://crud-api:8080
    // Locally:   falls back to http://localhost:8080
    const apiBase = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const minioBase = process.env.NEXT_PUBLIC_MINIO_URL ?? 'http://localhost:9000';

    return [
      {
        source: '/api/:path*',
        destination: `${apiBase}/api/:path*`,
      },
      {
        source: '/health/:path*',
        destination: `${apiBase}/health/:path*`,
      },
      {
        source: '/minio-assets/:path*',
        destination: `${minioBase}/frontend-assets/:path*`,
      },
    ];
  },
};

export default nextConfig;
