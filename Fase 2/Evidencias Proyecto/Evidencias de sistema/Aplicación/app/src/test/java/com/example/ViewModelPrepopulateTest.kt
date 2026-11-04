package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Location
import com.example.ui.viewmodel.BarberiaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelPrepopulateTest {

  @Test
  fun viewModelPrepopulatesThreeLocations() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BarberiaViewModel(context)

    // Robolectric pauses the main looper: run its queue until the three locations are inserted
    var locations = emptyList<Location>()
    val job = CoroutineScope(Dispatchers.Unconfined).launch {
      viewModel.locationsFlow.collect { locations = it }
    }
    val deadline = System.currentTimeMillis() + 30_000
    while (locations.size < 3 && System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(50)
    }
    job.cancel()

    assertEquals("Locations should be prepopulated with 3 items", 3, locations.size)
  }
}
