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

// CP-56 y UAT-05. El barbero ve sus atenciones y lo cobrado del día, sin correos ni teléfonos de clientes.
QA.abrirPortal()
QA.ingresarPersonal('barbero')
WebUI.waitForElementPresent(QA.texto('Total cobrado'), GlobalVariable.TIMEOUT)
WebUI.verifyElementPresent(QA.texto('Propinas'), 5)
WebUI.verifyElementPresent(QA.texto('no se muestran datos de contacto'), 5)
WebUI.delay(2)
String pagina = QA.textoPagina()
QA.verificar(!(pagina =~ /[\w.+-]+@[\w-]+\.[\w.]+/), 'La vista del barbero no muestra correos de clientes')
QA.verificar(!pagina.contains('+56'), 'La vista del barbero no muestra teléfonos de clientes')
WebUI.verifyElementNotPresent(findTestObject('Web/Panel/tab_Ventas'), 3)
WebUI.takeScreenshot()
WebUI.closeBrowser()
