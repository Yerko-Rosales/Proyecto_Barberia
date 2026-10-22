/**
 * Utilitarios oficiales de validación y formateo:
 * - Algoritmo chileno oficial Módulo 11 para RUT
 * - Limpieza y formateo con puntos y guion (Ej: 19.342.110-3)
 * - Verificación de correo RFC
 * - Requisitos de seguridad de contraseñas y hash SHA-256
 */

export const ValidationUtils = {
  /**
   * Limpia un RUT chileno eliminando puntos, guiones y espacios.
   */
  cleanChileanRut(rawRut) {
    if (!rawRut) return '';
    return rawRut
      .toString()
      .toUpperCase()
      .replace(/\./g, '')
      .replace(/-/g, '')
      .replace(/\s+/g, '')
      .trim();
  },

  /**
   * Valida un RUT chileno utilizando el algoritmo de Módulo 11.
   * Multiplicadores cíclicos 2, 3, 4, 5, 6, 7 de derecha a izquierda.
   */
  isValidChileanRut(rawRut) {
    const clean = this.cleanChileanRut(rawRut);
    if (clean.length < 8 || clean.length > 9) return false;

    const body = clean.slice(0, -1);
    const dv = clean.slice(-1);

    if (!/^\d+$/.test(body)) return false;
    if (!/^[\dK]$/.test(dv)) return false;

    let sum = 0;
    let multiplier = 2;

    for (let i = body.length - 1; i >= 0; i--) {
      sum += parseInt(body[i], 10) * multiplier;
      multiplier = multiplier >= 7 ? 2 : multiplier + 1;
    }

    const remainder = sum % 11;
    const calculatedDvValue = 11 - remainder;

    let expectedDv = '0';
    if (calculatedDvValue === 11) {
      expectedDv = '0';
    } else if (calculatedDvValue === 10) {
      expectedDv = 'K';
    } else {
      expectedDv = calculatedDvValue.toString();
    }

    return dv === expectedDv;
  },

  /**
   * Formatea un RUT con separadores de miles y guion.
   * Ejemplo: "193421103" -> "19.342.110-3"
   */
  formatChileanRut(rawRut) {
    const clean = this.cleanChileanRut(rawRut);
    if (clean.length < 2) return clean;

    const body = clean.slice(0, -1);
    const dv = clean.slice(-1);

    const formattedBody = new Intl.NumberFormat('es-CL').format(parseInt(body, 10));
    return `${formattedBody}-${dv}`;
  },

  /**
   * Valida estrictamente el formato de un correo electrónico.
   */
  isValidEmail(email) {
    if (!email) return false;
    const clean = email.trim();
    if (clean.length > 254 || clean.includes(' ')) return false;
    const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return emailRegex.test(clean);
  },

  /**
   * Valida requisitos de contraseña:
   * - Mínimo 6 caracteres
   * - Al menos una letra
   * - Al menos un número
   */
  validatePasswordRequirements(password) {
    const errors = [];
    if (!password || password.length < 6) {
      errors.push('Mínimo 6 caracteres');
    }
    if (!/[A-Za-z]/.test(password || '')) {
      errors.push('Debe contener al menos una letra');
    }
    if (!/\d/.test(password || '')) {
      errors.push('Debe contener al menos un número');
    }
    return errors;
  },

  /**
   * Genera hash SHA-256 estándar usando la API Web Crypto
   */
  async hashPassword(password) {
    const cleanPass = (password || '').trim();
    const encoder = new TextEncoder();
    const data = encoder.encode(cleanPass);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  }
};
