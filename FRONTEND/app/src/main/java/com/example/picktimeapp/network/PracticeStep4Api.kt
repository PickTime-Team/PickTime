//package com.example.picktimeapp.network
//

// 음악 모드 시도했던 흔적
//import retrofit2.http.GET
//import retrofit2.http.Path
//import retrofit2.Response
//
//
//data class Measure(
//    val measureIndex: Int,
//    val chordBlocks: List<String>
//)
//
//data class SongResponse(
//    val title: String,
//    val artist: String,
//    val durationSec: Int,
//    val bpm: Int,
//    val timeSignature: String,
//    val chordProgression: List<Measure>,
//    val songUri: String
//)
//
//data class PracticeStepResponse(
//    val chords: String?,
//    val song: SongResponse
//)
//
//interface PracticeStep4Api {
//    @GET("practice/{stepId}")
//    suspend fun getPracticeStep(@Path("stepId") stepId: Int): Response<PracticeStepResponse>
//}
