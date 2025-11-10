package com.example.sumdays.daily.memo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.content.ClipData
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.DragShadowBuilder
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyleDao
import com.example.sumdays.network.ApiClient
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStreamReader

class MemoMergeAdapter(
    private val memoList: MutableList<Memo>,
    private val scope: CoroutineScope,
    private val onAllMergesDone: () -> Unit,
    private val useStableIds: Boolean = true,
    private val userStatsPrefs: UserStatsPrefs,
    private val userStyleDao: UserStyleDao
) : RecyclerView.Adapter<MemoMergeAdapter.VH>() {

    data class MergeRecord(
        val fromIndexBefore: Int,
        val toIndexBefore: Int,
        val fromMemo: Memo,
        val toMemoBefore: Memo,
        val mergedIds: MutableList<Int>
    )

    private val undoStack = ArrayDeque<MergeRecord>()

    /** id to mergedIds */
    private val idToMergedIds = mutableMapOf<Int, MutableList<Int>>()
    private val originalMemoMap: Map<Int, Memo> = memoList.associateBy { it.id }

    init {
        memoList.forEach { idToMergedIds[it.id] = mutableListOf<Int>(it.id) }
    }

    init {
        if (useStableIds) {
            try { setHasStableIds(true) } catch (_: Throwable) { /* 테스트 환경에서는 NPE 방지 */ }
        }
    }

    /**
     * suppose that the timestamp is STABLE (since ID must be STABLE)
     */
    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long =
        (memoList[position].content + memoList[position].timestamp).hashCode().toLong()

    fun getMemoContent(index: Int): String{
        return memoList.get(index).content
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val memo = memoList[position]
        holder.bind(memo)

        // 드래그 시작: 시작 index를 localState로 넘김
        holder.itemView.isLongClickable = true
        holder.itemView.setOnLongClickListener { v ->
            val fromIndex = holder.adapterPosition
            if (fromIndex == RecyclerView.NO_POSITION) return@setOnLongClickListener false

            val data = ClipData.newPlainText("memo", "drag")
            val shadow = DragShadowBuilder(v)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(data, shadow, fromIndex, 0)
            } else {
                @Suppress("DEPRECATION") v.startDrag(data, shadow, fromIndex, 0)
            }
            v.alpha = 0.6f
            true // Completed the task successfully
        }

        // 타깃 리스너: STARTED에서 반드시 true 반환 (안 그러면 DROP 안 옴)
        holder.itemView.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { view.scaleX = 1.03f; view.scaleY = 1.03f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { view.scaleX = 1f;    view.scaleY = 1f;    true }
                DragEvent.ACTION_DROP -> {
                    view.scaleX = 1f; view.scaleY = 1f
                    val fromIndex = event.localState as? Int ?: return@setOnDragListener false
                    val toIndex   = holder.adapterPosition
                    if (toIndex == RecyclerView.NO_POSITION) return@setOnDragListener false
                    // 드래그 순서 상관 없이 합치는 순서는 항상 정해져 있음
                    if (fromIndex != toIndex) {
                        // {1, 2, 4, 6} 머지된 메모의 id들이 오름차순으로 정렬됨
                        val fromIds: List<Int> = idToMergedIds[memoList[fromIndex].id] ?: emptyList()
                        val toIds:   List<Int> = idToMergedIds[memoList[toIndex].id]  ?: emptyList()
                        val mergedIds: List<Int> = fromIds + toIds
                        val sortedIdsByOrder: List<Int> = mergedIds
                            .mapNotNull { id -> originalMemoMap[id] }
                            .sortedBy { it.order }
                            .map { it.id }
                        mergeByIndex(fromIndex, toIndex, sortedIdsByOrder)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { holder.itemView.alpha = 1f; true }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = memoList.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: TextView = itemView.findViewById(R.id.memo_text)
        private val timestamp: TextView = itemView.findViewById(R.id.memo_time)
        fun bind(m: Memo) { content.text = m.content; timestamp.text = m.timestamp }
        fun updateTextOnly(text: String) {
            content.text = text
        }
    }

    private fun maybeNotifyAllMerged() {
        if (memoList.size <= 1) onAllMergesDone()
    }

    /** 합칠 MemoList index들과 id들을 인자로 주면 두 메모를 하나로 합친다. */
    private fun mergeByIndex(fromIndex: Int, toIndex: Int, mergedIds: List<Int>) {
        if (fromIndex !in memoList.indices || toIndex !in memoList.indices) return

        val record = MergeRecord(
            fromIndexBefore = fromIndex,
            toIndexBefore   = toIndex,
            fromMemo        = memoList[fromIndex],
            toMemoBefore    = memoList[toIndex],
            mergedIds = mutableListOf()
        )

        scope.launch(Dispatchers.IO) {

            // 1) 먼저 UI에서 fromIndex 제거 → **이게 먼저 되어야 스트리밍 UI 업데이트가 안전해짐**
            var targetIndex: Int
            withContext(Dispatchers.Main) {
                memoList.removeAt(fromIndex)
                notifyItemRemoved(fromIndex)

                // 제거 후에야 올바른 targetIndex 계산 가능
                targetIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex
            }

            // 2) 스트리밍 병합 시작
            val finalMergedText = mergeTextByIds(mergedIds, endFlag = false) { partial ->

                scope.launch(Dispatchers.Main) {
                    // ✅ 안전성 체크 꼭 필요
                    if (targetIndex in memoList.indices) {
                        memoList[targetIndex] = memoList[targetIndex].copy(content = partial)
                        notifyItemChanged(targetIndex, partial)
                    }
                }
            }

            // 3) ID 업데이트
            mergedIds.forEach { updateIdMap(it, mergedIds) }

            // 4) 스트림 종료 후 최종 내용 고정
            withContext(Dispatchers.Main) {
                if (targetIndex in memoList.indices) {
                    memoList[targetIndex] = memoList[targetIndex].copy(content = finalMergedText)
                    notifyItemChanged(targetIndex, finalMergedText)
                }

                undoStack.addLast(record)
                maybeNotifyAllMerged()
            }
        }
    }


    fun updateIdMap(targetId: Int, mergedIds: List<Int>){
        idToMergedIds[targetId] = mergedIds.toMutableList()
    }

    /** 주어진 Memo Id들에 해당하는 Memo text들을 OpenAI를 이용해 하나로 합친다. 합쳐진 문장 반환 */
    suspend fun mergeTextByIds(
        mergedIds: List<Int>,
        endFlag: Boolean = false,
        onPartial: (String) -> Unit = {}
    ): String {

        // 1️. 서버에 보낼 Memo 리스트 구성
        val memos = mutableListOf<MemoPayload>()
        mergedIds.forEach {
            val memo = originalMemoMap[it]
            memos.add(MemoPayload(memo!!.id,memo.content, memo.order))
        }

        // ★★★ 2️. 활성 스타일 데이터 로드 또는 더미 데이터 사용 ★★★
        val activeStyleId = userStatsPrefs.getActiveStyleId()
        val styleData = if (activeStyleId != null) {
            // Room에서 활성 스타일 데이터 조회 (IO 스레드에서 suspend 호출)
            userStyleDao.getStyleById(activeStyleId)
        } else {
            null // 활성 스타일이 없음
        }

        val stylePrompt: Map<String, Any>
        val styleExample: List<String>

        if (styleData != null) {
            // ✅ 활성 스타일이 있을 경우: Room DB의 실제 데이터 사용
            stylePrompt = convertStylePromptToMap(styleData.stylePrompt) // StylePrompt 객체를 Map으로 변환 필요
            styleExample = styleData.styleExamples
        } else {
            // ✅ 활성 스타일이 없을 경우: 더미 데이터 사용 (기존 test 데이터)
            stylePrompt = mapOf(
                "common_phrases" to listOf("자고 싶어", "그냥 없었다", "왜 있을까?"),
                "emotional_tone" to "감정 표현이 강하며 다채롭고 직접적, 때로는 불평과 슬픔이 섞임",
                "formality" to "반말, 구어체, 친근하고 자연스러운 말투",
                "irony_or_sarcasm" to "없음",
                "lexical_choice" to "구어체적 어휘와 감정을 드러내는 단어가 주를 이룸",
                "pacing" to "빠름, 감정을 빠르게 전달하는 리듬",
                "sentence_endings" to listOf("~!", "~지롱~", "!!", "??"),
                "sentence_length" to "짧음",
                "sentence_structure" to "단문 위주, 감정을 직설적으로 표현하는 구조",
                "slang_or_dialect" to "반말, 일부 인터넷체적 어법 사용",
                "tone" to "경쾌하고 자주 감정을 드러내는, 일상적이고 솔직한 분위기"
            )
            styleExample = listOf(
                "자고 싶어! 졸려! 나는 아무것도 하기 싫지롱~",
                "오늘은 일기 쓸게 아무리 생각해도 없다",
                "일기는 세상에 왜 있을까? 일기가 없으면 안 될까? 일기를 꼭 써야 되나??",
                "눈물도 나오고 콧물도 나왔다"
            )
        }
        Log.d("test", "TEST: 0")
        val request = MergeRequest(memos = memos, endFlag = endFlag, stylePrompt, styleExample)

        if (endFlag) {
            // skip button => 마지막 완성 → JSON 반환
            val response = ApiClient.api.mergeMemos(request)
            val json = response.body() ?: throw IllegalStateException("Empty final response")
            return extractMergedText(json)
        }

        Log.d("test", "TEST: 1")
        // 3️. API 호출
        val call = ApiClient.api.mergeMemosStream(request)
        val response = call.execute()

        val stream = response.body()?.byteStream()
            ?: throw IllegalStateException("Empty streaming body")

        val reader = InputStreamReader(stream, Charsets.UTF_8)
        val sb = StringBuilder()
        val charBuffer = CharArray(64)

        while (true) {
            val read = reader.read(charBuffer)
            if (read == -1) break

            val chunk = String(charBuffer, 0, read)
            sb.append(chunk)

            onPartial?.invoke(sb.toString()) // 여기서 UI로 즉시 전달
        }

        return sb.toString()
    }

    /** StylePrompt 객체를 서버 요청 형식(Map<String, Any>)으로 변환 */
    private fun convertStylePromptToMap(prompt: StylePrompt): Map<String, Any> {
        // Gson을 사용하여 객체를 Map으로 변환하는 것이 가장 안전하고 빠릅니다.
        // Gson 라이브러리 사용 가정
        val gson = Gson()
        // Map<String, Any> 타입 토큰 정의
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type

        // StylePrompt 객체를 JSON 문자열로 변환 후, 다시 Map으로 변환
        val jsonString = gson.toJson(prompt)
        return gson.fromJson(jsonString, type)
    }

    /** extract merged text from json file */
    private fun extractMergedText(json: JsonObject): String {
        Log.d("test", "TEST: " + json.toString())
        // case 1: end_flag = true → diary
        if (json.has("result")) {
            val result = json.getAsJsonObject("result")

            if (result.has("diary") && result.get("diary").isJsonPrimitive) {
                return result.get("diary").asString
            }
        }
        // case 2: end_flag = false → {"merged_content": {"merged_content": "..."}}
        if (json.has("merged_content") && json.get("merged_content").isJsonObject) {
            val inner = json.getAsJsonObject("merged_content")
            if (inner.has("merged_content") && inner.get("merged_content").isJsonPrimitive) {
                return inner.get("merged_content").asString
            }
        }
        return ""
    }



    /** 마지막 머지를 되돌린다. 성공하면 true */
    fun undoLastMerge(): Boolean {
        val rec = undoStack.removeLastOrNull() ?: return false

        // 현재 리스트 상태에서 '타깃'이 있는 인덱스 계산
        // 머지 직후에는 fromIndex가 빠져서,
        // - from < to 였다면 현재 타깃 인덱스는 (to - 1)
        // - from > to 였다면 현재 타깃 인덱스는 to
        val toCurrent = if (rec.fromIndexBefore < rec.toIndexBefore)
            rec.toIndexBefore - 1
        else
            rec.toIndexBefore

        // 1) 타깃을 머지 전 내용으로 복구
        if (toCurrent in memoList.indices) {
            memoList[toCurrent] = rec.toMemoBefore
            notifyItemChanged(toCurrent)
        } else {
            // 인덱스가 틀어진 예외 상황 방지용: 안전 장치
            return false
        }

        // 2) from 메모를 원래 위치에 다시 삽입
        val insertIndex = rec.fromIndexBefore.coerceIn(0, memoList.size)
        memoList.add(insertIndex, rec.fromMemo)
        notifyItemInserted(insertIndex)

        return true
    }


    suspend fun mergeAllMemo(): String{
        val idMutableList = mutableListOf<Int>()
        for (memo in originalMemoMap) {
            idMutableList.add(memo.value.id)
        }
        val idList = idMutableList.toList()

        return mergeTextByIds(idList, endFlag = true)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val partialText = payloads.last() as String
            holder.updateTextOnly(partialText)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

}

