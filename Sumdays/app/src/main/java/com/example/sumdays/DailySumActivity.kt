package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoMergeAdapter
import java.time.LocalDate
import com.example.sumdays.daily.diary.AnalysisRepository
import kotlinx.coroutines.launch
import com.example.sumdays.data.DailyEntry
import androidx.activity.viewModels
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import android.util.Log
class DailySumActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoMergeAdapter: MemoMergeAdapter
    private val viewModel: DailyEntryViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sum)

        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"

        findViewById<TextView>(R.id.date_text_view).text = date

//        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
//            startActivity(Intent(this, DailyWriteActivity::class.java).putExtra("date", date))
//            finish()
//        }

        findViewById<ImageView>(R.id.undo_icon).setOnClickListener {
            memoMergeAdapter.undoLastMerge()
        }

        findViewById<ImageButton>(R.id.skip_icon).setOnClickListener {
                lifecycleScope.launch {
                    val mergedResult = memoMergeAdapter.skipMerge()
                    viewModel.updateEntry(date = date, diary = mergedResult)
                    AnalysisRepository.requestAnalysis(date, mergedResult, viewModel)
                    moveToReadActivity()
                }
        }

        // 1. Intent에서 메모 리스트를 받습니다.
        val receivedMemoList: List<Memo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("memo_list", Memo::class.java) ?: emptyList()
        } else {
            // Deprecated 메서드를 사용하는 이전 API 레벨 대응 코드
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Memo>("memo_list") ?: emptyList()
        }

        // 2. MemoMergeAdapter는 MutableList<Memo>를 기대하므로 변환합니다.
        val initialMemoList: MutableList<Memo> = receivedMemoList.toMutableList()


        recyclerView = findViewById(R.id.memo_list_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ 드래그-머지 지원 어댑터로 교체
        memoMergeAdapter = MemoMergeAdapter(initialMemoList, lifecycleScope)
        recyclerView.adapter = memoMergeAdapter

        setupNavigationBar()
    }
    private fun moveToReadActivity() {
        val intent = Intent(this, DailyReadActivity::class.java).putExtra("date", date)
        startActivity(intent)
        finish()
    }
    // 하단 네비게이션 바의 버튼들 클릭 이벤트 처리
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
