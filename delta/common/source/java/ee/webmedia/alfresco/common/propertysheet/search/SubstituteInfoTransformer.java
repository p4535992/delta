package ee.webmedia.alfresco.common.propertysheet.search;

import java.io.Serializable;

import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Keit Tehvan
 */
class SubstituteInfoTransformer extends Transformer<Search> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String propName;

    public SubstituteInfoTransformer(String propName) {
        this.propName = propName;
    }

    @Override
    public Object tr(Search search) {
        UIPropertySheet ancestorComponent = ComponentUtil.getAncestorComponent(search, UIPropertySheet.class, true);
        String username = (String) ancestorComponent.getNode().getProperties().get(propName);
        return UserUtil.getSubstitute(username);
    }
}
