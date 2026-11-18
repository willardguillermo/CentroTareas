package com.willard.centrotareas.data

import kotlinx.coroutines.flow.Flow

interface TareasRepository {
    suspend fun contarPrimos(hasta: Int, alAvanzar: (Float) -> Unit = {}): Int
    suspend fun cargarPerfil(): String
    suspend fun cargarPedidos(simularError: Boolean): List<String>
    fun tareaConProgreso(): Flow<Float>
}