//@formatter: off
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

import static ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty.REPO_NODE;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.ATTR_FORCED_MANDATORY;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.VALDIATION_DISABLED;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.VALIDATION_MARKER_DISABLED;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIGraphic;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.validator.Validator;

import org.alfresco.repo.dictionary.constraint.AbstractConstraint;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.repo.dictionary.constraint.NumericRangeConstraint;
import org.alfresco.repo.dictionary.constraint.RegexConstraint;
import org.alfresco.repo.dictionary.constraint.StringLengthConstraint;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.Constraint;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.DataDictionary;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.BaseAssociationEditor;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet.ClientValidation;
import org.alfresco.web.ui.repo.component.property.UISeparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.converter.ListNonBlankStringsWithCommaConverter;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;
import ee.webmedia.alfresco.common.propertysheet.validator.ForcedMandatoryValidator;
import ee.webmedia.alfresco.common.propertysheet.validator.MandatoryIfValidator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;

public abstract class BaseComponentGenerator implements IComponentGenerator, CustomAttributes
{
   private static Log logger = LogFactory.getLog(BaseComponentGenerator.class);
   
   protected enum ControlType { FIELD, SELECTOR; }
   
   private DataDictionary dataDictionary;
   
   protected Map<String, String> propertySheetItemAttributes;
   
   public static final String READONLY_IF = "readOnlyIf";
   public static final String RENDERED = "rendered";
   /** should control component of propertySheet item be disabled */
   private static final String DISABLED = "disabled";
   
   /**
    * when using readOnlyIf attribute on subPropertySheet, you can refer to property of some ancestor node using this constant. For example <br>
    * <code>readOnlyIf="parent.parent.doccom:docStatus=lõpetatud||peatatud"</code>
    */
   private static final String READONLY_IF_PARENT_IDENTIFIER = "parent";

   public static final String OUTPUT_TEXT = "outputText";

    /**
     * Custom attributes that are used accross many subclasses
     */
    public interface CustomAttributeNames {
        String STYLE_CLASS = "styleClass";
        String ATTR_FORCED_MANDATORY = "forcedMandatory";
        String VALDIATION_DISABLED = "validationDisabled";
        //enable to show mandatory marker independently of VALDIATION_DISABLED value
        String VALIDATION_MARKER_DISABLED = "validationMarkerDisabled";
    }
    
    public interface CustomConstants {
        String VALUE_INDEX_IN_MULTIVALUED_PROPERTY = "VALUE_INDEX_IN_MULTIVALUED_PROPERTY";
    }
   
   @SuppressWarnings("unchecked")
   public UIComponent generateAndAdd(FacesContext context, UIPropertySheet propertySheet, 
         PropertySheetItem item)
   {
      UIComponent component = null;
      
      if (item instanceof UIProperty)
      {

          // get the property definition
          PropertyDefinition propertyDef = getPropertyDefinition(context,
                propertySheet.getNode(), item.getName());          
          
          if (!isComponentRendered(component, item, context, propertySheet, propertyDef)) {
              return component;
          }

         saveExistingValue4ComponentGenerator(context, propertySheet.getNode(), item.getName());
         
         // create the component and add it to the property sheet
         component = createComponent(context, propertySheet, item);
         
         // setup the component for multi value editing if necessary
         component = setupMultiValuePropertyIfNecessary(context, propertySheet, 
               item, propertyDef, component);
         
         // setup common aspects of the property i.e. value binding
         setupProperty(context, propertySheet, item, propertyDef, component);
         
         // add the component now, it needs to be added before the validations
         // are setup as we need access to the component id, which in turn needs 
         // to have a parent to get the correct id
         item.getChildren().add(component);

         if (!isCreateOutputText(context)) {
             // setup the component for mandatory validation if necessary
             setupMandatoryPropertyIfNecessary(context, propertySheet, item, propertyDef, component);

             setupForceMandatory(context, propertySheet, item,  propertyDef, component);

             // setup any constraints the property has
             setupConstraints(context, propertySheet, item, propertyDef, component); 
         }
         
         // setup any converter the property needs
         setupConverter(context, propertySheet, item, propertyDef, component);
      }
      else if (item instanceof UISeparator)
      {
         // just create the component and add it
         component = createComponent(context, propertySheet, item);
         item.getChildren().add(component);
      }
      else
      {
         // get the association definition
         AssociationDefinition assocationDef = this.getAssociationDefinition(context, 
               propertySheet.getNode(), item.getName());
         
         // create the component and add it to the property sheet
         component = createComponent(context, propertySheet, item);
         
         // setup common aspects of the association i.e. value binding
         setupAssociation(context, propertySheet, item, assocationDef, component);
         
         // add the component now, it needs to be added before the validations
         // are setup as we need access to the component id, which needs have a
         // parent to get the correct id
         item.getChildren().add(component);
         
         // setup the component for mandatory validation if necessary
         setupMandatoryAssociationIfNecessary(context, propertySheet, item, 
               assocationDef, component);
         
         // setup any converter the association needs
         setupConverter(context, propertySheet, item, assocationDef, component);
      }
      
      if (component instanceof UIInput && propertySheet.inEditMode()) {
          addMandatoryIfValidator(context, (UIInput) component);
          addCustomValidator((UIInput) component);
      }
      
      setReadOnlyBasedOnExpressionIfNessesary(component, item, context, propertySheet);
      setDisabledBasedOnAttribute(component, item, context);

      processCustomAttributes(context, component);

      return component;
   }

