package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.TypeConverter
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchResult
import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun searchListToString(searchResult: List<RPSearchResult>): String {
        val searchList = Types.newParameterizedType(List::class.java, RPSearchResult::class.java)
        val adapter: JsonAdapter<List<RPSearchResult>> = moshi.adapter(searchList)
        return adapter.toJson(searchResult)
    }

    @TypeConverter
    fun stringToSearchList(value: String): List<RPSearchResult> {
        val searchList = Types.newParameterizedType(List::class.java, RPSearchResult::class.java)
        val adapter: JsonAdapter<List<RPSearchResult>> = moshi.adapter(searchList)
        return adapter.fromJson(value)!!
    }

    @TypeConverter
    fun stringListToJson(stringList: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter: JsonAdapter<List<String>> = moshi.adapter(type)
        return adapter.toJson(stringList)!!
    }

    @TypeConverter
    fun getStringListFromJson(json: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter: JsonAdapter<List<String>> = moshi.adapter(type)
        return adapter.fromJson(json)!!
    }

    @TypeConverter
    fun thumbnailDetailsToJson(thumbnailDetails: RPThumbnailDetails): String {
        val adapter: JsonAdapter<RPThumbnailDetails> = moshi.adapter(RPThumbnailDetails::class.java)
        return adapter.toJson(thumbnailDetails)!!
    }

    @TypeConverter
    fun getThumbnailDetailsFromJson(json: String): RPThumbnailDetails {
        val adapter: JsonAdapter<RPThumbnailDetails> = moshi.adapter(RPThumbnailDetails::class.java)
        return adapter.fromJson(json)!!
    }
}