<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;

import org.alfresco.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.model.ClassificatorValueProviderWithSpecifier;

public class MultiClassificatorSelectorGenerator extends ClassificatorSelectorGenerator {

    public static final String CLASSIFICATOR_NAME_SEPARATOR = "¤";
    public static final String ATTR_CLASSIFICATOR_SPECIFIERS = "attrClassificatorSpecifiers";
    public static final String ATTR_CLASSIFICATOR_SPECIFIER_LABELS = "attrClassificatorSpecifierLabels";
    public static final String ATTR_FILTER_NUMERIC = "attrFilterNumeric";
    List<ClassificatorSelectorValueProvider> valueProviders = null;

    @SuppressWarnings("unchecked")
    @Override
    protected List<ClassificatorSelectorValueProvider> getSelectorValueProviders(String classificatorNameStr, UIComponent component, FacesContext context) {
        if (valueProviders == null) {
            List<String> classificatorNames = Arrays.asList(StringUtils.split(classificatorNameStr, CLASSIFICATOR_NAME_SEPARATOR));
            final List<String> specifiers = getClassificatorSpecifiers();
            final List<String> specifierLabels = getClassificatorSpecifierLabels();
            valueProviders = new ArrayList<ClassificatorSelectorValueProvider>();
            int specifiersSize = specifiers == null ? 0 : specifiers.size();
            int specifierLabelsSize = specifierLabels == null ? 0 : specifierLabels.size();
            int specifierIndex = 0;
            for (String classificatorName : classificatorNames) {
                List<ClassificatorValue> classificatorValues //
                = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificatorName));
                Collections.sort(classificatorValues);
                final String specifier = specifierIndex < specifiersSize ? specifiers.get(specifierIndex) : "";
                final String specifierLabel = specifierIndex < specifierLabelsSize ? specifierLabels.get(specifierIndex) : "";
                if (isFilterNumericOnly()) {
                    filterNumeric(classificatorValues);
                }
                valueProviders.addAll(CollectionUtils.collect(classificatorValues, new Transformer() {

                    @Override
                    public Object transform(Object arg0) {
                        return new ClassificatorValueProviderWithSpecifier((ClassificatorValue) arg0, specifier, specifierLabel);
                    }

                }));
                specifierIndex++;
            }
        }
        List<ClassificatorSelectorValueProvider> componentValueProviders = new ArrayList<ClassificatorSelectorValueProvider>(valueProviders);
        addSelectedValueIfNeeded(componentValueProviders, component, context);
        return componentValueProviders;
    }

    private void filterNumeric(List<ClassificatorValue> classificatorValues) {
        CollectionUtils.filter(classificatorValues, new Predicate() {

            @Override
            public boolean evaluate(Object arg0) {
                ClassificatorValue classificatorValue = (ClassificatorValue) arg0;
                try {
                    Integer.parseInt(classificatorValue.getValueName().trim());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }

        });
        CollectionUtils.transform(classificatorValues, new Transformer() {
            @Override
            public Object transform(Object arg0) {
                ClassificatorValue classificatorValue = (ClassificatorValue) arg0;
                classificatorValue.setValueName(classificatorValue.getValueName().trim());
                return classificatorValue;
            }

        });
    }

    private List<String> getClassificatorSpecifiers() {
        return getOptionalAttributes(ATTR_CLASSIFICATOR_SPECIFIERS);
    }

    private List<String> getClassificatorSpecifierLabels() {
        return getOptionalAttributes(ATTR_CLASSIFICATOR_SPECIFIER_LABELS);
    }

    private List<String> getOptionalAttributes(String attrName) {
        String specifierStr = getCustomAttributes().get(attrName);
        List<String> specifiers = new ArrayList<String>();
        if (StringUtils.isNotBlank(specifierStr)) {
            specifiers.addAll(Arrays.asList(specifierStr.split(CLASSIFICATOR_NAME_SEPARATOR)));
        }
        return specifiers;
    }

    @SuppressWarnings("unchecked")
    private void addSelectedValueIfNeeded(List<ClassificatorSelectorValueProvider> valueProviders, UIComponent component, FacesContext context) {
        Converter converter = RendererUtils.findUIOutputConverter(context, (UIOutput) component);
        ValueBinding valueBinding = component.getValueBinding("value");
        if (converter == null || valueBinding == null) {
            return;
        }

        Object valueObj = valueBinding.getValue(context);
        if (!(valueObj instanceof List)) {
            return;
        }
        List<Object> value = (List<Object>) valueObj;
        if (value.get(0) == null || value.get(1) == null) {
            return;
        }
        String selectedValue = converter.getAsString(context, component, value);
        for (ClassificatorSelectorValueProvider valueProvider : valueProviders) {
            ClassificatorSelectorValueProvider classificatorValueValue = valueProvider;
            String selectorValueName = classificatorValueValue.getSelectorValueName();
            if (selectedValue.equals(selectorValueName)) {
                return;
            }
        }
        Pair<String, String> specifierAndLabel = getSpecifierAndLabel(value.get(1));
        if (specifierAndLabel != null) {
            ClassificatorValue classificatorValue = new ClassificatorValue();
            classificatorValue.setValueName(value.get(0).toString());
            ClassificatorValueProviderWithSpecifier selectedClassificatorValue = new ClassificatorValueProviderWithSpecifier(classificatorValue, specifierAndLabel.getFirst(),
                    specifierAndLabel.getSecond());
            valueProviders.add(0, selectedClassificatorValue);
        }
    }

    private Pair<String, String> getSpecifierAndLabel(Object object) {
        int specifierIndex = 0;
        for (String specifier : getClassificatorSpecifiers()) {
            if (specifier.equalsIgnoreCase(object.toString())) {
                return new Pair<String, String>(specifier, getClassificatorSpecifierLabels().get(specifierIndex));
            }
            specifierIndex++;
        }
        return null;
    }

    private boolean isFilterNumericOnly() {
        return Boolean.parseBoolean(getCustomAttributes().get(ATTR_FILTER_NUMERIC));
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;

import org.alfresco.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.model.ClassificatorValueProviderWithSpecifier;

public class MultiClassificatorSelectorGenerator extends ClassificatorSelectorGenerator {

    public static final String CLASSIFICATOR_NAME_SEPARATOR = "¤";
    public static final String ATTR_CLASSIFICATOR_SPECIFIERS = "attrClassificatorSpecifiers";
    public static final String ATTR_CLASSIFICATOR_SPECIFIER_LABELS = "attrClassificatorSpecifierLabels";
    public static final String ATTR_FILTER_NUMERIC = "attrFilterNumeric";
    List<ClassificatorSelectorValueProvider> valueProviders = null;

    @SuppressWarnings("unchecked")
    @Override
    protected List<ClassificatorSelectorValueProvider> getSelectorValueProviders(String classificatorNameStr, UIComponent component, FacesContext context) {
        if (valueProviders == null) {
            List<String> classificatorNames = Arrays.asList(StringUtils.split(classificatorNameStr, CLASSIFICATOR_NAME_SEPARATOR));
            final List<String> specifiers = getClassificatorSpecifiers();
            final List<String> specifierLabels = getClassificatorSpecifierLabels();
            valueProviders = new ArrayList<ClassificatorSelectorValueProvider>();
            int specifiersSize = specifiers == null ? 0 : specifiers.size();
            int specifierLabelsSize = specifierLabels == null ? 0 : specifierLabels.size();
            int specifierIndex = 0;
            for (String classificatorName : classificatorNames) {
                List<ClassificatorValue> classificatorValues //
                = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificatorName));
                Collections.sort(classificatorValues);
                final String specifier = specifierIndex < specifiersSize ? specifiers.get(specifierIndex) : "";
                final String specifierLabel = specifierIndex < specifierLabelsSize ? specifierLabels.get(specifierIndex) : "";
                if (isFilterNumericOnly()) {
                    filterNumeric(classificatorValues);
                }
                valueProviders.addAll(CollectionUtils.collect(classificatorValues, new Transformer() {

                    @Override
                    public Object transform(Object arg0) {
                        return new ClassificatorValueProviderWithSpecifier((ClassificatorValue) arg0, specifier, specifierLabel);
                    }

                }));
                specifierIndex++;
            }
        }
        List<ClassificatorSelectorValueProvider> componentValueProviders = new ArrayList<ClassificatorSelectorValueProvider>(valueProviders);
        addSelectedValueIfNeeded(componentValueProviders, component, context);
        return componentValueProviders;
    }

    private void filterNumeric(List<ClassificatorValue> classificatorValues) {
        CollectionUtils.filter(classificatorValues, new Predicate() {

            @Override
            public boolean evaluate(Object arg0) {
                ClassificatorValue classificatorValue = (ClassificatorValue) arg0;
                try {
                    Integer.parseInt(classificatorValue.getValueName().trim());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }

        });
        CollectionUtils.transform(classificatorValues, new Transformer() {
            @Override
            public Object transform(Object arg0) {
                ClassificatorValue classificatorValue = (ClassificatorValue) arg0;
                classificatorValue.setValueName(classificatorValue.getValueName().trim());
                return classificatorValue;
            }

        });
    }

    private List<String> getClassificatorSpecifiers() {
        return getOptionalAttributes(ATTR_CLASSIFICATOR_SPECIFIERS);
    }

    private List<String> getClassificatorSpecifierLabels() {
        return getOptionalAttributes(ATTR_CLASSIFICATOR_SPECIFIER_LABELS);
    }

    private List<String> getOptionalAttributes(String attrName) {
        String specifierStr = getCustomAttributes().get(attrName);
        List<String> specifiers = new ArrayList<String>();
        if (StringUtils.isNotBlank(specifierStr)) {
            specifiers.addAll(Arrays.asList(specifierStr.split(CLASSIFICATOR_NAME_SEPARATOR)));
        }
        return specifiers;
    }

    @SuppressWarnings("unchecked")
    private void addSelectedValueIfNeeded(List<ClassificatorSelectorValueProvider> valueProviders, UIComponent component, FacesContext context) {
        Converter converter = RendererUtils.findUIOutputConverter(context, (UIOutput) component);
        ValueBinding valueBinding = component.getValueBinding("value");
        if (converter == null || valueBinding == null) {
            return;
        }

        Object valueObj = valueBinding.getValue(context);
        if (!(valueObj instanceof List)) {
            return;
        }
        List<Object> value = (List<Object>) valueObj;
        if (value.get(0) == null || value.get(1) == null) {
            return;
        }
        String selectedValue = converter.getAsString(context, component, value);
        for (ClassificatorSelectorValueProvider valueProvider : valueProviders) {
            ClassificatorSelectorValueProvider classificatorValueValue = valueProvider;
            String selectorValueName = classificatorValueValue.getSelectorValueName();
            if (selectedValue.equals(selectorValueName)) {
                return;
            }
        }
        Pair<String, String> specifierAndLabel = getSpecifierAndLabel(value.get(1));
        if (specifierAndLabel != null) {
            ClassificatorValue classificatorValue = new ClassificatorValue();
            classificatorValue.setValueName(value.get(0).toString());
            ClassificatorValueProviderWithSpecifier selectedClassificatorValue = new ClassificatorValueProviderWithSpecifier(classificatorValue, specifierAndLabel.getFirst(),
                    specifierAndLabel.getSecond());
            valueProviders.add(0, selectedClassificatorValue);
        }
    }

    private Pair<String, String> getSpecifierAndLabel(Object object) {
        int specifierIndex = 0;
        for (String specifier : getClassificatorSpecifiers()) {
            if (specifier.equalsIgnoreCase(object.toString())) {
                return new Pair<String, String>(specifier, getClassificatorSpecifierLabels().get(specifierIndex));
            }
            specifierIndex++;
        }
        return null;
    }

    private boolean isFilterNumericOnly() {
        return Boolean.parseBoolean(getCustomAttributes().get(ATTR_FILTER_NUMERIC));
    }

}
>>>>>>> develop-5.1
