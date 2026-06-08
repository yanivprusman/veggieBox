package com.automatelinux.veggieBox.data.model

// Mirrors the JSON returned by the veggieBox Next.js backend (camelCase keys).

data class RouteResponse(
    val ok: Boolean = false,
    val business: Business? = null,
    val worker: Worker? = null,
    val routeId: Int? = null,
    val stops: List<Stop> = emptyList(),
    val progress: Progress? = null,
    val error: String? = null,
)

data class Business(
    val id: Int,
    val slug: String,
    val name: String,
    val area: String?,
    val defaultCentralDrop: String?,
    val ratePerDelivery: Double,
    val currency: String,
    val mapCenterLat: Double?,
    val mapCenterLon: Double?,
)

data class Worker(val id: Int, val name: String)

data class Progress(
    val total: Int = 0,
    val delivered: Int = 0,
    val notHome: Int = 0,
    val pending: Int = 0,
    val remaining: Int = 0,
)

data class Stop(
    val stopId: Int,
    val customerId: Int,
    val name: String,
    val phone: String?,
    val phoneIntl: String?,
    val address: String?,
    val houseInstructions: String?,
    val lat: Double?,
    val lon: Double?,
    val dropPreference: String?,   // central | beside | null
    val detailsToken: String?,
    val hasDetails: Boolean = false,
    val seq: Int = 0,
    val status: String = "pending", // pending | delivered | not_home | skipped
    val cartons: Int = 1,
    val dropUsed: String?,          // home | central | beside | null
    val mediaPath: String?,
    val onMyWaySentAt: String?,
    val deliveredAt: String?,
)

data class Earnings(
    val ok: Boolean = false,
    val rate: Double = 0.0,
    val currency: String = "₪",
    val today: EarnBucket = EarnBucket(),
    val week: EarnBucket = EarnBucket(),
    val all: EarnBucket = EarnBucket(),
)

data class EarnBucket(val deliveries: Int = 0, val amount: Double = 0.0)

data class GreetResponse(val ok: Boolean = false, val count: Int = 0, val targets: List<GreetTarget> = emptyList())

data class GreetTarget(
    val customerId: Int,
    val name: String,
    val phoneIntl: String?,
    val link: String,
    val message: String,
    val waUrl: String?,
)

data class OnMyWayResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val phoneIntl: String? = null,
    val waUrl: String? = null,
)

data class SimpleOk(val ok: Boolean = false, val error: String? = null)

// Request bodies
data class StatusBody(
    val status: String? = null,
    val cartons: Int? = null,
    val dropUsed: String? = null,
    val notes: String? = null,
    val mediaPath: String? = null,
)

data class OptimizeBody(val routeId: Int, val startLat: Double? = null, val startLon: Double? = null)
