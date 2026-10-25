package com.winhub.hernandez.renderer;

import com.winhub.hernandez.xserver.Drawable;

class RenderableWindow {
    final Drawable content;
    short rootX;
    short rootY;

    public RenderableWindow(Drawable content, int rootX, int rootY) {
        this.content = content;
        this.rootX = (short) rootX;
        this.rootY = (short) rootY;
    }
}
