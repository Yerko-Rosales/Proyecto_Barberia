/**
 * NotificationsModal Component - Buzón de Alertas y Notificaciones en la App
 */

import { store } from '../state/store.js';

export function NotificationsModal({ isOpen, onClose, notifications }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-md p-5 relative shadow-2xl">
        <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
          <div className="flex items-center space-x-2">
            <span className="text-lg">🔔</span>
            <h3 className="text-sm font-black uppercase text-white font-brand">
              BUZÓN DE ALERTAS ({notifications.length})
            </h3>
          </div>

          <div className="flex items-center space-x-2">
            {notifications.length > 0 && (
              <button
                onClick={() => store.clearNotifications()}
                className="text-[11px] font-bold text-[#FFD700] hover:underline"
              >
                Limpiar
              </button>
            )}
            <button
              onClick={onClose}
              className="w-7 h-7 rounded-lg bg-zinc-800 text-zinc-400 hover:text-white flex items-center justify-center text-xs ml-2"
            >
              ✕
            </button>
          </div>
        </div>

        <div className="max-h-80 overflow-y-auto py-3 space-y-2.5">
          {notifications.length === 0 ? (
            <div className="py-8 text-center text-zinc-500 text-xs">
              <span className="text-3xl block mb-2">📭</span>
              No tienes nuevas notificaciones por ahora.
            </div>
          ) : (
            notifications.map(item => (
              <div
                key={item.id}
                className="p-3 rounded-xl bg-[#121212] border border-zinc-800 flex items-start space-x-3"
              >
                <span className="text-base text-[#FFD700] mt-0.5">💬</span>
                <div className="flex-1">
                  <div className="flex justify-between items-start">
                    <h5 className="text-xs font-bold text-[#FFD700]">{item.title}</h5>
                    <span className="text-[10px] text-zinc-500 font-mono">{item.timestamp}</span>
                  </div>
                  <p className="text-xs text-zinc-300 mt-0.5 leading-relaxed">{item.message}</p>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="pt-2 border-t border-zinc-800 text-center">
          <button
            onClick={onClose}
            className="w-full py-2 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase rounded-xl transition-colors"
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  );
}
