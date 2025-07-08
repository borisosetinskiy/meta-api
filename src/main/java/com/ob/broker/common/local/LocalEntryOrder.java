package com.ob.broker.common.local;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.OrderRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.ob.broker.common.local.LocalEntryStatus.CREATED;
import static com.ob.broker.util.Util.toLong;

@Data
public final class LocalEntryOrder {
    private final long id;
    private final OrderOperationType type;
    private final String symbol;
    private final AtomicReference<LocalEntryStatus> state = new AtomicReference<>();
    private Object accountId;
    private Long brokerId;
    private Integer version;
    private String comment;
    private BigDecimal price;
    private BigDecimal sl;
    private BigDecimal tp;
    private BigDecimal lots;
    private long time = System.currentTimeMillis();

    public LocalEntryOrder(long id
            , int version
            , Object accountId
            , Long brokerId
            , String comment
            , BigDecimal price
            , BigDecimal stopLossPrice
            , BigDecimal takeProfitPrice
            , OrderOperationType type
            , BigDecimal lots
            , String symbol) {
        this.id = id;
        this.version = version;
        this.brokerId = brokerId;
        this.accountId = accountId;
        this.comment = comment;
        this.price = price;
        this.sl = stopLossPrice;
        this.tp = takeProfitPrice;
        this.type = type;
        this.lots = lots;
        this.symbol = symbol;
    }

    public static LocalEntryOrder create(OrderRequest command) {
        if (command.getVersion() == 0) {
            final LocalEntryOrder localEntryOrder
                    = new LocalEntryOrder(toLong(command.getTicket())
                    , command.getVersion()
                    , command.getAccountId()
                    , command.getBrokerId()
                    , command.getComment()
                    , command.getPrice()
                    , command.getSl()
                    , command.getTp()
                    , OrderOperationType.toOrderOperationType(command.getType())
                    , command.getLot()
                    , command.getSymbol());
            localEntryOrder.state.getAndSet(CREATED);
            return localEntryOrder;
        }
        throw new CodeException(Code.ORDER_ALREADY_EXIST);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalEntryOrder that = (LocalEntryOrder) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void update(OrderRequest command) {
        if (version < command.getVersion() && time <= command.getCreated()) {
            version = command.getVersion();
            time = command.getCreated();
            if (command.getComment() != null) {
                comment = command.getComment();
            }
            if (command.getLot() != null) {
                lots = command.getLot();
            }
            if (command.getPrice() != null) {
                price = command.getPrice();
            }

            sl = command.getSl();
            tp = command.getTp();

        }
    }

    public OrderData toOrder() {
        return OrderData.builder()
                .time(time)
                .lot(lots)
                .ticket(id)
                .accountId(accountId)
                .brokerId(brokerId)
                .TID(id)
                .orderType(type.toOrderTypeData())
                .comment(comment)
                .price(price)
                .sl(sl)
                .tp(tp)
                .symbol(symbol)
                .build();
    }

    @Override
    public String toString() {
        return "{" +
               "\"id\":" + id +
               ", \"version\":" + version +
               ", \"comment\":\"" + comment + "\"" +
               ", \"price\":" + price +
               ", \"sl\":" + sl +
               ", \"tp\":" + tp +
               ", \"type\":" + type +
               ", \"lots\":" + lots +
               ", \"symbol\":\"" + symbol + "\"" +
               ", \"time\":" + time +
               ", \"state\":" + state +
               '}';
    }

}
