package com.ob.api.dx.model.data;

import lombok.Data;

@Data
public class OrderLink {
    Types.LinkType linkType;
    String linkedOrder;
    String linkedClientOrderId;
}
