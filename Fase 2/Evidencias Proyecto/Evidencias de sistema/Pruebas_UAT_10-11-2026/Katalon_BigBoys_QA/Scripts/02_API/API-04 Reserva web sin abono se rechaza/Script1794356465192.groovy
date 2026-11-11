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

// CP-32 y regla del abono (OE2). Un visitante no puede crear una reserva confirmada sin pasar por Webpay.
QA.rpcRechaza('api_crear_reserva', [p: [canal: 'WEB', barber_id: 1, service_name: 'Corte', date_time: '2026-12-15 10:00', status: 'Confirmada', customer_name: 'Prueba Katalon']], 'PAGO_REQUERIDO')
