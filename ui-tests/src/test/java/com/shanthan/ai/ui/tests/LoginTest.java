package com.shanthan.ai.ui.tests;

import com.shanthan.ai.ui.base.BaseTest;
import com.shanthan.ai.ui.listener.AiFailureListener;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(AiFailureListener.class)
public class LoginTest extends BaseTest {

    @Test
    public void Login_failure_with_invalid_credentials() {
        // Intentionally brittle locator so it fails sooner or later.
        getDriver().findElement(By.id("fake-username-field")).sendKeys("admin");
        getDriver().findElement(By.id("fake-password-field")).sendKeys("invalid");
        getDriver().findElement(By.cssSelector("button[type='submit']")).click();

        getDriver().get("https://opensource-demo.orangehrmlive.com/");

        // This assertion will probably never reach, added for the demo.
        Assert.assertTrue(driver.getPageSource().contains("Invalid credentials"),
                "Expected invalid credentials message");
    }
}
