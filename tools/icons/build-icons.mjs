// 图标构建脚本：用无头 Chrome 把 SVG 母版按各尺寸渲染为 PNG，并封装 favicon.ico
// 用法：node tools/icons/build-icons.mjs
import { execFileSync } from 'node:child_process'
import { readFileSync, writeFileSync, mkdirSync, copyFileSync, existsSync, unlinkSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { tmpdir } from 'node:os'

const here = dirname(fileURLToPath(import.meta.url))
const staticDir = resolve(here, '../../campus-server/src/main/resources/static')

const CHROME_CANDIDATES = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
]
const chrome = CHROME_CANDIDATES.find(existsSync)
if (!chrome) throw new Error('未找到 Chrome/Edge，无法渲染图标')

const work = join(tmpdir(), 'campus-icons')
mkdirSync(work, { recursive: true })

function pngSize(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) throw new Error('不是 PNG')
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) }
}

function render(html, size, outPng) {
  if (existsSync(outPng)) unlinkSync(outPng)
  execFileSync(chrome, [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--no-sandbox',
    '--force-device-scale-factor=1',
    '--default-background-color=00000000',
    `--window-size=${size},${size}`,
    `--screenshot=${outPng}`,
    pathToFileURL(join(here, html)).href,
  ], { stdio: 'ignore' })
  const buf = readFileSync(outPng)
  const { w, h } = pngSize(buf)
  if (w !== size || h !== size) {
    throw new Error(`${html} @${size} 渲染尺寸异常：${w}x${h}`)
  }
  return buf
}

// ---- ICO 封装（PNG 载荷，Windows/现代浏览器均支持）----
function buildIco(pngs) {
  const count = pngs.length
  const header = Buffer.alloc(6)
  header.writeUInt16LE(0, 0)      // reserved
  header.writeUInt16LE(1, 2)      // type = icon
  header.writeUInt16LE(count, 4)  // count
  const entries = []
  let offset = 6 + count * 16
  for (const { buf, size } of pngs) {
    const e = Buffer.alloc(16)
    e.writeUInt8(size >= 256 ? 0 : size, 0)   // width (0 => 256)
    e.writeUInt8(size >= 256 ? 0 : size, 1)   // height
    e.writeUInt8(0, 2)                        // palette
    e.writeUInt8(0, 3)                        // reserved
    e.writeUInt16LE(1, 4)                     // planes
    e.writeUInt16LE(32, 6)                    // bpp
    e.writeUInt32LE(buf.length, 8)             // size
    e.writeUInt32LE(offset, 12)                // offset
    entries.push(e)
    offset += buf.length
  }
  return Buffer.concat([header, ...entries, ...pngs.map(p => p.buf)])
}

const out = []
// favicon / PWA 图标：圆角磁贴版
const icoSizes = [16, 32, 48]
const icoPngs = icoSizes.map(size => ({ size, buf: render('render-tile.html', size, join(work, `tile-${size}.png`)) }))
writeFileSync(join(staticDir, 'favicon.ico'), buildIco(icoPngs))
out.push('favicon.ico (16/32/48)')

// 矢量 favicon
copyFileSync(join(here, 'icon-tile.svg'), join(staticDir, 'favicon.svg'))
out.push('favicon.svg')

// 满幅方版：iOS/Android 自行裁切圆角，故不加圆角与留白
for (const [size, name] of [[180, 'apple-touch-icon.png'], [192, 'icon-192.png'], [512, 'icon-512.png']]) {
  const buf = render('render-square.html', size, join(work, `square-${size}.png`))
  writeFileSync(join(staticDir, name), buf)
  out.push(`${name} (${size})`)
}

// PWA manifest（App 端"添加到主屏幕"后即以此图标启动）
const manifest = {
  name: 'CampusSkill 校园技能工坊',
  short_name: 'CampusSkill',
  description: '校园技能互助服务市场 · 用户端与技能者端双身份工作台',
  start_url: '/user-app/',
  scope: '/user-app/',
  display: 'standalone',
  orientation: 'portrait',
  background_color: '#f4f7fb',
  theme_color: '#00AEEC',
  icons: [
    { src: '/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
    { src: '/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
    { src: '/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  ],
}
writeFileSync(join(staticDir, 'manifest.webmanifest'), JSON.stringify(manifest, null, 2) + '\n')
out.push('manifest.webmanifest')

console.log('图标已生成到 static/:')
for (const line of out) console.log('  -', line)
