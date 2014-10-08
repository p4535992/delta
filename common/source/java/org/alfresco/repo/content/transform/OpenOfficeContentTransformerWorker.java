<<<<<<< HEAD
/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.content.transform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jooreports.converter.DocumentFamily;
import net.sf.jooreports.converter.DocumentFormat;
import net.sf.jooreports.converter.DocumentFormatRegistry;
import net.sf.jooreports.converter.XmlDocumentFormatRegistry;
import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;
import net.sf.jooreports.openoffice.connection.OpenOfficeException;
import net.sf.jooreports.openoffice.converter.AbstractOpenOfficeDocumentConverter;
import net.sf.jooreports.openoffice.converter.OpenOfficeDocumentConverter;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.mso.service.MsoService;

/**
 * Makes use of the {@link http://sourceforge.net/projects/joott/JOOConverter} library to perform OpenOffice-drive
 * conversions.
 * 
 * @author Derek Hulley
 */
public class OpenOfficeContentTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker, InitializingBean
{
    private static Log log = LogFactory.getLog(OpenOfficeContentTransformerWorker.class);

    private OpenOfficeConnection connection;
    private AbstractOpenOfficeDocumentConverter converter;
    private String documentFormatsConfiguration;
    private DocumentFormatRegistry formatRegistry;
    private MsoService msoService;

    /**
     * @param connection
     *            the connection that the converter uses
     */
    public void setConnection(OpenOfficeConnection connection)
    {
        this.connection = connection;
    }

    /**
     * Explicitly set the converter to be used. The converter must use the same connection set in
     * {@link #setConnection(OpenOfficeConnection)}.
     * <p>
     * If not set, then the <code>OpenOfficeDocumentConverter</code> will be used.
     * 
     * @param converter
     *            the converter to use.
     */
    public void setConverter(AbstractOpenOfficeDocumentConverter converter)
    {
        this.converter = converter;
    }

    /**
     * Set a non-default location from which to load the document format mappings.
     * 
     * @param path
     *            a resource location supporting the <b>file:</b> or <b>classpath:</b> prefixes
     */
    public void setDocumentFormatsConfiguration(String path)
    {
        this.documentFormatsConfiguration = path;
    }

    public void setMsoService(MsoService msoService) {
        this.msoService = msoService;
    }

    public boolean isAvailable()
    {
        return this.connection.isConnected();
    }

    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory("OpenOfficeContentTransformerWorker", "connection", this.connection);

        // load the document conversion configuration
        if (this.documentFormatsConfiguration != null)
        {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            try
            {
                InputStream is = resourceLoader.getResource(this.documentFormatsConfiguration).getInputStream();
                this.formatRegistry = new XmlDocumentFormatRegistry(is);
            }
            catch (IOException e)
            {
                throw new AlfrescoRuntimeException("Unable to load document formats configuration file: "
                        + this.documentFormatsConfiguration);
            }
        }
        else
        {
            this.formatRegistry = new XmlDocumentFormatRegistry();
        }

