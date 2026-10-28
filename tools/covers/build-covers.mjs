// 生成技能类目本地封面占位图（离线可用，替代 picsum.photos 外链）
// 用法：node tools/covers/build-covers.mjs
import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(here, '../../campus-server/src/main/resources/static/assets/covers')
mkdirSync(outDir, { recursive: true })

const W = 640
const H = 400

// slug -> [类目名, 主色, 辅色, 快捷键标记]
const CATEGORIES = {
  ppt:        ['PPT 美化',  '#00AEEC', '#2E6BFF'],
  poster:     ['海报设计',  '#FB7299', '#FF9D6E'],
  video:      ['视频剪辑',  '#7B61FF', '#00AEEC'],
  photo:      ['摄影约拍',  '#FF8A3D', '#FFC53D'],
  code:       ['编程调试',  '#10B981', '#059669'],
  resume:     ['简历美化',  '#06B6D4', '#0EA5E9'],
  translate:  ['翻译润色',  '#6366F1', '#8B5CF6'],
  craft:      ['手工定制',  '#EC4899', '#F59E0B'],
  generic:    ['技能服务',  '#00AEEC', '#FB7299'],
  portfolio:  ['作品集',    '#3FC0F2', '#FB7299'],
}

function cover(name, c1, c2, label) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${c1}"/>
      <stop offset="1" stop-color="${c2}"/>
    </linearGradient>
  </defs>
  <rect width="${W}" height="${H}" fill="url(#g)"/>
  <g fill="#ffffff" opacity="0.14">
    <circle cx="548" cy="86" r="132"/>
    <circle cx="600" cy="330" r="74"/>
    <rect x="-70" y="268" width="330" height="330" rx="90" transform="rotate(24 95 433)"/>
  </g>
  <g transform="translate(56 250)" opacity="0.92">
    <text x="0" y="0" fill="#ffffff" font-size="52" font-weight="700"
          font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif">${name}</text>
    <rect x="0" y="20" width="72" height="6" rx="3" fill="#ffffff" opacity="0.85"/>
    <text x="0" y="58" fill="#ffffff" font-size="21" opacity="0.86"
          font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif">CampusSkill · ${label}</text>
  </g>
</svg>
`
}

const written = []
for (const [slug, [name, c1, c2]] of Object.entries(CATEGORIES)) {
  const file = join(outDir, `${slug}.svg`)
  writeFileSync(file, cover(name, c1, c2, '校园技能工坊'))
  written.push(`${slug}.svg`)
}

// 默认头像（用户 / 技能者资料未上传头像时使用）
const avatar = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">
  <defs>
    <linearGradient id="a" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#00AEEC"/>
      <stop offset="1" stop-color="#FB7299"/>
    </linearGradient>
  </defs>
  <rect width="200" height="200" rx="100" fill="url(#a)"/>
  <circle cx="100" cy="78" r="34" fill="#ffffff" opacity="0.95"/>
  <path d="M32 190c0-38 30-62 68-62s68 24 68 62z" fill="#ffffff" opacity="0.95"/>
</svg>
`
writeFileSync(join(outDir, 'avatar-default.svg'), avatar)
written.push('avatar-default.svg')

console.log(`封面占位图已生成到 static/assets/covers/（${written.length} 个）：`)
console.log('  ' + written.join(', '))
