package com.winhub.hernandez.xenvironment.components;

import com.winhub.hernandez.sysvshm.SysVSHMConnectionHandler;
import com.winhub.hernandez.sysvshm.SysVSHMRequestHandler;
import com.winhub.hernandez.sysvshm.SysVSharedMemory;
import com.winhub.hernandez.xconnector.UnixSocketConfig;
import com.winhub.hernandez.xconnector.XConnectorEpoll;
import com.winhub.hernandez.xenvironment.EnvironmentComponent;
import com.winhub.hernandez.xserver.SHMSegmentManager;
import com.winhub.hernandez.xserver.XServer;

public class SysVSharedMemoryComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    public final UnixSocketConfig socketConfig;
    private SysVSharedMemory sysVSharedMemory;
    private final XServer xServer;

    public SysVSharedMemoryComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        sysVSharedMemory = new SysVSharedMemory();
        connector = new XConnectorEpoll(socketConfig, new SysVSHMConnectionHandler(sysVSharedMemory), new SysVSHMRequestHandler());
        connector.start();

        xServer.setSHMSegmentManager(new SHMSegmentManager(sysVSharedMemory));
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }

        sysVSharedMemory.deleteAll();
    }
}
