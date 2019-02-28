package lx.own.calculator.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.LinkedBlockingQueue

/**
 * <b> </b><br/>
 *
 * @author LeiXun
 * Created on 2019/1/3.
 */
class CalculatorCore private constructor() {

    companion object {
        val instance: CalculatorCore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { CalculatorCore() }
    }

    private var mValueNum: BigDecimal = BigDecimal(0)
    private var mOperatorNum: BigDecimal = BigDecimal(0)
    private var mOperatingNum: BigDecimal
        get() :BigDecimal {
            return if (isOperatingValue) mValueNum else mOperatorNum
        }
        set(value) {
            if (isOperatingValue) mValueNum = value else mOperatorNum = value
        }
    private val mSubscribers: LinkedBlockingQueue<CalculatorSubscriber> = LinkedBlockingQueue()
    private var mSavedOperator: CalculatorOperator = CalculatorOperator.None
    private var mCurrentlyOperator: CalculatorOperator = CalculatorOperator.None
    private var hasPoint: Boolean = false
    private var isOperatingValue: Boolean = true
    private var isAllClearMode: Boolean = true

    fun subscribe(s: CalculatorSubscriber) {
        mSubscribers.offer(s)
        s.onValueChanged(getDisplayNumber())
        s.onClearModeChanged(isAllClearMode)
    }

    fun unsubscribe(s: CalculatorSubscriber) {
        val iterator = mSubscribers.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == s) {
                iterator.remove()
                break
            }
        }
    }

    /**
     * number button clicked
     * @param number the number of clicked button.
     */
    fun number(number: BigDecimal) {
        var notifyClearModeChanged = false
        var notifyOperatorUsing = false
        if (isAllClearMode) {
            isAllClearMode = false
            notifyClearModeChanged = true
        }
        if (mCurrentlyOperator != CalculatorOperator.None) {
            mSavedOperator = mCurrentlyOperator
            mCurrentlyOperator = CalculatorOperator.None
            notifyOperatorUsing = true
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
        val displayNumber = getDisplayNumber()
        mSubscribers.forEach {
            if (notifyClearModeChanged) it.onClearModeChanged(isAllClearMode)
            if (notifyOperatorUsing) it.onOperatorUsing(mCurrentlyOperator)
            it.onValueChanged(displayNumber)
        }
    }

    /**
     * point
     */
    fun point() {
        hasPoint = true
        if (mCurrentlyOperator != CalculatorOperator.None) {
            val displayNumber = getDisplayNumber()
            mSubscribers.forEach {
                it.onValueChanged(displayNumber)
            }
        }
    }

    fun negate() {
        mOperatingNum = mOperatingNum.negate()
        val displayNumber = getDisplayNumber()
        mSubscribers.forEach {
            it.onValueChanged(displayNumber)
        }
    }

    fun backspace() {
        val currentlyScale = mOperatingNum.scale()
        mOperatingNum = if (currentlyScale > 0) mOperatingNum.setScale(currentlyScale - 1, RoundingMode.FLOOR) else mOperatingNum.divideToIntegralValue(BigDecimal(10))
        val displayNumber = getDisplayNumber()
        mSubscribers.forEach {
            it.onValueChanged(displayNumber)
        }
    }

    /**
     * reset
     */
    fun clear() {
        var notifyOperatorChanged = false
        var notifyClearModeChanged = false
        if (isAllClearMode) {
            notifyOperatorChanged = true
            mValueNum = BigDecimal(0)
            mOperatorNum = BigDecimal(0)
            mCurrentlyOperator = CalculatorOperator.None
            mSavedOperator = CalculatorOperator.None
            hasPoint = false
            isOperatingValue = true
        } else {
            notifyClearModeChanged = true
            mOperatingNum = BigDecimal(0)
            if (mSavedOperator != CalculatorOperator.None) {
                notifyOperatorChanged = true
                mCurrentlyOperator = mSavedOperator
            }
            hasPoint = false
            isAllClearMode = true
        }
        val displayNumber = getDisplayNumber()
        mSubscribers.forEach {
            it.onValueChanged(displayNumber)
            if (notifyOperatorChanged) it.onOperatorUsing(mCurrentlyOperator)
            if (notifyClearModeChanged) it.onClearModeChanged(isAllClearMode)
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
        mSubscribers.forEach {
            it.onOperatorUsing(mCurrentlyOperator)
        }
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
                    clear()
                    mSubscribers.forEach {
                        it.onClearModeChanged(isAllClearMode)
                        it.onValueChanged("Error")
                    }
                    return
                }
            }
            CalculatorOperator.Modulus -> {
                mValueNum = mValueNum.rem(mOperatorNum).stripTrailingZeros()
            }
            else -> {
                return
            }
        }
        if (mValueNum.compareTo(BigDecimal.ZERO) == 0)
            mValueNum = BigDecimal(0)
        mSavedOperator = CalculatorOperator.None
        mCurrentlyOperator = CalculatorOperator.None
        mOperatorNum = BigDecimal(0)
        isOperatingValue = true
        hasPoint = mOperatingNum.compareTo(BigDecimal(mOperatingNum.toBigInteger())) != 0
        val displayNumber = getDisplayNumber()
        mSubscribers.forEach {
            it.onValueChanged(displayNumber)
        }
    }

    fun percent() {
        if (mOperatingNum.compareTo(BigDecimal.ZERO) == 0) return
        mOperatingNum = mOperatingNum.movePointLeft(2).stripTrailingZeros()
        if (!hasPoint && mOperatingNum.compareTo(BigDecimal(mOperatingNum.toBigInteger())) != 0)
            hasPoint = true
        val displayNumber = getDisplayNumber()
        mSubscribers.forEach {
            it.onValueChanged(displayNumber)
        }
    }

    private fun getDisplayNumber(): String {
        return formatNumberStr(mOperatingNum.toPlainString(), if (mOperatingNum.signum() < 0) 1 else 0)
    }

    private fun formatNumberStr(numStr: String, offset: Int): String {
        var pointIndex = numStr.indexOf(".")
        if (pointIndex in 0..3 || numStr.length <= 3) return numStr
        if (pointIndex == -1)
            pointIndex = numStr.length
        val returnValue = StringBuilder(numStr)
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