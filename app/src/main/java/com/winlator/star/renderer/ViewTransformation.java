package com.winlator.star.renderer;

import com.winlator.star.container.Container;

public class ViewTransformation {
    public int viewOffsetX;
    public int viewOffsetY;
    public int viewWidth;
    public int viewHeight;
    public float aspect;
    public float sceneScaleX;
    public float sceneScaleY;
    public float sceneOffsetX;
    public float sceneOffsetY;

    // Legacy entry point: letterbox (preserve aspect, center with bars). Same as OFF/FIT.
    public void update(int outerWidth, int outerHeight, int innerWidth, int innerHeight) {
        update(outerWidth, outerHeight, innerWidth, innerHeight, Container.FULLSCREEN_FIT);
    }

    // Fullscreen aspect-ratio mode (issue #71). The whole mapping is driven by a single `aspect`
    // scale factor; each mode only changes how that scale is chosen:
    //   OFF/FIT   -> min(sx, sy)               letterbox: largest fit that keeps the guest fully on-screen
    //   FILL      -> max(sx, sy)               crop-to-fill: smallest scale that covers the surface, overflow
    //                                          runs off the edges (negative offset, oversized view rect -> cropped)
    //   INTEGER   -> max(1, floor(min))        pixel-perfect: largest whole-number scale that still fits, centered
    // STRETCH is handled by the renderers directly (full-surface viewport) and never reaches this.
    public void update(int outerWidth, int outerHeight, int innerWidth, int innerHeight, int fullscreenMode) {
        float sx = (float)outerWidth / innerWidth;
        float sy = (float)outerHeight / innerHeight;

        switch (fullscreenMode) {
            case Container.FULLSCREEN_FILL:
                aspect = Math.max(sx, sy);
                break;
            case Container.FULLSCREEN_INTEGER:
                aspect = Math.max(1.0f, (float)Math.floor(Math.min(sx, sy)));
                break;
            case Container.FULLSCREEN_OFF:
            case Container.FULLSCREEN_FIT:
            default:
                aspect = Math.min(sx, sy);
                break;
        }

        viewWidth = (int)Math.ceil(innerWidth * aspect);
        viewHeight = (int)Math.ceil(innerHeight * aspect);
        viewOffsetX = (int)((outerWidth - innerWidth * aspect) * 0.5f);
        viewOffsetY = (int)((outerHeight - innerHeight * aspect) * 0.5f);

        sceneScaleX = (innerWidth * aspect) / outerWidth;
        sceneScaleY = (innerHeight * aspect) / outerHeight;
        sceneOffsetX = (innerWidth - innerWidth * sceneScaleX) * 0.5f;
        sceneOffsetY = (innerHeight - innerHeight * sceneScaleY) * 0.5f;
    }
}
