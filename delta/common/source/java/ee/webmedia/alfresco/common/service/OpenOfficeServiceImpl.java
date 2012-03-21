package ee.webmedia.alfresco.common.service;

import java.io.File;
import java.util.Map;

import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XRefreshable;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;

public class OpenOfficeServiceImpl implements OpenOfficeService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OpenOfficeServiceImpl.class);

    private MimetypeService mimetypeService;
    private OpenOfficeConnection openOfficeConnection;

    @Override
    public void replace(ContentReader reader, ContentWriter writer, Map<String, String> formulas) throws Exception {
        if (1 == 1) {
            // Temporary safeguard until OO document templates & formulas are properly implemented
            throw new RuntimeException(
                    "Generating Word files or replacing formulas is not supported with OpenOffice; a more user-friendly exception should have been thrown by previous code");
        }

        // create temporary file to replace from
        File tempFromFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "."
                + mimetypeService.getExtension(reader.getMimetype()));
        File tempToFile = null;
        try {
            log.debug("Copying file from contentstore to temporary file: " + tempFromFile + "\n  " + reader);
            reader.getContent(tempFromFile);

            long startTime = System.currentTimeMillis();
            try {
                synchronized (openOfficeConnection) {
                    String tempFromFileurl = toUrl(tempFromFile);
                    log.debug("Loading file into OpenOffice from URL: " + tempFromFileurl);
                    XComponent xComponent = loadComponent(tempFromFileurl);

                    XTextDocument xTextDocument = queryInterface(XTextDocument.class, xComponent);
                    XSearchDescriptor xSearchDescriptor;
                    XSearchable xSearchable = queryInterface(XSearchable.class, xTextDocument);

                    // You need a descriptor to set properies for Replace
                    xSearchDescriptor = xSearchable.createSearchDescriptor();
                    xSearchDescriptor.setPropertyValue("SearchRegularExpression", Boolean.TRUE);
                    // Set the properties the replace method need
                    xSearchDescriptor.setSearchString(REGEXP_PATTERN);
                    XIndexAccess findAll = xSearchable.findAll(xSearchDescriptor);
                    log.debug("Processing file contents, found " + findAll.getCount() + " pattern matches");
                    for (int i = 0; i < findAll.getCount(); i++) {
                        Object byIndex = findAll.getByIndex(i);
                        XTextRange xTextRange = queryInterface(XTextRange.class, byIndex);
                        if (xTextRange.getString().length() < 3) {
                            continue;
                        }
                        String formulaKey = xTextRange.getString().substring(1, xTextRange.getString().length() - 1);
                        String formulaValue = formulas.get(formulaKey);
                        if (formulaValue == null) {
                            /*
                             * Spetsifikatsioon "Dokumendi ekraanivorm - Tegevused.docx" punkt 7.1.5.2
                             * Kui vastav metaandme väli on täitmata, siis asendamist ei toimu.
                             */
                            continue;
                        }
                        // New paragraph because justified text screws up the layout when \n is used.
                        formulaValue = StringUtils.replace(formulaValue, "\n", "\r");
                        xTextRange.setString(formulaValue);
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
                    storeProps[0].Value = "MS Word 97"; // "Microsoft Word 97/2000/XP"
                    XStorable storable = queryInterface(XStorable.class, xComponent);
                    storable.storeToURL(tempToFileUrl, storeProps); // Second replacing run requires new URL
                }
            } finally {
                StatisticsPhaseListener.addTiming(StatisticsPhaseListenerLogColumn.SRV_OOO, System.currentTimeMillis() - startTime);
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

    private static <T> T queryInterface(Class<T> clazz, Object argument) throws OpenOfficeReturnedNullInterfaceException {
        Assert.notNull(clazz);
        @SuppressWarnings("unchecked")
        T clazzInstance = (T) UnoRuntime.queryInterface(clazz, argument);
        if (clazzInstance == null) {
            throw new OpenOfficeReturnedNullInterfaceException(clazz);
        }
        return clazzInstance;
    }

    private XComponent loadComponent(String loadUrl) throws IOException, IllegalArgumentException {
        XComponentLoader desktop = openOfficeConnection.getDesktop();
        PropertyValue[] loadProps = new PropertyValue[1];
        loadProps[0] = new PropertyValue();
        loadProps[0].Name = "Hidden";
        loadProps[0].Value = Boolean.TRUE;
        // load
        return desktop.loadComponentFromURL(loadUrl, "_blank", 0, loadProps);
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
