/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.faces.context.FacesContext;

import org.alfresco.repo.i18n.MessageService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.jsf.FacesContextUtils;

<<<<<<< HEAD
=======
import ee.webmedia.alfresco.utils.MessageUtil;
>>>>>>> develop-5.1

/**
 * Wrapper around Alfresco Resource Bundle objects. Used to catch and handle missing
 * resource exception to help identify missing I18N strings in client apps.
 * Also used to look for the requested string in a custom resource bundle.
 * 
 * @author Kevin Roast
 */
public final class ResourceBundleWrapper extends ResourceBundle implements Serializable
{
   private static final long serialVersionUID = -3230653664902689948L;
   private static Log logger = LogFactory.getLog(ResourceBundleWrapper.class);
   
   /** List of custom bundle names */
   private static List<String> addedBundleNames = new ArrayList<String>(10);
   
   /** Serializable details of the resource bundles being wrapped */
   private Locale locale;
   private String bundleName;

   /** List of delegate resource bundles */
   transient private List<ResourceBundle> delegates;
   
   /** Message service */
   transient private MessageService messageService;
   
   public static final String BEAN_RESOURCE_MESSAGE_SERVICE = "messageService";      
   public static final String PATH = "app:company_home/app:dictionary/app:webclient_extension";
   
   /**
    * Constructor
    * 
    * @param locale         the locale
    * @param bundleName     the bundle name
    */
   private ResourceBundleWrapper(Locale locale, String bundleName) 
   {
      this.locale = locale;
      this.bundleName = bundleName;
   }
   
   /**
    * Get the message service
    * 
    * @return   MessageService  message service
    */
   private MessageService getMessageService()
   {
       if (this.messageService == null && FacesContext.getCurrentInstance() != null)
       {
           this.messageService = (MessageService)FacesContextUtils.getRequiredWebApplicationContext(
                                       FacesContext.getCurrentInstance()).getBean(BEAN_RESOURCE_MESSAGE_SERVICE);
       }
       return this.messageService;
   }
   
   /** 
    * Get a list of the delegate resource bundles
    * 
    * @return   List<ResourceBundle>    list of delegate resource bundles
    */
   private List<ResourceBundle> getDelegates()
   {
      if (this.delegates == null)
      {
         this.delegates = new ArrayList<ResourceBundle>(ResourceBundleWrapper.addedBundleNames.size() + 2); 
         
         // Check for custom bundle (if any) - first try in the repo otherwise try the classpath
         ResourceBundle customBundle = null;

<<<<<<< HEAD
         // ALAR: Don't search from repo, we don't use it, saves time
=======
         // Don't search from repo, we don't use it, saves time
>>>>>>> develop-5.1
         
         if (customBundle == null)
         {
            // also look up the custom version of the bundle in the extension package
            String customName = determineCustomBundleName(this.bundleName);
            customBundle = getResourceBundle(locale, customName);
         }
         
         // Add the custom bundle (if any) - add first to allow override (eg. in MT/dynamic web client env)
         if (customBundle != null)
         {
            this.delegates.add(customBundle);
         }
         
         // Add the normal bundle
         this.delegates.add(getResourceBundle(locale, this.bundleName));
                  
         // Add the added bundles
         for (String addedBundleName : ResourceBundleWrapper.addedBundleNames)
         {
            this.delegates.add(getResourceBundle(locale, addedBundleName));
         }
      }
      return this.delegates;
   }
   
   /**
    * Given a local and name, gets the resource bundle
    * 
    * @param locale             locale
    * @param bundleName         bundle name
    * @return ResourceBundle    resource bundle
    */
   private ResourceBundle getResourceBundle(Locale locale, String bundleName)
   {
       ResourceBundle bundle = null;
       try
       {
           // Load the bundle
           bundle = ResourceBundle.getBundle(bundleName, locale);
           this.delegates.add(bundle);

           if (logger.isDebugEnabled())
           {
              logger.debug("Located and loaded bundle " + bundleName);
           }
       }
       catch (MissingResourceException mre)
       {
          // ignore the error, just log some debug info
          if (logger.isDebugEnabled())
          {
              logger.debug("Unable to load bundle " + bundleName);
          }
       }
       return bundle;
   }
   
   /**
    * @see java.util.ResourceBundle#getKeys()
    */
   public Enumeration<String> getKeys()
   {
      if (getDelegates().size() == 1)
      {
         return getDelegates().get(0).getKeys();
      }
      else
      {
         Vector<String> allKeys = new Vector<String>(100, 2); 
         for (ResourceBundle delegate : getDelegates()) 
         {
             Enumeration<String> keys = delegate.getKeys();
             while(keys.hasMoreElements() == true)
             {
                 allKeys.add(keys.nextElement());
             }
         }           
         
         return allKeys.elements();
      }
   }

   /**
    * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
    */
   protected Object handleGetObject(String key)
   {
      Object result = null;
      
      for (ResourceBundle delegate : getDelegates()) 
      {
         try
         {
            // Try and lookup the key from the resource bundle 
            result = delegate.getObject(key);
            if (result != null)
            {
                break;
            }
         }
         catch (MissingResourceException mre)
         {
            // ignore as this means the key was not present  
         }
      }
   
      // if the key was not found return a default string 
      if (result == null)
      {
<<<<<<< HEAD
         if (logger.isWarnEnabled() == true)
=======
         boolean hasI18nTranslation = MessageUtil.hasI18nTranslation(key);
         if (logger.isWarnEnabled() == true && !hasI18nTranslation)
>>>>>>> develop-5.1
         {
            logger.warn("Failed to find I18N message string key: " + key);
         }
      
         result = "$$" + key + "$$";
      }
      
      return result;
   }
   
   /**
    * Factory method to get a named wrapped resource bundle for a particular locale.
    * 
    * @param servletContext     ServletContext
    * @param name               Bundle name
    * @param locale             Locale to retrieve bundle for
    * 
    * @return Wrapped ResourceBundle instance for specified locale
    */
   public static ResourceBundle getResourceBundle(String name, Locale locale)
   {
       return new ResourceBundleWrapper(locale, name);
   }
   
   /**
    * Adds a resource bundle to the collection of custom bundles available
    * 
    * @param name   the name of the resource bundle
    */
   public static void addResourceBundle(String name)
   {
       ResourceBundleWrapper.addedBundleNames.add(name);
   }
   
   /**
    * Determines the name for the custom bundle to lookup based on the given
    * bundle name
    * 
    * @param bundleName The standard bundle
    * @return The name of the custom bundle (in the alfresco.extension package)
    */
   protected static String determineCustomBundleName(String bundleName)
   {
      String extensionPackage = "alfresco.extension.";
      String customBundleName = null;
      
      int idx = bundleName.lastIndexOf(".");
      if (idx == -1)
      {
         customBundleName = extensionPackage + bundleName;
      }
      else
      {
         customBundleName = extensionPackage + 
               bundleName.substring(idx+1, bundleName.length());
      }
      
      return customBundleName;
   }

   @Override
   public Locale getLocale() {
      return locale;
   }

}
