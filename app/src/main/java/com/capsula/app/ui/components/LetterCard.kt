package com.capsula.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.capsula.app.ui.theme.Parchment
import com.capsula.app.ui.theme.ParchmentInk

/** A sheet of paper. The letter always looks like paper, in light and dark theme alike. */
@Composable
fun LetterCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Parchment, contentColor = ParchmentInk),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides ParchmentInk) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp), content = content)
        }
    }
}
