
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
 *         &lt;element name="strIDOName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strPropertyList" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strFilter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strOrderBy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="strPostQueryMethod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="iRecordCap" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "strIDOName",
    "strPropertyList",
    "strFilter",
    "strOrderBy",
    "strPostQueryMethod",
    "iRecordCap"
})
@XmlRootElement(name = "LoadJson")
public class LoadJson {

    protected String strSessionToken;
    protected String strIDOName;
    protected String strPropertyList;
    protected String strFilter;
    protected String strOrderBy;
    protected String strPostQueryMethod;
    protected int iRecordCap;

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
     * 获取strIDOName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrIDOName() {
        return strIDOName;
    }

    /**
     * 设置strIDOName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrIDOName(String value) {
        this.strIDOName = value;
    }

    /**
     * 获取strPropertyList属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrPropertyList() {
        return strPropertyList;
    }

    /**
     * 设置strPropertyList属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrPropertyList(String value) {
        this.strPropertyList = value;
    }

    /**
     * 获取strFilter属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrFilter() {
        return strFilter;
    }

    /**
     * 设置strFilter属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrFilter(String value) {
        this.strFilter = value;
    }

    /**
     * 获取strOrderBy属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrOrderBy() {
        return strOrderBy;
    }

    /**
     * 设置strOrderBy属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrOrderBy(String value) {
        this.strOrderBy = value;
    }

    /**
     * 获取strPostQueryMethod属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrPostQueryMethod() {
        return strPostQueryMethod;
    }

    /**
     * 设置strPostQueryMethod属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrPostQueryMethod(String value) {
        this.strPostQueryMethod = value;
    }

    /**
     * 获取iRecordCap属性的值。
     * 
     */
    public int getIRecordCap() {
        return iRecordCap;
    }

    /**
     * 设置iRecordCap属性的值。
     * 
     */
    public void setIRecordCap(int value) {
        this.iRecordCap = value;
    }

}
