package open.shadoweyer.shearray

import open.shadoweyer.shearray.bookmark.BookmarkIO
import org.junit.Test
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.StringWriter

class BookmarkIOTest {
    private val path="D:\\Income\\bookmarks_2021_2_20.html"
    private val path_lite="D:\\Income\\bookmarks_lite.html"
    var content: BufferedReader
    var content_lite: BufferedReader
    init {
        content=File(path).bufferedReader()
        content_lite=File(path_lite).bufferedReader()
    }

    @Test
    fun test_toBookmarkTree() {
        val rst= BookmarkIO.toBookmarkEntity(content)
        print(rst?.size)
    }
    @Test
    fun test_fromBookmarkTree() {
        var rst= BookmarkIO.toBookmarkEntity(content_lite)
        val writer:StringWriter=StringWriter()
        BookmarkIO.fromBookmarkEntity(writer,rst!!)
        val output=writer.toString()
        print(output)
    }
}