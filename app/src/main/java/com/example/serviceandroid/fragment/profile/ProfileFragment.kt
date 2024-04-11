package com.example.serviceandroid.fragment.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentProfileBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    private var uri: Uri? = null

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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Cá nhân"
        onClickView()
    }

    private fun onClickView() {
        with(binding) {
            selectImage.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
            convertUriToFile.setOnClickListener {
                upload()
            }
        }
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
}