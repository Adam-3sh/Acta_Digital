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

        // Necesitamos un TextPaint para los saltos de línea automáticos
        val textPaintValor = TextPaint(paintTexto)

        // --- DIBUJO ---

        // 1. Logo
        val bitmapLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_fimal_1)
        if (bitmapLogo != null) {
            val bitmapEscalado = android.graphics.Bitmap.createScaledBitmap(bitmapLogo, 60, 60, false)
            canvas.drawBitmap(bitmapEscalado, 40f, 30f, null)
        }

        // 2. Encabezado
        canvas.drawText("INSA", 40f, 100f, paintBold)
        canvas.drawText("77.272.880-8", 40f, 110f, paintTexto)

        canvas.drawText("ACTA ENTREGA - CERTIFICADO", pageWidth / 2f, 50f, paintTitulo)
        canvas.drawText("INSTALACIÓN SISTEMAS CONTRA INCENDIOS AFSS-CF", pageWidth / 2f, 65f, paintTitulo)

        // Folio
        canvas.drawText("Folio Nº:", 480f, 50f, paintBold)
        canvas.drawText(acta.folio, 530f, 50f, Paint().apply { textSize = 14f; color = Color.RED })

        // Registro N
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

        // 4. FUNCION INTELIGENTE PARA DIBUJAR CAMPOS (Detecta texto largo)
        fun dibujarCampo(titulo: String, valor: String, x: Float, yPos: Float, anchoTotal: Float) {
            val anchoTitulo = anchoTotal * 0.35f // 35% para el título gris
            val anchoValor = anchoTotal - anchoTitulo

            // Dibujar Cajas
            canvas.drawRect(x, yPos, x + anchoTitulo, yPos + 15, paintFondoTitulo) // Fondo Gris
            canvas.drawRect(x, yPos, x + anchoTitulo, yPos + 15, paintBorde)       // Borde Gris
            canvas.drawRect(x + anchoTitulo, yPos, x + anchoTotal, yPos + 15, paintBorde) // Borde Blanco

            // Dibujar Título
            canvas.drawText(titulo, x + 2, yPos + 10, paintBold)

            // --- LÓGICA DE TEXTO INTELIGENTE ---
            // Medimos cuánto ocuparía el texto en una sola línea
            val anchoTexto = textPaintValor.measureText(valor)
            val padding = 4f
            val anchoDisponible = anchoValor - padding

            if (anchoTexto > anchoDisponible) {
                // SI EL TEXTO ES MUY LARGO: Usamos StaticLayout para envolverlo (multilínea)
                canvas.save()
                // Nos movemos al inicio de la caja blanca + un poquito de margen
                canvas.translate(x + anchoTitulo + 2, yPos + 2)

                val staticLayout = StaticLayout.Builder.obtain(valor, 0, valor.length, textPaintValor, anchoDisponible.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 0.8f) // Espaciado entre líneas un poco más apretado
                    .build()

                staticLayout.draw(canvas)
                canvas.restore()
            } else {
                // SI EL TEXTO CABE: Lo dibujamos normal en una línea
                canvas.drawText(valor, x + anchoTitulo + 2, yPos + 10, paintTexto)
            }
        }

        y = 130f
        val anchoCol = 250f
        val xDer = 300f

        dibujarCampo("Tipo de unidad:", acta.tipoUnidad, 40f, y, anchoCol); dibujarCampo("Marca:", acta.marca, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Patente/Sigla:", acta.patente, 40f, y, anchoCol); dibujarCampo("Modelo:", acta.modelo, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Mandante:", acta.mandante, 40f, y, anchoCol); dibujarCampo("Horómetro:", acta.horometro, xDer, y, anchoCol)
        y+=20
        // AQUÍ ES DONDE SOLÍA FALLAR: Ahora se ajustará solo
        dibujarCampo("Obs. Logísticas:", acta.obsLogisticas, 40f, y, anchoCol); dibujarCampo("Nº Pin Fabr:", acta.pinFabricante, xDer, y, anchoCol)
        y+=30
        dibujarCampo("Lugar Instalación:", acta.lugarInst, 40f, y, anchoCol); dibujarCampo("Fecha Inst:", acta.fechaInst, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Nº Sist AFSS-CF:", acta.nSistema, 40f, y, anchoCol); dibujarCampo("Nº Precinto:", acta.nPrecinto, xDer, y, anchoCol)
        y+=20
        dibujarCampo("Obs. Técnicas:", acta.obsTecnicas, 40f, y, anchoCol); dibujarCampo("Cap. Cilindro:", acta.capCilindro, xDer, y, anchoCol)

        // 5. TEXTO LEGAL
        y += 30f

        val textoNegrita = "Se deja constancia que el sistema AFSS-CF para extinción de incendios, instalado en maquinaria identificada precedentemente:"
        val textoCuerpo = "Cuenta con agente extintor Cold Fire, multipropósito para fuegos clases A, B, C, D y K, fabricado en los Estados Unidos de América, " +
                "debidamente certificado como: agente No toxico bajo SGS 203697-10/23/97, ingesta oral y ocular SGS 202536-02-08/16/96, pruebas de " +
                "toxicidad acuática por USTC-11/03/93, agente anticorrosivo SGS 409277-11/19/96, agente biodegradable SGS 203408-2-04/23/97, " +
                "certificaciones CESMEC, EPA-SNAP-USDA-LISTING, UL report wetting agent (2N75), US Foresty 5162 11/07/03. Reconocimiento de Green Seal.\n" +
                "Que la empresa INSA, responsable de la instalación, se encuentra debidamente certificada y con su certificación al día, bajo el decreto supremo 44 (DS44).\n" +
                "Que el sistema AFSS es un sistema autónomo, con activación automática por efecto térmico y activación manual ubicada convenientemente cercana al habitáculo del operador.\n" +
                "Que las instalaciones se realizaron de acuerdo a lo especificado y solicitado."

        val anchoTexto = 515
        val textPaintBold = TextPaint(paintBold)
        val textPaintNormal = TextPaint(paintTexto).apply { textSize = 6f }

        val layoutNegrita = StaticLayout.Builder.obtain(textoNegrita, 0, textoNegrita.length, textPaintBold, anchoTexto)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save(); canvas.translate(40f, y); layoutNegrita.draw(canvas); canvas.restore()

        y += layoutNegrita.height + 5f

        val layoutCuerpo = StaticLayout.Builder.obtain(textoCuerpo, 0, textoCuerpo.length, textPaintNormal, anchoTexto)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.0f).build()
        canvas.save(); canvas.translate(40f, y); layoutCuerpo.draw(canvas); canvas.restore()

        y += layoutCuerpo.height + 15f

        // 6. FIRMAS
        val anchoFirma = 250f
        val altoFirma = 80f

        fun dibujarCajaFirma(titulo: String, nombre: String, run: String, x: Float, yPos: Float) {
            canvas.drawRect(x, yPos, x + anchoFirma, yPos + altoFirma, paintBorde)
            canvas.drawRect(x, yPos, x + anchoFirma, yPos + 15, paintFondoTitulo)
            canvas.drawText(titulo, x + 5, yPos + 10, paintBold)
            canvas.drawText("Nombre: $nombre", x + 5, yPos + 30, paintTexto)
            canvas.drawText("RUN: $run", x + 5, yPos + 45, paintTexto)
            canvas.drawText("Firma: _________________", x + 5, yPos + 70, paintTexto)
        }

        dibujarCajaFirma("Técnico Encargado de la Instalación", acta.nombreTecnico, acta.runTecnico, 40f, y)
        dibujarCajaFirma("Aprobación Supervisor INSA", acta.nombreSupervisor, acta.runSupervisor, xDer, y)

        y += altoFirma + 10
        dibujarCajaFirma("Aprobación Jefe Mecánico", acta.nombreJefe, acta.runJefe, 40f, y)
        dibujarCajaFirma("Responsable Recepción Cliente", acta.nombreCliente, acta.runCliente, xDer, y)

        // 7. OBS ENTREGA (También inteligente para mucho texto)
        y += altoFirma + 20

        canvas.drawRect(40f, y, 550f, y+15, paintFondoTitulo)
        canvas.drawRect(40f, y, 550f, y+15, paintBorde)
        canvas.drawText("OBSERVACIONES A LA ENTREGA:", 45f, y+10, paintBold)

        y += 15
        val altoCajaObs = 50f
        canvas.drawRect(40f, y, 550f, y+altoCajaObs, paintBorde)

        // Dibujamos las observaciones ajustadas al ancho
        val obsPaint = TextPaint(paintTexto)
        if (obsPaint.measureText(acta.obsEntrega) > 500) {
            val layoutObs = StaticLayout.Builder.obtain(acta.obsEntrega, 0, acta.obsEntrega.length, obsPaint, 500)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            canvas.save(); canvas.translate(45f, y+5); layoutObs.draw(canvas); canvas.restore()
        } else {
            canvas.drawText(acta.obsEntrega, 45f, y+15, paintTexto)
        }

        // Footer
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