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
package org.alfresco.web.ui.repo.component.property;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIForm;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.event.FacesListener;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigLookupContext;
import org.alfresco.config.ConfigService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.config.PropertySheetConfigElement.AssociationConfig;
import org.alfresco.web.config.PropertySheetConfigElement.ChildAssociationConfig;
import org.alfresco.web.config.PropertySheetConfigElement.ItemConfig;
import org.alfresco.web.config.PropertySheetConfigElement.PropertyConfig;
import org.alfresco.web.config.PropertySheetConfigElement.SeparatorConfig;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.web.WeakReferenceSerializable;

/**
 * Component that represents the properties of a Node
 * 
 * @author gavinc
 */
public class UIPropertySheet extends UIPanel implements NamingContainer, AjaxUpdateable
{
   public static final String VIEW_MODE = "view";
   public static final String EDIT_MODE = "edit";
   
   private static Log logger = LogFactory.getLog(UIPropertySheet.class);
   private static String DEFAULT_VAR_NAME = "node";
   protected static String PROP_ID_PREFIX = "prop_";
   protected static String ASSOC_ID_PREFIX = "assoc_";
   protected static String SEP_ID_PREFIX = "sep_";
   
   private List<ClientValidation> validations = new ArrayList<ClientValidation>();
   private String variable;
   private NodeRef nodeRef;
   private Node node;
   private Boolean readOnly;
   private Boolean validationEnabled;
   private String mode;
   private String configArea;
   private String nextButtonId;
   private String finishButtonId;
   private PropertySheetConfigElement config;
   
   /**
    * Default constructor
    */
   public UIPropertySheet()
   {
      // set the default renderer for a property sheet
      setRendererType(ComponentConstants.JAVAX_FACES_GRID);
      addFacesListener(new DummyListener());
   }

   public static class DummyListener implements FacesListener {
       // Do nothing
   }
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   @Override
   public String getFamily()
   {
      return UIPanel.COMPONENT_FAMILY;
   }

   /**
    * @see javax.faces.component.UIComponent#encodeBegin(javax.faces.context.FacesContext)
    */
   @Override
   public void encodeBegin(FacesContext context) throws IOException
   {
      int howManyChildren = getChildren().size();
      Boolean externalConfig = (Boolean)getAttributes().get("externalConfig");
      
      // generate a variable name to use if necessary
      if (this.variable == null)
      {
         this.variable = DEFAULT_VAR_NAME;
      }
      
      // force retrieval of node info
      Node node = getNode();
      
      if (howManyChildren == 0)
      {
         if (externalConfig != null && externalConfig.booleanValue())
         {
            // configure the component using the config service
            if (logger.isDebugEnabled())
               logger.debug("Configuring property sheet using ConfigService");

            PropertySheetConfigElement itemsToDisplay = getConfig();
            if (itemsToDisplay == null)
            {
               Config configProps;
               // get the properties to display
               ConfigService configSvc = Application.getConfigService(FacesContext.getCurrentInstance());
               if (getConfigArea() == null)
               {
                  configProps = configSvc.getConfig(node);
               }
               else
               {
                  // only look within the given area
                  configProps = configSvc.getConfig(node, new ConfigLookupContext(getConfigArea()));
               }
               itemsToDisplay = (PropertySheetConfigElement)configProps.
                  getConfigElement("property-sheet");
            }

            if (itemsToDisplay != null)
            {
               Collection<ItemConfig> itemsToRender = null;
               
               if (this.getMode().equalsIgnoreCase(EDIT_MODE))
               {
                  itemsToRender = itemsToDisplay.getEditableItemsToShow().values();
                  
                  if (logger.isDebugEnabled())
                     logger.debug("Items to render: " + itemsToDisplay.getEditableItemNamesToShow());
               }
               else
               {
                  itemsToRender = itemsToDisplay.getItemsToShow().values();
                  
                  if (logger.isDebugEnabled())
                     logger.debug("Items to render: " + itemsToDisplay.getItemNamesToShow());
               }
            
               createComponentsFromConfig(context, itemsToRender);
            }
            else
            {
               if (logger.isDebugEnabled())
                  logger.debug("There are no items to render!");
            }
         }
         else
         {
            // show all the properties for the current node
            if (logger.isDebugEnabled())
               logger.debug("Configuring property sheet using node's current state");
            
            createComponentsFromNode(context, node);
         }
      }
      
      // put the node in the session if it is not there already
      storePropSheetVariable(node);

      ResponseWriter out = context.getResponseWriter();
      out.write("<div id=\"");
      out.write(getAjaxClientId(context));
      out.write("\">");

      super.encodeBegin(context);
   }

