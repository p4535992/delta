package ee.webmedia.alfresco.common.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XRefreshable;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.Closure;

public class OpenOfficeServiceImpl implements OpenOfficeService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OpenOfficeServiceImpl.class);

    private MimetypeService mimetypeService;
    private OpenOfficeConnection openOfficeConnection;
    private DocumentTemplateService documentTemplateService;

    @Override
    public boolean isAvailable() {
        return openOfficeConnection != null && openOfficeConnection.isConnected();
    }

    @Override
    public Map<String, String> getUsedFormulasAndValues(ContentReader fileContentReader) throws Exception {
        final Map<String, String> formulas = new HashMap<String, String>();
        doWithFields(fileContentReader, new Closure<XPropertySet>() {

            @Override
            public void exec(XPropertySet xPropertySet) {
                try {
                    String propertyName = StringUtils.removeStartIgnoreCase((String) xPropertySet.getPropertyValue("Hint"), "delta_");
                    String propertyValue = (String) xPropertySet.getPropertyValue("Content");
                    formulas.put(propertyName, propertyValue);
                } catch (UnknownPropertyException e) {
                    if (log.isDebugEnabled()) {
                        log.debug(e);
                    }
                } catch (WrappedTargetException e) {
                    if (log.isDebugEnabled()) {
                        log.debug(e);
                    }
                }
            }

        });

        return formulas;
    }

    @Override
    public Map<String, String> modifiedFormulas(ContentReader fileContentReader, NodeRef documentNodeRef, NodeRef fileNodeRef) throws Exception {
        final Map<String, String> formulas = documentTemplateService.getDocumentFormulas(documentNodeRef);
        final Map<String, String> templateDefaultValues = documentTemplateService.getDefaultFieldValues(fileNodeRef);
        final Map<String, String> modified = new HashMap<String, String>();

        doWithFields(fileContentReader, new Closure<XPropertySet>() {

            @Override
            public void exec(XPropertySet xPropertySet) {
                try {
                    String propertyName = StringUtils.removeStartIgnoreCase((String) xPropertySet.getPropertyValue("Hint"), "delta_");
                    String propertyValue = (String) xPropertySet.getPropertyValue("Content");
                    String value = formulas.get(propertyName);
                    String defaultValue = templateDefaultValues.get(propertyName);

                    if (!ObjectUtils.equals(defaultValue, propertyValue) && !ObjectUtils.equals(propertyValue, value)) {
                        modified.put(propertyName, propertyValue);
                    }
                } catch (UnknownPropertyException e) {
                    log.debug(e);
                } catch (WrappedTargetException e) {
                    log.debug(e);
                }
            }
        });

        return modified;
    }

    private XComponent loadXComponent(File modifiedFile) throws Exception, IllegalArgumentException, IOException, OpenOfficeReturnedNullInterfaceException {
        String tempFromFileurl = toUrl(modifiedFile);
        log.debug("Loading file into OpenOffice from URL: " + tempFromFileurl);
        return loadComponent(tempFromFileurl);
    }

    private XEnumeration loadFieldEnumeration(XComponent xComponent) throws OpenOfficeReturnedNullInterfaceException {
        XTextDocument xTextDocument = queryInterface(XTextDocument.class, xComponent);
        XTextFieldsSupplier supplier = (XTextFieldsSupplier) UnoRuntime.queryInterface(XTextFieldsSupplier.class, xTextDocument);
        XEnumerationAccess enumAccess = supplier.getTextFields();
        XEnumeration xEnum = enumAccess.createEnumeration();
        return xEnum;
    }

    @Override
    public void replace(ContentReader reader, ContentWriter writer, Map<String, String> formulas, boolean finalize) throws Exception {
        // create temporary file to replace from
        File tempFromFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "."
                + mimetypeService.getExtension(reader.getMimetype()));
        File tempToFile = null;
        try {
            log.debug("Copying file from contentstore to temporary file: " + tempFromFile + "\n  " + reader);
            reader.getContent(tempFromFile);

            long startTime = System.nanoTime();
            try {
                synchronized (openOfficeConnection) {
                    XComponent xComponent = loadXComponent(tempFromFile);
                    XEnumeration xEnum = loadFieldEnumeration(xComponent);

                    while (xEnum.hasMoreElements()) {
                        Object o = xEnum.nextElement();
                        XTextField text = (XTextField) UnoRuntime.queryInterface(XTextField.class, o);
                        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, text);
                        XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, text);
                        if (xServiceInfo.supportsService("com.sun.star.text.TextField.Input")) {
                            String propertyName = (String) xPropertySet.getPropertyValue("Hint");
                            String repoPropName = StringUtils.removeStartIgnoreCase(propertyName, "delta_");
                            String value = formulas.get(repoPropName);
                            if (StringUtils.isNotBlank(value)) {
                                setInputField(text, value);
                            }
                        }
                    }

                    if (finalize) {
                        removeEmptyBlocksAndDelimiters(xComponent);
                    }

                    XRefreshable refreshable = queryInterface(XRefreshable.class, xComponent);
                    if (refreshable != null) {
                        refreshable.refresh();
                    }

                    tempToFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "."
                            + mimetypeService.getExtension(reader.getMimetype()));
                    String tempToFileUrl = toUrl(tempToFile);
                    log.debug("Saving file from OpenOffice to URL: " + tempToFileUrl);
                    PropertyValue[] storeProps = new PropertyValue[1];
                    storeProps[0] = new PropertyValue();
                    storeProps[0].Name = "FilterName";
                    storeProps[0].Value = "StarOffice XML (Writer)"; // OpenOffice Text Document

                    XStorable storable = queryInterface(XStorable.class, xComponent);
                    storable.storeToURL(tempToFileUrl, storeProps); // Second replacing run requires new URL
                    MonitoringUtil.logSuccess(MonitoredService.OUT_OPENOFFICE);
                }
            } catch (Exception e) {
                MonitoringUtil.logError(MonitoredService.OUT_OPENOFFICE, e);
                throw e;
            } finally {
                StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_OOO, startTime);
            }

            writer.putContent(tempToFile);
            log.debug("Copied file back to contentstore from temporary file: " + tempToFile + "\n  "
                    + writer + "\nEntire replacement took " + (System.currentTimeMillis() - startTime) + " ms");
        } finally {
            if (tempFromFile != null && tempFromFile.exists()) {
                tempFromFile.delete();
            }
            if (tempToFile != null && tempToFile.exists()) {
                tempToFile.delete();
            }
        }
    }

    private void removeEmptyBlocksAndDelimiters(XComponent xComponent) throws OpenOfficeReturnedNullInterfaceException, UnknownPropertyException, PropertyVetoException,
    IllegalArgumentException, WrappedTargetException, IndexOutOfBoundsException {
        XSearchDescriptor xSearchDescriptor;
        XTextDocument xTextDocument = queryInterface(XTextDocument.class, xComponent);
        XSearchable xSearchable = queryInterface(XSearchable.class, xTextDocument);

        // You need a descriptor to set properies for Replace
        xSearchDescriptor = xSearchable.createSearchDescriptor();
        xSearchDescriptor.setPropertyValue("SearchRegularExpression", Boolean.TRUE);
        // Set the properties the replace method need
        xSearchDescriptor.setSearchString(REGEXP_GROUP_PATTERN);
        XIndexAccess findAll = xSearchable.findAll(xSearchDescriptor);
        log.debug("Processing file contents, found " + findAll.getCount() + " pattern matches");
        for (int i = 0; i < findAll.getCount(); i++) {
            Object byIndex = findAll.getByIndex(i);
            XTextRange xTextRange = queryInterface(XTextRange.class, byIndex);
            if (xTextRange.getString().length() < 5) {
                continue;
            }
            // FIXME TODO KAAREL - Siin peaks otsima tekstiala sees olevaid fielde ja kontrollima, kas neid on muudetud või mitte.
            xTextRange.setString(xTextRange.getString().substring(2, xTextRange.getString().length() - 2));
        }
    }

    private void doWithFields(ContentReader fileContentReader, Closure<XPropertySet> closure) throws Exception {
        File modifiedFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "."
                + mimetypeService.getExtension(fileContentReader.getMimetype()));

        try {
            log.debug("Copying file from contentstore to temporary file: " + modifiedFile + "\n  " + fileContentReader);
            fileContentReader.getContent(modifiedFile);

            long startTime = System.nanoTime();
            try {
                synchronized (openOfficeConnection) {
                    XComponent xComponent = loadXComponent(modifiedFile);
                    XEnumeration xEnum = loadFieldEnumeration(xComponent);
                    while (xEnum.hasMoreElements()) {
                        Object o = xEnum.nextElement();
                        XTextField text = (XTextField) UnoRuntime.queryInterface(XTextField.class, o);
                        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, text);
                        XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, text);
                        if (xServiceInfo.supportsService("com.sun.star.text.TextField.Input")) {
                            closure.exec(xPropertySet);
                        }
                    }
                    MonitoringUtil.logSuccess(MonitoredService.OUT_OPENOFFICE);
                }
            } catch (Exception e) {
                MonitoringUtil.logError(MonitoredService.OUT_OPENOFFICE, e);
                throw e;
            } finally {
                StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_OOO, startTime);
            }
        } finally {
            if (modifiedFile != null && modifiedFile.exists()) {
                modifiedFile.delete();
            }
        }
    }

    private static <T> T queryInterface(Class<T> clazz, Object argument) throws OpenOfficeReturnedNullInterfaceException {
        Assert.notNull(clazz);
        @SuppressWarnings("unchecked")
        T clazzInstance = (T) UnoRuntime.queryInterface(clazz, argument);
        if (clazzInstance == null) {
            throw new OpenOfficeReturnedNullInterfaceException(clazz);
        }
        return clazzInstance;
    }

    private XComponent loadComponent(String loadUrl) throws IllegalArgumentException, IOException {
        XComponentLoader desktop = openOfficeConnection.getDesktop();
        PropertyValue[] loadProps = new PropertyValue[1];
        loadProps[0] = new PropertyValue();
        loadProps[0].Name = "Hidden";
        loadProps[0].Value = Boolean.TRUE;
        // load
        return desktop.loadComponentFromURL(loadUrl, "_blank", 0, loadProps);
    }

    private void setInputField(XTextField field, String value) throws UnknownPropertyException, PropertyVetoException,
            IllegalArgumentException, WrappedTargetException {
        String newValue = (value == null) ? "" : value.replaceAll("\\u0000", "");
        if (StringUtils.isBlank(newValue)) {
            return;
        }
        newValue = (newValue.contains("\r\n")) ? newValue.replace("\r", "") : newValue.replace("\r", "\n");
        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, field);
        xPropertySet.setPropertyValue("Content", newValue);
    }

    private String toUrl(File file) throws Exception {
        Object contentProvider = openOfficeConnection.getFileContentProvider();
        XFileIdentifierConverter fic = queryInterface(XFileIdentifierConverter.class, contentProvider);
        return fic.getFileURLFromSystemPath("", file.getAbsolutePath());
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setOpenOfficeConnection(OpenOfficeConnection openOfficeConnection) {
        this.openOfficeConnection = openOfficeConnection;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }
}