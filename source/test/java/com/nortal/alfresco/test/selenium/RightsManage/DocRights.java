package com.nortal.alfresco.test.selenium.RightsManage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class DocRights {

	
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
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Doc rights management");
			Thread.sleep(3000);
	
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-AutomaatSari Selenium");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-AutomaatSari ToimikYes");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000);	
			driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
			}
	
	@Test (dependsOnMethods={"createDoc"})
	public void avaDoc () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	
    	driver.findElement(By.xpath("//*[@id='dashboard:quickSearch']")).sendKeys("Doc rights management");
    	driver.findElement(By.id("dashboard:quickSearchBtn")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.partialLinkText("Doc rights management")).click();
    	Thread.sleep(3000);
    	String str = driver.findElement(By.className("error-message")).getText(); 
    	Assert.assertEquals(str, "Tegevus ebaõnnestus, Sul puudub dokumendile juurdepääsu õigus");
    	Thread.sleep(3000);
    	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc"})
	public void addFailiVaatamiseOigus () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.partialLinkText("NA7-AutomaatSari Toimi...")).click();
			driver.findElement(By.partialLinkText("Automaattest ")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Doc rights management")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Õiguste haldus")).click();
			Thread.sleep(3000);
			WebElement haldus = driver.switchTo().activeElement();
			haldus.sendKeys("Andra Olm");
			WebElement parent = haldus.findElement(By.xpath("..")); //get parent
			WebElement button = parent.findElement(By.className("specificAction"));
			button.click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker_results']/option")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker']/tbody/tr[6]/td/input")).click();
			Thread.sleep(3000);
			
			//kontrolli kas kasutaja on olemas
			WebElement dialog = driver.findElement(By.className("left"));
			String parent2 = dialog.findElement(By.xpath("../../..")).getText(); //get parent
			Assert.assertTrue(parent2.contains("Andra Olm (Webmedia, RIK)"));
	
			//add rights for user
			WebElement baseTable = dialog.findElement(By.xpath("../../.."));
		
	
			List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	
				for (WebElement element : tableRows) {
						element.getText();
							if (element.getText().contains("Andra Olm (Webmedia, RIK)")) {
								element.findElement(By.className("permission_viewDocumentFiles")).click();
								break;
										}	
								}
				
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
			}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc", "addFailiVaatamiseOigus"})
	public void avaDocAfter () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	
    	driver.findElement(By.xpath("//*[@id='dashboard:quickSearch']")).sendKeys("Doc rights management");
    	driver.findElement(By.id("dashboard:quickSearchBtn")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.partialLinkText("Doc rights management")).click();
    	Thread.sleep(3000);
    	String str = driver.findElement(By.className("info-message")).getText(); 
    	Assert.assertEquals(str, "Dokument on registreerimata.");
    	Thread.sleep(3000);
    	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc", "avaDocAfter"})
	public void addDocMuutmiseOigus () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.partialLinkText("NA7-AutomaatSari Toimi...")).click();
			driver.findElement(By.partialLinkText("Automaattest ")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Doc rights management")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Õiguste haldus")).click();
			Thread.sleep(3000);
			
			//kontrolli kas kasutaja on olemas
			WebElement dialog = driver.findElement(By.className("left"));
			String parent2 = dialog.findElement(By.xpath("../../..")).getText(); //get parent
			Assert.assertTrue(parent2.contains("Andra Olm (Webmedia, RIK)"));
	
			//add rights for user
			WebElement baseTable = dialog.findElement(By.xpath("../../.."));
		
	
			List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	
				for (WebElement element : tableRows) {
						element.getText();
							if (element.getText().contains("Andra Olm (Webmedia, RIK)")) {
								element.findElement(By.className("permission_editDocument")).click();
								break;
										}	
								}
				
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
			}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc", "addFailiVaatamiseOigus", "avaDocAfter", "addDocMuutmiseOigus"})
	public void avaDocAfter2 () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	
    	driver.findElement(By.xpath("//*[@id='dashboard:quickSearch']")).sendKeys("Doc rights management");
    	driver.findElement(By.id("dashboard:quickSearchBtn")).click();
    	Thread.sleep(5000);
    	driver.findElement(By.partialLinkText("Doc rights management")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("metadata-link-edit")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_ownerPhone:docdynx003a_ownerPhone")).sendKeys("55554321");
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:finish-button")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.linkText("Dokumendi logi")).click();
    	WebElement dialog = driver.findElement(By.id("document-block-block-panel-panel-border"));
		String parent = dialog.findElement(By.xpath("..")).getText(); //get parent
		Assert.assertTrue(parent.contains("Dokumendi metaandmeid on muudetud. Vastutaja telefon"));
		
    	Thread.sleep(3000);
    	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc", "avaDocAfter", "avaDocAfter2"})
	public void deleteOigus () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.partialLinkText("NA7-AutomaatSari Toimi...")).click();
			driver.findElement(By.partialLinkText("Automaattest ")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Doc rights management")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Õiguste haldus")).click();
			Thread.sleep(3000);
			
			//kontrolli kas kasutaja on olemas
			WebElement dialog = driver.findElement(By.className("left"));
			String parent2 = dialog.findElement(By.xpath("../../..")).getText(); //get parent
			Assert.assertTrue(parent2.contains("Andra Olm (Webmedia, RIK)"));
	
			//add rights for user
			WebElement baseTable = dialog.findElement(By.xpath("../../.."));
		
	
			List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	
				for (WebElement element : tableRows) {
						element.getText();
							if (element.getText().contains("Andra Olm (Webmedia, RIK)")) {
								element.findElement(By.className("removePerson")).click();
								  Alert alert = driver.switchTo().alert();
								    alert.accept();
								break;
										}	
								}
				
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
			}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc", "avaDocAfter", "avaDocAfter2", "deleteOigus"})
	public void avaDoc2 () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	
    	driver.findElement(By.xpath("//*[@id='dashboard:quickSearch']")).sendKeys("Doc rights management");
    	driver.findElement(By.id("dashboard:quickSearchBtn")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.partialLinkText("Doc rights management")).click();
    	Thread.sleep(3000);
    	String str = driver.findElement(By.className("error-message")).getText(); 
    	Assert.assertEquals(str, "Tegevus ebaõnnestus, Sul puudub dokumendile juurdepääsu õigus");
    	Thread.sleep(3000);
    	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"createDoc", "avaDoc", "avaDocAfter", "avaDocAfter2", "deleteOigus", "avaDoc2"})
	public void deleteDoc () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.partialLinkText("NA7-AutomaatSari Toimi...")).click();
			driver.findElement(By.partialLinkText("Automaattest ")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Doc rights management")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Kustuta dokument")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:document-delete-reason-popup-_id2_reason")).sendKeys("Testimine lõpetatud");
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:document-delete-reason-popup-_id2_reason_btn")).click();
			Thread.sleep(3000);
			String str = driver.findElement(By.className("info-message")).getText(); 
	    	Assert.assertEquals(str, "Dokument kustutatud");
	    	Thread.sleep(3000);
	}
	
	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
	
}
