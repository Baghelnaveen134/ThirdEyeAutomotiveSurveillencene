package com.example.thirdeyeautomotivesurveillence

import java.io.File

data class TestResult(
    val testCaseId: String,
    val description: String,
    val result: Boolean,
    val images: List<File>,
    val videos: List<File>,
    val logs: String
)
