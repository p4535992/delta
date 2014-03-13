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
package org.alfresco.web.ui.common.component;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.PanelGenerator;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.evaluator.PermissionEvaluator;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * @author kevinr
 */
public class UIPanel extends UICommand
{
   // ------------------------------------------------------------------------------
   // Component Impl
    public static final String STYLE_CLASS_NONFLOATING_ELEMENT = "nonfloating-element";
    
   /**
    * Default constructor
    */
   public UIPanel()
   {
      setRendererType(null);
   }

   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Controls";
   }

   /**
    * Return the UI Component to be displayed on the right of the panel title area
    *
    * @return UIComponent
    */
   public UIComponent getTitleComponent()
   {
      UIComponent titleComponent = null;

      // attempt to find a component with the specified ID
      String facetsId = getFacetsId();
      if (facetsId != null)
      {
         UIForm parent = Utils.getParentForm(FacesContext.getCurrentInstance(), this);
         UIComponent facetsComponent = parent.findComponent(facetsId);
         if (facetsComponent != null)
         {
            // get the 'title' facet from the component
            titleComponent = facetsComponent.getFacet("title");
         }
      } else {
          titleComponent = getFacet("title");
      }

      return titleComponent;
   }
   
   /**
    * Return the UI Component to be displayed in the bottom panel area
    *
    * @return UIComponent
    */
   public UIComponent getFooterComponent()
   {
      UIComponent footerComponent = null;
      // attempt to find a component with the specified ID
      String facetsId = getFacetsId();
      if (facetsId != null)
      {
         UIForm parent = Utils.getParentForm(FacesContext.getCurrentInstance(), this);
         UIComponent facetsComponent = parent.findComponent(facetsId);
         if (facetsComponent != null)
         {
            // get the 'footer' facet from the component
            footerComponent = facetsComponent.getFacet("footer");
         }
      } else {
          footerComponent = getFacet("footer");
      }
      return footerComponent;
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

      // determine if we have a component on the header
      UIComponent titleComponent = getTitleComponent();
      
      boolean isIconAction = false;
      UIComponent titleParent = titleComponent != null ? titleComponent.getParent() : null; 
      String styleClass = (String) (titleParent != null ? ComponentUtil.getAttribute(titleParent, "styleClass") : null);
      if (titleComponent instanceof PermissionEvaluator || titleComponent instanceof UIActions || STYLE_CLASS_NONFLOATING_ELEMENT.equals(styleClass)) 
      {
          isIconAction = true;
      }
 
      // determine the panel id
      String panelId = this.getId();
      if(panelId == null)
      {
          panelId = "";
      }

      // determine whether we have any adornments
      String label = getLabel();
      if (label != null)
      {
         label = Utils.encode(label);
      }
      if (label != null || isProgressive() || titleComponent != null)
      {
         this.hasAdornments = true;
      }

      // make sure we have a default background color for the content area
      String bgcolor = getBgcolor();
      if (bgcolor == null)
      {
         bgcolor = PanelGenerator.BGCOLOR_WHITE;
      }

      // determine if we have a bordered title area, note, we also need to have
      // the content area border defined as well
      if (getTitleBgcolor() != null && getTitleBorder() != null &&
          getBorder() != null && this.hasAdornments)
      {
         this.hasBorderedTitleArea = true;
      }

      // output first part of border table
      if (this.hasBorderedTitleArea)
      {
         PanelGenerator.generatePanelStart(
               out,
               context.getExternalContext().getRequestContextPath(),
               getTitleBorder(),
               getTitleBgcolor(),
               false,
               panelId,
               getAttributes().get("styleClass"));
      }
      else if (getBorder() != null)
      {
         PanelGenerator.generatePanelStart(
               out,
               context.getExternalContext().getRequestContextPath(),
               getBorder(),
               bgcolor,
               false,
               panelId,
               getAttributes().get("styleClass"));
      }
      else
      {
         PanelGenerator.generatePanelStart(
               out,
               context.getExternalContext().getRequestContextPath(),
               "",
               bgcolor,
               false,
               panelId,
               getAttributes().get("styleClass"));
      }

      if (this.hasAdornments)
      {
         // start the container if we have any adornments
         out.write("<div");
         Utils.outputAttribute(out, getAttributes().get("style"), "style");
         Utils.outputAttribute(out, "panel-wrapper", "class");
         out.write(">");
      }

      // Start outputting label.
      if (label != null)
      {
         out.write("<h3>");
      }


       // Id of the div, that is expanded/closed.
       String hideableDivId = panelId + "-panel-border";
      // output progressive disclosure icon in appropriate state
      // TODO: manage state of this icon via component Id!
      if (isProgressive())
      {
         out.write("<a onclick=\"togglePanelWithStateUpdate('#" + hideableDivId + "', '" + panelId + "', '" + context.getViewRoot().getViewId() + "');\">");

         /*
         if (isExpanded() == true)
         {
            out.write(Utils.buildImageTag(context, WebResources.TABLE_PANEL_EXPANDED, 14, 14, getLabel()));
         }
         else
         {
            out.write(Utils.buildImageTag(context, WebResources.TABLE_PANEL_COLLAPSED, 14, 14, getLabel()));
         }
         */
      }

      // output textual label
      if (label != null)
      {
         out.write(label);    // already encoded above
      }
      
      if (isProgressive() == true) {
         out.write("</a>&nbsp;&nbsp;");
      }
      
      if (titleComponent != null && isIconAction)
      {
          out.write("<span class='title-component-nonfloating'>");
          Utils.encodeRecursive(context, titleComponent);
          out.write("</span>");
      }

      if(label != null)
      {
         out.write("</h3>");
      }

      if (this.hasAdornments)
      {
         //We don't need to add any closing elements
      }

      // render the title component if supplied
      if (titleComponent != null && !isIconAction)
      {
         out.write("<span class='title-component'>");
         Utils.encodeRecursive(context, titleComponent);
         out.write("</span>");
      }

      if (this.hasAdornments)
      {
          out.write("<div id='"+hideableDivId+"' class='panel-border'");
          isExpanded(); // call this out to refresh the expanded variable, but do not use it because it returns always true
          if (!expanded) {
              out.write(" style=\"display:none;\"");
          }
          out.write(">");
          
          
      }

      // if we have the titled border area, output the middle section
      if (this.hasBorderedTitleArea)
      {
         if (getExpandedTitleBorder() != null)
         {
            PanelGenerator.generateExpandedTitledPanelMiddle(
                  out,
                  context.getExternalContext().getRequestContextPath(),
                  getTitleBorder(),
                  getExpandedTitleBorder(),
                  getBorder(),
                  getBgcolor());
         }
         else
         {
            PanelGenerator.generateTitledPanelMiddle(
                  out,
                  context.getExternalContext().getRequestContextPath(),
                  getTitleBorder(),
                  getBorder(),
                  getBgcolor());
         }
      }
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeEnd(javax.faces.context.FacesContext)
    */
   public void encodeEnd(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }

      ResponseWriter out = context.getResponseWriter();

      // render the footer component if supplied
      UIComponent footerComponent = getFooterComponent();
      if (footerComponent != null)
      {
         out.write("<span class='footer-component'>");
         Utils.encodeRecursive(context, footerComponent);
         out.write("</span>");
      }

      // output final part of border table

      if (this.hasAdornments)
      {
         // table-border
         out.write("</div>");
         //table-wrapper
         out.write("</div>");
      }

      if (getBorder() != null)
      {
         PanelGenerator.generatePanelEnd(
               out,
               context.getExternalContext().getRequestContextPath(),
               getBorder());
      }
      else
      {
         PanelGenerator.generatePanelEnd(
               out,
               context.getExternalContext().getRequestContextPath(),
               "");
      }
   }

   /**
    * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
     // Decode nothing, state of the panel is handled using AJAX calls, see PanelStateBean.
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      setExpanded( ((Boolean)values[1]).booleanValue() );
      this.progressive = (Boolean)values[2];
      this.border = (String)values[3];
      this.bgcolor = (String)values[4];
      this.label = (String)values[5];
      this.titleBgcolor = (String)values[6];
      this.titleBorder = (String)values[7];
      this.expandedTitleBorder = (String)values[8];
      this.expandedActionListener = (MethodBinding)restoreAttachedState(context, values[9]);
      this.facetsId = (String)values[10];
   }

   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[] {
         super.saveState(context),
         this.expanded,
         this.progressive,
         this.border,
         this.bgcolor,
         this.label,
         this.titleBgcolor,
         this.titleBorder,
         this.expandedTitleBorder,
         saveAttachedState(context, this.expandedActionListener),
         this.facetsId};
      return values;
   }


   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors

   /**
    * @param binding    The MethodBinding to call when expand/collapse is performed by the user.
    */
   public void setExpandedActionListener(MethodBinding binding)
   {
      this.expandedActionListener = binding;
   }

   /**
    * @return The MethodBinding to call when expand/collapse is performed by the user.
    */
   public MethodBinding getExpandedActionListener()
   {
      return this.expandedActionListener;
   }

   /**
    * @return Returns the bgcolor.
    */
   public String getBgcolor()
   {
      ValueBinding vb = getValueBinding("bgcolor");
      if (vb != null)
      {
         this.bgcolor = (String)vb.getValue(getFacesContext());
      }

      return this.bgcolor;
   }

   /**
    * @param bgcolor    The bgcolor to set.
    */
   public void setBgcolor(String bgcolor)
   {
      this.bgcolor = bgcolor;
   }

   /**
    * @return Returns the border name.
    */
   public String getBorder()
   {
      ValueBinding vb = getValueBinding("border");
      if (vb != null)
      {
         this.border = (String)vb.getValue(getFacesContext());
      }

      return this.border;
   }

   /**
    * @param border  The border name to user.
    */
   public void setBorder(String border)
   {
      this.border = border;
   }

   /**
    * @return Returns the bgcolor of the title area
    */
   public String getTitleBgcolor()
   {
      ValueBinding vb = getValueBinding("titleBgcolor");
      if (vb != null)
      {
         this.titleBgcolor = (String)vb.getValue(getFacesContext());
      }

      return this.titleBgcolor;
   }

   /**
    * @param titleBgcolor Sets the bgcolor of the title area
    */
   public void setTitleBgcolor(String titleBgcolor)
   {
      this.titleBgcolor = titleBgcolor;
   }

   /**
    * @return Returns the border style of the title area
    */
   public String getTitleBorder()
   {
      ValueBinding vb = getValueBinding("titleBorder");
      if (vb != null)
      {
         this.titleBorder = (String)vb.getValue(getFacesContext());
      }

      return this.titleBorder;
   }

   /**
    * @param titleBorder Sets the border style of the title area
    */
   public void setTitleBorder(String titleBorder)
   {
      this.titleBorder = titleBorder;
   }

   /**
    * @return Returns the border style of the expanded title area
    */
   public String getExpandedTitleBorder()
   {
      ValueBinding vb = getValueBinding("expandedTitleBorder");
      if (vb != null)
      {
         this.expandedTitleBorder = (String)vb.getValue(getFacesContext());
      }

      return this.expandedTitleBorder;
   }

   /**
    * @param expandedTitleBorder Sets the border style of the expanded title area
    */
   public void setExpandedTitleBorder(String expandedTitleBorder)
   {
      this.expandedTitleBorder = expandedTitleBorder;
   }

   /**
    * @return Returns the label.
    */
   public String getLabel()
   {
      ValueBinding vb = getValueBinding("label");
      if (vb != null)
      {
         this.label = (String)vb.getValue(getFacesContext());
      }

      return this.label;
   }

   /**
    * @param label The label to set.
    */
   public void setLabel(String label)
   {
      this.label = label;
   }

   /**
    * @return Returns the progressive display setting.
    */
   public boolean isProgressive()
   {
      ValueBinding vb = getValueBinding("progressive");
      if (vb != null)
      {
         this.progressive = (Boolean)vb.getValue(getFacesContext());
      }

      if (this.progressive != null)
      {
         return this.progressive.booleanValue();
      }
      else
      {
         // return default
         return false;
      }
   }

   /**
    * @param progressive   The progressive display boolean to set.
    */
   public void setProgressive(boolean progressive)
   {
      this.progressive = Boolean.valueOf(progressive);
   }

   /**
    * Returns whether the component show allow rendering of its child components.
    */
   public boolean isExpanded()
   {
       ValueBinding vb = getValueBinding("expanded");
       if (vb != null)
       {
          this.expanded = (Boolean)vb.getValue(getFacesContext());
       }

      // Child components are always rendered, their expansion are handled in browser using javascript. 
      return true; // return always true, because otherwise child components will not be rendered
   }

   /**
    * Sets whether the component show allow rendering of its child components.
    * For this component we change this value if the user indicates to change the
    * hidden/visible state of the progressive panel.
    */
   public void setExpanded(boolean expanded)
   {
      this.expanded = Boolean.valueOf(expanded);
   }
   
   public Boolean getExpandedState(){
       return expanded;
   }

   /**
    * Get the facets component Id to use
    *
    * @return the facets component Id
    */
   public String getFacetsId()
   {
      ValueBinding vb = getValueBinding("facets");
      if (vb != null)
      {
         this.facetsId = (String)vb.getValue(getFacesContext());
      }

      return this.facetsId;
   }

   /**
    * Set the facets component Id to use
    *
    * @param facets     the facets component Id
    */
   public void setFacetsId(String facets)
   {
      this.facetsId = facets;
   }


   // ------------------------------------------------------------------------------
   // Private members

   // component settings
   private String border = null;
   private String bgcolor = null;
   private String titleBorder = null;
   private String titleBgcolor = null;
   private String expandedTitleBorder = null;
   private Boolean progressive = null;
   private String label = null;
   private String facetsId = null;
   private MethodBinding expandedActionListener = null;

   // component state
   private boolean hasAdornments = false;
   private boolean hasBorderedTitleArea = false;
   private Boolean expanded = Boolean.TRUE;
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing the an action relevant when the panel is expanded or collapsed.
    */
   public static class ExpandedEvent extends ActionEvent
   {
      public ExpandedEvent(UIComponent component, boolean state)
      {
         super(component);
         State = state;
      }
      
      public boolean State;
   }
}
