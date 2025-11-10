package com.example.sumdays.data.style

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserStyleViewModel(application: Application) : AndroidViewModel(application) {

    // StyleDatabase 인스턴스에서 DAO를 가져옵니다.
    private val dao = StyleDatabase.getDatabase(application).userStyleDao()

    /**
     * 특정 사용자 ID에 해당하는 모든 스타일 목록을 LiveData로 반환합니다.
     * DiaryStyleSettingsActivity에서 스타일 목록을 표시하는 데 사용됩니다.
     */
    fun getAllStyles(): LiveData<List<UserStyle>> {
        // Room DAO는 LiveData를 직접 반환하므로, ViewModelScope를 사용하지 않고 바로 호출합니다.
        return dao.getAllStyles()
    }

    /**
     * 새로운 스타일 데이터를 Room DB에 저장합니다.
     * 스타일 추출 API 호출 성공 후 사용됩니다.
     */
    fun insertStyle(style: UserStyle) = viewModelScope.launch(Dispatchers.IO) {
        // 삽입 작업은 I/O 스레드에서 수행합니다.
        dao.insertStyle(style)
    }

    /**
     * 특정 스타일을 Room DB에서 삭제합니다.
     * 설정 화면에서 스타일 삭제 버튼 클릭 시 사용됩니다.
     */
    fun deleteStyle(style: UserStyle) = viewModelScope.launch(Dispatchers.IO) {
        // 삭제 작업은 I/O 스레드에서 수행합니다.
        dao.deleteStyle(style)
    }

    /**
     * ID를 통해 단일 스타일 데이터를 조회합니다.
     * (활성 스타일 정보가 필요할 때 사용될 수 있습니다.)
     * LiveData가 아니므로 suspend 함수로 정의하고 호출하는 곳에서 결과를 기다립니다.
     */
    suspend fun getStyleById(styleId: Long): UserStyle? {
        return dao.getStyleById(styleId)
    }

    // 추가: 모든 스타일 데이터를 삭제하는 함수
    fun deleteAllStyles() = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteAllStyles()
    }
}