package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import android.widget.AutoCompleteTextView
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.applyColorFilter

class MyAutoCompleteTextView : AutoCompleteTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        background?.mutate()?.applyColorFilter(accentColor)

        // requires android:textCursorDrawable="@null" in xml to color the cursor too
        setTextColor(resources.getColor(R.color.md_grey_black_dark))
        setLinkTextColor(accentColor)
    }
}
