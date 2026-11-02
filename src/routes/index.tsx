import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "PanelPop — Traductor flotante de cómics para Android" },
      { name: "description", content: "Traduce mangas, manhwas y cómics al español sobre cualquier app Android, con OCR y traducción local." },
      { property: "og:title", content: "PanelPop — Traductor flotante de cómics" },
      { property: "og:description", content: "OCR y traducción local al español sobre cualquier app de lectura." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

const steps = [
  ["1", "Descarga el APK", "GitHub → Actions → Build PanelPop APK → artefacto PanelPop-debug-apk."],
  ["2", "Permite la superposición", "Ajustes → Mostrar sobre otras apps → PanelPop."],
  ["3", "ACTIVAR TRADUCTOR", "Acepta la captura de pantalla y abre tu app de cómics."],
  ["4", "Lee en español", "Toca la burbuja para pausar, arrástrala para moverla, mantenla para cerrar."],
];

const split = [
  ["En Lovable (esta web)", "Guía de instalación, explicación y documentación del proyecto."],
  ["En Android nativo (Kotlin)", "Burbuja flotante, MediaProjection, OCR ML Kit, traducción local ML Kit, capa de superposición y ajustes."],
];

function Index() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-3xl px-6 py-16">
        <span className="inline-block rounded-full bg-primary px-3 py-1 text-xs font-bold text-primary-foreground">ES</span>
        <h1 className="mt-6 text-5xl font-black tracking-tight sm:text-7xl">PanelPop</h1>
        <p className="mt-4 text-lg text-muted-foreground">
          Traductor flotante para mangas, manhwas y cómics. Lee en cualquier app y ve el texto en español encima, sin importar archivos ni pagar.
        </p>
        <div className="mt-12 grid gap-4">
          {steps.map(([n, t, d]) => (
            <div key={n} className="flex gap-4 rounded-2xl border bg-card p-5">
              <span className="text-3xl font-black text-primary">{n}</span>
              <div>
                <h2 className="font-bold">{t}</h2>
                <p className="text-sm text-muted-foreground">{d}</p>
              </div>
            </div>
          ))}
        </div>
        <h2 className="mt-14 text-2xl font-black">Qué hace cada parte</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {split.map(([t, d]) => (
            <div key={t} className="rounded-2xl bg-secondary p-5">
              <h3 className="font-bold">{t}</h3>
              <p className="mt-1 text-sm text-muted-foreground">{d}</p>
            </div>
          ))}
        </div>
        <p className="mt-10 text-xs text-muted-foreground">Sin anuncios, sin suscripciones. Las capturas nunca se guardan.</p>
      </section>
    </main>
  );
}
