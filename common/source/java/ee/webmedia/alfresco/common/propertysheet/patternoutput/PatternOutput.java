package ee.webmedia.alfresco.common.propertysheet.patternoutput;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIOutput;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.component.HandlesShowUnvalued;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.TextPatternUtil;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Outputs text, using {@code pattern} attribute and replacing formulas with node property values.
 * Formulas must be in the form of <code>{propertyLocalName}</code>.
 * Property values are taken from ancestor {@link UIPropertySheet} component's node and {@code docdyn:} namespace is used.
 * Cancellation groups in the form of <code>&#47;*text {propertyLocalName}*&#47;</code> are also supported.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class PatternOutput extends UIOutput implements HandlesShowUnvalued {

    public static final String PATTERN_ATTR = "pattern";

    private static final String CALCULATED_VALUE_ATTR = "_calculatedValue";
    private static final String HAS_NON_BLANK_FORMULA_VALUE_ATTR = "_hasNonBlankFormulaValue";

    @Override
    public Object getValue() {
        return getCalculatedValue();
    }

    @Override
    public boolean isShow() {
        getCalculatedValue();
        Boolean hasNonBlankFormulaValue = (Boolean) ComponentUtil.getAttributes(this).get(HAS_NON_BLANK_FORMULA_VALUE_ATTR);
        return hasNonBlankFormulaValue;
    }

    private String getCalculatedValue() {
        Map<String, Object> attributes = ComponentUtil.getAttributes(this);
        String calculatedValue = (String) attributes.get(CALCULATED_VALUE_ATTR);
        if (calculatedValue == null) {
            String pattern = (String) ComponentUtil.getAttributes(this).get(PATTERN_ATTR);
            Map<String, String> formulaValues = Collections.emptyMap();
            List<String> formulas = TextPatternUtil.getFormulas(pattern);
            boolean hasNonBlankFormulaValue = false;
            if (!formulas.isEmpty()) {
                formulaValues = getFormulaValues(formulas);
                hasNonBlankFormulaValue = !TextUtil.isBlank(formulaValues.values());
            }
            calculatedValue = StringUtils.defaultString(TextPatternUtil.getResult(pattern, formulaValues));
            attributes.put(HAS_NON_BLANK_FORMULA_VALUE_ATTR, hasNonBlankFormulaValue);
            attributes.put(CALCULATED_VALUE_ATTR, calculatedValue);
        }
        return calculatedValue;
    }

    private Map<String, String> getFormulaValues(List<String> formulas) {
        UIPropertySheet propSheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class, true);
        Node node = propSheet.getNode();
        return TextUtil.getFormulaValues(formulas, node);
    }

}
