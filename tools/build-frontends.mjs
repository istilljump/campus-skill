// 前端构建 + 产物同步脚本
// 用法：node tools/build-frontends.mjs
// 作用：分别构建 web-app / admin-web，并把 dist/ 同步进 campus-server 静态目录，
//       避免出现"源码改了但 jar 里仍是旧页面"的经典问题。
import { execFileSync } from 'node:child_process'
import { rmSync, cpSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const root = resolve(here, '..')
const staticDir = join(root, 'campus-server', 'src', 'main', 'resources', 'static')
const isWin = process.platform === 'win32'
const npm = isWin ? 'npm.cmd' : 'npm'

for (const app of ['web-app', 'admin-web']) {
  const dir = join(root, app)
  console.log(`\n== 构建 ${app} ==`)
  execFileSync(npm, ['run', 'build'], { cwd: dir, stdio: 'inherit', shell: isWin })
  const out = join(staticDir, app)
  rmSync(out, { recursive: true, force: true })
  cpSync(join(dir, 'dist'), out, { recursive: true })
  console.log(`== ${app}/dist 已同步到 static/${app}/`)
}
console.log('\n前端构建与同步完成。')
