-- 21_biografia_barberos.sql · 12-11-2026
-- SC-33: biografía pública de los barberos, visible en «Ver Perfil» de la web y en «Trayectoria y biografía» de la app.
-- Hasta ahora api_barberos devolvía siempre 'bio' vacío. Textos propuestos por Jonathan Ojeda (12-11-2026).
BEGIN;

ALTER TABLE barbero ADD COLUMN IF NOT EXISTS biografia text
  CHECK (biografia IS NULL OR char_length(biografia) <= 300);
COMMENT ON COLUMN barbero.biografia IS 'Biografía pública del barbero (máx. 300 caracteres); se muestra en el catálogo de la web y la app';

CREATE OR REPLACE FUNCTION public.api_barberos()
 RETURNS jsonb
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  SELECT coalesce(jsonb_agg(jsonb_build_object(
    'id', b.id_barbero, 'location_id', b.id_sucursal, 'name', b.nombre_publico,
    'specialty', coalesce(b.especialidad, ''), 'bio', coalesce(b.biografia, ''),
    'avatar_url', coalesce((SELECT u.foto_url FROM usuario u WHERE u.id_usuario = b.id_usuario), ''),
    'available_hours', coalesce((SELECT string_agg(DISTINCT to_char(h.hora_inicio, 'HH24:MI'), ',' ORDER BY to_char(h.hora_inicio, 'HH24:MI'))
                                 FROM horario_barbero h WHERE h.id_barbero = b.id_barbero), ''),
    'rating', coalesce((SELECT round(avg(calificacion), 1) FROM resena WHERE id_barbero = b.id_barbero AND visible), 5.0),
    'reviews', (SELECT count(*) FROM resena WHERE id_barbero = b.id_barbero AND visible),
    'active', b.activo) ORDER BY b.id_barbero), '[]')
  FROM barbero b;
$function$;

UPDATE barbero SET biografia = 'Especialista chileno en degradados milimétricos, low fade y diseños de autor.' WHERE id_barbero = 1;
UPDATE barbero SET biografia = 'Maestro barbero tradicional con amplia trayectoria en perfilado clásico y toalla caliente.' WHERE id_barbero = 2;
UPDATE barbero SET biografia = 'Creativo urbano en freestyle, decoloración, colorimetría y diseño de cejas y barba.' WHERE id_barbero = 3;
UPDATE barbero SET biografia = 'Experto en barboterapia y afeitados al ras con navaja tradicional en Talagante.' WHERE id_barbero = 4;

COMMIT;
