package com.example.librarytogether.feature.onboarding.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.librarytogether.databinding.FragmentOnboardingListBinding
import com.example.librarytogether.feature.onboarding.ui.adapter.ChipSelectableAdapter

class OnboardingAuthorsFragment: Fragment(R.layout.fragment_onboarding_list) {
    private var _b: FragmentOnboardingListBinding? = null
    private val b get() = _b!!
    private val vm: OnboardingViewModel by activityViewModels()
    private lateinit var adapter: ChipSelectableAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentOnboardingListBinding.bind(view)
        b.tvTitle.text = "Choose 3 or more authors you like"

        adapter = ChipSelectableAdapter { id, checked -> vm.toggleAuthor(id, checked) }
        b.rvChips.apply {
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = this@OnboardingAuthorsFragment.adapter
        }

        b.etSearch.setup(vm::searchAuthors)
        vm.authors.observe(viewLifecycleOwner) { list ->
            val items = list.map { ChipSelectableAdapter.Item(it.id, it.name) }
            adapter.submit(items, adapter.selectedIds.toSet())
        }
        vm.canProceedAuthors.observe(viewLifecycleOwner) { b.btnConfirm.isEnabled = it }

        b.btnConfirm.setOnClickListener {
            (parentFragment as? OnboardingHostFragment)?.goNext(1)
        }

        vm.searchAuthors(null)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
