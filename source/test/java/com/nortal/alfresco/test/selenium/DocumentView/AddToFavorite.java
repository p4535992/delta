package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class AddToFavorite {
	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	

	@Test 
	public void addFavorite () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm8")).click();
	driver.findElement(By.id("pm8_12")).click();
	Thread.sleep(5000);
	driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Lisa dokument lemmikute hulka");
	Thread.sleep(3000);
	
	Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
	Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
	
	driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
	Thread.sleep(7000);
	driver.findElement(By.linkText("Lisa lemmikutesse")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//textarea[starts-with(@id, 'dialogx003a_dialog-bodyx003a_favorite-popup-_')]")).sendKeys("Lemmikud dokumendid");
	Thread.sleep(5000);
	driver.findElement(By.xpath("//input[starts-with(@id, 'dialog:dialog-body:favorite-popup-_id')]")).click();
	Thread.sleep(10000);
	driver.findElement(By.id("pm3")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("sm1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lemmikud dokumendid")).click();
	Thread.sleep(3000);
	driver.findElement(By.partialLinkText("Lisa dokument lemmikute")).click();
	Thread.sleep(4000);
	driver.findElement(By.linkText("Kustuta lemmikutest")).click();
	Thread.sleep(4000);
	driver.findElement(By.linkText("Lisa lemmikutesse"));
	
	}

	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}

}
