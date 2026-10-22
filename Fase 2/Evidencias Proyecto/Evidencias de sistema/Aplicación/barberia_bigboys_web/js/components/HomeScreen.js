/**
 * HomeScreen Component - Vista Principal Oficial de Big Boys
 * Hero banner, cupón PIN dorado de regalo, resumen de sucursales y citas activas del cliente
 */

import { AppFormatters } from '../utils/formatters.js';

export function HomeScreen({
  user,
  coupon,
  appointments,
  locations,
  onOpenBooking,
  onOpenRegister,
  onNavigateToTab
}) {
  const [copiedPin, setCopiedPin] = React.useState(false);

  // Filtrar citas del usuario conectado (o las más recientes si es modo invitado)
  const userAppointments = React.useMemo(() => {
    if (!user) return [];
    return appointments.filter(a => a.userId === user.id);
  }, [appointments, user]);

  const handleCopyPin = (pin) => {
    navigator.clipboard.writeText(pin);
    setCopiedPin(true);
    setTimeout(() => setCopiedPin(false), 2000);
  };

  const openGoogleMaps = (locOrLat, maybeLng, label) => {
    let query = '';
    if (typeof locOrLat === 'object' && locOrLat !== null) {
      query = encodeURIComponent(`${locOrLat.name}, ${locOrLat.address}`);
    } else if (label) {
      query = encodeURIComponent(`${label}, ${locOrLat},${maybeLng}`);
    } else {
      query = `${locOrLat},${maybeLng}`;
    }
    const url = `https://www.google.com/maps/search/?api=1&query=${query}`;
    window.open(url, '_blank');
  };

  return (
    <div className="space-y-6 pb-20 md:pb-12 animate-fade-in">
      {/* 1. HERO BANNER DE LUJO (Gold Primary) */}
      <div className="rounded-3xl bg-gradient-to-r from-[#FFD700] via-[#F2CA00] to-[#D4AF37] p-6 md:p-8 text-black shadow-xl relative overflow-hidden">
        {/* Barber Stripes subtle background accent */}
        <div className="absolute -right-8 -top-8 w-44 h-44 rounded-full bg-black/5 blur-2xl pointer-events-none"></div>

        <div className="flex flex-col md:flex-row justify-between items-start md:items-center relative z-10 gap-6">
          <div className="max-w-xl">
            <div className="inline-flex items-center space-x-2 px-3 py-1 bg-black/10 rounded-full text-xs font-black uppercase tracking-wider mb-3">
              <span>🇨🇱</span>
              <span>Barberías de Élite en Santiago</span>
            </div>

            <h1 className="text-2xl sm:text-3xl md:text-4xl font-black italic tracking-tight font-brand mb-2">
              {user ? `👋 ¡HOLA, ${user.fullName.toUpperCase()}!` : '💈 BIENVENIDOS A BIG BOYS'}
            </h1>

            <p className="text-xs sm:text-sm font-medium text-black/85 leading-relaxed mb-6">
              Donde el estilo se junta con la distinción. Agenda tu corte de manera ágil con prepagos garantizados
              y atención de primer nivel en nuestras sucursales de Talagante y Peñaflor.
            </p>

            <div className="flex flex-wrap gap-3">
              <button
                onClick={onOpenBooking}
                className="px-6 py-3 bg-[#121212] hover:bg-black text-[#FFD700] font-black text-xs uppercase tracking-wider rounded-xl shadow-lg hover:scale-105 active:scale-95 transition-all"
              >
                ✂️ Agendar Cita Online
              </button>

              {!user && (
                <button
                  onClick={onOpenRegister}
                  className="px-6 py-3 border-2 border-[#121212] hover:bg-[#121212]/10 text-[#121212] font-black text-xs uppercase tracking-wider rounded-xl transition-all"
                >
                  🎁 Registrarme & Recibir Regalo
                </button>
              )}
            </div>
          </div>

          {/* Barber Columns Decoration */}
          <div className="hidden lg:flex items-end space-x-2 bg-black/10 p-3 rounded-2xl">
            <div className="w-2.5 h-16 bg-black/20 rounded-full overflow-hidden flex flex-col justify-end">
              <div className="w-full h-3/4 bg-black rounded-full"></div>
            </div>
            <div className="w-2.5 h-16 bg-black/20 rounded-full overflow-hidden flex flex-col justify-end">
              <div className="w-full h-1/2 bg-black rounded-full"></div>
            </div>
            <div className="w-2.5 h-16 bg-black/20 rounded-full overflow-hidden flex flex-col justify-end">
              <div className="w-full h-full bg-black rounded-full"></div>
            </div>
          </div>
        </div>
      </div>

      {/* 2. TARJETA CUPÓN DE BIENVENIDA (Si el usuario tiene cupón activo) */}
      {user && coupon && (
        <div className={`p-5 rounded-2xl border transition-all ${
          coupon.isRedeemed
            ? 'bg-[#18181B] border-zinc-800 opacity-80'
            : 'bg-[#1E1A16] border-[#FFD700]/60 shadow-[0_0_20px_rgba(255,215,0,0.15)]'
        }`}>
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div className="flex items-center space-x-3.5">
              <div className={`w-11 h-11 rounded-full flex items-center justify-center text-xl ${
                coupon.isRedeemed ? 'bg-zinc-800 text-zinc-400' : 'bg-[#FFD700]/20 text-[#FFD700] border border-[#FFD700]/40'
              }`}>
                {coupon.isRedeemed ? '✓' : '🎁'}
              </div>
              <div>
                <span className={`text-[11px] font-black tracking-wider uppercase ${
                  coupon.isRedeemed ? 'text-zinc-500' : 'text-[#FFD700]'
                }`}>
                  {coupon.isRedeemed ? 'REGALO CANJEADO 🏅' : '¡TU REGALO DE BIENVENIDA! 🎁'}
                </span>
                <h3 className={`text-base font-extrabold ${coupon.isRedeemed ? 'text-zinc-400' : 'text-white'}`}>
                  1x Perfilado de Cejas Profesional Gratis
                </h3>
              </div>
            </div>

            <div className="flex items-center space-x-3 w-full sm:w-auto justify-between sm:justify-end">
              <div className="text-right">
                <span className="text-[10px] text-zinc-400 block">CÓDIGO DE CANJE:</span>
                <span className={`text-2xl font-black font-mono tracking-widest ${
                  coupon.isRedeemed ? 'text-zinc-500' : 'text-[#FFD700]'
                }`}>
                  PIN: {coupon.pin}
                </span>
              </div>

              {!coupon.isRedeemed && (
                <button
                  onClick={() => handleCopyPin(coupon.pin)}
                  className="px-3 py-1.5 bg-[#FFD700]/15 hover:bg-[#FFD700]/25 text-[#FFD700] border border-[#FFD700]/40 text-xs font-bold rounded-lg transition-colors"
                >
                  {copiedPin ? '✓ Copiado' : 'Copiar'}
                </button>
              )}

              <span className={`px-2.5 py-1 text-[10px] font-black uppercase rounded-md ${
                coupon.isRedeemed
                  ? 'bg-zinc-800 text-zinc-400'
                  : 'bg-[#FFD700]/20 text-[#FFD700] border border-[#FFD700]/40'
              }`}>
                {coupon.isRedeemed ? 'CANJEADO' : 'DISPONIBLE'}
              </span>
            </div>
          </div>

          <p className="text-xs text-zinc-400 mt-3 pt-3 border-t border-zinc-800">
            {coupon.isRedeemed
              ? `Este cupón ya fue validado en caja el ${coupon.createdAt}. ¡Esperamos que disfrutes de tu estilo Big Boys!`
              : 'Muestra este PIN exclusivo de 4 dígitos a tu barbero al momento de tu primer servicio para activar tu perfilado de cejas de regalo.'}
          </p>
        </div>
      )}

      {/* 3. RESUMEN SUCURSALES (Carrusel con enlace a Google Maps) */}
      <div>
        <div className="flex justify-between items-center mb-3">
          <div className="flex items-center space-x-2">
            <span className="text-xs font-black tracking-widest text-zinc-400 uppercase">
              SUCURSALES CHILE (MAPA)
            </span>
            <span className="text-xs">🇨🇱</span>
          </div>

          <button
            onClick={() => onNavigateToTab('sucursales')}
            className="text-xs font-bold text-[#FFD700] hover:underline flex items-center space-x-1"
          >
            <span>Ver en Mapa Interactivo</span>
            <span>🗺️</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {locations.map(loc => (
            <div
              key={loc.id}
              className="bg-[#1E1E1E] border border-zinc-800 hover:border-[#FFD700]/50 rounded-2xl p-4 transition-all hover:shadow-lg group"
            >
              <div className="flex justify-between items-start mb-2">
                <div className="flex items-center space-x-2">
                  <span className="text-base text-[#FFD700]">📍</span>
                  <h4 className="text-xs font-black text-white group-hover:text-[#FFD700] uppercase tracking-wide">
                    {loc.name.replace('Big Boys - ', '')}
                  </h4>
                </div>

                <button
                  onClick={() => openGoogleMaps(loc)}
                  title="Abrir en Google Maps"
                  className="p-1.5 bg-[#121212] hover:bg-[#FFD700] hover:text-black text-[#FFD700] rounded-lg text-xs transition-colors"
                >
                  Maps ↗
                </button>
              </div>

              <p className="text-xs text-zinc-400 line-clamp-1 mb-1">{loc.address}</p>
              <p className="text-xs text-zinc-300 font-semibold mb-3">Tel: {loc.phone}</p>

              <div className="flex justify-between items-center text-[10px] pt-2 border-t border-zinc-800">
                <span className="text-zinc-500 font-bold">4 SILLONES VIP</span>
                <button
                  onClick={() => onNavigateToTab('sucursales')}
                  className="text-[#FFD700] font-bold hover:underline"
                >
                  Conocer Sucursal &rarr;
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 4. TUS AGENDAMIENTOS ACTIVOS */}
      <div>
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-sm font-black tracking-wider uppercase text-[#FFD700]">
            Tus Agendamientos Activos
          </h3>

          {userAppointments.length > 0 && (
            <button
              onClick={() => onNavigateToTab('historial')}
              className="text-xs text-zinc-400 hover:text-white"
            >
              Ver historial completo &rarr;
            </button>
          )}
        </div>

        {!user ? (
          <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-6 text-center">
            <span className="text-3xl block mb-2">ℹ️</span>
            <p className="text-xs text-zinc-400 max-w-sm mx-auto mb-4 leading-relaxed">
              Inicia sesión para ver tu historial de agendamientos, acumular beneficios y recibir notificaciones de tus reservas.
            </p>
            <button
              onClick={onOpenRegister}
              className="px-5 py-2.5 bg-[#FFD700] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md hover:bg-[#E5C100] transition-colors"
            >
              Iniciar Sesión o Registrarme
            </button>
          </div>
        ) : userAppointments.length === 0 ? (
          <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-6 text-center">
            <span className="text-3xl block mb-2">📅</span>
            <p className="text-xs text-zinc-400 mb-4">
              Aún no tienes ninguna reserva agendada en Big Boys.
            </p>
            <button
              onClick={onOpenBooking}
              className="px-5 py-2.5 bg-[#FFD700] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md hover:bg-[#E5C100] transition-colors"
            >
              Reserva tu primera hora
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {userAppointments.map(appt => (
              <div
                key={appt.id}
                className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3"
              >
                <div>
                  <div className="flex items-center space-x-2 mb-1">
                    <span className="text-sm">💈</span>
                    <h4 className="text-xs font-bold text-white">{appt.serviceName}</h4>
                    <span className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-full ${
                      appt.status === 'Confirmada'
                        ? 'bg-green-500/15 text-green-400 border border-green-500/30'
                        : appt.status === 'Completada'
                        ? 'bg-[#FFD700]/15 text-[#FFD700] border border-[#FFD700]/30'
                        : 'bg-red-500/15 text-red-400 border border-red-500/30'
                    }`}>
                      {appt.status}
                    </span>
                  </div>

                  <p className="text-xs text-zinc-400">
                    Fecha: <strong className="text-zinc-200">{appt.dateTime} hrs</strong>
                  </p>
                  <p className="text-[11px] text-zinc-500">
                    Abono online garantizado: {AppFormatters.formatClp(appt.prepayAmount)}
                  </p>
                </div>

                <div className="text-left sm:text-right">
                  <span className="text-sm font-black text-[#FFD700]">
                    {AppFormatters.formatClp(appt.servicePrice)}
                  </span>
                  <span className="block text-[10px] text-zinc-400">Costo total servicio</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
