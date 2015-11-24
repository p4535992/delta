package com.nortal.alfresco.test.selenium.Search;

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

public class DocSearch {

	
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
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Try to find this doc");
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
	public void searchDoc () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm4")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("sm0_0")).click();
			Thread.sleep(3000);
			
			
			
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[3]/td[1]/table/tbody/tr/td[2]/input")).click(); //dok liik
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[4]/td[1]/table/tbody/tr/td[2]/input")).click(); //saatmisviis
			Thread.sleep(3000);
	
		
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[28]/td[1]/table/tbody/tr/td[2]/input")).click(); //viit	
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[21]/td[1]/table/tbody/tr/td[2]/input")).click(); //adressaadi nimi
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[16]/td[1]/table/tbody/tr/td[2]/input")).click(); // saatja
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[34]/td[1]/table/tbody/tr/td[2]/input")).click(); // reg.kpv
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[24]/td[1]/table/tbody/tr/td[2]/input")).click(); //tahtaeg
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[14]/td[1]/table/tbody/tr/td[2]/input")).click(); //marksonad
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter']/tbody/tr[27]/td[1]/table/tbody/tr/td[2]/input")).click(); //vastamise kpv
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:document-search-filter:prop_docdynx003a_docName:docdynx003a_docName']")).sendKeys("Try to find this doc");
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Try to find this doc")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:cancel-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:cancel-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:searchTitle")).sendKeys("Try to find");
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:toAllUsers")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:save")).click();
			Thread.sleep(3000);
	    	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
			
	} 
	
	@Test (dependsOnMethods={"createDoc", "searchDoc"})
	public void useSearch () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("pm4")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("sm0_0")).click();
		Thread.sleep(3000);
		Util.returnSelectedValueText("dialog:dialog-body:filters", driver, "Try to find");
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:finish-button")).click();
		Thread.sleep(3000);
		driver.findElement(By.linkText("Try to find this doc")).click();
		Thread.sleep(3000);
    	String str = driver.findElement(By.className("error-message")).getText(); 
    	Assert.assertEquals(str, "Tegevus ebaõnnestus, Sul puudub dokumendile juurdepääsu õigus");
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:cancel-button")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:dialog-body:delete")).click();
		Thread.sleep(3000);
    	
	}
    	
	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
	
}
