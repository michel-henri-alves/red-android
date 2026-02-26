package com.m4.red_android

sealed class Screen(val route: String) {
    object Pos : Screen("pos")
    object Create : Screen("create")
    object Payment : Screen("payment")
    object Scanner : Screen("scanner")


}