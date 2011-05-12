package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorValueProvider;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;

public class DimensionSelectorGenerator extends ClassificatorSelectorGenerator {

    public static final String ATTR_DIMENSION_NAME = "dimensionName";
    private final Predicate filter = null;

    @SuppressWarnings("unchecked")
    @Override
    protected List<ClassificatorSelectorValueProvider> getSelectorValueProviders(String dimensionName) {
        List<ClassificatorSelectorValueProvider> valueProviders = new ArrayList<ClassificatorSelectorValueProvider>();
        NodeRef dimensionRef = BeanHelper.getEInvoiceService().getDimension(Dimensions.get(dimensionName));
        if (dimensionRef != null) {
            List<DimensionValue> dimensions = BeanHelper.getEInvoiceService().getActiveDimensionValues(dimensionRef);
            Collections.sort(dimensions);
            if (filter != null) {
                valueProviders.addAll(CollectionUtils.select(dimensions, filter));
            } else {
                valueProviders.addAll(dimensions);
            }
        }
        return valueProviders;
    }

    @Override
    protected String getValueProviderName() {
        return getCustomAttributes().get(ATTR_DIMENSION_NAME);
    }

}
