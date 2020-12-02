package com.highserpot.background.store

object ScreenHistory {
    var history: MutableList<Screen> = mutableListOf()

    fun add(screen: Screen): Boolean {

        //chk
        if (history.isEmpty()) {
            history.add(screen)
            return true
        } else {
            var new = screen
            var chk = history.takeLast(1).let { last ->
                var old = last.get(0)
                if ((old.screenStatus == new.screenStatus &&
                                (old.created - new.created) / 1000 < 3) || old.screenStatus != new.screenStatus) {
                    history.add(screen)
                    return@let true
                }
                return@let false
            }

            return chk
        }

    }
}