package lx.own.calculator.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import lx.own.calculator.R
import lx.own.calculator.core.CalculatorCore
import lx.own.calculator.utils.notNullVal
import lx.own.calculator.utils.ThemeManager
import java.math.BigDecimal
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private var display: TextView by Delegates.notNullVal()
    private var operatorViews: Array<TextView> by Delegates.notNullVal()
    private var ac: TextView by Delegates.notNullVal()
    private var operatorTextColors: IntArray by Delegates.notNullVal()
    private var operatorBackgrounds: IntArray by Delegates.notNullVal()
    private val subscriber: CalculatorCore.CalculatorSubscriber

    init {
        subscriber = object : CalculatorCore.CalculatorSubscriber {
            override fun onClearModeChanged(isAll: Boolean) {
                ac.text = if (isAll) "AC" else "C"
            }

            override fun onOperatorUsing(operator: CalculatorCore.CalculatorOperator) {
                operatorViews.forEach {
                    if (it.tag == operator) {
                        it.setBackgroundResource(operatorBackgrounds[1])
                        it.setTextColor(operatorTextColors[1])
                    } else {
                        it.setBackgroundResource(operatorBackgrounds[0])
                        it.setTextColor(operatorTextColors[0])
                    }
                }
            }

            override fun onValueChanged(value: String) {
                setSuitableSize(display, value)
                display.text = value
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.instance.getTheme().resId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        init()
        CalculatorCore.instance.subscribe(subscriber)
    }

    override fun onDestroy() {
        super.onDestroy()
        CalculatorCore.instance.unsubscribe(subscriber)
    }

    private fun bindViews() {
        display = findViewById(R.id.display)
        ac = findViewById(R.id.ac)
        operatorViews = arrayOf(division, multiplication, subtraction, addition)
    }

    private fun setSuitableSize(v: TextView, value: String) {
        val vWidth: Int = v.width
        when {
            (vWidth <= 0) -> {
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 90f)
            }
            else -> {
                var size = 90f
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
                var tWidth: Float = v.paint.measureText(value)
                while (tWidth > vWidth) {
                    size--
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
                    tWidth = v.paint.measureText(value)
                }
            }
        }
    }

    private fun init() {
        division.tag = CalculatorCore.CalculatorOperator.Division
        multiplication.tag = CalculatorCore.CalculatorOperator.Multiplication
        subtraction.tag = CalculatorCore.CalculatorOperator.Subtraction
        addition.tag = CalculatorCore.CalculatorOperator.Addition

        val attr = theme.obtainStyledAttributes(intArrayOf(R.attr.specialButton2TextColor, R.attr.specialButton2TextColorRevert, R.attr.specialButton2Background, R.attr.specialButton2BackgroundRevert))
        operatorTextColors = intArrayOf(attr.getColor(0, 0), attr.getColor(1, 0))
        operatorBackgrounds = intArrayOf(attr.getResourceId(2, 0), attr.getResourceId(3, 0))
        attr.recycle()
        num0.tag = BigDecimal.ZERO
        num1.tag = BigDecimal.ONE
        num2.tag = BigDecimal(2)
        num3.tag = BigDecimal(3)
        num4.tag = BigDecimal(4)
        num5.tag = BigDecimal(5)
        num6.tag = BigDecimal(6)
        num7.tag = BigDecimal(7)
        num8.tag = BigDecimal(8)
        num9.tag = BigDecimal(9)

        val l = InnerClickListener()
        num0.setOnClickListener(l)
        num1.setOnClickListener(l)
        num2.setOnClickListener(l)
        num3.setOnClickListener(l)
        num4.setOnClickListener(l)
        num5.setOnClickListener(l)
        num6.setOnClickListener(l)
        num7.setOnClickListener(l)
        num8.setOnClickListener(l)
        num9.setOnClickListener(l)
        point.setOnClickListener(l)
        ac.setOnClickListener(l)
        pm.setOnClickListener(l)
        backspace.setOnClickListener(l)
        division.setOnClickListener(l)
        multiplication.setOnClickListener(l)
        subtraction.setOnClickListener(l)
        addition.setOnClickListener(l)
        evaluate.setOnClickListener(l)
    }

    private inner class InnerClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.num0,
                R.id.num1,
                R.id.num2,
                R.id.num3,
                R.id.num4,
                R.id.num5,
                R.id.num6,
                R.id.num7,
                R.id.num8,
                R.id.num9 -> {
                    CalculatorCore.instance.number(v.tag as BigDecimal)
                }
                R.id.point -> {
                    CalculatorCore.instance.point()
                }
                R.id.ac -> {
                    CalculatorCore.instance.clear()
                }
                R.id.pm -> {
                    CalculatorCore.instance.negate()
                }
                R.id.backspace -> {
                    CalculatorCore.instance.backspace()
                }
                R.id.division,
                R.id.multiplication,
                R.id.subtraction,
                R.id.addition -> {
                    CalculatorCore.instance.operate(v.tag as CalculatorCore.CalculatorOperator)
                }
                R.id.evaluate -> {
                    CalculatorCore.instance.evaluate()
                }
            }
        }
    }
}
