
package mypackage;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>anonymous complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="strSessionToken" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="updateDataSet" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;any maxOccurs="2" minOccurs="2"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="refreshAfterSave" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="strCustomInsert" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strCustomUpdate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strCustomDelete" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "strSessionToken",
    "updateDataSet",
    "refreshAfterSave",
    "strCustomInsert",
    "strCustomUpdate",
    "strCustomDelete"
})
@XmlRootElement(name = "SaveDataSet")
public class SaveDataSet {

    protected String strSessionToken;
    protected SaveDataSet.UpdateDataSet updateDataSet;
    protected boolean refreshAfterSave;
    protected String strCustomInsert;
    protected String strCustomUpdate;
    protected String strCustomDelete;

    /**
     * ��ȡstrSessionToken���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrSessionToken() {
        return strSessionToken;
    }

    /**
     * ����strSessionToken���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrSessionToken(String value) {
        this.strSessionToken = value;
    }

    /**
     * ��ȡupdateDataSet���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link SaveDataSet.UpdateDataSet }
     *     
     */
    public SaveDataSet.UpdateDataSet getUpdateDataSet() {
        return updateDataSet;
    }

    /**
     * ����updateDataSet���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link SaveDataSet.UpdateDataSet }
     *     
     */
    public void setUpdateDataSet(SaveDataSet.UpdateDataSet value) {
        this.updateDataSet = value;
    }

    /**
     * ��ȡrefreshAfterSave���Ե�ֵ��
     * 
     */
    public boolean isRefreshAfterSave() {
        return refreshAfterSave;
    }

    /**
     * ����refreshAfterSave���Ե�ֵ��
     * 
     */
    public void setRefreshAfterSave(boolean value) {
        this.refreshAfterSave = value;
    }

    /**
     * ��ȡstrCustomInsert���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrCustomInsert() {
        return strCustomInsert;
    }

    /**
     * ����strCustomInsert���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrCustomInsert(String value) {
        this.strCustomInsert = value;
    }

    /**
     * ��ȡstrCustomUpdate���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrCustomUpdate() {
        return strCustomUpdate;
    }

    /**
     * ����strCustomUpdate���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrCustomUpdate(String value) {
        this.strCustomUpdate = value;
    }

    /**
     * ��ȡstrCustomDelete���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrCustomDelete() {
        return strCustomDelete;
    }

    /**
     * ����strCustomDelete���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrCustomDelete(String value) {
        this.strCustomDelete = value;
    }


    /**
     * <p>anonymous complex type�� Java �ࡣ
     * 
     * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;any maxOccurs="2" minOccurs="2"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "any"
    })
    public static class UpdateDataSet {

        @XmlAnyElement(lax = true)
        protected List<Object> any;

        /**
         * Gets the value of the any property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the any property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getAny().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Object }
         * 
         * 
         */
        public List<Object> getAny() {
            if (any == null) {
                any = new ArrayList<Object>();
            }
            return this.any;
        }

    }

}
