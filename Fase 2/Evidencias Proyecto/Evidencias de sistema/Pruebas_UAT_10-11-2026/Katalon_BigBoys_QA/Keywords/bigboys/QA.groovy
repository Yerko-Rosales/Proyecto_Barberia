package bigboys

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration

import org.openqa.selenium.By
import org.openqa.selenium.WebElement

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import internal.GlobalVariable

/**
 * Utilidades de las pruebas de Barbería Big-Boys.
 * Las contraseñas del personal NO están en el proyecto: se leen de Aplicación/uat.env (fuera de git)
 * y nunca se escriben en el registro de Katalon (la API recibe la contraseña como SHA-256).
 */
class QA {

	static final Map<String, List<String>> CUENTAS = [
		'dueno'  : [
			'admin@bigboys.cl',
			'BB_CLAVE_DUENO'
		],
		'caja'   : [
			'caja1@bigboys.cl',
			'BB_CLAVE_CAJA'
		],
		'barbero': [
			'mateo@bigboys.cl',
			'BB_CLAVE_BARBERO'
		]
	]
	private static Map<String, String> env = null

	/** Lee uat.env (CLAVE=valor). */
	static Map<String, String> uatEnv() {
		if (env == null) {
			File f = new File(GlobalVariable.RUTA_UAT_ENV.toString())
			if (!f.isAbsolute()) f = new File(RunConfiguration.getProjectDir(), GlobalVariable.RUTA_UAT_ENV.toString())
			if (!f.exists()) KeywordUtil.markFailedAndStop('No se encontró uat.env en ' + f.getCanonicalPath() + '. Revisa el perfil (RUTA_UAT_ENV).')
			env = [:]
			f.getText('UTF-8').readLines().each { String l ->
				int i = l.indexOf('=')
				if (i > 0 && !l.trim().startsWith('#')) env[l.substring(0, i).trim()] = l.substring(i + 1).trim()
			}
		}
		return env
	}

	static String correo(String rol) {
		CUENTAS[rol][0]
	}

	/** Contraseña en texto plano (solo para escribirla en el navegador, sin registrarla). */
	static String clave(String rol) {
		String v = uatEnv()[CUENTAS[rol][1]]
		if (!v) KeywordUtil.markFailedAndStop('Falta ' + CUENTAS[rol][1] + ' en uat.env')
		return v
	}

	static String sha256(String s) {
		MessageDigest md = MessageDigest.getInstance('SHA-256')
		return md.digest(s.trim().getBytes(StandardCharsets.UTF_8)).collect { String.format('%02x', it) }.join('')
	}

	/** Credenciales de la API para un rol (p_email y p_password_sha256). */
	static Map cred(String rol) {
		[p_email: correo(rol), p_password_sha256: sha256(clave(rol))]
	}

	// ----------------------------- API -----------------------------
	private static HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()

