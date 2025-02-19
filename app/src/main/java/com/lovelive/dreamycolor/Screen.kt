package com.lovelive.dreamycolor


sealed class Screen(val titleRes: Int) {
    data object Exclusive : Screen(R.string.navigation_exclusive)
    data object Inspiration : Screen(R.string.navigation_inspiration)
    data object Encyclopedia : Screen(R.string.navigation_encyclopedia)
    data object Profile : Screen(R.string.navigation_profile)
}
