<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.TextUtil;

public class DueDateDaysConverter implements Converter {

    public static final String CONVERTER_ID = DueDateDaysConverter.class.getCanonicalName();

    @Override
    public Object getAsObject(FacesContext paramFacesContext, UIComponent paramUIComponent, String paramString) throws ConverterException {
        try {
            if (StringUtils.isBlank(paramString)) {
                return null;
            }
            List<Object> values = new ArrayList<Object>();
            paramString = paramString.trim();
            int i = paramString.lastIndexOf(" ");
            if (i >= 0) {
                values.add(Integer.parseInt(paramString.substring(0, i)));
                values.add(Boolean.parseBoolean(paramString.substring(i + 1)));
                return values;
            }
            throw new ConverterException("Wrong format in number of days!");
        } catch (Exception e) {
            throw new ConverterException("Error parsing number of days!", e);
        }
    }

    @Override
    public String getAsString(FacesContext paramFacesContext, UIComponent paramUIComponent, Object paramObject) throws ConverterException {
        if (!(paramObject instanceof List)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) paramObject;
        if (values.get(0) == null || values.get(1) == null) {
            return null;
        }
        return TextUtil.joinStringAndStringWithSpace(values.get(0).toString(), values.get(1).toString());
    }
}
=======
package ee.webmedia.alfresco.workflow.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.TextUtil;

public class DueDateDaysConverter implements Converter {

    public static final String CONVERTER_ID = DueDateDaysConverter.class.getCanonicalName();

    @Override
    public Object getAsObject(FacesContext paramFacesContext, UIComponent paramUIComponent, String paramString) throws ConverterException {
        try {
            if (StringUtils.isBlank(paramString)) {
                return null;
            }
            List<Object> values = new ArrayList<Object>();
            paramString = paramString.trim();
            int i = paramString.lastIndexOf(" ");
            if (i >= 0) {
                values.add(Integer.parseInt(paramString.substring(0, i)));
                values.add(Boolean.parseBoolean(paramString.substring(i + 1)));
                return values;
            }
            throw new ConverterException("Wrong format in number of days!");
        } catch (Exception e) {
            throw new ConverterException("Error parsing number of days!", e);
        }
    }

    @Override
    public String getAsString(FacesContext paramFacesContext, UIComponent paramUIComponent, Object paramObject) throws ConverterException {
        if (!(paramObject instanceof List)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) paramObject;
        if (values.get(0) == null || values.get(1) == null) {
            return null;
        }
        return TextUtil.joinStringAndStringWithSpace(values.get(0).toString(), values.get(1).toString());
    }
}
>>>>>>> develop-5.1
