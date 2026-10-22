/**
 * SplashScreen Component - Pantalla de Bienvenida de Lujo
 * Animación oficial de poste de barbero y logotipo de Big Boys Chile
 */

export function SplashScreen({ onFinish }) {
  const [progress, setProgress] = React.useState(0);

  React.useEffect(() => {
    const timer = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          clearInterval(timer);
          onFinish();
          return 100;
        }
        return prev + 35;
      });
    }, 100);

    const emergencyTimer = setTimeout(() => {
      clearInterval(timer);
      onFinish();
    }, 450);

    return () => {
      clearInterval(timer);
      clearTimeout(emergencyTimer);
    };
  }, []);

  return (
    <div onClick={onFinish} className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-[#121212] px-6 text-center select-none cursor-pointer">
      {/* Barber Pole Stripes Top Bar */}
      <div className="absolute top-0 left-0 right-0 h-3 barber-pole-border shadow-lg"></div>

      <div className="max-w-md w-full flex flex-col items-center animate-fade-in">
        {/* Golden Scissors / Barber Icon Frame */}
        <div className="relative mb-6">
          <div className="w-24 h-24 rounded-full border-2 border-[#FFD700] flex items-center justify-center bg-[#1E1E1E] shadow-[0_0_25px_rgba(255,215,0,0.3)]">
            <span className="text-4xl animate-bounce">✂️</span>
          </div>
          <div className="absolute -bottom-1 -right-1 w-8 h-8 rounded-full bg-[#FFD700] flex items-center justify-center text-black font-black text-xs shadow-md">
            🇨🇱
          </div>
        </div>

        {/* Brand Name */}
        <div className="flex items-center space-x-1 mb-1">
          <h1 className="text-4xl md:text-5xl font-black italic tracking-tighter text-white font-brand">
            BIG BOYS
          </h1>
          <span className="text-4xl md:text-5xl font-black text-[#FFD700]">.</span>
        </div>

        <p className="text-xs uppercase tracking-[0.3em] font-bold text-[#FFD700] mb-3">
          BARBERSHOP · CHILE
        </p>

        <p className="text-xs text-zinc-400 max-w-xs mb-8 leading-relaxed">
          Donde el estilo se junta con la distinción. Reserva tu hora en Talagante y Peñaflor con prepagos garantizados.
        </p>

        {/* Progress Bar */}
        <div className="w-48 h-1.5 bg-zinc-800 rounded-full overflow-hidden mb-6 border border-zinc-700">
          <div
            className="h-full bg-gradient-to-r from-[#FFD700] to-[#E5C100] transition-all duration-200"
            style={{ width: `${progress}%` }}
          ></div>
        </div>

        {/* Quick Skip Button */}
        <button
          onClick={onFinish}
          className="text-xs font-bold text-zinc-400 hover:text-[#FFD700] tracking-wider uppercase border-b border-transparent hover:border-[#FFD700] pb-0.5 transition-colors"
        >
          Entrar al Club &rarr;
        </button>
      </div>

      {/* Barber Pole Stripes Bottom Bar */}
      <div className="absolute bottom-0 left-0 right-0 h-3 barber-pole-border shadow-lg"></div>
    </div>
  );
}
