package com.example.mindlog.features.journal.presentation.write

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.model.Emotion
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.features.journal.domain.usecase.DeleteJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.ExtractKeywordsUseCase
import com.example.mindlog.features.journal.domain.usecase.GenerateImageUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.UpdateJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.UploadJournalImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class JournalEditViewModel @Inject constructor(
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
    private val deleteJournalUseCase: DeleteJournalUseCase,
    private val uploadJournalImageUseCase: UploadJournalImageUseCase,
    private val generateImageUseCase: GenerateImageUseCase,
    private val extractKeywordsUseCase: ExtractKeywordsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _journalState = MutableLiveData<Result<JournalEntry>>()
    val journalState: LiveData<Result<JournalEntry>> = _journalState

    private val _editResult = MutableSharedFlow<Result<String>>()
    val editResult = _editResult.asSharedFlow()

    val title = MutableLiveData<String>()
    val content = MutableLiveData<String>()
    val gratitude = MutableLiveData<String>()

    val keywords = MutableLiveData<List<Keyword>>()
    val emotions = MutableLiveData<List<Emotion>>()
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val existingImageUrl = MutableStateFlow<String?>(null)

    val generatedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val isLoading = MutableStateFlow(false)
    val aiGenerationError = MutableSharedFlow<String>()
    val noImage = MutableSharedFlow<Boolean>()

    var journalId: Int? = null
        private set

    private var originalJournal: JournalEntry? = null

    fun loadJournalDetails(id: Int, forceRefresh: Boolean = false) {
        if (journalId == id && !forceRefresh) return

        journalId = id
        viewModelScope.launch {
            try {
                // ✨ UseCase가 gratitude가 포함된 완전한 JournalEntry 모델을 반환
                val journal = getJournalByIdUseCase(id)
                originalJournal = journal

                // ✨ UI 모델의 필드를 LiveData에 바인딩
                title.value = journal.title
                content.value = journal.content
                gratitude.value = journal.gratitude ?: "" // Null일 경우 빈 문자열로 처리
                existingImageUrl.value = journal.imageUrl
                emotions.value = journal.emotions
                keywords.value = journal.keywords

                _journalState.value = Result.Success(journal)

                // 키워드가 비어있으면 분석 요청 (기존 로직 유지)
                if (journal.keywords.isEmpty()) {
                    extractJournalKeywords(id)
                }

            } catch (e: Exception) {
                _journalState.value = Result.Error(message = e.message ?: "일기를 불러오는데 실패했습니다.")
            }
        }
    }

    private fun extractJournalKeywords(id: Int) {
        viewModelScope.launch {
            try {
                extractKeywordsUseCase(id)
                // 분석 요청 성공 후, 최신 데이터 다시 로드
                loadJournalDetails(id, forceRefresh = true)
            } catch (e: Exception) {
                // 키워드 분석 실패는 치명적이지 않으므로, 에러를 UI에 노출하지 않음
            }
        }
    }

    fun updateJournal() {
        val id = journalId ?: return
        val originalData = originalJournal ?: return
        val newTitle = title.value ?: ""
        val newContent = content.value ?: ""
        val newGratitude = gratitude.value ?: ""
        val newImageUri = selectedImageUri.value
        val aiGeneratedBitmap = generatedImageBitmap.value

        when {
            newTitle.isBlank() -> {
                viewModelScope.launch { _editResult.emit(Result.Error(message = "제목을 입력해주세요.")) }
                return
            }
            newContent.isBlank() -> {
                viewModelScope.launch { _editResult.emit(Result.Error(message = "오늘의 하루를 입력해주세요.")) }
                return
            }
        }

        val isTextChanged = originalData.title != newTitle ||
                originalData.content != newContent ||
                originalData.gratitude != newGratitude

        val isImageChanged = newImageUri != null || aiGeneratedBitmap != null || (originalData.imageUrl != null && existingImageUrl.value == null)

        if (!isTextChanged && !isImageChanged) {
            viewModelScope.launch { _editResult.emit(Result.Success("수정 완료")) }
            return
        }

        viewModelScope.launch {
            try {
                if (isTextChanged) {
                    updateJournalUseCase(
                        journalId = id,
                        originalTitle = originalData.title,
                        originalContent = originalData.content,
                        originalGratitude = originalData.gratitude ?: "",
                        newTitle = newTitle,
                        newContent = newContent,
                        newGratitude = newGratitude
                    )
                }

                val imageData: Triple<ByteArray, String, String>? = when {
                    newImageUri != null -> {
                        context.contentResolver.openInputStream(newImageUri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val type = context.contentResolver.getType(newImageUri) ?: "image/jpeg"
                            val name = "gallery_${System.currentTimeMillis()}.jpg"
                            Triple(bytes, type, name)
                        }
                    }
                    aiGeneratedBitmap != null -> {
                        val stream = ByteArrayOutputStream()
                        aiGeneratedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val bytes = stream.toByteArray()
                        Triple(bytes, "image/jpeg", "ai_${System.currentTimeMillis()}.jpg")
                    }
                    else -> null
                }

                if (imageData != null) {
                    val (bytes, contentType, fileName) = imageData
                    uploadJournalImageUseCase(
                        journalId = id,
                        imageBytes = bytes,
                        contentType = contentType,
                        fileName = fileName
                    )
                }
                _editResult.emit(Result.Success("수정 완료"))
            } catch (e: Exception) {
                _editResult.emit(Result.Error(message = e.message ?: "수정에 실패했습니다."))
            }
        }
    }

    fun deleteJournal() {
        val id = journalId ?: return
        viewModelScope.launch {
            try {
                deleteJournalUseCase(id)
                _editResult.emit(Result.Success("삭제 완료"))
            } catch (e: Exception) {
                _editResult.emit(Result.Error(message = e.message ?: "삭제에 실패했습니다."))
            }
        }
    }

    fun setGalleryImageUri(uri: Uri?) {
        if (uri != null) {
            selectedImageUri.value = uri
            generatedImageBitmap.value = null
            existingImageUrl.value = null
        }
    }

    fun generateImage(style: String) {
        val textContent = content.value?.ifBlank { title.value }
        if (textContent.isNullOrBlank()) {
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
                existingImageUrl.value = null

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
        existingImageUrl.value = null
        generatedImageBitmap.value = null
        viewModelScope.launch {
            noImage.emit(true)
        }
    }
}
