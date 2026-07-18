/**
 * Global TypeScript declarations for non-TypeScript file types.
 * Next.js auto-generates next-env.d.ts for its own types, but CSS
 * side-effect imports still need an explicit declaration.
 */

// Allow importing CSS files as side effects (e.g. `import './globals.css'`)
declare module '*.css';

// Allow importing SVG files as React components or raw strings
declare module '*.svg' {
  const content: string;
  export default content;
}
