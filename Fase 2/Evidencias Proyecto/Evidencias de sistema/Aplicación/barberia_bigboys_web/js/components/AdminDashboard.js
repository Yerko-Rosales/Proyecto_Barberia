/**
 * AdminDashboard Component - Panel de Control Central de Big Boys
 */

import { AppFormatters } from '../utils/formatters.js';
import { store, SERVICES_LIST } from '../state/store.js';

export function AdminDashboard({ locations, barbers, appointments, reviews, coupons, users, emailDatabase = [], sentEmailsLog = [], adminConfig, gmailConfig, onLogout }) {
      const [subTab, setSubTab] = useState('reportes');

      // Estado de Base de Correos
      const [emailViewMode, setEmailViewMode] = useState('database'); // 'database' | 'sent_logs'
      const [emailSearch, setEmailSearch] = useState('');
      const [emailSourceFilter, setEmailSourceFilter] = useState('all');
      const [showAddEmailModal, setShowAddEmailModal] = useState(false);
      const [newEmailVal, setNewEmailVal] = useState('');
      const [newNameVal, setNewNameVal] = useState('');
      const [newPhoneVal, setNewPhoneVal] = useState('');
      const [newNotesVal, setNewNotesVal] = useState('');
      const [addEmailFeedback, setAddEmailFeedback] = useState('');
      const [copySuccess, setCopySuccess] = useState(false);
      const [viewingEmailMsg, setViewingEmailMsg] = useState(null);

      // --- Estados para Gestión de Barberos (Agregar, Editar, Eliminar) ---
      const [showAddBarberModal, setShowAddBarberModal] = useState(false);
      const [showEditBarberModal, setShowEditBarberModal] = useState(false);
      const [editingBarber, setEditingBarber] = useState(null);
      const [barberLocFilter, setBarberLocFilter] = useState('all');
      const [barberSearch, setBarberSearch] = useState('');

      // Formulario Barbero (Agregar / Editar)
      const [barberNameInput, setBarberNameInput] = useState('');
      const [barberLocIdInput, setBarberLocIdInput] = useState(1);
      const [barberSpecialtyInput, setBarberSpecialtyInput] = useState('');
      const [barberBioInput, setBarberBioInput] = useState('');
      const [barberAvatarInput, setBarberAvatarInput] = useState('');
      const [barberHoursInput, setBarberHoursInput] = useState('09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00');
      const [barberFeedbackMsg, setBarberFeedbackMsg] = useState('');

      // --- Estados para Generador de Excel (.xlsx) con Periodos ---
      const [excelPeriod, setExcelPeriod] = useState('mensual'); // 'diario' | 'semanal' | 'mensual' | 'anual' | 'historico'
      const [excelBranchFilter, setExcelBranchFilter] = useState('all');
      const [excelCustomDate, setExcelCustomDate] = useState(() => {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
      });
      const [excelFeedback, setExcelFeedback] = useState('');

      // --- Estados para Agenda y Cancelación ---
      const [agendaStatusFilter, setAgendaStatusFilter] = useState('all');
      const [cancelFeedbackMsg, setCancelFeedbackMsg] = useState('');

      const filteredAgendaAppointments = useMemo(() => {
        if (agendaStatusFilter === 'all') return appointments;
        return appointments.filter(a => a.status === agendaStatusFilter);
      }, [appointments, agendaStatusFilter]);

      // Handlers de Gestión de Barberos
      const handleOpenAddBarber = () => {
        setBarberNameInput('');
        setBarberLocIdInput(locations[0]?.id || 1);
        setBarberSpecialtyInput('');
        setBarberBioInput('');
        setBarberAvatarInput('https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300');
        setBarberHoursInput('09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00');
        setBarberFeedbackMsg('');
        setShowAddBarberModal(true);
      };

      const handleOpenEditBarber = (b) => {
        setEditingBarber(b);
        setBarberNameInput(b.name || '');
        setBarberLocIdInput(b.locationId || 1);
        setBarberSpecialtyInput(b.specialty || '');
        setBarberBioInput(b.bio || '');
        setBarberAvatarInput(b.avatarUrl || '');
        setBarberHoursInput(b.availableHours === 'NO_DISPONIBLE' ? '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00' : (b.availableHours || '09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00'));
        setBarberFeedbackMsg('');
        setShowEditBarberModal(true);
      };

      const handleSaveNewBarber = (e) => {
        e.preventDefault();
        if (!barberNameInput.trim()) {
          setBarberFeedbackMsg('Ingresa el nombre del barbero.');
          return;
        }
        store.addBarberAdmin({
          name: barberNameInput,
          locationId: barberLocIdInput,
          specialty: barberSpecialtyInput,
          bio: barberBioInput,
          avatarUrl: barberAvatarInput,
          initialHours: barberHoursInput
        });
        setBarberFeedbackMsg('✓ Barbero agregado exitosamente al equipo.');
        setTimeout(() => {
          setShowAddBarberModal(false);
          setBarberFeedbackMsg('');
        }, 1200);
      };

      const handleSaveEditBarber = (e) => {
        e.preventDefault();
        if (!editingBarber) return;
        if (!barberNameInput.trim()) {
          setBarberFeedbackMsg('El nombre no puede estar vacío.');
          return;
        }
        store.updateBarberAdmin(editingBarber.id, {
          name: barberNameInput,
          locationId: barberLocIdInput,
          specialty: barberSpecialtyInput,
          bio: barberBioInput,
          avatarUrl: barberAvatarInput,
          availableHours: barberHoursInput
        });
        setBarberFeedbackMsg('✓ Cambios guardados correctamente.');
        setTimeout(() => {
          setShowEditBarberModal(false);
          setEditingBarber(null);
          setBarberFeedbackMsg('');
        }, 1200);
      };

      const handleDeleteBarber = (b) => {
        if (confirm(`¿Estás seguro de eliminar a "${b.name}" del equipo de barberos? Esta acción no se puede deshacer.`)) {
          store.deleteBarberAdmin(b.id);
        }
      };

      // Lista filtrada de barberos
      const filteredBarbersList = useMemo(() => {
        return barbers.filter(b => {
          const matchesLoc = barberLocFilter === 'all' || b.locationId === parseInt(barberLocFilter, 10);
          const q = barberSearch.toLowerCase();
          const matchesSearch = (b.name || '').toLowerCase().includes(q) ||
            (b.specialty || '').toLowerCase().includes(q);
          return matchesLoc && matchesSearch;
        });
      }, [barbers, barberLocFilter, barberSearch]);

      // Helper para parsear fecha de cita "dd-MM-yyyy HH:mm" o "yyyy-MM-dd HH:mm"
      const parseApptDateTime = (dateTimeStr) => {
        if (!dateTimeStr) return new Date(0);
        const parts = dateTimeStr.trim().split(' ');
        const datePart = parts[0] || '';
        const timePart = parts[1] || '00:00';
        const [hour, minute] = timePart.split(':').map(Number);

        const dateTokens = datePart.split('-').map(Number);
        if (dateTokens.length !== 3) return new Date(0);

        let year, month, day;
        if (dateTokens[0] > 1000) {
          // yyyy-MM-dd
          year = dateTokens[0];
          month = dateTokens[1];
          day = dateTokens[2];
        } else {
          // dd-MM-yyyy
          day = dateTokens[0];
          month = dateTokens[1];
          year = dateTokens[2];
        }
        if (!day || !month || !year) return new Date(0);
        return new Date(year, month - 1, day, hour || 0, minute || 0);
      };

      // Citas filtradas para el Centro de Exportación Excel
      const filteredExcelAppointments = useMemo(() => {
        const now = new Date();
        const customD = new Date(excelCustomDate + 'T00:00:00');
        const refDate = isNaN(customD.getTime()) ? now : customD;

        return appointments.filter(appt => {
          if (excelBranchFilter !== 'all' && appt.locationId !== parseInt(excelBranchFilter, 10)) {
            return false;
          }

          const apptDate = parseApptDateTime(appt.dateTime);
          if (isNaN(apptDate.getTime())) return false;

          if (excelPeriod === 'diario') {
            return (
              apptDate.getDate() === refDate.getDate() &&
              apptDate.getMonth() === refDate.getMonth() &&
              apptDate.getFullYear() === refDate.getFullYear()
            );
          } else if (excelPeriod === 'semanal') {
            const diffMs = refDate.getTime() - apptDate.getTime();
            const diffDays = diffMs / (1000 * 60 * 60 * 24);
            return diffDays >= -1 && diffDays <= 7;
          } else if (excelPeriod === 'mensual') {
            return (
              apptDate.getMonth() === refDate.getMonth() &&
              apptDate.getFullYear() === refDate.getFullYear()
            );
          } else if (excelPeriod === 'anual') {
            return apptDate.getFullYear() === refDate.getFullYear();
          }
          return true; // 'historico'
        });
      }, [appointments, excelPeriod, excelBranchFilter, excelCustomDate]);

      // Generador de Libro Excel Real (.xlsx) con SheetJS y fallback XML
      const handleGenerateRealExcel = () => {
        const periodLabels = {
          diario: `Diario (${excelCustomDate})`,
          semanal: 'Semanal (Últimos 7 días)',
          mensual: 'Mensual (Mes en Curso)',
          anual: 'Anual (Año en Curso)',
          historico: 'Histórico Completo'
        };

        const totalVentas = filteredExcelAppointments.reduce((sum, a) => sum + (a.servicePrice || 0), 0);
        const totalAbonos = filteredExcelAppointments.reduce((sum, a) => sum + (a.prepayAmount || 0), 0);
        const totalSaldo = filteredExcelAppointments.reduce((sum, a) => sum + Math.max(0, (a.servicePrice || 0) - (a.prepayAmount || 0)), 0);
        const totalCompletadas = filteredExcelAppointments.filter(a => a.status === 'Completada').length;
        const totalCanceladas = filteredExcelAppointments.filter(a => a.status === 'Cancelada').length;

        // Hoja 1: Movimientos_y_Citas
        const rowsSheet1 = filteredExcelAppointments.map((a, idx) => {
          const loc = locations.find(l => l.id === a.locationId)?.name || 'Big Boys';
          const b = barbers.find(x => x.id === a.barberId)?.name || `Barbero #${a.barberId}`;
          const saldo = Math.max(0, (a.servicePrice || 0) - (a.prepayAmount || 0));
          return {
            "N°": idx + 1,
            "ID Cita": a.id,
            "Fecha y Hora": a.dateTime,
            "Sucursal": loc,
            "Barbero": b,
            "Cliente": a.customerName || 'Cliente',
            "Teléfono": a.customerPhone || '—',
            "Correo": a.customerEmail || '—',
            "Tipo Cliente": a.isGuest ? 'Invitado' : 'Registrado (VIP)',
            "Servicio": a.serviceName,
            "Precio Total CLP": a.servicePrice || 0,
            "Abono Webpay CLP": a.prepayAmount || 0,
            "Saldo en Local CLP": saldo,
            "Estado": a.status
          };
        });

        // Hoja 2: Resumen_Ejecutivo
        const rowsSheet2 = [
          { "Métrica": "Empresa", "Valor": "Big Boys Barberías Chile" },
          { "Métrica": "Periodo del Reporte", "Valor": periodLabels[excelPeriod] },
          { "Métrica": "Filtro Sucursal", "Valor": excelBranchFilter === 'all' ? 'Todas las Sucursales' : (locations.find(l => l.id === parseInt(excelBranchFilter, 10))?.name || 'Sucursal') },
          { "Métrica": "Fecha de Generación", "Valor": AppFormatters.currentDateTimeString() },
          { "Métrica": "Total de Citas Registradas", "Valor": filteredExcelAppointments.length },
          { "Métrica": "Citas Atendidas / Completadas", "Valor": totalCompletadas },
          { "Métrica": "Citas Canceladas / Anuladas", "Valor": totalCanceladas },
          { "Métrica": "Recaudación Total", "Valor": AppFormatters.formatClp(totalVentas) },
          { "Métrica": "Total Abonos Webpay Plus", "Valor": AppFormatters.formatClp(totalAbonos) },
          { "Métrica": "Saldo Total a Cobrar en Local", "Valor": AppFormatters.formatClp(totalSaldo) }
        ];

        // Desglose por Barbero
        const barberBreakdown = {};
        filteredExcelAppointments.forEach(a => {
          const b = barbers.find(x => x.id === a.barberId)?.name || `Barbero #${a.barberId}`;
          if (!barberBreakdown[b]) barberBreakdown[b] = { count: 0, total: 0 };
          barberBreakdown[b].count++;
          barberBreakdown[b].total += (a.servicePrice || 0);
        });
        Object.keys(barberBreakdown).forEach(bName => {
          rowsSheet2.push({
            "Métrica": `Rendimiento - ${bName}`,
            "Valor": `${barberBreakdown[bName].count} citas | ${AppFormatters.formatClp(barberBreakdown[bName].total)}`
          });
        });

        const filename = `BigBoys_Reporte_Excel_${excelPeriod.toUpperCase()}_${AppFormatters.currentDateString()}.xlsx`;

        if (window.XLSX) {
          const wb = window.XLSX.utils.book_new();

          const wsCitas = window.XLSX.utils.json_to_sheet(rowsSheet1.length > 0 ? rowsSheet1 : [{ "Información": "No hay movimientos registrados en este periodo" }]);
          const wsResumen = window.XLSX.utils.json_to_sheet(rowsSheet2);

          wsCitas['!cols'] = [
            { wch: 6 }, { wch: 14 }, { wch: 18 }, { wch: 28 }, { wch: 20 },
            { wch: 22 }, { wch: 16 }, { wch: 26 }, { wch: 18 }, { wch: 26 },
            { wch: 18 }, { wch: 18 }, { wch: 18 }, { wch: 14 }
          ];
          wsResumen['!cols'] = [{ wch: 35 }, { wch: 45 }];

          window.XLSX.utils.book_append_sheet(wb, wsCitas, "Movimientos_y_Citas");
          window.XLSX.utils.book_append_sheet(wb, wsResumen, "Resumen_Ejecutivo");

          window.XLSX.writeFile(wb, filename);
          setExcelFeedback(`✓ Archivo Excel generado con éxito: ${filename}`);
          setTimeout(() => setExcelFeedback(''), 4500);
        } else {
          // Fallback nativo oficial XML Spreadsheet para Microsoft Excel (.xls)
          let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
          xml += '<?mso-application progid="Excel.Sheet"?>\n';
          xml += '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">\n';
          
          xml += '<Worksheet ss:Name="Movimientos_y_Citas"><Table>\n';
          if (rowsSheet1.length > 0) {
            const keys = Object.keys(rowsSheet1[0]);
            xml += '<Row>';
            keys.forEach(k => xml += `<Cell><Data ss:Type="String">${k}</Data></Cell>`);
            xml += '</Row>\n';
            rowsSheet1.forEach(r => {
              xml += '<Row>';
              keys.forEach(k => {
                const val = r[k];
                const isNum = typeof val === 'number';
                xml += `<Cell><Data ss:Type="${isNum ? 'Number' : 'String'}">${val}</Data></Cell>`;
              });
              xml += '</Row>\n';
            });
          }
          xml += '</Table></Worksheet>\n';

          xml += '<Worksheet ss:Name="Resumen_Ejecutivo"><Table>\n';
          xml += '<Row><Cell><Data ss:Type="String">Métrica</Data></Cell><Cell><Data ss:Type="String">Valor</Data></Cell></Row>\n';
          rowsSheet2.forEach(sr => {
            xml += `<Row><Cell><Data ss:Type="String">${sr.Métrica}</Data></Cell><Cell><Data ss:Type="String">${sr.Valor}</Data></Cell></Row>\n`;
          });
          xml += '</Table></Worksheet>\n';

          xml += '</Workbook>';
          const blob = new Blob([xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = filename.replace('.xlsx', '.xls');
          a.click();
          setExcelFeedback(`✓ Archivo Excel generado: ${filename.replace('.xlsx', '.xls')}`);
          setTimeout(() => setExcelFeedback(''), 4500);
        }
      };

      const completedAppts = appointments.filter(a => a.status === 'Completada');
      const totalRevenue = completedAppts.reduce((sum, a) => sum + (a.servicePrice || 0), 0);
      const totalPrepay = completedAppts.reduce((sum, a) => sum + (a.prepayAmount || 0), 0);

      // Exportar TSV de citas para Excel
      const handleExportAppointmentsCsv = () => {
        const header = "ID_Cita\tFecha_Corte\tSucursal\tBarbero\tCliente\tCelular\tServicio\tTotal_Cobrado\tPrepago_Online\tEstado\n";
        const rows = appointments.map(a => {
          const b = barbers.find(x => x.id === a.barberId)?.name || `Barbero #${a.barberId}`;
          const loc = locations.find(x => x.id === a.locationId)?.name || 'Chile';
          return `${a.id}\t${a.dateTime}\t${loc}\t${b}\t${a.customerName}\t${a.customerPhone}\t${a.serviceName}\t${a.servicePrice}\t${a.prepayAmount}\t${a.status}`;
        }).join('\n');

        const blob = new Blob([header + rows], { type: 'text/tab-separated-values;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `BigBoys_Movimientos_${AppFormatters.currentDateString()}.tsv`;
        link.click();
      };

      // Exportar Base de Correos (.csv compatible con Excel)
      const handleExportEmailsCsv = () => {
        const header = "ID;Correo;Nombre_Cliente;Telefono;Origen;Fecha_Registro;Ultima_Actividad;Total_Interacciones;Notas\n";
        const rows = (emailDatabase || []).map(e => {
          const cleanNotes = (e.notes || '').replace(/"/g, '""');
          return `${e.id};"${e.email}";"${e.fullName || ''}";"${e.phone || ''}";"${e.source || ''}";"${e.addedAt || ''}";"${e.lastActivityAt || ''}";${e.interactionsCount || 1};"${cleanNotes}"`;
        }).join('\n');

        const bom = "\uFEFF"; // Para visualización inmediata de tildes y caracteres en Excel
        const blob = new Blob([bom + header + rows], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `BigBoys_Base_Correos_${AppFormatters.currentDateString()}.csv`;
        link.click();
      };

      // Copiar todos los correos al portapapeles
      const handleCopyAllEmails = () => {
        if (!emailDatabase || emailDatabase.length === 0) return;
        const allMails = emailDatabase.map(e => e.email).filter(Boolean).join(', ');
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(allMails);
        } else {
          const textarea = document.createElement('textarea');
          textarea.value = allMails;
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand('copy');
          document.body.removeChild(textarea);
        }
        setCopySuccess(true);
        setTimeout(() => setCopySuccess(false), 3000);
      };

      // Agregar correo manual
      const handleAddManualEmail = (e) => {
        e.preventDefault();
        if (!newEmailVal || !newEmailVal.includes('@')) {
          setAddEmailFeedback('Ingresa un correo electrónico válido.');
          return;
        }
        store.saveEmailToDatabase(newEmailVal, newNameVal, newPhoneVal, 'Ingreso Manual Admin', newNotesVal || 'Contacto agregado desde Panel Admin');
        setNewEmailVal('');
        setNewNameVal('');
        setNewPhoneVal('');
        setNewNotesVal('');
        setAddEmailFeedback('✓ Correo guardado exitosamente en la base de datos.');
        setTimeout(() => {
          setAddEmailFeedback('');
          setShowAddEmailModal(false);
        }, 1500);
      };

      // Filtro reactivo de correos
      const filteredEmails = useMemo(() => {
        return (emailDatabase || []).filter(item => {
          const query = emailSearch.toLowerCase();
          const matchesSearch = (item.email || '').toLowerCase().includes(query) ||
            (item.fullName || '').toLowerCase().includes(query) ||
            (item.phone || '').includes(query);
          const matchesSource = emailSourceFilter === 'all' || item.source === emailSourceFilter;
          return matchesSearch && matchesSource;
        });
      }, [emailDatabase, emailSearch, emailSourceFilter]);

      // Canjear Cupón
      const [couponInput, setCouponInput] = useState('');
      const [redeemMsg, setRedeemMsg] = useState(null);

      const handleRedeem = (e) => {
        e.preventDefault();
        const res = store.redeemCoupon(couponInput);
        setRedeemMsg(res);
        if (res.success) setCouponInput('');
      };

      return (
        <div className="space-y-6 pb-20 md:pb-12 animate-fade-in">
          <div className="p-4 rounded-2xl bg-[#1E293B] border border-[#334155] flex flex-wrap justify-between items-center gap-3 text-xs">
            <div className="flex items-center space-x-2">
              <span className="w-2.5 h-2.5 rounded-full bg-green-500 animate-pulse"></span>
              <strong className="text-white">Citas: {appointments.length}</strong>
              <span className="text-zinc-400">· Barberos: {barbers.length}</span>
              <span className="text-[#FFD700] font-bold">· Correos Guardados: {(emailDatabase || []).length}</span>
            </div>
            <button onClick={onLogout} className="px-3 py-1 bg-red-950/40 text-red-400 border border-red-500/40 rounded-lg font-bold">
              Salir de Admin ✕
            </button>
          </div>

          <div className="flex space-x-2 overflow-x-auto pb-1 scrollbar-none border-b border-zinc-800">
            {[
              { id: 'reportes', label: '📊 Ventas' },
              { id: 'agenda', label: '📅 Agenda' },
              { id: 'correos_db', label: `📧 Base de Correos (${(emailDatabase || []).length})` },
              { id: 'cupones', label: '🎁 Cupones PIN' },
              { id: 'barberos_mgr', label: `💈 Barberos (${barbers.length})` },
              { id: 'excel', label: '📊 Reportes Excel (.xlsx)' }
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setSubTab(tab.id)}
                className={`px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${subTab === tab.id ? 'bg-[#FFD700] text-black shadow-md font-black' : 'bg-[#1E1E1E] text-zinc-400 hover:text-white'}`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {subTab === 'reportes' && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-5">
                  <span className="text-[10px] text-zinc-400 font-bold uppercase block">Recaudación Total</span>
                  <h3 className="text-2xl font-black text-[#FFD700] mt-1">{AppFormatters.formatClp(totalRevenue)}</h3>
                </div>
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-5">
                  <span className="text-[10px] text-zinc-400 font-bold uppercase block">Abonos Garantizados</span>
                  <h3 className="text-2xl font-black text-green-400 mt-1">{AppFormatters.formatClp(totalPrepay)}</h3>
                </div>
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-5">
                  <span className="text-[10px] text-zinc-400 font-bold uppercase block">Cortes Realizados</span>
                  <h3 className="text-2xl font-black text-white mt-1">{completedAppts.length}</h3>
                </div>
              </div>
            </div>
          )}

          {subTab === 'agenda' && (
            <div className="space-y-4 animate-fade-in">
              <div className="flex flex-wrap justify-between items-center gap-3">
                <div>
                  <h4 className="text-sm font-black text-white uppercase tracking-wider">
                    Control de Agenda & Notificaciones Automáticas ({appointments.length})
                  </h4>
                  <p className="text-xs text-zinc-400 mt-0.5">
                    Al anular una cita, se despacha automáticamente el correo de disculpas a <strong>bigboysbarberia.chile@gmail.com</strong> solicitando los datos bancarios del cliente para reembolsar su abono.
                  </p>
                </div>

                <div className="flex space-x-1.5 bg-[#1E1E1E] p-1 rounded-xl border border-zinc-800 text-xs">
                  {[
                    { id: 'all', label: `Todas (${appointments.length})` },
                    { id: 'Confirmada', label: 'Confirmadas' },
                    { id: 'Completada', label: 'Completadas' },
                    { id: 'Cancelada', label: 'Canceladas' }
                  ].map(tab => (
                    <button
                      key={tab.id}
                      onClick={() => setAgendaStatusFilter(tab.id)}
                      className={`px-3 py-1 rounded-lg font-bold transition-all ${
                        agendaStatusFilter === tab.id
                          ? 'bg-[#FFD700] text-black shadow font-black'
                          : 'text-zinc-400 hover:text-white'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>
              </div>

              {cancelFeedbackMsg && (
                <div className="p-3.5 bg-emerald-950/40 border border-emerald-500/50 rounded-xl text-emerald-300 text-xs font-bold animate-fade-in flex items-center space-x-2 shadow-lg">
                  <span className="text-base">✉️</span>
                  <span>{cancelFeedbackMsg}</span>
                </div>
              )}

              <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl overflow-x-auto shadow-xl">
                <table className="w-full text-left text-xs text-zinc-300">
                  <thead className="bg-[#121212] text-zinc-400 uppercase text-[10px] tracking-wider border-b border-zinc-800">
                    <tr>
                      <th className="p-3.5">ID / Fecha</th>
                      <th className="p-3.5">Cliente</th>
                      <th className="p-3.5">Barbero & Sucursal</th>
                      <th className="p-3.5">Servicio</th>
                      <th className="p-3.5">Estado</th>
                      <th className="p-3.5 text-right">Acción / Cancelación</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/60">
                    {filteredAgendaAppointments.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="p-8 text-center text-zinc-500 text-xs">
                          No hay citas en este filtro ({agendaStatusFilter}).
                        </td>
                      </tr>
                    ) : (
                      filteredAgendaAppointments.map(a => {
                        const b = barbers.find(x => x.id === a.barberId)?.name || `Barbero #${a.barberId}`;
                        const loc = locations.find(l => l.id === a.locationId)?.name || 'Big Boys';

                        return (
                          <tr key={a.id} className="hover:bg-zinc-800/30 transition-colors">
                            <td className="p-3.5 font-mono">
                              <span className="font-bold text-white block">#{a.id}</span>
                              <span className="text-zinc-400 text-[11px]">{a.dateTime}</span>
                            </td>
                            <td className="p-3.5">
                              <strong className="text-white block">{a.customerName}</strong>
                              <span className="text-zinc-400 text-[11px] block">{a.customerPhone}</span>
                              <span className="text-[#FFD700] text-[10px] block truncate">{a.customerEmail || 'Sin email'}</span>
                            </td>
                            <td className="p-3.5">
                              <span className="text-white font-bold block">{b}</span>
                              <span className="text-zinc-400 text-[10px] block">📍 {loc}</span>
                            </td>
                            <td className="p-3.5">
                              <span className="text-zinc-200 font-semibold block">{a.serviceName}</span>
                              <span className="text-[#FFD700] text-xs font-bold block">{AppFormatters.formatClp(a.servicePrice)}</span>
                              <span className="text-emerald-400 text-[10px] block">Abono: {AppFormatters.formatClp(a.prepayAmount)}</span>
                            </td>
                            <td className="p-3.5">
                              <select
                                value={a.status}
                                onChange={e => {
                                  const newSt = e.target.value;
                                  store.updateAppointmentStatusAdmin(a.id, newSt);
                                  if (newSt === 'Cancelada') {
                                    setCancelFeedbackMsg(`✓ Cita #${a.id} marcada como Cancelada. Correo de disculpas y solicitud de datos bancarios enviado automáticamente a ${a.customerEmail || 'cliente'}.`);
                                    setTimeout(() => setCancelFeedbackMsg(''), 6000);
                                  }
                                }}
                                className={`px-2 py-1 rounded-lg text-[10px] font-black uppercase bg-[#121212] border ${
                                  a.status === 'Confirmada' ? 'border-green-500 text-green-400' :
                                  a.status === 'Completada' ? 'border-[#FFD700] text-[#FFD700]' :
                                  'border-red-500 text-red-400'
                                }`}
                              >
                                <option value="Confirmada">Confirmada</option>
                                <option value="Completada">Completada</option>
                                <option value="Cancelada">Cancelada</option>
                              </select>
                            </td>
                            <td className="p-3.5 text-right whitespace-nowrap">
                              {a.status !== 'Cancelada' ? (
                                <button
                                  onClick={() => {
                                    if (confirm(`¿Anular la cita #${a.id} de "${a.customerName}"? Se le enviará automáticamente un correo formal de disculpas a "${a.customerEmail || 'su correo'}" solicitando sus datos bancarios para reembolsar el abono de ${AppFormatters.formatClp(a.prepayAmount)}.`)) {
                                      store.cancelAppointmentAdmin(a.id, b, loc);
                                      setCancelFeedbackMsg(`✓ Cita #${a.id} anulada. Correo de disculpas y solicitud de datos bancarios enviado automáticamente a ${a.customerEmail || 'cliente'}.`);
                                      setTimeout(() => setCancelFeedbackMsg(''), 6000);
                                    }
                                  }}
                                  className="px-3 py-1.5 bg-red-950/40 hover:bg-red-900/60 text-red-300 border border-red-500/30 font-bold text-[11px] rounded-xl transition-all shadow"
                                >
                                  ⚠️ Anular Cita & Pedir Devolución
                                </button>
                              ) : (
                                <span className="px-2.5 py-1 bg-red-950/20 text-red-400 border border-red-500/20 rounded-lg text-[10px] font-bold inline-block">
                                  ✓ Correo Devolución Enviado
                                </span>
                              )}
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ========================================================= */}
          {/* PESTAÑA: BASE DE DATOS DE CORREOS ELECTRÓNICOS             */}
          {/* ========================================================= */}
          {subTab === 'correos_db' && (
            <div className="space-y-5 animate-fade-in">
              {/* Tarjetas KPI de Correos */}
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <div className="bg-[#1E1E1E] border border-[#FFD700]/30 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Base de Correos</span>
                  <h4 className="text-2xl font-black text-[#FFD700] mt-0.5">{(emailDatabase || []).length}</h4>
                  <p className="text-[10px] text-zinc-400 mt-1">Correos únicos guardados</p>
                </div>
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Clientes Registrados</span>
                  <h4 className="text-2xl font-black text-white mt-0.5">
                    {(emailDatabase || []).filter(e => e.source === 'Registro Oficial').length}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-1">Con cuenta en la plataforma</p>
                </div>
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Reservas Online</span>
                  <h4 className="text-2xl font-black text-blue-400 mt-0.5">
                    {(emailDatabase || []).filter(e => (e.source || '').includes('Reserva')).length}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-1">Capturados al agendar cita</p>
                </div>
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Envíos / Comprobantes</span>
                  <h4 className="text-2xl font-black text-green-400 mt-0.5">{(sentEmailsLog || []).length}</h4>
                  <p className="text-[10px] text-zinc-400 mt-1">Vouchers y avisos emitidos</p>
                </div>
              </div>

              {/* Barra de Acciones de Marketing / Exportación */}
              <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-wrap items-center gap-2.5">
                  <button
                    onClick={handleExportEmailsCsv}
                    className="px-4 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md flex items-center space-x-1.5 transition-all"
                  >
                    <span>📥</span>
                    <span>Descargar Base (.csv / Excel)</span>
                  </button>

                  <button
                    onClick={handleCopyAllEmails}
                    className="px-4 py-2.5 bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-200 font-bold text-xs rounded-xl flex items-center space-x-1.5 transition-all"
                  >
                    <span>{copySuccess ? '✅' : '📋'}</span>
                    <span>{copySuccess ? '¡Correos Copiados!' : 'Copiar Todos los Correos'}</span>
                  </button>

                  <button
                    onClick={() => setShowAddEmailModal(true)}
                    className="px-4 py-2.5 bg-emerald-950/40 hover:bg-emerald-900/50 border border-emerald-500/50 text-emerald-300 font-bold text-xs rounded-xl flex items-center space-x-1.5 transition-all"
                  >
                    <span>➕</span>
                    <span>Agregar Correo Manual</span>
                  </button>
                </div>

                {/* Alternador de Vista (Directorio vs Bandeja de Envíos) */}
                <div className="flex bg-[#121212] p-1 rounded-xl border border-zinc-800 text-xs">
                  <button
                    onClick={() => setEmailViewMode('database')}
                    className={`px-3 py-1.5 rounded-lg font-bold transition-colors ${emailViewMode === 'database' ? 'bg-[#FFD700] text-black font-black' : 'text-zinc-400 hover:text-white'}`}
                  >
                    Directorio ({(emailDatabase || []).length})
                  </button>
                  <button
                    onClick={() => setEmailViewMode('sent_logs')}
                    className={`px-3 py-1.5 rounded-lg font-bold transition-colors ${emailViewMode === 'sent_logs' ? 'bg-[#FFD700] text-black font-black' : 'text-zinc-400 hover:text-white'}`}
                  >
                    Historial de Envíos ({(sentEmailsLog || []).length})
                  </button>
                </div>
              </div>

              {/* VISTA 1: DIRECTORIO DE CORREOS GUARDADOS */}
              {emailViewMode === 'database' && (
                <div className="space-y-3">
                  {/* Buscador y Filtros en vivo */}
                  <div className="flex flex-col sm:flex-row gap-3">
                    <div className="relative flex-1">
                      <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500 text-xs">🔍</span>
                      <input
                        type="text"
                        value={emailSearch}
                        onChange={e => setEmailSearch(e.target.value)}
                        placeholder="Buscar por correo, nombre o teléfono..."
                        className="w-full pl-9 pr-4 py-2.5 bg-[#1E1E1E] border border-zinc-800 rounded-xl text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-[#FFD700]"
                      />
                    </div>
                    <select
                      value={emailSourceFilter}
                      onChange={e => setEmailSourceFilter(e.target.value)}
                      className="px-3.5 py-2.5 bg-[#1E1E1E] border border-zinc-800 rounded-xl text-xs text-zinc-300 focus:outline-none focus:border-[#FFD700]"
                    >
                      <option value="all">Todos los Orígenes</option>
                      <option value="Registro Oficial">Registro Oficial</option>
                      <option value="Reserva Online">Reserva Online</option>
                      <option value="Reserva Invitado">Reserva Invitado</option>
                      <option value="Corte Presencial (Walk-in)">Corte Presencial</option>
                      <option value="Ingreso Manual Admin">Ingreso Manual</option>
                    </select>
                  </div>

                  {/* Tabla de Base de Datos */}
                  <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl overflow-hidden shadow-xl">
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs text-zinc-300">
                        <thead className="bg-[#121212] text-zinc-400 uppercase text-[10px] tracking-wider border-b border-zinc-800">
                          <tr>
                            <th className="p-3.5 font-black">Correo Electrónico</th>
                            <th className="p-3.5 font-black">Cliente / Nombre</th>
                            <th className="p-3.5 font-black">Teléfono</th>
                            <th className="p-3.5 font-black">Origen de Captura</th>
                            <th className="p-3.5 font-black">Fecha Registro</th>
                            <th className="p-3.5 font-black">Citas / Acciones</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-zinc-800/60">
                          {filteredEmails.length === 0 ? (
                            <tr>
                              <td colSpan={6} className="p-8 text-center text-zinc-500 text-xs">
                                No se encontraron correos con los filtros actuales.
                              </td>
                            </tr>
                          ) : (
                            filteredEmails.map(item => (
                              <tr key={item.id} className="hover:bg-zinc-800/30 transition-colors">
                                <td className="p-3.5">
                                  <a href={`mailto:${item.email}`} className="font-bold text-white hover:text-[#FFD700] flex items-center space-x-1.5">
                                    <span>✉️</span>
                                    <span>{item.email}</span>
                                  </a>
                                  {item.notes && <span className="block text-[10px] text-zinc-500 mt-0.5">{item.notes}</span>}
                                </td>
                                <td className="p-3.5">
                                  <strong className="text-zinc-200">{item.fullName || 'Cliente'}</strong>
                                </td>
                                <td className="p-3.5 font-mono text-zinc-400">
                                  {item.phone ? (
                                    <a href={`tel:${item.phone}`} className="hover:text-white">{item.phone}</a>
                                  ) : (
                                    <span className="text-zinc-600">—</span>
                                  )}
                                </td>
                                <td className="p-3.5">
                                  <span className={`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                                    item.source === 'Registro Oficial' ? 'bg-amber-500/15 text-[#FFD700] border border-[#FFD700]/30' :
                                    item.source === 'Reserva Online' ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30' :
                                    item.source === 'Reserva Invitado' ? 'bg-blue-500/15 text-blue-400 border border-blue-500/30' :
                                    'bg-purple-500/15 text-purple-400 border border-purple-500/30'
                                  }`}>
                                    {item.source}
                                  </span>
                                </td>
                                <td className="p-3.5 text-zinc-400 text-[11px]">
                                  {item.addedAt || '—'}
                                </td>
                                <td className="p-3.5">
                                  <div className="flex items-center space-x-2">
                                    <span className="px-2 py-0.5 rounded bg-zinc-800 text-[10px] text-zinc-300 font-bold">
                                      {item.interactionsCount || 1} {item.interactionsCount === 1 ? 'contacto' : 'contactos'}
                                    </span>
                                    <button
                                      onClick={() => {
                                        if (confirm(`¿Eliminar ${item.email} de la base de datos?`)) {
                                          store.deleteEmailFromDatabase(item.id);
                                        }
                                      }}
                                      title="Eliminar de la base de datos"
                                      className="p-1 text-zinc-500 hover:text-red-400 rounded hover:bg-zinc-800 text-xs"
                                    >
                                      🗑️
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}

              {/* VISTA 2: HISTORIAL DE CORREOS / COMPROBANTES ENVIADOS */}
              {emailViewMode === 'sent_logs' && (
                <div className="space-y-3">
                  <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl overflow-hidden shadow-xl">
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs text-zinc-300">
                        <thead className="bg-[#121212] text-zinc-400 uppercase text-[10px] tracking-wider border-b border-zinc-800">
                          <tr>
                            <th className="p-3.5 font-black">Fecha / Hora</th>
                            <th className="p-3.5 font-black">Destinatario</th>
                            <th className="p-3.5 font-black">Asunto</th>
                            <th className="p-3.5 font-black">Estado</th>
                            <th className="p-3.5 font-black">Mensaje</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-zinc-800/60">
                          {(sentEmailsLog || []).length === 0 ? (
                            <tr>
                              <td colSpan={5} className="p-8 text-center text-zinc-500 text-xs">
                                Aún no se registran correos en la bitácora de envíos.
                              </td>
                            </tr>
                          ) : (
                            sentEmailsLog.map(log => (
                              <tr key={log.id} className="hover:bg-zinc-800/30 transition-colors">
                                <td className="p-3.5 text-zinc-400 font-mono text-[11px] whitespace-nowrap">
                                  {log.sentAt}
                                </td>
                                <td className="p-3.5">
                                  <strong className="text-white block">{log.toName || 'Cliente'}</strong>
                                  <span className="text-zinc-400 text-[11px]">{log.toEmail}</span>
                                </td>
                                <td className="p-3.5 font-bold text-zinc-200">
                                  {log.subject}
                                </td>
                                <td className="p-3.5">
                                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                    {log.status || 'Enviado ✓'}
                                  </span>
                                </td>
                                <td className="p-3.5">
                                  <button
                                    onClick={() => setViewingEmailMsg(log)}
                                    className="px-2.5 py-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs rounded-lg font-bold flex items-center space-x-1"
                                  >
                                    <span>👁️</span><span>Ver Correo</span>
                                  </button>
                                </td>
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}

              {/* MODAL: VER MENSAJE COMPLETO DE COMPROBANTE/CORREO */}
              {viewingEmailMsg && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-fade-in">
                  <div className="bg-[#1E1E1E] border border-[#FFD700] rounded-3xl w-full max-w-lg p-6 relative shadow-2xl space-y-4">
                    <div className="flex justify-between items-start pb-3 border-b border-zinc-800">
                      <div>
                        <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-widest block">COMPROBANTE ELECTRÓNICO</span>
                        <h4 className="text-base font-black text-white font-brand mt-0.5">{viewingEmailMsg.subject}</h4>
                      </div>
                      <button onClick={() => setViewingEmailMsg(null)} className="w-8 h-8 rounded-full bg-zinc-800 text-zinc-400 hover:text-white flex items-center justify-center text-sm">✕</button>
                    </div>

                    <div className="space-y-1 text-xs text-zinc-400 bg-[#121212] p-3 rounded-xl border border-zinc-800 font-mono">
                      <p><strong className="text-zinc-300">De:</strong> Big Boys Barberías Chile &lt;contacto.bigboyschile@gmail.com&gt;</p>
                      <p><strong className="text-zinc-300">Para:</strong> {viewingEmailMsg.toName} &lt;{viewingEmailMsg.toEmail}&gt;</p>
                      <p><strong className="text-zinc-300">Fecha:</strong> {viewingEmailMsg.sentAt}</p>
                      <p><strong className="text-zinc-300">Estado:</strong> <span className="text-emerald-400 font-bold">{viewingEmailMsg.status}</span></p>
                    </div>

                    <div className="p-4 rounded-2xl bg-zinc-900 border border-zinc-800 text-xs text-zinc-200 leading-relaxed whitespace-pre-line max-h-60 overflow-y-auto">
                      {viewingEmailMsg.body}
                    </div>

                    <div className="flex justify-end pt-2">
                      <button
                        onClick={() => setViewingEmailMsg(null)}
                        className="px-5 py-2 bg-[#FFD700] text-black font-black text-xs uppercase rounded-xl"
                      >
                        Cerrar Vista
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* MODAL: AGREGAR CORREO MANUALMENTE */}
              {showAddEmailModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-fade-in">
                  <div className="bg-[#1E1E1E] border border-[#FFD700] rounded-3xl w-full max-w-md p-6 relative shadow-2xl space-y-4">
                    <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
                      <div>
                        <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-widest block">ADMINISTRACIÓN</span>
                        <h4 className="text-base font-black text-white font-brand">AGREGAR CORREO A LA BASE</h4>
                      </div>
                      <button onClick={() => setShowAddEmailModal(false)} className="w-8 h-8 rounded-full bg-zinc-800 text-zinc-400 hover:text-white flex items-center justify-center text-sm">✕</button>
                    </div>

                    {addEmailFeedback && (
                      <div className="p-3 bg-emerald-950/40 border border-emerald-500/50 rounded-xl text-emerald-300 text-xs">
                        {addEmailFeedback}
                      </div>
                    )}

                    <form onSubmit={handleAddManualEmail} className="space-y-3">
                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Correo Electrónico *</label>
                        <input
                          type="email"
                          required
                          value={newEmailVal}
                          onChange={e => setNewEmailVal(e.target.value)}
                          placeholder="cliente@ejemplo.cl"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Nombre Completo</label>
                        <input
                          type="text"
                          value={newNameVal}
                          onChange={e => setNewNameVal(e.target.value)}
                          placeholder="Nombre y Apellido"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Teléfono (opcional)</label>
                        <input
                          type="tel"
                          value={newPhoneVal}
                          onChange={e => setNewPhoneVal(e.target.value)}
                          placeholder="+56 9 1234 5678"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Notas o Referencia</label>
                        <input
                          type="text"
                          value={newNotesVal}
                          onChange={e => setNewNotesVal(e.target.value)}
                          placeholder="Ej: Cliente frecuente Talagante, prefiere corte fade"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div className="flex space-x-3 pt-2">
                        <button
                          type="button"
                          onClick={() => setShowAddEmailModal(false)}
                          className="flex-1 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-bold rounded-xl"
                        >
                          Cancelar
                        </button>
                        <button
                          type="submit"
                          className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase rounded-xl shadow-md"
                        >
                          Guardar Correo
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {subTab === 'cupones' && (
            <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-6 max-w-md">
              <h4 className="text-sm font-black text-white font-brand mb-3">CANJEAR CUPÓN PIN</h4>
              {redeemMsg && (
                <div className={`p-3 rounded-xl mb-3 text-xs ${redeemMsg.success ? 'bg-green-950/40 text-green-300' : 'bg-red-950/40 text-red-300'}`}>
                  {redeemMsg.message}
                </div>
              )}
              <form onSubmit={handleRedeem} className="flex space-x-2">
                <input type="text" maxLength={4} required value={couponInput} onChange={e => setCouponInput(e.target.value)} placeholder="PIN 4 dígitos" className="flex-1 px-4 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white font-mono text-center text-lg" />
                <button type="submit" className="px-5 py-2 bg-[#FFD700] text-black font-black text-xs uppercase rounded-xl">Canjear</button>
              </form>
            </div>
          )}

          {/* 5. GESTIÓN INTEGRAL DE BARBEROS (AGREGAR, EDITAR, ELIMINAR, PAUSAR) */}
          {subTab === 'barberos_mgr' && (
            <div className="space-y-5 animate-fade-in">
              {/* Barra Superior de Gestión */}
              <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3 shadow-lg">
                <div>
                  <h4 className="text-sm font-black text-white uppercase tracking-wider flex items-center space-x-2">
                    <span>💈</span>
                    <span>Nómina Oficial de Barberos ({barbers.length})</span>
                  </h4>
                  <p className="text-xs text-zinc-400 mt-0.5">
                    Agrega nuevos estilistas, modifica datos, actualiza horarios o remueve profesionales.
                  </p>
                </div>

                <button
                  onClick={handleOpenAddBarber}
                  className="px-4 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-md flex items-center space-x-2 transition-all transform hover:scale-105 active:scale-95"
                >
                  <span className="text-base leading-none">➕</span>
                  <span>Agregar Nuevo Barbero</span>
                </button>
              </div>

              {/* Barra de Filtro y Búsqueda */}
              <div className="flex flex-col sm:flex-row gap-3">
                <div className="relative flex-1">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500 text-xs">🔍</span>
                  <input
                    type="text"
                    value={barberSearch}
                    onChange={e => setBarberSearch(e.target.value)}
                    placeholder="Buscar barbero por nombre o especialidad..."
                    className="w-full pl-9 pr-4 py-2.5 bg-[#1E1E1E] border border-zinc-800 rounded-xl text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-[#FFD700]"
                  />
                </div>

                <select
                  value={barberLocFilter}
                  onChange={e => setBarberLocFilter(e.target.value)}
                  className="px-3.5 py-2.5 bg-[#1E1E1E] border border-zinc-800 rounded-xl text-xs text-zinc-300 focus:outline-none focus:border-[#FFD700]"
                >
                  <option value="all">Todas las Sucursales</option>
                  {locations.map(loc => (
                    <option key={loc.id} value={loc.id}>{loc.name}</option>
                  ))}
                </select>
              </div>

              {/* Grid de Barberos */}
              {filteredBarbersList.length === 0 ? (
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-10 text-center text-zinc-500 text-xs">
                  No se encontraron barberos con los filtros seleccionados.
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {filteredBarbersList.map(b => {
                    const loc = locations.find(l => l.id === b.locationId);
                    const isAvailable = b.availableHours && b.availableHours !== 'NO_DISPONIBLE';

                    return (
                      <div
                        key={b.id}
                        className="bg-[#1E1E1E] border border-zinc-800 hover:border-zinc-700 rounded-2xl p-5 flex flex-col justify-between space-y-4 shadow-xl transition-all"
                      >
                        <div className="flex items-start space-x-3.5">
                          <div className="relative">
                            <img
                              src={b.avatarUrl}
                              alt={b.name}
                              className="w-14 h-14 rounded-2xl object-cover border-2 border-[#FFD700]/70 shadow-md"
                              onError={(e) => { e.target.src = 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300'; }}
                            />
                            <span
                              className={`absolute -bottom-1 -right-1 w-3.5 h-3.5 rounded-full border-2 border-[#1E1E1E] ${
                                isAvailable ? 'bg-emerald-500' : 'bg-zinc-500'
                              }`}
                              title={isAvailable ? 'Disponible' : 'Pausado'}
                            ></span>
                          </div>

                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <h5 className="text-sm font-black text-white truncate">{b.name}</h5>
                              <span className="text-xs font-bold text-[#FFD700] flex items-center space-x-0.5">
                                <span>★</span><span>{b.rating || '5.0'}</span>
                              </span>
                            </div>
                            <span className="text-xs text-[#FFD700] font-semibold block truncate">{b.specialty}</span>
                            <span className="text-[10px] text-zinc-400 block mt-0.5 truncate">📍 {loc ? loc.name : 'Sucursal Big Boys'}</span>
                          </div>
                        </div>

                        {/* Bio y Horarios */}
                        <div className="bg-[#121212] p-2.5 rounded-xl border border-zinc-800 text-[11px] text-zinc-400 leading-relaxed">
                          <p className="line-clamp-2">{b.bio || 'Barbero profesional de Big Boys Barberías Chile.'}</p>
                          <div className="mt-2 pt-2 border-t border-zinc-800/80 flex items-center justify-between text-[10px]">
                            <span className="text-zinc-500">Disponibilidad:</span>
                            <span className={`font-bold ${isAvailable ? 'text-emerald-400' : 'text-amber-400'}`}>
                              {isAvailable ? 'En Turno Activo' : 'Pausado Temporalmente'}
                            </span>
                          </div>
                        </div>

                        {/* Botonera de Acciones: Editar, Pausar/Activar, Eliminar */}
                        <div className="flex items-center space-x-2 pt-1 border-t border-zinc-800">
                          <button
                            onClick={() => handleOpenEditBarber(b)}
                            className="flex-1 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs font-bold rounded-xl flex items-center justify-center space-x-1 transition-colors"
                          >
                            <span>✏️</span><span>Editar</span>
                          </button>

                          <button
                            onClick={() => store.toggleBarberAvailability(b.id)}
                            className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors ${
                              isAvailable
                                ? 'bg-amber-950/40 hover:bg-amber-900/60 text-amber-300 border border-amber-500/30'
                                : 'bg-emerald-950/40 hover:bg-emerald-900/60 text-emerald-300 border border-emerald-500/30'
                            }`}
                          >
                            {isAvailable ? '⏸️ Pausar' : '▶️ Activar'}
                          </button>

                          <button
                            onClick={() => handleDeleteBarber(b)}
                            title="Eliminar del equipo"
                            className="p-1.5 bg-red-950/40 hover:bg-red-900/60 text-red-400 border border-red-500/30 rounded-xl text-xs font-bold transition-colors"
                          >
                            🗑️
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* MODAL: AGREGAR BARBERO */}
              {showAddBarberModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-fade-in overflow-y-auto">
                  <div className="bg-[#1E1E1E] border border-[#FFD700] rounded-3xl w-full max-w-md p-6 relative shadow-2xl space-y-4 my-8">
                    <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
                      <div>
                        <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-widest block">ADMINISTRACIÓN</span>
                        <h4 className="text-base font-black text-white font-brand">AGREGAR NUEVO BARBERO</h4>
                      </div>
                      <button onClick={() => setShowAddBarberModal(false)} className="w-8 h-8 rounded-full bg-zinc-800 text-zinc-400 hover:text-white flex items-center justify-center text-sm">✕</button>
                    </div>

                    {barberFeedbackMsg && (
                      <div className={`p-3 rounded-xl text-xs font-bold ${barberFeedbackMsg.includes('✓') ? 'bg-emerald-950/40 border border-emerald-500/50 text-emerald-300' : 'bg-red-950/40 border border-red-500/50 text-red-300'}`}>
                        {barberFeedbackMsg}
                      </div>
                    )}

                    <form onSubmit={handleSaveNewBarber} className="space-y-3">
                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Nombre Completo *</label>
                        <input
                          type="text"
                          required
                          value={barberNameInput}
                          onChange={e => setBarberNameInput(e.target.value)}
                          placeholder="Ej: Marcelo Fernández"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Sucursal Asignada *</label>
                        <select
                          value={barberLocIdInput}
                          onChange={e => setBarberLocIdInput(parseInt(e.target.value, 10))}
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        >
                          {locations.map(l => (
                            <option key={l.id} value={l.id}>{l.name}</option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Especialidad Principal *</label>
                        <input
                          type="text"
                          required
                          value={barberSpecialtyInput}
                          onChange={e => setBarberSpecialtyInput(e.target.value)}
                          placeholder="Ej: Fade & Degrado Americano / Barba Royale"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">URL de Foto de Perfil</label>
                        <input
                          type="url"
                          value={barberAvatarInput}
                          onChange={e => setBarberAvatarInput(e.target.value)}
                          placeholder="https://..."
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Presentación / Biografía</label>
                        <textarea
                          rows={2}
                          value={barberBioInput}
                          onChange={e => setBarberBioInput(e.target.value)}
                          placeholder="Descripción breve de su experiencia y técnica..."
                          className="w-full px-3.5 py-2 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        ></textarea>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Horarios Disponibles (separados por coma)</label>
                        <input
                          type="text"
                          value={barberHoursInput}
                          onChange={e => setBarberHoursInput(e.target.value)}
                          placeholder="09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00"
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700] font-mono"
                        />
                      </div>

                      <div className="flex space-x-3 pt-2">
                        <button
                          type="button"
                          onClick={() => setShowAddBarberModal(false)}
                          className="flex-1 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-bold rounded-xl"
                        >
                          Cancelar
                        </button>
                        <button
                          type="submit"
                          className="flex-1 py-2.5 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase rounded-xl shadow-md"
                        >
                          Guardar Barbero
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              )}

              {/* MODAL: EDITAR BARBERO */}
              {showEditBarberModal && editingBarber && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-fade-in overflow-y-auto">
                  <div className="bg-[#1E1E1E] border border-[#FFD700] rounded-3xl w-full max-w-md p-6 relative shadow-2xl space-y-4 my-8">
                    <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
                      <div>
                        <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-widest block">ADMINISTRACIÓN</span>
                        <h4 className="text-base font-black text-white font-brand">EDITAR BARBERO: {editingBarber.name}</h4>
                      </div>
                      <button onClick={() => setShowEditBarberModal(false)} className="w-8 h-8 rounded-full bg-zinc-800 text-zinc-400 hover:text-white flex items-center justify-center text-sm">✕</button>
                    </div>

                    {barberFeedbackMsg && (
                      <div className={`p-3 rounded-xl text-xs font-bold ${barberFeedbackMsg.includes('✓') ? 'bg-emerald-950/40 border border-emerald-500/50 text-emerald-300' : 'bg-red-950/40 border border-red-500/50 text-red-300'}`}>
                        {barberFeedbackMsg}
                      </div>
                    )}

                    <form onSubmit={handleSaveEditBarber} className="space-y-3">
                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Nombre Completo *</label>
                        <input
                          type="text"
                          required
                          value={barberNameInput}
                          onChange={e => setBarberNameInput(e.target.value)}
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Sucursal Asignada *</label>
                        <select
                          value={barberLocIdInput}
                          onChange={e => setBarberLocIdInput(parseInt(e.target.value, 10))}
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        >
                          {locations.map(l => (
                            <option key={l.id} value={l.id}>{l.name}</option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Especialidad Principal *</label>
                        <input
                          type="text"
                          required
                          value={barberSpecialtyInput}
                          onChange={e => setBarberSpecialtyInput(e.target.value)}
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">URL de Foto de Perfil</label>
                        <input
                          type="url"
                          value={barberAvatarInput}
                          onChange={e => setBarberAvatarInput(e.target.value)}
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Presentación / Biografía</label>
                        <textarea
                          rows={2}
                          value={barberBioInput}
                          onChange={e => setBarberBioInput(e.target.value)}
                          className="w-full px-3.5 py-2 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700]"
                        ></textarea>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-zinc-300 mb-1">Horarios Disponibles (separados por coma)</label>
                        <input
                          type="text"
                          value={barberHoursInput}
                          onChange={e => setBarberHoursInput(e.target.value)}
                          className="w-full px-3.5 py-2.5 bg-[#121212] border border-zinc-700 rounded-xl text-white text-xs focus:border-[#FFD700] font-mono"
                        />
                      </div>

                      <div className="flex space-x-3 pt-2">
                        <button
                          type="button"
                          onClick={() => setShowEditBarberModal(false)}
                          className="flex-1 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-bold rounded-xl"
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
              )}
            </div>
          )}

          {/* 6. CENTRO DE REPORTES EXCEL NATIVO (.XLSX) CON PERIODOS */}
          {subTab === 'excel' && (
            <div className="space-y-5 animate-fade-in">
              {/* Encabezado del Módulo */}
              <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-5 shadow-xl flex flex-wrap justify-between items-center gap-4">
                <div>
                  <h4 className="text-sm font-black text-white uppercase tracking-wider flex items-center space-x-2">
                    <span>📊</span>
                    <span>Generador Oficial de Archivos Microsoft Excel (.xlsx)</span>
                  </h4>
                  <p className="text-xs text-zinc-400 mt-1">
                    Exporta libros con formato oficial de Excel (.xlsx) multihíbrido: incluye hoja detallada de citas y hoja de resumen financiero con cálculo de recaudación y abonos Webpay.
                  </p>
                </div>

                {/* Botón de Descarga Principal */}
                <button
                  onClick={handleGenerateRealExcel}
                  className="px-5 py-3 bg-[#FFD700] hover:bg-[#E5C100] text-black font-black text-xs uppercase tracking-wider rounded-xl shadow-[0_4px_20px_rgba(255,215,0,0.35)] flex items-center space-x-2 transition-all transform hover:scale-105 active:scale-95"
                >
                  <span className="text-lg leading-none">📥</span>
                  <span>Descargar Archivo Excel (.xlsx)</span>
                </button>
              </div>

              {/* Mensaje de Confirmación de Descarga */}
              {excelFeedback && (
                <div className="p-3 bg-emerald-950/40 border border-emerald-500/50 rounded-xl text-emerald-300 text-xs font-bold animate-fade-in flex items-center space-x-2">
                  <span>✅</span><span>{excelFeedback}</span>
                </div>
              )}

              {/* Selector de Periodo: Diario, Semanal, Mensual, Anual, Histórico */}
              <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4 space-y-3">
                <span className="text-[10px] font-black uppercase text-[#FFD700] tracking-wider block">
                  1. SELECCIONA EL PERIODO TEMPORAL DEL REPORTE
                </span>

                <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                  {[
                    { id: 'diario', label: '📅 Diario', desc: 'Día específico / Hoy' },
                    { id: 'semanal', label: '📅 Semanal', desc: 'Últimos 7 días' },
                    { id: 'mensual', label: '📅 Mensual', desc: 'Mes en curso' },
                    { id: 'anual', label: '📅 Anual', desc: 'Año en curso' },
                    { id: 'historico', label: '📅 Histórico', desc: 'Todas las citas' }
                  ].map(p => (
                    <button
                      key={p.id}
                      onClick={() => setExcelPeriod(p.id)}
                      className={`p-3 rounded-xl text-left border transition-all ${
                        excelPeriod === p.id
                          ? 'bg-[#FFD700] text-black border-[#FFD700] shadow-md font-bold'
                          : 'bg-[#121212] text-zinc-300 border-zinc-800 hover:border-zinc-700'
                      }`}
                    >
                      <strong className="text-xs block">{p.label}</strong>
                      <span className={`text-[10px] block mt-0.5 ${excelPeriod === p.id ? 'text-black/80' : 'text-zinc-500'}`}>
                        {p.desc}
                      </span>
                    </button>
                  ))}
                </div>

                {/* Filtros secundarios: Fecha (si es diario) y Sucursal */}
                <div className="pt-2 border-t border-zinc-800/80 flex flex-wrap items-center gap-3">
                  {excelPeriod === 'diario' && (
                    <div className="flex items-center space-x-2">
                      <span className="text-xs text-zinc-400 font-bold">Fecha del Día:</span>
                      <input
                        type="date"
                        value={excelCustomDate}
                        onChange={e => setExcelCustomDate(e.target.value)}
                        className="px-3 py-1.5 bg-[#121212] border border-zinc-700 text-white text-xs rounded-xl focus:border-[#FFD700]"
                      />
                    </div>
                  )}

                  <div className="flex items-center space-x-2">
                    <span className="text-xs text-zinc-400 font-bold">Filtrar Sucursal:</span>
                    <select
                      value={excelBranchFilter}
                      onChange={e => setExcelBranchFilter(e.target.value)}
                      className="px-3 py-1.5 bg-[#121212] border border-zinc-700 text-white text-xs rounded-xl focus:border-[#FFD700]"
                    >
                      <option value="all">Todas las Sucursales</option>
                      {locations.map(loc => (
                        <option key={loc.id} value={loc.id}>{loc.name}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* Tarjetas KPI para el Periodo Seleccionado */}
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Citas en Periodo</span>
                  <h4 className="text-2xl font-black text-white mt-1">{filteredExcelAppointments.length}</h4>
                  <p className="text-[10px] text-zinc-400 mt-0.5">Filtradas para exportar</p>
                </div>

                <div className="bg-[#1E1E1E] border border-[#FFD700]/30 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Recaudación Total</span>
                  <h4 className="text-2xl font-black text-[#FFD700] mt-1">
                    {AppFormatters.formatClp(filteredExcelAppointments.reduce((s, a) => s + (a.servicePrice || 0), 0))}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-0.5">Total bruto servicios</p>
                </div>

                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Abonos Webpay</span>
                  <h4 className="text-2xl font-black text-emerald-400 mt-1">
                    {AppFormatters.formatClp(filteredExcelAppointments.reduce((s, a) => s + (a.prepayAmount || 0), 0))}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-0.5">Pagos online recibidos</p>
                </div>

                <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl p-4">
                  <span className="text-[10px] text-zinc-400 uppercase font-bold block">Saldo en Local</span>
                  <h4 className="text-2xl font-black text-blue-400 mt-1">
                    {AppFormatters.formatClp(filteredExcelAppointments.reduce((s, a) => s + Math.max(0, (a.servicePrice || 0) - (a.prepayAmount || 0)), 0))}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-0.5">A percibir en recepción</p>
                </div>
              </div>

              {/* Previsualización de Datos para la Planilla Excel */}
              <div className="bg-[#1E1E1E] border border-zinc-800 rounded-2xl overflow-hidden shadow-xl">
                <div className="p-3.5 bg-[#18181b] border-b border-zinc-800 flex justify-between items-center">
                  <span className="text-xs font-bold text-white uppercase tracking-wider">
                    Vista Previa de Filas a Exportar ({filteredExcelAppointments.length})
                  </span>
                  <span className="text-[10px] text-[#FFD700] font-mono font-bold">
                    Formato: Libro de Trabajo de Microsoft Excel (.xlsx)
                  </span>
                </div>

                <div className="overflow-x-auto max-h-80">
                  <table className="w-full text-left text-xs text-zinc-300">
                    <thead className="bg-[#121212] text-zinc-400 uppercase text-[10px] tracking-wider border-b border-zinc-800 sticky top-0">
                      <tr>
                        <th className="p-3">ID / Fecha</th>
                        <th className="p-3">Sucursal</th>
                        <th className="p-3">Barbero</th>
                        <th className="p-3">Cliente</th>
                        <th className="p-3">Servicio</th>
                        <th className="p-3 text-right">Total</th>
                        <th className="p-3 text-right">Abono Webpay</th>
                        <th className="p-3">Estado</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800/60">
                      {filteredExcelAppointments.length === 0 ? (
                        <tr>
                          <td colSpan={8} className="p-8 text-center text-zinc-500 text-xs">
                            No hay citas registradas en el periodo ({excelPeriod}) seleccionado.
                          </td>
                        </tr>
                      ) : (
                        filteredExcelAppointments.map(a => {
                          const b = barbers.find(x => x.id === a.barberId)?.name || `Barbero #${a.barberId}`;
                          const loc = locations.find(l => l.id === a.locationId)?.name || 'Big Boys';
                          return (
                            <tr key={a.id} className="hover:bg-zinc-800/30 transition-colors">
                              <td className="p-3 font-mono text-zinc-400 text-[11px] whitespace-nowrap">
                                <span className="font-bold text-white block">#{a.id}</span>
                                <span>{a.dateTime}</span>
                              </td>
                              <td className="p-3 text-zinc-300 whitespace-nowrap">{loc}</td>
                              <td className="p-3 text-zinc-200 font-bold whitespace-nowrap">{b}</td>
                              <td className="p-3">
                                <strong className="text-white block">{a.customerName || 'Cliente'}</strong>
                                <span className="text-zinc-500 text-[10px]">{a.customerPhone || '—'}</span>
                              </td>
                              <td className="p-3 text-zinc-300 whitespace-nowrap">{a.serviceName}</td>
                              <td className="p-3 text-right font-bold text-white whitespace-nowrap">
                                {AppFormatters.formatClp(a.servicePrice)}
                              </td>
                              <td className="p-3 text-right font-mono text-emerald-400 whitespace-nowrap">
                                {AppFormatters.formatClp(a.prepayAmount)}
                              </td>
                              <td className="p-3 whitespace-nowrap">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase ${
                                  a.status === 'Completada' ? 'bg-green-500/20 text-green-300' :
                                  a.status === 'Confirmada' ? 'bg-blue-500/20 text-blue-300' :
                                  'bg-red-500/20 text-red-300'
                                }`}>
                                  {a.status}
                                </span>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>
      );
    }

    // Notifications Modal
    function NotificationsModal({ isOpen, onClose, notifications }) {

      return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-md p-5 relative shadow-2xl">
            <div className="flex justify-between items-center pb-3 border-b border-zinc-800">
              <h3 className="text-sm font-black uppercase text-white font-brand">BUZÓN DE ALERTAS ({notifications.length})</h3>
              <button onClick={onClose} className="text-zinc-400 hover:text-white">✕</button>
            </div>

            <div className="max-h-80 overflow-y-auto py-3 space-y-2.5">
              {notifications.length === 0 ? (
                <p className="text-center text-zinc-500 text-xs py-6">No tienes nuevas notificaciones.</p>
              ) : (
                notifications.map(item => (
                  <div key={item.id} className="p-3 rounded-xl bg-[#121212] border border-zinc-800">
                    <div className="flex justify-between">
                      <strong className="text-xs text-[#FFD700]">{item.title}</strong>
                      <span className="text-[10px] text-zinc-500 font-mono">{item.timestamp}</span>
                    </div>
                    <p className="text-xs text-zinc-300 mt-1">{item.message}</p>
                  </div>
                ))
              )}
            </div>

            <button onClick={onClose} className="w-full py-2 bg-[#FFD700] text-black font-black text-xs uppercase rounded-xl">Entendido</button>
          </div>
        </div>
      );
    }

    // Profile Modal
    function ProfileModal({ isOpen, onClose, user }) {
      const [name, setName] = useState('');
      const [phone, setPhone] = useState('');
      const [currPass, setCurrPass] = useState('');
      const [newPass, setNewPass] = useState('');
      const [msg, setMsg] = useState('');

      useEffect(() => {
        if (user) {
          setName(user.fullName || '');
          setPhone(user.phone || '');
        }
      }, [user, isOpen]);

      if (!isOpen || !user) return null;

      const handleSave = async (e) => {
        e.preventDefault();
        const res = await store.updateUserProfile(name, phone, currPass, newPass);
        setMsg(res.message);
        if (res.success) setTimeout(onClose, 1200);
      };

      return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E1E1E] border border-[#FFD700]/50 rounded-2xl w-full max-w-md p-6 relative shadow-2xl">
            <button onClick={onClose} className="absolute top-4 right-4 text-zinc-400 hover:text-white">✕</button>
            <h3 className="text-base font-black text-white font-brand mb-4">MI PERFIL DE CLIENTE</h3>

            {msg && <div className="p-2 mb-3 rounded bg-zinc-800 text-xs text-[#FFD700]">{msg}</div>}

            <form onSubmit={handleSave} className="space-y-3">
              <input type="text" required value={name} onChange={e => setName(e.target.value)} placeholder="Nombre completo" className="w-full px-3 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs" />
              <input type="tel" required value={phone} onChange={e => setPhone(e.target.value)} placeholder="Teléfono" className="w-full px-3 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs" />
              <input type="password" value={currPass} onChange={e => setCurrPass(e.target.value)} placeholder="Contraseña actual" className="w-full px-3 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs" />
              <input type="password" value={newPass} onChange={e => setNewPass(e.target.value)} placeholder="Nueva contraseña (opcional)" className="w-full px-3 py-2 rounded-xl bg-[#121212] border border-zinc-700 text-white text-xs" />
              <button type="submit" className="w-full py-2.5 bg-[#FFD700] text-black font-black text-xs uppercase rounded-xl">Guardar Cambios</button>
            </form>
          </div>
        </div>
      );
    }

    // ==========================================
    // 5. COMPONENTE RAÍZ APP
    // ==========================================