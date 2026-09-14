package com.example.serviceandroid.fragment.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.R
import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.data.auth.AuthUser
import com.example.serviceandroid.data.user.UserRepository
import com.example.serviceandroid.model.UpdateAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(user = authRepository.currentUser())
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.value.user?.let(::ensureExistingProfile)
    }

    fun signInWithGoogle(idToken: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val signInResult = runCatching {
                authRepository.signInWithGoogle(idToken)
            }
            val user = signInResult.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Không thể đăng nhập bằng Google",
                )
                return@launch
            }

            val syncError = runCatching {
                userRepository.upsertAfterLogin(user)
            }.exceptionOrNull()
            _uiState.value = ProfileUiState(
                user = user,
                errorMessage = syncError?.let {
                    "Đăng nhập thành công nhưng chưa thể đồng bộ hồ sơ lên Firestore"
                },
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = ProfileUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun ensureExistingProfile(user: AuthUser) {
        viewModelScope.launch {
            runCatching {
                userRepository.ensureProfile(user)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Chưa thể đồng bộ hồ sơ người dùng lên Firestore",
                )
            }
        }
    }

    fun getUpdateAccounts(): MutableList<UpdateAccount> = mutableListOf(
        UpdateAccount(
            "Plus",
            "19,000đ",
            "Nghe nhạc với chất lượng cao nhất, không \nquảng cáo",
            "Loại bỏ quảng cáo",
            R.drawable.ic_advertisement,
            "Lưu trữ nhạc không giới hạn",
            R.drawable.ic_download_thin,
            "Tuỳ chỉnh chế độ phát nhạc",
            R.drawable.ic_custom,
            R.drawable.bg_purple_corner_10_stroke_1,
            R.color.purple_1
        ),
        UpdateAccount(
            "Premium",
            "49,000đ",
            "Toàn bộ đăc quyền Plus cùng kho nhạc Premium",
            "Nghe và tải tất cả",
            R.drawable.ic_diamond,
            "Loại bỏ quảng cáo",
            R.drawable.ic_advertisement,
            "Lưu trữ nhạc không giới hạn",
            R.drawable.ic_download_thin,
            R.drawable.bg_orange_corner_1,
            R.color.bg_orange
        )
    )
}

data class ProfileUiState(
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
