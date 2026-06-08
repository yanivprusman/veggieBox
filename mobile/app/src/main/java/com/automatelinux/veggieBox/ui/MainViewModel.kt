package com.automatelinux.veggieBox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automatelinux.veggieBox.data.api.VeggieApi
import com.automatelinux.veggieBox.util.SettingsStore
import com.automatelinux.veggieBox.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

data class UiState(
    val loading: Boolean = true,
    val route: RouteResponse? = null,
    val earnings: Earnings? = null,
    val greet: List<GreetTarget> = emptyList(),
    val hideDelivered: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val api: VeggieApi,
    private val settings: SettingsStore,
) : ViewModel() {

    // Restore persisted preferences (e.g. the hide-delivered toggle) on launch.
    private val _state = MutableStateFlow(UiState(hideDelivered = settings.hideDelivered))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val route = api.route()
                val earnings = runCatching { api.earnings() }.getOrNull()
                val greet = runCatching { api.greeting() }.getOrNull()?.targets ?: emptyList()
                _state.update {
                    it.copy(loading = false, route = route, earnings = earnings, greet = greet)
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "load failed") }
            }
        }
    }

    fun toggleHideDelivered() {
        val next = !_state.value.hideDelivered
        settings.hideDelivered = next   // persist across launches
        _state.update { it.copy(hideDelivered = next) }
    }

    fun stops(): List<Stop> = _state.value.route?.stops ?: emptyList()
    fun stop(id: Int): Stop? = stops().firstOrNull { it.stopId == id }

    fun deliver(stopId: Int, drop: String = "home", cartons: Int? = null) {
        update(stopId, StatusBody(status = "delivered", dropUsed = drop, cartons = cartons))
    }

    fun undeliver(stopId: Int) {
        update(stopId, StatusBody(status = "pending", dropUsed = null))
    }

    fun skip(stopId: Int) {
        update(stopId, StatusBody(status = "skipped"))
    }

    fun setCartons(stopId: Int, cartons: Int) {
        update(stopId, StatusBody(cartons = cartons))
    }

    private fun update(stopId: Int, body: StatusBody) {
        viewModelScope.launch {
            runCatching { api.updateStop(stopId, body) }
            load()
        }
    }

    /** Records "on my way" server-side and returns the wa.me URL for the UI to open. */
    suspend fun onMyWay(stopId: Int): String? =
        runCatching { api.onMyWay(stopId).waUrl }.getOrNull().also { load() }

    fun optimize(lat: Double?, lon: Double?, onDone: () -> Unit = {}) {
        val routeId = _state.value.route?.routeId ?: return
        viewModelScope.launch {
            runCatching { api.optimize(OptimizeBody(routeId, lat, lon)) }
            load()
            onDone()
        }
    }

    fun setCustomerLocation(customerId: Int, lat: Double, lon: Double, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { api.updateCustomer(customerId, mapOf("lat" to lat, "lon" to lon)) }
            load()
            onDone()
        }
    }

    fun uploadMedia(stopId: Int, file: File, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val type = if (file.extension.lowercase() in listOf("mp4", "3gp", "mov")) "video/*" else "image/*"
                val part = MultipartBody.Part.createFormData(
                    "file", file.name, file.asRequestBody(type.toMediaTypeOrNull()),
                )
                api.uploadMedia(stopId, part)
            }
            load()
            onDone()
        }
    }
}
