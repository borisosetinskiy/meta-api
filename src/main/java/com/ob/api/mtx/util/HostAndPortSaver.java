package com.ob.api.mtx.util;

import lombok.extern.slf4j.Slf4j;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class HostAndPortSaver {
    private final Map<String, HostAndPortStructs> data = new ConcurrentHashMap<>();

    public HostAndPortSaver() {
        final ScheduledExecutorService scheduledWriter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SAVER");
            t.setDaemon(true);
            return t;
        });
        scheduledWriter.scheduleAtFixedRate(() -> {
            try {
                for (String key : data.keySet()) {
                    HostAndPortStructs hp = data.remove(key);
                    if (hp != null) {
                        final Path path = Path.of(key);
                        saveInFile(path, hp);
                    }
                }
            } catch (Exception e) {
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public void add(Path path, HostAndPortStructs hp) {
        data.put(path.toString(), hp);
    }

    void lockFile(Path path, Consumer<FileChannel> consumer, String mode) {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), mode);
             FileChannel channel = file.getChannel();
             FileLock lock = channel.tryLock()) {
            if (lock != null) {
                consumer.accept(channel);
            }
        } catch (Exception e) {
            log.error("Error locking file", e);
        }
    }

    void saveInFile(Path path, HostAndPortStructs structs) {
        lockFile(path, fileChannel -> {
                    try {
                        final int size = structs.size();
                        ByteBuffer content = ByteBuffer.allocate(size);
                        structs.write(content);
                        fileChannel.write(content);
                    } catch (Exception e) {
                        log.error("Error saving file", e);
                    }
                }
                , "rw");
    }
}
