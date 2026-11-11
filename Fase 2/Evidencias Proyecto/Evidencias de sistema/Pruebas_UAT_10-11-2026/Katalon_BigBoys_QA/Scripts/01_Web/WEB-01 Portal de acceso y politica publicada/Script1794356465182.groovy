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

// CP-73 (DEF-008) y CP-27. El portal carga con sus 4 accesos y la Política de Privacidad y los T&C están publicados.
QA.abrirPortal()
WebUI.verifyElementPresent(findTestObject('Web/Portal/btn_IniciarSesion'), 10)
WebUI.verifyElementPresent(findTestObject('Web/Portal/btn_Registrate'), 10)
WebUI.verifyElementPresent(findTestObject('Web/Portal/btn_Barbero'), 10)
WebUI.verifyElementPresent(findTestObject('Web/Portal/btn_Administracion'), 10)
WebUI.verifyElementPresent(findTestObject('Web/Portal/btn_ContinuarInvitado'), 10)
WebUI.takeScreenshot()

WebUI.navigateToUrl(GlobalVariable.URL_WEB + 'privacidad.html')
WebUI.waitForElementPresent(QA.texto('Política de Privacidad'), GlobalVariable.TIMEOUT)
String pagina = QA.textoPagina()
QA.verificar(pagina.contains('Ley') && pagina.contains('21.719'), 'La política publicada cita la Ley N° 21.719')
QA.verificar(pagina.toLowerCase().contains('términos'), 'La página incluye los Términos y Condiciones')
WebUI.takeScreenshot()
WebUI.closeBrowser()
