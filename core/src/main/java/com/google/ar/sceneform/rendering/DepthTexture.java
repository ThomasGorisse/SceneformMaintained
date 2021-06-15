package com.google.ar.sceneform.rendering;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.filament.Texture;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.BufferHelper;

import java.nio.ByteBuffer;

/**
 * <pre>
 *     The DepthTexture class holds a special Texture to store
 *     information from a DepthImage to realize the occlusion of
 *     virtual objects behind real objects.
 * </pre>
 */
public class DepthTexture {
    @Nullable private final com.google.android.filament.Texture filamentTexture;
    private final Handler handler = new Handler(Looper.myLooper());

    /**
     * <pre>
     *      A call to this constructor creates a new Filament Texture which is
     *      later used to feed in data from a DepthImage.
     * </pre>
     *
     * @param width
     * @param height
     */
    public DepthTexture(int width, int height) {
        filamentTexture = new com.google.android.filament.Texture.Builder()
                .width(width)
                .height(height)
                .sampler(com.google.android.filament.Texture.Sampler.SAMPLER_2D)
                .format(Texture.InternalFormat.RG8)
                .levels(1)
                .build(EngineInstance.getEngine().getFilamentEngine());

        ResourceManager.getInstance()
                .getDepthTextureCleanupRegistry()
                .register(this, new CleanupCallback(filamentTexture));
    }

    @Nullable
    public Texture getFilamentTexture() {
        return filamentTexture;
    }


    /**
     * <pre>
     *     This is the most important function of this class.
     *     The Filament Texture is updated based on the newest
     *     DepthImage. To solve a problem with a to early
     *     released DepthImage the ByteBuffer which holds all
     *     necessary data is cloned. The cloned ByteBuffer is unaffected
     *     of a released DepthImage and therefore produces not
     *     a flickering result.
     * </pre>
     *
     * @param depthImage {@link Image}
     */
    public void updateDepthTexture(Image depthImage) {
        if (filamentTexture == null)
            return;

        IEngine engine = EngineInstance.getEngine();

        Image.Plane plane = depthImage.getPlanes()[0];

        ByteBuffer buffer = plane.getBuffer();
        ByteBuffer clonedBuffer = BufferHelper.cloneByteBuffer(buffer);

        Texture.PixelBufferDescriptor pixelBufferDescriptor = new Texture.PixelBufferDescriptor(
                clonedBuffer,
                Texture.Format.RG,
                Texture.Type.UBYTE,
                1,
                0,
                0,
                0,
                handler,
                null
        );

        filamentTexture.setImage(
                engine.getFilamentEngine(),
                0,
                pixelBufferDescriptor
        );
    }

    /**
     * Cleanup filament objects after garbage collection
     */
    private static final class CleanupCallback implements Runnable {
        @Nullable private final com.google.android.filament.Texture filamentTexture;

        CleanupCallback(@Nullable com.google.android.filament.Texture filamentTexture) {
            this.filamentTexture = filamentTexture;
        }

        @Override
        public void run() {
            AndroidPreconditions.checkUiThread();

            IEngine engine = EngineInstance.getEngine();
            if (engine == null || !engine.isValid()) {
                return;
            }
            if (filamentTexture != null) {
                engine.destroyTexture(filamentTexture);
            }
        }
    }
}