package com.usbmediaexplorer.qa

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usbmediaexplorer.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

/** API/device smoke gate; USB/SAF matrix remains a physical-device test. */
@RunWith(AndroidJUnit4::class)
class ApplicationSmokeTest {
    @Test
    fun mainActivityLaunches() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> check(!activity.isFinishing) }
        }
    }
}
