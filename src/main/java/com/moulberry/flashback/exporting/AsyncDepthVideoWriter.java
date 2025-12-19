package com.moulberry.flashback.exporting;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.SneakyThrow;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class AsyncDepthVideoWriter implements AutoCloseable {

    private final ArrayBlockingQueue<DepthFrame> encodeQueue;
    private final ArrayBlockingQueue<Long> reusePictureData; // Object pool for memory addresses

    private final AtomicBoolean finishEncodeThread = new AtomicBoolean(false);
    private final AtomicBoolean finishedWriting = new AtomicBoolean(false);
    private final AtomicReference<Throwable> threadedError = new AtomicReference<>(null);

    // Simple record to hold the native pointer to the frame data
    private record DepthFrame(long pointer, int size, int width, int height) implements AutoCloseable {
        public void close() {
            MemoryUtil.nmemFree(this.pointer);
        }
    }

    public AsyncDepthVideoWriter(String filename, int width, int height, double fps) {
        // Force settings for Depth: MKV container, FFV1 codec, Gray16LE pixel format
        try {
            // Ensure filename ends in .mkv
            if (!filename.endsWith(".mkv")) filename += ".mkv";

            Flashback.LOGGER.info("Starting Depth Pass Export: " + filename);

            final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(filename, width, height, 0);

            recorder.setVideoCodec(avcodec.AV_CODEC_ID_FFV1); // Lossless codec
            recorder.setFormat("matroska");
            recorder.setFrameRate(fps);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_GRAY16LE); // 16-bit Grayscale

            // GOP size can be large for FFV1
            recorder.setGopSize((int) Math.max(20, Math.min(240, Math.ceil(fps * 2))));

            recorder.start();

            // Queue setup
            this.encodeQueue = new ArrayBlockingQueue<>(32);
            this.reusePictureData = new ArrayBlockingQueue<>(32);

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

                    // Wrap the native pointer in a ByteBuffer for JavaCV to read
                    ByteBuffer buffer = MemoryUtil.memByteBuffer(src.pointer, src.size);

                    // Record the image
                    // depth=FRAME.DEPTH_SHORT (16-bit), channels=1, stride=width*2 bytes
                    recorder.recordImage(src.width, src.height, Frame.DEPTH_SHORT, 1, src.width * 2, avutil.AV_PIX_FMT_GRAY16LE, buffer);

                    // Reuse memory if possible
                    if (this.reusePictureData != null) {
                        if (this.reusePictureData.offer(src.pointer)) {
                            src = null; // Prevent closing/freeing
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

        int sizeBytes = width * height * 2;

        Long ptr = this.reusePictureData.poll();
        if (ptr == null) {
            ptr = MemoryUtil.nmemAlloc(sizeBytes);
        }

        // UPDATED LINE: Use memByteBuffer instead of memShortBuffer
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
        // Cleanup memory pool
        for (Long ptr : this.reusePictureData) {
            if (ptr != null && ptr != 0) MemoryUtil.nmemFree(ptr);
        }
    }
}