# CentroTareas

## Observación del Paso 1

**Qué pasa con la pantalla:** al tocar **Tarea larga (MAL)** y enseguida pulsar **Tócame** varias veces, durante 4 segundos nada responde: el contador no sube y "Trabajando..." nunca llega a verse. Al terminar la espera aparece **Terminó** y el contador salta de golpe al total de toques. Los toques no se perdieron: quedaron en la cola de eventos y el hilo principal los procesó todos juntos al liberarse. Después de eso, el contador vuelve a subir de uno en uno con cada toque.

**Por qué no se ve "Trabajando...":** `Thread.sleep(4000)` bloquea el hilo principal, que es el mismo que dibuja la pantalla y atiende los toques. El mensaje sí cambia a "Trabajando...", pero el hilo no tiene oportunidad de volver a dibujar. Cuando se libera, el mensaje ya vale "Terminó" y solo se dibuja ese último valor.

**Si la espera fuera de 10 segundos:** Android mostraría el diálogo **ANR** ("La app no responde"), porque el hilo principal pasa más de 5 segundos sin atender los eventos del usuario. Ese diálogo da la opción de esperar o cerrar la app.