package com.example.librarytogether.feature.home
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.librarytogether.R
import com.example.librarytogether.databinding.FragmentHomeBinding
import com.example.librarytogether.feature.home.data.Post
import com.example.librarytogether.network.RetrofitClient
import com.example.librarytogether.feature.home.data.HomeApi
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val feedAdapter by lazy { FeedAdapter(
        FeedClicks(
            onClickReview =:: onClickReview,
            onClickExchange =:: onClickExchange,
            onClickAdd =:: onClickAdd,
            onClickMore =:: onClickMore,
            onClickProfile =:: onClickProfile,
            onClickUserName =:: onClickUserName,
            onClickTitle =:: onClickTitle,
            onClickAuthor =:: onClickAuthor,
            onClickContent =:: onClickContent,
        )
    ) }


    private val service: HomeApi by lazy {
        RetrofitClient.getClient(requireContext()).create(HomeApi::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            setHasFixedSize(true)
        }

        lifecycleScope.launch {
            val response = service.feed()
            if (response.isSuccessful) {
                val posts = response.body()?.results
                feedAdapter.submitList(posts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onClickReview(post: Post) {
        Toast.makeText(requireContext(), "서평", Toast.LENGTH_SHORT).show()
    }

    private fun onClickExchange(post: Post) {
        Toast.makeText(requireContext(), "교환", Toast.LENGTH_SHORT).show()
    }

    private fun onClickAdd(post: Post) {
        Toast.makeText(requireContext(), "찜하기", Toast.LENGTH_SHORT).show()
    }

    private fun onClickMore(post: Post) {
        Toast.makeText(requireContext(), "메뉴", Toast.LENGTH_SHORT).show()
    }

    private fun onClickProfile(post: Post) {
        Toast.makeText(requireContext(), "유저", Toast.LENGTH_SHORT).show()
    }

    private fun onClickUserName(post: Post) = onClickProfile(post)

    private fun onClickTitle(post: Post) {
        Toast.makeText(requireContext(), "제목", Toast.LENGTH_SHORT).show()
    }

    private fun onClickAuthor(post: Post) {
        Toast.makeText(requireContext(), "작가", Toast.LENGTH_SHORT).show()
    }

    private fun onClickContent(post: Post) {
        feedAdapter.toggleExpand(post.id)
    }
}