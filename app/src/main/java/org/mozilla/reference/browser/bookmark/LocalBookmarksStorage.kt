package org.mozilla.reference.browser.bookmark

import android.content.Context
import androidx.room.*
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.BookmarksStorage
import java.util.*


class LocalBookmarksStorage(val context: Context) : BookmarksStorage {

    lateinit var db: BookmarkDatabase

    override suspend fun addFolder(parentGuid: String, title: String, position: Int?): String {
        val uid = UUID.randomUUID().toString()
        val bm = BookmarkEntity(1, uid, parentGuid, title, null)
        db.getDao().insert(bm)
        return uid
    }

    override suspend fun addItem(parentGuid: String, url: String, title: String, position: Int?): String {
        val uid = UUID.randomUUID().toString()
        val bm = BookmarkEntity(0, uid, parentGuid, title, url)
        db.getDao().insert(bm)
        return uid
    }

    override suspend fun addSeparator(parentGuid: String, position: Int?): String {
        val uid = UUID.randomUUID().toString()
        val bm = BookmarkEntity(2, uid, parentGuid, null, null)
        db.getDao().insert(bm)
        return uid
    }

    override fun cleanup() {
        db.close()
    }

    override suspend fun deleteNode(guid: String): Boolean {
        val rst = db.getDao().deleteByUid(guid)
        return rst != null
    }

    override suspend fun getBookmark(guid: String): BookmarkNode? {
        val bm = db.getDao().findById(guid)[0]
        if (bm != null) {
            return BookmarkNode(toNodeType(bm.type), guid, bm.puid, 0, bm.title, bm.url, null)
        }
        return null
    }

    suspend fun getBookmarkByParent(guid: String): List<BookmarkNode> {
        val eList = db.getDao().findByParent(guid)
        if (eList != null) {
            return eList.map { BookmarkNode(toNodeType(it.type), it.uid, it.puid, 0, it.title, it.url, null) }
        }
        return emptyList()
    }

    override suspend fun getBookmarksWithUrl(url: String): List<BookmarkNode> {
        val eList = db.getDao().findByUrl(url)
        if (eList != null) {
            return eList.map { BookmarkNode(toNodeType(it.type), it.uid, it.puid, 0, it.title, it.url, null) }
        }
        return emptyList()
    }

    override suspend fun getTree(guid: String, recursive: Boolean): BookmarkNode? {
        val eList = db.getDao().getAll()
        val root = BookmarkEntity(1, BOOKMARK_ROOT_ID, "", null, null)

        val childrenListMap = mutableMapOf<String, MutableList<BookmarkEntity>>()

        eList.forEach {
            if (!childrenListMap.containsKey(it.puid)) childrenListMap.put(it.puid, mutableListOf())
            childrenListMap[it.puid]!!.add(it)
        }

        fun makeFolder(bm: BookmarkEntity): BookmarkNode {
            val childrenList = childrenListMap[bm.uid]
            val childrenNodeList: List<BookmarkNode>
            childrenNodeList = if (childrenList == null) {
                emptyList()
            } else {
                childrenList.map {
                    if (it.type == 1) {
                        makeFolder(it)
                    } else {
                        BookmarkNode(toNodeType(it.type), it.uid, it.puid, 0, it.title, it.url, null)
                    }
                }
            }
            return BookmarkNode(BookmarkNodeType.FOLDER, bm.uid, bm.puid, 0, bm.title, null, childrenNodeList)
        }

        return makeFolder(root)
    }

    override suspend fun runMaintenance() {
        TODO("Not yet implemented")
    }

    override suspend fun searchBookmarks(query: String, limit: Int): List<BookmarkNode> {
        return emptyList()
    }

    override suspend fun updateNode(guid: String, info: BookmarkInfo) {
        val bm = db.getDao().findById(guid)[0]
        bm.puid = info.parentGuid ?: bm.puid
        bm.title = info.title ?: bm.title
        bm.url = info.url ?: bm.url

        db.getDao().update(bm)
    }

    override suspend fun warmUp() {
        db = BookmarkDatabase.getInstance(context)
//        db.getDao().deleteAll()
//        db.getDao().insert(BookmarkEntity(0, "a item", BOOKMARK_ROOT_ID, "same title", "some url"))
    }

    private fun toNodeType(t: Int): BookmarkNodeType {
        return when (t) {
            0 -> BookmarkNodeType.ITEM
            1 -> BookmarkNodeType.FOLDER
            2 -> BookmarkNodeType.SEPARATOR
            else -> TODO("Not yet implemented")
        }
    }

    companion object {
        const val BOOKMARK_ROOT_ID = "B_ROOT"
    }
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
        var type: Int,
        @PrimaryKey() var uid: String,
        var puid: String,
        var title: String?,
        var url: String?,
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE uid == (:id)")
    suspend fun findById(id: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE url LIKE (:url)")
    suspend fun findByUrl(url: String): List<BookmarkEntity>

    @Insert
    suspend fun insert(vararg bm: BookmarkEntity)

    @Update
    suspend fun update(vararg bm: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE uid == (:id)")
    suspend fun deleteByUid(id: String): Int

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM bookmarks WHERE puid == (:guid)")
    suspend fun findByParent(guid: String): List<BookmarkEntity>
}

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {

    abstract fun getDao(): BookmarkDao

    companion object {
        private const val DATABASE_NAME = "basic-sample-db"
        private var sInstance: BookmarkDatabase? = null

        fun getInstance(context: Context): BookmarkDatabase {
            synchronized(BookmarkDatabase::class.java) {
                var ins = sInstance
                if (ins == null) {
                    ins = Room.databaseBuilder(context.applicationContext, BookmarkDatabase::class.java, DATABASE_NAME).build()
                    sInstance = ins
                }
                return ins
            }
        }
    }
}

