//package com.pedro.rtpstreamer.openglexample
//
//import android.graphics.BitmapFactory
//import android.graphics.Color
//import android.graphics.SurfaceTexture
//import android.media.MediaPlayer
//import android.os.Build.VERSION_CODES
//import android.os.Bundle
//import android.os.Environment
//import android.view.Menu
//import android.view.MenuItem
//import android.view.MotionEvent
//import android.view.Surface
//import android.view.SurfaceHolder
//import android.view.SurfaceHolder.Callback
//import android.view.View
//import android.view.View.OnClickListener
//import android.view.View.OnTouchListener
//import android.view.WindowManager.LayoutParams
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.annotation.RequiresApi
//import androidx.appcompat.app.AppCompatActivity
//import com.pedro.encoder.input.gl.SpriteGestureController
//import com.pedro.encoder.input.gl.render.filters.AnalogTVFilterRender
//import com.pedro.encoder.input.gl.render.filters.AndroidViewFilterRender
//import com.pedro.encoder.input.gl.render.filters.BasicDeformationFilterRender
//import com.pedro.encoder.input.gl.render.filters.BeautyFilterRender
//import com.pedro.encoder.input.gl.render.filters.BlackFilterRender
//import com.pedro.encoder.input.gl.render.filters.BlurFilterRender
//import com.pedro.encoder.input.gl.render.filters.BrightnessFilterRender
//import com.pedro.encoder.input.gl.render.filters.CartoonFilterRender
//import com.pedro.encoder.input.gl.render.filters.CircleFilterRender
//import com.pedro.encoder.input.gl.render.filters.ColorFilterRender
//import com.pedro.encoder.input.gl.render.filters.ContrastFilterRender
//import com.pedro.encoder.input.gl.render.filters.DuotoneFilterRender
//import com.pedro.encoder.input.gl.render.filters.EarlyBirdFilterRender
//import com.pedro.encoder.input.gl.render.filters.EdgeDetectionFilterRender
//import com.pedro.encoder.input.gl.render.filters.ExposureFilterRender
//import com.pedro.encoder.input.gl.render.filters.FireFilterRender
//import com.pedro.encoder.input.gl.render.filters.GammaFilterRender
//import com.pedro.encoder.input.gl.render.filters.GlitchFilterRender
//import com.pedro.encoder.input.gl.render.filters.GreyScaleFilterRender
//import com.pedro.encoder.input.gl.render.filters.HalftoneLinesFilterRender
//import com.pedro.encoder.input.gl.render.filters.Image70sFilterRender
//import com.pedro.encoder.input.gl.render.filters.LamoishFilterRender
//import com.pedro.encoder.input.gl.render.filters.MoneyFilterRender
//import com.pedro.encoder.input.gl.render.filters.NegativeFilterRender
//import com.pedro.encoder.input.gl.render.filters.NoFilterRender
//import com.pedro.encoder.input.gl.render.filters.PixelatedFilterRender
//import com.pedro.encoder.input.gl.render.filters.PolygonizationFilterRender
//import com.pedro.encoder.input.gl.render.filters.RGBSaturationFilterRender
//import com.pedro.encoder.input.gl.render.filters.RainbowFilterRender
//import com.pedro.encoder.input.gl.render.filters.RippleFilterRender
//import com.pedro.encoder.input.gl.render.filters.RotationFilterRender
//import com.pedro.encoder.input.gl.render.filters.SaturationFilterRender
//import com.pedro.encoder.input.gl.render.filters.SepiaFilterRender
//import com.pedro.encoder.input.gl.render.filters.SharpnessFilterRender
//import com.pedro.encoder.input.gl.render.filters.SnowFilterRender
//import com.pedro.encoder.input.gl.render.filters.SwirlFilterRender
//import com.pedro.encoder.input.gl.render.filters.TemperatureFilterRender
//import com.pedro.encoder.input.gl.render.filters.ZebraFilterRender
//import com.pedro.encoder.input.gl.render.filters.`object`.GifObjectFilterRender
//import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
//import com.pedro.encoder.input.gl.render.filters.`object`.SurfaceFilterRender
//import com.pedro.encoder.input.gl.render.filters.`object`.SurfaceFilterRender.SurfaceReadyCallback
//import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
//import com.pedro.encoder.input.video.CameraOpenException
//import com.pedro.encoder.utils.gl.TranslateTo.BOTTOM
//import com.pedro.encoder.utils.gl.TranslateTo.CENTER
//import com.pedro.encoder.utils.gl.TranslateTo.RIGHT
//import com.pedro.rtplibrary.rtmp.RtmpCamera1
//import com.pedro.rtplibrary.view.OpenGlView
//import com.pedro.rtpstreamer.R
//import com.pedro.rtpstreamer.R.id
//import com.pedro.rtpstreamer.R.layout
//import com.pedro.rtpstreamer.R.mipmap
//import com.pedro.rtpstreamer.R.raw
//import com.pedro.rtpstreamer.R.string
//import net.ossrs.rtmp.ConnectCheckerRtmp
//import java.io.File
//import java.io.IOException
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
///**
// * More documentation see:
// * [com.pedro.rtplibrary.base.Camera1Base]
// * [com.pedro.rtplibrary.rtmp.RtmpCamera1]
// */
//@RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
//class OpenGlRtmpActivity : AppCompatActivity(),
//    ConnectCheckerRtmp,
//    OnClickListener,
//    Callback,
//    OnTouchListener {
//  private var rtmpCamera1: RtmpCamera1? = null
//  private var button: Button? = null
//  private var bRecord: Button? = null
//  private var etUrl: EditText? = null
//  private var currentDateAndTime = ""
//  private val folder = File(
//      Environment.getExternalStorageDirectory()
//          .absolutePath
//          + "/rtmp-rtsp-stream-client-java"
//  )
//  private var openGlView: OpenGlView? = null
//  private val spriteGestureController = SpriteGestureController()
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
//    setContentView(layout.activity_open_gl)
//    openGlView = findViewById(id.surfaceView)
//    button = findViewById(id.b_start_stop)
//    button?.setOnClickListener(this)
//    bRecord = findViewById(id.b_record)
//    bRecord?.setOnClickListener(this)
//    val switchCamera =
//      findViewById<Button>(id.switch_camera)
//    switchCamera.setOnClickListener(this)
//    etUrl = findViewById(id.et_rtp_url)
//    etUrl?.setHint(string.hint_rtmp)
//    rtmpCamera1 = RtmpCamera1(openGlView, this, this)
//    openGlView?.getHolder()
//        ?.addCallback(this)
//    openGlView?.setOnTouchListener(this)
//  }
//
//  override fun onCreateOptionsMenu(menu: Menu): Boolean {
//    menuInflater.inflate(R.menu.gl_menu, menu)
//    return true
//  }
//
//  override fun onOptionsItemSelected(item: MenuItem): Boolean {
//    //Stop listener for image, text and gif stream objects.
//    spriteGestureController.stopListener()
//    return when (item.itemId) {
//      id.e_d_fxaa -> {
//        rtmpCamera1!!.glInterface
//            .enableAA(
//                !rtmpCamera1!!.glInterface
//                    .isAAEnabled
//            )
//        Toast.makeText(
//            this,
//            "FXAA " + if (rtmpCamera1!!.glInterface
//                    .isAAEnabled
//            ) "enabled" else "disabled",
//            Toast.LENGTH_SHORT
//        )
//            .show()
//        true
//      }
//      id.no_filter -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(NoFilterRender())
//        true
//      }
//      id.analog_tv -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(AnalogTVFilterRender())
//        true
//      }
//      id.android_view -> {
//        val androidViewFilterRender = AndroidViewFilterRender()
//        androidViewFilterRender.view = findViewById(id.switch_camera)
//        rtmpCamera1!!.glInterface
//            .setFilter(androidViewFilterRender)
//        true
//      }
//      id.basic_deformation -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(BasicDeformationFilterRender())
//        true
//      }
//      id.beauty -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(BeautyFilterRender())
//        true
//      }
//      id.black -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(BlackFilterRender())
//        true
//      }
//      id.blur -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(BlurFilterRender())
//        true
//      }
//      id.brightness -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(BrightnessFilterRender())
//        true
//      }
//      id.cartoon -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(CartoonFilterRender())
//        true
//      }
//      id.circle -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(CircleFilterRender())
//        true
//      }
//      id.color -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(ColorFilterRender())
//        true
//      }
//      id.contrast -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(ContrastFilterRender())
//        true
//      }
//      id.duotone -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(DuotoneFilterRender())
//        true
//      }
//      id.early_bird -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(EarlyBirdFilterRender())
//        true
//      }
//      id.edge_detection -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(EdgeDetectionFilterRender())
//        true
//      }
//      id.exposure -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(ExposureFilterRender())
//        true
//      }
//      id.fire -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(FireFilterRender())
//        true
//      }
//      id.gamma -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(GammaFilterRender())
//        true
//      }
//      id.glitch -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(GlitchFilterRender())
//        true
//      }
//      id.gif -> {
//        setGifToStream()
//        true
//      }
//      id.grey_scale -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(GreyScaleFilterRender())
//        true
//      }
//      id.halftone_lines -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(HalftoneLinesFilterRender())
//        true
//      }
//      id.image -> {
//        setImageToStream()
//        true
//      }
//      id.image_70s -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(Image70sFilterRender())
//        true
//      }
//      id.lamoish -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(LamoishFilterRender())
//        true
//      }
//      id.money -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(MoneyFilterRender())
//        true
//      }
//      id.negative -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(NegativeFilterRender())
//        true
//      }
//      id.pixelated -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(PixelatedFilterRender())
//        true
//      }
//      id.polygonization -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(PolygonizationFilterRender())
//        true
//      }
//      id.rainbow -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(RainbowFilterRender())
//        true
//      }
//      id.rgb_saturate -> {
//        val rgbSaturationFilterRender = RGBSaturationFilterRender()
//        rtmpCamera1!!.glInterface
//            .setFilter(rgbSaturationFilterRender)
//        //Reduce green and blue colors 20%. Red will predominate.
//        rgbSaturationFilterRender.setRGBSaturation(1f, 0.8f, 0.8f)
//        true
//      }
//      id.ripple -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(RippleFilterRender())
//        true
//      }
//      id.rotation -> {
//        val rotationFilterRender = RotationFilterRender()
//        rtmpCamera1!!.glInterface
//            .setFilter(rotationFilterRender)
//        rotationFilterRender.rotation = 90
//        true
//      }
//      id.saturation -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(SaturationFilterRender())
//        true
//      }
//      id.sepia -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(SepiaFilterRender())
//        true
//      }
//      id.sharpness -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(SharpnessFilterRender())
//        true
//      }
//      id.snow -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(SnowFilterRender())
//        true
//      }
//      id.swirl -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(SwirlFilterRender())
//        true
//      }
//      id.surface_filter -> {
//        val surfaceFilterRender =
//          SurfaceFilterRender(object : SurfaceReadyCallback {
//            override fun surfaceReady(surfaceTexture: SurfaceTexture) {
//              //You can render this filter with other api that draw in a surface. for example you can use VLC
//              val mediaPlayer =
//                MediaPlayer.create(this@OpenGlRtmpActivity, raw.big_bunny_240p)
//              mediaPlayer.setSurface(Surface(surfaceTexture))
//              mediaPlayer.start()
//            }
//          })
//        rtmpCamera1!!.glInterface
//            .setFilter(surfaceFilterRender)
//        //Video is 360x240 so select a percent to keep aspect ratio (50% x 33.3% screen)
//        surfaceFilterRender.setScale(50f, 33.3f)
//        spriteGestureController.setBaseObjectFilterRender(surfaceFilterRender) //Optional
//        true
//      }
//      id.temperature -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(TemperatureFilterRender())
//        true
//      }
//      id.text -> {
//        setTextToStream()
//        true
//      }
//      id.zebra -> {
//        rtmpCamera1!!.glInterface
//            .setFilter(ZebraFilterRender())
//        true
//      }
//      else -> false
//    }
//  }
//
//  private fun setTextToStream() {
//    val textObjectFilterRender = TextObjectFilterRender()
//    rtmpCamera1!!.glInterface
//        .setFilter(textObjectFilterRender)
//    textObjectFilterRender.setText("Hello world", 22f, Color.RED)
//    textObjectFilterRender.setDefaultScale(
//        rtmpCamera1!!.streamWidth,
//        rtmpCamera1!!.streamHeight
//    )
//    textObjectFilterRender.setPosition(CENTER)
//    spriteGestureController.setBaseObjectFilterRender(textObjectFilterRender) //Optional
//  }
//
//  private fun setImageToStream() {
//    val imageObjectFilterRender = ImageObjectFilterRender()
//    rtmpCamera1!!.glInterface
//        .setFilter(imageObjectFilterRender)
//    imageObjectFilterRender.setImage(
//        BitmapFactory.decodeResource(resources, mipmap.ic_launcher)
//    )
//    imageObjectFilterRender.setDefaultScale(
//        rtmpCamera1!!.streamWidth,
//        rtmpCamera1!!.streamHeight
//    )
//    imageObjectFilterRender.setPosition(RIGHT)
//    spriteGestureController.setBaseObjectFilterRender(imageObjectFilterRender) //Optional
//    spriteGestureController.setPreventMoveOutside(false) //Optional
//  }
//
//  private fun setGifToStream() {
//    try {
//      val gifObjectFilterRender = GifObjectFilterRender()
//      gifObjectFilterRender.setGif(resources.openRawResource(raw.banana))
//      rtmpCamera1!!.glInterface
//          .setFilter(gifObjectFilterRender)
//      gifObjectFilterRender.setDefaultScale(
//          rtmpCamera1!!.streamWidth,
//          rtmpCamera1!!.streamHeight
//      )
//      gifObjectFilterRender.setPosition(BOTTOM)
//      spriteGestureController.setBaseObjectFilterRender(gifObjectFilterRender) //Optional
//    } catch (e: IOException) {
//      Toast.makeText(this, e.message, Toast.LENGTH_SHORT)
//          .show()
//    }
//  }
//
//  override fun onConnectionSuccessRtmp() {
//    runOnUiThread {
//      Toast.makeText(this@OpenGlRtmpActivity, "Connection success", Toast.LENGTH_SHORT)
//          .show()
//    }
//  }
//
//  override fun onConnectionFailedRtmp(reason: String) {
//    runOnUiThread {
//      Toast.makeText(this@OpenGlRtmpActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
//          .show()
//      rtmpCamera1!!.stopStream()
//      button!!.setText(string.start_button)
//    }
//  }
//
//  override fun onNewBitrateRtmp(bitrate: Long) {}
//  override fun onDisconnectRtmp() {
//    runOnUiThread {
//      Toast.makeText(this@OpenGlRtmpActivity, "Disconnected", Toast.LENGTH_SHORT)
//          .show()
//    }
//  }
//
//  override fun onAuthErrorRtmp() {
//    runOnUiThread {
//      Toast.makeText(this@OpenGlRtmpActivity, "Auth error", Toast.LENGTH_SHORT)
//          .show()
//    }
//  }
//
//  override fun onAuthSuccessRtmp() {
//    runOnUiThread {
//      Toast.makeText(this@OpenGlRtmpActivity, "Auth success", Toast.LENGTH_SHORT)
//          .show()
//    }
//  }
//
//  override fun onClick(view: View) {
//    when (view.id) {
//      id.b_start_stop -> if (!rtmpCamera1!!.isStreaming) {
//        if (rtmpCamera1!!.isRecording
//            || rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()
//        ) {
//          button!!.setText(string.stop_button)
//          rtmpCamera1!!.startStream(
//              etUrl!!.text
//                  .toString()
//          )
//        } else {
//          Toast.makeText(
//              this, "Error preparing stream, This device cant do it",
//              Toast.LENGTH_SHORT
//          )
//              .show()
//        }
//      } else {
//        button!!.setText(string.start_button)
//        rtmpCamera1!!.stopStream()
//      }
//      id.switch_camera -> try {
//        rtmpCamera1!!.switchCamera()
//      } catch (e: CameraOpenException) {
//        Toast.makeText(this, e.message, Toast.LENGTH_SHORT)
//            .show()
//      }
//      id.b_record -> if (!rtmpCamera1!!.isRecording) {
//        try {
//          if (!folder.exists()) {
//            folder.mkdir()
//          }
//          val sdf =
//            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//          currentDateAndTime = sdf.format(Date())
//          if (!rtmpCamera1!!.isStreaming) {
//            if (rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()) {
//              rtmpCamera1!!.startRecord(
//                  folder.absolutePath + "/" + currentDateAndTime + ".mp4"
//              )
//              bRecord!!.setText(string.stop_record)
//              Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT)
//                  .show()
//            } else {
//              Toast.makeText(
//                  this, "Error preparing stream, This device cant do it",
//                  Toast.LENGTH_SHORT
//              )
//                  .show()
//            }
//          } else {
//            rtmpCamera1!!.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
//            bRecord!!.setText(string.stop_record)
//            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT)
//                .show()
//          }
//        } catch (e: IOException) {
//          rtmpCamera1!!.stopRecord()
//          bRecord!!.setText(string.start_record)
//          Toast.makeText(this, e.message, Toast.LENGTH_SHORT)
//              .show()
//        }
//      } else {
//        rtmpCamera1!!.stopRecord()
//        bRecord!!.setText(string.start_record)
//        Toast.makeText(
//            this,
//            "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
//            Toast.LENGTH_SHORT
//        )
//            .show()
//        currentDateAndTime = ""
//      }
//      else -> {
//      }
//    }
//  }
//
//  override fun surfaceCreated(surfaceHolder: SurfaceHolder) {}
//  override fun surfaceChanged(
//    surfaceHolder: SurfaceHolder,
//    i: Int,
//    i1: Int,
//    i2: Int
//  ) {
//    rtmpCamera1!!.startPreview()
//  }
//
//  override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
//    if (rtmpCamera1!!.isRecording) {
//      rtmpCamera1!!.stopRecord()
//      bRecord!!.setText(string.start_record)
//      Toast.makeText(
//          this,
//          "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
//          Toast.LENGTH_SHORT
//      )
//          .show()
//      currentDateAndTime = ""
//    }
//    if (rtmpCamera1!!.isStreaming) {
//      rtmpCamera1!!.stopStream()
//      button!!.text = resources.getString(string.start_button)
//    }
//    rtmpCamera1!!.stopPreview()
//  }
//
//  override fun onTouch(
//    view: View,
//    motionEvent: MotionEvent
//  ): Boolean {
//    if (spriteGestureController.spriteTouched(view, motionEvent)) {
//      spriteGestureController.moveSprite(view, motionEvent)
//      spriteGestureController.scaleSprite(motionEvent)
//      return true
//    }
//    return false
//  }
//}