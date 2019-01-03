package lx.own.calculator

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * <b> </b><br/>
 *
 * @author LeiXun
 * Created on 2019/1/3.
 */
fun <T : Any> Delegates.notNullVal(): ReadWriteProperty<Any?, T> = NotNullVal()

private class NotNullVal<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null    // 持有属性的值

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
                ?: throw IllegalStateException("Property ${property.name} should be initialized before get.")  // 如果属性的值为空，就会抛出异常
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (null == this.value) this.value = value
        else throw IllegalStateException("Property ${property.name} already initialized")   // 第二次赋值就抛出异常
    }
}