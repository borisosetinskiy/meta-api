package com.ob.api.mtx.mt5;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

/*[StructLayout(LayoutKind.Explicit, Pack = 1, Size = 0x98, CharSet = CharSet.Unicode)]*/
public class TransactionInfo implements FromBufReader {
    /**
     * Transaction ticket
     */
    /*[FieldOffset(0)]*/ public int UpdateId;
    /**
     * Order ticket
     */
    /*[FieldOffset(4)]*/ public int Action;
    /**
     * Deal ticket
     */
    /*[FieldOffset(8)]*/ public long TicketNumber;
    /*[FieldOffset(16)]*/ /*[MarshalAsAttribute(UnmanagedType.ByValTStr, SizeConst = 32)]*/ public String Currency;
    /**
     * Symbol currency
     */
    /*[FieldOffset(80)]*/ public int Id;
    /**
     * Significant digits
     */
    /*[FieldOffset(84)]*/ public int s54;
    /**
     * Transaction id
     */
    /*[FieldOffset(88)]*/ public OrderType s58 = OrderType.values()[0];
    /**
     * Transaction type
     */
    /*[FieldOffset(92)]*/ public int s5C;
    /**
     * Order type
     */
    /*[FieldOffset(96)]*/ public OrderState orderState;
    /**
     * Order state
     */
    /*[FieldOffset(100)]*/ public ExpirationType expirationType;
    /**
     * Order placed type
     */
    /*[FieldOffset(104)]*/ public long ExpirationTime;
    /**
     * Deal type
     */
    /*[FieldOffset(112)]*/ public double OpenPrice;
    /**
     * Deal placed type
     */
    /*[FieldOffset(120)]*/ public double OrderPrice;
    /**
     * Expiration type
     */
    /*[FieldOffset(128)]*/ public double StopLoss;
    /**
     * Expiration time
     */
    /*[FieldOffset(136)]*/ public double TakeProfit;
    /**
     * Open price
     */
    /*[FieldOffset(144)]*///C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ulong Volume;
    public long Volume;

    public TransactionInfo() {
    }

    @Override
    public Object ReadFromBuf(InBuf buf) {
        int endInd = buf.Ind + 152;
        TransactionInfo st = new TransactionInfo();
        st.UpdateId = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.Action = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.TicketNumber = BitConverter.ToInt64(buf.Bytes(8), 0);
        st.Currency = GetString(buf.Bytes(64));
        st.Id = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.s54 = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.s58 = OrderType.forValue(BitConverter.ToInt32(buf.Bytes(4), 0));
        st.s5C = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.orderState = OrderState.forValue(BitConverter.ToInt32(buf.Bytes(4), 0));
        st.expirationType = ExpirationType.forValue(BitConverter.ToInt32(buf.Bytes(4), 0));
        st.ExpirationTime = BitConverter.ToInt64(buf.Bytes(8), 0);
        st.OpenPrice = BitConverter.ToDouble(buf.Bytes(8), 0);
        st.OrderPrice = BitConverter.ToDouble(buf.Bytes(8), 0);
        st.StopLoss = BitConverter.ToDouble(buf.Bytes(8), 0);
        st.TakeProfit = BitConverter.ToDouble(buf.Bytes(8), 0);
        st.Volume = BitConverter.ToUInt64(buf.Bytes(8), 0);
        if (buf.Ind != endInd) {
            throw new CodeException("Wrong reading from buffer(buf.Ind != endInd): " + buf.Ind + " != " + endInd, Code.NETWORK_ERROR);
        }
        return st;
    }

    @Override
    public String toString() {
        return "TransactionInfo{" +
               "UpdateId=" + UpdateId +
               ", Action=" + Action +
               ", TicketNumber=" + TicketNumber +
               ", Currency='" + Currency + '\'' +
               ", Id=" + Id +
               ", s54=" + s54 +
               ", s58=" + s58 +
               ", s5C=" + s5C +
               ", OrderState=" + orderState +
               ", ExpirationType=" + expirationType +
               ", ExpirationTime=" + ExpirationTime +
               ", OpenPrice=" + OpenPrice +
               ", OrderPrice=" + OrderPrice +
               ", StopLoss=" + StopLoss +
               ", TakeProfit=" + TakeProfit +
               ", Volume=" + Volume +
               '}';
    }
}