package com.nortal.alfresco.test.selenium.Asjatoimikud;

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

public class WorkflowsRights {

	WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
	}
	
	@Test 
	public void create () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm9")).click();
			driver.findElement(By.id("pm9_0")).click();
			driver.findElement(By.id("pm9_0_0")).click();
			Thread.sleep(5000);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-AutomaatAT Asjtaoimikute hoidmiseks");
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:doc-metatada:prop_docdynx003a_title:docdynx003a_title']")).sendKeys("Automaat AT WR");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000);
	}
	
	
	
	@Test (dependsOnMethods={"create"})
	public void asjatoimikuLogi () throws Exception  {
			driver.findElement(By.linkText("Asjatoimiku logi")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Sulge toimik")).click();
			Thread.sleep(3000);
			String suletud = driver.findElement(By.className("info-message")).getText(); 
			Assert.assertEquals(suletud, "Asjatoimik suletud");
			
			driver.findElement(By.linkText("Taasava")).click();
			Thread.sleep(3000);
			String str = driver.findElement(By.className("info-message")).getText(); 
			Assert.assertEquals(str, "Asjatoimik taasavatud");
			
			String str2 = driver.findElement(By.id("dialog:dialog-body:logList:0:col3-txt")).getText(); 
			Assert.assertEquals(str2, "Asjatoimiku loomine");
			String str3 = driver.findElement(By.id("dialog:dialog-body:logList:1:col3-txt")).getText(); 
			Assert.assertEquals(str3, "Asjatoimiku sulgemine");
			String str4 = driver.findElement(By.id("dialog:dialog-body:logList:2:col3-txt")).getText(); 
			Assert.assertEquals(str4, "Asjatoimiku avamine");
			Thread.sleep(3000);
	}
		
	@Test (dependsOnMethods={"create", "asjatoimikuLogi"})
	public void asjatoimikuWorkflowLogi () throws Exception  {

			driver.findElement(By.xpath("//*[@id='actions']/ul/li/a")).click();
			Thread.sleep(4000);
			driver.findElement(By.linkText("!!Koosta töövoog!!")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[3]/span/a")).click();
			Thread.sleep(3000);
			Util.returnSelectedValueIndex("dialog:dialog-body:compound-workflow-panel-group:workflow-1:prop_wfcx003a_parallelTasks:wfcx003a_parallelTasks", driver, 1);
			Thread.sleep(3000);
			Select alus = new Select(driver.findElement(By.xpath("//select[starts-with(@id, 'dialog:dialog-body:compound-workflow-panel-group:workflow-1:prop_x007b_tempx007d_workflowTasks:task-dueDateDays-_id')]")));
			alus.selectByIndex(6);
			
			Thread.sleep(3000);
			
			driver.findElement(By.xpath("//a[starts-with(@id, 'task-search-link-_id')]")).click();
			Thread.sleep(6000);
			Util.modalLisaKasutaja(driver, "Andra Olm", "Andra Olm (Webmedia, RIK)");
			driver.findElement(By.id("dialog:compound_workflow_start")).click();
			Thread.sleep(6000);
			driver.findElement(By.id("dialog:cancel-button")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Asjatoimiku logi")).click();
			Thread.sleep(3000);
			
			
			
			driver.findElement(By.linkText("Tööülesanded")).click();
			String str1 = driver.findElement(By.id("col4-act")).getText(); 
			Assert.assertEquals(str1, "Kooskõlastamiseks");
			
			String str2 = driver.findElement(By.id("dialog:dialog-body:workflowList:0:col8-text")).getText(); 
			Assert.assertEquals(str2, "teostamisel");
			
			String str3 = driver.findElement(By.id("dialog:dialog-body:logList:3:col3-txt")).getText(); 
			Assert.assertEquals(str3, "Asjatoimikuga on seotud terviktöövoog");
			
			String str4 = driver.findElement(By.id("dialog:dialog-body:logList:4:col3-txt")).getText(); 
			Assert.assertEquals(str4, "Teavituse \"Dokumendihaldus: Sulle saabus tööülesanne\" edastamine aadressidele kristel.madiste@nortal.com");
			Thread.sleep(3000);
			
			driver.findElement(By.linkText("Õiguste haldus")).click();
			Thread.sleep(3000);
			
			//kontrolli kas kasutaja on olemas
			WebElement dialog = driver.findElement(By.className("left"));
			String parent = dialog.findElement(By.xpath("../../..")).getText(); //get parent
			Assert.assertTrue(parent.contains("Andra Olm (Webmedia, RIK)"));
			
			//add rights for user
			WebElement baseTable = dialog.findElement(By.xpath("../../.."));
			
			
			List<WebElement> tableRows = baseTable.findElements(By.tagName("tr"));
			
			for (WebElement element : tableRows) {
				element.getText();
				if (element.getText().contains("Andra Olm (Webmedia, RIK)")) {
					element.findElement(By.className("permission_editCaseFile")).click();
					break;
				}
			}
			
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(3000);
			
	}
	
	@Test (dependsOnMethods={"create", "asjatoimikuLogi", "asjatoimikuWorkflowLogi"})
	public void asjatoimikuDocWorkflowLogi () throws Exception  {	
			driver.findElement(By.id("pm8")).click();
			driver.findElement(By.id("pm8_12")).click();
			Thread.sleep(5000);
			driver.findElement(By.id("dialog:dialog-body:doc-metatada:prop_docdynx003a_docName:docdynx003a_docName")).sendKeys("Dokumentide kinnitamiseks töövood");
			Thread.sleep(3000);
			
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestriction:docdynx003a_accessRestriction", driver, 2);
			Util.returnSelectedValueIndex("dialog:dialog-body:doc-metatada:prop_docdynx003a_accessRestrictionReason:select_source", driver, 2);
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_function:docdynx003a_function", driver, "NA7 Nastja testfunktsioon");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_series:docdynx003a_series", driver, "NA7-AutomaatAT Asjtaoimikute hoidmiseks");
			Util.returnSelectedValueText("dialog:dialog-body:doc-metatada:prop_docdynx003a_volume:docdynx003a_volume", driver, "NA7-AutomaatAT/14 Automaat AT WR");
			
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(7000);
			
			
			driver.findElement(By.xpath("//*[@id='actions']/ul/li[2]/a")).click();
			Thread.sleep(4000);
			driver.findElement(By.linkText("Koosta töövoog!")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:compound:prop_wfcx003a_title:wfcx003a_title")).sendKeys("Kinnitamiseks automaattest");
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[5]/span/a")).click();
			Thread.sleep(3000);
			
			Select alus2 = new Select(driver.findElement(By.xpath("//select[starts-with(@id, 'dialog:dialog-body:compound-workflow-panel-group:workflow-1:prop_x007b_tempx007d_workflowTasks:task-dueDateDays-_id')]")));
			alus2.selectByIndex(6);
			
			Thread.sleep(3000);
			
			driver.findElement(By.xpath("//a[starts-with(@id, 'task-search-link-_id')]")).click();
			Thread.sleep(6000);
			Util.modalLisaKasutaja(driver, "Andra Olm", "Andra Olm (Webmedia, RIK)");
			driver.findElement(By.id("dialog:compound_workflow_start")).click();
			Thread.sleep(7000);
			driver.findElement(By.id("pm1")).click();
			Thread.sleep(2000);
			driver.findElement(By.id("sm0")).click();
			Thread.sleep(5000);
			Util.returnSelectedValueIndex("dialog:dialog-body:functionsList:selPageSize", driver, 4);
			Thread.sleep(3000);
			driver.findElement(By.linkText("Nastja testfunktsioon")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Asjtaoimikute hoidmiseks")).click();
			Thread.sleep(3000);
			driver.findElement(By.partialLinkText("Automaat AT WR")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Dokumentide töövood")).click();
			
			WebElement dialog = driver.findElement(By.id("caseFile-document-workflows-panel-panel-border"));
			String parent = dialog.findElement(By.xpath("..")).getText(); //get parent
			Assert.assertTrue(parent.contains("Kinnitamiseks (Andra Olm)"));
			Assert.assertTrue(parent.contains("Kinnitamiseks automaattest"));
			
		
			Thread.sleep(3000);
			
			
	}
	
	@Test (dependsOnMethods={"create", "asjatoimikuLogi", "asjatoimikuWorkflowLogi", "asjatoimikuDocWorkflowLogi"})
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
