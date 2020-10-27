/**
 * HXCRMServicePortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.yunker.yayun.oaPackage;

public interface HXCRMServicePortType extends java.rmi.Remote {
    public com.yunker.yayun.oaPackage.AnyType2AnyTypeMapEntry[][] getAllStaffInfoList() throws java.rmi.RemoteException;
    public com.yunker.yayun.oaPackage.AnyType2AnyTypeMapEntry[][] getFramworkProcessesById(java.lang.String in0) throws java.rmi.RemoteException;
    public com.yunker.yayun.oaPackage.AnyType2AnyTypeMapEntry[][] getWFStatusByIdList(java.lang.String in0) throws java.rmi.RemoteException;
}
