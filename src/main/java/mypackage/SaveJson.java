
package mypackage;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="updateJsonObject" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "updateJsonObject",
    "strCustomInsert",
    "strCustomUpdate",
    "strCustomDelete"
})
@XmlRootElement(name = "SaveJson")
public class SaveJson {

    protected String strSessionToken;
    protected String updateJsonObject;
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
     * ��ȡupdateJsonObject���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdateJsonObject() {
        return updateJsonObject;
    }

    /**
     * ����updateJsonObject���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdateJsonObject(String value) {
        this.updateJsonObject = value;
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

}
