'use client';

import React, { useEffect, useState } from 'react';

interface MinioIconProps extends React.SVGProps<SVGSVGElement> {
  name: string;
}

export const MinioIcon: React.FC<MinioIconProps> = ({ name, className, ...props }) => {
  const [svgContent, setSvgContent] = useState<string | null>(null);

  useEffect(() => {
    const fetchIcon = async () => {
      try {
        // Uses our Next.js API route which proxies to MinIO and
        // returns a fallback SVG when MinIO is unavailable — never 500s.
        const url = `/api/icons/${name}`;
        const response = await fetch(url);
        if (response.ok) {
          const text = await response.text();
          setSvgContent(text);
        }
      } catch (e) {
        console.error(`Failed to load icon: ${name}`, e);
      }
    };
    fetchIcon();
  }, [name]);

  if (!svgContent) {
    return <div className={`w-6 h-6 animate-pulse bg-slate-700 rounded-full ${className || ''}`} />;
  }

  return (
    <span 
      className={`inline-flex items-center justify-center ${className || ''}`}
      dangerouslySetInnerHTML={{ __html: svgContent }} 
      {...props as any}
    />
  );
};
