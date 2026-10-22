package com.urreadiego.enigmacode

import android.content.Context

object EstructuraDatos {

    // === OPERACIÓN CRUD: CREATE (Insertar) ===
    fun crearEmpleado(lista: List<Empleado>, nuevo: Empleado): List<Empleado> {
        return lista + nuevo
    }

    // === OPERACIONES CRUD: READ (Consultas y Muestreos) ===
    fun buscarPorRFC(lista: List<Empleado>, rfcBuscado: String): Empleado? {
        for (empleado in lista) {
            if (empleado.rfc.equals(rfcBuscado, ignoreCase = true)) {
                return empleado
            }
        }
        return null
    }

    fun buscarPorNombre(lista: List<Empleado>, nombreBuscado: String): List<Empleado> {
        val coincidencias = mutableListOf<Empleado>()
        for (empleado in lista) {
            if (empleado.nombre.contains(nombreBuscado, ignoreCase = true)) {
                coincidencias.add(empleado)
            }
        }
        return coincidencias
    }

    // === OPERACIÓN CRUD: UPDATE (Modificar registro existente) ===
    fun actualizarEmpleado(lista: List<Empleado>, empleadoEditado: Empleado): List<Empleado> {
        // Mapea la lista: si encuentra la llave primaria (RFC), la sustituye con los datos nuevos
        return lista.map { empleadoActual ->
            if (empleadoActual.rfc == empleadoEditado.rfc) empleadoEditado else empleadoActual
        }
    }

    // === OPERACIÓN CRUD: DELETE (Eliminar registro) ===
    fun eliminarEmpleado(lista: List<Empleado>, rfcParaEliminar: String): List<Empleado> {
        // Filtra la lista para excluir el elemento que coincide con el RFC seleccionado
        return lista.filter { empleadoActual ->
            empleadoActual.rfc != rfcParaEliminar
        }
    }

    // === MÉTODOS DE ORDENAMIENTO ===
    fun ordenarPorSueldoFinal(lista: List<Empleado>): List<Empleado> {
        val arr = lista.toMutableList()
        val n = arr.size
        for (i in 0 until n - 1) {
            for (j in 0 until n - i - 1) {
                if (arr[j].sueldoFinal < arr[j + 1].sueldoFinal) {
                    val temp = arr[j]
                    arr[j] = arr[j + 1]
                    arr[j + 1] = temp
                }
            }
        }
        return arr
    }

    fun ordenarPorLocalidad(lista: List<Empleado>): List<Empleado> {
        val arr = lista.toMutableList()
        val n = arr.size
        for (i in 0 until n - 1) {
            for (j in 0 until n - i - 1) {
                if (arr[j].localidad.nombre.compareTo(arr[j + 1].localidad.nombre, ignoreCase = true) > 0) {
                    val temp = arr[j]
                    arr[j] = arr[j + 1]
                    arr[j + 1] = temp
                }
            }
        }
        return arr
    }

    fun borrarBaseDeDatos(context: Context) {
        val sharedPreferences = context.getSharedPreferences("EnigmaPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("lista_empleados").apply()
    }

}