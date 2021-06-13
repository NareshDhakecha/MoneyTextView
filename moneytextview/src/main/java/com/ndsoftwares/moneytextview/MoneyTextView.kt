package com.ndsoftwares.moneytextview

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import org.apache.commons.lang3.math.NumberUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


class MoneyTextView : View {
    private var mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var mDecimalFormat: DecimalFormat? = null
    private var mSymbolSection: Section = Section()
    private var mIntegerSection: Section = Section()
    private var mDecimalSection: Section = Section()
    private var mDecimalSeparator = 0.toChar()
    private var mAmount = 0.0
    private var mGravity = 0
    private var mSymbolGravity = 0
    private var mDecimalGravity = 0
    private var mSymbolMargin = 0f
    private var mDecimalMargin = 0f
    private var mIncludeDecimalSeparator = false
    private var mWidth = 0
    private var mHeight = 0
    private var mTextPaintRoomSize = 0f

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }


    companion object {
        private const val GRAVITY_START = 1
        private const val GRAVITY_END = 2
        private const val GRAVITY_TOP = 4
        private const val GRAVITY_BOTTOM = 8
        private const val GRAVITY_CENTER_VERTICAL = 16
        private const val GRAVITY_CENTER_HORIZONTAL = 32
        private const val MIN_PADDING = 2f
    }

    private fun init(context: Context, attrs: AttributeSet?) {

        val r: Resources = resources
        mTextPaintRoomSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            mTextPaint.density,
            r.displayMetrics
        )
        val typedArray: TypedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.MoneyTextView,
            0, R.style.MoneyTextViewDefaultStyle
        )
        try {
            mSymbolSection.text = typedArray.getString(R.styleable.MoneyTextView_symbol).toString()
            val givenAmount = typedArray.getString(R.styleable.MoneyTextView_amount)
            if (givenAmount != null)
                mAmount = getAmountAsDouble(givenAmount)
            mGravity = typedArray.getInt(
                R.styleable.MoneyTextView_gravity,
                GRAVITY_CENTER_VERTICAL or GRAVITY_CENTER_HORIZONTAL
            )
            mSymbolGravity = typedArray.getInt(
                R.styleable.MoneyTextView_symbolGravity,
                GRAVITY_TOP or GRAVITY_START
            )
            mDecimalGravity =
                typedArray.getInt(R.styleable.MoneyTextView_decimalGravity, GRAVITY_TOP)
            mIncludeDecimalSeparator =
                typedArray.getBoolean(R.styleable.MoneyTextView_includeDecimalSeparator, true)
            mSymbolMargin =
                typedArray.getDimensionPixelSize(R.styleable.MoneyTextView_symbolMargin, 0)
                    .toFloat()
            mDecimalMargin =
                typedArray.getDimensionPixelSize(R.styleable.MoneyTextView_decimalMargin, 0)
                    .toFloat()
            mIntegerSection.textSize =
                typedArray.getDimension(R.styleable.MoneyTextView_baseTextSize, 12f)
            mSymbolSection.textSize = typedArray.getDimension(
                R.styleable.MoneyTextView_symbolTextSize,
                mIntegerSection.textSize
            )
            mDecimalSection.textSize = typedArray.getDimension(
                R.styleable.MoneyTextView_decimalDigitsTextSize,
                mIntegerSection.textSize
            )
            mIntegerSection.color = typedArray.getInt(R.styleable.MoneyTextView_baseTextColor, 0)
            mSymbolSection.color = typedArray.getInt(
                R.styleable.MoneyTextView_symbolTextColor,
                mIntegerSection.color
            )
            mDecimalSection.color = typedArray.getInt(
                R.styleable.MoneyTextView_decimalTextColor,
                mIntegerSection.color
            )
            mDecimalSection.drawUnderline =
                typedArray.getBoolean(R.styleable.MoneyTextView_decimalUnderline, false)
            var format = typedArray.getString(R.styleable.MoneyTextView_format)
            val decimalSeparator = typedArray.getString(R.styleable.MoneyTextView_decimalSeparator)
            val fontPath = typedArray.getString(R.styleable.MoneyTextView_fontPath)
            var typeface  = Typeface.DEFAULT_BOLD
            if (fontPath != null) {
                typeface = Typeface.create(Typeface.createFromAsset(context.assets, fontPath), Typeface.BOLD)
            }
            mTextPaint.typeface = typeface
            if (format == null) {
                format = context.getString(R.string.default_format)
            }
            mDecimalFormat = DecimalFormat(format)
            val decimalFormatSymbol = DecimalFormatSymbols(Locale.getDefault())
            mDecimalSeparator = if (!TextUtils.isEmpty(decimalSeparator)) {
                decimalSeparator!![0]
            } else {
                context.getString(R.string.default_decimal_separator)[0]
            }
            decimalFormatSymbol.decimalSeparator = mDecimalSeparator
            mDecimalFormat!!.decimalFormatSymbols = decimalFormatSymbol
            amount = mAmount
        } finally {
            typedArray.recycle()
        }
    }

    private fun getAmountAsDouble(givenAmount: String): Double {
        // check if amount is not empty and is double
        if (TextUtils.isEmpty(givenAmount)) return 0.0

        return if (!NumberUtils.isCreatable(givenAmount)) {
            0.0
        } else {
            NumberUtils.toDouble(givenAmount, 0.0)
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setPadding(
            getMinPadding(paddingLeft), getMinVerticalPadding(paddingTop),
            getMinPadding(paddingRight), getMinVerticalPadding(paddingBottom)
        )
        createTextFromAmount()
        calculateBounds(widthMeasureSpec, heightMeasureSpec)
        calculatePositions()
        setMeasuredDimension(mWidth, mHeight)
    }

    private fun createTextFromAmount() {
        val formattedAmount: String = mDecimalFormat!!.format(mAmount)
        val separatorIndex = formattedAmount.lastIndexOf(mDecimalSeparator)
        if (separatorIndex > -1) {
            mIntegerSection.text = formattedAmount.substring(0, separatorIndex)
            mDecimalSection.text =
                formattedAmount.substring(if (mIncludeDecimalSeparator) separatorIndex else separatorIndex + 1)
        } else {
            mIntegerSection.text = formattedAmount
            mDecimalSection.text = ""
        }
    }

    private fun calculateBounds(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        mSymbolSection.calculateBounds(mTextPaint)
        mIntegerSection.calculateBounds(mTextPaint)
        mDecimalSection.calculateBounds(mTextPaint)
        mDecimalSection.calculateNumbersHeight(mTextPaint)
        mIntegerSection.calculateNumbersHeight(mTextPaint)
        when (widthMode) {
            MeasureSpec.EXACTLY -> mWidth = widthSize
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> mWidth =
                ((mIntegerSection.width + mDecimalSection.width + mSymbolSection.width
                        + mSymbolMargin + mDecimalMargin + paddingLeft + paddingRight).toInt())
            else -> {
            }
        }
        when (heightMode) {
            MeasureSpec.EXACTLY -> mHeight = heightSize
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> mHeight =
                (paddingTop + paddingBottom
                        + mIntegerSection.height.coerceAtLeast(
                    mDecimalSection.height.coerceAtLeast(mSymbolSection.height)
                ))
            else -> {
            }
        }
    }

    private fun calculatePositions() {
        val symbolGravityXAxis = mSymbolGravity and GRAVITY_START
        val symbolGravityYAxis = mSymbolGravity and GRAVITY_TOP
        val fromY: Int
        val fromX: Int
        val width = (mIntegerSection.width + mDecimalSection.width + mSymbolSection.width
                + mSymbolMargin + mDecimalMargin).toInt()
        if (mGravity and GRAVITY_START == GRAVITY_START) {
            fromX = paddingLeft
        } else if (mGravity and GRAVITY_END == GRAVITY_END) {
            fromX = mWidth - width - paddingRight
        } else {
            fromX = (mWidth shr 1) - (width shr 1)
        }
        if (mGravity and GRAVITY_TOP == GRAVITY_TOP) {
            fromY = paddingTop + mIntegerSection.height
        } else if (mGravity and GRAVITY_BOTTOM == GRAVITY_BOTTOM) {
            fromY = mHeight - paddingBottom
        } else {
            fromY = (mHeight shr 1) + (mIntegerSection.height shr 1)
        }
        calculateY(fromY, symbolGravityYAxis)
        calculateX(fromX, symbolGravityXAxis)
    }

    private fun calculateY(from: Int, symbolGravityYAxis: Int) {
        mIntegerSection.y = from
        mSymbolSection.y =
            from - if (symbolGravityYAxis == GRAVITY_TOP) mIntegerSection.height - mSymbolSection.height + mSymbolSection.bounds.bottom else 0
        mDecimalSection.y =
            from - if (mDecimalGravity == GRAVITY_TOP) mIntegerSection.height - mDecimalSection.height else 0
    }

    private fun calculateX(from: Int, symbolGravityXAxis: Int) {
        if (symbolGravityXAxis == GRAVITY_START) {
            mSymbolSection.x = from
            mIntegerSection.x =
                (mSymbolSection.x + mSymbolSection.width + mSymbolMargin).toInt()
            mDecimalSection.x =
                (mIntegerSection.x + mIntegerSection.width + mDecimalMargin).toInt()
        } else {
            mIntegerSection.x = from
            mDecimalSection.x =
                (mIntegerSection.x + mIntegerSection.width + mDecimalMargin).toInt()
            mSymbolSection.x =
                (mDecimalSection.x + mDecimalSection.width + mSymbolMargin).toInt()
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawSection(canvas, mIntegerSection)
        drawSection(canvas, mDecimalSection)
        drawSection(canvas, mSymbolSection)
    }

    private fun drawSection(canvas: Canvas, section: Section) {
        mTextPaint.textSize = section.textSize
        mTextPaint.color = section.color
        mTextPaint.isUnderlineText = section.drawUnderline
        canvas.drawText(
            section.text,
            section.x - mTextPaintRoomSize * 2,
            section.y - mTextPaintRoomSize / 2,
            mTextPaint
        )
    }

    ///
    /// SETTERS
    ///
    fun setAmount(amount: Double, symbol: String) {
        mAmount = amount
        mSymbolSection.text = symbol
        requestLayout()
    }

    fun setDecimalFormat(decimalFormat: DecimalFormat?) {
        mDecimalFormat = decimalFormat
        requestLayout()
    }

    fun setDecimalSeparator(decimalSeparator: Char) {
        mDecimalSeparator = decimalSeparator
        requestLayout()
    }

    fun setSymbolMargin(symbolMargin: Float) {
        mSymbolMargin = symbolMargin
        requestLayout()
    }

    fun setDecimalMargin(decimalMargin: Float) {
        mDecimalMargin = decimalMargin
        requestLayout()
    }

    fun setIncludeDecimalSeparator(includeDecimalSeparator: Boolean) {
        mIncludeDecimalSeparator = includeDecimalSeparator
        requestLayout()
    }

    fun setBaseTextSize(textSize: Float) {
        mIntegerSection.textSize = textSize
        requestLayout()
    }

    fun setSymbol(symbol: String) {
        mSymbolSection.text = symbol
        requestLayout()
    }

    fun setSymbolTextSize(textSize: Float) {
        mSymbolSection.textSize = textSize
        requestLayout()
    }

    fun setDecimalsTextSize(textSize: Float) {
        mDecimalSection.textSize = textSize
        requestLayout()
    }

    fun setBaseColor(color: Int) {
        mIntegerSection.color = color
        invalidate()
    }

    fun setSymbolColor(color: Int) {
        mSymbolSection.color = color
        invalidate()
    }

    fun setDecimalsColor(color: Int) {
        mDecimalSection.color = color
        invalidate()
    }

    var amount: Double
        get() = mAmount
        set(amount) {
            mAmount = amount
            requestLayout()
        }

    private fun getMinPadding(padding: Int): Int {
        return if (padding == 0) {
            (MIN_PADDING * Resources.getSystem().displayMetrics.density).toInt()
        } else padding
    }

    private fun getMinVerticalPadding(padding: Int): Int {
        val maxTextSize = Math.max(mIntegerSection.textSize, mDecimalSection.textSize)
        mTextPaint.textSize = maxTextSize
        val maximumDistanceLowestGlyph = mTextPaint.fontMetrics.bottom
        return if (padding < maximumDistanceLowestGlyph) {
            maximumDistanceLowestGlyph.toInt()
        } else padding
    }

    private class Section {
        var x = 0
        var y = 0
        var bounds: Rect = Rect()
        var text: String = ""
        var textSize = 0f
        var color = 0
        var width = 0
        var height = 0
        var drawUnderline = false

        fun calculateBounds(paint: TextPaint) {
            paint.textSize = textSize
            paint.getTextBounds(text, 0, text.length, bounds)
            width = bounds.width()
            height = bounds.height()
        }

        fun calculateNumbersHeight(paint: TextPaint) {
            val numbers = text.replace("[^0-9]".toRegex(), "")
            paint.textSize = textSize
            paint.getTextBounds(numbers, 0, numbers.length, bounds)
            height = bounds.height()
        }

    }

}