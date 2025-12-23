package com.millalemu.actadigitalinsaglobal

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfUtil {

    fun generarPdf(context: Context, acta: ActaEntity): File {
        val pageHeight = 842
        val pageWidth = 595
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // --- PINCELES ---
        val paintTexto = Paint().apply { color = Color.BLACK; textSize = 8f }
        val paintBold = Paint().apply { color = Color.BLACK; textSize = 8f; isFakeBoldText = true }
        val paintTitulo = Paint().apply { color = Color.BLACK; textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val paintBorde = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 0.5f; color = Color.BLACK }
        val paintFondoTitulo = Paint().apply { style = Paint.Style.FILL; color = Color.LTGRAY }

        val textPaintValor = TextPaint(paintTexto)

        // --- DIBUJO ---
        // 1. Logo (CORREGIDO: ESCALADO PROPORCIONAL)
        val bitmapLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_fimal_1)
        if (bitmapLogo != null) {
            // Definimos el espacio máximo que queremos que ocupe
            val maxAlto = 60f
            val maxAncho = 150f // Para que no choque con el título central

            val ratio = bitmapLogo.width.toFloat() / bitmapLogo.height.toFloat()

            // Calculamos dimensiones manteniendo proporción
            var altoFinal = maxAlto
            var anchoFinal = maxAlto * ratio

            // Si es muy ancho, lo limitamos por el ancho
            if (anchoFinal > maxAncho) {
                anchoFinal = maxAncho
                altoFinal = maxAncho / ratio
            }

            val bitmapEscalado = android.graphics.Bitmap.createScaledBitmap(bitmapLogo, anchoFinal.toInt(), altoFinal.toInt(), true)
            canvas.drawBitmap(bitmapEscalado, 40f, 30f, null)
        }

        // 2. Encabezado
        canvas.drawText("INSA", 40f, 100f, paintBold)
        canvas.drawText("77.272.880-8", 40f, 110f, paintTexto)
        canvas.drawText("ACTA ENTREGA - CERTIFICADO", pageWidth / 2f, 50f, paintTitulo)
        canvas.drawText("INSTALACIÓN SISTEMAS CONTRA INCENDIOS AFSS-CF", pageWidth / 2f, 65f, paintTitulo)

        // Folio y Registro
        canvas.drawText("Folio Nº:", 480f, 50f, paintBold)
        canvas.drawText(acta.folio, 530f, 50f, Paint().apply { textSize = 14f; color = Color.RED })

        var y = 80f
        canvas.drawRect(300f, y, 380f, y+15, paintFondoTitulo)
        canvas.drawRect(300f, y, 550f, y+15, paintBorde)
        canvas.drawRect(300f, y, 380f, y+15, paintBorde)
        canvas.drawText("Registro Nº:", 305f, y+10, paintBold)
        canvas.drawText(acta.registro, 390f, y+10, paintTexto)

        // 3. Checkboxes
        y = 110f
        val servicios = listOf("Instalación", "Mantención", "Post venta", "Garantía")
        var xCh = 60f
        servicios.forEach { servicio ->
            canvas.drawText(servicio, xCh, y, paintBold)
            canvas.drawRect(xCh + 60, y-10, xCh + 80, y+5, paintBorde)
            if (acta.tipoServicio == servicio) {
                canvas.drawLine(xCh+60, y-10, xCh+80, y+5, paintBorde)
                canvas.drawLine(xCh+80, y-10, xCh+60, y+5, paintBorde)
            }
            xCh += 130f
        }

        // 4. Campos Inteligentes (Pequeños)
        fun dibujarCampo(titulo: String, valor: String, x: Float, yPos: Float, anchoTotal: Float) {
            val anchoTitulo = anchoTotal * 0.35f
            val anchoValor = anchoTotal - anchoTitulo
            canvas.drawRect(x, yPos, x + anchoTitulo, yPos + 15, paintFondoTitulo)
            canvas.drawRect(x, yPos, x + anchoTitulo, yPos + 15, paintBorde)
            canvas.drawRect(x + anchoTitulo, yPos, x + anchoTotal, yPos + 15, paintBorde)
            canvas.drawText(titulo, x + 2, yPos + 10, paintBold)

            val anchoTexto = textPaintValor.measureText(valor)
            val padding = 4f
            val anchoDisponible = anchoValor - padding

            if (anchoTexto > anchoDisponible) {
                canvas.save()
                canvas.translate(x + anchoTitulo + 2, yPos + 2)
                val staticLayout = StaticLayout.Builder.obtain(valor, 0, valor.length, textPaintValor, anchoDisponible.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 0.8f)
                    .build()
                staticLayout.draw(canvas)
                canvas.restore()
            } else {
                canvas.drawText(valor, x + anchoTitulo + 2, yPos + 10, paintTexto)
            }
        }

        y = 130f
        val anchoCol = 250f
        val xDer = 300f

        // Datos básicos
        dibujarCampo("Tipo de unidad:", acta.tipoUnidad, 40f, y, anchoCol); dibujarCampo("Marca:", acta.marca, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Patente/Sigla:", acta.patente, 40f, y, anchoCol); dibujarCampo("Modelo:", acta.modelo, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Mandante:", acta.mandante, 40f, y, anchoCol); dibujarCampo("Horómetro:", acta.horometro, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Nº Pin Fabr:", acta.pinFabricante, 40f, y, anchoCol)

        // --- OBS LOGÍSTICAS (CAJA GRANDE) ---
        y+=25
        canvas.drawRect(40f, y, 550f, y+15, paintFondoTitulo)
        canvas.drawRect(40f, y, 550f, y+15, paintBorde)
        canvas.drawText("CONFIGURACIÓN SISTEMA / OBS. LOGÍSTICAS:", 45f, y+10, paintBold)
        y += 15
        val altoCajaLog = 35f
        canvas.drawRect(40f, y, 550f, y+altoCajaLog, paintBorde)

        val logPaint = TextPaint(paintTexto)
        if (logPaint.measureText(acta.obsLogisticas) > 500) {
            val layoutLog = StaticLayout.Builder.obtain(acta.obsLogisticas, 0, acta.obsLogisticas.length, logPaint, 500)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            canvas.save(); canvas.translate(45f, y+5); layoutLog.draw(canvas); canvas.restore()
        } else {
            canvas.drawText(acta.obsLogisticas, 45f, y+15, paintTexto)
        }
        y += altoCajaLog + 10
        // ------------------------------------

        dibujarCampo("Lugar Instalación:", acta.lugarInst, 40f, y, anchoCol); dibujarCampo("Fecha Inst:", acta.fechaInst, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Nº Sist AFSS-CF:", acta.nSistema, 40f, y, anchoCol); dibujarCampo("Nº Precinto:", acta.nPrecinto, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Obs. Técnicas:", acta.obsTecnicas, 40f, y, anchoCol); dibujarCampo("Cap. Cilindro:", acta.capCilindro, xDer, y, anchoCol)

        // 5. Texto Legal
        y += 30f
        val textoNegrita = "Se deja constancia que el sistema AFSS-CF para extinción de incendios, instalado en maquinaria identificada precedentemente:"
        val textoCuerpo = "Cuenta con agente extintor Cold Fire, multipropósito para fuegos clases A, B, C, D y K, fabricado en los Estados Unidos de América, debidamente certificado como: agente No toxico bajo SGS 203697-10/23/97, ingesta oral y ocular SGS 202536-02-08/16/96, pruebas de toxicidad acuática por USTC-11/03/93, agente anticorrosivo SGS 409277-11/19/96, agente biodegradable SGS 203408-2-04/23/97, certificaciones CESMEC, EPA-SNAP-USDA-LISTING, UL report wetting agent (2N75), US Foresty 5162 11/07/03. Reconocimiento de Green Seal.\n" +
                "Que la empresa INSA, responsable de la instalación, se encuentra debidamente certificada y con su certificación al día, bajo el decreto supremo 44 (DS44).\n" +
                "Que el sistema AFSS es un sistema autónomo, con activación automática por efecto térmico y activación manual ubicada convenientemente cercana al habitáculo del operador.\n" +
                "Que las instalaciones se realizaron de acuerdo a lo especificado y solicitado."
        val anchoTexto = 515
        val textPaintBold = TextPaint(paintBold)
        val textPaintNormal = TextPaint(paintTexto).apply { textSize = 6f }

        val layoutNegrita = StaticLayout.Builder.obtain(textoNegrita, 0, textoNegrita.length, textPaintBold, anchoTexto).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save(); canvas.translate(40f, y); layoutNegrita.draw(canvas); canvas.restore()
        y += layoutNegrita.height + 5f
        val layoutCuerpo = StaticLayout.Builder.obtain(textoCuerpo, 0, textoCuerpo.length, textPaintNormal, anchoTexto).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.0f).build()
        canvas.save(); canvas.translate(40f, y); layoutCuerpo.draw(canvas); canvas.restore()
        y += layoutCuerpo.height + 15f

        // 6. FIRMAS (Sin líneas, solo imagen)
        val anchoFirmaCaja = 250f
        val altoFirmaCaja = 140f

        fun dibujarCajaFirma(titulo: String, nombre: String, run: String, firmaB64: String?, x: Float, yPos: Float) {
            // Fondo y borde
            canvas.drawRect(x, yPos, x + anchoFirmaCaja, yPos + altoFirmaCaja, paintBorde)
            canvas.drawRect(x, yPos, x + anchoFirmaCaja, yPos + 15, paintFondoTitulo)

            // Textos informativos
            canvas.drawText(titulo, x + 5, yPos + 10, paintBold)
            canvas.drawText("Nombre: $nombre", x + 5, yPos + 25, paintTexto)
            canvas.drawText("RUN: $run", x + 5, yPos + 35, paintTexto)

            // Posición base donde iría la firma
            val yLinea = yPos + 120

            if (firmaB64 != null) {
                val bitmapFirma = base64ToBitmap(firmaB64)
                if (bitmapFirma != null) {
                    val maxFirmaWidth = 220f
                    val maxFirmaHeight = 80f

                    val ratioX = maxFirmaWidth / bitmapFirma.width
                    val ratioY = maxFirmaHeight / bitmapFirma.height
                    val finalScale = min(ratioX, ratioY)

                    val finalWidth = (bitmapFirma.width * finalScale).toInt()
                    val finalHeight = (bitmapFirma.height * finalScale).toInt()

                    val firmaEscalada = android.graphics.Bitmap.createScaledBitmap(bitmapFirma, finalWidth, finalHeight, true)

                    val xPosFirma = x + (anchoFirmaCaja - finalWidth) / 2
                    val yPosFirma = yLinea - finalHeight + 5

                    canvas.drawBitmap(firmaEscalada, xPosFirma, yPosFirma, null)
                }
            }
        }

        dibujarCajaFirma("Técnico Encargado de la Instalación", acta.nombreTecnico, acta.runTecnico, acta.firmaTecnicoB64, 40f, y)
        dibujarCajaFirma("Aprobación Supervisor INSA", acta.nombreSupervisor, acta.runSupervisor, acta.firmaSupervisorB64, xDer, y)

        y += altoFirmaCaja + 10

        dibujarCajaFirma("Aprobación Jefe Mecánico", acta.nombreJefe, acta.runJefe, acta.firmaJefeB64, 40f, y)
        dibujarCajaFirma("Responsable Recepción Cliente", acta.nombreCliente, acta.runCliente, acta.firmaClienteB64, xDer, y)

        // 7. Obs Final
        y += altoFirmaCaja + 20

        if (y > pageHeight - 100) {
            pdfDocument.finishPage(page)
        }

        canvas.drawRect(40f, y, 550f, y+15, paintFondoTitulo)
        canvas.drawRect(40f, y, 550f, y+15, paintBorde)
        canvas.drawText("OBSERVACIONES A LA ENTREGA:", 45f, y+10, paintBold)
        y += 15
        val altoCajaObs = 50f
        canvas.drawRect(40f, y, 550f, y+altoCajaObs, paintBorde)

        val obsPaint = TextPaint(paintTexto)
        if (obsPaint.measureText(acta.obsEntrega) > 500) {
            val layoutObs = StaticLayout.Builder.obtain(acta.obsEntrega, 0, acta.obsEntrega.length, obsPaint, 500).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            canvas.save(); canvas.translate(45f, y+5); layoutObs.draw(canvas); canvas.restore()
        } else {
            canvas.drawText(acta.obsEntrega, 45f, y+15, paintTexto)
        }

        val yFooter = pageHeight - 40f
        canvas.drawText("LO ECHEVERS 901-1101 BODEGA 41 a 44, QUILICURA, SANTIAGO, CHILE", pageWidth/2f, yFooter, paintTitulo.apply { textSize = 8f })
        canvas.drawText("WWW.INSA.GLOBAL | INFO@INSA.GLOBAL | +56 2 2897 4777", pageWidth/2f, yFooter+10, paintTitulo)

        pdfDocument.finishPage(page)
        val nombreArchivo = "Acta_${acta.folio}.pdf"
        val file = File(context.cacheDir, nombreArchivo)
        try { pdfDocument.writeTo(FileOutputStream(file)) } catch (e: Exception) { e.printStackTrace() }
        pdfDocument.close()
        return file
    }

    fun compartirPdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Acta PDF"))
    }
}