    /**
     * @param node - variable to be stored
     */
    protected void storePropSheetVariable(Node node) {
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionMap = getFacesContext().getExternalContext().getSessionMap();
        sessionMap.put(this.variable, new WeakReferenceSerializable(node));

        if (logger.isDebugEnabled())
           logger.debug("Put node into session with key '" + this.variable + "': " + node);
    }
    
    /** If reference to bound variable is dismissed, there is no point in processing submitted data */
    @Override
    public void processDecodes(FacesContext context) {
        Map<String, Object> sessionMap = getFacesContext().getExternalContext().getSessionMap();
        WeakReferenceSerializable variableRef = (WeakReferenceSerializable) sessionMap.get(variable);
        if (variableRef != null && variableRef.get() != null) {
            super.processDecodes(context);
        }
    }

    /** If reference to bound variable is dismissed, there is no point in processing submitted data */
    @Override
    public void processUpdates(FacesContext context) {
        Map<String, Object> sessionMap = getFacesContext().getExternalContext().getSessionMap();
        WeakReferenceSerializable variableRef = (WeakReferenceSerializable) sessionMap.get(variable);
        if (variableRef != null && variableRef.get() != null) {
            super.processUpdates(context);
        }
    }

    /** If reference to bound variable is dismissed, there is no point in processing submitted data */
    @Override
    public void processValidators(FacesContext context) {
        Map<String, Object> sessionMap = getFacesContext().getExternalContext().getSessionMap();
        WeakReferenceSerializable variableRef = (WeakReferenceSerializable) sessionMap.get(variable);
        if (variableRef != null && variableRef.get() != null) {
            super.processValidators(context);
        }
    }    

   /**
    * @see javax.faces.component.UIComponent#encodeBegin(javax.faces.context.FacesContext)
    */
   @Override
   public void encodeEnd(FacesContext context) throws IOException
   {
      super.encodeEnd(context);
      
      // NOTE: We should really use a renderer to output the JavaScript below but that would
      //       require extending the MyFaces HtmlGridRenderer class which we should avoid doing.
      //       Until we support multiple client types this will be OK.
      
      // output the JavaScript to enforce the required validations (if validation is enabled)
      if (isValidationEnabled())
      {
         renderValidationScript(context);
      }

      ResponseWriter out = context.getResponseWriter();
      out.write("</div>");
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   @Override
   @SuppressWarnings("unchecked")
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.nodeRef = (NodeRef)values[1];
      this.node = (Node)values[2];
      this.variable = (String)values[3];
      this.readOnly = (Boolean)values[4];
      this.mode = (String)values[5];
      this.configArea = (String)values[6];
      this.validationEnabled = (Boolean)values[7];
      this.validations = (List<ClientValidation>)values[8];
      this.finishButtonId = (String)values[9];
      this.nextButtonId = (String)values[10];
      this.config = (PropertySheetConfigElement)values[11];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   @Override
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[12];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.nodeRef;
      values[2] = this.node;
      values[3] = this.variable;
      values[4] = this.readOnly;
      values[5] = this.mode;
      values[6] = this.configArea;
      values[7] = this.validationEnabled;
      values[8] = this.validations;
      values[9] = this.finishButtonId;
      values[10] = this.nextButtonId;
      values[11] = this.config;
      return (values);
   }
   
   /**
    * @return Returns the node
    */
   public Node getNode()
   {
      Node node = null;
      
      if (this.node == null)
      {
         // use the value to get hold of the actual object
         Object value = getAttributes().get("value");
         
         if (value == null)
         {
            ValueBinding vb = getValueBinding("value");
            if (vb != null)
            {
               value = vb.getValue(getFacesContext());
            }
         }

         if (value instanceof Node)
         {
            node = (Node)value;
         }
      }
      else
      {
         node = this.node;
      }
      
      return node;
   }
   
