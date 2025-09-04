package com.example.sampleview.voc.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Answer(
    val problems: ArrayList<String> = arrayListOf(),
    var hasProblem: Boolean = false,
    var otherReason: String = "",
): Parcelable