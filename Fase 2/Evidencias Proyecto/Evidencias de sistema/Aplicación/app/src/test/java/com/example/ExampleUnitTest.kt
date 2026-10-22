package com.example

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.BarberiaViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testViewModelInitializationAndPrepopulate() = runTest {
    try {
      val context = ApplicationProvider.getApplicationContext<Application>()
      assertNotNull("Context should not be null", context)

      val viewModel = BarberiaViewModel(context)
      assertNotNull("ViewModel should initialize successfully", viewModel)

      // Let prepopulate finish and collect the locations Reactively
      val locations = viewModel.locationsFlow.first { it.isNotEmpty() }
      println("LOCATIONS_SIZE: ${locations.size}")
      for (loc in locations) {
        println("LOCATION_FOUND: id=${loc.id}, name=${loc.name}")
      }

      // Assert that we have 3 prepopulated Locations
      assertEquals("Locations should be prepopulated with 3 items", 3, locations.size)
    } finally {
      coroutineContext.cancelChildren()
    }
  }
}
