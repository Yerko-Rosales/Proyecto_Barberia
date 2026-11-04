import react from 'eslint-plugin-react';
import globals from 'globals';

const reglas = {
  'no-unused-vars': ['warn', { vars: 'all', args: 'after-used', caughtErrors: 'none', ignoreRestSiblings: true }],
  'no-unreachable': 'warn',
  'no-useless-catch': 'warn',
  'no-empty': ['warn', { allowEmptyCatch: true }],
  'no-unused-expressions': 'warn',
  'no-dupe-keys': 'warn',
  'no-duplicate-case': 'warn',
  'no-redeclare': 'warn',
  'no-self-assign': 'warn',
  'no-constant-condition': 'warn',
  'no-useless-escape': 'warn',
  'no-undef': 'warn',
};
const reglasReact = {
  ...reglas,
  'react/jsx-uses-vars': 'warn',
  'react/jsx-uses-react': 'warn',
  'react/jsx-key': 'warn',
  'react/no-unknown-property': 'warn',
  'react/jsx-no-duplicate-props': 'warn',
};

export default [
  {
    files: ['app.jsx'],
    plugins: { react },
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'script',
      parserOptions: { ecmaFeatures: { jsx: true } },
      globals: { ...globals.browser, React: 'readonly', ReactDOM: 'readonly', supabase: 'readonly', XLSX: 'readonly', L: 'readonly' },
    },
    settings: { react: { version: '18.2' } },
    rules: reglasReact,
  },
  {
    files: ['mail_server.js'],
    languageOptions: { ecmaVersion: 2022, sourceType: 'commonjs', globals: { ...globals.node } },
    rules: reglas,
  },
];
