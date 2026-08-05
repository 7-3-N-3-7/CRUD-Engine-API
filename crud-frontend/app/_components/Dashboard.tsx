'use client';

/**
 * Dashboard — the full SPA shell, now running as a Client Component inside
 * the Next.js App Router. All the interactivity (tab switching, auth) lives here.
 * Translations were already resolved server-side and injected via I18nProvider.
 */
import React, { useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { useI18n } from '../../src/providers/I18nProvider';
import { ArchitectureView } from '../../src/views/ArchitectureView';
import { ApiTesterView } from '../../src/views/ApiTesterView';
import { MinioIcon } from '../../src/components/MinioIcon';

export const Dashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'architecture' | 'apiTester'>('architecture');
  const { t } = useI18n();
  const auth = useAuth();

  const handleLogin = () => auth.signinRedirect();
  const handleLogout = () => auth.signoutRedirect();

  if (auth.isLoading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center">
        <div className="text-slate-400 animate-pulse text-xl">Initialising…</div>
      </div>
    );
  }

  return (
    <div className="h-full w-full bg-slate-900 text-slate-100 flex flex-col md:flex-row overflow-hidden">
      {/* Sidebar */}
      <aside className="w-full md:w-64 glass-panel border-r border-slate-800 m-4 rounded-xl flex flex-col">
        <div className="flex items-center gap-3 p-4 border-b border-slate-700">
          <MinioIcon name="logo" className="w-8 h-8 text-blue-400" />
          <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-emerald-400">
            CRUD Engine
          </h1>
        </div>
        <nav className="flex-1 p-4 space-y-2">
          <button
            onClick={() => setActiveTab('architecture')}
            className={`w-full text-left px-4 py-2 rounded-lg transition-colors flex items-center gap-3 ${
              activeTab === 'architecture'
                ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30'
                : 'hover:bg-slate-800 text-slate-400'
            }`}
          >
            <MinioIcon name="architecture" className="w-5 h-5" />
            {t('nav.architecture')}
          </button>
          <button
            onClick={() => setActiveTab('apiTester')}
            className={`w-full text-left px-4 py-2 rounded-lg transition-colors flex items-center gap-3 ${
              activeTab === 'apiTester'
                ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30'
                : 'hover:bg-slate-800 text-slate-400'
            }`}
          >
            <MinioIcon name="api" className="w-5 h-5" />
            {t('nav.apitester')}
          </button>
        </nav>

        {/* Auth section */}
        <div className="p-4 border-t border-slate-700">
          {auth.isAuthenticated ? (
            <div className="space-y-2">
              <div className="flex items-center gap-2 px-2">
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-emerald-500 flex items-center justify-center text-sm font-bold text-white flex-shrink-0">
                  {auth.user?.profile.preferred_username?.[0]?.toUpperCase() ?? '?'}
                </div>
                <div className="overflow-hidden">
                  <p className="text-sm font-medium text-slate-200 truncate">
                    {auth.user?.profile.preferred_username}
                  </p>
                  <p className="text-xs text-slate-500 truncate">
                    {(auth.user?.profile.realm_access as any)?.roles
                      ?.filter(
                        (r: string) =>
                          !['default-roles-crud-realm', 'offline_access', 'uma_authorization'].includes(r),
                      )
                      .join(', ')}
                  </p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="w-full text-left px-4 py-2 rounded-lg text-sm text-red-400 hover:bg-red-900/20 transition-colors"
              >
                ↩ Sign out
              </button>
            </div>
          ) : (
            <button
              onClick={handleLogin}
              className="w-full px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-medium text-sm transition-colors"
            >
              🔑 Sign in with Keycloak
            </button>
          )}
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-4 md:p-8 overflow-y-auto">
        <header className="mb-8 flex justify-between items-center glass-panel py-4 px-6 rounded-xl">
          <h2 className="text-2xl font-semibold text-slate-200">
            {activeTab === 'architecture' ? t('architecture.title') : t('api.tester.title')}
          </h2>
          <div className="flex items-center gap-4">
            {auth.isAuthenticated ? (
              <span className="text-xs px-3 py-1 rounded-full bg-emerald-900/40 text-emerald-400 border border-emerald-500/30">
                ● Authenticated
              </span>
            ) : (
              <span className="text-xs px-3 py-1 rounded-full bg-slate-800 text-slate-500 border border-slate-700">
                ○ Not signed in
              </span>
            )}
          </div>
        </header>

        <div className="transition-all duration-300">
          {activeTab === 'architecture' && <ArchitectureView />}
          {activeTab === 'apiTester' && <ApiTesterView />}
        </div>
      </main>
    </div>
  );
};
