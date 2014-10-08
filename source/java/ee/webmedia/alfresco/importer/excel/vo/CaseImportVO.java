package ee.webmedia.alfresco.importer.excel.vo;

import java.util.Date;

import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.common.service.IClonable;
import ee.webmedia.alfresco.importer.excel.service.DocumentImportServiceImpl;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class CaseImportVO extends Case implements IClonable<CaseImportVO> {
    private static final BeanPropertyMapper<CaseImportVO> caseBeanPropertyMapper = BeanPropertyMapper.newInstance(CaseImportVO.class);

    private static final long serialVersionUID = 1L;
    @AlfrescoModelProperty(isMappable = false)
    private String baseRegNumber;
    @AlfrescoModelProperty(isMappable = false)
    private Date dateOfEarliestDocRegistration;
    @AlfrescoModelProperty(isMappable = false)
    private Node initialDocNode;

    @Override
    public CaseImportVO clone() {
        Assert.notNull(getNode(), "Node shouldn't be null");
        final CaseImportVO clone = caseBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(getNode().getProperties()));
        clone.setVolumeNodeRef(getVolumeNodeRef());
        clone.setNode(getNode());
        clone.baseRegNumber = baseRegNumber;
        clone.dateOfEarliestDocRegistration = dateOfEarliestDocRegistration;
        clone.initialDocNode = initialDocNode;
        return clone;
    }

    public boolean couldContain(ImportDocument doc) {
        if (StringUtils.equals(baseRegNumber, doc.getRegNumber())) {
            return true;
        }
        return false;
    }

    public void setRegNumber(String regNr) {
        baseRegNumber = DocumentImportServiceImpl.getRegNrWoIndividualizingNr(regNr);
    }

    // START: getters / setters
    public Date getDateOfEarliestDocRegistration() {
        return dateOfEarliestDocRegistration;
    }

    public void setDateOfEarliestDocRegistration(Date docRegDateTime) {
        dateOfEarliestDocRegistration = docRegDateTime;
    }

    public void setInitialDocNode(Node initialDocNode) {
        this.initialDocNode = initialDocNode;
    }

    public Node getInitialDocNode() {
        return initialDocNode;
    }

    // END: getters / setters

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}