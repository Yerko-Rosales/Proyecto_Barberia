/**
 * WebpayModal Component - Simulador Oficial Transbank Webpay Plus (Chile)
 * Flujo realista en 5 etapas: Banco emisor, Redirección SSL, Portal bancario, Transbank y Voucher final
 */

import { AppFormatters } from '../utils/formatters.js';

export const CHILEAN_BANKS = [
  { name: 'BancoEstado', color: '#003366', factorName: 'BE Pass / Clave de Transferencias' },
  { name: 'Banco de Chile / Edwards', color: '#0E1E38', factorName: 'Dispositivo Mi Pass / Digipass' },
  { name: 'Santander Chile', color: '#CC0000', factorName: 'SuperClave Dinámica Santander' },
  { name: 'BCI / TBanc', color: '#1E351A', factorName: 'BCI Pass / Clave Internet' },
  { name: 'Itaú Corpbanca', color: '#EC7000', factorName: 'Itaú Pass SMS' },
  { name: 'Banco Falabella', color: '#6A9E19', factorName: 'Clave Dinámica Falabella' },
  { name: 'Scotiabank Chile', color: '#E51D24', factorName: 'KeyPass Scotiabank' },
  { name: 'Banco BICE', color: '#121824', factorName: 'BICE Pass' },
  { name: 'Coopeuch', color: '#007FA3', factorName: 'Clave Digital Coopeuch' }
];

