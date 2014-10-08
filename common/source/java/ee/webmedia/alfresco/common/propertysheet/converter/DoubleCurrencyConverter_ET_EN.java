<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.DoubleConverter;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;

/**
 * Number converter that allows both dot and comma as decimal separator
 * and allows whitespaces (including non-breaking space) between digits (i.e. grouping is supported).
 * In output string replaces non-breaking spaces with regular spaces to avoid dealing with non-breaking spaces in javascript.
 * Round both input and output to two decimal numbers (i.e. saving more than two decimal places is not allowed)
 * 
 * @author Riina Tens
 */
public class DoubleCurrencyConverter_ET_EN extends DoubleConverter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        String modifiedValue = prepareDoubleString(value);
        // assume that super.getAsObject performs no rounding
        Double exactDouble = (Double) super.getAsObject(facesContext, uiComponent, modifiedValue);

        return exactDouble != null ? EInvoiceUtil.roundDouble2Decimals(exactDouble) : null;
    }

    public static String prepareDoubleString(String value) {
        String modifiedValue = value;
        if (modifiedValue != null) {
            modifiedValue = StringUtils.replace(modifiedValue, ",", ".");
            modifiedValue = StringUtils.deleteWhitespace(modifiedValue);
            modifiedValue = replaceNonBreakingSpace(modifiedValue, false);
        }
        return modifiedValue;
    }

    private static String replaceNonBreakingSpace(String modifiedValue, boolean addSpace) {
        StringBuilder sb = new StringBuilder();
        // remove non-breaking spaces
        for (int i = 0; i < modifiedValue.length(); i++) {
            int code = modifiedValue.codePointAt(i);
            if (code != 160) {
                sb.append(modifiedValue.charAt(i));
            } else if (addSpace) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return getAsString(value);
    }

    public String getAsString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            double number = ((Number) value).doubleValue();
            number = EInvoiceUtil.roundDouble2Decimals(number);
            String numberStr = EInvoiceUtil.getInvoiceNumberFormat().format(number);
            return replaceNonBreakingSpace(numberStr, true);
        } catch (Exception e) {
            throw new ConverterException(e);
        }
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.DoubleConverter;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;

/**
 * Number converter that allows both dot and comma as decimal separator
 * and allows whitespaces (including non-breaking space) between digits (i.e. grouping is supported).
 * In output string replaces non-breaking spaces with regular spaces to avoid dealing with non-breaking spaces in javascript.
 * Round both input and output to two decimal numbers (i.e. saving more than two decimal places is not allowed)
 */
public class DoubleCurrencyConverter_ET_EN extends DoubleConverter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        String modifiedValue = prepareDoubleString(value);
        // assume that super.getAsObject performs no rounding
        Double exactDouble = (Double) super.getAsObject(facesContext, uiComponent, modifiedValue);

        return exactDouble != null ? EInvoiceUtil.roundDouble2Decimals(exactDouble) : null;
    }

    public static String prepareDoubleString(String value) {
        String modifiedValue = value;
        if (modifiedValue != null) {
            modifiedValue = StringUtils.replace(modifiedValue, ",", ".");
            modifiedValue = StringUtils.deleteWhitespace(modifiedValue);
            modifiedValue = replaceNonBreakingSpace(modifiedValue, false);
        }
        return modifiedValue;
    }

    private static String replaceNonBreakingSpace(String modifiedValue, boolean addSpace) {
        StringBuilder sb = new StringBuilder();
        // remove non-breaking spaces
        for (int i = 0; i < modifiedValue.length(); i++) {
            int code = modifiedValue.codePointAt(i);
            if (code != 160) {
                sb.append(modifiedValue.charAt(i));
            } else if (addSpace) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return getAsString(value);
    }

    public String getAsString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            double number = ((Number) value).doubleValue();
            number = EInvoiceUtil.roundDouble2Decimals(number);
            String numberStr = EInvoiceUtil.getInvoiceNumberFormat().format(number);
            return replaceNonBreakingSpace(numberStr, true);
        } catch (Exception e) {
            throw new ConverterException(e);
        }
    }

}
>>>>>>> develop-5.1
