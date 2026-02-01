package com.example.pricesapp.data

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Product(
    @SerialName("id")
    val id: String = "",

    @SerialName("name")
    val name: String,

    @SerialName("price")
    val price: Double,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("barcode")
    val barcode: String? = null
)
