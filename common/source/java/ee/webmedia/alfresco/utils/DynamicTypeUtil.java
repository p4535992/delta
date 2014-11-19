package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;
import static ee.webmedia.alfresco.utils.RepoUtil.copyProperties;
import static ee.webmedia.alfresco.utils.RepoUtil.toQNameProperties;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DynamicTypeUtil {

    public static void setTypeProps(Pair<String, Integer> docTypeIdAndVersionNr, Map<QName, Serializable> props) {
        props.put(Props.OBJECT_TYPE_ID, docTypeIdAndVersionNr.getFirst());
        props.put(Props.OBJECT_TYPE_VERSION_NR, docTypeIdAndVersionNr.getSecond());
    }

    public static void setTypePropsStringMap(Pair<String, Integer> docTypeIdAndVersionNr, Map<String, Object> props) {
        props.put(Props.OBJECT_TYPE_ID.toString(), docTypeIdAndVersionNr.getFirst());
        props.put(Props.OBJECT_TYPE_VERSION_NR.toString(), docTypeIdAndVersionNr.getSecond());
    }

    public static void setTypeProps(DocumentTypeVersion docTypeVersion, Map<QName, Serializable> props) {
        setTypeProps(getDocTypeIdAndVersionNr(docTypeVersion), props);

        // If possible, copy the default values from document type
        WmNode docTypeNode = docTypeVersion.getParent().getNode();
        copyProperties(toQNameProperties(docTypeNode.getProperties()), props, DocumentCommonModel.Props.FUNCTION, DocumentCommonModel.Props.SERIES,
                DocumentCommonModel.Props.VOLUME, DocumentCommonModel.Props.CASE);
    }
}