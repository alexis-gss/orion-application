package com.orion.app

import android.app.Application

class TestApplication : Application() {
    // onCreate() intentionally left empty: no ApiKeyStore, no WorkManager,
    // no NetworkModule. Room tests build AppDatabase themselves.
}