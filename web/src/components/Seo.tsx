import React from 'react';
import { Helmet } from 'react-helmet-async';

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

  return (
    <Helmet>
      {/* Standard metadata tags */}
      <title>{title}</title>
      <meta name='description' content={description} />
      {keywords && <meta name='keywords' content={keywords} />}
      <link rel="canonical" href={fullUrl} />

      {/* Structured Data */}
      {schema && (
        <script type="application/ld+json">
          {JSON.stringify(schema)}
        </script>
      )}
    </Helmet>
  );
}
