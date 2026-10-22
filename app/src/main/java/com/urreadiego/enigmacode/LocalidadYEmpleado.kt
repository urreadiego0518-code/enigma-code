package com.urreadiego.enigmacode

enum class Localidad(val id: Int, val nombre: String, val bono: Double){
    YUCATAN(1, "Yucatán", 0.40),
    CHIAPAS(2, "Chiapas", 0.10),
    VERACRUZ(3, "Veracruz", 0.20),
    PUEBLA(4, "Puebla", 0.30),
    CAMPECHE(5, "Campeche", 400.0)
}

data class Empleado(
    val rfc: String,
    val nombre: String,
    val diasTrabajados: Int,
    val horasExtra: Int,
    val faltas: Int,
    val localidad: Localidad,

    val sueldoBase: Double = diasTrabajados * Constantes.SUELDO_DIARIO,
    val sueldoNeto: Double = calcularSueldoNeto(sueldoBase, localidad),
    val percepciones: Double = (sueldoNeto * 0.20) + (horasExtra * Constantes.PAGO_HORA_EXTRA),
    val deducciones: Double = (sueldoNeto * 0.22) + (faltas * Constantes.PENALIZACION_POR_FALTA),
    val sueldoFinal: Double = sueldoNeto + percepciones - deducciones
){
    companion object{
        private fun calcularSueldoNeto(base: Double, loc: Localidad): Double{
            return if (loc == Localidad.CAMPECHE) base + loc.bono else base + (base * loc.bono)
        }

        fun validarRFC(rfc: String): Boolean {
            val regex = Regex("^[A-Z&ñÑ]{3,4}[0-9]{6}[A-Z0-9]{3}\$|^[A-Z&ñÑ]{3,4}[0-9]{6}\$")
            return rfc.uppercase().matches(regex)
        }
    }
}