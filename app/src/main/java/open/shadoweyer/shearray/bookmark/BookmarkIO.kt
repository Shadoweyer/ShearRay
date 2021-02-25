package open.shadoweyer.shearray.bookmark

import java.util.*
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import java.io.Reader
import java.io.Writer

private const val cl_ab = '<'.toInt()
private const val cr_ab = '>'.toInt()
private const val cq = '"'.toInt()
private const val csp = ' '.toInt()
private const val ceq = '='.toInt()

//read string
private fun readUntilMeet(sb: StringBuilder, r: BackableReader, targetList: String): String {
    var charValue: Char
    var intValue: Int = r.read()
    while (intValue != -1) {
        charValue = intValue.toChar()
        if (charValue !in targetList) {
            sb.append(charValue)
        } else {
            break
        }
        intValue = r.read()
    }
    val rst = sb.toString()
    sb.setLength(0)
    return rst
}

//read quoted string
private fun readQStr(sb: StringBuilder, r: BackableReader): String {
    return readUntilMeet(sb, r, "\"")
}

private fun readTextTag(sb: StringBuilder, r: BackableReader): Tag {
    val str = readUntilMeet(sb, r, "<").trim()
    r.back()
    return Tag("text", null, str)
}

private fun skipSpace(r: BackableReader) {
    var charValue: Char
    val sp = "\r\t\n "
    var intValue = r.read()
    while (intValue != -1) {
        charValue = intValue.toChar()
        if (charValue !in sp) {
            break
        }
        intValue = r.read()
    }
}

private fun readNamedTag(sb: StringBuilder, r: BackableReader): Tag {
    var value = r.read()
    return when (value) {
        -1 -> Tag("")
        else -> {
            r.back()
            val name = readUntilMeet(sb, r, " >").trim()
            value = r.last()
            if (value == -1 || value == cr_ab) {
                Tag(name)
            } else {
                Tag(name, readAttrMap(sb, r))
            }
        }
    }
}

private fun readAttr(sb: StringBuilder, r: BackableReader, map: MutableMap<String, String>) {
    skipSpace(r)
    r.back()
    val key = readUntilMeet(sb, r, " =>").trim()
    if (key == "") return
    when (r.last()) {
        ceq -> {
            skipSpace(r)
            if (r.last() == cq) {
                map[key] = readQStr(sb, r)
                return
            } else {
                r.back()
                map[key] = ""
                return
            }
        }
        -1, cr_ab -> {
            map[key] = ""
            return
        }
        csp -> {
            skipSpace(r)
            if (r.last() == ceq) {
                skipSpace(r)
                if (r.last() == cq) {
                    map[key] = readQStr(sb, r)
                    return
                } else {
                    r.back()
                    map[key] = ""
                    return
                }
            } else {
                r.back()
                map[key] = ""
                return
            }
        }
    }
}

private fun readAttrMap(sb: StringBuilder, r: BackableReader): MutableMap<String, String> {
    val attrMap = mutableMapOf<String, String>()
    while (r.last() != -1) {
        readAttr(sb, r, attrMap)
        if (r.last() == cr_ab) {
            break
        }
    }
    return attrMap
}

private fun readTag(r: BackableReader): Tag? {
    val value = r.read()
    val sb = StringBuilder()
    return when (value) {
        cl_ab -> readNamedTag(sb, r)
        -1 -> null
        else -> {
            r.back()
            return readTextTag(sb, r)
        }
    }
}

object BookmarkIO {
    private const val fileHeader = "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<Title>Bookmarks</Title>\n<H1>Bookmarks</H1>\n"

    private fun writeNode(content: Writer, rootid: String,parentMap:MutableMap<String, MutableList<BookmarkEntity>>,indent:String) {

        val cList= parentMap[rootid] ?: return
        content.write("$indent<DL><p>\n")
        for(e in cList){
            val cIndent="$indent\t"
            if (e.type == 1) {
                content.write("$cIndent<DT><H3>${e.title}</H3>\n")
                writeNode(content,e.uid,parentMap,cIndent)
            } else {
                content.write("$cIndent<DT><A HREF=\"${e.url}\">${e.title}</A>\n")
            }
        }
        content.write("$indent</DL><p>\n")
    }

