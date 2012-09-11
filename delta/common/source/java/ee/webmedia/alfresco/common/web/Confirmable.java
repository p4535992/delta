package ee.webmedia.alfresco.common.web;

/**
 * Class which needs confirmation from user
 * 
 * @author Vladimir Drozdik
 */
public interface Confirmable {

    void afterConfirmationAction(Object action);

}
