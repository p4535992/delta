package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class GoToDocChangeView {

	
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
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Metaandmete muutmine");
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
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_comment:docdynx003a_comment")).sendKeys("Siia sai lisatud täiendav info");
			Thread.sleep(4000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerPhone:docdynx003a_ownerPhone")).sendKeys("56785678");
			Thread.sleep(4000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerWorkAddress:docdynx003a_ownerWorkAddress")).sendKeys("Raatuse 20, Tartu, Eesti");
			Thread.sleep(4000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerServiceRank:docdynx003a_ownerServiceRank")).sendKeys("QA specialist");
			Thread.sleep(4000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionEndDesc:docdynx003a_accessRestrictionEndDesc")).sendKeys("Kuni uue dokumendi kooskõlastamiseni");
			Thread.sleep(4000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerName:docdynx003a_ownerName_container']/table/tbody/tr/td[2]/a")).click();
			Thread.sleep(4000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerName:docdynx003a_ownerName:picker_docdynx003a_ownerName']/tbody/tr[1]/td/button")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerName:docdynx003a_ownerName:picker_docdynx003a_ownerName_results']/option[2]")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerName:docdynx003a_ownerName:picker_docdynx003a_ownerName']/tbody/tr[5]/td/input")).click();
			Thread.sleep(5000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(5000);
			driver.findElement(By.xpath("//textarea[starts-with(@id, 'dialog:dialog-body:access-restriction-change-reason-popup-_')]")).sendKeys("Muudetud lisatud automatiseeritud");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//input[starts-with(@id, 'dialog:dialog-body:access-restriction-change-reason-popup-_id')]")).click();
			Thread.sleep(6000);
			driver.findElement(By.id("metadata-link-edit")).click();
			Thread.sleep(4000);
			driver.findElement(By.id("dialog:cancel-button")).click();
			Thread.sleep(4000);
			
	}
	
	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