	/** Llama a una función api_* y devuelve [status: int, json: objeto, error: código o null]. No registra el cuerpo (lleva credenciales). */
	@Keyword
	static Map rpc(String fn, Map body) {
		String key = GlobalVariable.SUPABASE_ANON_KEY.toString()
		HttpRequest req = HttpRequest.newBuilder(URI.create(GlobalVariable.SUPABASE_URL.toString() + '/rest/v1/rpc/' + fn))
				.timeout(Duration.ofSeconds(60))
				.header('apikey', key).header('Authorization', 'Bearer ' + key).header('Content-Type', 'application/json')
				.POST(HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(body), StandardCharsets.UTF_8)).build()
		long t0 = System.currentTimeMillis()
		HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
		long ms = System.currentTimeMillis() - t0
		def json = null
		try {
			json = new JsonSlurper().parseText(r.body() ?: 'null')
		} catch (Exception e) {
			json = r.body()
		}
		String error = r.statusCode() >= 300 ? ((json instanceof Map && json.message) ? json.message.toString() : r.body()) : null
		KeywordUtil.logInfo("POST /rpc/${fn} → HTTP ${r.statusCode()} en ${ms} ms" + (error ? " · error: ${error}" : ''))
		return [status: r.statusCode(), json: json, error: error, ms: ms]
	}

	/** Igual que rpc() pero falla la prueba si la API responde con error. */
	@Keyword
	static def rpcOk(String fn, Map body) {
		Map r = rpc(fn, body)
		if (r.error) KeywordUtil.markFailedAndStop("${fn} respondió con error: ${r.error}")
		return r.json
	}

	/** Verifica que la API rechace la operación con el código de error esperado. */
	@Keyword
	static void rpcRechaza(String fn, Map body, String codigoEsperado) {
		Map r = rpc(fn, body)
		if (!r.error) KeywordUtil.markFailedAndStop("${fn} debía rechazar la operación con ${codigoEsperado}, pero respondió HTTP ${r.status}")
		if (!r.error.contains(codigoEsperado)) KeywordUtil.markFailedAndStop("${fn} rechazó con «${r.error}» y se esperaba ${codigoEsperado}")
		KeywordUtil.markPassed("${fn} rechazada con ${codigoEsperado}, como se esperaba")
	}

	static void verificar(boolean cond, String msg) {
		if (cond) KeywordUtil.markPassed(msg) else KeywordUtil.markFailedAndStop('NO SE CUMPLE: ' + msg)
	}

	// ----------------------------- Web -----------------------------
	/** Objeto de prueba por XPath (para textos que cambian o se repiten). */
	static TestObject xpath(String xp) {
		TestObject to = new TestObject(xp)
		to.addProperty('xpath', ConditionType.EQUALS, xp)
		return to
	}

	/** Elemento cuyo texto propio contiene t (no considera mayúsculas por CSS). */
	static TestObject texto(String t) {
		xpath("//*[text()[contains(normalize-space(.), \"${t}\")]]")
	}

	/** Abre el ambiente UAT y espera el portal (la web compila React en el navegador). */
	@Keyword
	static void abrirPortal() {
		WebUI.openBrowser('')
		WebUI.setViewPortSize(1366, 900)
		WebUI.navigateToUrl(GlobalVariable.URL_WEB.toString() + '?qa=' + System.currentTimeMillis())
		WebUI.waitForElementPresent(texto('Portal de Acceso'), (int) GlobalVariable.TIMEOUT)
		WebUI.verifyElementPresent(texto('Portal de Acceso'), 5)
	}

	/** Clic normal y, si un elemento superpuesto lo impide, clic por JavaScript. */
	@Keyword
	static void clic(TestObject to) {
		WebUI.waitForElementClickable(to, (int) GlobalVariable.TIMEOUT)
		try {
			WebUI.click(to)
		} catch (Exception e) {
			WebUI.executeJavaScript('arguments[0].click()', [WebUI.findWebElement(to, 10)])
		}
	}

	/** Ingresa como personal desde «Administración» o «Barbero». La contraseña se escribe sin registrarla en el log. */
	@Keyword
	static void ingresarPersonal(String rol) {
		clic(xpath(rol == 'barbero' ? "//button[.//span[normalize-space()='Barbero']]" : "//button[.//span[normalize-space()='Administración']]"))
			TestObject campoCorreo = xpath("//input[@placeholder='correo@bigboys.cl']")
		WebUI.waitForElementVisible(campoCorreo, (int) GlobalVariable.TIMEOUT)
		WebUI.setText(campoCorreo, correo(rol))
		WebElement pass = DriverFactory.getWebDriver().findElement(By.xpath("//input[@placeholder='Contraseña']"))
		pass.clear(); pass.sendKeys(clave(rol))
		KeywordUtil.logInfo('Contraseña de ' + correo(rol) + ' escrita (no se registra)')
		clic(xpath("//button[@type='submit' and contains(., 'Ingresar')]"))
	}

	static String textoPagina() {
		WebUI.executeJavaScript('return document.body.innerText', null).toString()
	}
}
