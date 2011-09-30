package ee.webmedia.alfresco.addressbook.web.bean;

import static ee.webmedia.alfresco.addressbook.util.AddressbookUtil.transformAddressbookNodesToSelectItems;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.common.web.BeanHelper;

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
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchContacts(int filterIndex, String contains) {
        return transformAddressbookNodesToSelectItems(getAddressbookService().search(contains));
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchOrgContacts(int filterIndex, String contains) {
        return transformAddressbookNodesToSelectItems(getAddressbookService().searchOrgContacts(contains));
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchContactGroups(int filterIndex, String contains) {
        return transformAddressbookNodesToSelectItems(getAddressbookService().searchContactGroups(contains, false, false));
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
