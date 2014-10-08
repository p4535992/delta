package ee.webmedia.alfresco.common.constraint;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.dictionary.constraint.AbstractConstraint;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Alfresco Repository constraint that checks if value is a String (d:text) and an Enum name.
 */
public class EnumConstraint<T extends Enum<T>> extends AbstractConstraint {

    private Class<T> enumClass;

    @Override
    public void initialize() {
        checkPropertyNotNull("enumClass", enumClass);
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("enumClass", enumClass);
        return params;
    }

    @Override
    protected void evaluateSingleValue(Object value) {
        if (!(value instanceof String)) {
            throw new ConstraintException(ERR_EVALUATE_EXCEPTION, getClass().getSimpleName(), "value is not a String");
        }
        if (StringUtils.equals(ComponentUtil.DEFAULT_SELECT_VALUE, (String) value)) {
            return; // workaround for the web-client, that can't use null-value for SelectItem
        }
        try {
            Enum.valueOf(enumClass, (String) value);
        } catch (IllegalArgumentException e) {
            throw new ConstraintException(ERR_EVALUATE_EXCEPTION, getClass().getSimpleName(), e.getMessage());
        }
    }

    public Class<T> getEnumClass() {
        return enumClass;
    }

    public void setEnumClass(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

}
