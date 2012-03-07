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
package org.alfresco.web.ui.repo.renderer.property;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletContext;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.apache.commons.lang.StringUtils;

/**
 * Renderer for a PropertySheetItem component
 * 
 * @author gavinc
 */
public class PropertySheetItemRenderer extends BaseRenderer
{
    
   /**
    * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   @Override
public void encodeBegin(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeChildren(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   @Override
@SuppressWarnings("unchecked")
   public void encodeChildren(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      // make sure there are 2 or 3 child components
      int count = component.getChildCount();
      
      if (count == 2 || count == 3)
      {
         // get the label and the control
         List<UIComponent> children = component.getChildren();
         UIComponent label = children.get(0);
         UIComponent control = children.get(1);
         
         // NOTE: Replacement for the mandatory marker 
         if(count == 3)
         {
             out.write("<span class=\"red\">*&nbsp;</span>");
         }
         // encode the label
         Utils.encodeRecursive(context, label);

         ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
         Map<String, Map<String, Boolean>> helpTexts = (Map<String, Map<String, Boolean>>) servletContext.getAttribute("helpText");
         String property = StringUtils.substringAfter(((PropertySheetItem) component).getName(), ":");
         if (helpTexts != null && helpTexts.get("field") != null && Boolean.TRUE.equals(helpTexts.get("field").get(property))) {
           out.write("<span>&nbsp;&nbsp;&nbsp;&nbsp;<img src=\"");
           out.write(context.getExternalContext().getRequestContextPath());
           out.write("/images/icons/Help.gif\" alt=\"Abiinfo\" title=\"Abiinfo\" onclick=\"popup('");
           out.write(servletContext.getContextPath());
           out.write("/help/field/");
           out.write(property);
           out.write("')\" style=\"cursor:pointer\"/></span>");
         }

         // encode the control
         out.write("</td><td>");
         Utils.encodeRecursive(context, control);
         
         // NOTE: we'll allow the property sheet's grid renderer close off the last <td>
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeEnd(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   @Override
public void encodeEnd(FacesContext context, UIComponent component) throws IOException
   {
      // we don't need to do anything in here
   }

   /**
    * @see javax.faces.render.Renderer#getRendersChildren()
    */
   @Override
public boolean getRendersChildren()
   {
      return true;
   }
}
