package com.millalemu.actadigitalinsaglobal

import android.graphics.Bitmap
import android.os.Bundle
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
        // Asegúrate de tener PdfPreviewScreen importado o en el mismo paquete
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

    // Estados Generales
    var folio by remember { mutableStateOf("5193") }
    var registro by remember { mutableStateOf("") }
    var tipoServicio by remember { mutableStateOf("Instalación") }
    var expandidoServicio by remember { mutableStateOf(false) }
    val servicios = listOf("Instalación", "Mantención", "Post venta", "Garantía")

    // Maquinaria
    var tipoUnidad by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var patente by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var mandante by remember { mutableStateOf("") }
    var horometro by remember { mutableStateOf("") }
    var obsLogistica by remember { mutableStateOf("") }
    var pinFabr by remember { mutableStateOf("") }

    // Instalación
    var lugarInst by remember { mutableStateOf("") }
    var fechaInst by remember { mutableStateOf("") }
    var nSistema by remember { mutableStateOf("") }
    var nPrecinto by remember { mutableStateOf("") }
    var obsTecnica by remember { mutableStateOf("") }
    var capCilindro by remember { mutableStateOf("") }

    // --- ESTADOS PARA FIRMAS ---
    var nomTecnico by remember { mutableStateOf("") }; var runTecnico by remember { mutableStateOf("") }
    var firmaTecnico by remember { mutableStateOf<Bitmap?>(null) }

    var nomSuper by remember { mutableStateOf("") }; var runSuper by remember { mutableStateOf("") }
    var firmaSupervisor by remember { mutableStateOf<Bitmap?>(null) }

    var nomJefe by remember { mutableStateOf("") }; var runJefe by remember { mutableStateOf("") }
    var firmaJefe by remember { mutableStateOf<Bitmap?>(null) }

    var nomCliente by remember { mutableStateOf("") }; var runCliente by remember { mutableStateOf("") }
    var firmaCliente by remember { mutableStateOf<Bitmap?>(null) }

    var obsEntrega by remember { mutableStateOf("") }

    // Control del Diálogo de Firma
    var mostrarDialogoFirma by remember { mutableStateOf(false) }
    var quienFirmaActual by remember { mutableStateOf("") }

    // --- LOGICA DIÁLOGO FLOTANTE ---
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

                ExposedDropdownMenuBox(
                    expanded = expandidoServicio,
                    onExpandedChange = { expandidoServicio = !expandidoServicio }
                ) {
                    OutlinedTextField(
                        value = tipoServicio,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Servicio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoServicio) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulInsa,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandidoServicio,
                        onDismissRequest = { expandidoServicio = false }
                    ) {
                        servicios.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    tipoServicio = s
                                    expandidoServicio = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- SECCIÓN 2: UNIDAD / MAQUINARIA ---
        item {
            SeccionCard(titulo = "Datos de Unidad", icono = Icons.Default.Settings) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = tipoUnidad, onValueChange = { tipoUnidad = it }, label = "Tipo Unidad", icon = Icons.Default.ArrowForward, modifier = Modifier.weight(1f))
                    CampoTexto(value = marca, onValueChange = { marca = it }, label = "Marca", icon = Icons.Default.Star, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = modelo, onValueChange = { modelo = it }, label = "Modelo", icon = Icons.Default.Settings, modifier = Modifier.weight(1f))
                    CampoTexto(value = patente, onValueChange = { patente = it }, label = "Patente", icon = Icons.Default.AccountBox, modifier = Modifier.weight(1f))
                }
                CampoTexto(value = mandante, onValueChange = { mandante = it }, label = "Mandante / Contratista", icon = Icons.Default.Home)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = horometro, onValueChange = { horometro = it }, label = "Horómetro", icon = Icons.Default.DateRange, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    CampoTexto(value = pinFabr, onValueChange = { pinFabr = it }, label = "PIN Fabr.", icon = Icons.Default.Info, modifier = Modifier.weight(1f))
                }
                CampoTexto(value = obsLogistica, onValueChange = { obsLogistica = it }, label = "Obs. Logísticas", icon = Icons.Default.Edit)
            }
        }

        // --- SECCIÓN 3: INSTALACIÓN ---
        item {
            SeccionCard(titulo = "Datos de Instalación", icono = Icons.Default.Create) {
                CampoTexto(value = lugarInst, onValueChange = { lugarInst = it }, label = "Lugar Instalación", icon = Icons.Default.Place)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = fechaInst, onValueChange = { fechaInst = it }, label = "Fecha", icon = Icons.Default.DateRange, modifier = Modifier.weight(1f))
                    CampoTexto(value = capCilindro, onValueChange = { capCilindro = it }, label = "Cap. Cilindro", icon = Icons.Default.Info, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(value = nSistema, onValueChange = { nSistema = it }, label = "Nº Sist. AFSS", icon = Icons.Default.Info, modifier = Modifier.weight(1f))
                    CampoTexto(value = nPrecinto, onValueChange = { nPrecinto = it }, label = "Nº Precinto", icon = Icons.Default.Lock, modifier = Modifier.weight(1f))
                }
                CampoTexto(value = obsTecnica, onValueChange = { obsTecnica = it }, label = "Obs. Técnicas", icon = Icons.Default.Edit)
            }
        }

        // --- SECCIÓN 4: RESPONSABLES Y FIRMAS (NUEVO) ---
        item {
            SeccionCard(titulo = "Firmas y Responsables", icono = Icons.Default.Person) {

                ItemResponsable(
                    titulo = "Técnico Encargado",
                    nombre = nomTecnico, onNombreChange = { nomTecnico = it },
                    run = runTecnico, onRunChange = { runTecnico = it },
                    firmaBitmap = firmaTecnico,
                    onClickFirma = { quienFirmaActual = "Técnico"; mostrarDialogoFirma = true }
                )

                ItemResponsable(
                    titulo = "Supervisor INSA",
                    nombre = nomSuper, onNombreChange = { nomSuper = it },
                    run = runSuper, onRunChange = { runSuper = it },
                    firmaBitmap = firmaSupervisor,
                    onClickFirma = { quienFirmaActual = "Supervisor"; mostrarDialogoFirma = true }
                )

                ItemResponsable(
                    titulo = "Jefe Mecánico",
                    nombre = nomJefe, onNombreChange = { nomJefe = it },
                    run = runJefe, onRunChange = { runJefe = it },
                    firmaBitmap = firmaJefe,
                    onClickFirma = { quienFirmaActual = "Jefe Mecánico"; mostrarDialogoFirma = true }
                )

                ItemResponsable(
                    titulo = "Recepción Cliente",
                    nombre = nomCliente, onNombreChange = { nomCliente = it },
                    run = runCliente, onRunChange = { runCliente = it },
                    firmaBitmap = firmaCliente,
                    onClickFirma = { quienFirmaActual = "Cliente"; mostrarDialogoFirma = true }
                )
            }
        }

        // --- OBS FINAL ---
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
                    // Convertir Bitmaps a Base64
                    val fTecStr = firmaTecnico?.let { bitmapToBase64(it) }
                    val fSupStr = firmaSupervisor?.let { bitmapToBase64(it) }
                    val fJefeStr = firmaJefe?.let { bitmapToBase64(it) }
                    val fCliStr = firmaCliente?.let { bitmapToBase64(it) }

                    val acta = ActaEntity(
                        folio = folio, registro = registro, tipoServicio = tipoServicio,
                        tipoUnidad = tipoUnidad, patente = patente, mandante = mandante, obsLogisticas = obsLogistica,
                        marca = marca, modelo = modelo, horometro = horometro, pinFabricante = pinFabr,
                        lugarInst = lugarInst, fechaInst = fechaInst, nSistema = nSistema, nPrecinto = nPrecinto,
                        obsTecnicas = obsTecnica, capCilindro = capCilindro,

                        // Datos Responsables
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

// --- COMPONENTES VISUALES ---

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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AzulInsa,
            focusedLabelColor = AzulInsa,
            unfocusedBorderColor = Color.LightGray
        )
    )
}

@Composable
fun ItemResponsable(
    titulo: String,
    nombre: String,
    onNombreChange: (String) -> Unit,
    run: String,
    onRunChange: (String) -> Unit,
    firmaBitmap: Bitmap?,
    onClickFirma: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = AzulInsa)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoTexto(
                value = nombre,
                onValueChange = onNombreChange,
                label = "Nombre",
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f)
            )
            CampoTexto(
                value = run,
                onValueChange = onRunChange,
                label = "RUN",
                icon = Icons.Default.Face,
                modifier = Modifier.weight(0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Zona de la firma
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable { onClickFirma() },
            colors = CardDefaults.cardColors(containerColor = FondoGris),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (firmaBitmap != null) {
                    Image(
                        bitmap = firmaBitmap.asImageBitmap(),
                        contentDescription = "Firma",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        "Toca para cambiar",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                    )
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
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Folio: ${acta.folio}", fontWeight = FontWeight.Bold, color = AzulInsa)
                            Text("${acta.marca} - ${acta.modelo}", style = MaterialTheme.typography.bodyMedium)
                            Text(acta.fechaInst, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(
                            onClick = { onVerPdf(acta) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = NaranjaInsa)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Ver PDF")
                        }
                    }
                }
            }
        }
    }
}