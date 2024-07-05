package com.example.thirdeyeautomotivesurveillence

import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.remote.MobileCapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import java.net.MalformedURLException
import java.net.URL

class BaseTest {
    protected var driver: AndroidDriver<MobileElement>? = null

    @BeforeClass
    @Throws(MalformedURLException::class)
    fun setUp() {
        val capabilities: DesiredCapabilities = DesiredCapabilities()
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android")
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "emulator-5554")
        capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, "com.example.myapp")
        capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, ".MainActivity")

        val url = URL("http://localhost:4723/wd/hub")
        driver = AndroidDriver(url, capabilities)
    }

    @AfterClass
    fun tearDown() {
        if (driver != null) {
            driver.quit()
        }
    }
}