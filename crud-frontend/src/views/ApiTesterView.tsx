'use client';

import React, { useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { useI18n } from '../providers/I18nProvider';

export const ApiTesterView: React.FC = () => {
  const { t } = useI18n();
  const auth = useAuth();
  const [response, setResponse] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const handleTestPublic = async () => {
    setLoading(true);
    try {
      // Relative URL — Vite proxy forwards this to http://localhost:8080/api/translations
      const res = await fetch('/api/translations');
      const data = await res.json();
      setResponse(JSON.stringify(data, null, 2));
    } catch (err: any) {
      setResponse(`Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleTestSecure = async () => {
    setLoading(true);
    try {
      // Grab the live access token from the OIDC context — never expired
      // because react-oidc-context refreshes it automatically.
      const token = auth.user?.access_token;

      if (!token) {
        setResponse('⚠️  Not authenticated. Please sign in first using the "Sign in with Keycloak" button in the sidebar.');
        setLoading(false);
        return;
      }

      // Relative URL — Vite proxy forwards this to http://localhost:8080/api/products
      const res = await fetch('/api/products', {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!res.ok) {
        const body = await res.text();
        setResponse(`Error ${res.status}: ${body || 'Failed to fetch products.'}`);
      } else {
        const data = await res.json();
        setResponse(JSON.stringify(data, null, 2));
      }
    } catch (err: any) {
      setResponse(`Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="glass-panel text-dark-text animate-fade-in space-y-6">
      <h2 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-cyan-400">
        {t('api.tester.title') || 'API Tester'}
      </h2>
      <p className="text-slate-300 text-lg">
        {t('api.tester.description') || 'Test connectivity to the backend services.'}
      </p>

      {/* Auth hint */}
      {!auth.isAuthenticated && (
        <div className="px-4 py-3 rounded-lg bg-amber-900/30 border border-amber-500/30 text-amber-300 text-sm">
          ⚠️ The <strong>Secure API</strong> requires authentication. Sign in via the sidebar first.
        </div>
      )}

      <div className="flex gap-4 flex-wrap">
        <button
          onClick={handleTestPublic}
          disabled={loading}
          className="px-6 py-2 rounded bg-emerald-600 hover:bg-emerald-500 font-medium transition-colors disabled:opacity-50"
        >
          {loading ? 'Loading…' : 'Test Public API (Translations)'}
        </button>
        <button
          onClick={handleTestSecure}
          disabled={loading || !auth.isAuthenticated}
          title={!auth.isAuthenticated ? 'Sign in first' : ''}
          className="px-6 py-2 rounded bg-purple-600 hover:bg-purple-500 font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? 'Loading…' : 'Test Secure API (Products)'}
        </button>
      </div>

      <div className="mt-6">
        <h3 className="text-xl font-semibold mb-2">Response:</h3>
        <pre className="bg-slate-900 p-4 rounded-lg overflow-x-auto text-green-400 border border-slate-700 min-h-[150px]">
          {response || 'Awaiting request…'}
        </pre>
      </div>
    </div>
  );
};
