package com.example.librarytogether.feature.library
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.librarytogether.R
import com.example.librarytogether.databinding.FragmentHomeBinding
import com.example.librarytogether.databinding.FragmentLibraryBinding
import com.example.librarytogether.feature.home.FeedAdapter
import com.example.librarytogether.feature.home.FeedClicks
import com.example.librarytogether.feature.home.data.HomeApi
import com.example.librarytogether.feature.library.LibraryViewModel
import com.example.librarytogether.network.RetrofitClient
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val viewModel: LibraryViewModel by viewModels()
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val reviewAdapter by lazy { ReviewAdapter(
    ) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (binding.contentTabs.checkedButtonId == View.NO_ID) { binding.contentTabs.check(R.id.tabReviews) }

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

    }

    private fun setupRecyclerView() {
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            when (binding.contentTabs.checkedButtonId) {
                R.id.tabReviews   -> {
                    WriteReviewSheet().show(childFragmentManager, "WriteReviewSheet")
                }
                R.id.tabBookshelf -> { /* TODO: 책 추가 화면 */ }
                R.id.tabProfile -> {}
            }
        }
    }

    private fun observeViewModel() {
        viewModel.myReviews.observe(viewLifecycleOwner) { reviews ->
            reviewAdapter.submitList(reviews)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}