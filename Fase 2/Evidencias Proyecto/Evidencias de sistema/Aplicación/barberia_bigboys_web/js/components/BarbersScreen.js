/**
 * BarbersScreen Component - Catálogo Oficial de Barberos de Big Boys
 * 12 barberos oficiales, filtro por sucursal, biografía detallada, calificación por estrellas y reserva directa
 */

import { store } from '../state/store.js';

export function BarbersScreen({ barbers, locations, onBookWithBarber }) {
  const [selectedLocationId, setSelectedLocationId] = React.useState('all');
  const [activeBioBarber, setActiveBioBarber] = React.useState(null);

  const filteredBarbers = React.useMemo(() => {
    if (selectedLocationId === 'all') return barbers;
    return barbers.filter(b => b.locationId === parseInt(selectedLocationId, 10));
  }, [barbers, selectedLocationId]);

  const getLocationName = (locId) => {
    const loc = locations.find(l => l.id === locId);
    return loc ? loc.name.replace('Big Boys - ', '') : 'Sucursal';
  };

  const handleRateBarber = (barberId, rating) => {
    store.rateBarber(barberId, rating);
  };

  return (
    <div className="space-y-6 pb-20 md:pb-12 animate-fade-in">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-800 pb-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black italic tracking-tighter text-white font-brand">
            NUESTRO EQUIPO DE MAESTROS BARBEROS
          </h2>
          <p className="text-xs text-zinc-400">
            Profesionales especializados en corte clásico, fade urbano, visagismo y afeitado tradicional a navaja.
          </p>
        </div>

        <div className="flex items-center space-x-1 text-xs bg-[#1E1E1E] p-1 rounded-xl border border-zinc-800">
          <span className="px-2 font-bold text-[#FFD700]">{filteredBarbers.length}</span>
          <span className="text-zinc-400">Barberos</span>
        </div>
      </div>

      {/* Filter Chips by Branch */}
      <div className="flex space-x-2 overflow-x-auto pb-2 scrollbar-none">
        <button
          onClick={() => setSelectedLocationId('all')}
          className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${
            selectedLocationId === 'all'
              ? 'bg-[#FFD700] text-black shadow-md'
              : 'bg-[#1E1E1E] text-zinc-300 hover:border-[#FFD700]/40 border border-zinc-800'
          }`}
        >
          🌟 Todas las Sucursales
        </button>

        {locations.map(loc => {
          const isSelected = selectedLocationId === loc.id.toString();
          return (
            <button
              key={loc.id}
              onClick={() => setSelectedLocationId(loc.id.toString())}
              className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${
                isSelected
                  ? 'bg-[#FFD700] text-black shadow-md'
                  : 'bg-[#1E1E1E] text-zinc-300 hover:border-[#FFD700]/40 border border-zinc-800'
              }`}
            >
              📍 {loc.name.replace('Big Boys - ', '')}
            </button>
          );
        })}
      </div>

      {/* Barbers Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {filteredBarbers.map(barber => {
          const hoursCount = barber.availableHours === 'NO_DISPONIBLE'
            ? 0
            : barber.availableHours.split(',').length;

          const isUnavailable = barber.availableHours === 'NO_DISPONIBLE';

          return (
            <div
              key={barber.id}
              className="bg-[#1E1E1E] border border-zinc-800 hover:border-[#FFD700]/40 rounded-2xl p-5 flex flex-col justify-between transition-all hover:shadow-xl group"
            >
              <div>
                {/* Avatar with Status Indicator */}
                <div className="flex justify-center mb-4 relative">
                  <div className="relative">
                    <img
                      src={barber.avatarUrl}
                      alt={barber.name}
                      className="w-20 h-20 rounded-full object-cover border-2 border-[#FFD700] shadow-md group-hover:scale-105 transition-transform"
                    />
                    <span
                      title={isUnavailable ? 'No disponible hoy' : 'Activo y disponible'}
                      className={`absolute bottom-0 right-0 w-5 h-5 rounded-full border-2 border-[#1E1E1E] flex items-center justify-center text-[9px] ${
                        isUnavailable ? 'bg-red-500 text-white' : 'bg-green-500 text-white'
                      }`}
                    >
                      {isUnavailable ? '✕' : '✓'}
                    </span>
                  </div>
                </div>

                {/* Name & Branch */}
                <div className="text-center mb-2">
                  <h3 className="text-base font-black text-white group-hover:text-[#FFD700] transition-colors">
                    {barber.name}
                  </h3>
                  <span className="inline-block px-2 py-0.5 mt-1 bg-[#FFD700]/15 text-[#FFD700] text-[10px] font-black uppercase rounded-md tracking-wider">
                    {getLocationName(barber.locationId)}
                  </span>
                </div>

                {/* Specialty */}
                <p className="text-xs font-semibold text-zinc-300 text-center mb-2">
                  {barber.specialty}
                </p>

                {/* Bio snippet */}
                <p className="text-[11px] text-zinc-400 line-clamp-2 text-center mb-4 leading-relaxed">
                  {barber.bio}
                </p>
              </div>

              <div>
                {/* Rating & Availability Details */}
                <div className="flex justify-between items-center text-xs py-2 border-t border-zinc-800 mb-3">
                  <div className="flex items-center space-x-1">
                    <span className="text-[#FFD700]">★</span>
                    <span className="font-extrabold text-white">{barber.rating.toFixed(1)}</span>
                    <span className="text-[10px] text-zinc-500">/ 5.0</span>
                  </div>

                  <span className={`text-[10px] font-extrabold uppercase ${
                    isUnavailable ? 'text-red-400' : 'text-zinc-400'
                  }`}>
                    {isUnavailable ? 'NO DISPONIBLE HOY' : `🕒 ${hoursCount} HORARIOS`}
                  </span>
                </div>

                {/* Quick Rating Selector (1-5 stars) */}
                <div className="flex items-center justify-center space-x-1 mb-3">
                  <span className="text-[10px] text-zinc-400 mr-1">Calificar:</span>
                  {[1, 2, 3, 4, 5].map(star => (
                    <button
                      key={star}
                      onClick={() => handleRateBarber(barber.id, star)}
                      title={`Calificar con ${star} estrellas`}
                      className={`text-sm transition-transform hover:scale-125 ${
                        star <= Math.round(barber.rating) ? 'text-[#FFD700]' : 'text-zinc-600'
                      }`}
                    >
                      ★
                    </button>
                  ))}
                </div>

                {/* Action Buttons */}
                <div className="flex space-x-2">
                  <button
                    onClick={() => setActiveBioBarber(barber)}
                    className="flex-1 py-2 text-xs font-bold text-zinc-300 hover:text-white bg-zinc-800/80 hover:bg-zinc-800 rounded-xl transition-colors"
                  >
                    Ver Perfil
                  </button>

                  <button
                    disabled={isUnavailable}
                    onClick={() => onBookWithBarber(barber)}
                    className={`flex-1 py-2 text-xs font-black uppercase rounded-xl transition-all shadow-md ${
                      isUnavailable
                        ? 'bg-zinc-800 text-zinc-500 cursor-not-allowed'
                        : 'bg-[#FFD700] hover:bg-[#E5C100] text-black hover:scale-[1.02]'
                    }`}
                  >
                    Reservar
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Barber Bio Modal Dialog */}
      {activeBioBarber && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-md p-6 relative shadow-2xl">
            <button
              onClick={() => setActiveBioBarber(null)}
              className="absolute top-4 right-4 text-zinc-400 hover:text-white text-lg"
            >
              ✕
            </button>

            <div className="flex items-center space-x-4 mb-4">
              <img
                src={activeBioBarber.avatarUrl}
                alt={activeBioBarber.name}
                className="w-16 h-16 rounded-full object-cover border-2 border-[#FFD700]"
              />
              <div>
                <h3 className="text-lg font-black text-white font-brand">
                  {activeBioBarber.name}
                </h3>
                <p className="text-xs text-[#FFD700] font-bold">
                  {activeBioBarber.specialty}
                </p>
                <p className="text-[11px] text-zinc-400">
                  📍 Sucursal {getLocationName(activeBioBarber.locationId)}
                </p>
              </div>
            </div>

            <div className="bg-[#121212] p-4 rounded-xl border border-zinc-800 mb-4">
              <span className="text-[10px] font-black uppercase text-zinc-400 tracking-wider block mb-1">
                TRAYECTORIA & EXPERIENCIA:
              </span>
              <p className="text-xs text-zinc-300 leading-relaxed">
                {activeBioBarber.bio}
              </p>
            </div>

            <div className="mb-4">
              <span className="text-[10px] font-black uppercase text-zinc-400 tracking-wider block mb-2">
                HORARIOS HABITUALES DE ATENCIÓN:
              </span>
              <div className="flex flex-wrap gap-1.5">
                {activeBioBarber.availableHours === 'NO_DISPONIBLE' ? (
                  <span className="text-xs text-red-400 font-bold">No disponible temporalmente</span>
                ) : (
                  activeBioBarber.availableHours.split(',').map((hr, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-1 bg-zinc-800 text-zinc-300 text-[10px] font-mono font-bold rounded-md"
                    >
                      {hr.trim()} hrs
                    </span>
                  ))
                )}
              </div>
            </div>

            <div className="flex space-x-3 pt-2">
              <button
                onClick={() => setActiveBioBarber(null)}
                className="flex-1 py-2.5 bg-zinc-800 text-zinc-300 font-bold text-xs rounded-xl"
              >
                Cerrar
              </button>
              <button
                disabled={activeBioBarber.availableHours === 'NO_DISPONIBLE'}
                onClick={() => {
                  const b = activeBioBarber;
                  setActiveBioBarber(null);
                  onBookWithBarber(b);
                }}
                className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase rounded-xl shadow-md"
              >
                Agendar con {activeBioBarber.name.split(' ')[0]}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
