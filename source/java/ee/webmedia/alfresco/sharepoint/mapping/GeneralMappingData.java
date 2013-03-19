package ee.webmedia.alfresco.sharepoint.mapping;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.dom4j.Element;

/**
 * @author Martti Tamm
 */
public class GeneralMappingData {

    private final String documentTypeElement;

    private final String directionElement;

    private final String subtypeElement;

    public GeneralMappingData(Element generalElement) {
        String docTypeEl = null;
        String directionEl = null;
        String subtypeEl = null;

        for (Object o : generalElement.elements()) {
            Element mapping = (Element) o;
            String to = mapping.attributeValue("to");

            if ("_documentTypeFrom".equals(to)) {
                docTypeEl = mapping.attributeValue("from");
            } else if ("_direction".equals(to)) {
                directionEl = mapping.attributeValue("from");
            } else if ("_subtype".equals(to)) {
                subtypeEl = mapping.attributeValue("from");
            }
        }

        documentTypeElement = docTypeEl != null ? docTypeEl : "contentType";
        directionElement = directionEl != null ? directionEl : "receivedSent";
        subtypeElement = subtypeEl != null ? subtypeEl : "documentSubspecies";
    }

    public String getDocumentTypeElement() {
        return documentTypeElement;
    }

    public String getDirectionElement() {
        return directionElement;
    }

    public String getSubtypeElement() {
        return subtypeElement;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
