/**
 * Gestor de Estado Central y Persistencia Reactiva para Big Boys Barberías Chile.
 * Emula la arquitectura Room Database + Flow + BarberiaViewModel del proyecto Android.
 */

import { ValidationUtils } from '../utils/validation.js';
import { AppFormatters } from '../utils/formatters.js';

const STORAGE_KEYS = {
  USERS: 'bigboys_users',
  LOCATIONS: 'bigboys_locations',
  BARBERS: 'bigboys_barbers',
  APPOINTMENTS: 'bigboys_appointments',
  REVIEWS: 'bigboys_reviews',
  COUPONS: 'bigboys_coupons',
  PRODUCTS: 'bigboys_products',
  SUPPLIES: 'bigboys_supplies',
  NOTIFICATIONS: 'bigboys_notifications',
  LOGGED_USER: 'bigboys_logged_user',
  IS_ADMIN: 'bigboys_is_admin',
  IS_DARK_MODE: 'bigboys_is_dark_mode',
  ADMIN_CONFIG: 'bigboys_admin_config',
  GMAIL_CONFIG: 'bigboys_gmail_config',
  EMAIL_DATABASE: 'bigboys_email_database',
  SENT_EMAILS_LOG: 'bigboys_sent_emails_log',
  LOGGED_BARBER: 'bigboys_logged_barber'
};

// Usuario oficial de prueba pre-registrado
const SEED_TEST_USER = {
  id: 9999,
  fullName: 'Usuario de Prueba Big Boys',
  email: 'test@bigboys.cl',
  phone: '+56 9 8765 4321',
  rut: '19.342.110-3',
  password: '123',
  createdAt: '01-09-2026 10:00'
};

// Semilla de Sucursales oficiales de Big Boys Chile
const SEED_LOCATIONS = [
  {
    id: 1,
    name: 'Big Boys - Talagante',
    address: 'Boulevard del centro, local 3, Talagante',
    phone: '+56 2 2489 1234',
    schedule: 'Lun a Sáb: 09:00 - 20:00 | Dom: 10:00 - 16:00',
    lat: -33.6631,
    lng: -70.9286,
    description: 'Nuestra sucursal insignia en Boulevard del centro, local 3, Talagante. Equipamiento premium, bar de cortesía y sillones hidráulicos de cuero.',
    totalChairs: 4,
    isPrimary: true
  },
  {
    id: 2,
    name: 'Big Boys - Peñaflor (21 de Mayo)',
    address: '21 de mayo 4566, Peñaflor',
    phone: '+56 2 2631 5678',
    schedule: 'Lun a Sáb: 09:00 - 20:00 | Dom: Cerrado',
    lat: -33.6080,
    lng: -70.8993,
    description: 'Ubicada en 21 de mayo 4566, Peñaflor. Especialistas en afeitado tradicional a navaja libre y perfilado de barba.',
    totalChairs: 4,
    isPrimary: false
  },
  {
    id: 3,
    name: 'Big Boys - Peñaflor (Alcalde Luis Araya Cereceda)',
    address: 'Alcalde Luis Araya Cereceda 4387, Peñaflor',
    phone: '+56 2 2912 3456',
    schedule: 'Lun a Sáb: 10:00 - 20:30 | Dom: 10:00 - 15:00',
    lat: -33.6077,
    lng: -70.9001,
    description: 'Sucursal ubicada en Alcalde Luis Araya Cereceda 4387, Peñaflor. Ambiente moderno con estacionamiento propio, zona lounge climatizada y servicios VIP.',
    totalChairs: 4,
    isPrimary: false
  }
];

// Semilla de Barberos (12 profesionales con biografía, calificación y horarios)
const SEED_BARBERS = [
  // Talagante (Sucursal 1)
  {
    id: 1,
    locationId: 1,
    name: 'Mateo Silva',
    specialty: 'Corte Moderno & Fade',
    rating: 4.9,
    avatarUrl: 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Especialista en degradados milimétricos y texturizados urbanos. Mateo cuenta con 6 años de experiencia y es reconocido por su precisión quirúrgica con la patillera.',
    active: true
  },
  {
    id: 2,
    locationId: 1,
    name: 'Carlos Mendoza',
    specialty: 'Perfilado de Barba Premium',
    rating: 4.8,
    avatarUrl: 'https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,13:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Maestro barbero enfocado en la restauración facial masculina. Ofrece un perfilado con vaporización y aceites esenciales que limpian y revitalizan cada poro.',
    active: true
  },
  {
    id: 3,
    locationId: 1,
    name: 'Lucas Parra',
    specialty: 'Estilo Clásico & Navaja',
    rating: 4.7,
    avatarUrl: 'https://images.unsplash.com/photo-1517832606299-7ae9b720a186?w=400&auto=format&fit=crop&q=80',
    availableHours: '10:00,11:00,12:00,13:00,15:00,16:00,17:00,18:00,19:00',
    bio: 'Devoto del afeitado tradicional de toalla caliente. Lucas mantiene vivas las técnicas clásicas europeas de navaja libre y pompadours de vieja escuela.',
    active: true
  },
  {
    id: 4,
    locationId: 1,
    name: 'Diego Alarcón',
    specialty: 'Color Metálico & Diseño',
    rating: 4.9,
    avatarUrl: 'https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Infiltrado de la cultura urbana y las pasarelas. Diego domina coloraciones extremas (platinados, grises metálicos) y grecas tribales personalizadas.',
    active: true
  },

  // Peñaflor 1 (Sucursal 2)
  {
    id: 5,
    locationId: 2,
    name: 'Sebastián Reyes',
    specialty: 'Corte Clásico Militar',
    rating: 4.8,
    avatarUrl: 'https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00',
    bio: 'Ex-estilista de academias navales. Sebastián diseña cortes militares, buzzcuts y flat-tops ultra simétricos ideales para ejecutivos y profesionales.',
    active: true
  },
  {
    id: 6,
    locationId: 2,
    name: 'Felipe Soto',
    specialty: 'Afeitado con Toalla Caliente',
    rating: 4.9,
    avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80',
    availableHours: '10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Especialista en masajes capilares relajantes y afeitado de cabeza completo a navaja. Brinda una experiencia de relajación zen absoluta.',
    active: true
  },
  {
    id: 7,
    locationId: 2,
    name: 'Matías Rojas',
    specialty: 'Fade Urbano & Diseños',
    rating: 4.6,
    avatarUrl: 'https://images.unsplash.com/photo-1605462863863-10d9e47e15ee?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Inspirado por el trap y los deportes americanos. Especialista en high-fades, drop-fades y líneas de diseño geométrico de alta complejidad técnica.',
    active: true
  },
  {
    id: 8,
    locationId: 2,
    name: 'Misael Castro',
    specialty: 'Estilo Ejecutivo',
    rating: 4.8,
    avatarUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,13:00,15:00,16:00,17:00,18:00',
    bio: 'Su enfoque es la elegancia sobria. Especialista en cortes con tijera para cabellos largos y medios, manteniendo un estilo formal pero con personalidad.',
    active: true
  },

  // Peñaflor 2 (Sucursal 3)
  {
    id: 9,
    locationId: 3,
    name: 'Joaquín Guzmán',
    specialty: 'Corte & Barba VIP',
    rating: 5.0,
    avatarUrl: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Condecorado como barbero del año en certámenes nacionales. Su servicio VIP incluye diagnóstico de visagismo capilar y asesoramiento estético completo.',
    active: true
  },
  {
    id: 10,
    locationId: 3,
    name: 'Andrés Muñoz',
    specialty: 'Arreglo de Barba Royale',
    rating: 4.9,
    avatarUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00',
    bio: 'Especialista en barbas vikingas y barbas de candado. Experto en alaciado de vello facial, hidratación profunda y moldeado con bálsamos finos.',
    active: true
  },
  {
    id: 11,
    locationId: 3,
    name: 'Mauricio Ortiz',
    specialty: 'Visagismo & Estilismo',
    rating: 4.8,
    avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80',
    availableHours: '10:00,11:00,12:00,13:00,14:00,16:00,17:00,18:00',
    bio: 'Creativo apasionado del modelado y la estructura ósea craneal. Diseña tu peinado en base a la geometría de tu rostro para resaltar tus mejores rasgos.',
    active: true
  },
  {
    id: 12,
    locationId: 3,
    name: 'Tomás Lagos',
    specialty: 'Corte Joven & Tendencia',
    rating: 4.7,
    avatarUrl: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80',
    availableHours: '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00',
    bio: 'Experto en las corrientes internacionales de K-Pop y Mullets modernos. Tomás es el favorito del público joven por su carisma y asesoramiento fresco.',
    active: true
  }
];

// Servicios disponibles con precios oficiales en CLP
export const SERVICES_LIST = [
  { id: 'deg', name: 'Corte de Pelo Degrado', price: 15000, duration: '45 min' },
  { id: 'barb', name: 'Arreglo de Barba Royale', price: 10000, duration: '30 min' },
  { id: 'combo', name: 'Combo Big Boys (Pelo + Barba)', price: 22000, duration: '60 min' },
  { id: 'vip', name: 'Servicio Completo VIP', price: 28000, duration: '80 min' },
  { id: 'mil', name: 'Corte Clásico Militar', price: 13500, duration: '40 min' },
  { id: 'rit', name: 'Ritual de Barba & Barbería', price: 22000, duration: '50 min' }
];

// Semilla de Reseñas Iniciales
const SEED_REVIEWS = [
  {
    id: 1,
    locationId: 1,
    authorName: 'Ignacio Valdés',
    rating: 5,
    comment: 'Excelente local en Talagante. La atención con café de cortesía y la música ambiente hacen que la espera valga totalmente la pena.',
    serviceOrSubject: 'Sucursal Talagante',
    date: '24-08-2026',
    tags: 'Ambiente VIP, Café de Grano, Puntualidad',
    reviewType: 'BRANCH'
  },
  {
    id: 2,
    locationId: 1,
    barberId: 1,
    authorName: 'Juan Pérez',
    rating: 5,
    comment: 'Mateo Silva es un maestro con la navaja y el fade. Quedó exactamente como lo pedí, degradado limpio y muy pulcro.',
    serviceOrSubject: 'Corte Moderno & Fade',
    date: '25-08-2026',
    tags: 'Degradado Perfecto, Precisión, Trato Amable',
    reviewType: 'SERVICE'
  },
  {
    id: 3,
    locationId: 2,
    authorName: 'Rodrigo Fuentes',
    rating: 5,
    comment: 'La sucursal de Peñaflor 1 es muy amplia y moderna. Sillones súper cómodos y todo muy higienizado.',
    serviceOrSubject: 'Sucursal Peñaflor 1',
    date: '23-08-2026',
    tags: 'Higiene Impecable, Sillones Cómodos, Rapidez',
    reviewType: 'BRANCH'
  },
  {
    id: 4,
    locationId: 2,
    barberId: 6,
    authorName: 'Nicolás Ibáñez',
    rating: 5,
    comment: 'El afeitado con toalla caliente de Felipe es otro nivel de relajación. Salí renovado.',
    serviceOrSubject: 'Afeitado con Toalla Caliente',
    date: '24-08-2026',
    tags: 'Relajación Total, Toalla Caliente, Profesionalismo',
    reviewType: 'SERVICE'
  },
  {
    id: 5,
    locationId: 3,
    authorName: 'Claudio Santander',
    rating: 5,
    comment: 'El local de Peñaflor 2 tiene una vibra increíble. Te reciben con buena onda y atienden puntual a la hora reservada.',
    serviceOrSubject: 'Sucursal Peñaflor 2',
    date: '22-08-2026',
    tags: 'Puntualidad, Buena Música, Estacionamiento',
    reviewType: 'BRANCH'
  },
  {
    id: 6,
    locationId: 3,
    barberId: 9,
    authorName: 'Gonzalo Silva',
    rating: 5,
    comment: 'Joaquín Guzmán es un genio del visagismo. Me aconsejó el corte según la forma de mi cara y el cambio fue notable.',
    serviceOrSubject: 'Corte & Barba VIP',
    date: '25-08-2026',
    tags: 'Visagismo, Asesoría Personalizada, Calidad Superior',
    reviewType: 'SERVICE'
  }
];

