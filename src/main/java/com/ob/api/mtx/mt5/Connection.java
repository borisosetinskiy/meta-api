package com.ob.api.mtx.mt5;


import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.ob.broker.util.Util.toLong;

class Connection extends SecureSocket {


    public static final long CLIENT_BUILD = 4353;
    private static final Object IdLock = new Object();
    private static int Id = 0;
    private final Object SendLock = new Object();
    public String Host;
    public int Port;
    public String ProxyHost;
    public int ProxyPort;
    public String ProxyUser;
    public String ProxyPass;
    public MT5API api;
    public String ServerName;
    public byte[] SrvCert;
    public byte[] AesKey;
    public byte[] ProtectKey;
    //public byte[] CryptKey;
    public PackDecrypt Decryptor;
    public PackEncrypt Encryptor;
    public short TradeBuild;
    public short SymBuild;
    public long LoginId;
    public long LoginIdEx;
    private int User;
    private String Pass;
    private boolean IsProxy;
    private byte[] SignData;

    private byte[] RandData;

    public Connection(int user, String pass, String host, int port, MT5API api) {
        User = user;
        Pass = pass;
        if (Pass.length() > 15) {
            Pass = DotNetToJavaStringHelper.remove(Pass, 15);
        }
        Host = host;
        Port = port;
        IsProxy = false;
        this.api = api;

    }

    public Connection(int user, String pass, String host, int port, String proxyHost, int proxyPort, String proxyUser, String proxyPass, MT5API qc) {
        User = user;
        Pass = pass;
        if (Pass.length() > 15) {
            Pass = DotNetToJavaStringHelper.remove(Pass, 15);
        }
        Host = host;
        Port = port;
        ProxyHost = proxyHost;
        ProxyPort = proxyPort;
        ProxyUser = proxyUser;
        ProxyPass = proxyPass;
        IsProxy = true;
        api = qc;
    }

    static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    public final Connection logon() throws IOException {

        try {
            Connect(Host, Port);
            SendLogin();
        } catch (Exception ex) {

            Close();
            throw ex;
        }
        return this;
    }

    public final void SendPacket(byte type, OutBuf buf) throws IOException {
        SendPacket(type, buf, false);
    }

    public final void SendPacket(byte type, OutBuf buf, boolean compressed) throws IOException {
        synchronized (SendLock) {
            buf.CreateHeader(type, GetSendId(), compressed);
            Encryptor.EncryptPacket(buf);
            Send(buf.ToArray());
        }
    }

    public final void SendCompress(byte type, OutBuf buf) throws IOException {
        //for (int i = 303; i < 353; i++) //TODO
        //    buf.List[i] = 254;
        //var compr = new Compr().Deflate(buf.ToArray());
        byte[] compr = Compressor.compress(buf.ToArray());
        OutBuf res = new OutBuf();
        res.Add(buf.List.size());
        res.Add(compr.length);
        res.Add(compr);
        SendPacket(type, res, true);
    }

    public final InBuf ReceivePacket() throws IOException {
        byte[] bytes = Receive(9);
        PacketHdr hdr = UDT.ReadStruct(bytes, 0, 9, new PacketHdr());
        byte cmd = hdr.Type;
        bytes = Receive(hdr.PacketSize);
        return new InBuf(Decryptor.Decrypt(bytes), hdr);
    }

    private int GetSendId() {
        synchronized (IdLock) {
            return Id++;
        }
    }

