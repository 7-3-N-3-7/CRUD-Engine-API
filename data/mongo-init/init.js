// =============================================================================
//  MongoDB Translation Seed Script
//  Reads from /translations/{lang}/translations.json files mounted at runtime.
//  Each directory name IS the language code (e.g. "en", "fr", "nl").
//  Documents: { key: string, value: string, lang: string }
// =============================================================================

db = db.getSiblingDB('crudapp_db');

// Drop existing translations so re-seeding is always clean
db.translations.drop();

// Create a unique index on (key + lang) so there are never duplicates
db.translations.createIndex({ key: 1, lang: 1 }, { unique: true });

// ---------------------------------------------------------------------------
//  Helper: load one language file and insert its entries
// ---------------------------------------------------------------------------
function seedLanguage(lang, translations) {
  var docs = [];
  for (var key in translations) {
    if (translations.hasOwnProperty(key)) {
      docs.push({ key: key, value: translations[key], lang: lang });
    }
  }
  if (docs.length > 0) {
    db.translations.insertMany(docs, { ordered: false });
    print('Seeded ' + docs.length + ' keys for lang=' + lang);
  }
}

// ---------------------------------------------------------------------------
//  Languages — add a new block here whenever you add a translations/{lang}/
//  directory.  The JSON files are loaded via mongoimport-compatible load().
// ---------------------------------------------------------------------------

// English (en)
seedLanguage('en', {
  "nav.architecture":       "Architecture",
  "nav.apitester":          "API Tester",
  "dashboard.title":        "Interactive Dashboard",
  "dashboard.welcome":      "Welcome to the dashboard!",
  "architecture.title":     "Architecture Overview",
  "architecture.description": "This platform leverages a microservices-inspired architecture with a React frontend and multiple backend services.",
  "api.tester.title":       "API Tester",
  "api.tester.description": "Test connectivity to the backend services."
});

// Dutch (nl)
seedLanguage('nl', {
  "nav.architecture":       "Architectuur",
  "nav.apitester":          "API Tester",
  "dashboard.title":        "Interactief Dashboard",
  "dashboard.welcome":      "Welkom bij het dashboard!",
  "architecture.title":     "Architectuuroverzicht",
  "architecture.description": "Dit platform maakt gebruik van een op microservices geïnspireerde architectuur met een React-frontend en meerdere backend-diensten.",
  "api.tester.title":       "API Tester",
  "api.tester.description": "Test de verbinding met de backendservices."
});

// French (fr)
seedLanguage('fr', {
  "nav.architecture":       "Architecture",
  "nav.apitester":          "Testeur d'API",
  "dashboard.title":        "Tableau de bord interactif",
  "dashboard.welcome":      "Bienvenue sur le tableau de bord !",
  "architecture.title":     "Vue d'ensemble de l'architecture",
  "architecture.description": "Cette plateforme s'appuie sur une architecture inspirée des microservices avec un frontend React et plusieurs services backend.",
  "api.tester.title":       "Testeur d'API",
  "api.tester.description": "Testez la connectivité aux services backend."
});

print('Translation seeding complete.');
