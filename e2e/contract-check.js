// 前后端接口契约比对：前端所有 API 引用 vs 后端 Controller 路由（仅本地静态分析，无任何输入来源）
const fs = require('fs'), path = require('path');
const PRJ = path.join(__dirname, '..', 'campus-runner');

const walk = (d) => fs.readdirSync(d, { withFileTypes: true })
  .flatMap(e => e.isDirectory() ? walk(path.join(d, e.name)) : [path.join(d, e.name)]);

// 1. 前端文件
const feRel = [
  'campus-server/src/main/resources/static/user-app/index.html',
  'campus-server/src/main/resources/static/skiller-app/index.html',
  'campus-server/src/main/resources/static/admin-app/index.html',
];
const contents = feRel.map(f => fs.readFileSync(path.join(PRJ, f), 'utf8'));
contents.push(...walk(path.join(PRJ, 'admin-web', 'src')).filter(f => /\.(js|vue)$/.test(f)).map(f => fs.readFileSync(f, 'utf8')));

// 2. 提取前端 API 路径
const urls = new Set();
const collect = (re, transform) => {
  for (const c of contents) for (const m of c.matchAll(re)) {
    const u = transform(m[1]);
    if (!u.includes('$') && !u.includes('{')) urls.add(u);
  }
};
collect(/['"`](\/(?:user|skiller|admin)\/[A-Za-z0-9_\-\/]*)['"`]/g, u => u);
collect(/['"`](\/(?:user|skiller|admin)\/[A-Za-z0-9_\-\/]+)\$\{[^}]+\}/g, u => u + '/X');

// 3. 后端路由
const routes = [];
for (const f of walk(path.join(PRJ, 'campus-server', 'src', 'main', 'java', 'com', 'campus', 'runner', 'controller')).filter(f => f.endsWith('.java'))) {
  const src = fs.readFileSync(f, 'utf8');
  const base = (src.match(/@RequestMapping\("([^"]+)"\)/) || [])[1] || '';
  for (const mm of src.matchAll(/@(Get|Post|Put|Delete)Mapping(?:\("([^"]*)"\))?/g)) {
    const p = (base + '/' + (mm[2] || '')).replace(/\/+/g, '/');
    routes.push(p === '/' ? '' : p.replace(/\/$/, ''));
  }
}

// 4. 段级比对（X 通配一段）
function match(fePath) {
  const segs = fePath.split('/').filter(Boolean);
  return routes.some(r => {
    const rs = r.split('/').filter(Boolean);
    let i = 0;
    for (const s of segs) {
      if (s === 'X') { if (i >= rs.length) return false; i++; continue; }
      if (i >= rs.length) return false;
      if (rs[i].startsWith('{')) { i++; continue; }
      if (rs[i] !== s) return false;
      i++;
    }
    return i === rs.length;
  });
}

const missing = [...urls].sort().filter(u => !match(u));
console.log('前端引用接口数:', urls.size, ' 后端路由数:', routes.length);
console.log(missing.length ? '未匹配到后端的调用:' : 'OK: 前端所有可静态提取的接口均匹配后端路由');
missing.forEach(u => console.log('  -', u));
