/* ============================================================
   modo-estatico.js — Solo para la versión publicada en GitHub Pages.
   Allí no hay servidor ni panel de administración: nadie podría revisar
   un comprobante de pago, así que las suscripciones se coordinan por
   WhatsApp y las cuentas se guardan únicamente en este navegador.
   ============================================================ */
(function () {
  const WHATSAPP = 'https://wa.me/573225065554';

  // Registro e inicio de sesión: aviso de dónde queda guardada la cuenta
  const panelCuenta = document.querySelector('.auth-sub');
  if (panelCuenta) {
    const aviso = document.createElement('div');
    aviso.className = 'alerta alerta--info aviso-estatico';
    aviso.textContent = 'Tu cuenta y tus pedidos se guardan solo en este navegador. No estarán disponibles en otros dispositivos.';
    panelCuenta.insertAdjacentElement('afterend', aviso);
  }

  // Suscripciones: la solicitud va directo a la droguería por WhatsApp
  if (document.querySelector('.planes-grid')) {
    window.seleccionarPlan = function (nombre, descuento, precio) {
      const plan = Seguridad.planCanonico(nombre);
      if (!plan) return;
      const mensaje = `Hola, quiero suscribirme al plan ${plan.nombre} ` +
        `(${plan.descuento}% de descuento, $${plan.precio.toLocaleString('es-CO')} al mes). ¿Cómo puedo pagarlo?`;
      window.open(`${WHATSAPP}?text=${encodeURIComponent(mensaje)}`, '_blank', 'noopener');
    };

    document.querySelectorAll('.btn-plan').forEach(b => { b.textContent = 'Solicitar por WhatsApp'; });

    const descripcion = document.querySelector('.sus-hero .hero-desc');
    if (descripcion) descripcion.textContent = 'Elige tu plan y coordínalo con la droguería por WhatsApp.';

    const respuestas = [
      'Escríbenos por WhatsApp con el plan que elegiste. La droguería te confirma cómo activarlo y cómo se aplica tu descuento.',
      'Sí. Escríbenos por WhatsApp o acércate a la droguería para cancelarla.',
      'Sí, el descuento de tu plan aplica a todos los medicamentos y variedades de la tienda.',
      'Te indicamos las opciones de pago por WhatsApp. También puedes pagar directamente en la droguería.'
    ];
    document.querySelectorAll('.faq-respuesta > div').forEach((el, i) => {
      if (respuestas[i]) el.textContent = respuestas[i];
    });
  }
})();
