package ee.webmedia.alfresco.common.propertysheet.component;

public enum NodeAssocBrand {
    CHILDREN;// PARENTS, SOURCES, TARGETS; TODO: When extending subpropertysheet mechanism, associations:

    static NodeAssocBrand get(String name) {
        return valueOf(name.toUpperCase());
    }
}
