<<<<<<< HEAD
package ee.webmedia.alfresco.thesaurus.model;

import static ee.webmedia.alfresco.docadmin.service.MetadataItemCompareUtil.cast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("thesaurus")
public class Thesaurus implements Serializable, Comparable<Thesaurus> {
    private static final long serialVersionUID = 1L;
    private static Comparator<Thesaurus> COMPARATOR = createComparator();

    @XStreamOmitField
    private NodeRef nodeRef;
    private String name;
    private String description;

    private List<HierarchicalKeyword> keywords;
    @XStreamOmitField
    private List<HierarchicalKeyword> removedKeywords = new ArrayList<HierarchicalKeyword>();

    public Thesaurus() {
        // Default constructor
        nodeRef = RepoUtil.createNewUnsavedNodeRef();
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
        if (RepoUtil.isSaved(keyword.getNodeRef())) {
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

    @Override
    public int compareTo(Thesaurus o) {
        return COMPARATOR.compare(this, o);
    }

    private static Comparator<Thesaurus> createComparator() {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Thesaurus>() {
            @Override
            public Comparable<?> tr(Thesaurus input) {
                return input.getName();
            }
        }, new NullComparator()));
        return cast(chain, Thesaurus.class);
    }

}
=======
package ee.webmedia.alfresco.thesaurus.model;

import static ee.webmedia.alfresco.docadmin.service.MetadataItemCompareUtil.cast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.RepoUtil;

@XStreamAlias("thesaurus")
public class Thesaurus implements Serializable, Comparable<Thesaurus> {
    private static final long serialVersionUID = 1L;
    private static Comparator<Thesaurus> COMPARATOR = createComparator();

    @XStreamOmitField
    private NodeRef nodeRef;
    private String name;
    private String description;

    private List<HierarchicalKeyword> keywords;
    @XStreamOmitField
    private List<HierarchicalKeyword> removedKeywords = new ArrayList<HierarchicalKeyword>();

    public Thesaurus() {
        // Default constructor
        nodeRef = RepoUtil.createNewUnsavedNodeRef();
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
        if (RepoUtil.isSaved(keyword.getNodeRef())) {
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

    @Override
    public int compareTo(Thesaurus o) {
        return COMPARATOR.compare(this, o);
    }

    private static Comparator<Thesaurus> createComparator() {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Thesaurus>() {
            @Override
            public Comparable<?> tr(Thesaurus input) {
                return input.getName();
            }
        }, new NullComparator()));
        return cast(chain, Thesaurus.class);
    }

}
>>>>>>> develop-5.1
