package ee.webmedia.alfresco.classificator.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.FileUploadBean;
import org.springframework.web.jsf.FacesContextUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorExportVO;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for importing classificators
 * 
 * @author Ats Uiboupin
 */
public class ClassificatorsImportDialog extends AbstractImportDialog {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ClassificatorsImportDialog.class);
    private static final long serialVersionUID = 1L;
    public String BEAN_NAME = "ClassificatorsImportDialog";

    private transient ClassificatorService classificatorService;
    private Collection<ClassificatorExportVO> changedClassificators;
    private Map<String /* classifName */, List<ClassificatorValue>> classificatorsToImport;
    private List<ClassificatorExportVO> classificatorsOverview;
    private boolean containsUnknownClassificators;

    protected ClassificatorsImportDialog() {
        super(".xml", "classificators_import_error_wrongExtension");
    }

    private Map<String /* classifName */, List<ClassificatorValue>> getClassificatorsToImport() {
        FileUploadBean fileBean = getFileUploadBean();
        File upFile = fileBean.getFile();

        XStream xstream = new XStream();
        xstream.processAnnotations(ClassificatorExportVO.class);
        xstream.processAnnotations(ClassificatorValue.class);
        try {
            @SuppressWarnings("unchecked")
            final List<ClassificatorExportVO> classificatorsFromXML = (List<ClassificatorExportVO>) xstream.fromXML(new FileInputStream(upFile));
            final Map<String /* classifName */, List<ClassificatorValue>> classifValues = new HashMap<String /* classifName */, List<ClassificatorValue>>(
                    classificatorsFromXML.size());
            for (ClassificatorExportVO classificatorExportVO : classificatorsFromXML) {
                classifValues.put(classificatorExportVO.getName(), classificatorExportVO.getClassificatorValues());
            }
            return classifValues;
        } catch (StreamException e1) {
            // veateate näitamine ja sulgemine millegi pärast ei toimi
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "ccccc_error_wrongFileContent", getFileName());
            context.getApplication().getNavigationHandler().handleNavigation(context, null, AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME);
            reset();
            return null;
        } catch (FileNotFoundException e1) {
            throw new RuntimeException("Failed to read classificators from file '" + upFile.getName() + "'", e1);
        }
    }

    public List<ClassificatorExportVO> getImportableClassificators() {
        if (classificatorsToImport == null) {
            classificatorsToImport = getClassificatorsToImport();
            if (classificatorsToImport == null) {
                return null;
            }
        } else {
            return classificatorsOverview;
        }
        classificatorsOverview = new ArrayList<ClassificatorExportVO>(classificatorsToImport.size());
        final List<Classificator> existingClassificators = getClassificatorService().getAllClassificators();
        final Map<String, ClassificatorExportVO> classificatorsToUpdate = new HashMap<String, ClassificatorExportVO>(existingClassificators.size());
        final Set<String> existingClassifNames = new HashSet<String>(existingClassificators.size());
        // create import VOs for classificators that already exist
        for (Classificator classificator : existingClassificators) {
            final String classifName = classificator.getName();
            final List<ClassificatorValue> importableClassificatorValues = classificatorsToImport.get(classifName);
            if (importableClassificatorValues == null) {
                continue; // this classificator was not included in import file
            }
            final ClassificatorExportVO classificatorExportVO = new ClassificatorExportVO(classificator, importableClassificatorValues);
            classificatorExportVO.setNodeRef(classificator.getNodeRef());
            final List<ClassificatorValue> existingClassifValues = getClassificatorService().getAllClassificatorValues(classificatorExportVO);
            classificatorExportVO.setPreviousClassificatorValues(existingClassifValues != null ? existingClassifValues : Collections
                    .<ClassificatorValue> emptyList());
            if (classificatorExportVO.isValuesChanged()) {
                classificatorsToUpdate.put(classifName, classificatorExportVO);
            }
            classificatorsOverview.add(classificatorExportVO);
            existingClassifNames.add(classifName);
        }
        // create import VOs for classificators that doesn't exist (only for overview - should not be saved!)
        final Set<String> newClassificatorNames = new HashSet<String>(classificatorsToImport.keySet());
        newClassificatorNames.removeAll(existingClassifNames);
        if (newClassificatorNames.size() > 0) {
            containsUnknownClassificators = true;
            final StringBuilder sb = new StringBuilder();
            int i = newClassificatorNames.size();
            for (String newClassifName : newClassificatorNames) {
                i++;
                final List<ClassificatorValue> importableClassificatorValues = classificatorsToImport.get(newClassifName);
                final ClassificatorExportVO classificatorExportVO = new ClassificatorExportVO(newClassifName, importableClassificatorValues);
                classificatorsOverview.add(classificatorExportVO);
                final boolean appendSepparator = i < newClassificatorNames.size();
                sb.append(newClassifName + (appendSepparator ? ", " : ""));
            }
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "classificators_import_error_unknownClassificatorsExist", sb.toString());
        }

        // iterate over existing classificators, remove all items, that have not changed, leaving only items that must be updated
        for (Iterator<Entry<String, ClassificatorExportVO>> iterator = classificatorsToUpdate.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, ClassificatorExportVO> entry = iterator.next();
            final ClassificatorExportVO classificator = entry.getValue();

            if (!classificator.isValuesChanged()) {
                iterator.remove();
            } else {
                log.debug("values of classificator '" + entry.getKey() + "' are different");
            }
            this.changedClassificators = new ArrayList<ClassificatorExportVO>(classificatorsToUpdate.values()); // make HashMap$Values serializable
        }
        Collections.sort(classificatorsOverview, new Comparator<ClassificatorExportVO>() {

            @Override
            public int compare(ClassificatorExportVO o1, ClassificatorExportVO o2) {
                return o1.getStatus().compareTo(o2.getStatus());
            }

        });
        return classificatorsOverview;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (containsUnknownClassificators) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "classificators_import_error_unknownClassificatorsExist");
            return null;
        }
        if (changedClassificators == null || changedClassificators.size() == 0) {
            log.info("Values in uploaded file contain no changes to existing classificators");
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "classificators_import_info_noChanges", getFileName());
        } else {
            log.info("Starting to import classificators");
            getClassificatorService().importClassificators(changedClassificators);
            log.info("Finished importing classificators");
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "classificators_import_success", changedClassificators.size(), getFileName());
        }
        return outcome;
    }

    @Override
    public String reset() {
        super.reset();
        changedClassificators = null;
        classificatorsToImport = null;
        classificatorsOverview = null;
        containsUnknownClassificators = false;
        return "dialog:close";
    }

    @Override
    public String getFileUploadSuccessMsg() {
        return MessageUtil.getMessage("classificator_import_reviewChanges");
    }

    // START: getters / setters
    public ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

    // END: getters / setters
}
