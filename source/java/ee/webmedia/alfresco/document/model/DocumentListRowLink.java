package ee.webmedia.alfresco.document.model;

import javax.faces.event.ActionEvent;

/**
 * @author Riina Tens
 */
public interface DocumentListRowLink {

    String getAction();

    void open(ActionEvent event);

}
