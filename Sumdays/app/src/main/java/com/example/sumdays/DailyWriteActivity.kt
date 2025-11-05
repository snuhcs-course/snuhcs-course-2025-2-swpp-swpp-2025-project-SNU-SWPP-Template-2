package com.example.sumdays

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoAdapter
import com.example.sumdays.daily.memo.MemoViewModel
import com.example.sumdays.daily.memo.MemoViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.sumdays.daily.memo.MemoDragAndDropCallback
import com.example.sumdays.audio.AudioRecorderHelper
import com.example.sumdays.image.ImageOcrHelper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.ArrayList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 일기 작성 및 수정 화면을 담당하는 액티비티
class DailyWriteActivity : AppCompatActivity() {

    // 오늘 날짜를 저장하는 변수
    private lateinit var date: String
    // 메모 목록을 표시하는 어댑터
    private lateinit var memoAdapter: MemoAdapter

    // UI 뷰 변수들
    private lateinit var dateTextView: TextView
    private lateinit var memoListView: RecyclerView
    private lateinit var memoInputEditText: EditText
    private lateinit var sendIcon: ImageView

    private lateinit var micIcon: ImageView
    private lateinit var stopIcon: ImageView

    // ★★★ 음파 뷰 관련 변수 ★★★
    private lateinit var audioWaveView: LinearLayout
    private lateinit var waveBar1: View
    private lateinit var waveBar2: View
    private lateinit var waveBar3: View

    //  AudioRecorderHelper 인스턴스 생성 (lazy 초기화)
    private lateinit var audioRecorderHelper: AudioRecorderHelper
    private lateinit var readDiaryButton: Button

    // ViewModel 초기화 (앱의 싱글톤 저장소를 사용)
    private val memoViewModel: MemoViewModel by viewModels {
        MemoViewModelFactory(
            (application as MyApplication).repository
        )
    }

    // ★★★ 힌트 및 애니메이터 변수 추가 ★★★
    private var defaultMemoHint: String = ""
    private var isRecording = false
    private var waveAnimatorSet: AnimatorSet? = null // ★★★ AnimatorSet ★★★

