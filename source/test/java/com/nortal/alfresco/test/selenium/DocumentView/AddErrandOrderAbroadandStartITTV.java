package com.nortal.alfresco.test.selenium.DocumentView;

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

public class AddErrandOrderAbroadandStartITTV {

	
WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	 
	
	@Test
	public void createStartITTVforGroup () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm8")).click();
			driver.findElement(By.id("pm8_48")).click();
//			driver.findElement(By.id("pm8_26_7")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Errand order abroad lisamine");
			Thread.sleep(3000);
	
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-TESTsari Testsari7");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-TESTsari Teemadega toimik");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_x007b_tempx007d_caseLabelEditable:x007b_tempx007d_caseLabelEditable']")).sendKeys("Automaattest");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(5000);
			
			driver.findElement(By.linkText("Registreeri")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Lisa välislähetuse taotlus")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).clear();
			Thread.sleep(2000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Selenium order abroad");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000); 
		
			driver.findElement(By.xpath("//*[@id='actions']/ul/li[1]/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("!! Koosta töövoog !!")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[3]/span/a")).click();
			Thread.sleep(3000);
			Select alus = new Select(driver.findElement(By.xpath("//select[starts-with(@id, 'dialog:dialog-body:compound-workflow-panel-group:workflow-1:prop_x007b_tempx007d_workflowTasks:task-dueDateDays-_id')]")));
			alus.selectByIndex(6);
			
			Thread.sleep(3000); 
			
			driver.findElement(By.xpath("//a[starts-with(@id, 'task-search-link-_id')]")).click();
			Thread.sleep(6000);
			
				
				WebElement dialog = driver.switchTo().activeElement();
				String activeId = dialog.getAttribute("name");
				
				dialog.sendKeys("Administraatorid");
			//	String activeId = dialog.getAttribute("class");
				WebElement parent = dialog.findElement(By.xpath("..")); //get parent
				WebElement button2 = parent.findElement(By.tagName("select"));
				button2.sendKeys("Kasutajagrupid");
				Thread.sleep(6000);
				
				
				WebElement button = parent.findElement(By.className("specificAction"));
				button.click();
				Thread.sleep(3000); 

				
				String modalId = activeId.substring(0, activeId.lastIndexOf("_"));
				String resultsBoxId = modalId + "_results";
				WebElement select = driver.findElement(By.id(resultsBoxId));
				Select dropDown = new Select(select);
				dropDown.selectByVisibleText("Administraatorid");
				
				Thread.sleep(3000); 
				
				driver.findElement(By.id(modalId)).findElement(By.className("picker-add")).click();
				
				Thread.sleep(6000);   
			
				driver.findElement(By.id("dialog:compound_workflow_start")).click();
				Thread.sleep(6000);
		
				
				String str = driver.findElement(By.className("info-message")).getText(); 
				Assert.assertEquals(str, "Terviktöövoog käivitatud");
				Thread.sleep(6000);
				
			
			
	}
	
	
	@Test (dependsOnMethods={"createStartITTVforGroup"})
	public void taskForGroup () throws Exception {
		
			driver.findElement(By.id("pm0")).click();
			Thread.sleep(3000);	
			//driver.findElement(By.id("sm0")).click();
			Thread.sleep(2000);
			driver.findElement(By.id("sm0_0")).click();
			Thread.sleep(4000);
			driver.findElement(By.partialLinkText("Selenium order abroad")).click();
			Thread.sleep(5000);
	    	driver.findElement(By.xpath("//textarea[@id[contains(.,'prop_wfsx003a_comment:wfsx003a_comment')]]")).sendKeys("Sai täidetud Anastassia poolt");
	    	Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:outcome-id-0-0")).click();
			Thread.sleep(5000);
			
			WebElement dialog = driver.findElement(By.className("info-message"));
			String parent = dialog.findElement(By.xpath("..")).getText(); //get parent
			Assert.assertTrue(parent.contains("Tööülesanne teostatud"));
		
			
			Thread.sleep(3000);
			
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
