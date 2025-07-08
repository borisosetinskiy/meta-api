package com.ob.api.mtx.util;

import com.ob.broker.common.model.HostPort;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ServerData {
    String serverName;
    String companyName;
    List<HostPort> servers = new ArrayList<>();
}
