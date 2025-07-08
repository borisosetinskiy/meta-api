package com.ob.api.mtx.mt4.connector.entity.dto;

import com.ob.api.mtx.util.ServerData;
import com.ob.broker.common.model.HostPort;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;

@Data
@Slf4j
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerDetails {
    String serverName;
    String companyName;
    LinkedList<ServerDto> servers = new LinkedList<>();

    public static ServerDetails parse(byte[] buf) {
        final ServerDetails mainServerDto = new ServerDetails();
        int size = 160;
        byte[] rows = new byte[buf.length - 4];

        System.arraycopy(buf, 4, rows, 0, rows.length - 4);
        for (int i = 0; ; ++i) {
            try {
                final ServerDto serverDto = ServerDto.parse(rows, i * size);
                mainServerDto.servers.add(serverDto);
            } catch (Exception e) {
                mainServerDto.servers.removeLast();
                try {
                    ServerDto.parseDetails(rows, (i - 1) * size, mainServerDto);
                } catch (Exception e2) {
                    log.error("Error parsing server details", e2);
                }
                break;
            }
        }
        return mainServerDto;
    }

    public ServerData toServerData() {
        final ServerData serverData = new ServerData();
        serverData.setServerName(serverName);
        serverData.setCompanyName(companyName);
        for (ServerDto serverDto : servers) {
            HostPort hostPort = serverDto.toHostPort();
            if (hostPort != null)
                serverData.getServers().add(hostPort);
        }
        return serverData;
    }
}