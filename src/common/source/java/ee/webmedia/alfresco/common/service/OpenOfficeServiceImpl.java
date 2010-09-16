package ee.webmedia.alfresco.common.service;

import java.io.File;

import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.TempFileProvider;

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

public class OpenOfficeServiceImpl implements OpenOfficeService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OpenOfficeServiceImpl.class);

    private MimetypeService mimetypeService;
    private OpenOfficeConnection openOfficeConnection;

    @Override
    public void replace(ContentReader reader, ContentWriter writer, ReplaceCallback callback) throws Exception {
        long startTime = System.currentTimeMillis();

        // create temporary file to replace from
        File tempFromFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "."
                + mimetypeService.getExtension(reader.getMimetype()));
        File tempToFile;
        log.debug("Copying file from contentstore to temporary file: " + tempFromFile + "\n  " + reader);
        reader.getContent(tempFromFile);

        synchronized (openOfficeConnection) {
            String tempFromFileurl = toUrl(tempFromFile);
            log.debug("Loading file into OpenOffice from URL: " + tempFromFileurl);
            XComponent xComponent = loadComponent(tempFromFileurl);
    
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xComponent);
            XSearchDescriptor xSearchDescriptor;
            XSearchable xSearchable = null;
            xSearchable = (XSearchable) UnoRuntime.queryInterface(XSearchable.class, xTextDocument);
            // You need a descriptor to set properies for Replace
            xSearchDescriptor = xSearchable.createSearchDescriptor();
            xSearchDescriptor.setPropertyValue("SearchRegularExpression", Boolean.TRUE);
            // Set the properties the replace method need
            xSearchDescriptor.setSearchString(REGEXP_PATTERN);
            XIndexAccess findAll = xSearchable.findAll(xSearchDescriptor);
            log.debug("Processing file contents, found " + findAll.getCount() + " pattern matches");
            for (int i = 0; i < findAll.getCount(); i++) {
                Object byIndex = findAll.getByIndex(i);
                XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, byIndex);
                xTextRange.setString(callback.getReplace(xTextRange.getString()));
            }
            XRefreshable refreshable = (XRefreshable) UnoRuntime.queryInterface(XRefreshable.class, xComponent);
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
            XStorable storable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xComponent);
            storable.storeToURL(tempToFileUrl, storeProps); // Second replacing run requires new URL
        }

        writer.putContent(tempToFile);
        log.debug("Copied file back to contentstore from temporary file: " + tempToFile + "\n  " + writer + "\nEntire replacement took " + (System.currentTimeMillis() - startTime) + " ms");
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

    private String toUrl(File file) {
        Object contentProvider = openOfficeConnection.getFileContentProvider();
        XFileIdentifierConverter fic = (XFileIdentifierConverter) UnoRuntime.queryInterface(XFileIdentifierConverter.class, contentProvider);
        return fic.getFileURLFromSystemPath("", file.getAbsolutePath());
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setOpenOfficeConnection(OpenOfficeConnection openOfficeConnection) {
        this.openOfficeConnection = openOfficeConnection;
    }

}
