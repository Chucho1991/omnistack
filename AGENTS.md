# AGENTS.md

## Alcance
Este archivo define las reglas estándar de implementación para futuras integraciones y evolutivos del backend OMNISTACK.

## Principios generales
- Mantener una arquitectura limpia y hexagonal.
- Priorizar bajo acoplamiento, alta cohesión y separación clara de responsabilidades.
- Evitar lógica de negocio en controllers, configs o adapters de entrada.
- Diseñar el código para crecer por nuevas líneas de negocio, nuevos proveedores y nuevas capacidades sin reescribir el núcleo.

## Arquitectura obligatoria
- Respetar la separación por capas: `controller`, `application`, `domain`, `infrastructure`, `config`, `shared`.
- Dentro del enfoque hexagonal, separar contratos de implementaciones:
  - `application/port/in`
  - `application/port/out`
  - `infrastructure/adapter/...`
- `domain` no debe depender de Spring, JPA, clientes HTTP ni detalles de infraestructura.
- No usar entidades JPA dentro de `domain`; cualquier persistencia futura debe mapearse explícitamente.
- Los controllers solo exponen endpoints, validan entrada, documentan contrato y delegan al caso de uso.
- Los casos de uso viven en `application/service` y orquestan el flujo funcional.
- Las integraciones externas deben resolverse mediante estrategias y puertos, no con `if/else` gigantes en controllers.

## Estándar para nuevas integraciones
- Toda nueva integración debe entrar por puertos y adapters.
- Modelar contratos desacoplados para:
  - request interno
  - response interno
  - request externo
  - response externo
  - errores externos
- Resolver el flujo por estrategia/factory cuando aplique:
  - `ProviderFlowResolver`
  - `TransactionFlowStrategy`
  - `ExternalProviderClient`
  - estrategias por capacidad
- No incrustar reglas específicas de proveedor en controllers.
- Toda nueva integración debe quedar preparada para mocking y pruebas unitarias.
- Si una integración requiere configuración dinámica, esta debe abstraerse por puerto; no acoplar la lógica del negocio a la fuente de configuración.

## Controllers y endpoints
- Exponer endpoints REST consistentes en nombres, códigos HTTP y formato de respuesta.
- Mantener contratos request/response explícitos con DTOs.
- Aplicar `jakarta.validation` en todos los DTOs de entrada.
- Toda inclusión o modificación de endpoint debe venir acompañada de:
  - documentación Swagger/OpenAPI
  - pruebas unitarias
  - actualización de `README.md`
  - actualización de la colección Postman y su environment si el contrato expuesto cambió
- Los endpoints expuestos deben quedar siempre documentados en Swagger/OpenAPI.

## Documentación obligatoria
- `README.md` debe mantenerse siempre actualizado cuando cambie:
  - arquitectura
  - endpoints
  - contratos API
  - configuración
  - integraciones
  - forma de ejecución
- `AGENTS.md` debe mantenerse siempre actualizado cuando cambien las reglas, estándares o convenciones de implementación del proyecto.
- La colección Postman y sus environments versionados en el repositorio deben mantenerse siempre actualizados cuando cambie:
  - un endpoint expuesto
  - el contrato request/response
  - headers requeridos
  - variables necesarias para pruebas manuales
- Documentar clases, DTOs y métodos públicos con JavaDoc.
- Incluir `@param`, `@return` y `@throws` cuando aplique.
- Mantener JavaDocs breves, técnicos y claros.
- No dejar endpoints nuevos sin documentación funcional ni técnica.

## Validaciones y manejo de errores
- Validar entradas con anotaciones Bean Validation.
- Mantener manejo uniforme de errores mediante excepciones específicas y handler global.
- Conservar un formato de error consistente para toda la API.
- No propagar errores técnicos crudos al consumidor final.

## Seguridad
- Todo endpoint nuevo o modificado debe evaluar seguridad por rol o mecanismo equivalente antes de considerarse terminado.
- No dejar endpoints productivos expuestos sin criterio de autorización definido.
- Si la seguridad no se implementa en una fase inicial, debe quedar explícitamente documentado como pendiente técnico.

## Persistencia y configuración
- No mezclar lógica de persistencia con lógica de negocio.
- Toda futura integración con base de datos debe entrar por puertos de salida y adapters de persistencia.
- Evitar hardcodear configuraciones operativas dentro del código.
- Las propiedades de aplicación deben mantenerse ordenadas y por perfil cuando corresponda.

## Logging y observabilidad
- Mantener logging transversal y estructurado.
- Usar correlation id por request cuando aplique.
- Registrar eventos funcionales y errores relevantes sin exponer datos sensibles.
- Toda integración crítica debe contemplar trazabilidad suficiente para soporte y auditoría.

## Testing obligatorio
- Generar pruebas unitarias para todo endpoint nuevo.
- Si un endpoint existente cambia, ajustar sus pruebas.
- Cubrir al menos:
  - caso exitoso
  - validaciones
  - errores de negocio
  - errores de integración cuando aplique
- Probar resolvers, estrategias y casos de uso cuando intervengan en el flujo.

## Convenciones de implementación
- Usar nombres expresivos y consistentes con el negocio.
- Evitar clases gigantes y responsabilidades mezcladas.
- Reutilizar mappers, factories y utilitarios antes de duplicar lógica.
- Dejar `TODO` solo cuando exista una razón real y con contexto suficiente.
- Mantener el código preparado para escalar sin romper contratos existentes.

## Flujo mínimo para cambios futuros
1. Definir el caso de uso y sus puertos de entrada/salida.
2. Crear o ajustar DTOs, validaciones y modelos necesarios.
3. Implementar adapters de entrada y salida respetando la arquitectura.
4. Documentar endpoints en Swagger/OpenAPI.
5. Agregar o actualizar JavaDocs.
6. Actualizar `README.md`.
7. Actualizar la colección Postman y el environment si cambió el contrato expuesto.
8. Crear o ajustar pruebas unitarias.
9. Verificar que el cambio conserve coherencia arquitectónica.

## Checklist de cierre
- [ ] Arquitectura respetada.
- [ ] Casos de uso y puertos definidos correctamente.
- [ ] DTOs y validaciones implementados.
- [ ] Endpoints documentados en Swagger/OpenAPI.
- [ ] JavaDocs completos en clases y métodos públicos relevantes.
- [ ] `README.md` actualizado.
- [ ] `AGENTS.md` actualizado si cambiaron reglas o estándares.
- [ ] Colección Postman y environment actualizados si cambió el contrato expuesto.
- [ ] Manejo de errores consistente.
- [ ] Seguridad evaluada/aplicada.
- [ ] Pruebas unitarias creadas o ajustadas.
