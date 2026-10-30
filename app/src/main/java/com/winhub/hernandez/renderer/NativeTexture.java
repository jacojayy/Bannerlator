package com.winhub.hernandez.renderer;

import java.nio.ByteBuffer;

/**
 * Common base for {@link Texture}s that are backed by a native AHardwareBuffer whose
 * pixels are directly reachable from Java through a {@link ByteBuffer} view.
 *
 * <p>Introduced with the GameNative #1620 GPUImage split so that {@code Drawable}
 * (and any renderer) can obtain the virtual data / row stride of a hardware-buffer-backed
 * texture without caring which path produced it (Vulkan/DRI3/Present).</p>
 */
public abstract class NativeTexture extends Texture {
    public abstract short getStride();
    public abstract ByteBuffer getVirtualData();
}
