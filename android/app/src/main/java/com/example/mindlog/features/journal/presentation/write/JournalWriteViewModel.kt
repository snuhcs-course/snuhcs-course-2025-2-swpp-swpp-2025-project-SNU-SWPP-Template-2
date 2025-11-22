package com.example.mindlog.features.journal.presentation.write

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import com.example.mindlog.features.journal.domain.usecase.CreateJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.GenerateImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class JournalWriteViewModel @Inject constructor(
    private val createJournalUseCase: CreateJournalUseCase,
    private val journalRepository: JournalRepository,
    private val generateImageUseCase: GenerateImageUseCase,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val KEY_EMOTION_SCORES = "emotionScores"

        private fun defaultScores(): Map<String, Int?> = mapOf(
            "happy" to null, "sad" to null, "anxious" to null, "calm" to null,
            "annoyed" to null, "satisfied" to null, "bored" to null, "interested" to null,
            "lethargic" to null, "energetic" to null
        )
    }

    private val _emotionScores = MutableStateFlow(
        savedStateHandle.get<Map<String, Int?>>(KEY_EMOTION_SCORES) ?: defaultScores()
    )
    val emotionScores: StateFlow<Map<String, Int?>> = _emotionScores


    fun updateEmotionScore(emotionName: String, score: Int) {
        val updatedScores = _emotionScores.value.toMutableMap().apply {
            this[emotionName] = score
        }
        _emotionScores.value = updatedScores
        savedStateHandle[KEY_EMOTION_SCORES] = updatedScores
    }

    private fun getEmotionsForSaving(): Map<String, Int> {
        return _emotionScores.value
            .mapValues { (_, score) -> score ?: 2 }
            .filterValues { score -> score != 0 }
    }


    val title = MutableStateFlow("")
    val content = MutableStateFlow("")
    val gratitude = MutableStateFlow("")
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val generatedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val isLoading = MutableStateFlow(false)
    val aiGenerationError = MutableSharedFlow<String>()
    val noImage = MutableSharedFlow<Boolean>()
    private val _saveResult = MutableSharedFlow<Result<Unit>>()
    val saveResult = _saveResult.asSharedFlow()


    fun saveJournal() {
        val currentTitle = title.value
        val currentContent = content.value
        val currentGratitude = gratitude.value
        val galleryImageUri = selectedImageUri.value
        val aiGeneratedBitmap = generatedImageBitmap.value

        when {
            currentTitle.isBlank() -> {
                viewModelScope.launch {
                    _saveResult.emit(Result.Error(message = "제목을 입력해주세요."))
                }
                return
            }
            currentContent.isBlank() -> {
                viewModelScope.launch {
                    _saveResult.emit(Result.Error(message = "오늘의 하루를 입력해주세요."))
                }
                return
            }
        }

        viewModelScope.launch {
            try {
                val emotionsToSend = getEmotionsForSaving()

                val journalId = createJournalUseCase(
                    title = currentTitle,
                    content = currentContent,
                    emotions = emotionsToSend,
                    gratitude = currentGratitude
                )

                val imageData: Triple<ByteArray, String, String>? = when {
                    galleryImageUri != null -> {
                        context.contentResolver.openInputStream(galleryImageUri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val type = context.contentResolver.getType(galleryImageUri) ?: "image/jpeg"
                            val name = "gallery_image_${System.currentTimeMillis()}.jpg"
                            Triple(bytes, type, name)
                        }
                    }
                    aiGeneratedBitmap != null -> {
                        val stream = ByteArrayOutputStream()
                        aiGeneratedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val bytes = stream.toByteArray()
                        Triple(bytes, "image/jpeg", "ai_image_${System.currentTimeMillis()}.jpg")
                    }
                    else -> null
                }

                if (imageData != null) {
                    val (bytes, contentType, fileName) = imageData
                    journalRepository.uploadJournalImage(
                        journalId = journalId,
                        imageBytes = bytes,
                        contentType = contentType,
                        fileName = fileName
                    )
                }

                try {
                    journalRepository.extractKeywords(journalId)
                } catch (e: Exception) {
                }

                _saveResult.emit(Result.Success(Unit))

            } catch (e: Exception) {
                _saveResult.emit(Result.Error(message = e.message ?: "알 수 없는 오류가 발생했습니다."))
            }
        }
    }

    fun setGalleryImageUri(uri: Uri?) {
        if (uri != null) {
            selectedImageUri.value = uri
            generatedImageBitmap.value = null
        }
    }

    fun generateImage(style: String) {
        val textContent = content.value.ifBlank { title.value }
        if (textContent.isBlank()) {
            viewModelScope.launch {
                aiGenerationError.emit("이미지를 생성하려면 내용이나 제목을 먼저 입력해주세요.")
            }
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            try {
                val base64Image = generateImageUseCase(style, textContent)
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                generatedImageBitmap.value = decodedBitmap
                selectedImageUri.value = null

            } catch (e: Exception) {
                aiGenerationError.emit(e.message ?: "이미지 생성에 실패했습니다.")
                noImage.emit(true)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearSelectedImage() {
        selectedImageUri.value = null
        generatedImageBitmap.value = null
        viewModelScope.launch {
            noImage.emit(true)
        }
    }
}
