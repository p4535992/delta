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
package org.alfresco.web.bean.generator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FloatConverter;
import javax.faces.convert.IntegerConverter;
import javax.faces.convert.LongConverter;
import javax.faces.model.SelectItem;

import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.cmr.dictionary.Constraint;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet.ClientValidation;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;

/**
 * Generates a text field component.
 * 
 * @author gavinc
 */
public class TextFieldGenerator extends BaseComponentGenerator
{
   private int size = 35;
   private int maxLength = 1024;
   private static final String NUMBER_VALIDATION_EE_ET_SUFFIX = "_EE_EN";
   
   /**
    * @return Returns the default size for a text field
    */
   public int getSize()
   {
      return size;
   }

   /**
    * @param size Sets the size of a text field
    */
   public void setSize(int size)
   {
      this.size = size;
   }

   /**
    * @return Returns the max length for the text field
    */
   public int getMaxLength()
   {
      return maxLength;
   }

   /**
    * @param maxLength Sets the max length of the text field
    */
   public void setMaxLength(int maxLength)
   {
      this.maxLength = maxLength;
   }

   @SuppressWarnings("unchecked")
   public UIComponent generate(FacesContext context, String id)
   {
      UIComponent component = context.getApplication().
            createComponent(ComponentConstants.JAVAX_FACES_INPUT);
      component.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
      FacesHelper.setupComponentId(context, component, id);

      component.getAttributes().put("size", this.size);
      component.getAttributes().put("maxlength", this.maxLength);
      
      return component;
   }