        // set up the converter
        if (this.converter == null)
        {
            this.converter = new OpenOfficeDocumentConverter(this.connection);
        }
    }

    /**
     * @see DocumentFormatRegistry
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (MimetypeMap.MIMETYPE_PDF.equalsIgnoreCase(targetMimetype)) {
            // Refuse to produce PDF files with OpenOffice; use MS Office instead
            if (msoService.isAvailable() && msoService.isTransformableToPdf(sourceMimetype)) {
                return false;
            }
        }

        if (!isAvailable())
        {
            // The connection management is must take care of this
            return false;
        }

        // there are some conversions that fail, despite the converter believing them possible
        if (targetMimetype.equals(MimetypeMap.MIMETYPE_XHTML))
        {
            return false;
        }
        else if (targetMimetype.equals(MimetypeMap.MIMETYPE_WORDPERFECT))
        {
            return false;
        }
        else if (targetMimetype.equals(MimetypeMap.MIMETYPE_FLASH))
        {
            return false;
        }

        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = this.formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // no document format
            return false;
        }
        // query the registry for the target format
        DocumentFormat targetFormat = this.formatRegistry.getFormatByFileExtension(targetExtension);
        if (targetFormat == null)
        {
            // no document format
            return false;
        }

        // get the family of the target document
        DocumentFamily sourceFamily = sourceFormat.getFamily();
        // does the format support the conversion
        if (!targetFormat.isExportableFrom(sourceFamily))
        {
            // unable to export from source family of documents to the target format
            return false;
        }
        else
        {
            return true;
        }
    }

    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception
    {
        String sourceMimetype = getMimetype(reader);
        String targetMimetype = getMimetype(writer);

        log.info("Using OpenOffice for conversion: " + sourceMimetype + " -> " + targetMimetype);

        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = this.formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // source format is not recognised
            throw new ContentIOException("No OpenOffice document format for source extension: " + sourceExtension);
        }
        // query the registry for the target format
        DocumentFormat targetFormat = this.formatRegistry.getFormatByFileExtension(targetExtension);
        if (targetFormat == null)
        {
            // target format is not recognised
            throw new ContentIOException("No OpenOffice document format for target extension: " + targetExtension);
        }
        // get the family of the target document
        DocumentFamily sourceFamily = sourceFormat.getFamily();
        // does the format support the conversion
        if (!targetFormat.isExportableFrom(sourceFamily))
        {
            throw new ContentIOException("OpenOffice conversion not supported: \n" + "   reader: " + reader + "\n"
                    + "   writer: " + writer);
        }

        // create temporary files to convert from and to
        File tempFromFile = TempFileProvider.createTempFile("OpenOfficeContentTransformer-source-", "."
                + sourceExtension);
        File tempToFile = TempFileProvider
                .createTempFile("OpenOfficeContentTransformer-target-", "." + targetExtension);

        if (MimetypeMap.MIMETYPE_TEXT_PLAIN.equalsIgnoreCase(sourceMimetype)) {
            String content = reader.getContentString();

            String systemLineSeparator = System.getProperty("line.separator");
            // XXX On Linux, OpenOffice API fails to load plaintext files with Windows line breaks
            if ("\n".equals(systemLineSeparator)) {
                content = StringUtils.replace(content, "\r\n", "\n");
            }

            OutputStream output = new BufferedOutputStream(new FileOutputStream(tempFromFile));
            IOUtils.write(content, output, System.getProperty("file.encoding")); // XXX OpenOfficeDocumentConverter does not take reader.getEncoding() into account
                                                                                 //     then OpenOffice expects text file in system default encoding
                // Also tried to use "CharacterSet PropertyValue", but openoffice doesn't care about it
            IOUtils.closeQuietly(output);

        } else if (MimetypeMap.MIMETYPE_HTML.equalsIgnoreCase(sourceMimetype)) {
            String content = reader.getContentString();

            // OpenOffice does not care for "CharacterSet" PropertyValue, so we have to specify the charset inside html
            if (!Pattern.compile("<meta\\s+http-equiv=\"?content-type\"?\\s+content=", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
                Matcher headMatcher = Pattern.compile("<head[^>]*?>", Pattern.CASE_INSENSITIVE).matcher(content);
                String contentTypeHeader = "<meta http-equiv=\"content-type\" content=\"text/html; charset=" + reader.getEncoding() + "\">";
                if (headMatcher.find()) {
                    content = content.substring(0, headMatcher.end()) + contentTypeHeader + content.substring(headMatcher.end());
                } else {
                    Matcher htmlMatcher = Pattern.compile("<html[^>]*?>", Pattern.CASE_INSENSITIVE).matcher(content);
                    if (htmlMatcher.find()) {
                        content = content.substring(0, htmlMatcher.end()) + "<head>" + contentTypeHeader + "</head>" + content.substring(htmlMatcher.end());
                    } else {
                        log.debug("When parsing HTML contents, didn't find 'content-type' HTML header and didn't find HTML start tag either; OpenOffice probably fails to load this file; size="
                                        + reader.getSize() + " bytes, encoding=" + reader.getEncoding());
                    }
                }
            }

            OutputStream output = new BufferedOutputStream(new FileOutputStream(tempFromFile));
            IOUtils.write(content, output, reader.getEncoding());
            IOUtils.closeQuietly(output);
        } else {
            // download the content from the source reader
            reader.getContent(tempFromFile);
        }

        long startTime = System.nanoTime();
        try
        {
            this.converter.convert(tempFromFile, sourceFormat, tempToFile, targetFormat);
            MonitoringUtil.logSuccess(MonitoredService.OUT_OPENOFFICE);
            // conversion success
        }
        catch (OpenOfficeException e)
        {
            MonitoringUtil.logError(MonitoredService.OUT_OPENOFFICE, e);
            throw new ContentIOException("OpenOffice server conversion failed: \n" + "   reader: " + reader + "\n"
                    + "   writer: " + writer + "\n" + "   from file: " + tempFromFile + "\n" + "   to file: "
                    + tempToFile, e);
        } 
        catch (RuntimeException e) 
        {
            MonitoringUtil.logError(MonitoredService.OUT_OPENOFFICE, e);
            throw e;
        } finally {
            StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_OOO, startTime);
        }

        // upload the temp output to the writer given us
        writer.putContent(tempToFile);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.content.transform.ContentTransformerWorker#getVersionString()
     */
    public String getVersionString()
    {
        // Actual version information owned by OpenOfficeConnectionTester
        return "";
    }
}
=======
/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.content.transform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jooreports.converter.DocumentFamily;
import net.sf.jooreports.converter.DocumentFormat;
import net.sf.jooreports.converter.DocumentFormatRegistry;
import net.sf.jooreports.converter.XmlDocumentFormatRegistry;
import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;
import net.sf.jooreports.openoffice.connection.OpenOfficeException;
import net.sf.jooreports.openoffice.converter.AbstractOpenOfficeDocumentConverter;
import net.sf.jooreports.openoffice.converter.OpenOfficeDocumentConverter;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.mso.service.MsoService;

