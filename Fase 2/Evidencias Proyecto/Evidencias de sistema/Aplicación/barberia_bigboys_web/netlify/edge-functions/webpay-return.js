// Retorno de Webpay Plus: Transbank envía token_ws (pago terminado) o TBK_TOKEN / TBK_ORDEN_COMPRA
// (pago anulado) en un formulario POST. Se redirige con 303 a la misma página por GET para que la web lea
// los parámetros de la URL y confirme el pago como siempre.
export default async (request, context) => {
  if (request.method !== "POST") return context.next();
  try {
    const form = await request.formData();
    const url = new URL(request.url);
    for (const campo of ["token_ws", "TBK_TOKEN", "TBK_ORDEN_COMPRA", "TBK_ID_SESION"]) {
      const valor = form.get(campo);
      if (typeof valor === "string" && valor) url.searchParams.set(campo, valor);
    }
    return Response.redirect(url.toString(), 303);
  } catch {
    return context.next();
  }
};
