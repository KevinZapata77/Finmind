import { useEffect, useRef } from 'react'

const CLAVE_SITIO = import.meta.env.VITE_CAPTCHA_SITE_KEY

/**
 * Widget de CAPTCHA (Cloudflare Turnstile).
 *
 * Solo obtiene un token y lo entrega hacia arriba. Quien decide si es válido
 * es el backend: comprobarlo aquí no sería seguridad, porque cualquiera puede
 * llamar al endpoint sin pasar por esta pantalla.
 *
 * Si no hay clave configurada, no se muestra nada y el registro sigue
 * funcionando: en desarrollo el backend también lo tiene deshabilitado.
 */
export default function Captcha({ onToken }) {
  const contenedor = useRef(null)

  useEffect(() => {
    if (!CLAVE_SITIO) {
      onToken('')
      return
    }

    let idWidget
    const render = () => {
      if (!window.turnstile || !contenedor.current) return
      idWidget = window.turnstile.render(contenedor.current, {
        sitekey: CLAVE_SITIO,
        language: 'es',
        callback: (token) => onToken(token),
        'expired-callback': () => onToken(''),
        'error-callback': () => onToken(''),
      })
    }

    if (window.turnstile) {
      render()
    } else {
      const script = document.createElement('script')
      script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
      script.async = true
      script.onload = render
      document.head.appendChild(script)
    }

    return () => {
      if (idWidget && window.turnstile) window.turnstile.remove(idWidget)
    }
  }, [onToken])

  if (!CLAVE_SITIO) return null
  return <div className="captcha" ref={contenedor} aria-label="Verificación de seguridad" />
}
