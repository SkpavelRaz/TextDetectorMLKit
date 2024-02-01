package com.example.textdetectorbymlkit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val requestImageCapture = 1
    private lateinit var currentPhotoPath: String
    private var imageBitmap //to store the captured image
            : Bitmap? = null
    private lateinit var captureImageButton:Button
    private lateinit var detectTextButton:Button
    private lateinit var photoContainer:ImageView
    private lateinit var recognizedText:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureImageButton=findViewById(R.id.captureImageButton)
        photoContainer=findViewById(R.id.photoContainer)
        recognizedText=findViewById(R.id.recognizedText)
        detectTextButton=findViewById(R.id.detectTextButton)
        captureImageButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        detectTextButton.setOnClickListener {
            detectTextFromImage()
        }


    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    return
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.textdetectorbymlkit.fileprovider", //must be the same as manifest
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, requestImageCapture)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            galleryAddPic() // If we want to save the picture
            setPic()
        }
    }

    private fun setPic() {
        // Get the dimensions of the View
        val targetW: Int = photoContainer.width
        val targetH: Int = photoContainer.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            imageBitmap=bitmap
            photoContainer.setImageBitmap(imageBitmap)
        }
    }

    private fun detectTextFromImage(){
//        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap!!)
//        val firebaseVisionTextDetector = FirebaseVision.getInstance().cloudDocumentTextRecognizer
//        firebaseVisionTextDetector.processImage(firebaseVisionImage).addOnSuccessListener {
//            val blocks = it.blocks
//            if(blocks.isNotEmpty()){
//                val stringBuffer = StringBuffer()
//                blocks.forEach { bloc->
//                    stringBuffer.append(bloc.text)
//                }
//
//            }
//
//        }.addOnFailureListener{
//            Log.d("p4yam",it.toString())
//        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        imageBitmap?.let {
            recognizer.process(it,180)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    // ...

                    for (i in visionText.textBlocks){
                        Log.d("visionText", i.text)

                    }
                    recognizedText.text=visionText.text

                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                    Log.d("e",e.toString())
                }
        }
    }
}