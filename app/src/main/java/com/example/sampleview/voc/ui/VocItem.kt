package com.example.sampleview.voc.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 问卷选项实体
 *
 * @property string 选项文本
 * @property select 是否已选择
 */
@Parcelize
data class VocItem(
    val string: String,
    var select: Boolean = false,
) : Parcelable