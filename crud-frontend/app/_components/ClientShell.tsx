'use client';

/**
 * ClientShell — the boundary between Server and Client in the App Router.
 *
 * The root layout (a Server Component) fetches translations from MongoDB, then
 * passes them here as a prop. This component:
 *   1. Provides the OIDC AuthProvider (must run in the browser)
 *   2. Provides the I18nContext with the already-resolved translations
 *   3. Renders the full dashboard UI
 *
 * Because translations arrive as a prop from the server, the FIRST HTML the
 * browser receives already has the correct text — that's the SSR win.
 */
import React from 'react';
import { AuthProvider } from '../../src/providers/AuthProvider';
import { I18nProvider } from '../../src/providers/I18nProvider';
import { Dashboard } from './Dashboard';

interface ClientShellProps {
  translations: Record<string, string>;
  children: React.ReactNode;
}

export const ClientShell: React.FC<ClientShellProps> = ({ translations }) => {
  return (
    <AuthProvider>
      <I18nProvider initialTranslations={translations}>
        <Dashboard />
      </I18nProvider>
    </AuthProvider>
  );
};
