package com.millalemu.actadigitalinsaglobal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.ByteArrayOutputStream

// Función para convertir Bitmap a String Base64 (Para guardar en BD)
fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

// Función para convertir String Base64 a Bitmap (Para el PDF)
fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogoFirma(
    titulo: String,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    // Estado de las líneas dibujadas
    val path = remember { Path() }
    // Un estado mutable para forzar la recomposición al dibujar
    var triggerDraw by remember { mutableStateOf(0L) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(450.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, color = AzulInsa)
                Spacer(modifier = Modifier.height(10.dp))

                // --- AREA DE DIBUJO ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .background(Color.White)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { motionEvent ->
                                when (motionEvent.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        path.moveTo(motionEvent.x, motionEvent.y)
                                        triggerDraw = System.currentTimeMillis()
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        path.lineTo(motionEvent.x, motionEvent.y)
                                        triggerDraw = System.currentTimeMillis()
                                    }
                                    // Se podrían manejar más eventos, pero con estos basta
                                }
                                true
                            }
                    ) {
                        // Usamos la variable para recomponer
                        triggerDraw.let {
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    // Texto de ayuda fondo
                    if (path.isEmpty) {
                        Text(
                            "Firme aquí con el dedo",
                            color = Color.LightGray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botones
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { path.reset(); triggerDraw = System.currentTimeMillis() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Borrar")
                    }

                    Button(
                        onClick = {
                            // Crear Bitmap desde el Path
                            val bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE) // Fondo blanco

                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 5f
                                isAntiAlias = true
                            }
                            // Convertir Compose Path a Android Path
                            canvas.drawPath(path.asAndroidPath(), paint)

                            onConfirm(bitmap)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulInsa),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Guardar")
                    }
                }
            }
        }
    }
}