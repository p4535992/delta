package com.nortal.alfresco.test.selenium.Search;

/* ***************************************** */
/* poolik tuleb veel l6puni viia!!!!!!!!!!!*/


import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class TaskSearch {

	
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
			driver.findElement(By.id("pm4")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("sm0_1")).click();
			Thread.sleep(3000);
			Util.returnSelectedValueText("dialog:dialog-body:task-search-filter:prop_tasksearchx003a_startedDateTimeBegin:tasksearchx003a_startedDateTimeBegin_DateRangePicker", driver, "Eelmise kuu algusest");
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:task-search-filter:prop_tasksearchx003a_ownerName:tasksearchx003a_ownerName:tasksearchx003a_ownerName_0")).sendKeys("Anastassia Soikonen");
			Thread.sleep(3000);
			driver.findElement(By.id("dialog:dialog-body:task-search-filter:prop_tasksearchx003a_creatorName:tasksearchx003a_creatorName:picker_tasksearchx003a_creatorNamerow_3")).sendKeys("Anastassia Soikonen");
			Thread.sleep(3000);
			
	}
	
	

    	
	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
	
}
