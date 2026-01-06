/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f4fa',
          100: '#d9e2f5',
          200: '#b3c6eb',
          300: '#8da9e0',
          400: '#668dd6',
          500: '#4070cc',
          600: '#3257a3',
          700: '#24417a',
          800: '#172b52',
          900: '#091529',
        },
        secondary: {
          50: '#f4f6f8',
          100: '#e3e8ed',
          200: '#c7d1db',
          300: '#aab9c9',
          400: '#8ea2b7',
          500: '#728ba5',
          600: '#5b6f84',
          700: '#445363',
          800: '#2e3842',
          900: '#171c21',
        },
        accent: {
          50: '#effbfa',
          100: '#d4f3f0',
          200: '#a9e7e1',
          300: '#7fdbd2',
          400: '#54cfc3',
          500: '#29c3b4',
          600: '#219c90',
          700: '#19756c',
          800: '#104e48',
          900: '#082724',
        },
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-in': 'slideIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideIn: {
          '0%': { transform: 'translateX(-10px)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
    },
  },
  plugins: [],
};