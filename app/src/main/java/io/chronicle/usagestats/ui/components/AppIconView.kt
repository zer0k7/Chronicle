package io.chronicle.usagestats.ui.components

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.chronicle.usagestats.R
import io.chronicle.usagestats.ui.theme.ColorRemoved
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val iconMemoryCache = ConcurrentHashMap<String, Drawable>()

@Composable
fun AppIconView(
    packageName: String,
    appName: String,
    isRemoved: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (isRemoved) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ColorRemoved.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = appName,
                tint = ColorRemoved,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        var iconDrawable by remember(packageName) { mutableStateOf(iconMemoryCache[packageName]) }

        LaunchedEffect(packageName) {
            if (iconDrawable == null) {
                val loaded = withContext(Dispatchers.IO) {
                    try {
                        val pm = context.packageManager
                        val icon = pm.getApplicationIcon(packageName)
                        iconMemoryCache[packageName] = icon
                        icon
                    } catch (_: PackageManager.NameNotFoundException) {
                        null
                    } catch (_: Exception) {
                        null
                    }
                }
                iconDrawable = loaded
            }
        }

        val currentDrawable = iconDrawable
        if (currentDrawable != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentDrawable)
                    .crossfade(true)
                    .build(),
                contentDescription = appName,
                modifier = modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = appName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
