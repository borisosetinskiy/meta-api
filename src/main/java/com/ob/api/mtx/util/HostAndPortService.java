package com.ob.api.mtx.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ob.broker.common.model.HostPort;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostAndPortService {
    final static long TIME_OUT = TimeUnit.MINUTES.toMillis(15);
    final static String FILE_NAME = "server.host.port.";
    final static HostAndPortSaver hostAndPortSaver = new HostAndPortSaver();
    static final ExecutorService SERVER_PROCESSOR
            = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                    .setNameFormat("SERVER_FILE_%s")
                    .setDaemon(false).build());
    private final static Broker broker = new Broker();
    public final AtomicLong brokerDataTime = new AtomicLong();
    final Map<HostPort, Long> hosts = new ConcurrentHashMap<>();
    final ObjectMapper objectMapper = new ObjectMapper();
    final Lock lock = new ReentrantLock();
    private final AtomicLong index = new AtomicLong();
    final AtomicBoolean serverSearch = new AtomicBoolean(false);
    Path path;
    private long lastUpdate;
    private Long brokerId;
    private String brokerServerName;

    private static Set<HostPort> fromHostAndPortStruct(HostAndPortStructs structs) {
        Set<HostPort> out = new HashSet<>();
        for (HostAndPortStruct struct : structs.getHosts()) {
            out.add(new HostPort(new String(struct.getHost(), StandardCharsets.UTF_8)
                    , struct.getPort()));
        }
        return out;
    }

    public void init(Long id, String brokerServerName, String serverType, List<HostPort> h) {
        this.brokerId = id;
        this.brokerServerName = brokerServerName;
        path = Paths.get(FILE_NAME + id + ".data");
        if (h != null && !h.isEmpty())
            append(h);
        updateFromFile();
        if (outOfDate(lastUpdate) || hosts.isEmpty()) {
            if (brokerServerName != null && !brokerServerName.isEmpty()) {
                update(brokerServerName, brokerServerName, serverType);
            }
        }
        if (hosts.isEmpty())
            throw new RuntimeException("No data to connect");
    }

    public HostPort next() {
        if (brokerId == null)
            throw new RuntimeException("Host not initialized by broker id");
        final int r = (int) (index.getAndIncrement() % size());
        return get(r);
    }

    public HostPort current() {
        final int r = (int) (index.get() % size());
        return get(r);
    }

    public int size() {
        return hosts.size();
    }

    private HostPort get(int r) {
        int i = 0;
        HostPort result = null;
        for (HostPort hostPort : hosts.keySet()) {
            result = hostPort;
            if (i++ == r)
                break;
        }
        return result;
    }

    public boolean update(String key1, String key2, String serverType) {
        try {
            Set<HostPort> searched
                    = searchInService(key1
                    , key2, serverType);
            if (!searched.isEmpty()) {
                append(searched);
                lastUpdate = System.currentTimeMillis();
                save();
                brokerDataTime.set(System.currentTimeMillis());
                return true;
            }
        } catch (Exception e) {
            log.error("Error on search host for broker {} {}", key1, key2, e);
        }
        return false;
    }

    public void update(ServerData serverData, String serverType) {
        SERVER_PROCESSOR.execute(() -> {
            try {
                final List<HostPort> hostPorts = serverData.getServers();
                if (!hostPorts.isEmpty()) {
                    append(hostPorts);
                    save();
                }
            } catch (Exception e) {
            }
            if (outOfDate(lastUpdate)) {
                if (!update(serverData.getCompanyName(), serverData.getServerName(), serverType)) {
                    update(serverData.getServerName(), serverData.getServerName(), serverType);
                }
            }
        });
    }

    void save() {
        final HostAndPortStructs structsFromServer = toHostAndPortStruct(hosts.keySet());
        final HostAndPortStructs structsFromFile = searchInFile();
        if (!structsFromServer.equals(structsFromFile)) {
            hostAndPortSaver.add(path, structsFromServer);
        }
    }

    private void append(Collection<HostPort> h) {
        for (HostPort hostPort : h) {
            hosts.put(hostPort, lastUpdate);
        }
    }

    private void updateFromFile() {
        try {
            final HostAndPortStructs structs = searchInFile();
            if (structs.getTimestamp() > lastUpdate) {
                final Set<HostPort> hostPorts = fromHostAndPortStruct(structs);
                lastUpdate = structs.getTimestamp();
                append(hostPorts);
            }
        } catch (Exception ignored) {
        }
    }

    boolean outOfDate(long t) {
        return System.currentTimeMillis() - t > TIME_OUT;
    }


    public Set<HostPort> searchInService(String companyName, String serverName, String serverType) {
        Set<HostPort> result = new HashSet<>();
        if (lock.tryLock()) {
            try {
                final String content = Objects.requireNonNull(broker.Search(companyName, serverType));
                Companies companies = objectMapper.readValue(content, Companies.class);
                if (companies != null) {
                    for (Company company : companies.result) {
                        for (Server server : company.results) {
                            if (server.name.equals(serverName)) {
                                HostUtil.parseHosts(server.getAccess(), result);
                            }
                        }
                    }
                }
            } catch (Exception e) {

            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    private HostAndPortStructs toHostAndPortStruct(Collection<HostPort> in) {
        final HostAndPortStructs structs = new HostAndPortStructs();
        for (HostPort hp : in) {
            structs.getHosts().add(new HostAndPortStruct(hp.getPort()
                    , hp.getHost().getBytes(StandardCharsets.UTF_8)));
        }
        structs.setTimestamp(lastUpdate);
        return structs;
    }

    HostAndPortStructs searchInFile() {
        final HostAndPortStructs struct = new HostAndPortStructs();
        try {
            final ByteBuffer fileContent = readFile();
            if (fileContent != null && fileContent.capacity() > 0) {
                struct.read(fileContent);
            }
        } catch (Exception ignored) {

        }
        return struct;
    }

    ByteBuffer readFile() throws IOException {
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (Exception e) {
            }
            return null;
        }
        byte[] fileContent = Files.readAllBytes(path);
        if (fileContent.length > 0)
            return ByteBuffer.wrap(fileContent);
        return null;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Companies {
        List<Company> result;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Company {
        String company;
        List<Server> results;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Server {
        String name;
        List<String> access;
        @JsonProperty(value = "is_demo")
        Integer isDemo;
    }


}
