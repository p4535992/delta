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

public class CreateGroup {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void create () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm2")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("sm1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Loo grupp")).click();
	Thread.sleep(3000);
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
	
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:create-metadata:prop_abx003a_groupName:abx003a_groupName")).sendKeys("Selenium grupp");
	driver.findElement(By.id("dialog:dialog-body:create-metadata:prop_abx003a_taskCapable:abx003a_taskCapable")).click();
	driver.findElement(By.id("dialog:dialog-body:create-metadata:prop_abx003a_manageableForAdmin:abx003a_manageableForAdmin")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str1 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str1, "Andmed salvestatud");
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"create"})
	public void addToGroup ()  throws Exception {
	driver.findElement(By.id("sm0")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa organisatsioon")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgName:abx003a_orgName")).sendKeys("Aasta Halvemad Asutused");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgAcronym:abx003a_orgAcronym")).sendKeys("AHA");
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_orgCode:abx003a_orgCode")).sendKeys("halvemad12345");
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:search-text")).sendKeys("Aasta Halvemad Asutused");
	driver.findElement(By.id("dialog:dialog-body:search-btn")).click();
	Thread.sleep(3000);
	
	driver.findElement(By.linkText("Aasta Halvemad Asutused (AHA)")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa kontakt kontaktgruppi")).click();
	Util.modalLisaKasutaja(driver, "", "Selenium grupp");
	Thread.sleep(3000);
	String str = driver.findElement(By.className("error-message")).getText(); 
	Assert.assertEquals(str, "Kontaktgrupil on märgitud tööülesannete saatmine võimalikuks, kuid organisatsioonil mitte ja seda ei saa määrata, sest organisatsioonil puudub e-posti aadress");
	Thread.sleep(3000);
	driver.findElement(By.linkText("Muuda organisatsiooni andmeid")).click();
	driver.findElement(By.id("dialog:dialog-body:node-props:prop_abx003a_email:abx003a_email")).sendKeys("  services@aha.com  ");
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa kontakt kontaktgruppi")).click();
	Util.modalLisaKasutaja(driver, "", "Selenium grupp");
	Thread.sleep(3000);
	String str4 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str4, "Kontakt kontaktgruppi lisatud");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:cancel-button")).click();
	chooseIkon("Aasta Halvemad Asutused (AHA)", 1);
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str3 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str3, "Organisatsioon kustutatud");
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
	Thread.sleep(3000);
	}

	private void chooseIkon(String linkText, int arg) {
		WebElement isik = driver.findElement(By.linkText(linkText));
		WebElement parent1 = isik.findElement(By.xpath("..")); //get parent
		WebElement parent3 = parent1.findElement(By.xpath("..")); //get parent
		List <WebElement> buttons2 = parent3.findElements(By.className("icon-link"));
		WebElement query_enquirymode2 = buttons2.get(arg);
		query_enquirymode2.click();
	}
	
	
	@Test (dependsOnMethods={"create", "addToGroup"})
	public void userMakeAddToGroup () throws Exception  {
	driver.get("https://delta.webmedia.int/5.1");
    WebElement username = driver.findElement(By.id("username"));
    username.sendKeys("andra.olm");
    	
    WebElement password = driver.findElement(By.id("password"));
    password.sendKeys("Qwerty7");
    	
    driver.findElement(By.name("submit")).click();
    Thread.sleep(3000);
	driver.findElement(By.id("pm2")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("sm1")).click();
	Thread.sleep(3000);
	chooseIkon("Selenium grupp", 1);
	Thread.sleep(3000);
	Util.modalLisaKasutaja(driver, "AAAsta OÜ", "AAAsta OÜ (organisatsioon, anastassia.soikonen@nortal.com)");
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Selenium grupp")).click();
	Thread.sleep(3000);
	chooseIkon("AAAsta OÜ", 0);
	Thread.sleep(3000);
	String str = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str, "Kontakt kontaktgrupist eemaldatud");
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='logout']")).click(); 
	Thread.sleep(3000);
	
	}
	
	@Test (dependsOnMethods={"create", "addToGroup", "userMakeAddToGroup" })
	public void deleteGroup () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm2")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("sm1")).click();
	Thread.sleep(3000);
	chooseIkon("Selenium grupp", 2);
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	String str2 = driver.findElement(By.className("info-message")).getText(); 
	Assert.assertEquals(str2, "Kontaktgrupp kustutatud");
	Thread.sleep(3000);
	}
	
	@AfterTest
	public void tearDown(){
	driver.close();
	driver.quit();
	}
	
}
