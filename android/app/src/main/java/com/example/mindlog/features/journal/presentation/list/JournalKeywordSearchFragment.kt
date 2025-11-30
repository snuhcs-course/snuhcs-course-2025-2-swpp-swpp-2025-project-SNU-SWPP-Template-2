package com.example.mindlog.features.journal.presentation.list

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.databinding.FragmentJournalKeywordSearchBinding
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JournalKeywordSearchFragment : Fragment() {

    private var _binding: FragmentJournalKeywordSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalKeywordSearchViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    // DetailActivity에서 돌아왔을 때 목록을 갱신하기 위한 Launcher
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 수정 또는 삭제가 발생했을 수 있으므로 목록을 새로고침
            val keyword = arguments?.getString(ARG_KEYWORD)
            if (!keyword.isNullOrBlank()) {
                viewModel.loadJournals(keyword)
            }
        }
    }

    companion object {
        private const val ARG_KEYWORD = "keyword"
        fun newInstance(keyword: String) = JournalKeywordSearchFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_KEYWORD, keyword)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalKeywordSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        val keyword = arguments?.getString(ARG_KEYWORD)
        if (!keyword.isNullOrBlank()) {
            viewModel.loadJournals(keyword)
        } else {
            Toast.makeText(requireContext(), "검색할 키워드가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter { journalId ->
            val intent = Intent(requireContext(), JournalDetailActivity::class.java).apply {
                putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journalId)
            }
            detailLauncher.launch(intent)
        }
        binding.rvKeywordSearchResults.apply {
            adapter = journalAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount
                    if (dy > 0 && !viewModel.isLoading.value!! && !viewModel.isLastPage) {
                        if (lastVisibleItemPosition >= totalItemCount - 3) {
                            viewModel.loadMoreJournals()
                        }
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.journals.observe(viewLifecycleOwner) { journals ->
            journalAdapter.submitList(journals)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
