# FinMind — Frontend

Cliente web en React que consume la API REST de FinMind.

Responsable: **Luis Miguel Méndez** (desarrollo frontend).

## Puesta en marcha

```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

Abre en `http://localhost:5173`. **El backend debe estar corriendo en el puerto 8080**,
porque su configuración de CORS solo permite ese origen. Si el backend no responde,
la aplicación muestra un aviso de conexión en lugar de fallar en silencio.

## Qué está implementado

| Pantalla | Historia / Requisito | Endpoint |
|---|---|---|
| `UI-001` Iniciar sesión | HU-002 / RF-002 | `POST /auth/login` |
| `UI-002` Crear cuenta | HU-001 / RF-001 | `POST /auth/registro` |
| `UI-003` Panel (estructura) | HU-003 / RF-003 | `GET /usuarios/me` |

## Cómo está organizado

```
src/
├── api/cliente.js        único punto que habla con la API
├── auth/                 contexto de sesión y ruta protegida
├── componentes/          Boton, Campo y Alerta reutilizables
├── paginas/              una por pantalla del inventario UI
└── estilos/
    ├── tokens.css        el sistema de diseño
    └── app.css           los estilos, siempre a partir de tokens
```

## Tres decisiones que hay que poder sustentar

**1. Los colores no se escriben en los componentes.** Todos salen de `tokens.css`, que
contiene los mismos valores del documento de diseño UX/UI con sus ratios de contraste ya
verificados contra WCAG AA. Si el diseño cambia un color, cambia en un solo archivo.

**2. Ocultar rutas en el cliente no es seguridad.** `RutaProtegida` solo evita que el
usuario navegue a una pantalla que no le sirve. Quien rechaza de verdad una petición sin
token válido es el backend. Un control que viva solo en el frontend es una falla crítica.

**3. El token se guarda en `sessionStorage`, no en `localStorage`.** Así desaparece al
cerrar la pestaña, lo que reduce la ventana de exposición si alguien queda con la sesión
abierta en un equipo compartido.

## Estados de interfaz cubiertos

Carga, éxito, error de validación por campo, error general del servidor, sin conexión y
vacío. Cada estado se comunica con **icono y texto además de color**, porque el color por
sí solo excluye a personas con baja visión cromática.

## Pendiente

Movimientos, presupuestos y el panel con datos reales, cuando el backend exponga
`RF-012` a `RF-022`.
