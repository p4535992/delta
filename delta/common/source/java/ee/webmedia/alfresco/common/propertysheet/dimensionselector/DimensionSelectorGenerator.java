package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorValueProvider;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;

public class DimensionSelectorGenerator extends ClassificatorSelectorGenerator {

    public static final String ATTR_DIMENSION_NAME = "dimensionName";
    public static final String ATTR_FILTER = "filter";
    public static final String EA_PREFIX_INCLUDE_FILTER_KEY = "eaPrefixInclude";
    public static final String EA_PREFIX_EXCLUDE_FILTER_KEY = "eaPrefixExclude";
    public static Map<String, Predicate> predefinedFilters = new HashMap<String, Predicate>();

    private static final String EA_PREFIX = "EA";
    private Predicate filter = null;

    static {
        predefinedFilters.put(EA_PREFIX_INCLUDE_FILTER_KEY, getEAInclusivePredicate());
    }

    public DimensionSelectorGenerator(Predicate filter) {
        this.filter = filter;
    }

    public DimensionSelectorGenerator() {
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<ClassificatorSelectorValueProvider> getSelectorValueProviders(String dimensionName) {
        List<ClassificatorSelectorValueProvider> valueProviders = new ArrayList<ClassificatorSelectorValueProvider>();
        NodeRef dimensionRef = BeanHelper.getEInvoiceService().getDimension(Dimensions.get(dimensionName));
        if (dimensionRef != null) {
            List<DimensionValue> dimensions = BeanHelper.getEInvoiceService().getActiveDimensionValues(dimensionRef);
            Predicate activeFilter = getFilter();
            if (activeFilter != null) {
                valueProviders.addAll(CollectionUtils.select(dimensions, activeFilter));
            } else {
                valueProviders.addAll(dimensions);
            }
        }
        return valueProviders;
    }

    private Predicate getFilter() {
        String predefinedFilterKey = getCustomAttributes().get(ATTR_FILTER);
        if (StringUtils.isNotEmpty(predefinedFilterKey)) {
            Predicate predefinedFilter = predefinedFilters.get(predefinedFilterKey);
            if (predefinedFilter != null) {
                return predefinedFilter;
            }
        }
        return filter;
    }

    @Override
    protected String getValueProviderName() {
        return getCustomAttributes().get(ATTR_DIMENSION_NAME);
    }

    public static Predicate getEAExclusivePredicate() {
        return PredicateUtils.notPredicate(getEAInclusivePredicate());
    }

    public static Predicate getEAInclusivePredicate() {
        return new Predicate() {
            @Override
            public boolean evaluate(Object arg0) {
                String valueName = ((DimensionValue) arg0).getValueName();
                return StringUtils.startsWith(valueName, EA_PREFIX);
            }
        };
    }

}
