package com.winhub.hernandez.xserver.events;

import com.winhub.hernandez.xconnector.XOutputStream;
import com.winhub.hernandez.xconnector.XStreamLock;

import java.io.IOException;

public class RawEvent extends Event {
    private final byte[] data;

    public RawEvent(byte[] data) {
        super(data[0]);
        this.data = data;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.write(data);
        }
    }
}
