/**
 * BookingWizard Component - Asistente de Agendamiento en 5 Pasos
 * Paso 1: Sucursal, Paso 2: Barbero, Paso 3: Servicio, Paso 4: Calendario en tiempo real, Paso 5: Resumen & Webpay
 */

import { AppFormatters } from '../utils/formatters.js';
import { SERVICES_LIST } from '../state/store.js';

export function BookingWizard({
  isOpen,
  onClose,
  locations,
  barbers,
  appointments,
  user,
  preselectedBarber = null,
  preselectedLocation = null,
  onProceedToPayment
}) {
  const [step, setStep] = React.useState(1);

  // Form selections
  const [selectedLocation, setSelectedLocation] = React.useState(null);
  const [selectedBarber, setSelectedBarber] = React.useState(null);
  const [selectedService, setSelectedService] = React.useState(SERVICES_LIST[0]);
  const [selectedDate, setSelectedDate] = React.useState(AppFormatters.currentDateString());
  const [selectedHour, setSelectedHour] = React.useState('');

  // Guest inputs if not logged in
  const [guestName, setGuestName] = React.useState('');
  const [guestPhone, setGuestPhone] = React.useState('');
  const [guestEmail, setGuestEmail] = React.useState('');
  const [formError, setFormError] = React.useState('');

  // Calendar state for Step 4
  const [calendarMonth, setCalendarMonth] = React.useState(new Date().getMonth());
  const [calendarYear, setCalendarYear] = React.useState(new Date().getFullYear());

  React.useEffect(() => {
    if (preselectedBarber) {
      setSelectedBarber(preselectedBarber);
      const loc = locations.find(l => l.id === preselectedBarber.locationId);
      if (loc) setSelectedLocation(loc);
      setStep(3); // Saltar directamente a selección de servicio
    } else if (preselectedLocation) {
      setSelectedLocation(preselectedLocation);
      setSelectedBarber(null);
      setStep(2); // Seleccionar barbero de esa sucursal
    } else {
      setStep(1);
    }
  }, [isOpen, preselectedBarber, preselectedLocation, locations]);

  // Barberos filtrados estrictamente por la sucursal seleccionada
  const branchBarbers = React.useMemo(() => {
    if (!selectedLocation || !barbers) return [];
    return (barbers || []).filter(b => b && b.locationId === selectedLocation.id && b.availableHours !== 'NO_DISPONIBLE');
  }, [selectedLocation, barbers]);

  // Horas tomadas para el barbero y fecha seleccionados
  const takenHours = React.useMemo(() => {
    if (!selectedBarber || !selectedDate || !appointments) return new Set();
    const taken = (appointments || [])
      .filter(a =>
        a &&
        a.barberId === selectedBarber.id &&
        a.dateTime &&
        typeof a.dateTime === 'string' &&
        a.dateTime.startsWith(selectedDate) &&
        a.status !== 'Cancelada'
      )
      .map(a => {
        const parts = (a.dateTime || '').split(' ');
        return parts.length > 1 ? parts[1] : '';
      })
      .filter(Boolean);
    return new Set(taken);
  }, [selectedBarber, selectedDate, appointments]);

  // Lista de horas del barbero
  const barberHours = React.useMemo(() => {
    if (!selectedBarber || !selectedBarber.availableHours || selectedBarber.availableHours === 'NO_DISPONIBLE') return [];
    return selectedBarber.availableHours.split(',').map(h => h.trim()).filter(Boolean);
  }, [selectedBarber]);

  // Generar días del mes para el calendario
  const calendarDays = React.useMemo(() => {
    const firstDayIndex = new Date(calendarYear, calendarMonth, 1).getDay(); // 0 = Domingo
    const totalDays = new Date(calendarYear, calendarMonth + 1, 0).getDate();
    const days = [];

    // Relleno inicial
    const offset = firstDayIndex === 0 ? 6 : firstDayIndex - 1; // Ajuste lunes = 0
    for (let i = 0; i < offset; i++) {
      days.push(null);
    }

    const today = new Date();
    const todayDay = today.getDate();
    const todayMonth = today.getMonth();
    const todayYear = today.getFullYear();

    for (let d = 1; d <= totalDays; d++) {
      const isPast =
        calendarYear < todayYear ||
        (calendarYear === todayYear && calendarMonth < todayMonth) ||
        (calendarYear === todayYear && calendarMonth === todayMonth && d < todayDay);

      const dateString = `${String(d).padStart(2, '0')}-${String(calendarMonth + 1).padStart(2, '0')}-${calendarYear}`;

      days.push({
        dayNumber: d,
        dateString,
        isPast,
        isToday: calendarYear === todayYear && calendarMonth === todayMonth && d === todayDay
      });
    }

    return days;
  }, [calendarMonth, calendarYear]);

  const monthNames = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ];

  if (!isOpen) return null;

  const handleNextMonth = () => {
    if (calendarMonth === 11) {
      setCalendarMonth(0);
      setCalendarYear(calendarYear + 1);
    } else {
      setCalendarMonth(calendarMonth + 1);
    }
  };

  const handlePrevMonth = () => {
    const today = new Date();
    // Bloquear retroceso a meses anteriores al actual
    if (calendarYear === today.getFullYear() && calendarMonth <= today.getMonth()) {
      return;
    }
    if (calendarMonth === 0) {
      setCalendarMonth(11);
      setCalendarYear(calendarYear - 1);
    } else {
      setCalendarMonth(calendarMonth - 1);
    }
  };

  const handleNextStep = () => {
    setFormError('');
    if (step === 1 && !selectedLocation) {
      setFormError('Por favor selecciona una sucursal.');
      return;
    }
    if (step === 2 && !selectedBarber) {
      setFormError('Por favor selecciona un barbero.');
      return;
    }
    if (step === 3 && !selectedService) {
      setFormError('Por favor selecciona un servicio.');
      return;
    }
    if (step === 4) {
      if (!selectedDate) {
        setFormError('Por favor selecciona una fecha.');
        return;
      }
      if (!selectedHour) {
        setFormError('Por favor selecciona una hora disponible.');
        return;
      }
    }
    setStep(step + 1);
  };

  const handleLaunchPayment = () => {
    setFormError('');
    if (!user) {
      if (!guestName.trim()) {
        setFormError('Por favor ingresa tu nombre completo.');
        return;
      }
      if (!guestPhone.trim()) {
        setFormError('Por favor ingresa tu número telefónico.');
        return;
      }
    }

    onProceedToPayment({
      location: selectedLocation,
      barber: selectedBarber,
      service: selectedService,
      date: selectedDate,
      hour: selectedHour,
      guestName,
      guestPhone,
      guestEmail,
      prepayAmount: 5000
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-sm animate-fade-in overflow-y-auto">
      <div className="bg-[#1E1E1E] border border-[#FFD700] rounded-3xl w-full max-w-xl p-5 sm:p-7 relative shadow-2xl my-6 flex flex-col max-h-[92vh]">
        {/* Header with Step indicator */}
        <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
          <div>
            <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-widest block">
              AGENDAMIENTO ONLINE (PASO {step} DE 5)
            </span>
            <h3 className="text-base sm:text-lg font-black italic text-white font-brand">
              RESERVA DE ATENCIÓN BIG BOYS
            </h3>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-zinc-800 text-zinc-400 hover:text-white flex items-center justify-center text-sm"
          >
            ✕
          </button>
        </div>

        {/* Progress Bar */}
        <div className="w-full h-1.5 bg-zinc-800 rounded-full my-3 overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-[#FFD700] to-[#E5C100] transition-all duration-300"
            style={{ width: `${(step / 5) * 100}%` }}
          ></div>
        </div>

        {formError && (
          <div className="p-2.5 mb-3 bg-red-950/40 border border-red-500/50 rounded-xl text-red-300 text-xs flex items-center space-x-2">
            <span>⚠️</span>
            <span>{formError}</span>
          </div>
        )}

        {/* Step Content Body (Scrollable) */}
        <div className="flex-1 overflow-y-auto pr-1 space-y-4 py-2">
          {/* PASO 1: SELECCIONAR SUCURSAL */}
          {step === 1 && (
            <div>
              <h4 className="text-xs font-bold text-white mb-1">Paso 1: Selecciona la Sucursal</h4>
              <p className="text-xs text-zinc-400 mb-3">Elige el local donde deseas ser atendido:</p>

              <div className="space-y-2.5">
                {locations.map(loc => {
                  const isSelected = selectedLocation?.id === loc.id;
                  const count = barbers.filter(b => b.locationId === loc.id && b.availableHours !== 'NO_DISPONIBLE').length;

                  return (
                    <div
                      key={loc.id}
                      onClick={() => setSelectedLocation(loc)}
                      className={`p-4 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                        isSelected
                          ? 'bg-[#FFD700]/15 border-[#FFD700] shadow-md'
                          : 'bg-[#121212] border-zinc-800 hover:border-zinc-700'
                      }`}
                    >
                      <div className="flex items-center space-x-3">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center text-lg ${
                          isSelected ? 'bg-[#FFD700] text-black' : 'bg-zinc-800 text-[#FFD700]'
                        }`}>
                          📍
                        </div>
                        <div>
                          <h5 className="text-xs font-bold text-white">{loc.name}</h5>
                          <p className="text-[11px] text-zinc-400">{loc.address}</p>
                          <span className="text-[10px] text-[#FFD700] font-semibold">
                            {count} barberos disponibles
                          </span>
                        </div>
                      </div>

                      <input
                        type="radio"
                        name="loc_radio"
                        checked={isSelected}
                        onChange={() => setSelectedLocation(loc)}
                        className="accent-[#FFD700]"
                      />
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* PASO 2: SELECCIONAR BARBERO */}
          {step === 2 && (
            <div>
              <h4 className="text-xs font-bold text-white mb-1">Paso 2: Selecciona tu Barbero</h4>
              <p className="text-xs text-[#FFD700] font-semibold mb-3">
                Barberos activos en {selectedLocation?.name}
              </p>

              {branchBarbers.length === 0 ? (
                <div className="p-6 bg-[#121212] rounded-2xl border border-zinc-800 text-center text-zinc-400 text-xs">
                  No hay barberos disponibles en esta sucursal actualmente.
                </div>
              ) : (
                <div className="space-y-2.5">
                  {branchBarbers.map(barber => {
                    const isSelected = selectedBarber?.id === barber.id;
                    return (
                      <div
                        key={barber.id}
                        onClick={() => setSelectedBarber(barber)}
                        className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                          isSelected
                            ? 'bg-[#FFD700]/15 border-[#FFD700] shadow-md'
                            : 'bg-[#121212] border-zinc-800 hover:border-zinc-700'
                        }`}
                      >
                        <div className="flex items-center space-x-3">
                          <img
                            src={barber.avatarUrl}
                            alt={barber.name}
                            className="w-12 h-12 rounded-full object-cover border-2 border-[#FFD700]"
                          />
                          <div>
                            <h5 className="text-xs font-bold text-white">{barber.name}</h5>
                            <p className="text-[11px] text-zinc-400">{barber.specialty}</p>
                            <div className="flex items-center space-x-2 text-[10px] mt-0.5">
                              <span className="text-[#FFD700]">★ {barber.rating.toFixed(1)}</span>
                              <span className="text-zinc-500">· {barber.availableHours.split(',').length} turnos</span>
                            </div>
                          </div>
                        </div>

                        <input
                          type="radio"
                          name="barber_radio"
                          checked={isSelected}
                          onChange={() => setSelectedBarber(barber)}
                          className="accent-[#FFD700]"
                        />
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* PASO 3: SELECCIONAR SERVICIO */}
          {step === 3 && (
            <div>
              <h4 className="text-xs font-bold text-white mb-1">Paso 3: Selecciona el Servicio</h4>
              <p className="text-xs text-zinc-400 mb-3">Elige el tratamiento o corte deseado:</p>

              <div className="space-y-2.5">
                {SERVICES_LIST.map(svc => {
                  const isSelected = selectedService?.name === svc.name;
                  return (
                    <div
                      key={svc.id}
                      onClick={() => setSelectedService(svc)}
                      className={`p-4 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                        isSelected
                          ? 'bg-[#FFD700]/15 border-[#FFD700] shadow-md'
                          : 'bg-[#121212] border-zinc-800 hover:border-zinc-700'
                      }`}
                    >
                      <div>
                        <h5 className="text-xs font-bold text-white">{svc.name}</h5>
                        <p className="text-[10px] text-zinc-400">Duración aprox: {svc.duration} · Estilo Big Boys</p>
                      </div>

                      <span className="text-xs font-black text-[#FFD700]">
                        {AppFormatters.formatClp(svc.price)}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* PASO 4: CALENDARIO & HORARIO EN TIEMPO REAL */}
          {step === 4 && (
            <div className="space-y-4">
              <div>
                <h4 className="text-xs font-bold text-white mb-1">Paso 4: Selecciona Fecha y Hora</h4>
                <p className="text-xs text-zinc-400">No se permiten fechas pasadas. Horarios bloqueados en tiempo real.</p>
              </div>

              {/* Calendario Dinámico */}
              <div className="bg-[#121212] border border-zinc-800 rounded-2xl p-4">
                {/* Mes y Controles */}
                <div className="flex justify-between items-center mb-3">
                  <span className="text-xs font-black uppercase text-[#FFD700] tracking-wider">
                    {monthNames[calendarMonth]} {calendarYear}
                  </span>
                  <div className="flex space-x-1">
                    <button
                      onClick={handlePrevMonth}
                      className="w-7 h-7 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-white text-xs flex items-center justify-center"
                    >
                      &larr;
                    </button>
                    <button
                      onClick={handleNextMonth}
                      className="w-7 h-7 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-white text-xs flex items-center justify-center"
                    >
                      &rarr;
                    </button>
                  </div>
                </div>

                {/* Días de la semana */}
                <div className="grid grid-cols-7 gap-1 text-center text-[10px] font-bold text-zinc-500 mb-1">
                  <span>Lu</span><span>Ma</span><span>Mi</span><span>Ju</span><span>Vi</span><span>Sá</span><span>Do</span>
                </div>

                {/* Grid de días */}
                <div className="grid grid-cols-7 gap-1">
                  {calendarDays.map((dObj, idx) => {
                    if (!dObj) {
                      return <div key={`empty-${idx}`} className="h-8"></div>;
                    }
                    const isSelected = selectedDate === dObj.dateString;

                    return (
                      <button
                        key={dObj.dateString}
                        disabled={dObj.isPast}
                        onClick={() => {
                          setSelectedDate(dObj.dateString);
                          setSelectedHour(''); // resetear hora al cambiar día
                        }}
                        className={`h-8 rounded-lg text-xs font-bold transition-all flex items-center justify-center ${
                          dObj.isPast
                            ? 'text-zinc-700 cursor-not-allowed'
                            : isSelected
                            ? 'bg-[#FFD700] text-black shadow-md font-black'
                            : dObj.isToday
                            ? 'border border-[#FFD700] text-white hover:bg-zinc-800'
                            : 'text-zinc-200 hover:bg-zinc-800'
                        }`}
                      >
                        {dObj.dayNumber}
                      </button>
                    );
                  })}
                </div>

                <div className="mt-3 pt-2 border-t border-zinc-800 text-[11px] text-zinc-400">
                  Fecha seleccionada: <strong className="text-white">{AppFormatters.formatDateToLongSpanish(selectedDate)}</strong>
                </div>
              </div>

              {/* Selector de Horarios */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <span className="text-xs font-bold text-white">Horarios disponibles:</span>
                  {takenHours.size > 0 && (
                    <span className="text-[10px] text-red-400 font-bold">
                      🔒 {takenHours.size} hora(s) ocupada(s)
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  {barberHours.map(hour => {
                    const isTaken = takenHours.has(hour);
                    const isSelected = selectedHour === hour;

                    return (
                      <button
                        key={hour}
                        disabled={isTaken}
                        onClick={() => setSelectedHour(hour)}
                        className={`py-2 px-1 rounded-xl text-xs font-bold transition-all flex flex-col items-center justify-center border ${
                          isTaken
                            ? 'bg-red-950/20 border-red-500/30 text-red-400/50 cursor-not-allowed'
                            : isSelected
                            ? 'bg-[#FFD700] border-[#FFD700] text-black shadow-md font-black'
                            : 'bg-[#121212] border-zinc-800 hover:border-[#FFD700]/50 text-white'
                        }`}
                      >
                        <span>{hour} hrs</span>
                        {isTaken && <span className="text-[8px] uppercase tracking-tighter">Ocupado</span>}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* PASO 5: RESUMEN DE AGENDAMIENTO & DETALLES DE PAGO */}
          {step === 5 && (
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-white">Paso 5: Resumen de tu Agendamiento</h4>

              {/* Tarjeta de Resumen Oficial */}
              <div className="p-4 rounded-2xl bg-[#1E1A16] border border-[#FFD700]/50 space-y-3">
                <div className="flex items-center space-x-2 pb-2 border-b border-zinc-800">
                  <span className="text-base text-[#FFD700]">📋</span>
                  <span className="text-xs font-black uppercase text-[#FFD700] tracking-wider">
                    COMPROBANTE DE RESERVA
                  </span>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex items-start space-x-2">
                    <span>🕰️</span>
                    <div>
                      <span className="text-[10px] text-zinc-400 block font-bold">DÍA Y HORA:</span>
                      <strong className="text-white">{selectedDate} a las {selectedHour} hrs</strong>
                    </div>
                  </div>

                  <div className="flex items-start space-x-2">
                    <span>👤</span>
                    <div>
                      <span className="text-[10px] text-zinc-400 block font-bold">BARBERO ASIGNADO:</span>
                      <strong className="text-white">{selectedBarber?.name}</strong> ({selectedBarber?.specialty})
                    </div>
                  </div>

                  <div className="flex items-start space-x-2">
                    <span>📍</span>
                    <div>
                      <span className="text-[10px] text-zinc-400 block font-bold">SUCURSAL:</span>
                      <strong className="text-white">{selectedLocation?.name}</strong> - {selectedLocation?.address}
                    </div>
                  </div>

                  <div className="flex items-start space-x-2">
                    <span>✂️</span>
                    <div>
                      <span className="text-[10px] text-zinc-400 block font-bold">SERVICIO & PRECIO:</span>
                      <strong className="text-white">{selectedService?.name}</strong> — {AppFormatters.formatClp(selectedService?.price)}
                    </div>
                  </div>
                </div>
              </div>

              {/* Datos de contacto (si es modo invitado) */}
              {!user ? (
                <div className="space-y-2.5 pt-1">
                  <span className="text-xs font-bold text-[#FFD700]">
                    Estás reservando como Invitado. Ingresa tus datos:
                  </span>

                  <div>
                    <label className="block text-[11px] text-zinc-400 mb-1">Nombre Completo *</label>
                    <input
                      type="text"
                      required
                      value={guestName}
                      onChange={e => setGuestName(e.target.value)}
                      placeholder="Ej: Marcelo Ríos"
                      className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] text-zinc-400 mb-1">Teléfono *</label>
                      <input
                        type="tel"
                        required
                        value={guestPhone}
                        onChange={e => setGuestPhone(e.target.value)}
                        placeholder="+56 9 9999 9999"
                        className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] text-zinc-400 mb-1">Correo Electrónico</label>
                      <input
                        type="email"
                        value={guestEmail}
                        onChange={e => setGuestEmail(e.target.value)}
                        placeholder="para recibir el voucher"
                        className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                      />
                    </div>
                  </div>
                </div>
              ) : (
                <div className="p-3 bg-[#121212] rounded-xl border border-zinc-800 text-xs">
                  <span className="text-[10px] text-zinc-400 block">Cliente Registrado:</span>
                  <strong className="text-white">{user.fullName}</strong> · {user.phone} ({user.email})
                </div>
              )}

              {/* Alerta de Abono Anticipado */}
              <div className="p-3.5 rounded-xl bg-red-950/30 border border-red-500/40 text-xs text-red-200 flex items-start space-x-2.5">
                <span className="text-lg">💳</span>
                <div>
                  <strong className="block text-red-300 font-black">ABONO DE RESERVA ANTICIPADO: $5.000 CLP</strong>
                  <p className="text-[11px] text-zinc-300 mt-0.5 leading-relaxed">
                    Para asegurar tu sillón y barbero en Big Boys se requiere un abono online de $5.000 CLP vía Webpay Plus.
                    El saldo de {AppFormatters.formatClp(selectedService?.price - 5000)} se cancela presencialmente en el local.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer Navigation Buttons */}
        <div className="pt-4 border-t border-zinc-800 flex justify-between items-center">
          {step > 1 ? (
            <button
              type="button"
              onClick={() => setStep(step - 1)}
              className="px-4 py-2.5 border border-zinc-700 hover:border-zinc-500 text-zinc-300 text-xs font-bold rounded-xl transition-colors"
            >
              &larr; Atrás
            </button>
          ) : (
            <div></div>
          )}

          {step < 5 ? (
            <button
              type="button"
              onClick={handleNextStep}
              className="px-6 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md transition-all"
            >
              Continuar &rarr;
            </button>
          ) : (
            <button
              type="button"
              onClick={handleLaunchPayment}
              className="px-6 py-2.5 bg-gradient-to-r from-[#FFD700] to-[#E5C100] hover:scale-105 active:scale-95 text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-[0_0_20px_rgba(255,215,0,0.4)] transition-all flex items-center space-x-1.5"
            >
              <span>🔒</span>
              <span>Pagar Abono con Webpay ($5.000 CLP)</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
