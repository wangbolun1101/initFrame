
package mypackage;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element name="GetConfigurationNamesResult" type="{http://frontstep.com/IDOWebService}ArrayOfString" minOccurs="0"/>
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
    "getConfigurationNamesResult"
})
@XmlRootElement(name = "GetConfigurationNamesResponse")
public class GetConfigurationNamesResponse {

    @XmlElement(name = "GetConfigurationNamesResult")
    protected ArrayOfString getConfigurationNamesResult;

    /**
     * 获取getConfigurationNamesResult属性的值。
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfString }
     *     
     */
    public ArrayOfString getGetConfigurationNamesResult() {
        return getConfigurationNamesResult;
    }

    /**
     * 设置getConfigurationNamesResult属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfString }
     *     
     */
    public void setGetConfigurationNamesResult(ArrayOfString value) {
        this.getConfigurationNamesResult = value;
    }

}
