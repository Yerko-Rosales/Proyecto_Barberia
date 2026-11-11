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

// CP-13 y CP-15 (DEF-002). El invitado entra sin cuenta y el modal de ingreso ya no ofrece el acceso «Admin» con PIN.
QA.abrirPortal()
QA.clic(findTestObject('Web/Portal/btn_ContinuarInvitado'))
WebUI.waitForElementPresent(findTestObject('Web/Cliente/btn_AgendarCita'), GlobalVariable.TIMEOUT)
WebUI.verifyElementPresent(QA.texto('BIENVENIDOS A BIG BOYS'), 10)
WebUI.takeScreenshot()
QA.clic(findTestObject('Web/Cliente/btn_IngresarRegistro'))
WebUI.delay(1)
WebUI.verifyElementNotPresent(QA.texto('PIN Administrador'), 3)
WebUI.verifyElementNotPresent(QA.xpath("//button[contains(., 'Admin') and contains(., '🛡')]"), 3)
def adminCfg = WebUI.executeJavaScript("return localStorage.getItem('bigboys_admin_config')", null)
QA.verificar(adminCfg == null, 'No existe configuración local de administrador (bigboys_admin_config)')
WebUI.takeScreenshot()
WebUI.closeBrowser()
