package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorValueProvider;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionModel;
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
    private String selectedValue = null;

    static {
        predefinedFilters.put(EA_PREFIX_INCLUDE_FILTER_KEY, getEAInclusivePredicate());
    }

    public DimensionSelectorGenerator(Predicate filter) {
        this.filter = filter;
    }

    public DimensionSelectorGenerator() {
    }

    @Override
    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        final UIComponent selectComponent = super.generateSelectComponent(context, id, multiValued);
        selectComponent.setRendererType(DimensionSelectorRenderer.DIMENSION_SELECTOR_RENDERER_TYPE);
        return selectComponent;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<ClassificatorSelectorValueProvider> getSelectorValueProviders(String dimensionName, UIComponent component, FacesContext context) {
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
        if (selectedValue != null) {
            addSelectedDimensionIfNeeded(valueProviders, dimensionRef);
        }
        return valueProviders;
    }

    private void addSelectedDimensionIfNeeded(List<ClassificatorSelectorValueProvider> valueProviders, NodeRef dimensionRef) {
        if (selectedValue == null) {
            return;
        }
        int insertIndex = 0;
        for (ClassificatorSelectorValueProvider valueProvider : valueProviders) {
            DimensionValue dimensionValue = (DimensionValue) valueProvider;
            String selectorValueName = dimensionValue.getSelectorValueName();
            if (StringUtils.equals(selectorValueName, selectedValue)) {
                return;
            }
        }
        // at the moment there is no need that we search for actually existing dimensionValue with given name,
        // just create dummy dimension value for selectbox data provider
        DimensionValue selectedDimensionValue = new DimensionValue(BeanHelper.getGeneralService().createNewUnSaved(DimensionModel.Types.DIMENSION_VALUE, null));
        selectedDimensionValue.setValueName(selectedValue);
        valueProviders.add(0, selectedDimensionValue);
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
    protected String getValueProviderName(Node node) {
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

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

}
