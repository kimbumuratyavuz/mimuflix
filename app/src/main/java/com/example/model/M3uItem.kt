package com.example.model

data class M3uItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val tvgName: String? = null,
    val durationSeconds: Long = -1L,
    val estimatedSize: String = "~1.2 GB",
    val httpReferrer: String? = null,
    val httpUserAgent: String? = null,
    val isSelected: Boolean = false
)
