package com.example.serviceandroid.fragment.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.MediaController
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentProfileBinding
import com.example.serviceandroid.utils.ExtensionFunctions.getFileName
import com.example.serviceandroid.utils.ExtensionFunctions.snackBar
import com.example.serviceandroid.utils.UploadRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ProfileFragment : BaseFragment<FragmentProfileBinding>(), UploadRequestBody.UploadCallback {
    private var uri: Uri? = null
    private var myVideoController: MediaController? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uriImage ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uriImage != null) {
            uri = uriImage
            binding.selectImage.setImageURI(uriImage)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uriImage ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uriImage != null) {
            uri = uriImage
            binding.selectImage.setImageURI(uriImage)
            binding.root.snackBar("Select Image Success")
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Cá nhân"
        setUpVideo()
        onClickView()
    }

    private fun setUpVideo() {
        if(myVideoController == null) {
            myVideoController = MediaController(context)
            myVideoController?.setAnchorView(binding.videoView)
        }

        with(binding.videoView) {
            setMediaController(myVideoController)
            setVideoURI(Uri.parse("android.resource://" + context?.packageName + "/" + com.example.serviceandroid.R.raw.master_kotlin_android))
            requestFocus()
            start()
            setOnCompletionListener {
                this.rootView.snackBar("Video Completed")
            }
        }

    }

    private fun onClickView() {
        with(binding) {
            selectImage.setOnClickListener {
//                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                Intent(Intent.ACTION_PICK).also {
                    it.type = "image/*"
                    val mimeTypes = arrayOf("image/jpeg", "image/png")
                    it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    pickImage.launch("image/*")
                }
            }
            convertUriToFile.setOnClickListener {
//                upload()
                uploadImage()
            }
        }
    }

    @SuppressLint("Recycle")
    private fun uploadImage() {
        val parcelFileDescriptor =
            context?.contentResolver?.openFileDescriptor(uri!!, "r", null) ?: return
        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = context?.contentResolver?.getFileName(uri!!)?.let { File(context?.cacheDir, it) }
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        val body = UploadRequestBody(file!!, "image", this)
    }

    private fun upload() {
        val fileDir = context?.filesDir
        val file = File(fileDir, "image.jpg")

        val inputStream = uri?.let { context?.contentResolver?.openInputStream(it) }
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        val requestBody = file.asRequestBody(("image/*").toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("profile", file.name, requestBody)
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentProfileBinding.inflate(inflater)

    override fun onProgressUpdate(percentage: Int) {

    }
}