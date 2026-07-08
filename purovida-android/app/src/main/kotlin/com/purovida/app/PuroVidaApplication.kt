package com.purovida.app

import android.app.Application
import com.purovida.app.data.HumidorStore
import com.purovida.app.data.JournalStore
import com.purovida.app.data.UserStore
import com.purovida.core.CigarApi

class PuroVidaApplication : Application() {

    lateinit var journalStore: JournalStore
        private set
    lateinit var humidorStore: HumidorStore
        private set
    lateinit var userStore: UserStore
        private set
    lateinit var cigarApi: CigarApi
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("puro_vida", MODE_PRIVATE)
        journalStore = JournalStore(prefs)
        humidorStore = HumidorStore(prefs)
        userStore = UserStore(prefs)
        cigarApi = CigarApi(BuildConfig.API_BASE)
    }
}
