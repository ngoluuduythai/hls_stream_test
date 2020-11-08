package com.pedro.rtplibrary.network

import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import kotlinx.coroutines.runBlocking
import java.util.logging.Level
import java.util.logging.Logger

class AmazonUploader(
  private val context: Context,
  private val view: AmazonView
) {
  private var file: HLSFile? = null
  private var IMAGE_URL: String? = null
  var IMAGE_LINK = "https://zenvms.s3-us-west-2.amazonaws.com/"



  fun uploadPhoto(
    file: HLSFile,
    fileNameKey: String
  ) {
    view.startUpLoad()
    this.file = file
      Logger.getLogger("com.amazonaws.request").level = Level.FINEST
      util = S3Util()
      val transferUtility =
        util!!.getTransferUtility(context)
      val observer = transferUtility!!.upload(
          "hls/$fileNameKey",
          file.data
      )
      IMAGE_URL = String.format("%s%s", IMAGE_LINK, fileNameKey)
      Log.d(
          TAG,
          "begin upload" + fileNameKey + " in time: " + System.currentTimeMillis()
      )

      //Log.d(TAG, "file link: " + IMAGE_URL);

      //imageAttach = new ImageAttach( UUID.randomUUID().toString(), IMAGE_URL);
      Log.d(
          TAG, observer.id
          .toString()
      )
      Log.d(
          TAG, observer.bytesTransferred
          .toString()
      )
      observer.setTransferListener(UploadListener())

  }

  interface AmazonView {
    fun updateImage(file: HLSFile?)
    fun startUpLoad()
    fun upLoadFail()
    fun percentUpLoad(percent: Long)
  }

  private inner class UploadListener : TransferListener {
    override fun onStateChanged(
      id: Int,
      state: TransferState
    ) {
      Log.d(TAG, state.name)
      when (state.name) {
        "COMPLETED" -> {
          if (file != null) {
            Log.d(
                TAG, "file path: " + file!!.data
                .absolutePath
            )
          }
          Log.d(
              TAG,
              "done upload" + " in time: " + System.currentTimeMillis()
          )
          view.updateImage(file)
        }
        "FAILED" -> view.upLoadFail()
        else -> {
        }
      }
    }

    override fun onProgressChanged(
      id: Int,
      bytesCurrent: Long,
      bytesTotal: Long
    ) {
      view.percentUpLoad(bytesCurrent / bytesTotal)
      Log.d(
          TAG,
          String.format("%tL %s %tL", bytesCurrent, "/", bytesTotal)
      )
    }

    override fun onError(
      id: Int,
      ex: Exception
    ) {
      Log.d(
          TAG, String.format("%s %s", "Error during upload: ", id),
          ex
      )
    }
  }

  companion object {
    const val TAG = "Amazon S3"
    private var util: S3Util? = null
  }

}