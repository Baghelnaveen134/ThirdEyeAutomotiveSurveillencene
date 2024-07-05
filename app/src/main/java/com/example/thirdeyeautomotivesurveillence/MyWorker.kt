package com.example.thirdeyeautomotivesurveillence

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Do background work here
        return Result.success()
    }
}

