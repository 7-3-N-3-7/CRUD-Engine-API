'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';

interface Translations {
  [key: string]: string;
}

interface I18nContextType {
  t: (key: string) => string;
  loading: boolean;
  error: string | null;
}

const I18nContext = createContext<I18nContextType>({
  t: (key) => key,
  loading: false,
  error: null,
});

export const useI18n = () => useContext(I18nContext);

interface I18nProviderProps {
  children: React.ReactNode;
  /**
   * Pre-fetched translations supplied by the Server Component (layout.tsx).
   * When provided, no client-side fetch is made on first render — the page
   * is already translated in the initial HTML (SSR).
   * Falls back to a client-side fetch if this prop is empty or undefined
   * (e.g. when the provider is used outside the Next.js app directory).
   */
  initialTranslations?: Translations;
}

export const I18nProvider: React.FC<I18nProviderProps> = ({
  children,
  initialTranslations,
}) => {
  const [translations, setTranslations] = useState<Translations>(
    initialTranslations ?? {},
  );
  const [loading, setLoading] = useState(!initialTranslations || Object.keys(initialTranslations).length === 0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // If we already have translations from SSR, skip the client-side fetch.
    if (initialTranslations && Object.keys(initialTranslations).length > 0) {
      setLoading(false);
      return;
    }

    // Fallback: client-side fetch (used when running the old Vite SPA or in tests).
    const fetchTranslations = async () => {
      try {
        // /api/translations is now handled by our Next.js API route handler
        // which never returns 500 — it returns [] when the backend is down.
        const response = await fetch('/api/translations');
        if (!response.ok) {
          // Graceful fallback — will show hardcoded English strings
          setLoading(false);
          return;
        }
        const data = await response.json();
        const trans: Translations = {};
        if (Array.isArray(data)) {
          data.forEach((item: { key: string; value: string }) => {
            trans[item.key] = item.value;
          });
        } else {
          Object.assign(trans, data.translations ?? data);
        }
        setTranslations(trans);
      } catch (err: any) {
        // Network completely unavailable — fail silently, show fallbacks
        setError(err.message ?? 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    fetchTranslations();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const t = (key: string): string => translations[key] ?? key;

  return (
    <I18nContext.Provider value={{ t, loading, error }}>
      {children}
    </I18nContext.Provider>
  );
};
