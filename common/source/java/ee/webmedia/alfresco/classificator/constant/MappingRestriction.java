package ee.webmedia.alfresco.classificator.constant;

/**
 * Constants for field mapping restrictions when creating associated node.
 */
public enum MappingRestriction {
    /** property of the field must not be copied to another node */
    MAPPING_FORBIDDEN,
    /** property of the field can be copied to another node when identical property exists on target node */
    IDENTICAL_FIELD_MAPPING_ONLY
}
