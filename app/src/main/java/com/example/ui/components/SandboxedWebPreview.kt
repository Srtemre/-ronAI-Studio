package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import com.example.util.ProjectFileManager
import com.example.util.SecurityValidator
import java.io.ByteArrayInputStream
import java.io.File

enum class PreviewDeviceMode(val label: String, val widthDp: Int?) {
    FULL("Fluid", null),
    MOBILE("Phone", 360),
    TABLET("Tablet", 600)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxedWebPreview(
    project: Project,
    fileManager: ProjectFileManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf(project.name) }
    var deviceMode by remember { mutableStateOf(PreviewDeviceMode.FULL) }
    var hasError by remember { mutableStateOf(false) }
    var errorDescription by remember { mutableStateOf<String?>(null) }

    val projectDir = remember(project.id) {
        fileManager.getProjectDir(project.id)
    }

    val entryFile = remember(project.id, project.targetUrl, project.lastModified) {
        fileManager.getEntryPointFile(project.id, project.targetUrl)
    }

    val isOnlineSource = remember(project.sourceType, project.targetUrl) {
        (project.sourceType == SourceType.URL || project.sourceType == SourceType.PWA) &&
            (project.targetUrl.startsWith("http://", ignoreCase = true) || project.targetUrl.startsWith("https://", ignoreCase = true))
    }

    val initialLoadUrl = remember(project, entryFile, isOnlineSource) {
        when {
            isOnlineSource -> project.targetUrl.trim()
            entryFile.exists() -> "file://${entryFile.absolutePath}"
            else -> "about:blank"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("sandboxed_web_preview")
    ) {
        // Preview Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator (Green = Ready, Orange = Loading, Red = Error)
            val indicatorColor = when {
                hasError -> Color(0xFFFF3B30)
                isLoading -> Color(0xFFFF9500)
                else -> Color(0xFF34C759)
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pageTitle.ifBlank { project.name },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (initialLoadUrl.startsWith("file://")) {
                        "Local Sandbox: ${entryFile.name}"
                    } else if (hasError) {
                        "Connection Error"
                    } else {
                        initialLoadUrl
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mode Selector Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
            ) {
                IconButton(
                    onClick = { deviceMode = PreviewDeviceMode.FULL },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Full width",
                        tint = if (deviceMode == PreviewDeviceMode.FULL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { deviceMode = PreviewDeviceMode.MOBILE },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Mobile mode",
                        tint = if (deviceMode == PreviewDeviceMode.MOBILE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { deviceMode = PreviewDeviceMode.TABLET },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tablet,
                        contentDescription = "Tablet mode",
                        tint = if (deviceMode == PreviewDeviceMode.TABLET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Reload Button
            IconButton(
                onClick = {
                    hasError = false
                    errorDescription = null
                    isLoading = true
                    webViewRef?.reload() ?: webViewRef?.loadUrl(initialLoadUrl)
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_reload_preview")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Preview Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(vertical = if (deviceMode != PreviewDeviceMode.FULL) 12.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            val canvasModifier = when (deviceMode) {
                PreviewDeviceMode.FULL -> Modifier.fillMaxSize()
                PreviewDeviceMode.MOBILE -> Modifier
                    .width(360.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                PreviewDeviceMode.TABLET -> Modifier
                    .width(600.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            }

            Box(
                modifier = canvasModifier
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = project.enableJavaScript
                                domStorageEnabled = project.enableLocalStorage
                                allowFileAccess = true
                                allowContentAccess = false // Block content:// provider access
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = true
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = false // Prevent cross-origin arbitrary file leaks
                                cacheMode = if (project.enableOfflineCaching) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = false
                                setGeolocationEnabled(false)
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    if (!hasError) {
                                        pageTitle = view?.title ?: project.name
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUri = request?.url ?: return null
                                    val scheme = reqUri.scheme?.lowercase() ?: ""

                                    // Strictly sandbox local file:// requests to project workspace only
                                    if (scheme == "file") {
                                        val reqPath = reqUri.path ?: ""
                                        val reqFile = File(reqPath)
                                        if (!SecurityValidator.isPathSafe(projectDir, reqFile)) {
                                            // Intercept & block unauthorized file traversal outside project dir
                                            return WebResourceResponse(
                                                "text/plain",
                                                "UTF-8",
                                                403,
                                                "Forbidden",
                                                mapOf("Access-Control-Allow-Origin" to "*"),
                                                ByteArrayInputStream("Security sandbox violation: Access denied".toByteArray(Charsets.UTF_8))
                                            )
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError = true
                                        val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            error?.description?.toString()
                                        } else {
                                            "Network or connection error"
                                        }
                                        errorDescription = desc ?: "Unable to load the requested webpage"
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                        isLoading = false
                                        hasError = true
                                        errorDescription = "HTTP Error ${errorResponse?.statusCode}: ${errorResponse?.reasonPhrase ?: "Server error"}"
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    // Prevent webview crash from taking down whole app
                                    hasError = true
                                    errorDescription = "WebView render process disconnected. Tap reload to restart."
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank() && !hasError) {
                                        pageTitle = title
                                    }
                                }
                            }

                            if (initialLoadUrl.isNotBlank()) {
                                loadUrl(initialLoadUrl)
                            }
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Elegant In-Preview Error / Offline Placeholder
                if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isOnlineSource) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isOnlineSource) "Website Offline or Unreachable" else "Preview Load Error",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = errorDescription ?: "Failed to establish connection to target URL.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            IosButton(
                                text = "Try Again",
                                onClick = {
                                    hasError = false
                                    errorDescription = null
                                    isLoading = true
                                    webViewRef?.loadUrl(initialLoadUrl)
                                },
                                style = IosButtonStyle.PRIMARY,
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }

                if (isLoading && !hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
