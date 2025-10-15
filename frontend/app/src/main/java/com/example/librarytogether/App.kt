package com.example.librarytogether

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, BuildConfig.KAKAO_API_KEY)
    }
}