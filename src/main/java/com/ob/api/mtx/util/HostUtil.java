package com.ob.api.mtx.util;

import com.ob.broker.common.model.HostPort;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class HostUtil {

    public static void parseHosts(List<String> accesses, Collection<HostPort> hostPorts) {
        for (String access : accesses) {
            try {
                HostPort hostPort = toHostPort(access);
                if (hostPort != null)
                    hostPorts.add(hostPort);
            } catch (Exception ignored) {
            }
        }
    }

    public static HostPort toHostPort(String access) {
        try {
            String[] sp = access.split(":");
            int port = 443;
            final String host = sp[0];
            try {
                if (sp.length > 1) {
                    port = Integer.parseInt(sp[1]);
                }
            } catch (Exception ignored) {
            }
            return new HostPort(host, port);
        } catch (Exception ignored) {
        }
        return null;
    }
}
