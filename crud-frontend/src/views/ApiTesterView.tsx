import React, { useState } from 'react';
import { useI18n } from '../providers/I18nProvider';

export const ApiTesterView: React.FC = () => {
  const { t } = useI18n();
  const [response, setResponse] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const handleTestPublic = async () => {
    setLoading(true);
    try {
      // Relative URL — Vite proxy forwards this to http://127.0.0.1:8080/api/translations
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
      // Relative URL — Vite proxy forwards this to http://127.0.0.1:8080/api/products
      // The Authorization header is sent through the proxy to the backend.
      const token = import.meta.env.VITE_API_TOKEN;
      const headers: HeadersInit = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const res = await fetch('/api/products', { headers });
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

      <div className="flex gap-4">
        <button 
          onClick={handleTestPublic}
          disabled={loading}
          className="px-6 py-2 rounded bg-emerald-600 hover:bg-emerald-500 font-medium transition-colors disabled:opacity-50"
        >
          {loading ? 'Loading...' : 'Test Public API (Translations)'}
        </button>
        <button 
          onClick={handleTestSecure}
          disabled={loading}
          className="px-6 py-2 rounded bg-purple-600 hover:bg-purple-500 font-medium transition-colors disabled:opacity-50"
        >
          {loading ? 'Loading...' : 'Test Secure API (Products)'}
        </button>
      </div>

      <div className="mt-6">
        <h3 className="text-xl font-semibold mb-2">Response:</h3>
        <pre className="bg-slate-900 p-4 rounded-lg overflow-x-auto text-green-400 border border-slate-700 min-h-[150px]">
          {response || 'Awaiting request...'}
        </pre>
      </div>
    </div>
  );
};
