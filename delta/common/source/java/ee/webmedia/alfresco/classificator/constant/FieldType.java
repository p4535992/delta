package ee.webmedia.alfresco.classificator.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;

/**
 * Constant for different type of fields that can be added under document type
 * 
 * @author Ats Uiboupin
 */
public enum FieldType {
    /** Tekstiväli */
    TEXT_FIELD(Props.DEFAULT_VALUE),
    /** Valikuväli */
    COMBOBOX(Props.CLASSIFICATOR, Props.CLASSIFICATOR_DEFAULT_VALUE),
    /** Täisarv */
    INT(Props.DEFAULT_VALUE),
    /** Komakohaga arv */
    DOUBLE(Props.DEFAULT_VALUE),
    /** Kuupäev */
    DATE(Props.DEFAULT_DATE_SYSDATE),
    /** Redigeeritav valikuväli */
    COMBOBOX_EDITABLE(Props.CLASSIFICATOR, Props.CLASSIFICATOR_DEFAULT_VALUE),
    /** Kasutaja */
    USER(Props.DEFAULT_VALUE, Props.DEFAULT_USER_LOGGED_IN),
    /** Kontakt */
    CONTACT(Props.DEFAULT_VALUE, Props.DEFAULT_USER_LOGGED_IN),
    /** Kasutaja/kontakt */
    USER_CONTACT(Props.DEFAULT_VALUE, Props.DEFAULT_USER_LOGGED_IN),
    /** Kasutajad */
    USERS(Props.DEFAULT_USER_LOGGED_IN),
    /** Kontaktid */
    CONTACTS(Props.DEFAULT_USER_LOGGED_IN),
    /** Kasutajad/kontaktid */
    USERS_CONTACTS(Props.DEFAULT_USER_LOGGED_IN),
    /** Valikuväli koos tekstiväljaga */
    COMBOBOX_AND_TEXT(Props.CLASSIFICATOR, Props.CLASSIFICATOR_DEFAULT_VALUE),
    /** Valikuväli koos mittemuudetava tekstiväljaga */
    COMBOBOX_AND_TEXT_NOT_EDITABLE(Props.CLASSIFICATOR, Props.CLASSIFICATOR_DEFAULT_VALUE),
    /** Mitmevalikuväli */
    LISTBOX(Props.CLASSIFICATOR, Props.CLASSIFICATOR_DEFAULT_VALUE),
    /** Märkeruut */
    CHECKBOX(Props.DEFAULT_SELECTED),
    /** Infotekst */
    INFORMATION_TEXT(Props.DEFAULT_VALUE),
    /** Hierarhilise märksõnastiku 1. tase */
    HIERARCHICAL_KEYWORD_LEVEL1(),
    /** Hierarhilise märksõnastiku 2. tase */
    HIERARCHICAL_KEYWORD_LEVEL2();

    private List<QName> fieldsUsed;

    FieldType(QName... fieldsUsed) {
        if (fieldsUsed == null) {
            this.fieldsUsed = Collections.<QName> emptyList();
        } else {
            this.fieldsUsed = Arrays.asList(fieldsUsed);
        }
    }

    public List<QName> getFieldsUsed() {
        return fieldsUsed;
    }
}
