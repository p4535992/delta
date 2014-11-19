<<<<<<< HEAD
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
package org.alfresco.web.ui.repo.component;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.search.SearchContext;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.WebResources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Kevin Roast
 */
public class UISimpleSearch extends UICommand
{
   // ------------------------------------------------------------------------------
   // Component implementation

   /**
    * Default Constructor
    */
   public UISimpleSearch()
   {
      // specifically set the renderer type to null to indicate to the framework
      // that this component renders itself - there is no abstract renderer class
      setRendererType(null);
   }
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.SimpleSearch";
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.search = (SearchContext)values[1];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[2];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.search;
      return (values);
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = Utils.getActionHiddenFieldName(context, this);
      String value = (String)requestMap.get(fieldId);
      // we are clicked if the hidden field contained our client id
      if (value != null)
      {
         if (value.equals(this.getClientId(context)))
         {
            String searchText = (String)requestMap.get(getClientId(context));
            
            if (searchText.length() != 0)
            {
               if (logger.isDebugEnabled())
                  logger.debug("Search text submitted: " + searchText);
               int option = -1;
               String optionFieldName = getClientId(context) + NamingContainer.SEPARATOR_CHAR + OPTION_PARAM;
               String optionStr = (String)requestMap.get(optionFieldName);
               if (optionStr.length() != 0)
               {
                  option = Integer.parseInt(optionStr);
               }
               if (logger.isDebugEnabled())
                  logger.debug("Search option submitted: " + option);
               
               // queue event so system can perform a search and update the component
               SearchEvent event = new SearchEvent(this, searchText, option);
               this.queueEvent(event);
            }
         }
         else if (value.equals(ADVSEARCH_PARAM))
         {
            // found advanced search navigation action
            // TODO: TEMP: set this outcome from a component attribute!
            AdvancedSearchEvent event = new AdvancedSearchEvent(this, "advSearch");
            this.queueEvent(event);
         }
      }
   }
   
   /**
    * @see javax.faces.component.UICommand#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      FacesContext fc = getFacesContext();
      if (event instanceof SearchEvent)
      {
         // update the component parameters from the search event details
         SearchEvent searchEvent = (SearchEvent)event;
         
         // construct the Search Context object
         SearchContext context = new SearchContext();
         context.setText(searchEvent.SearchText);
         context.setMode(searchEvent.SearchMode);
         context.setForceAndTerms(Application.getClientConfig(fc).getForceAndTerms());
         context.setSimpleSearchAdditionalAttributes(Application.getClientConfig(fc).getSimpleSearchAdditionalAttributes());
         this.search = context;
         
         super.broadcast(event);
      }
      else if (event instanceof AdvancedSearchEvent)
      {
         // special case to navigate to the advanced search screen
         AdvancedSearchEvent searchEvent = (AdvancedSearchEvent)event;
         fc.getApplication().getNavigationHandler().handleNavigation(fc, null, searchEvent.Outcome);
         
         // NOTE: we don't call super() here so that our nav outcome is the one that occurs!
      }
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      ResourceBundle bundle = (ResourceBundle)Application.getBundle(context);
      
      // script for dynamic simple search menu drop-down options
      out.write("<script type='text/javascript'>");
      out.write("function _noenter(event) {" +
                "if (event && event.keyCode == 13) {" +
                "   _searchSubmit();return false; }" +
                "else {" +
                "   return true; } }");
      out.write("function _searchSubmit() {");
      out.write(Utils.generateFormSubmit(context, this, Utils.getActionHiddenFieldName(context, this), getClientId(context)));
      out.write("}");
      out.write("</script>");
      
      String searchImage = Utils.buildImageTag(context, WebResources.IMAGE_SEARCH_ICON, 15, 15,
            bundle.getString(MSG_SEARCH), "_searchSubmit();");
      
      out.write(Utils.buildImageTag(context, WebResources.IMAGE_SEARCH_CONTROLS, 34, 21,
            bundle.getString(MSG_OPTIONS), "javascript:_toggleMenu(event, 'alfsearch_menu');", "-1px", ""));
      
      // dynamic DIV area containing search options
      out.write("<div id='alfsearch_menu' style='display: none;'>");
      out.write("<ul>");
      
      // output each option - setting the current one to CHECKED
      String optionFieldName = getClientId(context) + NamingContainer.SEPARATOR_CHAR + OPTION_PARAM;
      String radioOption = "<li class='userInputForm'><input type='radio' name='" + optionFieldName + "'";
      int searchMode = getSearchMode();
      out.write(radioOption);
      out.write(" value='4'");
      if (searchMode == 4) out.write(" checked='checked'");
      out.write("/>" + "Metaandmetest" + "</li>");
      out.write(radioOption);
      out.write(" value='0'");
      if (searchMode == 0) out.write(" checked='checked'");
      out.write("/>" + "Metaandmetest ja faili sisust" + "</li>");
      out.write("</ul>");
      
      // row with table containing advanced search link and Search Go button 
      // generate a link that will cause an action event to navigate to the advanced search screen
      out.write("<div>");
      out.write("<a class='small' href='#' onclick=\"");
      out.write(Utils.generateFormSubmit(context, this, Utils.getActionHiddenFieldName(context, this), ADVSEARCH_PARAM));
      out.write("\">");
      out.write(bundle.getString(MSG_ADVANCED_SEARCH));
      out.write("</a>");
      out.write(searchImage);
      out.write("</div></div>");
      
      // input text box
      out.write("<input name='");
      out.write(getClientId(context));
      // TODO: style and class from component properties!
      out.write("' onkeypress=\"return _noenter(event)\"");
      out.write(" type='text' maxlength='1024' value=\"");
      // output previous search text stored in this component!
      out.write(Utils.replace(getLastSearch(), "\"", "&quot;"));
      out.write("\">");
      
      // search Go image button
      //out.write(searchImage);
      out.write("<input type='submit' value='");
      out.write(bundle.getString(MSG_SEARCH));
      out.write("' onclick='_searchSubmit();' />");
   }
   
   /**
    * Return the current Search Context
    */
   public SearchContext getSearchContext()
   {
      return this.search;
   }
   
   /**
    * @return The last set search text value
    */
   public String getLastSearch()
   {
      return this.search != null ? this.search.getText() : "";
   }
   
   /** 
    * @return The current search mode (see constants) 
    */
   public int getSearchMode()
   {
      return this.search != null ? this.search.getMode() : 4;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   private static final Log logger = LogFactory.getLog(UISimpleSearch.class);
   
   /** I18N message Ids */
   private static final String MSG_ADVANCED_SEARCH = "advanced_search";
   private static final String MSG_OPTIONS = "options";
   private static final String MSG_SEARCH = "search";
   private static final String MSG_SPACE_NAMES_ONLY = "space_names";
   private static final String MSG_FILE_NAMES_ONLY = "file_names";
   private static final String MSG_FILE_NAMES_CONTENTS = "file_names_contents";
   private static final String MSG_ALL_ITEMS = "all_items";
   
   private static final String OPTION_PARAM = "_option";
   private static final String ADVSEARCH_PARAM = "_advsearch";
   
   /** last search context used */
   private SearchContext search = null;
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing a search execution from the UISimpleSearch component.
    */
   public static class SearchEvent extends ActionEvent
   {
      private static final long serialVersionUID = 3918135612344774322L;

      public SearchEvent(UIComponent component, String text, int mode)
      {
         super(component);
         SearchText = text;
         SearchMode = mode;
      }
      
      public String SearchText;
      public int SearchMode;
   }
   
   public static class AdvancedSearchEvent extends ActionEvent
   {
      public AdvancedSearchEvent(UIComponent component, String outcome)
      {
         super(component);
         Outcome = outcome;
      }
      
      public String Outcome;
   }
}
=======
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
package org.alfresco.web.ui.repo.component;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.search.SearchContext;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.WebResources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Kevin Roast
 */
