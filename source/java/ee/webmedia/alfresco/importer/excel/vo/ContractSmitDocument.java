<<<<<<< HEAD
package ee.webmedia.alfresco.importer.excel.vo;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DocumentSpecificModel.URI)
public class ContractSmitDocument extends ImportDocument {
    private String firstPartyName;
    private String secondPartyName;
    private String thirdPartyName;
    //
    private Date contractSmitEndDate;
    private String contractEndAnnotation;
    //
    private String warranty;
    private String contractChange;
    //
    private String volumeTitle;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String getFirstPartyName() {
        return firstPartyName;
    }

    public void setFirstPartyName(String firstPartyName) {
        this.firstPartyName = firstPartyName;
    }

    public String getSecondPartyName() {
        return secondPartyName;
    }

    public void setSecondPartyName(String secondPartyName) {
        this.secondPartyName = secondPartyName;
    }

    public String getThirdPartyName() {
        return thirdPartyName;
    }

    public void setThirdPartyName(String thirdPartyName) {
        this.thirdPartyName = thirdPartyName;
    }

    public Date getContractSmitEndDate() {
        return contractSmitEndDate;
    }

    public void setContractSmitEndDate(Date contractSmitEndDate) {
        this.contractSmitEndDate = contractSmitEndDate;
    }

    public String getContractEndAnnotation() {
        return contractEndAnnotation;
    }

    public void setContractEndAnnotation(String contractEndAnnotation) {
        this.contractEndAnnotation = contractEndAnnotation;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setContractChange(String contractChange) {
        this.contractChange = contractChange;
    }

    public String getContractChange() {
        return contractChange;
    }

    public void setVolumeTitle(String volumeTitle) {
        this.volumeTitle = volumeTitle;
    }

    public String getVolumeTitle() {
        return volumeTitle;
    }
=======
package ee.webmedia.alfresco.importer.excel.vo;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DocumentSpecificModel.URI)
public class ContractSmitDocument extends ImportDocument {
    private String firstPartyName;
    private String secondPartyName;
    private String thirdPartyName;
    //
    private Date contractSmitEndDate;
    private String contractEndAnnotation;
    //
    private String warranty;
    private String contractChange;
    //
    private String volumeTitle;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String getFirstPartyName() {
        return firstPartyName;
    }

    public void setFirstPartyName(String firstPartyName) {
        this.firstPartyName = firstPartyName;
    }

    public String getSecondPartyName() {
        return secondPartyName;
    }

    public void setSecondPartyName(String secondPartyName) {
        this.secondPartyName = secondPartyName;
    }

    public String getThirdPartyName() {
        return thirdPartyName;
    }

    public void setThirdPartyName(String thirdPartyName) {
        this.thirdPartyName = thirdPartyName;
    }

    public Date getContractSmitEndDate() {
        return contractSmitEndDate;
    }

    public void setContractSmitEndDate(Date contractSmitEndDate) {
        this.contractSmitEndDate = contractSmitEndDate;
    }

    public String getContractEndAnnotation() {
        return contractEndAnnotation;
    }

    public void setContractEndAnnotation(String contractEndAnnotation) {
        this.contractEndAnnotation = contractEndAnnotation;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setContractChange(String contractChange) {
        this.contractChange = contractChange;
    }

    public String getContractChange() {
        return contractChange;
    }

    public void setVolumeTitle(String volumeTitle) {
        this.volumeTitle = volumeTitle;
    }

    public String getVolumeTitle() {
        return volumeTitle;
    }
>>>>>>> develop-5.1
}