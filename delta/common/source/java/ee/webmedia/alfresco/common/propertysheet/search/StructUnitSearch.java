package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;

import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Search structure units, return paths list as result
 * 
 * @author Riina Tens
 */
public class StructUnitSearch extends Search {

    public static final String STRUCT_UNIT_SEARCH_FAMILY = StructUnitSearch.class.getCanonicalName();

    @Override
    public String getFamily() {
        return STRUCT_UNIT_SEARCH_FAMILY;
    }

    @Override
    protected boolean isMultiSelect() {
        return false;
    }

    @Override
    protected void createExistingComponents(FacesContext context) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();

        UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        FacesHelper.setupComponentId(context, container, null);
        children.add(container);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) getList(context);
        appendRowComponent(context, UserUtil.getLongestValueIndex(results));
    }

    @Override
    protected void multiValuedPickerFinish(String[] results, FacesContext context) {
        removeRow(context, 0);

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) getList(context);
        List<String> resultList = Arrays.asList(results);
        list.addAll(resultList);
        appendRowComponent(context, UserUtil.getLongestValueIndex(resultList));
    }

    /** Only one row can be displayed, deleting it means deleting all rows */
    @Override
    protected void removeRow(FacesContext context, int removeIndex) {
        List<?> list = getList(context);
        list.clear();
        @SuppressWarnings("unchecked")
        List<UIComponent> children = ((UIComponent) getChildren().get(0)).getChildren();
        // remove a row from the middle
        children.clear();
    }

}
