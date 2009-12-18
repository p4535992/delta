package ee.webmedia.alfresco.template.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateServiceImpl implements DocumentTemplateService {

    private GeneralService generalService;
    private NodeService nodeService;
    private FileService fileService;

    private static BeanPropertyMapper<DocumentTemplate> templateBeanPropertyMapper;
    static {
        templateBeanPropertyMapper = BeanPropertyMapper.newInstance(DocumentTemplate.class);
    }

    @Override
    public List<DocumentTemplate> getTemplates() {
        NodeRef root = getRoot();
        List<ChildAssociationRef> templateRefs = nodeService.getChildAssocs(root);
        List<DocumentTemplate> templates = new ArrayList<DocumentTemplate>(templateRefs.size());
        for (ChildAssociationRef templateRef : templateRefs) {
            DocumentTemplate docTmpl = templateBeanPropertyMapper.toObject(nodeService.getProperties(templateRef.getChildRef()));
            docTmpl.setNodeRef(templateRef.getChildRef());
            docTmpl.setDownloadUrl(fileService.generateURL(templateRef.getChildRef(), null));
            NodeRef nodeRef = generalService.getNodeRef(DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE+ "/" + docTmpl.getDocTypeId());
            if(nodeRef != null) {
                docTmpl.setDocTypeName((new Node(nodeRef)).getProperties().get(DocumentTypeModel.Props.NAME).toString());
                templates.add(docTmpl);
            }
        }
        return templates;
    }

    @Override
    public NodeRef getRoot() {
        return generalService.getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE);
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    // END: getters / setters

}
