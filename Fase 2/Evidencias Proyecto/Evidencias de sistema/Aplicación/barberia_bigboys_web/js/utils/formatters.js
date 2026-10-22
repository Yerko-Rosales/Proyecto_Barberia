/**
 * Formateadores de moneda chilena (CLP) y fechas en español
 */

export const AppFormatters = {
  /**
   * Formatea un valor numérico como pesos chilenos (CLP)
   * Ejemplo: 15000 -> "$15.000 CLP"
   */
  formatClp(amount, withSuffix = true) {
    const num = Math.round(Number(amount) || 0);
    const formatted = new Intl.NumberFormat('es-CL').format(num);
    return withSuffix ? `$${formatted} CLP` : `$${formatted}`;
  },

  /**
   * Retorna la fecha actual en formato "dd-MM-yyyy"
   */
  currentDateString() {
    const now = new Date();
    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();
    return `${day}-${month}-${year}`;
  },

  /**
   * Retorna la hora actual en formato "HH:mm"
   */
  currentTimeString() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  /**
   * Retorna fecha y hora combinadas "dd-MM-yyyy HH:mm"
   */
  currentDateTimeString() {
    return `${this.currentDateString()} ${this.currentTimeString()}`;
  },

  /**
   * Convierte una fecha "dd-MM-yyyy" a formato textual completo en español
   * Ejemplo: "04-09-2026" -> "Viernes, 4 de Septiembre de 2026"
   */
  formatDateToLongSpanish(dateStr) {
    if (!dateStr) return '';
    try {
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        const day = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1;
        const year = parseInt(parts[2], 10);
        const d = new Date(year, month, day);

        const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
        const meses = [
          'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
          'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
        ];

        return `${dias[d.getDay()]}, ${day} de ${meses[month]} de ${year}`;
      }
    } catch (e) {
      console.error(e);
    }
    return dateStr;
  }
};
