package ee.webmedia.alfresco.common.propertysheet.renderer;

import static ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ACTION_INDEX;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.util.Pair;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlButtonRendererBase;

import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Custom renderer to generate onclick javascript based on component attributes
 * 
 * @author Riina Tens
 */

public class HtmlButtonRenderer extends HtmlButtonRendererBase {

    public static final String HTML_BUTTON_RENDERER_TYPE = HtmlButtonRenderer.class.getCanonicalName();
    public static final String ATTR_ONCLICK_DATA = "onclickData";

    @Override
    protected StringBuffer buildOnClick(UIComponent uiComponent, FacesContext facesContext, ResponseWriter writer)
            throws IOException {

        Map<String, Object> attributes = ComponentUtil.getAttributes(uiComponent);
        if (attributes.containsKey(ATTR_ONCLICK_DATA)) {
            @SuppressWarnings("unchecked")
            Pair<UIComponent, Integer> data = (Pair<UIComponent, Integer>) attributes.get(ATTR_ONCLICK_DATA);
            generateOnClick(facesContext, (ModalLayerComponent) data.getFirst(), data.getSecond(), (HtmlCommandButton) uiComponent);
        }

        return super.buildOnClick(uiComponent, facesContext, writer);
    }

    private void generateOnClick(FacesContext context, ModalLayerComponent dueDateExtensionLayer, int index, HtmlCommandButton outcomeButton) {
        String onclick = ComponentUtil.generateFieldSetter(context, dueDateExtensionLayer, getActionId(context, dueDateExtensionLayer),
                ACTION_INDEX + ";" + index);
        onclick += "showModal('" + WorkflowUtil.getDialogId(context, dueDateExtensionLayer) + "');return false;";
        outcomeButton.setOnclick(onclick);
    }

}