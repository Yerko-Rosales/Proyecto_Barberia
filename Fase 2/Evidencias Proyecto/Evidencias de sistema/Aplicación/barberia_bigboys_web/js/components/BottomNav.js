/**
 * BottomNav & TopTabs Component - Navegación Principal Oficial
 * Soporta barra inferior móvil y barra de navegación superior en pantallas medianas/grandes
 */

export function BottomNav({ activeTab, onTabChange, hasUser, isAdmin, onOpenBooking }) {
  const tabs = [
    { id: 'inicio', label: 'Inicio', icon: '🏠' },
    { id: 'barberos', label: 'Barberos', icon: '💈' },
    { id: 'sucursales', label: 'Sucursales', icon: '📍' },
    { id: 'resenas', label: 'Reseñas', icon: '⭐' },
    ...(hasUser ? [{ id: 'historial', label: 'Historial', icon: '📜' }] : []),
    ...(isAdmin ? [{ id: 'admin', label: 'Panel Admin', icon: '🛡️' }] : [])
  ];

  return (
    <>
      {/* Botón flotante para Agendar Cita (en móvil y escritorio si no está en admin) */}
      {activeTab !== 'admin' && (
        <div className="fixed bottom-20 md:bottom-8 right-6 z-30 animate-fade-in">
          <button
            onClick={onOpenBooking}
            className="flex items-center space-x-2 px-5 py-3.5 bg-gradient-to-r from-[#FFD700] to-[#E5C100] text-black font-black text-sm tracking-wider uppercase rounded-2xl shadow-[0_4px_25px_rgba(255,215,0,0.5)] hover:scale-105 active:scale-95 transition-all"
          >
            <span className="text-lg">✂️</span>
            <span>Agendar Cita</span>
          </button>
        </div>
      )}

      {/* Barra de Navegación Móvil (Fixed Bottom) */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-30 bg-[#121212]/95 backdrop-blur-md border-t border-zinc-800 px-2 py-1.5 transition-colors">
        <div className="flex items-center justify-around">
          {tabs.map(tab => {
            const isSelected = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                className={`flex flex-col items-center justify-center py-1 px-2 rounded-xl transition-all ${
                  isSelected ? 'text-[#FFD700]' : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                <span className={`text-lg transition-transform ${isSelected ? 'scale-110' : ''}`}>
                  {tab.icon}
                </span>
                <span className={`text-[10px] tracking-tight font-bold mt-0.5 ${isSelected ? 'text-[#FFD700]' : ''}`}>
                  {tab.label}
                </span>
                {isSelected && (
                  <span className="w-1.5 h-1.5 bg-[#FFD700] rounded-full mt-0.5 shadow-[0_0_6px_#FFD700]"></span>
                )}
              </button>
            );
          })}
        </div>
      </nav>

      {/* Pestañas para Escritorio (Debajo del Navbar) */}
      <nav className="hidden md:block bg-[#1E1E1E]/80 border-b border-zinc-800 transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-1 py-2">
            {tabs.map(tab => {
              const isSelected = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => onTabChange(tab.id)}
                  className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                    isSelected
                      ? 'bg-[#FFD700] text-black shadow-md'
                      : 'text-zinc-300 hover:bg-zinc-800 hover:text-white'
                  }`}
                >
                  <span className="text-base">{tab.icon}</span>
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>
      </nav>
    </>
  );
}
