package com.nortal.alfresco.test.selenium.Util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class Util {

	
	
	
	public static void doLogin(WebDriver driver)
    {
		
        driver.get("https://delta.webmedia.int/5.1");
    	//or WebElement username = driver.findElement(By.xpath("//*[@id='username']"));
    	WebElement username = driver.findElement(By.id("username"));
    	username.sendKeys("anastassia.soikonen");
    	
    	WebElement password = driver.findElement(By.id("password"));
    	password.sendKeys("Qwerty7");
    	
    	driver.findElement(By.name("submit")).click();
    	
    }
	
	public static void returnSelectedValueIndex (String id, WebDriver driver, int arg) throws InterruptedException {
		
		Select alus = new Select(driver.findElement(By.id(id)));
		alus.selectByIndex(arg);
		Thread.sleep(3000);
	}
	
	public static void returnSelectedValueText (String id, WebDriver driver, String arg) throws InterruptedException {
		
		Select alus = new Select(driver.findElement(By.id(id)));
		alus.selectByVisibleText(arg);
		Thread.sleep(3000);
	}
	
	public static void searchByXpathreturnSelectedValueText (String xpath, WebDriver driver, String arg) throws InterruptedException {
		
		Select alus = new Select(driver.findElement(By.xpath(xpath)));
		alus.selectByVisibleText(arg);
		Thread.sleep(3000);
	}

	public static boolean existsElement(WebDriver driver, String text) {
	    try {
	        driver.findElement(By.partialLinkText(text));
	    } catch (NoSuchElementException e) {
	        return false;
	    }
	    return true;
	}

	
	public static void modalLisaKasutaja(WebDriver driver, String name, String selectName) throws InterruptedException {
		WebElement dialog = driver.switchTo().activeElement();
		dialog.sendKeys(name);
		String activeId = dialog.getAttribute("name");
		
		WebElement parent = dialog.findElement(By.xpath("..")); //get parent
		WebElement button = parent.findElement(By.className("specificAction"));
		button.click();
		Thread.sleep(3000); 
		
		String modalId = activeId.substring(0, activeId.lastIndexOf("_"));
		String resultsBoxId = modalId + "_results";
		WebElement select = driver.findElement(By.id(resultsBoxId));
		Select dropDown = new Select(select);
		dropDown.selectByVisibleText(selectName);
		
		Thread.sleep(3000); 
		
		driver.findElement(By.id(modalId)).findElement(By.className("picker-add")).click();
		
		Thread.sleep(6000);
	}

	
}
