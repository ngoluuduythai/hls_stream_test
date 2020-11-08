package com.pedro.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.pedro.encoder.utils.CodecUtil;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by pedro on 18/09/19.
 */
public abstract class BaseEncoder implements EncoderCallback {

  private static final String TAG = "BaseEncoder";
  private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
  private HandlerThread handlerThread;
  protected BlockingQueue<Frame> queue = new ArrayBlockingQueue<>(80);
  protected MediaCodec codec;
  protected long presentTimeUs;
  protected volatile boolean running = false;
  protected boolean isBufferMode = true;
  protected CodecUtil.Force force = CodecUtil.Force.FIRST_COMPATIBLE_FOUND;
  private MediaCodec.Callback callback;

  public void restart() {
    start(false);
    initCodec();
  }

  public void start() {
    start(true);
    initCodec();
  }

  private void initCodec() {
    handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      createAsyncCallback();
      codec.setCallback(callback, handler);
      codec.start();
    } else {
      codec.start();
      handler.post(new Runnable() {
        @Override
        public void run() {
          while (running) {
            try {
              getDataFromEncoder();
            } catch (IllegalStateException e) {
              Log.i(TAG, "Encoding error", e);
            }
          }
        }
      });
    }
    running = true;
  }

  public abstract void start(boolean resetTs);

  protected abstract void stopImp();

  public void stop() {
    running = false;
    stopImp();
    if (handlerThread != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        handlerThread.quitSafely();
      } else {
        handlerThread.quit();
      }
    }
    queue.clear();
    try {
      codec.stop();
      codec.release();
      codec = null;
    } catch (IllegalStateException | NullPointerException e) {
      codec = null;
    }
  }

  protected abstract MediaCodecInfo chooseEncoder(String mime);

  protected void getDataFromEncoder() throws IllegalStateException {
    if (isBufferMode) {
      int inBufferIndex = codec.dequeueInputBuffer(0);
      if (inBufferIndex >= 0) {
        inputAvailable(codec, inBufferIndex);
      }
    }
    for (; running; ) {
      int outBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);


      //while (outBufferIndex >= 0) {
      //  ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
      //  byte[] outData = new byte[bufferInfo.size];
      //  outputBuffer.get(outData);
      //  if (sps != null && pps != null) {
      //    ByteBuffer frameBuffer = ByteBuffer.wrap(outData);
      //    frameBuffer.putInt(bufferInfo.size - 4);
      //    frameListener.frameReceived(outData, 0, outData.length);
      //  } else {
      //    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
      //    if (spsPpsBuffer.getInt() == 0x00000001) {
      //      System.out.println("parsing sps/pps");
      //    } else {
      //      System.out.println("something is amiss?");
      //    }
      //    int ppsIndex = 0;
      //    while(!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {
      //
      //    }
      //    ppsIndex = spsPpsBuffer.position();
      //    sps = new byte[ppsIndex - 8];
      //    System.arraycopy(outData, 4, sps, 0, sps.length);
      //    pps = new byte[outData.length - ppsIndex];
      //    System.arraycopy(outData, ppsIndex, pps, 0, pps.length);
      //    if (null != parameterSetsListener) {
      //      parameterSetsListener.avcParametersSetsEstablished(sps, pps);
      //    }
      //  }
      //  mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
      //  outBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
      //}

      if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        MediaFormat mediaFormat = codec.getOutputFormat();

        // SPS, PPS NALU data in Annex-B format in extradata
        //mediaFormat.setByteBuffer("csd-0", extradata);
        //// …
        //mediaCodec.configure(mediaFormat, surface, 0, 0);

        formatChanged(codec, mediaFormat);
      } else if (outBufferIndex >= 0) {
        outputAvailable(codec, outBufferIndex, bufferInfo);
      } else {
        break;
      }
    }
  }

  protected abstract Frame getInputFrame() throws InterruptedException;

  private void processInput(@NonNull ByteBuffer byteBuffer, @NonNull MediaCodec mediaCodec,
      int inBufferIndex) throws IllegalStateException {
    try {
      Frame frame = getInputFrame();
      byteBuffer.clear();
      byteBuffer.put(frame.getBuffer(), frame.getOffset(), frame.getSize());
      long pts = System.nanoTime() / 1000 - presentTimeUs;
      mediaCodec.queueInputBuffer(inBufferIndex, 0, frame.getSize(), pts, 0);
    } catch (InterruptedException | NullPointerException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected abstract void checkBuffer(@NonNull ByteBuffer byteBuffer,
      @NonNull MediaCodec.BufferInfo bufferInfo);

  protected abstract void sendBuffer(@NonNull ByteBuffer byteBuffer,
      @NonNull MediaCodec.BufferInfo bufferInfo);

  private void processOutput(@NonNull ByteBuffer byteBuffer, @NonNull MediaCodec mediaCodec,
      int outBufferIndex, @NonNull MediaCodec.BufferInfo bufferInfo) throws IllegalStateException {
    checkBuffer(byteBuffer, bufferInfo);
    sendBuffer(byteBuffer, bufferInfo);
    mediaCodec.releaseOutputBuffer(outBufferIndex, false);
  }

  public void setForce(CodecUtil.Force force) {
    this.force = force;
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public void inputAvailable(@NonNull MediaCodec mediaCodec, int inBufferIndex)
      throws IllegalStateException {
    ByteBuffer byteBuffer;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      byteBuffer = mediaCodec.getInputBuffer(inBufferIndex);
    } else {
      byteBuffer = mediaCodec.getInputBuffers()[inBufferIndex];
    }
    processInput(byteBuffer, mediaCodec, inBufferIndex);
  }

  @Override
  public void outputAvailable(@NonNull MediaCodec mediaCodec, int outBufferIndex,
      @NonNull MediaCodec.BufferInfo bufferInfo) throws IllegalStateException {
    ByteBuffer byteBuffer;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      byteBuffer = mediaCodec.getOutputBuffer(outBufferIndex);
    } else {
      byteBuffer = mediaCodec.getOutputBuffers()[outBufferIndex];
    }
    processOutput(byteBuffer, mediaCodec, outBufferIndex, bufferInfo);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private void createAsyncCallback() {
    callback = new MediaCodec.Callback() {
      @Override
      public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inBufferIndex) {
        try {
          inputAvailable(mediaCodec, inBufferIndex);
        } catch (IllegalStateException e) {
          Log.i(TAG, "Encoding error", e);
        }
      }

      @Override
      public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outBufferIndex,
          @NonNull MediaCodec.BufferInfo bufferInfo) {
        try {
          outputAvailable(mediaCodec, outBufferIndex, bufferInfo);
        } catch (IllegalStateException e) {
          Log.i(TAG, "Encoding error", e);
        }
      }

      @Override
      public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
        Log.e(TAG, "Error", e);
      }

      @Override
      public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
          @NonNull MediaFormat mediaFormat) {
        formatChanged(mediaCodec, mediaFormat);
      }
    };
  }
}