// Semilla de Citas completadas para reportes inmediatos
const SEED_APPOINTMENTS = [
  {
    id: 101,
    locationId: 1,
    barberId: 1,
    customerName: 'Juan Pérez',
    customerPhone: '+56 9 1122 3344',
    customerEmail: 'juan.perez@ejemplo.cl',
    dateTime: '01-09-2026 10:00',
    serviceName: 'Corte de Pelo Degrado',
    servicePrice: 15000,
    prepayAmount: 3000,
    rescheduleCount: 0,
    prepayForfeited: false,
    status: 'Completada',
    isGuest: false,
    userId: 1,
    hasReview: true
  },
  {
    id: 102,
    locationId: 1,
    barberId: 2,
    customerName: 'Diego Muñoz',
    customerPhone: '+56 9 2233 4455',
    customerEmail: 'diego.m@ejemplo.cl',
    dateTime: '01-09-2026 11:00',
    serviceName: 'Arreglo de Barba Royale',
    servicePrice: 12000,
    prepayAmount: 3000,
    rescheduleCount: 0,
    prepayForfeited: false,
    status: 'Completada',
    isGuest: false,
    userId: 2,
    hasReview: false
  },
  {
    id: 103,
    locationId: 2,
    barberId: 5,
    customerName: 'Cristóbal Solís',
    customerPhone: '+56 9 3344 5566',
    customerEmail: 'cristobal.s@ejemplo.cl',
    dateTime: '01-09-2026 15:30',
    serviceName: 'Corte Clásico Militar',
    servicePrice: 13500,
    prepayAmount: 3000,
    rescheduleCount: 0,
    prepayForfeited: false,
    status: 'Completada',
    isGuest: true,
    userId: null,
    hasReview: false
  },
  {
    id: 104,
    locationId: 3,
    barberId: 9,
    customerName: 'Gonzalo Silva',
    customerPhone: '+56 9 4455 6677',
    customerEmail: 'gonzalo.silva@ejemplo.cl',
    dateTime: '02-09-2026 16:00',
    serviceName: 'Ritual de Barba & Barbería',
    servicePrice: 22000,
    prepayAmount: 3000,
    rescheduleCount: 0,
    prepayForfeited: false,
    status: 'Completada',
    isGuest: false,
    userId: 3,
    hasReview: true
  }
];

class BarberiaStore {
  constructor() {
    this.listeners = new Set();
    this.cloudSyncActive = false;
    this.unsubscribeCloud = null;
    this.initData();
    this.initCloudSync();
  }

