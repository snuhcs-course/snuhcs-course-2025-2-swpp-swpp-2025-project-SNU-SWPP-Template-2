package com.example.sumdays

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.view.View
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
import androidx.activity.viewModels
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.sumdays.data.style.StyleDatabase
import com.example.sumdays.data.style.UserStyleDao
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.bumptech.glide.Glide

class DailySumActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoMergeAdapter: MemoMergeAdapter
    private val viewModel: DailyEntryViewModel by viewModels()

    // ★★★ 1. Prefs 및 DAO 인스턴스 선언 ★★★
    private lateinit var userStatsPrefs: UserStatsPrefs
    private lateinit var userStyleDao: UserStyleDao
    private lateinit var loadingOverlay: View
    private lateinit var loadingGifView: ImageView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sum)

        // ★★★ 2. Prefs 및 DAO 초기화 ★★★
        userStatsPrefs = UserStatsPrefs(this)
        userStyleDao = StyleDatabase.getDatabase(this).userStyleDao() // StyleDatabase를 통해 DAO 획득

        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"
        loadingOverlay = findViewById(R.id.loading_overlay)
        loadingGifView = findViewById(R.id.loading_gif_view)
        findViewById<TextView>(R.id.date_text_view).text = date

//        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
//            startActivity(Intent(this, DailyWriteActivity::class.java).putExtra("date", date))
//            finish()
//        }

        findViewById<ImageView>(R.id.undo_icon).setOnClickListener {
            memoMergeAdapter.undoLastMerge()
        }
        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.skip_icon).setOnClickListener {
                showLoading(true)
                lifecycleScope.launch {
                    saveDiary(memoMergeAdapter.mergeAllMemo())
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
        memoMergeAdapter = MemoMergeAdapter(
            initialMemoList,
            lifecycleScope,
            ::showAllMergedSheet,
            userStatsPrefs = userStatsPrefs,      // UserStatsPrefs 전달
            userStyleDao = userStyleDao           // UserStyleDao 전달
        )
        recyclerView.itemAnimator = null // streaming시 메모 깜빡임 방지
        recyclerView.adapter = memoMergeAdapter
    }

    private fun showLoading(isLoading: Boolean) {
        runOnUiThread {
            if (isLoading) {
                loadingOverlay.visibility = View.VISIBLE
                loadingGifView.visibility = View.VISIBLE
                // Glide로 GIF 로드 (R.drawable.loading_animation.gif 파일이 있다고 가정)
                Glide.with(this)
                    .asGif()
                    .load(R.drawable.loading_animation) // "loading_animation.gif" 파일 이름
                    .into(loadingGifView)
            } else {
                loadingOverlay.visibility = View.GONE
                loadingGifView.visibility = View.GONE
                // Glide 로드 중지
                Glide.with(this).clear(loadingGifView)
            }
        }
    }
    private fun moveToReadActivity() {
        val intent = Intent(this, DailyReadActivity::class.java).putExtra("date", date)
        startActivity(intent)
        finish()
    }

    private suspend fun saveDiary(mergedResult: String){
        viewModel.updateEntry(date = date, diary = mergedResult)
        AnalysisRepository.requestAnalysis(date, mergedResult, viewModel)
    }

    private var mergeSheetShowing = false  // ✅ 중복 방지용

    private fun showAllMergedSheet() {
        if (mergeSheetShowing) return
        mergeSheetShowing = true

        val sheet = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_merge_done, null, false)
        sheet.setContentView(view)

        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnSave   = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)

        btnCancel.setOnClickListener {
            sheet.dismiss()
        }

        btnSave.setOnClickListener {
            // 서버 머지 결과를 최종 일기로 저장하고 읽기화면 이동
            lifecycleScope.launch {
                try {
                    saveDiary(memoMergeAdapter.getMemoContent(0))
                    moveToReadActivity()
                } finally {
                    sheet.dismiss()
                }
            }
        }

        sheet.setOnDismissListener { mergeSheetShowing = false }
        sheet.show()

        // 화면 폭의 90%로 설정 (원하면 0.95f 등으로 조절)
        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        sheet.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        // 중앙 정렬(기본이지만 혹시 몰라 명시)
        sheet.window?.setGravity(android.view.Gravity.CENTER)
    }
    override fun onDestroy() {
        super.onDestroy()
    }
}
