/**
 * HistoryScreen Component - Historial de Citas del Cliente
 * Lista de servicios realizados, comprobante de abono, detalle de barbero y opción para calificar
 */

import { AppFormatters } from '../utils/formatters.js';
import { store } from '../state/store.js';

export function HistoryScreen({ user, appointments, locations, barbers, onOpenProfile, onOpenBooking }) {
  const [reviewingAppointment, setReviewingAppointment] = React.useState(null);
  const [reviewRating, setReviewRating] = React.useState(5);
  const [reviewComment, setReviewComment] = React.useState('');

  const userAppointments = React.useMemo(() => {
    if (!user) return [];
    return appointments.filter(a => a.userId === user.id);
  }, [appointments, user]);

  const handleReviewSubmit = (e) => {
    e.preventDefault();
    if (!reviewingAppointment) return;

    store.submitReview({
      appointmentId: reviewingAppointment.id,
      locationId: reviewingAppointment.locationId,
      barberId: reviewingAppointment.barberId,
      rating: reviewRating,
      comment: reviewComment,
      tags: ['Cliente Verificado', 'Atención Oficial'],
      serviceOrSubject: reviewingAppointment.serviceName,
      reviewType: 'SERVICE'
    });

    setReviewingAppointment(null);
    setReviewComment('');
  };

  return (
    <div className="space-y-6 pb-20 md:pb-12 animate-fade-in">
      {/* Client Profile Header Card */}
      {user && (
        <div className="bg-[#1E1E1E] border border-[#FFD700]/30 rounded-3xl p-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div className="flex items-center space-x-4">
            {user.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt={user.fullName}
                className="w-16 h-16 rounded-full object-cover border-2 border-[#FFD700]"
              />
            ) : (
              <div className="w-16 h-16 rounded-full bg-[#FFD700] text-black flex items-center justify-center font-black text-2xl shadow-lg">
                {user.fullName ? user.fullName[0].toUpperCase() : 'U'}
              </div>
            )}

            <div>
              <h2 className="text-xl font-black text-white">{user.fullName}</h2>
              <span className="text-xs text-[#FFD700] font-extrabold block">
                ⭐ Cliente Exclusivo Big Boys Chile
              </span>
              <p className="text-xs text-zinc-400 mt-0.5">
                {user.email} · Tel: {user.phone} {user.rut ? `· RUT: ${user.rut}` : ''}
              </p>
            </div>
          </div>

          <button
            onClick={onOpenProfile}
            className="px-4 py-2 border border-[#FFD700] hover:bg-[#FFD700] hover:text-black text-[#FFD700] text-xs font-bold rounded-xl transition-all flex items-center space-x-1.5"
          >
            <span>✏️</span>
            <span>Editar Perfil</span>
          </button>
        </div>
      )}

      {/* Appointments History List */}
      <div>
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-base font-black italic tracking-wide text-white font-brand uppercase">
            REGISTRO DE AGENDAMIENTOS ({userAppointments.length})
          </h3>

          <button
            onClick={onOpenBooking}
            className="text-xs font-bold text-[#FFD700] hover:underline"
          >
            + Nueva Cita
          </button>
        </div>

        {userAppointments.length === 0 ? (
          <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-8 text-center">
            <span className="text-4xl block mb-2">📜</span>
            <p className="text-xs text-zinc-400 mb-4">
              Aún no registras atenciones en tu historial.
            </p>
            <button
              onClick={onOpenBooking}
              className="px-5 py-2.5 bg-[#FFD700] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md"
            >
              Agendar mi primera cita
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {userAppointments.map(appt => {
              const b = barbers.find(x => x.id === appt.barberId);
              const loc = locations.find(x => x.id === appt.locationId);

              return (
                <div
                  key={appt.id}
                  className="bg-[#1E1E1E] border border-zinc-800 hover:border-zinc-700 rounded-2xl p-5 transition-all flex flex-col justify-between gap-4"
                >
                  <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                    <div>
                      <div className="flex items-center space-x-2 mb-1">
                        <span className="text-base">💈</span>
                        <h4 className="text-sm font-extrabold text-white">{appt.serviceName}</h4>
                        <span className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                          appt.status === 'Confirmada'
                            ? 'bg-green-500/15 text-green-400 border border-green-500/30'
                            : appt.status === 'Completada'
                            ? 'bg-[#FFD700]/15 text-[#FFD700] border border-[#FFD700]/30'
                            : 'bg-red-500/15 text-red-400 border border-red-500/30'
                        }`}>
                          {appt.status}
                        </span>
                      </div>

                      <p className="text-xs text-zinc-300">
                        Barbero: <strong>{b ? b.name : 'Barbero'}</strong> · {loc ? loc.name : 'Sucursal'}
                      </p>
                      <p className="text-xs text-zinc-400 mt-0.5">
                        Fecha y Hora: <strong className="text-zinc-200">{appt.dateTime} hrs</strong>
                      </p>
                      <p className="text-xs text-[#FFD700] mt-1 font-medium">
                        Abono garantizado Webpay: {AppFormatters.formatClp(appt.prepayAmount)}
                      </p>
                    </div>

                    <div className="text-left sm:text-right">
                      <span className="text-base font-black text-[#FFD700]">
                        {AppFormatters.formatClp(appt.servicePrice)}
                      </span>
                      <span className="block text-[10px] text-zinc-400">Total del Servicio</span>
                    </div>
                  </div>

                  {/* Review Section */}
                  <div className="pt-3 border-t border-zinc-800 flex justify-between items-center text-xs">
                    {appt.hasReview ? (
                      <span className="text-xs text-[#FFD700] font-bold flex items-center space-x-1">
                        <span>★</span>
                        <span>¡Ya calificaste este servicio! Gracias por tu reseña.</span>
                      </span>
                    ) : appt.status === 'Completada' ? (
                      <>
                        <span className="text-zinc-400">¿Cómo estuvo tu atención?</span>
                        <button
                          onClick={() => setReviewingAppointment(appt)}
                          className="px-3 py-1.5 bg-[#FFD700] text-black font-bold text-xs rounded-lg hover:bg-[#E5C100] transition-colors"
                        >
                          ⭐ Calificar Corte
                        </button>
                      </>
                    ) : (
                      <span className="text-zinc-500 text-[11px]">
                        Podrás calificar una vez completada tu atención.
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Mini Modal para Calificar Cita */}
      {reviewingAppointment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-md p-6 relative shadow-2xl">
            <button
              onClick={() => setReviewingAppointment(null)}
              className="absolute top-4 right-4 text-zinc-400 hover:text-white text-lg"
            >
              ✕
            </button>

            <h3 className="text-base font-black text-white font-brand mb-1">
              VALORAR SERVICIO: {reviewingAppointment.serviceName}
            </h3>
            <p className="text-xs text-zinc-400 mb-4">
              Atención del {reviewingAppointment.dateTime}
            </p>

            <form onSubmit={handleReviewSubmit} className="space-y-4">
              <div className="text-center">
                <label className="block text-xs font-bold text-zinc-300 mb-1">
                  Puntuación:
                </label>
                <div className="flex justify-center space-x-2 text-2xl text-[#FFD700]">
                  {[1, 2, 3, 4, 5].map(star => (
                    <button
                      type="button"
                      key={star}
                      onClick={() => setReviewRating(star)}
                      className="hover:scale-125 transition-transform"
                    >
                      {star <= reviewRating ? '★' : '☆'}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">
                  Comentario sobre tu corte o atención:
                </label>
                <textarea
                  rows={3}
                  required
                  value={reviewComment}
                  onChange={e => setReviewComment(e.target.value)}
                  placeholder="Excelente corte, degradado limpio, ambiente muy agradable..."
                  className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                ></textarea>
              </div>

              <div className="flex space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setReviewingAppointment(null)}
                  className="flex-1 py-2.5 bg-zinc-800 text-zinc-300 font-bold text-xs rounded-xl"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase rounded-xl shadow-md"
                >
                  Enviar Calificación
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
