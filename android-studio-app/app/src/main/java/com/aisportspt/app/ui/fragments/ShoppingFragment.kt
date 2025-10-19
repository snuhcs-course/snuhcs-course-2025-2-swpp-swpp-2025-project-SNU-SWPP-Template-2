package com.aisportspt.app.ui.fragments

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aisportspt.app.R
import com.aisportspt.app.ui.adapters.ShoppingAdapter
import com.aisportspt.app.databinding.FragmentShoppingBinding
import com.aisportspt.app.data.remote.RetrofitClient
import com.aisportspt.app.model.Category
import com.aisportspt.app.model.ShoppingItem
import kotlinx.coroutines.launch

class ShoppingFragment : Fragment() {

    private var _binding: FragmentShoppingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView → Grid 2열 배치
        binding.rvShoppingItems.layoutManager = GridLayoutManager(requireContext(), 2)

        // Adapter 연결 (Mock 데이터 예시)
//        binding.rvShoppingItems.adapter = ShoppingAdapter(
//            listOf(
//                ShoppingItem("입문용 골프채", "₩150,000", R.drawable.ic_logo, 3),
//                ShoppingItem("고급 골프공 세트", "₩45,000", R.drawable.ic_logo, 4),
//                ShoppingItem("골프화 (남성용)", "₩120,000", R.drawable.ic_logo, 2),
//                ShoppingItem("골프 장갑", "₩25,000", R.drawable.ic_logo, 5),
//            )
//        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getRecommendations()
                val items = response.recommendations.map { dto ->
                    ShoppingItem(
                        name = dto.name,
                        price = "₩${dto.price}",
                        level = dto.level,
                        imageRes = R.drawable.ic_logo // 이미지 URL이 있으면 Glide/Picasso로 바꾸면 됨
                    )
                }

                // RecyclerView 업데이트
                binding.rvShoppingItems.adapter = ShoppingAdapter(items)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "장비 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
