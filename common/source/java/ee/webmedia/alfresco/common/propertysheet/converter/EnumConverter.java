package ee.webmedia.alfresco.common.propertysheet.converter;

import static ee.webmedia.alfresco.utils.ComponentUtil.DEFAULT_SELECT_VALUE;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;

/**
 * Converter that converts String->String based on Enum name.
 */
public class EnumConverter implements Converter, StateHolder {
    private boolean trans;
    private String enumClass;

    @Override
    public String getAsString(FacesContext context, UIComponent enumSelect, Object value) throws ConverterException {
        try {
            return DefaultTypeConverter.INSTANCE.convert(String.class, value);
        } catch (Throwable e) {
            throw new ConverterException("Failed to convert to string", e);
        }
    }

    /**
     * @return String(when no matching enum constant is found, then the same value is returned as is used for default select value in {@link HtmlSelectOneMenu})
     */
    @Override
    public String getAsObject(FacesContext context, UIComponent enumSelect, String strEnumValue) throws ConverterException {
        try {
            Enum<?> enumValue = DefaultTypeConverter.INSTANCE.convert(getEnumClass(enumClass), strEnumValue);
            return enumValue == null ? DEFAULT_SELECT_VALUE : enumValue.name();
        } catch (Exception e) {
            throw new ConverterException("Failed to convert to enum", e);
        }
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        trans = (Boolean) values[0];
        enumClass = (String) values[1];
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[2];
        values[0] = trans;
        values[1] = enumClass;
        return values;
    }

    @Override
    public boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(boolean trans) {
        this.trans = trans;
    }

    public void setEnumClass(String enumClass) {
        this.enumClass = enumClass;
    }

    public String getEnumClass() {
        return enumClass;
    }

    public static Class<? extends Enum<?>> getEnumClass(String enumClassName) {
        Class<? extends Enum<?>> en = null;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> tmp = (Class<? extends Enum<?>>) ((Class<?>) Class.forName(enumClassName));
            en = tmp;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Enum class not found: '" + enumClassName + "'", e);
        }
        return en;
    }
}
