package ee.webmedia.alfresco.classificator.model;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorValueProvider;

public class ClassificatorValueProviderWithSpecifier implements ClassificatorSelectorValueProvider {

    private static final long serialVersionUID = 1L;

    private String specifier = "";
    private String specifierLabel = "";
    private final ClassificatorValue classificatorValue;

    public ClassificatorValueProviderWithSpecifier(ClassificatorValue classificatorValue, String specifier, String specifierLabel) {
        Assert.notNull(classificatorValue);
        this.specifier = specifier;
        this.specifierLabel = specifierLabel;
        this.classificatorValue = classificatorValue;
    }

    @Override
    public String getSelectorValueName() {
        return classificatorValue.getSelectorValueName() + " " + specifier;
    }

    @Override
    public String getClassificatorDescription() {
        return classificatorValue.getSelectorValueName() + " " + specifierLabel;
    }

    @Override
    public boolean isByDefault() {
        return classificatorValue.isByDefault();
    }

}
