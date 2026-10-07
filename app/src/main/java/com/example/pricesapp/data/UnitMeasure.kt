package com.example.pricesapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnitMeasure(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,

    @SerialName("abbreviation")
    val abbreviation: String
)
