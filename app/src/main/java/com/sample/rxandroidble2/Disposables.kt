package com.sample.rxandroidble2

import io.reactivex.disposables.Disposable

class Disposables {

    private val ongoing = hashMapOf<Int, Disposable>()

    val size: Int get() = ongoing.size

    fun release() = ongoing.apply {
        forEach { it.value.disposeIfNeeded() }
        clear()
    }

    fun remove(id: Int) = ongoing.remove(id)?.disposeIfNeeded()

    fun add(id: Int, disposable: Disposable) = ongoing.put(id, disposable)?.disposeIfNeeded()

}

private fun Disposable.disposeIfNeeded() {
    if (isDisposed.not()) {
        dispose()
    }
}