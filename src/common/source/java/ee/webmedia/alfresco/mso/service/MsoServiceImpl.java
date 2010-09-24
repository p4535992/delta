package ee.webmedia.alfresco.mso.service;

import java.util.HashSet;
import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.ws.BindingProvider;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.utils.ContentReaderDataSource;
import ee.webmedia.mso.Mso;
import ee.webmedia.mso.MsoInput;
import ee.webmedia.mso.MsoOutput;

public class MsoServiceImpl implements MsoService, InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MsoServiceImpl.class);

    private String endpointAddress;

    private Mso mso;
    
    private Set<String> supportedSourceMimetypes;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(endpointAddress)) {
//            if (log.isDebugEnabled()) {
                log.debug("Mso service endpoint address not set");
//            }
            return;
        }

//        if (log.isDebugEnabled()) {
            log.debug("Initializing Mso service port");
//        }
        Mso port = (new ee.webmedia.mso.MsoService()).getMsoPort();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);

        // Set HTTP request read timeout at CXF layer
        // http://lhein.blogspot.com/2008/09/apache-cxf-and-time-outs.html
        HTTPConduit http = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        http.getClient().setReceiveTimeout(120000); // 2 minutes timeout

        mso = port;
        if (log.isDebugEnabled()) {
            log.debug("Successfully initialized Mso service port and set endpoint address: " + endpointAddress);
        }

        supportedSourceMimetypes = new HashSet<String>();
        supportedSourceMimetypes.add("application/msword"); // DOC
        supportedSourceMimetypes.add("application/rtf"); // RTF
        supportedSourceMimetypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document"); // DOCX
        // currently DOT/DOTM/DOTX/DOCM are assigned mime-type application/octet-stream, so we don't support them
    }

    @Override
    public boolean isMsoAvailable() {
        return mso != null;
    }

    @Override
    public boolean isTransformableToPdf(String sourceMimetype) {
        return supportedSourceMimetypes.contains(StringUtils.lowerCase(sourceMimetype));
    }

    @Override
    public void transformToPdf(ContentReader reader, ContentWriter writer) throws Exception {
        if (mso == null) {
            throw new IllegalStateException("Mso service is not available");
        }
        if (!MimetypeMap.MIMETYPE_PDF.equalsIgnoreCase(writer.getMimetype())) {
            throw new IllegalArgumentException("Only target mime type " + MimetypeMap.MIMETYPE_PDF + " is supported");
        }
        if (reader == null) {
            return;
        }
        if (!isTransformableToPdf(reader.getMimetype())) {
            throw new IllegalArgumentException("Source mime type is not supported: " + reader.getMimetype());
        }

        long startTime = System.currentTimeMillis();

        MsoInput msoInput = new MsoInput();
        ContentReaderDataSource dataSource = new ContentReaderDataSource(reader, null);
//        try {
            msoInput.setContent(new DataHandler(dataSource));
    //        if (log.isDebugEnabled()) {
                log.debug("Sending request to perform Mso.convertToPdf, reader=" + reader);
    //        }
            MsoOutput msoOutput = mso.convertToPdf(msoInput);
    
            String mimeType = msoOutput.getContent().getContentType();
            writer.setMimetype(mimeType);
            writer.setEncoding("UTF-8"); // reset encoding to default
            writer.putContent(msoOutput.getContent().getInputStream());
    
    //        if (log.isDebugEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Completed Mso.transformToPdf in " + duration + " ms, writer=" + writer);
    //        }
//        } finally {
//            dataSource.closeInputStream();
//        }
    }

    public void setEndpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

}
