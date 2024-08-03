package com.example.serviceandroid.fragment.profile

import android.R
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.serviceandroid.adapter.CustomArrayAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentProfileBinding
import com.example.serviceandroid.utils.ExtensionFunctions.snackBar
import com.example.serviceandroid.utils.UploadRequestBody
import java.lang.reflect.Field


class ProfileFragment : BaseFragment<FragmentProfileBinding>(), UploadRequestBody.UploadCallback {
    private var uri: Uri? = null
    private var myVideoController: MediaController? = null
    private var imageUris = arrayListOf<Uri>()

    companion object {
        const val PICK_IMAGES_REQUEST = 1
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {

        }
    }

    private fun handleSendImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            binding.selectImage.setImageURI(it)
        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
            // Update UI to reflect multiple images being shared
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.apply {
            when {
                intent?.action == Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        handleSendText(intent) // Handle text being sent
                    } else if (intent.type?.startsWith("image/") == true) {
                        handleSendImage(intent) // Handle single image being sent
                    }
                }
                intent?.action == Intent.ACTION_SEND_MULTIPLE
                        && intent.type?.startsWith("image/") == true -> {
                    handleSendMultipleImages(intent) // Handle multiple images being sent
                }
                else -> {
                    // Handle other intents, such as being started from the home screen
                }
            }
        }

        binding.header.title.text = "Cá nhân"
        setUpVideo()
        onClickView()

        binding.autoCompleteTextView.apply {
            val data: List<String?> = mutableListOf<String?>("Apple", "Banana", "Cherry", "Date", "Grape", "Kiwi", "Lemon", "Mango", "Orange", "Pineapple")
            val adapter = CustomArrayAdapter(requireActivity(), R.layout.simple_dropdown_item_1line, data)
            setAdapter(adapter)
            if(data.size > 5) dropDownHeight = 600

            post {
                try {
                    val popupField: Field = AutoCompleteTextView::class.java.getDeclaredField("mPopup")
                    popupField.isAccessible = true
                    val popup: ListPopupWindow = popupField.get(this) as ListPopupWindow
                    popup.width = this.width
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Hiển thị danh sách khi AutoCompleteTextView nhận được focus
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                }
            }

            // Hiển thị danh sách khi AutoCompleteTextView được click
            setOnClickListener {
                showDropDown()
            }
        }
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
                openImagePicker()
            }

            convertUriToFile.setOnClickListener {
                if(imageUris.isNotEmpty()) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                        type = "image/*"
                    }
                    startActivity(Intent.createChooser(shareIntent, null))
                }
            }
        }
    }

    // Hàm để mở picker và chọn ảnh
    private fun openImagePicker() {
        // Kiểm tra xem quyền đã được cấp hay chưa
        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Nếu quyền chưa được cấp, yêu cầu người dùng cấp quyền
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                10
            )
        } else {
            val intent = Intent().apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                action = Intent.ACTION_OPEN_DOCUMENT
            }
            startActivityForResult(Intent.createChooser(intent, ""), PICK_IMAGES_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            10 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent().apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        action = Intent.ACTION_OPEN_DOCUMENT
                    }
                    startActivityForResult(Intent.createChooser(intent, ""), PICK_IMAGES_REQUEST)
                } else {
                    Toast.makeText(requireActivity(), "Chua cap quyen", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            if (data != null) {
                if (data.clipData != null) {
                    // Đã chọn nhiều ảnh
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = data.clipData!!.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else {
                    // Chỉ chọn một ảnh
                    val imageUri = data.data
                    imageUris.add(imageUri!!)
                }
            }
        }
    }


    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentProfileBinding.inflate(inflater)

    override fun onProgressUpdate(percentage: Int) {

    }
}