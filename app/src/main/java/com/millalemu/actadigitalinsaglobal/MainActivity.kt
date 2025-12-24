package com.millalemu.actadigitalinsaglobal

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- COLORES CORPORATIVOS ---
val AzulInsa = Color(0xFF003399)
val NaranjaInsa = Color(0xFFFF9900)
val FondoGris = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = AzulInsa,
                    secondary = NaranjaInsa,
                    background = FondoGris
                )
            ) {
                AppPrincipal()
            }
        }
    }
}

@Composable
fun AppPrincipal() {
    var pantallaActual by remember { mutableStateOf(0) }
    var archivoParaVer: File? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    if (pantallaActual == 2 && archivoParaVer != null) {
        PantallaPrevisualizacion(
            archivoPdf = archivoParaVer!!,
            onCerrar = { pantallaActual = 1 },
            onCompartir = { PdfUtil.compartirPdf(context, archivoParaVer!!) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, "Nuevo") },
                        label = { Text("Nueva Acta") },
                        selected = pantallaActual == 0,
                        onClick = { pantallaActual = 0 },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AzulInsa, indicatorColor = AzulInsa.copy(alpha = 0.2f))
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, "Historial") },
                        label = { Text("Historial") },
                        selected = pantallaActual == 1,
                        onClick = { pantallaActual = 1 },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AzulInsa, indicatorColor = AzulInsa.copy(alpha = 0.2f))
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).background(FondoGris)) {
                if (pantallaActual == 0) {
                    PantallaFormulario(onGuardado = { pantallaActual = 1 })
                } else {
                    PantallaHistorial(
                        onVerPdf = { acta ->
                            val file = PdfUtil.generarPdf(context, acta)
                            archivoParaVer = file
                            pantallaActual = 2
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(onGuardado: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.getDatabase(context)

    // --- LISTAS DE SELECCIÓN ---
    val listaServicios = listOf("Instalación", "Mantención", "Post venta", "Garantía")

    val listaTiposUnidad = listOf(
        "Clasificadora", "Procesador", "Feller", "Torre Alphine",
        "Asistencia", "Skidder", "Rodillo", "Motoniveladora",
        "Shovel", "Astilladora", "Camión Tolva", "Subsoladora",
        "Plantadora", "Cargador Frontal"
    )

    val listaCapacidades = listOf("6 Litros", "9 Litros", "12 Litros", "50 Litros")

    val listaObsAccion = listOf("Instalación", "Mantención")
    // CAMBIO: Nombres de sistemas actualizados
    val listaObsSistema = listOf("Sistema A.F.S.S de Cabina", "Sistema A.F.S.S de Motor", "Sistema de Supresión Externo")
    val listaObsTipo = listOf("Electrónico", "Neumático")

    // --- ESTADOS ---
    var folio by remember { mutableStateOf("") }
    var registro by remember { mutableStateOf("") }

    var tipoServicio by remember { mutableStateOf(listaServicios[0]) }

    // Maquinaria
    var tipoUnidad by remember { mutableStateOf(listaTiposUnidad[0]) }
    var marca by remember { mutableStateOf("") }
    var patente by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var mandante by remember { mutableStateOf("") }
    var horometro by remember { mutableStateOf("") }

    // Módulo Obs Logísticas
    var obsAccion by remember { mutableStateOf(listaObsAccion[0]) }
    var obsSistema by remember { mutableStateOf(listaObsSistema[0]) }
    var obsTipo by remember { mutableStateOf(listaObsTipo[0]) }

    // LÓGICA POST VENTA
    LaunchedEffect(tipoServicio) {
        if (tipoServicio == "Post venta") {
            obsAccion = "Revisión"
        } else {
            if (obsAccion == "Revisión") {
                obsAccion = listaObsAccion[0]
            }
        }
    }

    var pinFabr by remember { mutableStateOf("") }

    // Instalación
    var lugarInst by remember { mutableStateOf("") }

    val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    var fechaInst by remember { mutableStateOf(fechaHoy) }

    var nSistema by remember { mutableStateOf("") }
    var nPrecinto by remember { mutableStateOf("") }
    var obsTecnica by remember { mutableStateOf("") }
    var capCilindro by remember { mutableStateOf(listaCapacidades[0]) }

    // Firmas
    var nomTecnico by remember { mutableStateOf("") }; var runTecnico by remember { mutableStateOf("") }
    var firmaTecnico by remember { mutableStateOf<Bitmap?>(null) }

    var nomSuper by remember { mutableStateOf("") }; var runSuper by remember { mutableStateOf("") }
    var firmaSupervisor by remember { mutableStateOf<Bitmap?>(null) }

    var nomJefe by remember { mutableStateOf("") }; var runJefe by remember { mutableStateOf("") }
    var firmaJefe by remember { mutableStateOf<Bitmap?>(null) }

    var nomCliente by remember { mutableStateOf("") }; var runCliente by remember { mutableStateOf("") }
    var firmaCliente by remember { mutableStateOf<Bitmap?>(null) }

    var obsEntrega by remember { mutableStateOf("") }

    // Dialogo Firma
    var mostrarDialogoFirma by remember { mutableStateOf(false) }
    var quienFirmaActual by remember { mutableStateOf("") }

    if (mostrarDialogoFirma) {
        DialogoFirma(
            titulo = "Firma de $quienFirmaActual",
            onDismiss = { mostrarDialogoFirma = false },
            onConfirm = { bitmap ->
                when (quienFirmaActual) {
                    "Técnico" -> firmaTecnico = bitmap
                    "Supervisor" -> firmaSupervisor = bitmap
                    "Jefe Mecánico" -> firmaJefe = bitmap
                    "Cliente" -> firmaCliente = bitmap
                }
                mostrarDialogoFirma = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Nueva Acta Digital",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = AzulInsa),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // --- SECCIÓN 1: GENERAL ---
        item {
            SeccionCard(titulo = "Información General", icono = Icons.Default.Info) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = folio, onValueChange = { folio = it }, label = "Folio", icon = Icons.Default.Info, modifier = Modifier.weight(1f))
                    CampoTexto(value = registro, onValueChange = { registro = it }, label = "Registro Nº", icon = Icons.Default.List, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                DropdownInput(
                    label = "Tipo de Servicio",
                    opciones = listaServicios,
                    seleccionado = tipoServicio,
                    onSeleccionado = { tipoServicio = it }
                )
            }
        }

        // --- SECCIÓN 2: UNIDAD / MAQUINARIA ---
        item {
            SeccionCard(titulo = "Datos de Unidad", icono = Icons.Default.Settings) {
                DropdownInput(
                    label = "Tipo de Unidad",
                    opciones = listaTiposUnidad,
                    seleccionado = tipoUnidad,
                    onSeleccionado = { tipoUnidad = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = marca, onValueChange = { marca = it }, label = "Marca", icon = Icons.Default.Star, modifier = Modifier.weight(1f))
                    CampoTexto(value = modelo, onValueChange = { modelo = it }, label = "Modelo", icon = Icons.Default.Settings, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = patente, onValueChange = { patente = it }, label = "Patente / Sigla", icon = Icons.Default.AccountBox, modifier = Modifier.weight(1f))
                    CampoTexto(value = horometro, onValueChange = { horometro = it }, label = "Horómetro", icon = Icons.Default.DateRange, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }

                CampoTexto(value = mandante, onValueChange = { mandante = it }, label = "Mandante / Contratista", icon = Icons.Default.Home)

                // Módulo Obs Logísticas
                Spacer(Modifier.height(8.dp))
                Text("Configuración Sistema (Obs. Logísticas)", style = MaterialTheme.typography.labelMedium, color = AzulInsa)
                Card(
                    colors = CardDefaults.cardColors(containerColor = FondoGris),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        if (obsAccion == "Revisión") {
                            CampoTexto(
                                value = obsAccion,
                                onValueChange = {},
                                label = "Acción (Automático)",
                                icon = Icons.Default.Check,
                                readOnly = true
                            )
                        } else {
                            DropdownInput(
                                label = "Acción",
                                opciones = listaObsAccion,
                                seleccionado = obsAccion,
                                onSeleccionado = { obsAccion = it }
                            )
                        }

                        DropdownInput(
                            label = "Sistema",
                            opciones = listaObsSistema,
                            seleccionado = obsSistema,
                            onSeleccionado = { obsSistema = it }
                        )
                        DropdownInput(
                            label = "Tipo de Accionamiento",
                            opciones = listaObsTipo,
                            seleccionado = obsTipo,
                            onSeleccionado = { obsTipo = it }
                        )
                        Text(
                            text = "Resultado: $obsAccion del $obsSistema ($obsTipo)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                CampoTexto(value = pinFabr, onValueChange = { pinFabr = it }, label = "PIN Fabr.", icon = Icons.Default.Info)
            }
        }

        // --- SECCIÓN 3: INSTALACIÓN ---
        item {
            SeccionCard(titulo = "Datos de Instalación", icono = Icons.Default.Create) {
                CampoTexto(value = lugarInst, onValueChange = { lugarInst = it }, label = "Lugar Instalación", icon = Icons.Default.Place)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(
                        value = fechaInst,
                        onValueChange = {},
                        label = "Fecha",
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        DropdownInput(
                            label = "Cap. Cilindro",
                            opciones = listaCapacidades,
                            seleccionado = capCilindro,
                            onSeleccionado = { capCilindro = it }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = nSistema, onValueChange = { nSistema = it }, label = "Nº Sist. AFSS", icon = Icons.Default.Info, modifier = Modifier.weight(1f))
                    CampoTexto(value = nPrecinto, onValueChange = { nPrecinto = it }, label = "Nº Precinto", icon = Icons.Default.Lock, modifier = Modifier.weight(1f))
                }

                CampoTexto(
                    value = obsTecnica,
                    onValueChange = { obsTecnica = it },
                    label = "Obs. Técnicas",
                    icon = Icons.Default.Edit,
                    singleLine = false,
                    minLines = 3
                )
            }
        }

        // --- SECCIÓN 4: FIRMAS ---
        item {
            SeccionCard(titulo = "Firmas y Responsables", icono = Icons.Default.Person) {
                ItemResponsable("Técnico Encargado", nomTecnico, { nomTecnico = it }, runTecnico, { runTecnico = it }, firmaTecnico) { quienFirmaActual = "Técnico"; mostrarDialogoFirma = true }
                ItemResponsable("Supervisor INSA", nomSuper, { nomSuper = it }, runSuper, { runSuper = it }, firmaSupervisor) { quienFirmaActual = "Supervisor"; mostrarDialogoFirma = true }
                ItemResponsable("Jefe Mecánico", nomJefe, { nomJefe = it }, runJefe, { runJefe = it }, firmaJefe) { quienFirmaActual = "Jefe Mecánico"; mostrarDialogoFirma = true }
                ItemResponsable("Recepción Cliente", nomCliente, { nomCliente = it }, runCliente, { runCliente = it }, firmaCliente) { quienFirmaActual = "Cliente"; mostrarDialogoFirma = true }
            }
        }

        // --- CIERRE ---
        item {
            SeccionCard(titulo = "Cierre", icono = Icons.Default.Check) {
                OutlinedTextField(
                    value = obsEntrega,
                    onValueChange = { obsEntrega = it },
                    label = { Text("Observaciones a la Entrega") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AzulInsa)
                )
            }
        }

        item {
            Button(
                onClick = {
                    val camposVacios = mutableListOf<String>()

                    if (folio.isBlank()) camposVacios.add("Folio")
                    if (registro.isBlank()) camposVacios.add("Registro")
                    if (marca.isBlank()) camposVacios.add("Marca")
                    if (modelo.isBlank()) camposVacios.add("Modelo")
                    if (patente.isBlank()) camposVacios.add("Patente")
                    if (mandante.isBlank()) camposVacios.add("Mandante")
                    if (horometro.isBlank()) camposVacios.add("Horómetro")
                    if (pinFabr.isBlank()) camposVacios.add("PIN Fabr.")
                    if (lugarInst.isBlank()) camposVacios.add("Lugar Instalación")
                    if (nSistema.isBlank()) camposVacios.add("Nº Sist. AFSS")
                    if (nPrecinto.isBlank()) camposVacios.add("Nº Precinto")
                    if (obsTecnica.isBlank()) camposVacios.add("Obs. Técnicas")

                    if (camposVacios.isNotEmpty()) {
                        Toast.makeText(context, "Falta completar: ${camposVacios.joinToString(", ")}", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    val fTecStr = firmaTecnico?.let { bitmapToBase64(it) }
                    val fSupStr = firmaSupervisor?.let { bitmapToBase64(it) }
                    val fJefeStr = firmaJefe?.let { bitmapToBase64(it) }
                    val fCliStr = firmaCliente?.let { bitmapToBase64(it) }

                    val obsLogisticaFinal = "$obsAccion del $obsSistema ($obsTipo)"

                    val acta = ActaEntity(
                        folio = folio, registro = registro, tipoServicio = tipoServicio,
                        tipoUnidad = tipoUnidad, patente = patente, mandante = mandante,
                        obsLogisticas = obsLogisticaFinal,
                        marca = marca, modelo = modelo, horometro = horometro, pinFabricante = pinFabr,
                        lugarInst = lugarInst, fechaInst = fechaInst, nSistema = nSistema, nPrecinto = nPrecinto,
                        obsTecnicas = obsTecnica, capCilindro = capCilindro,
                        nombreTecnico = nomTecnico, runTecnico = runTecnico, firmaTecnicoB64 = fTecStr,
                        nombreSupervisor = nomSuper, runSupervisor = runSuper, firmaSupervisorB64 = fSupStr,
                        nombreJefe = nomJefe, runJefe = runJefe, firmaJefeB64 = fJefeStr,
                        nombreCliente = nomCliente, runCliente = runCliente, firmaClienteB64 = fCliStr,
                        obsEntrega = obsEntrega
                    )
                    scope.launch {
                        db.actaDao().insertar(acta)
                        onGuardado()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulInsa),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("GUARDAR Y FINALIZAR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

// --- COMPONENTES REUTILIZABLES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInput(label: String, opciones: List<String>, seleccionado: String, onSeleccionado: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = !expandido }) {
        OutlinedTextField(
            value = seleccionado, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AzulInsa, unfocusedBorderColor = Color.LightGray),
            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(text = { Text(opcion) }, onClick = { onSeleccionado(opcion); expandido = false })
            }
        }
    }
}

@Composable
fun SeccionCard(titulo: String, icono: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = AzulInsa)
                Spacer(Modifier.width(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AzulInsa)
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AzulInsa, focusedLabelColor = AzulInsa, unfocusedBorderColor = Color.LightGray),
        readOnly = readOnly
    )
}

@Composable
fun ItemResponsable(titulo: String, nombre: String, onNombreChange: (String) -> Unit, run: String, onRunChange: (String) -> Unit, firmaBitmap: Bitmap?, onClickFirma: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = AzulInsa)
        Spacer(modifier = Modifier.height(8.dp))
        CampoTexto(value = nombre, onValueChange = onNombreChange, label = "Nombre", icon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(8.dp))
        CampoTexto(value = run, onValueChange = onRunChange, label = "RUN", icon = Icons.Default.Face)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClickFirma() },
            colors = CardDefaults.cardColors(containerColor = FondoGris),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (firmaBitmap != null) {
                    Image(bitmap = firmaBitmap.asImageBitmap(), contentDescription = "Firma", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Create, contentDescription = null, tint = AzulInsa)
                        Text("Tocar para firmar", color = AzulInsa)
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun PantallaHistorial(onVerPdf: (ActaEntity) -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    var listaActas by remember { mutableStateOf(emptyList<ActaEntity>()) }
    LaunchedEffect(Unit) { listaActas = db.actaDao().obtenerTodas() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historial de Actas", style = MaterialTheme.typography.headlineMedium, color = AzulInsa, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listaActas) { acta ->
                Card(
                    elevation = CardDefaults.cardElevation(3.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Folio: ${acta.folio}", fontWeight = FontWeight.Bold, color = AzulInsa)
                            Text("${acta.marca} - ${acta.modelo}", style = MaterialTheme.typography.bodyMedium)
                            Text(acta.fechaInst, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { onVerPdf(acta) }, colors = IconButtonDefaults.iconButtonColors(contentColor = NaranjaInsa)) {
                            Icon(Icons.Default.Info, contentDescription = "Ver PDF")
                        }
                    }
                }
            }
        }
    }
}