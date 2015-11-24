package com.nortal.alfresco.test.selenium.Asjatoimikud;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class BeginAndTegevused {

	WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void createNew () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm9")).click();
			driver.findElement(By.id("pm9_0")).click();
			driver.findElement(By.id("pm9_0_0")).click();
			Thread.sleep(5000);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_docdynx003a_title:docdynx003a_title']")).sendKeys("Automaat asjatoimik");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"createNew"})
	public void addToFavorite () throws Exception  {

			driver.findElement(By.linkText("Lisa lemmikutesse")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//textarea[starts-with(@id, 'dialogx003a_dialog-bodyx003a_favorite-popup-_')]")).sendKeys("Lemmik asjatoimik");
			Thread.sleep(5000);
			driver.findElement(By.xpath("//input[starts-with(@id, 'dialog:dialog-body:favorite-popup-_id')]")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("pm3")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("sm1")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Lemmik asjatoimik")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Automaat asjatoimik")).click();
			Thread.sleep(4000);
			driver.findElement(By.linkText("Kustuta lemmikutest")).click();
			Thread.sleep(4000);
			driver.findElement(By.linkText("Lisa lemmikutesse"));	
	}
	
	
	@Test (dependsOnMethods={"createNew", "addToFavorite"})
	public void sulgeTaasavaToimik () throws Exception  {

			driver.findElement(By.linkText("Sulge toimik")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Taasava")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Sulge toimik"));
				
	}
	
	@Test (dependsOnMethods={"createNew", "addToFavorite", "sulgeTaasavaToimik"})
	public void edastaInfoks () throws Exception  {

			driver.findElement(By.linkText("Edasta infoks")).click();
			Thread.sleep(3000);
	
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:searchSendForInformationUsers_container']/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:searchSendForInformationUsers:picker_searchSendForInformationUsers']/tbody/tr[1]/td/input[1]")).sendKeys("Andra Olm");
			Thread.sleep(5000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:searchSendForInformationUsers:picker_searchSendForInformationUsers_results']/option")).click();
			Thread.sleep(4000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:searchSendForInformationUsers:picker_searchSendForInformationUsers']/tbody/tr[5]/td/input")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='sendForInformation-panel-panel-border']/table/tbody/tr[2]/td[2]/textarea")).sendKeys("Asjatoimiku edastamine infoks test");
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
				
	}
	
	@Test (dependsOnMethods={"createNew", "addToFavorite", "sulgeTaasavaToimik", "edastaInfoks"})
	public void vaataURL () throws Exception {
			driver.findElement(By.linkText("Vaata asjatoimiku URLi")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='generateCaseFileLinkModal']/div[1]/p/a")).click();
			Thread.sleep(3000);		
	}
	
	@Test (dependsOnMethods={"createNew", "addToFavorite", "sulgeTaasavaToimik", "edastaInfoks", "vaataURL"})
	public void kustutaAsjatoimik () throws Exception {
			driver.findElement(By.linkText("Kustuta asjatoimik")).click();
			Thread.sleep(3000);	
			driver.findElement(By.xpath("//textarea[starts-with(@id, 'dialog:dialog-body:caseFile-delete-reason-popup-_id')]")).sendKeys("Testimine lõpetatud");
			Thread.sleep(4000);
			driver.findElement(By.xpath("//input[starts-with(@id, 'dialog:dialog-body:caseFile-delete-reason-popup-_id')]")).click();
			Thread.sleep(3000);
			
			String str = driver.findElement(By.className("info-message")).getText(); 
			Assert.assertEquals(str, "Asjatoimik kustutatud");
			Thread.sleep(3000);
	}
	
	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
	
	
}
