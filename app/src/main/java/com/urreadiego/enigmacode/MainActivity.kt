package com.urreadiego.enigmacode

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : FragmentActivity() { // Cambiado a FragmentActivity para soportar BiometricPrompt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ControlDeAccesoScreen(activity = this)
                }
            }
        }
    }
}

fun guardarEmpleadosLocales(context: Context, lista: List<Empleado>) {
    val sharedPreferences = context.getSharedPreferences("EnigmaPrefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(lista)
    sharedPreferences.edit().putString("lista_empleados", json).apply()
}

fun cargarEmpleadosLocales(context: Context): List<Empleado> {
    val sharedPreferences = context.getSharedPreferences("EnigmaPrefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("lista_empleados", null) ?: return emptyList()
    val type = object : TypeToken<List<Empleado>>() {}.type
    return Gson().fromJson(json, type)
}

@Composable
fun ControlDeAccesoScreen(activity: FragmentActivity) {
    var rolUsuario by remember { mutableStateOf("") }
    var rfcAutenticado by remember { mutableStateOf("") }
    var textoRfcInput by remember { mutableStateOf("") }

    var mostrarDialogoPassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val listaPersistida = remember { cargarEmpleadosLocales(context) }
    var listaEmpleados by remember { mutableStateOf(listaPersistida) }

    // Función encargada de detonar el lector de huellas dactilares del teléfono
    fun ejecutarAutenticacionBiometrica() {
        val biometricManager = BiometricManager.from(context)

        // Verifica si el hardware está disponible y tiene huellas configuradas
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        rolUsuario = Constantes.ROL_JEFE
                        Toast.makeText(context, "Acceso Biométrico Concedido", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Si falla la lectura repetidas veces, abrimos el teclado numérico de respaldo
                        mostrarDialogoPassword = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Si se cancela o no se puede usar, salta el diálogo por contraseña
                        mostrarDialogoPassword = true
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación de Jefes")
                .setSubtitle("Coloque su huella en el lector digital")
                .setNegativeButtonText("Usar Contraseña")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            // Si el dispositivo no tiene lector o huellas, abrimos contraseña directo
            mostrarDialogoPassword = true
        }
    }

    if (rolUsuario.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ENIGMACODE", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Selecciona tu perfil de ingreso", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

            Button(
                onClick = { ejecutarAutenticacionBiometrica() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Ingresar como Jefe")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Acceso para Empleados", fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = textoRfcInput,
                onValueChange = { textoRfcInput = it },
                label = { Text("Introduce tu RFC") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Button(
                onClick = {
                    val rfcLimpio = textoRfcInput.trim().uppercase()
                    if (rfcLimpio.isBlank()) {
                        Toast.makeText(context, "Por favor escribe tu RFC", Toast.LENGTH_SHORT).show()
                    } else if (!Empleado.validarRFC(rfcLimpio)) {
                        Toast.makeText(context, "El formato del RFC introducido no es válido", Toast.LENGTH_SHORT).show()
                    } else {
                        val emp = EstructuraDatos.buscarPorRFC(listaEmpleados, rfcLimpio)
                        if (emp != null) {
                            rfcAutenticado = rfcLimpio
                            rolUsuario = Constantes.ROL_EMPLEADO
                        } else {
                            Toast.makeText(context, "El RFC no se encuentra registrado", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Consultar Nómina")
            }
        }

        if (mostrarDialogoPassword) {
            AlertDialog(
                onDismissRequest = {
                    mostrarDialogoPassword = false
                    passwordInput = ""
                },
                title = { Text("Autenticación") },
                text = {
                    Column {
                        Text("Introduce la contraseña:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (passwordInput == Constantes.CONTRASENA_JEFE) {
                            rolUsuario = Constantes.ROL_JEFE
                            mostrarDialogoPassword = false
                            passwordInput = ""
                        } else {
                            Toast.makeText(context, "Contraseña Incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Ingresar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mostrarDialogoPassword = false
                        passwordInput = ""
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    } else {
        MenuPrincipalScreen(
            rol = rolUsuario,
            rfcFiltro = rfcAutenticado,
            listaInicial = listaEmpleados,
            onListaCambiada = { nuevaLista ->
                listaEmpleados = nuevaLista
                guardarEmpleadosLocales(context, nuevaLista)
            },
            onCerrarSesion = {
                rolUsuario = ""
                rfcAutenticado = ""
                textoRfcInput = ""
            }
        )
    }
}

@Composable
fun MenuPrincipalScreen(
    rol: String,
    rfcFiltro: String,
    listaInicial: List<Empleado>,
    onListaCambiada: (List<Empleado>) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    var vistaActual by remember { mutableStateOf("MENU") }
    var listaEmpleados by remember { mutableStateOf(listaInicial) }
    var mostrarConfirmarBorradoAll by remember { mutableStateOf(false) }

    LaunchedEffect(listaEmpleados) {
        onListaCambiada(listaEmpleados)
    }

    // Estado del buffer para creación y edición temporal
    var rfc by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var diasTxt by remember { mutableStateOf("") }
    var horasExtraTxt by remember { mutableStateOf("") }
    var faltasTxt by remember { mutableStateOf("") }
    var localidadSeleccionada by remember { mutableStateOf(Localidad.CHIAPAS) }

    // Empleado seleccionado para modificación (Update)
    var empleadoParaEditar by remember { mutableStateOf<Empleado?>(null) }

    var queryBuscar by remember { mutableStateOf("") }
    var tipoBusqueda by remember { mutableStateOf("RFC") }
    var resultadosBusqueda by remember { mutableStateOf<List<Empleado>>(emptyList()) }

    val tieneElementos = listaEmpleados.isNotEmpty()

    when (vistaActual) {
        "MENU" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SISTEMA DE NÓMINA", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("Perfil actual: $rol", fontSize = 14.sp, color = if(rol == "JEFE") Color(0xFF0284C7) else Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                if (rol == "JEFE") {
                    Button(onClick = {
                        rfc = ""; nombre = ""; diasTxt = ""; horasExtraTxt = ""; faltasTxt = ""
                        vistaActual = "AGREGAR"
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("1. Agregar Empleado")
                    }
                    Button(
                        onClick = {
                            listaEmpleados = EstructuraDatos.ordenarPorSueldoFinal(listaEmpleados)
                            Toast.makeText(context, "Ordenado por Sueldo Final", Toast.LENGTH_SHORT).show()
                            vistaActual = "MOSTRAR"
                        },
                        enabled = tieneElementos,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("2. Ordenar por Sueldo Final")
                    }
                    Button(
                        onClick = {
                            listaEmpleados = EstructuraDatos.ordenarPorLocalidad(listaEmpleados)
                            Toast.makeText(context, "Ordenado por Localidad (A-Z)", Toast.LENGTH_SHORT).show()
                            vistaActual = "MOSTRAR"
                        },
                        enabled = tieneElementos,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("3. Ordenar por Localidad")
                    }
                    Button(
                        onClick = {
                            resultadosBusqueda = emptyList()
                            queryBuscar = ""
                            vistaActual = "BUSCAR"
                        },
                        enabled = tieneElementos,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("4. Buscar Empleado")
                    }
                    Button(
                        onClick = { vistaActual = "MOSTRAR" },
                        enabled = tieneElementos,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("5. Mostrar Empleados")
                    }

                    Button(
                        onClick = { mostrarConfirmarBorradoAll = true }, // Abre la confirmación
                        enabled = tieneElementos, // Solo se activa si hay empleados que borrar
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), // Rojo de advertencia
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("6. Reiniciar Datos")
                    }

// --- CUADRO DE DIÁLOGO DE CONFIRMACIÓN (RESTRICCIÓN DE SEGURIDAD) ---
                    if (mostrarConfirmarBorradoAll) {
                        AlertDialog(
                            onDismissRequest = { mostrarConfirmarBorradoAll = false },
                            title = {
                                Text("¿Eliminar todos los registros?", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            },
                            text = {
                                Text("Esta acción borrará de forma permanente a TODOS los empleados y sus nóminas del almacenamiento local. No se puede deshacer. ¿Deseas continuar?")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        EstructuraDatos.borrarBaseDeDatos(context)
                                        listaEmpleados = emptyList() // Limpia la pantalla inmediatamente
                                        mostrarConfirmarBorradoAll = false // Cierra el diálogo
                                        Toast.makeText(context, "Base de datos reiniciada con éxito", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)) // Confirmación en rojo
                                ) {
                                    Text("Sí, Borrar Todo")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { mostrarConfirmarBorradoAll = false } // Cancela sin hacer nada
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                } else {
                    Button(
                        onClick = {
                            val emp = EstructuraDatos.buscarPorRFC(listaEmpleados, rfcFiltro)
                            if (emp != null) {
                                resultadosBusqueda = listOf(emp)
                                vistaActual = "MI_NOMINA"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Ver Detalle de Mi Sueldo")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { onCerrarSesion() }) {
                    Text("Cerrar Sesión", color = Color.Red)
                }
            }
        }

        "AGREGAR" -> {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Text("Agregar Nuevo Empleado", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = rfc, onValueChange = { rfc = it }, label = { Text("RFC") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = diasTxt, onValueChange = { diasTxt = it }, label = { Text("Días Trabajados") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = horasExtraTxt, onValueChange = { horasExtraTxt = it }, label = { Text("Horas Extra") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = faltasTxt, onValueChange = { faltasTxt = it }, label = { Text("Faltas") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Seleccionar Localidad:", fontWeight = FontWeight.Medium)

                    Localidad.entries.forEach { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = (localidadSeleccionada == loc), onClick = { localidadSeleccionada = loc })
                            Text(loc.nombre, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        val rfcTratado = rfc.trim().uppercase()
                        val nombreTratado = nombre.trim()

                        if (rfcTratado.isBlank() || nombreTratado.isBlank()) {
                            Toast.makeText(context, "Error: El RFC y el Nombre son obligatorios.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (!Empleado.validarRFC(rfcTratado)) {
                            Toast.makeText(context, "Error: El formato de RFC no es válido.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (EstructuraDatos.buscarPorRFC(listaEmpleados, rfcTratado) != null) {
                            Toast.makeText(context, "Error: Este RFC ya existe.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val dias = diasTxt.toIntOrNull() ?: 0
                        val horas = horasExtraTxt.toIntOrNull() ?: 0
                        val faltas = faltasTxt.toIntOrNull() ?: 0

                        if (dias < 0 || horas < 0 || faltas < 0) {
                            Toast.makeText(context, "Error: Valores negativos prohibidos.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val nuevoEmpleado = Empleado(rfcTratado, nombreTratado, dias, horas, faltas, localidadSeleccionada)
                        listaEmpleados = EstructuraDatos.crearEmpleado(listaEmpleados, nuevoEmpleado)

                        Toast.makeText(context, "¡Empleado Guardado!", Toast.LENGTH_SHORT).show()
                        vistaActual = "MENU"
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar Empleado")
                    }

                    TextButton(onClick = { vistaActual = "MENU" }) { Text("Cancelar") }
                }
            }
        }

        // ================= PANTA DE EDICIÓN: UPDATE =================
        "EDITAR" -> {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Text("Modificar Datos de Empleado", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("RFC: ${empleadoParaEditar?.rfc}", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = diasTxt, onValueChange = { diasTxt = it }, label = { Text("Días Trabajados") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = horasExtraTxt, onValueChange = { horasExtraTxt = it }, label = { Text("Horas Extra") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = faltasTxt, onValueChange = { faltasTxt = it }, label = { Text("Faltas") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Actualizar Localidad:", fontWeight = FontWeight.Medium)

                    Localidad.entries.forEach { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = (localidadSeleccionada == loc), onClick = { localidadSeleccionada = loc })
                            Text(loc.nombre, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        val nombreTratado = nombre.trim()
                        if (nombreTratado.isBlank()) {
                            Toast.makeText(context, "Este campo no debe estar vacio.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val dias = diasTxt.toIntOrNull() ?: 0
                        val horas = horasExtraTxt.toIntOrNull() ?: 0
                        val faltas = faltasTxt.toIntOrNull() ?: 0

                        // --- AQUÍ ESTÁ EL CANDADO RESTRICTIVO CONTRA NEGATIVOS ---
                        if (dias < 0 || horas < 0 || faltas < 0) {
                            Toast.makeText(context, "Error: Valores negativos prohibidos en días, horas o faltas.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        empleadoParaEditar?.let { empOriginal ->
                            val empleadoModificado = Empleado(
                                rfc = empOriginal.rfc, // Llave primaria intacta e inmutable
                                nombre = nombreTratado,
                                diasTrabajados = dias,
                                horasExtra = horas,
                                faltas = faltas,
                                localidad = localidadSeleccionada
                            )
                            // Ejecutamos operación CRUD de actualización
                            listaEmpleados = EstructuraDatos.actualizarEmpleado(listaEmpleados, empleadoModificado)
                            Toast.makeText(context, "¡Registro actualizado exitosamente!", Toast.LENGTH_SHORT).show()
                        }
                        vistaActual = "MENU"
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Confirmar Cambios")
                    }

                    TextButton(onClick = { vistaActual = "MENU" }) { Text("Descartar") }
                }
            }
        }

        "MOSTRAR" -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Lista de Empleados", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(listaEmpleados) { emp ->
                        TarjetaEmpleadoTicket(
                            emp = emp,
                            modoJefe = true,
                            onEditarClick = {
                                empleadoParaEditar = emp
                                nombre = emp.nombre
                                diasTxt = emp.diasTrabajados.toString()
                                horasExtraTxt = emp.horasExtra.toString()
                                faltasTxt = emp.faltas.toString()
                                localidadSeleccionada = emp.localidad
                                vistaActual = "EDITAR"
                            },
                            onBorrarClick = {
                                // Ejecutamos operación CRUD de eliminación
                                listaEmpleados = EstructuraDatos.eliminarEmpleado(listaEmpleados, emp.rfc)
                                Toast.makeText(context, "Empleado eliminado del sistema", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                Button(onClick = { vistaActual = "MENU" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Volver al Menú")
                }
            }
        }

        "BUSCAR" -> {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Buscar Personal", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (tipoBusqueda == "RFC"), onClick = { tipoBusqueda = "RFC" })
                    Text("Por RFC", modifier = Modifier.padding(start = 4.dp, end = 16.dp))

                    RadioButton(selected = (tipoBusqueda == "NOMBRE"), onClick = { tipoBusqueda = "NOMBRE" })
                    Text("Por Nombre", modifier = Modifier.padding(start = 4.dp))
                }

                OutlinedTextField(
                    value = queryBuscar,
                    onValueChange = { queryBuscar = it },
                    label = { Text(if (tipoBusqueda == "RFC") "Introduce el RFC" else "Introduce el Nombre") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )

                Button(onClick = {
                    val entradaLimpia = queryBuscar.trim()
                    if (tipoBusqueda == "RFC") {
                        val emp = EstructuraDatos.buscarPorRFC(listaEmpleados, entradaLimpia)
                        resultadosBusqueda = if (emp != null) listOf(emp) else emptyList()
                    } else {
                        resultadosBusqueda = EstructuraDatos.buscarPorNombre(listaEmpleados, entradaLimpia)
                    }

                    if (resultadosBusqueda.isEmpty()) {
                        Toast.makeText(context, "No se encontraron coincidencias.", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Buscar")
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(resultadosBusqueda) { emp ->
                        TarjetaEmpleadoTicket(
                            emp = emp,
                            modoJefe = true,
                            onEditarClick = {
                                empleadoParaEditar = emp
                                nombre = emp.nombre
                                diasTxt = emp.diasTrabajados.toString()
                                horasExtraTxt = emp.horasExtra.toString()
                                faltasTxt = emp.faltas.toString()
                                localidadSeleccionada = emp.localidad
                                vistaActual = "EDITAR"
                            },
                            onBorrarClick = {
                                listaEmpleados = EstructuraDatos.eliminarEmpleado(listaEmpleados, emp.rfc)
                                resultadosBusqueda = resultadosBusqueda.filter { it.rfc != emp.rfc }
                                Toast.makeText(context, "Empleado eliminado", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { vistaActual = "MENU"; resultadosBusqueda = emptyList(); queryBuscar = "" }) {
                    Text("Volver al Menú")
                }
            }
        }

        "MI_NOMINA" -> {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Resumen de Nómina", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(16.dp))

                if (resultadosBusqueda.isNotEmpty()) {
                    val emp = resultadosBusqueda.first()
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Empleado: ${emp.nombre}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("RFC: ${emp.rfc}", color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Días trabajados: ${emp.diasTrabajados}")
                            Text("Bono por Localidad (${emp.localidad.nombre}): $${String.format("%.2f", emp.sueldoNeto - emp.sueldoBase)}")
                            Text("Percepciones totales: $${String.format("%.2f", emp.percepciones)}")
                            Text("Deducciones totales: $${String.format("%.2f", emp.deducciones)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Sueldo Neto: $${String.format("%.2f", emp.sueldoFinal)}", color = Color(0xFF16A34A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { vistaActual = "MENU"; resultadosBusqueda = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Regresar")
                }
            }
        }
    }
}

@Composable
fun TarjetaEmpleadoTicket(
    emp: Empleado,
    modoJefe: Boolean = false,
    onEditarClick: () -> Unit = {},
    onBorrarClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior con controles si eres Jefe
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Nombre: ${emp.nombre}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("RFC: ${emp.rfc}", fontWeight = FontWeight.Medium, color = Color.Gray, fontSize = 13.sp)
                }
                if (modoJefe) {
                    Row {
                        IconButton(onClick = { onEditarClick() }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF0284C7))
                        }
                        IconButton(onClick = { onBorrarClick() }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                        }
                    }
                }
            }

            Text("Localidad: ${emp.localidad.nombre}", fontWeight = FontWeight.Medium, fontSize = 14.sp)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("DESGLOSE DEL EMPLEADO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))

            FilaTicketTexto(concepto = "Días Trabajados", valor = "${emp.diasTrabajados}")
            FilaTicketTexto(concepto = "Horas Extras", valor = "${emp.horasExtra} hrs")
            FilaTicketTexto(concepto = "Faltas Acumuladas", valor = "${emp.faltas}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("DESGLOSE ECONÓMICO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sueldo Base", fontSize = 12.sp, color = Color.Gray)
                    Text("$${String.format("%.2f", emp.sueldoBase)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Percepciones", fontSize = 12.sp, color = Color.Gray)
                    Text("$${String.format("%.2f", emp.percepciones)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Deducciones", fontSize = 12.sp, color = Color.Gray)
                    Text("$${String.format("%.2f", emp.deducciones)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SUELDO TOTAL:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$${String.format("%.2f", emp.sueldoFinal)}",
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
fun FilaTicketTexto(concepto: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(concepto, fontSize = 13.sp, color = Color(0xFF475569))
        Text(valor, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}