package com.nortal.alfresco.test.selenium.IndependentCW;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class StartITTV {

	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}

	@Test 
	public void startITTV () throws Exception  {
			Util.doLogin(driver);
			
			driver.findElement(By.id("pm8")).click();
			driver.findElement(By.id("pm8_12")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("ITTVle lisamiseks");
			Thread.sleep(3000);
	
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(5000);
		
			driver.findElement(By.id("pm9")).click();
			driver.findElement(By.id("pm9_1")).click();
			Thread.sleep(2000);
			driver.findElement(By.id("pm9_1_0")).click();
			Thread.sleep(5000);
			
			driver.findElement(By.id("dialog:dialog-body:compound:prop_wfcx003a_title:wfcx003a_title")).sendKeys("ITTV täitmiseks");
			Thread.sleep(3000);
			driver.findElement(By.id("act-link-0")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:assoc-object-search-filter:prop_docsearchx003a_objectTitle:docsearchx003a_objectTitle")).sendKeys("ITTVle lisamiseks");
			driver.findElement(By.id("dialog:dialog-body:quickSearchBtn2")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("col-actions-act1")).click();
			Thread.sleep(3000); 
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[8]/span/a")).click();
			Thread.sleep(3000);
			
			Select alus = new Select(driver.findElement(By.xpath("//select[starts-with(@id, 'dialog:dialog-body:compound-workflow-panel-group:workflow-1:prop_x007b_tempx007d_workflowTasks:task-dueDateDays-_id')]")));
			alus.selectByIndex(6);
			
			Thread.sleep(3000);
			
			driver.findElement(By.xpath("//a[starts-with(@id, 'task-search-link-_id')]")).click();
			Thread.sleep(6000);
			Util.modalLisaKasutaja(driver, "Andra Olm", "Andra Olm (Webmedia, RIK)");
			
			driver.findElement(By.xpath("//a[starts-with(@id, 'task-add-link-_id')]")).click();
			Thread.sleep(6000); 
			driver.findElement(By.xpath("//a[starts-with(@id, 'task-add-date-link-_id')]")).click();
			Thread.sleep(6000); 
		
			
			String cssSelectorOfSameElements="a[class='icon-link margin-left-4 search']";

			 List<WebElement> a=driver.findElements(By.cssSelector(cssSelectorOfSameElements)) ;
			 a.get(2).click();
			//a.get(1).click();
			
			Thread.sleep(6000);
			
			Util.modalLisaKasutaja(driver, "Heili Sepp", "Heili Sepp (Webmedia, RIK)");
			driver.findElement(By.id("dialog:compound_workflow_start")).click();
			Thread.sleep(6000);
			driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
			
	}
	
	@Test (dependsOnMethods={"startITTV"})
	public void resolveAssigmentTask1 () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("andra.olm");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("sm0_0")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.partialLinkText("ITTV täitmiseks")).click();
    	Thread.sleep(5000);
    	driver.findElement(By.xpath("//textarea[@id[contains(.,'prop_wfsx003a_comment:wfsx003a_comment')]]")).sendKeys("Sai täidetud Andra Olm poolt");
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:dialog-body:outcome-id-0-0")).click(); //M2rgi tooylesanne teostatuks
    	Thread.sleep(6000);
    
		String parent = driver.findElement(By.id("workflowList")).getText(); 
		Assert.assertTrue(parent.contains("Sai täidetud Andra"));
    	Thread.sleep(3000);
		driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"startITTV"})
	public void resolveAssigmentTask2 () throws Exception  {
		driver.get("https://delta.webmedia.int/5.1");
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("heili.sepp");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("sm0_0")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.partialLinkText("ITTV täitmiseks")).click();
    	Thread.sleep(5000);
    	driver.findElement(By.xpath("//textarea[@id[contains(.,'prop_wfsx003a_comment:wfsx003a_comment')]]")).sendKeys("Sai täidetud Heili Sepp poolt");
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:dialog-body:outcome-id-0-0")).click(); //Margi tooylesanne teostatuks
    	Thread.sleep(6000);
  
    	String parent = driver.findElement(By.id("workflowList")).getText(); 
		Assert.assertTrue(parent.contains("Sai täidetud Heili"));
    	Thread.sleep(3000);
		driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"startITTV", "resolveAssigmentTask1", "resolveAssigmentTask2"})
	public void deleteITTV () throws Exception  {
	Util.doLogin(driver);
    	
    	driver.findElement(By.id("pm4")).click();
    	Thread.sleep(3000);
    	driver.findElement(By.id("sm0_3")).click();
    	Thread.sleep(3000);
    	Util.returnSelectedValueText("dialog:dialog-body:task-search-filter:prop_cwsearchx003a_type:cwsearchx003a_type", driver, "Iseseisev terviktöövoog");
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:dialog-body:task-search-filter:prop_cwsearchx003a_title:cwsearchx003a_title")).sendKeys("ITTV täitmiseks");
    	Thread.sleep(3000);
    	driver.findElement(By.id("dialog:finish-button")).click();
    	Thread.sleep(5000);
    	driver.findElement(By.linkText("ITTV täitmiseks")).click();
    	Thread.sleep(5000);
    	driver.findElement(By.linkText("Kustuta terviktöövoog")).click();
    	Thread.sleep(5000);
		driver.findElement(By.xpath("//*[@id='logout']")).click(); 
		Thread.sleep(3000);
	}
	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}