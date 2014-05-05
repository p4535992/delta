package ee.webmedia.alfresco.privilege.service;

public class NotSupportedPermissionSystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotSupportedPermissionSystemException() {
        super("Alfresco permission system is not supported, use PrivilegeService for permission handling!");
    }

}
