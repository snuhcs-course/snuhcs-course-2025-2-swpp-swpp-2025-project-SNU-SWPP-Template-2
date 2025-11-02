package com.example.librarytogether.feature.onboarding.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.librarytogether.databinding.FragmentOnboardingListBinding
import com.example.librarytogether.feature.onboarding.ui.adapter.ChipSelectableAdapter

class OnboardingGenresFragment: Fragment(R.layout.fragment_onboarding_list) {
    private var _b: FragmentOnboardingListBinding? = null
    private val b get() = _b!!
    private val vm: OnboardingViewModel by activityViewModels()
    private lateinit var adapter: ChipSelectableAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentOnboardingListBinding.bind(view)
        b.tvTitle.text = "Choose 3 or more genres you like"

        adapter = ChipSelectableAdapter { id, checked -> vm.toggleGenre(id, checked) }
        b.rvChips.apply {
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = this@OnboardingGenresFragment.adapter
        }

        b.etSearch.setup(vm::searchGenres)
        vm.genres.observe(viewLifecycleOwner) { list ->
            val items = list.map { ChipSelectableAdapter.Item(it.id, it.name) }
            adapter.submit(items, adapter.selectedIds.toSet())
        }
        vm.canProceedGenres.observe(viewLifecycleOwner) { b.btnConfirm.isEnabled = it }

        b.btnConfirm.setOnClickListener {
            // 최종 제출
            lifecycleScope.launchWhenStarted {
                b.btnConfirm.isEnabled = false
                val ok = vm.submit()
                if (ok) {
                    // 메인으로 이동 or NavGraph 전환
                    (parentFragment as? OnboardingHostFragment)?.goNext(2)
                } else {
                    b.btnConfirm.isEnabled = true
                    // 토스트 등 에러 핸들
                }
            }
        }

        vm.searchGenres(null)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

