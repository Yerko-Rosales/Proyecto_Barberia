// Edge Function "asesor-ia" (19-11-2026): Asesor de estilo con IA (visagismo) de Big Boys.
// Recibe la foto del cliente (base64) y le pide a Gemini un diagnóstico y el corte recomendado,
// cruzado con el catálogo de servicios. La clave de Gemini vive solo aquí (secreto GEMINI_API_KEY),
// nunca en la web ni en la app. La foto no se guarda: viaja a Gemini y se descarta (Ley 21.719).
// Secretos: GEMINI_API_KEY y, opcional, GEMINI_MODEL (por defecto gemini-2.5-flash).

const ORIGENES = [
  'https://barberia-bigboys.netlify.app',
  'https://yerko-rosales.github.io',
  'http://localhost:3000',
  'http://localhost:3123',
];
const MIME_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_BASE64 = 6_000_000; // ~4,5 MB de imagen
const SERVICIOS: Record<string, number> = { 'Corte': 11900, 'Corte + Cejas': 15470, 'Corte + Barba': 20230, 'S. Completo': 23800 };

// Límite simple por IP (por instancia): 10 análisis cada 10 minutos.
const usos = new Map<string, number[]>();
function permitido(ip: string): boolean {
  const ahora = Date.now();
  const recientes = (usos.get(ip) ?? []).filter((t) => ahora - t < 10 * 60_000);
  if (recientes.length >= 10) { usos.set(ip, recientes); return false; }
  recientes.push(ahora);
  usos.set(ip, recientes);
  return true;
}

function cors(origin: string) {
  return {
    'Access-Control-Allow-Origin': ORIGENES.includes(origin) ? origin : ORIGENES[0],
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Vary': 'Origin',
  };
}

const PROMPT = `Actúa como un Maestro Barbero y Especialista en Visagismo Facial de "Big Boys Barberías Chile" (locales en Peñaflor y Talagante).
Analiza la morfología facial del cliente a partir de su fotografía: forma del rostro, mandíbula, frente, textura del cabello y vello facial.
Si la foto está algo borrosa, oscura, girada o en ángulo pero se distingue un rostro humano, analízalo igual con tu mejor estimación.
Solo si en la foto no hay ningún rostro humano, responde {"error": "sin_rostro"}.
Cruza el diagnóstico con el catálogo oficial de servicios de Big Boys:
- "Corte" ($11.900 CLP)
- "Corte + Cejas" ($15.470 CLP)
- "Corte + Barba" ($20.230 CLP)
- "S. Completo" ($23.800 CLP)

Responde SOLO un JSON válido (sin texto adicional ni markdown) con esta estructura exacta:
{
  "morphology": "oval" | "square" | "round" | "diamond" | "long",
  "morphologyLabel": "Ovalado" | "Cuadrado" | "Redondo" | "Diamante" | "Alargado",
  "hairTexture": "ej. Ondulado / Densidad Media",
  "beardType": "ej. Barba corta perfilada",
  "recommendedLook": "Nombre profesional del corte (ej. Mid Skin Fade con Textured Quiff)",
  "anatomicalDiagnosis": "Explicación breve de por qué este corte equilibra su mandíbula, pómulos y frente",
  "officialService": "Corte" | "Corte + Barba" | "S. Completo" | "Corte + Cejas",
  "stylingProduct": "ej. Polvo texturizador mate o pomada fijadora",
  "stylingTips": "Consejo de peinado diario",
  "maintenanceDays": 14 | 18 | 21
}`;

