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

// CP-NF-04 (seguridad, RLS). Con la clave pública no se leen tablas con datos personales; solo se accede por las funciones api_*.
['usuario', 'cliente', 'reserva', 'pago', 'registro_auditoria', 'solicitud_derecho'].each { tabla ->
	RequestObject req = new RequestObject('tabla_' + tabla)
	req.setRestUrl(GlobalVariable.SUPABASE_URL + '/rest/v1/' + tabla + '?select=*&limit=1')
	req.setRestRequestMethod('GET')
	req.setHttpHeaderProperties([
		new TestObjectProperty('apikey', ConditionType.EQUALS, GlobalVariable.SUPABASE_ANON_KEY),
		new TestObjectProperty('Authorization', ConditionType.EQUALS, 'Bearer ' + GlobalVariable.SUPABASE_ANON_KEY)])
	ResponseObject res = WS.sendRequest(req)
	String body = res.getResponseText()
	QA.verificar(res.getStatusCode() >= 400 || body.trim() == '[]', "Tabla ${tabla}: HTTP ${res.getStatusCode()} sin datos")
}
