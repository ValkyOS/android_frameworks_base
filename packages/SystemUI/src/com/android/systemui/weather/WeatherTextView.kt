/*
 * Copyright (C) 2023-2024 risingOS Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.weather

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

/**
 * Weather placeholder used by some lock screen clock styles. LineageOS has no
 * weather provider, so the view stays hidden.
 */
class WeatherTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextView(context, attrs, defStyle) {

    init {
        visibility = View.GONE
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(View.GONE)
    }
}
