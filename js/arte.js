/* ============================================================
   arte.js — Ilustraciones vectoriales propias del catálogo.
   Sustituyen a las fotos mientras no existan. Cuando tengas las fotos,
   colócalas en /img/productos/ con el nombre del campo `imagen` del
   catálogo y cambia FOTOS_DISPONIBLES a true.
   ============================================================ */
window.Arte = (function () {
  const FOTOS_DISPONIBLES = false;
  const TONOS = {
    analgesicos: ['#244CDB', '#4778F5'], antiinflamatorios: ['#1F3F9E', '#4778F5'],
    antibioticos: ['#8E2338', '#C2455F'], antihistaminicos: ['#1B7459', '#269B78'], otros: ['#101B35', '#3B4F86']
  };

  function capsula(a, b) {
    return `<g transform="rotate(-32 100 80)">
      <rect x="44" y="58" width="112" height="44" rx="22" fill="${a}"/>
      <path d="M100 58h34a22 22 0 0 1 0 44h-34z" fill="${b}"/>
      <rect x="56" y="66" width="36" height="6" rx="3" fill="#fff" opacity=".45"/>
    </g>`;
  }

  function blister(a) {
    let pastillas = '';
    for (let f = 0; f < 2; f++) for (let c = 0; c < 4; c++) {
      pastillas += `<circle cx="${61 + c * 26}" cy="${62 + f * 34}" r="10.5" fill="#fff"/><circle cx="${58 + c * 26}" cy="${59 + f * 34}" r="3" fill="${a}" opacity=".25"/>`;
    }
    return `<g transform="rotate(-8 100 80)">
      <rect x="40" y="40" width="120" height="80" rx="11" fill="${a}"/>
      <rect x="40" y="40" width="120" height="80" rx="11" fill="#fff" opacity=".1"/>${pastillas}
    </g>`;
  }

  function caja(a, b) {
    return `<path d="M58 44 100 26l44 18v76l-44 22-42-22z" fill="${a}"/>
      <path d="M100 62v80l44-22V44z" fill="${b}"/>
      <path d="M58 44l42 18 44-18-44-18z" fill="#fff" opacity=".28"/>
      <rect x="66" y="82" width="26" height="4" rx="2" fill="#fff" opacity=".85" transform="skewY(23 79 84)"/>
      <rect x="66" y="92" width="18" height="4" rx="2" fill="#fff" opacity=".55" transform="skewY(23 75 94)"/>`;
  }

  function envolver(contenido) {
    return `<svg viewBox="0 0 200 160" role="presentation" aria-hidden="true">${contenido}</svg>`;
  }

  function medicamento(med, presentacion) {
    const [a, b] = TONOS[med.cat] || TONOS.otros;
    if (FOTOS_DISPONIBLES && med.imagen) return `<img src="/DrogueriaYvariedadesLosTresHermanos/img/productos/${Seguridad.esc(med.imagen)}" alt="" loading="lazy">`;
    const tipo = (presentacion && presentacion.tipo) || 'Unidad';
    if (tipo.startsWith('Caja')) return envolver(caja(a, b));
    if (tipo.startsWith('Tableta')) return envolver(blister(a));
    return envolver(capsula(a, b));
  }

  const FORMAS = {
    101: () => perfume('#8E2338', '#C2455F'),
    102: () => perfume('#244CDB', '#7FA2FF'),
    103: () => perfume('#101B35', '#C2455F'),
    104: () => `<g transform="rotate(-6 100 80)"><rect x="56" y="28" width="92" height="108" rx="7" fill="#101B35"/><rect x="56" y="28" width="16" height="108" rx="7" fill="#8E2338"/><rect x="84" y="50" width="48" height="20" rx="3" fill="#fff"/><path d="M90 57h36M90 63h22" stroke="#667085" stroke-width="2.500"/></g>`,
    105: () => [0, 1, 2].map(i => `<g transform="rotate(${-26 + i * 26} 100 130)"><rect x="93" y="30" width="14" height="94" rx="4" fill="${['#244CDB', '#101B35', '#8E2338'][i]}"/><path d="M93 124h14l-7 14z" fill="#E4E9F0"/><rect x="93" y="30" width="14" height="14" rx="4" fill="#fff" opacity=".35"/></g>`).join(''),
    106: () => `<g transform="rotate(-14 100 80)"><rect x="46" y="52" width="108" height="56" rx="11" fill="#FAFBFD" stroke="#CCD4E0" stroke-width="2"/><path d="M46 63a11 11 0 0 1 11-11h43v56H57a11 11 0 0 1-11-11z" fill="#244CDB"/></g>`,
    107: () => ['#8E2338', '#E5A23C', '#269B78', '#4778F5', '#244CDB', '#101B35'].map((c, i) => `<g transform="translate(${38 + i * 22} ${i % 2 ? 26 : 18})"><path d="M0 22 7 4l7 18z" fill="#E9D8BE"/><rect y="22" width="14" height="96" fill="${c}"/><path d="M4.500 10 7 4l2.500 6z" fill="${c}"/></g>`).join(''),
    108: () => `<rect x="74" y="58" width="52" height="84" rx="10" fill="#244CDB"/><rect x="78" y="22" width="44" height="38" rx="15" fill="#E4E9F0"/><rect x="84" y="82" width="32" height="30" rx="4" fill="#fff" opacity=".9"/><path d="M90 92h20M90 100h13" stroke="#244CDB" stroke-width="2.500"/>`,
    109: () => `<rect x="86" y="18" width="28" height="20" rx="4" fill="#8E2338"/><path d="M90 38h20l14 20v76a8 8 0 0 1-8 8H84a8 8 0 0 1-8-8V58z" fill="#DCE6FF"/><rect x="76" y="76" width="48" height="42" fill="#fff"/><path d="M100 86v22M89 97h22" stroke="#8E2338" stroke-width="5" stroke-linecap="round"/>`,
    110: () => `<g transform="rotate(38 100 80)"><rect x="89" y="4" width="22" height="118" rx="11" fill="#FAFBFD" stroke="#CCD4E0" stroke-width="2"/><rect x="93" y="28" width="14" height="30" rx="3" fill="#101B35"/><rect x="95" y="118" width="10" height="28" rx="5" fill="#667085"/><circle cx="100" cy="82" r="5" fill="#244CDB"/></g>`,
    111: () => `<path d="M58 54c28-12 56-12 84 0v50c-28 24-56 24-84 0z" fill="#DCE6FF"/><path d="M58 70c28-10 56-10 84 0M58 86c28 8 56 8 84 0" stroke="#fff" stroke-width="3" fill="none"/><path d="M58 62C40 56 32 72 40 88c5 9 12 12 18 12M142 62c18-6 26 10 18 26-5 9-12 12-18 12" fill="none" stroke="#667085" stroke-width="3"/>`
  };

  function perfume(a, b) {
    return `<rect x="88" y="16" width="24" height="24" rx="5" fill="#101B35"/><rect x="93" y="38" width="14" height="12" fill="#CCD4E0"/>
      <path d="M72 62a12 12 0 0 1 12-12h32a12 12 0 0 1 12 12v68a12 12 0 0 1-12 12H84a12 12 0 0 1-12-12z" fill="${a}"/>
      <path d="M100 50h16a12 12 0 0 1 12 12v68a12 12 0 0 1-12 12h-16z" fill="${b}" opacity=".85"/>
      <rect x="82" y="60" width="6" height="58" rx="3" fill="#fff" opacity=".35"/>`;
  }

  function variedad(v) {
    if (FOTOS_DISPONIBLES && v.imagen) return `<img src="/DrogueriaYvariedadesLosTresHermanos/img/productos/${Seguridad.esc(v.imagen)}" alt="" loading="lazy">`;
    return envolver((FORMAS[v.id] || FORMAS[106])());
  }

  return { medicamento, variedad };
})();
