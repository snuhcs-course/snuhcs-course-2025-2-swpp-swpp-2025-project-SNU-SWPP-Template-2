/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./App.{js,jsx,ts,tsx}",
    "./app/**/*.{js,jsx,ts,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        "primary": "#f66c51",
        "background-light": "#f8f6f5",
        "background-dark": "#221310",
      },
      fontFamily: {
        "display": ["PlusJakartaSans-Regular", "System"],
        "display-bold": ["PlusJakartaSans-Bold", "System"],
      },
      borderRadius: {
        "DEFAULT": "0.5rem",
        "lg": "1rem",
        "xl": "1.5rem",
        "full": "9999px"
      },
    },
  },
  plugins: [],
}

