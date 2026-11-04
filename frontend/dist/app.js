
'use strict';
const view = document.querySelector('#view');
const nav = document.querySelector('#nav');
const notice = document.querySelector('#notice');
let token = sessionStorage.getItem('framenoteToken');
let mediaUrl;
let revision = 0;
let canLeavePage = () => true;
let activeRoute = location.hash || '#/spaces';
let navigationGeneration = 0;

const labels = {DRAFT:'Borrador',EDITING:'En edición',IN_REVIEW:'En revisión',CHANGES_REQUESTED:'Cambios solicitados',APPROVED:'Aprobado',ARCHIVED:'Archivado',PENDING:'Pendiente',IN_PROGRESS:'En curso',RESOLVED:'Resuelta',REOPENED:'Reabierta',OWNER:'Propietario',EDITOR:'Editor',REVIEWER:'Revisor'};
const projectStates = {DRAFT:['EDITING'],EDITING:['IN_REVIEW'],IN_REVIEW:['CHANGES_REQUESTED','APPROVED'],CHANGES_REQUESTED:['EDITING'],APPROVED:['ARCHIVED']};
const noteStates = {PENDING:['IN_PROGRESS','RESOLVED'],IN_PROGRESS:['RESOLVED'],RESOLVED:['REOPENED'],REOPENED:['IN_PROGRESS','RESOLVED']};
const label = value => labels[value] || value;
function node(parent, tag, text) {
  const element = document.createElement(tag);
  if (text != null) element.textContent = text;
  parent.append(element);
  return element;
}
async function run(control, action) {
  if (control.disabled) return;
  control.disabled = true;
  notice.textContent = '';
  try { await action(); }
  catch (error) { notice.textContent = error.message; }
  finally { control.disabled = false; }
}
function button(parent, title, action) {
  const control = node(parent,'button',title);
  control.onclick = () => run(control,action);
  return control;
}
function input(form, title, name, type, options = {}) {
  const field = node(form,'label',title);
  const control = node(field,type === 'textarea' ? 'textarea' : 'input');
  Object.assign(control,{name,required:true},options);
  if (type !== 'textarea') control.type = type;
  form.insertBefore(field,form.querySelector('button[type="submit"]'));
  return control;
}
function form(parent, title, submit) {
  const element = node(parent,'form');
  const control = node(element,'button',title);
  control.type = 'submit';
  element.onsubmit = event => {
    event.preventDefault();
    const values = new FormData(element);
    run(control,() => submit(values));
  };
  return element;
}
function choose(form, title, name, options) {
  const select = node(node(form,'label',title),'select');
  select.name = name; form.insertBefore(select.parentElement,form.querySelector('button[type="submit"]'));
  for (const value of options) node(select,'option',label(value)).value = value;
}
async function api(path, method = 'GET', body) {
  const headers = {};
  if (token) headers.Authorization = 'Bearer ' + token;
  if (body && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }
  const response = await fetch('/api' + path,{method,headers,body});
  if (!response.ok) {
    if (response.status === 401 && token) { signOut(); throw Error('Tu sesión terminó. Inicia sesión de nuevo.'); }
    const data = await response.json().catch(() => ({}));
    throw Error(data.error || data.message || 'La solicitud falló (' + response.status + ').');
  }
  return path.endsWith('/file') ? response.blob() : response.json();
}
function page(title) {
  canLeavePage = () => true;
  revision++;
  if (mediaUrl) { URL.revokeObjectURL(mediaUrl); mediaUrl = undefined; }
  view.replaceChildren();
  node(view,'h1',title);
  return node(view,'section');
}
function signOut() {
  token = null;
  sessionStorage.removeItem('framenoteToken');
  auth();
}
function authView(register = false) {
  nav.replaceChildren();
  const section = page(register ? 'Crea tu cuenta' : 'Inicia sesión');
  section.className = 'auth';
  node(section,'p','Organiza tus proyectos audiovisuales y revisa cada versión con tu equipo.');
  const fields = form(section,register ? 'Registrarme' : 'Entrar',async values => {
    const result = await api('/auth/' + (register ? 'register' : 'login'),'POST',Object.fromEntries(values));
    token = result.token;
    sessionStorage.setItem('framenoteToken',token);
    await home();
  });
  if (register) input(fields,'Nombre','name','text');
  input(fields,'Correo','email','email',{autocomplete:'email',placeholder:'tu@correo.com'});
  input(fields,'Contraseña','password','password',register ? {minLength:10,autocomplete:'new-password'} : {autocomplete:'current-password'});
  button(section,register ? 'Ya tengo cuenta' : 'Crear cuenta',() => auth(!register));
}
function empty(parent, items, message) {
  if (!items.length) node(parent,'p',message);
}
async function homeView() {
  const section = page('Espacios de trabajo');
  nav.replaceChildren();
  button(nav,'Inicio',home);
  button(nav,'Notificaciones',notifications);
  button(nav,'Salir',signOut);
  const spaces = await api('/workspace-access');
  empty(section,spaces,'Todavía no tienes espacios disponibles.');
  for (const space of spaces) button(section,space.name,() => workspace(space));
}
async function workspaceView(space) {
  const section = page(space.name);
  button(section,'← Espacios de trabajo',home);
  const base = '/workspaces/' + space.id;
  const [usage,projects] = await Promise.all([api(base + '/usage'),api(base + '/projects')]);
  node(section,'p',usage.plan + ' · ' + (usage.usedBytes / 1048576).toFixed(1) + ' de ' + Math.round(usage.maxBytes / 1048576) + ' MB · Hasta ' + usage.maxProjects + ' proyecto(s) activo(s)');
  if (space.role === 'OWNER' && projects.filter(item => item.status !== 'ARCHIVED').length < usage.maxProjects) {
    const fields = form(section,'Crear proyecto',async values => project(space,await api(base + '/projects','POST',Object.fromEntries(values))));
    input(fields,'Nombre del proyecto','name','text',{maxLength:120});
  }
  empty(section,projects,'Aún no hay proyectos.');
  for (const item of projects) {
    const row = node(section,'article'); row.className = 'resource-row';
    button(row,item.name + ' · ' + label(item.status),() => project(space,item));
    if (space.role === 'OWNER') removalButton(row,'Borrar proyecto','/projects/' + item.id,
      '¿Borrar el proyecto «' + item.name + '», todos sus videos y versiones?','/spaces/' + space.id,
      !['APPROVED','ARCHIVED'].includes(item.status));
  }
}
async function projectView(space,item) {
  const section = page(item.name);
  button(section,'Volver al espacio',() => workspace(space));
  const base = '/projects/' + item.id;
  const [videos, access] = await Promise.all([api(base + '/videos'),api(base + '/access')]);
  const role = access.role;
  item = access.project;
  node(section,'p',label(item.status) + ' · ' + label(role));
  const editing = role === 'OWNER' || role === 'EDITOR';
  const editable = !['APPROVED','ARCHIVED'].includes(item.status);
  const transitions = (projectStates[item.status] || []).filter(next =>
    next === 'ARCHIVED' ? role === 'OWNER' : ['EDITING','IN_REVIEW'].includes(next) ? editing : role !== 'EDITOR');
  if (role === 'OWNER') removalButton(section,'Borrar proyecto',base,
    '¿Borrar el proyecto «' + item.name + '», todos sus videos y versiones?','/spaces/' + space.id,editable);
  if (transitions.length) {
    const fields = form(section,'Cambiar estado',async values => project(space,await api(base + '/status','PATCH',Object.fromEntries(values))));
    choose(fields,'Nuevo estado','state',transitions);
  }
  if (role === 'OWNER') {
    const fields = form(section,'Agregar integrante',async values => {
      await api(base + '/members','POST',Object.fromEntries(values));
      fields.reset();
      notice.textContent = 'Integrante agregado. Ya puede ver el proyecto.';
    });
    input(fields,'Correo de una cuenta registrada','email','email');
    choose(fields,'Rol','role',['EDITOR','REVIEWER']);
    button(section,'Ver actividad',() => navigate('/projects/' + item.id + '/audit'));
  }
  if (editing && editable) {
    const fields = form(section,'Crear video',async values => video(space,item,await api(base + '/videos','POST',Object.fromEntries(values)),role));
    input(fields,'Título del video','title','text',{maxLength:120});
  }
  node(section,'h2','Videos');
  empty(section,videos,'Todavía no hay videos.');
  for (const entry of videos) {
    const row = node(section,'article'); row.className = 'resource-row';
    button(row,entry.title,() => video(space,item,entry,role));
    if (editing) removalButton(row,'Borrar video','/videos/' + entry.id,
      '¿Borrar el video «' + entry.title + '», sus versiones y observaciones?','/projects/' + item.id,editable);
  }
}
function duration(file) {
  return new Promise((resolve,reject) => {
    const probe = document.createElement('video');
    const url = URL.createObjectURL(file);
    const finish = result => {
      clearTimeout(timeout);
      URL.revokeObjectURL(url);
      if (Number.isSafeInteger(result) && result > 0) resolve(result);
      else reject(Error('No pudimos leer la duración. Selecciona un MP4 válido.'));
    };
    const timeout = setTimeout(() => finish(0),15000);
    probe.onloadedmetadata = () => finish(Math.round(probe.duration * 1000));
    probe.onerror = () => finish(0);
    probe.preload = 'metadata';
    probe.src = url;
  });
}
async function videoView(space,item,entry,role) {
  const section = page(entry.title);
  button(section,'Volver al proyecto',() => project(space,item));
  const base = '/videos/' + entry.id;
  if (role !== 'REVIEWER') removalButton(section,'Borrar video',base,
    '¿Borrar el video «' + entry.title + '», sus versiones y observaciones?','/projects/' + item.id,
    !['APPROVED','ARCHIVED'].includes(item.status));
  if (role !== 'REVIEWER' && !['APPROVED','ARCHIVED'].includes(item.status)) {
    node(section,'p','Selecciona un MP4. FrameNote leerá su duración automáticamente.');
    const fields = form(section,'Subir versión',async values => {
      const file = values.get('file');
      if (!/\.mp4$/i.test(file.name)) throw Error('Selecciona un archivo MP4.');
      notice.textContent = 'Leyendo el video…';
      values.set('durationMs',String(await duration(file)));
      notice.textContent = 'Subiendo el video…';
      const uploaded = await api(base + '/upload','POST',values);
      notice.textContent = 'Versión subida.';
      await version(space,item,entry,role,uploaded);
    });
    input(fields,'Archivo MP4','file','file',{accept:'.mp4,video/mp4'});
  }
  const versions = await api(base + '/versions');
  node(section,'h2','Versiones');
  empty(section,versions,'Sube el archivo para crear la primera versión.');
  for (const entryVersion of versions) {
    const number = entryVersion.version;
    const row = node(section,'div'); row.className = 'version-row';
    button(row,'Versión ' + number + ' · ' + (entryVersion.durationMs / 1000).toFixed(1) + ' s',() => version(space,item,entry,role,entryVersion));
    if (role !== 'REVIEWER') removalButton(row,'Borrar versión','/versions/' + entryVersion.id,
      '¿Borrar la versión ' + number + ' y sus observaciones?','/videos/' + entry.id,
      !['APPROVED','ARCHIVED'].includes(item.status));
  }
}

