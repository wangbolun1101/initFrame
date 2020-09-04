
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
     * ��ȡstrIDOName���Ե�ֵ��
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
     * ����strIDOName���Ե�ֵ��
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
     * ��ȡstrPropertyList���Ե�ֵ��
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
     * ����strPropertyList���Ե�ֵ��
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
     * ��ȡstrFilter���Ե�ֵ��
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
     * ����strFilter���Ե�ֵ��
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
     * ��ȡstrOrderBy���Ե�ֵ��
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
     * ����strOrderBy���Ե�ֵ��
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
     * ��ȡstrPostQueryMethod���Ե�ֵ��
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
     * ����strPostQueryMethod���Ե�ֵ��
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
     * ��ȡiRecordCap���Ե�ֵ��
     * 
     */
    public int getIRecordCap() {
        return iRecordCap;
    }

    /**
     * ����iRecordCap���Ե�ֵ��
     * 
     */
    public void setIRecordCap(int value) {
        this.iRecordCap = value;
    }

}
