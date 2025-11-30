package com.example.mindlog.features.selfaware.presentation.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentSelfAwareHistoryBinding
import com.example.mindlog.features.selfaware.presentation.adapter.SelfAwareHistoryAdapter
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareHistoryViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@AndroidEntryPoint
class SelfAwareHistoryFragment : Fragment(R.layout.fragment_self_aware_history) {

    private var _binding: FragmentSelfAwareHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelfAwareHistoryViewModel by viewModels()
    private val adapter by lazy { SelfAwareHistoryAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareHistoryBinding.bind(view)

        // Toolbar: 뒤로가기
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // RecyclerView
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        // 무한 스크롤(바닥 도달 시 다음 페이지 로드)
        binding.recyclerHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private fun tryLoadNext(rv: RecyclerView) {
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val s = viewModel.state.value
                if (s.isLoading) return
                val atBottom = !rv.canScrollVertically(1)
                val last = lm.findLastVisibleItemPosition()
                val total = lm.itemCount
                val nearBottom = last >= total - 2
                Log.d("scroll", "atBottom: $atBottom, nearBottom: $nearBottom")
                if (atBottom || nearBottom) {
                    Log.d("scroll", "load more")
                    viewModel.loadNext()
                }
            }
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                tryLoadNext(rv)
            }
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    tryLoadNext(rv)
                }
            }
        })

        // 초기 로드
        viewModel.refresh()

        // 상태 구독
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    adapter.submitList(s.items.toList())
                    binding.recyclerHistory.isEnabled = s.items.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}