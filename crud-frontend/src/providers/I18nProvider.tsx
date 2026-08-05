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
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Read lang from URL
    const params = new URLSearchParams(window.location.search);
    const lang = params.get('lang') ?? 'en';

    // If 'en' is requested and we have SSR translations, use them.
    if (lang === 'en' && initialTranslations && Object.keys(initialTranslations).length > 0) {
      setLoading(false);
      return;
    }

    // Otherwise, fetch the requested language
    const fetchTranslations = async () => {
      setLoading(true);
      try {
        const response = await fetch(`/api/translations?lang=${lang}`);
        if (!response.ok) {
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
