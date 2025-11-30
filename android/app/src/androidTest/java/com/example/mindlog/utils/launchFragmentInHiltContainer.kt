package com.example.mindlog.utils

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.mindlog.HiltTestActivity
import com.example.mindlog.R

inline fun <reified F : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.Theme_MindLog,
    fragmentTag: String = "TEST",
    noinline factory: (() -> F)? = null,
    crossinline action: F.() -> Unit = {}
) {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(context, HiltTestActivity::class.java)
    ).putExtra("theme", themeResId)

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        activity.setTheme(themeResId)

        val fragment: F = if (factory != null) {
            factory.invoke()
        } else {
            activity.supportFragmentManager.fragmentFactory.instantiate(
                F::class.java.classLoader!!,
                F::class.java.name
            ) as F
        }

        if (fragmentArgs != null) {
            fragment.arguments = (fragment.arguments ?: Bundle()).apply {
                putAll(fragmentArgs)
            }
        }

        activity.supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, fragment, fragmentTag)
            .commitNow()

        fragment.action()
    }
}