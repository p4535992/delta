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

public class AddOrganisation {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void organisation () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm2")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa organisatsioon")).click();
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
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgName:abx003a_orgName")).sendKeys("Aasta Parimad Asutused");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgAltName:abx003a_orgAltName")).sendKeys("Best Year Institutions");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgAcronym:abx003a_orgAcronym")).sendKeys("APA");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgCode:abx003a_orgCode")).sendKeys("parimad1234");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_email:abx003a_email")).sendKeys("  services@apa.com  ");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_country:abx003a_country")).sendKeys("&&&&");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_taskCapable:abx003a_taskCapable")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str, "Andmed salvestatud");
	Thread.sleep(3000);
	driver.findElement(By.linkText("Aasta Parimad Asutused (APA)")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa organisatsiooni kontaktisik")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_personFirstName:abx003a_personFirstName")).sendKeys("Anastassia");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_personLastName:abx003a_personLastName")).sendKeys("Soikonen");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_email:abx003a_email")).sendKeys("anastassia.soikonen@nortal.com");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	WebElement dialog = driver.findElement(By.className("recordSetRow"));
	String parent2 = dialog.findElement(By.xpath("../../..")).getText(); //get parent
	Assert.assertTrue(parent2.contains("Anastassia Soikonen ()"));
	
	Thread.sleep(3000);
	WebElement contact = driver.findElement(By.linkText("Anastassia Soikonen ()"));
	WebElement parent = contact.findElement(By.xpath("..")); //get parent
	WebElement parent11 = parent.findElement(By.xpath("..")); //get parent
	List <WebElement> buttons1 = parent11.findElements(By.className("icon-link"));
	WebElement query_enquirymode1 = buttons1.get(1);
	query_enquirymode1.click();

	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str2 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str2, "Organisatsiooni kontaktisik kustutatud");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:cancel-button")).click();
	
	Thread.sleep(3000);
	WebElement func = driver.findElement(By.linkText("Aasta Parimad Asutused (APA)"));
	WebElement parent1 = func.findElement(By.xpath("..")); //get parent
	WebElement parent3 = parent1.findElement(By.xpath("..")); //get parent
	List <WebElement> buttons = parent3.findElements(By.className("icon-link"));
	WebElement query_enquirymode = buttons.get(1);
	query_enquirymode.click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str3 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str3, "Organisatsioon kustutatud");
	
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