public class UISimpleSearch extends UICommand
{
   // ------------------------------------------------------------------------------
   // Component implementation

   /**
    * Default Constructor
    */
   public UISimpleSearch()
   {
      // specifically set the renderer type to null to indicate to the framework
      // that this component renders itself - there is no abstract renderer class
      setRendererType(null);
   }
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.SimpleSearch";
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.search = (SearchContext)values[1];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[2];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.search;
      return (values);
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = Utils.getActionHiddenFieldName(context, this);
      String value = (String)requestMap.get(fieldId);
      // we are clicked if the hidden field contained our client id
      if (value != null)
      {
         if (value.equals(this.getClientId(context)))
         {
            String searchText = (String)requestMap.get(getClientId(context));
            
            if (searchText.length() != 0)
            {
               if (logger.isDebugEnabled())
                  logger.debug("Search text submitted: " + searchText);
               int option = -1;
               String optionFieldName = getClientId(context) + NamingContainer.SEPARATOR_CHAR + OPTION_PARAM;
               String optionStr = (String)requestMap.get(optionFieldName);
               if (optionStr.length() != 0)
               {
                  option = Integer.parseInt(optionStr);
               }
               if (logger.isDebugEnabled())
                  logger.debug("Search option submitted: " + option);
               
               // queue event so system can perform a search and update the component
               SearchEvent event = new SearchEvent(this, searchText, option);
               this.queueEvent(event);
            }
         }
         else if (value.equals(ADVSEARCH_PARAM))
         {
            // found advanced search navigation action
            // TODO: TEMP: set this outcome from a component attribute!
            AdvancedSearchEvent event = new AdvancedSearchEvent(this, "advSearch");
            this.queueEvent(event);
         }
      }
   }
   
   /**
    * @see javax.faces.component.UICommand#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      FacesContext fc = getFacesContext();
      if (event instanceof SearchEvent)
      {
         // update the component parameters from the search event details
         SearchEvent searchEvent = (SearchEvent)event;
         
         // construct the Search Context object
         SearchContext context = new SearchContext();
         context.setText(searchEvent.SearchText);
         context.setMode(searchEvent.SearchMode);
         context.setForceAndTerms(Application.getClientConfig(fc).getForceAndTerms());
         context.setSimpleSearchAdditionalAttributes(Application.getClientConfig(fc).getSimpleSearchAdditionalAttributes());
         this.search = context;
         
         super.broadcast(event);
      }
      else if (event instanceof AdvancedSearchEvent)
      {
         // special case to navigate to the advanced search screen
         AdvancedSearchEvent searchEvent = (AdvancedSearchEvent)event;
         fc.getApplication().getNavigationHandler().handleNavigation(fc, null, searchEvent.Outcome);
         
         // NOTE: we don't call super() here so that our nav outcome is the one that occurs!
      }
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      ResourceBundle bundle = (ResourceBundle)Application.getBundle(context);
      
      // script for dynamic simple search menu drop-down options
      out.write("<script type='text/javascript'>");
      out.write("function _noenter(event) {" +
                "if (event && event.keyCode == 13) {" +
                "   _searchSubmit();return false; }" +
                "else {" +
                "   return true; } }");
      out.write("function _searchSubmit() {");
      out.write(Utils.generateFormSubmit(context, this, Utils.getActionHiddenFieldName(context, this), getClientId(context)));
      out.write("}");
      out.write("</script>");
      
      String searchImage = Utils.buildImageTag(context, WebResources.IMAGE_SEARCH_ICON, 15, 15,
            bundle.getString(MSG_SEARCH), "_searchSubmit();");
      
      out.write(Utils.buildImageTag(context, WebResources.IMAGE_SEARCH_CONTROLS, 34, 21,
            bundle.getString(MSG_OPTIONS), "javascript:_toggleMenu(event, 'alfsearch_menu');", "-1px", ""));
      
      // dynamic DIV area containing search options
      out.write("<div id='alfsearch_menu' style='display: none;'>");
      out.write("<ul>");
      
      // output each option - setting the current one to CHECKED
      String optionFieldName = getClientId(context) + NamingContainer.SEPARATOR_CHAR + OPTION_PARAM;
      String radioOption = "<li class='userInputForm'><input type='radio' name='" + optionFieldName + "'";
      int searchMode = getSearchMode();
      out.write(radioOption);
      out.write(" value='4'");
      if (searchMode == 4) out.write(" checked='checked'");
      out.write("/>" + "Metaandmetest" + "</li>");
      out.write(radioOption);
      out.write(" value='0'");
      if (searchMode == 0) out.write(" checked='checked'");
      out.write("/>" + "Metaandmetest ja faili sisust" + "</li>");
      out.write("</ul>");
      
      // row with table containing advanced search link and Search Go button 
      // generate a link that will cause an action event to navigate to the advanced search screen
      out.write("<div>");
      out.write("<a class='small' href='#' onclick=\"");
      out.write(Utils.generateFormSubmit(context, this, Utils.getActionHiddenFieldName(context, this), ADVSEARCH_PARAM));
      out.write("\">");
      out.write(bundle.getString(MSG_ADVANCED_SEARCH));
      out.write("</a>");
      out.write(searchImage);
      out.write("</div></div>");
      
      // input text box
      out.write("<input name='");
      out.write(getClientId(context));
      // TODO: style and class from component properties!
      out.write("' onkeypress=\"return _noenter(event)\"");
      out.write(" type='text' maxlength='1024' value=\"");
      // output previous search text stored in this component!
      out.write(Utils.replace(getLastSearch(), "\"", "&quot;"));
      out.write("\">");
      
      // search Go image button
      //out.write(searchImage);
      out.write("<input type='submit' value='");
      out.write(bundle.getString(MSG_SEARCH));
      out.write("' onclick='_searchSubmit();' />");
   }
   
   /**
    * Return the current Search Context
    */
   public SearchContext getSearchContext()
   {
      return this.search;
   }
   
   /**
    * @return The last set search text value
    */
   public String getLastSearch()
   {
      return this.search != null ? this.search.getText() : "";
   }
   
   /** 
    * @return The current search mode (see constants) 
    */
   public int getSearchMode()
   {
      return this.search != null ? this.search.getMode() : 4;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   private static final Log logger = LogFactory.getLog(UISimpleSearch.class);
   
   /** I18N message Ids */
   private static final String MSG_ADVANCED_SEARCH = "advanced_search";
   private static final String MSG_OPTIONS = "options";
   private static final String MSG_SEARCH = "search";
   private static final String MSG_SPACE_NAMES_ONLY = "space_names";
   private static final String MSG_FILE_NAMES_ONLY = "file_names";
   private static final String MSG_FILE_NAMES_CONTENTS = "file_names_contents";
   private static final String MSG_ALL_ITEMS = "all_items";
   
   private static final String OPTION_PARAM = "_option";
   private static final String ADVSEARCH_PARAM = "_advsearch";
   
   /** last search context used */
   private SearchContext search = null;
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing a search execution from the UISimpleSearch component.
    */
   public static class SearchEvent extends ActionEvent
   {
      private static final long serialVersionUID = 3918135612344774322L;

      public SearchEvent(UIComponent component, String text, int mode)
      {
         super(component);
         SearchText = text;
         SearchMode = mode;
      }
      
      public String SearchText;
      public int SearchMode;
   }
   
   public static class AdvancedSearchEvent extends ActionEvent
   {
      public AdvancedSearchEvent(UIComponent component, String outcome)
      {
         super(component);
         Outcome = outcome;
      }
      
      public String Outcome;
   }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
