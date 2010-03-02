package ee.webmedia.alfresco.document.type.web;

import java.io.Serializable;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.document.type.model.DocumentTypeModel;


public class TypeBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Node selector;
    
    public void init() {
        // New empty selector
        selector = new TransientNode(DocumentTypeModel.Types.SELECTOR, null, null);
    }
    
    public void setSelected(String newType) {
        selector.getProperties().put(DocumentTypeModel.Props.SELECTED.toString(), newType);
    }

    // START: getters / setters
    public Node getSelector() {
        return selector;
    }
    // END: getters / setters
}
