package com.ob.api.mtx.mt5;

/**
 * Connect event argumnets.
 */
public class ConnectEventArgs {
    /**
     * Connect exception. Can be null.
     */
    public Exception Exception;
    /**
     * Connect exception. Can be null.
     */
    public ConnectProgress Progress = ConnectProgress.values()[0];
}