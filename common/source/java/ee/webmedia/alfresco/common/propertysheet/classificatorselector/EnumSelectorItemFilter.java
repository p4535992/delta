package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

public interface EnumSelectorItemFilter<E extends Enum<?>> {

    boolean showItem(E enumItem);

}
