/* ============================================================
   modo-estatico.js — Solo para la versión publicada en GitHub Pages.
   Allí no hay servidor ni base de datos: no hay cuentas, pedidos en
   línea ni pagos de planes. La tienda ya lo detecta (Cuenta.disponible()
   es falso) y ofrece WhatsApp; aquí solo se ajustan textos.
   ============================================================ */
(function () {
  // Registro e inicio de sesión: explicar por qué no se puede crear la cuenta aquí
  const panelCuenta = document.querySelector('.auth-sub');
  if (panelCuenta) {
    const aviso = document.createElement('div');
    aviso.className = 'alerta alerta--info aviso-estatico';
    aviso.textContent = 'Esta es una vista previa del catálogo. Las cuentas y los pedidos en línea funcionan en la página oficial de la droguería; mientras tanto, pide por WhatsApp.';
    panelCuenta.insertAdjacentElement('afterend', aviso);
  }

  // Suscripciones: las respuestas hablan de WhatsApp en lugar de pagos en línea
  if (document.querySelector('.planes-grid')) {
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
