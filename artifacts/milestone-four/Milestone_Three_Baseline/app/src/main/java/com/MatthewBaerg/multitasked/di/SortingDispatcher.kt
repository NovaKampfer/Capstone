package com.MatthewBaerg.multitasked.di

import javax.inject.Qualifier

/** Distinguishes CPU-bound sorting work from any future I/O dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SortingDispatcher
