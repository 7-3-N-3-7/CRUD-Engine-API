import React, { useState } from 'react';
import { I18nProvider, useI18n } from './providers/I18nProvider';
import { ArchitectureView } from './views/ArchitectureView';
import { ApiTesterView } from './views/ApiTesterView';
import { MinioIcon } from './components/MinioIcon';
import './index.css';

const DashboardContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'architecture' | 'apiTester'>('architecture');
  const { t } = useI18n();

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col md:flex-row">
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
              activeTab === 'architecture' ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30' : 'hover:bg-slate-800 text-slate-400'
            }`}
          >
            <MinioIcon name="architecture" className="w-5 h-5" />
            {t('nav.architecture') || 'Architecture'}
          </button>
          <button
            onClick={() => setActiveTab('apiTester')}
            className={`w-full text-left px-4 py-2 rounded-lg transition-colors flex items-center gap-3 ${
              activeTab === 'apiTester' ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30' : 'hover:bg-slate-800 text-slate-400'
            }`}
          >
            <MinioIcon name="api" className="w-5 h-5" />
            {t('nav.apitester') || 'API Tester'}
          </button>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-4 md:p-8 overflow-y-auto">
        <header className="mb-8 flex justify-between items-center glass-panel py-4 px-6 rounded-xl">
          <h2 className="text-2xl font-semibold text-slate-200">
            {activeTab === 'architecture' ? 'Architecture Overview' : 'API Tester Dashboard'}
          </h2>
          <div className="flex items-center gap-4">
             <div className="w-8 h-8 rounded-full bg-slate-700 border border-slate-600"></div>
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

const App: React.FC = () => {
  return (
    <I18nProvider>
      <DashboardContent />
    </I18nProvider>
  );
};

export default App;
