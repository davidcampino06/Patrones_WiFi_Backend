# Auditoría técnica final de WiFiSense (Avance 18)

Auditoría de los cuatro repositorios contra los requisitos del proyecto final, con las correcciones hechas y su
evidencia. Fecha: 23/10/2026. Todo lo que dice "ejecutado" se corrió de verdad; lo que no se pudo comprobar dice
**no verificable** y por qué.

## 1. Repositorios revisados

| Repositorio | Copia revisada | Estado de Git al empezar | Commits en `main` |
|---|---|---|---|
| Base de datos — `davidcampino06/Patrones_WiFi_db` | `WiFiSense-database` | idéntica a `main` de GitHub (Avance 17 con merge) | 160 |
| Backend — `davidcampino06/Patrones_WiFi_Backend` | `WiFiSense-backend` | idéntica a `main` | 269 |
| IA y agente — `davidcampino06/Patrones_WiFi_-Artificial_Intelligence` | `WiFiSense-ai` | idéntica a `main` | 116 |
| Página — `davidcampino06/Patrones_WiFi_Froned` | `WiFiSense-frontend` | idéntica a `main` | 177 |

Los repositorios de `Chindoy12` (IA y página) son los *forks* que despliegan Railway y Vercel: se actualizan con
*Sync fork* desde los de `davidcampino06`, que es donde se trabaja. No había cambios locales sin subir.

## 2. Pruebas antes de cambiar nada (ejecutadas)

| Repositorio | Comando | Resultado |
|---|---|---|
| Backend | compilar + JUnit 5 (unitarias, controladores, seguridad) + integración contra PostgreSQL 16 como `wifisense_app` | 351 de 351 (12 de integración) |
| IA | `python -m pytest -q` | 239 aprobadas, 2 omitidas (solo corren en Windows) |
| Página | `npx tsc --noEmit`, `npm test`, `npm run build` | tipos sin errores, 133 de 133, build correcto |
| Base de datos | `apply.sh --seed --security --test` en PostgreSQL 16 vacío | 166 comprobaciones aprobadas, 0 fallas |

## 3. Matriz inicial de cumplimiento

Estados: **C** cumple · **P** parcial · **N** no cumple · **NV** no verificable.

