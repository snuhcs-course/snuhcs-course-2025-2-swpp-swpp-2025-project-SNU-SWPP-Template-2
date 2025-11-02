package com.example.librarytogether.feature.onboarding.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.librarytogether.R
import com.example.librarytogether.databinding.FragmentOnboardingHostBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingHostFragment: Fragment(R.layout.fragment_onboarding_host) {

    private var _b: FragmentOnboardingHostBinding? = null
    private val b get() = _b!!
    private val vm: OnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentOnboardingHostBinding.bind(view)

        b.pager.isUserInputEnabled = false
        b.pager.adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(pos: Int) = when(pos) {
                0 -> OnboardingBooksFragment()
                1 -> OnboardingAuthorsFragment()
                else -> OnboardingGenresFragment()
            }
        }
        b.stepIndicator.progress = 1
    }

    fun goNext(stepIndex: Int) { // 0→1→2→완료
        if (stepIndex < 2) {
            b.pager.setCurrentItem(stepIndex + 1, true)
            b.stepIndicator.progress = stepIndex + 2
        } else {
            // 완료 후 메인으로 이동(또는 NavGraph popUpTo)
            requireActivity().finish() // 필요에 맞게 교체
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

