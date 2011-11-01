package ee.webmedia.alfresco.common.propertysheet.upload;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class UploadFileGenerator extends BaseComponentGenerator {

    public static final String ATTR_SUCCESS_MSG_KEY = "successMsgKey";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UploadFileInput component = new UploadFileInput();
        FacesHelper.setupComponentId(context, component, id);
        String successMsgKey = getCustomAttributes().get(ATTR_SUCCESS_MSG_KEY);
        if (StringUtils.isNotBlank(successMsgKey)) {
            ComponentUtil.putAttribute(component, ATTR_SUCCESS_MSG_KEY, successMsgKey);
        }
        return component;
    }
}
