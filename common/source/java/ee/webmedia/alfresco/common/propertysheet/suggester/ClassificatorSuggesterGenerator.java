<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.suggester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator, that generates a TextArea that suggests values of classificator name (defined using "classificatorName" attribute in the show-property
 * element), but allows to insert value that is not in classificator values
 * 
 * @author Ats Uiboupin
 */
public class ClassificatorSuggesterGenerator extends SuggesterGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ClassificatorSuggesterGenerator.class);
    private ClassificatorService classificatorService;

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {
        final UIComponent suggestComponent = super.setupMultiValuePropertyIfNecessary( //
                context, propertySheet, property, propertyDef, component);
        if (!log.isDebugEnabled()) {
            return suggestComponent;
        }
        // for debugging purpose in development
        return ComponentUtil.setTooltip(suggestComponent, MessageUtil.getMessage("classificator_source", getClassificatorName()));
    }

    @Override
    public Pair<List<String>, String> getSuggesterValues(FacesContext context, UIInput component) {
        String classificatorName = getClassificatorName();
        if (StringUtils.isBlank(classificatorName)) {
            return new Pair<List<String>, String>(Collections.<String> emptyList(), null);
        }
        List<ClassificatorValue> classificators //
        = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificatorName));

        Collections.sort(classificators);
        String defaultValue = "";
        final ArrayList<String> classificatorValues = new ArrayList<String>(classificators.size());
        for (ClassificatorValue classificator : classificators) {
            classificatorValues.add(classificator.getValueName());
            if (classificator.isByDefault()) {
                defaultValue = classificator.getValueName();
            }
        }
        return new Pair<List<String>, String>(classificatorValues, defaultValue);
    }

    private String getClassificatorName() {
        return getCustomAttributes().get(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME);
    }

    private ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.suggester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator, that generates a TextArea that suggests values of classificator name (defined using "classificatorName" attribute in the show-property
 * element), but allows to insert value that is not in classificator values
 */
public class ClassificatorSuggesterGenerator extends SuggesterGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ClassificatorSuggesterGenerator.class);
    private ClassificatorService classificatorService;

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {
        final UIComponent suggestComponent = super.setupMultiValuePropertyIfNecessary( //
                context, propertySheet, property, propertyDef, component);
        if (!log.isDebugEnabled()) {
            return suggestComponent;
        }
        // for debugging purpose in development
        return ComponentUtil.setTooltip(suggestComponent, MessageUtil.getMessage("classificator_source", getClassificatorName()));
    }

    @Override
    public Pair<List<String>, String> getSuggesterValues(FacesContext context, UIInput component) {
        String classificatorName = getClassificatorName();
        if (StringUtils.isBlank(classificatorName)) {
            return new Pair<List<String>, String>(Collections.<String> emptyList(), null);
        }
        List<ClassificatorValue> classificators //
        = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificatorName));

        Collections.sort(classificators);
        String defaultValue = "";
        final ArrayList<String> classificatorValues = new ArrayList<String>(classificators.size());
        for (ClassificatorValue classificator : classificators) {
            classificatorValues.add(classificator.getValueName());
            if (classificator.isByDefault()) {
                defaultValue = classificator.getValueName();
            }
        }
        return new Pair<List<String>, String>(classificatorValues, defaultValue);
    }

    private String getClassificatorName() {
        return getCustomAttributes().get(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME);
    }

    private ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

}
>>>>>>> develop-5.1
