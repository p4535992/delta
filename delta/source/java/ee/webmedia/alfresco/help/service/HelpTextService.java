package ee.webmedia.alfresco.help.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.help.model.HelpText;

/**
 * Service contract for working with HelpText nodes.
 * <p>
 * Specification: <i>Kontekstitundlik abiinfo</i>.
 * 
 * @author Martti Tamm
 */
public interface HelpTextService {

    String BEAN_NAME = "HelpTextService";

    List<HelpText> getHelpTexts();

    Node addDialogHelp(String code, String content);

    Node addDocumentTypeHelp(String code, String content);

    Node addFieldHelp(String code, String content);

    String getHelpContent(String type, String code);

    void editHelp(Node helpTextNode);

    void deleteHelp(NodeRef helpTextRef);

    Map<String, Map<String, Boolean>> getHelpTextKeys();
}