   /**
    * @param node The node
    */
   public void setNode(Node node)
   {
      this.node = node;
   }
   
   /**
    * @return Returns the variable.
    */
   public String getVar()
   {
      return this.variable;
   }

   /**
    * @param variable The variable to set.
    */
   public void setVar(String variable)
   {
      this.variable = variable;
   }
   
   /**
    * @return Returns whether the property sheet is read only
    */
   public boolean isReadOnly()
   {
      if (this.readOnly == null)
      {
         ValueBinding vb = getValueBinding("readOnly");
         if (vb != null)
         {
            this.readOnly = (Boolean)vb.getValue(getFacesContext());
         }
      }
      
      if (this.readOnly == null)
      {
         this.readOnly = Boolean.FALSE;
      }
      
      return this.readOnly; 
   }

   /**
    * @param readOnly Sets the read only flag for the property sheet
    */
   public void setReadOnly(boolean readOnly)
   {
      this.readOnly = Boolean.valueOf(readOnly);
   }

   /**
    * @return true if validation is enabled for this property sheet
    */
   public boolean isValidationEnabled()
   {
      // if the property sheet is in "view" mode validation will
      // always be disabled
      if (inEditMode() == false)
      {
         return false;
      }
      
      if (this.validationEnabled == null)
      {
         ValueBinding vb = getValueBinding("validationEnabled");
         if (vb != null)
         {
            this.validationEnabled = (Boolean)vb.getValue(getFacesContext());
         }
      }
      
      if (this.validationEnabled == null)
      {
         this.validationEnabled = Boolean.TRUE;
      }
      
      return this.validationEnabled; 
   }

   /**
    * @param validationEnabled Sets the validationEnabled flag
    */
   public void setValidationEnabled(boolean validationEnabled)
   {
      this.validationEnabled = Boolean.valueOf(validationEnabled);
   }

   /**
    * Returns the id of the finish button
    * 
    * @return The id of the finish button on the page
    */
   public String getFinishButtonId()
   {
      // NOTE: This parameter isn't value binding enabled
      if (this.finishButtonId == null)
      {
         this.finishButtonId = "finish-button";
      }
      
      return this.finishButtonId;
   }

   /**
    * Sets the id of the finish button being used on the page
    * 
    * @param finishButtonId The id of the finish button
    */
   public void setFinishButtonId(String finishButtonId)
   {
      this.finishButtonId = finishButtonId;
   }
   
   /**
    * Returns the id of the next button
    * 
    * @return The id of the next button on the page
    */
   public String getNextButtonId()
   {
      return this.nextButtonId;
   }

   /**
    * Sets the id of the next button being used on the page
    * 
    * @param nextButtonId The id of the next button
    */
   public void setNextButtonId(String nextButtonId)
   {
      this.nextButtonId = nextButtonId;
   }

   /**
    * @return Returns the mode
    */
   public String getMode()
   {
      if (this.mode == null)
      {
         ValueBinding vb = getValueBinding("mode");
         if (vb != null)
         {
            this.mode = (String)vb.getValue(getFacesContext());
         }
      }
      
      if (this.mode == null)
      {
         mode = EDIT_MODE;
      }
      
      return mode;
   }

   /**
    * @param mode Sets the mode
    */
   public void setMode(String mode)
   {
      this.mode = mode;
   }
   
   /**
    * Determines whether the property sheet is in edit mode
    * 
    * @return true if in edit mode
    */
   public boolean inEditMode()
   {
      return getMode().equalsIgnoreCase(EDIT_MODE);
   }
   
   /**
    * @return Returns the config area to use
    */
   public String getConfigArea()
   {
      if (this.configArea == null)
      {
         ValueBinding vb = getValueBinding("configArea");
         if (vb != null)
         {
            this.configArea = (String)vb.getValue(getFacesContext());
         }
      }
      
      return configArea;
   }
   
   /**
    * @param configArea Sets the config area to use
    */
   public void setConfigArea(String configArea)
   {
      this.configArea = configArea;
   }
   
