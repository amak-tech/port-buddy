import React from 'react';
import { Helmet } from 'react-helmet-async';

interface SeoProps {
  title: string;
  description: string;
  path?: string;
  type?: string;
  name?: string;
  keywords?: string;
  schema?: object;
  image?: string;
}

export default function Seo({ 
  title, 
  description, 
  path = '', 
  type = 'website',
  name = 'Port Buddy',
  keywords,
  schema,
  image = '/og-image.png'
}: SeoProps) {
  const baseUrl = import.meta.env.VITE_CANONICAL || '';
  const fullUrl = `${baseUrl}${path}`;
  const fullImageUrl = `${baseUrl}${image}`;

  return (
    <Helmet>
      {/* Standard metadata tags */}
      <title>{title}</title>
      <meta name='description' content={description} />
      {keywords && <meta name='keywords' content={keywords} />}
      <link rel="canonical" href={fullUrl} />

      {/* Open Graph tags */}
      <meta property="og:site_name" content={name} />
      <meta property="og:type" content={type} />
      <meta property="og:title" content={title} />
      <meta property="og:description" content={description} />
      <meta property="og:locale" content="en_US" />
      <meta property="og:url" content={fullUrl} />
      <meta property="og:image" content={fullImageUrl} />
      
      {/* Twitter tags */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={title} />
      <meta name="twitter:description" content={description} />
      <meta name="twitter:creator" content="@anton_liashenka" />
      <meta name="twitter:image" content={fullImageUrl} />

      {/* Structured Data */}
      {schema && (
        <script type="application/ld+json">
          {JSON.stringify(schema)}
        </script>
      )}
    </Helmet>
  );
}
