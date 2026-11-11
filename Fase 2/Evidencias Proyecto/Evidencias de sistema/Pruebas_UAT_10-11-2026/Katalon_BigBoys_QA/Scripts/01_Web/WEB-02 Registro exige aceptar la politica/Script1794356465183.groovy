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

// CP-01 y CP-02 (DEF-003). Sin marcar la casilla de aceptación no se crea la cuenta y se explica el motivo.
QA.abrirPortal()
QA.clic(findTestObject('Web/Portal/btn_Registrate'))
WebUI.waitForElementVisible(findTestObject('Web/Registro/input_Nombre'), GlobalVariable.TIMEOUT)
WebUI.setText(findTestObject('Web/Registro/input_Nombre'), 'Prueba Katalon')
WebUI.setText(findTestObject('Web/Registro/input_Telefono'), '+56 9 1111 4444')
WebUI.setText(findTestObject('Web/Registro/input_Correo'), 'katalon.qa@prueba.invalid')
WebUI.setText(findTestObject('Web/Registro/input_Clave'), 'Prueba123')
WebUI.verifyElementPresent(findTestObject('Web/Registro/lnk_Politica'), 5)
WebUI.verifyElementNotChecked(findTestObject('Web/Registro/chk_AceptoPolitica'), 5)
WebUI.takeScreenshot()
QA.clic(findTestObject('Web/Registro/btn_CrearCuenta'))
WebUI.waitForElementPresent(QA.texto('debes aceptar la Política'), 15)
WebUI.verifyElementPresent(QA.texto('debes aceptar la Política'), 5)
WebUI.takeScreenshot()
WebUI.closeBrowser()
