package com.horizon.doodle

interface ResultListener {
    /**
     * This method will callback after getting result or task finished <br></br>
     * and before bitmap(or drawable)  return by callback or set to ImageView.
     *
     * @param success true if got the result, false otherwise
     */
    fun onResult(success: Boolean)
}