    // ★★★ 오디오 분석 중인 임시 메모의 ID를 저장할 변수 (Int?) ★★★
    private var pendingAudioMemoId: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daily_write)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.write)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 모든 뷰 초기화
        initViews()

        // 헬퍼 초기화 (initViews 이후 호출)
        audioRecorderHelper = createAudioRecorderHelper()

        // 인텐트 데이터 처리 및 데이터 관찰 시작
        handleIntent(intent)

        // UI 요소에 클릭 리스너 설정
        setupClickListeners()

        // 하단 네비게이션 바 설정
        setupNavigationBar()
    }

    /**
     * AudioRecorderHelper 인스턴스를 생성하고 콜백을 정의하는 함수
     */
    private fun createAudioRecorderHelper(): AudioRecorderHelper {
        return AudioRecorderHelper(
            activity = this,
            onRecordingStarted = {
                runOnUiThread {
                    Toast.makeText(this, "녹음 시작...", Toast.LENGTH_SHORT).show()
                    isRecording = true
                    micIcon.visibility = View.GONE
                    stopIcon.visibility = View.VISIBLE
                    audioWaveView.visibility = View.VISIBLE
                    memoInputEditText.visibility = View.INVISIBLE
                    startWaveAnimation()
                }
            },
            onRecordingStopped = {
                runOnUiThread {
                    Toast.makeText(this, "녹음 완료. 텍스트 변환 중...", Toast.LENGTH_SHORT).show()
                    isRecording = false
                    micIcon.visibility = View.VISIBLE
                    stopIcon.visibility = View.GONE
                    audioWaveView.visibility = View.GONE
                    memoInputEditText.visibility = View.VISIBLE
                    stopWaveAnimation()

                    val tempId = System.currentTimeMillis().toInt()
                    val dummyMemo = Memo(
                        id = tempId,
                        content = "음성 인식 중...",
                        timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time),
                        date = date,
                        order = memoAdapter.itemCount ,
                        type = "audio"
                    )
                    pendingAudioMemoId = tempId

                    val currentList = memoAdapter.currentList.toMutableList()
                    currentList.add(dummyMemo)
                    memoAdapter.submitList(currentList)
                    memoListView.smoothScrollToPosition(currentList.size - 1)
                }
            },
            onRecordingComplete = { filePath, transcribedText ->
                Log.d("DailyWriteActivity", "녹음 완료, 파일 경로: $filePath")
                Log.d("DailyWriteActivity", "변환된 텍스트: $transcribedText")
                runOnUiThread {
                    val finalContent = transcribedText ?: "[오디오 파일: $filePath]"
                    removeDummyMemoAndAddFinal(finalContent, "audio")
                    Toast.makeText(this, "텍스트 변환 완료.", Toast.LENGTH_SHORT).show()
                }
            },
            onRecordingFailed = { errorMessage ->
                runOnUiThread {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    val errorContent = "[오류: $errorMessage]"
                    removeDummyMemoAndAddFinal(errorContent, "audio")
                }
            },
            onPermissionDenied = {
                runOnUiThread {
                    Toast.makeText(this, "음성 녹음을 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onShowPermissionRationale = {
                runOnUiThread {
                    Toast.makeText(this, "메모를 음성으로 녹음하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 액티비티가 재사용될 때 새로운 인텐트 처리 (변경 없음)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // UI 뷰 변수들을 레이아웃 ID와 연결 (수정됨)
    private fun initViews() {
        dateTextView = findViewById(R.id.date_text_view)
        memoListView = findViewById(R.id.memo_list_view)
        memoInputEditText = findViewById(R.id.memo_input_edittext)
        sendIcon = findViewById(R.id.send_icon)
        micIcon = findViewById(R.id.mic_icon)
        stopIcon = findViewById(R.id.stop_icon)
        readDiaryButton = findViewById(R.id.read_diary_button)

        audioWaveView = findViewById(R.id.audio_wave_view)
        waveBar1 = findViewById(R.id.wave_bar_1)
        waveBar2 = findViewById(R.id.wave_bar_2)
        waveBar3 = findViewById(R.id.wave_bar_3)

        memoListView.layoutManager = LinearLayoutManager(this)
        memoAdapter = MemoAdapter()
        memoListView.adapter = memoAdapter

        // ★★★ 힌트 저장 로직 제거 ★★★
        // defaultMemoHint = memoInputEditText.hint.toString()

        // 메모 아이템 클릭 시 수정 다이얼로그 표시 (변경 없음)
        memoAdapter.setOnItemClickListener(object : MemoAdapter.OnItemClickListener {
            override fun onItemClick(memo: Memo) {
                showEditMemoDialog(memo)
            }
        })

        // 드래그 앤 드롭 및 스와이프 콜백 정의 (변경 없음)
        val dragAndDropCallback = MemoDragAndDropCallback(
            adapter = memoAdapter,
            onMove = { fromPosition, toPosition -> /* ... */ },
            onDelete = { position ->
                val memoToDelete = memoAdapter.currentList[position]
                memoViewModel.delete(memoToDelete)
            },
            onDragStart = { },
            onDragEnd = {
                val updatedList = memoAdapter.currentList.toMutableList()
                for (i in updatedList.indices) {
                    updatedList[i] = updatedList[i].copy(order = i)
                }
                memoViewModel.updateAll(updatedList)
            }
        )

        val itemTouchHelper = ItemTouchHelper(dragAndDropCallback)
        itemTouchHelper.attachToRecyclerView(memoListView)
    }

    // 인텐트 데이터 처리 및 데이터 관찰 시작 (수정됨: LiveData 갱신 로직)
    private fun handleIntent(intent: Intent?) {
        date = intent?.getStringExtra("date") ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        dateTextView.text = date

        memoViewModel.getMemosForDate(date).observe(this) { memos ->
            memos?.let {
                // LiveData가 갱신될 때, 현재 UI에 임시 메모가 있다면 유지
                if (pendingAudioMemoId != null && !it.any { memo -> memo.id == pendingAudioMemoId }) {
                    val currentList = memoAdapter.currentList.toMutableList()
                    memoAdapter.submitList(currentList) // LiveData 업데이트 대신 현재 리스트 유지
                } else {
                    memoAdapter.submitList(it)
                }
            }
        }
    }

    // 메모 수정 다이얼로그를 보여주는 함수 (변경 없음)
    fun showEditMemoDialog(memo: Memo) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_memo, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text_memo_content)
        editText.setText(memo.content)

        builder.setView(dialogView)
            .setPositiveButton("수정") { dialog, id ->
                val newContent = editText.text.toString()
                if (newContent.isNotEmpty()) {
                    val updatedMemo = memo.copy(content = newContent)
                    memoViewModel.update(updatedMemo)
                }
            }
            .setNegativeButton("취소") { dialog, id ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // 주요 UI 요소에 클릭 리스너를 설정하는 함수 (수정됨)
    private fun setupClickListeners() {
        readDiaryButton.setOnClickListener {
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
        }

        sendIcon.setOnClickListener {
            val memoContent = memoInputEditText.text.toString().trim()
            if (memoContent.isNotEmpty()) {
                addTextMemoToList(memoContent, "text")
                memoInputEditText.text.clear()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            } else {
                Toast.makeText(this, "메모 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        micIcon.setOnClickListener {
            audioRecorderHelper.checkPermissionAndToggleRecording()
        }
        stopIcon.setOnClickListener {
            audioRecorderHelper.checkPermissionAndToggleRecording()
        }

        // ★★★ EditText 포커스 리스너 추가 ★★★
        memoInputEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                /*micIcon.visibility = View.GONE
                stopIcon.visibility = View.GONE*/
            } else {
                // 포커스 잃음: photoIcon은 필요 없고, 녹음 상태에 따라 아이콘 복원
                /*
                if (isRecording) {
                    micIcon.visibility = View.GONE
                    stopIcon.visibility = View.VISIBLE
                } else {
                    micIcon.visibility = View.VISIBLE
                    stopIcon.visibility = View.GONE
                }*/
            }
        }
    }

    // 하단 네비게이션 바의 버튼들 클릭 이벤트를 처리 (변경 없음)
    private fun setupNavigationBar() {
        val btnCalendar: ImageButton = findViewById(R.id.btnCalendar)
        val btnDaily: ImageButton = findViewById(R.id.btnDaily)
        val btnInfo: ImageButton = findViewById(R.id.btnInfo)
        val btnSum: ImageButton = findViewById(R.id.btnSum)

        btnSum.setOnClickListener {
            val currentMemos = memoAdapter.currentList
            val intent = Intent(this, DailySumActivity::class.java)
            intent.putExtra("date", date)
            intent.putParcelableArrayListExtra("memo_list", ArrayList(currentMemos))
            startActivity(intent)
        }
        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }
        btnDaily.setOnClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val currentLoadedDate = this.date
            if (today != currentLoadedDate) {
                val intent = Intent(this, DailyWriteActivity::class.java)
                intent.putExtra("date", today)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            } else {
                Toast.makeText(this, "이미 오늘 날짜입니다.", Toast.LENGTH_SHORT).show()
            }
        }
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorderHelper.release()
        // ★★★ 애니메이터 정리 ★★★
        waveAnimatorSet?.cancel()
    }

    // --- ★★★ 신규 함수들 (수정됨) ★★★ ---

    /**
     * "파동" 애니메이션을 시작합니다. (변경 없음)
     */
    private fun startWaveAnimation() {
        waveAnimatorSet?.cancel() // 기존 애니메이션 세트 취소

        // 3개의 막대에 대한 애니메이터 생성
        val anim1 = ObjectAnimator.ofFloat(waveBar1, "scaleY", 1.0f, 0.3f, 0.7f, 1.0f).apply {
            duration = 400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val anim2 = ObjectAnimator.ofFloat(waveBar2, "scaleY", 1.0f, 0.5f, 0.2f, 0.8f, 1.0f).apply {
            duration = 400
            startDelay = 150 // 0.15초 지연
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val anim3 = ObjectAnimator.ofFloat(waveBar3, "scaleY", 1.0f, 0.6f, 1.0f, 0.4f, 1.0f).apply {
            duration = 400
            startDelay = 300 // 0.3초 지연
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }

        // 3. 세 애니메이션을 함께 실행 (startDelay로 인해 시차 발생)
        waveAnimatorSet = AnimatorSet().apply {
            playTogether(anim1, anim2, anim3)
            start()
        }
    }

    /**
     * "파동" 애니메이션을 중지하고 막대들의 스케일을 1.0으로 복원합니다. (변경 없음)
     */
    private fun stopWaveAnimation() {
        waveAnimatorSet?.cancel()
        waveAnimatorSet = null
        // 스케일 원상 복구
        waveBar1.scaleY = 1.0f
        waveBar2.scaleY = 1.0f
        waveBar3.scaleY = 1.0f
    }

    private fun addTextMemoToList(content: String, memoType: String = "text") {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        val newMemo = Memo(
            id = 0, // Room이 자동 생성
            content = content,
            timestamp = currentTime,
            date = date,
            order = memoAdapter.itemCount,
            type = memoType // ★★★ 전달받은 memoType 사용 ★★★
        )
        memoViewModel.insert(newMemo)
        memoListView.smoothScrollToPosition(memoAdapter.itemCount)
    }

    /**
     * ★★★ 임시 메모를 제거하고 최종 메모를 ViewModel에 저장하는 함수 (수정됨) ★★★
     */
    private fun removeDummyMemoAndAddFinal(newContent: String, memoType: String) {
        if (pendingAudioMemoId != null) {
            val currentList = memoAdapter.currentList.toMutableList()
            val removed = currentList.removeAll { it.id == pendingAudioMemoId }

            if (removed) {
                memoAdapter.submitList(currentList)
            }
            pendingAudioMemoId = null
        }

        // ★★★ 최종 텍스트로 실제 메모를 DB에 저장 (memoType 전달) ★★★
        addTextMemoToList(newContent, memoType)
    }
}

