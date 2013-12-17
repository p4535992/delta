package ee.webmedia.alfresco.addressbook.web.bean;

import static ee.webmedia.alfresco.addressbook.util.AddressbookUtil.transformAddressbookNodesToSelectItems;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.common.component.PickerSearchParams;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;

/**
 * @author Keit Tehvan
 */
public class AddressbookSearchBean {

    public static final String BEAN_NAME = "AddressbookSearchBean";

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchContacts(PickerSearchParams params) {
        Integer filter = params.isIncludeFilterIndex() ? UserContactGroupSearchBean.CONTACTS_FILTER : null;
        return transformAddressbookNodesToSelectItems(getAddressbookService().search(params.getSearchString(), params.getLimit()), filter);
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchOrgContacts(PickerSearchParams params) {
        Integer filter = params.isIncludeFilterIndex() ? UserContactGroupSearchBean.CONTACTS_FILTER : null;
        return transformAddressbookNodesToSelectItems(getAddressbookService().searchOrgContacts(params.getSearchString(), params.getLimit()), filter);
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchContactGroups(PickerSearchParams params) {
        Integer filter = params.isIncludeFilterIndex() ? UserContactGroupSearchBean.CONTACT_GROUPS_FILTER : null;
        return transformAddressbookNodesToSelectItems(getAddressbookService().searchContactGroups(params.getSearchString(), false, false, params.getLimit()), filter);
    }

    public List<String> getContactData(String nodeRef) {
        return AddressbookUtil.getContactData(nodeRef);
    }

    public boolean isUserHasPermission() {
        return getAddressbookService().hasManagePermission();
    }

    public void setupViewEntry(String refString) {
        NodeRef contactRef = new NodeRef(refString);
        FacesContext context = FacesContext.getCurrentInstance();
        QName type = BeanHelper.getNodeService().getType(contactRef);
        String callback;
        if (AddressbookModel.Types.ORGANIZATION.equals(type)) {
            callback = "AddressbookOrgDetailsDialog.setupViewEntry";
        } else {
            callback = "AddressbookPersonDetailsDialog.setupViewEntry";
        }
        MethodBinding b = context.getApplication().createMethodBinding("#{" + callback + "}", new Class[] { String.class });
        b.invoke(context, new Object[] { refString });
    }
}
