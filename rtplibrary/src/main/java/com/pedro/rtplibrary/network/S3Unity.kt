package com.pedro.rtplibrary.network

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.provider.Settings.Global.getString
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.pedro.rtplibrary.R
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class S3Unity(val applicationContext: Context?) {

  val POOL_ID = ""
  val bucket_name = ""
  val TAG = S3Unity::class.simpleName

  init {

  }



  private fun uploadToS3(dateTitle: String, s3file: File   ) = runBlocking {
    val awsCredentialsProvider: AWSCredentialsProvider =
      CognitoCachingCredentialsProvider(applicationContext, POOL_ID, Regions.US_EAST_2)
    val client = AmazonS3Client(awsCredentialsProvider)
    val s3 = TransferUtility
        .builder()
        .context(applicationContext)
        .s3Client(client)
        .build()
    val observer = s3.upload(
       bucket_name, dateTitle, s3file
    )
    observer.setTransferListener(object : TransferListener {
      override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
      }

      override fun onStateChanged(id: Int, state: TransferState?) {
        if (TransferState.COMPLETED == state) {
          Log.d(TAG, "Transfer completed")
        } else {
          Log.d(TAG, "Uhoh!")
        }
      }

      override fun onError(id: Int, ex: Exception?) {
        Log.d(TAG, "Error: $ex")
      }
    })
  }

  private fun saveToInternalStorage(bitmapImage: Bitmap): String {
    val storageDir: File? =
      ContextWrapper(applicationContext).getDir("imageDir", Context.MODE_PRIVATE)
    val timeStamp: String = SimpleDateFormat("HHmmss").format(Date())

    val imageFile = File(storageDir, "$timeStamp.jpg")
    var fos: FileOutputStream? = null
    try {
      fos = FileOutputStream(imageFile)
      bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      try {
        fos?.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    return storageDir?.absolutePath!!
  }
}