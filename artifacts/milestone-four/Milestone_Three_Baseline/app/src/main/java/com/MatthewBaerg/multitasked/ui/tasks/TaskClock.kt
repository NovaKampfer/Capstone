package com.MatthewBaerg.multitasked.ui.tasks

/** Inject a fixed clock in tests; production uses System.currentTimeMillis. */
fun interface TaskClock {
    fun nowMillis(): Long
}
