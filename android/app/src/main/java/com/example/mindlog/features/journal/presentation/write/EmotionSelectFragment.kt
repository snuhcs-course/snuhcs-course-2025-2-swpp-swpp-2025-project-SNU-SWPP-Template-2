package com.example.mindlog.features.journal.presentation.write

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentEmotionSelectBinding
import com.example.mindlog.databinding.ItemEmotionRowBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmotionSelectFragment : Fragment() {

    private var _binding: FragmentEmotionSelectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalWriteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmotionSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAndSetupRow(binding.rowSadHappy, "슬픔", "sad", "행복", "happy", R.color.emotion_sad, R.color.emotion_happy)
        observeAndSetupRow(binding.rowAnxiousConfident, "불안", "anxious", "만족", "satisfied", R.color.emotion_anxious, R.color.emotion_satisfied)
        observeAndSetupRow(binding.rowAngryCalm, "짜증", "annoyed", "편안", "calm", R.color.emotion_annoyed, R.color.emotion_calm)
        observeAndSetupRow(binding.rowBoredExcited, "지루함", "bored", "흥미", "interested", R.color.emotion_bored, R.color.emotion_interested)
        observeAndSetupRow(binding.rowLethargicEnergetic, "무기력", "lethargic", "활력", "energetic", R.color.emotion_lethargic, R.color.emotion_energetic)
    }

    private fun observeAndSetupRow(
        rowBinding: ItemEmotionRowBinding,
        leftText: String, leftApiName: String,
        rightText: String, rightApiName: String,
        leftColorRes: Int, rightColorRes: Int
    ) {
        // --- 1. UI 겉모습 설정 ---
        val leftColor = ContextCompat.getColor(requireContext(), leftColorRes)
        val rightColor = ContextCompat.getColor(requireContext(), rightColorRes)
        val centerGrayColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        rowBinding.tvEmotionLeft.text = leftText
        rowBinding.tvEmotionLeft.setTextColor(leftColor)
        rowBinding.tvEmotionRight.text = rightText
        rowBinding.tvEmotionRight.setTextColor(rightColor)

        rowBinding.rbLeft.backgroundTintList = ColorStateList.valueOf(leftColor)
        rowBinding.rbCenter.backgroundTintList = ColorStateList.valueOf(centerGrayColor)
        rowBinding.rbRight.backgroundTintList = ColorStateList.valueOf(rightColor)
        rowBinding.rbMidLeft.backgroundTintList = ColorStateList.valueOf(interpolateColor(leftColor, centerGrayColor, 0.5f))
        rowBinding.rbMidRight.backgroundTintList = ColorStateList.valueOf(interpolateColor(centerGrayColor, rightColor, 0.5f))

        // --- 2. 상태 관찰 및 UI 복원 (안전한 코루틴 내에서) ---
        viewLifecycleOwner.lifecycleScope.launch {
            // Fragment의 생명주기를 따르는 안전한 실행 블록
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ViewModel의 전체 scores 맵에서 이 줄에 해당하는 '왼쪽 감정'의 점수만 관찰합니다.
                viewModel.emotionScores
                    .map { it[leftApiName] } // 전체 맵에서 내 점수만 추출
                    .distinctUntilChanged() // 이전 값과 같으면 무시 (불필요한 UI 갱신 방지)
                    .collect { scoreLeft -> // scoreLeft는 Int? 타입 (null일 수 있음)
                        // 리스너를 잠시 null로 설정하여 UI 변경 중 이벤트가 발생하지 않도록 합니다.
                        rowBinding.radioGroupEmotion.setOnCheckedChangeListener(null)

                        val buttonIdToCheck = when (scoreLeft) {
                            4 -> R.id.rb_left
                            3 -> R.id.rb_mid_left
                            2 -> R.id.rb_center
                            1 -> R.id.rb_mid_right
                            0 -> R.id.rb_right
                            else -> -1
                        }

                        if (buttonIdToCheck != -1) {
                            rowBinding.radioGroupEmotion.check(buttonIdToCheck)
                        } else {
                            rowBinding.radioGroupEmotion.clearCheck() // 처음 진입 시 또는 값이 없을 때 선택 해제
                        }

                        // --- 3. 사용자 입력 리스너 다시 설정 ---
                        rowBinding.radioGroupEmotion.setOnCheckedChangeListener { _, checkedId ->
                            val newScoreLeft = when (checkedId) {
                                R.id.rb_left -> 4
                                R.id.rb_mid_left -> 3
                                R.id.rb_center -> 2
                                R.id.rb_mid_right -> 1
                                R.id.rb_right -> 0
                                else -> return@setOnCheckedChangeListener
                            }
                            val newScoreRight = 4 - newScoreLeft
                            viewModel.updateEmotionScore(leftApiName, newScoreLeft)
                            viewModel.updateEmotionScore(rightApiName, newScoreRight)
                        }
                    }
            }
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