   /**
    * Adds a validation case to the property sheet
    * 
    * @param validation The validation case to enforce
    */
   public void addClientValidation(ClientValidation validation)
   {
      this.validations.add(validation);
   }
   
   /**
    * @return Returns the list of client validations to enforce
    */
   public List<ClientValidation> getClientValidations()
   {
      return this.validations;
   }

    public void setConfig(PropertySheetConfigElement config)
    {
        this.config = config;
    }

    public PropertySheetConfigElement getConfig() {
        if (this.config == null)
        {
            ValueBinding vb = getValueBinding("config");
            if (vb != null)
            {
                this.config = (PropertySheetConfigElement)vb.getValue(getFacesContext());
            }
        }
        return config;
    }

    /**
     * @param text
     * @return text where special characters are replaced with "_" 
     */
    private static String replaceChars(final String text) {
        String specialCharacters = ".,:[]()\"/\\{}";
        return StringUtils.replaceChars(text, specialCharacters, StringUtils.repeat("_", specialCharacters.length()));
    }
   
   /**
    * Renders the necessary JavaScript to enforce any constraints the properties have.
    * 
    * HEAVILY REFACTORED TO SUPPORT MULTIPLE PROPERTY SHEETS ON THE SAME PAGE! General logic was moved to scripts.js file.
    * 
    * @param context FacesContext
    */
   private void renderValidationScript(FacesContext context) throws IOException {
      final String prefix = replaceChars(getVar());
      ResponseWriter out = context.getResponseWriter();
      UIForm form = Utils.getParentForm(context, this);
         
      // output the validation.js script
      out.write("\n<script type='text/javascript'>\n");
      
      // output the validateSubmit() function
      out.write("function " + prefix + "validateSubmit() {\n");
      out.write("   return (");
      int numberValidations = this.validations.size();
      List<ClientValidation> realTimeValidations = new ArrayList<ClientValidation>(numberValidations);
      if (numberValidations > 0) {
         for (int x = 0; x < numberValidations; x++) {
            ClientValidation validation = this.validations.get(x);
            if (validation.RealTimeChecking) {
               realTimeValidations.add(validation);
            }
            renderValidationMethod(out, validation, (x == (numberValidations-1)), true, false);
         }
      } else {
         out.write("true)");
      }
      out.write(";\n}\n\n");
      
      // register our validation methods so that they are called by general validation methods in scripts.js
      out.write("registerPropertySheetValidator(" + prefix + "validateSubmit, '" + form.getClientId(context) + "', '"
              + getFinishButtonId() + "', '" + (this.nextButtonId != null ? this.nextButtonId : "") + "');\n");
      
      // close out the script block
      out.write("</script>\n");
   }

   private void renderValidationMethod(ResponseWriter out, ClientValidation validation,
         boolean lastMethod, boolean showMessage, boolean useNegative) throws IOException
   {
      if (useNegative) {
      out.write("!");
      }
      out.write(validation.Type);
      out.write("(");
      
      // add the parameters
      int numberParams = validation.Params.size();
      for (int p = 0; p < numberParams; p++)
      {
         out.write(validation.Params.get(p));
         if (p != (numberParams-1))
         {
            out.write(", ");
         }
      }
      
      // add the parameter to show any validation messages
      out.write(", ");
      out.write(Boolean.toString(showMessage));
      out.write(")");
      
      final String lineSeparator;
      if(logger.isInfoEnabled()) {
          lineSeparator = "\n"; //pretty print
      } else {
          lineSeparator = ""; //no line separator
      }
      
      if (lastMethod)
      {
         out.write(")");
      }
      else
      {
          if (useNegative) {
         out.write(lineSeparator + " || ");
      }
          else {
              out.write(lineSeparator + " && ");
          }
      }
   }
   
