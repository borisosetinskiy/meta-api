package com.ob.broker.common;

import com.ob.broker.common.event.EventConsumer;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.model.ConnectionStatus;
import com.ob.broker.common.model.ContractData;

import java.util.List;


public interface IBaseApi {
    IBaseApi setApiCredentials(ApiCredentials apiCredentials);

    IBaseApi setApiSetting(ApiSetting apiSetting);

    void connect(ApiCredentials apiCredentials);

    void connect();

    void simulateConnect();

    void disconnect();

    Long getBrokerId();

    Object getAccountId();

    boolean isConnected();

    ConnectionStatus getConnectionStatus();

    void subscribe(String symbol, boolean critical);

    void subscribe(String symbol);

    void unsubscribe(String symbol);

    void addListener(EventTopic topic, EventConsumer eventConsumer);

    void removeListener(EventTopic topic, EventConsumer eventConsumer);

    List<ContractData> getAllContractData();

    ContractData getContractData(String symbol);

    void shutdown();
}