    private void SendLogin() throws IOException {
        byte[] buf = new byte[34];
        buf[0] = (byte) LocalDateTime.now().getNano(); //(byte)DateTime.Now.Ticks; TODO
        buf[1] = 0; //  m_ConnectInfo.m_Server.m_nSrvType
        System.arraycopy(BitConverter.GetBytes((short) CLIENT_BUILD), 0, buf, 2, BitConverter.GetBytes((short) CLIENT_BUILD).length); //MT5 Terminal build
        System.arraycopy(BitConverter.GetBytes((short) 20813), 0, buf, 4, BitConverter.GetBytes((short) 20813).length); //MQ
        System.arraycopy(BitConverter.getBytesLong((Long) api.apiCredentials.get().getAccountId()), 0, buf, 6, BitConverter.getBytesLong((Long) api.apiCredentials.get().getAccountId()).length);
        //byte[] key = new byte[] {0x3a, 0x00, (byte) 0xfa,  (byte)0x9d, (byte) 0xd4,  (byte)0xed, 0x15, 0x5f, 0x71, 0x68,  (byte)0xe5,  (byte)0xf2, 0x66,  (byte)0x9f, (byte) 0xd1,  (byte)0xe8}; //TODO GetCommonKey
        byte[] key = new byte[16];
        Random rand = new Random();
        for (int i = 1; i < key.length; i++) {
            byte v = (byte) rand.nextInt();
            key[i] = v;
            key[0] += v;
        }
        System.arraycopy(key, 0, buf, 14, key.length);
        System.arraycopy(BitConverter.GetBytes(0x11528a15), 0, buf, 30, BitConverter.GetBytes(0x11528a15).length); //TODO random
        byte[] pack = new byte[9 + buf.length];
        pack[0] = 0;
        System.arraycopy(BitConverter.GetBytes(buf.length), 0, pack, 1, BitConverter.GetBytes(buf.length).length);
        byte[] id = BitConverter.GetBytes((short) GetSendId());
        System.arraycopy(id, 0, pack, 5, id.length);
        System.arraycopy(BitConverter.GetBytes(((short) (2 & 0xFFFF)) & 0xFFFF), 0, pack, 7, BitConverter.GetBytes(((short) (2 & 0xFFFF)) & 0xFFFF).length); //Flags PHF_COMPLETE
        Crypt.EasyCrypt(buf);
        System.arraycopy(buf, 0, pack, 9, buf.length);
        Send(pack);
        byte[] res = Receive(9);
        PacketHdr hdr = UDT.ReadStruct(res, 0, 9, new PacketHdr());
        if ((hdr.Type & 0xFF) != 0 || hdr.PacketSize != 32) {
            throw new CodeException("Login package problem", Code.NETWORK_ERROR);
        }
        res = Crypt.EasyDecrypt(Receive(hdr.PacketSize));
        BuildRec rec = UDT.ReadStruct(res, 0, 32, new BuildRec());
        if (rec.StatusCode != Code.DONE) {
            throw new CodeException(rec.StatusCode);
        }
        SignData = rec.SignData;
        //SignData = new byte[]{(byte) 0x8f, 0x65, 0x23, 0x5a, (byte) 0xd0, (byte) 0xcf, (byte) 0x9b, 0x11, (byte) 0x8d, 0x2b, 0x0d, 0x67, (byte) 0xc8, 0x02, 0x31, 0x44};
        SendAccountPassword();
    }

    public final void Disconnect() throws IOException {
        try {
            Close();
        } catch (Exception e) {
        }
    }

    public final void Decompress(InBuf buf) {
        int realSize = buf.Int();
        int comprSize = buf.Int();
        byte[] data = buf.Bytes(comprSize);
        byte[] res = Decompressor.decompress(data, realSize);
        buf.SetBuf(res);
    }