export function WebpayModal({ isOpen, bookingDraft, onPaymentSuccess, onCancel }) {
  const [currentStage, setCurrentStage] = React.useState(1); // 1 to 5
  const [selectedBankIndex, setSelectedBankIndex] = React.useState(0);
  const [paymentType, setPaymentType] = React.useState('Débito (Redcompra)');
  const [userRut, setUserRut] = React.useState('19.342.110-3');
  const [bankPassword, setBankPassword] = React.useState('');
  const [securityToken, setSecurityToken] = React.useState('');

  // Identificadores de transacción generados
  const [transactionData, setTransactionData] = React.useState({
    token: '',
    authCode: '',
    buyOrder: ''
  });

  React.useEffect(() => {
    if (isOpen) {
      setCurrentStage(1);
      setBankPassword('');
      setSecurityToken('');
      setTransactionData({
        token: 'TSB_' + Math.random().toString(36).substring(2, 10).toUpperCase(),
        authCode: Math.floor(100000 + Math.random() * 900000).toString(),
        buyOrder: 'BO-' + Math.floor(20000 + Math.random() * 80000).toString()
      });
    }
  }, [isOpen]);

  // Manejo de transiciones asíncronas automáticas
  React.useEffect(() => {
    if (currentStage === 2) {
      const timer = setTimeout(() => {
        setCurrentStage(3);
      }, 1500);
      return () => clearTimeout(timer);
    }
    if (currentStage === 4) {
      const timer = setTimeout(() => {
        setCurrentStage(5);
      }, 1800);
      return () => clearTimeout(timer);
    }
  }, [currentStage]);

  if (!isOpen || !bookingDraft) return null;

  const selectedBank = CHILEAN_BANKS[selectedBankIndex];
  const amountToPay = bookingDraft.prepayAmount || 5000;

  const handleAuthorize = (e) => {
    e.preventDefault();
    if (bankPassword && securityToken) {
      setCurrentStage(4);
    }
  };

  const handlePrintVoucher = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/90 backdrop-blur-md animate-fade-in overflow-y-auto">
      <div className="bg-[#121212] border border-zinc-700 rounded-3xl w-full max-w-md p-5 sm:p-6 relative shadow-2xl my-6">
        {/* ETAPA 1: SELECCIONAR BANCO Y MEDIO DE PAGO */}
        {currentStage === 1 && (
          <div className="space-y-4">
            <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
              <div className="flex items-center space-x-2">
                <span className="w-3 h-3 rounded-full bg-red-600"></span>
                <span className="text-xs font-black tracking-wider uppercase text-white font-mono">
                  WEBPAY PLUS · TRANSBANK
                </span>
              </div>
              <button onClick={onCancel} className="text-zinc-500 hover:text-white text-xs font-bold">
                Cancelar
              </button>
            </div>

            <div className="p-3 bg-[#1E1E1E] rounded-2xl border border-zinc-800 flex justify-between items-center">
              <div>
                <span className="text-[10px] text-zinc-400 block uppercase font-bold">Monto del Abono:</span>
                <strong className="text-base text-[#FFD700] font-black">
                  {AppFormatters.formatClp(amountToPay)}
                </strong>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-zinc-400 block uppercase font-bold">Comercio:</span>
                <span className="text-xs font-bold text-white">Big Boys Barbershop</span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1.5">
                Tipo de Tarjeta o Medio de Pago:
              </label>
              <div className="grid grid-cols-3 gap-2">
                {['Débito (Redcompra)', 'Crédito', 'Prepago'].map(type => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setPaymentType(type)}
                    className={`py-2 px-1 text-[11px] font-bold rounded-xl border transition-colors ${
                      paymentType === type
                        ? 'bg-[#FFD700] text-black border-[#FFD700]'
                        : 'bg-[#1E1E1E] text-zinc-400 border-zinc-800'
                    }`}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-300 mb-1.5">
                Selecciona tu Banco Emisor:
              </label>
              <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
                {CHILEAN_BANKS.map((bank, idx) => {
                  const isSelected = selectedBankIndex === idx;
                  return (
                    <div
                      key={bank.name}
                      onClick={() => setSelectedBankIndex(idx)}
                      className={`p-2.5 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
                        isSelected
                          ? 'border-[#FFD700] bg-[#1E1E1E]'
                          : 'border-zinc-800 bg-[#171717] hover:border-zinc-700'
                      }`}
                    >
                      <div className="flex items-center space-x-2.5">
                        <span
                          className="w-3 h-3 rounded-full"
                          style={{ backgroundColor: bank.color }}
                        ></span>
                        <span className="text-xs font-bold text-white">{bank.name}</span>
                      </div>
                      <input
                        type="radio"
                        name="bank_choice"
                        checked={isSelected}
                        onChange={() => setSelectedBankIndex(idx)}
                        className="accent-[#FFD700]"
                      />
                    </div>
                  );
                })}
              </div>
            </div>

            <button
              onClick={() => setCurrentStage(2)}
              className="w-full py-3 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-lg transition-all"
            >
              Continuar al Portal Seguro de {selectedBank.name} &rarr;
            </button>
          </div>
        )}

        {/* ETAPA 2: REDIRECCIONANDO AL BANCO (ANIMACIÓN) */}
        {currentStage === 2 && (
          <div className="py-12 flex flex-col items-center justify-center text-center space-y-4">
            <div className="w-12 h-12 border-3 border-[#FFD700] border-t-transparent rounded-full animate-spin"></div>
            <p className="text-xs text-zinc-300">
              Conectando de forma segura con el servidor de...
            </p>
            <h4
              className="text-lg font-black uppercase tracking-wider"
              style={{ color: selectedBank.color === '#121824' ? '#FFD700' : selectedBank.color }}
            >
              {selectedBank.name}
            </h4>
            <div className="flex items-center space-x-1.5 text-[10px] text-zinc-500 pt-4">
              <span>🔒</span>
              <span>Conexión Encriptada TLS 1.3 · Transbank Chile</span>
            </div>
          </div>
        )}

        {/* ETAPA 3: PORTAL SEGURO DEL BANCO EMISOR */}
        {currentStage === 3 && (
          <form onSubmit={handleAuthorize} className="space-y-4 animate-fade-in">
            {/* Header del banco */}
            <div
              className="p-3.5 rounded-2xl text-white flex justify-between items-center shadow-md"
              style={{ backgroundColor: selectedBank.color }}
            >
              <div>
                <span className="text-[10px] font-bold block opacity-80 uppercase">Portal de Autorización</span>
                <h4 className="text-sm font-black">{selectedBank.name}</h4>
              </div>
              <span className="px-2 py-0.5 bg-white/20 rounded text-[9px] font-black uppercase">
                SSL Seguro
              </span>
            </div>

            <p className="text-xs text-zinc-400">
              Autoriza el abono de reserva por <strong>{AppFormatters.formatClp(amountToPay)}</strong> a nombre de Club Big Boys Chile.
            </p>

            <div>
              <label className="block text-[11px] font-bold text-zinc-300 mb-1">RUT Cliente</label>
              <input
                type="text"
                disabled
                value={userRut}
                className="w-full px-3 py-2 rounded-xl bg-zinc-900 border border-zinc-700 text-zinc-400 text-xs"
              />
            </div>

            <div>
              <label className="block text-[11px] font-bold text-zinc-300 mb-1">Clave Internet / Clave de Acceso</label>
              <input
                type="password"
                required
                value={bankPassword}
                onChange={e => setBankPassword(e.target.value)}
                placeholder="Ingresa tu clave de banco"
                className="w-full px-3 py-2 rounded-xl bg-[#1E1E1E] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
            </div>

            {/* Factor de seguridad Dinámico */}
            <div className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 space-y-2">
              <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-wide block">
                {selectedBank.factorName}:
              </span>
              <p className="text-[10px] text-zinc-400">
                Ingresa el código dinámico enviado o generado en tu app bancaria:
              </p>

              <div className="flex space-x-2">
                <input
                  type="text"
                  required
                  maxLength={6}
                  value={securityToken}
                  onChange={e => setSecurityToken(e.target.value)}
                  placeholder="Ej: 84920"
                  className="flex-1 px-3 py-1.5 rounded-lg bg-[#1E1E1E] border border-zinc-700 text-white text-xs font-mono font-bold tracking-widest text-center focus:outline-none focus:border-[#FFD700]"
                />

                <button
                  type="button"
                  onClick={() => setSecurityToken(Math.floor(10000 + Math.random() * 90000).toString())}
                  className="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-xs font-bold text-[#FFD700] rounded-lg transition-colors"
                >
                  Generar Token
                </button>
              </div>
            </div>

            <div className="flex space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setCurrentStage(1)}
                className="flex-1 py-2.5 bg-zinc-800 text-zinc-400 text-xs font-bold rounded-xl"
              >
                Rechazar
              </button>

              <button
                type="submit"
                disabled={!bankPassword || !securityToken}
                className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] disabled:bg-zinc-800 disabled:text-zinc-600 text-black font-black text-xs uppercase rounded-xl transition-all shadow-md"
              >
                Autorizar Pago
              </button>
            </div>
          </form>
        )}

        {/* ETAPA 4: PROCESANDO CON TRANSBANK */}
        {currentStage === 4 && (
          <div className="py-12 flex flex-col items-center justify-center text-center space-y-4 animate-fade-in">
            <div className="w-14 h-14 border-4 border-red-600 border-t-transparent rounded-full animate-spin"></div>
            <h4 className="text-sm font-black text-white uppercase tracking-wider">
              Procesando con Transbank Webpay Plus...
            </h4>
            <p className="text-xs text-zinc-400 max-w-xs">
              Validando fondos y confirmando reserva con {selectedBank.name}...
            </p>
            <span className="text-[10px] text-zinc-500 font-mono">
              Token: {transactionData.token}
            </span>
          </div>
        )}

        {/* ETAPA 5: COMPROBANTE OFICIAL DE TRANSBANK */}
        {currentStage === 5 && (
          <div className="space-y-4 animate-fade-in text-center" id="transbank-receipt-voucher">
            <div className="w-14 h-14 rounded-full bg-green-500/20 border-2 border-green-500 flex items-center justify-center text-green-400 text-2xl mx-auto">
              ✓
            </div>

            <div>
              <span className="text-[10px] font-black uppercase text-green-400 tracking-widest block">
                TRANSACCIÓN APROBADA
              </span>
              <h3 className="text-lg font-black text-white font-brand">
                COMPROBANTE DE PAGO ELECTRÓNICO
              </h3>
            </div>

            {/* Voucher Table */}
            <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4 text-xs space-y-2 text-left">
              <div className="flex justify-between py-1 border-b border-zinc-800/60">
                <span className="text-zinc-400">Comercio:</span>
                <strong className="text-white">Club Big Boys Chile Ltda.</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-zinc-800/60">
                <span className="text-zinc-400">RUT Comercio:</span>
                <strong className="text-white">76.438.291-K</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-zinc-800/60">
                <span className="text-zinc-400">Orden de Compra:</span>
                <strong className="text-zinc-200 font-mono">{transactionData.buyOrder}</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-zinc-800/60">
                <span className="text-zinc-400">Cód. Autorización:</span>
                <strong className="text-[#FFD700] font-mono">{transactionData.authCode}</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-zinc-800/60">
                <span className="text-zinc-400">Medio de Pago:</span>
                <strong className="text-white">{paymentType} ({selectedBank.name})</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-zinc-800/60">
                <span className="text-zinc-400">Servicio Agendado:</span>
                <strong className="text-white">{bookingDraft.service?.name}</strong>
              </div>

              <div className="flex justify-between pt-2">
                <span className="text-zinc-400 font-bold">Monto Abono Pagado:</span>
                <strong className="text-base text-green-400 font-black">
                  {AppFormatters.formatClp(amountToPay)}
                </strong>
              </div>
            </div>

            <div className="flex space-x-2 pt-2">
              <button
                type="button"
                onClick={handlePrintVoucher}
                className="flex-1 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-bold rounded-xl transition-colors"
              >
                🖨️ Imprimir / Guardar
              </button>

              <button
                type="button"
                onClick={onPaymentSuccess}
                className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-lg transition-all"
              >
                Finalizar & Ver mi Cita &rarr;
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