/**
 * Makes use of the {@link http://sourceforge.net/projects/joott/JOOConverter} library to perform OpenOffice-drive
 * conversions.
 * 
 * @author Derek Hulley
 */
public class OpenOfficeContentTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker, InitializingBean
{
    private static Log log = LogFactory.getLog(OpenOfficeContentTransformerWorker.class);

    private OpenOfficeConnection connection;
    private AbstractOpenOfficeDocumentConverter converter;
    private String documentFormatsConfiguration;
    private DocumentFormatRegistry formatRegistry;
    private MsoService msoService;

    /**
     * @param connection
     *            the connection that the converter uses
     */
    public void setConnection(OpenOfficeConnection connection)
    {
        this.connection = connection;
    }

    /**
     * Explicitly set the converter to be used. The converter must use the same connection set in
     * {@link #setConnection(OpenOfficeConnection)}.
     * <p>
     * If not set, then the <code>OpenOfficeDocumentConverter</code> will be used.
     * 
     * @param converter
     *            the converter to use.
     */
    public void setConverter(AbstractOpenOfficeDocumentConverter converter)
    {
        this.converter = converter;
    }

    /**
     * Set a non-default location from which to load the document format mappings.
     * 
     * @param path
     *            a resource location supporting the <b>file:</b> or <b>classpath:</b> prefixes
     */
    public void setDocumentFormatsConfiguration(String path)
    {
        this.documentFormatsConfiguration = path;
    }

    public void setMsoService(MsoService msoService) {
        this.msoService = msoService;
    }

    public boolean isAvailable()
    {
        return this.connection.isConnected();
    }

    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory("OpenOfficeContentTransformerWorker", "connection", this.connection);

        // load the document conversion configuration
        if (this.documentFormatsConfiguration != null)
        {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            try
            {
                InputStream is = resourceLoader.getResource(this.documentFormatsConfiguration).getInputStream();
                this.formatRegistry = new XmlDocumentFormatRegistry(is);
            }
            catch (IOException e)
            {
                throw new AlfrescoRuntimeException("Unable to load document formats configuration file: "
                        + this.documentFormatsConfiguration);
            }
        }
        else
        {
            this.formatRegistry = new XmlDocumentFormatRegistry();
        }