    private void SendAccountPassword() throws IOException {
        byte[] buf = new byte[8 + api.apiCredentials.get().getPassword().length() * 2 + 2 * 2];
        System.arraycopy(BitConverter.getBytesLong((Long) api.apiCredentials.get().getAccountId()), 0, buf, 0, BitConverter.getBytesLong((Long) api.apiCredentials.get().getAccountId()).length);
        System.arraycopy(api.apiCredentials.get().getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_16LE), 0, buf, 8, api.apiCredentials.get().getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_16LE).length);
        System.arraycopy(("MQ").getBytes(java.nio.charset.StandardCharsets.UTF_16LE), 0, buf, 8 + api.apiCredentials.get().getPassword().length() * 2, ("MQ").getBytes(java.nio.charset.StandardCharsets.UTF_16LE).length);
        byte[] cryptKey = MD5.computeMD5(new byte[][]{buf, SignData});
        RandData = new byte[]{(byte) 0x84, 0x3f, 0x63, (byte) 0xdb, (byte) 0xd4, 0x50, (byte) 0xd4, (byte) 0xab, (byte) 0xdf, (byte) 0x9a, (byte) 0xa0, (byte) 0x86, 0x5d, 0x2a, (byte) 0xc7, 0x3e}; //TODO
        buf = new byte[0x22];
        System.arraycopy(BitConverter.GetBytes(((short) (0xbbda & 0xFFFF)) & 0xFFFF), 0, buf, 0, BitConverter.GetBytes(((short) (0xbbda & 0xFFFF)) & 0xFFFF).length); //s0 TODO rand
        System.arraycopy(cryptKey, 0, buf, 2, cryptKey.length); // cryp key
        System.arraycopy(cryptKey, 0, buf, 0x12, cryptKey.length); // rand data
        byte[] pack = new byte[9 + buf.length];
        pack[0] = 1; //Type
        System.arraycopy(BitConverter.GetBytes(buf.length), 0, pack, 1, BitConverter.GetBytes(buf.length).length); //size
        byte[] id = BitConverter.GetBytes((short) GetSendId());
        System.arraycopy(id, 0, pack, 5, id.length); //ID
        System.arraycopy(BitConverter.GetBytes(((short) (2 & 0xFFFF)) & 0xFFFF), 0, pack, 7, BitConverter.GetBytes(((short) (2 & 0xFFFF)) & 0xFFFF).length); //Flags PHF_COMPLETE
        Crypt.EasyCrypt(buf);
        System.arraycopy(buf, 0, pack, 9, buf.length);
        Send(pack);
        AcceptAuthorized();
    }

    private void AcceptAuthorized() throws IOException {
        byte[] res = Receive(9);
        PacketHdr hdr = UDT.ReadStruct(res, 0, 9, new PacketHdr());
        if ((hdr.Type & 0xFF) != 1) //|| hdr.PacketSize != 32
        {
            throw new CodeException("Password issue", Code.INVALID_PASSWORD);
        }
        res = Crypt.EasyDecrypt(Receive(hdr.PacketSize));
        LoginRcv1 rec = UDT.ReadStruct(res, 0, 0x2C, new LoginRcv1());
        if (rec.StatusCode != Code.DONE && rec.StatusCode != Code.ADVANCED_AUTHORIZATION) {
            throw new CodeException(rec.StatusCode);
        }
        TradeBuild = rec.TradeBuild;
        SymBuild = rec.SymBuild;
        InBuf buf = new InBuf(res, 0x2C);
        while (buf.gethasData()) {
            switch ((buf.Byte() & 0xFF)) {
                case 0:
                    ServerName = buf.Str();
                    break;
                case 1:
                    SrvCert = buf.ByteAr();
                    break;
                case 7:
                    AesKey = buf.ByteAr();
                    byte[] key = GetCryptKey(AesKey);
                    Encryptor = new PackEncrypt(key);
                    Decryptor = new PackDecrypt(key);
                    break;
                case 0xA:
                    buf.ByteAr(); //bStatus = bufMan.GetRecord(pConnectTime, szConnectTime);
                    break;
                case 0xB:
                    buf.ByteAr(); //bStatus = bufMan.GetRecord(pNetAddr, szNetAddr);
                    break;
                case 0xC:
                    buf.ByteAr(); //bStatus = bufMan.GetRecord(varÑ4, varC0);
                    break;
                case 0x13:
                    buf.ByteAr(); //bStatus = bufMan.GetRecord(pOneTimeKey, szOneTimeKey);
                    break;
                case 0x1B:
                    ProtectKey = GetProtectKey(buf.ByteAr());
                    break;
                case (byte) 0x1C:
                    LoginId = new LoginId().Decode(buf.ByteAr()) ^ ((Long) api.apiCredentials.get().getAccountId()) ^ CLIENT_BUILD ^ BitConverter.ToInt64(SignData, 0) ^ 0x05286AED3286692AL;
                    break;
                case 0x23:
//					LoginIdEx = GetLoginId(QC.LoginIdWebServerUrls, buf.ByteAr(), "DecodeEx");
                    LoginIdEx = GetLoginId(((Mt5ApiCredentials) api.apiCredentials.get()).getLoginIdWebServerUrls(), buf.ByteAr(), "decodeex");
                    break;
                default:
                    buf.ByteAr();
                    break;
            }
        }
        if (rec.StatusCode == Code.ADVANCED_AUTHORIZATION) {
            try {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                KeyStore keystore = KeyStore.getInstance("PKCS12");
                keystore.load(new ByteArrayInputStream(api.PfxFile), api.PfxFilePassword.toCharArray());
                kmf.init(keystore, api.PfxFilePassword.toCharArray());
                Enumeration<String> aliases = keystore.aliases();
                PrivateKey key = null;
                X509Certificate cert = null;
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    key = (PrivateKey) keystore.getKey(alias, api.PfxFilePassword.toCharArray());
                    if (key != null)
                        cert = (X509Certificate) keystore.getCertificate(alias);
                }
                if (cert == null)
                    throw new RuntimeException("RSA private key not found");
                byte[] encodedCert = cert.getEncoded();
                Signature privateSignature = Signature.getInstance("SHA1withRSA");
                privateSignature.initSign(key);
                privateSignature.update(SignData);
                byte[] sign = privateSignature.sign();
                reverse(sign);
                OutBuf ob = new OutBuf();
                ob.DataToBuffer(new byte[16]);
                ob.ByteToBuffer((byte) 4);
                ob.LongToBuffer(sign.length);
                ob.DataToBuffer(sign);
                ob.ByteToBuffer((byte) 3);
                ob.LongToBuffer(encodedCert.length);
                ob.DataToBuffer(encodedCert);
                SendPacket((byte) 2, ob, false);
            } catch (Exception ex) {
                throw new IOException("Invalid cerificate: " + ex.getMessage());
            }
        }
        RequestTradeInfo();
    }

    private long GetLoginId(List<String> urls, byte[] loginhash, String function) throws IOException {
        LoginIdWebServer lid = new LoginIdWebServer();
        long loginid = 0;
        boolean got = false;
        Exception ex = null;
        for (int i = 0; i < urls.size(); i++) {
            try {
                loginid = lid.Decode(urls.get(i) + "/" + function, api.guid, loginhash);
                got = true;
                break;
            } catch (Exception e) {
                ex = e;
            }
        }
        if (!got)
            if (ex == null)
                throw new RuntimeException("Cannot recieve LoginID");
            else
                throw new RuntimeException("Cannot recieve LoginID: " + ex.getMessage());
        return loginid ^ toLong(api.apiCredentials.get().getAccountId()) ^ CLIENT_BUILD ^ BitConverter.ToInt64(SignData, 0) ^ 0x05286AED3286692AL;
    }


    private byte[] GetProtectKey(byte[] protectKey) {
        byte[] buf = new byte[8 + api.apiCredentials.get().getPassword().length() * 2 + 2 * 2];
        System.arraycopy(BitConverter.GetBytes((Long) api.apiCredentials.get().getAccountId()), 0, buf, 0, BitConverter.GetBytes((Long) api.apiCredentials.get().getAccountId()).length);
        System.arraycopy(api.apiCredentials.get().getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_16LE), 0, buf, 8, api.apiCredentials.get().getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_16LE).length);
        System.arraycopy(("MQ").getBytes(java.nio.charset.StandardCharsets.UTF_16LE), 0, buf, 8 + api.apiCredentials.get().getPassword().length() * 2, ("MQ").getBytes(java.nio.charset.StandardCharsets.UTF_16LE).length);
        byte[] key = MD5.computeMD5(buf);
        byte[] data = new byte[protectKey.length + 16];
        System.arraycopy(key, 0, data, protectKey.length, key.length);
        System.arraycopy(Crypt.Decode(protectKey, key), 0, data, 0, Crypt.Decode(protectKey, key).length);
        SHA256Native sha = new SHA256Native();
        return sha.ComputeHash(data);
    }

    public final void RequestTradeInfo() throws IOException {
        OutBuf buf = new OutBuf();
        buf.ByteToBuffer((byte) 0x22); //cmd
        buf.LongToBuffer(8); //size
        buf.LongLongToBuffer(-1); //data
        buf.ByteToBuffer((byte) 0x27); //cmd
        buf.LongToBuffer(4); //size
        buf.LongToBuffer(0); //data m_Config.s10E TODO?

        long softid = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        buf.ByteToBuffer((byte) 0x6C);                            //cmd (send soft id)
        buf.LongToBuffer(8);                               //size
        buf.LongLongToBuffer(softid);                          //data

        buf.ByteToBuffer((byte) 0x84);                                //cmd (send application info)
        buf.LongToBuffer(0);                                   //size
        buf.ByteToBuffer((byte) 0x7F);

        byte[] ap = ApplicationInfo.getApplicationInfo();
        buf.LongToBuffer((short) ap.length);
        buf.DataToBuffer(ap);

        buf.ByteToBuffer((byte) 0x18); //cmd (request Mails)
        buf.LongToBuffer(8); //size
        buf.LongLongToBuffer(0); //data m_Mails.GetLastMailTime()
        RequestSymbols(buf); //request Symbols
        RequestSpreads(buf); //request Spreads
        RequestTickers(buf); //request Tickers
        AcceptLoginId(buf);
        SendPacket((byte) 0xC, buf);
    }

	/*void vSubscribeClient::RequestTicks()
	{
	    if (m_pClient->GetServerStatus() != vAcceptAccount)
	        return;
	    vSockBufManager bufMan(0);
	    m_Crit.EnterCriticalSection();
	    bufMan.ByteToBuffer(9);
	    bufMan.LongToBuffer(m_arrTicks.GetSize());
	    for (int i = 0; i < m_arrTicks.GetSize(); i++)
	        bufMan.LongToBuffer(m_arrTicks[i]->m_SubSym.m_nId);
	    m_bRequest = true;
	    m_Crit.LeaveCriticalSection();
	    m_pClient->SendPacket(0x69, &bufMan);
	}*/


    //C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] GetCryptKey(byte[] aesKey)
    private byte[] GetCryptKey(byte[] aesKey) {
        byte[] buf = new byte[8 + api.apiCredentials.get().getPassword().length() * 2 + 2 * 2];
        System.arraycopy(BitConverter.getBytesLong((Long) api.apiCredentials.get().getAccountId()), 0, buf, 0, BitConverter.getBytesLong(toLong(api.apiCredentials.get().getAccountId())).length);
        System.arraycopy(api.apiCredentials.get().getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_16LE), 0, buf, 8, api.apiCredentials.get().getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_16LE).length);
        System.arraycopy(("MQ").getBytes(java.nio.charset.StandardCharsets.UTF_16LE), 0, buf, 8 + api.apiCredentials.get().getPassword().length() * 2, ("MQ").getBytes(java.nio.charset.StandardCharsets.UTF_16LE).length);
        byte key[] = MD5.computeMD5(buf);
        //int[] tmp = new int[]{0x41, 0x23, 0xde, 0xfb, 0x6b, 0xb3, 0x40, 0xdb, 0xfc, 0xb0, 0x7a, 0xd9, 0x34, 0xa8, 0xf0, 0x9f, 0xaf, 0x1b, 0x31, 0x4d, 0x93, 0xe0, 0xcc, 0x05, 0x0e, 0xc3, 0x65, 0xad, 0x00, 0x73, 0x07, 0xf0, 0xf4, 0x9c, 0xbc, 0x86, 0x55, 0x50, 0x5b, 0xb6, 0xdf, 0xab, 0xa2, 0x19, 0x53, 0xb7, 0x84, 0xa0, 0x6f, 0x8d, 0xc4, 0xd8, 0x1d, 0xbd, 0xc3, 0x6d, 0xeb, 0xf0, 0x97, 0xee, 0xbb, 0xcc, 0x5c, 0xcf, 0xbb, 0x18, 0xcf, 0xb4, 0x98, 0x21, 0x19, 0xed, 0xef, 0x5c, 0xe8, 0x3c, 0x05, 0x4c, 0xc4, 0xdf, 0xb7, 0xa6, 0xa1, 0xca, 0xb4, 0xb4, 0xb0, 0x35, 0xe9, 0xf7, 0x7b, 0x56, 0x3d, 0x10, 0x31, 0x70, 0x7f, 0xe1, 0x3e, 0x0c, 0x9c, 0xef, 0x1f, 0x86, 0x16, 0x0a, 0x75, 0xcc, 0xb1, 0x31, 0x58, 0x63, 0x70, 0xb0, 0xed, 0xab, 0xbf, 0x8b, 0x3b, 0x63, 0xf2, 0x1f, 0x3a, 0x6f, 0xee, 0x07, 0x2d, 0xd9, 0x26, 0x3d, 0x32, 0x17, 0xca, 0x81, 0x18, 0x8c, 0x3a, 0xff, 0x70, 0x51, 0xc1, 0x2c, 0xe7, 0x34, 0x80, 0xf1, 0xd3, 0x01, 0xa8, 0x0b, 0x0b, 0x01, 0xed, 0xb2, 0xfc, 0xc1, 0x37, 0x78, 0xf9, 0x14, 0x9a, 0x75, 0xd3, 0x5b, 0x88, 0xa1, 0xaa, 0x04, 0x45, 0x81, 0x02, 0x51, 0x9c, 0x06, 0x19, 0x5b, 0xd1, 0xb2, 0x79, 0x56, 0xd6, 0xfc, 0xc9, 0x16, 0xc1, 0xf6, 0xe8, 0xd2, 0x7e, 0x2d, 0x3d, 0x29, 0xc1, 0xd0, 0x48, 0x62, 0x3f, 0x15, 0x7d, 0xf8, 0x1e, 0xd9, 0x53, 0x56, 0xaa, 0x87, 0x98, 0xdf, 0x49, 0x3a, 0x07, 0x31, 0xb1, 0x26, 0x1c, 0xab, 0x58, 0x34, 0x27, 0x2d, 0x2c, 0xec, 0xa0, 0x1e, 0x85, 0x98, 0xba, 0xb4, 0x58, 0xa6, 0x3a, 0x6f, 0xad, 0x4f, 0x8a, 0xe7, 0x53, 0x76, 0x09, 0xc8, 0xd2, 0xd3, 0xa6, 0x1b, 0xa1, 0x50, 0xad, 0xc7, 0x99, 0xb3, 0xe0, 0x57, 0xaa, 0x7e, 0xca, 0xfd};
        //for (int i = 0; i <aesKey.length ; i++) {aesKey[i] = (byte)tmp[i];}
        vAES aes = new vAES();
        return aes.EncryptData(aesKey, key);
    }

    private void AcceptLoginId(OutBuf buf) {
        buf.ByteToBuffer((byte) 0x58); //cmd
        buf.LongToBuffer(8); //size
        buf.Add(LoginId ^ 0x5286AED3286692AL); //data

        buf.ByteToBuffer((byte) 0x86);                                    //cmd
        buf.LongToBuffer(8);                                       //size
        buf.Add(LoginIdEx ^ 0x05286AED3286692AL);   //data
    }

    private void RequestSymbols(OutBuf buf) {
        //MD5Managed md = new MD5Managed();
        //md.HashCore(new byte[16], 0 , 16);
        //md.HashFinal();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var key = new byte[]{ 0xd4, 0x1d, 0x8c, 0xd9, 0x8f, 0x00, 0xb2, 0x04, 0xe9, 0x80, 0x09, 0x98, 0xec, 0xf8, 0x42, 0x7e };
        byte[] key = new byte[]{(byte) 0xd4, 0x1d, (byte) 0x8c, (byte) 0xd9, (byte) 0x8f, 0x00, (byte) 0xb2, 0x04, (byte) 0xe9, (byte) 0x80, 0x09, (byte) 0x98, (byte) 0xec, (byte) 0xf8, 0x42, 0x7e};
        buf.ByteToBuffer((byte) 7);
        buf.LongToBuffer(0x2C);
        buf.LongLongToBuffer(0); //time
        buf.LongToBuffer(0); //m_arrSym.GetSize()
        buf.DataToBuffer(key);
        buf.DataToBuffer(key);
    }

    private void RequestSpreads(OutBuf buf) {
        //MD5Managed md = new MD5Managed();
        //md.HashCore(new byte[16], 0 , 16);
        //md.HashFinal();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var key = new byte[] { 0xd4, 0x1d, 0x8c, 0xd9, 0x8f, 0x00, 0xb2, 0x04, 0xe9, 0x80, 0x09, 0x98, 0xec, 0xf8, 0x42, 0x7e };
        byte[] key = new byte[]{(byte) 0xd4, 0x1d, (byte) 0x8c, (byte) 0xd9, (byte) 0x8f, 0x00, (byte) 0xb2, 0x04, (byte) 0xe9, (byte) 0x80, 0x09, (byte) 0x98, (byte) 0xec, (byte) 0xf8, 0x42, 0x7e};
        buf.ByteToBuffer((byte) 0x28);
        buf.LongToBuffer(28);
        buf.LongLongToBuffer(0); //time
        buf.LongToBuffer(0); //m_arrSym.GetSize()
        buf.DataToBuffer(key);
    }

    private void RequestTickers(OutBuf buf) {
        buf.ByteToBuffer((byte) 0x11);
        buf.LongToBuffer(16);
        buf.DataToBuffer(new byte[16]);
    }


    public final void Connect() throws IOException {
        try {
            Connect(Host, Port);
        } catch (RuntimeException e) {
            Close();
            throw e;
        }
    }
}