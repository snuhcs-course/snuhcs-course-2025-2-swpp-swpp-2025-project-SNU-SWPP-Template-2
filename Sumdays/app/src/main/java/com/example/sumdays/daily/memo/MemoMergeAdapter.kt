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
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R
import com.example.sumdays.network.ApiClient
import com.google.gson.JsonObject


class MemoMergeAdapter(
    private val memoList: MutableList<Memo>,
    private val scope: CoroutineScope,
    private val useStableIds: Boolean = true
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
    }

    /** 합칠 MemoList index들과 id들을 인자로 주면 두 메모를 하나로 합친다. */
    private fun mergeByIndex(fromIndex: Int, toIndex: Int, mergedIds: List<Int>) {
        if (fromIndex !in memoList.indices || toIndex !in memoList.indices) return

        // 1) 되돌리기 레코드 저장 (변경 전 스냅샷)
        val record = MergeRecord(
            fromIndexBefore = fromIndex,
            toIndexBefore   = toIndex,
            fromMemo        = memoList[fromIndex],
            toMemoBefore    = memoList[toIndex],
            mergedIds = mutableListOf<Int>()
        )


        // 2) 실제 머지 수행 (기존 네 로직)
        val fromMemo = memoList[fromIndex]
        val toMemo   = memoList[toIndex]

        scope.launch(Dispatchers.IO) {
            try {
                val mergedText = mergeTextByIds(mergedIds)
                mergedIds.forEach {
                    updateIdMap(it, mergedIds)
                }

                withContext(Dispatchers.Main) {
                    // 1) 먼저 from 제거해서 인덱스 변동을 적용
                    memoList.removeAt(fromIndex)
                    notifyItemRemoved(fromIndex)

                    // 2) 제거 후의 타깃 인덱스 계산
                    val targetIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex

                    // 3) 타깃에 병합 내용 반영
                    memoList[targetIndex] = memoList[targetIndex].copy(content = mergedText.toString())
                    notifyItemChanged(targetIndex)

                    // 4) undo 스택 push (원한다면 record도 타깃 인덱스 after 기준으로 보정)
                    undoStack.addLast(record)
                }
            } catch (e: Exception) {
                Log.e("MemoMergeAdapter", "merge failed: ${e.message}", e)
            }
        }
    }

    fun updateIdMap(targetId: Int, mergedIds: List<Int>){
        idToMergedIds[targetId] = mergedIds.toMutableList()
    }

    /** 주어진 Memo Id들에 해당하는 Memo text들을 OpenAI를 이용해 하나로 합친다. 합쳐진 문장 반환 */
    suspend fun mergeTextByIds(mergedIds: List<Int>): String {

        // 1️. 서버에 보낼 Memo 리스트 구성
        val memos = mutableListOf<MemoPayload>()
        mergedIds.forEach {
            val memo = originalMemoMap[it]
            memos.add(MemoPayload(memo!!.id,memo.content, memo.order))
        }

        // 2️. 요청 객체 생성
        val request = MergeRequest(memos = memos, endFlag = false)

        Log.d("test", "test: " + request.toString())
        // 3️. API 호출
        val response = ApiClient.api.mergeMemos(request)

        val json = response.body() ?: throw IllegalStateException("Empty body")
        val merged = extractMergedText(json)

        // 4️. 서버가 돌려준 병합 결과 반환
        return merged
    }

    /** extract merged text from json file */
    private fun extractMergedText(json: JsonObject): String {
        // case 1: end_flag = true → diary
        if (json.has("diary") && json.get("diary").isJsonPrimitive) {
            return json.get("diary").asString
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


    suspend fun skipMerge(): String{
        /* 임시 testing 부분
        var output = ""
        for (memo in originalMemoMap) {
            output += memo.value.content
        }
        return output
        임시 testing 부분 */
        val idMutableList = mutableListOf<Int>()
        for (memo in originalMemoMap) {
            idMutableList.add(memo.value.id)
        }
        val idList = idMutableList.toList()

        return mergeTextByIds(idList)
    }
}

