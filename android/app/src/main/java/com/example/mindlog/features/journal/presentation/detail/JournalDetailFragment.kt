package com.example.mindlog.features.journal.presentation.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.model.Emotion
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.databinding.FragmentJournalDetailBinding
import com.example.mindlog.features.journal.presentation.write.JournalEditViewModel
import com.example.mindlog.features.journal.presentation.list.JournalKeywordSearchActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class JournalDetailFragment : Fragment() {

    private var _binding: FragmentJournalDetailBinding? = null
    private val binding get() = _binding!!

    // Activity와 ViewModel 공유
    private val viewModel: JournalEditViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        // 일기 데이터(제목, 내용 등) 관찰
        viewModel.journalState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val journal = result.data
                    binding.tvTitle.text = journal.title
                    binding.tvContent.text = journal.content
                    binding.tvGratitude.text = journal.gratitude

                    // 날짜 형식 변환 (JournalEntry는 이미 Date 객체를 가지고 있음)
                    val displayFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)
                    binding.tvDate.text = displayFormat.format(journal.createdAt)

                    // ✨ [수정] 이미지 표시: 완성된 imageUrl을 바로 사용
                    if (!journal.imageUrl.isNullOrBlank()) {
                        binding.ivImage.isVisible = true
                        Glide.with(this).load(journal.imageUrl).into(binding.ivImage)
                    } else {
                        binding.ivImage.isVisible = false
                    }
                }
                is Result.Error -> {
                    Toast.makeText(context, "데이터를 불러오는 데 실패했습니다: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.emotions.observe(viewLifecycleOwner) { emotions ->
            updateEmotionsUI(emotions)
        }

        // 키워드 데이터 관찰
        viewModel.keywords.observe(viewLifecycleOwner) { keywords ->
            updateKeywordsUI(keywords)
        }
    }

    private fun updateEmotionsUI(emotions: List<Emotion>) {
        val filteredEmotions = emotions
            .filter { it.intensity >= 3 }
            .sortedByDescending { it.intensity }

        binding.flexboxEmotions.isVisible = filteredEmotions.isNotEmpty()
        binding.flexboxEmotions.removeAllViews()

        if (filteredEmotions.isNotEmpty()) {
            filteredEmotions.forEach { emotion ->
                val emotionView = createEmotionView(emotion)
                binding.flexboxEmotions.addView(emotionView)
            }
        }
    }

    private fun createEmotionView(emotion: Emotion): TextView {
        val emotionView = layoutInflater.inflate(R.layout.item_keyword_chip, binding.flexboxEmotions, false) as TextView
        emotionView.text = getKoreanEmotionName(emotion.emotion)
        emotionView.setTextColor(getEmotionColor(emotion.emotion, emotion.intensity))

        if (emotion.intensity == 4) {
            emotionView.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            emotionView.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        return emotionView
    }

    private fun getKoreanEmotionName(apiName: String): String {
        return when (apiName) {
            "happy" -> "행복"
            "sad" -> "슬픔"
            "anxious" -> "불안"
            "calm" -> "편안"
            "annoyed" -> "짜증"
            "satisfied" -> "만족"
            "bored" -> "지루함"
            "interested" -> "흥미"
            "lethargic" -> "무기력"
            "energetic" -> "활력"
            else -> apiName
        }
    }

    private fun getEmotionColor(emotionName: String, intensity: Int): Int {
        val colorResId = when (emotionName.lowercase()) {
            "happy" -> R.color.emotion_happy
            "sad" -> R.color.emotion_sad
            "anxious" -> R.color.emotion_anxious
            "calm" -> R.color.emotion_calm
            "annoyed" -> R.color.emotion_annoyed
            "satisfied" -> R.color.emotion_satisfied
            "bored" -> R.color.emotion_bored
            "interested" -> R.color.emotion_interested
            "lethargic" -> R.color.emotion_lethargic
            "energetic" -> R.color.emotion_energetic
            else -> R.color.text_secondary
        }
        val targetColor = ContextCompat.getColor(requireContext(), colorResId)

        if (intensity == 3) {
            val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
            return interpolateColor(targetColor, whiteColor, 0.5f)
        }
        return targetColor
    }

    private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        val a = (android.graphics.Color.alpha(color1) * inverseRatio + android.graphics.Color.alpha(color2) * ratio).toInt()
        val r = (android.graphics.Color.red(color1) * inverseRatio + android.graphics.Color.red(color2) * ratio).toInt()
        val g = (android.graphics.Color.green(color1) * inverseRatio + android.graphics.Color.green(color2) * ratio).toInt()
        val b = (android.graphics.Color.blue(color1) * inverseRatio + android.graphics.Color.blue(color2) * ratio).toInt()
        return android.graphics.Color.argb(a, r, g, b)
    }

    private fun updateKeywordsUI(keywords: List<Keyword>) {
        val hasKeywords = keywords.isNotEmpty()
        binding.flexboxKeywords.isVisible = hasKeywords
        binding.flexboxKeywords.removeAllViews()

        if (hasKeywords) {
            keywords.forEach { keyword ->
                val keywordChip = createKeywordChip(keyword)
                binding.flexboxKeywords.addView(keywordChip)
            }
        }
    }

    private fun createKeywordChip(keyword: Keyword): View {
        val keywordView = layoutInflater.inflate(R.layout.item_keyword_chip, binding.flexboxKeywords, false) as android.widget.TextView
        keywordView.text = "#${keyword.keyword}"
        keywordView.setOnClickListener {
            val intent = Intent(requireContext(), JournalKeywordSearchActivity::class.java).apply {
                putExtra(JournalKeywordSearchActivity.EXTRA_KEYWORD, keyword.keyword)
            }
            startActivity(intent)
        }
        return keywordView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
