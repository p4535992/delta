package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class DeleteDoc {

	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	
	@Test 
	public void deleteRegisteredDocs () throws Exception  {
			Util.doLogin(driver); 
			driver.findElement(By.id("pm1")).click();
			driver.findElement(By.partialLinkText("Täna registreeritud dokumendid"));
			
			while (Util.existsElement(driver, "Selenium test registreerimiseks")) {
				driver.findElement(By.partialLinkText("Selenium test registreerimiseks")).click();
				Thread.sleep(3000);
				driver.findElement(By.linkText("Kustuta dokument")).click();
				Thread.sleep(3000);
				driver.findElement(By.id("dialog:dialog-body:document-delete-reason-popup-_id2_reason")).sendKeys("Testimine lõpetatud");
				Thread.sleep(3000);
				driver.findElement(By.id("dialog:dialog-body:document-delete-reason-popup-_id2_reason_btn")).click();
				Thread.sleep(3000);
				driver.findElement(By.id("pm1")).click();
				driver.findElement(By.partialLinkText("Täna registreeritud dokumendid")); 
				
			}
	}
			
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteAruteluDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);
	/*	com.nortal.alfresco.test.selenium.Util.returnSelectedValueIndex("dialog:dialog-body:documentList:selPageSize", driver, 4);
		Thread.sleep(3000);
		WebElement exists = driver.findElement(By.id("remove-doc-limitation-link"));
		
		if (exists != null){
			exists.click();
		
		com.nortal.alfresco.test.selenium.Util.returnSelectedValueIndex("dialog:dialog-body:functionsList:selPageSize", driver, 4);
		Thread.sleep(3000);
		}*/
		while (Util.existsElement(driver, "Alusta arutelu dokumendiga")) {
			
			driver.findElement(By.partialLinkText("Alusta arutelu dokumendiga")).click();
			kustutaDoc();
			
		}
	
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteLemmikDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Lisa dokument lemmikute")) {
			driver.findElement(By.partialLinkText("Lisa dokument lemmikute")).click();
			kustutaDoc();
			
		}
	
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteChangedDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Metaandmete muutmine")) {
			driver.findElement(By.partialLinkText("Metaandmete muutmine")).click();
			kustutaDoc();
			
		}
	
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteTeadmiseksDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Teadmiseks töövoogu algat")) {
			driver.findElement(By.partialLinkText("Teadmiseks töövoogu algat")).click();
			kustutaDoc();
			
		}
	
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteKooskylastDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);
	
		while (Util.existsElement(driver, "Kooskõlastamiseks tööülesanne")) {
			driver.findElement(By.partialLinkText("Kooskõlastamiseks tööülesanne")).click();
			kustutaDoc();
			
		}
	
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteEdastaInfoksDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Edasta infoks dokument")) {
			driver.findElement(By.partialLinkText("Edasta infoks dokument")).click();
			kustutaDoc();
			
		}
	
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteTestVastusDocs () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Selenium test vastuse")) {
			driver.findElement(By.partialLinkText("Selenium test vastuse")).click();
			kustutaDoc();
			
		}
		
	}
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteVastusDoc () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Selenium vastusdokument")) {
			driver.findElement(By.partialLinkText("Selenium vastusdokument")).click();
			kustutaDoc();
			
		}
		
	}
	
	@Test (dependsOnMethods={"deleteRegisteredDocs"})
	public void deleteJargDoc () throws Exception  {
		
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
		Thread.sleep(3000);

		while (Util.existsElement(driver, "Selenium järgdokument")) {
			driver.findElement(By.partialLinkText("Selenium järgdokument")).click();
			kustutaDoc();
			
		}
		
	}
	
	//delete from Teema nimega Automaattest 
	private void kustutaDoc() throws InterruptedException {
		Thread.sleep(3000);
		driver.findElement(By.linkText("Kustuta dokument")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:dialog-body:document-delete-reason-popup-_id2_reason")).sendKeys("Testimine lõpetatud");
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:dialog-body:document-delete-reason-popup-_id2_reason_btn")).click();
		Thread.sleep(3000);
		driver.findElement(By.partialLinkText("NA7-TESTsari Teemadega")).click();
		driver.findElement(By.partialLinkText("Automaattest ")).click();
//		WebElement exists = driver.findElement(By.id("remove-doc-limitation-link"));
//		
//		if (exists != null){
//			exists.click();
//		}
		
		
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
