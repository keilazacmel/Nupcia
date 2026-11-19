# Especificación Técnica de Producto y Plan de Ejecución
**Proyecto:** Plataforma Integral de Planificación de Bodas, Marketplace con Mapa Geoespacial, Gestión de Proveedores, Papelería Personalizable y Asistente IA de Presupuesto.  
**Referencias Conceptuales:** Zola (planificación, sitio web de boda, gestión de invitados/RSVP, *seating chart*, *registry*/*cash funds*) + The Knot Paper (*suites* coordinadas, editor visual de papelería e impresión bajo demanda).

---

## 1. Visión General de la Idea y Propuesta de Valor

Construir una aplicación web/móvil multi-rol que centralice todo el ecosistema de planificación de bodas en un solo flujo conectado:
1. **Experiencia Personalizada para la Novia / Pareja:** Desde el primer inicio de sesión, la plataforma captura la visión estética, expectativas emocionales y prioridades de la novia para adaptar visualmente el catálogo, las plantillas y la distribución financiera.
2. **Asistente IA con Presupuesto Dinámico por Prioridades:** Un *chatbot* contextual que recalcula y asigna porcentajes del presupuesto total según lo que la novia considere prioritario, recomendando proveedores reales dentro de ese rango y permitiendo aplicar los ajustes con un clic.
3. **Descubrimiento Geoespacial (Mapa + API):** Búsqueda interactiva de *venues* y proveedores filtrados por presupuesto, estilo, disponibilidad en fecha y radio de cobertura.
4. **Portal Operativo para Proveedores:** Herramienta B2B donde los proveedores gestionan su catálogo, clientes, calendario y métricas, y donde **cada solicitud aceptada se convierte automáticamente en tareas operativas por cumplir**.
5. **Núcleo de Evento y Papelería (Zola + The Knot Paper):** Sitio web de boda, recolección de direcciones, RSVP dinámico, *seating chart*, *registry* con fondos en efectivo y un editor visual de papelería conectado a los datos de invitados.

---

## 2. Arquitectura Técnica Recomendada (Stack para IA de Código)

* **Frontend:** Next.js (App Router) + React + TypeScript + Tailwind CSS + Shadcn/UI.
* **Editor de Papelería:** HTML5 Canvas / SVG (Fabric.js o Konva.js) con serialización en JSON Scene Graph.
* **Mapas y Geolocalización:** Mapbox GL JS o Google Maps Platform (Places Autocomplete, Geocoding, Distance Matrix) + extensión espacial `PostGIS` en base de datos.
* **Backend & APIs:** Next.js Route Handlers / Server Actions o NestJS (Node.js + TypeScript) con validación estricta mediante `Zod`.
* **Base de Datos y Caché:** PostgreSQL (con soporte `JSONB` y `PostGIS`) + Prisma ORM o Drizzle ORM; Redis para colas de trabajos (generación de PDFs de impresión y notificaciones).
* **Autenticación y Seguridad:** Auth.js / Clerk / Supabase Auth con RBAC (*Role-Based Access Control*) para roles: `couple`, `vendor`, `guest`, `admin`.
* **Pagos y Almacenamiento:** Stripe Connect (para *cash funds* y pagos) + AWS S3 / Cloudflare R2 (para activos gráficos y PDFs).
* **IA / Chatbot:** Vercel AI SDK / OpenAI / Gemini API con *Tool Calling* conectado a funciones deterministas de cálculo de presupuesto y consultas SQL.

---

## 3. Modelo de Datos Principal (Esquema Conceptual)

* `users`: `id`, `email`, `password_hash`, `role` (`couple` | `vendor` | `guest` | `admin`), `created_at`.
* `weddings`: `id`, `owner_user_id`, `partner_name`, `wedding_date`, `city`, `lat`, `lng`, `total_budget`, `estimated_guests`, `slug`, `selected_suite_id`.
* `bride_preferences`: `id`, `wedding_id`, `style_tags` (`JSONB`), `color_palette` (`JSONB`), `ceremony_type`, `vision_expectations` (`TEXT`), `priority_ranking` (`JSONB` con categoría, posición y peso).
* `budget_allocations`: `id`, `wedding_id`, `category`, `allocated_amount`, `spent_amount`, `priority_rank`, `is_locked`.
* `vendor_profiles`: `id`, `user_id`, `business_name`, `category`, `description`, `address`, `lat`, `lng`, `coverage_radius_km`, `min_price`, `max_price`, `style_tags` (`JSONB`), `rating_avg`.
* `vendor_catalog_items`: `id`, `vendor_id`, `title`, `description`, `base_price`, `pricing_unit`, `images` (`JSONB`), `is_active`.
* `service_requests`: `id`, `wedding_id`, `vendor_id`, `catalog_item_id`, `event_date`, `proposed_budget`, `notes`, `status` (`pending` | `accepted` | `rejected` | `countered` | `completed`).
* `vendor_tasks`: `id`, `vendor_id`, `service_request_id`, `wedding_id`, `title`, `description`, `due_date`, `status` (`pending` | `in_progress` | `completed`), `is_milestone`.
* `vendor_calendar_blocks`: `id`, `vendor_id`, `service_request_id`, `start_date`, `end_date`, `block_type` (`booked` | `manual_block`).
* `households` & `guests`: `id`, `wedding_id`, `household_id`, `full_name`, `email`, `phone`, `postal_address`, `rsvp_status`, `meal_choice`, `dietary_notes`, `table_id`.
* `registry_items` & `contributions`: `id`, `wedding_id`, `type` (`product` | `cash_fund`), `title`, `target_amount`, `collected_amount`, `contributor_name`, `amount`, `status`.
* `stationery_designs`: `id`, `wedding_id`, `suite_id`, `product_type` (`invitation` | `save_the_date` | `menu` | `envelope`), `scene_graph` (`JSONB`), `proof_approved`, `print_pdf_url`.

---

## 4. Requerimientos Funcionales (RF)

### Módulo A: Autenticación, Onboarding y Personalización de la Novia
* **RF-01 — Login Multi-Rol:** Permitir registro e inicio de sesión (correo/contraseña y OAuth) redirigiendo al dashboard correspondiente según el rol (`couple` o `vendor`).
* **RF-02 — Cuestionario de Visión y Gustos de la Novia (Onboarding):** Al ingresar por primera vez, guiar a la novia por un *quiz* interactivo que capture:
  * Estilo estético (clásico, bohemio, moderno, jardín, minimalista, destino) y paleta de colores.
  * Tipo de ceremonia/recepción, fecha, ubicación y número de invitados.
  * Expectativas clave de su boda (texto abierto + selección de momentos prioritarios: fiesta/música, fotos editoriales, gastronomía, decoración floral, ceremonia íntima).
  * **Ranking de Prioridades:** Interfaz ordenable donde la novia clasifica las categorías de gasto de mayor a menor importancia.
* **RF-03 — Adaptación Visual y Financiera Inicial:** Con base en el *onboarding*, preseleccionar plantillas de sitio web y *suites* de papelería acordes a su estilo, ordenar el catálogo de proveedores por afinidad y calcular la distribución inicial del presupuesto (`budget_allocations`) ponderada por sus prioridades.

### Módulo B: Chatbot de Recomendaciones con Ajuste Dinámico de Presupuesto
* **RF-04 — Contexto Completo de la Novia:** El asistente conversacional debe leer en tiempo real `total_budget`, montos ya comprometidos (`spent_amount`), `bride_preferences`, `priority_ranking` y fecha/ubicación del evento.
* **RF-05 — Motor Determinista de Reasignación de Presupuesto:** Cuando la novia solicite recomendaciones para uno o varios rubros (ej. *"Busco fotógrafo y flores"*), el backend (vía *tool calling*, nunca cálculo libre del LLM) debe:
  1. Calcular pesos proporcionales dando mayor porcentaje del saldo disponible a las categorías marcadas como **Prioridad Alta** y reduciendo el rango sugerido en categorías de **Prioridad Baja**.
  2. Garantizar que la suma de partidas nunca supere `total_budget`.
  3. Devolver rangos exactos (`min`, `ideal`, `max`) y explicar el impacto en el resto del presupuesto.
* **RF-06 — Recomendación de Proveedores y Acciones Directas:** Consultar proveedores que coincidan con el rango recalculado, estilo y disponibilidad, mostrando tarjetas interactivas dentro del chat con botones para: **"Aplicar este ajuste de presupuesto"**, **"Ver en el mapa"** y **"Enviar solicitud"**.

### Módulo C: Mapa Interactivo + API Geoespacial
* **RF-07 — Búsqueda en Mapa con Filtros Combinados:** Visualizar *venues* y proveedores como pines agrupables (*clustering*) en un mapa interactivo con filtros por categoría, rango de presupuesto asignado, estilo, disponibilidad de fecha y distancia.
* **RF-08 — Geocodificación y Radio de Cobertura:** Autocompletar direcciones mediante API de mapas, convertir ubicaciones a coordenadas (`lat`, `lng`) y filtrar proveedores cuyo `coverage_radius_km` incluya la ubicación de la boda.

### Módulo D: Portal de Proveedor (Catálogo, Solicitudes $\rightarrow$ Tareas, Calendario, Clientes y Estadísticas)
* **RF-09 — Gestión de Catálogo:** Permitir al proveedor crear, editar, activar/desactivar paquetes, servicios y precios con galería de imágenes.
* **RF-10 — Flujo de Solicitudes y Generación Automática de Tareas:** Permitir al proveedor revisar solicitudes recibidas (`pending`) y **aceptar**, **rechazar** o **contraofertar**. Al **aceptar una solicitud**, el sistema debe ejecutar atómicamente:
  1. Actualizar `service_requests.status = 'accepted'`.
  2. Bloquear la fecha en `vendor_calendar_blocks`.
  3. **Crear automáticamente las tareas por cumplir (`vendor_tasks`)** vinculadas al cliente y a la fecha del evento (ej. *Confirmación de contrato y anticipo*, *Reunión de alineación*, *Entrega/Ejecución del servicio*, *Cierre post-evento*), visibles en un tablero Kanban/Lista de tareas del proveedor.
* **RF-11 — Calendario Unificado:** Mostrar fechas reservadas, bloqueos manuales y vencimientos de tareas derivadas de solicitudes aceptadas.
* **RF-12 — Gestión de Clientes:** Listado de parejas con solicitudes activas o completadas, historial de acuerdos, estado de tareas y notas del evento.
* **RF-13 — Estadísticas de Eventos Cumplidos:** Dashboard analítico con total de eventos completados (`status = 'completed'`), tasa de conversión de solicitudes, ingresos acumulados, ingresos por mes y calificación promedio.

### Módulo E: Sitio Web de Boda, Invitados/RSVP, Registry y Papelería (Zola + The Knot Paper)
* **RF-14 — Sitio Web de Boda Personalizable:** Generar una URL pública/protegida por contraseña con secciones modulares (historia, itinerario, mapa del *venue*, alojamiento, RSVP y *registry*) heredando la estética de la *suite* elegida.
* **RF-15 — Gestión de Invitados, Hogares y RSVP:** Agrupar invitados por hogar (`households`), compartir enlace de recolección de direcciones postales, gestionar confirmaciones por evento, elección de menú y alergias, y asignar mesas en un *seating chart* interactivo.
* **RF-16 — Registry y Fondos en Efectivo:** Permitir crear listas de regalos físicos y *cash funds* (luna de miel, hogar) con aportes grupales vía Stripe y registro de agradecimientos.
* **RF-17 — Editor Visual de Papelería e Impresión:** Permitir personalizar invitaciones, menús y sobres por *suite*, inyectar automáticamente nombres y direcciones desde la tabla `households`, validar márgenes de sangrado (*bleed*) y aprobar pruebas para generar archivos PDF listos para impresión.

---

## 5. Reglas de Negocio Críticas

1. **Integridad de Presupuesto:** Ninguna recomendación del *chatbot* puede asignar un monto que deje el saldo restante en negativo; cualquier cambio sugerido requiere confirmación explícita de la novia antes de sobrescribir `budget_allocations`.
2. **Automatización Solicitud $\rightarrow$ Tareas:** Una tarea operativa del proveedor solo nace cuando `service_requests.status` transiciona a `accepted`. Si una solicitud se cancela posteriormente, sus tareas pendientes pasan a `cancelled` y se libera el bloque en el calendario.
3. **Conteo de Eventos Cumplidos:** Las estadísticas de *"eventos cumplidos"* del proveedor solo contabilizan registros donde la fecha del evento haya pasado y el servicio esté marcado como `completed` (no solicitudes futuras).
4. **Aislamiento de Privacidad:** Los invitados de una boda solo pueden ver y editar los datos de su propio hogar mediante un *token* único; los proveedores solo acceden a datos de contacto de parejas que les hayan enviado una solicitud.

---

## 6. Plan de Ejecución Actualizado (16 Semanas)

### Fase 1: Arquitectura Base, Auth, Onboarding de la Novia y Presupuesto Ponderado (Semanas 1–2)
* Configurar repositorio (Next.js + TypeScript + PostgreSQL/PostGIS + Prisma + Auth).
* Implementar registro/login multi-rol (`couple` / `vendor`).
* Construir el **Quiz de Onboarding de la Novia** (gustos, estilo, expectativas y ranking de prioridades) y el motor inicial de `budget_allocations`.

### Fase 2: Portal de Proveedor (Catálogo, Solicitudes $\rightarrow$ Tareas, Calendario, Clientes y Estadísticas) (Semanas 3–5)
* Desarrollar el CRUD de catálogo de servicios del proveedor.
* Implementar el flujo completo de `service_requests`: envío por parte de la novia y aceptación por parte del proveedor con **generación automática de `vendor_tasks`** y bloqueo en `vendor_calendar_blocks`.
* Construir vistas de Clientes, Calendario y Panel de Estadísticas de Eventos Cumplidos.

### Fase 3: Mapa Interactivo y Descubrimiento Geoespacial (Semanas 6–7)
* Integrar API de Mapas (Mapbox/Google Maps) y consultas espaciales en PostGIS.
* Construir la vista dividida (Lista + Mapa) con filtros por categoría, presupuesto asignado, estilo y radio de cobertura.

### Fase 4: Chatbot IA con Rebalanceo Dinámico de Presupuesto (Semanas 8–9)
* Implementar las herramientas deterministas del backend (`calculate_priority_budget` y `search_matching_vendors`).
* Conectar el LLM con *Tool Calling* para explicar ajustes financieros según prioridades de la novia y renderizar tarjetas accionables (aplicar presupuesto, ver en mapa, solicitar cotización).

### Fase 5: Núcleo de Boda (Sitio Web, Invitados/RSVP, Seating Chart, Registry y Papelería) (Semanas 10–13)
* Desarrollar el sitio web público de boda, recolección de direcciones, RSVP dinámico y *seating chart*.
* Integrar *registry* y *cash funds* con Stripe Connect y *webhooks*.
* Construir el editor visual de papelería (Canvas + JSON Scene Graph), combinación de direcciones de invitados y exportación de prueba/impresión.

### Fase 6: QA Integral, Seguridad y Lanzamiento Piloto (Semanas 14–16)
* Pruebas E2E en flujos críticos: *Onboarding* de novia $\rightarrow$ ajuste de presupuesto en *chatbot* $\rightarrow$ búsqueda en mapa $\rightarrow$ solicitud $\rightarrow$ aceptación y tareas del proveedor $\rightarrow$ RSVP/pagos.
* Optimización de rendimiento, auditoría de permisos RBAC y despliegue piloto.

---

## 7. Guía Rápida de Prompts para Desarrollar con una IA de Código

Puedes alimentar este documento a tu IA de código (Cursor, Windsurf, Claude Code, etc.) y ejecutar el desarrollo iterativamente con estas instrucciones:

1. **Paso 1 (Base de Datos):** *"Lee `especificacion_plataforma_bodas.md` y genera el archivo `schema.prisma` completo con PostgreSQL, PostGIS, índices y relaciones para todas las entidades descritas."*
2. **Paso 2 (Onboarding & Presupuesto):** *"Implementa el flujo multi-paso del Onboarding de la Novia (`RF-02` y `RF-03`) junto con la función pura en TypeScript `rebalanceBudgetByPriorities()` que calcule las partidas según el ranking de prioridades."*
3. **Paso 3 (Portal Proveedor + Tareas):** *"Crea los Server Actions y la UI del Portal de Proveedor (`RF-09` a `RF-13`), asegurando que al aceptar una `ServiceRequest` se cree una transacción que bloquee la fecha en el calendario y genere las tareas por defecto en `VendorTask`."*
4. **Paso 4 (Mapa + Chatbot):** *"Implementa el componente de mapa interactivo con filtros geoespaciales (`RF-07`) y el endpoint `/api/chat` usando Vercel AI SDK con tools conectadas a `rebalanceBudgetByPriorities` y búsqueda de proveedores."*
5. **Paso 5 (Sitio de Boda + Editor de Papelería):** *"Construye el generador dinámico de sitio de boda con RSVP por hogar (`RF-14`, `RF-15`) y el editor Canvas de papelería con inyección de variables de invitados (`RF-17`)."*