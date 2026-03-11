/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        acc:    '#e94560',
        acc2:   '#7c3aed',
        acc3:   '#06b6d4',
        success:'#10b981',
        warn:   '#f59e0b',
        bg0:    '#0a0a14',
        bg1:    '#0f0f1e',
        bg2:    '#14142a',
        bg3:    '#1a1a36',
        line:   '#22224a',
        text:   '#e8e8f8',
        muted:  '#6b6b9a',
      },
      fontFamily: {
        sans: ['Inter', 'Helvetica', 'Arial', 'sans-serif'],
        mono: ['JetBrains Mono', 'Courier New', 'monospace'],
      },
      animation: {
        'pulse-ring': 'pulse-ring 2s ease-in-out infinite',
        'fade-up':    'fade-up 0.4s ease-out',
        'spin-slow':  'spin 3s linear infinite',
      },
      keyframes: {
        'pulse-ring': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(233,69,96,0.3)' },
          '50%':       { boxShadow: '0 0 0 16px rgba(233,69,96,0)' },
        },
        'fade-up': {
          from: { opacity: 0, transform: 'translateY(12px)' },
          to:   { opacity: 1, transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}
