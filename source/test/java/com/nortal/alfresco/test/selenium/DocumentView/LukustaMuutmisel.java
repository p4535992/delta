package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class LukustaMuutmisel {

WebDriver driver;
WebDriver driver2;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
    driver2 = new FirefoxDriver();
    driver2.manage().window().maximize();
    driver2.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	
	@Test 
	public void goToDocChangeView () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm8")).click();
			driver.findElement(By.id("pm8_12")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Metaandmete lukustamine");
			Thread.sleep(3000);
	
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 1);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(5000);
			driver.findElement(By.linkText("Õiguste haldus")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker']/tbody/tr[1]/td/input[1]")).sendKeys("Andra Olm");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker']/tbody/tr[1]/td/button")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker_results']/option")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker']/tbody/tr[6]/td/input")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:permissions-list:1:permissions-list_6']")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:cancel-button")).click();
			Thread.sleep(4000);
			driver.findElement(By.id("metadata-link-edit")).click();
			Thread.sleep(21000);
			
	}
	
	@Test 
	public void kontrolliLukustamist () throws Exception  {
		driver2.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver2.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver2.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver2.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
		driver2.findElement(By.id("pm1")).click();
		driver2.findElement(By.id("sm0")).click();
		Thread.sleep(5000);
		Util.returnSelectedValueIndex("dialog:dialog-body:functionsList:selPageSize", driver2, 4);
		Thread.sleep(3000);
		driver2.findElement(By.linkText("Nastja testfunktsioon")).click();
		Thread.sleep(3000);
		
		driver2.findElement(By.partialLinkText("Testsari7")).click();
		Thread.sleep(3000);
		driver2.findElement(By.partialLinkText("Teemadega toimik")).click();
		Thread.sleep(3000);
		driver2.findElement(By.partialLinkText("Automaattest")).click();
		Thread.sleep(3000);
		driver2.findElement(By.partialLinkText("Metaandmete lukustamine")).click();
		Thread.sleep(3000);
		driver2.findElement(By.id("metadata-link-edit")).click();
		Thread.sleep(3000);
		String str = driver2.findElement(By.className("error-message")).getText(); 
		Assert.assertEquals(str, "Dokument on avatud muutmiseks Anastassia Soikonen poolt");
		
    	
	}
	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	driver2.close();
	driver2.quit();
	
	}
	
}
