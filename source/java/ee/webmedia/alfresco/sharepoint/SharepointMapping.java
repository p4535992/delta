package ee.webmedia.alfresco.sharepoint;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.sharepoint.mapping.DocumentMetadata;
import ee.webmedia.alfresco.sharepoint.mapping.GeneralMappingData;
import ee.webmedia.alfresco.sharepoint.mapping.MappedDocument;
import ee.webmedia.alfresco.sharepoint.mapping.Mapping;
import ee.webmedia.alfresco.sharepoint.mapping.PropMapping;
import ee.webmedia.alfresco.sharepoint.mapping.TypeInfo;
import ee.webmedia.alfresco.sharepoint.mapping.TypeInfoContext;

/**
 * Mapping class handles information from the mappings XML file and applies the read mapping when reading document XML files.
 * 
 * @author Aleksei Lissitsin
 * @author Martti Tamm
 */
public class SharepointMapping {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SharepointMapping.class);

    public static final String PREFIX_DEFAULT = "docdyn";
    public static final String PREFIX_DOCCOM = "doccom";
    private static final String PREFIX_CHILD = "docchild";

    // ====== SERVICES:

    private final NamespaceService namespaceService = BeanHelper.getNamespaceService();

    private final TypeInfoContext context = new TypeInfoContext();

    // ====== INSTANCE DATA:

    private final Map<String, String> restrictionValues = new HashMap<String, String>();

    private final Map<String, Mapping> docTypeMappings = new HashMap<String, Mapping>();

    private final GeneralMappingData generalSettings;

    /**
     * Initializes a new Sharepoint mapping instance by reading mapping information from the provided mappings file (XML).
     * <p>
     * When the mapping file is read, the data is also analyzed and check. Therefore this constructor may also throw a runtime exception when the target document type or document
     * property does not exist.
     * <p>
     * Also note that empty XML element bodies and empty attribute values are treated as <code>null</code>s not empty strings.
     * 
     * @param mappingsFile The mapping file to use.
     * @throws DocumentException When reading the mapping file fails.
     */
    public SharepointMapping(File mappingsFile, boolean amphora) throws DocumentException {
        log.info("Loading meta-data mappings from file: " + mappingsFile);
        Element root = new SAXReader().read(mappingsFile).getRootElement();

        Element restrictionElement = root.element("propValues");
        if (restrictionElement != null) {
            Assert.isTrue("accessRestriction".equals(restrictionElement.attributeValue("to")), "<propValues> attribute 'to' must be equal to 'accessRestriction'.");

            for (Object v : root.element("propValues").elements("value")) {
                Element value = (Element) v;
                restrictionValues.put(trimToNull(value.attributeValue("from")), trimToNull(value.attributeValue("to")));
            }
        }

        generalSettings = !amphora ? new GeneralMappingData(root.element("generalType")) : null;

        Mapping generalMapping = createGeneralMapping(root.element("generalType"));

        for (Object o : root.elements("documentType")) {
            Mapping m = createMapping(generalMapping, (Element) o);

            final String from = m.getFrom();
            Assert.isTrue(!docTypeMappings.containsKey(from), "Cannot have multiple documentType mappings from '" + from + "'");
            docTypeMappings.put(from, m);
        }

        if (log.isInfoEnabled()) {
            StringBuilder s = new StringBuilder("Loaded ").append(docTypeMappings.size()).append(" document type mappings:");
            for (Mapping m : docTypeMappings.values()) {
                s.append("\n").append(m);
            }
            s.append("\nGeneral mapping settings: ").append(generalSettings);
            s.append("\nAlso found following restriction value mappings:");
            for (Entry<String, String> entry : restrictionValues.entrySet()) {
                s.append("\n").append(entry);
            }
            log.info(s.toString());
        }
    }

    private Mapping createGeneralMapping(Element generalType) {
        Mapping result = new Mapping();

        TypeInfo typeInfo = context.begin(PREFIX_DOCCOM, "document");

        for (Object o : generalType.elements("prop")) {
            Element element = (Element) o;
            String to = element.attributeValue("to");

            if (to == null || !to.startsWith("_")) {
                result.add(new PropMapping(element, typeInfo));
            }
        }

        context.end();

        return result;
    }

    private Mapping createMapping(Mapping general, Element el) {
        String from = trimToNull(el.attributeValue("from"));
        String to = trimToNull(el.attributeValue("to"));
        String assoc = trimToNull(el.attributeValue("assoc"));
        boolean child = general == null;

        String prefix = child ? PREFIX_CHILD : PREFIX_DEFAULT;
        QName assocType = isNotEmpty(assoc) ? QName.createQName(prefix, assoc, namespaceService) : null;
        TypeInfo typeInfo = context.begin(prefix, to);

        Mapping result = new Mapping(general, from, typeInfo, assocType);

        for (Object o : el.elements("prop")) {
            result.add(new PropMapping((Element) o, typeInfo));
        }

        if (!child) {
            for (Object o : el.elements("child")) {
                result.add(createMapping(null, (Element) o));
            }
        }

        context.end();

        return result;
    }

    /**
     * Provides the substitute value when the provided value is overridden in the mappings file. When value is not overridden, it will be returned by this method.
     * 
     * @param originalValue The access restriction value to check for overridden value from mappings file.
     * @return The override value, or the passed in original value.
     */
    public String getRestrictionValue(String originalValue) {
        return restrictionValues.containsKey(originalValue) ? restrictionValues.get(originalValue) : originalValue;
    }

    /**
     * Reads well known and most important data from document XML. This data determines some aspects of document import, such as parent volume, the document type and document file
     * information.
     * 
     * @param docRoot Document XML root element.
     * @return Document meta-data.
     */
    public DocumentMetadata getMetadata(Element docRoot, File dirFiles, ImportSettings settings) {
        return DocumentMetadata.create(docRoot, dirFiles, generalSettings, settings);
    }

    /**
     * Reads document XML and returns a mapped document by the rules of mappings file. This method may also end with a runtime exception.
     * 
     * @param docRoot Document XML root element.
     * @return The mapped document.
     */
    public MappedDocument createMappedDocument(Element docRoot, DocumentMetadata meta) {
        Mapping mapping = getDocumentMapping(meta);
        return mapping != null ? new MappedDocument(docRoot, mapping) : null;
    }

    private Mapping getDocumentMapping(DocumentMetadata meta) {
        Mapping propMapping = null;

        if (StringUtils.isNotBlank(meta.getDirection())) {
            propMapping = docTypeMappings.get(meta.getDocumentType() + "-" + meta.getDirection());
        }

        if (propMapping == null && StringUtils.isNotBlank(meta.getSubtype())) {
            propMapping = docTypeMappings.get(meta.getDocumentType() + "-" + meta.getSubtype());
        }

        if (propMapping == null) {
            propMapping = docTypeMappings.get(meta.getDocumentType());
        }

        return propMapping;
    }

}
