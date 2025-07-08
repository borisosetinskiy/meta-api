package com.ob.api.mtx.mt5;

/**
 * Quote history event args.
 */
//C# TO JAVA CONVERTER WARNING: Java does not allow user-defined value types. The behavior of this class will differ from the original:
//ORIGINAL LINE: public struct QuoteHistoryEventArgs
public final class QuoteHistoryEventArgs {
    /**
     * Instrument.
     */
    public String Symbol;
    /**
     * History bars.
     */
    public Bar[] Bars;

    public QuoteHistoryEventArgs clone() {
        QuoteHistoryEventArgs varCopy = new QuoteHistoryEventArgs();

        varCopy.Symbol = this.Symbol;
        varCopy.Bars = this.Bars.clone();

        return varCopy;
    }
}