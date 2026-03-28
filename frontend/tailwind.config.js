/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,ts}'],
  theme: {
    extend: {
      colors: {
        primary: '#5c4033',
        secondary: '#faf6f1',
        'accent-olive': '#8b9d77',
        'accent-wheat': '#d4a373',
        'accent-sand': '#e9e0d4',
        'accent-teal': '#75a191',
      },
      fontFamily: {
        serif: ['"Cormorant Garamond"', 'serif'],
        sans: ['Manrope', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      boxShadow: {
        organic: '0 10px 30px rgba(92, 64, 51, 0.08)',
        'organic-hover': '0 16px 40px rgba(92, 64, 51, 0.12)',
      },
      borderRadius: {
        organic: '2rem',
      },
      backgroundImage: {
        paper:
          "radial-gradient(circle at 1px 1px, rgba(139,157,119,0.08) 1px, transparent 0)",
      },
    },
  },
  plugins: [],
}
