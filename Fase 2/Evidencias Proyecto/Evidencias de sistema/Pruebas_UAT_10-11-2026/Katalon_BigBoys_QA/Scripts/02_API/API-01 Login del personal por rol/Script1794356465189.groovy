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

// CP-06. Dueño, cajero/a y barbero inician sesión y la API devuelve su rol y alcance (el cajero/a queda en su sucursal).
def d = QA.rpcOk('api_login', QA.cred('dueno'))
QA.verificar(d.role == 'DUENO', "Dueño: rol ${d.role}")
def c = QA.rpcOk('api_login', QA.cred('caja'))
QA.verificar(c.role == 'ADMIN_SUCURSAL' && c.location_id == 1, "Cajero/a: rol ${c.role}, sucursal ${c.location_id}")
def b = QA.rpcOk('api_login', QA.cred('barbero'))
QA.verificar(b.role == 'BARBERO', "Barbero: rol ${b.role}")
QA.verificar(!d.keySet().any { it.toLowerCase().contains('hash') || it.toLowerCase() == 'password' || it.toLowerCase().contains('sha256') }, 'La respuesta del login no devuelve la contraseña ni su hash')
