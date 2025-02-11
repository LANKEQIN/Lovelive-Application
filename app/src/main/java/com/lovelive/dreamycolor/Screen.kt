package com.lovelive.dreamycolor

import androidx.compose.ui.res.stringResource
import com.lovelive.dreamycolor.R

sealed class Screen(val route: String, val titleRes: Int) {
    object Exclusive : Screen("exclusive", R.string.navigation_exclusive)
    object Inspiration : Screen("inspiration", R.string.navigation_inspiration)
    object Encyclopedia : Screen("encyclopedia", R.string.navigation_encyclopedia)
    object Profile : Screen("profile", R.string.navigation_profile)
}
