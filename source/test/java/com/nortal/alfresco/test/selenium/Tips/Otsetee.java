package com.nortal.alfresco.test.selenium.Tips;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class Otsetee {
	
	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	

	@Test 
	public void lisaOtsetee () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm1")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("sm1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Juhtimine")).click();
	Thread.sleep(3000);
	driver.findElement(By.partialLinkText("22112012")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("22112012 (2)")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("volume-shortcut-add")).click();
	Thread.sleep(4000);
	driver.findElement(By.id("outcome-shortcut-remove")).click();
	Thread.sleep(3000);
	
	
	}

	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
}
