/**
 * HXCRMServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.yunker.eai.oaPackage;

public class HXCRMServiceLocator extends org.apache.axis.client.Service implements com.yunker.eai.oaPackage.HXCRMService {

    public HXCRMServiceLocator() {
    }


    public HXCRMServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public HXCRMServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for HXCRMServiceHttpPort
//    private java.lang.String HXCRMServiceHttpPort_address = "http://10.10.0.251:8089/services/HXCRMService";
    private java.lang.String HXCRMServiceHttpPort_address = "http://bloomageoa.bloomagebiotech.com/services/HXCRMService";

    public java.lang.String getHXCRMServiceHttpPortAddress() {
        return HXCRMServiceHttpPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String HXCRMServiceHttpPortWSDDServiceName = "HXCRMServiceHttpPort";

    public java.lang.String getHXCRMServiceHttpPortWSDDServiceName() {
        return HXCRMServiceHttpPortWSDDServiceName;
    }

    public void setHXCRMServiceHttpPortWSDDServiceName(java.lang.String name) {
        HXCRMServiceHttpPortWSDDServiceName = name;
    }

    public com.yunker.eai.oaPackage.HXCRMServicePortType getHXCRMServiceHttpPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(HXCRMServiceHttpPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getHXCRMServiceHttpPort(endpoint);
    }

    public com.yunker.eai.oaPackage.HXCRMServicePortType getHXCRMServiceHttpPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.yunker.eai.oaPackage.HXCRMServiceHttpBindingStub _stub = new com.yunker.eai.oaPackage.HXCRMServiceHttpBindingStub(portAddress, this);
            _stub.setPortName(getHXCRMServiceHttpPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setHXCRMServiceHttpPortEndpointAddress(java.lang.String address) {
        HXCRMServiceHttpPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.yunker.eai.oaPackage.HXCRMServicePortType.class.isAssignableFrom(serviceEndpointInterface)) {
                com.yunker.eai.oaPackage.HXCRMServiceHttpBindingStub _stub = new com.yunker.eai.oaPackage.HXCRMServiceHttpBindingStub(new java.net.URL(HXCRMServiceHttpPort_address), this);
                _stub.setPortName(getHXCRMServiceHttpPortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("HXCRMServiceHttpPort".equals(inputPortName)) {
            return getHXCRMServiceHttpPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://10.10.0.251/services/HXCRMService", "HXCRMService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://10.10.0.251/services/HXCRMService", "HXCRMServiceHttpPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("HXCRMServiceHttpPort".equals(portName)) {
            setHXCRMServiceHttpPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
