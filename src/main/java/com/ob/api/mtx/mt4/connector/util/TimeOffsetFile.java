package com.ob.api.mtx.mt4.connector.util;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TimeOffsetFile {
    public static final TimeOffsetFile TIME_OFFSET_FILE = new TimeOffsetFile();
    private final static int VERSION = 1;
    private final static String FILE_NAME = "mt4.api.tz.offset.v." + VERSION + ".data";

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                TIME_OFFSET_FILE.write();
            } catch (Exception e) {
            }
        }));
    }

    final ScheduledExecutorService scheduledWriter = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OFFSET_WRITER");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, AtomicLong> offsetData = new ConcurrentHashMap<>();
    private final AtomicBoolean writeFlush = new AtomicBoolean();
    private Path path;

    private TimeOffsetFile() {
        path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (Exception e) {
            }
        } else {
            read();
        }

        scheduledWriter.scheduleAtFixedRate(() -> {
            try {
                if (writeFlush.getAndSet(false)) {
                    write();
                }
            } catch (Exception e) {
            }
        }, 10, 10, TimeUnit.SECONDS);
    }


    private void write() throws Exception {
        int size = 8;
        final List<TimeOffsetStruct> data = new LinkedList<>();
        for (Map.Entry<String, AtomicLong> entry : offsetData.entrySet()) {
            TimeOffsetStruct struct
                    = new TimeOffsetStruct(entry.getValue().get(), entry.getKey().getBytes(StandardCharsets.UTF_8));
            data.add(struct);
            size += struct.size();
        }
        final ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.putLong(data.size());
        for (TimeOffsetStruct struct : data) {
            struct.write(byteBuffer);
        }
        Files.write(path, byteBuffer.array());
    }

    private void read() {
        try {
            load(Files.readAllBytes(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load(byte[] bytes) {
        if (bytes.length > 0) {
            if (bytes[0] == 0 && bytes[bytes.length - 1] == 0)
                return;
        }
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long size = byteBuffer.getLong();//8
        for (int i = 0; i < size; i++) {
            TimeOffsetStruct struct = new TimeOffsetStruct();
            struct.read(byteBuffer);
            try {
                add(new String(struct.key), struct.offset);
            } catch (Exception e) {
            }
        }
    }

    public long get(String key) {
        AtomicLong value = offsetData.get(key);
        if (value == null) return 0;
        return value.get();
    }

    public void add(String key, long offset) {
        offsetData.computeIfAbsent(key, k
                        -> new AtomicLong())
                .getAndUpdate(operand -> {
                    if (offset != operand) {
                        writeFlush.compareAndSet(false, true);
                        return offset;
                    }
                    return operand;
                });
    }


}
