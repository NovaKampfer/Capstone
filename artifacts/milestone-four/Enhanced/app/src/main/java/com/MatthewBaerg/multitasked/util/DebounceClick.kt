package com.MatthewBaerg.multitasked.util

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.debounceClick(onClick: () -> Unit): Modifier {
    val debounceState = remember { DebounceState() }
    return this.clickable {
        debounceState.onDebouncedClick(onClick)
    }
}

private class DebounceState {
    private var lastClickTime = 0L

    fun onDebouncedClick(onClick: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            onClick()
        }
        lastClickTime = now
    }
}
