package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.BaseComponentGenerator;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class UnescapedOutputTextGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        return createOutputTextComponent(context, id);
    }

    @Override
    protected UIOutput createOutputTextComponent(FacesContext context, String id) {
        return ComponentUtil.createUnescapedOutputText(context, id);
    }

}
