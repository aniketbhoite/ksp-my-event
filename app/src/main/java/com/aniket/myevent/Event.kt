package com.aniket.myevent

import android.os.Bundle

interface Event {
    fun getHashMapOfParamsForCustomAnalytics(): HashMap<*, *>?

    fun getBundleOfParamsForFirebase(): Bundle
}