| # | Requisito o funcionalidad | Repositorio y archivos | Estado | Evidencia en el código | Problema concreto | Corrección | Prueba que lo demuestra |
|---|---|---|---|---|---|---|---|
| 1 | Caso de estudio real | todos; `docs/sustentacion.md` | C | Monitoreo de redes Wi-Fi de casas (Mi Wi-Fi) y de una sede (sensor por red) con datos reales del agente | — | — | Demostración en vivo |
| 2 | IA funcional | IA `app/model_service.py`, `app/training.py`; backend `AnomalyDetectionStrategy`, `AiTrainingService` | C | Isolation Forest entrenado, cargado al arrancar y usado en cada detección; reentrenamiento con mediciones reales | — | — | `test_model_service.py`, `test_training.py`, `AnomalyDetectionStrategyTest` |
| 3 | Distinguir modelo, reglas y servicio externo | IA `quality.py`, `security.py`, `channel.py`, `diagnosis.py`, `llm_advisor.py`; página | P | Solo la detección es aprendizaje automático; calidad, seguridad, canal y causa probable son reglas; Claude es opcional | La página dice "Consejos de la IA" y "Calificado por la IA" para resultados de reglas; ningún documento lo separa | Textos honestos y una tabla "qué es modelo y qué son reglas" en Modelo de IA y en el README de la IA | `aiModel.test.tsx` |
| 4 | Al menos 5 patrones implementados | backend `network/`, `analysis/`, `event/` | C | Factory Method, Decorator, Strategy, State, Observer y Facade en el flujo real, con pruebas | — | — | `NetworkDataSourceProviderTest`, `DataSourceDecoratorTest`, `*StrategyTest`, `NetworkStateTest`, `ObserverTest`, `NetworkAnalysisFacadeTest` |
| 5 | Documentación de patrones fiel al código | backend `PATTERNS.md` | P | — | Dice que **Adapter** no está implementado, pero `HttpAiAnalysisClient` (API REST de la IA) y `BrevoEmailSender` (API de Brevo) son adaptadores; Template Method sin prueba propia | Corregir `PATTERNS.md`, diagrama de clases, prueba del método plantilla | `DataSourceCreatorTemplateTest` |
| 6 | Investigar otros patrones | backend `PATTERNS.md` | P | Tabla de 6 patrones descartados | Falta el análisis de los nuevos candidatos (Circuit Breaker, Outbox, Chain of Responsibility, Singleton…) | Ampliar la investigación con decisión y motivo | Revisión del documento |
| 7 | IA no disponible: comportamiento controlado | backend `AgentInsights`, `HttpAiAnalysisClient`, `NetworkAnalysisFacade` | P | Si la IA falla, el reporte se guarda sin veredictos y el análisis responde 503 | Si la IA **no responde** (redespliegue, arranque en frío), cada llamada espera 10 s: un reporte del sensor hace hasta 4 llamadas (hasta 40 s ocupando un hilo) y el análisis con IA espera **dentro de una transacción**, ocupando una de las 10 conexiones a la base | **Proxy con Circuit Breaker** delante de `AiAnalysisClient`; la llamada a la IA del análisis sale de la transacción | `CircuitBreakerAiClientTest`, `NetworkAnalysisFacadeTest` |
| 8 | Contrato JSON backend ↔ IA | backend `analysis/Ai*`; IA `app/schemas*.py` | P | Nombres y tipos coinciden hoy (revisado a mano) | No hay prueba que lo asegure: un cambio de nombre en un lado solo se descubriría en producción | Pruebas de contrato con los mismos ejemplos JSON en los dos repositorios | `AiContractTest`, `test_contract.py` |
| 9 | La IA valida a quien la llama | IA `app/main.py` | P | Clave `X-API-Key` comparada en tiempo constante; límite de 8 MB; validación de cada campo | Si `AI_API_KEY` falta, la IA acepta cualquier solicitud, también en Railway | En Railway la IA no arranca sin clave; en local sigue siendo opcional | `test_security_config.py` |
| 10 | Frontend, backend y base de datos | 3 repositorios | C | React + TypeScript → API REST Spring Boot → PostgreSQL 16 | — | — | Pruebas de los tres |
| 11 | Java en el backend | backend `pom.xml` | C | Java 21, Spring Boot 3.5.6 | — | — | Compilación |
| 12 | El navegador solo habla con el backend | página `src/services/apiClient.ts` | C | Un único punto de salida HTTP | — | — | `security.test.ts` |
| 13 | URL de producción del backend en la página | página `apiClient.ts`, `vite.config.ts` | P | Usa `VITE_API_URL` | Si falta en Vercel, el build sale con `http://localhost:8080` sin avisar | El build de Vercel falla si falta `VITE_API_URL` | Build sin la variable con `VERCEL=1` |
| 14 | Autenticación y roles en el backend | backend `SecurityConfig`, controladores | C | JWT + refresh token de un uso, roles por ruta, rutas del equipo ocultas (404) a usuarios finales | — | — | `@WebMvcTest` por rol, `SessionFlowIntegrationTest` |
| 15 | Aislamiento entre usuarios | backend `HomeNetworkService`, `MyAgentController` | C | El dueño sale del token; la red de otro responde 404 | — | — | `MyNetworkControllerTest`, `MyAgentControllerTest` |
| 16 | Secretos fuera del código | 4 repositorios | C | Búsqueda de claves, tokens, URL con contraseña y `.env`: ninguno en los repositorios; `.gitignore` los excluye | — | — | Búsqueda con expresiones regulares (sección 5) |
| 17 | Agente: datos reales y límites honestos | IA `agent/` | P | El agente distingue equipo propio y router; el tipo de los demás es una suposición | La lista muestra el tipo deducido igual que un dato seguro ("Impresora", "Celular o tableta"); no hay un documento que diga qué es seguro y qué es supuesto | La página dice "Parece…" y explica que solo el router sabe con certeza quién está conectado; tabla en el README del agente | `identify.test.tsx` |
| 18 | Agente: autenticación, tiempos y cortes | IA `agent/wifisense_agent.py` | C | Clave por red, solo HTTPS, 30 s de espera, reintento en el siguiente ciclo, respeta `Retry-After`, olvida una clave revocada | — | — | `test_agent.py` |
| 19 | No expulsar ni interferir equipos | agente y página | C | No hay desautenticación ni bloqueo: se guía a cambiar la clave del Wi-Fi en el router | — | — | Revisión del código |
| 20 | Migraciones y esquema | base de datos `migrations/V1…V38` | C | Versionadas, sin cambios a migraciones aplicadas, restricciones e índices probados | — | — | `apply.sh --test`, `actualizacion_test.sh` |
| 21 | Datos simulados identificados | base de datos `seeds/`, backend `DataSourcePolicy` | C | Fuente `SIMULATION` marcada; producción no las permite | — | — | `DataSourcePolicyTest`, `verificar_sensor.sql` |
| 22 | Respaldo y recuperación | base de datos `docs/respaldo.md` | C | Pasos con DBeaver y `pg_dump` | — | — | Restauración probada en local (Avance 17) |
| 23 | Pruebas por repositorio y CI | 4 repositorios `.github/workflows` | C | GitHub Actions en cada Pull Request en los 4 | — | — | Checks en verde |
| 24 | Pruebas de contrato con la IA | backend + IA | N | — | No existían | Ver fila 8 | — |
| 25 | Desplegado en la nube | Railway + Vercel | NV | Configuración revisada: puerto `${PORT}`, salud sin detalles, secretos por variables | Desde esta sesión no hay acceso a Railway ni a Vercel | Pasos manuales en la sección 7 | Manual |
| 26 | Presentación del caso | backend `docs/sustentacion.md` | C | Guion, cifras y preguntas | Falta el patrón nuevo y la auditoría | Actualizar | — |
| 27 | Grupo de máximo 3 | GitHub | C | Commits de Jaider Chindoy, David Campiño y Juan Gallardo | — | — | Historial de GitHub |
| 28 | IA: entradas inválidas y valores extremos | IA `app/schemas*.py`, `app/main.py` | N | Cada campo tiene rangos | Una medición `NaN` (JSON lo permite y Jackson escribe NaN como el texto `"NaN"`) respondía **500** en vez de 422, y una latencia `Infinity` se **aceptaba** (pasa "mayor o igual a 0") y llegaba al modelo; una semilla de entrenamiento negativa también daba 500 | Rechazar NaN e infinito en todo lo que llega (`InputModel`), respuesta 422 aunque el valor no se pueda escribir en JSON, semilla entre 0 y 2³²−1 | `test_invalid_inputs.py` |
| 29 | No duplicar lógica | backend `AgentReportService`, `SensorDeviceService` | P | — | El registro de equipos (nuevo o conocido, fabricante, nombre, tipo, MAC privada, no autorizado, historial) estaba copiado en Mi Wi-Fi y en el sensor | **Template Method** `DeviceRegistry` con `HomeDeviceRegistry` y `TeamDeviceRegistry` | `DeviceRegistryTest` y las pruebas existentes de los dos servicios |
| 30 | Página: errores inesperados | página `App.tsx`, `Layout.tsx` | N | Los errores de las peticiones se muestran en cada página (`DataState`) | Un error al dibujar una página (o una versión nueva publicada mientras la página está abierta) dejaba la pantalla en blanco | **Error Boundary** con mensaje, "Recargar" y "Volver al inicio"; el menú sigue | `errorBoundary.test.tsx` |
| 31 | IA: asesor opcional sin `None` | IA `app/llm_advisor.py`, `app/main.py` | P | Sin clave de Claude se usaban las reglas | El asesor era `None` y cada endpoint lo comprobaba | **Null Object** `RuleAdvisor` | `test_llm_advisor.py`, `test_api.py` |
| 32 | Aislamiento entre usuarios con la base real | backend `integration` | P | Probado con controladores simulados | No había una prueba por HTTP contra PostgreSQL que cambiara el id de la red de otra persona | Prueba de integración con dos usuarios reales | `UserIsolationIntegrationTest` |
| 33 | Borrar una cuenta borra sus datos | base de datos | P | `schema_test.sql` comprobaba redes, clave, reportes y equipos | Faltaban periodos de conexión, alertas, códigos, preferencias y enlaces de recuperación, y nada impedía que una tabla futura guardara datos de una cuenta borrada | Prueba de privacidad con revisión del catálogo | `privacidad_test.sql` |
| 34 | Rutas solo para administrador en la página | página `guards.tsx` | P | `RequireAdmin` existía | Sin prueba | Pruebas por rol de la ruta Usuarios | `guards.test.tsx` |

