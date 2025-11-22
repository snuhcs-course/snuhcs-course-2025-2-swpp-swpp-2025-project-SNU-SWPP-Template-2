package com.example.mindlog.features.journal.presentation.write

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mindlog.R
import com.example.mindlog.databinding.DialogAiStyleSelectorBinding
import com.example.mindlog.databinding.FragmentContentWriteBinding
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContentWriteFragment : Fragment() {

    private var _binding: FragmentContentWriteBinding? = null
    private val binding get() = _binding!!

    // Activity와 ViewModel 공유
    private val writeViewModel: JournalWriteViewModel by activityViewModels()
    private val editViewModel: JournalEditViewModel by activityViewModels()

    // 갤러리 런처
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            when (activity) {
                is JournalWriteActivity -> writeViewModel.setGalleryImageUri(uri)
                is JournalEditActivity -> editViewModel.setGalleryImageUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✨ [핵심 수정 1] 모드에 따라 데이터 관찰과 UI 설정을 올바른 순서로 호출
        when (activity) {
            is JournalDetailActivity -> {
                // 상세 모드: 데이터를 먼저 바인딩하고, 그 후에 UI를 읽기 전용으로 만듦
                bindViewModelToUi()
                observeEditViewModel()
                configureUiForReadOnly()
            }
            is JournalEditActivity -> {
                // 수정 모드: 데이터를 바인딩하고 UI를 편집 가능하게 설정
                bindViewModelToUi()
                observeEditViewModel()
                configureUiForEditing()
            }
            is JournalWriteActivity -> {
                // 작성 모드: 데이터를 바인딩하고 UI를 편집 가능하게 설정
                bindViewModelToUi()
                observeWriteViewModel()
                configureUiForEditing()
            }
        }
    }

    // ✨ [핵심 수정 2] 상세 화면 (읽기 전용) UI 설정 - 가장 안정적인 방식으로 변경
    private fun configureUiForReadOnly() {
        binding.etTitle.apply {
            isFocusable = false
            isClickable = false
            isLongClickable = false
            background = null // 밑줄 배경 제거
        }
        binding.etContent.apply {
            isFocusable = false
            isClickable = false
            isLongClickable = false
            background = null // 밑줄 배경 제거
        }
        binding.etGratitude.apply {
            isFocusable = false
            isClickable = false
            isLongClickable = false
            background = null // 밑줄 배경 제거
        }

        // 하단 버튼들을 모두 숨김
        binding.bottomActionButtons.isVisible = false
        // '사진 추가' 프레임의 클릭 이벤트를 막음
        binding.layoutAddImage.isClickable = false
        // 이미지 미리보기 클릭 이벤트 제거
        binding.ivPreview.setOnClickListener(null)
    }

    // 작성/수정 화면 (편집 가능) UI 설정
    private fun configureUiForEditing() {
        setupClickListeners()
        // EditText의 기본 상태를 복원 (필요시)
        binding.etTitle.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isLongClickable = true
            // 배경은 기본 스타일을 따르도록 XML에 정의된 대로 둠
        }
        binding.etContent.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isLongClickable = true
        }
        binding.etGratitude.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isLongClickable = true
        }
    }

    // ViewModel의 데이터를 UI EditText에 바인딩
    private fun bindViewModelToUi() {
        when (val currentViewModel = if (activity is JournalWriteActivity) writeViewModel else editViewModel) {
            is JournalWriteViewModel -> {
                binding.etTitle.addTextChangedListener { text -> currentViewModel.title.value = text.toString() }
                binding.etContent.addTextChangedListener { text -> currentViewModel.content.value = text.toString() }
                binding.etGratitude.addTextChangedListener { text -> currentViewModel.gratitude.value = text.toString() }
            }
            is JournalEditViewModel -> {
                binding.etTitle.addTextChangedListener { text -> currentViewModel.title.value = text.toString() }
                binding.etContent.addTextChangedListener { text -> currentViewModel.content.value = text.toString() }
                binding.etGratitude.addTextChangedListener { text -> currentViewModel.gratitude.value = text.toString() }
            }
        }
    }

    // 클릭 이벤트 리스너 설정 (작성/수정 모드에서만 호출됨)
    private fun setupClickListeners() {
        binding.ivPreview.setOnClickListener {
            showImageDeleteConfirmDialog()
        }
        binding.layoutAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnAddPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnGenerateAiPhoto.setOnClickListener {
            showAiStyleSelector()
        }
    }

    // 작성 ViewModel 관찰
    private fun observeWriteViewModel() {
        val vm = writeViewModel
        viewLifecycleOwner.lifecycleScope.launch { vm.selectedImageUri.collectLatest { updateImageView(it, null, null) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.generatedImageBitmap.collectLatest { updateImageView(null, null, it) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.isLoading.collect { handleLoading(it) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.aiGenerationError.collectLatest { showError(it) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.noImage.collectLatest { updateImageView(null, null, null) } }

        // 데이터 채우기
        viewLifecycleOwner.lifecycleScope.launch {
            vm.title.collect { if (binding.etTitle.text.toString() != it) binding.etTitle.setText(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.content.collect { if (binding.etContent.text.toString() != it) binding.etContent.setText(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.gratitude.collect { if (binding.etGratitude.text.toString() != it) binding.etGratitude.setText(it) }
        }
    }

    // 상세/수정 ViewModel 관찰
    private fun observeEditViewModel() {
        val vm = editViewModel
        viewLifecycleOwner.lifecycleScope.launch { vm.selectedImageUri.collectLatest { updateImageView(it, null, null) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.existingImageUrl.collectLatest { updateImageView(null, it, null) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.generatedImageBitmap.collectLatest { updateImageView(null, null, it) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.isLoading.collect { handleLoading(it) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.aiGenerationError.collectLatest { showError(it) } }
        viewLifecycleOwner.lifecycleScope.launch { vm.noImage.collectLatest { updateImageView(null, null, null) } }

        vm.title.observe(viewLifecycleOwner) { if (binding.etTitle.text.toString() != it) binding.etTitle.setText(it) }
        vm.content.observe(viewLifecycleOwner) { if (binding.etContent.text.toString() != it) binding.etContent.setText(it) }
        vm.gratitude.observe(viewLifecycleOwner) { if (binding.etGratitude.text.toString() != it) binding.etGratitude.setText(it) }
    }

    private fun updateImageView(uri: Uri?, url: String?, bitmap: Bitmap?) {
        val sources = listOfNotNull(uri, url, bitmap)
        val hasImage = sources.isNotEmpty()

        binding.imageContainer.isVisible = hasImage
        binding.layoutAddImage.isVisible = !hasImage

        val constraintLayout = binding.root.getChildAt(0) as androidx.constraintlayout.widget.ConstraintLayout
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(constraintLayout)

        val marginTop = resources.getDimensionPixelSize(R.dimen.space_l)

        if (hasImage) {
            constraintSet.connect(
                binding.labelContent.id,
                androidx.constraintlayout.widget.ConstraintSet.TOP,
                binding.imageContainer.id,
                androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
                marginTop
            )
            if (isAdded) {
                Glide.with(this).load(sources.first()).into(binding.ivPreview)
            }
        } else {
            constraintSet.connect(
                binding.labelContent.id,
                androidx.constraintlayout.widget.ConstraintSet.TOP,
                binding.layoutAddImage.id,
                androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
                marginTop
            )
            if (isAdded) {
                Glide.with(this).clear(binding.ivPreview)
                binding.ivPreview.setImageDrawable(null)
            }
        }
        constraintSet.applyTo(constraintLayout)

        if (activity is JournalWriteActivity || activity is JournalEditActivity) {
            binding.bottomActionButtons.isVisible = true
        }
    }

    private fun handleLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.layoutAddImage.isClickable = false

            if (binding.layoutAddImage.isVisible) {
                binding.tvAddImagePlaceholder.isVisible = false
                binding.placeholderLoadingGroup.isVisible = true
                binding.layoutAddImage.setBackgroundResource(R.drawable.bg_image_border_loading)
            } else {
                binding.imageLoadingOverlay.isVisible = true
                binding.imageLoadingGroup.isVisible = true
            }
        } else {
            binding.layoutAddImage.isClickable = true

            binding.placeholderLoadingGroup.isVisible = false
            binding.imageLoadingOverlay.isVisible = false
            binding.imageLoadingGroup.isVisible = false
            binding.layoutAddImage.setBackgroundResource(R.drawable.bg_image_border_double)
        }

        if (activity !is JournalDetailActivity) {
            binding.btnGenerateAiPhoto.isEnabled = !isLoading
        }
    }

    private fun showError(message: String) {
        if (isAdded) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showAiStyleSelector() {
        val dialogBinding = DialogAiStyleSelectorBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnDialogGenerate.setOnClickListener {
            val selectedRadioButtonId = dialogBinding.rgStyleOptions.checkedRadioButtonId
            if (selectedRadioButtonId == -1) {
                Toast.makeText(context, "스타일을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedRadioButton = dialogBinding.root.findViewById<android.widget.RadioButton>(selectedRadioButtonId)
            val selectedStyle = selectedRadioButton.tag.toString()

            if (activity is JournalWriteActivity) writeViewModel.generateImage(selectedStyle)
            else if (activity is JournalEditActivity) editViewModel.generateImage(selectedStyle)

            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showImageDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("이미지 삭제")
            .setMessage("선택한 이미지를 삭제하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                if (activity is JournalWriteActivity) writeViewModel.clearSelectedImage()
                else if (activity is JournalEditActivity) editViewModel.clearSelectedImage()
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
