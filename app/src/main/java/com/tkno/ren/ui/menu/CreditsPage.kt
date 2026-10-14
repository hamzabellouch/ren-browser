package com.tkno.ren.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.ren.R
import com.tkno.ren.ui.svg.drawablevectors.DynamicColorImageVectors
import com.tkno.ren.ui.svg.drawablevectors.coder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val creditsList = remember {
        listOf(
            Triple("Android Jetpack", "Apache License, Version 2.0", "https://github.com/androidx/androidx"),
            Triple("Kotlin", "Apache License, Version 2.0", "https://kotlinlang.org/"),
            Triple("Material Design 3", "Apache License, Version 2.0", "https://m3.material.io/"),
            Triple("Material Icons", "Apache License, Version 2.0", "https://fonts.google.com/icons"),
            Triple("Accompanist", "Apache License, Version 2.0", "https://github.com/google/accompanist"),
            Triple("ZXing", "Apache License, Version 2.0", "https://github.com/zxing/zxing"),
            Triple("App icon by Icons8", "Universal Multimedia Licensing Agreement for Icons8", "https://icons8.com/"),
        )
    }

    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.credits),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        content = { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .clip(MaterialTheme.shapes.large)
                            .clickable {}
                            .clearAndSetSemantics {},
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        val painter = rememberVectorPainter(image = DynamicColorImageVectors.coder())
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 72.dp, vertical = 48.dp),
                        )
                    }
                }
                items(creditsList) { item ->
                    Surface(modifier = Modifier.clickable { uriHandler.openUri(item.third) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    text = item.first,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = item.second,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
