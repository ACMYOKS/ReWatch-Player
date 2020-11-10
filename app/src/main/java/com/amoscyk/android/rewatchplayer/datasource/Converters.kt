package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.TypeConverter
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchResult
import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.ResourceFormat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val intListAdapter: JsonAdapter<List<Int>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, Int::class.javaObjectType))
    private val stringListAdapter: JsonAdapter<List<String>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, String::class.java))
    private val intStringMapAdapter: JsonAdapter<Map<Int, String>> =
        moshi.adapter(Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, String::class.java))
    private val thumbnailAdapter: JsonAdapter<RPThumbnailDetails> = moshi.adapter(RPThumbnailDetails::class.java)
    private val listResourceFormatAdapter: JsonAdapter<List<ResourceFormat>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, ResourceFormat::class.java))

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
    fun intListToJson(intList: List<Int>): String {
        return intListAdapter.toJson(intList)!!
    }

    @TypeConverter
    fun getIntListFromJson(json: String): List<Int> {
        return intListAdapter.fromJson(json)!!
    }

    @TypeConverter
    fun stringListToJson(stringList: List<String>): String {
        return stringListAdapter.toJson(stringList)!!
    }

    @TypeConverter
    fun getStringListFromJson(json: String): List<String> {
        return stringListAdapter.fromJson(json)!!
    }

    @TypeConverter
    fun thumbnailDetailsToJson(thumbnailDetails: RPThumbnailDetails): String {
        return thumbnailAdapter.toJson(thumbnailDetails)!!
    }

    @TypeConverter
    fun getThumbnailDetailsFromJson(json: String): RPThumbnailDetails {
        return thumbnailAdapter.fromJson(json)!!
    }

    @TypeConverter
    fun listResourceFormatToJson(list: List<ResourceFormat>): String {
        return listResourceFormatAdapter.toJson(list)!!
    }

    @TypeConverter
    fun getListResourceFormatFromJson(json: String): List<ResourceFormat> {
        return listResourceFormatAdapter.fromJson(json)!!
    }

    @TypeConverter
    fun intStringMapToJson(map: Map<Int, String>): String {
        return intStringMapAdapter.toJson(map)!!
    }

    @TypeConverter
    fun getIntStringMapFromJson(json: String): Map<Int, String> {
        return intStringMapAdapter.fromJson(json)!!
    }
}