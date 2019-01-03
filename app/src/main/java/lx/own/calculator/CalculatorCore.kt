package lx.own.calculator

import java.lang.StringBuilder
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * <b> </b><br/>
 *
 * @author LeiXun
 * Created on 2019/1/3.
 */
class CalculatorCore {

    private var mValueNum: BigDecimal = BigDecimal("0")
    private var mOperatorNum: BigDecimal = BigDecimal("0")
    private var mOperatingNum: BigDecimal
        get() :BigDecimal {
            return if (isOperatingValue) mValueNum else mOperatorNum
        }
        set(value: BigDecimal) {
            if (isOperatingValue) mValueNum = value else mOperatorNum = value
        }
    private var mSubscriber: CalculatorCore.CalculatorSubscriber? = null

    private var mSavedOperator: CalculatorOperator = CalculatorOperator.None
    private var mCurrentlyOperator: CalculatorOperator = CalculatorOperator.None
    private var hasPoint: Boolean = false
    private var isOperatingValue: Boolean = true
    private var isAllClearMode: Boolean = true

    fun subscribe(s: CalculatorSubscriber) {
        mSubscriber = s
        mSubscriber?.onValueChanged(getDisplayNumber())
        mSubscriber?.onClearModeChanged(isAllClearMode)
    }

    /**
     * number button clicked
     * @param number the number of clicked button.
     */
    fun number(number: BigDecimal) {
        if (isAllClearMode) {
            isAllClearMode = false
            mSubscriber?.onClearModeChanged(isAllClearMode)
        }
        if (mCurrentlyOperator != CalculatorOperator.None) {
            mSavedOperator = mCurrentlyOperator
            mCurrentlyOperator = CalculatorOperator.None
            mSubscriber?.onOperatorUsing(mCurrentlyOperator)
            if (isOperatingValue) {
                isOperatingValue = false
                if (hasPoint) hasPoint = false
            }
        }
        if (hasPoint) {
            mOperatingNum = mOperatingNum.add(number.scaleByPowerOfTen(-(mOperatingNum.scale() + 1)))
        } else {
            mOperatingNum = mOperatingNum.multiply(BigDecimal.TEN)
            mOperatingNum = mOperatingNum.add(number)
        }
        mSubscriber?.onValueChanged(getDisplayNumber())
    }

    /**
     * point
     */
    fun point() {
        hasPoint = true
        if (mCurrentlyOperator != CalculatorOperator.None)
            mSubscriber?.onValueChanged(getDisplayNumber())
    }

    fun pm() {
        mOperatingNum = mOperatingNum.negate()
        mSubscriber?.onValueChanged(getDisplayNumber())
    }

    fun backspace() {
        val currentlyScale = mOperatingNum.scale()
        mOperatingNum = if (currentlyScale > 0) mOperatingNum.setScale(currentlyScale - 1, RoundingMode.FLOOR) else mOperatingNum.divideToIntegralValue(BigDecimal(10))
        mSubscriber?.onValueChanged(getDisplayNumber())
    }

    /**
     * reset
     */
    fun ac() {
        if (isAllClearMode) {
            mValueNum = BigDecimal("0")
            mOperatorNum = BigDecimal("0")
            mSubscriber?.onValueChanged(getDisplayNumber())
            mCurrentlyOperator = CalculatorOperator.None
            mSavedOperator = CalculatorOperator.None
            hasPoint = false
            isOperatingValue = true
        } else {
            mOperatingNum = BigDecimal("0")
            mSubscriber?.onValueChanged(getDisplayNumber())
            if (mSavedOperator != CalculatorOperator.None) {
                mCurrentlyOperator = mSavedOperator
                mSubscriber?.onOperatorUsing(mCurrentlyOperator)
            }
            isAllClearMode = true
            mSubscriber?.onClearModeChanged(isAllClearMode)
        }
    }

    fun operate(operator: CalculatorOperator) {
        if (mSavedOperator != CalculatorOperator.None)
            evaluate()
        mCurrentlyOperator = operator
        if (isOperatingValue)
            hasPoint = false
        isOperatingValue = false
        isAllClearMode = false
        mSubscriber?.onOperatorUsing(mCurrentlyOperator)
    }

    fun evaluate() {
        when (mSavedOperator) {
            CalculatorOperator.Addition -> {
                mValueNum = mValueNum.add(mOperatorNum).stripTrailingZeros()
            }
            CalculatorOperator.Subtraction -> {
                mValueNum = mValueNum.subtract(mOperatorNum).stripTrailingZeros()
            }
            CalculatorOperator.Multiplication -> {
                mValueNum = mValueNum.multiply(mOperatorNum).stripTrailingZeros()
            }
            CalculatorOperator.Division -> {
                try {
                    mValueNum = mValueNum.divide(mOperatorNum, MathContext.DECIMAL128).setScale(Math.max(mValueNum.scale(), 9), RoundingMode.HALF_UP).stripTrailingZeros()
                } catch (e: Throwable) {
                    isAllClearMode = true
                    mSubscriber?.onClearModeChanged(isAllClearMode)
                    ac()
                    mSubscriber?.onValueChanged("Error")
                    return
                }
            }
            CalculatorOperator.Modulus -> {
                mValueNum = mValueNum.rem(mOperatorNum).stripTrailingZeros()
            }
        }
        if (mValueNum.compareTo(BigDecimal.ZERO) == 0)
            mValueNum = BigDecimal(0)
        mSavedOperator = CalculatorOperator.None
        mCurrentlyOperator = CalculatorOperator.None
        mOperatorNum = BigDecimal("0")
        isOperatingValue = true
        hasPoint = mOperatingNum.compareTo(BigDecimal(mOperatingNum.toBigInteger())) != 0
        mSubscriber?.onValueChanged(getDisplayNumber())
    }

    fun percent() {
        if (mOperatingNum.compareTo(BigDecimal.ZERO) == 0) return
        mOperatingNum = mOperatingNum.movePointLeft(2).stripTrailingZeros()
        if (!hasPoint && mOperatingNum.compareTo(BigDecimal(mOperatingNum.toBigInteger())) != 0)
            hasPoint = true
        mSubscriber?.onValueChanged(getDisplayNumber())
    }

    private fun getDisplayNumber(): String {
        return formatNumberStr(mOperatingNum.toPlainString(), if (mOperatingNum.signum() < 0) 1 else 0)
    }

    private fun formatNumberStr(numStr: String, offset: Int): String {
        var pointIndex = numStr.indexOf(".")
        if (pointIndex in 0..3 || numStr.length <= 3) return numStr
        if (pointIndex == -1)
            pointIndex = numStr.length
        var returnValue = StringBuilder(numStr)
        pointIndex -= 3
        while (pointIndex > 0 + offset) {
            returnValue.insert(pointIndex, ",")
            pointIndex -= 3
        }
        return returnValue.toString()
    }

    interface CalculatorSubscriber {
        fun onValueChanged(value: String)
        fun onOperatorUsing(operator: CalculatorOperator)
        fun onClearModeChanged(isAll: Boolean)
    }

    enum class CalculatorOperator {
        Addition, Subtraction, Multiplication, Division, Modulus, None;
    }
}