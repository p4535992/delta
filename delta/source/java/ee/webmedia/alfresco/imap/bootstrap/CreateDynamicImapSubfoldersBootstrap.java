package ee.webmedia.alfresco.imap.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.imap.model.ImapModel;

public class CreateDynamicImapSubfoldersBootstrap extends AbstractModuleComponent {

    private String incomingLettersSubfolderType;
    private String attachmentsSubfolderType;
    private String outgoingLettersSubfolderType;
    private static final String TYPE_PREFIX_FIXED = "fixed";

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        addSubfolders(incomingLettersSubfolderType, ImapModel.Repo.INCOMING_SPACE);
        addSubfolders(attachmentsSubfolderType, ImapModel.Repo.ATTACHMENT_SPACE);
        addSubfolders(outgoingLettersSubfolderType, ImapModel.Repo.SENT_SPACE);
    }

    private void addSubfolders(String subfolderType, String parentNodeXPath) {
        if (StringUtils.isNotBlank(subfolderType)) {
            StringTokenizer tokenizer = new StringTokenizer(subfolderType, ";");
            if (TYPE_PREFIX_FIXED.equals(tokenizer.nextToken())) {
                NodeRef parentFolderNodeRef = generalService.getNodeRef(parentNodeXPath);
                String behaviour = (String) nodeService.getProperty(parentFolderNodeRef, ImapModel.Properties.APPEND_BEHAVIOUR);
                List<String> newLocalnames = new ArrayList<String>();
                while (tokenizer.hasMoreTokens()) {
                    String folderName = tokenizer.nextToken();
                    if (StringUtils.isNotBlank(folderName)) {
                        String assocLocalName = QName.createValidLocalName(folderName);
                        NodeRef childFolder = nodeService.getChildByName(parentFolderNodeRef, ContentModel.ASSOC_CONTAINS, assocLocalName);
                        if (childFolder == null && !newLocalnames.contains(assocLocalName)) {
                            QName assocName = QName.createQName(ImapModel.URI, assocLocalName);
                            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                            props.put(ContentModel.PROP_NAME, folderName);
                            props.put(ImapModel.Properties.APPEND_BEHAVIOUR, behaviour);
                            nodeService.createNode(parentFolderNodeRef, ContentModel.ASSOC_CONTAINS, assocName, ImapModel.Types.IMAP_FOLDER, props);
                            newLocalnames.add(assocLocalName);
                        }
                    }
                }
            }
        }
    }

    public void setIncomingLettersSubfolderType(String incomingLettersSubfolderType) {
        this.incomingLettersSubfolderType = incomingLettersSubfolderType;
    }

    public void setAttachmentsSubfolderType(String attachmentsSubfolderType) {
        this.attachmentsSubfolderType = attachmentsSubfolderType;
    }

    public void setOutgoingLettersSubfolderType(String outgoingLettersSubfolderType) {
        this.outgoingLettersSubfolderType = outgoingLettersSubfolderType;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
