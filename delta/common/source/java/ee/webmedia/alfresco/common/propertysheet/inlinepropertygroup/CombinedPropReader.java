package ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Read property definition into list of ComponentPropVO
 * 
 * @author Ats Uiboupin
 */
public class CombinedPropReader {
    private static Log missingPropsLogger = LogFactory.getLog("alfresco.missingProperties");

    public interface AttributeNames {
        String OPTIONS_SEPARATOR = "optionsSeparator";
        String PROPERTIES_SEPARATOR = "propertiesSeparator";
        String DEFAULT_PROPERTIES_SEPARATOR = ",";
        String PROPS_GENERATION = "propsGeneration";
        String PROPS = "props";
        String TEXT_ID = "textId";
        String PROP_GENERATOR_DESCRIPTORS = "propGeneratorDescriptors";
    }

    /**
     * Create list of ComponentPropVO that describe how each component should be generated.<br>
     * If componentGenerator is not encoded in <code>propertyDescriptions</code>, then TextFieldGenerator is set as default.
     * 
     * @param propertyDescriptionsEncoded
     * @param propertiesSeparator
     * @param optionsSeparator
     * @param labelsEncoded - comma separated list of labels
     * @return
     */
    public static List<ComponentPropVO> readProperties(String propertyDescriptionsEncoded, String propertiesSeparator, String optionsSeparator, String labelsEncoded) {
        List<String> propTitles = extractPropLabels(labelsEncoded);
        final List<ComponentPropVO> componentPropVOs = readProperties(propertyDescriptionsEncoded, propertiesSeparator, optionsSeparator, null, null);
        int i = 0;
        for (ComponentPropVO componentPropVO : componentPropVOs) {
            componentPropVO.setUseComponentGenerator(false);
            componentPropVO.setPropertyLabel(propTitles.get(i++));
            if (StringUtils.isBlank(componentPropVO.getGeneratorName())) {
                componentPropVO.setGeneratorName(RepoConstants.GENERATOR_TEXT_FIELD);
            }
        }
        return componentPropVOs;
    }

    /**
     * Create list of ComponentPropVO that describe how each component should be generated.<br>
     * If <code>node</code> is given then label is filled based on propertyName encoded in <code>propertyDescriptions</code> and generator is determined based
     * on type of property
     * 
     * @param propertyDescriptionsEncoded
     * @param propertiesSeparator
     * @param optionsSeparator
     * @param node - optional - if null, then labels are not filled and generator is not set unless it is explicitly given in <code>propertyDescriptions</code>
     * @param context - optional
     * @return
     */
    public static List<ComponentPropVO> readProperties(String propertyDescriptionsEncoded, String propertiesSeparator, String optionsSeparator, Node node,
            FacesContext context) {
        if (StringUtils.isBlank(propertiesSeparator)) {
            propertiesSeparator = AttributeNames.DEFAULT_PROPERTIES_SEPARATOR;
        }
        if (StringUtils.isBlank(optionsSeparator)) {
            optionsSeparator = "\\|"; // XXX: default could be "¤", as it is probably not used as often as "|".
        }

        List<String> propGeneratorDescriptors = Arrays.asList(StringUtils.split(propertyDescriptionsEncoded, propertiesSeparator));

        final ArrayList<ComponentPropVO> componentPropVOs = new ArrayList<ComponentPropVO>(propGeneratorDescriptors.size());
        for (String propDesc : propGeneratorDescriptors) {
            final ComponentPropVO componentPropVO = new ComponentPropVO();
            propDesc = propDesc.trim();
            String[] fields = propDesc.split(optionsSeparator);

            // get property name
            if (fields.length < 1) {
                throw new RuntimeException("Property name must be specified");
            }
            final String propName = fields[0].trim();
            componentPropVO.setPropertyName(propName);

            // get component generator name
            String generatorName = null;
            if (fields.length >= 2 && StringUtils.isNotEmpty(fields[1])) {
                generatorName = fields[1].trim();
            }
            if (node != null) { // set label and component generator, if it was not explicitly specified
                // get property definition
                PropertyDefinition propDef = ComponentUtil.getPropertyDefinition(context, node, propName);

                // set property label
                componentPropVO.setPropertyLabel(ComponentUtil.resolveDisplayLabel(context, propDef, propName));

                if (StringUtils.isBlank(generatorName)) {
                    if (propDef != null) {
                        QName dataTypeName = propDef.getDataType().getName();
                        generatorName = ComponentUtil.getDefaultGeneratorName(dataTypeName);
                        if (generatorName == null) {
                            throw new RuntimeException("Component generator name not specified and default generator not found for data type " //
                                    + dataTypeName + ", property name '" + propName + "'");
                        }
                    } else {
                        missingPropsLogger.warn("CombinedPropReader failed to find Property '" + propName
                                + "'. propertiesSeparator='" + propertiesSeparator
                                + "'; optionsSeparator='" + optionsSeparator
                                + "' when parsing string:\n\t" + propertyDescriptionsEncoded
                                + "\nNode\n:" + node);
                    }
                }
            }
            componentPropVO.setGeneratorName(generatorName);

            // get attributes
            componentPropVO.setCustomAttributes(getAttributes(propDesc, fields));

            componentPropVOs.add(componentPropVO);
        }
        return componentPropVOs;
    }

    private static Map<String, String> getAttributes(String propDesc, String[] fields) {
        Map<String, String> customAttributes = new HashMap<String, String>();
        if (fields.length >= 3) {
            for (int i = 2; i < fields.length; i++) { // first is propertyName, second is componentGenerator name
                String fieldValue = fields[i];
                if (fieldValue == null || (fieldValue.indexOf("=") < 0 && fieldValue.indexOf("#{") < 0)) {
                    throw new RuntimeException((i + 1) + ". field of property description '" + propDesc + "' does not contain custom attribute.");
                }
                final int attrNameIndex = fieldValue.indexOf("=");
                final String attrName = fieldValue.substring(0, attrNameIndex).trim();
                final String attrValue = fieldValue.substring(attrNameIndex + 1).trim();
                customAttributes.put(attrName, attrValue);
            }
        }
        return customAttributes;
    }

    private static List<String> extractPropLabels(String titles) {
        List<String> propTitles = new ArrayList<String>();
        for (String title : titles.split(",")) {
            if (StringUtils.isNotBlank(title)) {
                propTitles.add(MessageUtil.getMessage(FacesContext.getCurrentInstance(), title));
            } else {
                propTitles.add("");
            }
        }
        return propTitles;
    }

}
