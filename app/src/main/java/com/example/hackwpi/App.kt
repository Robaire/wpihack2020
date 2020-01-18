package com.example.hackwpi

import android.app.Application
import com.bose.wearable.BoseWearable

// The application class manages global state information
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the Bose Wearable Library
        BoseWearable.configure(this)
    }

}