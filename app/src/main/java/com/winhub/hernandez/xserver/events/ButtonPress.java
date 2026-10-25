package com.winhub.hernandez.xserver.events;

import com.winhub.hernandez.xserver.Bitmask;
import com.winhub.hernandez.xserver.Window;

public class ButtonPress extends InputDeviceEvent {
    public ButtonPress(byte detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(4, detail, root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
