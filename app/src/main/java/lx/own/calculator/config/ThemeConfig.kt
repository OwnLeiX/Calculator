package lx.own.calculator.config

import lx.own.calculator.R

/**
 * <b> </b><br/>
 *
 * @author LeiXun
 * Created on 2019/1/4.
 */
enum class ThemeConfig(val id: Int, val resId: Int) {
    Black(0, R.style.blackTheme);


    companion object {
        fun look(id: Int): ThemeConfig {
            ThemeConfig.values().forEach {
                if (id == it.id)
                    return it
            }
            return Black
        }
    }
}