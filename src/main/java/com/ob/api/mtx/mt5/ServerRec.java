package com.ob.api.mtx.mt5;


import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

import java.util.Objects;

/*[StructLayout(LayoutKind.Explicit, Pack = 1, Size = 0x214, CharSet = CharSet.Unicode)]*/
public class ServerRec implements FromBufReader {
    /*[FieldOffset(0)]*/ /*[MarshalAsAttribute(UnmanagedType.ByValTStr, SizeConst = 64)]*/ public String ServerName;
    /*[FieldOffset(128)]*/ /*[MarshalAsAttribute(UnmanagedType.ByValTStr, SizeConst = 128)]*/ public String CompanyName;
    /*[FieldOffset(384)]*/ public int s180;
    /*[FieldOffset(388)]*/ public int s184;
    /*[FieldOffset(392)]*/ public int DST;
    /*[FieldOffset(396)]*/ public int TimeZone;
    /*[FieldOffset(400)]*/ public int s190;
    /*[FieldOffset(404)]*/ /*[MarshalAsAttribute(UnmanagedType.ByValArray, SizeConst = 128)]*///C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] s194;
    public byte[] s194;

    @Override
    public Object ReadFromBuf(InBuf buf) {
        int endInd = buf.Ind + 532;
        ServerRec st = new ServerRec();
        st.ServerName = GetString(buf.Bytes(128));
        st.CompanyName = GetString(buf.Bytes(256));
        st.s180 = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.s184 = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.DST = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.TimeZone = BitConverter.ToInt32(buf.Bytes(4), 0);
        st.s190 = BitConverter.ToInt32(buf.Bytes(4), 0);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: st.s194 = new byte[128];
        st.s194 = new byte[128];
        for (int i = 0; i < 128; i++) {
            st.s194[i] = buf.Byte();
        }
        if (buf.Ind != endInd) {
            throw new CodeException("Wrong reading from buffer(buf.Ind != endInd): " + buf.Ind + " != " + endInd, Code.NETWORK_PROBLEM);
        }
        return st;
    }

    @Override
    public String toString() {
        return "ServerRec{" +
               "ServerName='" + ServerName + '\'' +
               ", CompanyName='" + CompanyName + '\'' +
               ", s180=" + s180 +
               ", s184=" + s184 +
               ", DST=" + DST +
               ", TimeZone=" + TimeZone +
               ", s190=" + s190 +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerRec serverRec = (ServerRec) o;
        return ServerName.equals(serverRec.ServerName) && CompanyName.equals(serverRec.CompanyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ServerName, CompanyName);
    }
}