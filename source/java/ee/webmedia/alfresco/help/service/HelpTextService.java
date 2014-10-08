package ee.webmedia.alfresco.help.service;

import java.util.List;
import java.util.Map;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.help.model.HelpText;

/**
 * Service contract for working with HelpText nodes.
 * <p>
 * Specification: <i>Kontekstitundlik abiinfo</i>.
 */
public interface HelpTextService {

    String BEAN_NAME = "HelpTextService";

    List<HelpText> getHelpTexts();

    Node addDialogHelp(String code, String content);

    Node addDocumentTypeHelp(String code, String content);

    Node addFieldHelp(String code, String content);

    String getHelpContent(String type, String code);

    void editHelp(Node helpTextNode);

    Map<String, Map<String, Boolean>> getHelpTextKeys();
}