        // set up the converter
        if (this.converter == null)
        {
            this.converter = new OpenOfficeDocumentConverter(this.connection);
        }
    }

    /**
     * @see DocumentFormatRegistry
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (MimetypeMap.MIMETYPE_PDF.equalsIgnoreCase(targetMimetype)) {
            // Refuse to produce PDF files with OpenOffice; use MS Office instead
            if (msoService.isAvailable() && msoService.isTransformableToPdf(sourceMimetype)) {
                return false;
            }
        }

        if (!isAvailable())
        {
            // The connection management is must take care of this
            return false;
        }

        // there are some conversions that fail, despite the converter believing them possible
        if (targetMimetype.equals(MimetypeMap.MIMETYPE_XHTML))
        {
            return false;
        }
        else if (targetMimetype.equals(MimetypeMap.MIMETYPE_WORDPERFECT))
        {
            return false;
        }
        else if (targetMimetype.equals(MimetypeMap.MIMETYPE_FLASH))
        {
            return false;
        }

        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = this.formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // no document format
            return false;
        }
        // query the registry for the target format
        DocumentFormat targetFormat = this.formatRegistry.getFormatByFileExtension(targetExtension);
        if (targetFormat == null)
        {
            // no document format
            return false;
        }

        // get the family of the target document
        DocumentFamily sourceFamily = sourceFormat.getFamily();
        // does the format support the conversion
        if (!targetFormat.isExportableFrom(sourceFamily))
        {
            // unable to export from source family of documents to the target format
            return false;
        }
        else
        {
            return true;
        }
    }

    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception
    {
        String sourceMimetype = getMimetype(reader);
        String targetMimetype = getMimetype(writer);

        log.info("Using OpenOffice for conversion: " + sourceMimetype + " -> " + targetMimetype);

        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = this.formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // source format is not recognised
            throw new ContentIOException("No OpenOffice document format for source extension: " + sourceExtension);
        }
        // query the registry for the target format
        DocumentFormat targetFormat = this.formatRegistry.getFormatByFileExtension(targetExtension);
        if (targetFormat == null)
        {
            // target format is not recognised
            throw new ContentIOException("No OpenOffice document format for target extension: " + targetExtension);
        }
        // get the family of the target document
        DocumentFamily sourceFamily = sourceFormat.getFamily();
        // does the format support the conversion
        if (!targetFormat.isExportableFrom(sourceFamily))
        {
            throw new ContentIOException("OpenOffice conversion not supported: \n" + "   reader: " + reader + "\n"
                    + "   writer: " + writer);
        }

        // create temporary files to convert from and to
        File tempFromFile = TempFileProvider.createTempFile("OpenOfficeContentTransformer-source-", "."
                + sourceExtension);
        File tempToFile = TempFileProvider
                .createTempFile("OpenOfficeContentTransformer-target-", "." + targetExtension);

        if (MimetypeMap.MIMETYPE_TEXT_PLAIN.equalsIgnoreCase(sourceMimetype)) {
            String content = reader.getContentString();

            String systemLineSeparator = System.getProperty("line.separator");
            // XXX On Linux, OpenOffice API fails to load plaintext files with Windows line breaks
            if ("\n".equals(systemLineSeparator)) {
                content = StringUtils.replace(content, "\r\n", "\n");
            }

            OutputStream output = new BufferedOutputStream(new FileOutputStream(tempFromFile));
            IOUtils.write(content, output, System.getProperty("file.encoding")); // XXX OpenOfficeDocumentConverter does not take reader.getEncoding() into account
                                                                                 //     then OpenOffice expects text file in system default encoding
                // Also tried to use "CharacterSet PropertyValue", but openoffice doesn't care about it
            IOUtils.closeQuietly(output);

        } else if (MimetypeMap.MIMETYPE_HTML.equalsIgnoreCase(sourceMimetype)) {
            String content = reader.getContentString();

            // OpenOffice does not care for "CharacterSet" PropertyValue, so we have to specify the charset inside html
            if (!Pattern.compile("<meta\\s+http-equiv=\"?content-type\"?\\s+content=", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
                Matcher headMatcher = Pattern.compile("<head[^>]*?>", Pattern.CASE_INSENSITIVE).matcher(content);
                String contentTypeHeader = "<meta http-equiv=\"content-type\" content=\"text/html; charset=" + reader.getEncoding() + "\">";
                if (headMatcher.find()) {
                    content = content.substring(0, headMatcher.end()) + contentTypeHeader + content.substring(headMatcher.end());
                } else {
                    Matcher htmlMatcher = Pattern.compile("<html[^>]*?>", Pattern.CASE_INSENSITIVE).matcher(content);
                    if (htmlMatcher.find()) {
                        content = content.substring(0, htmlMatcher.end()) + "<head>" + contentTypeHeader + "</head>" + content.substring(htmlMatcher.end());
                    } else {
                        log.debug("When parsing HTML contents, didn't find 'content-type' HTML header and didn't find HTML start tag either; OpenOffice probably fails to load this file; size="
                                        + reader.getSize() + " bytes, encoding=" + reader.getEncoding());
                    }
                }
            }

            OutputStream output = new BufferedOutputStream(new FileOutputStream(tempFromFile));
            IOUtils.write(content, output, reader.getEncoding());
            IOUtils.closeQuietly(output);
        } else {
            // download the content from the source reader
            reader.getContent(tempFromFile);
        }

        long startTime = System.nanoTime();
        try
        {
            this.converter.convert(tempFromFile, sourceFormat, tempToFile, targetFormat);
            MonitoringUtil.logSuccess(MonitoredService.OUT_OPENOFFICE);
            // conversion success
        }
        catch (OpenOfficeException e)
        {
            MonitoringUtil.logError(MonitoredService.OUT_OPENOFFICE, e);
            throw new ContentIOException("OpenOffice server conversion failed: \n" + "   reader: " + reader + "\n"
                    + "   writer: " + writer + "\n" + "   from file: " + tempFromFile + "\n" + "   to file: "
                    + tempToFile, e);
        } 
        catch (RuntimeException e) 
        {
            MonitoringUtil.logError(MonitoredService.OUT_OPENOFFICE, e);
            throw e;
        } finally {
            StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_OOO, startTime);
        }

        // upload the temp output to the writer given us
        writer.putContent(tempToFile);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.content.transform.ContentTransformerWorker#getVersionString()
     */
    public String getVersionString()
    {
        // Actual version information owned by OpenOfficeConnectionTester
        return "";
    }
}
>>>>>>> develop-5.1
