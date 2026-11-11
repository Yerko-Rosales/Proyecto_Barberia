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

// CP-27 y CP-28. Usa las palabras clave WS de Katalon: 3 sucursales con dirección, servicios con precio y barberos activos.
RequestObject req = new RequestObject('api_sucursales')
req.setRestUrl(GlobalVariable.SUPABASE_URL + '/rest/v1/rpc/api_sucursales')
req.setRestRequestMethod('POST')
req.setHttpHeaderProperties([
	new TestObjectProperty('apikey', ConditionType.EQUALS, GlobalVariable.SUPABASE_ANON_KEY),
	new TestObjectProperty('Authorization', ConditionType.EQUALS, 'Bearer ' + GlobalVariable.SUPABASE_ANON_KEY),
	new TestObjectProperty('Content-Type', ConditionType.EQUALS, 'application/json')])
req.setBodyContent(new com.kms.katalon.core.testobject.impl.HttpTextBodyContent('{}', 'UTF-8', 'application/json'))
ResponseObject res = WS.sendRequest(req)
WS.verifyResponseStatusCode(res, 200)
def suc = new groovy.json.JsonSlurper().parseText(res.getResponseText())
QA.verificar(suc.size() == 3 && suc.every { it.address }, 'Las 3 sucursales tienen dirección: ' + suc.collect { it.name }.join(' | '))

def srv = QA.rpcOk('api_servicios', [p_sucursal: null])
QA.verificar(srv.size() > 0 && srv.every { it.price > 0 }, "${srv.size()} servicios, todos con precio")
def bar = QA.rpcOk('api_barberos', [:])
QA.verificar(bar.findAll { it.active }.size() > 0, "${bar.findAll { it.active }.size()} barberos activos")