## 4. Prioridades

| Prioridad | Problema (fila) | Por qué esa prioridad |
|---|---|---|
| P0 | Ninguno | El proyecto compila, todas las pruebas pasan, no hay secretos expuestos ni riesgo de pérdida de datos |
| P1 | IA que no responde bloquea reportes y conexiones (7) | Es un requisito ("funcionamiento controlado si la IA no está disponible") y en Railway pasa en cada redespliegue de la IA |
| P1 | IA abierta si falta la clave en producción (9) | Un descuido de configuración dejaría el servicio de IA aceptando solicitudes de cualquiera |
| P2 | Sin pruebas de contrato backend ↔ IA (8, 24) | La integración es la parte más frágil y no tenía una prueba que la protegiera |
| P2 | Reglas presentadas como IA y tipo de equipo presentado como seguro (3, 17) | Requisito de honestidad del proyecto; no rompe nada, pero exagera lo que el sistema sabe |
| P2 | Build de producción con `localhost` si falta la variable (13) | Un error de configuración pasaría el build y dejaría la página sin backend |
| P2 | IA: NaN responde 500 e infinito se acepta (28) | Error de validación y de robustez; un 500 oculta la causa al backend |
| P2 | Lógica de equipos duplicada (29) | Requisito del enunciado ("no duplicar lógica") y riesgo real: la casa y la sede podrían contar distinto los equipos nuevos |
| P2 | Pantalla en blanco ante un error (30) | Pasa en producción cada vez que Vercel publica una versión con la página abierta |
| P2 | Aislamiento sin prueba contra la base real (32) | Es el control más importante de privacidad |
| P3 | Asesor `None` (31), privacidad al borrar cuenta (33), guardia de administrador (34) | Calidad y pruebas; el comportamiento ya era correcto |
| P3 | `PATTERNS.md` desactualizado, investigación de patrones y diagramas (5, 6) | Documentación académica obligatoria; el código ya era correcto |
| P3 | Guion de sustentación sin el patrón nuevo (26) | Presentación |