   @Override
   @SuppressWarnings("unchecked")
   protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, 
         PropertySheetItem item)
   {
      UIComponent component = null;
      
      final String id = getDefaultId(item);
      if (useGenerator(context, propertySheet))
      {
         // if the field has the list of values constraint 
         // and it is editable a SelectOne component is 
         // required otherwise create the standard edit component
         ListOfValuesConstraint constraint = getListOfValuesConstraint(
               context, propertySheet, item);
         
         PropertyDefinition propDef = this.getPropertyDefinition(context, 
               propertySheet.getNode(), item.getName());
         
         if (constraint != null && item.isReadOnly() == false &&
             propDef != null && propDef.isProtected() == false)
         {
            component = context.getApplication().createComponent(
                  UISelectOne.COMPONENT_TYPE);
            FacesHelper.setupComponentId(context, component, id);
            
            // create the list of choices
            UISelectItems itemsComponent = (UISelectItems)context.getApplication().
               createComponent("javax.faces.SelectItems");
            
            List<SelectItem> items = new ArrayList<SelectItem>(3);
            List<String> values = constraint.getAllowedValues();
            for (String value : values)
            {
               Object obj = null;
               
               // we need to setup the list with objects of the correct type
               if (propDef.getDataType().getName().equals(DataTypeDefinition.INT))
               {
                  obj = Integer.valueOf(value);
               }
               else if (propDef.getDataType().getName().equals(DataTypeDefinition.LONG))
               {
                  obj = Long.valueOf(value);
               }
               else if (propDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE))
               {
                  obj = Double.valueOf(value);
               }
               else if (propDef.getDataType().getName().equals(DataTypeDefinition.FLOAT))
               {
                  obj = Float.valueOf(value);
               }
               else
               {
                  obj = value;
               }
               
               items.add(new SelectItem(obj, value));
            }
            
            itemsComponent.setValue(items);
            
            // add the items as a child component
            component.getChildren().add(itemsComponent);
         }
         else
         {
            // use the standard component in edit mode
            component = generate(context, id);
         }
      }
      else
      {
         // create an output text component in view mode
         component = createOutputTextComponent(context, id);
      }
      
      return component;
   }

   @Override
   @SuppressWarnings("unchecked")
   protected void setupMandatoryValidation(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem item, 
         UIComponent component, boolean realTimeChecking, 
         String idSuffix)
   {
      if (component instanceof UIMultiValueEditor)
      {
         // Override the setup of the mandatory validation 
         // so we can send the _current_value id suffix.
         // We also enable real time so the page load
         // check disables the ok button if necessary, as the user
         // adds or removes items from the multi value list the 
         // page will be refreshed and therefore re-check the status.
         
         super.setupMandatoryValidation(context, propertySheet, item, 
               component, true, "_current_value");
      }
      else if (component instanceof UISelectOne)
      {
         // when there is a list of values constraint there
         // will always be a value so validation is not required.
      }
      else
      {
         // setup the client validation rule with real time validation enabled
         super.setupMandatoryValidation(context, propertySheet, item, 
               component, true, idSuffix);
      
         // add event handler to kick off real time checks
         component.getAttributes().put("onkeyup", "processButtonState();");
      }
   }
   
   @Override
   protected void setupConstraints(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      // do the default processing first
      super.setupConstraints(context, propertySheet, property, 
            propertyDef, component);
      
      // if the property type is a number based type and the property
      // sheet is in edit mode and validation is turned, on add the 
      // validateIsNumber validation function
      if (propertySheet.inEditMode() && propertySheet.isValidationEnabled() &&
          propertyDef != null)
      {
         // check the type of the property is a number
         if (propertyDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE) || 
             propertyDef.getDataType().getName().equals(DataTypeDefinition.FLOAT) ||
             propertyDef.getDataType().getName().equals(DataTypeDefinition.INT) || 
             propertyDef.getDataType().getName().equals(DataTypeDefinition.LONG))
         {
            List<String> params = new ArrayList<String>(3);
         
            // add the value parameter
            String value = "document.getElementById('" +
                  component.getClientId(context) + "')";
            params.add(value);
            
            String msg;
            String jsValidatingFunctionName;
            boolean isCommaAllowed = false;
            if (property instanceof WMUIProperty) {
                isCommaAllowed = Boolean.parseBoolean(((WMUIProperty) property).getCustomAttributes().get(UIProperty.ALLOW_COMMA_AS_DECIMAL_SEPARATOR_ATTR));
            }            
            if (propertyDef.getDataType().getName().equals(DataTypeDefinition.INT) ||
                    propertyDef.getDataType().getName().equals(DataTypeDefinition.LONG)) {
                msg = Application.getMessage(context, "validation_is_int_number");
                jsValidatingFunctionName = "validateIsLongNumber";
            } else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE)) {
                msg = Application.getMessage(context, "validation_is_double_number");
                jsValidatingFunctionName = "validateIsNumber";
                if (isCommaAllowed) {
                    jsValidatingFunctionName += NUMBER_VALIDATION_EE_ET_SUFFIX;
                }                
            } else {
                msg = Application.getMessage(context, "validation_is_number");
                jsValidatingFunctionName = "validateIsNumber";
                if (isCommaAllowed) {
                    jsValidatingFunctionName += NUMBER_VALIDATION_EE_ET_SUFFIX;
                }                
            }
            
            // add the validation failed message to show
            addStringConstraintParam(params, 
                  MessageFormat.format(msg, new Object[] {property.getResolvedDisplayLabel()}));
            
            // add the validation case to the property sheet
            propertySheet.addClientValidation(new ClientValidation(jsValidatingFunctionName,
               params, false));
         }
      }
   }

   /**
    * Retrieves the list of values constraint for the item, if it has one
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param item The item being generated
    * @return The constraint if the item has one, null otherwise
    */
   protected ListOfValuesConstraint getListOfValuesConstraint(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem item)
   {
      ListOfValuesConstraint lovConstraint = null;
      
      // get the property definition for the item
      PropertyDefinition propertyDef = getPropertyDefinition(context,
               propertySheet.getNode(), item.getName());
      
      if (propertyDef != null)
      {
         // go through the constaints and see if it has the
         // list of values constraint
         List<ConstraintDefinition> constraints = propertyDef.getConstraints();
         for (ConstraintDefinition constraintDef : constraints)
         {
            Constraint constraint = constraintDef.getConstraint();
               
            if (constraint instanceof ListOfValuesConstraint)
            {
               lovConstraint = (ListOfValuesConstraint)constraint;
               break;
            }
         }
      }
      
      return lovConstraint;
   }
   
   protected void setupConverter(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      // do default processing
      super.setupConverter(context, propertySheet, property, propertyDef, component);
      
      // if there isn't a converter and the property has a list of values constraint
      // on a number property we need to add the appropriate one.
      if (propertySheet.inEditMode() && propertyDef != null && 
          component instanceof UIOutput)
      {
         Converter converter = ((UIOutput)component).getConverter();
         if (converter == null)
         {
            ListOfValuesConstraint constraint = getListOfValuesConstraint(context, 
                     propertySheet, property);
              // don't understand why this if was initially added by alfresco developers.
              // uncommented it, to automatically add appropriate converter so that node.properties wouldn't contain string instead of number
//            if (constraint != null)
//            {
               String converterId = null;
               
               if (propertyDef.getDataType().getName().equals(DataTypeDefinition.INT))
               {
                  converterId = IntegerConverter.CONVERTER_ID;
               }
               else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.LONG))
               {
                  converterId = LongConverter.CONVERTER_ID;
               }
               else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE))
               {
                  // NOTE: the constant for the double converter is wrong in MyFaces!!
                  converterId = "javax.faces.Double";
               }
               else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.FLOAT))
               {
                  converterId = FloatConverter.CONVERTER_ID;
               }
               
               if (converterId != null)
               {
                  createAndSetConverter(context, converterId, component);
               }
//            }
         }
      }
   }
}
