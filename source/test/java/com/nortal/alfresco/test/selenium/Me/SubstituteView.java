package com.nortal.alfresco.test.selenium.Me;

import java.util.Calendar;
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

public class SubstituteView {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void arvamuseAndmiseksTTV () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm8")).click();
	driver.findElement(By.id("pm8_12")).click();
	Thread.sleep(5000);
	driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Asendajale teostamiseks");
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
	driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[2]/span/a")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:compound:prop_wfcx003a_title:wfcx003a_title")).sendKeys("Asendajale terviktöövoog test");
	Thread.sleep(3000);
	Select alus = new Select(driver.findElement(By.xpath("//select[starts-with(@id, 'dialog:dialog-body:compound-workflow-panel-group:workflow-1:prop_x007b_tempx007d_workflowTasks:task-dueDateDays-_id')]")));
	alus.selectByIndex(6);
	
	Thread.sleep(3000);
	
	driver.findElement(By.xpath("//a[starts-with(@id, 'task-search-link-_id')]")).click();
	Thread.sleep(6000);
	Util.modalLisaKasutaja(driver, "Andra Olm", "Andra Olm (Webmedia, RIK)");
	driver.findElement(By.id("dialog:compound_workflow_start")).click();
	Thread.sleep(6000);
	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"arvamuseAndmiseksTTV"})
	public void substitute () throws Exception  {
		 driver.get("https://delta.webmedia.int/5.1");
		   
	    	WebElement username = driver.findElement(By.id("username"));
	    	username.sendKeys("andra.olm");
	    	
	    	WebElement password = driver.findElement(By.id("password"));
	    	password.sendKeys("Qwerty7");
	    	
	    	driver.findElement(By.name("submit")).click();
	    	Thread.sleep(3000);
			driver.findElement(By.id("pm3")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("sm2_1")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Lisa asendaja")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:substituteList:0:substituteSearch_container']/table/tbody/tr/td[2]/a")).click();
			Thread.sleep(3000);
			Util.modalLisaKasutaja(driver, "Heili Sepp", "Heili Sepp (Webmedia, RIK)");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:substituteList:0:substitutionStartDateInput']")).click();
			Thread.sleep(3000);
			Calendar calendar = Calendar.getInstance();
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			System.out.println(day);
			String string = day + "";
			
			WebElement dialog = driver.findElement(By.className("ui-state-default"));
			String parent2 = dialog.findElement(By.xpath("../../..")).getText(); //get parent
			Assert.assertTrue(parent2.contains(string));
			
			WebElement baseTable = dialog.findElement(By.xpath("../../.."));
			
			
			List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
	
				for (WebElement element : tableRows) {
						element.getText();
							if (element.getText().contains(string)) {
								element.findElement(By.className("ui-state-hover")).click();
								break;
										}	
								}
			
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:substituteList:0:substitutionEndDateInput']")).click();
			Util.searchByXpathreturnSelectedValueText("//*[@id='ui-datepicker-div']/div/div/select[2]", driver, "2020"); //tuleb muuta aastal 2020 :)
			Thread.sleep(3000);
			WebElement dialog2 = driver.findElement(By.className("ui-state-default"));
			WebElement baseTable2 = dialog2.findElement(By.xpath("../../.."));
			
			List<WebElement> tableRows2 = baseTable2.findElements(By.tagName("tr"));
			int day2 = calendar.get(Calendar.DAY_OF_MONTH) + 7;
			System.out.println(day2);
			String string2 = day2 + "";
	
				for (WebElement element : tableRows2) {
						element.getText();
							if (element.getText().contains(string2)) {
								element.findElement(By.className("ui-state-default")).click();
								break;
										}	
								}
		Thread.sleep(3000);
		driver.findElement(By.id("dialog:finish-button")).click();
		Thread.sleep(3000);
		String str = driver.findElement(By.className("info-message")).getText(); 
    	Assert.assertEquals(str, "Andmed salvestatud");
    	Thread.sleep(3000);
    	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
    	Thread.sleep(3000);
    	
	}	
	

	
	@Test (dependsOnMethods={"arvamuseAndmiseksTTV", "substitute"})
	public void doneBySubstitute () throws Exception  {
		 driver.get("https://delta.webmedia.int/5.1");
		   
	    	WebElement username = driver.findElement(By.id("username"));
	    	username.sendKeys("heili.sepp");
	    	
	    	WebElement password = driver.findElement(By.id("password"));
	    	password.sendKeys("Qwerty7");
	    	
	    	driver.findElement(By.name("submit")).click();
	    	Thread.sleep(3000);
	    	
	    	Util.returnSelectedValueText("dashboard:select_user", driver, "Andra Olm");
	    	Thread.sleep(6000);
	    	driver.findElement(By.id("pm0")).click();
	    	Thread.sleep(3000);
	    	driver.findElement(By.id("sm0_3")).click();
	    	Thread.sleep(3000);
	    	driver.findElement(By.partialLinkText("Asendajale teostamiseks")).click();
	    	Thread.sleep(3000);
	    	
	    	driver.findElement(By.xpath(".//textarea[@id[contains(.,'comment:wfsx003a_comment')]]")).sendKeys("Arvamus Heili poolt");
	    	Thread.sleep(3000);
	    	driver.findElement(By.id("dialog:dialog-body:outcome-id-0-0")).click(); //Kinnitan
	    	Thread.sleep(6000);
	    	
	    	driver.findElement(By.linkText("Tööülesanded")).click();
	    	Thread.sleep(3000);
	    	
	    	WebElement dialog = driver.findElement(By.id("workflowSummaryBlock-panel-border"));
			String parent = dialog.findElement(By.xpath("..")).getText(); //get parent
			Assert.assertTrue(parent.contains("Arvamus antud"));
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='logout']")).click(); 
			Thread.sleep(3000);
	    	
	}
	
	@Test (dependsOnMethods={"arvamuseAndmiseksTTV", "substitute", "doneBySubstitute"})
	public void deleteSubstitute () throws Exception  {
		 driver.get("https://delta.webmedia.int/5.1");
		   
	    	WebElement username = driver.findElement(By.id("username"));
	    	username.sendKeys("andra.olm");
	    	
	    	WebElement password = driver.findElement(By.id("password"));
	    	password.sendKeys("Qwerty7");
	    	
	    	driver.findElement(By.name("submit")).click();
	    	Thread.sleep(3000);
			driver.findElement(By.id("pm3")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("sm2_1")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='deleteLink']")).click();
			Thread.sleep(3000);
			String str2 = driver.findElement(By.className("info-message")).getText(); 
			Assert.assertEquals(str2, "Asendus kustutatud");
			Thread.sleep(3000);
	}	
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
