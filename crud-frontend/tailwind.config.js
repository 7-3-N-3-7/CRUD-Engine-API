/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class', // Enable dark mode
  theme: {
    extend: {
      colors: {
        'glass-bg': 'rgba(255, 255, 255, 0.05)',
        'glass-border': 'rgba(255, 255, 255, 0.1)',
        'dark-bg': '#0f172a', // slate-900
        'dark-card': '#1e293b', // slate-800
        'dark-text': '#f8fafc', // slate-50
      }
    },
  },
  plugins: [],
}
