package ee.webmedia.alfresco.thesaurus.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("thesaurus")
public class Thesaurus implements Serializable {
    private static final long serialVersionUID = 1L;

    @XStreamOmitField
    private NodeRef nodeRef;
    private String name;
    private String description;

    private List<HierarchicalKeyword> keywords;
    @XStreamOmitField
    private List<HierarchicalKeyword> removedKeywords = new ArrayList<HierarchicalKeyword>();

    public Thesaurus() {
        // Default constructor
    }

    public Thesaurus(NodeRef nodeRef, String name, String description) {
        this.nodeRef = nodeRef;
        this.name = name;
        this.description = description;
    }

    public boolean addKeyword() {
        return getKeywords().add(new HierarchicalKeyword());
    }

    public boolean removeKeyword(HierarchicalKeyword keyword) {
        NodeRef nodeRef2 = keyword.getNodeRef();
        if (nodeRef2 != null) {
            removedKeywords.add(keyword);
        }
        return keywords.remove(keyword);
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<HierarchicalKeyword> getKeywords() {
        if (keywords == null) {
            keywords = new ArrayList<HierarchicalKeyword>();
        }
        return keywords;
    }

    public void setKeywords(List<HierarchicalKeyword> keywords) {
        this.keywords = keywords;
    }

    public List<HierarchicalKeyword> getRemovedKeywords() {
        if (removedKeywords == null) {
            removedKeywords = new ArrayList<HierarchicalKeyword>();
        }
        return removedKeywords;
    }

    public void setRemovedKeywords(List<HierarchicalKeyword> removedKeywords) {
        this.removedKeywords = removedKeywords;
    }

    @Override
    public String toString() {
        return "Thesaurus [nodeRef=" + nodeRef + ", name=" + name + ", description=" + description + ", keywords=" + keywords + ", removedKeywords="
        + removedKeywords + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Thesaurus other = (Thesaurus) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
