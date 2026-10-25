package com.winhub.hernandez.xserver.extensions;

import com.winhub.hernandez.xconnector.XInputStream;
import com.winhub.hernandez.xconnector.XOutputStream;
import com.winhub.hernandez.xserver.XClient;
import com.winhub.hernandez.xserver.errors.XRequestError;

import java.io.IOException;

public interface Extension {
    String getName();

    byte getMajorOpcode();

    byte getFirstErrorId();

    byte getFirstEventId();

    void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError;
}
