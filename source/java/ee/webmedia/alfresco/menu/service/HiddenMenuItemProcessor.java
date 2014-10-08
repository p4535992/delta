<<<<<<< HEAD
package ee.webmedia.alfresco.menu.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Disables rendering for menu items
 * 
 * @author Kaarel Jõgeva
 */
public class HiddenMenuItemProcessor implements MenuItemProcessor, InitializingBean {

    private static final String CASE_FILE_TYPE_MANAGEMENT = "caseFileTypeManagement";
    private static final String DEPARTMENT_DOCUMENTS = "departmentDocuments";
    private MenuService menuService;
    private VolumeService volumeService;
    private ParametersService parametersService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor(DEPARTMENT_DOCUMENTS, this, false, true);
        menuService.addProcessor(CASE_FILE_TYPE_MANAGEMENT, this, true);
    }

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        boolean rendered = true;
        if (DEPARTMENT_DOCUMENTS.equals(menuItem.getId())) {
            rendered = StringUtils.isNotBlank(parametersService.getStringParameter(Parameters.WORKING_DOCUMENTS_ADDRESS));
        } else if (CASE_FILE_TYPE_MANAGEMENT.equals(menuItem.getId())) {
            rendered = volumeService.isCaseVolumeEnabled();
        }

        menuItem.setRenderingDisabled(!rendered);
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
=======
package ee.webmedia.alfresco.menu.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemProcessor;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Disables rendering for menu items
 */
public class HiddenMenuItemProcessor implements MenuItemProcessor, InitializingBean {

    private static final String CASE_FILE_TYPE_MANAGEMENT = "caseFileTypeManagement";
    private static final String DEPARTMENT_DOCUMENTS = "departmentDocuments";
    private MenuService menuService;
    private VolumeService volumeService;
    private ParametersService parametersService;

    @Override
    public void afterPropertiesSet() throws Exception {
        menuService.addProcessor(DEPARTMENT_DOCUMENTS, this, false, true);
        menuService.addProcessor(CASE_FILE_TYPE_MANAGEMENT, this, true);
    }

    @Override
    public void doWithMenuItem(MenuItem menuItem) {
        boolean rendered = true;
        if (DEPARTMENT_DOCUMENTS.equals(menuItem.getId())) {
            rendered = StringUtils.isNotBlank(parametersService.getStringParameter(Parameters.WORKING_DOCUMENTS_ADDRESS));
        } else if (CASE_FILE_TYPE_MANAGEMENT.equals(menuItem.getId())) {
            rendered = volumeService.isCaseVolumeEnabled();
        }

        menuItem.setRenderingDisabled(!rendered);
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
>>>>>>> develop-5.1
