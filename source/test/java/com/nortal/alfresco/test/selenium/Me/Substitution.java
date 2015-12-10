package com.nortal.alfresco.test.selenium.Me;

import java.util.Calendar;
import java.util.List;
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

public class Substitution {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void addSubstitute () throws Exception  {
		 driver.get("https://delta.webmedia.int/5.1");
		   
	    	WebElement username = driver.findElement(By.id("username"));
	    	username.sendKeys("ebe.sarapuu");
	    	
	    	WebElement password = driver.findElement(By.id("password"));
	    	password.sendKeys("Qwerty7");
	    	
	    	driver.findElement(By.name("submit")).click();
	    	Thread.sleep(3000);
			driver.findElement(By.id("pm3")).click();
			Thread.sleep(3000);
			driver.findElement(By.id("sm2_1")).click();
			Thread.sleep(3000);
			driver.findElement(By.linkText("Lisa asendaja")).click();Thread.sleep(3000);
			Thread.sleep(3000);
			driver.findElement(By.xpath("//*[@id='dialog:dialog-body:substituteList:0:substituteSearch_container']/table/tbody/tr/td[2]/a")).click();
			Thread.sleep(3000);
			Util.modalLisaKasutaja(driver, "Andra Olm", "Andra Olm (Webmedia, RIK)");
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
