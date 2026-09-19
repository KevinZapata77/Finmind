import { useEffect, useRef, useState } from 'react'

const CLAVE_SITIO = import.meta.env.VITE_CAPTCHA_SITE_KEY
const URL_SCRIPT = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

/*
  El script se carga UNA vez para toda la aplicacion, no una por montaje.

  Antes cada montaje del componente añadia otro <script> si window.turnstile
  todavia no existia. Con React en modo estricto el efecto corre dos veces en
  desarrollo, asi que se inyectaban dos scripts que competian por el mismo
  contenedor. Esta promesa compartida hace que el segundo montaje espere al
  primero en vez de empezar de nuevo.
*/
let cargando = null
function cargarTurnstile() {
  if (window.turnstile) return Promise.resolve()
  if (cargando) return cargando
  cargando = new Promise((resolver, rechazar) => {
    const script = document.createElement('script')
    script.src = URL_SCRIPT
    script.async = true
    script.onload = () => resolver()
    script.onerror = () => { cargando = null; rechazar(new Error('no se pudo cargar')) }
    document.head.appendChild(script)
  })
  return cargando
}

/**
 * Widget de CAPTCHA (Cloudflare Turnstile).
 *
 * Solo obtiene un token y lo entrega hacia arriba. Quien decide si es válido es
 * el backend: comprobarlo aquí no sería seguridad, porque cualquiera puede
 * llamar al endpoint sin pasar por esta pantalla. La verificación real vive en
 * ServicioCaptcha, contra el proveedor y con la clave secreta.
 *
 * Si no hay clave configurada no se muestra nada y el registro sigue
 * funcionando: en desarrollo el backend también lo tiene deshabilitado.
 *
 * POR QUE AHORA MUESTRA ERRORES
 * Antes, si el widget no llegaba a dibujarse, no pasaba nada visible: quedaba
 * un hueco en blanco y el botón "Crear cuenta" deshabilitado, sin ninguna
 * explicación. El usuario no podía registrarse y no había forma de saber por
 * qué. Eso ocurrió de verdad — el 16/09 la pantalla de registro salió sin
 * widget en las capturas del manual.
 *
 * La causa más frecuente es que el dominio desde el que se abre la aplicación
 * no esté dado de alta en el panel de Turnstile. Las claves de prueba aceptan
 * cualquier dominio; las reales, solo los que estén en la lista. Eso vuelve a
 * morder al desplegar, porque el dominio de producción también hay que
 * agregarlo.
 *
 * Un control de seguridad que falla en silencio es peor que no tenerlo: nadie
 * se entera de que está roto.
 */
export default function Captcha({ onToken }) {
  const contenedor = useRef(null)
  const [estado, setEstado] = useState('cargando')

  useEffect(() => {
    if (!CLAVE_SITIO) {
      onToken('')
      return
    }

    let idWidget
    let vivo = true

    cargarTurnstile()
      .then(() => {
        if (!vivo || !contenedor.current || !window.turnstile) return
        idWidget = window.turnstile.render(contenedor.current, {
          sitekey: CLAVE_SITIO,
          language: 'es',
          callback: (token) => { onToken(token); setEstado('listo') },
          'expired-callback': () => { onToken(''); setEstado('vencido') },
          'error-callback': () => { onToken(''); setEstado('error') },
        })
        setEstado('listo')
      })
      .catch(() => { if (vivo) { onToken(''); setEstado('error') } })

    return () => {
      vivo = false
      if (idWidget && window.turnstile) window.turnstile.remove(idWidget)
    }
  }, [onToken])

  if (!CLAVE_SITIO) return null

  return (
    <div className="captcha">
      <div ref={contenedor} aria-label="Verificación de seguridad" />

      {estado === 'error' && (
        <p className="campo__error" role="alert">
          No pudimos cargar la verificación de seguridad. Revisa tu conexión y
          recarga la página. Si el problema sigue, el dominio de esta página no
          está habilitado en el panel del proveedor.
        </p>
      )}

      {estado === 'vencido' && (
        <p className="campo__error" role="alert">
          La verificación caducó. Vuelve a marcarla para continuar.
        </p>
      )}
    </div>
  )
}
