package com.winhub.hernandez.xserver;

public interface XLock extends AutoCloseable {
    @Override
    void close();
}
