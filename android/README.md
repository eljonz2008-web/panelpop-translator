# PanelPop — app Android nativa (Kotlin)

## Obtener el APK
1. Cada cambio en `android/` lanza GitHub Actions → pestaña **Actions** → "Build PanelPop APK" → descarga el artefacto `PanelPop-debug-apk`.
2. O abre la carpeta `android/` en Android Studio y pulsa Run.

## Cómo funciona
- `MainActivity` — permisos (superposición, notificaciones), botón ACTIVAR TRADUCTOR, ajustes.
- `TranslatorService` — servicio en primer plano tipo `mediaProjection`: captura con MediaProjection → firma 24×24 para saltar fotogramas iguales → OCR ML Kit (latín/japonés/coreano/chino) → identificación de idioma → ML Kit Translate al español (local, gratis, caché LRU) → `OverlayView`.
- `OverlayView` — capa no táctil (los toques pasan a la app de cómics), ajusta la letra automáticamente, modo cubrir o etiqueta.
- Burbuja flotante: tocar = pausar/reanudar, arrastrar = mover, mantener = cerrar. Notificación con Pausar/Cerrar.

Las capturas solo viven en memoria y se descartan tras el OCR. No hay claves secretas.
La primera vez se descarga el modelo de traducción (~30 MB) por idioma.
