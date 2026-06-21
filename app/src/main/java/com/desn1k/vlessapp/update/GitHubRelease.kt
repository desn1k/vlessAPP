package com.desn1k.vlessapp.update

data class GitHubRelease(
    val tagName: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val body: String
)
