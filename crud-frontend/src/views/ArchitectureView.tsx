import React from 'react';
import { useI18n } from '../providers/I18nProvider';

export const ArchitectureView: React.FC = () => {
  const { t } = useI18n();

  return (
    <div className="glass-panel text-dark-text animate-fade-in space-y-6">
      <h2 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-emerald-400">
        {t('architecture.title') || 'Architecture Overview'}
      </h2>
      <p className="text-slate-300 text-lg">
        {t('architecture.description') || 'This platform leverages a microservices-inspired architecture with React frontend and multiple backend services.'}
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-8">
        <div className="p-5 rounded-lg bg-slate-800/50 border border-slate-700 hover:border-blue-500 transition-colors">
          <h3 className="text-xl font-semibold text-blue-400 mb-2">Frontend</h3>
          <p className="text-slate-400">React + TypeScript, TailwindCSS</p>
        </div>
        <div className="p-5 rounded-lg bg-slate-800/50 border border-slate-700 hover:border-emerald-500 transition-colors">
          <h3 className="text-xl font-semibold text-emerald-400 mb-2">API Gateway</h3>
          <p className="text-slate-400">Spring Cloud Gateway</p>
        </div>
        <div className="p-5 rounded-lg bg-slate-800/50 border border-slate-700 hover:border-purple-500 transition-colors">
          <h3 className="text-xl font-semibold text-purple-400 mb-2">Identity</h3>
          <p className="text-slate-400">Keycloak</p>
        </div>
        <div className="p-5 rounded-lg bg-slate-800/50 border border-slate-700 hover:border-yellow-500 transition-colors">
          <h3 className="text-xl font-semibold text-yellow-400 mb-2">Assets</h3>
          <p className="text-slate-400">MinIO (S3 Compatible)</p>
        </div>
        <div className="p-5 rounded-lg bg-slate-800/50 border border-slate-700 hover:border-red-500 transition-colors">
          <h3 className="text-xl font-semibold text-red-400 mb-2">Translations</h3>
          <p className="text-slate-400">MongoDB</p>
        </div>
        <div className="p-5 rounded-lg bg-slate-800/50 border border-slate-700 hover:border-indigo-500 transition-colors">
          <h3 className="text-xl font-semibold text-indigo-400 mb-2">Products</h3>
          <p className="text-slate-400">PostgreSQL (JPA)</p>
        </div>
      </div>
    </div>
  );
};
