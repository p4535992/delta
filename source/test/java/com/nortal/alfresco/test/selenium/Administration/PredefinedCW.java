package com.nortal.alfresco.test.selenium.Administration;

import java.awt.Robot;
import java.awt.event.KeyEvent;
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

public class PredefinedCW {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void predefineCW () throws Exception  {
			Util.doLogin(driver);
			driver.findElement(By.id("pm5")).click();
			Thread.sleep(2000);
			driver.findElement(By.id("sm5")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Lisa uus terviktöövoog")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:compound:prop_wfcx003a_name:wfcx003a_name")).sendKeys("Selenium terviktöövoog");
			Thread.sleep(3000);
			Util.returnSelectedValueIndex("dialog:dialog-body:compound:prop_wfcx003a_type:wfcx003a_type", driver, 2);
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes_container']/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes']/tbody/tr[1]/td/button")).click();
			Thread.sleep(3000);
			Robot r = new Robot();
			r.keyPress(KeyEvent.VK_CONTROL);
			Thread.sleep(5000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes_results']/option[1]")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes_results']/option[2]")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes_results']/option[3]")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes_results']/option[4]")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes_results']/option[5]")).click();			
			r.keyRelease(KeyEvent.VK_CONTROL);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound:prop_wfcx003a_documentTypes:wfcx003a_documentTypes:picker_wfcx003a_documentTypes']/tbody/tr[6]/td/input")).click();
			Thread.sleep(3000);
			
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[1]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound-workflow-panel-group:action-group-1']/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:compound-workflow-panel-group:action-add-1']/li[2]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[3]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[4]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[5]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[6]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[7]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[8]/span/a")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='compound-workflow-panel']/div/span/a")).click();
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:action-add-0']/li[9]/span/a")).click();
			Thread.sleep(3000);
			Util.returnSelectedValueIndex("dialog:dialog-body:compound-workflow-panel-group:workflow-4:prop_wfcx003a_parallelTasks:wfcx003a_parallelTasks", driver, 1);
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000);
			Util.returnSelectedValueText("dialog:dialog-body:compoundWorkflowsList:selPageSize", driver, "100");
			Thread.sleep(3000);
			driver.findElement(By.linkText("Selenium terviktöövoog")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='action-remove-link-1']")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='action-remove-link-1']")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='action-remove-link-1']")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='action-remove-link-1']")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='action-remove-link-1']")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='action-remove-link-1']")).click();
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:finish-button']")).click();
			Thread.sleep(3000);
			WebElement func = driver.findElement(By.linkText("Selenium terviktöövoog"));
			WebElement parent1 = func.findElement(By.xpath("..")); //get parent
			WebElement parent2 = parent1.findElement(By.xpath("..")); //get parent
			WebElement button = parent2.findElement(By.className("icon-link"));
			button.click();
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:finish-button")).click();
			Thread.sleep(5000);

			
			String str = driver.findElement(By.className("info-message")).getText(); 
			Assert.assertEquals(str, "Terviktöövoog kustutatud");
			
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
	
}
