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

// UAT-02 y CP-47. El cajero/a entra a su sucursal, ve la fila por orden de llegada y las reservas del día, sin el panel del Dueño.
QA.abrirPortal()
QA.ingresarPersonal('caja')
WebUI.waitForElementPresent(QA.texto('Cajero/a'), GlobalVariable.TIMEOUT)
WebUI.verifyElementPresent(QA.xpath("//button[contains(., 'Fila por llegada')]"), 10)
WebUI.verifyElementPresent(QA.xpath("//button[contains(., 'Reservas de hoy')]"), 10)
WebUI.verifyElementNotPresent(findTestObject('Web/Panel/tab_Personal'), 3)
WebUI.verifyElementNotPresent(findTestObject('Web/Panel/tab_Derechos'), 3)
WebUI.delay(2)
WebUI.takeScreenshot()
QA.clic(QA.xpath("//button[contains(., 'Reservas de hoy')]"))
WebUI.delay(1)
WebUI.takeScreenshot()
WebUI.closeBrowser()
