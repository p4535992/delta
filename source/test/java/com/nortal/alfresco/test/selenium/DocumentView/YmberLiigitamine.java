package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class YmberLiigitamine {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	
	@Test 
	public void liigitaYmber () throws Exception  {
			Util.doLogin(driver);
			
			driver.findElement(By.id("pm1")).click();
			Thread.sleep(2000);
			driver.findElement(By.id("sm0")).click();
			Thread.sleep(5000);
			Util.returnSelectedValueIndex("dialog:dialog-body:functionsList:selPageSize", driver, 4);
			Thread.sleep(3000);
			driver.findElement(By.linkText("Nastja testfunktsioon")).click();
			Thread.sleep(3000);
			
			driver.findElement(By.partialLinkText("Testsari7")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Teemadega toimik")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Automaattest")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:documentList:col0-header")).click();
			Thread.sleep(3000);
		
			driver.findElement(By.linkText("Liigita ümber")).click();
			Thread.sleep(3000);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metadata:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Thread.sleep(3000);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metadata:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Thread.sleep(3000);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metadata:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metadata:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest_liigita");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:_idJsp49_1_doc_loc_btn']")).click();
			Thread.sleep(5000);
			
			WebDriverWait wait = new WebDriverWait(driver, 2);
			try{
			    wait.until(ExpectedConditions.alertIsPresent());
			    Alert alert = driver.switchTo().alert();
			    alert.accept();
			}
			catch (Exception e){
			    System.out.println("No alert");
			}
			
			//kui teema/toimiku all leiduvad dokumendid, millidel on vastus-j�rg seosed, siis Alert will be presented
		
			 
				
			
			Thread.sleep(8000);
			
			
			
	}
	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
	
}
