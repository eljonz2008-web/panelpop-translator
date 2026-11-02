# PanelPop Translator

Analiza

Crea PanelPop, una aplicación Android de traducción de pantalla en tiempo real, especializada en cómics, mangas y manhwas.



OBJETIVO:

Permitir que el usuario abra cualquier aplicación o página web donde lea cómics, active un traductor flotante y vea los textos traducidos al español directamente sobre la pantalla original, sin importar archivos ni abandonar la aplicación que está leyendo.



IMPORTANTE: No quiero un simple traductor web. Quiero una aplicación Android funcional que traduzca el contenido visible de otras aplicaciones.



1. TRADUCTOR FLOTANTE



- Crear un botón flotante que permanezca visible sobre otras aplicaciones.

- Al tocarlo, activar o desactivar la traducción.

- Permitir mover el botón por la pantalla.

- Añadir opciones para pausar, reanudar y cerrar el traductor.

- No bloquear los controles de la aplicación original.



2. CAPTURA DE PANTALLA



- Utilizar Android MediaProjection para capturar la pantalla después de solicitar el permiso del usuario.

- Procesar los fotogramas capturados para detectar los textos visibles.

- No intentar capturar contenido sin autorización.

- Gestionar correctamente los permisos y las notificaciones del servicio de captura.



3. OCR Y TRADUCCIÓN EN VIVO



- Detectar automáticamente los textos de la pantalla mediante OCR.

- Reconocer globos de diálogo, cuadros de narración y otros textos de cómics.

- Detectar el idioma original y traducirlo al español.

- Utilizar un motor OCR y un motor de traducción gratuitos que puedan ejecutarse localmente cuando sea posible.

- Evitar procesar repetidamente regiones que no hayan cambiado.

- Actualizar las traducciones cuando el usuario cambie de página o desplace el contenido.

- Mantener la traducción sincronizada con el contenido visible.



4. SUPERPOSICIÓN VISUAL



- Mostrar las traducciones en una capa flotante transparente sobre la aplicación original.

- Colocar cada traducción cerca de su texto de origen.

- Ajustar automáticamente el tamaño de la fuente y el ancho del texto.

- Incluir un modo que cubra visualmente el texto original con un fondo adecuado y muestre la traducción encima.

- Permitir modificar el tamaño de la letra, la opacidad del fondo y la posición de las traducciones.

- Conservar los dibujos originales siempre que sea posible.



5. INTERFAZ



- Crear una interfaz sencilla, moderna y optimizada para Android.

- Pantalla principal con un botón grande: ACTIVAR TRADUCTOR.

- Mostrar el estado de los permisos y una guía para activar la superposición.

- Incluir ajustes de idioma, tamaño de letra y rendimiento.

- No incluir importación de imágenes, PDF o ZIP.

- No incluir suscripciones, anuncios ni compras integradas.



6. TECNOLOGÍAS Y GITHUB



- Utilizar Lovable para construir la interfaz de administración y configuración.

- Sincronizar el proyecto con GitHub.

- Desarrollar el componente Android nativo con Kotlin y Android Studio.

- Utilizar las API oficiales de Android para captura de pantalla y superposición.

- Utilizar OCR local, por ejemplo ML Kit Text Recognition, y evaluar un motor de traducción local compatible.

- No depender de servicios de traducción de pago.

- No almacenar capturas de pantalla innecesariamente.

- No publicar claves secretas en GitHub.



7. FUNCIONAMIENTO REAL

   Implementar las funciones reales, no crear botones que solamente aparenten funcionar.



Primero desarrollar un prototipo Android que pueda solicitar los permisos, capturar la pantalla, detectar texto, traducirlo y mostrar el resultado en una superposición.



Después optimizarlo para mangas y manhwas, reduciendo retrasos, consumo de batería y procesamiento repetido.



Explicar qué partes se pueden desarrollar en Lovable y cuáles deben implementarse en el proyecto Android nativo.



RESULTADO FINAL:

Una aplicación Android instalable que permita activar un traductor flotante, leer cómics en cualquier aplicación compatible y visualizar las traducciones al español en tiempo real, sin importar archivos y sin pagar suscripciones.







Analiza

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/f7cc6eb7-514a-4b4e-b6f6-60c404b81726).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
