package ee.webmedia.alfresco.privilege.service;

import java.util.List;

import org.alfresco.repo.domain.AccessControlListDAO;
import org.alfresco.repo.domain.hibernate.DirtySessionAnnotation;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Riina Tens
 */
public interface AccessControlListExtDAO extends AccessControlListDAO {

    @DirtySessionAnnotation(markDirty = true)
    void setFixedAcls(NodeRef nodeRef, Long inheritFrom, Long mergeFrom, List<AclChange> changes, boolean set);

    @DirtySessionAnnotation(markDirty = true)
    void fixAclInheritFromNull(Long childAclId, Long primaryParentAclId, NodeRef parentNodeRef);

    @DirtySessionAnnotation(markDirty = true)
    Long getInheritedAccessControlList(Long aclId);

}
