package com.jian.relive.core.common.navigation

/**
 * 跨模块全局导航解耦契约。
 */
interface AppNavigator {

    /**
     * 导航至指定的目标路由。
     *
     * @param route 类型安全的路由对象。
     */
    fun navigate(route: Any)

    /**
     * 弹出当前回退栈顶部的页面。
     *
     * @return 若成功弹出则返回 true，否则返回 false。
     */
    fun popBackStack(): Boolean

    /**
     * 向上导航一层。
     *
     * @return 若成功向上导航则返回 true，否则返回 false。
     */
    fun navigateUp(): Boolean
}
