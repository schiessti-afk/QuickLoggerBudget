package com.quicklogger.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * `ic_action_gallery`, DESIGN §6), not Material Symbols — `material-icons-core` has
 * no camera/gallery glyph, and matching the pictogram/toolbar ink family reads more
 * consistent than pulling in `material-icons-extended` for two icons. `IconButton`
 * already gives each a 48 dp touch target (DESIGN §2/§7); the `contentDescription`
 * keeps them "not unlabeled" for screen readers even though the visible label is
 * gone.
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
                        .clip(RoundedCornerShape(4.dp)),
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
                IconButton(onClick = onCapture) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_camera),
                        contentDescription = stringResource(R.string.receipt_take_photo),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onPick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_gallery),
                        contentDescription = stringResource(R.string.receipt_choose_image),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
