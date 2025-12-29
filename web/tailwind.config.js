/**** @type {import('tailwindcss').Config} ****/
export default {
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace']
      },
      colors: {
        primary: '#0f172a',
        accent: '#22d3ee',
      },
      keyframes: {
        progress: {
          '0%': { transform: 'translateX(-100%)' },
          '50%': { transform: 'translateX(-30%)' },
          '100%': { transform: 'translateX(0%)' },
        },
      },
      animation: {
        progress: 'progress 2s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
