package com.ob.api.mtx.mt5;

/**
 * New quote event arguments.
 */
public class Quote {
    public int id;
    /**
     * Trading instrument.
     */
    public String Symbol;
    /**
     * Bid.
     */
    public double Bid;
    /**
     * Ask.
     */
    public double Ask;
    /**
     * Server time.
     */
    public java.time.LocalDateTime Time = java.time.LocalDateTime.MIN;
    /**
     * Last deal price.
     */
    public double Last;
    /**
     * Volume
     */
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ulong Volume;
    public long Volume;
    //C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: internal ulong UpdateMask;
    public long UpdateMask;
    public short BankId;
    public long s44;

    /**
     * Convert to string.
     *
     * @return "Symbol Bid Ask"
     */
    @Override
    public String toString() {
        return Symbol + " " + Bid + " " + Ask;
    }
}