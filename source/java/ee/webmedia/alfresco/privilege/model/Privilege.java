package ee.webmedia.alfresco.privilege.model;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.getDbFieldNameFromCamelCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum Privilege {

    /** Permission used on dynamic document types. Indicates that user can create new document of specific type. */
    CREATE_DOCUMENT("createDocument"),
    VIEW_DOCUMENT_META_DATA("viewDocumentMetaData"),
    EDIT_DOCUMENT("editDocument"),
    VIEW_DOCUMENT_FILES("viewDocumentFiles"),
    CREATE_CASE_FILE("createCaseFile"),
    VIEW_CASE_FILE("viewCaseFile"),
    EDIT_CASE_FILE("editCaseFile"),
    PARTICIPATE_AT_FORUM("participateAtForum");

    /** when entry.key is added, then entry.values should be also added */
    public static final Map<Privilege, Set<Privilege>> PRIVILEGE_DEPENDENCIES;
    static {
        Map<Privilege, Set<Privilege>> m = new HashMap<Privilege, Set<Privilege>>();
        m.put(Privilege.EDIT_DOCUMENT, Collections.unmodifiableSet(new HashSet<Privilege>(Arrays.asList(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES))));
        m.put(Privilege.VIEW_DOCUMENT_FILES, Collections.singleton(Privilege.VIEW_DOCUMENT_META_DATA));
        m.put(Privilege.EDIT_CASE_FILE, Collections.singleton(Privilege.VIEW_CASE_FILE));
        PRIVILEGE_DEPENDENCIES = Collections.unmodifiableMap(m);
    }
    private final String privilegeName;
    private final String dbFieldName;

    Privilege(String privilegeName) {
        this.privilegeName = privilegeName;
        dbFieldName = getDbFieldNameFromCamelCase(privilegeName);
    }

    public String getPrivilegeName() {
        return privilegeName;
    }

    public static Privilege getPrivilegeByName(String name) {
        for (Privilege privilege : values()) {
            if (privilege.getPrivilegeName().equals(name)) {
                return privilege;
            }
        }
        throw new RuntimeException("Privilege for name " + name + " not defined!");
    }

    public String getDbFieldName() {
        return dbFieldName;
    }

}
