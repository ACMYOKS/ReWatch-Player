package com.amoscyk.android.rewatchplayer.datasource.vo

class NoNetworkException(message: String? = "No network connection."): Exception(message)
// YouTube data api exceptions
class GmsUsernameNotSetException(message: String? = "Username for Google Mobile Service is not set."): Exception(message)
class GooglePlayServicesNotAvailableException(message: String? = "Google Play Service is not available."): Exception(message)
// cloud exceptions
class InvalidArgumentException(message: String?): Exception(message)
class NoSuchVideoIdException(message: String?): Exception(message)
class ServerErrorException(message: String?): Exception(message)
// data exception
class NoVideoMetaException : Exception("No video meta.")
class NoYtInfoException : Exception("No YtInfo.")
class NoAvailableQualityException : Exception("No available quality.")

data class ExceptionWithActionTag(val e: Exception, val actionTag: String)