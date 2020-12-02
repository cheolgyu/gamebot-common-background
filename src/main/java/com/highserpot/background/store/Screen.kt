package com.highserpot.background.store

data class Screen(val screenStatus: ScreenStatus) {
    val created: Long = System.currentTimeMillis()

}
