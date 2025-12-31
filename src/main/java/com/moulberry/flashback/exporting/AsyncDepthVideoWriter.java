package com.moulberry.flashback.exporting;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.SneakyThrow;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class AsyncDepthVideoWriter implements AutoCloseable {

    private final ArrayBlockingQueue<DepthFrame> encodeQueue;
    private final ArrayBlockingQueue<Long> reusePictureData;

    private final AtomicBoolean finishEncodeThread = new AtomicBoolean(false);
    private final AtomicBoolean finishedWriting = new AtomicBoolean(false);
    private final AtomicReference<Throwable> threadedError = new AtomicReference<>(null);

    private record DepthFrame(long pointer, int size, int width, int height) implements AutoCloseable {
        public void close() {
            MemoryUtil.nmemFree(this.pointer);
        }
    }

    public AsyncDepthVideoWriter(String filename, int width, int height, double fps, float nearPlane, float farPlane) {
        try {
            // Force .mov extension for Raw Video container
            if (filename.contains(".")) {
                filename = filename.substring(0, filename.lastIndexOf('.'));
            }
            filename += ".mov";

            Flashback.LOGGER.info("Starting RAW Float Depth Export: " + filename);
            Flashback.LOGGER.info("WARNING: High Bandwidth Required (~740 MB/s)");

            final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(filename, width, height, 0);

            // --- SETTINGS FOR RAW FLOAT MOV ---
            recorder.setFormat("mov");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_RAWVIDEO); // Uncompressed
            recorder.setPixelFormat(avutil.AV_PIX_FMT_GRAYF32LE); // 32-bit Float
            recorder.setFrameRate(fps);

            recorder.setMetadata("comment", "Flashback Depth Pass | Near: " + nearPlane + " | Far: " + farPlane);

            recorder.setMetadata("clip_start", String.valueOf(nearPlane));
            recorder.setMetadata("clip_end", String.valueOf(farPlane));
            // No GOP size needed for raw video

            recorder.start();

            // Queue setup (Reduced queue size to save RAM since frames are huge)
            this.encodeQueue = new ArrayBlockingQueue<>(16);
            this.reusePictureData = new ArrayBlockingQueue<>(16);

            Thread encodeThread = createEncodeThread(recorder);
            encodeThread.start();

        } catch (Exception e) {
            throw SneakyThrow.sneakyThrow(e);
        }
    }

    private Thread createEncodeThread(FFmpegFrameRecorder recorder) {
        Thread encodeThread = new Thread(() -> {
            while (true) {
                DepthFrame src;
                try {
                    src = this.encodeQueue.poll(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw SneakyThrow.sneakyThrow(e);
                }

                try {
                    if (src == null) {
                        if (this.finishEncodeThread.get()) {
                            recorder.stop();
                            recorder.close();
                            this.finishedWriting.set(true);
                            return;
                        } else {
                            continue;
                        }
                    }

                    ByteBuffer buffer = MemoryUtil.memByteBuffer(src.pointer, src.size);

                    // RECORDING 32-BIT FLOAT
                    recorder.recordImage(
                            src.width,
                            src.height,
                            Frame.DEPTH_FLOAT,
                            1,
                            src.width * 4, // Stride = Width * 4 Bytes
                            avutil.AV_PIX_FMT_GRAYF32LE,
                            buffer
                    );

                    if (this.reusePictureData != null) {
                        if (this.reusePictureData.offer(src.pointer)) {
                            src = null;
                        }
                    }

                } catch (Throwable t) {
                    try { recorder.release(); } catch (Exception e) { e.printStackTrace(); }
                    this.threadedError.set(t);
                    this.finishEncodeThread.set(true);
                    this.finishedWriting.set(true);
                    return;
                } finally {
                    if (src != null) src.close();
                }
            }
        });
        encodeThread.setName("Depth Encode Thread");
        return encodeThread;
    }

    public void encode(ByteBuffer src, int width, int height) {
        Throwable t = this.threadedError.get();
        if (t != null) SneakyThrow.sneakyThrow(t);

        if (this.finishEncodeThread.get()) throw new IllegalStateException("Cannot encode after finish()");

        // 4 BYTES PER PIXEL
        int sizeBytes = width * height * 4;

        Long ptr = this.reusePictureData.poll();
        if (ptr == null) {
            ptr = MemoryUtil.nmemAlloc(sizeBytes);
        }

        MemoryUtil.memByteBuffer(ptr, sizeBytes).put(src);

        src.rewind();

        while (true) {
            try {
                this.encodeQueue.put(new DepthFrame(ptr, sizeBytes, width, height));
                break;
            } catch (InterruptedException ignored) {}
        }
    }

    public void finish() {
        while (!this.encodeQueue.isEmpty()) {
            LockSupport.parkNanos("waiting for depth queue", 100000L);
        }
        this.finishEncodeThread.set(true);
        while (!this.finishedWriting.get()) {
            LockSupport.parkNanos("waiting for depth thread", 100000L);
        }
    }

    @Override
    public void close() {
        finish();
        for (Long ptr : this.reusePictureData) {
            if (ptr != null && ptr != 0) MemoryUtil.nmemFree(ptr);
        }
    }
}