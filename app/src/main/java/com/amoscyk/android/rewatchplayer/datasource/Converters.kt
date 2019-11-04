package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.TypeConverter
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class Converters {

    @TypeConverter
    fun searchListToString(searchResult: List<RPSearchResult>): String {
        val moshi = Moshi.Builder().build()
        val searchList = Types.newParameterizedType(List::class.java, RPSearchResult::class.java)
        val adapter: JsonAdapter<List<RPSearchResult>> = moshi.adapter(searchList)
        return adapter.toJson(searchResult)
    }

    @TypeConverter
    fun stringToSearchList(value: String): List<RPSearchResult> {
        val moshi = Moshi.Builder().build()
        val searchList = Types.newParameterizedType(List::class.java, RPSearchResult::class.java)
        val adapter: JsonAdapter<List<RPSearchResult>> = moshi.adapter(searchList)
        return adapter.fromJson(value)!!
    }

}