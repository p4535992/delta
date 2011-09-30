package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorRenderer;

/**
 * Render different texts in value attribute and option label
 * 
 * @author Riina Tens
 */
public class LabelAndValueSelectorRenderer extends DimensionSelectorRenderer {

    public static final String LABEL_AND_VALUE_SELECTOR_RENDERER_TYPE = LabelAndValueSelectorRenderer.class.getCanonicalName();

    @Override
    protected void renderSelectOptions(FacesContext context, UIComponent component, Converter converter, Set lookupSet, List selectItemList)
            throws IOException {
        renderOptions(context, component, converter, lookupSet, selectItemList, true, true);
    }

}
