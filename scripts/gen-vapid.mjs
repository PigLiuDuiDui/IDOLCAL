#!/usr/bin/env node
// ============================================================
// 生成 VAPID 密钥对（Web Push 用，RFC 8292）
//   node scripts/gen-vapid.mjs
// 输出：
//   - 控制台打印公钥/私钥（base64url）
//   - 写入 api/.vapid-keys（本地开发自动读取；生产用环境变量
//     IDOLCAL_VAPID_PUBLIC_KEY / IDOLCAL_VAPID_PRIVATE_KEY 覆盖）
// 密钥格式与 web-push 库一致：
//   公钥 = base64url(SEC1 65 字节 04||X||Y)
//   私钥 = base64url(32 字节标量 d)
// ============================================================
import { generateKeyPairSync } from 'node:crypto'
import { writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const { publicKey, privateKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' })

const pub = publicKey.export({ format: 'jwk' }) // x / y（base64url 32 字节）
const priv = privateKey.export({ format: 'jwk' }) // d（base64url 32 字节）

const X = Buffer.from(pub.x, 'base64url')
const Y = Buffer.from(pub.y, 'base64url')
const publicKeyB64u = Buffer.concat([Buffer.from([0x04]), X, Y]).toString('base64url')
const privateKeyB64u = priv.d

const outPath = path.join(__dirname, '..', 'api', '.vapid-keys')
writeFileSync(outPath, `${publicKeyB64u}\n${privateKeyB64u}\n`, 'utf8')

console.log('VAPID public key :', publicKeyB64u)
console.log('VAPID private key:', privateKeyB64u)
console.log(`已写入 ${outPath}（本地开发用；生产请配置环境变量）`)
