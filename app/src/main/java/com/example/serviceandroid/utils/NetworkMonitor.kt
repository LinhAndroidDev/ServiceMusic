package com.example.serviceandroid.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.serviceandroid.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val offlineMessage = context.getString(R.string.network_offline)
    private val onlineMessage = context.getString(R.string.network_online)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var offlineDebounceJob: Job? = null
    private var hideOnlineBannerJob: Job? = null
    private var lastKnownOnline: Boolean? = null
    private var wasNetworkValidated = false
    private var isInitialCheck = true

    private val _state = MutableStateFlow(
        NetworkUiState(
            isOnline = true,
            showBanner = false,
            message = "",
        ),
    )
    val state: StateFlow<NetworkUiState> = _state.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "onAvailable: network=$network")
            evaluateNetwork()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "onLost: network=$network")
            evaluateNetwork(forceOfflineIfNoActiveNetwork = true)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            Log.d(TAG, "onCapabilitiesChanged: network=$network validated=$validated")
            evaluateNetwork()
        }
    }

    init {
        Log.d(TAG, "init: registering default network callback")
        evaluateNetwork()
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun evaluateNetwork(forceOfflineIfNoActiveNetwork: Boolean = false) {
        val online = isNetworkValidated()
        val reconnecting = online && !wasNetworkValidated
        Log.d(
            TAG,
            "evaluateNetwork: online=$online reconnecting=$reconnecting wasNetworkValidated=$wasNetworkValidated " +
                "forceOfflineIfNoActiveNetwork=$forceOfflineIfNoActiveNetwork " +
                "lastKnownOnline=$lastKnownOnline isInitialCheck=$isInitialCheck",
        )
        if (!online && forceOfflineIfNoActiveNetwork) {
            wasNetworkValidated = false
            scheduleOfflineState()
            return
        }
        if (online) {
            applyOnlineState(showReconnectBanner = reconnecting)
            wasNetworkValidated = true
        } else {
            wasNetworkValidated = false
            scheduleOfflineState()
        }
    }

    private fun isNetworkValidated(): Boolean {
        val network = connectivityManager.activeNetwork
        if (network == null) {
            Log.d(TAG, "isNetworkValidated: activeNetwork=null -> false")
            return false
        }
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null) {
            Log.d(TAG, "isNetworkValidated: capabilities=null -> false")
            return false
        }
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        Log.d(
            TAG,
            "isNetworkValidated: network=$network internet=$hasInternet validated=$validated wifi=$wifi cellular=$cellular",
        )
        return hasInternet && validated
    }

    private fun scheduleOfflineState() {
        Log.d(TAG, "scheduleOfflineState: debounce ${OFFLINE_DEBOUNCE_MS}ms")
        offlineDebounceJob?.cancel()
        offlineDebounceJob = scope.launch {
            delay(OFFLINE_DEBOUNCE_MS)
            val stillOffline = !isNetworkValidated()
            Log.d(TAG, "scheduleOfflineState: after debounce stillOffline=$stillOffline")
            if (stillOffline) {
                applyOfflineState()
            }
        }
    }

    private fun applyOfflineState() {
        Log.d(TAG, "applyOfflineState: show offline banner")
        offlineDebounceJob?.cancel()
        hideOnlineBannerJob?.cancel()
        lastKnownOnline = false
        wasNetworkValidated = false
        isInitialCheck = false
        _state.value = NetworkUiState(
            isOnline = false,
            showBanner = true,
            message = offlineMessage,
        )
    }

    private fun applyOnlineState(showReconnectBanner: Boolean = false) {
        offlineDebounceJob?.cancel()
        val wasOffline = lastKnownOnline == false
        val showingOfflineBanner = _state.value.showBanner && !_state.value.isOnline
        lastKnownOnline = true

        if (isInitialCheck) {
            Log.d(TAG, "applyOnlineState: initial check, hide banner")
            isInitialCheck = false
            _state.value = NetworkUiState(
                isOnline = true,
                showBanner = false,
                message = "",
            )
            return
        }

        val shouldShowBanner = showReconnectBanner || wasOffline || showingOfflineBanner
        if (!shouldShowBanner && _state.value.isOnline && !_state.value.showBanner) {
            Log.d(TAG, "applyOnlineState: skip, already online with hidden banner")
            return
        }

        Log.d(
            TAG,
            "applyOnlineState: show online banner reconnecting=$showReconnectBanner wasOffline=$wasOffline",
        )
        hideOnlineBannerJob?.cancel()
        _state.value = NetworkUiState(
            isOnline = true,
            showBanner = true,
            message = onlineMessage,
        )
        hideOnlineBannerJob = scope.launch {
            delay(ONLINE_BANNER_VISIBLE_MS)
            if (_state.value.isOnline) {
                Log.d(TAG, "applyOnlineState: hide online banner after delay")
                _state.value = NetworkUiState(
                    isOnline = true,
                    showBanner = false,
                    message = "",
                )
            }
        }
    }

    companion object {
        private const val TAG = "NetworkMonitor"
        private const val OFFLINE_DEBOUNCE_MS = 400L
        private const val ONLINE_BANNER_VISIBLE_MS = 2_500L
    }
}
