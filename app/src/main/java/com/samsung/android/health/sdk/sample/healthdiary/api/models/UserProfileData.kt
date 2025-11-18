package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

data class UserProfileData(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("birth_date")
    val birthDate: String? = null,
    @SerializedName("gender")
    val gender: String? = null,
    @SerializedName("height")
    val height: Float? = null,
    @SerializedName("weight")
    val weight: Float? = null
)


