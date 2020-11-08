package com.pedro.rtplibrary.rtmp

import android.content.Context
import android.content.ContextWrapper
import android.media.MediaCodec.BufferInfo
import android.os.Build.VERSION_CODES
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.pedro.encoder.ts.H264TsSegmenter
import com.pedro.encoder.ts.TsSegment
import com.pedro.encoder.ts.m3u8.M3U8
import com.pedro.encoder.ts.m3u8.M3u8Builder
import com.pedro.rtplibrary.base.Camera1Base
import com.pedro.rtplibrary.network.AmazonUploader
import com.pedro.rtplibrary.network.AmazonUploader.AmazonView
import com.pedro.rtplibrary.network.FileType
import com.pedro.rtplibrary.network.FileType.TS
import com.pedro.rtplibrary.network.HLSFile
import com.pedro.rtplibrary.view.LightOpenGlView
import com.pedro.rtplibrary.view.OpenGlView
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.runBlocking
import net.ossrs.rtmp.ConnectCheckerRtmp
import net.ossrs.rtmp.SrsFlvMuxer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong

/**
 * More documentation see:
 * [com.pedro.rtplibrary.base.Camera1Base]
 *
 * Created by pedro on 25/01/17.
 */
class RtmpCamera1 : Camera1Base, AmazonView {
  private var srsFlvMuxer: SrsFlvMuxer
  private var context: Context? = null
  private var h264TsSegmenter: H264TsSegmenter? = null
  private val tsIndexGen =
    AtomicLong(1) //  ads 1,2,3   normal 4...
  var h264TsSegs: MutableList<TsSegment> = ArrayList()
  var segmentCount: Int = 0

  // m3u8
  private var m3u8: M3U8? = null
  private val m3u8Builder = M3u8Builder(H264TsSegmenter.TS_DURATION)
  var amazonUploader: AmazonUploader? = null
  var m3u8Uploader: AmazonUploader? = null

  constructor(
    surfaceView: SurfaceView?,
    connectChecker: ConnectCheckerRtmp?,
    context: Context?
  ) : super(surfaceView) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker, context)
    this.context = context
    h264TsSegmenter = H264TsSegmenter()
    h264TsSegmenter!!.initialize(44100f, 16, 1, 10)
    amazonUploader = AmazonUploader(context!!, this)
    m3u8Uploader = AmazonUploader(context!!, this)
