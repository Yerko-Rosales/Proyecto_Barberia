package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.MainAppScreen
import com.example.ui.viewmodel.BarberiaViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1080dp-h2400dp")
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testGuestTabsCrashValidation() {
    try {
      val context = ApplicationProvider.getApplicationContext<Application>()
      val viewModel = BarberiaViewModel(context)

      composeTestRule.setContent {
        MainAppScreen(viewModel = viewModel, initialShowSplash = false)
      }
      
      // Explicitly wait for database prepopulate and initial layout frame
      composeTestRule.waitForIdle()

      // 1. Enter Guest mode
      composeTestRule.onNodeWithTag("guest_login_button").performClick()
      composeTestRule.waitForIdle()

      // Dismiss beautiful guest promo dialog to allow tab clicking
      composeTestRule.onNodeWithTag("guest_promo_not_now_button").performClick()
      composeTestRule.waitForIdle()

      // 2. Navigate to "Inicio"
      composeTestRule.onNodeWithText("Inicio").performClick()
      composeTestRule.waitForIdle()

      // 3. Navigate to "Barberos"
      composeTestRule.onNodeWithText("Barberos").performClick()
      composeTestRule.waitForIdle()

      // 4. Navigate to "Sucursales"
      composeTestRule.onNodeWithText("Sucursales").performClick()
      composeTestRule.waitForIdle()

      // 5. Navigate to "Historial"
      composeTestRule.onNodeWithText("Historial").performClick()
      composeTestRule.waitForIdle()
    } catch (t: Throwable) {
      System.err.println("GUEST_TEST_EXCEPTION_START")
      t.printStackTrace(System.err)
      System.err.println("GUEST_TEST_EXCEPTION_END")
      throw t
    }
  }

  @Test
  fun testAdminTabsCrashValidation() {
    try {
      val context = ApplicationProvider.getApplicationContext<Application>()
      val viewModel = BarberiaViewModel(context)

      composeTestRule.setContent {
        MainAppScreen(viewModel = viewModel, initialShowSplash = false)
      }
      
      // Explicitly wait for database prepopulate and initial layout frame
      composeTestRule.waitForIdle()

      // 1. Open Admin Pin Dialog
      composeTestRule.onNodeWithText("INGRESAR PIN DE ADMIN").performClick()
      composeTestRule.waitForIdle()

      // 2. Input PIN and click Verify
      composeTestRule.onNodeWithTag("admin_pin_field").performTextInput("1234")
      composeTestRule.onNodeWithText("Verificar").performClick()
      composeTestRule.waitForIdle()

      // 3. Check if Admin option is present and click it
      composeTestRule.onNodeWithText("Admin").performClick()
      composeTestRule.waitForIdle()

      // 4. Try clicking around Sub-tabs: Reportes, Agenda, Corte Manual, etc.
      composeTestRule.onNodeWithText("Reportes & Gráficos").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("Agenda Reservas").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("Corte Manual (Walk-in)").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("Gestionar Barberos").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("Planillas Excel").performClick()
      composeTestRule.waitForIdle()
    } catch (t: Throwable) {
      System.err.println("ADMIN_TEST_EXCEPTION_START")
      t.printStackTrace(System.err)
      System.err.println("ADMIN_TEST_EXCEPTION_END")
      throw t
    }
  }
}