async function versionView(space,item,entry,role,current) {
  const section = page('Sala de revisión');
  const pageRevision = revision;
  section.classList.add('review');
  const top = node(section,'div');
  top.className = 'review-top';
  const heading = node(top,'div');
  node(heading,'small',item.name + ' / Revisión audiovisual');
  node(heading,'h2',entry.title);
  button(top,'← Versiones',() => video(space,item,entry,role));
  const [versions,notes] = await Promise.all([
    api('/videos/' + entry.id + '/versions'), api('/versions/' + current.id + '/annotations')
  ]);
  if (revision !== pageRevision) return;
  if (role !== 'REVIEWER') removalButton(top,'Borrar esta versión','/versions/' + current.id,
    '¿Borrar esta versión y sus observaciones?','/videos/' + entry.id,
    !['APPROVED','ARCHIVED'].includes(item.status));
  const base = '/versions/' + current.id;
  const editable = current.status === 'AVAILABLE' && versions[0]?.id === current.id &&
    !['APPROVED','ARCHIVED'].includes(item.status);
  const layout = node(section,'div');
  layout.className = 'review-grid';
  const desk = node(layout,'div');
  desk.className = 'desk';
  const side = node(layout,'aside');
  const stage = node(desk,'div');
  stage.className = 'stage';
  const player = node(stage,'video');
  player.playsInline = true;
  const svg = document.createElementNS('http://www.w3.org/2000/svg','svg');
  svg.setAttribute('viewBox','0 0 1000 1000');
  svg.setAttribute('preserveAspectRatio','none');
  stage.append(svg);
  const toolbar = node(desk,'div');
  toolbar.className = 'review-tools';
  const play = button(toolbar,'▶ Reproducir',async () => {
    if (!player.src) throw Error('El video todavía se está cargando.');
    if (!discardDraft()) return;
    if (player.paused) await player.play();
    else player.pause();
  });
  const clock = node(toolbar,'output','00:00.000');
  const timeline = node(desk,'div');
  timeline.className = 'timeline';
  const markers = node(timeline,'div');
  markers.className = 'markers';
  const seek = node(timeline,'input');
  Object.assign(seek,{type:'range',min:0,max:Math.max(0,current.durationMs - 1),value:0});
  seek.setAttribute('aria-label','Posición del video');
  const time = ms => {
    const value = Math.max(0,Math.floor(ms));
    return String(Math.floor(value / 60000)).padStart(2,'0') + ':' +
      String(Math.floor(value / 1000) % 60).padStart(2,'0') + '.' + String(value % 1000).padStart(3,'0');
  };
  node(desk,'p','Las banderas marcan instantes con observaciones. Selecciona una para ver su dibujo y conversación.').className = 'review-hint';
  const foot = node(desk,'small','Versión ' + current.version + ' · ' + time(current.durationMs));
  const tools = node(desk,'div');
  tools.className = 'review-tools';
  const draft = node(side,'div');
  draft.className = 'draft';
  const detail = node(side,'div');
  const noteList = node(side,'div');
  let drawing = false;
  let mode = 'pen';
  let active = null;
  let strokes = []; let colors = []; let ink = "#fff058";
  let selected = null;
  let draftTime = 0;
  let selection = 0;
  let textBox;
  canLeavePage = discardDraft;
  function fit() {
    if (!player.videoWidth || !stage.clientWidth) return;
    const {width, height} = stage.getBoundingClientRect();
    const ratio = Math.min(width / player.videoWidth, height / player.videoHeight);
    const frameWidth = player.videoWidth * ratio;
    const frameHeight = player.videoHeight * ratio;
    for (const layer of [player, svg]) {
      Object.assign(layer.style, {
        width: frameWidth + 'px',
        height: frameHeight + 'px',
        left: (width - frameWidth) / 2 + 'px',
        top: (height - frameHeight) / 2 + 'px'
      });
    }
  }
  const observer = new ResizeObserver(() => {
    if (!stage.isConnected) observer.disconnect();
    else fit();
  });
  observer.observe(stage);
  const viewObserver = new MutationObserver(() => {
    if (!stage.isConnected) { observer.disconnect(); viewObserver.disconnect(); }
  });
  viewObserver.observe(view,{childList:true});
  player.addEventListener('loadedmetadata',fit);
  function paint() {
    svg.replaceChildren();
    svg.classList.toggle('drawing',drawing);
    const paths = drawing ? [...strokes,...(active ? [active.points] : [])] : selected?.paths || [];
    for (const [index,points] of paths.entries()) {
      if (points.length < 2) continue;
      const line = document.createElementNS(svg.namespaceURI,'polyline');
      line.setAttribute('points',points.map(([x,y]) => x * 1000 + ',' + y * 1000).join(' '));
      line.setAttribute('vector-effect','non-scaling-stroke');
      const color = drawing ? (colors[index] || active?.color) : selected?.colors?.[index];
      if (/^#[0-9a-f]{6}$/i.test(color || '')) line.style.stroke = color;
      svg.append(line);
    }
    if (!drawing && selected && Number.isFinite(selected.x) && Number.isFinite(selected.y)) {
      const dot = document.createElementNS(svg.namespaceURI,'circle');
      dot.setAttribute('cx',selected.x * 1000);
      dot.setAttribute('cy',selected.y * 1000);
      dot.setAttribute('r','12');
      svg.append(dot);
    }
  }
  function clearDraft() {
    drawing = false;
    active = null;
    strokes = []; colors = [];
    draft.replaceChildren();
    paint();
  }
  function discardDraft() {
    if (!drawing) return true;
    if (draft.querySelector('button[type="submit"]:disabled')) return false;
    const hasContent = strokes.length || active || textBox?.value.trim();
    if (hasContent && !window.confirm('¿Descartar la nota sin guardar?')) return false;
    clearDraft();
    return true;
  }
  function position(event) {
    const rect = svg.getBoundingClientRect();
    const clamp = value => Math.round(Math.max(0,Math.min(1,value)) * 1000) / 1000;
    return [clamp((event.clientX - rect.left) / rect.width),clamp((event.clientY - rect.top) / rect.height)];
  }
  svg.onpointerdown = event => {
    if (!drawing || active || event.button !== 0) return;
    const point = position(event);
    active = {id:event.pointerId,color:ink,points:[point,point]};
    svg.setPointerCapture(event.pointerId);
    paint();
  };
  svg.onpointermove = event => {
    if (active?.id !== event.pointerId) return;
    const point = position(event);
    if (mode === 'line') active.points[1] = point;
    else active.points.push(point);

    paint();
  };
  svg.onpointerup = event => {
    if (active?.id !== event.pointerId) return;
    const candidate = [...strokes,active.points];
    strokes = candidate; colors.push(active.color);

    active = null;
    paint();
  };
  svg.onpointercancel = () => { active = null; paint(); };
  function jump(ms) {
    if (!player.src) throw Error('Espera a que el video termine de cargar.');
    player.pause();
    player.currentTime = ms / 1000;
    seek.value = ms;
    clock.textContent = time(ms);
  }
  seek.oninput = () => {
    if (!discardDraft()) { seek.value = draftTime; return; }
    clearDraft();
    selected = null;
    paint();
    if (player.src) jump(Number(seek.value));
  };
  player.ontimeupdate = () => {
    const ms = Math.round(player.currentTime * 1000);
    clock.textContent = time(ms);
    seek.value = ms;
  };
  player.onplay = () => { play.textContent = 'Ⅱ Pausar'; selected = null; paint(); };
  player.onpause = () => { play.textContent = '▶ Reproducir'; };
  player.onended = player.onpause;
  function drawDraft() {
    if (drawing) { textBox.focus(); return; }
    if (!player.videoWidth) throw Error('Espera a que el video esté listo.');
    player.pause();
    selected = null;
    detail.replaceChildren();
    renderNotes();
    draftTime = Math.max(0,Math.min(current.durationMs - 1,Math.round(player.currentTime * 1000)));
    drawing = true;
    fit();
    paint();
    node(draft,'h3','Nueva nota · ' + time(draftTime));
    const actions = node(draft,'div');
    actions.className = 'review-tools';
    const pen = button(actions,'Lápiz',() => setMode('pen'));
    const line = button(actions,'Línea',() => setMode('line'));
    function setMode(next) {
      mode = next;
      pen.setAttribute('aria-pressed',String(mode === 'pen'));
      line.setAttribute('aria-pressed',String(mode === 'line'));
    }
    setMode('pen');
    const palette = node(node(actions,'label','Color del trazo'),'input');
    palette.type = 'color';
    palette.value = ink;
    palette.oninput = () => { ink = palette.value; };
    button(actions,'Limpiar dibujo',() => { strokes = []; colors = []; paint(); });
    button(actions,'Deshacer trazo',() => { strokes.pop(); colors.pop(); paint(); });
    button(actions,'Descartar nota',discardDraft);
    node(draft,'small','Dibuja sobre el fotograma, escribe una observación o combina ambos.');
    const fields = form(draft,'Guardar nota',async values => {
      const text = values.get('text').trim();
      if (!text && !strokes.length) throw Error('Escribe una nota o dibuja sobre el video.');
      const annotation = await api(base + '/annotations','POST',{
        timestampMs:draftTime,text,geometryJson:strokes.length ? JSON.stringify({paths:strokes,colors}) : null
      });
      if (revision !== pageRevision) return;
      notes.push(annotation);
      notes.sort((a,b) => a.timestampMs - b.timestampMs);
      clearDraft();
      renderNotes();
      await openNote(annotation);
    });
    textBox = input(fields,'Observación','text','textarea',{maxLength:2000,required:false});
    textBox.focus();
  }
  if (editable) button(tools,'＋ Nueva nota',drawDraft).classList.add('primary');
  else node(tools,'small','Solo lectura · ' + (versions[0]?.id !== current.id ? 'Versión anterior' : label(item.status)));
  button(tools,'Actualizar notas',async () => {
    const fresh = await api(base + '/annotations');
    notes.splice(0,notes.length,...fresh);
    renderNotes();
  });
  function pathsFor(note) {
    try {
      const paths = JSON.parse(note.geometryJson)?.paths;
      return Array.isArray(paths) ? paths.filter(path => Array.isArray(path) && path.every(point =>
        Array.isArray(point) && point.length === 2 && point.every(value => Number.isFinite(value) && value >= 0 && value <= 1))) : [];
    } catch { return []; }
  }
  async function openNote(note) {
    if (!discardDraft()) return;
    jump(note.timestampMs);
    clearDraft();
    selected = {...note,paths:pathsFor(note)};
    try { selected.colors = JSON.parse(note.geometryJson)?.colors; } catch {}
    paint();
    renderNotes();
    const request = ++selection;
    detail.replaceChildren();
    node(detail,'h3',time(note.timestampMs) + ' · ' + label(note.status));
    node(detail,'p',note.text || 'Dibujo sobre el fotograma');
    const endpoint = '/annotations/' + note.id;
    if (editable && role !== 'REVIEWER') {
      for (const state of noteStates[note.status] || []) button(detail,label(state),async () => {
        const updated = await api(endpoint + '/status','PATCH',{state});
        Object.assign(note,updated);
        if (request === selection) await openNote(note);
        else renderNotes();
      });
    }
    let actor;
    try { actor = JSON.parse(atob(token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/'))).sub; } catch {}
    if (editable && (role === 'OWNER' || note.authorId === actor)) {
      button(detail,'Borrar nota',async () => {
        if (!window.confirm('¿Borrar esta nota y ocultar sus respuestas?')) return;
        await api(endpoint,'DELETE');
        notes.splice(notes.findIndex(entry => entry.id === note.id),1);
        if (selected?.id === note.id) { selected = null; selection++; detail.replaceChildren(); paint(); }
        renderNotes();
      });
    }
    const thread = node(detail,'div');
    const comments = await api(endpoint + '/comments');
    if (request !== selection || revision !== pageRevision) return;
    node(thread,'h3','Conversación');
    for (const comment of comments) node(thread,'p',comment.text);
    if (editable) {
      const fields = form(detail,'Responder',async values => {
        const comment = await api(endpoint + '/comments','POST',Object.fromEntries(values));
        node(thread,'p',comment.text);
        fields.reset();
      });
      input(fields,'Respuesta','text','textarea',{maxLength:2000});
    }
  }
  function renderNotes() {
    markers.replaceChildren();
    noteList.replaceChildren();
    node(noteList,'h3','Observaciones · ' + notes.length);
    empty(noteList,notes,'Las notas y los dibujos de esta versión aparecerán aquí.');
    const groups = new Map();
    for (const note of notes) {
      const item = button(noteList,(note.status === 'RESOLVED' ? '✓ ' : '') + time(note.timestampMs) + ' · ' + (note.text || 'Dibujo'),() => openNote(note));
      item.classList.add('note-item'); if (note.status === 'RESOLVED') item.classList.add('resolved');
      item.setAttribute('aria-pressed',String(selected?.id === note.id));
      const bin = Math.round(note.timestampMs / current.durationMs * 50) * 2;
      if (!groups.has(bin)) groups.set(bin,[]);
      groups.get(bin).push(note);
    }
    for (const [bin,group] of groups) {
      const note = group.find(entry => entry.id === selected?.id) || group[0];
      const flag = button(markers,group.length > 1 ? String(group.length) : '⚑',() => {
        const index = group.findIndex(entry => entry.id === selected?.id);
        return openNote(group[(index + 1) % group.length]);
      });
      if (group.every(entry => entry.status === 'RESOLVED')) { flag.classList.add('resolved'); if (group.length === 1) flag.textContent = '✓'; }
      flag.title = group.length === 1 ? 'Nota en ' + time(note.timestampMs) : group.length + ' notas cercanas · pulsa para recorrerlas';
      flag.setAttribute('aria-label',flag.title);
      flag.style.left = bin + '%';
    }
  }
  renderNotes();
  if (current.status === 'AVAILABLE') {
    foot.textContent += ' · Cargando video…';
    const file = await api(base + '/file');
    if (revision !== pageRevision) return;
    mediaUrl = URL.createObjectURL(file);
    player.src = mediaUrl;
    foot.textContent = 'Versión ' + current.version + ' · ' + time(current.durationMs);
  }
}

function showRecords(title,records,field,back) {
  const section = page(title);
  button(section,'Volver',back);
  empty(section,records,'Todavía no hay registros.');
  for (const record of records) node(section,'p',record[field]);
}
async function notificationsView() {
  showRecords('Notificaciones',await api('/notifications'),'message',home);
}





function home() { return navigate('/spaces'); }
function workspace(space) { return navigate('/spaces/' + space.id); }
function project(space,item) { return navigate('/projects/' + item.id); }
function video(space,item,entry,role) { return navigate('/videos/' + entry.id); }
function version(space,item,entry,role,current) { return navigate('/versions/' + current.id); }
function notifications() { return navigate('/notifications'); }
function auth(register = false) { return navigate(register ? '/register' : '/login',true); }

async function removeResource(path,message,destination) {
  if (!canLeavePage() || !window.confirm(message + ' Esta acción no se puede deshacer.')) return;
  await api(path,'DELETE');
  await navigate(destination,true);
  notice.textContent = 'Contenido eliminado. La cuota se actualizará al terminar la limpieza de sus archivos.';
}
function removalButton(parent,label,path,message,destination,editable) {
  const control = button(parent,label,() => removeResource(path,message,destination));
  control.classList.add('danger');
  if (!editable) {
    control.disabled = true;
    control.title = 'El proyecto aprobado o archivado está protegido como solo lectura.';
  }
}
async function navigate(path,replace = false) {
  if (!canLeavePage()) return;
  const hash = '#' + path;
  if (location.hash !== hash) {
    const depth = replace ? 0 : (history.state?.frameDepth || 0) + 1;
    history[replace ? 'replaceState' : 'pushState']({frameDepth:depth},'',hash);
  }
  await renderRoute();
}
function breadcrumbs(entries) {
  const trail = document.getElementById('breadcrumbs');
  trail.replaceChildren();
  if (entries.length > 1) button(trail,'← Atrás',() => {
    if (!canLeavePage()) return;
    if ((history.state?.frameDepth || 0) > 0) history.back();
    else return navigate(entries.at(-2).path,true);
  });
  for (const [index,entry] of entries.entries()) {
    if (index) node(trail,'span','/');
    if (index === entries.length - 1) node(trail,'strong',entry.name);
    else button(trail,entry.name,() => navigate(entry.path));
  }
}
async function renderRoute() {
  const generation = ++navigationGeneration;
  let path = location.hash.slice(1) || '/spaces';
  activeRoute = '#' + path;
  const trail = document.getElementById('breadcrumbs');
  try {
    if (!token) {
      trail.replaceChildren();
      const register = path === '/register';
      if (!register && path !== '/login') {
        history.replaceState({frameDepth:0},'','#/login');
        activeRoute = '#/login';
      }
      authView(register);
      return;
    }
    nav.replaceChildren();
    button(nav,'Inicio',home);
    button(nav,'Notificaciones',notifications);
    button(nav,'Salir',signOut);
    let entries = [{name:'Espacios de trabajo',path:'/spaces'}];
    if (['/spaces','/login','/register'].includes(path)) {
      if (path !== '/spaces') {
        history.replaceState({frameDepth:0},'','#/spaces');
        activeRoute = '#/spaces';
      }
      breadcrumbs(entries);
      await homeView();
      return;
    }
    if (path === '/notifications') {
      breadcrumbs([...entries,{name:'Notificaciones',path}]);
      await notificationsView();
      return;
    }
    const parts = path.split('/').filter(Boolean);
    if (parts[0] === 'spaces') {
      const spaces = await api('/workspace-access');
      const space = spaces.find(item => item.id === parts[1]);
      if (!space) throw Error('No tienes acceso a este espacio.');
      if (generation !== navigationGeneration) return;
      breadcrumbs([...entries,{name:space.name,path}]);
      await workspaceView(space);
      return;
    }
    let entry,current,projectId;
    if (parts[0] === 'versions') {
      current = await api('/versions/' + parts[1]);
      entry = await api('/videos/' + current.videoId);
      projectId = entry.projectId;
    } else if (parts[0] === 'videos') {
      entry = await api('/videos/' + parts[1]);
      projectId = entry.projectId;
    } else if (parts[0] === 'projects') projectId = parts[1];
    else throw Error('La pantalla solicitada no existe.');
    const access = await api('/projects/' + projectId + '/access');
    const item = access.project;
    const spaces = await api('/workspace-access');
    const space = spaces.find(candidate => candidate.id === item.workspaceId);
    if (!space) throw Error('No tienes acceso al espacio del proyecto.');
    if (generation !== navigationGeneration) return;
    entries.push({name:space.name,path:'/spaces/' + space.id},{name:item.name,path:'/projects/' + item.id});
    if (entry) entries.push({name:entry.title,path:'/videos/' + entry.id});
    if (current) entries.push({name:'Versión ' + current.version,path});
    if (parts[2] === 'audit') entries.push({name:'Actividad',path});
    breadcrumbs(entries);
    if (current) await versionView(space,item,entry,access.role,current);
    else if (entry) await videoView(space,item,entry,access.role);
    else if (parts[2] === 'audit') showRecords('Actividad del proyecto',await api('/projects/' + item.id + '/audit'),'action',() => project(space,item));
    else await projectView(space,item);
  } catch (error) {
    if (generation !== navigationGeneration) return;
    notice.textContent = error.message;
    const section = page('No se pudo abrir esta pantalla');
    button(section,'Volver a mis espacios',home);
    breadcrumbs([{name:'Espacios de trabajo',path:'/spaces'},{name:'Recurso no disponible',path}]);
  }
}
window.addEventListener('hashchange',() => {
  if (!canLeavePage()) {
    history.pushState(history.state,'',activeRoute);
    return;
  }
  renderRoute();
});
renderRoute();
