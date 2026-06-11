package com.automatelinux.veggieBox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automatelinux.veggieBox.data.api.VeggieApi
import com.automatelinux.veggieBox.util.SettingsStore
import com.automatelinux.veggieBox.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // Bumped after a reorder has completed AND the route has reloaded, so the UI can
    // snap the list to the top — the reordered #1 is otherwise off-screen and the
    // change reads as "nothing happened".
    private val _reorderTick = MutableStateFlow(0)
    val reorderTick: StateFlow<Int> = _reorderTick.asStateFlow()

    // Set when the user taps a map pin and chooses "open in route list". The Route
    // screen scrolls to and briefly highlights this stop, then clears it.
    private val _focusStopId = MutableStateFlow<Int?>(null)
    val focusStopId: StateFlow<Int?> = _focusStopId.asStateFlow()

    fun focusStopInRoute(stopId: Int) { _focusStopId.update { stopId } }
    fun clearFocusStop() { _focusStopId.update { null } }

    // One-shot user-facing messages (mostly write failures). A delivery worker in a
    // village with patchy reception MUST know a "delivered" tap didn't reach the
    // server — silently swallowing the error loses earnings data.
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private suspend fun reportFailure(what: String, e: Throwable) {
        _messages.emit("$what — בדוק חיבור לאינטרנט (${e.message ?: "שגיאה"})")
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { reload() }
    }

    // Awaitable reload so callers (e.g. optimize) can sequence work after the new
    // route is actually in state — `load()` alone is fire-and-forget.
    private suspend fun reload() {
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
                .onFailure { reportFailure("העדכון לא נשמר", it) }
            load()
        }
    }

    /**
     * Records "on my way" server-side and returns the wa.me URL for the UI to open.
     * Distinguishes a network failure (reported via [messages], returns null) from a
     * customer with no phone (also null — caller shows its own hint).
     */
    suspend fun onMyWay(stopId: Int): String? {
        val result = runCatching { api.onMyWay(stopId) }
        result.exceptionOrNull()?.let {
            reportFailure("שליחת 'בדרך' נכשלה", it)
            return null
        }
        load()
        val waUrl = result.getOrNull()?.waUrl
        if (waUrl == null) _messages.emit("אין מספר טלפון ללקוח")
        return waUrl
    }

    fun optimize(lat: Double?, lon: Double?, onDone: () -> Unit = {}) {
        val routeId = _state.value.route?.routeId ?: return
        viewModelScope.launch {
            runCatching { api.optimize(OptimizeBody(routeId, lat, lon)) }
                .onFailure { reportFailure("סידור המסלול נכשל", it) }
                .onSuccess {
                    reload()                       // await: state holds the new order before we signal
                    _reorderTick.update { it + 1 } // tells the UI to scroll the (now-reordered) list to top
                }
            onDone()
        }
    }

    fun setCustomerLocation(customerId: Int, lat: Double, lon: Double, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { api.updateCustomer(customerId, mapOf("lat" to lat, "lon" to lon)) }
                .onFailure { reportFailure("שמירת המיקום נכשלה", it) }
                .onSuccess { _messages.emit("📍 מיקום הבית נשמר") }
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
            }.onFailure { reportFailure("העלאת התיעוד נכשלה", it) }
            load()
            onDone()
        }
    }
}