Deno.serve(async (req) => {
  const origin = req.headers.get('origin') ?? '';
  const headers = { ...cors(origin), 'Content-Type': 'application/json' };
  const responder = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers });

  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors(origin) });
  if (req.method !== 'POST') return responder(405, { error: 'Método no permitido.' });

  let body: { imagen?: string; mime?: string; consentimiento?: boolean };
  try { body = await req.json(); } catch { return responder(400, { error: 'Solicitud inválida.' }); }

  const { imagen, mime, consentimiento } = body;
  if (consentimiento !== true) return responder(400, { error: 'Debes aceptar el uso de tu foto para continuar.' });
  if (!mime || !MIME_PERMITIDOS.includes(mime)) return responder(400, { error: 'La foto debe ser JPG, PNG o WebP.' });
  if (!imagen || typeof imagen !== 'string' || imagen.length > MAX_BASE64) {
    return responder(400, { error: 'La foto es demasiado grande (máximo 4 MB).' });
  }

  const ip = (req.headers.get('x-forwarded-for') ?? '').split(',')[0].trim() || 'desconocida';
  if (!permitido(ip)) return responder(429, { error: 'Alcanzaste el límite de análisis. Intenta de nuevo en unos minutos.' });

  const key = Deno.env.get('GEMINI_API_KEY');
  if (!key) return responder(500, { error: 'El asesor de IA aún no está configurado.' });
  const modelo = Deno.env.get('GEMINI_MODEL') ?? 'gemini-2.5-flash';

  // Supabase corta la función a los 150 s: dejamos margen para responder un error claro.
  const inicio = Date.now();
  const LIMITE_MS = 135_000;
  const llamarGemini = async (conPensamientoBajo: boolean): Promise<Response> => {
    const generationConfig: Record<string, unknown> = { responseMimeType: 'application/json', temperature: 0.2 };
    // Razonamiento mínimo: el análisis es simple y así la respuesta baja de ~60 s a ~30 s.
    if (conPensamientoBajo) generationConfig.thinkingConfig = { thinkingLevel: 'minimal' };
    return await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${modelo}:generateContent`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key },
      body: JSON.stringify({
        contents: [{ parts: [{ text: PROMPT }, { inline_data: { mime_type: mime, data: imagen } }] }],
        generationConfig,
      }),
      signal: AbortSignal.timeout(Math.max(5_000, LIMITE_MS - (Date.now() - inicio))),
    });
  };

  let r: Response;
  try {
    r = await llamarGemini(true);
    // Si el modelo no acepta el ajuste de razonamiento, se repite sin él.
    if (r.status === 400) {
      const detalle = await r.clone().text().catch(() => '');
      if (/thinking/i.test(detalle)) r = await llamarGemini(false);
    }
    // Saturación momentánea de Gemini: un reintento si aún queda tiempo.
    if ((r.status === 503 || r.status === 429 || r.status === 500) && Date.now() - inicio < 30_000) {
      console.error('Gemini', r.status, 'reintentando');
      await new Promise((ok) => setTimeout(ok, 2_000));
      r = await llamarGemini(true);
    }
  } catch (e) {
    if (e instanceof DOMException && (e.name === 'TimeoutError' || e.name === 'AbortError')) {
      console.error('Gemini tiempo agotado', Date.now() - inicio, 'ms');
      return responder(504, { error: 'La IA tardó demasiado en responder. Intenta de nuevo en un momento.' });
    }
    return responder(502, { error: 'No pudimos conectar con el servicio de IA. Intenta de nuevo.' });
  }

  if (!r.ok) {
    // Solo el código y el mensaje del proveedor; nunca la imagen.
    const detalle = await r.text().catch(() => '');
    console.error('Gemini', r.status, detalle.slice(0, 500));
    if (r.status === 429 || r.status === 503) return responder(429, { error: 'El servicio de IA está con mucha demanda. Intenta en un minuto.' });
    return responder(502, { error: 'El servicio de IA no pudo analizar la foto.' });
  }

  let analisis: Record<string, unknown>;
  try {
    const data = await r.json();
    analisis = JSON.parse(data?.candidates?.[0]?.content?.parts?.[0]?.text ?? '');
  } catch {
    return responder(502, { error: 'El servicio de IA respondió en un formato inesperado. Intenta de nuevo.' });
  }
  if (analisis.error || !analisis.recommendedLook) {
    return responder(422, { error: 'No detectamos tu rostro con claridad. Usa una foto de frente, con buena luz.' });
  }

  // El servicio y el precio siempre salen del catálogo, no de la IA.
  const servicio = typeof analisis.officialService === 'string' && SERVICIOS[analisis.officialService] ? analisis.officialService : 'Corte';
  return responder(200, { ...analisis, officialService: servicio, servicePrice: SERVICIOS[servicio], modelo });
});
