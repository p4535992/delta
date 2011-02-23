package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;

public class UnescapedOutputTextGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        return createOutputTextComponent(context, id);
    }

    @Override
    protected UIOutput createOutputTextComponent(FacesContext context, String id) {
        HtmlOutputText outputText = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        FacesHelper.setupComponentId(context, outputText, id);
        outputText.setEscape(false);
        return outputText;
    }

}
