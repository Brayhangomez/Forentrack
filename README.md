# ForenTrack 2.0

Plataforma forense académica con **8 proyectos Maven independientes** (Java 21, Spring Boot 4.0.8, MongoDB),
**10 roles con acciones exclusivas**, **API Gateway** como punto de entrada único y **bitácora de auditoría** de cada acción.

```
Navegador ──► forentrack-web (8080) ──► api-gateway (8090) ──┬─► usuarios-service        (8081)  usuarios_db
                                              ▲              ├─► casos-service           (8082)  casos_db
                                              │              ├─► evidencias-service      (8083)  evidencias_db
                       llamadas entre        │              ├─► cadena-custodia-service (8084)  custodia_db
                       servicios (JWT) ──────┘              ├─► auditoria-service       (8085)  auditoria_db
                                                             └─► laboratorio-service     (8086)  laboratorio_db
```

Todas las llamadas (web → servicios y servicio → servicio) pasan por el gateway y **propagan el JWT del usuario**.
Las operaciones que solo un servicio puede pedir (cambio de estado de una evidencia, registro de eventos de auditoría)
llevan además la cabecera `X-Internal-Key`, derivada automáticamente de `JWT_SECRET` (no hay que configurarla).

## 1. Orden de arranque y puertos

Cada servicio arranca solo (si otro está caído, solo fallan las operaciones que lo necesitan, con 503).
Orden recomendado para una demo local:

| # | Proyecto | Puerto | Base MongoDB por defecto |
|---|----------|--------|--------------------------|
| 1 | MongoDB | 27017 | — |
| 2 | `usuarios-service` (crea los 10 usuarios de prueba) | 8081 | `usuarios_db` |
| 3 | `auditoria-service` | 8085 | `auditoria_db` |
| 4 | `casos-service` | 8082 | `casos_db` |
| 5 | `evidencias-service` | 8083 | `evidencias_db` |
| 6 | `cadena-custodia-service` | 8084 | `custodia_db` |
| 7 | `laboratorio-service` | 8086 | `laboratorio_db` |
| 8 | `api-gateway` | 8090 | — |
| 9 | `forentrack-web` | 8080 | — |

- Web: <http://localhost:8080>
- Swagger unificado (gateway): <http://localhost:8090/swagger-ui.html>. Cada servicio también tiene el suyo en `/swagger-ui.html`.
- Importar en Spring Tools: *File > Import > Existing Maven Projects* y seleccionar la carpeta `forentrack`.
- Compilar: `mvn clean package` dentro de cada proyecto (las pruebas son unitarias, no necesitan MongoDB).
- Alternativa local: copia `.env.example` a `.env`, define `JWT_SECRET` y ejecuta `docker compose up --build`.

## 2. Variables de entorno

| Variable | Proyectos | Valor por defecto | Uso |
|----------|-----------|-------------------|-----|
| `JWT_SECRET` | los 6 servicios | **ninguno (obligatoria)** | Secreto HS256 compartido, mínimo 32 caracteres |
| `MONGODB_URI` | los 6 servicios | `mongodb://localhost:27017/<servicio>_db` | Base propia de cada servicio |
| `PORT` | todos | ver tabla de puertos | Render lo define solo |
| `GATEWAY_API_URL` | los 6 servicios y la web | `http://localhost:8090` | URL pública del gateway |
| `USUARIOS_API_URL`, `CASOS_API_URL`, `EVIDENCIAS_API_URL`, `CUSTODIA_API_URL`, `AUDITORIA_API_URL`, `LABORATORIO_API_URL` | `api-gateway` | `http://localhost:808x` | Destino de cada ruta del gateway |
| `JWT_EXPIRATION_MINUTES` | usuarios-service | `120` | Vida del token |
| `SEED_ENABLED` | usuarios-service | `true` | Crear o no los usuarios de prueba |
| `SEED_PASSWORD` | usuarios-service | `Forentrack2026!` (**solo desarrollo**) | Contraseña común de los usuarios de prueba |
| `<ROL>_PASSWORD` (`ADMIN_PASSWORD`, `FISCAL_PASSWORD`, …) | usuarios-service | `SEED_PASSWORD` | Contraseña de un rol concreto |
| `ADMIN_CORREO`, `SEED_DOMINIO` | usuarios-service | `admin@forentrack.test`, `forentrack.test` | Correos de prueba |
| `SESSION_TIMEOUT` | forentrack-web | `120m` | Sesión HTTP de la web |

> En Render define `SEED_PASSWORD` (o la variable de cada rol) con un valor propio o `SEED_ENABLED=false`.
> Los usuarios de prueba solo se crean si su correo no existe; nunca se modifican cuentas existentes.

## 3. Usuarios de prueba (uno por rol)

Contraseña por defecto en desarrollo: `Forentrack2026!` (o la de `SEED_PASSWORD` / `<ROL>_PASSWORD`).

