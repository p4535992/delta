package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.docdynamic.service.DocumentDynamicTypeMenuItemProcessor.processDynamicTypeMenuItem;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicTypeMenuItemProcessor;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

public class CaseFileAndWorkflowMenuItemProcessor implements InitializingBean, MenuItemProcessor {

    private MenuService menuService;
    private WorkflowService workflowService;
    private VolumeService volumeService;
    private DocumentAdminService documentAdminService;
    private WorkflowConstantsBean workflowConstantsBean;
    private ApplicationConstantsBean applicationConstantsBean;

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        for (Iterator<MenuItem> i = menuItem.getSubItems().iterator(); i.hasNext();) {
            MenuItem item = i.next();
            if (!(item instanceof DropdownMenuItem)) {
                continue;
            }
            DropdownMenuItem dropdownItem = (DropdownMenuItem) item;
            if ("caseFile-submenu".equals(dropdownItem.getSubmenuId())) {
                if (!applicationConstantsBean.isCaseVolumeEnabled()) {
                    i.remove();
                    continue;
                }
                if (!addCaseFileSubitems(dropdownItem)) {
                    dropdownItem.setRenderingDisabled(true);
                }
            }
            if ("workflow-submenu".equals(dropdownItem.getSubmenuId())) {
                if (!workflowConstantsBean.isIndependentWorkflowEnabled()) {
                    i.remove();
                    continue;
                }
                addWorkflowSubitems(dropdownItem);
            }
        }
    }

    private boolean addCaseFileSubitems(DropdownMenuItem dropdownItem) {
        List<MenuItem> children = dropdownItem.getSubItems();
        children.clear();

        List<CaseFileType> caseFileTypes = documentAdminService.getUsedCaseFileTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
        processDynamicTypeMenuItem(BeanHelper.getPrivilegeService(), Privilege.CREATE_CASE_FILE, caseFileTypes, dropdownItem, "#{CaseFileDialog.createDraft}");

        if (children.isEmpty()) {
            return false;
        }

        if (children.size() == 1) { // If only one was added, transfer its setting to parent
            MenuItem menuItem = children.get(0);
            if (menuItem instanceof DropdownMenuItem && ((DropdownMenuItem) menuItem).getSubItems().size() == 1) { // Check if we have one submenu with one subitem
                menuItem = ((DropdownMenuItem) menuItem).getSubItems().get(0);
            }

            dropdownItem.setActionListener(menuItem.getActionListener());
            dropdownItem.setParams(menuItem.getParams());
            dropdownItem.setBrowse(true); // Disable custom JS output
        }
        return true;
    }

    private void addWorkflowSubitems(MenuItem dropdownItem) {
        List<MenuItem> children = dropdownItem.getSubItems();
        children.clear();
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = workflowService.getIndependentCompoundWorkflowDefinitions(AuthenticationUtil.getRunAsUser());
        NodeRef parentRef = BeanHelper.getConstantNodeRefsBean().getIndependentWorkflowsRoot();
        for (CompoundWorkflowDefinition compoundWorkflowDefinition : compoundWorkflowDefinitions) {
            MenuItem item = new MenuItem();
            item.setTitle(compoundWorkflowDefinition.getName());
            item.setOutcome(CompoundWorkflowDialog.DIALOG_NAME);
            item.setActionListener("#{CompoundWorkflowDialog.setupNewWorkflow}");
            Map<String, String> params = item.getParams();
            params.put(WorkflowBlockBean.PARAM_COMPOUND_WORKFLOF_DEFINITION_NODEREF, compoundWorkflowDefinition.getNodeRef().toString());
            params.put("parentNodeRef", parentRef.toString());
            children.add(item);
        }
        Collections.sort(children, DocumentDynamicTypeMenuItemProcessor.COMPARATOR);

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor("newCaseFileOrWorkflow", this, true, true);
    }

    // START: getters / setters

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setWorkflowConstantsBean(WorkflowConstantsBean workflowConstantsBean) {
        this.workflowConstantsBean = workflowConstantsBean;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    // END: getters / setters

}
