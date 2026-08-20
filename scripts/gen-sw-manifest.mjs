#!/usr/bin/env node
/**
 * gen-sw-manifest.mjs — 构建后为 Service Worker 注入实际资源预缓存清单
 *
 * 用法（vite build 之后执行）：node scripts/gen-sw-manifest.mjs
 * 流程：
 *   1. 扫描 dist/ 下全部产物（index.html / icon-192.png / calendar.ics / assets/*.js|css）
 *   2. 读取 dist/sw.js（vite 从 public/ 原样拷贝）
 *   3. 将 /* __PRECACHE_ASSETS__ *\/ 占位符替换为实际资源清单 JSON
 *   4. 回写 dist/sw.js（同时把 CACHE_VERSION 升级为带内容哈希的版本号，
 *      保证每次构建 hash 变化都会触发缓存版本更新、自动清理旧缓存）
 */
import { readdirSync, readFileSync, writeFileSync, statSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'

const distDir = resolve(process.cwd(), 'dist')
const swPath = join(distDir, 'sw.js')

function collectFiles(dir) {
  const out = []
  for (const name of readdirSync(dir)) {
    const full = join(dir, name)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      out.push(...collectFiles(full))
    } else {
      out.push(full)
    }
  }
  return out
}

function toUrl(fullPath) {
  const rel = relative(distDir, fullPath).replace(/\\/g, '/')
  return '/' + rel
}

function buildAssetList() {
  const files = collectFiles(distDir).filter((f) => f !== swPath)
  const urls = files.map(toUrl)
  // 首页与静态资源确保在清单中
  for (const required of ['/index.html', '/icon-192.png', '/calendar.ics']) {
    if (!urls.includes(required)) urls.push(required)
  }
  // 构建产物 hash 汇总为版本号（assets 变化即版本变化）
  const hash = urls
    .join('|')
    .split('')
    .reduce((h, c) => ((h << 5) - h + c.charCodeAt(0)) | 0, 0)
  return { urls, version: `idolcal-v1-${Math.abs(hash).toString(36)}` }
}

const sw = readFileSync(swPath, 'utf-8')
const { urls, version } = buildAssetList()

if (!sw.includes('/* __PRECACHE_ASSETS__')) {
  console.error('[gen-sw-manifest] dist/sw.js 中未找到预缓存占位符，请确认 public/sw.js 模板包含 /* __PRECACHE_ASSETS__ */')
  process.exit(1)
}

const manifestJson = JSON.stringify(urls, null, 2)
const updated = sw
  .replace("const CACHE_VERSION = 'idolcal-v1'", `const CACHE_VERSION = '${version}'`)
  .replace('/* __PRECACHE_ASSETS__ 占位符会被构建脚本替换为实际 JS/CSS 清单 */', manifestJson)

writeFileSync(swPath, updated, 'utf-8')
console.log(`[gen-sw-manifest] 已注入 ${urls.length} 个预缓存资源 -> dist/sw.js（版本 ${version}）`)
