package ee.webmedia.alfresco.common.propertysheet.patternoutput;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;

/**
 * @see PatternOutput
 * @author Alar Kvell
 */
public class PatternOutputGenerator extends BaseComponentGenerator implements HandlesViewMode {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(PatternOutput.class.getCanonicalName());
        FacesHelper.setupComponentId(context, component, id);

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        addValueFromCustomAttributes(PatternOutput.PATTERN_ATTR, attributes);

        return component;
    }

}
