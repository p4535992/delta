package ee.webmedia.alfresco.sharepoint.mapping;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;

/**
 * Represents a mapping group from mappings XML file. A mapping group contains property mappings and sub-mappings, which can also contain property mappings.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public class Mapping {

    private final String from;
    private final QName to;
    private final QName assoc;
    private final TypeInfo typeInfo;

    private final List<PropMapping> props;
    private final Set<Mapping> subMappings = new HashSet<Mapping>();

    /**
     * This constructor is to be used by general mapping.
     */
    public Mapping() {
        from = null;
        to = null;
        typeInfo = null;
        assoc = null;
        props = new ArrayList<PropMapping>(0);
    }

    /**
     * This constructor is to be used by document type mapping.
     */
    public Mapping(Mapping general, String from, TypeInfo typeInfo, QName assoc) {
        to = typeInfo == null ? null : typeInfo.getQName();
        this.from = from;
        this.typeInfo = typeInfo;
        this.assoc = assoc;
        if (general == null) {
            props = new ArrayList<PropMapping>(0);
        } else {
            props = new ArrayList<PropMapping>(general.props);
        }
    }

    public void add(PropMapping propertyMapping) {
        props.add(propertyMapping);
    }

    public void add(Mapping submapping) {
        subMappings.add(submapping);
    }

    public String getFrom() {
        return from;
    }

    public QName getTo() {
        return to;
    }

    public DocumentTypeVersion getDocumentVersion() {
        return typeInfo.getDocVer();
    }

    public QName[] getHierarchy() {
        return typeInfo.getHierarchy();
    }

    public Set<Mapping> getSubMappings() {
        return subMappings;
    }

    public List<PropMapping> getPropertyMapping() {
        return props;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(String.format("[%s -> %s %s :\n",
                from,
                to == null ? "(no target)" : to.toPrefixString(getNamespaceService()),
                assoc == null ? "(no assoc)" : assoc.toPrefixString(getNamespaceService())));

        for (PropMapping m : props) {
            s.append("  ").append(m).append("\n");
        }

        if (subMappings.isEmpty()) {
            s.append("  [no children]\n");
        } else {
            s.append("# Children (").append(subMappings.size()).append("):\n");
            for (Mapping m : subMappings) {
                s.append(m).append("\n");
            }
        }

        return s.append("]").toString();
    }

}