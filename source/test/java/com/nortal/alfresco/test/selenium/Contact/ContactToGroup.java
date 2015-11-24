package com.nortal.alfresco.test.selenium.Contact;

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

public class ContactToGroup {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	
	@Test 
	public void addToGroup () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm2")).click();
	Thread.sleep(3000);
	
	Thread.sleep(3000);
	driver.findElement(By.linkText("AAS Gjensidige Baltic Eesti filiaal (Kindlustus)")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa kontakt kontaktgruppi")).click();
	Util.modalLisaKasutaja(driver, "", "Organisatsioonide grupp");
	
	driver.findElement(By.linkText("Organisatsioonide grupp")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa kontakt")).click();
	//Util.modalLisaKasutaja(driver, "AAAsta OÜ", "AAAsta OÜ (organisatsioon, anastassia.soikonen@nortal.com)");
	
	driver.findElement(By.id("dialog:finish-button")).click();
	WebElement contact = driver.findElement(By.linkText("AAAsta OÜ"));
	WebElement parent1 = contact.findElement(By.xpath("..")); //get parent
	WebElement parent3 = parent1.findElement(By.xpath("..")); //get parent
	List <WebElement> buttons = parent3.findElements(By.className("icon-link"));
	WebElement query_enquirymode = buttons.get(0);
	query_enquirymode.click();
	Thread.sleep(3000);
	String str2 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str2, "Kontakt kontaktgrupist eemaldatud");
	
	driver.findElement(By.id("dialog:cancel-button")).click();
	WebElement org = driver.findElement(By.linkText("Organisatsioonide grupp"));
	WebElement parent2 = org.findElement(By.xpath("..")); //get parent
	WebElement parent4 = parent2.findElement(By.xpath("..")); //get parent
	List <WebElement> buttons1 = parent4.findElements(By.className("inlineAction"));
	WebElement query_enquirymode1 = buttons1.get(1);
	query_enquirymode1.click();
	
	Thread.sleep(3000);
	String str3 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str3, "Kontakt kontaktgrupist eemaldatud");
	
	driver.findElement(By.linkText("Muuda organisatsiooni andmeid")).click();
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgAcronym:abx003a_orgAcronym")).clear();
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgAcronym:abx003a_orgAcronym")).sendKeys("Kindlustus");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str, "Andmed salvestatud");
	Thread.sleep(3000);
	
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
}
