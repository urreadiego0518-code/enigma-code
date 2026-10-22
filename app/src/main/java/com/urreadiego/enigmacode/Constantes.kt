package com.urreadiego.enigmacode

/**
 * Constantes de configuración de la aplicación.
 *
 * Nota: la contraseña del rol JEFE se mantiene aquí por tratarse de un
 * proyecto académico. En un proyecto real debe living en un servidor
 * cifrado / Keystore y nunca en el código fuente.
 */
object Constantes {

    /** Credencial de respaldo cuando el dispositivo no dispone de biometría. */
    const val CONTRASENA_JEFE = "160118"

    /** Roles disponibles en el sistema. */
    const val ROL_JEFE = "JEFE"
    const val ROL_EMPLEADO = "EMPLEADO"

    /** Sueldo diario base usado para el cálculo de nómina. */
    const val SUELDO_DIARIO = 200.0

    /** Costo por hora extra. */
    const val PAGO_HORA_EXTRA = 50.0

    /** Costo por falta. */
    const val PENALIZACION_POR_FALTA = 200.0
}
