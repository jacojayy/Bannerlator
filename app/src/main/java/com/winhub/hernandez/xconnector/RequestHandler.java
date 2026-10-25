package com.winhub.hernandez.xconnector;

import java.io.IOException;

public interface RequestHandler {
    boolean handleRequest(Client client) throws IOException;
}
