
package ee.webmedia.alfresco.adr;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for dokumentDetailidega complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dokumentDetailidega">
 *   &lt;complexContent>
 *     &lt;extension base="{http://alfresco/avalikdokumendiregister}dokument">
 *       &lt;sequence>
 *         &lt;element name="juurdepaasuPiirang" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="juurdepaasuPiiranguAlus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="juurdepaasuPiiranguKehtivuseAlgusKuupaev" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="juurdepaasuPiiranguKehtivuseLoppKuupaev" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="juurdepaasuPiiranguLopp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="tahtaeg" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="vastamiseKuupaev" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="koostaja" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="allkirjastaja" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fail" type="{http://alfresco/avalikdokumendiregister}fail" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dokumentDetailidegaFailita", propOrder = {
    "juurdepaasuPiirang",
    "juurdepaasuPiiranguAlus",
    "juurdepaasuPiiranguKehtivuseAlgusKuupaev",
    "juurdepaasuPiiranguKehtivuseLoppKuupaev",
    "juurdepaasuPiiranguLopp",
    "tahtaeg",
    "vastamiseKuupaev",
    "vastutaja",
    "allkirjastaja",
    "saatjaViit",
    "lepinguObjekt",
    "saatjaKuupaev",
    "seotudDokument"
})
public class DokumentDetailidegaFailita
    extends TkDokument
{

    @XmlElement(required = true)
    protected String juurdepaasuPiirang;
    protected String juurdepaasuPiiranguAlus;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar juurdepaasuPiiranguKehtivuseAlgusKuupaev;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar juurdepaasuPiiranguKehtivuseLoppKuupaev;
    protected String juurdepaasuPiiranguLopp;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar tahtaeg;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar vastamiseKuupaev;
    protected String vastutaja;
    protected String allkirjastaja;
    private String saatjaViit;
    private String lepinguObjekt;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar saatjaKuupaev;
    protected List<TkDokument> seotudDokument;

    /**
     * Gets the value of the juurdepaasuPiirang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJuurdepaasuPiirang() {
        return juurdepaasuPiirang;
    }

    /**
     * Sets the value of the juurdepaasuPiirang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJuurdepaasuPiirang(String value) {
        this.juurdepaasuPiirang = value;
    }

    /**
     * Gets the value of the juurdepaasuPiiranguAlus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJuurdepaasuPiiranguAlus() {
        return juurdepaasuPiiranguAlus;
    }

    /**
     * Sets the value of the juurdepaasuPiiranguAlus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJuurdepaasuPiiranguAlus(String value) {
        this.juurdepaasuPiiranguAlus = value;
    }

    /**
     * Gets the value of the juurdepaasuPiiranguKehtivuseAlgusKuupaev property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getJuurdepaasuPiiranguKehtivuseAlgusKuupaev() {
        return juurdepaasuPiiranguKehtivuseAlgusKuupaev;
    }

    /**
     * Sets the value of the juurdepaasuPiiranguKehtivuseAlgusKuupaev property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(XMLGregorianCalendar value) {
        this.juurdepaasuPiiranguKehtivuseAlgusKuupaev = value;
    }

    /**
     * Gets the value of the juurdepaasuPiiranguKehtivuseLoppKuupaev property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getJuurdepaasuPiiranguKehtivuseLoppKuupaev() {
        return juurdepaasuPiiranguKehtivuseLoppKuupaev;
    }

    /**
     * Sets the value of the juurdepaasuPiiranguKehtivuseLoppKuupaev property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setJuurdepaasuPiiranguKehtivuseLoppKuupaev(XMLGregorianCalendar value) {
        this.juurdepaasuPiiranguKehtivuseLoppKuupaev = value;
    }

    /**
     * Gets the value of the juurdepaasuPiiranguLopp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJuurdepaasuPiiranguLopp() {
        return juurdepaasuPiiranguLopp;
    }

    /**
     * Sets the value of the juurdepaasuPiiranguLopp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJuurdepaasuPiiranguLopp(String value) {
        this.juurdepaasuPiiranguLopp = value;
    }

    /**
     * Gets the value of the tahtaeg property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTahtaeg() {
        return tahtaeg;
    }

    /**
     * Sets the value of the tahtaeg property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTahtaeg(XMLGregorianCalendar value) {
        this.tahtaeg = value;
    }

    /**
     * Gets the value of the vastamiseKuupaev property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getVastamiseKuupaev() {
        return vastamiseKuupaev;
    }

    /**
     * Sets the value of the vastamiseKuupaev property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setVastamiseKuupaev(XMLGregorianCalendar value) {
        this.vastamiseKuupaev = value;
    }

    /**
     * Gets the value of the koostaja property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVastutaja() {
        return vastutaja;
    }

    /**
     * Sets the value of the koostaja property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVastutaja(String value) {
        this.vastutaja = value;
    }

    /**
     * Gets the value of the allkirjastaja property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllkirjastaja() {
        return allkirjastaja;
    }

    /**
     * Sets the value of the allkirjastaja property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllkirjastaja(String value) {
        this.allkirjastaja = value;
    }

    public List<TkDokument> getSeotudDokument() {
        if (seotudDokument == null) {
            seotudDokument = new ArrayList<TkDokument>();
        }
        return seotudDokument;
    }

    public void setSaatjaViit(String saatjaViit) {
        this.saatjaViit = saatjaViit;
    }

    public String getSaatjaViit() {
        return saatjaViit;
    }

    public void setSaatjaKuupaev(XMLGregorianCalendar saatjaKuupaev) {
        this.saatjaKuupaev = saatjaKuupaev;
    }

    public XMLGregorianCalendar getSaatjaKuupaev() {
        return saatjaKuupaev;
    }

    public void setLepinguObjekt(String lepinguObjekt) {
        this.lepinguObjekt = lepinguObjekt;
    }

    public String getLepinguObjekt() {
        return lepinguObjekt;
    }

}