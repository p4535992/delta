<<<<<<< HEAD
package ee.webmedia.alfresco.versions.model;

import org.alfresco.service.namespace.QName;

public interface VersionsModel {
    String URI = "http://alfresco.webmedia.ee/model/versions/1.0";
    String NAMESPACE_PREFFIX = "vs:";

    interface Aspects {
        QName VERSION_LOCKABLE = QName.createQName(URI, "versionLockable");
        QName VERSION_MODIFIED = QName.createQName(URI, "versionModified");
    }

    public interface Props {
        public interface VersionLockable {
            QName LOCKED = QName.createQName(URI, "locked");
        }

        public interface VersionModified {
            QName MODIFIED = QName.createQName(URI, "modifiedTime");
            QName FIRSTNAME = QName.createQName(URI, "firstName");
            QName LASTNAME = QName.createQName(URI, "lastName");
            QName COMMENT = QName.createQName(URI, "comment");
        }
    }
}
=======
package ee.webmedia.alfresco.versions.model;

import org.alfresco.service.namespace.QName;

public interface VersionsModel {
    String URI = "http://alfresco.webmedia.ee/model/versions/1.0";
    String NAMESPACE_PREFFIX = "vs:";

    interface Aspects {
        QName VERSION_LOCKABLE = QName.createQName(URI, "versionLockable");
        QName VERSION_MODIFIED = QName.createQName(URI, "versionModified");
    }

    public interface Props {
        public interface VersionLockable {
            QName LOCKED = QName.createQName(URI, "locked");
        }

        public interface VersionModified {
            QName MODIFIED = QName.createQName(URI, "modifiedTime");
            QName FIRSTNAME = QName.createQName(URI, "firstName");
            QName LASTNAME = QName.createQName(URI, "lastName");
            QName COMMENT = QName.createQName(URI, "comment");
        }
    }
}
>>>>>>> develop-5.1
