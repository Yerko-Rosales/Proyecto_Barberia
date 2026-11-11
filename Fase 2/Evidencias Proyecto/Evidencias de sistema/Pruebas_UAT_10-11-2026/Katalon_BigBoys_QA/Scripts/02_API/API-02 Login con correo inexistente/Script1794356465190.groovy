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

// CP-07. La API no revela si el correo existe: responde con el mismo error genérico.
Map r = QA.rpc('api_login', [p_email: 'no.existe.katalon@bigboys.cl', p_password_sha256: QA.sha256('ClaveIncorrecta1')])
QA.verificar(r.error != null || r.json == null, "Ingreso rechazado (HTTP ${r.status}: ${r.error})")
QA.verificar(r.error == null || !(r.error.toLowerCase().contains('no existe')), 'El mensaje no revela si el correo está registrado')
