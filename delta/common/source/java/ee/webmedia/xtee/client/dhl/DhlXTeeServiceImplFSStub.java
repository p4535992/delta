package ee.webmedia.xtee.client.dhl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Implementation of DhlXTeeServiceImplStub that creates <dhl:dokument> element from file in File System
 * 
 * @author Ats Uiboupin
 */
public class DhlXTeeServiceImplFSStub extends DhlXTeeServiceImplStub {
    public static String BEAN_NAME = "stubDhlXTeeService";
    private File dvkXmlFile;

    @Override
    public String getDvkDokumentXml() {

        BufferedReader reader = null;
        try {
            StringBuilder fileData = new StringBuilder(1000);
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(dvkXmlFile), "UTF-8"));
            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                fileData.append(buf, 0, numRead);
                buf = new char[1024];
            }
            String wrapperElem = "<dokumendid>";
            if (fileData.indexOf(wrapperElem) == 0) { // remove wrapper element if it exists
                return fileData.substring(wrapperElem.length(), fileData.length() - wrapperElem.length() - 1).toString();
            }
            return fileData.toString();
        } catch (IOException e) {
            throw new UnableToPerformException("Failed to read data from file : '" + dvkXmlFile + "'", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public void setDvkXmlFile(String file) {
        File tmp = new File(file);
        if (!tmp.exists()) {
            throw new UnableToPerformException("File doesn't exist: '" + file + "'");
        }
        dvkXmlFile = tmp;
    }

}
