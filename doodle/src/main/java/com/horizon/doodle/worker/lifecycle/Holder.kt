package com.horizon.doodle.worker.lifecycle

import java.lang.ref.WeakReference
import java.util.*

internal class Holder {
    private val listeners = LinkedList<WeakReference<LifeListener>>()
    private var hidden = false

    fun add(listener: LifeListener) {
        var contain = false
        for (reference in listeners) {
            if (reference.get() === listener) {
                contain = true
                break
            }
        }
        if (!contain) {
            listeners.add(WeakReference(listener))
        }
    }

    fun remove(listener: LifeListener) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val reference = iterator.next()
            val target = reference.get()
            if (target === null || target === listener) {
                iterator.remove()
            }
        }
    }

    fun notify(event: Int) {
        if (event == LifeEvent.DESTROY) {
            dispatch(event)
        } else if (event == LifeEvent.SHOW) {
            if (hidden) {
                hidden = false
                dispatch(event)
            }
        } else if (event == LifeEvent.HIDE) {
            if (!hidden) {
                hidden = true
                dispatch(event)
            }
        }
    }

    private fun dispatch(event: Int) {
        for (reference in listeners) {
            reference.get()?.onEvent(event)
        }
    }
}
