package ee.webmedia.alfresco.common.service;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean for holding application configuration data that is not going to change during uptime.
 */
public class ApplicationConstantsBean implements InitializingBean {

    public static final String BEAN_NAME = "applicationConstantsBean";

    /**
     * If document list contains more than given number of rows, then only initial sorting is performed when loading list
     * and other sorting functionalities are disabled.
     */
    public static final int SORT_ALLOWED_LIMIT = 10000;

    /**
     * Seoses asutuseülese töövoo testimisega meie testis, kus asutus peab saama saata ülesandeid ka endale:
     * dokumendi vastuvõtmisel ja olemasoleva dokumendi otsimisele kontrollitakse
     * lisaks originalDvkId-le ka seda, et dokumendil oleks olemas aspekt notEditable, property notEditable=true.
     * Kui ei ole, siis tehakse uus dok. (Max peaks saama ühes süsteemis olla kaks dokumenti
     * sama originalDvkId-ga taskiga ja üks on alati notEditable sel juhul).
     * Ülesande teostamise vastuvõtmisel eelistatakse sellise dokumendi küljes olevat ülesannet,
     * millel ei ole notEditable aspekti. (Võib olla, et dokument on korduvalt edasi saadetud,
     * sel juhul ei ole ilma notEditable aspektita dokumenti olemas).
     * Testis tekib probleem sellise edasisaatmise korral, kui saata endale ja siis
     * edasisaadetud dokument uuesti endale, seda varianti ei saa testida.
     * Et asutus saaks tööülesannet saata iseendale, tuleb INTERNAL_TESTING väärtustada true,
     * sel juhul kuvatakse tööülesande täitja otsingus kontaktide nimekirjas ka
     * asutuse enda regitrikoodiga kontakt.
     * NB! Live keskkonnas PEAB INTERNAL_TESTING väärtus olema false!!!
     */
    private boolean INTERNAL_TESTING;
    private boolean caseVolumeEnabled;
    private String defaultVolumeSortingField;
    private boolean groupsEditingAllowed;
    private boolean createOrgStructGroups;
	private boolean substitutionTaskEndDateRestricted;
    private String messageNo;
    private String messageYes;

    private boolean generateNewRegNumberInReregistration;
    private boolean finishUnregisteredDocumentEnabled;
    private boolean volumeColumnEnabled;
    private boolean myTasksAndDocumentsMenuClosed;

    public boolean isGenerateNewRegNumberInReregistration() {
        return generateNewRegNumberInReregistration;
    }

    public void setGenerateNewRegNumberInReregistration(boolean generateNewRegNumberInReregistration) {
        this.generateNewRegNumberInReregistration = generateNewRegNumberInReregistration;
    }

    public boolean isFinishUnregisteredDocumentEnabled() {
        return finishUnregisteredDocumentEnabled;
    }

    public void setFinishUnregisteredDocumentEnabled(boolean finishUnregisteredDocumentEnabled) {
        this.finishUnregisteredDocumentEnabled = finishUnregisteredDocumentEnabled;
    }

    public boolean isVolumeColumnEnabled() {
        return volumeColumnEnabled;
    }

    public void setVolumeColumnEnabled(boolean volumeColumnEnabled) {
        this.volumeColumnEnabled = volumeColumnEnabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        messageYes = MessageUtil.getMessage("yes");
        messageNo = MessageUtil.getMessage("no");
    }

    public boolean isInternalTesting() {
        return INTERNAL_TESTING;
    }

    public void setInternalTesting(boolean internalTesting) {
        INTERNAL_TESTING = internalTesting;
    }

    public boolean isCaseVolumeEnabled() {
        return caseVolumeEnabled;
    }

    public void setCaseVolumeEnabled(boolean caseVolumeEnabled) {
        this.caseVolumeEnabled = caseVolumeEnabled;
    }

    public String getDefaultVolumeSortingField() {
        return defaultVolumeSortingField;
    }

    public void setDefaultVolumeSortingField(String defaultVolumeSortingField) {
        this.defaultVolumeSortingField = defaultVolumeSortingField;
    }

    public boolean isGroupsEditingAllowed() {
        return groupsEditingAllowed;
    }

    public void setGroupsEditingAllowed(boolean groupsEditingAllowed) {
        this.groupsEditingAllowed = groupsEditingAllowed;
    }
    
    public boolean isCreateOrgStructGroups() {
		return createOrgStructGroups;
	}

	public void setCreateOrgStructGroups(boolean createOrgStructGroups) {
		this.createOrgStructGroups = createOrgStructGroups;
	}

    public boolean isSubstitutionTaskEndDateRestricted() {
        return substitutionTaskEndDateRestricted;
    }

    public void setSubstitutionTaskEndDateRestricted(boolean substitutionTaskEndDateRestricted) {
        this.substitutionTaskEndDateRestricted = substitutionTaskEndDateRestricted;
    }

    public boolean isEinvoiceEnabled() {
        return false;
    }

    public String getMessageNo() {
        return messageNo;
    }

    public String getMessageYes() {
        return messageYes;
    }

    public boolean isMyTasksAndDocumentsMenuClosed() {
        return myTasksAndDocumentsMenuClosed;
    }

    public void setMyTasksAndDocumentsMenuClosed(boolean myTasksAndDocumentsMenuClosed) {
        this.myTasksAndDocumentsMenuClosed = myTasksAndDocumentsMenuClosed;
    }

}
