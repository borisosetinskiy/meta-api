package com.ob.api.mtx.mt5;


import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

/*[StructLayout(LayoutKind.Explicit, Pack = 1, Size = 0x40, CharSet = CharSet.Unicode)]*/
public class PumpDeals698 implements FromBufReader {
    /*[FieldOffset(0)]*/ /*[MarshalAsAttribute(UnmanagedType.ByValArray, SizeConst = 64)]*/
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] s0;
    public byte[] s0;

    @Override
    public Object ReadFromBuf(InBuf buf) {
        int endInd = buf.Ind + 64;
        PumpDeals698 st = new PumpDeals698();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: st.s0 = new byte[64];
        st.s0 = new byte[64];
        for (int i = 0; i < 64; i++) {
            st.s0[i] = buf.Byte();
        }
        if (buf.Ind != endInd) {
            throw new CodeException("Wrong reading from buffer(buf.Ind != endInd): " + buf.Ind + " != " + endInd, Code.NETWORK_ERROR);
        }
        return st;
    }
}