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
  loading: true,
  error: null,
});

export const useI18n = () => useContext(I18nContext);

export const I18nProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [translations, setTranslations] = useState<Translations>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchTranslations = async () => {
      try {
        const response = await fetch('http://127.0.0.1:8080/api/translations', {
          headers: {
            'Authorization': `Bearer ${import.meta.env.VITE_API_TOKEN}`
          }
        });
        if (!response.ok) {
          throw new Error('Failed to fetch translations');
        }
        const data = await response.json();
        // Assume data is { translations: { ... } } or just { ... }
        const trans = data.translations || data;
        setTranslations(trans);
      } catch (err: any) {
        setError(err.message || 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    fetchTranslations();
  }, []);

  const t = (key: string): string => {
    return translations[key] || key;
  };

  return (
    <I18nContext.Provider value={{ t, loading, error }}>
      {children}
    </I18nContext.Provider>
  );
};
