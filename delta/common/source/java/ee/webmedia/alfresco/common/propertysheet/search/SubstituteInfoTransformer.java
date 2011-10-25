package ee.webmedia.alfresco.common.propertysheet.search;

import java.io.Serializable;

import javax.faces.component.UIComponent;

import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Keit Tehvan
 */
class SubstituteInfoTransformer extends Transformer<UIComponent, String> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String propName;

    public SubstituteInfoTransformer(String propName) {
        this.propName = propName;
    }

    @Override

    public String tr(UIComponent component) {
        UIPropertySheet ancestorComponent = ComponentUtil.getAncestorComponent(component, UIPropertySheet.class, true);
        String username = (String) ancestorComponent.getNode().getProperties().get(propName);
        return UserUtil.getSubstitute(username);
    }
}