    fun fromBookmarkEntity(content: Writer, eList: List<BookmarkEntity>) {
        val childrenListMap = mutableMapOf<String, MutableList<BookmarkEntity>>()

        eList.forEach {
            if (!childrenListMap.containsKey(it.puid)) childrenListMap[it.puid] = mutableListOf()
            childrenListMap[it.puid]?.add(it)
        }
        val root=LocalBookmarksStorage.BOOKMARK_ROOT_ID

        content.write(fileHeader)
        writeNode(content, root,childrenListMap,"")
    }

    fun toBookmarkEntity(content: Reader): MutableList<BookmarkEntity>? {
        val r = BackableReader(content)
        var tag = readTag(r)
        //check doc type
        if (tag != null) {
            if (tag.name != "!DOCTYPE" || tag.attr?.containsKey("NETSCAPE-Bookmark-file-1") == false) return null
        } else {
            return null
        }

        val tags = mutableListOf<Tag>()
        tag = readTag(r)
        while (tag != null) {
            if (tag.name != "text" || tag.value != "") {
                tags.add(tag)
            }
            tag = readTag(r)
        }

        val startIndex = tags.indexOfFirst { it.name == "DL" }

        val tree = ImportedBookmarkTree()
        tree.solve(tags, startIndex)

        return tree.entityList
    }
}

class ImportedBookmarkTree {

    private lateinit var curItem: BookmarkEntity
    private lateinit var curFolder: BookmarkEntity
    private var folderStack = mutableListOf<BookmarkEntity>()

    private var mode = "DL"
    private var modeStack = mutableListOf<String>()

    lateinit var entityList:MutableList<BookmarkEntity>

    internal fun solve(tags: MutableList<Tag>, startIndex: Int) {
        mode = "DL"
        entityList= mutableListOf()
        curItem = BookmarkEntity(1, LocalBookmarksStorage.BOOKMARK_ROOT_ID, "", "","")
        curFolder = BookmarkEntity(1,"virtual", "", "","")

        var tag: Tag
        for (index in startIndex..tags.size) {
            tag = tags[index]
            //ignore tag
            if (tag.name == "p") continue

            when (mode) {
                "DL" -> solveDL(tag)
                "DT" -> solveDT(tag)
                "H3" -> solveH3(tag)
                "A" -> solveA(tag)
                else -> {
                    throw Exception("wrong tag")
                }
            }
            if (folderStack.size == 0) break
        }
    }

    private fun pushMode(newMode: String) {
        modeStack.add(mode)
        mode = newMode
    }

    private fun popMode() {
        mode = modeStack.removeLast()
    }

    private fun solveDL(tag: Tag) {
        when (tag.name) {
            "DT" -> {
                val uid = UUID.randomUUID().toString()
                curItem = BookmarkEntity(0,uid,"","","")
                pushMode("DT")
            }
            "DL" -> {
                folderStack.add(curFolder)
                curFolder = curItem
                pushMode("DL")
            }
            "/DL" -> {
                curItem = curFolder
                curFolder = folderStack.removeLast()
                popMode()
            }
            else -> {
                throw Exception("wrong tag")
            }
        }
    }

    private fun solveDT(tag: Tag) {
        when (tag.name) {
            "H3" -> {
                curItem.type = 1
                pushMode("H3")
            }
            "A" -> {
                curItem.type = 0
                curItem.url = tag.attr?.get("HREF")
                pushMode("A")
            }
            else -> {
                throw Exception("wrong tag")
            }
        }
    }

    private fun solveA(tag: Tag) {
        when (tag.name) {
            "text" -> {
                curItem.title = tag.value
            }
            "/A" -> {
                curItem.puid=curFolder.uid
                entityList.add(curItem)
                popMode()
                popMode()
            }
            else -> {
                throw Exception("wrong tag")
            }
        }
    }

    private fun solveH3(tag: Tag) {
        when (tag.name) {
            "text" -> {
                curItem.title = tag.value
            }
            "/H3" -> {
                curItem.puid=curFolder.uid
                entityList.add(curItem)
                popMode()
                popMode()
            }
            else -> {
                throw Exception("wrong tag")
            }
        }
    }
}

private class BackableReader(var r: Reader) {
    private var cached: Int = 0
    private var fromCache = false
    fun read(): Int {
        if (fromCache) {
            fromCache = false
        } else {
            cached = r.read()
        }
        return cached
    }

    fun back() {
        fromCache = true
    }

    fun last(): Int {
        return cached
    }
}

internal class Tag(
        val name: String,
        val attr: MutableMap<String, String>? = null,
        val value: String = ""
)