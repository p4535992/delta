package com.nortal.alfresco.test.selenium.Menu;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class DoLoginLogOut {
	
	
	/* Doesn't work with IE 11. See http://code.google.com/p/selenium/wiki/InternetExplorerDriver
		System.setProperty("webdriver.ie.driver", System.getProperty("user.dir") + "\\iedriver\\IEDriverServer.exe");
		DesiredCapabilities cap = DesiredCapabilities.internetExplorer();
		cap.setCapability("ignoreZoomSetting", true);
		WebDriver dr1;
		dr1 = new InternetExplorerDriver(cap);
		dr1.get("https://nastja-pc.webmedia.int:9443/cas/login");
	*/
	
	WebDriver driver;
	
	
	@BeforeClass
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
	}
	
	@Test	
	public void testLoginLogOut(){
		Util.doLogin(driver);
		driver.findElement(By.xpath("//*[@id='logout']")).click();
	}	
	

	@AfterTest
	public void tearDown(){
	driver.quit();
	}
	
	
}
