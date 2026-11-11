-- 20_politica_v1_1.sql · 11-11-2026
-- Política de Privacidad y T&C v1.1: completa la razón social («Barbería Big-Boys») y el RUT (55.555.555-5) del responsable
-- con datos provisorios del ambiente de aceptación. Se publica como versión nueva para que los consentimientos dados a la
-- v1.0 conserven su versión y su huella. La huella SHA-256 se calcula sobre privacidad.html publicado en GitHub Pages.
BEGIN;
UPDATE politica SET vigente = false WHERE tipo IN ('PRIVACIDAD', 'TERMINOS') AND vigente;
INSERT INTO politica (tipo, version, publicada_en, url, hash_sha256, vigente)
SELECT tipo, '1.1', DATE '2026-11-11', url, '3089d68289bd2a64bba0b8cc4a4288a0e10eabf4fb4c901dcfbfd0f930f22dac', true
  FROM politica WHERE version = '1.0' AND tipo IN ('PRIVACIDAD', 'TERMINOS');
COMMIT;
