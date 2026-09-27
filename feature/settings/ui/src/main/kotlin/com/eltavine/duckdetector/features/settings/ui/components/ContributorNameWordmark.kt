/*
 * Copyright 2026 Duck Apps Contributor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.features.settings.ui.components

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** A small credits signature made from the names in the same bundled snapshot as the avatar wall. */
@Composable
fun ContributorNameWordmark(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val names = remember(context) {
        loadContributorSnapshots(context).map { it.name }
    }
    if (names.isEmpty()) return

    val nameColor = MaterialTheme.colorScheme.primary.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f).toArgb()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.9f)
            .semantics { contentDescription = "Duck Detector" },
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val width = size.width
        val height = size.height
        val letters = Path().apply {
            addWord("DUCK", RectF(width * 0.06f, height * 0.05f, width * 0.94f, height * 0.49f))
            addWord("DETECTOR", RectF(width * 0.04f, height * 0.51f, width * 0.96f, height * 0.95f))
        }
        val canvas = drawContext.canvas.nativeCanvas
        canvas.drawPath(letters, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor })

        val textSize = width / 34f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = nameColor
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            this.textSize = textSize
        }
        val spacing = textSize * 0.65f
        var nameIndex = 0
        var baseline = textSize
        var row = 0
        canvas.save()
        canvas.clipPath(letters)
        while (baseline < height + textSize) {
            var x = if (row % 2 == 0) 0f else -width * 0.08f
            while (x < width) {
                val name = names[nameIndex % names.size]
                canvas.drawText(name, x, baseline, namePaint)
                x += namePaint.measureText(name) + spacing
                nameIndex++
            }
            baseline += textSize * 1.15f
            row++
        }
        canvas.restore()
    }
}

private fun Path.addWord(word: String, target: RectF) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textSize = 100f
    }
    val wordPath = Path()
    paint.getTextPath(word, 0, word.length, 0f, 0f, wordPath)
    val bounds = RectF()
    wordPath.computeBounds(bounds, true)
    val transform = Matrix().apply { setRectToRect(bounds, target, Matrix.ScaleToFit.CENTER) }
    wordPath.transform(transform)
    addPath(wordPath)
}