## 5. Revisión de secretos (ejecutada)

Búsqueda con expresiones regulares en los 4 repositorios (sin `node_modules`, `dist` ni `.git`) de claves de Claude
(`sk-ant-`), de Brevo (`xkeysib-`), llaves privadas, URL de PostgreSQL con contraseña, claves de agente reales
(`wsa_` + 43 caracteres) y tokens JWT. Resultado: **ninguno**; la única coincidencia es un texto falso a propósito en
`WiFiSense-database/tests/schema_test.sql` que comprueba que la base rechaza una clave en claro. No hay archivos `.env`
y los cuatro `.gitignore` los excluyen. `npm audit` (página) y `pip-audit` (IA): 0 vulnerabilidades conocidas.

## 6. Informe final

| Requisito (fila) | Estado inicial | Cambios implementados | Archivos | Pruebas ejecutadas | Resultado real | Estado final |
|---|---|---|---|---|---|---|
| IA no disponible (7) | P | Proxy `CircuitBreakerAiClient` (2 fallas → abierto 30 s → una llamada de prueba); análisis con IA fuera de la transacción | backend `analysis/CircuitBreakerAiClient.java`, `analysis/AiServiceUnavailableException.java`, `analysis/NetworkAnalysisFacade.java` | `CircuitBreakerAiClientTest` (9), `NetworkAnalysisFacadeTest.theStrategyRunsOutsideAnyTransaction…`, `AiWiringIntegrationTest`; medición con una IA que no responde | Reporte del sensor: 30,6 s / 30,1 s / 30,1 s sin el proxy; 21,3 s / 0,1 s / 0,1 s / 0,1 s con el proxy | C |
| IA abierta sin clave (9) | P | En Railway la IA no arranca sin `AI_API_KEY`; el backend avisa si llama a una IA remota sin clave | IA `app/main.py`, `.env.example`; backend `analysis/HttpAiAnalysisClient.java` | `test_security_config.py` (3), `HttpAiAnalysisClientTest.knowsWhenTheServiceIsLocal…` | Aprobadas | C |
| Contrato backend ↔ IA (8, 24) | N | Ejemplos JSON reales en los dos repositorios y en la página; pruebas en los tres | IA `tests/contracts/*.json`, `tests/test_contract.py`; backend `src/test/resources/contracts/*.json`, `service/AiContractTest.java`; página `tests/contracts/*.json`, `tests/contract.test.ts` | 6 + 6 + 4 pruebas | Aprobadas; además detectan un campo renombrado (probado) | C |
| Reglas presentadas como IA (3) | P | Textos honestos; tabla "Qué es aprendizaje automático y qué son reglas" en la página y en el README de la IA | página `ConnectionPanel.tsx`, `DashboardPage.tsx`, `AgentPanel.tsx`, `AiModelPage.tsx`; IA `README.md` | `aiModel.test.tsx`, `myNetworks.test.tsx` | Aprobadas | C |
| Identidad de equipos supuesta (17) | P | "Parece…" para el tipo deducido y explicación en la lista; tabla de certeza en el README del agente | página `AgentPanel.tsx`; IA `agent/README.md` | `identify.test.tsx` | Aprobadas | C |
| URL de producción (13) | P | El build de Vercel se detiene si falta `VITE_API_URL` | página `vite.config.ts` | `buildConfig.test.ts`; build real con `VERCEL=1` sin y con la variable | Sin variable: se detiene con el mensaje; con variable: build correcto | C |
| Documentación de patrones (5, 6) | P | `PATTERNS.md`: 9 patrones con ejemplo y prueba, diagrama de clases, investigación de 11 patrones descartados; Adapter corregido | backend `PATTERNS.md`, `DataSourceCreatorTemplateTest.java` | `DataSourceCreatorTemplateTest` (2) | Aprobadas | C |
| Arquitectura y variables | P | `docs/arquitectura.md` con diagrama, conexiones y variables sin valores | backend `docs/arquitectura.md` | Revisión | — | C |
| Acceso a la base | P | Tabla de quién se conecta y con qué usuario; se quitó una afirmación que no se podía comprobar | base de datos `docs/seguridad.md`, `README.md` | Revisión | — | C |
| Presentación (26) | P | Guion con los 9 patrones, la medición y preguntas nuevas | backend `docs/sustentacion.md` | Revisión | — | C |
| Despliegue en la nube (25) | NV | Sin cambios de infraestructura | — | No se pudo: sin acceso a Railway ni a Vercel desde esta sesión | — | NV (pasos en la sección 8) |
| Entradas inválidas en la IA (28) | N | `InputModel` sin NaN ni infinito, respuesta 422 siempre escribible, semilla acotada | IA `app/schemas.py`, `schemas_channel.py`, `schemas_quality.py`, `schemas_security.py`, `app/main.py` | `test_invalid_inputs.py` (13) | Aprobadas; antes: NaN → 500 e `Infinity` → 200 | C |
| Lógica duplicada de equipos (29) | P | Template Method `DeviceRegistry` + interfaz `TrackedDevice` | backend `service/DeviceRegistry.java`, `HomeDeviceRegistry.java`, `TeamDeviceRegistry.java`, `model/TrackedDevice.java`, `AgentDevice.java`, `SensorDevice.java`, `AgentReportService.java`, `SensorDeviceService.java`, `SensorReportService.java` | `DeviceRegistryTest` (3) y las existentes de los dos servicios | Aprobadas | C |
| Pantalla en blanco (30) | N | `ErrorBoundary` en `App.tsx` y `Layout.tsx` | página `components/ErrorBoundary.tsx`, `App.tsx`, `Layout.tsx`, `index.css` | `errorBoundary.test.tsx` (4) | Aprobadas | C |
| Asesor opcional (31) | P | Null Object `RuleAdvisor` | IA `app/llm_advisor.py`, `app/main.py` | `test_llm_advisor.py`, `test_api.py` | Aprobadas | C |
| Aislamiento con la base real (32) | P | Prueba de integración | backend `integration/UserIsolationIntegrationTest.java` | 2 pruebas contra PostgreSQL | Aprobadas | C |
| Borrar cuenta (33) | P | Prueba de privacidad | base de datos `tests/privacidad_test.sql` | 7 comprobaciones | Aprobadas | C |
| Guardia de administrador (34) | P | Pruebas por rol | página `tests/guards.test.tsx` | 4 pruebas nuevas | Aprobadas | C |
| Patrones nuevos documentados | — | Template Method (2 usos) en `PATTERNS.md`; `docs/patrones.md` en la IA (Null Object, Strategy, Pipeline, Middleware) y en la página (Error Boundary, Provider, guardias, Custom Hook, Single-flight); Bulkhead analizado y descartado con datos | backend `PATTERNS.md`; IA `docs/patrones.md`; página `docs/patrones.md` | Revisión | — | C |

