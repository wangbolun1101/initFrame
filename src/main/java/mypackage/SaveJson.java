
package mypackage;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>anonymous complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
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
     * 获取strSessionToken属性的值。
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
     * 设置strSessionToken属性的值。
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
     * 获取updateJsonObject属性的值。
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
     * 设置updateJsonObject属性的值。
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
     * 获取strCustomInsert属性的值。
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
     * 设置strCustomInsert属性的值。
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
     * 获取strCustomUpdate属性的值。
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
     * 设置strCustomUpdate属性的值。
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
     * 获取strCustomDelete属性的值。
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
     * 设置strCustomDelete属性的值。
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
