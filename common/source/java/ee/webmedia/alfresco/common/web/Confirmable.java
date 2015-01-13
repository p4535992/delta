package ee.webmedia.alfresco.common.web;

/**
 * Class which needs confirmation from user
 */
public interface Confirmable {

    void afterConfirmationAction(Object action);

}
