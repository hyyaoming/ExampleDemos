package com.example.sampleview.voc.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 问卷问题数据。
 *
 * @property scene 问卷所属场景标识。
 * @property title 问题标题。
 * @property subTitle 问题副标题或提示信息。
 * @property canClose 是否允许用户关闭或跳过该问题。
 * @property options 可选答案列表。
 */
@Parcelize
data class Question(
    val scene: String,
    val title: String,
    val subTitle: String,
    val canClose: Boolean,
    val options: List<String>,
) : Parcelable
