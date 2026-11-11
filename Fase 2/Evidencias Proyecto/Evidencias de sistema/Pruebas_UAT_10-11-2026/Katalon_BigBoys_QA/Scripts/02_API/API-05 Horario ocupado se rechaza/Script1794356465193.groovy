import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType
import internal.GlobalVariable as GlobalVariable
import bigboys.QA

// CP-33. La base de datos impide reservar un bloque ya tomado por otra reserva (restricción de exclusión).
def reservas = QA.rpcOk('api_listar_reservas', [p_formato: 'WEB'] + QA.cred('dueno'))
String ahora = new Date().format('yyyy-MM-dd HH:mm', TimeZone.getTimeZone('America/Santiago'))
def r = reservas.find { it.status == 'Confirmada' && it.date_time > ahora }
QA.verificar(r != null, "Hay una reserva futura confirmada para la prueba (${r?.codigo}, ${r?.date_time})")
QA.rpcRechaza('api_crear_reserva', [p: [id: System.currentTimeMillis(), canal: 'WEB', barber_id: r.barber_id, service_name: 'Corte', date_time: r.date_time, status: 'PENDIENTE_PAGO', customer_name: 'Prueba Katalon']], 'HORARIO_OCUPADO')
