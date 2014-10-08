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
package org.alfresco.web.ui.common.renderer;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIListItem;
import org.alfresco.web.ui.common.component.UIListItems;
import org.alfresco.web.ui.common.component.UIMenu;
import org.alfresco.web.ui.common.component.UIModeList;

/**
 * @author kevinr
 */
public class ModeListRenderer extends BaseRenderer
{
   // ------------------------------------------------------------------------------
   // Renderer implemenation
   
   /**
    * @see javax.faces.render.Renderer#decode(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void decode(FacesContext context, UIComponent component)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = getHiddenFieldName(context, component);
      String value = (String)requestMap.get(fieldId);
      
      // we encoded the value to start with our Id
      if (value != null && value.startsWith(component.getClientId(context) + NamingContainer.SEPARATOR_CHAR))
      {
         // found a new selected value for this ModeList
         // queue an event to represent the change
         // TODO: NOTE: The value object is passed in as a String here - is this a problem?
         //             As the 'value' field for a ModeListItem can contain Object...  
         Object selectedValue = value.substring(component.getClientId(context).length() + 1);
         UIModeList.ModeListItemSelectedEvent event = new UIModeList.ModeListItemSelectedEvent(component, selectedValue);
         component.queueEvent(event);
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeBegin(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      UIModeList list = (UIModeList)component;
      
      ResponseWriter out = context.getResponseWriter();

      Map attrs = list.getAttributes();
      
      if (!list.isMenu())
      {
         out.write("<ul");
         
         // horizontal rendering outputs a single row with each item as a column cell
         if (list.isHorizontal())
         {
            outputAttribute(out, attrs.get("styleClass") + " horizontal", "class");
         }
         else
         {
             outputAttribute(out, attrs.get("styleClass"), "class");
         }
                 
         outputAttribute(out, attrs.get("style"), "style");
         // TODO - implement width attribute with the list system
         out.write(">");
         
         // output title row if present
         if (list.getLabel() != null)
         {
            
            // output icon column
            if (list.getIconColumnWidth() != 0)
            {
                // with lists, we dont need an empty cell to mimic missing icon
            }
            
            // output title label
            out.write("<li");
            outputAttribute(out, attrs.get("labelStyle") + " title", "style");
            outputAttribute(out, attrs.get("labelStyleClass"), "class");
            out.write("><span>");
            out.write(Utils.encode(list.getLabel()));
            out.write("</span></li>");
            
         }
      }
      else
      {
         // render as a pop-up menu
         // TODO: show the image set for the individual item if available?
         
         String menuId = UIMenu.getNextMenuId(list, context);
         
         out.write("<span ");
         
         String selectedImage = (String)attrs.get("selectedImage");
         if (selectedImage != null)
         {
            out.write("class='icon-link' style='background: url(");
            out.write(context.getExternalContext().getRequestContextPath() + selectedImage);
            out.write(") no-repeat;'");
         }
         
         out.write("><a href='#' onclick=\"javascript:_toggleMenu(event, '");
         out.write(menuId);
         out.write("');return false;\">");
         
         // use default label if available
         String label = list.getLabel();
         if (label == null || label.length() == 0)
         {
            // else get the child components and walk to find the selected
            for (Iterator i=list.getChildren().iterator(); i.hasNext(); /**/)
            {
               UIComponent child = (UIComponent)i.next();
               if (child instanceof UIListItems)
               {
                  // get the value of the list items component and iterate
                  // through it's collection
                  Object listItems = ((UIListItems)child).getValue();
                  if (listItems instanceof Collection)
                  {
                     Iterator iter = ((Collection)listItems).iterator();
                     while (iter.hasNext())
                     {
                        UIListItem item = (UIListItem)iter.next();
                        
                        // if selected render as the label
                        if (item.getValue().equals(list.getValue()))
                        {
                           label = item.getLabel();
                           break;
                        }
                     }
                  }
               }
               else if (child instanceof UIListItem && child.isRendered())
               {
                  // found a valid UIListItem child to render
                  UIListItem item = (UIListItem)child;
                  
                  // if selected render as the label
                  if (item.getValue().equals(list.getValue()))
                  {
                     label = item.getLabel();
                     break;
                  }
               }
            }
         }
         
         // render the label
         if (label != null && label.length() != 0)
         {
            out.write("<span");
            outputAttribute(out, attrs.get("labelStyle"), "style");
            outputAttribute(out, attrs.get("labelStyleClass"), "class");
            out.write('>');
            out.write(Utils.encode(label));
            out.write("</span>&nbsp;");
         }
         
         // output image
         if (list.getMenuImage() != null)
         {
            out.write(Utils.buildImageTag(context, list.getMenuImage(), null, "2px"));
         }
         
         out.write("</a>");
         out.write("</span>");
         
         out.write("<ul style='display: none;' id='");
         out.write(menuId);
         out.write("' ");
         outputAttribute(out, attrs.get("styleClass"), "class");
         outputAttribute(out, attrs.get("style"), "style");
         //outputAttribute(out, attrs.get("width"), "width");
         out.write('>');
      }
   }
   
   /**
    * @see javax.faces.render.Renderer#encodeChildren(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeChildren(FacesContext context, UIComponent component) throws IOException
   {
      if (!component.isRendered())
      {
         return;
      }
      
      UIModeList list = (UIModeList)component;
      ResponseWriter out = context.getResponseWriter();
      
      // get the child components
      for (Iterator i=list.getChildren().iterator(); i.hasNext(); /**/)
      {
         UIComponent child = (UIComponent)i.next();
         if (child instanceof UIListItems)
         {
            // get the value of the list items component and iterate
            // through it's collection
            Object listItems = ((UIListItems)child).getValue();
            if (listItems instanceof Collection)
            {
               Iterator iter = ((Collection)listItems).iterator();
               while (iter.hasNext())
               {
                  UIListItem item = (UIListItem)iter.next();
                  if (item.isRendered())
                  {
                     renderItem(context, out, list, item);
                  }
               }
            }
         }
         else if (child instanceof UIListItem && child.isRendered())
         {
            // found a valid UIListItem child to render
            renderItem(context, out, list, (UIListItem)child);
         }
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeEnd(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeEnd(FacesContext context, UIComponent component) throws IOException
   {
      if (!component.isRendered())
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      // end outer table
      UIModeList list = (UIModeList)component;
      
      out.write("</ul>");
      if (list.isMenu())
      {
         // unnecessary at the moment, but might be useful in the future
      }
   }

   /**
    * @see javax.faces.render.Renderer#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return true;
   }
   
   /**
    * Renders the given item for the given list
    * 
    * @param context FacesContext
    * @param out ResponseWriter to write to
    * @param list The parent list
    * @param item The item to render
    * @throws IOException
    */
   protected void renderItem(FacesContext context, ResponseWriter out,
         UIModeList list, UIListItem item) throws IOException
   {
      Map attrs = list.getAttributes();
      String selectedImage = (String)attrs.get("selectedImage");
      
      out.write("<li");
      
      // if selected value render different style for the item
      boolean selected = item.getValue().equals(list.getValue());
      if (selected == true)
      {
         if (list.getIconColumnWidth() != 0)
         {
            String iconImage = "";
            String image = item.getImage();
            
            if (selectedImage != null)
            {
               iconImage = " background:url(" + context.getExternalContext().getRequestContextPath() + selectedImage + ") no-repeat";
            }
            else if (image != null)
            {
               iconImage = " background:url(" + context.getExternalContext().getRequestContextPath() + image + ") no-repeat";
            }
            
            outputAttribute(out, attrs.get("selectedStyleClass"), "class");
            outputAttribute(out, attrs.get("selectedStyle") + iconImage, "style");
         }
      }
      else
      {
         if (list.getIconColumnWidth() != 0)
         {
            String iconImage = "";
            String image = item.getImage();
            
            if(image != null)
            {
               iconImage = " background: url(" + context.getExternalContext().getRequestContextPath() + image + ") no-repeat";
            }
            outputAttribute(out, attrs.get("itemStyleClass"), "class");
            outputAttribute(out, attrs.get("itemStyle") + iconImage, "style");
         }
      }
      out.write(">");
      
      // output item link
      if (!list.isDisabled() && !item.isDisabled())
      {
         out.write("<a href='#' onclick=\"");
         // generate javascript to submit the value of the child component
         String value = list.getClientId(context) + NamingContainer.SEPARATOR_CHAR + (String)item.getAttributes().get("value");
         out.write(Utils.generateFormSubmit(context, list, getHiddenFieldName(context, list), value));
         out.write('"');
      }
      else
      {
         out.write("<span");
         outputAttribute(out, attrs.get("disabledStyleClass"), "class");
         outputAttribute(out, attrs.get("disabledStyle"), "style");
      }
      
      // render style for the item link
      if (item.getValue().equals(list.getValue()))
      {
         outputAttribute(out, attrs.get("selectedLinkStyleClass"), "class");
         outputAttribute(out, attrs.get("selectedLinkStyle"), "style");
      }
      else
      {
         outputAttribute(out, attrs.get("itemLinkStyleClass"), "class");
         outputAttribute(out, attrs.get("itemLinkStyle"), "style");
      }
      
      outputAttribute(out, item.getAttributes().get("tooltip"), "title");
      out.write('>');
      out.write(Utils.encode(item.getLabel()));
      if (!list.isDisabled() && !item.isDisabled())
      {
         out.write("</a>");
      }
      else
      {
         out.write("</span>");
      }
      out.write("</li>");
   }
   
   /**
    * We use a hidden field name based on the parent form component Id and
    * the string "modelist" to give a hidden field name that can be shared by all
    * ModeList components within a single UIForm component.
    * 
    * @return hidden field name
    */
   private static String getHiddenFieldName(FacesContext context, UIComponent component)
   {
      UIForm form = Utils.getParentForm(context, component);
      return form.getClientId(context) + NamingContainer.SEPARATOR_CHAR + "modelist";
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
package org.alfresco.web.ui.common.renderer;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIListItem;
import org.alfresco.web.ui.common.component.UIListItems;
import org.alfresco.web.ui.common.component.UIMenu;
import org.alfresco.web.ui.common.component.UIModeList;

/**
 * @author kevinr
 */
public class ModeListRenderer extends BaseRenderer
{
   // ------------------------------------------------------------------------------
   // Renderer implemenation
   
   /**
    * @see javax.faces.render.Renderer#decode(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void decode(FacesContext context, UIComponent component)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = getHiddenFieldName(context, component);
      String value = (String)requestMap.get(fieldId);
      
      // we encoded the value to start with our Id
      if (value != null && value.startsWith(component.getClientId(context) + NamingContainer.SEPARATOR_CHAR))
      {
         // found a new selected value for this ModeList
         // queue an event to represent the change
         // TODO: NOTE: The value object is passed in as a String here - is this a problem?
         //             As the 'value' field for a ModeListItem can contain Object...  
         Object selectedValue = value.substring(component.getClientId(context).length() + 1);
         UIModeList.ModeListItemSelectedEvent event = new UIModeList.ModeListItemSelectedEvent(component, selectedValue);
         component.queueEvent(event);
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeBegin(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      UIModeList list = (UIModeList)component;
      
      ResponseWriter out = context.getResponseWriter();

      Map attrs = list.getAttributes();
      
      if (!list.isMenu())
      {
         out.write("<ul");
         
         // horizontal rendering outputs a single row with each item as a column cell
         if (list.isHorizontal())
         {
            outputAttribute(out, attrs.get("styleClass") + " horizontal", "class");
         }
         else
         {
             outputAttribute(out, attrs.get("styleClass"), "class");
         }
                 
         outputAttribute(out, attrs.get("style"), "style");
         // TODO - implement width attribute with the list system
         out.write(">");
         
         // output title row if present
         if (list.getLabel() != null)
         {
            
            // output icon column
            if (list.getIconColumnWidth() != 0)
            {
                // with lists, we dont need an empty cell to mimic missing icon
            }
            
            // output title label
            out.write("<li");
            outputAttribute(out, attrs.get("labelStyle") + " title", "style");
            outputAttribute(out, attrs.get("labelStyleClass"), "class");
            out.write("><span>");
            out.write(Utils.encode(list.getLabel()));
            out.write("</span></li>");
            
         }
      }
      else
      {
         // render as a pop-up menu
         // TODO: show the image set for the individual item if available?
         
         String menuId = UIMenu.getNextMenuId(list, context);
         
         out.write("<span ");
         
         String selectedImage = (String)attrs.get("selectedImage");
         if (selectedImage != null)
         {
            out.write("class='icon-link' style='background: url(");
            out.write(context.getExternalContext().getRequestContextPath() + selectedImage);
            out.write(") no-repeat;'");
         }
         
         out.write("><a href='#' onclick=\"javascript:_toggleMenu(event, '");
         out.write(menuId);
         out.write("');return false;\">");
         
         // use default label if available
         String label = list.getLabel();
         if (label == null || label.length() == 0)
         {
            // else get the child components and walk to find the selected
            for (Iterator i=list.getChildren().iterator(); i.hasNext(); /**/)
            {
               UIComponent child = (UIComponent)i.next();
               if (child instanceof UIListItems)
               {
                  // get the value of the list items component and iterate
                  // through it's collection
                  Object listItems = ((UIListItems)child).getValue();
                  if (listItems instanceof Collection)
                  {
                     Iterator iter = ((Collection)listItems).iterator();
                     while (iter.hasNext())
                     {
                        UIListItem item = (UIListItem)iter.next();
                        
                        // if selected render as the label
                        if (item.getValue().equals(list.getValue()))
                        {
                           label = item.getLabel();
                           break;
                        }
                     }
                  }
               }
               else if (child instanceof UIListItem && child.isRendered())
               {
                  // found a valid UIListItem child to render
                  UIListItem item = (UIListItem)child;
                  
                  // if selected render as the label
                  if (item.getValue().equals(list.getValue()))
                  {
                     label = item.getLabel();
                     break;
                  }
               }
            }
         }
         
         // render the label
         if (label != null && label.length() != 0)
         {
            out.write("<span");
            outputAttribute(out, attrs.get("labelStyle"), "style");
            outputAttribute(out, attrs.get("labelStyleClass"), "class");
            out.write('>');
            out.write(Utils.encode(label));
            out.write("</span>&nbsp;");
         }
         
         // output image
         if (list.getMenuImage() != null)
         {
            out.write(Utils.buildImageTag(context, list.getMenuImage(), null, "2px"));
         }
         
         out.write("</a>");
         out.write("</span>");
         
         out.write("<ul style='display: none;' id='");
         out.write(menuId);
         out.write("' ");
         outputAttribute(out, attrs.get("styleClass"), "class");
         outputAttribute(out, attrs.get("style"), "style");
         //outputAttribute(out, attrs.get("width"), "width");
         out.write('>');
      }
   }
   
   /**
    * @see javax.faces.render.Renderer#encodeChildren(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeChildren(FacesContext context, UIComponent component) throws IOException
   {
      if (!component.isRendered())
      {
         return;
      }
      
      UIModeList list = (UIModeList)component;
      ResponseWriter out = context.getResponseWriter();
      
      // get the child components
      for (Iterator i=list.getChildren().iterator(); i.hasNext(); /**/)
      {
         UIComponent child = (UIComponent)i.next();
         if (child instanceof UIListItems)
         {
            // get the value of the list items component and iterate
            // through it's collection
            Object listItems = ((UIListItems)child).getValue();
            if (listItems instanceof Collection)
            {
               Iterator iter = ((Collection)listItems).iterator();
               while (iter.hasNext())
               {
                  UIListItem item = (UIListItem)iter.next();
                  if (item.isRendered())
                  {
                     renderItem(context, out, list, item);
                  }
               }
            }
         }
         else if (child instanceof UIListItem && child.isRendered())
         {
            // found a valid UIListItem child to render
            renderItem(context, out, list, (UIListItem)child);
         }
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeEnd(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeEnd(FacesContext context, UIComponent component) throws IOException
   {
      if (!component.isRendered())
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      // end outer table
      UIModeList list = (UIModeList)component;
      
      out.write("</ul>");
      if (list.isMenu())
      {
         // unnecessary at the moment, but might be useful in the future
      }
   }

   /**
    * @see javax.faces.render.Renderer#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return true;
   }
   
   /**
    * Renders the given item for the given list
    * 
    * @param context FacesContext
    * @param out ResponseWriter to write to
    * @param list The parent list
    * @param item The item to render
    * @throws IOException
    */
   protected void renderItem(FacesContext context, ResponseWriter out,
         UIModeList list, UIListItem item) throws IOException
   {
      Map attrs = list.getAttributes();
      String selectedImage = (String)attrs.get("selectedImage");
      
      out.write("<li");
      
      // if selected value render different style for the item
      boolean selected = item.getValue().equals(list.getValue());
      if (selected == true)
      {
         if (list.getIconColumnWidth() != 0)
         {
            String iconImage = "";
            String image = item.getImage();
            
            if (selectedImage != null)
            {
               iconImage = " background:url(" + context.getExternalContext().getRequestContextPath() + selectedImage + ") no-repeat";
            }
            else if (image != null)
            {
               iconImage = " background:url(" + context.getExternalContext().getRequestContextPath() + image + ") no-repeat";
            }
            
            outputAttribute(out, attrs.get("selectedStyleClass"), "class");
            outputAttribute(out, attrs.get("selectedStyle") + iconImage, "style");
         }
      }
      else
      {
         if (list.getIconColumnWidth() != 0)
         {
            String iconImage = "";
            String image = item.getImage();
            
            if(image != null)
            {
               iconImage = " background: url(" + context.getExternalContext().getRequestContextPath() + image + ") no-repeat";
            }
            outputAttribute(out, attrs.get("itemStyleClass"), "class");
            outputAttribute(out, attrs.get("itemStyle") + iconImage, "style");
         }
      }
      out.write(">");
      
      // output item link
      if (!list.isDisabled() && !item.isDisabled())
      {
         out.write("<a href='#' onclick=\"");
         // generate javascript to submit the value of the child component
         String value = list.getClientId(context) + NamingContainer.SEPARATOR_CHAR + (String)item.getAttributes().get("value");
         out.write(Utils.generateFormSubmit(context, list, getHiddenFieldName(context, list), value));
         out.write('"');
      }
      else
      {
         out.write("<span");
         outputAttribute(out, attrs.get("disabledStyleClass"), "class");
         outputAttribute(out, attrs.get("disabledStyle"), "style");
      }
      
      // render style for the item link
      if (item.getValue().equals(list.getValue()))
      {
         outputAttribute(out, attrs.get("selectedLinkStyleClass"), "class");
         outputAttribute(out, attrs.get("selectedLinkStyle"), "style");
      }
      else
      {
         outputAttribute(out, attrs.get("itemLinkStyleClass"), "class");
         outputAttribute(out, attrs.get("itemLinkStyle"), "style");
      }
      
      outputAttribute(out, item.getAttributes().get("tooltip"), "title");
      out.write('>');
      out.write(Utils.encode(item.getLabel()));
      if (!list.isDisabled() && !item.isDisabled())
      {
         out.write("</a>");
      }
      else
      {
         out.write("</span>");
      }
      out.write("</li>");
   }
   
   /**
    * We use a hidden field name based on the parent form component Id and
    * the string "modelist" to give a hidden field name that can be shared by all
    * ModeList components within a single UIForm component.
    * 
    * @return hidden field name
    */
   private static String getHiddenFieldName(FacesContext context, UIComponent component)
   {
      UIForm form = Utils.getParentForm(context, component);
      return form.getClientId(context) + NamingContainer.SEPARATOR_CHAR + "modelist";
   }
}
>>>>>>> develop-5.1
