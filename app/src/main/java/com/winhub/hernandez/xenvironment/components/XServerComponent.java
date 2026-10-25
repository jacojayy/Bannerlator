package com.winhub.hernandez.xenvironment.components;

import com.winhub.hernandez.xenvironment.EnvironmentComponent;
import com.winhub.hernandez.xconnector.XConnectorEpoll;
import com.winhub.hernandez.xconnector.UnixSocketConfig;
import com.winhub.hernandez.xserver.XClientConnectionHandler;
import com.winhub.hernandez.xserver.XClientRequestHandler;
import com.winhub.hernandez.xserver.XServer;

public class XServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final XServer xServer;
    private final UnixSocketConfig socketConfig;

    public XServerComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, new XClientConnectionHandler(xServer), new XClientRequestHandler());
        connector.setInitialInputBufferCapacity(262144);
        connector.setCanReceiveAncillaryMessages(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }

    public XServer getXServer() {
        return xServer;
    }
}
