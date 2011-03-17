package ee.webmedia.alfresco.common.propertysheet.upload;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;

public class UploadFileGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UploadFileInput component = new UploadFileInput();
        FacesHelper.setupComponentId(context, component, id);

        return component;
    }
}
