package ee.webmedia.alfresco.thesaurus.web;

import static ee.webmedia.alfresco.utils.ActionUtil.getParam;
import static ee.webmedia.alfresco.utils.ActionUtil.hasParam;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.thesaurus.model.HierarchicalKeyword;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;
import ee.webmedia.alfresco.thesaurus.service.ThesaurusService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.WebUtil;

public class ThesaurusDetailsDialog extends BaseDialogBean {

    public static final String BEAN_NAME = "ThesaurusDetailsDialog";

    private static final long serialVersionUID = 1L;
    private transient ThesaurusService thesaurusService;

    private Thesaurus thesaurus;
    private boolean isNew;
    private String keywordFilter;
    private int rowIndex = 0;

    @Override
    public Object getActionsContext() {
        return thesaurus;
    }

    @Override
    public void clean() {
        thesaurus = null;
        keywordFilter = null;
        rowIndex = 0;
    }

    public void setup(ActionEvent event) {
        thesaurus = getThesaurusService().getThesaurus(new NodeRef(ActionUtil.getParam(event, "nodeRef")), true);
        isNew = false;
        keywordFilter = null;
    }

    public void addNew(@SuppressWarnings("unused") ActionEvent event) {
        MessageUtil.addInfoMessage("thesaurus_name_info");
        isNew = true;
        thesaurus = new Thesaurus();
        keywordFilter = null;
    }

    public void addKeyword(@SuppressWarnings("unused") ActionEvent event) {
        thesaurus.addKeyword();
    }

    public void removeKeyword(ActionEvent event) {
        if (!hasParam(event, "nodeRef") || !hasParam(event, "keywordLevel1") || !hasParam(event, "keywordLevel2")) {
            return;
        }
        NodeRef nodeRef = getParam(event, "nodeRef", NodeRef.class);
        String l1 = defaultIfEmpty(getParam(event, "keywordLevel1"), "");
        String l2 = defaultIfEmpty(getParam(event, "keywordLevel2"), "");
        HierarchicalKeyword keyword = new HierarchicalKeyword(nodeRef, "null".equals(l1) ? "" : l1, "null".equals(l2) ? "" : l2);
        if (thesaurus.removeKeyword(keyword)) {
            MessageUtil.addInfoMessage("thesaurus_keyword_removed");
        }
    }

    public void filterKeywords() {
        if (StringUtils.isBlank(keywordFilter)) {
            MessageUtil.addInfoMessage("thesaurus_insert_filter");
            return;
        }
    }

    public void showAll() {
        keywordFilter = null;
    }

    @SuppressWarnings("unchecked")
    public Collection<HierarchicalKeyword> getKeywords() {
        Collection<HierarchicalKeyword> keywords = CollectionUtils.subtract(thesaurus.getKeywords(), thesaurus.getRemovedKeywords());

        if (StringUtils.isNotBlank(keywordFilter)) {
            CollectionUtils.filter(keywords, new Predicate() {

                @Override
                public boolean evaluate(Object object) {
                    HierarchicalKeyword keyword = (HierarchicalKeyword) object;
                    return StringUtils.containsIgnoreCase(keyword.getKeywordLevel1(), keywordFilter)
                            || StringUtils.containsIgnoreCase(keyword.getKeywordLevel2(), keywordFilter);
                }
            });
        }

        Collections.sort((List<HierarchicalKeyword>) keywords);
        return keywords;
    }

    @Override
    public String getFinishButtonLabel() {
        if (isNew) {
            return MessageUtil.getMessage("thesaurus_add");
        }
        return MessageUtil.getMessage("save");
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // Check empty values
        List<HierarchicalKeyword> keywords = thesaurus.getKeywords();
        for (HierarchicalKeyword keyword : keywords) {
            if (StringUtils.isBlank(keyword.getKeywordLevel1())) {
                MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("thesaurus_keyword_level_1"));
                isFinished = false;
                return null;
            }
        }
        @SuppressWarnings("unchecked")
        Map<HierarchicalKeyword, Integer> cardinalityMap = CollectionUtils.getCardinalityMap(keywords);
        for (Entry<HierarchicalKeyword, Integer> entry : cardinalityMap.entrySet()) {
            if (entry.getValue() > 1) {
                HierarchicalKeyword key = entry.getKey();
                MessageUtil.addErrorMessage("thesaurus_keyword_duplicate", key.getKeywordLevel1(), key.getKeywordLevel2());
                isFinished = false;
                return null;
            }
        }

        // Save
        try {
            getThesaurusService().saveThesaurus(thesaurus);
        } catch (UnableToPerformException e) {
            isFinished = false;
            MessageUtil.addStatusMessage(e);
            return null;
        }

        MessageUtil.addInfoMessage("save_success");
        if (isNew) {
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }
        isFinished = false; // This way we can save again without restarting the dialog.
        return null;
    }

    @Override
    public String getContainerTitle() {
        if (isNew) {
            return MessageUtil.getMessage("thesaurus_add");
        }
        return thesaurus.getName();
    }

    @Override
    public String getActionsConfigId() {
        if (!isNew) {
            return "details_actions_thesaurus";
        }
        return null;
    }

    public String getLevel1Keywords() {
        Collection<HierarchicalKeyword> keywords = getKeywords();
        Set<String> level1 = new HashSet<String>(keywords.size());
        for (HierarchicalKeyword keyword : keywords) {
            String keywordLevel1 = keyword.getKeywordLevel1();
            if (StringUtils.isNotBlank(keywordLevel1)) {
                level1.add(keywordLevel1);
            }
        }

        return WebUtil.getValuesAsJsArrayString(level1);
    }

    public int getAndIncreaseRowIndex() {
        int index = rowIndex;
        rowIndex++;
        return index;
    }

    // START: getters / setters

    public boolean isNew() {
        return isNew;
    }

    public Thesaurus getThesaurus() {
        return thesaurus;
    }

    public void setThesaurus(Thesaurus thesaurus) {
        this.thesaurus = thesaurus;
    }

    public void setThesaurusService(ThesaurusService thesaurusService) {
        this.thesaurusService = thesaurusService;
    }

    protected ThesaurusService getThesaurusService() {
        if (thesaurusService == null) {
            thesaurusService = BeanHelper.getThesaurusService();
        }
        return thesaurusService;
    }

    public String getKeywordFilter() {
        return keywordFilter;
    }

    public void setKeywordFilter(String keywordFilter) {
        this.keywordFilter = keywordFilter;
    }

    // END: getters / setters
}
