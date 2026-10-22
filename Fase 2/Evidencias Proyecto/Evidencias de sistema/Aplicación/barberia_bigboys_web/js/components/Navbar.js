/**
 * Navbar Component - Barra Superior Oficial de Big Boys
 * Logotipo, sucursal, alternador de modo oscuro/claro, notificaciones y perfil de usuario
 */

export function Navbar({
  user,
  isAdmin,
  previewAsClient,
  onTogglePreviewClient,
  isDarkMode,
  notificationsCount,
  onToggleTheme,
  onOpenNotifications,
  onOpenProfile,
  onOpenAuth,
  onLogout
}) {
  const [showUserMenu, setShowUserMenu] = React.useState(false);

  return (
    <header className={`sticky top-0 z-40 border-b transition-colors ${isAdmin && !previewAsClient ? 'bg-[#0e0e0f] border-amber-500/40 shadow-lg' : 'bg-[#121212] border-zinc-800'}`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo & Subtitle */}
          <div className="flex flex-col cursor-pointer" onClick={() => window.location.hash = '#inicio'}>
            <div className="flex items-center space-x-2">
              <span className="text-[10px] font-extrabold tracking-[0.2em] text-[#FFD700] uppercase">
                CHILE · TALAGANTE & PEÑAFLOR
              </span>
              {isAdmin && !previewAsClient && (
                <span className="px-2 py-0.5 rounded-full bg-amber-500/20 text-[#FFD700] text-[9px] font-black uppercase tracking-wider border border-amber-500/40">
                  🛡️ MODO ADMINISTRADOR
                </span>
              )}
            </div>
            <div className="flex items-center space-x-0.5">
              <span className="text-2xl font-black italic tracking-tighter text-white font-brand">
                BIG BOYS
              </span>
              <span className="text-2xl font-black text-[#FFD700]">.</span>
              {isAdmin && !previewAsClient && (
                <span className="text-xs font-bold text-zinc-400 ml-2 hidden sm:inline">Panel Central</span>
              )}
            </div>
          </div>

          {/* Right Header Actions */}
          <div className="flex items-center space-x-2 sm:space-x-3">
            {/* Botón de alternar Vista Cliente para el Administrador */}
            {isAdmin && (
              <button
                onClick={onTogglePreviewClient}
                title={previewAsClient ? 'Volver al Panel de Administración' : 'Revisar cómo ven la página los clientes'}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all flex items-center space-x-1.5 ${
                  previewAsClient
                    ? 'bg-[#FFD700] text-black border-[#FFD700] font-black shadow-md'
                    : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border-zinc-700'
                }`}
              >
                <span>{previewAsClient ? '🛡️' : '👁️'}</span>
                <span className="hidden sm:inline">{previewAsClient ? 'Volver a Admin' : 'Vista Cliente'}</span>
              </button>
            )}

            {/* Theme Toggle Button */}
            <button
              onClick={onToggleTheme}
              title={isDarkMode ? 'Cambiar a Modo Claro' : 'Cambiar a Modo Oscuro'}
              className="p-2 rounded-xl text-[#FFD700] hover:bg-zinc-800 transition-colors"
            >
              {isDarkMode ? (
                <span className="text-lg">☀️</span>
              ) : (
                <span className="text-lg">🌙</span>
              )}
            </button>

            {/* Notification Bell with Badge */}
            <button
              onClick={onOpenNotifications}
              title="Buzón de Notificaciones"
              className="relative p-2 rounded-xl text-zinc-300 hover:text-[#FFD700] hover:bg-zinc-800 transition-colors"
            >
              <span className="text-lg">🔔</span>
              {notificationsCount > 0 && (
                <span className="absolute top-1 right-1 min-w-[18px] h-[18px] px-1 bg-[#FFD700] text-black text-[10px] font-black rounded-full flex items-center justify-center shadow-md">
                  {notificationsCount}
                </span>
              )}
            </button>

            {/* Si es Admin, botón directo de Salir */}
            {isAdmin ? (
              <button
                onClick={onLogout}
                className="px-3 py-1.5 bg-red-950/40 hover:bg-red-900/60 text-red-400 border border-red-500/40 rounded-xl text-xs font-bold transition-colors flex items-center space-x-1"
              >
                <span>🚪</span>
                <span className="hidden sm:inline">Cerrar Sesión</span>
              </button>
            ) : user ? (
              <div className="relative">
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="flex items-center space-x-2 px-3 py-1.5 rounded-xl border border-[#FFD700]/40 bg-[#1E1E1E] text-white hover:border-[#FFD700] transition-all"
                >
                  {user.avatarUrl ? (
                    <img
                      src={user.avatarUrl}
                      alt={user.fullName}
                      className="w-7 h-7 rounded-full object-cover border border-[#FFD700]"
                    />
                  ) : (
                    <div className="w-7 h-7 rounded-full bg-[#FFD700] text-black flex items-center justify-center font-black text-xs">
                      {user.fullName ? user.fullName[0].toUpperCase() : 'U'}
                    </div>
                  )}

                  <div className="hidden sm:flex flex-col text-left">
                    <span className="text-xs font-bold truncate max-w-[120px]">
                      {user.fullName}
                    </span>
                    <span className="text-[9px] text-[#FFD700] font-semibold">
                      ⭐ Socio VIP
                    </span>
                  </div>

                  <span className="text-xs text-zinc-400">▼</span>
                </button>

                {/* Dropdown Menu */}
                {showUserMenu && (
                  <div
                    className="absolute right-0 mt-2 w-52 bg-[#1E1E1E] border border-[#FFD700]/30 rounded-xl shadow-2xl py-2 z-50 animate-fade-in"
                    onMouseLeave={() => setShowUserMenu(false)}
                  >
                    <div className="px-4 py-2 border-b border-zinc-800">
                      <p className="text-xs text-zinc-400">Sesión iniciada como</p>
                      <p className="text-xs font-bold text-white truncate">{user.email}</p>
                    </div>

                    <button
                      onClick={() => {
                        setShowUserMenu(false);
                        onOpenProfile();
                      }}
                      className="w-full text-left px-4 py-2 text-xs text-zinc-200 hover:bg-zinc-800 flex items-center space-x-2"
                    >
                      <span>👤</span>
                      <span>Mi Perfil & Datos</span>
                    </button>

                    <button
                      onClick={() => {
                        setShowUserMenu(false);
                        onLogout();
                      }}
                      className="w-full text-left px-4 py-2 text-xs text-red-400 hover:bg-red-950/30 flex items-center space-x-2"
                    >
                      <span>🚪</span>
                      <span>Cerrar Sesión</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <button
                onClick={onOpenAuth}
                className="flex items-center space-x-1 px-3.5 py-1.5 bg-[#FFD700] hover:bg-[#E5C100] text-black text-xs font-black rounded-xl shadow-[0_0_15px_rgba(255,215,0,0.3)] transition-all"
              >
                <span>🔑</span>
                <span>Ingresar / Registro</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
