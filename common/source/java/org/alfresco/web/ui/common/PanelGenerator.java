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
package org.alfresco.web.ui.common;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper to generate the rounded panel HTML templates
 * 
 * @author kevinr
 */
public final class PanelGenerator
{
   public static void generatePanel(Writer out, String contextPath, String panel, String inner)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, BGCOLOR_WHITE);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, bgColor, false);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, String panelId)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, bgColor, false, panelId, "");
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, String panelId, Object styleClass)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, bgColor, false, panelId, styleClass);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, boolean dialog)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, dialog, "", "");
      out.write(inner);
      generatePanelEnd(out, contextPath, panel);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, boolean dialog, String panelId, Object styleClass)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, dialog, panelId, styleClass);
      out.write(inner);
      generatePanelEnd(out, contextPath, panel);
   }
   
   public static void generatePanelStart(Writer out, String contextPath, String panel, String bgColor)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, false, "", "");
   }
   
   public static void generatePanelStart(Writer out, String contextPath, String panel, String bgColor, String panelId)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, false, panelId, "");
   }
   
   public static void generatePanelStart(final Writer out, 
                                         final String contextPath, 
                                         final String panel, 
                                         final String bgColor, 
                                         final boolean dialog, 
                                         final String panelId, 
                                         final Object styleClass)
      throws IOException
   {
      out.write("<div");
      if (panelId.length() > 0)
      {
         out.write(" id='");
         out.write(panelId);
         out.write("'");
      }
      out.write(" class='panel");
      if (dialog)
      {
         out.write(" dialog");
      }
      if (styleClass != null)
      {
         out.write(" " + styleClass.toString());
      }
      out.write(" " + panel + "'>");
   }
   
   public static void generatePanelStartWithBgImg(final Writer out, 
            final String contextPath, final String panel, String bgColor)
            throws IOException
   {
      out.write("<div class='panel " + panel + "-background'>");
   }
   
   public static void generatePanelEnd(final Writer out, 
                                       final String contextPath, 
                                       final String panel)
      throws IOException
   {
      out.write("</div>");
   }
   
   public static void generatePanelEndWithBgImg(final Writer out, 
                                       final String contextPath, 
                                       final String panel)
      throws IOException
   {
      out.write("</div>");
}
   
   public static void generateTitledPanelMiddle(final Writer out, 
                                                final String contextPath, 
                                                final String titlePanel, 
                                                final String contentPanel, 
                                                final String contentBgColor) 
      throws IOException
   {
      // generate the expanded part, just under the title
   }
   
   public static void generateExpandedTitledPanelMiddle(final Writer out,
                                                        final String contextPath, 
                                                        final String titlePanel, 
                                                        final String expandedTitlePanel, 
                                                        final String contentPanel, 
                                                        final String contentBgColor)
      throws IOException
   {
      // generate the expanded part, just under the title
   }
   
   public final static String BGCOLOR_WHITE = "#FFFFFF";
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
package org.alfresco.web.ui.common;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper to generate the rounded panel HTML templates
 * 
 * @author kevinr
 */
public final class PanelGenerator
{
   public static void generatePanel(Writer out, String contextPath, String panel, String inner)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, BGCOLOR_WHITE);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, bgColor, false);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, String panelId)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, bgColor, false, panelId, "");
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, String panelId, Object styleClass)
      throws IOException
   {
      generatePanel(out, contextPath, panel, inner, bgColor, false, panelId, styleClass);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, boolean dialog)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, dialog, "", "");
      out.write(inner);
      generatePanelEnd(out, contextPath, panel);
   }
   
   public static void generatePanel(Writer out, String contextPath, String panel, String inner, String bgColor, boolean dialog, String panelId, Object styleClass)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, dialog, panelId, styleClass);
      out.write(inner);
      generatePanelEnd(out, contextPath, panel);
   }
   
   public static void generatePanelStart(Writer out, String contextPath, String panel, String bgColor)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, false, "", "");
   }
   
   public static void generatePanelStart(Writer out, String contextPath, String panel, String bgColor, String panelId)
      throws IOException
   {
      generatePanelStart(out, contextPath, panel, bgColor, false, panelId, "");
   }
   
   public static void generatePanelStart(final Writer out, 
                                         final String contextPath, 
                                         final String panel, 
                                         final String bgColor, 
                                         final boolean dialog, 
                                         final String panelId, 
                                         final Object styleClass)
      throws IOException
   {
      out.write("<div");
      if (panelId.length() > 0)
      {
         out.write(" id='");
         out.write(panelId);
         out.write("'");
      }
      out.write(" class='panel");
      if (dialog)
      {
         out.write(" dialog");
      }
      if (styleClass != null)
      {
         out.write(" " + styleClass.toString());
      }
      out.write(" " + panel + "'>");
   }
   
   public static void generatePanelStartWithBgImg(final Writer out, 
            final String contextPath, final String panel, String bgColor)
            throws IOException
   {
      out.write("<div class='panel " + panel + "-background'>");
   }
   
   public static void generatePanelEnd(final Writer out, 
                                       final String contextPath, 
                                       final String panel)
      throws IOException
   {
      out.write("</div>");
   }
   
   public static void generatePanelEndWithBgImg(final Writer out, 
                                       final String contextPath, 
                                       final String panel)
      throws IOException
   {
      out.write("</div>");
}
   
   public static void generateTitledPanelMiddle(final Writer out, 
                                                final String contextPath, 
                                                final String titlePanel, 
                                                final String contentPanel, 
                                                final String contentBgColor) 
      throws IOException
   {
      // generate the expanded part, just under the title
   }
   
   public static void generateExpandedTitledPanelMiddle(final Writer out,
                                                        final String contextPath, 
                                                        final String titlePanel, 
                                                        final String expandedTitlePanel, 
                                                        final String contentPanel, 
                                                        final String contentBgColor)
      throws IOException
   {
      // generate the expanded part, just under the title
   }
   
   public final static String BGCOLOR_WHITE = "#FFFFFF";
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