### Pruebas al terminar (ejecutadas)

| Repositorio | Antes | Después |
|---|---|---|
| Backend | 351 (12 de integración) | **376** (15 de integración contra PostgreSQL 16 como `wifisense_app`), 0 fallas |
| IA | 239 + 2 omitidas | **261** + 2 omitidas (solo Windows), 0 fallas; `pip-audit` sin vulnerabilidades |
| Página | 133 | **148**, 0 fallas; tipos sin errores; build correcto; `npm audit` sin vulnerabilidades |
| Base de datos | 166 comprobaciones | **173** (sin cambios de esquema; no hizo falta ninguna migración) |

Además, cada commit del plan de entrega se reprodujo uno por uno sobre `main` de GitHub y se compiló y probó después
de cada uno (backend, IA y página), y el backend se probó con la base de datos de cada parte.

## 7. Cómo probarlo en local

```bash
# Base de datos (PostgreSQL 16)
cd WiFiSense-database && DATABASE_URL=postgresql:///wifisense bash apply.sh --seed --security --test
# IA
cd WiFiSense-ai && pip install -r requirements.txt && python -m pytest -q && uvicorn app.main:app --port 8000
# Backend (Java 21; variables como en .env.example, sin valores reales en archivos)
cd WiFiSense-backend && mvn verify && mvn spring-boot:run
# Página
cd WiFiSense-frontend && npm ci && npm test && npm run dev
```

