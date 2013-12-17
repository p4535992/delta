package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;

import java.util.Collections;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public abstract class BaseLimitedListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private boolean showAllUnlimited = true;
    private int limit = -1;
    private boolean limited = false;
    private boolean showShowAll = true;

    /**
     * Must call this from subclasses on every entrance to dialog
     */
    protected void resetLimit(boolean limitedShowAll) {
        showAllUnlimited = limitedShowAll;
        limit = getParametersService().getLongParameter(Parameters.MAX_SEARCH_RESULT_ROWS).intValue();
        limited = false;
        showShowAll = true;
    }

    /**
     * Must pass search service results list through this method from subclasses
     */
    protected <E> E setLimited(Pair<E, Boolean> results) {
        limited = results.getSecond();
        return results.getFirst();
    }

    protected <E> List<E> setLimitedEmpty() {
        limited = false;
        return Collections.<E> emptyList();
    }

    /**
     * Subclasses must use this
     */
    protected int getLimit() {
        return limit;
    }

    /**
     * Subclasses must override if they use limiting
     */
    protected void limitChangedEvent() {
        // Subclasses must override if they use limiting
    }

    public void getAllRows(@SuppressWarnings("unused") ActionEvent event) {
        if (showAllUnlimited) {
            limit = getParametersService().getLongParameter(Parameters.MAX_SEARCH_SHOW_ALL_RESULT_ROWS).intValue();
        } else {
            limit = -1;
        }
        limited = false;
        showShowAll = false;
        limitChangedEvent();
    }

    public boolean isLimited() {
        return limited;
    }

    public boolean isShowShowAll() {
        return showShowAll;
    }

    public String getLimitedMessage() {
        return MessageUtil.getMessage("document_list_limited", limit);
    }

}
