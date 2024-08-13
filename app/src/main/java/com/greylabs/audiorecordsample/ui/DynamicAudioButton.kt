package com.greylabs.audiorecordsample.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.greylabs.audiorecordsample.R
import com.greylabs.audiorecordsample.ui.DynamicAudioButtonConsts.DURATION_DEFAULT
import com.greylabs.audiorecordsample.ui.DynamicAudioButtonConsts.DURATION_SCALE
import com.greylabs.audiorecordsample.ui.DynamicAudioButtonConsts.INITIAL_VAL
import com.greylabs.audiorecordsample.ui.DynamicAudioButtonConsts.LABEL_ROTATION
import com.greylabs.audiorecordsample.ui.DynamicAudioButtonConsts.SCALE_IN
import com.greylabs.audiorecordsample.ui.DynamicAudioButtonConsts.SCALE_OUT

@Composable
fun DynamicAudioButton(
    modifier: Modifier = Modifier,
    dynamicWavesResList: List<Int> = listOf(),
    isRecordActive: Boolean = false,
    scales: List<Float>,
    onClickAction: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    var dynamicScaleLock by remember {
        mutableStateOf(true)
    }

    val animatedScales = scales.map {
        remember {
            Animatable(SCALE_OUT)
        }
    }

    var testScale by remember {
        mutableFloatStateOf(SCALE_IN)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(DynamicAudioButtonConsts.defaultButtonSize)
            .background(
                color = Color(0xffDD9200),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = true,
            ) {
                onClickAction.invoke()
            }
    ) {
        val rotationTransitions = List(dynamicWavesResList.size) { index ->
            rememberInfiniteTransition(String.format(LABEL_ROTATION, index))
        }

        val rotationDurations = List(rotationTransitions.size) {
            remember {
                DynamicAudioButtonConsts.rotationDurationList.random()
            }
        }

        val rotations = List(rotationTransitions.size) {
            remember {
                DynamicAudioButtonConsts.targetRotationList.random()
            }
        }

        val rotationStateList = List(rotationTransitions.size) { index ->
            rotationTransitions[index].animateFloat(
                initialValue = INITIAL_VAL,
                targetValue = rotations[index],
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        rotationDurations[index],
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = String.format(LABEL_ROTATION, index)
            )
        }

        var animIndex = 0
        dynamicWavesResList.forEachIndexed { index, resId ->
            Image(
                imageVector = ImageVector.vectorResource(id = resId),
                contentDescription = "",
                modifier = Modifier.graphicsLayer {
                    this.rotationZ = rotationStateList[animIndex].value
                    scaleX = if (dynamicScaleLock) testScale else animatedScales[animIndex].value
                    scaleY = if (dynamicScaleLock) testScale else animatedScales[animIndex].value
                    transformOrigin = TransformOrigin.Center
                }
            )
            if (index == animatedScales.lastIndex) {
                animIndex = 0
            } else {
                animIndex += 1
            }
        }
        val iconVector = if (isRecordActive) {
            ImageVector.vectorResource(id = R.drawable.ic_stop)
        } else {
            ImageVector.vectorResource(id = R.drawable.ic_mic)
        }
        AnimatedContent(
            targetState = iconVector,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                scaleIn(
                    animationSpec = tween(DURATION_DEFAULT),
                ).togetherWith(
                    scaleOut(
                        animationSpec = tween(DURATION_DEFAULT),
                    )
                )
            }
        ) { state ->
            Image(
                imageVector = state,
                contentDescription = ""
            )
        }
    }

    LaunchedEffect(isRecordActive) {
        if (isRecordActive) {
            animate(
                initialValue = SCALE_IN,
                targetValue = SCALE_OUT,
                animationSpec = tween(DURATION_SCALE)
            ) { value: Float, _: Float ->
                testScale = value
            }
            dynamicScaleLock = false
        } else {
            dynamicScaleLock = true
            animate(
                initialValue = SCALE_OUT,
                targetValue = SCALE_IN,
                animationSpec = tween(DURATION_SCALE)
            ) { value: Float, _: Float ->
                testScale = value
            }
            animatedScales.forEach { it.snapTo(SCALE_OUT) }
        }
    }

    scales.forEachIndexed { index, scale ->
        LaunchedEffect(scale) {
            if (isRecordActive) {
                animatedScales[index].animateTo(
                    targetValue = scale,
                    animationSpec = tween(DURATION_DEFAULT)
                )
            }
        }
    }
}

internal object DynamicAudioButtonConsts {
    const val DURATION_DEFAULT = 200
    const val DURATION_SCALE = 400
    const val SCALE_OUT = 1.1f
    const val SCALE_IN = 0.1f
    const val INITIAL_VAL = 0f
    const val LABEL_ROTATION = "rotation%s"

    val defaultButtonSize = 100.dp
    val targetRotationList = listOf(360f, -360f)
    val rotationDurationList = listOf(8500, 5000, 7000)
}