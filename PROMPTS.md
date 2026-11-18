# PROMPTS.md · Fase 2 con IA

**Herramienta:** Gemini en Android Studio (modo Agent) · **Fecha:** 17/11/2026 · **Rama:** `mejora-ia`

Regla aplicada: *la IA propone, yo decido*. Antes de cada prompt definí el diseño y las restricciones (no usar `GlobalScope`, `runBlocking` ni `Thread.sleep`, relanzar siempre la `CancellationException` y respetar MVVM + Repository). Después de cada respuesta revisé el diff y probé en el emulador antes de hacer el commit.

## Resumen

| # | Prompt que escribí | Qué respondió Gemini | Qué cambié o descarté | Por qué |
|---|---|---|---|---|
| 1 | «Mueve la tarea simulada con progreso del ViewModel al Repository y expón el progreso como un `Flow` con `flow { emit(...) }` y `flowOn(Dispatchers.Default)`» ([completo](#prompt-1--progreso-desde-el-repository)) | Agregó `tareaConProgreso(): Flow<Float>` a la interface, la implementó con `flow { }` + `flowOn(Dispatchers.Default)` y reemplazó en el ViewModel el `withContext` + `for` por `collect`. Quitó 3 imports sin uso y explicó la cancelación y en qué hilo corre cada parte | No cambié nada del código. Verifiqué en Logcat que siga saliendo `Antes: main`, `Dentro: DefaultDispatcher-worker-…` y `Despues: main` | En una app real el trabajo va en el Repository y el ViewModel solo traduce los datos a estado. Mantuve los logs `HILO` para no romper la prueba de aceptación del Paso 8 |
| 2 | «Muestra el porcentaje del cálculo de primos mientras avanza: el Repository avisa con `alAvanzar` cada 1 % y el ViewModel lo expone como `StateFlow<Float>`» ([completo](#prompt-2--primos-con-progreso)) | Cambió la firma a `contarPrimos(hasta, alAvanzar)`, avisa cada `paso = hasta / 100` y al final con `1f`, agregó `progresoPrimos` al ViewModel y una barra con "Calculando... X %" y "Cancelado al X %". Afirmó que los resultados no cambiaban | No cambié el código. Comprobé yo mismo en el emulador que siguen saliendo 25, 168, 1229 y 788 060 | Gemini afirmó los resultados sin ejecutar la app, así que había que verificarlos. Decidí antes del prompt no tocar `Estados.kt` (archivo [E]) y avisar cada 1 % y no en cada número, para no frenar el cálculo |
| 3 | «Agrega una tabla que acumule los tiempos de "Una por una" y "A la vez", con promedio, diferencia y botón Limpiar» ([completo](#prompt-3--comparación-de-tiempos)) | Agregó dos `StateFlow<List<Long>>` con las últimas 5 cargas exitosas, `limpiarTiempos()` y el Composable `TablaTiempos` con filas, promedio, diferencia y botón Limpiar | **Corregí el ancho de la primera columna:** `weight(1f)` → `weight(1.4f)` | Con `weight(1f)` la fila salía pegada: "Promedio5021 ms". También verifiqué que una carga con error no agrega fila |

### Mejoras descartadas

| Mejora | Por qué la descarté |
|---|---|
| Tiempo máximo con `withTimeout` de 2 s | Los pedidos simulados tardan 3 s, así que **siempre** saldría "Tardó demasiado" y se rompería la comparación del Paso 6 |
| Historial con DataStore | Agrega una dependencia nueva y persistencia; preferí mejoras que refuerzan el tema del laboratorio (corrutinas y `Flow`) |

## Prompts completos

### Prompt 1 · Progreso desde el Repository

```
Estoy en el proyecto CentroTareas (Kotlin, Jetpack Compose, Material 3, MVVM + Repository, corrutinas). Quiero aplicar esta mejora: mover la tarea simulada con progreso del ViewModel al Repository y exponer el progreso como un Flow.

Archivos a modificar:
- app/src/main/java/com/tecsup/centrotareas/data/TareasRepository.kt
- app/src/main/java/com/tecsup/centrotareas/data/TareasRepositoryLocal.kt
- app/src/main/java/com/tecsup/centrotareas/viewmodel/TareasViewModel.kt

Requisitos:
1. En la interface TareasRepository agrega: fun tareaConProgreso(): Flow<Float>
2. En TareasRepositoryLocal impleméntala con flow { }: primero un Log.d("HILO", "Dentro: ${Thread.currentThread().name}"), luego 100 vueltas donde en cada una haces delay(40) y emit(i / 100f). Aplica .flowOn(Dispatchers.Default) al flow.
3. En TareasViewModel.tareaLarga() reemplaza el bloque withContext(Dispatchers.Default) { ... for ... } por repo.tareaConProgreso().collect { _progreso.value = it }.
4. En tareaLarga() mantén exactamente igual: el return si _corriendo ya es true, _corriendo en true antes del launch y en false dentro del finally, el Job guardado en trabajo, _progreso en 0f al empezar, los Log.d("HILO", "Antes: ...") y Log.d("HILO", "Despues: ..."), y los mensajes "Trabajando..." y "Terminó". La función cancelar() no cambia.
5. No toques PantallaTareas.kt ni las partes del ViewModel de primos y de cargas.
6. Prohibido usar GlobalScope, runBlocking y Thread.sleep. Quita los imports que queden sin uso.

Al terminar, dame un resumen corto con: qué archivos cambiaste, qué agregaste o quitaste en cada uno, cómo funciona ahora la cancelación cuando el usuario pulsa Cancelar, y en qué hilo corre cada parte (Antes, Dentro, Despues).
```

**Respuesta de Gemini (resumen):** agregó `fun tareaConProgreso(): Flow<Float>` a la interface (con el import de `Flow`). En `TareasRepositoryLocal` la implementó con `flow { }`: log "Dentro", 100 iteraciones con `delay(40)` y `emit(i / 100f)`, y `.flowOn(Dispatchers.Default)`. En el ViewModel cambió el bloque por `repo.tareaConProgreso().collect { _progreso.value = it }` y quitó los imports `Dispatchers`, `delay` y `withContext`. Explicó que al pulsar Cancelar, `trabajo?.cancel()` corta el `collect`, que el `delay(40)` del flow lanza `CancellationException`, que el `finally` vuelve a poner `corriendo` en `false` y que los hilos son: Antes en `main`, Dentro en `DefaultDispatcher-worker` por `flowOn` y Despues en `main`.

### Prompt 2 · Primos con progreso

```
Estoy en el proyecto CentroTareas (Kotlin, Jetpack Compose, Material 3, MVVM + Repository, corrutinas). Quiero aplicar esta mejora: mostrar el porcentaje del cálculo de números primos mientras avanza.

Archivos a modificar:
- app/src/main/java/com/tecsup/centrotareas/data/TareasRepository.kt
- app/src/main/java/com/tecsup/centrotareas/data/TareasRepositoryLocal.kt
- app/src/main/java/com/tecsup/centrotareas/viewmodel/TareasViewModel.kt
- app/src/main/java/com/tecsup/centrotareas/ui/PantallaTareas.kt

Requisitos:
1. En la interface TareasRepository cambia la firma a: suspend fun contarPrimos(hasta: Int, alAvanzar: (Float) -> Unit = {}): Int
2. En TareasRepositoryLocal.contarPrimos mantén withContext(Dispatchers.Default), el ensureActive() en cada vuelta y la función esPrimo sin cambios. Calcula val paso = maxOf(1, hasta / 100) y, cuando n % paso == 0, llama a alAvanzar(n.toFloat() / hasta). Al terminar el ciclo llama a alAvanzar(1f) antes de devolver la cantidad.
3. En TareasViewModel agrega private val _progresoPrimos = MutableStateFlow(0f) y expón val progresoPrimos: StateFlow<Float> de solo lectura. En contar(hasta) ponlo en 0f al empezar y pasa al repositorio alAvanzar = { _progresoPrimos.value = it }. Actualiza el StateFlow directamente, sin withContext, porque MutableStateFlow es seguro entre hilos.
4. En contar(hasta) mantén igual: el return si ya está Calculando, la medición con System.currentTimeMillis(), el estado Listo(cantidad, ms) y el catch de CancellationException que pone Cancelado y hace throw e.
5. En PantallaTareas, dentro de la tarjeta "Contar números primos": recolecta progresoPrimos con collectAsState(). Cuando el estado sea Calculando, muestra un LinearProgressIndicator(progress = { progresoPrimos }, modifier = Modifier.fillMaxWidth()) y debajo el texto "Calculando... X %" con (progresoPrimos * 100).toInt(). Cuando sea Cancelado, muestra "Cancelado al X %". Lo demás de la pantalla no cambia.
6. No modifiques model/Estados.kt, ni la tarea con progreso, ni las cargas.
7. Prohibido usar GlobalScope, runBlocking y Thread.sleep. Quita los imports que queden sin uso.

Al terminar, dame un resumen corto con: qué archivos cambiaste, qué agregaste o quitaste en cada uno, en qué hilo se llama alAvanzar y por qué no hace falta cambiar al hilo principal para actualizar el StateFlow, y si el resultado de contar primos sigue siendo el mismo (25 con 100, 168 con 1000, 1229 con 10000).
```

**Respuesta de Gemini (resumen):** cambió la firma de `contarPrimos` en la interface y en la implementación, agregó `val paso = maxOf(1, hasta / 100)` y llamó a `alAvanzar(n.toFloat() / hasta)` cada paso y a `alAvanzar(1f)` al final, sin tocar `withContext`, `ensureActive()` ni `esPrimo`. En el ViewModel agregó `_progresoPrimos` / `progresoPrimos`, lo reinicia en `0f` y lo actualiza desde la lambda; en la pantalla agregó la barra, "Calculando... X %" y "Cancelado al X %". Explicó que `alAvanzar` corre en el hilo de `Dispatchers.Default` y que no hace falta cambiar a `main` porque `MutableStateFlow` es seguro entre hilos. Afirmó que los resultados 25 / 168 / 1229 no cambian porque la lógica de `esPrimo` es la misma.

### Prompt 3 · Comparación de tiempos

```
Estoy en el proyecto CentroTareas (Kotlin, Jetpack Compose, Material 3, MVVM + Repository, corrutinas). Quiero aplicar esta mejora: una tabla que acumule los tiempos de las cargas "Una por una" y "A la vez" para ver la diferencia.

Archivos a modificar:
- app/src/main/java/com/tecsup/centrotareas/viewmodel/TareasViewModel.kt
- app/src/main/java/com/tecsup/centrotareas/ui/PantallaTareas.kt

Requisitos:
1. En TareasViewModel agrega dos MutableStateFlow<List<Long>> privados, _tiemposUnaPorUna y _tiemposALaVez, que empiecen vacíos, y expónlos como StateFlow<List<Long>> de solo lectura (tiemposUnaPorUna y tiemposALaVez).
2. En cargarUnaPorUna(), justo después de poner el estado Exito, agrega ms a _tiemposUnaPorUna y conserva solo los últimos 5 valores con takeLast(5). Haz lo mismo en cargarALaVez() con _tiemposALaVez. Si la carga falla, no se agrega nada.
3. Agrega fun limpiarTiempos() que deje ambas listas vacías.
4. No cambies nada más de cargarUnaPorUna() ni de cargarALaVez(): se mantienen el return si ya está Cargando, el try, el catch de CancellationException con throw e, el catch de Exception y el uso de _simularError.
5. En PantallaTareas recolecta ambas listas con collectAsState(). Dentro de la tarjeta "Dos cargas", debajo del resultado, muestra la tabla solo si alguna de las dos listas tiene datos. Créala como un Composable privado TablaTiempos(unaPorUna: List<Long>, aLaVez: List<Long>, onLimpiar: () -> Unit) que muestre:
   - Un subtítulo "Comparación de tiempos".
   - Una fila de encabezado con tres columnas: "#", "Una por una" y "A la vez", usando Row con Modifier.weight para alinear las columnas.
   - Una fila por cada medición (hasta el tamaño de la lista más larga), con el tiempo en ms o "-" si ese modo no tiene medición en esa posición.
   - Una fila "Promedio" con el promedio en ms de cada columna, o "-" si la columna está vacía.
   - Un texto "Diferencia: X ms" (promedio Una por una menos promedio A la vez) solo cuando ambas listas tienen datos.
   - Un TextButton "Limpiar" que llame a onLimpiar.
6. No modifiques model/Estados.kt, el Repository, la tarea con progreso ni los primos.
7. Prohibido usar GlobalScope, runBlocking y Thread.sleep. Quita los imports que queden sin uso.

Al terminar, dame un resumen corto con: qué archivos cambiaste, qué agregaste en cada uno, cómo se calcula el promedio y la diferencia, y qué pasa con la tabla cuando una carga termina en error.
```

**Respuesta de Gemini (resumen):** agregó `_tiemposUnaPorUna` / `_tiemposALaVez` con su versión de solo lectura y `limpiarTiempos()`. Guarda `ms` con `takeLast(5)` justo después del `Exito`. Creó `TablaTiempos` con encabezado, filas con "-" cuando falta dato, promedio con `average().toLong()`, "Diferencia: X ms" (promedio Una por una − promedio A la vez) y botón Limpiar. Explicó que, si la carga falla, la ejecución salta al `catch` antes de llegar a la línea que guarda el tiempo, así que la tabla no se altera.

## Qué aprendí

- Un `Flow` permite que el Repository produzca valores en segundo plano (`flowOn`) mientras el ViewModel solo los recolecta en el hilo principal.
- Una función como `alAvanzar` deja que una capa informe su avance sin conocer a la UI, y `MutableStateFlow` se puede actualizar desde cualquier hilo.
- La IA genera código correcto, pero no ejecuta la app: los resultados y el diseño visual hay que comprobarlos en el emulador. El error de la columna "Promedio" solo se vio al probar.