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

// CP-56. Las atenciones del barbero traen su total y no incluyen correos ni teléfonos.
String hoy = new Date().format('yyyy-MM-dd', TimeZone.getTimeZone('America/Santiago'))
def m = QA.rpcOk('api_mis_atenciones', QA.cred('barbero') + [p_fecha: hoy])
String txt = groovy.json.JsonOutput.toJson(m)
QA.verificar(m.total != null, "Total del día informado: ${m.total}")
QA.verificar(!(txt =~ /@/) && !txt.contains('+56'), 'La respuesta no contiene correos ni teléfonos')
