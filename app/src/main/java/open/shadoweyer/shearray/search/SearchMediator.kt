package open.shadoweyer.shearray.search

import android.content.Context
import android.net.Uri

class SearchMediator(context: Context) {
    var selectedID: Int = 0
    private val searchPattern = listOf("https://m.baidu.com/s?ie=UTF-8&wd={searchTerm}", "https://cn.bing.com/search?q={searchTerm}")
    fun getSearchUrl(searchTerm: String): String {
        val s = Uri.encode(searchTerm)
        val template = searchPattern.getOrNull(selectedID) ?: searchPattern[0]
        return template.replace("{searchTerm}", s)
    }
}