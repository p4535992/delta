package com.nortal.alfresco.test.selenium.Contact;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class AddEraisik {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void eraisik () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm2")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa eraisik")).click();
	Thread.sleep(5000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	WebDriverWait wait = new WebDriverWait(driver, 2);
	try{
	    wait.until(ExpectedConditions.alertIsPresent());
	    Alert alert = driver.switchTo().alert();
	    alert.accept();
	}
	catch (Exception e){
	    System.out.println("No alert");
	}
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_personFirstName:abx003a_personFirstName")).sendKeys("Klara");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_personLastName:abx003a_personLastName")).sendKeys("Putsep");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_email:abx003a_email")).sendKeys("  klara.p@gmail.com  ");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str, "Andmed salvestatud");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:search-text")).sendKeys("Klara Putsep");
	driver.findElement(By.id("dialog:dialog-body:search-btn")).click();
	Thread.sleep(3000);
	
	WebElement eraisik = driver.findElement(By.linkText("Klara Putsep"));
	WebElement parent2 = eraisik.findElement(By.xpath("..")); //get parent
	WebElement parent4 = parent2.findElement(By.xpath("..")); //get parent
	List <WebElement> buttons1 = parent4.findElements(By.className("icon-link"));
	WebElement query_enquirymode1 = buttons1.get(0);
	query_enquirymode1.click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_mobilePhone:abx003a_mobilePhone")).sendKeys("543544321");
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str2 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str2, "Andmed salvestatud");
	Thread.sleep(3000);
	WebElement isik = driver.findElement(By.linkText("Klara Putsep"));
	WebElement parent1 = isik.findElement(By.xpath("..")); //get parent
	WebElement parent3 = parent1.findElement(By.xpath("..")); //get parent
	List <WebElement> buttons2 = parent3.findElements(By.className("icon-link"));
	WebElement query_enquirymode2 = buttons2.get(1);
	query_enquirymode2.click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str3 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str3, "Eraisik kustutatud");
	Thread.sleep(3000);
	
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
