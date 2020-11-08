package com.pedro.rtpstreamer.customexample

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.WindowManager.LayoutParams
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import com.pedro.rtpstreamer.R
import com.pedro.rtpstreamer.R.drawable
import com.pedro.rtpstreamer.R.id
import com.pedro.rtpstreamer.R.layout
import com.pedro.rtpstreamer.R.menu
import com.pedro.rtpstreamer.R.string
import net.ossrs.rtmp.ConnectCheckerRtmp
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

/**
 * More documentation see:
 * [com.pedro.rtplibrary.base.Camera1Base]
 * [com.pedro.rtplibrary.rtmp.RtmpCamera1]
 */
class RtmpActivity : AppCompatActivity(),
    OnClickListener,
    ConnectCheckerRtmp,
    Callback,
    OnTouchListener {
  private val orientations = arrayOf(0, 90, 180, 270)
  private var rtmpCamera1: RtmpCamera1? = null
  private var bStartStop: Button? = null
  private var bRecord: Button? = null
  private var etUrl: EditText? = null
  private var currentDateAndTime = ""
  private val folder = File(
      Environment.getExternalStorageDirectory()
          .absolutePath
          + "/rtmp-rtsp-stream-client-java"
  )

  //options menu
  private var drawerLayout: DrawerLayout? = null
  private var navigationView: NavigationView? = null
  private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
  private var rgChannel: RadioGroup? = null
  private var spResolution: Spinner? = null
  private var cbEchoCanceler: CheckBox? = null
  private var cbNoiseSuppressor: CheckBox? = null
  private var etVideoBitrate: EditText? = null
  private var etFps: EditText? = null
  private var etAudioBitrate: EditText? = null
  private var etSampleRate: EditText? = null
  private var etWowzaUser: EditText? = null
  private var etWowzaPassword: EditText? = null
  private var lastVideoBitrate: String? = null
  private var tvBitrate: TextView? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(layout.activity_custom)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    supportActionBar!!.setHomeButtonEnabled(true)
    val surfaceView = findViewById<SurfaceView>(id.surfaceView)
    surfaceView.holder
        .addCallback(this)
    surfaceView.setOnTouchListener(this)
    rtmpCamera1 = RtmpCamera1(surfaceView, this, this)
    prepareOptionsMenuViews()
    tvBitrate = findViewById(id.tv_bitrate)
    etUrl = findViewById(id.et_rtp_url)
    etUrl?.setHint(string.hint_rtmp)
    bStartStop = findViewById(id.b_start_stop)
    bStartStop?.setOnClickListener(this)
    bRecord = findViewById(id.b_record)
    bRecord?.setOnClickListener(this)
    val switchCamera =
      findViewById<Button>(id.switch_camera)
    switchCamera.setOnClickListener(this)
  }

  private fun prepareOptionsMenuViews() {
    drawerLayout = findViewById(id.activity_custom)
    navigationView = findViewById(id.nv_rtp)
    navigationView?.inflateMenu(menu.options_rtmp)
    actionBarDrawerToggle = object : ActionBarDrawerToggle(
        this, drawerLayout, string.rtmp_streamer,
        string.rtmp_streamer
    ) {
      override fun onDrawerOpened(drawerView: View) {
        actionBarDrawerToggle!!.syncState()
        lastVideoBitrate = etVideoBitrate!!.text
            .toString()
      }

      override fun onDrawerClosed(view: View) {
        actionBarDrawerToggle!!.syncState()
        if (lastVideoBitrate != null && lastVideoBitrate != etVideoBitrate!!.text
                .toString() && rtmpCamera1!!.isStreaming
        ) {
          if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            val bitrate = etVideoBitrate!!.text
                .toString()
                .toInt() * 1024
            rtmpCamera1!!.setVideoBitrateOnFly(bitrate)
            Toast.makeText(this@RtmpActivity, "New bitrate: $bitrate", Toast.LENGTH_SHORT)
                .show()
          } else {
            Toast.makeText(
                this@RtmpActivity, "Bitrate on fly ignored, Required min API 19",
                Toast.LENGTH_SHORT
            )
                .show()
          }
        }
      }
    }
    drawerLayout?.addDrawerListener(actionBarDrawerToggle!!)
    //checkboxs
    cbEchoCanceler = navigationView?.getMenu()
        ?.findItem(id.cb_echo_canceler)
        ?.actionView as CheckBox
    cbNoiseSuppressor = navigationView?.getMenu()
        ?.findItem(id.cb_noise_suppressor)
        ?.actionView as CheckBox
    //radiobuttons
    val rbTcp = navigationView?.getMenu()
        ?.findItem(id.rb_tcp)
        ?.actionView as RadioButton
    rgChannel = navigationView!!.getMenu()
        .findItem(id.channel)
        .actionView as RadioGroup
    rbTcp.isChecked = true
    //spinners
    spResolution = navigationView!!.getMenu()
        .findItem(id.sp_resolution)
        .actionView as Spinner
    val orientationAdapter =
      ArrayAdapter<Int>(this, layout.support_simple_spinner_dropdown_item)
    orientationAdapter.addAll(*orientations)
    val resolutionAdapter =
      ArrayAdapter<String>(this, layout.support_simple_spinner_dropdown_item)
    val list: MutableList<String> = ArrayList()
    for (size in rtmpCamera1!!.resolutionsBack) {
      list.add(size.width.toString() + "X" + size.height)
    }
    resolutionAdapter.addAll(list)
    spResolution!!.adapter = resolutionAdapter
    //edittexts
    etVideoBitrate = navigationView?.getMenu()
        ?.findItem(id.et_video_bitrate)
        ?.actionView as EditText
    etFps = navigationView!!.getMenu()
        .findItem(id.et_fps)
        .actionView as EditText
    etAudioBitrate = navigationView!!.getMenu()
        .findItem(id.et_audio_bitrate)
        .actionView as EditText
    etSampleRate = navigationView!!.getMenu()
        .findItem(id.et_samplerate)
        .actionView as EditText
    etVideoBitrate!!.setText("2500")
    etFps!!.setText("30")
    etAudioBitrate!!.setText("128")
    etSampleRate!!.setText("44100")
    etWowzaUser = navigationView!!.getMenu()
        .findItem(id.et_user)
        .actionView as EditText
    etWowzaPassword = navigationView!!.getMenu()
        .findItem(id.et_password)
        .actionView as EditText
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    actionBarDrawerToggle!!.syncState()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        if (!drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
          drawerLayout!!.openDrawer(GravityCompat.START)
        } else {
          drawerLayout!!.closeDrawer(GravityCompat.START)
        }
        true
      }
      id.microphone -> {
        if (!rtmpCamera1!!.isAudioMuted) {
          item.icon = resources.getDrawable(drawable.icon_microphone_off)
          rtmpCamera1!!.disableAudio()
        } else {
          item.icon = resources.getDrawable(drawable.icon_microphone)
          rtmpCamera1!!.enableAudio()
        }
        true
      }
      else -> false
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      id.b_start_stop -> {
        Log.d("TAG_R", "b_start_stop: ")
        rtmpCamera1!!.prepareVideo()
        prepareEncoders()
        rtmpCamera1!!.startStream("")
      }
      id.b_record -> {
        Log.d("TAG_R", "b_start_stop: ")
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
          if (!rtmpCamera1!!.isRecording) {
            try {
              if (!folder.exists()) {
                folder.mkdir()
              }
              val sdf =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
              currentDateAndTime = sdf.format(Date())
              if (!rtmpCamera1!!.isStreaming) {
                if (prepareEncoders()) {
                  rtmpCamera1!!.startRecord(
                      folder.absolutePath + "/" + currentDateAndTime + ".mp4"
                  )
                  bRecord!!.setText(string.stop_record)
                  Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT)
                      .show()
                } else {
                  Toast.makeText(
                      this, "Error preparing stream, This device cant do it",
                      Toast.LENGTH_SHORT
                  )
                      .show()
                }
              } else {
                rtmpCamera1!!.startRecord(
                    folder.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                bRecord!!.setText(string.stop_record)
                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT)
                    .show()
              }
            } catch (e: IOException) {
              rtmpCamera1!!.stopRecord()
              bRecord!!.setText(string.start_record)
              Toast.makeText(this, e.message, Toast.LENGTH_SHORT)
                  .show()
            }
          } else {
            rtmpCamera1!!.stopRecord()
            bRecord!!.setText(string.start_record)
            Toast.makeText(
                this,
                "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
                Toast.LENGTH_SHORT
            )
                .show()
            currentDateAndTime = ""
          }
        } else {
          Toast.makeText(
              this, "You need min JELLY_BEAN_MR2(API 18) for do it...",
              Toast.LENGTH_SHORT
          )
              .show()
        }
      }
      id.switch_camera -> try {
        rtmpCamera1!!.switchCamera()
      } catch (e: CameraOpenException) {
        Toast.makeText(this@RtmpActivity, e.message, Toast.LENGTH_SHORT)
            .show()
      }
      else -> {
      }
    }
  }

  private fun prepareEncoders(): Boolean {
    val resolution = rtmpCamera1!!.resolutionsBack[spResolution!!.selectedItemPosition]
    val width = resolution.width
    val height = resolution.height
    return rtmpCamera1!!.prepareVideo(
        width, height, etFps!!.text
        .toString()
        .toInt(),
        etVideoBitrate!!.text
            .toString()
            .toInt() * 1024,
        CameraHelper.getCameraOrientation(this)
    ) && rtmpCamera1!!.prepareAudio(
        etAudioBitrate!!.text
            .toString()
            .toInt() * 1024, etSampleRate!!.text
        .toString()
        .toInt(),
        rgChannel!!.checkedRadioButtonId == id.rb_stereo, cbEchoCanceler!!.isChecked,
        cbNoiseSuppressor!!.isChecked
    )
  }

  override fun onConnectionSuccessRtmp() {
    runOnUiThread {
      Toast.makeText(this@RtmpActivity, "Connection success", Toast.LENGTH_SHORT)
          .show()
    }
  }

  override fun onConnectionFailedRtmp(reason: String) {
    runOnUiThread {
      Toast.makeText(this@RtmpActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
          .show()
      rtmpCamera1!!.stopStream()
      bStartStop!!.text = resources.getString(string.start_button)
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2
          && rtmpCamera1!!.isRecording
      ) {
        rtmpCamera1!!.stopRecord()
        bRecord!!.setText(string.start_record)
        Toast.makeText(
            this@RtmpActivity,
            "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
            Toast.LENGTH_SHORT
        )
            .show()
        currentDateAndTime = ""
      }
    }
  }

  override fun onNewBitrateRtmp(bitrate: Long) {
    runOnUiThread { tvBitrate!!.text = "$bitrate bps" }
  }

  override fun onDisconnectRtmp() {
    runOnUiThread {
      Toast.makeText(this@RtmpActivity, "Disconnected", Toast.LENGTH_SHORT)
          .show()
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2
          && rtmpCamera1!!.isRecording
      ) {
        rtmpCamera1!!.stopRecord()
        bRecord!!.setText(string.start_record)
        Toast.makeText(
            this@RtmpActivity,
            "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
            Toast.LENGTH_SHORT
        )
            .show()
        currentDateAndTime = ""
      }
    }
  }

  override fun onAuthErrorRtmp() {
    runOnUiThread {
      Toast.makeText(this@RtmpActivity, "Auth error", Toast.LENGTH_SHORT)
          .show()
    }
  }

  override fun onAuthSuccessRtmp() {
    runOnUiThread {
      Toast.makeText(this@RtmpActivity, "Auth success", Toast.LENGTH_SHORT)
          .show()
    }
  }

  override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
    drawerLayout!!.openDrawer(GravityCompat.START)
  }

  override fun surfaceChanged(
    surfaceHolder: SurfaceHolder,
    i: Int,
    i1: Int,
    i2: Int
  ) {
    rtmpCamera1!!.startPreview()
    // optionally:
    //rtmpCamera1.startPreview(CameraHelper.Facing.BACK);
    //or
    //rtmpCamera1.startPreview(CameraHelper.Facing.FRONT);
  }

  override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2 && rtmpCamera1!!.isRecording) {
      rtmpCamera1!!.stopRecord()
      bRecord!!.setText(string.start_record)
      Toast.makeText(
          this,
          "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
          Toast.LENGTH_SHORT
      )
          .show()
      currentDateAndTime = ""
    }
    if (rtmpCamera1!!.isStreaming) {
      rtmpCamera1!!.stopStream()
      bStartStop!!.text = resources.getString(string.start_button)
    }
    rtmpCamera1!!.stopPreview()
  }

  override fun onTouch(
    view: View,
    motionEvent: MotionEvent
  ): Boolean {
    val action = motionEvent.action
    if (motionEvent.pointerCount > 1) {
      if (action == MotionEvent.ACTION_MOVE) {
        rtmpCamera1!!.setZoom(motionEvent)
      }
    } else {
      if (action == MotionEvent.ACTION_UP) {
        // todo place to add autofocus functional.
      }
    }
    return true
  }
}