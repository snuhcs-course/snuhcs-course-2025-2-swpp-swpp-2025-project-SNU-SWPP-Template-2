package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.aisportspt.app.MainActivity
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentSportsBinding
import com.aisportspt.app.model.Sport
import com.aisportspt.app.ui.adapters.SportsAdapter

class SportsFragment : Fragment() {

    private var _binding: FragmentSportsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sportsAdapter: SportsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        setupEmptyState()
    }

    private fun setupRecyclerView() {
        // TODO:
    }

    private fun observeViewModel() {
        val viewModel = (requireActivity() as MainActivity).getViewModel()
        
        viewModel.sports.observe(viewLifecycleOwner, Observer { sports ->
            updateUI(sports)
        })
    }

    private fun setupEmptyState() {
        // TODO:
    }

    private fun updateUI(sports: List<Sport>) {
        if (sports.isEmpty()) {
            binding.recyclerViewSports.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewSports.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
            sportsAdapter.updateSports(sports)
        }
    }

    private fun showSportDetails(sport: Sport) {
        // TODO: Navigate to SportDetailFragment or Activity
        // For now, we can show a simple dialog or start a new activity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}