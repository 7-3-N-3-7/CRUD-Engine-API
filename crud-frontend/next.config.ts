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
    // NOTE: /api/* and /minio-assets/icons/* are handled by Next.js API
    // Route Handlers (app/api/**) which have proper error handling and never
    // propagate 500s to the browser.
    //
    // Only /health/* uses a raw rewrite since it doesn't need error shielding.
    const apiBase = process.env.API_URL ?? 'http://localhost:8080';

    return [
      {
        // Spring Boot actuator health endpoint — used by monitoring tools
        source: '/health/:path*',
        destination: `${apiBase}/health/:path*`,
      },
    ];
  },
};

export default nextConfig;
