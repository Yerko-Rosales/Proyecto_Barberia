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

// CP-15, CP-53 y CP-59. El cajero/a no crea personal y el barbero no ve las solicitudes de derechos ni la caja.
QA.rpcRechaza('api_personal_crear', QA.cred('caja') + [p: [full_name: 'No Debe Crearse', email: 'katalon.qa@bigboys.cl', role: 'BARBERO', location_id: 1, temp_password_sha256: QA.sha256('Temporal123')]], 'SIN_PERMISO')
QA.rpcRechaza('api_solicitudes_listar', QA.cred('barbero'), 'SIN_PERMISO')
QA.rpcRechaza('api_personal_listar', QA.cred('barbero'), 'SIN_PERMISO')
