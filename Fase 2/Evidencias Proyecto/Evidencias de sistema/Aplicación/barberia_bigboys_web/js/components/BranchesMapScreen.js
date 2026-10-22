/**
 * BranchesMapScreen Component - Mapa Interactivo y Sucursales de Big Boys Chile
 * Integración Exclusiva con Google Maps API Oficial, navegación GPS y Waze.
 */

export function BranchesMapScreen({ locations, barbers, onSelectBranchForBooking, isDarkMode = true }) {
  const [activeLocId, setActiveLocId] = React.useState(1);
  const [copiedAddressId, setCopiedAddressId] = React.useState(null);

  const activeLoc = locations.find(l => l.id === activeLocId) || locations[0] || {
    id: 1,
    name: 'Big Boys - Talagante',
    address: 'Boulevard del centro, local 3, Talagante',
    lat: -33.6631,
    lng: -70.9286
  };

  const handleFocusBranch = (loc) => {
    setActiveLocId(loc.id);
  };

  // Google Maps Directions API
  const openGoogleMapsDirections = (loc) => {
    const dest = encodeURIComponent(`${loc.address}, Chile`);
    const name = encodeURIComponent(loc.name);
    window.open(`https://www.google.com/maps/dir/?api=1&destination=${dest}&destination_place_id=${name}`, '_blank');
  };

  // Google Maps Search API
  const openGoogleMapsSearch = (loc) => {
    const query = encodeURIComponent(`${loc.name}, ${loc.address}`);
    window.open(`https://www.google.com/maps/search/?api=1&query=${query}`, '_blank');
  };

  // Waze Navigation Link
  const openWaze = (loc) => {
    const query = encodeURIComponent(`${loc.address}, Chile`);
    window.open(`https://waze.com/ul?q=${query}&navigate=yes`, '_blank');
  };

  const handleCopyAddress = (loc) => {
    navigator.clipboard.writeText(loc.address);
    setCopiedAddressId(loc.id);
    setTimeout(() => setCopiedAddressId(null), 2000);
  };

  const googleMapsEmbedUrl = `https://maps.google.com/maps?q=${encodeURIComponent(activeLoc.address + ', Chile')}&t=&z=16&ie=UTF8&iwloc=&output=embed`;

  return (
    <div className="space-y-6 pb-20 md:pb-12 animate-fade-in">
      {/* Header */}
      <div className="border-b border-zinc-800 pb-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-xl">📍</span>
              <h2 className="text-xl sm:text-2xl font-black italic tracking-tighter text-white font-brand">
                RED DE SUCURSALES BIG BOYS · CHILE
              </h2>
            </div>
            <p className="text-xs text-zinc-400 mt-1">
              Direcciones oficiales en Peñaflor y Talagante con visor satelital e interactivo de Google Maps API.
            </p>
          </div>

          {/* Badge Google Maps API */}
          <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 bg-[#1E1E1E] border border-[#FFD700]/40 rounded-xl text-xs font-bold text-[#FFD700] self-start sm:self-auto shadow-md">
            <span className="w-2 h-2 rounded-full bg-[#FFD700] animate-pulse"></span>
            <span>🗺️ Google Maps API Oficial</span>
          </div>
        </div>

        {/* Selector Rápido de Sucursal */}
        <div className="flex flex-wrap gap-2 mt-4 pt-3 border-t border-zinc-800/60">
          {locations.map(loc => {
            const isActive = activeLocId === loc.id;
            return (
              <button
                key={loc.id}
                onClick={() => handleFocusBranch(loc)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 ${
                  isActive
                    ? 'bg-[#FFD700] text-black font-black shadow-lg scale-102 ring-2 ring-[#FFD700]/50'
                    : 'bg-[#181818] text-zinc-400 border border-zinc-800 hover:border-zinc-700 hover:text-white'
                }`}
              >
                <span>💈</span>
                <span>{loc.name.replace('Big Boys - ', '')}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Visor de Mapa Exclusivo con Google Maps Embed */}
      <div className="relative rounded-3xl overflow-hidden border-2 border-[#FFD700]/40 shadow-2xl bg-[#121212]">
        <div className="w-full relative" style={{ minHeight: '420px', height: '420px' }}>
          <iframe
            title={`Google Maps - ${activeLoc.name}`}
            width="100%"
            height="100%"
            style={{ border: 0 }}
            loading="lazy"
            allowFullScreen
            referrerPolicy="no-referrer-when-downgrade"
            src={googleMapsEmbedUrl}
            className="w-full h-full"
          ></iframe>

          {/* Badge flotante */}
          <div className="absolute top-3 left-3 z-20 bg-[#121212]/95 backdrop-blur-md px-3.5 py-2 rounded-xl border border-[#FFD700]/50 text-xs shadow-lg flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#FFD700] animate-ping"></span>
            <div className="text-left">
              <span className="font-extrabold text-white block text-[11px]">{activeLoc.name}</span>
              <span className="text-[10px] text-zinc-400 block">{activeLoc.address}</span>
            </div>
          </div>

          <div className="absolute bottom-3 right-3 z-20 flex gap-2">
            <button
              onClick={() => openGoogleMapsDirections(activeLoc)}
              className="px-3.5 py-2 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs rounded-xl shadow-lg flex items-center space-x-1.5 transition-all active:scale-95"
            >
              <span>🧭</span>
              <span>Cómo Llegar (Google Maps)</span>
            </button>
          </div>
        </div>
      </div>

      {/* Cards Detalladas de Sucursales */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {locations.map(loc => {
          const locBarbers = barbers.filter(b => b.locationId === loc.id);
          const isSelected = activeLocId === loc.id;
          const isCopied = copiedAddressId === loc.id;

          return (
            <div
              key={loc.id}
              onClick={() => handleFocusBranch(loc)}
              className={`p-5 rounded-2xl border transition-all cursor-pointer flex flex-col justify-between ${
                isSelected
                  ? 'bg-[#1E1A16] border-[#FFD700] shadow-[0_0_25px_rgba(255,215,0,0.2)] ring-1 ring-[#FFD700]/40'
                  : 'bg-[#1E1E1E] border-zinc-800 hover:border-zinc-700'
              }`}
            >
              <div>
                <div className="flex justify-between items-start mb-2">
                  <span className="px-2.5 py-1 bg-[#FFD700]/15 text-[#FFD700] text-[10px] font-black uppercase rounded-lg">
                    {loc.isPrimary ? 'SUCURSAL CENTRAL' : 'SUCURSAL REGIONAL'}
                  </span>
                  <span className="text-xs font-bold text-zinc-400">4 Sillones VIP</span>
                </div>

                <h3 className="text-base font-black text-white mb-1">
                  {loc.name}
                </h3>

                <div className="flex items-start justify-between gap-2 mb-3 bg-[#141414] p-2.5 rounded-xl border border-zinc-800/80">
                  <p className="text-xs text-zinc-300 font-medium">
                    📍 {loc.address}
                  </p>
                  <button
                    type="button"
                    onClick={(e) => { e.stopPropagation(); handleCopyAddress(loc); }}
                    title="Copiar dirección"
                    className="text-[10px] px-2 py-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 rounded font-bold shrink-0 transition-colors"
                  >
                    {isCopied ? '✓' : 'Copiar'}
                  </button>
                </div>

                <div className="space-y-1.5 text-xs text-zinc-300 py-3 border-y border-zinc-800/80 mb-3">
                  <p className="flex items-center space-x-2">
                    <span>🕒</span>
                    <span className="text-[11px] text-zinc-400">{loc.schedule}</span>
                  </p>
                  <p className="flex items-center space-x-2">
                    <span>📞</span>
                    <a href={`tel:${loc.phone}`} onClick={(e) => e.stopPropagation()} className="text-[11px] font-bold text-[#FFD700] hover:underline">
                      {loc.phone}
                    </a>
                  </p>
                  <p className="flex items-center space-x-2">
                    <span>💈</span>
                    <span className="text-[11px] text-[#FFD700] font-bold">
                      {locBarbers.length} barberos disponibles
                    </span>
                  </p>
                </div>

                <p className="text-[11px] text-zinc-400 leading-relaxed mb-4">
                  {loc.description}
                </p>
              </div>

              <div className="space-y-2 pt-2 border-t border-zinc-800/60">
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      openGoogleMapsDirections(loc);
                    }}
                    className="py-2 px-2 bg-zinc-800 hover:bg-zinc-700 text-white text-[11px] font-bold rounded-xl flex items-center justify-center space-x-1 transition-colors"
                  >
                    <span>🗺️</span>
                    <span>Google Maps</span>
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      openWaze(loc);
                    }}
                    className="py-2 px-2 bg-zinc-800 hover:bg-zinc-700 text-sky-400 text-[11px] font-bold rounded-xl flex items-center justify-center space-x-1 transition-colors"
                  >
                    <span>🧭</span>
                    <span>Waze</span>
                  </button>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelectBranchForBooking(loc);
                  }}
                  className="w-full py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black text-xs font-black uppercase rounded-xl transition-all shadow-md active:scale-98"
                >
                  ✂️ Agendar en esta Sucursal
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
