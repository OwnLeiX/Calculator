package lx.own.calculator

import android.app.Application
import lx.own.calculator.config.Keys
import lx.own.calculator.utils.ThemeManager

/**
 * <b> </b><br/>
 *
 * @author LeiXun
 * Created on 2019/1/4.
 */
class OwnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.instance.init(this, Keys.SharePreference.app, Keys.Theme.theme)
    }
}