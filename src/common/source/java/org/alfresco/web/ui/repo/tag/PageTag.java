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
package org.alfresco.web.ui.repo.tag;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.coci.CCProperties;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A non-JSF tag library that adds the HTML begin and end tags if running in servlet mode
 * 
 * @author gavinc
 */
public class PageTag extends TagSupport
{
   private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PageTag.class);
   private static final long serialVersionUID = 8142765393181557228L;
   
   private final static String SCRIPTS_START = "<script type=\"text/javascript\" src=\"";
   private final static String SCRIPTS_END   = "\"></script>\n";
   private final static String STYLES_START  = "<link rel=\"stylesheet\" href=\"";
   private final static String STYLES_MAIN   = "\" type=\"text/css\">\n";
   private final static String IE6COND_START = "<!--[if IE 6]>\n";
   private final static String IE6COND_END   = "<![endif]-->\n";
   private final static String IE7COND_START = "<!--[if IE 7]>\n";
   private final static String IE7COND_END   = "<![endif]-->\n";
   
   private final static String[] SCRIPTS;
   static {
       List<String> scriptsList = getScripts();
       SCRIPTS = scriptsList.toArray(new String[scriptsList.size()]);
   }

    private static List<String> getScripts() {
        List<String> scriptsList = Arrays.asList(
                        // jQuery
                "/scripts/jquery/jquery" + (log.isDebugEnabled() ? "" : "-min") + ".js",
                "/scripts/jquery/jquery.scrollTo" + (log.isDebugEnabled() ? "" : "-min") + ".js",
                                // jQuery UI
                "/scripts/jquery/ui.core.js",
                "/scripts/jquery/ui.datepicker.js",
                                // jQuery Plugins
                "/scripts/jquery/jquery.textarea-expander.js",
                "/scripts/jquery/jquery.tooltip.js",
                "/scripts/jquery/jquery.hoverIntent.js",
                "/scripts/jquery/jquery.textmetrix.js",
                "/scripts/jquery/jquery.autocomplete.js",
                "/scripts/jquery/jquery.jLog-min.js",
                "/scripts/jquery/jquery.condense.js",
                "/scripts/jquery/jquery.ajaxqueue.js",
                                // menu javascript
                "/scripts/menu.js",
                                // webdav javascript
                "/scripts/webdav.js",
                                // base yahoo file
                "/scripts/ajax/yahoo/yahoo/yahoo-min.js",
                                // io handling (AJAX)
                "/scripts/ajax/yahoo/connection/connection-min.js",
                                // event handling
                "/scripts/ajax/yahoo/event/event-min.js",
                                // mootools
                "/scripts/ajax/mootools.v1.11.js",
                                // common Alfresco util methods
                "/scripts/ajax/common.js",
                                // pop-up panel helper objects
                "/scripts/ajax/summary-info.js",
                                // ajax pickers
                "/scripts/ajax/picker.js",
                "/scripts/ajax/tagger.js",
                "/scripts/json-2_2_min.js",
                "/scripts/rsh"+(!log.isDebugEnabled() ? "_compressed" : "") + ".js",
                "/scripts/validation.js",
                "/scripts/scripts.js"
                );
        if (log.isDebugEnabled()) {
            // construct new List, that supports adding
            /*
             * uncomment, if need to use firebug-lite (with non-firefox browser)
            scriptsList = new java.util.ArrayList<String>(scriptsList);
            scriptsList.add("/scripts/firebug-lite.js");
             */
        }
        return scriptsList;
    }
    
   private final static String[] CSS = 
   {
      "/css/main.css",
      "/css/picker.css",
      "/scripts/jquery/jquery.autocomplete.css",
      "/css/styles.css"
   };

   private final static String[] IE6COND_CSS = 
   {
      "/css/ie6.css"
   };
   
   private final static String[] IE7COND_CSS = 
   {
      "/css/ie7.css"
   };

