package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;

public class DimensionSelectorGenerator extends SuggesterGenerator {

    public static final String ATTR_DIMENSION_NAME = "dimensionName";
    public static final String ATTR_DIMENSION_VALUES = "dimensionValues";
    public static final String ATTR_ENTRY_DATE = "entryDate";
    public static final String ATTR_PREDEFINED_FILTER_NAME = "predefinedFilterName";
    public static final String ATTR_FILTER = "filter";
    public static final String ATTR_GENERATE_DIMENSION_VALUES = "generateDimensionValues";
    public static final String ATTR_USE_DFAULT_VALUE = "useDefaultValue";
    public static final String EA_PREFIX_INCLUDE_FILTER_KEY = "eaPrefixInclude";
    public static final String EA_PREFIX_EXCLUDE_FILTER_KEY = "eaPrefixExclude";
    public static final String PROP_SUGGESTER_VALUES = "suggesterValues";
    public static Map<String, Predicate> predefinedFilters = new HashMap<String, Predicate>();

    private static final String EA_PREFIX = "EA";
    private Predicate filter = null;
    private Date entryDate = null;

    static {
        predefinedFilters.put(EA_PREFIX_INCLUDE_FILTER_KEY, getEAInclusivePredicate());
        predefinedFilters.put(EA_PREFIX_EXCLUDE_FILTER_KEY, getEAExclusivePredicate());
    }

    public DimensionSelectorGenerator(Predicate filter) {
        this.filter = filter;
    }

    public DimensionSelectorGenerator() {
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIInput component = (UIInput) super.generate(context, id);
        component.setRendererType(DimensionSelectorRenderer.DIMENSION_SELECTOR_RENDERER_TYPE);
        String generateValues = getCustomAttributes().get(ATTR_GENERATE_DIMENSION_VALUES);
        String dimensionName = getCustomAttributes().get(ATTR_DIMENSION_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        if ((generateValues == null || Boolean.valueOf(generateValues))
                && (!getCustomAttributes().containsKey(VALUE_INDEX_IN_MULTIVALUED_PROPERTY) || Integer.valueOf(getCustomAttributes().get(VALUE_INDEX_IN_MULTIVALUED_PROPERTY)) == 0)) {
            NodeRef dimensionRef = BeanHelper.getEInvoiceService().getDimension(Dimensions.get(dimensionName));
            attributes.put(DimensionSelectorGenerator.ATTR_DIMENSION_VALUES, getDimensionValues(dimensionRef, getEntryDate()));
        }
        attributes.put(ATTR_DIMENSION_NAME, dimensionName);
        if (entryDate != null) {
            attributes.put(ATTR_ENTRY_DATE, entryDate);
        }
        return component;
    }

    @SuppressWarnings("unchecked")
    private List<DimensionValue> getDimensionValues(NodeRef dimensionRef, Date entryDate) {
        List<DimensionValue> dimensionValues = new ArrayList<DimensionValue>();
        if (dimensionRef != null) {
            List<DimensionValue> dimensions = BeanHelper.getEInvoiceService().searchDimensionValues(null, dimensionRef, entryDate, true);
            Predicate activeFilter = getFilter();
            if (activeFilter != null) {
                dimensionValues.addAll(CollectionUtils.select(dimensions, activeFilter));
            } else {
                dimensionValues.addAll(dimensions);
            }
        }
        return dimensionValues;
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
    }

    @Override
    public Pair<List<String>, String> getSuggesterValues(FacesContext context, UIInput component) {
        return null;
    }

    public Date getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(Date entryDate) {
        this.entryDate = entryDate;
    }

}
