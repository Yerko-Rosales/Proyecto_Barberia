-- DEF-008 (CP-73), 10-11-2026: la Política de Privacidad y los Términos y Condiciones v1.0 se publican en
-- https://yerko-rosales.github.io/Proyecto_Barberia/privacidad.html (ambiente UAT) y se registra su huella SHA-256
-- (calculada sobre el archivo publicado; coincide con barberia_bigboys_web/privacidad.html).
UPDATE politica
   SET url = 'https://yerko-rosales.github.io/Proyecto_Barberia/privacidad.html' || CASE WHEN tipo = 'TERMINOS' THEN '#terminos' ELSE '' END,
       hash_sha256 = '3a0ac82e45e339250ca3f70cff24b8de8041923a9128a4509597af94c3c85108',
       publicada_en = '2026-11-10 18:30-03'
 WHERE version = '1.0' AND tipo IN ('PRIVACIDAD', 'TERMINOS');
