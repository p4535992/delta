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

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.renderkit.PropertySheetGridRenderer;
import ee.webmedia.alfresco.help.web.HelpTextUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;

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

         // Field help:
         String property = StringUtils.substringAfter(((PropertySheetItem) component).getName(), ":");
         UIActionLink help = null;
         if (HelpTextUtil.hasHelpText(context, HelpTextUtil.TYPE_FIELD, property)) {
             help = HelpTextUtil.createHelpTextLink(context, HelpTextUtil.TYPE_FIELD, property);
         }

         boolean helpAdded = false;
         if (label instanceof HtmlPanelGrid && help != null) { // Search screens have checkboxes
             if (label.getChildCount() < 3) { // Check, if we have already added the help
                 label.getChildren().add(1, help); // Always after the label itself
                 ((HtmlPanelGrid) label).setColumns(label.getChildCount());
                 ((HtmlPanelGrid) label).setColumnClasses("propertiesLabel,padding-3," + StringUtils.chop(StringUtils.repeat("vertical-align-middle,", label.getChildCount()-2)));
             }
             helpAdded = true;
         }

         Utils.encodeRecursive(context, label);

         if (help != null && !helpAdded) {
             ComponentUtil.addStyleClass(help, "margin-left-4");
             Utils.encodeRecursive(context, help);
         }

         // encode the control
         out.write("</td><td");
         if (!PropertySheetGridRenderer.isInlineComponent(component)) {
             out.write(" colspan=\"3\"");
         }
         out.write(">");
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
