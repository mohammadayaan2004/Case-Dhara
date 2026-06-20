package com.casedhara.ui.screens.summarizer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casedhara.domain.model.CaseSummary
import com.casedhara.domain.model.mappingLabel
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.ErrorCard
import com.casedhara.ui.components.SectionNavEffects
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.SummaryView
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.screens.mapper.SectionNavViewModel
import androidx.compose.ui.draw.alpha
import com.casedhara.util.NetworkResult
import java.io.File
import java.io.FileOutputStream

private const val NOTIF_CHANNEL_ID   = "pdf_download"
private const val NOTIF_CHANNEL_NAME = "PDF Downloads"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarizerScreen(
    navController: NavController,
    viewModel: SummarizerViewModel = hiltViewModel(),
    sectionNavViewModel: SectionNavViewModel = hiltViewModel(),
) {
    val context  = LocalContext.current
    val state    by viewModel.state.collectAsStateWithLifecycle()
    val pastedText by viewModel.pastedText.collectAsStateWithLifecycle()
    val fileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val savedCases by viewModel.savedCases.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sectionLoading = SectionNavEffects(
        navController = navController,
        snackbarHostState = snackbarHostState,
        sectionNavViewModel = sectionNavViewModel,
    )

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.summarizePdf(context, it) } }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Case Summarizer") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val s = state
                    if (s is NetworkResult.Success) {
                        val saveTitle = fileName ?: s.data.caseTitle
                        val isAlreadySaved = savedCases.any { it.title == saveTitle }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (!isAlreadySaved) {
                                        viewModel.saveCase(s.data)
                                        snackbarHostState.showSnackbar(
                                            "✓ Case saved to Saved Cases",
                                            duration = SnackbarDuration.Short,
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Already saved in Saved Cases",
                                            duration = SnackbarDuration.Short,
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (isAlreadySaved) Icons.Default.BookmarkAdded
                                else Icons.Default.BookmarkAdd,
                                contentDescription = "Save case",
                                tint = if (isAlreadySaved) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        SplashThemeBackground {
            AshokaChakraAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(0.15f),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
            Button(
                onClick = { pdfPicker.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload PDF")
                }
            }
            fileName?.let {
                Text(
                    text = "Selected: $it",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Or paste judgment text:", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = pastedText,
                onValueChange = viewModel::onPastedTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("Minimum 100 characters…") },
            )
            Button(
                onClick = { viewModel.summarizeText() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Summarize text")
            }
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is NetworkResult.Loading -> CircularProgressIndicator()
                is NetworkResult.Error   -> ErrorCard(s.message)
                is NetworkResult.Success -> {
                    Box {
                        Column {
                            SummaryActionBar(summary = s.data, context = context)
                            Spacer(Modifier.height(8.dp))
                            SummaryView(
                                summary = s.data,
                                onSectionClick = sectionNavViewModel::openSection,
                            )
                        }
                        if (sectionLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
                is NetworkResult.Idle -> Text(
                    text = "Upload a PDF or paste text (100+ chars). Summaries may take up to ~60s.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            }
        }
    }
}

// ── Copy / Share / Download PDF bar ──────────────────────────────────────────

@Composable
private fun SummaryActionBar(summary: CaseSummary, context: Context) {
    val summaryText = buildSummaryText(summary)

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        glowColor = MaterialTheme.colorScheme.secondary,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Case Summary",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { copySummaryToClipboard(context, summaryText) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { shareSummary(context, summary.caseTitle, summaryText) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { downloadSummaryAsPdf(context, summary) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildSummaryText(summary: CaseSummary): String = buildString {
    appendLine("CASE SUMMARY")
    appendLine("============")
    summary.caseId?.let        { appendLine("Case ID  : $it") }
    appendLine("Case     : ${summary.caseTitle}")
    appendLine("Court    : ${summary.court}")
    summary.judge?.let         { appendLine("Judge    : $it") }
    summary.date?.let          { appendLine("Date     : $it") }
    summary.citation?.let      { appendLine("Citation : $it") }
    summary.petitioner?.let    { appendLine("Petitioner  : $it") }
    summary.respondent?.let    { appendLine("Respondent  : $it") }
    summary.caseType?.let      { appendLine("Case Type   : $it") }
    appendLine()

    if (summary.sectionsInvoked.isNotEmpty()) {
        appendLine("SECTIONS INVOKED")
        summary.sectionsInvoked.forEach { appendLine("  • ${it.mappingLabel()}") }
        appendLine()
    }

    appendLine("FACTS OF CASE"); appendLine(summary.factsOfCase); appendLine()

    if (summary.legalIssues.isNotEmpty()) {
        appendLine("LEGAL ISSUES")
        summary.legalIssues.forEach { appendLine("• $it") }
        appendLine()
    }

    appendLine("PETITIONER ARGUMENTS"); appendLine(summary.petitionerArguments); appendLine()
    appendLine("RESPONDENT ARGUMENTS"); appendLine(summary.respondentArguments); appendLine()

    if (summary.courtObservations.isNotBlank()) {
        appendLine("COURT OBSERVATIONS"); appendLine(summary.courtObservations); appendLine()
    }

    if (summary.legalProvisions.isNotEmpty()) {
        appendLine("LEGAL PROVISIONS")
        summary.legalProvisions.forEach { appendLine("• $it") }
        appendLine()
    }

    if (summary.precedentsCited.isNotEmpty()) {
        appendLine("PRECEDENTS CITED")
        summary.precedentsCited.forEach { appendLine("• $it") }
        appendLine()
    }

    appendLine("FINAL JUDGMENT"); appendLine(summary.finalJudgment); appendLine()
    appendLine("SUMMARY"); appendLine(summary.summary)
    appendLine()
    appendLine("— Generated by Case Dhara")
}

private fun copySummaryToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Case Summary", text))
    Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareSummary(context: Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Case Summary: $title")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Case Summary via"))
}

/**
 * Generates a multi-page PDF and saves it to the public Downloads folder.
 * Uses MediaStore on Android 10+ so the file appears in the Downloads app.
 * Shows a system notification; tapping it opens the PDF directly.
 */
private fun downloadSummaryAsPdf(context: Context, summary: CaseSummary) {
    try {
        val pdfBytes = buildPdfBytes(summary)
        val safeTitle = summary.caseTitle.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").take(40)
        val fileName  = "Summary_${safeTitle}_${System.currentTimeMillis()}.pdf"

        var savedUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore (no storage permission needed)
            val resolver      = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(pdfBytes) }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                savedUri = uri
            }
        } else {
            // Android 9 and below — write to external Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, fileName)
            FileOutputStream(outFile).use { it.write(pdfBytes) }
            savedUri = Uri.fromFile(outFile)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, savedUri))
        }

        if (savedUri != null) {
            Toast.makeText(context, "PDF saved to Downloads:\n$fileName", Toast.LENGTH_LONG).show()
            showDownloadNotification(context, fileName, savedUri)
        } else {
            Toast.makeText(context, "PDF export failed: could not create file", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "PDF export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun buildPdfBytes(summary: CaseSummary): ByteArray {
    val document   = PdfDocument()
    val pageWidth  = 595   // A4 width in points at 72 dpi
    val pageHeight = 842   // A4 height in points
    val margin     = 48f
    val lineHeight = 20f
    val maxLineWidth = pageWidth - margin * 2

    val titlePaint   = Paint().apply { textSize = 18f; isFakeBoldText = true }
    val headingPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
    val bodyPaint    = Paint().apply { textSize = 11f }
    val chipPaint    = Paint().apply { textSize = 10.5f }

    val lines = mutableListOf<Pair<String, Paint>>()

    fun addHeading(text: String) { lines.add(text to headingPaint) }
    fun addBody(text: String) {
        val words = text.split(" ")
        val sb = StringBuilder()
        for (word in words) {
            val test = if (sb.isEmpty()) word else "$sb $word"
            if (bodyPaint.measureText(test) > maxLineWidth) {
                lines.add(sb.toString() to bodyPaint); sb.clear(); sb.append(word)
            } else {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(word)
            }
        }
        if (sb.isNotEmpty()) lines.add(sb.toString() to bodyPaint)
    }
    fun addBlank() { lines.add("" to bodyPaint) }

    fun addPdfSectionsInvoked() {
        addBlank()
        addHeading("SECTIONS INVOKED")
        addBlank()
        summary.sectionsInvoked.forEach { sec ->
            lines.add("  ${sec.mappingLabel()}" to chipPaint)
        }
        addBlank()
        addBlank()
    }

    // Build content following canonical schema order
    lines.add(summary.caseTitle to titlePaint)
    summary.caseId?.let   { addBody("Case ID  : $it") }
    addBody("Court    : ${summary.court}")
    summary.judge?.let    { addBody("Judge    : $it") }
    summary.date?.let     { addBody("Date     : $it") }
    summary.citation?.let { addBody("Citation : $it") }
    summary.petitioner?.let    { addBody("Petitioner : $it") }
    summary.respondent?.let    { addBody("Respondent : $it") }
    summary.caseType?.let      { addBody("Case Type  : $it") }
    addBlank()

    addHeading("FACTS OF CASE");         addBody(summary.factsOfCase); addBlank()

    if (summary.legalIssues.isNotEmpty()) {
        addHeading("LEGAL ISSUES")
        summary.legalIssues.forEach { addBody("• $it") }
        addBlank()
    }

    addHeading("PETITIONER ARGUMENTS");  addBody(summary.petitionerArguments); addBlank()
    addHeading("RESPONDENT ARGUMENTS");  addBody(summary.respondentArguments); addBlank()

    if (summary.sectionsInvoked.isNotEmpty()) {
        addPdfSectionsInvoked()
    }

    if (summary.courtObservations.isNotBlank()) {
        addHeading("COURT OBSERVATIONS"); addBody(summary.courtObservations); addBlank()
    }

    if (summary.legalProvisions.isNotEmpty()) {
        addHeading("LEGAL PROVISIONS")
        summary.legalProvisions.forEach { addBody("• $it") }
        addBlank()
    }

    if (summary.precedentsCited.isNotEmpty()) {
        addHeading("PRECEDENTS CITED")
        summary.precedentsCited.forEach { addBody("• $it") }
        addBlank()
    }

    addHeading("FINAL JUDGMENT"); addBody(summary.finalJudgment); addBlank()
    addHeading("SUMMARY");        addBody(summary.summary); addBlank()
    addBody("— Generated by Case Dhara")

    // Paginate
    var pageIndex = 1
    var pageInfo  = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
    var page      = document.startPage(pageInfo)
    var canvas    = page.canvas
    var y         = margin + lineHeight

    for ((text, paint) in lines) {
        if (y + lineHeight > pageHeight - margin) {
            document.finishPage(page)
            pageIndex++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            page     = document.startPage(pageInfo)
            canvas   = page.canvas
            y        = margin + lineHeight
        }
        canvas.drawText(text, margin, y, paint)
        y += lineHeight
    }
    document.finishPage(page)

    val baos = java.io.ByteArrayOutputStream()
    document.writeTo(baos)
    document.close()
    return baos.toByteArray()
}

private fun showDownloadNotification(context: Context, fileName: String, fileUri: Uri) {
    val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create channel (required for API 26+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID,
            NOTIF_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Case Dhara PDF download notifications" }
        notifManager.createNotificationChannel(channel)
    }

    // Intent to open the PDF — works for both MediaStore URIs (Android 10+) and file URIs
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(fileUri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val pendingOpen = PendingIntent.getActivity(
        context, fileName.hashCode(), openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("PDF Downloaded")
        .setContentText(fileName)
        .setAutoCancel(true)
        .setContentIntent(pendingOpen)      // tapping notification body opens PDF
        .addAction(android.R.drawable.ic_menu_view, "Open", pendingOpen)
        .build()

    notifManager.notify(fileName.hashCode(), notification)
}
