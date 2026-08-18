package com.quicklogger.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.quicklogger.app.R
import java.io.File

/**
 * The optional receipt on the Log screen: two actions when empty, a thumbnail plus a
 * remove control once one is attached.
 *
 * Camera/gallery are generated ink-line glyphs (`ic_action_camera` /
 * `ic_action_gallery`, DESIGN §6) inside 56 dp filled tiles (`surfaceContainer`,
 * `shapes.medium`). The `contentDescription` on each icon keeps them "not unlabeled"
 * for screen readers (DESIGN §7).
 */
@Composable
fun ReceiptAttachment(
    receiptFile: File?,
    isAttaching: Boolean,
    onCapture: () -> Unit,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    Column(modifier) {
        when {
            isAttaching -> CircularProgressIndicator(Modifier.size(24.dp))

            receiptFile != null -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    // A file:// Uri rather than a raw path: every receipt is a fresh
                    // uuid, so nothing stale can be keyed in Coil's cache.
                    model = Uri.fromFile(receiptFile),
                    contentDescription = stringResource(R.string.receipt_attached),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = stringResource(R.string.receipt_attached),
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.receipt_remove),
                    )
                }
            }

            else -> Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReceiptActionTile(
                    onClick = onCapture,
                    painter = painterResource(R.drawable.ic_action_camera),
                    contentDescription = stringResource(R.string.receipt_take_photo),
                )
                ReceiptActionTile(
                    onClick = onPick,
                    painter = painterResource(R.drawable.ic_action_gallery),
                    contentDescription = stringResource(R.string.receipt_choose_image),
                )
            }
        }

        errorText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ReceiptActionTile(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
            )
        }
    }
}
