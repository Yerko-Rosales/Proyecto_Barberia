/**
 * ReviewsScreen Component - Sistema Oficial de Reseñas y Valoraciones
 * Estadísticas, filtros, tarjetas con tags y formulario para publicar nuevas valoraciones
 */

import { store } from '../state/store.js';

export const QUICK_TAGS = [
  'Puntualidad 10/10',
  'Corte Impecable',
  'Excelente Atención',
  'Ambiente VIP & Café',
  'Barbero Profesional',
  'Productos de Calidad',
  'Recomendado 100%',
  'Perfilado Perfecto'
];

export function ReviewsScreen({ reviews, locations, barbers, onNavigateToBooking }) {
  const [filterType, setFilterType] = React.useState('all'); // 'all', 'BRANCH', 'SERVICE'
  const [showModal, setShowModal] = React.useState(false);

  // Modal Form State
  const [newRating, setNewRating] = React.useState(5);
  const [newComment, setNewComment] = React.useState('');
  const [newSelectedTags, setNewSelectedTags] = React.useState([]);
  const [newReviewType, setNewReviewType] = React.useState('SERVICE');
  const [newSubject, setNewSubject] = React.useState('Corte Moderno & Fade');
  const [newLocationId, setNewLocationId] = React.useState(1);
  const [newBarberId, setNewBarberId] = React.useState(1);
  const [errorMsg, setErrorMsg] = React.useState('');

  const filteredReviews = React.useMemo(() => {
    if (filterType === 'all') return reviews;
    return reviews.filter(r => r.reviewType === filterType);
  }, [reviews, filterType]);

  const averageRating = React.useMemo(() => {
    if (!reviews.length) return 5.0;
    const sum = reviews.reduce((acc, r) => acc + r.rating, 0);
    return (sum / reviews.length).toFixed(1);
  }, [reviews]);

  const fiveStarCount = reviews.filter(r => r.rating === 5).length;
  const fourStarCount = reviews.filter(r => r.rating === 4).length;

  const toggleTag = (tag) => {
    if (newSelectedTags.includes(tag)) {
      setNewSelectedTags(newSelectedTags.filter(t => t !== tag));
    } else {
      setNewSelectedTags([...newSelectedTags, tag]);
    }
  };

  const handleSubmitReview = (e) => {
    e.preventDefault();
    if (!newComment.trim()) {
      setErrorMsg('Por favor escribe tu opinión.');
      return;
    }

    store.submitReview({
      locationId: parseInt(newLocationId, 10),
      barberId: newReviewType === 'SERVICE' ? parseInt(newBarberId, 10) : null,
      rating: newRating,
      comment: newComment,
      tags: newSelectedTags,
      serviceOrSubject: newSubject,
      reviewType: newReviewType
    });

    setNewComment('');
    setNewSelectedTags([]);
    setShowModal(false);
    setErrorMsg('');
  };

  return (
    <div className="space-y-6 pb-20 md:pb-12 animate-fade-in">
      {/* Header & Stats Summary */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-zinc-800 pb-6">
        <div>
          <div className="flex items-center space-x-2">
            <span className="text-xl">⭐</span>
            <h2 className="text-xl sm:text-2xl font-black italic tracking-tighter text-white font-brand">
              OPINIONES Y EXPERIENCIAS REALES
            </h2>
          </div>
          <p className="text-xs text-zinc-400">
            Conoce los testimonios de clientes satisfechos en nuestras sucursales de Talagante y Peñaflor.
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="px-5 py-3 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-[0_0_15px_rgba(255,215,0,0.3)] transition-all flex items-center space-x-2"
        >
          <span>✍️</span>
          <span>Dejar mi Reseña</span>
        </button>
      </div>

      {/* Ratings Overview Card */}
      <div className="bg-[#1E1E1E] border border-zinc-800 rounded-3xl p-6 flex flex-col sm:flex-row justify-around items-center gap-6">
        <div className="text-center sm:text-left flex items-center space-x-4">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-[#FFD700] to-[#B8860B] flex flex-col items-center justify-center text-black shadow-lg">
            <span className="text-3xl font-black">{averageRating}</span>
            <span className="text-[10px] font-black">DE 5.0</span>
          </div>

          <div>
            <div className="flex text-[#FFD700] text-lg mb-1">
              ★★★★★
            </div>
            <p className="text-xs font-bold text-white">Basado en {reviews.length} valoraciones</p>
            <p className="text-[11px] text-[#2ECC71] font-semibold">99.4% Clientes Satisfechos</p>
          </div>
        </div>

        {/* Progress Breakdown */}
        <div className="w-full sm:w-64 space-y-2">
          <div className="flex items-center space-x-2 text-xs">
            <span className="w-12 text-zinc-400 font-bold">5 Estrellas</span>
            <div className="flex-1 h-2 bg-zinc-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-[#FFD700] rounded-full"
                style={{ width: `${reviews.length ? (fiveStarCount / reviews.length) * 100 : 0}%` }}
              ></div>
            </div>
            <span className="w-6 text-right text-zinc-400 font-mono text-[10px]">{fiveStarCount}</span>
          </div>

          <div className="flex items-center space-x-2 text-xs">
            <span className="w-12 text-zinc-400 font-bold">4 Estrellas</span>
            <div className="flex-1 h-2 bg-zinc-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-[#FFD700]/70 rounded-full"
                style={{ width: `${reviews.length ? (fourStarCount / reviews.length) * 100 : 0}%` }}
              ></div>
            </div>
            <span className="w-6 text-right text-zinc-400 font-mono text-[10px]">{fourStarCount}</span>
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex space-x-2 overflow-x-auto pb-1">
        <button
          onClick={() => setFilterType('all')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filterType === 'all'
              ? 'bg-[#FFD700] text-black shadow-md'
              : 'bg-[#1E1E1E] text-zinc-300 hover:border-[#FFD700]/30 border border-zinc-800'
          }`}
        >
          Todas ({reviews.length})
        </button>

        <button
          onClick={() => setFilterType('SERVICE')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filterType === 'SERVICE'
              ? 'bg-[#FFD700] text-black shadow-md'
              : 'bg-[#1E1E1E] text-zinc-300 hover:border-[#FFD700]/30 border border-zinc-800'
          }`}
        >
          ✂️ Por Servicio / Barbero
        </button>

        <button
          onClick={() => setFilterType('BRANCH')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filterType === 'BRANCH'
              ? 'bg-[#FFD700] text-black shadow-md'
              : 'bg-[#1E1E1E] text-zinc-300 hover:border-[#FFD700]/30 border border-zinc-800'
          }`}
        >
          📍 Por Sucursal
        </button>
      </div>

      {/* Reviews List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filteredReviews.map(rev => (
          <div
            key={rev.id}
            className="bg-[#1E1E1E] border border-zinc-800 hover:border-[#FFD700]/30 rounded-2xl p-5 flex flex-col justify-between transition-all"
          >
            <div>
              <div className="flex justify-between items-start mb-3">
                <div className="flex items-center space-x-3">
                  {rev.authorAvatarUrl ? (
                    <img
                      src={rev.authorAvatarUrl}
                      alt={rev.authorName}
                      className="w-10 h-10 rounded-full object-cover border border-[#FFD700]"
                    />
                  ) : (
                    <div className="w-10 h-10 rounded-full bg-[#FFD700] text-black flex items-center justify-center font-black text-xs">
                      {rev.authorName ? rev.authorName[0].toUpperCase() : 'C'}
                    </div>
                  )}

                  <div>
                    <h4 className="text-xs font-bold text-white">{rev.authorName}</h4>
                    <span className="text-[10px] text-[#FFD700] font-semibold block">
                      {rev.serviceOrSubject}
                    </span>
                  </div>
                </div>

                <div className="text-right">
                  <div className="text-[#FFD700] text-xs">
                    {'★'.repeat(rev.rating)}
                    <span className="text-zinc-600">{'★'.repeat(5 - rev.rating)}</span>
                  </div>
                  <span className="text-[10px] text-zinc-500">{rev.date}</span>
                </div>
              </div>

              <p className="text-xs text-zinc-300 leading-relaxed mb-4">
                "{rev.comment}"
              </p>
            </div>

            {rev.tags && (
              <div className="flex flex-wrap gap-1.5 pt-3 border-t border-zinc-800">
                {rev.tags.split(',').map((tag, idx) => (
                  <span
                    key={idx}
                    className="px-2 py-0.5 bg-[#FFD700]/10 border border-[#FFD700]/20 text-[#FFD700] text-[9px] font-bold rounded-md"
                  >
                    #{tag.trim()}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Modal Nueva Reseña */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-lg p-6 relative shadow-2xl max-h-[85vh] overflow-y-auto">
            <button
              onClick={() => setShowModal(false)}
              className="absolute top-4 right-4 text-zinc-400 hover:text-white text-lg"
            >
              ✕
            </button>

            <div className="text-center mb-5">
              <span className="text-3xl block mb-1">⭐</span>
              <h3 className="text-xl font-black italic text-white font-brand">
                DEJAR MI RESEÑA OFICIAL
              </h3>
              <p className="text-xs text-zinc-400">
                Tu opinión nos ayuda a mantener el estándar de excelencia en Big Boys Chile.
              </p>
            </div>

            {errorMsg && (
              <div className="mb-4 p-3 bg-red-950/40 border border-red-500/50 rounded-xl text-red-300 text-xs">
                ⚠️ {errorMsg}
              </div>
            )}

            <form onSubmit={handleSubmitReview} className="space-y-4">
              {/* Star Rating Selector */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 text-center mb-1">
                  Tu Calificación:
                </label>
                <div className="flex justify-center space-x-2">
                  {[1, 2, 3, 4, 5].map(star => (
                    <button
                      type="button"
                      key={star}
                      onClick={() => setNewRating(star)}
                      className={`text-3xl transition-transform hover:scale-125 ${
                        star <= newRating ? 'text-[#FFD700]' : 'text-zinc-600'
                      }`}
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>

              {/* Review Type */}
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setNewReviewType('SERVICE')}
                  className={`py-2 text-xs font-bold rounded-xl border transition-colors ${
                    newReviewType === 'SERVICE'
                      ? 'bg-[#FFD700] text-black border-[#FFD700]'
                      : 'bg-[#121212] text-zinc-400 border-zinc-800'
                  }`}
                >
                  ✂️ Servicio / Barbero
                </button>
                <button
                  type="button"
                  onClick={() => setNewReviewType('BRANCH')}
                  className={`py-2 text-xs font-bold rounded-xl border transition-colors ${
                    newReviewType === 'BRANCH'
                      ? 'bg-[#FFD700] text-black border-[#FFD700]'
                      : 'bg-[#121212] text-zinc-400 border-zinc-800'
                  }`}
                >
                  📍 Sucursal / Local
                </button>
              </div>

              {/* Select Branch */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">Sucursal</label>
                <select
                  value={newLocationId}
                  onChange={e => setNewLocationId(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                >
                  {locations.map(loc => (
                    <option key={loc.id} value={loc.id}>{loc.name}</option>
                  ))}
                </select>
              </div>

              {newReviewType === 'SERVICE' && (
                <div>
                  <label className="block text-xs font-bold text-zinc-300 mb-1">Barbero que te atendió</label>
                  <select
                    value={newBarberId}
                    onChange={e => {
                      setNewBarberId(e.target.value);
                      const b = barbers.find(x => x.id === parseInt(e.target.value, 10));
                      if (b) setNewSubject(b.specialty);
                    }}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                  >
                    {barbers.map(b => (
                      <option key={b.id} value={b.id}>{b.name} ({b.specialty})</option>
                    ))}
                  </select>
                </div>
              )}

              {/* Quick Tags Pills */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1.5">
                  Etiquetas destacadas (selecciona las que correspondan):
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {QUICK_TAGS.map(tag => {
                    const isSelected = newSelectedTags.includes(tag);
                    return (
                      <button
                        type="button"
                        key={tag}
                        onClick={() => toggleTag(tag)}
                        className={`px-2.5 py-1 rounded-lg text-[10px] font-bold transition-colors ${
                          isSelected
                            ? 'bg-[#FFD700] text-black'
                            : 'bg-zinc-800 text-zinc-400 hover:text-white'
                        }`}
                      >
                        {isSelected ? '✓ ' : '+ '}{tag}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Comment Textarea */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">Tu Experiencia</label>
                <textarea
                  rows={3}
                  required
                  value={newComment}
                  onChange={e => setNewComment(e.target.value)}
                  placeholder="Describe cómo fue la atención, el corte, la puntualidad..."
                  className="w-full px-3.5 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs focus:outline-none focus:border-[#FFD700]"
                ></textarea>
              </div>

              <div className="flex space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 py-2.5 bg-zinc-800 text-zinc-300 font-bold text-xs rounded-xl"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md"
                >
                  Publicar Reseña
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
