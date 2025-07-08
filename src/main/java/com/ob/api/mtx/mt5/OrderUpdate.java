package com.ob.api.mtx.mt5;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class OrderUpdate {
    TransactionInfo Trans;
    OrderInternal OrderInternal;
    DealInternal Deal;
    DealInternal OppositeDeal;
    UpdateType Type;
    Order Order;
}