package com.millalemu.actadigitalinsaglobal

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrevisualizacion(
    archivoPdf: File,
    onCerrar: () -> Unit,
    onCompartir: () -> Unit
) {
    // Estado para guardar la imagen del PDF
    var bitmapPdf by remember { mutableStateOf<Bitmap?>(null) }

    // Generar la imagen al cargar la pantalla
    LaunchedEffect(archivoPdf) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(archivoPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val page = renderer.openPage(0) // Abrimos la página 1 (índice 0)

            // Creamos un bitmap de alta calidad (multiplicamos x2 para que se vea nítido)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            bitmapPdf = bitmap

            page.close()
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vista Previa") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onCompartir) {
                        Icon(Icons.Default.Share, "Compartir ahora")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.LightGray) // Fondo gris para que resalte la hoja
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            if (bitmapPdf != null) {
                Image(
                    bitmap = bitmapPdf!!.asImageBitmap(),
                    contentDescription = "Vista Previa PDF",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}