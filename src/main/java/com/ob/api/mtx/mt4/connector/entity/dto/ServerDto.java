package com.ob.api.mtx.mt4.connector.entity.dto;

import com.ob.api.mtx.mt4.connector.util.UDT;
import com.ob.api.mtx.util.HostUtil;
import com.ob.broker.common.model.HostPort;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerDto {
    String serverPort;
    String description;

    public static ServerDto parse(byte[] buf, int of) {
        final ServerDto serverDto = new ServerDto();
        serverDto.serverPort = UDT.readStringASCII(buf, of, 64);
        serverDto.description = UDT.readStringASCII(buf, of + 68, 64);
        if (serverDto.description != null && serverDto.description.length() > 2) {
            serverDto.description = serverDto.description.substring(0, serverDto.description.length() - 2);
        }
        return serverDto;
    }

    public static void parseDetails(byte[] buf, int of, ServerDetails serverDetails) {
        serverDetails.setServerName(UDT.readStringASCII(buf, of, 64));
        serverDetails.setCompanyName(UDT.readStringASCII(buf, of + 64, 64));
    }

    public HostPort toHostPort() {
        return HostUtil.toHostPort(serverPort);
    }

}