   /**
    * Creates all the property components required to display the properties held by the node.
    * 
    * @param context JSF context
    * @param node The Node to show all the properties for 
    * @throws IOException
    */
   @SuppressWarnings("unchecked")
   private void createComponentsFromNode(FacesContext context, Node node)
      throws IOException
   {
      // add all the properties of the node to the UI
      Map<String, Object> props = node.getProperties();
      for (String propertyName : props.keySet())
      {
         // create the property component
         UIProperty propComp = (UIProperty)context.getApplication().
               createComponent(RepoConstants.ALFRESCO_FACES_PROPERTY);
         
         // get the property name in it's prefix form
         QName qname = QName.createQName(propertyName);
         String prefixPropName = qname.toPrefixString();
         
         FacesHelper.setupComponentId(context, propComp, PROP_ID_PREFIX + prefixPropName);
         propComp.setName(prefixPropName);
         
         // if this property sheet is set as read only, set all properties to read only
         if (isReadOnly())
         {
            propComp.setReadOnly(true);
         }
         
         // NOTE: we don't know what the display label is so don't set it
         
         this.getChildren().add(propComp);
         
         if (logger.isDebugEnabled())
            logger.debug("Created property component " + propComp + "(" + 
                   propComp.getClientId(context) + 
                   ") for '" + prefixPropName +
                   "' and added it to property sheet " + this);
      }
      
      // add all the associations of the node to the UI
      Map associations = node.getAssociations();
      Iterator iter = associations.keySet().iterator();
      while (iter.hasNext())
      {
         String assocName = (String)iter.next();
         UIAssociation assocComp = (UIAssociation)context.getApplication().
               createComponent(RepoConstants.ALFRESCO_FACES_ASSOCIATION);
         
         // get the association name in it's prefix form
         QName qname = QName.createQName(assocName);
         String prefixAssocName = qname.toPrefixString();
         
         FacesHelper.setupComponentId(context, assocComp, ASSOC_ID_PREFIX + prefixAssocName);
         assocComp.setName(prefixAssocName);
         
         // if this property sheet is set as read only, set all properties to read only
         if (isReadOnly())
         {
            assocComp.setReadOnly(true);
         }
         
         // NOTE: we don't know what the display label is so don't set it
         
         this.getChildren().add(assocComp);
         
         if (logger.isDebugEnabled())
            logger.debug("Created association component " + assocComp + "(" + 
                   assocComp.getClientId(context) + 
                   ") for '" + prefixAssocName +
                   "' and added it to property sheet " + this);
      }
      
      // add all the child associations of the node to the UI
      Map childAssociations = node.getChildAssociations();
      iter = childAssociations.keySet().iterator();
      while (iter.hasNext())
      {
         String assocName = (String)iter.next();
         UIChildAssociation childAssocComp = (UIChildAssociation)context.getApplication().
               createComponent(RepoConstants.ALFRESCO_FACES_CHILD_ASSOCIATION);
         FacesHelper.setupComponentId(context, childAssocComp, ASSOC_ID_PREFIX + assocName);
         childAssocComp.setName(assocName);
         
         // if this property sheet is set as read only, set all properties to read only
         if (isReadOnly())
         {
            childAssocComp.setReadOnly(true);
         }
         
         // NOTE: we don't know what the display label is so don't set it
         
         this.getChildren().add(childAssocComp);
         
         if (logger.isDebugEnabled())
            logger.debug("Created child association component " + childAssocComp + "(" + 
                   childAssocComp.getClientId(context) + 
                   ") for '" + assocName +
                   "' and added it to property sheet " + this);
      }
   }
   
