package lx.own.calculator.utils

import android.content.Context
import android.content.SharedPreferences
import lx.own.calculator.config.ThemeConfig
import kotlin.properties.Delegates

/**
 * <b> </b><br/>
 *
 * @author LeiXun
 * Created on 2019/1/4.
 */
class ThemeManager private constructor() {
    companion object {
        val instance: ThemeManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ThemeManager() }
    }

    private var isInitialed: Boolean = false
    private var mCurrentlyTheme: ThemeConfig by Delegates.notNull()
    private var mKey: String by Delegates.notNull()
    private var mSharedPreferences: SharedPreferences by Delegates.notNull()

    fun init(context: Context, spKey: String, themeKey: String) {
        mSharedPreferences = context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
        mKey = themeKey
        mCurrentlyTheme = ThemeConfig.look(mSharedPreferences.getInt(themeKey, 0))
        isInitialed = true
    }

    fun switchTheme(theme: ThemeConfig) {
        checkInitial()
        mCurrentlyTheme = theme
        mSharedPreferences.edit().putInt(mKey, theme.id).apply()
    }

    fun getTheme(): ThemeConfig {
        checkInitial()
        return mCurrentlyTheme
    }

    fun checkInitial() {
        if (!isInitialed) throw IllegalStateException("ThemeManager does not initialed before use.")
    }
}