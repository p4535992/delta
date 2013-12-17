package ee.webmedia.alfresco.classificator.constant;

/**
 * Constant for different conditions when field should be changeable
 * 
 * @author Ats Uiboupin
 */
public enum FieldChangeableIf {
    /* alati muudetav */
    ALWAYS_CHANGEABLE,
    /* muudetav töös dokumendil */
    CHANGEABLE_IF_WORKING_DOC,
    /* alati mittemuudetav */
    ALWAYS_NOT_CHANGEABLE
}
