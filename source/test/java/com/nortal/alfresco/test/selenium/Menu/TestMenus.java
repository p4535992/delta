package com.nortal.alfresco.test.selenium.Menu;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nortal.alfresco.test.selenium.Util.Util;

public class TestMenus {

WebDriver driver;
	
	@BeforeTest
	public void setUp() throws Exception {
    driver = new FirefoxDriver();
	}
	

	@Test //(dependsOnMethods={"testLoginLogOut"}) //- to define execution order
	public void AdminLogin (){
		Util.doLogin(driver);
	}
	
	@Test (dependsOnMethods={"AdminLogin"}) 
	public void CheckMainUpperMenu (){
		  driver.findElement(By.id("pm0"));
		  driver.findElement(By.id("pm1"));
		  driver.findElement(By.id("pm2"));
		  driver.findElement(By.id("pm3"));
		  driver.findElement(By.id("pm4"));
		  driver.findElement(By.id("pm5"));
		  driver.findElement(By.id("pm8"));
		  driver.findElement(By.id("pm9"));
	}
	
	@Test (dependsOnMethods={"AdminLogin"}) 
	public void CheckMainLeftMenu (){
		  driver.findElement(By.id("pm0"));
		  driver.findElement(By.id("pm1"));
		  driver.findElement(By.id("pm2"));
		  driver.findElement(By.id("pm3"));
		  driver.findElement(By.id("pm4"));
		  driver.findElement(By.id("pm5"));
		  driver.findElement(By.id("pm8"));
		  driver.findElement(By.id("pm9"));
		  driver.findElement(By.xpath("//*[@id='shelf']/div/a"));
	}
	
	 @Test (dependsOnMethods={"AdminLogin", "CheckMainUpperMenu", "CheckMainLeftMenu"})
	  public void openMenuDocReg (){
		  driver.findElement(By.id("pm1")).click();
		  driver.findElement(By.id("sm0"));
		  driver.findElement(By.id("sm1"));
		  driver.findElement(By.xpath("//*[@id='shelf']/div/a"));
	  }
	
	 @Test (dependsOnMethods={"AdminLogin", "CheckMainUpperMenu", "CheckMainLeftMenu"})
	  public void openMenuContact (){
		  driver.findElement(By.id("pm2")).click();
		  driver.findElement(By.id("sm0"));
		  driver.findElement(By.id("sm1"));
		  driver.findElement(By.xpath("//*[@id='shelf']/div/a"));
	  }
	 
	 @Test (dependsOnMethods={"AdminLogin", "CheckMainUpperMenu", "CheckMainLeftMenu"})
	  public void openMenuMe (){
		  driver.findElement(By.id("pm3")).click();
		  driver.findElement(By.id("sm0"));
		  driver.findElement(By.id("sm1"));
		  driver.findElement(By.id("sm2"));
		  driver.findElement(By.id("sm2_0"));
		  driver.findElement(By.id("sm2_1"));
		  driver.findElement(By.xpath("//*[@id='shelf']/div/a"));
	  }
	
	 @Test (dependsOnMethods={"AdminLogin", "CheckMainUpperMenu", "CheckMainLeftMenu"})
	  public void openMenuSearch (){
		  driver.findElement(By.id("pm4")).click();
		  driver.findElement(By.id("sm0"));
		  driver.findElement(By.id("sm0_0"));
		  driver.findElement(By.id("sm0_1"));
		  driver.findElement(By.id("sm0_2"));
		  driver.findElement(By.id("sm0_3"));
		  driver.findElement(By.id("sm1"));
		  driver.findElement(By.id("sm1_0"));
		  driver.findElement(By.id("sm1_1"));
		  driver.findElement(By.id("sm1_2"));
		  driver.findElement(By.id("sm2"));
		  driver.findElement(By.xpath("//*[@id='shelf']/div/a"));
	  }
	
	 @Test (dependsOnMethods={"AdminLogin", "CheckMainUpperMenu", "CheckMainLeftMenu"})
	  public void openMenuAdmin () throws InterruptedException{
		  driver.findElement(By.id("pm5")).click();
		  driver.findElement(By.id("sm0"));
		  driver.findElement(By.id("sm1"));
		  driver.findElement(By.id("sm2"));
		  driver.findElement(By.id("sm3"));
		  driver.findElement(By.id("sm4"));
		  driver.findElement(By.id("sm5"));
		  driver.findElement(By.id("sm6"));
		  driver.findElement(By.id("sm7"));
		  driver.findElement(By.id("sm8"));
		  driver.findElement(By.id("sm9"));
		  driver.findElement(By.id("sm10"));
		  driver.findElement(By.id("sm11"));
		  driver.findElement(By.id("sm12"));
		  driver.findElement(By.id("sm13"));
		  driver.findElement(By.id("sm14"));
		  driver.findElement(By.id("sm15"));
		  driver.findElement(By.id("sm16"));
		  driver.findElement(By.id("sm17"));
		  driver.findElement(By.id("sm18"));
		  driver.findElement(By.id("sm19"));
		  driver.findElement(By.id("sm20"));
		  driver.findElement(By.xpath("//*[@id='shelf']/div/a"));
	  }
	 
	 @AfterTest
		public void tearDown(){
		driver.quit();
		}
	

	
}
