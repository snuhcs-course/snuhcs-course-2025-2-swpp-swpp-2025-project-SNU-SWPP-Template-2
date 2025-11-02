package com.example.librarytogether.feature.onboarding.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.librarytogether.R
import com.example.librarytogether.databinding.FragmentOnboardingListBinding
import com.example.librarytogether.feature.onboarding.ui.adapter.ChipSelectableAdapter
import com.google.android.flexbox.*

class OnboardingBooksFragment: Fragment(R.layout.fragment_onboarding_list) {
    private var _b: FragmentOnboardingListBinding? = null
    private val b get() = _b!!
    private val vm: OnboardingViewModel by activityViewModels()
    private lateinit var adapter: ChipSelectableAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentOnboardingListBinding.bind(view)
        b.tvTitle.text = "Choose 3 or more books you like"

        adapter = ChipSelectableAdapter { id, checked -> vm.toggleBook(id, checked) }
        b.rvChips.apply {
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = this@OnboardingBooksFragment.adapter
        }

        b.etSearch.setup(vm::searchBooks)
        vm.books.observe(viewLifecycleOwner) { list ->
            val items = list.map { ChipSelectableAdapter.Item(it.id, it.title) }
            adapter.submit(items, adapter.selectedIds.toSet())
        }
        vm.canProceedBooks.observe(viewLifecycleOwner) { b.btnConfirm.isEnabled = it }

        b.btnConfirm.setOnClickListener {
            (parentFragment as? OnboardingHostFragment)?.goNext(0)
        }

        vm.searchBooks(null) // 첫 로드
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
private fun EditText.setup(search: (String?) -> Unit) {
    doOnTextChanged { text, _, _, _ -> search(text?.toString()) }
}

