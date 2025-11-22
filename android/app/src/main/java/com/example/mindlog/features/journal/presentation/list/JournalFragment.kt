package com.example.mindlog.features.journal.presentation.list

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentJournalListBinding
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
// HomeActivity의 FAB 클릭 이벤트를 받기 위해 인터페이스 구현
class JournalFragment : Fragment(), HomeActivity.FabClickListener {

    private var _binding: FragmentJournalListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // 새 글 작성 후 또는 검색 조건 변경 후 맨 위로 스크롤하기 위한 플래그
    private var scrollToTopOnNextSubmit = false

    // HomeActivity의 FAB(작성 버튼)가 클릭되면 이 메서드가 호출됨
    override fun onFabClick() {
        val intent = Intent(requireContext(), JournalWriteActivity::class.java)
        // 결과를 돌려받기 위해 반드시 launcher로 액티비티를 시작
        activityResultLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면이 생성될 때 ActivityResultLauncher를 등록
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 다른 화면에서 돌아왔을 때, 결과 코드가 OK인 경우에만 처리
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val updatedId = data?.getIntExtra(JournalDetailActivity.EXTRA_UPDATED_JOURNAL_ID, -1) ?: -1
                val deletedId = data?.getIntExtra(JournalDetailActivity.EXTRA_DELETED_JOURNAL_ID, -1) ?: -1

                when {
                    // 케이스 1: 새 글 작성 후 메인으로 (Intent에 ID가 없음)
                    updatedId == -1 && deletedId == -1 -> {
                        scrollToTopOnNextSubmit = true // 다음 목록 업데이트 시 맨 위로 스크롤하도록 플래그 설정
                        viewModel.clearSearchAndReload() // 검색 조건 초기화 후 `journal/me` API 호출
                    }
                    // 케이스 3: 수정 또는 삭제 후 메인으로 (Intent에 ID가 있음)
                    else -> {
                        // 스크롤 위치 유지를 위해 특정 아이템만 업데이트/삭제
                        viewModel.updateOrRemoveJournalEntry(
                            updatedId = if (updatedId == -1) null else updatedId,
                            deletedId = if (deletedId == -1) null else deletedId
                        )
                    }
                }
            }
            // 케이스 2: 단순 뒤로가기, 작성 취소 등 (RESULT_OK가 아님)
            // -> 아무 작업도 하지 않으므로 스크롤 위치가 자동으로 유지됨
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWeeklyCalendar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        viewModel.startObservingSearchQuery()

        viewModel.loadJournals()
    }

    private fun setupClickListeners() {
        val topBar = binding.topBarLayout

        topBar.btnSearch.setOnClickListener {
            toggleSearchView(true)
        }

        topBar.btnSearchClose.setOnClickListener {
            toggleSearchView(false)
            if (viewModel.searchQuery.value?.isNotEmpty() == true) {
                viewModel.searchQuery.value = ""
                scrollToTopOnNextSubmit = true
                viewModel.clearSearchConditions()
                viewModel.loadJournals()
            }
        }

        topBar.btnDateRangeClose.setOnClickListener {
            val rootLayout = topBar.homeTopRoot
            val originalTransition = rootLayout.layoutTransition
            rootLayout.layoutTransition = null

            topBar.dateRangeBarContainer.visibility = View.GONE
            scrollToTopOnNextSubmit = true
            viewModel.setDateRange(null, null)
            viewModel.loadJournals()

            rootLayout.post {
                rootLayout.layoutTransition = originalTransition
            }
        }

        topBar.tvToday.setOnClickListener {
            showDateRangePicker()
        }

        topBar.etSearch.addTextChangedListener { text ->
            viewModel.searchQuery.value = text?.toString()
        }

        topBar.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                scrollToTopOnNextSubmit = true
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(topBar.etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun toggleSearchView(isSearchVisible: Boolean) {
        binding.topBarLayout.apply {
            if (isSearchVisible) {
                normalTopBar.visibility = View.GONE
                searchBarContainer.visibility = View.VISIBLE

                etSearch.requestFocus()
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
            } else {
                searchBarContainer.visibility = View.GONE
                normalTopBar.visibility = View.VISIBLE

                etSearch.text.clear()
                etSearch.clearFocus()
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)
            }
        }
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("기간으로 검색")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            val startDate = Date(selection.first)
            val endDate = Date(selection.second)

            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
            val chipFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)

            scrollToTopOnNextSubmit = true
            viewModel.setDateRange(apiFormat.format(startDate), apiFormat.format(endDate))
            viewModel.loadJournals()

            binding.topBarLayout.tvDateRange.text =
                "${chipFormat.format(startDate)} ~ ${chipFormat.format(endDate)}"

            val rootLayout = binding.topBarLayout.homeTopRoot
            val originalTransition = rootLayout.layoutTransition
            rootLayout.layoutTransition = null

            binding.topBarLayout.dateRangeBarContainer.visibility = View.VISIBLE

            rootLayout.post {
                rootLayout.layoutTransition = originalTransition
            }
        }

        dateRangePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupWeeklyCalendar() {
        val calendar = Calendar.getInstance()
        val todayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)
        binding.topBarLayout.tvToday.text = todayDateFormat.format(calendar.time)
    }


    private fun setupRecyclerView() {
        // 어댑터 아이템 클릭 시, Launcher를 통해 DetailActivity 실행
        journalAdapter = JournalAdapter { journalId ->
            val intent = Intent(requireContext(), JournalDetailActivity::class.java).apply {
                putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journalId)
            }
            activityResultLauncher.launch(intent)
        }
        linearLayoutManager = LinearLayoutManager(requireContext())
        binding.rvDiaryFeed.apply {
            layoutManager = linearLayoutManager
            adapter = journalAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        val lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition()
                        val totalItemCount = linearLayoutManager.itemCount
                        if (viewModel.isLoading.value == false && !viewModel.isLastPage) {
                            if (lastVisibleItemPosition >= totalItemCount - 3) {
                                viewModel.loadMoreJournals()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.journals.observe(viewLifecycleOwner, Observer { journalList ->
            // submitList의 콜백에서 플래그를 확인하고 스크롤 처리
            journalAdapter.submitList(journalList.toList()) {
                if (scrollToTopOnNextSubmit) {
                    binding.rvDiaryFeed.post {
                        linearLayoutManager.scrollToPositionWithOffset(0, 0)
                    }
                    // 플래그를 다시 false로 바꿔서, 다음번 업데이트 시에는 실행되지 않도록 함
                    scrollToTopOnNextSubmit = false
                }
            }
        })
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 로딩 처리
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
