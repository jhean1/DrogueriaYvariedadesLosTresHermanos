// ═══════════════════════════════════════════════════════════
// CATÁLOGO DE MEDICAMENTOS — PRECIOS REALES MERCADO COLOMBIANO 2024
// ⚠️ PREPARADO PARA BD: reemplazar con fetch('/api/medicamentos')
// Estructura: cada presentación tiene tipo + concentración + marca + precio
// ═══════════════════════════════════════════════════════════

const MEDICAMENTOS = [
  {
    id: 1, nombre: 'Acetaminofén', cat: 'analgesicos',
    desc: 'Analgésico y antipirético para dolor y fiebre.',
    imagen: 'acetaminofen.jfif',
    presentaciones: [
      { id:'1-a1', tipo:'Unidad',     conc:'500mg',  marca:'Genérico', precio:150  },
      { id:'1-a2', tipo:'Unidad',     conc:'500mg',  marca:'Genfar',   precio:200  },
      { id:'1-a3', tipo:'Unidad',     conc:'500mg',  marca:'MK',       precio:220  },
      { id:'1-b1', tipo:'Tableta x10',conc:'500mg',  marca:'Genérico', precio:1200 },
      { id:'1-b2', tipo:'Tableta x10',conc:'500mg',  marca:'Genfar',   precio:1800 },
      { id:'1-b3', tipo:'Tableta x10',conc:'500mg',  marca:'MK',       precio:1900 },
      { id:'1-b4', tipo:'Tableta x10',conc:'500mg',  marca:'Dolex',    precio:3000 },
      { id:'1-c1', tipo:'Caja x20',   conc:'500mg',  marca:'Genérico', precio:2200 },
      { id:'1-c2', tipo:'Caja x20',   conc:'500mg',  marca:'Genfar',   precio:3200 },
      { id:'1-c3', tipo:'Caja x20',   conc:'500mg',  marca:'Dolex',    precio:5500 },
      { id:'1-d1', tipo:'Tableta x10',conc:'1000mg', marca:'Genérico', precio:2000 },
      { id:'1-d2', tipo:'Caja x20',   conc:'1000mg', marca:'Genérico', precio:3500 },
    ]
  },
  {
    id: 2, nombre: 'Ibuprofeno', cat: 'antiinflamatorios',
    desc: 'Antiinflamatorio, analgésico y antipirético.',
    imagen: 'ibuprofeno.jfif',
    presentaciones: [
      { id:'2-a1', tipo:'Unidad',     conc:'200mg',  marca:'Genérico', precio:200  },
      { id:'2-a2', tipo:'Unidad',     conc:'200mg',  marca:'Genfar',   precio:280  },
      { id:'2-b1', tipo:'Tableta x10',conc:'200mg',  marca:'Genérico', precio:1800 },
      { id:'2-b2', tipo:'Tableta x10',conc:'200mg',  marca:'Genfar',   precio:2400 },
      { id:'2-b3', tipo:'Tableta x10',conc:'400mg',  marca:'Genérico', precio:2500 },
      { id:'2-b4', tipo:'Tableta x10',conc:'400mg',  marca:'Genfar',   precio:3200 },
      { id:'2-b5', tipo:'Tableta x10',conc:'400mg',  marca:'MK',       precio:3000 },
      { id:'2-b6', tipo:'Tableta x10',conc:'400mg',  marca:'Advil',    precio:4500 },
      { id:'2-b7', tipo:'Tableta x10',conc:'600mg',  marca:'Genérico', precio:3500 },
      { id:'2-c1', tipo:'Caja x20',   conc:'400mg',  marca:'Genérico', precio:4500 },
      { id:'2-c2', tipo:'Caja x20',   conc:'400mg',  marca:'Genfar',   precio:6000 },
    ]
  },
  {
    id: 3, nombre: 'Amoxicilina', cat: 'antibioticos',
    desc: 'Antibiótico de amplio espectro para infecciones bacterianas.',
    imagen: 'amoxicilina.jfif',
    presentaciones: [
      { id:'3-a1', tipo:'Unidad',     conc:'250mg',  marca:'Genérico', precio:400  },
      { id:'3-a2', tipo:'Unidad',     conc:'500mg',  marca:'Genérico', precio:600  },
      { id:'3-b1', tipo:'Tableta x7', conc:'250mg',  marca:'Genérico', precio:2500 },
      { id:'3-b2', tipo:'Tableta x7', conc:'250mg',  marca:'Genfar',   precio:3500 },
      { id:'3-b3', tipo:'Tableta x7', conc:'500mg',  marca:'Genérico', precio:3800 },
      { id:'3-b4', tipo:'Tableta x7', conc:'500mg',  marca:'Genfar',   precio:5000 },
      { id:'3-b5', tipo:'Tableta x7', conc:'500mg',  marca:'Amoxal',   precio:6500 },
      { id:'3-b6', tipo:'Tableta x7', conc:'875mg',  marca:'Genérico', precio:6000 },
      { id:'3-c1', tipo:'Caja x21',   conc:'500mg',  marca:'Genérico', precio:10500},
      { id:'3-c2', tipo:'Caja x21',   conc:'500mg',  marca:'Genfar',   precio:14000},
    ]
  },
  {
    id: 4, nombre: 'Omeprazol', cat: 'otros',
    desc: 'Inhibidor de la bomba de protones para gastritis y reflujo.',
    imagen: 'amoprazol.png',
    presentaciones: [
      { id:'4-a1', tipo:'Unidad',      conc:'20mg',  marca:'Genérico', precio:350  },
      { id:'4-a2', tipo:'Unidad',      conc:'20mg',  marca:'Genfar',   precio:500  },
      { id:'4-b1', tipo:'Tableta x14', conc:'20mg',  marca:'Genérico', precio:4500 },
      { id:'4-b2', tipo:'Tableta x14', conc:'20mg',  marca:'Genfar',   precio:6000 },
      { id:'4-b3', tipo:'Tableta x14', conc:'20mg',  marca:'MK',       precio:5500 },
      { id:'4-b4', tipo:'Tableta x14', conc:'20mg',  marca:'Losec',    precio:10000},
      { id:'4-b5', tipo:'Tableta x14', conc:'40mg',  marca:'Genérico', precio:6500 },
      { id:'4-b6', tipo:'Tableta x14', conc:'40mg',  marca:'Genfar',   precio:8500 },
    ]
  },
  {
    id: 5, nombre: 'Loratadina', cat: 'antihistaminicos',
    desc: 'Antihistamínico de segunda generación para alergias.',
    imagen: 'loratadina.png',
    presentaciones: [
      { id:'5-a1', tipo:'Unidad',      conc:'10mg',  marca:'Genérico',  precio:250  },
      { id:'5-a2', tipo:'Unidad',      conc:'10mg',  marca:'Genfar',    precio:350  },
      { id:'5-b1', tipo:'Tableta x10', conc:'10mg',  marca:'Genérico',  precio:2200 },
      { id:'5-b2', tipo:'Tableta x10', conc:'10mg',  marca:'Genfar',    precio:3000 },
      { id:'5-b3', tipo:'Tableta x10', conc:'10mg',  marca:'MK',        precio:2800 },
      { id:'5-b4', tipo:'Tableta x10', conc:'10mg',  marca:'Clarityne', precio:5500 },
    ]
  },
  {
    id: 6, nombre: 'Naproxeno', cat: 'antiinflamatorios',
    desc: 'Antiinflamatorio no esteroideo para dolor e inflamación.',
    imagen: 'naproxeno.jfif',
    presentaciones: [
      { id:'6-a1', tipo:'Unidad',      conc:'250mg', marca:'Genérico', precio:300  },
      { id:'6-a2', tipo:'Unidad',      conc:'500mg', marca:'Genérico', precio:500  },
      { id:'6-b1', tipo:'Tableta x10', conc:'250mg', marca:'Genérico', precio:2800 },
      { id:'6-b2', tipo:'Tableta x10', conc:'250mg', marca:'Genfar',   precio:3500 },
      { id:'6-b3', tipo:'Tableta x10', conc:'500mg', marca:'Genérico', precio:4500 },
      { id:'6-b4', tipo:'Tableta x10', conc:'500mg', marca:'Genfar',   precio:5500 },
      { id:'6-b5', tipo:'Tableta x10', conc:'500mg', marca:'MK',       precio:5000 },
      { id:'6-b6', tipo:'Tableta x10', conc:'500mg', marca:'Naprosyn', precio:7000 },
    ]
  },
  {
    id: 7, nombre: 'Cetirizina', cat: 'antihistaminicos',
    desc: 'Antihistamínico para rinitis alérgica y urticaria.',
    imagen: 'ceterizina.jfif',
    presentaciones: [
      { id:'7-a1', tipo:'Unidad',      conc:'10mg',  marca:'Genérico', precio:280  },
      { id:'7-b1', tipo:'Tableta x10', conc:'10mg',  marca:'Genérico', precio:2500 },
      { id:'7-b2', tipo:'Tableta x10', conc:'10mg',  marca:'Genfar',   precio:3200 },
      { id:'7-b3', tipo:'Tableta x10', conc:'10mg',  marca:'MK',       precio:3000 },
      { id:'7-b4', tipo:'Tableta x10', conc:'10mg',  marca:'Zyrtec',   precio:6000 },
    ]
  },
  {
    id: 8, nombre: 'Metformina', cat: 'otros',
    desc: 'Antidiabético oral para diabetes tipo 2.',
    imagen: 'Metformina.jfif',
    presentaciones: [
      { id:'8-a1', tipo:'Unidad',      conc:'500mg',  marca:'Genérico',    precio:200  },
      { id:'8-b1', tipo:'Tableta x30', conc:'500mg',  marca:'Genérico',    precio:5500 },
      { id:'8-b2', tipo:'Tableta x30', conc:'500mg',  marca:'Genfar',      precio:7000 },
      { id:'8-b3', tipo:'Tableta x30', conc:'500mg',  marca:'Glucophage',  precio:12000},
      { id:'8-b4', tipo:'Tableta x30', conc:'850mg',  marca:'Genérico',    precio:7000 },
      { id:'8-b5', tipo:'Tableta x30', conc:'850mg',  marca:'Genfar',      precio:9000 },
      { id:'8-b6', tipo:'Tableta x30', conc:'1000mg', marca:'Genérico',    precio:8500 },
    ]
  },
  {
    id: 9, nombre: 'Atorvastatina', cat: 'otros',
    desc: 'Hipolipemiante para reducir el colesterol.',
    imagen: 'Atorvastatina.png',
    presentaciones: [
      { id:'9-b1', tipo:'Tableta x30', conc:'10mg',  marca:'Genérico', precio:9000  },
      { id:'9-b2', tipo:'Tableta x30', conc:'10mg',  marca:'Genfar',   precio:12000 },
      { id:'9-b3', tipo:'Tableta x30', conc:'10mg',  marca:'MK',       precio:11000 },
      { id:'9-b4', tipo:'Tableta x30', conc:'10mg',  marca:'Lipitor',  precio:35000 },
      { id:'9-b5', tipo:'Tableta x30', conc:'20mg',  marca:'Genérico', precio:12000 },
      { id:'9-b6', tipo:'Tableta x30', conc:'20mg',  marca:'Genfar',   precio:16000 },
      { id:'9-b7', tipo:'Tableta x30', conc:'40mg',  marca:'Genérico', precio:16000 },
      { id:'9-b8', tipo:'Tableta x30', conc:'40mg',  marca:'Genfar',   precio:22000 },
    ]
  },
  {
    id: 10, nombre: 'Losartán', cat: 'otros',
    desc: 'Antihipertensivo bloqueador de receptores de angiotensina II.',
    imagen: 'losartan.png',
    presentaciones: [
      { id:'10-a1', tipo:'Unidad',      conc:'50mg',   marca:'Genérico', precio:350  },
      { id:'10-b1', tipo:'Tableta x30', conc:'50mg',   marca:'Genérico', precio:9500 },
      { id:'10-b2', tipo:'Tableta x30', conc:'50mg',   marca:'Genfar',   precio:13000},
      { id:'10-b3', tipo:'Tableta x30', conc:'50mg',   marca:'MK',       precio:12000},
      { id:'10-b4', tipo:'Tableta x30', conc:'50mg',   marca:'Cozaar',   precio:28000},
      { id:'10-b5', tipo:'Tableta x30', conc:'100mg',  marca:'Genérico', precio:14000},
      { id:'10-b6', tipo:'Tableta x30', conc:'100mg',  marca:'Genfar',   precio:19000},
    ]
  },
  {
    id: 11, nombre: 'Enalapril', cat: 'otros',
    desc: 'Antihipertensivo inhibidor de la enzima convertidora.',
    imagen: 'Enalapril.jfif',
    presentaciones: [
      { id:'11-a1', tipo:'Unidad',      conc:'5mg',   marca:'Genérico', precio:250  },
      { id:'11-b1', tipo:'Tableta x30', conc:'5mg',   marca:'Genérico', precio:7000 },
      { id:'11-b2', tipo:'Tableta x30', conc:'5mg',   marca:'Genfar',   precio:9500 },
      { id:'11-b3', tipo:'Tableta x30', conc:'10mg',  marca:'Genérico', precio:9000 },
      { id:'11-b4', tipo:'Tableta x30', conc:'10mg',  marca:'Genfar',   precio:12000},
      { id:'11-b5', tipo:'Tableta x30', conc:'20mg',  marca:'Genérico', precio:12000},
    ]
  },
  {
    id: 12, nombre: 'Aspirina', cat: 'analgesicos',
    desc: 'Analgésico, antipirético y antiagregante plaquetario.',
    imagen: 'aspirina.jfif',
    presentaciones: [
      { id:'12-a1', tipo:'Unidad',      conc:'100mg', marca:'Genérico', precio:150  },
      { id:'12-a2', tipo:'Unidad',      conc:'500mg', marca:'Genérico', precio:200  },
      { id:'12-b1', tipo:'Tableta x30', conc:'100mg', marca:'Genérico', precio:4000 },
      { id:'12-b2', tipo:'Tableta x30', conc:'100mg', marca:'Genfar',   precio:5500 },
      { id:'12-b3', tipo:'Tableta x30', conc:'100mg', marca:'Bayer',    precio:8500 },
      { id:'12-b4', tipo:'Tableta x10', conc:'500mg', marca:'Genérico', precio:1800 },
      { id:'12-b5', tipo:'Tableta x10', conc:'500mg', marca:'Bayer',    precio:4000 },
    ]
  },
  {
    id: 13, nombre: 'Paracetamol', cat: 'analgesicos',
    desc: 'Analgésico y antipirético de uso general.',
    imagen: 'paracetamol.jfif',
    presentaciones: [
      { id:'13-a1', tipo:'Unidad',      conc:'500mg',  marca:'Genérico', precio:120  },
      { id:'13-a2', tipo:'Unidad',      conc:'500mg',  marca:'Genfar',   precio:180  },
      { id:'13-a3', tipo:'Unidad',      conc:'1000mg', marca:'Genérico', precio:250  },
      { id:'13-b1', tipo:'Tableta x10', conc:'500mg',  marca:'Genérico', precio:1000 },
      { id:'13-b2', tipo:'Tableta x10', conc:'500mg',  marca:'Genfar',   precio:1500 },
      { id:'13-b3', tipo:'Tableta x10', conc:'500mg',  marca:'Tylenol',  precio:3500 },
      { id:'13-b4', tipo:'Tableta x10', conc:'1000mg', marca:'Genérico', precio:2200 },
      { id:'13-c1', tipo:'Caja x20',    conc:'500mg',  marca:'Genérico', precio:1800 },
      { id:'13-c2', tipo:'Caja x20',    conc:'500mg',  marca:'Genfar',   precio:2600 },
    ]
  },
  {
    id: 14, nombre: 'Clonazepam', cat: 'otros',
    desc: 'Ansiolítico benzodiacepina. Requiere fórmula médica.',
    imagen: 'Clonazepam.jfif',
    presentaciones: [
      { id:'14-a1', tipo:'Unidad',      conc:'0.5mg', marca:'Genérico', precio:400  },
      { id:'14-b1', tipo:'Tableta x30', conc:'0.5mg', marca:'Genérico', precio:11000},
      { id:'14-b2', tipo:'Tableta x30', conc:'0.5mg', marca:'Genfar',   precio:14000},
      { id:'14-b3', tipo:'Tableta x30', conc:'2mg',   marca:'Genérico', precio:14000},
      { id:'14-b4', tipo:'Tableta x30', conc:'2mg',   marca:'Rivotril', precio:28000},
    ]
  },
  {
    id: 15, nombre: 'Fluoxetina', cat: 'otros',
    desc: 'Antidepresivo inhibidor de la recaptación de serotonina.',
    imagen: 'Fluoxetina.png',
    presentaciones: [
      { id:'15-b1', tipo:'Tableta x14', conc:'20mg',  marca:'Genérico', precio:6500 },
      { id:'15-b2', tipo:'Tableta x14', conc:'20mg',  marca:'Genfar',   precio:8500 },
      { id:'15-b3', tipo:'Tableta x14', conc:'20mg',  marca:'MK',       precio:8000 },
      { id:'15-b4', tipo:'Tableta x14', conc:'20mg',  marca:'Prozac',   precio:22000},
      { id:'15-b5', tipo:'Tableta x28', conc:'20mg',  marca:'Genérico', precio:12000},
      { id:'15-b6', tipo:'Tableta x28', conc:'20mg',  marca:'Genfar',   precio:16000},
    ]
  },
];

const LABELS_CAT = {
  analgesicos:'Analgésico', antiinflamatorios:'Antiinflamatorio',
  antibioticos:'Antibiótico', antihistaminicos:'Antihistamínico', otros:'Otros'
};