/**
 * Please ensure you understand the terms of the license before changing the contents of this file.
 */
   
   private final static Log logger = LogFactory.getLog(PageTag.class);
   
   private long startTime = 0;
   private String title;
   private String titleId;
   private String doctypeRootElement;
   private String doctypePublic;
   private String doctypeSystem;
   
   /**
    * @return The title for the page
    */
   public String getTitle()
   {
      return title;
   }

   /**
    * @param title Sets the page title
    */
   public void setTitle(String title)
   {
      this.title = title;
   }
   
   /**
    * @return The title message Id for the page
    */
   public String getTitleId()
   {
      return titleId;
   }

   /**
    * @param titleId Sets the page title message Id
    */
   public void setTitleId(String titleId)
   {
      this.titleId = titleId;
   }

   public String getDoctypeRootElement()
   {
      return this.doctypeRootElement;
   }

   public void setDoctypeRootElement(final String doctypeRootElement)
   {
      this.doctypeRootElement = doctypeRootElement;
   }
   
   public String getDoctypePublic()
   {
      return this.doctypePublic;
   }

   public void setDoctypePublic(final String doctypePublic)
   {
      this.doctypePublic = doctypePublic;
   }

   public String getDoctypeSystem()
   {
      return this.doctypeSystem;
   }
   
   public void setDoctypeSystem(final String doctypeSystem)
   {
      this.doctypeSystem = doctypeSystem;
   }
   
   public void release()
   {
      super.release();
      this.title = null;
      this.titleId = null;
      this.doctypeRootElement = null;
      this.doctypeSystem = null;
      this.doctypePublic = null;
   }

   /**
    * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
    */
   public int doStartTag() throws JspException
   {
      if (logger.isDebugEnabled())
         startTime = System.currentTimeMillis();
      
      try
      {
         String reqPath = ((HttpServletRequest)pageContext.getRequest()).getContextPath();
         Writer out = pageContext.getOut();
         
         if (!Application.inPortalServer())
         {
            if (this.getDoctypeRootElement() != null &&
                this.getDoctypePublic() != null)
            {
               out.write("<!DOCTYPE ");
               out.write(this.getDoctypeRootElement().toLowerCase());
               out.write(" PUBLIC \"" + this.getDoctypePublic() + "\"");
               if (this.getDoctypeSystem() != null)
               {
                  out.write(" \"" + this.getDoctypeSystem() + "\"");
               }
               out.write(">\n");
            }
            else
            {
               out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
               out.write("    \"http://www.w3.org/TR/html4/loose.dtd\">\n");
            }
            out.write("<html><head><title>");
            if (this.titleId != null && this.titleId.length() != 0)
            {
               out.write(Utils.encode(Application.getMessage(pageContext.getSession(), this.titleId)));
            }
            else if (this.title != null && this.title.length() != 0)
            {
               out.write(Utils.encode(this.title));
            }
            else
            {
               out.write("Alfresco Web Client");
            }
            out.write("</title>\n");
            out.write("<link rel=\"search\" type=\"application/opensearchdescription+xml\" href=\"" + reqPath + 
                      "/wcservice/api/search/keyword/description.xml\" title=\"Alfresco Keyword Search\">\n");
            out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
         }
         
         // CSS style includes
         for (final String css : PageTag.CSS)
         {
            out.write(STYLES_START);
            out.write(reqPath);
            out.write(css);
            out.write(STYLES_MAIN);
         }
         
         // IE7COND_CSS style includes
         out.write(IE7COND_START);
         for (final String ie7cond_css : PageTag.IE7COND_CSS)
         {
            out.write(STYLES_START);
            out.write(reqPath);
            out.write(ie7cond_css);
            out.write(STYLES_MAIN);
         }
         out.write(IE7COND_END);
         
         // IE6COND_CSS style includes
         out.write(IE6COND_START);
         for (final String ie6cond_css : PageTag.IE6COND_CSS)
         {
            out.write(STYLES_START);
            out.write(reqPath);
            out.write(ie6cond_css);
            out.write(STYLES_MAIN);
         }
         out.write(IE6COND_END);

         // JavaScript includes
         for (final String s : PageTag.SCRIPTS)
         {
            out.write(SCRIPTS_START);
            out.write(reqPath);
            out.write(s);
            out.write(SCRIPTS_END);
         }
         
         out.write(IE7COND_START);
         out.write("<script type=\"text/javascript\">");
         out.write("var ie7 = true;");
         out.write("</script>\n");
         out.write(IE7COND_END);
         
         
         out.write("<script type=\"text/javascript\">"); // start - generate naked javascript code

         // set the context path used by some Alfresco script objects
         out.write("setContextPath('");
         out.write(reqPath);
         out.write("');");

         // generate window onload code
         generateWindowOnloadCode(out);

         out.write("</script>\n"); // end - generate naked javascript code

         if (!Application.inPortalServer())
         {
            out.write("</head>");
            out.write("<body>\n");
            out.write("<div id=\"wrapper\">");
         }
      }
      catch (IOException ioe)
      {
         throw new JspException(ioe.toString());
      }
      
      return EVAL_BODY_INCLUDE;
   }

/**
    * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
    */
   public int doEndTag() throws JspException
   {
      try
      {
         if (!Application.inPortalServer())
         {
            pageContext.getOut().write("\n</div></body></html>");
         }
      }
      catch (IOException ioe)
      {
         throw new JspException(ioe.toString());
      }
      
      final int result = super.doEndTag();
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to generate JSF page: " + (endTime - startTime) + "ms");
      }
      return result;
   }
   
   /**
    * This method generate code for setting window.onload reference as
    * we need to open WebDav or CIFS URL in a new window.
    * 
    * Executes via javascript code(function onloadFunc()) in "onload.js" include file.
    * 
    * @return Returns window.onload javascript code
    */
   private static void generateWindowOnloadCode(Writer out)
      throws IOException
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      if (fc != null)
      {
          CCProperties ccProps = (CCProperties)FacesHelper.getManagedBean(fc, "CCProperties");
          if (ccProps.getWebdavUrl() != null || ccProps.getCifsPath() != null)
          {
             out.write("window.onload=onloadFunc(\"");
             if (ccProps.getWebdavUrl() != null)
             {
                out.write(ccProps.getWebdavUrl());
             }
             out.write("\",\"");
             if (ccProps.getCifsPath() != null)
             {
                String val = ccProps.getCifsPath();
                val = Utils.replace(val, "\\", "\\\\");   // encode escape character
                out.write(val);
             }
             out.write("\");");
             
             // reset session bean state
             ccProps.setCifsPath(null);
             ccProps.setWebdavUrl(null);
          }
      }
   }
}
