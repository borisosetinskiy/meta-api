package com.ob.broker.common.model;

public record ChannelStatus(int code, String message, ConnectionStatus status) {
}
