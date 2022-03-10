package com.example.webrtcmeet.models

data class ResObj(
    val results: List<Result>
)

data class Result(
    val gender: String,
    val name: Name,
    val email: String,
    val picture: Picture,
    val login: Login
)

data class Name(
    val title: String,
    val first: String,
    val last: String,
    val picture: Picture
)

data class Login(
    val uuid: String,
    val username: String,
    val password: String,
    val salt: String,
    val md5: String,
    val sha1: String,
    val sha256: String
)

data class Picture(
    val large: String,
    val medium: String,
    val thumbnail: String
)
