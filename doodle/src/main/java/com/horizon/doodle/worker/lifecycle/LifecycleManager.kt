package com.horizon.doodle.worker.lifecycle

import android.util.SparseArray


object LifecycleManager {
    private val holders = SparseArray<Holder>()

    /**
     * Register listener
     *
     * @param hostHash identityHashCode of host，host may be one of Activity, Fragment or Dialog.
     * @param listener generally UITask or Dialog (dismiss when activity destroy, in case of window leak）
     */
    @JvmStatic
    @Synchronized
    fun register(hostHash: Int, listener: LifeListener?) {
        if (hostHash == 0 || listener == null) {
            return
        }
        var holder: Holder? = holders.get(hostHash)
        if (holder == null) {
            holder = Holder()
            holders.put(hostHash, holder)
        }
        holder.add(listener)
    }

    @JvmStatic
    @Synchronized
    fun unregister(hostHash: Int, listener: LifeListener?) {
        if (hostHash == 0 || listener == null) {
            return
        }
        holders.get(hostHash)?.remove(listener)
    }

    @JvmStatic
    @Synchronized
    fun notify(host: Any?, event: Int) {
        if (host == null) {
            return
        }
        val hostHash = System.identityHashCode(host)
        val index = holders.indexOfKey(hostHash)
        if (index >= 0) {
            val holder = holders.valueAt(index)
            if (event == LifeEvent.DESTROY) {
                holders.removeAt(index)
            }
            holder.notify(event)
        }
    }
}
