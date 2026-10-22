/**
 * App.js - Aplicación Raíz Oficial de Big Boys Barberías Chile
 * Monta React, gestiona subscripciones del Store central y coordina navegación y modales
 */

import { store } from './state/store.js';
import { SplashScreen } from './components/SplashScreen.js';
import { Navbar } from './components/Navbar.js';
import { BottomNav } from './components/BottomNav.js';
import { HomeScreen } from './components/HomeScreen.js';
import { BarbersScreen } from './components/BarbersScreen.js';
import { BranchesMapScreen } from './components/BranchesMapScreen.js';
import { ReviewsScreen } from './components/ReviewsScreen.js';
import { HistoryScreen } from './components/HistoryScreen.js';
import { AdminDashboard } from './components/AdminDashboard.js';
import { BookingWizard } from './components/BookingWizard.js';
import { WebpayModal } from './components/WebpayModal.js';
import { AuthModal } from './components/AuthModal.js';
import { ProfileModal } from './components/ProfileModal.js';
import { NotificationsModal } from './components/NotificationsModal.js';

export function App() {
  // Estado local sincronizado con el Store reactivo
  const [storeState, setStoreState] = React.useState({
    locations: store.getLocations(),
    barbers: store.getBarbers(),
    appointments: store.getAppointments(),
    reviews: store.getReviews(),
    coupons: store.getCoupons(),
    users: store.getUsers(),
    notifications: store.getNotifications(),
    emailDatabase: store.getEmailDatabase(),
    sentEmailsLog: store.getSentEmailsLog(),
    user: store.getLoggedInUser(),
    isAdmin: store.isAdmin(),
    isDarkMode: store.isDarkMode(),
    adminConfig: store.getAdminConfig(),
    gmailConfig: store.getGmailConfig(),
    userCoupon: store.getUserCoupon()
  });

  // Control de Vistas y Modales
  const [showSplash, setShowSplash] = React.useState(() => {
    if (store.isAdmin() || sessionStorage.getItem('bigboys_splash_shown')) {
      return false;
    }
    return true;
  });

  const [previewAsClient, setPreviewAsClient] = React.useState(false);
  const [activeTab, setActiveTab] = React.useState(() => {
    return store.isAdmin() ? 'admin' : 'inicio';
  });

  const [authModalOpen, setAuthModalOpen] = React.useState(false);
  const [authModalMode, setAuthModalMode] = React.useState('login');

  const [bookingWizardOpen, setBookingWizardOpen] = React.useState(false);
  const [preselectedBarber, setPreselectedBarber] = React.useState(null);
  const [preselectedLocation, setPreselectedLocation] = React.useState(null);

  const [webpayModalOpen, setWebpayModalOpen] = React.useState(false);
  const [currentBookingDraft, setCurrentBookingDraft] = React.useState(null);

  const [profileModalOpen, setProfileModalOpen] = React.useState(false);
  const [notificationsModalOpen, setNotificationsModalOpen] = React.useState(false);

  React.useEffect(() => {
    if (storeState.isAdmin && !previewAsClient) {
      setActiveTab('admin');
    }
  }, [storeState.isAdmin]);

  // Subscribirse al Store para re-renderizado automático
  React.useEffect(() => {
    const unsubscribe = store.subscribe(() => {
      setStoreState({
        locations: store.getLocations(),
        barbers: store.getBarbers(),
        appointments: store.getAppointments(),
        reviews: store.getReviews(),
        coupons: store.getCoupons(),
        users: store.getUsers(),
        notifications: store.getNotifications(),
        emailDatabase: store.getEmailDatabase(),
        sentEmailsLog: store.getSentEmailsLog(),
        user: store.getLoggedInUser(),
        isAdmin: store.isAdmin(),
        isDarkMode: store.isDarkMode(),
        adminConfig: store.getAdminConfig(),
        gmailConfig: store.getGmailConfig(),
        userCoupon: store.getUserCoupon()
      });
    });

    return unsubscribe;
  }, []);

  useEffect(() => {
    const isDark = storeState.isDarkMode;
    if (typeof document !== 'undefined') {
      document.body.classList.toggle('light-mode', !isDark);
      document.documentElement.classList.toggle('light-mode', !isDark);
      document.documentElement.classList.toggle('dark', isDark);
    }
  }, [storeState.isDarkMode]);

  // Manejadores de Flujo
  const handleOpenBooking = () => {
    setPreselectedBarber(null);
    setPreselectedLocation(null);
    setBookingWizardOpen(true);
  };

  const handleBookWithBarber = (barber) => {
    setPreselectedBarber(barber);
    setPreselectedLocation(null);
    setBookingWizardOpen(true);
  };

  const handleSelectBranchForBooking = (location) => {
    setPreselectedLocation(location);
    setPreselectedBarber(null);
    setBookingWizardOpen(true);
  };

  const handleProceedToPayment = (draft) => {
    setCurrentBookingDraft(draft);
    setBookingWizardOpen(false);
    setWebpayModalOpen(true);
  };

  const handlePaymentSuccess = () => {
    if (!currentBookingDraft) return;

    store.confirmAndBook({
      locationId: currentBookingDraft.location.id,
      barberId: currentBookingDraft.barber.id,
      serviceName: currentBookingDraft.service.name,
      servicePrice: currentBookingDraft.service.price,
      prepayAmount: currentBookingDraft.prepayAmount,
      dateTime: `${currentBookingDraft.date} ${currentBookingDraft.hour}`,
      customerName: currentBookingDraft.guestName,
      customerPhone: currentBookingDraft.guestPhone,
      customerEmail: currentBookingDraft.guestEmail,
      isGuest: !storeState.user
    });

    setWebpayModalOpen(false);
    setCurrentBookingDraft(null);
    setActiveTab('inicio');
  };

  const handleLogout = () => {
    store.logout();
    setActiveTab('inicio');
  };

  if (showSplash) {
    return (
      <SplashScreen
        onFinish={() => {
          sessionStorage.setItem('bigboys_splash_shown', 'true');
          setShowSplash(false);
        }}
      />
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-[#121212] transition-colors">
      {/* 1. Barra Superior (Navbar) */}
      <Navbar
        user={storeState.user}
        isAdmin={storeState.isAdmin}
        previewAsClient={previewAsClient}
        onTogglePreviewClient={() => {
          const next = !previewAsClient;
          setPreviewAsClient(next);
          setActiveTab(next ? 'inicio' : 'admin');
        }}
        isDarkMode={storeState.isDarkMode}
        notificationsCount={storeState.notifications.length}
        onToggleTheme={() => store.toggleDarkMode()}
        onOpenNotifications={() => setNotificationsModalOpen(true)}
        onOpenProfile={() => setProfileModalOpen(true)}
        onOpenAuth={() => {
          setAuthModalMode('login');
          setAuthModalOpen(true);
        }}
        onLogout={() => {
          store.logout();
          setPreviewAsClient(false);
          setActiveTab('inicio');
        }}
      />

      {/* Banner de previsualización para el Administrador */}
      {storeState.isAdmin && previewAsClient && (
        <div className="bg-gradient-to-r from-amber-950 via-zinc-900 to-amber-950 border-b border-[#FFD700]/50 py-2.5 px-4 text-center text-xs flex flex-wrap items-center justify-center gap-2 shadow-lg sticky top-16 z-30 animate-fade-in">
          <span className="text-[#FFD700] font-black tracking-wide">
            🛡️ VISTA PREVIA CLIENTE: Estás explorando cómo los clientes ven la web pública.
          </span>
          <button
            onClick={() => {
              setPreviewAsClient(false);
              setActiveTab('admin');
            }}
            className="px-3 py-1 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-[11px] uppercase rounded-lg shadow transition-all"
          >
            Volver al Panel de Administración &rarr;
          </button>
        </div>
      )}

      {/* 2. Navegación de Pestañas y botón flotante de Cita: SOLO si NO es admin o si está en Vista Previa Cliente */}
      {(!storeState.isAdmin || previewAsClient) && (
        <BottomNav
          activeTab={activeTab}
          onTabChange={(tab) => {
            setActiveTab(tab);
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
          hasUser={!!storeState.user}
          isAdmin={storeState.isAdmin}
          onOpenBooking={handleOpenBooking}
        />
      )}

      {/* 3. Contenedor Principal de Vistas */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-6">
        {/* Si es Admin y NO está en vista previa, mostrar EXCLUSIVAMENTE el Panel de Administración */}
        {storeState.isAdmin && !previewAsClient ? (
          <AdminDashboard
            locations={storeState.locations}
            barbers={storeState.barbers}
            appointments={storeState.appointments}
            reviews={storeState.reviews}
            coupons={storeState.coupons}
            users={storeState.users}
            emailDatabase={storeState.emailDatabase}
            sentEmailsLog={storeState.sentEmailsLog}
            adminConfig={storeState.adminConfig}
            gmailConfig={storeState.gmailConfig}
            onLogout={() => {
              store.logout();
              setPreviewAsClient(false);
              setActiveTab('inicio');
            }}
          />
        ) : (
          <>
            {activeTab === 'inicio' && (
              <HomeScreen
                user={storeState.user}
                coupon={storeState.userCoupon}
                appointments={storeState.appointments}
                locations={storeState.locations}
                onOpenBooking={handleOpenBooking}
                onOpenRegister={() => {
                  setAuthModalMode('register');
                  setAuthModalOpen(true);
                }}
                onNavigateToTab={(tab) => setActiveTab(tab)}
              />
            )}

            {activeTab === 'barberos' && (
              <BarbersScreen
                barbers={storeState.barbers}
                locations={storeState.locations}
                onBookWithBarber={handleBookWithBarber}
              />
            )}

            {activeTab === 'sucursales' && (
              <BranchesMapScreen
                locations={storeState.locations}
                barbers={storeState.barbers}
                onSelectBranchForBooking={handleSelectBranchForBooking}
                isDarkMode={storeState.isDarkMode}
              />
            )}

            {activeTab === 'resenas' && (
              <ReviewsScreen
                reviews={storeState.reviews}
                locations={storeState.locations}
                barbers={storeState.barbers}
                onNavigateToBooking={handleOpenBooking}
              />
            )}

            {activeTab === 'historial' && (
              <HistoryScreen
                user={storeState.user}
                appointments={storeState.appointments}
                locations={storeState.locations}
                barbers={storeState.barbers}
                onOpenProfile={() => setProfileModalOpen(true)}
                onOpenBooking={handleOpenBooking}
              />
            )}

            {activeTab === 'admin' && storeState.isAdmin && (
              <AdminDashboard
                locations={storeState.locations}
                barbers={storeState.barbers}
                appointments={storeState.appointments}
                reviews={storeState.reviews}
                coupons={storeState.coupons}
                users={storeState.users}
                emailDatabase={storeState.emailDatabase}
                sentEmailsLog={storeState.sentEmailsLog}
                adminConfig={storeState.adminConfig}
                gmailConfig={storeState.gmailConfig}
                onLogout={() => {
                  store.logout();
                  setPreviewAsClient(false);
                  setActiveTab('inicio');
                }}
              />
            )}
          </>
        )}
      </main>

      {/* 4. MODALES GLOBALES */}
      {authModalOpen && (
        <AuthModal
          isOpen={authModalOpen}
          initialMode={authModalMode}
          onClose={() => setAuthModalOpen(false)}
          onGuestContinue={() => {
            setAuthModalOpen(false);
            handleOpenBooking();
          }}
        />
      )}

      {bookingWizardOpen && (
        <BookingWizard
          isOpen={bookingWizardOpen}
          onClose={() => setBookingWizardOpen(false)}
          locations={storeState.locations}
          barbers={storeState.barbers}
          appointments={storeState.appointments}
          user={storeState.user}
          preselectedBarber={preselectedBarber}
          preselectedLocation={preselectedLocation}
          onProceedToPayment={handleProceedToPayment}
        />
      )}

      {webpayModalOpen && (
        <WebpayModal
          isOpen={webpayModalOpen}
          bookingDraft={currentBookingDraft}
          onPaymentSuccess={handlePaymentSuccess}
          onCancel={() => setWebpayModalOpen(false)}
        />
      )}

      {profileModalOpen && (
        <ProfileModal
          isOpen={profileModalOpen}
          onClose={() => setProfileModalOpen(false)}
          user={storeState.user}
        />
      )}

      {notificationsModalOpen && (
        <NotificationsModal
          isOpen={notificationsModalOpen}
          onClose={() => setNotificationsModalOpen(false)}
          notifications={storeState.notifications}
        />
      )}
    </div>
  );
}