    protected void processCustomAttributes(FacesContext context, UIComponent component) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();

        String styleClass = getCustomAttributes().get(STYLE_CLASS);
        if (isNotBlank(styleClass)) {
            final String existingStyleClass = (String) attributes.get(STYLE_CLASS);
            if (isNotBlank(existingStyleClass)) {
                logger.warn("component already has existing styleclass set from code ("+existingStyleClass+"), adding styleclass also from property-sheet: "+styleClass);
                attributes.put(STYLE_CLASS, existingStyleClass+" "+styleClass);
            } else {
                attributes.put(STYLE_CLASS, styleClass);
            }
        }
        
        String dontRenderIfDisabled = getCustomAttributes().get(WMUIProperty.DONT_RENDER_IF_DISABLED_ATTR);
        if (org.apache.commons.lang.StringUtils.isNotEmpty(dontRenderIfDisabled)) {
            attributes.put(WMUIProperty.DONT_RENDER_IF_DISABLED_ATTR, Boolean.parseBoolean(dontRenderIfDisabled));
        }
    }

   /**
    * Add MandatoryIfValidator to given component if property-sheet/show-property defines another property that is used to detect whether or not this property should be filled.
    * 
    * @param context FacesContext
    * @param component
    * @author Ats Uiboupin 
    */
    protected void addMandatoryIfValidator(FacesContext context, UIInput component) {
        if (isValidationDisabled()) {
            return;
        }
        String mandatoryIfValue = (propertySheetItemAttributes != null ? propertySheetItemAttributes.get(MandatoryIfValidator.ATTR_MANDATORY_IF) : null);
        if (isNotBlank(mandatoryIfValue)) {
            MandatoryIfValidator validator = new MandatoryIfValidator(mandatoryIfValue);
            String mandatoryIfLabelId = (propertySheetItemAttributes != null ? propertySheetItemAttributes.get(MandatoryIfValidator.ATTR_MANDATORY_IF_LABEL_ID) : null);
            validator.setMandatoryIfLabelId(mandatoryIfLabelId);
            boolean allMandatory = (propertySheetItemAttributes != null ? Boolean.valueOf(propertySheetItemAttributes.get(MandatoryIfValidator.ATTR_MANDATORY_IF_ALL_MANDATORY)) : false);
            validator.setAllMandatory(allMandatory);
            component.addValidator(validator);
            if (logger.isDebugEnabled()) {
                logger.debug("Adding MandatoryIfValidator based on property with name '" + mandatoryIfValue + "' to component with id '" + component.getId()
                        + "'. Validator: " + validator);
            }
        }
    }

    private void addCustomValidator(UIInput component) {
        String validatorBeanName = getCustomAttributes().get("validator");
        if(!StringUtils.hasText(validatorBeanName)) {
            return;
        }
        try {
            Validator valdiator = BeanHelper.getSpringBean(Validator.class, validatorBeanName);
            component.addValidator(valdiator);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create valdiator class from validator='"+validatorBeanName+"'", e);
        }
        
    }

    /**
     * Add mandatory validation if property-sheet/show-property has attribute <code>forcedMandatory="true"</code>
     * 
     * @param context
     * @param item
     * @param propertyDef
     * @param component
     */
    protected void setupForceMandatory(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        if (propertySheet.inEditMode() && propertySheet.isValidationEnabled()) {
            if (propertyDef != null && propertyDef.isMandatory()) {
                return;
            }
            if (isForceMandatory(context) && component instanceof UIInput) {
                UIInput uiInput = (UIInput) component;
                setupMandatoryValidation(context, propertySheet, item, component, true, null);
                uiInput.addValidator(new ForcedMandatoryValidator());
            }
        }
    }

    private boolean isForceMandatory(FacesContext context) {
        String strAttr = getCustomAttributes().get(ATTR_FORCED_MANDATORY);
        if(isNotBlank(strAttr)) {
            return evaluateBoolean(strAttr, context);
        }
        return false;
    }

    /**
     * @param component - UIComponent that could be disabled if expression from "readOnlyIf" attribute in property-sheet/show-property element is fulfilled
     * @param item - property-sheet/show-property element that might have custom attribute called "readOnlyIf" with expression that is for deciding if this
     *            component should be disabled or not.
     * @param context - FacesContext
     * @param propertySheet - UIPropertySheet where this <code>component</code> is located
     * @author Ats Uiboupin
     */
    private void setReadOnlyBasedOnExpressionIfNessesary(UIComponent component, PropertySheetItem item, FacesContext context, UIPropertySheet propertySheet) {
        if (item instanceof CustomAttributes) {
            String expression = ((CustomAttributes) item).getCustomAttributes().get(READONLY_IF);
            if (isNotBlank(expression)) {
                boolean isReadonly = checkCustomPropertyExpression(context, propertySheet, expression, READONLY_IF, item.getName());
                if (isReadonly) {
                    ComponentUtil.setReadonlyAttributeRecursively(component);
                }
            } else if (Boolean.valueOf(((CustomAttributes) item).getCustomAttributes().get("read-only"))) {
                ComponentUtil.setReadonlyAttributeRecursively(component);
            }
        }
    }

    private void setDisabledBasedOnAttribute(UIComponent component, PropertySheetItem item, FacesContext context) {
        String isDisabledEpression = ((CustomAttributes) item).getCustomAttributes().get(DISABLED);
        if (isNotBlank(isDisabledEpression) && evaluateBoolean(isDisabledEpression, context, item)) {
            ComponentUtil.setReadonlyAttributeRecursively(component);
        }
    }

    private boolean evaluateBoolean(String expression, FacesContext context) {
        return evaluateBoolean(expression, context, null);
    }

    public static boolean evaluateBoolean(String expression, FacesContext context, PropertySheetItem item) {
        if ("true".equals(expression)) {
            return true;
        }
        if ("false".equals(expression)) {
            return false;
        }
        try {
            return ((Boolean) context.getApplication().createMethodBinding(expression, new Class[] {}).invoke(context, null)).booleanValue();
        } catch (EvaluationException e) { // Fall back to value binding to allow negation - for example readOnlyIf="#{!FieldDetailsDialog.field.parameterInVolSearch}"
            try {
                ValueBinding vb = context.getApplication().createValueBinding(expression);
                return (Boolean) vb.getValue(context);
            } catch (EvaluationException e2) {
                try {
                    // decide based on method binding that accepts PropertySheetItem
                    MethodBinding isDisabledMB = context.getApplication().createMethodBinding(expression, new Class[] { PropertySheetItem.class });
                    return ((Boolean) isDisabledMB.invoke(context, new Object[] { item })).booleanValue();
                } catch (Exception e1) {
                    long msgId = System.currentTimeMillis();
                    logger.info(msgId+" - evaluation attempt 1 failed (using MB)", e);
                    logger.info(msgId+" - evaluation attempt 2 failed (using VB)", e2);
                    logger.error(msgId+" - evaluation attempt 3 failed (using MB with PropertySheetItem)", e1);
                    throw new RuntimeException(msgId+" Failed to evaluate expression '" + expression + "' 3 times (see causes from the log above)", e1);
                }
            }
        }
    }

    private boolean isComponentRendered(UIComponent component, PropertySheetItem item, FacesContext context, UIPropertySheet propertySheet, PropertyDefinition propertyDef) {
        if (item instanceof CustomAttributes) {
            String expression = ((CustomAttributes) item).getCustomAttributes().get(RENDERED);
            if (isNotBlank(expression)) {
                    return checkCustomPropertyExpression(context, propertySheet, expression, RENDERED, item.getName());
            }
        }
        return true;
    }

    /**
     * Refactored custom property expression checking into separate method. 
     * Evaluates expressions in format: "somePrefix:someProperty=someValue||otherValue;someOtherPrefix:someOtherProperty=someOtherValue||stillOtherValue". 
     * 
     * @param context
     * @param propertySheet
     * @param <code>expression := valueExpression | methodBinding
     * <code>valueExpression := expr + (";" + expr) *</code><br>
     * <code>expr := <code>propertyPath + ("=" | "!=") + staticValue1 + ( "||" + staticValue ) *</code><br>
     * <code>propertyPath := "parent." * + propName</code> <br>
     * <code>propName</code> - qName of the property to be evaluated
     * <code>methodBinding := "#{" + className + "." + methodName + "}"</code>
     * <code>className</code> - valid class name
     * <code>methodName</code> - valid method in the class className
     * @param fieldName
     * @param itemName
     * @return true, if any of expression parts (expr) evaluate to true based on property value from node. <br>
     * If using equals sign "=" then true will be returned if property value matches at least one of the given static values. <br>
     * If using negotation "!=" then true will be returned if none of the given static values match actual property value.
     */
    protected boolean checkCustomPropertyExpression(FacesContext context, UIPropertySheet propertySheet, String expression, String fieldName, String itemName) {
        final String errMsg = fieldName + " must be defined as 'somePrefix:someProperty=someValue||otherValue;someOtherPrefix:someOtherProperty=someOtherValue||stillOtherValue' or '#{ClassName.methodName}'";
        if(StringUtils.startsWithIgnoreCase(expression, "#{")){
            return evaluateBoolean(expression, context);
        }
        String[] split = expression.split(";");
        for (int i = 0; i < split.length; i++) {
            if (checkSinglePropertyExpression(context, propertySheet, split[i], fieldName, itemName, errMsg)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSinglePropertyExpression(FacesContext context, UIPropertySheet propertySheet, String expression, String fieldName, String itemName,
            final String errMsg) {
        String[] split = expression.split("=");
        if (split.length != 2) {
            throw new IllegalArgumentException(errMsg);
        }
        String propPath = split[0];
        boolean checkNotEqual = false;
        if (propPath.endsWith("!")) {
            checkNotEqual = true;
            propPath = propPath.substring(0, propPath.length() - 1);
        }
        String valueExpression = split[1];
        // try to figure out the node and the property, that should be inspected
        final Pair<Node, String> propNameAndNode = getBaseNodeAndPropName(propertySheet, expression, propPath); //XXX find basenode(can't use propertySheet.getVar() when dealing with subpropertysheets)
        Node propNode = propNameAndNode.getFirst();
        String propertyName = propNameAndNode.getSecond();
        PropertyDefinition propDef = getDataDictionary(context).getPropertyDefinition(propNode, propertyName);
        if (propDef == null) {
            throw new IllegalArgumentException("Can't evaluate conditional property '" + fieldName + "' based on attribute '" + expression
                    + "' for propertySheetItem '" + itemName + "', as given node with path: '" + propNode.getPath()
                    + "' has no property with propertyName='" + propertyName + "'.\nNode: " + propNode);
        }
        Object objectValue = propNode.getProperties().get(propDef.getName());
        String actualValue = DefaultTypeConverter.INSTANCE.convert(String.class, objectValue);
        String[] expressionValues = valueExpression.split("\\|\\|");
        boolean result = checkNotEqual ? true : false;
        for (String expressionValue : expressionValues) {
            if (expressionValue.equals(actualValue)) {
                result = !result;
                break;
            }
        }
        return result;
    }
    
    protected void saveExistingValue4ComponentGenerator(FacesContext context, Node node, String propertyName) {
        // subclasses can save value of existing property (for example to context) based on
        // propertyName and value corresponding to propertyName from node properties.
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        requestMap.put(REPO_NODE, new Object[] { node, propertyName });
    }

    private Integer getValueIndexInMultivaluedProperty(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        return (Integer) requestMap.get(VALUE_INDEX_IN_MULTIVALUED_PROPERTY);
    }

    /**
     * @return true if this componentGenerator is generated by another componentGenerator(for example by InlinePropertyGroupGenerator or MultiValueEditor)
     */
    private boolean isChildFromGeneratorsWrapper() {
        final String valueIndexStr = getCustomAttributes().get(VALUE_INDEX_IN_MULTIVALUED_PROPERTY);
        if (isNotBlank(valueIndexStr)) {
            return true;
        }
        return false;
    }

    /**
     * Multivalued property should be treated as single valued property for components
     * that get bound to single value of multivalued property - for example a component in multivalueeditor
     */
    protected boolean isSingleValued(FacesContext context, boolean multiValuedProperty) {
        boolean isSingleValued = !multiValuedProperty;
        if (multiValuedProperty) {
            final Integer valueIndexInMultivaluedProperty = getValueIndexInMultivaluedProperty(context);
            isSingleValued = null != valueIndexInMultivaluedProperty && valueIndexInMultivaluedProperty >= 0;
        }
        return isSingleValued;
    }

    protected boolean isValidationDisabled() {
        return Boolean.valueOf(getCustomAttributes().get(VALDIATION_DISABLED));
    }
    
    protected boolean isMandatoryMarkerDisabled() {
        String valMarkerDisabledStr = getCustomAttributes().get(VALIDATION_MARKER_DISABLED);
        if(org.apache.commons.lang.StringUtils.isNotBlank(valMarkerDisabledStr)){
            return Boolean.valueOf(valMarkerDisabledStr);
        }
        return isValidationDisabled();
    }

    protected String getIdPrefix() {
        return "";
    }

    protected String getIdInfix(PropertySheetItem property) {
        return property.getName();
    }
    
    protected String getDefaultId(PropertySheetItem propertySheetItem) {
        return getIdPrefix() + getIdInfix(propertySheetItem) + getIdSuffix();
    }

    protected String getIdSuffix() {
        final Integer valueIndexInMultivaluedProperty = getValueIndexInMultivaluedProperty(FacesContext.getCurrentInstance());
        return (valueIndexInMultivaluedProperty != null && valueIndexInMultivaluedProperty >= 0) ? "_"+valueIndexInMultivaluedProperty : "";
    }
   
    protected void addValueFromCustomAttributes(String key, Map<String, Object> attributes) {
        addValueFromCustomAttributes(key, attributes, null);
    }

    protected void addValueFromCustomAttributes(String key, Map<String, Object> attributes, Class<?> clazz) {
        addValueFromCustomAttributes(key, attributes, clazz, null);
    }

    protected <T> void addValueFromCustomAttributes(String key, Map<String, Object> attributes, Class<T> clazz, T defaultValue) {
        if (getCustomAttributes().containsKey(key)) {
            Object value = getCustomAttributes().get(key);
            if (clazz != null) {
                value = DefaultTypeConverter.INSTANCE.convert(clazz, value);
            }
            if (value == null) {
                value = defaultValue;
            }
            attributes.put(key, value);
        } else if (defaultValue != null) {
            attributes.put(key, defaultValue);
        }
    }

    private Pair<Node, String> getBaseNodeAndPropName(UIPropertySheet propertySheet, String readOnlyIfExpression, String propPath) {
        Node propNode = propertySheet.getNode();
        String propertyName = propPath;
        final String[] propPathTokens = propPath.split("\\.");
        UIPropertySheet ancestorPropSheet = propertySheet;
        for (int i = 0; i < propPathTokens.length; i++) {
            final String token = propPathTokens[i];
            if (i == propPathTokens.length - 1) {
                propertyName = token;
            } else {
                if (org.apache.commons.lang.StringUtils.equals(READONLY_IF_PARENT_IDENTIFIER, token)) {
                    ancestorPropSheet = ComponentUtil.getAncestorComponent(ancestorPropSheet.getParent(), UIPropertySheet.class, true);
                    if (ancestorPropSheet == null) {
                        throw new IllegalArgumentException((i + 1) + ". 'parent.' expression in readOnlyIf=\"" + readOnlyIfExpression
                                + "\" refers to parent propertySheet that is not present");
                    }
                    propNode = ancestorPropSheet.getNode();
                } else {
                    throw new RuntimeException("Unknown path token '" + token + "' in conditional readOnly expression '" + readOnlyIfExpression + "'");
                }
            }
        }
        return new Pair<Node, String>(propNode, propertyName);
    }
    
   /**
    * Creates the component for the given proerty sheet item.
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param item The property or association being generated
    * @return The newly created component
    */
   protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet,
         PropertySheetItem item)
   {
      UIComponent component = null;
      
    final String id = getDefaultId(item);
    if (item instanceof UIProperty)
      {
         if (useGenerator(context, propertySheet))
         {
            // use the standard component in edit mode
            component = generate(context, id);
         }
         else
         {
            // create an output text component in view mode
            component = createOutputTextComponent(context, id);
         }
      }
      else
      {
         // create the standard association component
         component = generate(context, id);
      }
      
      return component;
   }

    protected boolean useGenerator(FacesContext context, UIPropertySheet propertySheet) {
        return (propertySheet.inEditMode() || this instanceof HandlesViewMode || isAlwaysEdit(context)) && !isCreateOutputText(context);
    }

    protected boolean isCreateOutputText(FacesContext context) {
        String outputTextAttr = getCustomAttributes().get(OUTPUT_TEXT);
        return isNotBlank(outputTextAttr) ? evaluateBoolean(outputTextAttr, context) : false;
    }

    protected boolean isAlwaysEdit(FacesContext context) {
        String alwaysEdit = getCustomAttributes().get(ComponentUtil.IS_ALWAYS_EDIT);
        return isNotBlank(alwaysEdit) ? evaluateBoolean(alwaysEdit, context) : false;
    }
   
   /**
    * Creates the converter with the given id and adds it to the component.
    * 
    * @param context FacesContext
    * @param converterId The name of the converter to create
    * @param component The component to add the converter to
    */
   protected void createAndSetConverter(FacesContext context, String converterId, 
         UIComponent component)
   {
      if (converterId != null && component instanceof UIOutput)
      {
         try
         {
            Converter conv = context.getApplication().createConverter(converterId);
            ((UIOutput)component).setConverter(conv);
         }
         catch (NullPointerException npe)
         {
            // workaround a NPE bug in MyFaces
            logger.warn("Converter " + converterId + " could not be applied to component: " + component.getId());
         }
         catch (FacesException fe)
         {
            logger.warn("Converter " + converterId + " could not be applied to component: " + component.getId());
         }
      }
   }
   
   /**
    * Creates an output text component.
    * 
    * @param context FacesContext
    * @param id Optional id to set
    * @return The new component
    */
   protected UIOutput createOutputTextComponent(FacesContext context, String id)
   {
      UIOutput component = (UIOutput)context.getApplication().createComponent(
            ComponentConstants.JAVAX_FACES_OUTPUT);
      
      component.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
      FacesHelper.setupComponentId(context, component, id);
      
      return component;
   }
   
   /**
    * Sets up the property component i.e. setting the value binding
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet
    * @param item The parent component
    * @param propertyDef The property definition
    * @param component The component representing the property
    */
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet,
            PropertySheetItem item, PropertyDefinition propertyDef, UIComponent component) {
        // can contain index between square brackets or empty string, but not null - used with MultiValueEditor
        final ValueBinding vb = createValueBinding(context, propertySheet, item, propertyDef);
        component.setValueBinding("value", vb);

        // disable the component if it is read only or protected
        // or if the property sheet is in view mode
        if (!isAlwaysEdit(context) && (!propertySheet.inEditMode() || item.isReadOnly() ||
                (propertyDef != null && propertyDef.isProtected()))) {
            ComponentUtil.setReadonlyAttributeRecursively(component);
        }
    }

    public ValueBinding createValueBinding(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef) {
        String valueIndexSuffix = "";
        final Integer valueIndex = getValueIndexInMultivaluedProperty(context);
        if (propertyDef != null && propertyDef.isMultiValued()) {
            if (valueIndex != null && valueIndex >= 0) {
                valueIndexSuffix = "[" + valueIndex + "]";
            }
        }

        final String propKey;
        if (propertyDef != null) {
            propKey = propertyDef.getName().toString();
        } else {
            propKey = item.getName();
        }
        String binding = ComponentUtil.getValueBindingFromSubPropSheet(context, propertySheet, propKey, valueIndexSuffix);

        if (binding == null) {
            // property is directly on propertySheet, not on nested propertySheet
            binding = "#{" + propertySheet.getVar() + ".properties[\"" + propKey + "\"]" + valueIndexSuffix + "}";
        }
        final ValueBinding vb = context.getApplication().createValueBinding(binding);
        return vb;
    }

   /**
    * Sets up the association component i.e. setting the value binding
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet
    * @param item The parent component
    * @param associationDef The association definition
    * @param component The component representing the association
    */
   protected void setupAssociation(FacesContext context, UIPropertySheet propertySheet,
         PropertySheetItem item, AssociationDefinition associationDef, UIComponent component)
   {
      // create and set the value binding
      ValueBinding vb = context.getApplication().createValueBinding(
            "#{" + propertySheet.getVar() + "}");
      component.setValueBinding("value", vb);
      
      // set the association name and set to disabled if appropriate
      ((BaseAssociationEditor)component).setAssociationName(
            associationDef.getName().toString());
      
      // disable the component if it is read only or protected
      // or if the property sheet is in view mode
      if (propertySheet.inEditMode() == false || item.isReadOnly() || 
              (associationDef != null && associationDef.isProtected())) 
      {
         ComponentUtil.setReadonlyAttributeRecursively(component);
      }
   }
   
   /**
    * Creates a wrapper component around the given component to enable the user
    * to edit multiple values.
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param property The property being generated
    * @param propertyDef The data dictionary definition for the property
    * @param component The component representing the property
    * @return A wrapped component if the property is multi-valued or the 
    *         original component if it is not multi-valued
    */
   @SuppressWarnings("unchecked")
   protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      
      final boolean childOfMultivaluedProperty = isChildFromGeneratorsWrapper();
    if (propertySheet.inEditMode() && property.isReadOnly() == false && 
          propertyDef != null && propertyDef.isProtected() == false &&
          propertyDef.isMultiValued() && !childOfMultivaluedProperty)
      {
         // if the property is multi-valued create a multi value editor wrapper component
         String id = "multi_" + getDefaultId(property);
         UIComponent multiValueComponent = context.getApplication().createComponent(
               RepoConstants.ALFRESCO_FACES_MULTIVALUE_EDITOR);
         FacesHelper.setupComponentId(context, multiValueComponent, id);
            
         // set the renderer depending on whether the item is a 'field' or a 'selector'
         if (getControlType() == ControlType.FIELD)
         {
            multiValueComponent.setRendererType(RepoConstants.ALFRESCO_FACES_FIELD_RENDERER);
         }
         else
         {
            multiValueComponent.setRendererType(RepoConstants.ALFRESCO_FACES_SELECTOR_RENDERER);
            
            // set the value binding for the wrapped component and the lastItemAdded attribute of
            // the multi select component, needs to point somewhere that can hold any object, it
            // will store the item last added by the user.
            String expr = "#{MultiValueEditorBean.lastItemsAdded['" +
                  property.getName() + "']}";
            ValueBinding vb = context.getApplication().createValueBinding(expr);
            multiValueComponent.setValueBinding("lastItemAdded", vb);
            component.setValueBinding("value", vb);
         }
         
         // add the original component as a child of the wrapper
         multiValueComponent.getChildren().add(component);
         return multiValueComponent;
      } else if (childOfMultivaluedProperty) {
         if(!component.getId().endsWith(getIdSuffix())) {
             FacesHelper.setupComponentId(context, component, getIdPrefix() + component.getId() + getIdSuffix());
         }
      }
      return component;
   }

   /**
    * Sets up a mandatory validation rule for the given property.
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param property The property being generated
    * @param propertyDef The data dictionary definition of the property
    * @param component The component representing the property
    */
   protected void setupMandatoryPropertyIfNecessary(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      // only setup validations if the property sheet is in edit mode,
      // validation is enabled and the property is declared as mandatory
        if (propertySheet.inEditMode() && propertySheet.isValidationEnabled() &&
                (isForceMandatory(context)
                || (propertyDef != null && propertyDef.isMandatory())))
      {
         setupMandatoryValidation(context, propertySheet, property, component, false, null);
         setupMandatoryMarker(context, property);
      }
   }

   /**
    * Sets up a mandatory validation rule for the given association.
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param association The association being generated
    * @param associationDef The data dictionary definition of the association
    * @param component The component representing the association
    */
   protected void setupMandatoryAssociationIfNecessary(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem association, 
         AssociationDefinition associationDef, UIComponent component)
   {
      // only setup validations if the property sheet is in edit mode,
      // validation is enabled and the association is declared as mandatory
      if (propertySheet.inEditMode() && propertySheet.isValidationEnabled() &&
          associationDef != null && associationDef.isTargetMandatory())
      {
         setupMandatoryValidation(context, propertySheet, association, component, false, null);
         setupMandatoryMarker(context, association);
      }
   }
   
   /**
    * Sets up a client mandatory validation rule with the property
    * sheet for the given item.
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet to add the validation rule to
    * @param item The item being generated
    * @param component The component representing the item
    * @param realTimeChecking true to make the client validate as the user types
    * @param idSuffix An optional suffix to add to the client id
    */
   protected void setupMandatoryValidation(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem item, 
         UIComponent component, boolean realTimeChecking,
         String idSuffix)
   {
       if(isValidationDisabled()) {
           return;
       }
       if(isChildFromGeneratorsWrapper()) {
           realTimeChecking = false; // show errors only when submitting
       }

      List<String> params = new ArrayList<String>(3);
      
      // add the value parameter
      StringBuilder value = new StringBuilder("document.getElementById('");
      value.append(component.getClientId(context));
      if (idSuffix != null)
      {
         value.append(idSuffix);
      }
      value.append("')");
      params.add(value.toString());
      
      // add the validation failed message to show (use the value of the 
      // label component of the given item)
      String msg = Application.getMessage(context, "validation_mandatory");
      addStringConstraintParam(params, 
            MessageFormat.format(msg, new Object[] {item.getResolvedDisplayLabel()}));
      
      // add the validation case to the property sheet
      final String validateMandatoryJsFunctionName = getValidateMandatoryJsFunctionName();
      if(isNotBlank(validateMandatoryJsFunctionName))
         propertySheet.addClientValidation(new ClientValidation(validateMandatoryJsFunctionName,
            params, realTimeChecking));
   }

    protected String getValidateMandatoryJsFunctionName() {
        return "validateMandatory";
    }
   
   /**
    * Sets up the marker to show that the item is mandatory.
    * 
    * @param context FacesContext
    * @param item The item being generated
    */
   @SuppressWarnings("unchecked")
   protected void setupMandatoryMarker(FacesContext context, PropertySheetItem item)
   {
       if(isMandatoryMarkerDisabled() || isChildFromGeneratorsWrapper()) {
           return;// don't add marker to child components of multiValueEditor and InlinePropertyGroupGenerator
       }
      // create the required field graphic
      UIGraphic image = (UIGraphic)context.getApplication().
            createComponent(UIGraphic.COMPONENT_TYPE);
      image.setUrl("/images/icons/required_field.gif");
      image.getAttributes().put("style", "padding-right: 4px;");
      
      // add marker as child to the property sheet item
      item.getChildren().add(image);
   }
   
   /**
    * Sets up client validation rules for any constraints the property has.
    * 
    * @param context FacesContext
    * propertySheet The property sheet being generated
    * @param property The property being generated
    * @param propertyDef The data dictionary definition of the property
    * @param component The component representing the property
    */
   protected void setupConstraints(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      // only setup constraints if the property sheet is in edit mode,
      // validation is enabled
      if (propertySheet.inEditMode() && propertySheet.isValidationEnabled() &&
          propertyDef != null)
      {
         List<ConstraintDefinition> constraints = propertyDef.getConstraints();
         for (ConstraintDefinition constraintDef : constraints)
         {
            Constraint constraint = constraintDef.getConstraint();
            if (constraint instanceof AbstractConstraint && ((AbstractConstraint) constraint).isClientSideValidationDisabled()) {
                continue;
            }
            if (constraint instanceof RegexConstraint)
            {
               setupRegexConstraint(context, propertySheet, property, component,
                     (RegexConstraint)constraint, false);
            }
            else if (constraint instanceof StringLengthConstraint)
            {
               setupStringLengthConstraint(context, propertySheet, property, component,
                     (StringLengthConstraint)constraint, false);
            }
            else if (constraint instanceof NumericRangeConstraint)
            {
               setupNumericRangeConstraint(context, propertySheet, property, component,
                     (NumericRangeConstraint)constraint, false);
            }
            else if (constraint instanceof ListOfValuesConstraint)
            {
               // NOTE: This is dealt with at the component creation stage
               //       as a different component is usually required.
            }
         }
      }
   }
   
   /**
    * Sets up a default validation rule for the regular expression constraint
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet to add the validation rule to
    * @param property The property being generated
    * @param component The component representing the property
    * @param constraint The constraint to setup
    * @param realTimeChecking true to make the client validate as the user types
    */
   protected void setupRegexConstraint(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         UIComponent component, RegexConstraint constraint, 
         boolean realTimeChecking)
   {
      String expression = constraint.getExpression();
      boolean requiresMatch = constraint.getRequiresMatch();
      
      List<String> params = new ArrayList<String>(3);
      
      // add the value parameter
      String value = "document.getElementById('" +
            component.getClientId(context) + "')";
      params.add(value);
      
      // add the regular expression parameter
      try
      {
         // encode the expression so it can be unescaped by JavaScript
         addStringConstraintParam(params, URLEncoder.encode(expression, "UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {
         // just add the expression as is
         addStringConstraintParam(params, expression);
      }
      
      // add the requiresMatch parameter
      params.add(Boolean.toString(requiresMatch));
      
      // add the validation failed messages
      String matchMsg = Application.getMessage(context, "validation_regex");
      addStringConstraintParam(params, 
            MessageFormat.format(matchMsg, new Object[] {property.getResolvedDisplayLabel()}));

      String noMatchMsg = Application.getMessage(context, "validation_regex_not_match");
      addStringConstraintParam(params,
            MessageFormat.format(noMatchMsg, new Object[] {property.getResolvedDisplayLabel()}));
      
      // add the validation case to the property sheet
      propertySheet.addClientValidation(new ClientValidation("validateRegex",
         params, realTimeChecking));
   }
   
   /**
    * Sets up a default validation rule for the string length constraint
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet to add the validation rule to
    * @param property The property being generated
    * @param component The component representing the property
    * @param constraint The constraint to setup
    * @param realTimeChecking true to make the client validate as the user types
    */
   protected void setupStringLengthConstraint(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         UIComponent component, StringLengthConstraint constraint,
         boolean realTimeChecking)
   {
      int min = constraint.getMinLength();
      int max = constraint.getMaxLength();
      
      List<String> params = new ArrayList<String>(3);
      
      // add the value parameter
      String value = "document.getElementById('" +
            component.getClientId(context) + "')";
      params.add(value);
      
      // add the min parameter
      params.add(Integer.toString(min));
      
      // add the max parameter
      params.add(Integer.toString(max));
      
      // add the validation failed message to show
      String msg = Application.getMessage(context, "validation_string_length");
      addStringConstraintParam(params, 
            MessageFormat.format(msg, new Object[] {property.getResolvedDisplayLabel(), min, max}));
      
      // add the validation case to the property sheet
      propertySheet.addClientValidation(new ClientValidation("validateStringLength",
         params, realTimeChecking));
   }
   
   /**
    * Sets up a default validation rule for the numeric range constraint
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet to add the validation rule to
    * @param property The property being generated
    * @param component The component representing the property
    * @param constraint The constraint to setup
    * @param realTimeChecking true to make the client validate as the user types
    */
   protected void setupNumericRangeConstraint(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         UIComponent component, NumericRangeConstraint constraint,
         boolean realTimeChecking)
   {
      double min = constraint.getMinValue();
      double max = constraint.getMaxValue();
      
      List<String> params = new ArrayList<String>(3);
      
      // add the value parameter
      String value = "document.getElementById('" +
            component.getClientId(context) + "')";
      params.add(value);
      
      // add the min parameter
      params.add(Double.toString(min));
      
      // add the max parameter
      params.add(Double.toString(max));
      
      // add the validation failed message to show
      String msg = Application.getMessage(context, "validation_numeric_range");
      addStringConstraintParam(params, 
            MessageFormat.format(msg, new Object[] {property.getResolvedDisplayLabel(), min, max}));
      
      // add the validation case to the property sheet
      propertySheet.addClientValidation(new ClientValidation("validateNumberRange",
         params, false));
   }
   
   /**
    * Sets up the appropriate converter for the given property
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param property The property being generated
    * @param propertyDef The data dictionary definition of the property
    * @param component The component representing the property
    */
   protected void setupConverter(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      if (property.getConverter() != null)
      {
         // create and add the custom converter
         createAndSetConverter(context, property.getConverter(), component);
      }
      else if (propertySheet.inEditMode() == false && 
               propertyDef != null && propertyDef.isMultiValued() && !ComponentUtil.isAlwaysEditComponent(component))
      {
         // if there isn't a custom converter and the property is
         // multi-valued add the multi value converter as a default
         createAndSetConverter(context, ListNonBlankStringsWithCommaConverter.CONVERTER_ID,
               component);
      }
   }
   
   /**
    * Sets up the appropriate converter for the given association
    * 
    * @param context FacesContext
    * @param propertySheet The property sheet being generated
    * @param association The association being generated
    * @param associationDef The data dictionary definition of the property
    * @param component The component representing the association
    */
   protected void setupConverter(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem association, 
         AssociationDefinition associationDef, UIComponent component)
   {
      if (association.getConverter() != null)
      {
         // create and add the custom converter
         createAndSetConverter(context, association.getConverter(), component);
      }
   }
   
   /**
    * Returns the type of the control being generated
    * 
    * @return The type of the control either a FIELD or a SELECTOR
    */
   protected ControlType getControlType()
   {
      return ControlType.FIELD;
   }
   
   /**
    * Retrieve the PropertyDefinition for the given property name on the given node
    * 
    * @param node The node to get the property definition from
    * @param propertyName The name of the property
    * @return PropertyDefinition for the node or null if a definition can not be found
    */
   protected PropertyDefinition getPropertyDefinition(FacesContext context,
         Node node, String propertyName)
   {
      return getDataDictionary(context).getPropertyDefinition(node, propertyName);
   }
   
   /**
    * Retrieve the AssociationDefinition for the given property name on the given node
    * 
    * @param node The node to get the association definition from
    * @param associationName The name of the property
    * @return AssociationDefinition for the node or null if a definition can not be found
    */
   protected AssociationDefinition getAssociationDefinition(FacesContext context,
         Node node, String associationName)
   {
      return getDataDictionary(context).getAssociationDefinition(node, associationName);
   }
   
   /**
    * Adds the given string parameter to the list of parameters to be used for
    * validating constraints on the client.
    * This method adds the quotes around the given parameter and also escapes
    * any ocurrences of the double quote character.
    * 
    * @param params The list of parameters for the constraint
    * @param param The string parameter to add
    */
   protected void addStringConstraintParam(List<String> params, String param)
   {
      params.add("\"" + StringUtils.replace(param, "\"", "\\\"") + "\"");
   }
   
   private DataDictionary getDataDictionary(FacesContext context)
   {
      if (this.dataDictionary == null)
      {
         this.dataDictionary = (DataDictionary)FacesContextUtils.getRequiredWebApplicationContext(
            context).getBean(Application.BEAN_DATA_DICTIONARY);
      }
      
      return this.dataDictionary;
   }
   
   protected <T> T getValue(UIPropertySheet propertySheet, final PropertySheetItem item) {
       Node node = propertySheet.getNode();
       QName qName = QName.resolveToQName(node.getNamespacePrefixResolver(), item.getAttributes().get("name").toString());
       @SuppressWarnings("unchecked")
       T value = (T) node.getProperties().get(qName);
       return value;
   }

   // START: getters / setters
    @Override
    public Map<String, String> getCustomAttributes() {
        if (propertySheetItemAttributes == null) {
            propertySheetItemAttributes = new HashMap<String, String>(0);
        }
        return propertySheetItemAttributes;
    }

    @Override
    public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
        this.propertySheetItemAttributes = propertySheetItemAttributes;
    }

    @Override
    public String toString() {
        if (propertySheetItemAttributes == null) {
            return super.toString();
        }
        return new StringBuilder("name=").append(propertySheetItemAttributes.get("name")).toString();
    }
    // END: getters / setters
}
//@formatter: on