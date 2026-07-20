package com.atakolstudio.sure

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SURE uygulamasının giriş noktası.
 * Hilt Dependency Injection grafiğini burada başlatıyoruz.
 */
@HiltAndroidApp
class SureApplication : Application()