   /**
    * Creates all the property components required to display the properties specified
    * in an external config file.
    * 
    * @param context JSF context
    * @param properties Collection of properties to render (driven from configuration) 
    * @throws IOException
    */
   @SuppressWarnings("unchecked")
   protected void createComponentsFromConfig(FacesContext context, Collection<ItemConfig> items)
      throws IOException
   {
      for (ItemConfig item : items)
      {
         Pair<PropertySheetItem, String> itemAndId = createPropertySheetItemAndId(item, context);
         if (itemAndId == null) {
            throw new IllegalArgumentException("Unknown ItemConfig type '" + item.getClass().getCanonicalName() + "': " + item);
         }
         final String id = itemAndId.getSecond();
         final PropertySheetItem propSheetItem = itemAndId.getFirst();
         
         // now setup the common stuff across all component types
         if (propSheetItem != null)
         {
            changePropSheetItem(item, propSheetItem);
            FacesHelper.setupComponentId(context, propSheetItem, id);
            propSheetItem.setName(item.getName());
            propSheetItem.setConverter(item.getConverter());
            propSheetItem.setComponentGenerator(item.getComponentGenerator());
            propSheetItem.setIgnoreIfMissing(item.getIgnoreIfMissing());
   
            String displayLabel = item.getDisplayLabel();
            if (item.getDisplayLabelId() != null)
            {
               String label = Application.getMessage(context, item.getDisplayLabelId());
               if (label != null)
               {
                  displayLabel = label; 
               }
            }
            propSheetItem.setDisplayLabel(displayLabel);
            
            // if this property sheet is set as read only or the config says the property
            // should be read only set it as such
            if (isReadOnly() || item.isReadOnly())
            {
               propSheetItem.setReadOnly(true);
            }
            
            this.getChildren().add(propSheetItem);
            
            if (logger.isDebugEnabled())
               logger.debug("Created property sheet item component " + propSheetItem + "(" + 
                      propSheetItem.getClientId(context) + 
                      ") for '" + item.getName() + 
                      "' and added it to property sheet " + this);
         }
      }
   }
   
/**
    * Inner class representing a validation case that must be enforced.
    */
   @SuppressWarnings("serial") 
   public static class ClientValidation implements Serializable
   {
      public String Type;
      public List<String> Params;
      public boolean RealTimeChecking;
      
      /**
       * Default constructor
       * 
       * @param type The type of the validation
       * @param params A List of String parameters to use for the validation
       * @param realTimeChecking true to check the property sheet in real time
       *        i.e. as the user types or uses the mouse
       */
      public ClientValidation(String type, List<String> params, boolean realTimeChecking)
      {
         this.Type = type;
         this.Params = params;
         this.RealTimeChecking = realTimeChecking;
      }
      
      @Override
    public String toString() {
        return (RealTimeChecking ? "+" : "-")+Type+Params+"\n";
    }
   }
   
    /**
     * Allow changes to be made to this propSheetItem
     * 
     * @param item - configuration item
     * @param propSheetItem - item to be changed
     */
    protected void changePropSheetItem(ItemConfig item, PropertySheetItem propSheetItem) {
        // Alfresco implementation doesn't need this, but WM subclass needs it
    }

    /**
     * create the appropriate component
     * 
     * @param item
     * @param context
     * @return Pair&lt;PropertySheetItem, String&gt; or null if this class don't know how to create PropertySheetItem out of given item, and subclass should
     *         create the PropertySheetItem and id pair
     */
    protected Pair<PropertySheetItem, String> createPropertySheetItemAndId(ItemConfig item, FacesContext context) {
        final String id;
        final PropertySheetItem propSheetItem;
        if (item instanceof PropertyConfig) {
            id = PROP_ID_PREFIX + item.getName();
            propSheetItem = (PropertySheetItem) context.getApplication().
                    createComponent(RepoConstants.ALFRESCO_FACES_PROPERTY);
        } else if (item instanceof AssociationConfig) {
            id = ASSOC_ID_PREFIX + item.getName();
            propSheetItem = (PropertySheetItem) context.getApplication().
                    createComponent(RepoConstants.ALFRESCO_FACES_ASSOCIATION);
        } else if (item instanceof ChildAssociationConfig) {
            id = ASSOC_ID_PREFIX + item.getName();
            propSheetItem = (PropertySheetItem) context.getApplication().
                    createComponent(RepoConstants.ALFRESCO_FACES_CHILD_ASSOCIATION);
        } else if (item instanceof SeparatorConfig) {
            id = SEP_ID_PREFIX + item.getName();
            propSheetItem = (PropertySheetItem) context.getApplication().
                    createComponent(RepoConstants.ALFRESCO_FACES_SEPARATOR);
        } else {
            return null; // subclass might have a solution for this item type
        }
        return new Pair<PropertySheetItem, String>(propSheetItem, id);
    }

    @Override
    public String getAjaxClientId(FacesContext context) {
        return getClientId(getFacesContext()) + "_container";
    }

}
