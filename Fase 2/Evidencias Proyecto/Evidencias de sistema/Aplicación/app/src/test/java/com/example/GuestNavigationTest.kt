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
class GuestNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun guestCanBrowseAllTabs() {
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

      // 5. Navigate to "Reseñas"
      composeTestRule.onNodeWithText("Reseñas").performClick()
      composeTestRule.waitForIdle()

      // 6. Navigate to "Fila"
      composeTestRule.onNodeWithText("Fila").performClick()
      composeTestRule.waitForIdle()
    } catch (t: Throwable) {
      System.err.println("GUEST_TEST_EXCEPTION_START")
      t.printStackTrace(System.err)
      System.err.println("GUEST_TEST_EXCEPTION_END")
      throw t
    }
  }
}
