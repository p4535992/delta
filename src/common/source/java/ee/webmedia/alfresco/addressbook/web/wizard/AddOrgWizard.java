package ee.webmedia.alfresco.addressbook.web.wizard;

import java.util.Map;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;

/**
 * @author Keit Tehvan
 */
public class AddOrgWizard extends AddressbookEntryWizard {
    private static final long serialVersionUID = 1L;

    @Override
    public void init(Map<String, String> params) {
        setupEntry(Types.ORGANIZATION);
        super.init(params);
    }

}
