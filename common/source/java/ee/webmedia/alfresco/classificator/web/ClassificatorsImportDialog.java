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
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.FileUploadBean;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorExportVO;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Dialog for importing classificators
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class ClassificatorsImportDialog extends AbstractImportDialog {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ClassificatorsImportDialog.class);
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ClassificatorsImportDialog";

    private transient ClassificatorService classificatorService;
    private Collection<ClassificatorExportVO> changedClassificators;
    private Map<String /* classifName */, List<ClassificatorValue>> classificatorsToImport;
    private List<ClassificatorExportVO> classificatorsOverview;
    private List<ClassificatorExportVO> classificatorsToAdd;
    private Map<String /* classifName */, ClassificatorExportVO> classifObjects;

    protected ClassificatorsImportDialog() {
        super("xml", "classificators_import_error_wrongExtension");
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
            final Map<String /* classifName */, List<ClassificatorValue>> classifValues = new HashMap<String, List<ClassificatorValue>>(classificatorsFromXML.size());
            classifObjects = new HashMap<String, ClassificatorExportVO>(classificatorsFromXML.size());
            for (ClassificatorExportVO classificatorExportVO : classificatorsFromXML) {
                classifValues.put(classificatorExportVO.getName(), classificatorExportVO.getClassificatorValues());
                classifObjects.put(classificatorExportVO.getName(), classificatorExportVO);
            }
            return classifValues;
        } catch (StreamException e1) {
            // veateate näitamine ja sulgemine millegi pärast ei toimi
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "ccccc_error_wrongFileContent", getFileName());
            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME, context);
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
            if (!Boolean.TRUE.equals(classificator.isAddRemoveValues())) {
                existingClassifNames.add(classifName);
                continue;
            }
            final List<ClassificatorValue> importableClassificatorValues = classificatorsToImport.get(classifName);
            if (importableClassificatorValues == null) {
                continue; // this classificator was not included in import file
            }
            final String newClassifDescription = classifObjects.get(classifName).getDescription();
            final boolean newClassifDeleteEnabled = classifObjects.get(classifName).isDeleteEnabled();
            final ClassificatorExportVO classificatorExportVO = new ClassificatorExportVO(classificator, importableClassificatorValues);
            classificatorExportVO.setNodeRef(classificator.getNodeRef());
            final List<ClassificatorValue> existingClassifValues = getClassificatorService().getAllClassificatorValues(classificatorExportVO);
            classificatorExportVO.setPreviousDeleteEnabled(classificator.isDeleteEnabled());
            classificatorExportVO.setDeleteEnabled(newClassifDeleteEnabled);
            classificatorExportVO.setPreviousDescription(classificator.getDescription());
            classificatorExportVO.setDescription(newClassifDescription);
            classificatorExportVO.setPreviousClassificatorValues(existingClassifValues != null ? existingClassifValues : Collections
                    .<ClassificatorValue> emptyList());
            if (classificatorExportVO.isValuesChanged() || (classificatorExportVO.getChangedProperties() != null)) {
                classificatorsToUpdate.put(classifName, classificatorExportVO);
            }
            classificatorsOverview.add(classificatorExportVO);
            existingClassifNames.add(classifName);
        }
        final Set<String> newClassificatorNames = new HashSet<String>(classificatorsToImport.keySet());
        newClassificatorNames.removeAll(existingClassifNames);
        if (newClassificatorNames.size() > 0) {
            classificatorsToAdd = new ArrayList<ClassificatorExportVO>(newClassificatorNames.size());
            int i = newClassificatorNames.size();
            for (String newClassifName : newClassificatorNames) {
                i++;
                final List<ClassificatorValue> importableClassificatorValues = classificatorsToImport.get(newClassifName);
                ClassificatorExportVO newClassif = classifObjects.get(newClassifName);
                final ClassificatorExportVO classificatorExportVO = new ClassificatorExportVO(newClassifName, importableClassificatorValues);
                classificatorExportVO.setDescription(newClassif.getDescription());
                classificatorExportVO.setDeleteEnabled(newClassif.isDeleteEnabled());
                classificatorsToAdd.add(classificatorExportVO);
                classificatorsOverview.add(classificatorExportVO);
            }
        }

        // iterate over existing classificators, remove all items, that have not changed, leaving only items that must be updated
        for (Iterator<Entry<String, ClassificatorExportVO>> iterator = classificatorsToUpdate.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, ClassificatorExportVO> entry = iterator.next();
            final ClassificatorExportVO classificator = entry.getValue();

            if (!classificator.isValuesChanged() && classificator.getChangedProperties() == null) {
                iterator.remove();
            } else {
                log.debug("values of classificator '" + entry.getKey() + "' are different");
            }
            changedClassificators = new ArrayList<ClassificatorExportVO>(classificatorsToUpdate.values()); // make HashMap$Values serializable
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
        if (StringUtils.isBlank(getFileName())) {
            return null;
        }
        if ((changedClassificators == null || changedClassificators.isEmpty()) && (classificatorsToAdd == null || classificatorsToAdd.isEmpty())) {
            log.info("Values in uploaded file contain no changes to existing classificators");
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "classificators_import_info_noChanges", getFileName());
        } else {
            if (classificatorsToAdd != null && !classificatorsToAdd.isEmpty()) {
                log.info("Classificators in uploaded file contain new classificators");
                getClassificatorService().addNewClassificators(classificatorsToAdd);
            }
            if (changedClassificators != null && !changedClassificators.isEmpty()) {
                log.info("Starting to import classificators");
                getClassificatorService().importClassificators(changedClassificators);
                log.info("Finished importing classificators");
            }
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "classificators_import_success", getFileName());
        }
        return outcome;
    }

    @Override
    public String reset() {
        super.reset();
        changedClassificators = null;
        classificatorsToImport = null;
        classificatorsOverview = null;
        classificatorsToAdd = null;
        return getDefaultFinishOutcome();
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
