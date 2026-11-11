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

// UAT-04, CP-55 y CP-74 (DEF-016). El Dueño ve las 3 sucursales con subtotal Peñaflor y consolidado; el cajero/a solo la suya. En caja = monto − abono.
String desde = '2026-11-01', hasta = new Date().format('yyyy-MM-dd', TimeZone.getTimeZone('America/Santiago'))
def d = QA.rpcOk('api_caja_resumen', QA.cred('dueno') + [p_desde: desde, p_hasta: hasta, p_sucursal: null])
QA.verificar(d.sucursales instanceof List && d.consolidado != null && d.subtotal_penaflor != null, "Dueño: ${d.sucursales.size()} sucursales, subtotal Peñaflor y consolidado")
d.sucursales.each { s ->
	QA.verificar(s.en_caja == s.total_servicios - s.abonos_webpay, "${s.sucursal}: en caja ${s.en_caja} = servicios ${s.total_servicios} − abonos ${s.abonos_webpay} (${s.atenciones} atenciones, ${s.por_llegada} por llegada)")
}
QA.verificar(d.consolidado.en_caja == d.sucursales.sum(0) { it.en_caja }, "Consolidado en caja ${d.consolidado.en_caja} = suma de las sucursales")
def c = QA.rpcOk('api_caja_resumen', QA.cred('caja') + [p_desde: desde, p_hasta: hasta, p_sucursal: null])
QA.verificar(c.sucursales.every { it.id_sucursal == 1 }, "Cajero/a ve solo su sucursal: ${c.sucursales.collect { it.sucursal }}")
Map otra = QA.rpc('api_caja_resumen', QA.cred('caja') + [p_desde: desde, p_hasta: hasta, p_sucursal: 2])
QA.verificar(otra.error != null || otra.json.sucursales.every { it.id_sucursal == 1 }, "Cajero/a que pide la sucursal 2: " + (otra.error ?: 'recibe solo la suya'))
