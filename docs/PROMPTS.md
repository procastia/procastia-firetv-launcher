# Prompts para hacerlo con IA

Estos son los prompts, en orden, que puedes darle a **Claude** para recrear o adaptar este proyecto. Cambia lo que está entre `[corchetes]`.

> Tip: si tu IA puede ver tu pantalla o tus archivos (como Claude en la app de escritorio), dale acceso a la carpeta del proyecto: así puede revisar errores y archivos por ti.

---

## 1. Identifica tu dispositivo

> Te mando fotos de la etiqueta de mi Fire TV Stick. ¿Qué modelo exacto es y qué versión de Fire OS puede tener? ¿Se puede controlar por ADB?

Luego confirma en la tele: **Configuración → Mi Fire TV → Acerca de**.

## 2. Conéctate por ADB

> Ya activé las Opciones de desarrollador y la Depuración ADB en mi Fire TV. Su IP es [IP]. Uso [Windows/Mac]. Dame paso a paso cómo instalar adb y conectarme por Wi‑Fi, y unos comandos para probar que la tele me hace caso.

## 3. Diseña tu menú

🎨 **Aquí es donde Claude te ayuda a diseñar tu menú con tus colores y tus archivos gráficos.**

> Quiero diseñar el menú de inicio de mi Fire TV (pantalla 1920×1080, se maneja con flechas y OK). Mi estilo: [colores, tipografía, "pixel art", "minimalista", "retro"...]. Te adjunto [mi logo / mi personaje / mi paleta]. Debe tener: [hora, saludo, fila de apps, accesos, un personaje que hable...]. Hazme primero un diseño para verlo y luego lo convertimos en código.

Para cambiar el personaje:

> Te mando una imagen de [mi mascota / mi personaje]. Recórtala, quítale el fondo y ponla en el menú en lugar de Sirio.

## 4. Conviértelo en una app de Android

> Convierte este diseño en una app de Android para Fire TV: una sola Activity con un WebView que cargue el HTML desde assets, navegación con flechas del control, y que abra apps por su nombre de paquete. Dame el proyecto completo para Android Studio y los comandos para compilar e instalar con adb.

Si Amazon no te deja usarlo como Home:

> Amazon bloquea los launchers personalizados en mi Fire OS. Haz que cuando aparezca el Home de Amazon (com.amazon.tv.launcher) se abra mi app encima, usando UsageStatsManager y un servicio en primer plano. Dime qué permiso dar con adb.

## 5. Diseña tu control

📱 **Aquí también puedes coordinar a Claude para que diseñe tu propio control.**

> Quiero un control remoto para mi iPhone que hable directo con mi Fire TV (sin PC). Agrega a mi app de Android un servidor web pequeño (puerto 8765) que sirva una página con botones. El botón HOME debe abrir MI menú, no el de Amazon. Diseño del control: [cómo lo imaginas: colores, botones, estilo]. Que pueda guardarse en la pantalla de inicio del iPhone con mi ícono.

Ideas para pedirle después:

> Rediseña mi control con [estilo]. Agrega botones de acceso directo a [apps]. Hazlo más grande para [mis papás / niños].

## 6. Cuando algo falle

> Esto es lo que sale en mi terminal: [pega el error o manda captura]. ¿Qué significa y cómo lo arreglo?

Errores que nos salieron y su solución: ver la [guía paso a paso](GUIA-PASO-A-PASO.md#problemas-comunes).

## 7. Tu video

> Hice este proyecto y lo voy a grabar para YouTube. Ayúdame con un guion de 8–10 minutos: gancho, la explicación de por qué [mi TV] no se podía y el Fire TV sí, el proceso con Claude y un cierre honesto. Dame también 5 ganchos para Shorts.
