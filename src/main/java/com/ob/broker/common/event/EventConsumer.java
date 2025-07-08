package com.ob.broker.common.event;

import com.ob.broker.common.IBaseApi;

public interface EventConsumer extends Listener {
    void onNext(EventTopic topic, IBaseApi api, Event event);
    String getId();
}
