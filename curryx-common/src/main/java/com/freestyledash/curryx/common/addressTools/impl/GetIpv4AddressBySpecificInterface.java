package com.freestyledash.curryx.common.addressTools.impl;

import com.freestyledash.curryx.common.addressTools.GetAddressTool;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 使用指定网卡上的非0.0.0.0与127.0.0.1的ipv4地址
 *
 * @author zhangyanqi
 * @since 1.0 2018/8/24
 */
public class GetIpv4AddressBySpecificInterface implements GetAddressTool {

    private String interfaceName;

    public GetIpv4AddressBySpecificInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public String getAddress() {
        Enumeration<NetworkInterface> interfaceList;
        try {
            interfaceList = NetworkInterface.getNetworkInterfaces();
        } catch (Exception e) {
            throw new IllegalStateException("没有检测到网卡");
        }
        if (interfaceList == null) {
            throw new IllegalStateException("没有检测到网卡");
        } else {
            while (interfaceList.hasMoreElements()) {
                NetworkInterface iface = interfaceList.nextElement();
                if (!this.interfaceName.equals(iface.getName())) {
                    continue;
                }
                Enumeration<InetAddress> addrList = iface.getInetAddresses();
                while (addrList.hasMoreElements()) {
                    InetAddress address = addrList.nextElement();
                    if (address instanceof Inet4Address && !address.getHostAddress().equals("127.0.0.1") && !address.equals("0.0.0.0")) {
                        return address.getHostAddress();
                    }
                }
            }
        }
        throw new IllegalStateException("检测到合适的网卡");
    }

}
