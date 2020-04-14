package com.skripsi.arab

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.skripsi.arab.datasource.TranslateDataSource
import com.skripsi.arab.model.TranslateResponse
import com.theartofdev.edmodo.cropper.CropImage

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private var mUri: Uri? = null
    private var mCropImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        title = getString(R.string.app_name)
        supportActionBar?.title = title
        dismissProgress()

        translateButton.setOnClickListener {
            doTranslating()
        }
        openCameraButton.setOnClickListener { startCropImageActivity() }
        fab.setOnClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }
        infoButton.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }
        clearButton.setOnClickListener { resetAll() }
        copyButton.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Result", result.text.toString())
            clipboard.primaryClip = clip
            show("Berhasil di salin")
        }
    }

    private fun doTranslating() {
        showProgress()
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.TRANSLATE_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(TranslateDataSource::class.java)
        val call = service.translate(translateText.text.toString())
        call.enqueue(object : Callback<TranslateResponse> {
            override fun onFailure(call: Call<TranslateResponse>?, t: Throwable?) {
                dismissProgress()
                AlertDialog.Builder(applicationContext)
                    .setTitle("Alert")
                    .setMessage(t?.message)
                    .show()
            }

            override fun onResponse(
                call: Call<TranslateResponse>?,
                response: Response<TranslateResponse>?
            ) {
                dismissProgress()
                if (response?.code() == 200) {
                    result.text = response.body().text?.first()
                }
            }
        })
    }

    private fun startCropImageActivity() {
        CropImage.activity()
            .start(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val imageUri = CropImage.getPickImageResultUri(this, data)
                    if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                        mCropImageUri = imageUri
                        requestPermissions(
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE
                        )
                    } else {
                        startCropImageActivity()
                    }
                    val bitmap = BitmapFactory.decodeStream(
                        contentResolver.openInputStream(imageUri)
                    )
                    resultImage.setImageBitmap(bitmap)
                    resultImageText.text = ""
                }
            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    val bitmap = BitmapFactory.decodeStream(
                        contentResolver.openInputStream(result.uri)
                    )
                    resultImage.setImageBitmap(bitmap)
                    resultImageText.text = ""
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    show("Error get picture")
                }
            }
        }
    }

    fun startRecognizing(v: View) {
        showProgress()
        if (resultImage.drawable != null) {
            translateText.setText("")
            v.isEnabled = false
            val bitmap = (resultImage.drawable as BitmapDrawable).bitmap
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(listOf("ar"))
                .build()
            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    v.isEnabled = true
                    processResultText(firebaseVisionText)
                }
                .addOnFailureListener {
                    dismissProgress()
                    v.isEnabled = true
                    translateText.setText("Failed")
                }
        } else {
            Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
        }

    }

    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            translateText.setText("No Text Found")
            return
        }
        for (block in resultText.textBlocks) {
            val blockText = block.text
            translateText.append(blockText + "\n")
        }
        dismissProgress()
    }

    private fun show(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>
        , grantedResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        when (requestCode) {
            CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE ->
                if (mUri != null && grantedResults.isNotEmpty() && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCropImageActivity()
                } else {
                    show("Cancelling, required permissions are not granted")
                }
        }
    }

    private fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    private fun dismissProgress() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun resetAll() {
        resultImageText.text = getString(R.string.view_image)
        resultImage.setImageBitmap(null)
        result.text = ""
        translateText.setText("")
    }
}
