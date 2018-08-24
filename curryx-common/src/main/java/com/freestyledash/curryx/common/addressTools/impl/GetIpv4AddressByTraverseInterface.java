package com.freestyledash.curryx.common.addressTools.impl;

import com.freestyledash.curryx.common.addressTools.GetAddressTool;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 通过遍历本地网卡获得第一个不为0.0.0.0和127.0.0.1的ip4地址,适用于内网或者当前网卡有公网ip的场景
 *
 * @author zhangyanqi
 * @since 1.0 2018/8/24
 */
public class GetIpv4AddressByTraverseInterface implements GetAddressTool {

    @Override
    public String getAddress() {
        Enumeration<NetworkInterface> interfaceList = null;
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
