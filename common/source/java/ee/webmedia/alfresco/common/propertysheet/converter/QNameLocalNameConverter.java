<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.converter;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.validator.OnlyLettersValidator;
import ee.webmedia.alfresco.utils.MessageCreatorHelper;
import ee.webmedia.alfresco.utils.MessageCreatorHelper.ErrorMsgFormat;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Converter that can convert String to QName (if namespace is set) and always convert QName to String by returning localName of the QName
 * 
 * @author Ats Uiboupin
 */
public class QNameLocalNameConverter extends QNameConverter implements StateHolder {

    /** namespaceURI prefix with ":" or full namespaceURI */
    private boolean trans;
    /** required, otherwise only formatting can be done - exception will be thrown when trying to convert to QName */
    private String namespace;
    private String customErrorMsgPrefix;
    private ErrorMsgFormat customErrorMsgFormat;

    @Override
    public String getAsString(FacesContext context, UIComponent localNameInput, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        QName qName = (QName) value;
        return qName.getLocalName();
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent localNameInput, String localName) throws ConverterException {
        if (StringUtils.isBlank(namespace)) {
            throw new IllegalArgumentException("Can't convert localName to QName ('" + localName + "' should be localName of QName), because namespace uri is not set!");
        }
        MessageCreatorHelper errMsgCreator = OnlyLettersValidator.validateOnlyLetters(localName, localNameInput, new MessageCreatorHelper(customErrorMsgPrefix,
                customErrorMsgFormat));
        if (errMsgCreator.isMessageSet()) {
            throw new ConverterException(MessageUtil.getFacesMessage(errMsgCreator));
        }
        String namespaceURI = namespace;
        if (namespace.endsWith(":")) {
            namespaceURI = getNamespaceService().getNamespaceURI(namespace.substring(0, namespace.length() - 1));
        }
        return QName.createQName(namespaceURI, localName);
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        trans = (Boolean) values[0];
        namespace = (String) values[1];
        customErrorMsgPrefix = (String) values[2];
        customErrorMsgFormat = (ErrorMsgFormat) values[3];
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[4];
        values[0] = trans;
        values[1] = namespace;
        values[2] = customErrorMsgPrefix;
        values[3] = customErrorMsgFormat;
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

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setCustomErrorMsgPrefix(String customErrorMsgPrefix) {
        this.customErrorMsgPrefix = customErrorMsgPrefix;
    }

    public void setCustomErrorMsgFormat(String customErrorMsgFormat) {
        if (StringUtils.isNotBlank(customErrorMsgFormat)) {
            this.customErrorMsgFormat = ErrorMsgFormat.valueOf(customErrorMsgFormat);
        }
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.converter;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.validator.OnlyLettersValidator;
import ee.webmedia.alfresco.utils.MessageCreatorHelper;
import ee.webmedia.alfresco.utils.MessageCreatorHelper.ErrorMsgFormat;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Converter that can convert String to QName (if namespace is set) and always convert QName to String by returning localName of the QName
 */
public class QNameLocalNameConverter extends QNameConverter implements StateHolder {

    /** namespaceURI prefix with ":" or full namespaceURI */
    private boolean trans;
    /** required, otherwise only formatting can be done - exception will be thrown when trying to convert to QName */
    private String namespace;
    private String customErrorMsgPrefix;
    private ErrorMsgFormat customErrorMsgFormat;

    @Override
    public String getAsString(FacesContext context, UIComponent localNameInput, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        QName qName = (QName) value;
        return qName.getLocalName();
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent localNameInput, String localName) throws ConverterException {
        if (StringUtils.isBlank(namespace)) {
            throw new IllegalArgumentException("Can't convert localName to QName ('" + localName + "' should be localName of QName), because namespace uri is not set!");
        }
        MessageCreatorHelper errMsgCreator = OnlyLettersValidator.validateOnlyLetters(localName, localNameInput, new MessageCreatorHelper(customErrorMsgPrefix,
                customErrorMsgFormat));
        if (errMsgCreator.isMessageSet()) {
            throw new ConverterException(MessageUtil.getFacesMessage(errMsgCreator));
        }
        String namespaceURI = namespace;
        if (namespace.endsWith(":")) {
            namespaceURI = getNamespaceService().getNamespaceURI(namespace.substring(0, namespace.length() - 1));
        }
        return QName.createQName(namespaceURI, localName);
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        trans = (Boolean) values[0];
        namespace = (String) values[1];
        customErrorMsgPrefix = (String) values[2];
        customErrorMsgFormat = (ErrorMsgFormat) values[3];
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[4];
        values[0] = trans;
        values[1] = namespace;
        values[2] = customErrorMsgPrefix;
        values[3] = customErrorMsgFormat;
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

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setCustomErrorMsgPrefix(String customErrorMsgPrefix) {
        this.customErrorMsgPrefix = customErrorMsgPrefix;
    }

    public void setCustomErrorMsgFormat(String customErrorMsgFormat) {
        if (StringUtils.isNotBlank(customErrorMsgFormat)) {
            this.customErrorMsgFormat = ErrorMsgFormat.valueOf(customErrorMsgFormat);
        }
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
