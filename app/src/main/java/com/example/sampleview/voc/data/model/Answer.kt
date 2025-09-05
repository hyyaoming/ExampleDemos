package com.example.sampleview.voc.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户问卷回答数据。
 *
 * @property problems 用户选择的问题列表。
 * @property hasProblem 是否存在问题。
 * @property otherReason 其他原因说明（用户填写的自由文本）。
 */
@Parcelize
data class Answer(
    val problems: ArrayList<String> = arrayListOf(),
    var hasProblem: Boolean = false,
    var otherReason: String = "",
) : Parcelable
