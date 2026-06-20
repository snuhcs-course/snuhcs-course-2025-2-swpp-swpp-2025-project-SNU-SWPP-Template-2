package com.example.sumdays.image

import android.content.Context
import android.os.Build
import android.os.Looper
import android.widget.FrameLayout
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.TestApplication

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
    application = TestApplication::class
)
class PhotoGalleryAdapterTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var parent: FrameLayout
    private lateinit var adapter: PhotoGalleryAdapter

    private val clickedUrls = mutableListOf<String>()
    private val longClickedPositions = mutableListOf<Int>()
    private var addClicked = false

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        parent = FrameLayout(context)

        clickedUrls.clear()
        longClickedPositions.clear()
        addClicked = false

        adapter = PhotoGalleryAdapter(
            onPhotoClick = { url -> clickedUrls.add(url) },
            onPhotoLongClick = { pos -> longClickedPositions.add(pos) },
            onAddClick = { addClicked = true }
        )
    }

    @Test
    fun `submitList_withPhotoAndAdd_setsItemCountAndViewTypes`() {
        val items = listOf(
            GalleryItem.Photo("file:///test1.jpg"),
            GalleryItem.Add
        )

        adapter.submitList(items)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, adapter.itemCount)

        val type0 = adapter.getItemViewType(0)
        val type1 = adapter.getItemViewType(1)

        // 0번은 Photo, 1번은 Add 타입이어야 서로 다름
        assertNotEquals(type0, type1)
    }

    @Test
    fun `onBindViewHolder_forPhoto_bindsClickAndLongClickCallbacks`() {
        val url = "file:///photo.jpg"
        val items = listOf(
            GalleryItem.Photo(url)
        )
        adapter.submitList(items)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val viewType = adapter.getItemViewType(0)
        val holder = adapter.onCreateViewHolder(parent, viewType)

        adapter.onBindViewHolder(holder, 0)

        holder.itemView.performClick()
        holder.itemView.performLongClick()

        assertEquals(listOf(url), clickedUrls)
        assertEquals(listOf(0), longClickedPositions)
    }

    @Test
    fun `onBindViewHolder_forAdd_bindsAddClickCallback`() {
        val items = listOf(
            GalleryItem.Add
        )
        adapter.submitList(items)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val viewType = adapter.getItemViewType(0)
        val holder = adapter.onCreateViewHolder(parent, viewType)

        adapter.onBindViewHolder(holder, 0)

        assertFalse(addClicked)
        holder.itemView.performClick()
        assertTrue(addClicked)
    }

    @Test
    fun `onCreateViewHolder_inflatesCorrectLayouts`() {
        val photoTypeItems = listOf(GalleryItem.Photo("file:///a.jpg"))
        val addTypeItems = listOf(GalleryItem.Add)

        adapter.submitList(photoTypeItems)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val photoType = adapter.getItemViewType(0)
        val photoHolder = adapter.onCreateViewHolder(parent, photoType)
        assertNotNull(photoHolder.itemView.findViewById<ImageView>(R.id.gallery_image))

        adapter.submitList(addTypeItems)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val addType = adapter.getItemViewType(0)
        val addHolder = adapter.onCreateViewHolder(parent, addType)
        assertNotNull(addHolder.itemView)
    }
}
