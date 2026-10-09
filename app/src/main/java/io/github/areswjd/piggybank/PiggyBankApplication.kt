package io.github.areswjd.piggybank

import android.app.Application

class PiggyBankApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
