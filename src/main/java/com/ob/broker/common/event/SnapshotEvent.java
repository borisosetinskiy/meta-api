package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotEvent<T> extends LoadEvent {
    final long timestamp = System.currentTimeMillis();
    List<T> data;

    public SnapshotEvent(EventType eventType, Long brokerId, Object accountId, List<T> data) {
        super(eventType, brokerId, accountId);
        this.data = data;
    }
}
