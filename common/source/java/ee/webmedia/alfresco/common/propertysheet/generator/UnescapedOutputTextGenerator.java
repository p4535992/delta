package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
<<<<<<< HEAD
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;

=======
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.BaseComponentGenerator;

import ee.webmedia.alfresco.utils.ComponentUtil;

>>>>>>> develop-5.1
public class UnescapedOutputTextGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        return createOutputTextComponent(context, id);
    }

    @Override
    protected UIOutput createOutputTextComponent(FacesContext context, String id) {
<<<<<<< HEAD
        HtmlOutputText outputText = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        FacesHelper.setupComponentId(context, outputText, id);
        outputText.setEscape(false);
        return outputText;
=======
        return ComponentUtil.createUnescapedOutputText(context, id);
>>>>>>> develop-5.1
    }

}
