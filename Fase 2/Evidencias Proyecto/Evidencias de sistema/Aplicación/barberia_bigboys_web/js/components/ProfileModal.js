/**
 * ProfileModal Component - Edición de Datos de Perfil de Usuario
 * Actualización de nombre, celular, avatar y cambio de contraseña con verificación
 */

import { store } from '../state/store.js';

export function ProfileModal({ isOpen, onClose, user }) {
  const [name, setName] = React.useState('');
  const [phone, setPhone] = React.useState('');
  const [currPass, setCurrPass] = React.useState('');
  const [newPass, setNewPass] = React.useState('');
  const [avatar, setAvatar] = React.useState('');
  const [feedback, setFeedback] = React.useState({ text: '', isError: false });

  React.useEffect(() => {
    if (user) {
      setName(user.fullName || '');
      setPhone(user.phone || '');
      setAvatar(user.avatarUrl || '');
      setCurrPass('');
      setNewPass('');
      setFeedback({ text: '', isError: false });
    }
  }, [user, isOpen]);

  if (!isOpen || !user) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFeedback({ text: '', isError: false });

    const res = await store.updateUserProfile(name, phone, currPass, newPass, avatar);
    if (res.success) {
      setFeedback({ text: res.message, isError: false });
      setTimeout(onClose, 1800);
    } else {
      setFeedback({ text: res.message, isError: true });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in overflow-y-auto">
      <div className="bg-[#1E1E1E] border border-[#FFD700]/40 rounded-2xl w-full max-w-md p-6 relative shadow-2xl my-6">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-zinc-400 hover:text-white text-lg"
        >
          ✕
        </button>

        <div className="flex items-center space-x-3 mb-4 pb-3 border-b border-zinc-800">
          <div className="w-12 h-12 rounded-full bg-[#FFD700] text-black font-black text-xl flex items-center justify-center">
            {user.fullName ? user.fullName[0].toUpperCase() : 'U'}
          </div>
          <div>
            <h3 className="text-base font-black text-white font-brand">MI PERFIL DE CLIENTE</h3>
            <p className="text-xs text-zinc-400">{user.email}</p>
          </div>
        </div>

        {feedback.text && (
          <div className={`p-3 mb-4 rounded-xl text-xs ${
            feedback.isError
              ? 'bg-red-950/40 border border-red-500/40 text-red-300'
              : 'bg-green-950/40 border border-green-500/40 text-green-300'
          }`}>
            {feedback.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-zinc-300 mb-1">Nombre Completo</label>
            <input
              type="text"
              required
              value={name}
              onChange={e => setName(e.target.value)}
              className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-zinc-300 mb-1">Teléfono de Contacto</label>
            <input
              type="tel"
              required
              value={phone}
              onChange={e => setPhone(e.target.value)}
              className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-zinc-300 mb-1">URL de Foto de Perfil</label>
            <input
              type="url"
              value={avatar}
              onChange={e => setAvatar(e.target.value)}
              placeholder="https://..."
              className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
            />
          </div>

          <div className="pt-2 border-t border-zinc-800">
            <label className="block text-xs font-bold text-[#FFD700] mb-1">
              Cambiar Contraseña (Opcional):
            </label>
            <div className="space-y-2">
              <input
                type="password"
                value={currPass}
                onChange={e => setCurrPass(e.target.value)}
                placeholder="Contraseña actual"
                className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />

              <input
                type="password"
                value={newPass}
                onChange={e => setNewPass(e.target.value)}
                placeholder="Nueva contraseña (mínimo 6 car.)"
                className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
              />
            </div>
          </div>

          <div className="flex space-x-2 pt-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 bg-zinc-800 text-zinc-300 font-bold text-xs rounded-xl"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase rounded-xl shadow-md"
            >
              Guardar Cambios
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
