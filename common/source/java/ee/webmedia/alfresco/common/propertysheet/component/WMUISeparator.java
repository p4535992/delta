<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.component;

import static ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty.DONT_RENDER_IF_DISABLED_ATTR;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UISeparator;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Separator that receives custom attributes from ancestor class (PropertySheetItem) so it could be potentially displayed conditionally
 * 
 * @author Ats Uiboupin
 */
public class WMUISeparator extends UISeparator {

    @Override
    protected void generateItem(FacesContext context, UIPropertySheet propSheet) throws IOException {
        String componentGeneratorName = this.getComponentGenerator();
        if (componentGeneratorName == null) {
            componentGeneratorName = RepoConstants.GENERATOR_SEPARATOR;
        }

        // use componentGenerator that also receives custom attributes from configuration
        UIComponent separator = getComponentGenerator(context, componentGeneratorName).
                generateAndAdd(context, propSheet, this);
    }

    @Override
    public boolean isRendered() {
        List<UIComponent> children = ComponentUtil.getChildren(this);
        if (!children.isEmpty()) {
            UIComponent child = children.get(0);
            if (Boolean.TRUE.equals(child.getAttributes().get(DONT_RENDER_IF_DISABLED_ATTR)) && ComponentUtil.isComponentDisabledOrReadOnly(child)) {
                return false;
            }
        }
        return super.isRendered();
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.component;

import static ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty.DONT_RENDER_IF_DISABLED_ATTR;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UISeparator;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Separator that receives custom attributes from ancestor class (PropertySheetItem) so it could be potentially displayed conditionally
 */
public class WMUISeparator extends UISeparator {

    @Override
    protected void generateItem(FacesContext context, UIPropertySheet propSheet) throws IOException {
        String componentGeneratorName = this.getComponentGenerator();
        if (componentGeneratorName == null) {
            componentGeneratorName = RepoConstants.GENERATOR_SEPARATOR;
        }

        // use componentGenerator that also receives custom attributes from configuration
        UIComponent separator = getComponentGenerator(context, componentGeneratorName).
                generateAndAdd(context, propSheet, this);
    }

    @Override
    public boolean isRendered() {
        List<UIComponent> children = ComponentUtil.getChildren(this);
        if (!children.isEmpty()) {
            UIComponent child = children.get(0);
            if (Boolean.TRUE.equals(child.getAttributes().get(DONT_RENDER_IF_DISABLED_ATTR)) && ComponentUtil.isComponentDisabledOrReadOnly(child)) {
                return false;
            }
        }
        return super.isRendered();
    }

}
>>>>>>> develop-5.1
