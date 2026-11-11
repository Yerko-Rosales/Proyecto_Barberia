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

// DEF-018 y CP-01. El ingreso de clientes no muestra la «Cuenta de Prueba Rápida» ni su correo y clave,
// y el navegador no guarda una cuenta ni un cupón de prueba sembrados por la web.
QA.abrirPortal()
QA.clic(findTestObject('Web/Portal/btn_IniciarSesion'))
WebUI.delay(1)
WebUI.verifyElementPresent(QA.xpath("//input[@type='password']"), 10)
WebUI.verifyElementNotPresent(QA.texto('Cuenta de Prueba'), 3)
WebUI.verifyElementNotPresent(QA.xpath("//button[contains(., 'Usar Datos')]"), 3)
QA.verificar(!QA.textoPagina().contains('test@bigboys.cl'), 'El portal no publica el correo de la cuenta de prueba')
WebUI.takeScreenshot()
QA.clic(findTestObject('Web/Portal/btn_ContinuarInvitado'))
WebUI.waitForElementPresent(findTestObject('Web/Cliente/btn_AgendarCita'), GlobalVariable.TIMEOUT)
QA.clic(findTestObject('Web/Cliente/btn_IngresarRegistro'))
WebUI.delay(1)
WebUI.verifyElementNotPresent(QA.texto('Cuenta de Prueba'), 3)
QA.verificar(!QA.textoPagina().contains('test@bigboys.cl'), 'El modal de ingreso no publica el correo de la cuenta de prueba')
def sembrados = WebUI.executeJavaScript("return (localStorage.getItem('bigboys_users') || '') + (localStorage.getItem('bigboys_coupons') || '')", null).toString()
QA.verificar(!sembrados.contains('test@bigboys.cl') && !sembrados.contains('7788'), 'El navegador no guarda una cuenta ni un cupón de prueba')
WebUI.takeScreenshot()
WebUI.closeBrowser()
