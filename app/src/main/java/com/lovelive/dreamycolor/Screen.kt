/**
 * 应用程序的导航屏幕定义
 * 
 * 这个密封类定义了应用程序的主要导航结构，每个对象代表一个主要导航目标。
 * 每个导航项都关联了一个字符串资源ID，用于显示导航标题。
 */
package com.lovelive.dreamycolor


sealed class Screen(val titleRes: Int) {
    /**
     * 专属页面
     * 
     */
    data object Exclusive : Screen(R.string.navigation_exclusive)

    /**
     * 灵感页面
     * 
     */
    data object Inspiration : Screen(R.string.navigation_inspiration)

    /**
     * 百科页面
     * 
     */
    data object Encyclopedia : Screen(R.string.navigation_encyclopedia)

    /**
     * 个人资料页面
     * 
     */
    data object Profile : Screen(R.string.navigation_profile)
}
