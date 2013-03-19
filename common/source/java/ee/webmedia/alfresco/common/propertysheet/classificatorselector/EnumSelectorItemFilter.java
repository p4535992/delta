package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

/**
 * @author Riina Tens
 */
public interface EnumSelectorItemFilter<E extends Enum<?>> {

    boolean showItem(E enumItem);

}