//    scheduleM3U8(context)
  }

  constructor(
    textureView: TextureView?,
    connectChecker: ConnectCheckerRtmp?,
    context: Context?
  ) : super(textureView) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker, context)
    this.context = context
    h264TsSegmenter = H264TsSegmenter()
    h264TsSegmenter!!.initialize(44100f, 16, 1, 10)
  }

  @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
  constructor(
    openGlView: OpenGlView?,
    connectChecker: ConnectCheckerRtmp?
  ) : super(openGlView) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker)
  }

  @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
  constructor(
    lightOpenGlView: LightOpenGlView?,
    connectChecker: ConnectCheckerRtmp?
  ) : super(lightOpenGlView) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker)
  }

  @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
  constructor(
    context: Context?,
    connectChecker: ConnectCheckerRtmp?
  ) : super(context) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker)
  }

  /**
   * H264 profile.
   *
   * @param profileIop Could be ProfileIop.BASELINE or ProfileIop.CONSTRAINED
   */
  fun setProfileIop(profileIop: Byte) {
    srsFlvMuxer.setProfileIop(profileIop)
  }

  @Throws(
      RuntimeException::class
  ) override fun resizeCache(newSize: Int) {
    srsFlvMuxer.resizeFlvTagCache(newSize)
  }

  override fun getCacheSize(): Int {
    return srsFlvMuxer.flvTagCacheSize
  }

  override fun getSentAudioFrames(): Long {
    return srsFlvMuxer.sentAudioFrames
  }

  override fun getSentVideoFrames(): Long {
    return srsFlvMuxer.sentVideoFrames
  }

  override fun getDroppedAudioFrames(): Long {
    return srsFlvMuxer.droppedAudioFrames
  }

  override fun getDroppedVideoFrames(): Long {
    return srsFlvMuxer.droppedVideoFrames
  }

  override fun resetSentAudioFrames() {
    srsFlvMuxer.resetSentAudioFrames()
  }

  override fun resetSentVideoFrames() {
    srsFlvMuxer.resetSentVideoFrames()
  }

  override fun resetDroppedAudioFrames() {
    srsFlvMuxer.resetDroppedAudioFrames()
  }

  override fun resetDroppedVideoFrames() {
    srsFlvMuxer.resetDroppedVideoFrames()
  }

  override fun setAuthorization(
    user: String,
    password: String
  ) {
    srsFlvMuxer.setAuthorization(user, password)
  }

  /**
   * Some Livestream hosts use Akamai auth that requires RTMP packets to be sent with increasing timestamp order regardless of packet type.
   * Necessary with Servers like Dacast.
   * More info here:
   * https://learn.akamai.com/en-us/webhelp/media-services-live/media-services-live-encoder-compatibility-testing-and-qualification-guide-v4.0/GUID-F941C88B-9128-4BF4-A81B-C2E5CFD35BBF.html
   */
  fun forceAkamaiTs(enabled: Boolean) {
    srsFlvMuxer.forceAkamaiTs(enabled)
  }

  override fun prepareAudioRtp(
    isStereo: Boolean,
    sampleRate: Int
  ) {
    //srsFlvMuxer.setIsStereo(isStereo);
    //srsFlvMuxer.setSampleRate(sampleRate);
  }

  override fun startStreamRtp(url: String) {
    //if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
    //  srsFlvMuxer.setVideoResolution(videoEncoder.getHeight(), videoEncoder.getWidth());
    //} else {
    //  srsFlvMuxer.setVideoResolution(videoEncoder.getWidth(), videoEncoder.getHeight());
    //}
    //srsFlvMuxer.start(url);
  }

  override fun stopStreamRtp() {
    srsFlvMuxer.stop()
  }

  override fun setReTries(reTries: Int) {
    srsFlvMuxer.setReTries(reTries)
  }

  override fun shouldRetry(reason: String): Boolean {
    return srsFlvMuxer.shouldRetry(reason)
  }

  public override fun reConnect(delay: Long) {
    srsFlvMuxer.reConnect(delay)
  }

  override fun getAacDataRtp(
    aacBuffer: ByteBuffer,
    info: BufferInfo
  ) {
    srsFlvMuxer.sendAudio(aacBuffer, info)
  }

  override fun onSpsPpsVpsRtp(
    sps: ByteBuffer?,
    pps: ByteBuffer?,
    vps: ByteBuffer?
  ) {
    srsFlvMuxer.setSpsPPs(sps, pps)
  }

  override fun getH264DataRtp(
    h264Buffer: ByteBuffer,
    info: BufferInfo
  ) {

    //final Context mContext = this.context;
    val arr = ByteArray(h264Buffer.remaining())
    h264Buffer[arr]
    val tsSegment = h264TsSegmenter!!.getTsBuf(0x04.toByte(), arr, null)
    if (tsSegment != null) {
      val index = tsIndexGen.getAndIncrement()
      h264TsSegs.add(
          TsSegment(
              "$index.ts", tsSegment,
              h264TsSegmenter!!.tsSegTime, true
          )
      )
      println("xxx buf" + index + ".ts" + System.currentTimeMillis())
      writeTsFile("$index.ts", tsSegment, context, true)

      //AsyncTask.execute(new Runnable() {
      //  @Override
      //  public void run() {
      //
      //
      //    byte[] tsSegment = h264TsSegmenter.getTsBuf((byte) 0x04, arr, null );
      //
      //    if(tsSegment != null){
      //      long index = tsIndexGen.getAndIncrement();
      //
      //
      //      h264TsSegs.add(new TsSegment(index + ".ts", tsSegment,
      //          h264TsSegmenter.getTsSegTime(), true));
      //
      //      System.out.println("xxx buf"+index+".ts");
      //
      //    writeTsFile(index + ".ts", tsSegment, mContext, true);
      //
      //      return;
      //    }
      //
      //  }
      //});

      //tsSegmenter.haveIDRFrame(arr);

      //srsFlvMuxer.sendVideo(h264Buffer, info);
    }
  }

  fun scheduleM3U8(mContext: Context?) {
    Observable
        .interval(1, SECONDS)
        .takeWhile { it -> it == 480L }
        .subscribe(
            { }, { }) {
          var m3u8Seq = if (m3u8 == null) 0 else m3u8!!.seq
          m3u8Seq++
          println("Amazon seq: $m3u8Seq")
          m3u8 = m3u8Builder.generateM3u8(1241, h264TsSegs)
          println("response m3u8, {}" + m3u8.toString())
          m3u8?.let {
            writeTsFile("thai.m3u8", it.buf, mContext, false)
            scheduleM3U8(mContext)
          }
        }
  }

  private fun writeTsFile(
    pathname: String,
    buf: ByteArray,
    context: Context?,
    isTS: Boolean
  ) {
    var fos: FileOutputStream? = null
    val storageDir =
      ContextWrapper(context).getDir("hlsDir", Context.MODE_PRIVATE)
    val file = File(storageDir, pathname)
    try {
      fos = FileOutputStream(file)
      fos.write(buf)
      fos.flush()
    } catch (e: IOException) {
      e.printStackTrace()
    } finally {
      try {
        if (fos != null) {
          fos.close()
          var hlsFile: HLSFile? = null
          if (isTS) {
            runBlocking {
              hlsFile = HLSFile(file, TS)
              amazonUploader!!.uploadPhoto(hlsFile!!, pathname)
            }
          } else {
            runBlocking {
              segmentCount++
              hlsFile = HLSFile(file, FileType.M3U8)
              m3u8Uploader!!.uploadPhoto(hlsFile!!, pathname)
            }
          }
          println("aaa ts write" + pathname + " : " + System.currentTimeMillis())
        }
      } catch (ex: Exception) {
      }
    }
  }

  override fun updateImage(file: HLSFile?) {
    println("Ama success"+ System.currentTimeMillis())

    if(file != null && file.fileType == TS) {
      println("Ama TS"+ System.currentTimeMillis())


      var m3u8Seq = if (m3u8 == null) 0 else m3u8!!.seq
      m3u8Seq++
      println("Amazon seq: $m3u8Seq")
      m3u8 = m3u8Builder.generateM3u8(m3u8Seq, h264TsSegs)
      println("response m3u8, {}" + m3u8.toString())
      m3u8?.let {
        writeTsFile("thai.m3u8", it.buf, this.context, false)
     }

    }


    if (file != null) {
      //System.out.println(
      //    "aaa ts s3 done" + file.getData().getName() + " : " + System.currentTimeMillis());
      //
      //final Context mContext = this.context;
      //
      //AsyncTask.execute(new Runnable() {
      //  @Override
      //  public void run() {
      //    if (file.getFileType() == FileType.TS) {
      //      long m3u8Seq = m3u8 == null ? 0 : m3u8.getSeq();
      //      m3u8 = m3u8Builder.generateM3u8(m3u8Seq, h264TsSegs);
      //      System.out.println("response m3u8, {}" + m3u8.toString());
      //      writeTsFile("playlist.m3u8", m3u8.getBuf(), mContext, false);
      //    }
      //  }
      //});
    }
  }

  override fun startUpLoad() {}
  override fun upLoadFail() {}
  override fun percentUpLoad(percent: Long) {}

  companion object {
    // md5 ->> segments
    private val md5Segments: Map<String, List<TsSegment>> =
      ConcurrentHashMap()
  }
}