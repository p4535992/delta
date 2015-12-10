package com.nortal.alfresco.test.selenium.DocumentView;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class BeginDiscuss {

	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void startDiscussion () throws Exception  {
	
	Util.doLogin(driver);
	driver.findElement(By.id("pm8")).click();
	driver.findElement(By.id("pm8_12")).click();
	Thread.sleep(5000);
	driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Alusta arutelu dokumendiga");
	Thread.sleep(3000);

	
	Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
	Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
	
	driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
	Thread.sleep(7000);
	
	driver.findElement(By.linkText("Alusta arutelu")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:name")).sendKeys("Alustatud uus arutelu");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:message")).sendKeys("Lisatud esimene arutelu teade");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(4000);
	driver.findElement(By.linkText("Postita arutelu alla")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:message")).sendKeys("Lisatud teade arutelu alla");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Postita arutelu alla")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:cancel-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Halda arutelus osalejaid")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa kasutaja/kasutajagrupp")).click();
	Thread.sleep(4000);
	
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker']/tbody/tr[1]/td/input[1]")).sendKeys("Andra Olm");
	Thread.sleep(4000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker_results']/option")).click();
	Thread.sleep(6000);
	driver.findElement(By.xpath("//*[@class='picker-add']")).click();
	Thread.sleep(6000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(5000);
	driver.findElement(By.linkText("Lisa kasutaja/kasutajagrupp")).click();
	Thread.sleep(4000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker']/tbody/tr[1]/td/input[1]")).sendKeys("Anastassia Soikonen");
	Thread.sleep(4000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:picker_results']/option")).click();
	Thread.sleep(6000);
	
	driver.findElement(By.xpath("//*[@class='picker-add']")).click();
	Thread.sleep(6000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(5000);
	driver.findElement(By.id("dialog:cancel-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='logout']")).click();
	Thread.sleep(3000);
	
	}
	
	@Test (dependsOnMethods={"startDiscussion"})
	public void OtherUserDiscuss () throws Exception {
		 driver.get("https://delta.webmedia.int/5.1");
	   
	    	WebElement username = driver.findElement(By.id("username"));
	    	username.sendKeys("andra.olm");
	    	
	    	WebElement password = driver.findElement(By.id("password"));
	    	password.sendKeys("Qwerty7");
	    	
	    	driver.findElement(By.name("submit")).click();
	    	Thread.sleep(3000);
	    	driver.findElement(By.id("sm0_4")).click();
	    	Thread.sleep(5000);
	    	Util.existsElement(driver, "Alusta arutelu dok");
	    	driver.findElement(By.partialLinkText("Alusta arutelu dok")).click();
			Thread.sleep(5000);
			driver.findElement(By.linkText("Vaata arutelu")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Alustatud uus arutelu")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("post_reply")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:message")).sendKeys("Andra Olm postitas vastuse");;
			Thread.sleep(5000);
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='logout']")).click();
			Thread.sleep(3000);
	}

	
	@Test (dependsOnMethods={"startDiscussion", "OtherUserDiscuss"})
	public void deleteDiscuss () throws Exception {
		Util.doLogin(driver);
		driver.findElement(By.id("sm0_4")).click();
    	Thread.sleep(5000);
    	Util.existsElement(driver, "Alusta arutelu dok");
    	driver.findElement(By.partialLinkText("Alusta arutelu dok")).click();
		Thread.sleep(5000);
		driver.findElement(By.linkText("Vaata arutelu")).click();
		Thread.sleep(3000);
		driver.findElement(By.linkText("Kustuta arutelu")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:finish-button")).click();
		Thread.sleep(3000);
	}
	
	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
}
