package com.example.picktimeapp.ui.game.play

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.picktimeapp.R
import com.example.picktimeapp.ui.components.PauseDialogCustom
import com.example.picktimeapp.ui.theme.Brown40
import com.example.picktimeapp.ui.theme.Brown80
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun GamePlayScreen(
    navController: NavController,
    songId: Int
) {
    val viewModel : GamePlayViewModel = hiltViewModel()
    // 노래 불러오기 위해
    val context = LocalContext.current
    // 현재 멈춤을 눌렀는지 안눌렀는지 확인할 변수
    val (showPauseDialog, setShowPauseDialog) = remember { mutableStateOf(false) }

    LaunchedEffect(songId) {
        viewModel.loadGamePlay(songId)
    }

    BoxWithConstraints (
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                clip = false
            }
            .padding(top = 20.dp, bottom = 20.dp)
    ){
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 게임 데이터 불러오기
        val gameData = viewModel.gameData.collectAsState().value
        // 모든 코드 가지고오기
        val chordProgression = gameData?.chordProgression ?: emptyList()

        // 음악 재생하기
        DisposableEffect(gameData?.songUri) {
            val mediaPlayer = MediaPlayer()
            if (gameData?.songUri != null) {
                try {
                    mediaPlayer.setDataSource(context, Uri.parse(gameData.songUri))
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // 🧹 컴포저블이 dispose될 때 음악도 정리
            onDispose {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            }
        }

        // 코드들 일단 싹 다 불러오기
        val allChords = remember(chordProgression) {
            chordProgression.flatMap { it.chordBlocks }
        }

        // 경과 시간 상태 추가
        var elapsedTime by remember { mutableStateOf(0f) }

        // 코드 몇 초동안 보여야하는지 계산하기
        val durationPerNoteSec = remember(chordProgression, gameData?.durationSec) {
            val totalNotes = allChords.size
            (gameData?.durationSec?.toFloat() ?: 1f) / totalNotes
        }

        // 현재 코드 몇 번째인지
        val currentChordIndex = remember { mutableStateOf(0) }

        // 시간 계산해서 현재 코드 몇 번쨰인지 업데이트 및 경과 시간 추적
        LaunchedEffect(allChords, gameData?.durationSec) {
            val startTime = System.currentTimeMillis()
            while (currentChordIndex.value < allChords.size) {
                val current = (System.currentTimeMillis() - startTime) / 1000f
                elapsedTime = current
                val newIndex = (current / durationPerNoteSec).toInt()
                if (newIndex != currentChordIndex.value && newIndex < allChords.size) {
                    currentChordIndex.value = newIndex
                }
                kotlinx.coroutines.delay(16) // 약 60fps
            }
        }

        // 🔥 X를 제외한 실제 코드 2개 가져오기
        val (current, next) = getNextVisibleChords(allChords, currentChordIndex.value, 2)

        Column (modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                clip = false
            }
        ) {
            TopBar(
                onPauseClick = { setShowPauseDialog(true)},
                screenWidth = screenWidth,
                modifier = Modifier
                    .zIndex(3f)
            )

            // 코드 애니메이션 쪽
            Box (modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .graphicsLayer {
                    clip = false  // overflow 허용!
                }
            ){
                Spacer(Modifier.height(screenHeight * 0.05f))

                GuitarImage(
                    imageRes = R.drawable.guitar_neck,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    modifier = Modifier.zIndex(1f)
                )

                if (gameData != null) {
                    SlidingCodeBar(
                        screenWidth = screenWidth,
                        currentIndex = currentChordIndex.value,
                        elapsedTime = elapsedTime,
                        totalDuration = gameData.durationSec.toFloat(),
                        chordProgression = gameData.chordProgression,
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(top = screenHeight * 0.14f)
                            .zIndex(2f)
                            .graphicsLayer {
                                clip = false
                            }
                    )
                }
            }

            // 코드 & 영상 나오는 쪽
            Box(modifier = Modifier
                .fillMaxSize()
                .weight(1f)
            ) {
                ChordSection(
                    currentChord = current,
                    nextChord = next,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(end = screenWidth * 0.3f),
                    imageSize = screenWidth * 0.25f,
                    screenWidth = screenWidth
                )
            }

            // 팝업창 띄우기
            if (showPauseDialog) {
                PauseDialogCustom(
                    screenWidth = screenWidth,
                    onDismiss = { setShowPauseDialog(false) },
                    onExit = {
                        setShowPauseDialog(false)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

// 위에 상단 버튼
@Composable
fun TopBar(
    onPauseClick: () -> Unit,
    screenWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenWidth * 0.02f),
        horizontalArrangement = Arrangement.Absolute.Right,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 멈춤 버튼
        Image(
            painter = painterResource(id = R.drawable.pause_btn),
            contentDescription = "Pause",
            modifier = Modifier
                .size(screenWidth * 0.03f)
                .clickable { onPauseClick() }
        )
    }
}

// 기타 넥 이미지
@Composable
fun GuitarImage(imageRes: Int, screenWidth: Dp, screenHeight: Dp,modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Guitar Neck",
        modifier = Modifier
            .offset(x = -screenWidth * 0.1f)   // 왼쪽으로 조금 이동
            .height(screenHeight * 0.55f)
            .scale(1.8f)
    )
}

// 코드 나오는 부분
@Composable
fun ChordSection(
    modifier: Modifier = Modifier,
    imageSize: Dp,
    screenWidth: Dp,
    currentChord: String?,
    nextChord: String?,
) {
    Row(
        modifier = modifier,
    ) {
        if(!currentChord.isNullOrBlank() && currentChord != "X") {
            // 왼쪽 코드
            ChordBlock(
                title = currentChord,
                imageRes = getChordImageRes(currentChord),
                imageSize = imageSize,
                titleColor = Brown80,
                isHighlighted = true,
                screenWidth = screenWidth
            )
        }

        Spacer(modifier = Modifier.width(screenWidth * 0.05f))

        if(!nextChord.isNullOrBlank() && nextChord != "X") {
            ChordBlock(
                title = nextChord,
                imageRes = getChordImageRes(nextChord),
                imageSize = imageSize,
                titleColor = Brown40,
                screenWidth = screenWidth,
                modifier = Modifier.alpha(0.5f)
            )
        }
        // 여기에다가 사용자 영상 띄우기!!
    }
}

@Composable
fun getChordImageRes(chord: String): Int {
    return when (chord.uppercase()) {
        "G" -> R.drawable.code_g
        "C" -> R.drawable.code_c
        "D" -> R.drawable.code_d
        "A" -> R.drawable.code_a
        "AM" -> R.drawable.code_am
        else -> R.drawable.code_c
    }
}

@Composable
fun getNextVisibleChords(allChords: List<String>, fromIndex: Int, count: Int): List<String?> {
    val result = mutableListOf<String?>()
    var index = fromIndex

    while (index < allChords.size && result.size < count) {
        val chord = allChords[index]
        if (chord != "X") {
            result.add(chord)
        }
        index++
    }

    // 부족하면 null로 채움
    while (result.size < count) {
        result.add(null)
    }

    return result
}

@Composable
fun ChordBlock(
    title: String,
    imageRes: Int,
    imageSize: Dp,
    titleColor: Color,
    isHighlighted: Boolean = false,
    screenWidth: Dp,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.Start, modifier = modifier) {
        Text(
            text = title,
            modifier = Modifier.padding(start = screenWidth * 0.02f),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (screenWidth * 0.04f).value.sp,
                fontWeight = FontWeight.Bold
            ),
            color = titleColor
        )
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Chord Diagram: $title",
            modifier = Modifier
                .size(imageSize)
        )
    }
}
