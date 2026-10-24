package com.tniany.shiyu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class BottomBarDustCell(
  val topLeft: Offset,
  val size: Size,
  val origin: Offset,
  val delay: Float,
  val drift: Offset,
  val swirl: Float,
  val phase: Float,
  val radius: Float,
  val color: Color,
  val emitsDust: Boolean
)

@Composable
internal fun BottomBarDissolve(
  visible: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable (enabled: Boolean) -> Unit
) {
  val progress = animateFloatAsState(
    targetValue = if (visible) 0f else 1f,
    animationSpec = tween(if (visible) 560 else 680, easing = LinearEasing),
    label = "bottom-bar-dissolve"
  )
  val hasVisiblePixels by remember { derivedStateOf { progress.value < 1f } }
  val assembled by remember { derivedStateOf { progress.value < .12f } }
  val enabled = visible && assembled
  val surfaceColor = MaterialTheme.colorScheme.surface
  val dustColor = MaterialTheme.colorScheme.outline
  val accentColor = MaterialTheme.colorScheme.primary
  val travelSpace = 56.dp

  if (visible || hasVisiblePixels) {
    Box(
      modifier
        .then(if (enabled) Modifier else Modifier.clearAndSetSemantics {})
        .graphicsLayer {
          compositingStrategy = if (progress.value > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
        .drawWithCache {
          val contentTop = travelSpace.roundToPx().toFloat()
          val contentHeight = (size.height - contentTop).coerceAtLeast(0f)
          val cellSize = ceil(maxOf(4.dp.toPx(), sqrt(size.width * contentHeight / 900f)))
          val columns = ceil(size.width / cellSize).toInt()
          val rows = ceil(contentHeight / cellSize).toInt()
          val surfaceLeft = 20.dp.toPx()
          val surfaceRight = size.width - surfaceLeft
          val surfaceTop = contentTop + 10.dp.toPx()
          val surfaceBottom = size.height - 10.dp.toPx()
          val cornerRadius = 20.dp.toPx()
          val random = Random(7319)
          val cells = List(columns * rows) { index ->
            val left = (index % columns) * cellSize
            val top = contentTop + (index / columns) * cellSize
            val origin = Offset(left + cellSize * random.nextFloat(), top + cellSize * random.nextFloat())
            val cornerDx = maxOf(surfaceLeft + cornerRadius - origin.x, 0f, origin.x - surfaceRight + cornerRadius)
            val cornerDy = maxOf(surfaceTop + cornerRadius - origin.y, 0f, origin.y - surfaceBottom + cornerRadius)
            val insideSurface = origin.x in surfaceLeft..surfaceRight && origin.y in surfaceTop..surfaceBottom &&
              cornerDx * cornerDx + cornerDy * cornerDy <= cornerRadius * cornerRadius
            BottomBarDustCell(
              topLeft = Offset(left, top),
              size = Size(minOf(cellSize, size.width - left), minOf(cellSize, size.height - top)),
              origin = origin,
              delay = random.nextFloat() * .24f + (left / size.width) * .08f,
              drift = Offset((random.nextFloat() - .5f) * 60.dp.toPx(), -(18 + random.nextFloat() * 34).dp.toPx()),
              swirl = (3 + random.nextFloat() * 8).dp.toPx(),
              phase = random.nextFloat() * (2 * PI).toFloat(),
              radius = (.65f + random.nextFloat() * .8f).dp.toPx(),
              color = if (random.nextFloat() < .18f) accentColor else lerp(surfaceColor, dustColor, .35f + random.nextFloat() * .5f),
              emitsDust = insideSurface
            )
          }
          onDrawWithContent {
            drawContent()
            val amount = progress.value
            if (amount > 0f) {
              cells.forEach { cell ->
                val erosion = ((amount - cell.delay) / .2f).coerceIn(0f, 1f)
                if (erosion > 0f) {
                  drawRect(Color.Black, cell.topLeft, cell.size, alpha = erosion, blendMode = BlendMode.DstOut)
                }
              }
              cells.forEach { cell ->
                val lifetime = ((amount - cell.delay) / (1f - cell.delay)).coerceIn(0f, 1f)
                if (cell.emitsDust && lifetime > 0f && lifetime < 1f) {
                  val envelope = sin(lifetime * PI).toFloat()
                  val swirl = sin(lifetime * 7f + cell.phase) * envelope * cell.swirl
                  val center = cell.origin + cell.drift * lifetime + Offset(swirl, -envelope * cell.swirl * .3f)
                  val opacity = (lifetime * 9f).coerceAtMost(1f) * (1f - lifetime).pow(1.3f) * .9f
                  drawCircle(cell.color, cell.radius * (1f - lifetime * .65f), center, alpha = opacity)
                }
              }
            }
          }
        }
        .padding(top = travelSpace)
    ) {
      content(enabled)
    }
  }
}