| Rol | Correo | Variable de contraseña |
|-----|--------|------------------------|
| ADMINISTRADOR | `admin@forentrack.test` | `ADMIN_PASSWORD` |
| INVESTIGADOR | `investigador@forentrack.test` | `INVESTIGADOR_PASSWORD` |
| PERITO | `perito@forentrack.test` | `PERITO_PASSWORD` |
| CUSTODIO | `custodio@forentrack.test` | `CUSTODIO_PASSWORD` |
| FISCAL | `fiscal@forentrack.test` | `FISCAL_PASSWORD` |
| AUDITOR | `auditor@forentrack.test` | `AUDITOR_PASSWORD` |
| TECNICO_CAMPO | `tecnico@forentrack.test` | `TECNICO_CAMPO_PASSWORD` |
| ANALISTA_DIGITAL | `analista@forentrack.test` | `ANALISTA_DIGITAL_PASSWORD` |
| JEFE_LABORATORIO | `jefelab@forentrack.test` | `JEFE_LABORATORIO_PASSWORD` |
| REVISOR_LEGAL | `legal@forentrack.test` | `REVISOR_LEGAL_PASSWORD` |

## 4. Matriz Rol × Acción

Cada acción de escritura pertenece a **un solo rol** y se protege en dos niveles en el backend
(`SecurityFilterChain` por método y ruta + `@PreAuthorize`) y se oculta en la web (`Permiso` + `SesionInterceptor`, que responde 403).
Cualquier otro rol recibe **403** aunque llame la URL o la API directamente, y el intento queda en la auditoría.

| Rol | Acciones exclusivas (endpoint) | Consultas visibles | Total visibles |
|-----|--------------------------------|--------------------|----------------|
| **ADMINISTRADOR** | Crear usuario (`POST /api/v1/usuarios`) · Activar/desactivar cuenta (`PATCH /api/v1/usuarios/{id}/estado`) · Listar usuarios (`GET /api/v1/usuarios`) | Casos, Evidencias | 5 |
| **INVESTIGADOR** | Registrar caso (`POST /api/v1/casos`) · Iniciar investigación (`POST …/casos/{id}/inicio-investigacion`) · Agregar nota (`POST …/casos/{id}/notas`) · Solicitar análisis (`POST /api/v1/laboratorio/solicitudes`) | Casos, Evidencias, Custodia, Laboratorio | 8 |
| **FISCAL** | Asignar investigador (`POST …/casos/{id}/asignacion`) · Suspender (`…/suspension`) · Reactivar (`…/reactivacion`) · Cerrar caso (`…/cierre`) | Casos, Evidencias, Custodia, Laboratorio | 8 |
| **REVISOR_LEGAL** | Emitir concepto legal (`POST …/casos/{id}/concepto-legal`) · Validar cadena de custodia (`POST /api/v1/custodia/evidencias/{id}/validaciones`) · Autorizar descarte (`POST /api/v1/evidencias/{id}/descarte`) | Casos, Evidencias, Custodia, Laboratorio | 7 |
| **TECNICO_CAMPO** | Registrar evidencia (`POST /api/v1/evidencias`) · Completar acta de recolección (`PUT …/evidencias/{id}/recoleccion`) · Entregar a custodia (`POST /api/v1/custodia/entregas`) | Casos, Evidencias, Custodia | 6 |
| **CUSTODIO** | Recibir en bodega (`POST /api/v1/custodia/recepciones`) · Trasladar (`…/traslados`) · Devolver (`…/devoluciones`) | Casos, Evidencias, Custodia | 6 |
| **PERITO** | Iniciar peritaje (`POST …/solicitudes/{id}/peritaje/inicio`) · Emitir dictamen pericial (`…/peritaje/dictamen`) | Casos, Evidencias, Custodia, Laboratorio | 6 |
| **ANALISTA_DIGITAL** | Iniciar análisis digital (`…/digital/inicio`) · Registrar hash SHA-256 (`…/digital/hash`) · Emitir informe digital (`…/digital/informe`) | Casos, Evidencias, Custodia, Laboratorio | 7 |
| **JEFE_LABORATORIO** | Asignar análisis (`…/solicitudes/{id}/asignacion`) · Aprobar dictamen (`…/aprobacion`) · Devolver dictamen (`…/rechazo`) | Casos, Evidencias, Custodia, Laboratorio | 7 |
| **AUDITOR** | Consultar bitácora (`GET /api/v1/auditoria/eventos` y `/resumen`) · Exportar CSV (`…/eventos/exportar`) · Registrar hallazgo (`POST …/eventos/{id}/revision`) | Casos, Evidencias, Custodia, Laboratorio | 7 |

