/**
 * AuthModal Component - Login, Registro con RUT Chileno y Acceso Admin
 * Validación en tiempo real de RUT (Módulo 11), contraseñas y emisión de Cupón de Bienvenida
 */

import { ValidationUtils } from '../utils/validation.js';
import { store } from '../state/store.js';

export function AuthModal({ isOpen, onClose, initialMode = 'login', onGuestContinue }) {
  const [mode, setMode] = React.useState(initialMode); // 'login', 'register', 'admin'
  const [errorMsg, setErrorMsg] = React.useState('');
  const [successMsg, setSuccessMsg] = React.useState('');

  // Form State
  const [loginEmail, setLoginEmail] = React.useState('');
  const [loginPassword, setLoginPassword] = React.useState('');

  const [regName, setRegName] = React.useState('');
  const [regEmail, setRegEmail] = React.useState('');
  const [regPhone, setRegPhone] = React.useState('');
  const [regRut, setRegRut] = React.useState('');
  const [regPassword, setRegPassword] = React.useState('');
  const [regAvatar, setRegAvatar] = React.useState('');

  const [adminPin, setAdminPin] = React.useState('');
  const [registeredCouponPin, setRegisteredCouponPin] = React.useState(null);

  React.useEffect(() => {
    setMode(initialMode);
    setErrorMsg('');
    setSuccessMsg('');
    setRegisteredCouponPin(null);
  }, [isOpen, initialMode]);

  if (!isOpen) return null;

  // Real-time Rut Verification Feedback
  const cleanRut = ValidationUtils.cleanChileanRut(regRut);
  const isRutValid = cleanRut.length >= 8 && ValidationUtils.isValidChileanRut(cleanRut);

  const handleRutChange = (e) => {
    const val = e.target.value;
    setRegRut(val);
  };

  const handleRutBlur = () => {
    if (regRut.trim().length >= 8) {
      setRegRut(ValidationUtils.formatChileanRut(regRut));
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    const res = await store.login(loginEmail, loginPassword);
    if (res.success) {
      onClose();
    } else {
      setErrorMsg(res.message);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    const res = await store.register({
      fullName: regName,
      email: regEmail,
      phone: regPhone,
      rut: regRut,
      password: regPassword,
      avatarUrl: regAvatar
    });

    if (res.success) {
      setRegisteredCouponPin(res.couponPin);
      setSuccessMsg('¡Cuenta creada exitosamente!');
      setTimeout(() => {
        onClose();
      }, 3000);
    } else {
      setErrorMsg(res.message);
    }
  };

  const handleAdminLogin = (e) => {
    e.preventDefault();
    setErrorMsg('');
    const res = store.loginAsAdmin(adminPin);
    if (res.success) {
      onClose();
    } else {
      setErrorMsg(res.message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in overflow-y-auto">
      <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-md p-6 relative shadow-2xl my-8">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-zinc-400 hover:text-white text-lg p-1"
        >
          ✕
        </button>

        {/* Modal Header */}
        <div className="text-center mb-6">
          <div className="inline-flex p-3 rounded-full bg-[#121212] border border-[#FFD700] mb-2 shadow-md">
            <span className="text-2xl">✂️</span>
          </div>
          <h2 className="text-2xl font-black italic tracking-tighter text-white font-brand">
            BIG BOYS BARBERSHOP
          </h2>
          <p className="text-[11px] text-[#FFD700] font-bold tracking-widest uppercase">
            CHILE · TALAGANTE & PEÑAFLOR
          </p>
        </div>

        {/* Mode Selector Tabs */}
        <div className="flex border-b border-zinc-800 mb-6 text-xs font-bold">
          <button
            onClick={() => { setMode('login'); setErrorMsg(''); }}
            className={`flex-1 pb-2.5 border-b-2 transition-colors ${
              mode === 'login' ? 'border-[#FFD700] text-[#FFD700]' : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Iniciar Sesión
          </button>
          <button
            onClick={() => { setMode('register'); setErrorMsg(''); }}
            className={`flex-1 pb-2.5 border-b-2 transition-colors ${
              mode === 'register' ? 'border-[#FFD700] text-[#FFD700]' : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Crear Cuenta
          </button>
          <button
            onClick={() => { setMode('admin'); setErrorMsg(''); }}
            className={`flex-1 pb-2.5 border-b-2 transition-colors ${
              mode === 'admin' ? 'border-red-500 text-red-400' : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            🛡️ Admin
          </button>
        </div>

        {/* Feedback Messages */}
        {errorMsg && (
          <div className="mb-4 p-3 bg-red-950/40 border border-red-500/50 rounded-xl text-red-300 text-xs flex items-center space-x-2">
            <span>⚠️</span>
            <span>{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div className="mb-4 p-3 bg-green-950/40 border border-green-500/50 rounded-xl text-green-300 text-xs flex items-center space-x-2">
            <span>✓</span>
            <span>{successMsg}</span>
          </div>
        )}

        {/* Registered Golden Gift Coupon Card */}
        {registeredCouponPin && (
          <div className="mb-6 p-4 rounded-xl bg-gradient-to-r from-amber-950/40 to-yellow-950/30 border-2 border-dashed border-[#FFD700] text-center animate-fade-in">
            <span className="text-xs font-black uppercase text-[#FFD700] tracking-wider">
              🎁 ¡REGALO DE BIENVENIDA EXCLUSIVO!
            </span>
            <p className="text-sm font-bold text-white mt-1">1x Perfilado de Cejas Profesional GRATIS</p>
            <div className="my-3 py-2 px-6 bg-black border border-[#FFD700] rounded-xl inline-block">
              <span className="text-2xl font-black text-[#FFD700] tracking-[0.3em] font-mono">
                PIN: {registeredCouponPin}
              </span>
            </div>
            <p className="text-[11px] text-zinc-400">
              Guarda este PIN. Muéstralo en recepción o a tu barbero en tu primera visita.
            </p>
          </div>
        )}

        {/* LOGIN FORM */}
        {mode === 'login' && (
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1">Correo Electrónico</label>
              <input
                type="email"
                required
                value={loginEmail}
                onChange={e => setLoginEmail(e.target.value)}
                placeholder="ejemplo@correo.cl"
                className="w-full px-3.5 py-2.5 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1">Contraseña</label>
              <input
                type="password"
                required
                value={loginPassword}
                onChange={e => setLoginPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-3.5 py-2.5 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl transition-all shadow-[0_0_15px_rgba(255,215,0,0.3)] mt-2"
            >
              Iniciar Sesión
            </button>

            {/* Continuar como Invitado */}
            <div className="pt-2 border-t border-zinc-800 text-center">
              <button
                type="button"
                onClick={() => {
                  onGuestContinue();
                  onClose();
                }}
                className="text-xs font-bold text-zinc-400 hover:text-[#FFD700] transition-colors"
              >
                O continuar en Modo Invitado (sin registrarme) &rarr;
              </button>
            </div>
          </form>
        )}

        {/* REGISTER FORM */}
        {mode === 'register' && (
          <form onSubmit={handleRegister} className="space-y-3 max-h-[60vh] overflow-y-auto pr-1">
            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1">Nombre Completo</label>
              <input
                type="text"
                required
                value={regName}
                onChange={e => setRegName(e.target.value)}
                placeholder="Juan Pérez Silva"
                className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="text-xs font-bold text-zinc-300">RUT Chileno</label>
                {regRut.length >= 8 && (
                  <span className={`text-[10px] font-bold ${isRutValid ? 'text-green-400' : 'text-red-400'}`}>
                    {isRutValid ? '✓ RUT Válido (Módulo 11)' : '✗ RUT Inválido'}
                  </span>
                )}
              </div>
              <input
                type="text"
                required
                value={regRut}
                onChange={handleRutChange}
                onBlur={handleRutBlur}
                placeholder="19.342.110-3 o 193421103"
                className={`w-full px-3.5 py-2 rounded-xl bg-[#121212] border text-white text-xs focus:outline-none ${
                  regRut.length >= 8
                    ? isRutValid ? 'border-green-500' : 'border-red-500'
                    : 'border-zinc-700 focus:border-[#FFD700]'
                }`}
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">Correo</label>
                <input
                  type="email"
                  required
                  value={regEmail}
                  onChange={e => setRegEmail(e.target.value)}
                  placeholder="usuario@dominio.cl"
                  className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">Teléfono</label>
                <input
                  type="tel"
                  required
                  value={regPhone}
                  onChange={e => setRegPhone(e.target.value)}
                  placeholder="+56 9 1234 5678"
                  className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1">Contraseña</label>
              <input
                type="password"
                required
                value={regPassword}
                onChange={e => setRegPassword(e.target.value)}
                placeholder="Mínimo 6 caracteres con letras y números"
                className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
              <div className="flex space-x-3 text-[10px] text-zinc-500 mt-1">
                <span className={regPassword.length >= 6 ? 'text-green-400' : ''}>• Mínimo 6 car.</span>
                <span className={/[A-Za-z]/.test(regPassword) ? 'text-green-400' : ''}>• 1 letra</span>
                <span className={/\d/.test(regPassword) ? 'text-green-400' : ''}>• 1 número</span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1">URL de Foto de Perfil (Opcional)</label>
              <input
                type="url"
                value={regAvatar}
                onChange={e => setRegAvatar(e.target.value)}
                placeholder="https://images.unsplash.com/..."
                className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl transition-all shadow-md mt-3"
            >
              Registrarme y Obtener Regalo 🎁
            </button>
          </form>
        )}

        {/* ADMIN LOGIN FORM */}
        {mode === 'admin' && (
          <form onSubmit={handleAdminLogin} className="space-y-4">
            <div className="p-3 bg-red-950/20 border border-red-500/30 rounded-xl text-[11px] text-red-200">
              🛡️ Acceso restringido para administración central de Big Boys Barberías Chile.
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1">PIN de Administrador</label>
              <input
                type="password"
                required
                maxLength={8}
                value={adminPin}
                onChange={e => setAdminPin(e.target.value)}
                placeholder="PIN numérico (Predeterminado: 1234)"
                className="w-full px-3.5 py-2.5 rounded-xl bg-[#121212] border border-red-500/50 text-white text-xs focus:outline-none focus:border-red-400 font-mono tracking-widest text-center text-lg"
              />
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-red-600 hover:bg-red-500 text-white font-black text-xs uppercase tracking-wider rounded-xl transition-all shadow-md"
            >
              Acceder al Panel Administrativo
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
