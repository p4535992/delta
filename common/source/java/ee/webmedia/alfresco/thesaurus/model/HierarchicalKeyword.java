<<<<<<< HEAD
package ee.webmedia.alfresco.thesaurus.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("keyword")
public class HierarchicalKeyword implements Comparable<HierarchicalKeyword>, Serializable {
    private static final long serialVersionUID = 1L;

    @XStreamOmitField
    private NodeRef nodeRef;
    private String keywordLevel1;
    private String keywordLevel2;

    public HierarchicalKeyword() {
        // Default constructor
        nodeRef = RepoUtil.createNewUnsavedNodeRef();
    }

    public HierarchicalKeyword(NodeRef nodeRef, String level1, String level2) {
        this.nodeRef = nodeRef;
        keywordLevel1 = level1;
        keywordLevel2 = level2;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getKeywordLevel1() {
        return keywordLevel1;
    }

    public void setKeywordLevel1(String keywordLevel1) {
        this.keywordLevel1 = keywordLevel1;
    }

    public String getKeywordLevel2() {
        return keywordLevel2;
    }

    public void setKeywordLevel2(String keywordLevel2) {
        this.keywordLevel2 = keywordLevel2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keywordLevel1 == null) ? 0 : keywordLevel1.hashCode());
        result = prime * result + ((keywordLevel2 == null) ? 0 : keywordLevel2.hashCode());
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
        HierarchicalKeyword other = (HierarchicalKeyword) obj;
        if (keywordLevel1 == null) {
            if (other.keywordLevel1 != null) {
                return false;
            }
        } else if (!keywordLevel1.equals(other.keywordLevel1)) {
            return false;
        }
        if (keywordLevel2 == null) {
            if (other.keywordLevel2 != null) {
                return false;
            }
        } else if (!keywordLevel2.equals(other.keywordLevel2)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(HierarchicalKeyword o) {
        if (StringUtils.isBlank(getKeywordLevel1())) {
            return -1;
        }
        if (StringUtils.isBlank(o.getKeywordLevel1())) {
            return 1;
        }
        return getKeywordLevel1().compareToIgnoreCase(o.getKeywordLevel1());
    }

    @Override
    public String toString() {
        return "HierarchicalKeyword [nodeRef=" + nodeRef + ", keywordLevel1=" + keywordLevel1 + ", keywordLevel2=" + keywordLevel2 + "]";
    }

}
=======
package ee.webmedia.alfresco.thesaurus.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.utils.RepoUtil;

@XStreamAlias("keyword")
public class HierarchicalKeyword implements Comparable<HierarchicalKeyword>, Serializable {
    private static final long serialVersionUID = 1L;

    @XStreamOmitField
    private NodeRef nodeRef;
    private String keywordLevel1;
    private String keywordLevel2;

    public HierarchicalKeyword() {
        // Default constructor
        nodeRef = RepoUtil.createNewUnsavedNodeRef();
    }

    public HierarchicalKeyword(NodeRef nodeRef, String level1, String level2) {
        this.nodeRef = nodeRef;
        keywordLevel1 = level1;
        keywordLevel2 = level2;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getKeywordLevel1() {
        return keywordLevel1;
    }

    public void setKeywordLevel1(String keywordLevel1) {
        this.keywordLevel1 = keywordLevel1;
    }

    public String getKeywordLevel2() {
        return keywordLevel2;
    }

    public void setKeywordLevel2(String keywordLevel2) {
        this.keywordLevel2 = keywordLevel2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keywordLevel1 == null) ? 0 : keywordLevel1.hashCode());
        result = prime * result + ((keywordLevel2 == null) ? 0 : keywordLevel2.hashCode());
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
        HierarchicalKeyword other = (HierarchicalKeyword) obj;
        if (keywordLevel1 == null) {
            if (other.keywordLevel1 != null) {
                return false;
            }
        } else if (!keywordLevel1.equals(other.keywordLevel1)) {
            return false;
        }
        if (keywordLevel2 == null) {
            if (other.keywordLevel2 != null) {
                return false;
            }
        } else if (!keywordLevel2.equals(other.keywordLevel2)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(HierarchicalKeyword o) {
        if (StringUtils.isBlank(getKeywordLevel1())) {
            return -1;
        }
        if (StringUtils.isBlank(o.getKeywordLevel1())) {
            return 1;
        }
        return getKeywordLevel1().compareToIgnoreCase(o.getKeywordLevel1());
    }

    @Override
    public String toString() {
        return "HierarchicalKeyword [nodeRef=" + nodeRef + ", keywordLevel1=" + keywordLevel1 + ", keywordLevel2=" + keywordLevel2 + "]";
    }

}
>>>>>>> develop-5.1
