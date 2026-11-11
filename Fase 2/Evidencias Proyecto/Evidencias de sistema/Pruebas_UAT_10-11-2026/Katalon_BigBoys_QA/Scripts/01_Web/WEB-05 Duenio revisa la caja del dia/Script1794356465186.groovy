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

// UAT-04 y CP-55 (DEF-016). El Dueño entra al panel y ve «Caja del día», las cuentas del personal y los derechos del titular.
QA.abrirPortal()
QA.ingresarPersonal('dueno')
WebUI.waitForElementPresent(findTestObject('Web/Panel/tab_Ventas'), GlobalVariable.TIMEOUT)
WebUI.verifyElementPresent(findTestObject('Web/Panel/tab_Personal'), 5)
WebUI.verifyElementPresent(findTestObject('Web/Panel/tab_Derechos'), 5)
WebUI.verifyElementPresent(findTestObject('Web/Panel/tab_FilaLlegada'), 5)
QA.clic(findTestObject('Web/Panel/tab_Ventas'))
WebUI.waitForElementPresent(QA.texto('Caja del día'), GlobalVariable.TIMEOUT)
WebUI.verifyElementPresent(QA.texto('Caja del día'), 5)
WebUI.delay(3)
WebUI.takeScreenshot()
WebUI.closeBrowser()
