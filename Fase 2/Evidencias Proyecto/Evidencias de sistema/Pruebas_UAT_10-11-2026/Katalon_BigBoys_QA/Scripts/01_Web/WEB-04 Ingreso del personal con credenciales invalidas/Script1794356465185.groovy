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

// CP-07. Con un correo inexistente el sistema rechaza el ingreso con un mensaje genérico y no abre ningún panel.
QA.abrirPortal()
QA.clic(findTestObject('Web/Portal/btn_Administracion'))
WebUI.waitForElementVisible(findTestObject('Web/Portal/input_CorreoPersonal'), GlobalVariable.TIMEOUT)
WebUI.setText(findTestObject('Web/Portal/input_CorreoPersonal'), 'no.existe.katalon@bigboys.cl')
WebUI.setText(findTestObject('Web/Portal/input_ClavePersonal'), 'ClaveIncorrecta1')
QA.clic(findTestObject('Web/Portal/btn_IngresarPersonal'))
WebUI.waitForElementVisible(findTestObject('Web/Portal/msg_Error'), GlobalVariable.TIMEOUT)
WebUI.verifyElementPresent(findTestObject('Web/Portal/input_CorreoPersonal'), 5)
WebUI.verifyElementNotPresent(findTestObject('Web/Panel/btn_CerrarSesion'), 3)
WebUI.takeScreenshot()
WebUI.closeBrowser()
