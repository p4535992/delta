<<<<<<< HEAD
package ee.webmedia.alfresco.user.model;

import static ee.webmedia.alfresco.utils.UserUtil.getUserDisplayUnit;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

/**
 * Wrapper for user node used in user list dialog
 * 
 * @author Riina Tens
 */
public class UserListRowVO implements Serializable {

    Node node;

    public UserListRowVO(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public String getFirstName() {
        return (String) node.getProperties().get(ContentModel.PROP_FIRSTNAME);
    }

    public String getLastName() {
        return (String) node.getProperties().get(ContentModel.PROP_LASTNAME);
    }

    public String getUnit() {
        return getUserDisplayUnit(node.getProperties());
    }

    public String getUserName() {
        return (String) node.getProperties().get(ContentModel.PROP_USERNAME);
    }

}
=======
package ee.webmedia.alfresco.user.model;

import static ee.webmedia.alfresco.utils.UserUtil.getUserDisplayUnit;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

/**
 * Wrapper for user node used in user list dialog
 */
public class UserListRowVO implements Serializable {

    Node node;

    public UserListRowVO(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public String getFirstName() {
        return (String) node.getProperties().get(ContentModel.PROP_FIRSTNAME);
    }

    public String getLastName() {
        return (String) node.getProperties().get(ContentModel.PROP_LASTNAME);
    }

    public String getUnit() {
        return getUserDisplayUnit(node.getProperties());
    }

    public String getUserName() {
        return (String) node.getProperties().get(ContentModel.PROP_USERNAME);
    }

}
>>>>>>> develop-5.1
