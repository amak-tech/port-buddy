import React from 'react';

interface SeoProps {
  title: string;
  description: string;
  path?: string;
  keywords?: string;
  schema?: object;
}

export default function Seo({
  title,
  description,
  path = '',
  keywords,
  schema
}: SeoProps) {
  const baseUrl = import.meta.env.VITE_CANONICAL || '';
  const fullUrl = `${baseUrl}${path}`;

  React.useEffect(() => {
    document.title = title;

    const updateMeta = (name: string, content: string, attr = 'name') => {
        const el = document.querySelector(`meta[${attr}="${name}"]`);
        if (el) el.setAttribute('content', content);
    };

    updateMeta('description', description);
    if (keywords) updateMeta('keywords', keywords);

    updateMeta('og:title', title, 'property');
    updateMeta('og:description', description, 'property');
    updateMeta('og:url', fullUrl, 'property');

    updateMeta('twitter:title', title, 'name');
    updateMeta('twitter:description', description, 'name');

    const canonical = document.querySelector('link[rel="canonical"]');
    if (canonical) {
      canonical.setAttribute('href', fullUrl);
    }

    const schemaTag = document.querySelector('script[type="application/ld+json"]');
    if (schemaTag && schema) {
      schemaTag.textContent = JSON.stringify(schema);
    }

  }, [title, description, keywords, fullUrl, schema]);

  return null;
}
