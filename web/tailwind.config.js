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
        primary: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#0b1020',
        },
        accent: {
          50: '#ecfeff',
          100: '#cffafe',
          200: '#a5f3fc',
          300: '#67e8f9',
          400: '#22d3ee',
          500: '#06b6d4',
          600: '#0891b2',
          700: '#0e7490',
          800: '#155e75',
          900: '#164e63',
        },
        jb: {
          purple: '#cc33ff',
          pink: '#ff3399',
          orange: '#ff9933',
          blue: '#33ccff',
        }
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'mesh-gradient': 'radial-gradient(at 0% 0%, rgba(204, 51, 255, 0.15) 0, transparent 50%), radial-gradient(at 50% 0%, rgba(34, 211, 238, 0.15) 0, transparent 50%), radial-gradient(at 100% 0%, rgba(255, 51, 153, 0.15) 0, transparent 50%)',
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
