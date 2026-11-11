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

// CP-47 y CP-48. La fila pública de la sucursal muestra turnos y estados; el nombre va abreviado y sin contacto.
def f = QA.rpcOk('api_fila_listar', [p_sucursal: 1])
QA.verificar(f.turnos instanceof List, "Fila de la sucursal 1 del ${f.fecha}: ${f.turnos.size()} turnos (${f.turnos.collect { it.estado }.countBy { it }})")
QA.verificar(f.turnos.every { it.nombre ==~ /^\S+( \S\.)?$/ }, 'Los nombres se muestran abreviados: ' + f.turnos.collect { it.nombre }.join(', '))
String txt = groovy.json.JsonOutput.toJson(f)
QA.verificar(!(txt =~ /@/) && !txt.contains('+56'), 'La fila pública no expone correos ni teléfonos')