Las consultas son compartidas (no modifican datos): Casos y Evidencias para todos; Custodia para todos menos ADMINISTRADOR;
Laboratorio para INVESTIGADOR, FISCAL, PERITO, ANALISTA_DIGITAL, JEFE_LABORATORIO, REVISOR_LEGAL y AUDITOR.
Directorios auxiliares: `GET /api/v1/usuarios/directorio/investigadores` (FISCAL) y `/directorio/laboratorio` (JEFE_LABORATORIO).
La matriz se comprueba en `forentrack-web/src/test/.../MatrizPermisosTest.java`.

## 5. Estados y transiciones

**Caso:** `ABIERTO` → `ASIGNADO` (FISCAL) → `EN_INVESTIGACION` (INVESTIGADOR asignado) ⇄ `SUSPENDIDO` (FISCAL) → `CERRADO`
(FISCAL, exige concepto legal `FAVORABLE` del REVISOR_LEGAL). Las evidencias solo se registran en casos abiertos, asignados o en investigación.

**Evidencia:** `REGISTRADA` → `EN_CUSTODIA` (CUSTODIO, tras la entrega del técnico con acta completa) → `EN_ANALISIS`
(PERITO si es física/documental, ANALISTA_DIGITAL si es digital) → `ANALIZADA` (JEFE_LABORATORIO aprueba el dictamen)
→ `DEVUELTA` (CUSTODIO, exige cadena `VALIDA`). `DESCARTADA` (REVISOR_LEGAL) desde REGISTRADA, EN_CUSTODIA o ANALIZADA.

**Solicitud de laboratorio:** `SOLICITADA` (INVESTIGADOR) → `ASIGNADA` (JEFE) → `EN_PROCESO` (PERITO/ANALISTA) →
`DICTAMINADA` → `APROBADA` (JEFE) o vuelve a `EN_PROCESO` si el jefe la devuelve.

### Recorrido de demostración
1. **investigador** registra el caso → 2. **fiscal** lo asigna al investigador → 3. **investigador** inicia la investigación →
4. **tecnico** registra la evidencia, completa el acta y la entrega → 5. **custodio** la recibe en bodega →
6. **investigador** solicita el análisis → 7. **jefelab** lo asigna → 8. **perito** (o **analista**: hash + informe) inicia y dictamina →
9. **jefelab** aprueba → 10. **legal** valida la cadena y emite el concepto → 11. **custodio** devuelve → 12. **fiscal** cierra →
13. **auditor** revisa la bitácora y exporta el CSV.

## 6. Despliegue en Render

**Opción rápida:** el archivo `render.yaml` es un Blueprint que crea los 8 servicios (*New → Blueprint* y elegir este repositorio). Solo pide `MONGODB_URI` (la misma conexión de Atlas para los 6 servicios; el perfil `render` les da bases separadas `forentrack_<servicio>`). Render genera `JWT_SECRET` y `SEED_PASSWORD` en el grupo `forentrack-secretos`: la contraseña de los usuarios de prueba se consulta en *Env Groups → forentrack-secretos*.

**Opción manual:** Cada carpeta tiene su `Dockerfile` (igual que antes). Para los servicios nuevos crea un *Web Service* con *Root Directory*
`cadena-custodia-service`, `auditoria-service`, `laboratorio-service` y `api-gateway`.
- Servicios: `JWT_SECRET` (el mismo en todos), `MONGODB_URI` (una base distinta por servicio) y `GATEWAY_API_URL=https://<tu-gateway>.onrender.com`.
- Gateway: `USUARIOS_API_URL`, `CASOS_API_URL`, `EVIDENCIAS_API_URL`, `CUSTODIA_API_URL`, `AUDITORIA_API_URL`, `LABORATORIO_API_URL` con las URL públicas de cada servicio.
- Web: `GATEWAY_API_URL`. Las antiguas `USUARIOS_API_URL`/`CASOS_API_URL`/`EVIDENCIAS_API_URL` de la web y `CASOS_API_URL` de evidencias ya no se usan.

## 7. Compatibilidad con la versión anterior

- Se conservan los 4 proyectos, sus paquetes, los endpoints `/api/v1/...` y los datos: los estados antiguos siguen siendo válidos y los campos nuevos son opcionales.
- Cambios de permisos intencionales: `POST /api/v1/casos` ahora es solo de INVESTIGADOR y `POST /api/v1/evidencias` solo de TECNICO_CAMPO (antes también ADMINISTRADOR/INVESTIGADOR), para que ninguna acción tenga dos dueños.
- `LoginResponse` agrega el campo `correo` al final.

## 8. Errores y sesión

Todos los servicios devuelven el mismo JSON de error (`estado`, `mensaje`, `errores`, `fecha`) con 400/401/403/404/409/503.
El gateway responde 404 si la ruta no existe y 503 si el servicio destino no responde.
En la web, un token vencido o un 401 cierra la sesión y lleva a `/login?expiro`; un 403 muestra la página de acceso denegado.
