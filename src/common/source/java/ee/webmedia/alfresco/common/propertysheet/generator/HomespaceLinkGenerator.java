package ee.webmedia.alfresco.common.propertysheet.generator;

import java.util.List;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

public class HomespaceLinkGenerator extends BaseComponentGenerator {

    public UIComponent generateLink(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        Application application = context.getApplication();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, link, item.getName());
        
        Node user = propertySheet.getNode();
        NodeRef nRef = new NodeRef(user.getProperties().get(ContentModel.PROP_HOMEFOLDER).toString());
        link.setValue(user.getProperties().get(ContentModel.PROP_HOMEFOLDER));
        link.setActionListener(application.createMethodBinding("#{BrowseBean.clickSpace}", new Class[] { javax.faces.event.ActionEvent.class }));
        
        UIParameter param = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
        param.setName("id");
        param.setValue(new Node(nRef).getId());
        
        @SuppressWarnings("unchecked")
        List<UIComponent> children = link.getChildren();
        children.add(param);        
        link.setTooltip(item.getDisplayLabel());
                
        return link;
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        return generateLink(context, propertySheet, item);
    }

    /* 
     * We override createCommponent, so there is no need to add a body.
     * 
     * (non-Javadoc)
     * @see org.alfresco.web.bean.generator.IComponentGenerator#generate(javax.faces.context.FacesContext, java.lang.String)
     */
    @Override
    public UIComponent generate(FacesContext context, String id) {
        return null;
    }
}
