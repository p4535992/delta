package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSuggestingGenerator.ComponentAttributeNames.CLASSIFICATOR_VALUES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.util.Pair;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Generator, that generates a TextArea that suggests values of classificator name (defined using "classificatorName" attribute in the show-property
 * element), but allows to insert value that is not in classificator values
 * 
 * @author Ats Uiboupin
 */
public class ClassificatorSuggestingGenerator extends TextAreaGenerator {

    private ClassificatorService classificatorService;
    private GeneralService generalService;

    interface ComponentAttributeNames {
        String CLASSIFICATOR_VALUES = "classificatorValues";
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        final UIInput inputTextComp = (UIInput) super.generate(context, id);
        inputTextComp.setRendererType(ClassificatorSuggesterGeneratorRenderer.CLASSIFICATOR_SUGGESTER_RENDERER_TYPE);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = inputTextComp.getAttributes();
        final Pair<List<String>, String /* defaultClassificator */> classificatorValues = getClassificatorValues();
        String existingValue = getGeneralService().getExistingRepoValue4ComponentGenerator();
        if (existingValue == null) {
            existingValue = classificatorValues.getSecond();
        }
        if (existingValue != null) {
            inputTextComp.setValue(existingValue);
        }
        attributes.put(CLASSIFICATOR_VALUES, classificatorValues.getFirst());

        return inputTextComp;
    }

    private Pair<List<String>, String> getClassificatorValues() {
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

    private GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    private ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

}
