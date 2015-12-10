package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class CreateDoc{
	
	WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	 
	
	@Test 
	public void createDoc () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm8")).click();
			driver.findElement(By.id("pm8_12")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Selenium test registreerimiseks");
			Thread.sleep(3000);
	
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Metaandmed"));
			Thread.sleep(3000);
			driver.findElement(By.linkText("Registreeri")).click();
			
	}
	
	@Test (dependsOnMethods={"createDoc"})
	public void createDocCopy () throws Exception  {
		
		driver.findElement(By.id("pm1")).click();
		Thread.sleep(3000);
		driver.findElement(By.partialLinkText("Täna registreeritud dokumendid"));
		driver.findElement(By.partialLinkText("Selenium test registreerimiseks")).click();
		Thread.sleep(3000);
		driver.findElement(By.linkText("Loo koopia")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("- Koopia");
		driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
		Thread.sleep(3000);
		driver.findElement(By.partialLinkText("Metaandmed"));
		Thread.sleep(5000);
		driver.findElement(By.linkText("Registreeri")).click();
	}
	
	
	@Test (dependsOnMethods={"createDoc", "createDocCopy"})
	public void sendDocOut () throws Exception  {
		
		driver.findElement(By.id("pm1")).click();
		driver.findElement(By.partialLinkText("Täna registreeritud dokumendid"));
		driver.findElement(By.partialLinkText("Selenium test registreerimiseks-")).click();
		Thread.sleep(3000);
		driver.findElement(By.linkText("Saada välja")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:dialog-body:_idJsp69:recipientName_0")).sendKeys("Anastassia Soikonen");
		driver.findElement(By.id("dialog:dialog-body:_idJsp69:recipientEmail_0")).sendKeys("anastassia.soikonen@nortal.com");
		Util.returnSelectedValueIndex("dialog:dialog-body:_idJsp69:recipientSendMode_0", driver, 2);
		Util.searchByXpathreturnSelectedValueText("//*[@id='dialog:dialog-body:tableSendOut']/tbody/tr[7]/td[2]/span/select", driver, "text");
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:dialog-body:setTemplateBtn")).click();
		Thread.sleep(5000);
		driver.findElement(By.id("dialog:finish-button")).click();
	}
	
	@Test (dependsOnMethods={"createDoc", "createDocCopy", "sendDocOut"})
	public void generateWordFileFromDoc () throws Exception  {
		
		driver.findElement(By.id("pm1")).click();
		driver.findElement(By.partialLinkText("Täna registreeritud dokumendid"));
		driver.findElement(By.partialLinkText("Selenium test registreerimiseks")).click();
		Thread.sleep(6000);
		driver.findElement(By.linkText("Taasava dokument")).click();
		Thread.sleep(3000);
		//assertTrue(selenium.isTextPresent("Name of the Customer"));
	    driver.findElement(By.xpath("//*[@id='content']/div[3]/div")).getText().compareTo("Dokument taasavatud");
		Thread.sleep(3000);
		driver.findElement(By.linkText("Loo mallist fail")).click();
		Thread.sleep(5000);
		driver.findElement(By.xpath("//*[@id='files-panel']/div/h3/a")).getText().compareTo("Failid (1)");
		
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
	
	
}
