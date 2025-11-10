import androidx.lifecycle.ViewModel
import com.example.sumdays.daily.memo.MemoRepository
import com.example.sumdays.daily.memo.MemoViewModel
import com.example.sumdays.daily.memo.MemoViewModelFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

// 테스트용 더미 ViewModel (실패 경로 테스트에 사용)
class DummyViewModel : ViewModel()

class MemoViewModelFactoryTest {

    private lateinit var mockRepository: MemoRepository
    private lateinit var factory: MemoViewModelFactory

    @Before
    fun setup() {
        // Mock Repository를 생성하여 Factory에 주입합니다.
        mockRepository = mock(MemoRepository::class.java)
        factory = MemoViewModelFactory(mockRepository)
    }

    // =================================================================
    // 1. 성공 경로 테스트: MemoViewModel이 요청될 때 올바르게 생성되는지 검증
    // =================================================================
    @Test
    fun create_whenMemoViewModelIsRequested_returnsMemoViewModelInstance() {
        // Arrange
        val modelClass = MemoViewModel::class.java

        // Act
        val viewModel = factory.create(modelClass)

        // Assert
        // 반환된 객체가 MemoViewModel의 인스턴스인지 확인
        assertTrue("Returned ViewModel must be an instance of MemoViewModel.", viewModel is MemoViewModel)
    }

    // =================================================================
    // 2. 실패 경로 테스트: 알 수 없는 ViewModel이 요청될 때 예외 발생 검증
    // =================================================================
    @Test(expected = IllegalArgumentException::class)
    fun create_whenUnknownViewModelIsRequested_throwsIllegalArgumentException() {
        // Arrange
        val modelClass = DummyViewModel::class.java

        // Act
        // DummyViewModel을 요청하면 Factory는 예외를 던져야 합니다.
        factory.create(modelClass)

        // Assert: @Test(expected = ...)를 통해 예외 발생 여부를 검증하므로 추가적인 Assert 코드는 필요 없습니다.
    }
}