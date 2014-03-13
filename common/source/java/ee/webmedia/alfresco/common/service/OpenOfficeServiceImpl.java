package ee.webmedia.alfresco.common.service;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XRefreshable;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.utils.Closure;

public class OpenOfficeServiceImpl implements OpenOfficeService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OpenOfficeServiceImpl.class);

    private MimetypeService mimetypeService;
    private OpenOfficeConnection openOfficeConnection;

    @Override
    public boolean isAvailable() {
        return openOfficeConnection != null && openOfficeConnection.isConnected();
    }

    @Override
    public Map<String, String> modifiedFormulas(ContentReader fileContentReader) throws Exception {
        final Map<String, String> formulas = new LinkedHashMap<String, String>();
        doWithFields(fileContentReader, new Closure<XPropertySet>() {

            @Override
            public void exec(XPropertySet xPropertySet) {
                try {
                    String propertyName = StringUtils.trim((String) xPropertySet.getPropertyValue("Hint"));
                    if (!StringUtils.startsWith(propertyName, "delta_")) {
                        return;
                    }

                    propertyName = StringUtils.removeStartIgnoreCase(propertyName, "delta_");
                    if (StringUtils.isBlank(propertyName)) {
                        return;
                    }

                    String propertyValue = (String) xPropertySet.getPropertyValue("Content");
                    if (propertyValue == null || (propertyValue.startsWith("{") && propertyValue.endsWith("}"))) {
                        propertyValue = "";
                    }

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
    public boolean replace(ContentReader reader, ContentWriter writer, Map<String, String> formulas, boolean finalize) throws Exception {
        // create temporary file to replace from
        File tempFromFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "."
                + mimetypeService.getExtension(reader.getMimetype()));
        File tempToFile = null;
        try {
            log.debug("Copying file from contentstore to temporary file: " + tempFromFile + "\n  " + reader);
            reader.getContent(tempFromFile);
            boolean fileActuallyChanged = false;
            long startTime = System.nanoTime();
            try {
                synchronized (openOfficeConnection) {
                    XComponent xComponent = loadXComponent(tempFromFile);
                    XEnumeration xEnum = loadFieldEnumeration(xComponent);

                    boolean atLeastOneDeltaFieldHasValue = false;
                    XTextDocument xTextDocument = queryInterface(XTextDocument.class, xComponent);
                    XText docText = xTextDocument.getText();
                    XTextCursor xTextCursor = docText.createTextCursor();
                    while (xEnum.hasMoreElements()) {
                        Object o = xEnum.nextElement();
                        XTextField text = (XTextField) UnoRuntime.queryInterface(XTextField.class, o);
                        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, text);
                        XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, text);
                        if (xServiceInfo.supportsService("com.sun.star.text.TextField.Input")) {
                            String propertyName = StringUtils.trim((String) xPropertySet.getPropertyValue("Hint"));

                            if (finalize) { // TODO Figure out a way to erase text ranges from header/footer (http://stackoverflow.com/a/580714)
                                XTextRange anchor = text.getAnchor();
                                if (COMMENT_END.equals(propertyName)) { // NB! OO.Org enumerates these elements like 0, n, n-1, n-2...1 :/
                                    anchor.setString(""); // Delete delimiter
                                    xTextCursor.gotoRange(anchor, false);
                                    atLeastOneDeltaFieldHasValue = false;
                                    fileActuallyChanged = true;
                                } else if (COMMENT_START.equals(propertyName)) {
                                    anchor.setString(""); // Delete delimiter
                                    fileActuallyChanged = true;
                                    if (!atLeastOneDeltaFieldHasValue) {
                                        xTextCursor.gotoRange(anchor, true); // Select text between delimiters
                                        xTextCursor.setString("");
                                    }
                                }
                            }

                            // Don't bother with foreign fields
                            if (!StringUtils.startsWith(propertyName, "delta_")) {
                                continue;
                            }

                            String repoPropName = StringUtils.removeStart(propertyName, "delta_");
                            String value = formulas.get(repoPropName);

                            if (finalize && (value == null || value.length() == 0 || (value.startsWith("{") && value.endsWith("}")))) {
                                text.getAnchor().setString(""); // Delete empty valued fields
                                fileActuallyChanged = true;
                                continue;
                            }

                            if (value != null && value.length() > 0 && (!value.startsWith("{") || !value.endsWith("}"))) {
                                atLeastOneDeltaFieldHasValue = true;
                            }

                            // Restore empty field contents when updating
                            if (value == null || value.length() == 0) {
                                value = "{" + repoPropName + "}";
                            }

                            fileActuallyChanged |= setInputField(text, value);
                        }
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
            return fileActuallyChanged;
        } finally {
            if (tempFromFile != null && tempFromFile.exists()) {
                tempFromFile.delete();
            }
            if (tempToFile != null && tempToFile.exists()) {
                tempToFile.delete();
            }
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

    private boolean setInputField(XTextField field, String value) throws UnknownPropertyException, PropertyVetoException,
            IllegalArgumentException, WrappedTargetException {
        String newValue = (value == null) ? "" : value.replaceAll("\\u0000", "");
        newValue = (newValue.contains("\r\n")) ? newValue.replace("\r", "") : newValue.replace("\r", "\n");
        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, field);
        String contentPropKey = "Content";
        Object existingValue = xPropertySet.getPropertyValue(contentPropKey);
        xPropertySet.setPropertyValue(contentPropKey, newValue);
        return existingValue instanceof String && StringUtils.equals((String) existingValue, newValue);
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

}