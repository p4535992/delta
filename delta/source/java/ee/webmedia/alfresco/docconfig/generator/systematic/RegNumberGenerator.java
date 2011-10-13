package ee.webmedia.alfresco.docconfig.generator.systematic;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class RegNumberGenerator extends BaseSystematicFieldGenerator {

    private VolumeService volumeService;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.SHORT_REG_NUMBER.getLocalName(), DocumentCommonModel.Props.REG_NUMBER.getLocalName());

        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] { DocumentCommonModel.Props.REG_NUMBER.getLocalName() };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (!volumeService.isCaseVolumeEnabled()) {
            item.setReadOnly(true);
        }
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

}
