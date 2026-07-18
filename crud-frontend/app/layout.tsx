/**
 * Root layout — SERVER COMPONENT.
 *
 * 1. Fetches translations from MongoDB at request time (SSR)
 * 2. Renders the static shell (HTML skeleton, sidebar nav labels, page title)
 *    directly into the HTML — search engines and the browser's first paint
 *    see the real text immediately, not raw translation keys.
 * 3. Passes translations + children to the ClientShell for the interactive
 *    parts (tab switching, auth, API calls).
 */
import type { Metadata } from 'next';
import './globals.css';
import { ClientShell } from './_components/ClientShell';

export const metadata: Metadata = {
  title: 'CRUD Engine Dashboard',
  description:
    'A production-ready CRUD engine with Keycloak authentication, MongoDB translations, MinIO assets and PostgreSQL persistence.',
};

async function getTranslations(lang = 'en'): Promise<Record<string, string>> {
  const apiBase = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

  // Use a 3-second timeout so a starting/slow backend never hangs the SSR
  // render and causes a 500. On timeout or any error we return {} and the
  // page renders with the inline fallback strings — perfectly usable.
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 3000);

  try {
    const res = await fetch(`${apiBase}/api/translations?lang=${lang}`, {
      cache: 'no-store',
      signal: controller.signal,
    });
    if (!res.ok) return {};
    const data: Array<{ key: string; value: string }> = await res.json();
    return Object.fromEntries(data.map((d) => [d.key, d.value]));
  } catch {
    // Backend not yet reachable (ECONNREFUSED, ECONNRESET, timeout, etc.)
    // Return empty map — fallbacks in the JSX will display instead.
    return {};
  } finally {
    clearTimeout(timeout);
  }
}


export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const translations = await getTranslations('en');

  // Helper — resolve a key with a fallback, used inside this server component
  const t = (key: string, fallback: string) => translations[key] || fallback;

  return (
    <html lang="en" className="dark">
      <body>
        {/*
          The outer div and sidebar labels are rendered SERVER-SIDE.
          A search engine crawling this page will see "Architecture" and
          "API Tester" in the HTML — not blank placeholders.

          The ClientShell takes over for everything interactive (auth, tab
          switching, API calls) and receives the same translations map so
          the client never needs to re-fetch them.
        */}
        <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col md:flex-row">

          {/* ── Static sidebar shell (SSR) ─────────────────────────────────── */}
          <aside className="w-full md:w-64 glass-panel border-r border-slate-800 m-4 rounded-xl flex flex-col">
            <div className="flex items-center gap-3 p-4 border-b border-slate-700">
              {/* Logo placeholder — MinioIcon hydrates on client */}
              <div className="w-8 h-8 rounded-full bg-blue-400/20 flex-shrink-0" />
              <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-emerald-400">
                CRUD Engine
              </h1>
            </div>
            <nav className="flex-1 p-4 space-y-2">
              {/* Nav labels are real text in the HTML from the server */}
              <div className="px-4 py-2 text-blue-400 border border-blue-500/30 rounded-lg bg-blue-600/20 text-sm font-medium">
                {t('nav.architecture', 'Architecture')}
              </div>
              <div className="px-4 py-2 text-slate-400 rounded-lg text-sm">
                {t('nav.apitester', 'API Tester')}
              </div>
            </nav>
          </aside>

          {/* ── Static main header shell (SSR) ────────────────────────────── */}
          <main className="flex-1 p-4 md:p-8 overflow-y-auto">
            <header className="mb-8 flex justify-between items-center glass-panel py-4 px-6 rounded-xl">
              <h2 className="text-2xl font-semibold text-slate-200">
                {t('architecture.title', 'Architecture Overview')}
              </h2>
            </header>

            {/*
              ClientShell replaces the static shell above with the fully
              interactive dashboard once JavaScript hydrates.
              The noscript fallback preserves content for crawlers that
              don't execute JS at all.
            */}
            <noscript>
              <p className="text-slate-400">
                JavaScript is required for the interactive dashboard.
              </p>
            </noscript>
          </main>
        </div>

        {/*
          ClientShell renders the real interactive dashboard on top.
          It is positioned to cover the static shell once hydrated.
          We use an absolute overlay approach so the static text is in
          the DOM (for SEO) but the interactive UI takes over visually.
        */}
        <div className="fixed inset-0">
          <ClientShell translations={translations}>{children}</ClientShell>
        </div>
      </body>
    </html>
  );
}