Para ver el Circuit Breaker: con el backend corriendo, apaga la IA (o apunta `AI_SERVICE_URL` a un puerto que acepte
conexiones y no responda) y envía reportes del agente: el log dice `circuit open` y los reportes dejan de esperar;
al volver la IA, `circuit closed`.

## 8. Pasos manuales para verificar producción

1. Railway → servicio de IA → **Variables**: `AI_API_KEY` existe y es **igual** a la del backend. Hacerlo **antes**
   de sincronizar el fork de la IA: sin ella, la versión nueva no arranca (a propósito).
2. Vercel → **Settings → Environment Variables**: `VITE_API_URL` existe (la URL pública del backend).
3. Sincronizar los forks en orden IA → backend → página y esperar *Active* / *Ready*; en los Deploy Logs ninguna
   línea `ERROR`.
4. `https://<backend>/actuator/health` responde `{"status":"UP"…}` sin detalles; en la página, Panel → Estado del
   sistema dice "Servicio de IA: Disponible".
5. Enviar un reporte con el agente (o esperar 5 minutos) y verlo en Mi Wi-Fi o en el panel del sensor.
6. Opcional: en Railway apagar la IA un momento y comprobar que los reportes siguen llegando sin demoras (sin
   veredictos) y que, al encenderla, vuelven los veredictos.

## 9. Pendientes y limitaciones reales

- **SNMP y API del router:** el diseño las tiene (`SnmpDataSourceCreator`, `RouterApiDataSourceCreator`), pero los
  routers de la casa y de la sede no las ofrecen; por eso ningún equipo está "confirmado por el router".
- **Fallas reales etiquetadas:** la IA se evalúa con fallas simuladas sobre mediciones reales.
- **Producción no verificada desde aquí:** sección 8.
- **Agente solo para Windows** como instalador; en Linux corre el script de Python.
- **Contrato duplicado:** los ejemplos JSON están copiados en tres repositorios; si cambia el contrato hay que
  actualizarlos en los tres (las pruebas de cada lado lo exigen).
