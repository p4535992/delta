package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class ChangeDocLiik {
	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	
	@Test 
	public void changeDocMeta () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm8")).click();
			driver.findElement(By.id("pm8_12")).click();
			Thread.sleep(5000);
	
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Dokumendi liigi muutmine");
			Thread.sleep(3000);
			
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("metadata-link-edit")).click();
			Thread.sleep(3000);
			Util.returnSelectedValueText("dialog:dialog-body:doc-types-select", driver, "Aruanne (PPA piiratud Delta)");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
	
			Thread.sleep(3000);
			
			String str = driver.findElement(By.className("title-icon")).getText();
			Assert.assertEquals(str, "Aruanne (PPA piiratud Delta)"); 
	
	
	}
	
	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
