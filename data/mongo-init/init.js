db = db.getSiblingDB('crudapp_db');
db.translations.insertMany([
  { key: "dashboard.title", value: "Interactive Dashboard", lang: "en" },
  { key: "dashboard.welcome", value: "Welcome to the dashboard!", lang: "en" }
]);
