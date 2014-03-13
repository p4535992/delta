package ee.webmedia.alfresco.document.model;

import javax.faces.event.ActionEvent;

public interface DocumentListRowLink {

    String getAction();

    void open(ActionEvent event);

}
