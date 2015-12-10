package com.nortal.alfresco.test.selenium.DocRegister;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class NewPath {
	
	


WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	

	@Test 
	public void addFunction () throws Exception  {
	Util.doLogin(driver);
	driver.findElement(By.id("pm1")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("sm0")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa uus funktsioon")).click();
	Thread.sleep(3000);
	Util.returnSelectedValueIndex("dialog:dialog-body:fn-metadata:prop_fnx003a_type:fnx003a_type", driver, 1);
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:fn-metadata:prop_fnx003a_mark:fnx003a_mark")).sendKeys("ATF-1");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:fn-metadata:prop_fnx003a_title:fnx003a_title")).sendKeys("Aasta test funktsioon");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click(); 
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"addFunction"}) 
	public void addSari () throws Exception {
	driver.findElement(By.linkText("Pealkiri")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Pealkiri")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Aasta test funktsioon")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa uus sari")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType_container']/a")).click();
	Thread.sleep(3000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType']/tbody/tr[1]/td/button")).click();
	Thread.sleep(3000);
	Robot r = new Robot();
	r.keyPress(KeyEvent.VK_CONTROL);
	Thread.sleep(5000);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[1]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[2]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[3]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[4]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[5]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[6]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[7]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[8]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[9]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[10]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[11]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[12]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[13]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[14]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[15]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[16]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[17]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[18]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[19]")).click();
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType_results']/option[20]")).click();
	
	r.keyRelease(KeyEvent.VK_CONTROL);
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_docType:serx003a_docType:picker_serx003a_docType']/tbody/tr[6]/td/input")).click();
	Thread.sleep(3000);
	Util.returnSelectedValueIndex("dialog:dialog-body:ser-metatada:prop_serx003a_type:serx003a_type", driver, 1);
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:ser-metatada:prop_serx003a_seriesIdentifier:serx003a_seriesIdentifier")).sendKeys("1");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:ser-metatada:prop_serx003a_title:serx003a_title")).sendKeys("Aasta test sari");
	Thread.sleep(3000); 
	Util.returnSelectedValueText("dialog:dialog-body:ser-metatada:prop_serx003a_register:serx003a_register", driver, "Automaattest");
	Thread.sleep(3000); 
	driver.findElement(By.xpath("//*[@id='dialog:dialog-body:ser-metatada:prop_serx003a_volType:serx003a_volType']/option[3]")).click();
	Thread.sleep(3000);
	Util.searchByXpathreturnSelectedValueText("//*[@id='dialog:dialog-body:ser-metatada:prop_doccomx003a_accessRestriction:doccomx003a_accessRestriction']", driver, "Avalik");
	Thread.sleep(4000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(5000);
	}

	@Test (dependsOnMethods={"addFunction", "addSari"})
	
	public void addToimik () throws Exception {
	driver.findElement(By.partialLinkText("Aasta test sari")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa uus toimik")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:volume-metatada:prop_docdynx003a_title:docdynx003a_title")).clear();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:volume-metatada:prop_docdynx003a_title:docdynx003a_title")).sendKeys("Aasta test toimik");
	Thread.sleep(3000);
	Util.returnSelectedValueText("dialog:dialog-body:volume-metatada:prop_docdynx003a_volumeType:docdynx003a_volumeType", driver, "Teemapõhine toimik");
	driver.findElement(By.id("dialog:dialog-body:volume-metatada:prop_docdynx003a_containsCases:docdynx003a_containsCases")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	
	}
	
	@Test (dependsOnMethods={"addFunction", "addSari", "addToimik"})
	public void addTeema () throws Exception {
	driver.findElement(By.partialLinkText("Aasta test toimik")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Lisa uus teema")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:dialog-body:case-metatada:prop_casex003a_title:casex003a_title")).sendKeys("Aasta test teema");
	Thread.sleep(3000);
	driver.findElement(By.id("dialog:finish-button")).click();
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"addFunction", "addSari", "addToimik", "addTeema"})
	public void closeTeema () throws Exception {
	driver.findElement(By.partialLinkText("Aasta test teema"));
	driver.findElement(By.id("col7-act1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Sulge teema")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("col7-act1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Kustuta")).click();
	Thread.sleep(3000);
	}

	
	@Test (dependsOnMethods={"addFunction", "addSari", "addToimik", "addTeema", "closeTeema"})
	public void deleteToimik () throws Exception {
	driver.findElement(By.id("dialog:cancel-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("col8-act1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Sulge toimik")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Kustuta")).click();
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"addFunction", "addSari", "addToimik", "addTeema", "closeTeema", "deleteToimik"})
	public void deleteSari () throws Exception {
	driver.findElement(By.id("dialog:cancel-button")).click();
	Thread.sleep(3000);
	driver.findElement(By.id("col5-act1")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Sulge sari")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Kustuta")).click();
	Thread.sleep(3000);
	}
	
	@Test (dependsOnMethods={"addFunction", "addSari", "addToimik", "addTeema", "closeTeema", "deleteToimik", "deleteSari"})
	public void deleteFunction () throws Exception {
	/*	com.nortal.alfresco.test.selenium.Util.doLogin(driver);
		driver.findElement(By.id("pm1")).click();
		Thread.sleep(3000);
		driver.findElement(By.id("sm0")).click();
		Thread.sleep(3000);
		driver.findElement(By.linkText("Pealkiri")).click();
		Thread.sleep(3000);
		driver.findElement(By.linkText("Pealkiri")).click();
		Thread.sleep(3000); */
	driver.findElement(By.id("dialog:cancel-button")).click();
	Thread.sleep(3000);
	WebElement func = driver.findElement(By.linkText("Aasta test funktsioon"));
	WebElement parent1 = func.findElement(By.xpath("..")); //get parent
	WebElement parent2 = parent1.findElement(By.xpath("..")); //get parent
	WebElement button = parent2.findElement(By.className("icon-link"));
	button.click();
	
	driver.findElement(By.linkText("Sulge funktsioon")).click();
	Thread.sleep(3000);
	driver.findElement(By.linkText("Kustuta")).click();
	Thread.sleep(3000);
	
	}
	 
	
	
	@AfterTest
	public void tearDown(){
		driver.close();
		driver.quit();
	}
}