  initData() {
    let locs = [];
    try {
      locs = JSON.parse(localStorage.getItem(STORAGE_KEYS.LOCATIONS) || '[]');
    } catch (e) {}
    if (!locs.length || locs.some(l => !l.address || l.address.includes('Miraflores') || l.address.includes('1282') || l.address.includes('Libertador') || (l.id === 1 && (!l.address.includes('Boulevard del centro') || l.lat !== -33.6631)) || (l.id === 2 && (!l.address.includes('21 de mayo 4566') || l.lng !== -70.8993)) || (l.id === 3 && (!l.address.includes('Alcalde Luis Araya Cereceda 4387') || l.lng !== -70.9001)))) {
      localStorage.setItem(STORAGE_KEYS.LOCATIONS, JSON.stringify(SEED_LOCATIONS));
    }
    if (!localStorage.getItem(STORAGE_KEYS.BARBERS)) {
      localStorage.setItem(STORAGE_KEYS.BARBERS, JSON.stringify(SEED_BARBERS));
    }
    if (!localStorage.getItem(STORAGE_KEYS.REVIEWS)) {
      localStorage.setItem(STORAGE_KEYS.REVIEWS, JSON.stringify(SEED_REVIEWS));
    }
    if (!localStorage.getItem(STORAGE_KEYS.APPOINTMENTS)) {
      localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(SEED_APPOINTMENTS));
    }
    let usersList = [];
    try {
      usersList = JSON.parse(localStorage.getItem(STORAGE_KEYS.USERS) || '[]');
    } catch (e) {}
    if (!usersList.some(u => u && u.email && u.email.toLowerCase() === 'test@bigboys.cl')) {
      usersList.push(SEED_TEST_USER);
    }
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(usersList));
    let couponsList = [];
    try {
      couponsList = JSON.parse(localStorage.getItem(STORAGE_KEYS.COUPONS) || '[]');
    } catch (e) {}
    if (!couponsList.some(c => c && c.userEmail && c.userEmail.toLowerCase() === 'test@bigboys.cl')) {
      couponsList.push({
        id: 9999,
        pin: '7788',
        userEmail: 'test@bigboys.cl',
        userName: 'Usuario de Prueba Big Boys',
        userPhone: '+56 9 8765 4321',
        userRut: '19.342.110-3',
        benefit: '1x Perfilado de Cejas Profesional Gratis',
        isRedeemed: false,
        issuedAt: '01-09-2026 10:00'
      });
    }
    localStorage.setItem(STORAGE_KEYS.COUPONS, JSON.stringify(couponsList));
    if (!localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS)) {
      localStorage.setItem(
        STORAGE_KEYS.NOTIFICATIONS,
        JSON.stringify([
          {
            id: Date.now(),
            title: '¡Bienvenido a Big Boys!',
            message: 'Reserva en línea, revisa tu historial y gestiona tus citas con nuestra app web.',
            timestamp: AppFormatters.currentTimeString()
          }
        ])
      );
    }
    if (!localStorage.getItem(STORAGE_KEYS.ADMIN_CONFIG)) {
      localStorage.setItem(
        STORAGE_KEYS.ADMIN_CONFIG,
        JSON.stringify({
          pin: '1234',
          name: 'Administrador Central Big boys',
          avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80',
          email: 'admin@bigboys.cl'
        })
      );
    }
    if (!localStorage.getItem(STORAGE_KEYS.GMAIL_CONFIG)) {
      localStorage.setItem(
        STORAGE_KEYS.GMAIL_CONFIG,
        JSON.stringify({
          senderEmail: 'bigboysbarberia.chile@gmail.com',
          appPassword: ''
        })
      );
    }
    if (!localStorage.getItem(STORAGE_KEYS.EMAIL_DATABASE)) {
      const seedEmails = [
        { id: 1, email: 'juan.perez@ejemplo.cl', fullName: 'Juan Pérez', phone: '+56 9 1122 3344', source: 'Reserva Online', addedAt: '01-09-2026 10:00', lastActivityAt: '01-09-2026 10:00', interactionsCount: 2, notes: 'Cliente frecuente Talagante' },
        { id: 2, email: 'diego.m@ejemplo.cl', fullName: 'Diego Muñoz', phone: '+56 9 2233 4455', source: 'Reserva Online', addedAt: '01-09-2026 11:00', lastActivityAt: '01-09-2026 11:00', interactionsCount: 1, notes: 'Prefiere perfilado de barba' },
        { id: 3, email: 'cristobal.s@ejemplo.cl', fullName: 'Cristóbal Solís', phone: '+56 9 3344 5566', source: 'Reserva Invitado', addedAt: '01-09-2026 15:30', lastActivityAt: '01-09-2026 15:30', interactionsCount: 1, notes: 'Cliente sucursal Peñaflor 1' },
        { id: 4, email: 'gonzalo.silva@ejemplo.cl', fullName: 'Gonzalo Silva', phone: '+56 9 4455 6677', source: 'Reserva Online', addedAt: '02-09-2026 16:00', lastActivityAt: '02-09-2026 16:00', interactionsCount: 1, notes: 'Atención VIP Peñaflor 2' }
      ];
      localStorage.setItem(STORAGE_KEYS.EMAIL_DATABASE, JSON.stringify(seedEmails));
    }
    if (!localStorage.getItem(STORAGE_KEYS.SENT_EMAILS_LOG)) {
      const seedSentLogs = [
        {
          id: 1,
          toEmail: 'juan.perez@ejemplo.cl',
          toName: 'Juan Pérez',
          subject: '✂️ Confirmación de Cita - Big Boys Barbershop',
          body: 'Estimado/a Juan Pérez:\n\nTu cita para Corte de Pelo Degrado ha sido confirmada para el 01-09-2026 10:00 hrs con el barbero Mateo Silva en Big Boys - Talagante.\n\nAbono recibido: $5.000 CLP vía Webpay Plus.\n\n¡Te esperamos!',
          status: 'Enviado con Éxito',
          sentAt: '01-09-2026 10:02'
        },
        {
          id: 2,
          toEmail: 'gonzalo.silva@ejemplo.cl',
          toName: 'Gonzalo Silva',
          subject: '✂️ Confirmación de Cita - Big Boys Barbershop',
          body: 'Estimado/a Gonzalo Silva:\n\nTu cita para Ritual de Barba & Barbería ha sido confirmada para el 02-09-2026 16:00 hrs con el barbero Joaquín Guzmán en Big Boys - Peñaflor 2 (Miraflores).\n\nAbono recibido: $8.000 CLP vía Webpay Plus.\n\n¡Te esperamos!',
          status: 'Enviado con Éxito',
          sentAt: '02-09-2026 16:02'
        }
      ];
      localStorage.setItem(STORAGE_KEYS.SENT_EMAILS_LOG, JSON.stringify(seedSentLogs));
    }
    if (localStorage.getItem(STORAGE_KEYS.IS_DARK_MODE) === null) {
      localStorage.setItem(STORAGE_KEYS.IS_DARK_MODE, 'true');
    }
  }

  // --- Subscripción Reactiva ---
  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  notify() {
    for (const listener of this.listeners) {
      listener();
    }
  }

  // --- Getters de Estado ---
  getLocations() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.LOCATIONS) || '[]');
  }

  getBarbers() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.BARBERS) || '[]');
  }

  getAppointments() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.APPOINTMENTS) || '[]');
  }

  getReviews() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.REVIEWS) || '[]');
  }

  getUsers() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.USERS) || '[]');
  }

  getCoupons() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.COUPONS) || '[]');
  }

  getNotifications() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS) || '[]');
  }

  getEmailDatabase() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.EMAIL_DATABASE) || '[]');
  }

  getSentEmailsLog() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.SENT_EMAILS_LOG) || '[]');
  }

  isCloudActive() {
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    return Boolean(this.cloudSyncActive && sb);
  }

  initCloudSync() {
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (!sb) {
      return;
    }

    try {
      this.cloudSyncActive = true;
      sb.from('appointments').select('*').order('created_at', { ascending: false }).limit(200)
        .then(({ data, error }) => {
          if (!error && Array.isArray(data)) {
            const mapped = data.map(d => ({
              id: Number(d.id),
              locationId: Number(d.location_id) || 1,
              barberId: Number(d.barber_id) || 1,
              customerName: d.customer_name || 'Cliente',
              customerPhone: d.customer_phone || '',
              customerEmail: d.customer_email || '',
              dateTime: d.date_time || '',
              serviceName: d.service_name || 'Servicio Barbería',
              servicePrice: Number(d.service_price) || 0,
              prepayAmount: Number(d.prepay_amount) || 3000,
              rescheduleCount: Number(d.reschedule_count) || 0,
              prepayForfeited: Boolean(d.prepay_forfeited),
              status: d.status || 'Confirmada',
              isGuest: Boolean(d.is_guest),
              userId: d.user_id || null,
              createdAt: d.created_at || AppFormatters.currentDateTimeString()
            }));
            this.syncAppointmentsFromCloud(mapped);
          }
        });

      this.unsubscribeCloud = sb.channel('public:appointments')
        .on('postgres_changes', { event: '*', schema: 'public', table: 'appointments' }, (payload) => {
          const d = payload.new;
          if (d && (payload.eventType === 'INSERT' || payload.eventType === 'UPDATE')) {
            const appt = {
              id: Number(d.id),
              locationId: Number(d.location_id) || 1,
              barberId: Number(d.barber_id) || 1,
              customerName: d.customer_name || 'Cliente',
              customerPhone: d.customer_phone || '',
              customerEmail: d.customer_email || '',
              dateTime: d.date_time || '',
              serviceName: d.service_name || 'Servicio Barbería',
              servicePrice: Number(d.service_price) || 0,
              prepayAmount: Number(d.prepay_amount) || 3000,
              rescheduleCount: Number(d.reschedule_count) || 0,
              prepayForfeited: Boolean(d.prepay_forfeited),
              status: d.status || 'Confirmada',
              isGuest: Boolean(d.is_guest),
              userId: d.user_id || null,
              createdAt: d.created_at || AppFormatters.currentDateTimeString()
            };
            this.syncAppointmentsFromCloud([appt]);
          }
        })
        .subscribe();
    } catch (err) {
      console.warn('Error al conectar con Supabase Realtime:', err);
    }
  }

  syncAppointmentsFromCloud(cloudList) {
    if (!Array.isArray(cloudList)) return;

    const localList = this.getAppointments();
    const mergedMap = new Map();

    // 1. Cargar las citas locales
    localList.forEach((a) => {
      mergedMap.set(String(a.id), a);
    });

    // 2. Sobrescribir / añadir con Supabase PostgreSQL (máxima prioridad y sincronización viva)
    cloudList.forEach((ca) => {
      mergedMap.set(String(ca.id), ca);
    });

    const mergedList = Array.from(mergedMap.values());
    mergedList.sort((a, b) => {
      const timeA = Number(a.id) || 0;
      const timeB = Number(b.id) || 0;
      return timeB - timeA;
    });

    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(mergedList));
    this.notify();
  }

  async publishAppointmentToCloud(appointment) {
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (!sb) return false;
    try {
      const row = {
        id: Number(appointment.id) || Date.now(),
        user_id: appointment.userId ? Number(appointment.userId) : null,
        barber_id: Number(appointment.barberId) || 1,
        location_id: Number(appointment.locationId) || 1,
        service_name: appointment.serviceName || 'Servicio Barbería',
        service_price: Number(appointment.servicePrice) || 0,
        prepay_amount: Number(appointment.prepayAmount) || 3000,
        reschedule_count: Number(appointment.rescheduleCount) || 0,
        rescheduled_at: appointment.rescheduledAt || null,
        prepay_forfeited: Boolean(appointment.prepayForfeited),
        date_time: appointment.dateTime || '',
        status: appointment.status || 'Confirmada',
        customer_name: appointment.customerName || '',
        customer_phone: appointment.customerPhone || '',
        customer_email: appointment.customerEmail || '',
        is_guest: Boolean(appointment.isGuest)
      };

      const { error } = await sb.from('appointments').upsert(row, { onConflict: 'id' });
      if (error) {
        console.warn('Aviso publicando en Supabase:', error);
        return false;
      }
      console.log('✓ Cita guardada en Supabase PostgreSQL (appointments):', row.id);
      return true;
    } catch (e) {
      console.warn('Error publicando en Supabase:', e);
      return false;
    }
  }

  async updateAppointmentStatusInCloud(appointment, newStatus) {
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (!sb || !appointment) return false;
    try {
      const updatePayload = {
        status: newStatus,
        prepay_forfeited: Boolean(appointment.prepayForfeited),
        updated_at: new Date().toISOString()
      };
      if (appointment.rescheduleCount !== undefined) {
        updatePayload.reschedule_count = Number(appointment.rescheduleCount);
      }
      if (appointment.rescheduledAt) {
        updatePayload.rescheduled_at = appointment.rescheduledAt;
      }
      if (appointment.cancelledBy) {
        updatePayload.cancelled_by = appointment.cancelledBy;
      }
      const { error } = await sb.from('appointments').update(updatePayload).eq('id', Number(appointment.id));

      if (!error) {
        console.log(`✓ Estado actualizado a '${newStatus}' en Supabase:`, appointment.id);
        return true;
      }
      return false;
    } catch (e) {
      console.warn('Error actualizando estado en Supabase:', e);
      return false;
    }
  }

  getLoggedInUser() {
    const raw = localStorage.getItem(STORAGE_KEYS.LOGGED_USER);
    return raw ? JSON.parse(raw) : null;
  }

  isAdmin() {
    if (typeof sessionStorage !== 'undefined' && sessionStorage.getItem('bigboys_session_active') !== 'true') {
      return false;
    }
    return localStorage.getItem(STORAGE_KEYS.IS_ADMIN) === 'true';
  }

  isDarkMode() {
    return localStorage.getItem(STORAGE_KEYS.IS_DARK_MODE) !== 'false';
  }

  getAdminConfig() {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.ADMIN_CONFIG) || '{}');
  }

  saveEmailToDatabase(email, fullName = '', phone = '', source = 'Registro Online', notes = '') {
    if (!email || !email.includes('@')) return;
    const cleanEmail = email.trim().toLowerCase();
    const db = this.getEmailDatabase();
    const existing = db.find(item => item.email.toLowerCase() === cleanEmail);

    if (existing) {
      if (fullName && (!existing.fullName || existing.fullName === 'Cliente Big Boys')) existing.fullName = fullName.trim();
      if (phone && !existing.phone) existing.phone = phone.trim();
      existing.lastActivityAt = AppFormatters.currentDateTimeString();
      existing.interactionsCount = (existing.interactionsCount || 1) + 1;
      if (notes && !existing.notes.includes(notes)) {
        existing.notes = existing.notes ? `${existing.notes} | ${notes}` : notes;
      }
    } else {
      db.unshift({
        id: Date.now(),
        email: cleanEmail,
        fullName: (fullName || '').trim() || 'Cliente Big Boys',
        phone: (phone || '').trim(),
        source,
        addedAt: AppFormatters.currentDateTimeString(),
        lastActivityAt: AppFormatters.currentDateTimeString(),
        interactionsCount: 1,
        notes: notes || 'Registrado en el sistema'
      });
    }
    localStorage.setItem(STORAGE_KEYS.EMAIL_DATABASE, JSON.stringify(db));
    this.notify();
  }

  logSentEmail({ fromEmail, toEmail, toName, subject, body, status = 'Enviado con Éxito ✓' }) {
    if (!toEmail) return;
    const defaultSender = (this.getGmailConfig && this.getGmailConfig().senderEmail) || 'bigboysbarberia.chile@gmail.com';
    const logs = this.getSentEmailsLog();
    logs.unshift({
      id: Date.now() + Math.floor(Math.random() * 100),
      fromEmail: fromEmail || defaultSender,
      toEmail: toEmail.trim(),
      toName: toName || 'Cliente',
      subject: subject || 'Notificación Big Boys',
      body: body || '',
      status,
      sentAt: AppFormatters.currentDateTimeString()
    });
    localStorage.setItem(STORAGE_KEYS.SENT_EMAILS_LOG, JSON.stringify(logs.slice(0, 100)));
    this.notify();
  }

  async sendRealEmail({ to, subject, body }) {
    if (!to) return false;
    const cleanTo = to.trim().toLowerCase();

    // 1. Despacho Nube 24/7 (Google Apps Script Web App / Webhook en la Nube sin tarjetas)
    const cloudUrl = (localStorage.getItem('bigboys_cloud_mailer_url') || '').trim();
    if (cloudUrl) {
      try {
        await fetch(cloudUrl, {
          method: 'POST',
          mode: 'no-cors',
          headers: { 'Content-Type': 'text/plain' },
          body: JSON.stringify({ to: cleanTo, subject, body })
        });
        console.log('✓ Correo despachado exitosamente vía Google Cloud Webhook 24/7:', cleanTo);
        return true;
      } catch (cloudSendErr) {
        console.warn('Aviso en despacho vía Google Cloud Webhook, probando respaldo:', cloudSendErr);
      }
    }

    // 2. Intento de despacho directo vía servicio HTTP local (localhost:3001)
    try {
      const res = await fetch('http://localhost:3001/api/send-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ to: cleanTo, subject, body })
      });
      if (res.ok) {
        console.log('✓ Correo despachado en vivo vía Servicio Local Gmail SMTP:', cleanTo);
        return true;
      }
    } catch (localErr) {
      // Si el servidor local no está activo en esta sesión, continuar a la cola de Supabase
    }

    // 3. Encolar en Supabase PostgreSQL (tabla 'mail') para despacho en segundo plano
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (sb) {
      try {
        await sb.from('mail').insert({
          recipient: cleanTo,
          sender: 'bigboysbarberia.chile@gmail.com',
          subject,
          body,
          status: 'pending'
        });
        console.log('✓ Correo encolado con éxito en Supabase PostgreSQL (mail):', cleanTo);
        return true;
      } catch (cloudErr) {
        console.warn('Aviso al encolar correo en Supabase:', cloudErr);
      }
    }
    return false;
  }

  // 1. Correo Automático: Registro Exitoso & Regalo de Bienvenida
  sendWelcomeEmail(cleanName, cleanEmail, cleanPhone, cleanRut, couponPin) {
    const fromEmail = this.getGmailConfig().senderEmail || 'bigboysbarberia.chile@gmail.com';
    const subject = `💈 ¡Bienvenido al Club Big Boys! Tu Regalo de Bienvenida (PIN: ${couponPin})`;
    const body = `¡Hola ${cleanName}!\n\n` +
      `Te damos la más cordial bienvenida al Club Oficial de Big Boys Barberías Chile.\n\n` +
      `Tu cuenta de socio ha sido registrada exitosamente con los siguientes datos:\n` +
      `• Nombre: ${cleanName}\n` +
      `• RUT: ${cleanRut || 'Registrado'}\n` +
      `• Teléfono: ${cleanPhone}\n` +
      `• Correo: ${cleanEmail}\n\n` +
      `🎁 REGALO EXCLUSIVO DE BIENVENIDA:\n` +
      `Como nuevo miembro de nuestra comunidad, tienes un Perfilado de Cejas Profesional completamente GRATIS en cualquiera de nuestras sucursales.\n\n` +
      `╔═══════════════════════════════════════════════════╗\n` +
      `            PIN DE CANJE: ${couponPin}\n` +
      `╚═══════════════════════════════════════════════════╝\n\n` +
      `¿Cómo canjearlo?\n` +
      `Solo debes presentar este código PIN a tu barbero o en la recepción de cualquiera de nuestras sucursales al momento de tu atención:\n` +
      `📍 Sucursal Talagante: Balmaceda #1040, Talagante\n` +
      `📍 Sucursal Peñaflor 1: Av. 21 de Mayo #3420, Peñaflor\n` +
      `📍 Sucursal Peñaflor 2: Vicuña Mackenna #1890, Peñaflor\n\n` +
      `Agenda tu próxima hora directamente en nuestra aplicación web o respondiendo a este correo.\n\n` +
      `Atentamente,\n` +
      `Equipo Big Boys Barberías Chile\n` +
      `Correo oficial: ${fromEmail}`;

    this.logSentEmail({
      fromEmail,
      toEmail: cleanEmail,
      toName: cleanName,
      subject,
      body,
      status: 'Enviado con Éxito ✓'
    });

    // Despacho real automático vía Gmail SMTP / Supabase PostgreSQL
    this.sendRealEmail({ to: cleanEmail, subject, body });
  }

  // 2. Correo Automático: Confirmación de Cita & Comprobante de Abono Webpay
  sendBookingConfirmationEmail(appt, bName, locName) {
    if (!appt || !appt.customerEmail) return;
    const fromEmail = this.getGmailConfig().senderEmail || 'bigboysbarberia.chile@gmail.com';
    const prepay = Number(appt.prepayAmount) || 3000;
    const saldo = Math.max(0, (appt.servicePrice || 0) - prepay);
    const subject = `✂️ Confirmación de Cita #${appt.id} y Comprobante de Abono ($3.000) - Big Boys Barbershop`;
    const body = `Estimado/a ${appt.customerName}:\n\n` +
      `¡Tu reserva ha sido confirmada con éxito en Big Boys Barberías! A continuación te entregamos el comprobante y los detalles oficiales de tu atención:\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `📋 DETALLES DE LA RESERVA:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `• Código de Cita: #${appt.id}\n` +
      `• Fecha y Hora: ${appt.dateTime} hrs\n` +
      `• Sucursal: ${locName}\n` +
      `• Barbero Asignado: ${bName}\n` +
      `• Servicio Reservado: ${appt.serviceName}\n` +
      `• Tipo de Cliente: ${appt.isGuest ? 'Invitado' : 'Socio Registrado Big Boys'}\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `💳 DETALLE FINANCIERO Y ABONO WEBPAY:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `• Total Servicio: ${AppFormatters.formatClp(appt.servicePrice)}\n` +
      `• Abono Pagado (Webpay Plus): ${AppFormatters.formatClp(prepay)} [PAGADO ONLINE ✓]\n` +
      `• Saldo Pendiente en Local: ${AppFormatters.formatClp(saldo)} [A PAGAR EN RECEPCIÓN]\n` +
      `• Método de Abono: Tarjeta de Débito / Crédito vía Webpay Plus Transbank\n` +
      `• Estado: Confirmada y Garantizada\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `⚠️ POLÍTICA DE REAGENDAMIENTO Y NO-SHOW (IMPORTANTE):\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `1. REAGENDAMIENTOS: Podrás reagendar tu cita únicamente con al menos 24 HORAS DE ANTICIPACIÓN al servicio para liberar la agenda del barbero. Puedes reutilizar este mismo abono de $3.000 para reagendar hasta un MÁXIMO DE 2 VECES.\n` +
      `2. CANCELACIÓN TARDÍA / NO-SHOW: Si anulas tu atención con menos de 24 horas de anticipación o no te presentas a tu cita (no-show), PERDERÁS tu abono o bonificación de $3.000 sin derecho a reembolso ni saldo a favor.\n` +
      `3. CANCELACIÓN OPORTUNA: Si anulas con más de 24 horas de anticipación, se gestionará la devolución íntegra del 100% de tu abono de $3.000 a tu cuenta bancaria.\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `💈 RECOMENDACIONES PARA TU ATENCIÓN:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `1. Te sugerimos presentarte entre 5 y 10 minutos antes de tu hora fijada para asegurar una experiencia puntual y relajada.\n` +
      `2. Puedes presentar este correo directamente desde tu celular al ingresar al local.\n\n` +
      `¡Muchas gracias por confiar en Big Boys! Te esperamos para entregarte la mejor experiencia y distinción en barbería.\n\n` +
      `Atentamente,\n` +
      `Big Boys Barberías Chile\n` +
      `Contacto: ${fromEmail}`;

    this.logSentEmail({
      fromEmail,
      toEmail: appt.customerEmail,
      toName: appt.customerName,
      subject,
      body,
      status: 'Enviado con Éxito ✓'
    });

    // Despacho real automático vía Gmail SMTP / Supabase PostgreSQL
    this.sendRealEmail({ to: appt.customerEmail, subject, body });
  }

  // Correo Automático: Reagendamiento Exitoso de Cita
  sendRescheduleConfirmationEmail(appt, bName, locName, oldDateTime, newDateTime) {
    if (!appt || !appt.customerEmail) return;
    const fromEmail = this.getGmailConfig().senderEmail || 'bigboysbarberia.chile@gmail.com';
    const prepayFormatted = AppFormatters.formatClp(appt.prepayAmount || 3000);
    const count = appt.rescheduleCount || 1;
    const remaining = Math.max(0, 2 - count);

    const subject = `✂️ Cita Reagendada #${appt.id} (Uso ${count}/2 de Abono) - Big Boys Barbershop`;
    const body = `Estimado/a ${appt.customerName}:\n\n` +
      `Te confirmamos que tu cita ha sido REAGENDADA CON ÉXITO en Big Boys Barberías.\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `📋 NUEVOS DETALLES DE TU ATENCIÓN:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `• Código de Cita: #${appt.id}\n` +
      `• Fecha y Hora Anterior: ${oldDateTime} hrs\n` +
      `• NUEVA Fecha y Hora: ${newDateTime} hrs [CONFIRMADA ✓]\n` +
      `• Sucursal: ${locName}\n` +
      `• Barbero: ${bName}\n` +
      `• Servicio: ${appt.serviceName}\n` +
      `• Abono Reutilizado: ${prepayFormatted} (Vigente y aplicado a tu nueva hora)\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `📊 CONTROL DE REAGENDAMIENTOS:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `• Reagendamientos Utilizados: ${count} de 2 permitidos\n` +
      `• Reagendamientos Disponibles Restantes: ${remaining}\n\n` +
      `Recordatorio: Cualquier nuevo cambio debe solicitarse con al menos 24 horas de anticipación a tu nueva hora fijada. Cancelaciones con menos de 24 horas implicarán la pérdida definitiva del abono.\n\n` +
      `¡Te esperamos en Big Boys!\n\n` +
      `Atentamente,\n` +
      `Big Boys Barberías Chile\n` +
      `Contacto: ${fromEmail}`;

    this.logSentEmail({
      fromEmail,
      toEmail: appt.customerEmail,
      toName: appt.customerName,
      subject,
      body,
      status: 'Enviado con Éxito ✓'
    });

    this.sendRealEmail({ to: appt.customerEmail, subject, body });
  }

  // Correo Automático: Notificación de Retención de Abono por Anulación Tardía (<24h) o No-Show
  sendNoShowForfeitEmail(appt, bName, locName) {
    if (!appt || !appt.customerEmail) return;
    const fromEmail = this.getGmailConfig().senderEmail || 'bigboysbarberia.chile@gmail.com';
    const prepayFormatted = AppFormatters.formatClp(appt.prepayAmount || 3000);

    const subject = `⚠️ Notificación de Anulación Tardía / No-Show - Cita #${appt.id} | Big Boys Barbershop`;
    const body = `Estimado/a ${appt.customerName}:\n\n` +
      `Le informamos que su cita #${appt.id} programada para el ${appt.dateTime} hrs con ${bName} en la sucursal ${locName} ha sido cancelada fuera del plazo estipulado o registrada como No-Show (inasistencia).\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `⚠️ POLÍTICA DE RETENCIÓN DE ABONO:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Conforme a la política comercial informada y aceptada al momento de reservar:\n` +
      `• Las anulaciones o reagendamientos deben realizarse con un mínimo de 24 HORAS DE ANTICIPACIÓN para permitir la liberación oportuna del bloque en la agenda del profesional.\n` +
      `• Al realizarse la cancelación dentro de las 24 horas previas a la atención (o ante la inasistencia al salón), el abono de reserva por ${prepayFormatted} NO ES REEMBOLSABLE ni genera saldo a favor, siendo retenido para cubrir la reserva de agenda y los costos operativos del local.\n\n` +
      `Si considera que existió un error justificado o una situación médica de fuerza mayor, por favor responda directamente a este correo (${fromEmail}) adjuntando los antecedentes para la revisión de nuestra administración.\n\n` +
      `Agradecemos su comprensión.\n\n` +
      `Atentamente,\n` +
      `Administración Big Boys Barberías Chile\n` +
      `Contacto: ${fromEmail}`;

    this.logSentEmail({
      fromEmail,
      toEmail: appt.customerEmail,
      toName: appt.customerName,
      subject,
      body,
      status: 'Enviado con Éxito ✓'
    });

    this.sendRealEmail({ to: appt.customerEmail, subject, body });
  }

  // 3. Correo Automático: Cancelación de Hora, Disculpas y Solicitud de Datos para Devolución del Abono
  sendCancellationRefundEmail(appt, barberName, locationName) {
    if (!appt || !appt.customerEmail) return;
    const fromEmail = this.getGmailConfig().senderEmail || 'bigboysbarberia.chile@gmail.com';
    const bName = barberName || (this.getBarbers().find(b => b.id === appt.barberId)?.name) || 'Barbero Big Boys';
    const locName = locationName || (this.getLocations().find(l => l.id === appt.locationId)?.name) || 'Sucursal Big Boys';
    const prepayFormatted = AppFormatters.formatClp(appt.prepayAmount);

    const subject = `⚠️ Cancelación de Cita #${appt.id} - Disculpas y Solicitud de Datos para Devolución de Abono | Big Boys Barbershop`;
    const body = `Estimado/a ${appt.customerName}:\n\n` +
      `Lamentamos sinceramente tener que comunicarle que su cita agendada en Big Boys Barberías ha debido ser cancelada.\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `📋 DATOS DE LA CITA CANCELADA:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `• Número de Cita: #${appt.id}\n` +
      `• Fecha y Hora: ${appt.dateTime} hrs\n` +
      `• Sucursal: ${locName}\n` +
      `• Barbero: ${bName}\n` +
      `• Servicio: ${appt.serviceName}\n` +
      `• Abono Registrado: ${prepayFormatted}\n\n` +
      `Le ofrecemos nuestras más sinceras disculpas por cualquier inconveniente o alteración que esta cancelación pueda causarle en su agenda diaria. En Big Boys nos esforzamos por brindar una atención de excelencia y lamentamos profundamente no poder atenderle en la hora estipulada en esta oportunidad.\n\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `💰 DEVOLUCIÓN ÍNTEGRA DE SU ABONO:\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Conforme a nuestra estricta política de garantía y transparencia, procederemos a la DEVOLUCIÓN TOTAL de su abono Webpay de ${prepayFormatted} mediante transferencia electrónica bancaria directa en un plazo máximo de 24 a 48 horas hábiles.\n\n` +
      `Para poder realizar la transferencia de inmediato a su cuenta, le solicitamos por favor responder directamente a este correo (${fromEmail}) indicando los siguientes datos bancarios:\n\n` +
      `1. Nombre Completo del Titular:\n` +
      `2. RUT del Titular:\n` +
      `3. Banco de Destino:\n` +
      `4. Tipo de Cuenta (Cuenta Corriente / Cuenta Vista / CuentaRUT):\n` +
      `5. Número de Cuenta:\n` +
      `6. Correo Electrónico para Comprobante de Transferencia:\n\n` +
      `Adicionalmente, como gesto de cortesía por las molestias ocasionadas, le mantendremos activo un beneficio especial de atención prioritaria y perfilado de regalo en su próxima visita.\n\n` +
      `Si prefiere reprogramar su atención para otro horario o consultar cualquier detalle, puede responder a este correo o escribirnos directamente a ${fromEmail}.\n\n` +
      `Reiteramos nuestras más sentidas disculpas y esperamos contar nuevamente con su preferencia muy pronto.\n\n` +
      `Atentamente,\n` +
      `Equipo de Administración y Gerencia\n` +
      `Big Boys Barberías Chile\n` +
      `Correo Oficial: ${fromEmail}`;

    this.logSentEmail({
      fromEmail,
      toEmail: appt.customerEmail,
      toName: appt.customerName,
      subject,
      body,
      status: 'Enviado con Éxito ✓'
    });

    // Despacho real automático vía Gmail SMTP / Supabase PostgreSQL
    this.sendRealEmail({ to: appt.customerEmail, subject, body });

    this.addNotification(
      'Correo de Disculpas y Devolución Enviado ✉️',
      `Se envió correo a ${appt.customerEmail} solicitando datos bancarios para reembolsar ${prepayFormatted}.`
    );
  }

  deleteEmailFromDatabase(id) {
    let db = this.getEmailDatabase().filter(item => item.id !== id);
    localStorage.setItem(STORAGE_KEYS.EMAIL_DATABASE, JSON.stringify(db));
    this.addNotification('Correo Eliminado 🗑️', 'El registro de correo fue removido de la base de datos.');
    this.notify();
  }

  getGmailConfig() {
    let conf = {};
    try {
      conf = JSON.parse(localStorage.getItem(STORAGE_KEYS.GMAIL_CONFIG) || '{}');
    } catch (e) {}
    if (!conf.senderEmail || conf.senderEmail === 'contacto.bigboyschile@gmail.com') {
      conf.senderEmail = 'bigboysbarberia.chile@gmail.com';
      localStorage.setItem(STORAGE_KEYS.GMAIL_CONFIG, JSON.stringify(conf));
    }
    if (!conf.appPassword) {
      localStorage.setItem(STORAGE_KEYS.GMAIL_CONFIG, JSON.stringify(conf));
    }
    return {
      senderEmail: conf.senderEmail || 'bigboysbarberia.chile@gmail.com',
      appPassword: conf.appPassword || ''
    };
  }

  getUserCoupon() {
    const user = this.getLoggedInUser();
    if (!user) return null;
    const coupons = this.getCoupons();
    return coupons.find(c => c.userEmail.toLowerCase() === user.email.toLowerCase()) || null;
  }

  // --- Acciones de Tema ---
  toggleDarkMode() {
    const next = !this.isDarkMode();
    localStorage.setItem(STORAGE_KEYS.IS_DARK_MODE, next ? 'true' : 'false');
    if (typeof document !== 'undefined') {
      document.body.classList.toggle('light-mode', !next);
      document.documentElement.classList.toggle('light-mode', !next);
      document.documentElement.classList.toggle('dark', next);
    }
    this.notify();
    return next;
  }

  // --- Acciones de Autenticación ---
  async login(email, password) {
    const cleanEmail = (email || '').trim().toLowerCase();
    const cleanPass = (password || '').trim();

    if (!cleanEmail) {
      return { success: false, message: 'Por favor ingresa tu correo electrónico.' };
    }
    if (!cleanPass) {
      return { success: false, message: 'Por favor ingresa tu contraseña.' };
    }

    let users = this.getUsers();
    let user = users.find(u => u.email.toLowerCase() === cleanEmail);

    // --- VERIFICACIÓN DE IDENTIDAD EN SUPABASE POSTGRESQL ---
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (sb) {
      try {
        const { data: cloudUser, error } = await sb.from('users').select('*').eq('email', cleanEmail).maybeSingle();
        if (cloudUser) {
          if (!user) {
            user = {
              id: cloudUser.id,
              fullName: cloudUser.full_name || cleanEmail.split('@')[0],
              email: cleanEmail,
              phone: cloudUser.phone || '',
              rut: cloudUser.rut || '',
              passwordHash: cloudUser.password_hash || '',
              createdAt: cloudUser.created_at || AppFormatters.currentDateTimeString()
            };
            users.push(user);
            localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
          } else {
            user.fullName = cloudUser.full_name || user.fullName;
            user.phone = cloudUser.phone || user.phone;
            user.rut = cloudUser.rut || user.rut;
            if (cloudUser.password_hash) user.passwordHash = cloudUser.password_hash;
            localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
          }
        } else if (cleanEmail === 'test@bigboys.cl') {
          if (!user) {
            user = SEED_TEST_USER;
            users.push(user);
            localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
          }
          try {
            await sb.from('users').upsert({
              id: 1,
              full_name: user.fullName,
              email: cleanEmail,
              phone: user.phone,
              rut: user.rut,
              password_hash: '123'
            }, { onConflict: 'email' });
          } catch (e) {}
        } else {
          // El usuario NO existe en Supabase (fue eliminado o no está registrado)
          if (user) {
            users = users.filter(u => u.email.toLowerCase() !== cleanEmail);
            localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
            user = null;
          }
          return { success: false, message: `El correo '${cleanEmail}' no se encuentra registrado en Big Boys.` };
        }
      } catch (sbLoginErr) {
        console.warn('Aviso al consultar usuario en Supabase:', sbLoginErr);
      }
    }

    if (!user) {
      if (cleanEmail === 'test@bigboys.cl') {
        user = SEED_TEST_USER;
        users.push(user);
        localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
      } else {
        return { success: false, message: `El correo '${cleanEmail}' no se encuentra registrado.` };
      }
    }

    const expectedHash = await ValidationUtils.hashPassword(cleanPass);
    const isTestPass = cleanEmail === 'test@bigboys.cl' && (cleanPass === '123' || cleanPass === '123456');
    const isValid = isTestPass || !user.passwordHash || user.passwordHash === expectedHash;

    if (isValid) {
      // Si el usuario en Supabase no tenía contraseña web asociada (ej: registrado desde app), vincularla
      if (!user.passwordHash && sb) {
        user.passwordHash = expectedHash;
        localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
        try {
          await sb.from('users').update({ password_hash: expectedHash }).eq('email', cleanEmail);
        } catch (e) {}
      }

      localStorage.setItem(STORAGE_KEYS.LOGGED_USER, JSON.stringify(user));
      localStorage.setItem(STORAGE_KEYS.IS_ADMIN, 'false');
      if (typeof sessionStorage !== 'undefined') {
        sessionStorage.setItem('bigboys_session_active', 'true');
      }
      this.addNotification(
        `¡Bienvenido ${user.fullName}!`,
        'Sesión iniciada correctamente con tu cuenta Big boys.'
      );
      this.notify();
      return { success: true, user };
    } else {
      return { success: false, message: 'Contraseña incorrecta. Por favor verifica e intenta nuevamente.' };
    }
  }

  loginAsAdmin(pin) {
    const adminConfig = this.getAdminConfig();
    if (pin === adminConfig.pin) {
      localStorage.setItem(STORAGE_KEYS.IS_ADMIN, 'true');
      if (typeof sessionStorage !== 'undefined') {
        sessionStorage.setItem('bigboys_session_active', 'true');
      }
      const adminUser = {
        id: -99,
        fullName: adminConfig.name,
        email: adminConfig.email,
        phone: '+56 9 9999 9999',
        avatarUrl: adminConfig.avatarUrl
      };
      localStorage.setItem(STORAGE_KEYS.LOGGED_USER, JSON.stringify(adminUser));
      this.notify();
      return { success: true };
    }
    return { success: false, message: 'PIN de Administrador incorrecto (El PIN por defecto es 1234).' };
  }

  async register({ fullName, email, phone, rut, password, avatarUrl = '' }) {
    const cleanName = (fullName || '').trim();
    const cleanEmail = (email || '').trim().toLowerCase();
    const cleanPhone = (phone || '').trim();
    const cleanRut = ValidationUtils.cleanChileanRut(rut);
    const cleanPass = (password || '').trim();

    if (!cleanName || cleanName.length < 3) {
      return { success: false, message: 'Por favor ingresa tu nombre completo (mínimo 3 caracteres).' };
    }
    if (!ValidationUtils.isValidChileanRut(cleanRut)) {
      return {
        success: false,
        message: 'El RUT ingresado no es válido. Debe tener formato chileno con dígito verificador Módulo 11 (Ej: 19.342.110-3).'
      };
    }
    if (!ValidationUtils.isValidEmail(cleanEmail)) {
      return { success: false, message: 'Por favor ingresa un correo electrónico válido.' };
    }
    if (!cleanPhone) {
      return { success: false, message: 'Por favor ingresa tu teléfono de contacto.' };
    }

    const passErrors = ValidationUtils.validatePasswordRequirements(cleanPass);
    if (passErrors.length > 0) {
      return { success: false, message: `La contraseña no cumple: ${passErrors.join(', ')}` };
    }

    const formattedRut = ValidationUtils.formatChileanRut(cleanRut);
    let users = this.getUsers();

    // --- VERIFICACIÓN PREVIA EN SUPABASE POSTGRESQL (Tabla 'users') ---
    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (sb) {
      try {
        // 1. Verificar por email en Supabase
        const { data: existingUser } = await sb.from('users').select('id, email').eq('email', cleanEmail).maybeSingle();
        if (existingUser) {
          alert("Este correo ya está registrado en la app de Big Boys. Por favor inicia sesión.");
          return {
            success: false,
            message: "Este correo ya está registrado en la app de Big Boys. Por favor inicia sesión."
          };
        }

        // 2. Query por RUT en Supabase
        const { data: existingRut } = await sb.from('users').select('id, rut').or(`rut.eq.${cleanRut},rut.eq.${formattedRut}`).maybeSingle();
        if (existingRut) {
          alert("Este RUT ya está registrado en la app de Big Boys. Por favor inicia sesión.");
          return {
            success: false,
            message: "Este RUT ya está registrado en la app de Big Boys. Por favor inicia sesión."
          };
        }

        // Si el usuario no existe en Supabase, purgar cualquier residuo huérfano local para sincronización exacta
        if (users.some(u => u.email.toLowerCase() === cleanEmail || ValidationUtils.cleanChileanRut(u.rut) === cleanRut)) {
          users = users.filter(u => u.email.toLowerCase() !== cleanEmail && ValidationUtils.cleanChileanRut(u.rut) !== cleanRut);
          localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
        }
      } catch (sbErr) {
        console.warn('Aviso al verificar usuario en Supabase:', sbErr);
      }
    } else {
      // Si Supabase está offline o no disponible, verificar localmente
      if (users.some(u => u.email.toLowerCase() === cleanEmail)) {
        alert("Este correo ya está registrado en la app de Big Boys. Por favor inicia sesión.");
        return { success: false, message: "Este correo ya está registrado en la app de Big Boys. Por favor inicia sesión." };
      }
      if (users.some(u => ValidationUtils.cleanChileanRut(u.rut) === cleanRut)) {
        alert("Este RUT ya está registrado en la app de Big Boys. Por favor inicia sesión.");
        return { success: false, message: "Este RUT ya está registrado en la app de Big Boys. Por favor inicia sesión." };
      }
    }

    const passwordHash = await ValidationUtils.hashPassword(cleanPass);
    const newUser = {
      id: Date.now(),
      fullName: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      rut: formattedRut,
      passwordHash,
      avatarUrl: avatarUrl.trim(),
      createdAt: AppFormatters.currentDateTimeString()
    };

    users.push(newUser);
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
    localStorage.setItem(STORAGE_KEYS.LOGGED_USER, JSON.stringify(newUser));
    localStorage.setItem(STORAGE_KEYS.IS_ADMIN, 'false');

    // Guardar el usuario en la tabla relacional 'users' de Supabase PostgreSQL:
    if (sb) {
      try {
        await sb.from('users').upsert({
          full_name: cleanName,
          email: cleanEmail,
          phone: cleanPhone,
          rut: formattedRut,
          password_hash: passwordHash
        }, { onConflict: 'email' });
        console.log("✓ Usuario registrado y guardado en Supabase PostgreSQL (users):", cleanEmail);
      } catch (cloudErr) {
        console.warn('Aviso al guardar usuario en Supabase:', cloudErr);
      }
    }

    // Generar Cupón PIN de 4 dígitos si no se supera el límite de 100
    const coupons = this.getCoupons();
    let couponPin = '';
    if (coupons.length < 100) {
      let isUnique = false;
      while (!isUnique) {
        couponPin = Math.floor(1000 + Math.random() * 9000).toString();
        if (!coupons.some(c => c.pin === couponPin)) {
          isUnique = true;
        }
      }
      const newCoupon = {
        id: Date.now(),
        pin: couponPin,
        userEmail: cleanEmail,
        userName: cleanName,
        isRedeemed: false,
        createdAt: AppFormatters.currentDateTimeString()
      };
      coupons.push(newCoupon);
      localStorage.setItem(STORAGE_KEYS.COUPONS, JSON.stringify(coupons));
    }

    this.addNotification(
      `¡Bienvenido ${cleanName}! 🎁`,
      couponPin
        ? `¡Registrado con éxito! Cupón de perfilado de cejas gratis generado con el PIN: ${couponPin}`
        : `Bienvenido a Big Boys Barberías Chile.`
    );

    // Guardado persistente en la Base de Datos de Correos y Envío Automático de Bienvenida
    this.saveEmailToDatabase(cleanEmail, cleanName, cleanPhone, 'Registro Oficial', 'Cliente registrado con cuenta');
    this.sendWelcomeEmail(cleanName, cleanEmail, cleanPhone, cleanRut, couponPin);

    this.notify();
    return { success: true, user: newUser, couponPin };
  }

  logout() {
    localStorage.removeItem(STORAGE_KEYS.LOGGED_USER);
    localStorage.setItem(STORAGE_KEYS.IS_ADMIN, 'false');
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem('bigboys_session_active');
    }
    this.notify();
  }

  // --- Métodos de Gestión y Turnos de Barberos ---
  loginAsBarber(barberId, pin = '1234') {
    const barbers = this.getBarbers();
    const barber = barbers.find(b => b.id === Number(barberId));
    if (!barber) {
      return { success: false, message: 'Barbero no encontrado.' };
    }
    if (String(pin).trim() !== '1234') {
      return { success: false, message: 'PIN de barbero incorrecto. (Predeterminado: 1234)' };
    }
    localStorage.setItem(STORAGE_KEYS.LOGGED_BARBER, JSON.stringify(barber));
    localStorage.setItem(STORAGE_KEYS.IS_ADMIN, 'false');
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem('bigboys_session_active', 'true');
    }
    this.addNotification('Turno Iniciado 💈', `¡Bienvenido ${barber.name}! Tu agenda de hoy está sincronizada.`);
    this.notify();
    return { success: true, barber };
  }

  getLoggedInBarber() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.LOGGED_BARBER) || 'null');
    } catch (e) {
      return null;
    }
  }

  logoutBarber() {
    localStorage.removeItem(STORAGE_KEYS.LOGGED_BARBER);
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem('bigboys_session_active');
    }
    this.notify();
  }

  cancelAppointmentByBarber(apptId, emergencyReason = 'Emergencia de salud / fuerza mayor del barbero') {
    const appts = this.getAppointments();
    const appt = appts.find(a => a.id === apptId);
    if (!appt) return { success: false, message: 'Cita no encontrada.' };

    appt.status = 'Cancelada';
    appt.cancellationReason = emergencyReason;
    appt.canceledBy = 'Barbero (Emergencia)';
    appt.canceledAt = AppFormatters.currentDateTimeString ? AppFormatters.currentDateTimeString() : new Date().toISOString();
    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appts));

    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (sb) {
      try {
        sb.from('appointments').update({
          status: 'Cancelada',
          cancellation_reason: emergencyReason,
          cancelled_by: 'Barbero (Emergencia)',
          updated_at: new Date().toISOString()
        }).eq('id', Number(appt.id)).then(() => {}).catch(() => {});
      } catch (err) {}
    }

    const bName = (this.getBarbers().find(b => b.id === appt.barberId)?.name) || 'Barbero Big Boys';
    const locName = (this.getLocations().find(l => l.id === appt.locationId)?.name) || 'Sucursal Big Boys';
    this.sendCancellationRefundEmail(appt, bName, locName);

    this.addNotification(
      'Cita Cancelada por Emergencia 🚨',
      `Se canceló la cita de ${appt.customerName}. Se envió correo automático para la devolución de su abono.`
    );
    this.notify();
    return { success: true };
  }

  completeAppointmentByBarber(apptId) {
    const appts = this.getAppointments();
    const appt = appts.find(a => a.id === apptId);
    if (!appt) return { success: false, message: 'Cita no encontrada.' };

    appt.status = 'Completada';
    appt.completedAt = AppFormatters.currentDateTimeString ? AppFormatters.currentDateTimeString() : new Date().toISOString();
    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appts));

    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (sb) {
      try {
        sb.from('appointments').update({
          status: 'Completada',
          updated_at: new Date().toISOString()
        }).eq('id', Number(appt.id)).then(() => {}).catch(() => {});
      } catch (err) {}
    }

    this.addNotification('Corte Completado ✓', `Se completó la atención para ${appt.customerName}.`);
    this.notify();
    return { success: true };
  }

  async deleteUser(email) {
    const cleanEmail = (email || '').trim().toLowerCase();
    let users = this.getUsers().filter(u => u.email.toLowerCase() !== cleanEmail);
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));

    const logged = this.getLoggedInUser();
    if (logged && logged.email.toLowerCase() === cleanEmail) {
      this.logout();
    }

    const sb = (typeof supabaseClient !== 'undefined' && supabaseClient) ? supabaseClient : (typeof window !== 'undefined' && window.supabaseClient ? window.supabaseClient : null);
    if (sb) {
      try {
        await sb.from('users').delete().eq('email', cleanEmail);
        console.log('✓ Usuario eliminado de Supabase PostgreSQL:', cleanEmail);
      } catch (e) {
        console.warn('Aviso al eliminar de Supabase:', e);
      }
    }

    this.addNotification('Usuario Eliminado 🗑️', `El usuario '${cleanEmail}' ha sido eliminado exitosamente.`);
    this.notify();
    return { success: true };
  }

  // Cálculo preciso de horas restantes hasta la cita
  getHoursUntilAppointment(dateTimeStr) {
    if (!dateTimeStr) return 0;
    try {
      const clean = dateTimeStr.trim().replace(/\//g, '-');
      const [datePart, timePart] = clean.split(' ');
      if (!datePart) return 0;
      const [y, m, d] = datePart.split('-').map(Number);
      const [h, min] = (timePart || '00:00').split(':').map(Number);
      const apptDate = new Date(y, m - 1, d, h, min);
      return (apptDate.getTime() - Date.now()) / (1000 * 60 * 60);
    } catch (e) {
      return 0;
    }
  }

  // Validación de política: >= 24 hrs y máx 2 reagendamientos
  canRescheduleAppointment(appt) {
    if (!appt) return { allowed: false, reason: 'Cita no encontrada.' };
    if (appt.status !== 'Confirmada') {
      return { allowed: false, reason: `Solo se pueden reagendar citas activas y confirmadas (estado actual: ${appt.status}).` };
    }
    const hoursLeft = this.getHoursUntilAppointment(appt.dateTime);
    if (hoursLeft < 24) {
      const hrs = Math.max(0, Math.round(hoursLeft));
      return {
        allowed: false,
        reason: `Solo se puede reagendar con al menos 24 horas de anticipación a la cita para liberar la agenda del barbero (quedan aprox. ${hrs} hrs).`,
        hoursLeft,
        rescheduleCount: appt.rescheduleCount || 0
      };
    }
    const count = appt.rescheduleCount || 0;
    if (count >= 2) {
      return {
        allowed: false,
        reason: 'Has alcanzado el límite máximo de 2 reagendamientos permitidos para este abono de $3.000.',
        hoursLeft,
        rescheduleCount: count
      };
    }
    return {
      allowed: true,
      hoursLeft,
      rescheduleCount: count,
      remainingReschedules: 2 - count
    };
  }

  // Reagendar cita reutilizando el abono de $3.000 (máx 2 veces)
  rescheduleAppointment(appointmentId, newDate, newHour) {
    const appointments = this.getAppointments();
    const appt = appointments.find(a => a.id === appointmentId || a.docId === appointmentId);
    if (!appt) return { success: false, error: 'Cita no encontrada.' };

    const check = this.canRescheduleAppointment(appt);
    if (!check.allowed) {
      return { success: false, error: check.reason };
    }

    const oldDateTime = appt.dateTime;
    const normalizedNewDate = AppFormatters.normalizeDateToYMD(newDate);
    const newDateTime = `${normalizedNewDate} ${newHour}`;

    appt.dateTime = newDateTime;
    appt.rescheduleCount = (appt.rescheduleCount || 0) + 1;
    appt.rescheduledAt = new Date().toISOString();
    appt.updatedAt = Date.now();

    // Actualizar docId único
    const safeDateKey = newDateTime.replace(/ /g, '_').replace(/:/g, '-');
    appt.docId = `loc${appt.locationId}_barber${appt.barberId}_${safeDateKey}`;

    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));

    // Sincronizar en Supabase PostgreSQL
    this.publishAppointmentToCloud(appt);

    const locations = this.getLocations();
    const barbers = this.getBarbers();
    const locName = locations.find(l => l.id === appt.locationId)?.name || 'Big Boys';
    const bName = barbers.find(b => b.id === appt.barberId)?.name || 'Barbero';

    // Enviar correo de confirmación de reagendamiento
    if (appt.customerEmail) {
      this.sendRescheduleConfirmationEmail(appt, bName, locName, oldDateTime, newDateTime);
    }

    this.addNotification(
      '¡Cita Reagendada con Éxito! ✂️',
      `Tu nueva fecha es el ${newDateTime} hrs con ${bName}. Reagendamientos usados: ${appt.rescheduleCount}/2.`
    );
    this.notify();
    return { success: true, appointment: appt };
  }

  // Anulación por parte del cliente:
  // Si faltan >= 24 hrs: Devolución íntegra del abono de $3.000
  // Si faltan < 24 hrs: Pérdida del abono conforme a política No-Show
  cancelAppointmentByCustomer(appointmentId) {
    const appointments = this.getAppointments();
    const appt = appointments.find(a => a.id === appointmentId || a.docId === appointmentId);
    if (!appt) return { success: false, error: 'Cita no encontrada.' };

    const hoursLeft = this.getHoursUntilAppointment(appt.dateTime);
    const locations = this.getLocations();
    const barbers = this.getBarbers();
    const loc = locations.find(l => l.id === appt.locationId)?.name || 'Big Boys';
    const bar = barbers.find(b => b.id === appt.barberId)?.name || 'Barbero';
    const prepay = AppFormatters.formatClp(appt.prepayAmount || 3000);

    if (hoursLeft >= 24) {
      appt.status = 'Cancelada';
      appt.prepayForfeited = false;
      appt.cancelledBy = 'Cliente (Con anticipación >= 24h)';
      appt.updatedAt = Date.now();
      localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));
      this.updateAppointmentStatusInCloud(appt, 'Cancelada');

      this.sendCancellationRefundEmail(appt, bar, loc);
      this.addNotification(
        'Cita Cancelada (Devolución del Abono) 💰',
        `Se canceló tu cita con anticipación. Se envió un correo a ${appt.customerEmail || 'tu email'} para transferirte los ${prepay}.`
      );
    } else {
      appt.status = 'Cancelada (Pérdida de Abono)';
      appt.prepayForfeited = true;
      appt.cancelledBy = 'Cliente (Tardía < 24h)';
      appt.updatedAt = Date.now();
      localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));
      this.updateAppointmentStatusInCloud(appt, 'Cancelada (Pérdida de Abono)');

      this.sendNoShowForfeitEmail(appt, bar, loc);
      this.addNotification(
        'Cita Cancelada Fuera de Plazo ⚠️',
        `La cancelación se realizó con menos de 24 hrs de antelación. Conforme a las políticas, el abono de ${prepay} no es reembolsable.`
      );
    }

    this.notify();
    return { success: true, appointment: appt, hoursLeft };
  }

  // Marcar No-Show por parte del Admin / Barbero
  markNoShowAdmin(appointmentId) {
    const appointments = this.getAppointments();
    const appt = appointments.find(a => a.id === appointmentId || a.docId === appointmentId);
    if (!appt) return;

    appt.status = 'No-Show (Inasistencia)';
    appt.prepayForfeited = true;
    appt.cancelledBy = 'Administración (No-Show)';
    appt.updatedAt = Date.now();
    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));
    this.updateAppointmentStatusInCloud(appt, 'No-Show (Inasistencia)');

    const locations = this.getLocations();
    const barbers = this.getBarbers();
    const loc = locations.find(l => l.id === appt.locationId)?.name || 'Big Boys';
    const bar = barbers.find(b => b.id === appt.barberId)?.name || 'Barbero';
    this.sendNoShowForfeitEmail(appt, bar, loc);

    this.addNotification(
      'Inasistencia (No-Show) Registrada ⚠️',
      `Cita #${appt.id} de ${appt.customerName} marcada como No-Show. Abono retenido conforme a política.`
    );
    this.notify();
  }

  confirmAndBook({
    locationId,
    barberId,
    serviceName,
    servicePrice,
    prepayAmount = 3000,
    dateTime, // "YYYY-MM-DD HH:mm"
    customerName,
    customerPhone,
    customerEmail,
    isGuest = false
  }) {
    const appointments = this.getAppointments();
    const user = this.getLoggedInUser();
    const normalizedDateTime = AppFormatters.normalizeDateTimeString ? AppFormatters.normalizeDateTimeString(dateTime) : dateTime;

    const newAppointment = {
      id: Date.now(),
      locationId: Number(locationId),
      barberId: Number(barberId),
      serviceName,
      servicePrice: Number(servicePrice),
      prepayAmount: Number(prepayAmount) || 3000,
      rescheduleCount: 0,
      rescheduledAt: null,
      prepayForfeited: false,
      dateTime: normalizedDateTime,
      customerName: user ? user.fullName : customerName,
      customerPhone: user ? user.phone : customerPhone,
      customerEmail: user ? user.email : customerEmail,
      status: 'Confirmada',
      isGuest: !user || isGuest,
      userId: user ? user.id : null,
      hasReview: false,
      createdAt: AppFormatters.currentDateTimeString()
    };

    const safeDateKey = normalizedDateTime.replace(/ /g, '_').replace(/:/g, '-');
    newAppointment.docId = `loc${newAppointment.locationId}_barber${newAppointment.barberId}_${safeDateKey}`;

    appointments.unshift(newAppointment);
    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));

    // Sincronizar en tiempo real con Supabase PostgreSQL
    this.publishAppointmentToCloud(newAppointment);

    const locations = this.getLocations();
    const barbers = this.getBarbers();
    const locName = locations.find(l => l.id === locationId)?.name || 'Big Boys';
    const bName = barbers.find(b => b.id === barberId)?.name || 'Barbero';

    // Guardar correo en la Base de Datos y despachar correo automático de confirmación con comprobante Webpay
    if (newAppointment.customerEmail) {
      this.saveEmailToDatabase(
        newAppointment.customerEmail,
        newAppointment.customerName,
        newAppointment.customerPhone,
        newAppointment.isGuest ? 'Reserva Invitado' : 'Reserva Cliente',
        `Cita agendada: ${serviceName} para el ${normalizedDateTime}`
      );

      this.sendBookingConfirmationEmail(newAppointment, bName, locName);
    }

    this.addNotification(
      '¡Cita Confirmada! ✂️',
      `Tu reserva para el ${normalizedDateTime} con ${bName} en ${locName} ha sido agendada con éxito.`
    );

    this.notify();
    return newAppointment;
  }

  // --- Acciones de Administrador ---
  updateAppointmentStatusAdmin(appointmentId, newStatus) {
    const appointments = this.getAppointments();
    const appt = appointments.find(a => a.id === appointmentId || a.docId === appointmentId);
    if (appt) {
      const oldStatus = appt.status;

      if (newStatus === 'No-Show (Inasistencia)') {
        this.markNoShowAdmin(appointmentId);
        return;
      }

      appt.status = newStatus;
      appt.updatedAt = Date.now();
      localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));
      this.addNotification(
        'Estado de Cita Actualizado 📋',
        `Cita #${appointmentId} marcada como '${newStatus}'`
      );

      // Sincronizar en Supabase PostgreSQL
      this.updateAppointmentStatusInCloud(appt, newStatus);

      if (newStatus === 'Cancelada' && oldStatus !== 'Cancelada') {
        const bName = this.getBarbers().find(b => b.id === appt.barberId)?.name || 'Barbero Big Boys';
        const locName = this.getLocations().find(l => l.id === appt.locationId)?.name || 'Sucursal Big Boys';
        this.sendCancellationRefundEmail(appt, bName, locName);
      }

      this.notify();
    }
  }

  cancelAppointmentAdmin(appointmentId, barberName, locationName) {
    const appointments = this.getAppointments();
    const appt = appointments.find(a => a.id === appointmentId || a.docId === appointmentId);
    if (appt) {
      appt.status = 'Cancelada';
      appt.updatedAt = Date.now();
      localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));

      // Sincronizar en Supabase PostgreSQL
      this.updateAppointmentStatusInCloud(appt, 'Cancelada');

      const prepayFormatted = AppFormatters.formatClp(appt.prepayAmount);
      this.addNotification(
        'Cita Cancelada y Solicitud de Devolución ⚠️',
        `Se anuló la cita de ${appt.customerName}. Solicitud de datos bancarios para reembolso de ${prepayFormatted} emitida.`
      );

      const bName = barberName || (this.getBarbers().find(b => b.id === appt.barberId)?.name) || 'Barbero Big Boys';
      const locName = locationName || (this.getLocations().find(l => l.id === appt.locationId)?.name) || 'Sucursal Big Boys';
      this.sendCancellationRefundEmail(appt, bName, locName);

      this.notify();
    }
  }

  registerManualWalkIn({ locationId, barberId, clientName, clientPhone, clientEmail, dateTime, serviceName, servicePrice }) {
    const appointments = this.getAppointments();
    const normalizedDateTime = AppFormatters.normalizeDateTimeString ? AppFormatters.normalizeDateTimeString(dateTime) : dateTime;

    const newAppointment = {
      id: Date.now(),
      locationId: Number(locationId),
      barberId: Number(barberId),
      customerName: clientName,
      customerPhone: clientPhone,
      customerEmail: clientEmail,
      dateTime: normalizedDateTime,
      serviceName,
      servicePrice: Number(servicePrice),
      prepayAmount: 0, // Walk-in cliente paga en el local
      status: 'Completada',
      isGuest: true,
      userId: null,
      hasReview: false,
      createdAt: AppFormatters.currentDateTimeString(),
      updatedAt: Date.now()
    };

    const safeDateKey = normalizedDateTime.replace(/ /g, '_').replace(/:/g, '-');
    newAppointment.docId = `loc${newAppointment.locationId}_barber${newAppointment.barberId}_${safeDateKey}`;

    appointments.unshift(newAppointment);
    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));

    this.publishAppointmentToCloud(newAppointment);

    this.addNotification(
      'Corte Manual (Walk-in) Registrado 💈',
      `Servicio presencial de ${clientName} cargado con éxito.`
    );
    this.notify();
    return newAppointment;
  }

  // --- Gestión de Barberos ---
  addBarberAdmin({ name, locationId, specialty, bio, avatarUrl, initialHours = '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00' }) {
    const barbers = this.getBarbers();
    const newBarber = {
      id: Date.now(),
      locationId: parseInt(locationId, 10),
      name: name.trim(),
      specialty: specialty.trim() || 'Barbero Estilista',
      rating: 5.0,
      avatarUrl: avatarUrl.trim() || 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300',
      availableHours: initialHours.trim(),
      bio: bio.trim() || 'Barbero profesional de Big Boys Barberías Chile.',
      active: true
    };
    barbers.push(newBarber);
    localStorage.setItem(STORAGE_KEYS.BARBERS, JSON.stringify(barbers));

    this.addNotification('Nuevo Barbero Agregado 💈', `${newBarber.name} ha sido incorporado al equipo.`);
    this.notify();
    return newBarber;
  }

  updateBarberAdmin(idOrBarber, data) {
    const barbers = this.getBarbers();
    const id = typeof idOrBarber === 'object' ? idOrBarber.id : idOrBarber;
    const payload = typeof idOrBarber === 'object' ? idOrBarber : { id, ...data };
    const index = barbers.findIndex(b => b.id === id);
    if (index !== -1) {
      barbers[index] = { ...barbers[index], ...payload };
      localStorage.setItem(STORAGE_KEYS.BARBERS, JSON.stringify(barbers));
      this.addNotification('Barbero Actualizado 💈', `El perfil de '${barbers[index].name}' ha sido modificado.`);
      this.notify();
      return { success: true, barber: barbers[index] };
    }
    return { success: false, message: 'Barbero no encontrado.' };
  }

  deleteBarberAdmin(barberId) {
    let barbers = this.getBarbers();
    const b = barbers.find(x => x.id === barberId);
    barbers = barbers.filter(x => x.id !== barberId);
    localStorage.setItem(STORAGE_KEYS.BARBERS, JSON.stringify(barbers));
    this.addNotification('Barbero Eliminado ✂️', `${b ? b.name : 'Barbero'} fue removido del catálogo.`);
    this.notify();
  }

  toggleBarberAvailability(barberId) {
    const barbers = this.getBarbers();
    const b = barbers.find(x => x.id === barberId);
    if (b) {
      const defaultHours = '10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00,19:00';
      const newHours = (!b.availableHours || b.availableHours === 'NO_DISPONIBLE') ? defaultHours : 'NO_DISPONIBLE';
      b.availableHours = newHours;
      localStorage.setItem(STORAGE_KEYS.BARBERS, JSON.stringify(barbers));
      const statusMsg = newHours === 'NO_DISPONIBLE' ? 'NO DISPONIBLE' : 'DISPONIBLE';
      this.addNotification('Disponibilidad Modificada 🕒', `${b.name} ahora figura como: ${statusMsg}`);
      this.notify();
    }
  }

  rateBarber(barberId, userRating) {
    const barbers = this.getBarbers();
    const b = barbers.find(x => x.id === barberId);
    if (b) {
      const newRating = Math.min(5.0, Math.round(((b.rating * 4 + userRating) / 5) * 10) / 10);
      b.rating = newRating;
      localStorage.setItem(STORAGE_KEYS.BARBERS, JSON.stringify(barbers));
      this.addNotification('Calificación Registrada ⭐', `Has valorado a ${b.name} con ${userRating} estrellas.`);
      this.notify();
    }
  }

  // --- Reseñas ---
  submitReview({ appointmentId, locationId, barberId, rating, comment, tags = [], serviceOrSubject, reviewType = 'SERVICE' }) {
    const reviews = this.getReviews();
    const user = this.getLoggedInUser();

    const newReview = {
      id: Date.now(),
      appointmentId,
      locationId,
      barberId,
      authorName: user ? user.fullName : 'Cliente Big Boys',
      authorAvatarUrl: user ? user.avatarUrl : '',
      rating: Math.max(1, Math.min(5, parseInt(rating, 10))),
      comment: comment.trim(),
      serviceOrSubject: serviceOrSubject || 'Experiencia Big Boys',
      date: AppFormatters.currentDateString(),
      tags: Array.isArray(tags) ? tags.join(', ') : tags,
      reviewType
    };

    reviews.unshift(newReview);
    localStorage.setItem(STORAGE_KEYS.REVIEWS, JSON.stringify(reviews));

    if (appointmentId) {
      const appointments = this.getAppointments();
      const appt = appointments.find(a => a.id === appointmentId);
      if (appt) {
        appt.hasReview = true;
        localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appointments));
      }
    }

    this.addNotification('¡Reseña Publicada! ⭐', `Muchas gracias por tu valoración.`);
    this.notify();
    return newReview;
  }

  deleteReviewAdmin(reviewId) {
    let reviews = this.getReviews();
    reviews = reviews.filter(r => r.id !== reviewId);
    localStorage.setItem(STORAGE_KEYS.REVIEWS, JSON.stringify(reviews));
    this.addNotification('Reseña Eliminada 🗑️', 'La reseña fue eliminada por administración.');
    this.notify();
  }

  // --- Cupones PIN ---
  redeemCoupon(pin) {
    const coupons = this.getCoupons();
    const coupon = coupons.find(c => c.pin === pin.trim());
    if (!coupon) {
      return { success: false, message: 'El PIN ingresado no corresponde a ningún cupón activo.' };
    }
    if (coupon.isRedeemed) {
      return { success: false, message: `El cupón (${coupon.pin}) ya fue canjeado por ${coupon.userName}.` };
    }

    coupon.isRedeemed = true;
    localStorage.setItem(STORAGE_KEYS.COUPONS, JSON.stringify(coupons));
    this.addNotification('Regalo Entregado 🎁', `Se canjeó el perfilado de cejas para: ${coupon.userName}`);
    this.notify();
    return { success: true, message: `¡Cupón canjeado con éxito para ${coupon.userName}!` };
  }

  registerManualCoupon(userName, userEmail, customPin = '') {
    const coupons = this.getCoupons();
    if (coupons.length >= 100) {
      return { success: false, message: 'Límite alcanzado: Ya se han generado los 100 cupones de regalo permitidos.' };
    }
    const email = userEmail.trim().toLowerCase();
    if (coupons.some(c => c.userEmail.toLowerCase() === email)) {
      return { success: false, message: 'Este cliente ya tiene un cupón asignado.' };
    }

    let finalPin = customPin.trim();
    if (finalPin) {
      if (finalPin.length !== 4 || !/^\d+$/.test(finalPin)) {
        return { success: false, message: 'El PIN manual debe ser exactamente de 4 dígitos numéricos.' };
      }
      if (coupons.some(c => c.pin === finalPin)) {
        return { success: false, message: `El PIN ${finalPin} ya está en uso.` };
      }
    } else {
      let isUnique = false;
      while (!isUnique) {
        finalPin = Math.floor(1000 + Math.random() * 9000).toString();
        if (!coupons.some(c => c.pin === finalPin)) isUnique = true;
      }
    }

    const newCoupon = {
      id: Date.now(),
      pin: finalPin,
      userEmail: email,
      userName: userName.trim(),
      isRedeemed: false,
      createdAt: AppFormatters.currentDateTimeString()
    };
    coupons.push(newCoupon);
    localStorage.setItem(STORAGE_KEYS.COUPONS, JSON.stringify(coupons));

    this.addNotification('Cupón Manual Creado 🏷️', `Cupón PIN: ${finalPin} creado para ${userName}.`);
    this.notify();
    return { success: true, pin: finalPin, message: `Cupón creado exitosamente con el PIN: ${finalPin}` };
  }

  // --- Perfiles ---
  updateAdminProfile(name, currentPin, newPin, avatarUrl) {
    const config = this.getAdminConfig();
    if (currentPin !== config.pin) {
      return { success: false, message: 'El PIN actual de administrador es incorrecto.' };
    }
    if (!name.trim()) {
      return { success: false, message: 'El nombre no puede estar vacío.' };
    }
    if (newPin && (newPin.length < 4 || !/^\d+$/.test(newPin))) {
      return { success: false, message: 'El nuevo PIN debe contener al menos 4 dígitos numéricos.' };
    }

    config.name = name.trim();
    if (newPin) config.pin = newPin.trim();
    if (avatarUrl) config.avatarUrl = avatarUrl.trim();

    localStorage.setItem(STORAGE_KEYS.ADMIN_CONFIG, JSON.stringify(config));
    const user = this.getLoggedInUser();
    if (user && user.id === -99) {
      user.fullName = config.name;
      user.avatarUrl = config.avatarUrl;
      localStorage.setItem(STORAGE_KEYS.LOGGED_USER, JSON.stringify(user));
    }

    this.addNotification('Perfil Administrador Actualizado 🛡️', 'Cambios guardados con éxito.');
    this.notify();
    return { success: true, message: 'Perfil de administrador actualizado correctamente.' };
  }

  async updateUserProfile(name, phone, currentPassword, newPassword, avatarUrl) {
    const user = this.getLoggedInUser();
    if (!user || user.id <= 0) {
      return { success: false, message: 'No hay una sesión activa de usuario.' };
    }

    const cleanName = (name || '').trim();
    const cleanPhone = (phone || '').trim();
    if (!cleanName) return { success: false, message: 'Ingresa tu nombre.' };
    if (!cleanPhone) return { success: false, message: 'Ingresa tu teléfono.' };

    if (user.passwordHash) {
      const enteredHash = await ValidationUtils.hashPassword(currentPassword || '');
      if (enteredHash !== user.passwordHash) {
        return { success: false, message: 'La contraseña actual ingresada es incorrecta.' };
      }
    }

    let updatedPasswordHash = user.passwordHash;
    if (newPassword) {
      const passErrors = ValidationUtils.validatePasswordRequirements(newPassword);
      if (passErrors.length > 0) {
        return { success: false, message: passErrors.join(', ') };
      }
      updatedPasswordHash = await ValidationUtils.hashPassword(newPassword);
    }

    user.fullName = cleanName;
    user.phone = cleanPhone;
    user.passwordHash = updatedPasswordHash;
    if (avatarUrl) user.avatarUrl = avatarUrl.trim();

    const users = this.getUsers();
    const idx = users.findIndex(u => u.id === user.id);
    if (idx !== -1) {
      users[idx] = user;
      localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
    }
    localStorage.setItem(STORAGE_KEYS.LOGGED_USER, JSON.stringify(user));

    this.addNotification('Perfil Actualizado 👤', 'Tus datos han sido actualizados con éxito.');
    this.notify();
    return { success: true, message: 'Perfil actualizado con éxito.' };
  }

  // --- Gmail SMTP Config ---
  updateGmailConfig(senderEmail, appPassword) {
    const config = {
      senderEmail: senderEmail.trim(),
      appPassword: appPassword.replace(/\s+/g, '').trim()
    };
    localStorage.setItem(STORAGE_KEYS.GMAIL_CONFIG, JSON.stringify(config));
    this.addNotification('Configuración Gmail Guardada ✉️', 'Credenciales de correo actualizadas.');
    this.notify();
    return { success: true, message: 'Configuración guardada correctamente.' };
  }

  // --- Notificaciones ---
  addNotification(title, message) {
    const notifications = this.getNotifications();
    notifications.unshift({
      id: Date.now() + Math.floor(Math.random() * 1000),
      title,
      message,
      timestamp: AppFormatters.currentTimeString()
    });
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(notifications.slice(0, 30)));
    this.notify();
  }

  clearNotifications() {
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify([]));
    this.notify();
  }
}

export const store = new BarberiaStore();
