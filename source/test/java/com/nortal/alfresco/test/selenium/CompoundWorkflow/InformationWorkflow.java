package com.nortal.alfresco.test.selenium.CompoundWorkflow;

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

public class InformationWorkflow {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	

	@Test 
	public void edastaInfoks () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm8")).click();
	driver.findElement(By.id("pm8_12")).click();
	Thread.sleep(5000);
	driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Teadmiseks töövoogu algatamine");
	Thread.sleep(3000);
	
	Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
	Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
	Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
	driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
	Thread.sleep(7000);
	driver.findElement(By.xpath("//*[@id='actions']/ul/li[2]/a")).click();
	Thread.sleep(4000);
	driver.findElement(By.linkText("Koosta töövoog!")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:compound:prop_wfcx003a_title:wfcx003a_title")).sendKeys("Teadmiseks terviktöövoog test");
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[8]/span/a")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//a[starts-with(@id, 'task-search-link-_id')]")).click();
	Thread.sleep(6000);
	Util.modalLisaKasutaja(driver, "Andra Olm", "Andra Olm (Webmedia, RIK)");
	driver.findElement(By.id("dialog:compound_workflow_start")).click();
	Thread.sleep(6000);
	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"edastaInfoks"})
	public void votaTeadmiseks () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("sm0_1")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.partialLinkText("Teadmiseks töövoogu algat")).click();
    	Thread.sleep(5000);
    	driver.findElement(By.id("dialog:dialog-body:outcome-id-0-0")).click(); //v6tan teadmiseks
    	Thread.sleep(6000);
    	driver.findElement(By.linkText("Tööülesanded")).click();
    	Thread.sleep(3000);

    	WebElement dialog = driver.findElement(By.id("workflowSummaryBlock-panel-border"));
		String parent = dialog.findElement(By.xpath("..")).getText(); //get parent
		Assert.assertTrue(parent.contains("Teadmiseks võetud"));
    	
    	
	}
	
	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
}
