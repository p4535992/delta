package ee.webmedia.xtee.client.dhl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DokumentDocument;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.MetainfoDocument.Metainfo;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.TagasisideType;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.SignedDocType;

/**
 * Base class that helps to imitate importing documents from DVK web service. Subclass just needs to <br>
 * 1) implement {@link #getDvkDokumentXml()} that returns String containing <dhl:dokument> element <br>
 * 2) call {@link #setHasDocuments(boolean)} method before executing {@link #receiveDocuments(int)}.
 * 
 * @author Ats Uiboupin
 */
public abstract class DhlXTeeServiceImplStub extends DhlXTeeServiceImpl {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DhlXTeeServiceImplStub.class);

    private boolean hasDocuments;

    /**
     * @return String representing xml containing &lt;dhl:dokument&gt; element similar that is returned by web service
     */
    public abstract String getDvkDokumentXml();

    @Override
    public ReceivedDocumentsWrapper receiveDocuments(int maxNrOfDocuments) {
        return new ReceivedDocumentsWrapperImplStub();
    }

    public class ReceivedDocumentsWrapperImplStub extends AbstractMap<String, ReceivedDocumentsWrapper.ReceivedDocument> implements ReceivedDocumentsWrapper {
        private final List<DhlDokumentType> receivedDocuments;
        private final Map<String /* dhlId */, ReceivedDocument> dhlDocumentsMap;
        private File responseXml;

        public ReceivedDocumentsWrapperImplStub() {
            List<DokumentDocument> dokumentDocuments;
            String xml = null;
            receivedDocuments = new ArrayList<DhlDokumentType>();
            dhlDocumentsMap = new HashMap<String, ReceivedDocument>();
            if (hasDocuments) {
                xml = getDvkDokumentXml();
                dokumentDocuments = getTypeFromSoapArray(xml, DokumentDocument.class);
                hasDocuments = false;
                int newOrInvalidDocCount = 0;
                for (DokumentDocument dokumentDocument : dokumentDocuments) {
                    DhlDokumentType dokument = dokumentDocument.getDokument();
                    if (dokument == null) {
                        continue;
                    }
                    receivedDocuments.add(dokument);
                }
                LOG.debug("received " + receivedDocuments.size() + " documents");
                for (DhlDokumentType dhlDokument : receivedDocuments) {
                    Metainfo metainfo = dhlDokument.getMetainfo();
                    Assert.notNull(metainfo, "dokument element doesn't contain metainfo element that should be added by dvk server");
                    MetainfoHelper metaInfoHelper = new MetainfoHelper(metainfo);
                    String dhlId = metaInfoHelper.getDhlId();
                    dhlDocumentsMap.put(dhlId, new ReceivedDocumentImpl(dhlDokument, dhlId, metaInfoHelper));
                }
                LOG.warn(newOrInvalidDocCount + " dhl:dokument elements out of " + receivedDocuments.size()
                        + " are invalid (or unsupported version of dhl:dokument) - those will not be processed");
                createTempFile(xml);
            }
        }

        private void createTempFile(String xml) {
            try {
                responseXml = File.createTempFile("DVK_receiveStub", ".xml");
                responseXml.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(responseXml);
                StringInputStream is = new StringInputStream(xml, "UTF-8");
                IOUtils.copy(is, fos);
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(fos);
            } catch (IOException e) {
                throw new UnableToPerformException(MessageSeverity.ERROR, "#Failed to create tempfile", e);
            }

        }

        private <T> List<T> getTypeFromSoapArray(String responseString, Class<T> responseClass) {
            SchemaType unencodedType = null;
            try {
                unencodedType = (SchemaType) responseClass.getField("type").get(null);
                LOG.debug("unencodedType=" + unencodedType);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get value of '" + responseClass.getCanonicalName() + ".type' to get corresponding SchemaType object: ", e);
            }
            if (StringUtils.isBlank(responseString)) {
                return Collections.emptyList();
            }
            ArrayList<T> result = null;
            try {
                String responseStringWrapped = "<root>" + responseString + "</root>";
                XmlOptions options = new XmlOptions();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Starting to parse '" + responseStringWrapped + "' to class: " + responseClass.getCanonicalName() + "\n\n");
                }
                XmlObject xmlObject = XmlObject.Factory.parse(responseStringWrapped, options);
                XmlCursor cursor = xmlObject.newCursor();
                cursor.toFirstChild();
                cursor.toFirstChild();
                options.setDocumentType(unencodedType);

                result = new ArrayList<T>();
                int i = 0;
                do {
                    if (LOG.isTraceEnabled()) {
                        cursor.getObject();
                        LOG.trace("Type of token " + (i++) + ": '" + cursor.currentTokenType() + "'");
                    }
                    @SuppressWarnings("unchecked")
                    T resultItem = (T) XmlObject.Factory.parse(cursor.getDomNode(), options);
                    result.add(resultItem);
                } while (cursor.toNextSibling());
                cursor.dispose();
            } catch (XmlException e) {
                throw new RuntimeException("Failed to parse '" + responseString + "' to class: " + responseClass.getCanonicalName(), e);
            }
            return result;
        }

        @Override
        public Iterator<String> iterator() {
            return dhlDocumentsMap.keySet().iterator();
        }

        @Override
        public Set<Entry<String, ReceivedDocument>> entrySet() {
            return dhlDocumentsMap.entrySet();
        }

        // START: getters/setters
        @Override
        public Map<String, ReceivedDocument> getDhlDocumentsMap() {
            return dhlDocumentsMap;
        }

        @Override
        public List<DhlDokumentType> getReceivedDocuments() {
            return receivedDocuments;
        }

        @Override
        public File getResponseDocumentsXml() {
            if (responseXml == null) {
                try {
                    responseXml = File.createTempFile("DVK_receiveStub", ".xml");
                    responseXml.deleteOnExit();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create stub file", e);
                }
            }
            return responseXml;
        }

        // END: getters/setters

        public class ReceivedDocumentImpl implements ReceivedDocument {
            private final String dhlId;
            private final MetainfoHelper metaInfoHelper;
            private final DhlDokumentType dhlDocument;

            public ReceivedDocumentImpl(DhlDokumentType dhlDocument, String dhlId, MetainfoHelper metaInfoHelper) {
                this.dhlDocument = dhlDocument;
                this.dhlId = dhlId;
                this.metaInfoHelper = metaInfoHelper;
            }

            @Override
            public DhlDokumentType getDhlDocument() {
                return dhlDocument;
            }

            @Override
            public SignedDocType getSignedDoc() {
                return dhlDocument.getSignedDoc();
            }

            // START: getters/setters
            public String getDhlId() {
                return dhlId;
            }

            @Override
            public MetainfoHelper getMetaInfoHelper() {
                return metaInfoHelper;
            }
            // END: getters/setters

        }

    }

    @Override
    public void markDocumentsReceived(Collection<String> receivedDocumentIds) {
        LOG.debug("NOT MARKING DOCUMENTS RECEIVED");
    }

    @Override
    public void markDocumentsReceivedV2(Collection<TagasisideType> receivedDocsInfos) {
        LOG.debug("NOT MARKING DOCUMENTS RECEIVED");
    }

    // START: getters/setters

    public void setHasDocuments(boolean hasDocuments) {
        this.hasDocuments = hasDocuments;
    }

    // END: getters/setters
}