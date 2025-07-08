package com.ob.api.mtx.mt5;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

/*[StructLayout(LayoutKind.Explicit, Size = 0x2C, CharSet = CharSet.Unicode)]*/
public class LoginRcv1 implements FromBufReader {
    /*[FieldOffset(0)]*/ public int s0;
    /*[FieldOffset(4)]*/ public Code StatusCode = Code.values()[0]; //4
    /*[FieldOffset(8)]*/ public int CertType; //8
    /*[FieldOffset(0x14)]*/ public int PassLength; //14
    /*[FieldOffset(0x18)]*/ public short TradeBuild; //18
    /*[FieldOffset(0x1A)]*/ public short SymBuild; //1A
    /*[FieldOffset(0x1C)]*/  /*[MarshalAsAttribute(UnmanagedType.ByValArray, SizeConst = 16)]*///C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] CryptKey;
    public byte[] CryptKey; //1C
    /*[FieldOffset(0xC)]*/  private long SerialNumber; //C

    @Override
    public Object ReadFromBuf(InBuf buf) {
        int endInd = buf.Ind + 44;
        LoginRcv1 st = new LoginRcv1();
        st.s0 = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.StatusCode = Code.getById(BitConverter.ToInt32(buf.Bytes(4), 0));
        st.CertType = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.SerialNumber = BitConverter.ToInt64(buf.Bytes(8), 0);
        st.PassLength = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.TradeBuild = BitConverter.ToInt16(buf.Bytes(2), 0);
        st.SymBuild = BitConverter.ToInt16(buf.Bytes(2), 0);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: st.CryptKey = new byte[16];
        st.CryptKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            st.CryptKey[i] = buf.Byte();
        }
        if (buf.Ind != endInd) {
            throw new CodeException("Wrong reading from buffer(buf.Ind != endInd): " + buf.Ind + " != " + endInd, Code.NETWORK_ERROR);
        }
        return st;
    }
}