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
        driver.findElement(By.id("fake-username-field")).sendKeys("admin");
        driver.findElement(By.id("fake-password-field")).sendKeys("invalid");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        driver.get("https://opensource-demo.orangehrmlive.com/");

        // This assertion will probably never be reached, but it's fine for demo.
        Assert.assertTrue(driver.getPageSource().contains("Invalid credentials"),
                "Expected invalid credentials message");
    }
}
