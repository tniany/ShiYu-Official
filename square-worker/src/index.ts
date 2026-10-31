import { inspectGitHubRepository, mergePublicCatalog, reviewTransition, validateMarketplaceManifest } from './marketplace.mjs';
import { developerMarketPage, marketplaceDocsPage } from './marketplace-pages.mjs';
import { withMarketplaceSettingsDocs } from './marketplace-settings-docs.mjs';
import { WINDOWS_RELEASE, streamWindows } from "./windows-download";

type D1Database = any;
interface Env {
  DB: D1Database;
  USAGE_DB?: D1Database;
  ASSETS: { fetch(request: Request): Promise<Response> };
  AUTH_ISSUER: string;
  AUTH_CLIENT_ID: string;
  AUTH_CLIENT_SECRET?: string;
  AUTH_SERVICE?: { fetch(request: Request): Promise<Response> };
  ADMIN_ROUTE: string;
  ADMIN_SECRET: string;
  GITHUB_TOKEN?: string;
  APP_ORIGIN?: string;
}

const json = (value: unknown, status = 200) => Response.json(value, { status, headers: { 'cache-control': 'no-store' } });
const id = () => crypto.randomUUID();
const now = () => Math.floor(Date.now() / 1000);
function redirect(location: string, cookies: string[] = []) { const headers = new Headers({ location, 'cache-control': 'no-store' }); cookies.forEach(value => headers.append('set-cookie', value)); return new Response(null, { status: 302, headers }); }
function html(body: string, status = 200, cookies: string[] = []) { const headers = new Headers({ 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store', 'x-frame-options': 'DENY', 'referrer-policy': 'no-referrer' }); cookies.forEach(value => headers.append('set-cookie', value)); return new Response(body, { status, headers }); }
function safeReturnPath(url: URL, value = url.searchParams.get('return_to') || '/') { try { const target = new URL(value, url.origin); return target.origin === url.origin ? `${target.pathname}${target.search}${target.hash}` : '/'; } catch { return '/'; } }
function authResultPage(origin: string, returnTo: string, ok: boolean, message: string) {
  const payload = JSON.stringify({ type: 'shiyu-auth-complete', ok, message }).replace(/</g, '\\u003c');
  const target = JSON.stringify(returnTo).replace(/</g, '\\u003c');
  const targetOrigin = JSON.stringify(origin).replace(/</g, '\\u003c');
  const title = ok ? '登录成功' : '登录失败';
  const detail = ok ? '云端空间已连接，正在返回时遇。' : message;
  return `<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title} · 时遇</title><style>html{color-scheme:light}*{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;padding:24px;background:#fdf6f8;color:#3b2b33;font:15px/1.7 system-ui,sans-serif}.panel{width:min(420px,100%);padding:28px;border:1px solid #f4e3e9;border-radius:8px;background:#fff;box-shadow:0 18px 50px rgba(160,90,120,.15);text-align:center}.icon{display:grid;place-items:center;width:52px;height:52px;margin:0 auto 14px;border-radius:50%;background:${ok ? '#fdeaf0' : '#fdeaec'};color:${ok ? '#c23e63' : '#e05c6e'};font-size:24px}h1{margin:0;font-size:22px}p{margin:8px 0 20px;color:#8a7280}a{display:inline-flex;align-items:center;justify-content:center;min-height:42px;padding:0 18px;border-radius:8px;background:#ec6f92;color:#fff;text-decoration:none;font-weight:700}</style><main class="panel"><div class="icon">${ok ? '✓' : '!'}</div><h1>${title}</h1><p>${detail}</p><a href="${returnTo.replace(/&/g, '&amp;').replace(/"/g, '&quot;')}">返回时遇</a></main><script>const payload=${payload};const target=${target};try{if(window.opener&&!window.opener.closed){window.opener.postMessage(payload,${targetOrigin});setTimeout(()=>window.close(),180)}else if(payload.ok){window.location.replace(target)}}catch{if(payload.ok)window.location.replace(target)}</script></html>`;
}
function cors(request: Request, response: Response, env: Env) { const headers = new Headers(response.headers); const origin = request.headers.get('origin'); if (origin && (!env.APP_ORIGIN || origin === env.APP_ORIGIN)) { headers.set('access-control-allow-origin', origin); headers.set('access-control-allow-credentials', 'true'); } headers.set('access-control-allow-headers', 'content-type, authorization, x-shiyu-app'); headers.set('access-control-allow-methods', 'GET,POST,PATCH,DELETE,OPTIONS'); return new Response(response.body, { status: response.status, headers }); }
function cookie(request: Request, name: string) { return (request.headers.get('cookie') || '').split(';').map(v => v.trim()).find(v => v.startsWith(`${name}=`))?.slice(name.length + 1) || ''; }
function bearer(request: Request) { const value = request.headers.get('authorization') || ''; return value.startsWith('Bearer ') ? value.slice(7).trim() : ''; }
function authFetch(env: Env, path: string, init?: RequestInit) { const request = new Request(`${env.AUTH_ISSUER.replace(/\/$/, '')}${path}`, init); return env.AUTH_SERVICE ? env.AUTH_SERVICE.fetch(request) : fetch(request); }
async function auth(request: Request, env: Env) { const token = bearer(request) || cookie(request, 'shiyu_square_session'); if (!token) return null; const response = await authFetch(env, '/oauth/userinfo', { headers: { authorization: `Bearer ${token}` } }); if (!response.ok) return null; const user = await response.json() as any; const qqId = String(user.qq_id || ''); return { id: String(user.sub || qqId || ''), name: String(user.nickname || qqId || '用户'), qqId, avatar: qqId ? `https://q1.qlogo.cn/g?b=qq&nk=${encodeURIComponent(qqId)}&s=640` : '' }; }
async function admin(request: Request, env: Env) { const user = await auth(request, env); const key = request.headers.get('x-square-admin') || ''; return user && env.ADMIN_SECRET && key === env.ADMIN_SECRET ? user : null; }
async function reportWebLogin(env: Env, accessToken: string) {
  try {
    const response = await authFetch(env, '/oauth/userinfo', { headers: { authorization: `Bearer ${accessToken}` } });
    if (!response.ok) return;
    const user = await response.json() as any;
    const userId = String(user.sub || user.qq_id || '').trim();
    if (!userId) return;
    await fetch('https://shiy.tniay.top/api/open', { method: 'POST', headers: { 'content-type': 'application/json', 'x-shiyu-app': 'web-auth' }, body: JSON.stringify({ event: 'web_login', userId }) });
  } catch (_) {}
}
function base64url(bytes: ArrayBuffer | Uint8Array) { const data = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes); let binary = ''; for (const value of data) binary += String.fromCharCode(value); return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, ''); }
async function pkceChallenge(verifier: string) { return base64url(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))); }
function cleanText(value: unknown, max: number) { return String(value ?? '').trim().slice(0, max); }
const APK_094_SIZE = 86306680;
const APK_094_PARTS = 5;
const APK_DOWNLOAD_DEDUPE_MS = 10 * 60 * 1000;
function hongKongDay(timestamp: number) { return new Date(timestamp + 8 * 60 * 60 * 1000).toISOString().slice(0, 10); }
async function sha256Hex(value: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('');
}
async function recordApkDownload(request: Request, env: Env, version: string) {
  if (!env.USAGE_DB) return;
  const timestamp = Date.now();
  const ip = request.headers.get('cf-connecting-ip') || request.headers.get('x-forwarded-for') || 'unknown';
  const userAgent = request.headers.get('user-agent') || 'unknown';
  const fingerprint = await sha256Hex(
    [ip, userAgent, version].join('|')
  );
  const firstPass = await env.USAGE_DB.batch([
    env.USAGE_DB.prepare('DELETE FROM download_events WHERE created_at < ?').bind(timestamp - APK_DOWNLOAD_DEDUPE_MS),
    env.USAGE_DB.prepare('INSERT OR IGNORE INTO download_events (fingerprint, created_at) VALUES (?, ?)').bind(fingerprint, timestamp)
  ]);
  if (!Number(firstPass?.[1]?.meta?.changes || 0)) return;
  const day = hongKongDay(timestamp);
  await env.USAGE_DB.batch([
    env.USAGE_DB.prepare("INSERT INTO counters (name, value) VALUES ('apk_downloads', 1) ON CONFLICT(name) DO UPDATE SET value = value + 1"),
    env.USAGE_DB.prepare('INSERT INTO daily_downloads (day, count) VALUES (?, 1) ON CONFLICT(day) DO UPDATE SET count = count + 1').bind(day),
    env.USAGE_DB.prepare('INSERT INTO version_downloads (version, count) VALUES (?, 1) ON CONFLICT(version) DO UPDATE SET count = count + 1').bind(version)
  ]);
}
async function streamApk094(request: Request, env: Env) {
  const headers = new Headers({
    'content-type': 'application/vnd.android.package-archive',
    'content-length': String(APK_094_SIZE),
    'content-disposition': 'attachment; filename="shiyu-0.9.4.apk"',
    'cache-control': 'public, max-age=3600',
    'accept-ranges': 'none'
  });
  if (request.method === 'HEAD') return new Response(null, { headers });
  let part = 0;
  let counted = false;
  const body = new ReadableStream<Uint8Array>({
    async pull(controller) {
      if (part >= APK_094_PARTS) { controller.close(); return; }
      const path = `/downloads/shiyu-0.9.4.apk.part${String(part + 1).padStart(2, '0')}`;
      const response = await env.ASSETS.fetch(new Request(new URL(path, request.url)));
      if (!response.ok || !response.body) {
        controller.error(new Error(`APK part ${part + 1} unavailable`));
        return;
      }
      const reader = response.body.getReader();
      try {
        while (true) {
          const chunk = await reader.read();
          if (chunk.done) break;
          controller.enqueue(chunk.value);
          await new Promise<void>(resolve => setTimeout(resolve, 0));
        }
      } finally { reader.releaseLock(); }
      part += 1;
      if (part >= APK_094_PARTS) {
        if (!counted) {
          counted = true;
          try { await recordApkDownload(request, env, '0.9.4'); }
          catch (error) { console.error('APK download tracking failed', error); }
        }
        controller.close();
      }
    },
    cancel() {}
  });
  return new Response(body, { headers });
}
function adminPage() {
  const html = `<!doctype html><meta charset="utf-8"><meta name="robots" content="noindex,nofollow"><title>时遇角色卡广场管理</title><style>body{font:14px system-ui;max-width:1000px;margin:30px auto;padding:0 16px;background:#f6f8f5;color:#17211f}input,button{padding:9px;margin:4px}table{width:100%;border-collapse:collapse;background:white}td,th{padding:8px;border-bottom:1px solid #ddd;text-align:left}button{cursor:pointer}</style><h1>时遇角色卡广场管理</h1><p>当前页面要求同时具备时遇登录会话和后台密钥。</p><input id="key" type="password" placeholder="后台密钥" autocomplete="off"><button id="load">加载审核列表</button><table><thead><tr><th>角色</th><th>作者</th><th>状态</th><th>操作</th></tr></thead><tbody id="rows"></tbody></table><script>
const esc = value => String(value ?? '').replace(/[&<>\"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[ch]));
async function api(path, opt = {}) { opt.headers = {...(opt.headers || {}), 'content-type':'application/json', 'x-square-admin':document.querySelector('#key').value}; const response = await fetch(path, opt); let data = {}; try { data = await response.json(); } catch {} if (!response.ok) throw Error(data.message || data.error || response.status); return data; }
async function load() { try { const data = await api('/api/square/admin'); document.querySelector('#rows').innerHTML = (data.cards || []).map(card => '<tr><td>'+esc(card.title)+'</td><td>'+esc(card.owner_name)+'</td><td>'+esc(card.status)+'</td><td><button data-id="'+esc(card.id)+'" data-status="approved">通过</button><button data-id="'+esc(card.id)+'" data-status="hidden">隐藏</button><button data-id="'+esc(card.id)+'" data-status="rejected">拒绝</button></td></tr>').join('') || '<tr><td colspan="4">暂无角色卡</td></tr>'; } catch (error) { alert(error.message); } }
document.querySelector('#load').addEventListener('click', load);
document.querySelector('#rows').addEventListener('click', async event => { const button = event.target.closest('button[data-id]'); if (!button) return; try { await api('/api/square/admin/cards/'+encodeURIComponent(button.dataset.id), {method:'PATCH', body:JSON.stringify({status:button.dataset.status})}); await load(); } catch (error) { alert(error.message); } });
</script>`;
  return new Response(html, { headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store' } });
}
function shiyAdminPage() {
  const html = `<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>时遇管理后台</title><style>
body{margin:0;min-height:100vh;background:#f4f6fa;color:#1c2430;font:14px/1.65 system-ui,'Segoe UI','Microsoft YaHei',sans-serif}
header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px 22px;background:#fff;border-bottom:1px solid #e3e8f0;position:sticky;top:0;z-index:5}
header h1{margin:0;font-size:17px}header .who{color:#68758a;font-size:12px}
main{max-width:1080px;margin:20px auto 60px;padding:0 16px}
button{cursor:pointer;border:1px solid #ccd4e0;background:#fff;color:#1c2430;border-radius:8px;padding:7px 14px;font-size:13px}
button:hover{border-color:#9fb0cc}button.primary{background:#2f6fed;border-color:#2f6fed;color:#fff}button.primary:hover{background:#2861d4}
button.danger{color:#c23a3a;border-color:#e2b6b6}button.danger:hover{background:#fdf1f1}
button.small{padding:4px 10px;font-size:12px;border-radius:6px}
input,select,textarea{border:1px solid #ccd4e0;border-radius:8px;padding:8px 10px;font-size:13px;font-family:inherit;background:#fff}
textarea{width:100%;box-sizing:border-box;resize:vertical}
.card{background:#fff;border:1px solid #e3e8f0;border-radius:12px;padding:16px;margin-bottom:14px}
.gate{max-width:420px;margin:60px auto;text-align:center;padding:28px}
.gate input{width:100%;box-sizing:border-box;margin:14px 0}
.tabs{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:14px}
.tabs button{border-radius:999px}
.tabs button.on{background:#1c2430;color:#fff;border-color:#1c2430}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin-bottom:14px}
.stat{background:#fff;border:1px solid #e3e8f0;border-radius:12px;padding:14px}
.stat b{display:block;font-size:24px;margin-top:4px}
.stat span{color:#68758a;font-size:12px}
table{width:100%;border-collapse:collapse;background:#fff;border:1px solid #e3e8f0;border-radius:12px;overflow:hidden}
th,td{padding:9px 10px;border-bottom:1px solid #eef1f6;text-align:left;font-size:13px;vertical-align:top}
th{background:#f7f9fc;color:#68758a;font-weight:600;font-size:12px}
tr:last-child td{border-bottom:none}
td .ops{display:flex;gap:6px;flex-wrap:wrap}
.muted{color:#8a97ab}.tag{display:inline-block;padding:1px 8px;border-radius:999px;font-size:12px}
.tag.pending{background:#fdf3e2;color:#9a6b16}.tag.approved{background:#e8f6ec;color:#217a3c}.tag.rejected{background:#fdeaea;color:#b03333}.tag.hidden{background:#eef0f4;color:#5b6676}.tag.unread{background:#fdeaea;color:#b03333}.tag.reading{background:#fdf3e2;color:#9a6b16}.tag.resolved{background:#e8f6ec;color:#217a3c}.tag.archived{background:#eef0f4;color:#5b6676}.tag.ignored{background:#eceff3;color:#596579}
.bars{display:flex;align-items:flex-end;gap:6px;height:110px;padding:8px 4px 0}
.bar{flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;min-width:0}
.bar i{display:block;width:100%;max-width:34px;background:#7ea4f5;border-radius:4px 4px 0 0;min-height:2px}
.bar span{font-size:10px;color:#8a97ab;white-space:nowrap}
.toolbar{display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin-bottom:12px}
.modal{position:fixed;inset:0;background:rgba(20,26,36,.45);display:none;align-items:center;justify-content:center;padding:20px;z-index:20}
.modal.open{display:flex}
.modal .box{background:#fff;border-radius:12px;max-width:760px;width:100%;max-height:82vh;display:flex;flex-direction:column}
.modal .box h3{margin:0;padding:14px 16px;border-bottom:1px solid #eef1f6;display:flex;justify-content:space-between;align-items:center}
.modal pre{margin:0;padding:14px 16px;overflow:auto;font-size:12px;white-space:pre-wrap;word-break:break-all}
.form{display:grid;gap:10px;margin-bottom:16px}
.form label{display:grid;gap:5px;font-size:12px;color:#68758a}
.err{color:#c23a3a;margin:8px 0}
.ok{color:#217a3c;margin:8px 0}
@media (max-width:640px){th,td{padding:7px 6px}.hide-sm{display:none}}
</style><header><h1>时遇管理后台</h1><div class="who" id="who"></div></header><main>
<div class="card gate" id="gate-login" style="display:none"><h2 style="margin:0 0 8px">请先登录</h2><p class="muted">管理后台需要时遇账号登录与管理密钥。</p><button class="primary" onclick="location.href='/auth/login?return_to=%2Fshiyadmin'">使用时遇账号登录</button></div>
<div class="card gate" id="gate-key" style="display:none"><h2 style="margin:0 0 8px">输入管理密钥</h2><p class="muted">需要账号登录 + 管理密钥双重校验，密钥只保存在本机浏览器。</p><input id="key" type="password" placeholder="管理密钥" autocomplete="off"><div id="keymsg"></div><button class="primary" id="keybtn">进入后台</button></div>
<div id="app" style="display:none">
  <div class="tabs"><button data-tab="stats" class="on">数据统计</button><button data-tab="app-usage">App 使用统计</button><button data-tab="feedback">用户反馈</button><button data-tab="cards">角色卡</button><button data-tab="users">用户数据</button><button data-tab="comments">评论</button><button data-tab="ann">公告管理</button><button data-tab="market">市场审核</button></div>
  <div id="content"></div>
</div>
</main>
<div class="modal" id="modal"><div class="box"><h3><span id="modal-title">详情</span><button class="small" onclick="closeModal()">关闭</button></h3><pre id="modal-body"></pre></div></div>
<script>
var KEY = localStorage.getItem('shiyadmin_key') || '';
var TAB = 'stats';
var editingAnn = '';
function $(s){return document.querySelector(s)}
function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]})}
function fmtTime(sec){if(!sec)return '—';return new Date(sec*1000).toLocaleString('zh-CN',{hour12:false})}
function fmtBytes(n){if(!n)return '0 B';var u=['B','KB','MB','GB'],i=0;while(n>=1024&&i<u.length-1){n/=1024;i++}return n.toFixed(n>=100?0:1)+' '+u[i]}
function api(path,opt){opt=opt||{};opt.headers=Object.assign({'content-type':'application/json','x-square-admin':KEY},opt.headers||{});return fetch(path,opt).then(function(r){return r.json().catch(function(){return {}}).then(function(d){if(r.status===401)throw new Error(d.message||'管理密钥无效或登录已过期');if(!r.ok)throw new Error(d.message||d.error||('HTTP '+r.status));return d})})}
function show(which){$('#gate-login').style.display=which==='login'?'':'none';$('#gate-key').style.display=which==='key'?'':'none';$('#app').style.display=which==='app'?'':'none'}
function init(){fetch('/auth/me').then(function(r){return r.json()}).then(function(d){if(!d.loggedIn){show('login');return}$('#who').innerHTML=esc(d.user.name)+' · '+(d.user.qqId||'')+' · <button class="small" onclick="logout()">退出登录</button>';if(KEY)tryKey();else show('key')})}
function logout(){fetch('/auth/logout',{method:'POST'}).then(function(){location.reload()})}
function tryKey(){KEY=$('#key')?$('#key').value.trim()||KEY:KEY;api('/api/shiyadmin/session').then(function(d){KEY=($('#key')&&$('#key').value.trim())||KEY;localStorage.setItem('shiyadmin_key',KEY);show('app');render()}).catch(function(e){show('key');$('#keymsg').innerHTML='<div class="err">'+esc(e.message)+'</div>'})}
function render(){document.querySelectorAll('.tabs button').forEach(function(b){b.classList.toggle('on',b.dataset.tab===TAB)});var c=$('#content');c.innerHTML='<div class="card muted">加载中…</div>';if(TAB==='stats')renderStats();else if(TAB==='app-usage')renderAppUsage();else if(TAB==='feedback')renderFeedback();else if(TAB==='cards')renderCards();else if(TAB==='users')renderUsers();else if(TAB==='comments')renderComments();else if(TAB==='market')renderMarketReviews();else renderAnn()}
function fail(c,e){c.innerHTML='<div class="card"><div class="err">'+esc(e.message)+'</div><button onclick="render()">重试</button></div>'}
function renderStats(){var c=$('#content');api('/api/shiyadmin/stats').then(function(d){var days=[];var map={};(d.cards.trend||[]).forEach(function(t){map[t.d]=t.n});for(var i=13;i>=0;i--){var dt=new Date(Date.now()-i*86400000);var key=dt.getFullYear()+'-'+String(dt.getMonth()+1).padStart(2,'0')+'-'+String(dt.getDate()).padStart(2,'0');days.push({d:key,n:map[key]||0})}var max=1;days.forEach(function(x){if(x.n>max)max=x.n});var bars='';days.forEach(function(x){bars+='<div class="bar" title="'+x.d+'：'+x.n+' 张"><i style="height:'+Math.round(x.n/max*88+4)+'px"></i><span>'+(x.n||'')+'</span><span class="muted">'+x.d.slice(5)+'</span></div>'});var syncs=(d.recentSyncs||[]).map(function(s){return '<tr><td class="muted">'+esc(s.user_id)+'</td><td>'+fmtTime(s.updated_at)+'</td><td>'+fmtBytes(s.bytes)+'</td></tr>'}).join('');var cards=(d.recentCards||[]).map(function(s){return '<tr><td>'+esc(s.title)+'</td><td>'+esc(s.owner_name)+'</td><td><span class="tag '+s.status+'">'+s.status+'</span></td><td class="muted">'+fmtTime(s.created_at)+'</td></tr>'}).join('');c.innerHTML='<div class="stats"><div class="stat"><span>云同步用户</span><b>'+d.users.count+'</b></div><div class="stat"><span>云端存储总量</span><b>'+fmtBytes(d.users.bytes)+'</b></div><div class="stat"><span>角色卡总数</span><b>'+d.cards.total+'</b><span>待审 '+d.cards.pending+' · 通过 '+d.cards.approved+' · 拒绝 '+d.cards.rejected+' · 隐藏 '+d.cards.hidden+'</span></div><div class="stat"><span>评论总数</span><b>'+d.comments+'</b></div><div class="stat"><span>公告数量</span><b>'+d.announcements+'</b></div></div><div class="card"><b>近 14 天角色卡发布趋势</b><div class="bars">'+bars+'</div></div><div class="card"><b>最近同步的用户</b><table><thead><tr><th>用户</th><th>最近同步</th><th>占用</th></tr></thead><tbody>'+(syncs||'<tr><td colspan="3" class="muted">暂无数据</td></tr>')+'</tbody></table></div><div class="card"><b>最新角色卡</b><table><thead><tr><th>标题</th><th>作者</th><th>状态</th><th>提交时间</th></tr></thead><tbody>'+(cards||'<tr><td colspan="4" class="muted">暂无数据</td></tr>')+'</tbody></table></div>'}).catch(function(e){fail(c,e)})}
function renderAppUsage(){var c=$('#content');api('/api/shiyadmin/app-usage').then(function(d){var metric=function(label,value){return '<div class="stat"><span>'+label+'</span><b>'+Number(value||0).toLocaleString()+'</b></div>'};var dayRows=(d.daily||[]).map(function(x){return '<tr><td>'+esc(x.day)+'</td><td>'+Number(x.app_opens||0).toLocaleString()+'</td><td>'+Number(x.apk_downloads||0).toLocaleString()+'</td><td>'+Number(x.web_visits||0).toLocaleString()+'</td><td>'+Number(x.web_logins||0).toLocaleString()+'</td></tr>'}).join('');var demoRows=(d.demoDaily||[]).map(function(x){return '<tr><td>'+esc(x.day)+'</td><td>'+Number(x.app_opens||0).toLocaleString()+'</td><td>'+Number(x.apk_downloads||0).toLocaleString()+'</td><td>'+Number(x.web_visits||0).toLocaleString()+'</td><td>'+Number(x.web_logins||0).toLocaleString()+'</td><td>'+esc(x.note||'模拟补录')+'</td></tr>'}).join('');var versionRows=(d.versions||[]).map(function(x){return '<tr><td>'+esc(x.version)+'</td><td>'+Number(x.app_opens||0).toLocaleString()+'</td><td>'+Number(x.apk_downloads||0).toLocaleString()+'</td></tr>'}).join('');c.innerHTML='<div class="stats">'+metric('App 累计启动',d.totals.app_opens)+metric('APK 累计下载',d.totals.apk_downloads)+metric('网页访问',d.totals.web_visits)+metric('网页登录',d.totals.web_logins)+metric('去重登录用户',d.totals.login_people)+'</div><div class="card"><b>完整历史</b><div style="overflow:auto"><table><thead><tr><th>日期</th><th>App 启动</th><th>APK 下载</th><th>网页访问</th><th>网页登录</th></tr></thead><tbody>'+(dayRows||'<tr><td colspan="5" class="muted">暂无数据</td></tr>')+'</tbody></table></div></div><div class="card"><b>模拟补录（不计入真实累计）</b><p class="muted" style="margin:8px 0">以下数据仅用于填补历史展示，不代表真实访问、启动或下载记录。</p><div style="overflow:auto"><table><thead><tr><th>日期</th><th>App 启动</th><th>APK 下载</th><th>网页访问</th><th>网页登录</th><th>标记</th></tr></thead><tbody>'+(demoRows||'<tr><td colspan="6" class="muted">暂无模拟数据</td></tr>')+'</tbody></table></div></div><div class="card"><b>完整版本统计</b><div style="overflow:auto"><table><thead><tr><th>版本</th><th>App 启动</th><th>APK 下载</th></tr></thead><tbody>'+(versionRows||'<tr><td colspan="3" class="muted">暂无数据</td></tr>')+'</tbody></table></div></div>'}).catch(function(e){fail(c,e)})}
function renderFeedback(){var c=$('#content');api('/api/shiyadmin/feedback').then(function(d){var category={suggestion:'功能建议',bug:'问题反馈',experience:'体验意见',other:'其他'};var statusName={unread:'未处理',reading:'处理中',resolved:'已解决',archived:'已归档',ignored:'忽略'};var rows=(d.feedback||[]).map(function(x){var status=x.status||'unread';return '<tr><td><span class="tag '+status+'">'+esc(statusName[status]||status)+'</span><div class="muted" style="font-size:11px">'+esc(category[x.category]||x.category||'其他')+'</div></td><td><div style="white-space:pre-wrap;max-width:520px">'+esc(x.content)+'</div><div class="muted" style="font-size:11px">'+esc(x.source||'web')+' · '+esc(x.app_version||'unknown')+(x.contact?' · '+esc(x.contact):'')+' · '+esc(x.created_at||'')+'</div>'+(x.diagnostics?'<details><summary>诊断日志</summary><pre>'+esc(x.diagnostics)+'</pre></details>':'')+'</td><td><select data-feedback-status="'+esc(x.id)+'"><option value="unread"'+(status==='unread'?' selected':'')+'>未处理</option><option value="reading"'+(status==='reading'?' selected':'')+'>处理中</option><option value="resolved"'+(status==='resolved'?' selected':'')+'>已解决</option><option value="ignored"'+(status==='ignored'?' selected':'')+'>忽略</option><option value="archived"'+(status==='archived'?' selected':'')+'>已归档</option></select></td></tr>'}).join('');c.innerHTML='<div class="card"><div class="toolbar"><b>最近反馈</b><span class="muted">共 '+(d.feedback||[]).length+' 条，最多显示 200 条</span></div><div style="overflow:auto"><table><thead><tr><th>状态 / 类型</th><th>内容</th><th>处理</th></tr></thead><tbody>'+(rows||'<tr><td colspan="3" class="muted">暂无反馈</td></tr>')+'</tbody></table></div></div>';c.querySelectorAll('[data-feedback-status]').forEach(function(select){select.onchange=function(){api('/api/shiyadmin/feedback/'+encodeURIComponent(select.dataset.feedbackStatus),{method:'PATCH',body:JSON.stringify({status:select.value})}).catch(function(e){alert(e.message);renderFeedback()})}})}).catch(function(e){fail(c,e)})}
var cardFilter={status:'',q:''};
function renderCards(){var c=$('#content');var qs='?limit=200'+(cardFilter.status?'&status='+cardFilter.status:'')+(cardFilter.q?'&q='+encodeURIComponent(cardFilter.q):'');api('/api/shiyadmin/cards'+qs).then(function(d){var rows=(d.cards||[]).map(function(x){return '<tr><td><b>'+esc(x.title)+'</b><div class="muted" style="font-size:12px">'+esc((x.description||'').slice(0,60))+'</div></td><td>'+esc(x.owner_name)+'<div class="muted hide-sm" style="font-size:11px">'+esc(x.owner_id)+'</div></td><td><span class="tag '+x.status+'">'+x.status+'</span></td><td class="muted hide-sm">'+fmtTime(x.created_at)+'</td><td><div class="ops"><button class="small" data-act="approve" data-id="'+x.id+'">通过</button><button class="small" data-act="hide" data-id="'+x.id+'">隐藏</button><button class="small" data-act="reject" data-id="'+x.id+'">拒绝</button><button class="small" data-act="view" data-id="'+x.id+'">查看</button><button class="small danger" data-act="delcard" data-id="'+x.id+'">删除</button></div></td></tr>'}).join('');c.innerHTML='<div class="card"><div class="toolbar"><select id="f-status"><option value="">全部状态</option><option value="pending"'+(cardFilter.status==='pending'?' selected':'')+'>待审</option><option value="approved"'+(cardFilter.status==='approved'?' selected':'')+'>已通过</option><option value="rejected"'+(cardFilter.status==='rejected'?' selected':'')+'>已拒绝</option><option value="hidden"'+(cardFilter.status==='hidden'?' selected':'')+'>已隐藏</option></select><input id="f-q" placeholder="搜索标题 / 作者 / 简介" value="'+esc(cardFilter.q)+'" style="flex:1;min-width:160px"><button id="f-go" class="primary">筛选</button><span class="muted">共 '+(d.cards||[]).length+' 张</span></div><table><thead><tr><th>标题</th><th>作者</th><th>状态</th><th class="hide-sm">提交时间</th><th>操作</th></tr></thead><tbody>'+(rows||'<tr><td colspan="5" class="muted">没有符合条件的角色卡</td></tr>')+'</tbody></table></div>';$('#f-go').onclick=function(){cardFilter.status=$('#f-status').value;cardFilter.q=$('#f-q').value.trim();renderCards()}}).catch(function(e){fail(c,e)})}
function renderUsers(){var c=$('#content');api('/api/shiyadmin/users').then(function(d){var rows=(d.users||[]).map(function(x){return '<tr><td class="muted">'+esc(x.user_id)+'</td><td>'+fmtTime(x.updated_at)+'</td><td>'+fmtBytes(x.bytes)+'</td><td><button class="small danger" data-act="deluser" data-id="'+esc(x.user_id)+'">删除数据</button></td></tr>'}).join('');c.innerHTML='<div class="card"><div class="toolbar"><input id="u-q" placeholder="搜索用户 ID" style="flex:1;min-width:160px"><button id="u-go" class="primary">搜索</button><span class="muted">共 '+(d.users||[]).length+' 个（最多显示 200）</span></div><table><thead><tr><th>用户</th><th>最近同步</th><th>占用</th><th>操作</th></tr></thead><tbody>'+(rows||'<tr><td colspan="4" class="muted">暂无云同步用户</td></tr>')+'</tbody></table></div>';$('#u-go').onclick=function(){var q=$('#u-q').value.trim();api('/api/shiyadmin/users'+(q?'?q='+encodeURIComponent(q):'')).then(function(d2){c.innerHTML='';renderUsersRefresh(d2)}).catch(function(e){fail(c,e)})}}).catch(function(e){fail(c,e)})}
function renderUsersRefresh(d){var rows=(d.users||[]).map(function(x){return '<tr><td class="muted">'+esc(x.user_id)+'</td><td>'+fmtTime(x.updated_at)+'</td><td>'+fmtBytes(x.bytes)+'</td><td><button class="small danger" data-act="deluser" data-id="'+esc(x.user_id)+'">删除数据</button></td></tr>'}).join('');$('#content').innerHTML='<div class="card"><div class="toolbar"><input id="u-q" placeholder="搜索用户 ID" style="flex:1;min-width:160px"><button id="u-go" class="primary">搜索</button><span class="muted">共 '+(d.users||[]).length+' 个（最多显示 200）</span></div><table><thead><tr><th>用户</th><th>最近同步</th><th>占用</th><th>操作</th></tr></thead><tbody>'+(rows||'<tr><td colspan="4" class="muted">没有匹配的用户</td></tr>')+'</tbody></table></div>';$('#u-go').onclick=function(){var q=$('#u-q').value.trim();api('/api/shiyadmin/users'+(q?'?q='+encodeURIComponent(q):'')).then(renderUsersRefresh).catch(function(e){fail($('#content'),e)})}}
function renderComments(){var c=$('#content');api('/api/shiyadmin/comments').then(function(d){var rows=(d.comments||[]).map(function(x){return '<tr><td>'+esc(x.card_title)+'<div class="muted" style="font-size:11px">'+esc(x.user_name)+' · '+fmtTime(x.created_at)+'</div></td><td>'+esc((x.content||'').slice(0,120))+'</td><td><button class="small danger" data-act="delcomment" data-id="'+x.id+'">删除</button></td></tr>'}).join('');c.innerHTML='<div class="card"><b>最近评论（最多 100 条）</b><table><thead><tr><th>角色卡 / 用户</th><th>内容</th><th>操作</th></tr></thead><tbody>'+(rows||'<tr><td colspan="3" class="muted">暂无评论</td></tr>')+'</tbody></table></div>'}).catch(function(e){fail(c,e)})}
var annDraft=null;
function renderAnn(){var c=$('#content');api('/api/shiyadmin/announcements').then(function(d){var rows=(d.announcements||[]).map(function(x){return '<tr><td><b>'+esc(x.title)+'</b>'+(x.pinned?' <span class="tag approved">置顶</span>':'')+(x.type==='always'?' <span class="tag pending">每次显示</span>':' <span class="tag hidden">一次性</span>')+'<div class="muted" style="font-size:12px;white-space:pre-wrap">'+esc((x.content||'').slice(0,160))+'</div><div class="muted" style="font-size:11px">更新于 '+fmtTime(x.updated_at)+'</div></td><td><div class="ops"><button class="small" data-act="editann" data-id="'+x.id+'">编辑</button><button class="small" data-act="pinann" data-id="'+x.id+'">'+(x.pinned?'取消置顶':'置顶')+'</button><button class="small danger" data-act="delann" data-id="'+x.id+'">删除</button></div></td></tr>'}).join('');c.innerHTML='<div class="card"><b>'+(editingAnn?'编辑公告':'发布新公告')+'</b><div class="form" style="margin-top:10px"><label>标题<input id="ann-title" maxlength="120" placeholder="公告标题" value="'+esc(annDraft?annDraft.title:'')+'"></label><label>内容<textarea id="ann-content" rows="5" maxlength="5000" placeholder="公告正文，会展示在网页版顶部">'+esc(annDraft?annDraft.content:'')+'</textarea></label><label>显示类型<select id="ann-type"><option value="once"'+(annDraft&&annDraft.type==='always'?'':' selected')+'>一次性（用户关闭后不再显示）</option><option value="always"'+(annDraft&&annDraft.type==='always'?' selected':'')+'>每次打开都显示（可手动关闭当次）</option></select></label><label style="flex-direction:row;align-items:center;gap:8px;display:flex"><input type="checkbox" id="ann-pinned"'+(annDraft&&annDraft.pinned?' checked':'')+'> 置顶（置顶公告排在最前）</label><div class="toolbar"><button class="primary" id="ann-save">'+(editingAnn?'保存修改':'发布公告')+'</button>'+(editingAnn?'<button id="ann-cancel">取消编辑</button>':'')+'</div></div></div><div class="card"><b>已发布公告</b><table><thead><tr><th>公告</th><th>操作</th></tr></thead><tbody>'+(rows||'<tr><td colspan="2" class="muted">还没有公告</td></tr>')+'</tbody></table></div>';$('#ann-save').onclick=function(){var title=$('#ann-title').value.trim(),content=$('#ann-content').value,pinned=$('#ann-pinned').checked,type=$('#ann-type').value;if(!title)return alert('标题不能为空');var body=JSON.stringify({title:title,content:content,pinned:pinned,type:type});var req=editingAnn?api('/api/shiyadmin/announcements/'+editingAnn,{method:'PATCH',body:body}):api('/api/shiyadmin/announcements',{method:'POST',body:body});req.then(function(){editingAnn='';annDraft=null;renderAnn()}).catch(function(e){alert(e.message)})};var cancel=$('#ann-cancel');if(cancel)cancel.onclick=function(){editingAnn='';annDraft=null;renderAnn()}}).catch(function(e){fail(c,e)})}
var marketFilter='pending';
function renderMarketReviews(){var c=$('#content');var statusName={pending:'待审',approved:'已发布',rejected:'未通过',withdrawn:'开发者已撤回',superseded:'旧版本',hidden:'已下架'};var kindName={plugin:'插件',theme:'主题'};var capName={time:'时间',music:'音乐',web_search:'联网搜索',travel_guidance:'出行引导'};api('/api/shiyadmin/market/submissions'+(marketFilter?'?status='+encodeURIComponent(marketFilter):'')).then(function(d){var rows=(d.submissions||[]).map(function(x){var facets=[];try{facets=JSON.parse(x.capabilities||'[]')}catch{}var facetText=facets.map(function(v){return capName[v]||v}).join('、')||'未标注';var kind=x.asset_type==='theme'?'主题':'插件';var ops='<button class="small" data-market-act="view" data-id="'+esc(x.id)+'">查看</button>';if(x.status==='pending')ops+='<button class="small primary" data-market-act="approve" data-id="'+esc(x.id)+'">通过</button><button class="small danger" data-market-act="reject" data-id="'+esc(x.id)+'">拒绝</button>';if(x.status==='approved')ops+='<button class="small danger" data-market-act="unpublish" data-id="'+esc(x.id)+'">下架</button>';return '<tr><td><b>'+esc(x.name)+'</b> <span class="muted">v'+esc(x.version)+'</span><div class="muted" style="font-size:11px">'+esc(x.asset_id)+(x.source_ref?' / '+esc(x.source_ref):'')+'</div></td><td><span class="tag '+esc(x.status)+'">'+esc(kind)+'</span><div class="muted hide-sm" style="font-size:11px">'+esc(x.owner_name)+'<br>'+esc(x.owner_id)+'</div></td><td>'+esc(facetText)+'<div class="muted" style="max-width:340px;font-size:11px">'+esc((x.summary||'').slice(0,100))+'</div>'+(x.source_url?'<div class="muted" style="max-width:340px;font-size:11px;overflow-wrap:anywhere"><a href="'+esc(x.source_url)+'" target="_blank" rel="noopener noreferrer">'+esc(x.source_url)+'</a></div>':'')+'</td><td><span class="tag '+esc(x.status)+'">'+esc(statusName[x.status]||x.status)+'</span><div class="muted hide-sm" style="font-size:11px">'+fmtTime(x.created_at)+'</div></td><td><div class="ops">'+ops+'</div></td></tr>'}).join('');c.innerHTML='<div class="card"><div class="toolbar"><select id="market-status"><option value="pending"'+(marketFilter==='pending'?' selected':'')+'>待审核</option><option value="approved"'+(marketFilter==='approved'?' selected':'')+'>已发布</option><option value="rejected"'+(marketFilter==='rejected'?' selected':'')+'>未通过</option><option value="hidden"'+(marketFilter==='hidden'?' selected':'')+'>已下架</option><option value="withdrawn"'+(marketFilter==='withdrawn'?' selected':'')+'>开发者撤回</option><option value=""'+(marketFilter===''?' selected':'')+'>全部记录</option></select><b>插件与主题审核</b><span class="muted">发布、拒绝和下架需要管理员操作。审核材料来自仓库默认分支的 shiyu.json。</span></div><div style="overflow:auto"><table><thead><tr><th>作品</th><th>类型 / 开发者</th><th>能力或标签 / 仓库</th><th>状态</th><th>操作</th></tr></thead><tbody>'+(rows||'<tr><td colspan="5" class="muted">当前没有符合条件的投稿</td></tr>')+'</tbody></table></div></div>';$('#market-status').onchange=function(){marketFilter=this.value;renderMarketReviews()};c.querySelectorAll('[data-market-act]').forEach(function(b){b.onclick=function(){var id=b.dataset.id,act=b.dataset.marketAct;if(act==='view'){var item=(d.submissions||[]).find(function(x){return x.id===id});$('#modal-title').textContent=(item?item.name:'作品')+' / 审核材料';var detail='';try{detail=JSON.stringify(JSON.parse(item.manifest_json),null,2)}catch{detail=item?item.manifest_json:''}if(item&&item.source_url)detail='仓库：'+item.source_url+'\\n分支：'+item.source_ref+'\\n\\n'+detail;$('#modal-body').textContent=detail;$('#modal').classList.add('open');return}var note=prompt(act==='approve'?'审核备注（可选）':'必须填写拒绝或下架原因');if(note===null)return;if(act!=='approve'&&!note.trim())return alert('请填写审核说明');api('/api/shiyadmin/market/submissions/'+encodeURIComponent(id),{method:'PATCH',body:JSON.stringify({action:act,note:note.trim()})}).then(function(){renderMarketReviews()}).catch(function(e){alert(e.message)})}})}).catch(function(e){fail(c,e)})}
document.addEventListener('click',function(ev){var b=ev.target.closest('[data-act]');if(!b)return;var id=b.dataset.id,act=b.dataset.act;if(act==='approve'||act==='hide'||act==='reject'){var st=act==='approve'?'approved':act==='hide'?'hidden':'rejected';api('/api/shiyadmin/cards/'+id,{method:'PATCH',body:JSON.stringify({status:st})}).then(function(){renderCards()}).catch(function(e){alert(e.message)})}else if(act==='delcard'){if(!confirm('确定删除这张角色卡？其下所有评论也会删除，不可恢复。'))return;api('/api/shiyadmin/cards/'+id,{method:'DELETE'}).then(function(){renderCards()}).catch(function(e){alert(e.message)})}else if(act==='view'){api('/api/shiyadmin/cards/'+id).then(function(d){$('#modal-title').textContent=d.card.title+' · '+d.card.status;$('#modal-body').textContent=d.card.card_json||'（空）';$('#modal').classList.add('open')}).catch(function(e){alert(e.message)})}else if(act==='deluser'){if(!confirm('确定删除该用户的全部云端数据？用户本地数据不受影响，但下次同步会重新上传。'))return;api('/api/shiyadmin/users/'+encodeURIComponent(id),{method:'DELETE'}).then(function(){renderUsers()}).catch(function(e){alert(e.message)})}else if(act==='delcomment'){if(!confirm('确定删除这条评论？'))return;api('/api/shiyadmin/comments/'+id,{method:'DELETE'}).then(function(){renderComments()}).catch(function(e){alert(e.message)})}else if(act==='editann'){api('/api/shiyadmin/announcements').then(function(d){var a=(d.announcements||[]).find(function(x){return x.id===id});if(!a)return;editingAnn=id;annDraft={title:a.title,content:a.content,pinned:!!a.pinned,type:a.type||'once'};renderAnn()}).catch(function(e){alert(e.message)})}else if(act==='pinann'){api('/api/shiyadmin/announcements').then(function(d){var a=(d.announcements||[]).find(function(x){return x.id===id});if(!a)return;api('/api/shiyadmin/announcements/'+id,{method:'PATCH',body:JSON.stringify({title:a.title,content:a.content,pinned:!a.pinned,type:a.type||'once'})}).then(function(){renderAnn()})}).catch(function(e){alert(e.message)})}else if(act==='delann'){if(!confirm('确定删除这条公告？'))return;api('/api/shiyadmin/announcements/'+id,{method:'DELETE'}).then(function(){if(editingAnn===id){editingAnn='';annDraft=null}renderAnn()}).catch(function(e){alert(e.message)})}});
function closeModal(){$('#modal').classList.remove('open')}
document.querySelectorAll('.tabs button').forEach(function(b){b.addEventListener('click',function(){TAB=b.dataset.tab;render()})});
$('#keybtn').addEventListener('click',tryKey);
$('#key').addEventListener('keydown',function(e){if(e.key==='Enter')tryKey()});
$('#modal').addEventListener('click',function(e){if(e.target===this)closeModal()});
init();
</script></body></html>`;
  return new Response(html, { headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store', 'x-frame-options': 'DENY', 'referrer-policy': 'no-referrer' } });
}
async function route(request: Request, env: Env): Promise<Response> {
  if (request.method === 'OPTIONS') return new Response(null, { status: 204 });
  const url = new URL(request.url); const path = url.pathname.replace(/\/$/, '') || '/';
  if (request.method === 'OPTIONS') return new Response(null, { status: 204 });
  if (path === `/downloads/windows/${WINDOWS_RELEASE.filename}`) {
    if (request.method !== 'GET' && request.method !== 'HEAD') return new Response(null, { status: 405, headers: { allow: 'GET, HEAD' } });
    return streamWindows(request, env.ASSETS);
  }
  if ((request.method === 'GET' || request.method === 'HEAD') && path === '/downloads/shiyu-0.9.4.apk') return streamApk094(request, env);
  if (request.method === 'GET' && path === '/downloads/shiyu-0.9.3.apk') return redirect('https://web-shiy.tniay.top/downloads/shiyu-0.9.4.apk');
  if (request.method === 'GET' && path === '/downloads/shiyu-0.9.1.apk') return redirect('https://github.com/tniany/shiyu-releases/releases/download/v0.9.1/shiyu-0.9.1.apk');
  if (request.method === 'GET' && path === '/downloads/shiyu-0.9.0.apk') return redirect('https://github.com/tniany/shiyu-releases/releases/download/v0.9.0/app-release.apk');
  if (request.method === 'GET' && env.ADMIN_ROUTE && path === `/${env.ADMIN_ROUTE.replace(/^\/+|\/+$/g, '')}`) return adminPage();
  if (request.method === 'GET' && path === '/market/docs') return html(withMarketplaceSettingsDocs(marketplaceDocsPage()));
  if (request.method === 'GET' && path === '/market/submit') return html(developerMarketPage());
  if (request.method === 'GET' && path === '/api/market/catalog.json') {
    const baseResponse = await env.ASSETS.fetch(new Request(new URL('/market/catalog.json', url), { method: 'GET' }));
    if (!baseResponse.ok) return json({ error: 'MARKET_BASE_UNAVAILABLE', message: '官方市场基础目录暂不可用' }, 503);
    let base: any;
    try { base = await baseResponse.json(); } catch { return json({ error: 'MARKET_BASE_INVALID', message: '官方市场基础目录格式无效' }, 503); }
    let approved: any[] = [];
    try {
      const rows = await env.DB.prepare('SELECT asset_type, asset_id, source_url, source_ref, manifest_json FROM market_approved_assets ORDER BY approved_at DESC LIMIT 200').all();
      approved = rows.results || [];
    } catch (error) {
      console.error('market approved asset query failed', error);
    }
    const catalog = mergePublicCatalog(base, approved, hongKongDay(Date.now()));
    return new Response(JSON.stringify(catalog), { status: 200, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'public, max-age=60, stale-while-revalidate=120', 'x-content-type-options': 'nosniff' } });
  }
  if (request.method === 'POST' && path === '/api/market/repository/inspect') {
    const user = await auth(request, env);
    if (!user) return json({ error: 'LOGIN_REQUIRED', message: '请先使用时遇账号登录' }, 401);
    const raw = await request.text();
    if (raw.length > 4_000) return json({ error: 'TOO_LARGE', message: '仓库地址请求超过 4KB 限制' }, 413);
    let body: any;
    try { body = JSON.parse(raw); } catch { return json({ error: 'INVALID_JSON', message: '请求格式无效' }, 400); }
    const inspection = await inspectGitHubRepository(body.repositoryUrl, { token: env.GITHUB_TOKEN || '' });
    if (!inspection.ok) return json({ error: 'SOURCE_INSPECTION_FAILED', field: inspection.field, message: inspection.message }, 422);
    return json({
      ok: true,
      repository: inspection.repository,
      assetId: inspection.assetId,
      version: inspection.version,
      manifest: inspection.manifest,
      manifestSha256: inspection.manifestSha256
    });
  }

  if (request.method === 'POST' && (path === '/api/market/submissions' || path === '/api/market/plugin-submissions')) {
    const user = await auth(request, env);
    if (!user) return json({ error: 'LOGIN_REQUIRED', message: '请先使用时遇账号登录' }, 401);
    const raw = await request.text();
    if (raw.length > 4_000) return json({ error: 'TOO_LARGE', message: '仓库地址请求超过 4KB 限制' }, 413);
    let body: any;
    try { body = JSON.parse(raw); } catch { return json({ error: 'INVALID_JSON', message: '请求格式无效' }, 400); }
    const expected = cleanText(body.expectedManifestSha256, 64).toLowerCase();
    if (expected && !/^[a-f0-9]{64}$/.test(expected)) return json({ error: 'INVALID_SOURCE_HASH', message: '识别摘要格式无效' }, 400);
    const inspection = await inspectGitHubRepository(body.repositoryUrl, { token: env.GITHUB_TOKEN || '' });
    if (!inspection.ok) return json({ error: 'SOURCE_INSPECTION_FAILED', field: inspection.field, message: inspection.message }, 422);
    if (expected && expected !== inspection.manifestSha256) {
      return json({ error: 'SOURCE_CHANGED', message: '识别后 shiyu.json 已发生变化，请重新识别仓库' }, 409);
    }
    const assetType = inspection.manifest.kind;
    const existing = await env.DB.prepare('SELECT owner_id FROM market_plugin_submissions WHERE asset_type = ? AND plugin_id = ? ORDER BY created_at ASC LIMIT 1').bind(assetType, inspection.assetId).first();
    if (existing && String(existing.owner_id) !== user.id) return json({ error: 'ASSET_ID_OWNED', message: '这个作品 ID 已由其他开发者使用' }, 409);
    const pendingCount = await env.DB.prepare("SELECT COUNT(*) AS n FROM market_plugin_submissions WHERE owner_id = ? AND status = 'pending'").bind(user.id).first();
    if (Number(pendingCount?.n || 0) >= 3) return json({ error: 'TOO_MANY_PENDING', message: '每个账号最多同时保留 3 个待审作品，请等待审核或撤回' }, 429);
    const duplicate = await env.DB.prepare("SELECT id FROM market_plugin_submissions WHERE asset_type = ? AND plugin_id = ? AND status = 'pending' LIMIT 1").bind(assetType, inspection.assetId).first();
    if (duplicate) return json({ error: 'ALREADY_PENDING', message: '这个作品已有待审版本，请等待审核完成' }, 409);
    const submissionId = id(); const timestamp = now(); const manifest = inspection.manifest;
    const facetJson = assetType === 'plugin' ? manifest.capabilities : manifest.tags;
    const icon = assetType === 'plugin' ? manifest.icon : 'extension';
    const category = assetType === 'plugin' ? manifest.category : '主题外观';
    try {
      await env.DB.batch([
        env.DB.prepare('INSERT INTO market_plugin_submissions (id,owner_id,owner_name,plugin_id,version,name,summary,description,category,icon,capabilities_json,manifest_json,status,created_at,updated_at,asset_type,source_url,source_ref,manifest_sha256) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)').bind(submissionId,user.id,user.name,inspection.assetId,inspection.version,manifest.name,manifest.summary,manifest.description,category,icon,JSON.stringify(facetJson),JSON.stringify(manifest),'pending',timestamp,timestamp,assetType,inspection.repository.canonicalUrl,inspection.repository.defaultBranch,inspection.manifestSha256),
        env.DB.prepare('INSERT INTO market_plugin_review_events (id,submission_id,actor_id,action,note,created_at) VALUES (?,?,?,?,?,?)').bind(id(),submissionId,user.id,'submitted','',timestamp)
      ]);
    } catch (error) {
      if (String(error).includes('one_pending')) return json({ error: 'ALREADY_PENDING', message: '这个作品已有待审版本' }, 409);
      throw error;
    }
    return json({ id: submissionId, status: 'pending', assetType }, 201);
  }

  if (request.method === 'GET' && (path === '/api/market/submissions/mine' || path === '/api/market/plugin-submissions/mine')) {
    const user = await auth(request, env);
    if (!user) return json({ error: 'LOGIN_REQUIRED', message: '请先登录' }, 401);
    const rows = await env.DB.prepare('SELECT id,asset_type,plugin_id AS asset_id,version,name,summary,category,capabilities_json AS capabilities,source_url,source_ref,status,reviewer_note,created_at,updated_at FROM market_plugin_submissions WHERE owner_id = ? ORDER BY created_at DESC LIMIT 100').bind(user.id).all();
    return json({ submissions: rows.results || [] });
  }

  const withdrawMatch = path.match(/^\/api\/market\/(?:plugin-)?submissions\/([^/]+)\/withdraw$/);
  if (request.method === 'POST' && withdrawMatch) {
    const user = await auth(request, env); if (!user) return json({ error: 'LOGIN_REQUIRED' }, 401);
    const row = await env.DB.prepare('SELECT id,owner_id,status FROM market_plugin_submissions WHERE id = ?').bind(withdrawMatch[1]).first();
    if (!row || String(row.owner_id) !== user.id) return json({ error: 'NOT_FOUND' }, 404);
    if (String(row.status) !== 'pending') return json({ error: 'INVALID_STATUS', message: '只有待审版本可以撤回' }, 409);
    const timestamp = now();
    await env.DB.batch([
      env.DB.prepare("UPDATE market_plugin_submissions SET status = 'withdrawn',updated_at = ? WHERE id = ?").bind(timestamp,withdrawMatch[1]),
      env.DB.prepare('INSERT INTO market_plugin_review_events (id,submission_id,actor_id,action,note,created_at) VALUES (?,?,?,?,?,?)').bind(id(),withdrawMatch[1],user.id,'withdrawn','',timestamp)
    ]);
    return json({ success: true, status: 'withdrawn' });
  }
  if (request.method === 'GET' && path === '/auth/login') { const state = id(); const verifier = base64url(crypto.getRandomValues(new Uint8Array(48))); const challenge = await pkceChallenge(verifier); const redirectUri = `${url.origin}/auth/callback`; const returnTo = safeReturnPath(url); const authorize = new URL(`${env.AUTH_ISSUER.replace(/\/$/, '')}/oauth/authorize`); authorize.searchParams.set('response_type','code'); authorize.searchParams.set('client_id',env.AUTH_CLIENT_ID); authorize.searchParams.set('redirect_uri',redirectUri); authorize.searchParams.set('state',state); authorize.searchParams.set('scope','profile qq'); authorize.searchParams.set('code_challenge',challenge); authorize.searchParams.set('code_challenge_method','S256'); const secure = url.protocol === 'https:' ? '; Secure' : ''; const cookieBase = `Path=/; HttpOnly; SameSite=Lax${secure}; Max-Age=600`; return redirect(authorize.toString(), [`shiyu_square_oauth_state=${state}; ${cookieBase}`, `shiyu_square_oauth_verifier=${verifier}; ${cookieBase}`, `shiyu_square_auth_return=${encodeURIComponent(returnTo)}; ${cookieBase}`]); }
  if (request.method === 'GET' && path === '/auth/callback') { const code=url.searchParams.get('code')||''; const state=url.searchParams.get('state')||''; const oauthError=url.searchParams.get('error_description')||url.searchParams.get('error')||''; const verifier = cookie(request,'shiyu_square_oauth_verifier'); let returnTo = '/'; try { returnTo = safeReturnPath(url, decodeURIComponent(cookie(request,'shiyu_square_auth_return') || '/')); } catch {} const secure = url.protocol === 'https:' ? '; Secure' : ''; const cookieBase = `Path=/; HttpOnly; SameSite=Lax${secure}`; const clearCookies = [`shiyu_square_oauth_state=; ${cookieBase}; Max-Age=0`,`shiyu_square_oauth_verifier=; ${cookieBase}; Max-Age=0`,`shiyu_square_auth_return=; ${cookieBase}; Max-Age=0`]; if (oauthError) return html(authResultPage(url.origin,returnTo,false,oauthError),400,clearCookies); if (!code || !state || state !== cookie(request,'shiyu_square_oauth_state') || !verifier) return html(authResultPage(url.origin,returnTo,false,'state 或 PKCE 校验不匹配，请重新登录'),400,clearCookies); const form=new URLSearchParams({grant_type:'authorization_code',client_id:env.AUTH_CLIENT_ID,code,redirect_uri:`${url.origin}/auth/callback`,code_verifier:verifier}); const requestToken=(withSecret:boolean)=>authFetch(env,'/oauth/token',{method:'POST',headers:{'content-type':'application/x-www-form-urlencoded',...(withSecret&&env.AUTH_CLIENT_SECRET?{authorization:`Basic ${btoa(`${env.AUTH_CLIENT_ID}:${env.AUTH_CLIENT_SECRET}`)}`}:{})},body:form}); let tokenResponse=await requestToken(Boolean(env.AUTH_CLIENT_SECRET)); let tokenText=await tokenResponse.text(); let token:any={}; try { token=JSON.parse(tokenText); } catch {} if (!tokenResponse.ok && env.AUTH_CLIENT_SECRET && token.error==='invalid_client') { tokenResponse=await requestToken(false); tokenText=await tokenResponse.text(); try { token=JSON.parse(tokenText); } catch { token={}; } } if (!tokenResponse.ok) { const detail=token.error_description||token.error||`登录服务返回 HTTP ${tokenResponse.status}${tokenText ? `：${tokenText.slice(0,180)}` : ''}`; return html(authResultPage(url.origin,returnTo,false,detail),502,clearCookies); } if (!token.access_token) return html(authResultPage(url.origin,returnTo,false,'登录服务返回的令牌为空'),502,clearCookies); await reportWebLogin(env, token.access_token); return html(authResultPage(url.origin,returnTo,true,'登录成功'),200,[`shiyu_square_session=${token.access_token}; ${cookieBase}; Max-Age=${Number(token.expires_in||3600)}`,...clearCookies]); }
  if (request.method === 'GET' && path === '/auth/me') { const user=await auth(request,env); return json(user ? { loggedIn:true,user } : { loggedIn:false }); }
  if (request.method === 'POST' && path === '/auth/logout') { const secure = url.protocol === 'https:' ? '; Secure' : ''; return new Response(null,{status:204,headers:{'set-cookie':`shiyu_square_session=; Path=/; HttpOnly; SameSite=Lax${secure}; Max-Age=0`}}); }
  if (request.method === 'POST' && path === '/api/feedback') {
    if (!env.USAGE_DB) return json({ error: 'FEEDBACK_DATABASE_NOT_CONFIGURED', message: '反馈服务暂不可用' }, 503);
    let body: any;
    try { body = await request.json(); } catch { return json({ error: 'INVALID_JSON', message: '反馈内容格式无效' }, 400); }
    const feedbackId = id();
    const reference = feedbackId.replace(/-/g, '').slice(0, 12).toUpperCase();
    if (cleanText(body.website, 200)) return json({ reference }, 201);
    const category = ['suggestion', 'bug', 'experience', 'other'].includes(body.category) ? body.category : '';
    if (!category) return json({ error: 'INVALID_CATEGORY', message: '请选择有效的反馈类型' }, 400);
    const content = cleanText(body.content, 2000);
    if (content.length < 10) return json({ error: 'CONTENT_TOO_SHORT', message: '反馈内容至少需要 10 个字' }, 400);
    const contact = cleanText(body.contact, 100);
    const appVersion = cleanText(body.version, 40);
    const source = cleanText(body.source, 24) === 'android' ? 'android' : 'web';
    const diagnostics = cleanText(body.diagnostics, 12000);
    await env.USAGE_DB.prepare('INSERT INTO feedback (id,category,content,contact,app_version,source,diagnostics,created_at,status) VALUES (?,?,?,?,?,?,?,?,?)').bind(feedbackId,category,content,contact,appVersion,source,diagnostics,now(),'unread').run();
    return json({ reference }, 201);
  }
  if (request.method === 'GET' && path === '/api/sync') { const user=await auth(request,env); if (!user) return json({error:'LOGIN_REQUIRED'},401); const row=await env.DB.prepare('SELECT payload,updated_at FROM user_data WHERE user_id = ?').bind(user.id).first(); return json({ payload: row?.payload ? JSON.parse(row.payload as string) : null, updatedAt: row?.updated_at || 0 }); }
  if (request.method === 'PUT' && path === '/api/sync') { const user=await auth(request,env); if (!user) return json({error:'LOGIN_REQUIRED'},401); const body=await request.json() as any; const payload=JSON.stringify(body.payload || {}); if (payload.length>12000000) return json({error:'TOO_LARGE'},413); await env.DB.prepare('INSERT INTO user_data (user_id,payload,updated_at) VALUES (?,?,?) ON CONFLICT(user_id) DO UPDATE SET payload=excluded.payload,updated_at=excluded.updated_at').bind(user.id,payload,now()).run(); return json({success:true,updatedAt:now()}); }
  if (request.method === 'GET' && path === '/api/square/cards') { const rows = await env.DB.prepare("SELECT id,owner_id,owner_name,title,description,tags,avatar_data,status,created_at,updated_at FROM cards WHERE status = 'approved' ORDER BY created_at DESC LIMIT 100").all(); return json({ cards: rows.results || [] }); }
  let match = path.match(/^\/api\/square\/cards\/([^/]+)$/);
  if (request.method === 'GET' && match) { const card = await env.DB.prepare("SELECT id,owner_id,owner_name,title,description,tags,card_json,avatar_data,status,created_at,updated_at FROM cards WHERE id = ? AND status = 'approved'").bind(match[1]).first(); if (!card) return json({ error: 'NOT_FOUND' }, 404); const comments = await env.DB.prepare('SELECT id,user_id,user_name,content,created_at FROM comments WHERE card_id = ? ORDER BY created_at ASC LIMIT 200').bind(match[1]).all(); return json({ card, comments: comments.results || [] }); }
  if (request.method === 'POST' && path === '/api/square/cards') { const user = await auth(request, env); if (!user) return json({ error: 'LOGIN_REQUIRED', message: '登录后才可以发布角色卡' }, 401); const body = await request.json() as any; const cardJson = cleanText(body.cardJson, 180000); if (!cardJson) return json({ error: 'INVALID_CARD' }, 400); let parsed: any; try { parsed = JSON.parse(cardJson); } catch { return json({ error: 'INVALID_CARD', message: '角色卡必须是有效 JSON' }, 400); } const title = cleanText(body.title || parsed.name || parsed.data?.name || '未命名角色', 80); const description = cleanText(body.description || parsed.description || parsed.data?.description || '', 500); const tags = cleanText(Array.isArray(body.tags) ? body.tags.join(',') : body.tags, 240); const avatar = cleanText(body.avatarData, 600000); if (cardJson.length > 180000 || avatar.length > 600000) return json({ error: 'TOO_LARGE' }, 413); const cardId = id(); await env.DB.prepare('INSERT INTO cards (id,owner_id,owner_name,title,description,tags,card_json,avatar_data,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)').bind(cardId,user.id,user.name,title,description,tags,cardJson,avatar,'pending',now(),now()).run(); return json({ id: cardId, status: 'pending' }, 201); }
  match = path.match(/^\/api\/square\/cards\/([^/]+)\/comments$/); if (request.method === 'POST' && match) { const user = await auth(request, env); if (!user) return json({ error: 'LOGIN_REQUIRED' }, 401); const body = await request.json() as any; const content = cleanText(body.content, 500); if (!content) return json({ error: 'EMPTY_COMMENT' }, 400); const exists = await env.DB.prepare("SELECT id FROM cards WHERE id = ? AND status = 'approved'").bind(match[1]).first(); if (!exists) return json({ error: 'NOT_FOUND' }, 404); const commentId = id(); await env.DB.prepare('INSERT INTO comments (id,card_id,user_id,user_name,content,created_at) VALUES (?,?,?,?,?,?)').bind(commentId,match[1],user.id,user.name,content,now()).run(); return json({ id: commentId }, 201); }
  if (request.method === 'GET' && path === '/api/square/admin') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const rows = await env.DB.prepare("SELECT id,owner_id,owner_name,title,description,tags,status,created_at,updated_at FROM cards ORDER BY created_at DESC LIMIT 300").all(); return json({ cards: rows.results || [] }); }
  match = path.match(/^\/api\/square\/admin\/cards\/([^/]+)$/); if (request.method === 'PATCH' && match) { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const body = await request.json() as any; const status = ['pending','approved','rejected','hidden'].includes(body.status) ? body.status : ''; if (!status) return json({ error: 'INVALID_STATUS' }, 400); await env.DB.prepare('UPDATE cards SET status = ?, updated_at = ? WHERE id = ?').bind(status,now(),match[1]).run(); return json({ success: true }); }
  if (request.method === 'GET' && path === '/shiyadmin') return shiyAdminPage();
  if (request.method === 'GET' && path === '/api/announcements') { const rows = await env.DB.prepare('SELECT id,title,content,pinned,type,created_at,updated_at FROM announcements ORDER BY pinned DESC, updated_at DESC LIMIT 10').all(); return json({ announcements: rows.results || [] }); }
  if (path === '/api/shiyadmin/session' && request.method === 'GET') { const user = await admin(request, env); return user ? json({ ok: true, user }) : json({ error: 'ADMIN_REQUIRED' }, 401); }
  if (path === '/api/shiyadmin/stats' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const since = now() - 13 * 86400;
    const [users, byStatus, comments, announcements, trend, syncs, recentCards] = await Promise.all([
      env.DB.prepare('SELECT COUNT(*) AS n, COALESCE(SUM(LENGTH(payload)),0) AS bytes, COALESCE(MAX(updated_at),0) AS last FROM user_data').first(),
      env.DB.prepare('SELECT status, COUNT(*) AS n FROM cards GROUP BY status').all(),
      env.DB.prepare('SELECT COUNT(*) AS n FROM comments').first(),
      env.DB.prepare('SELECT COUNT(*) AS n FROM announcements').first(),
      env.DB.prepare("SELECT DATE(created_at, 'unixepoch') AS d, COUNT(*) AS n FROM cards WHERE created_at >= ? GROUP BY d ORDER BY d").bind(since).all(),
      env.DB.prepare('SELECT user_id, updated_at, LENGTH(payload) AS bytes FROM user_data ORDER BY updated_at DESC LIMIT 8').all(),
      env.DB.prepare('SELECT id, owner_name, title, status, created_at FROM cards ORDER BY created_at DESC LIMIT 8').all()
    ]);
    const statusMap: Record<string, number> = {}; for (const row of (byStatus.results || []) as any[]) statusMap[String(row.status)] = Number(row.n);
    return json({
      users: { count: Number(users?.n || 0), bytes: Number(users?.bytes || 0), lastSync: Number(users?.last || 0) },
      cards: { total: Object.values(statusMap).reduce((a, b) => a + b, 0), pending: statusMap['pending'] || 0, approved: statusMap['approved'] || 0, rejected: statusMap['rejected'] || 0, hidden: statusMap['hidden'] || 0, trend: (trend.results || []) },
      comments: Number(comments?.n || 0),
      announcements: Number(announcements?.n || 0),
      recentSyncs: syncs.results || [],
      recentCards: recentCards.results || []
    });
  }
  if (path === '/api/shiyadmin/app-usage' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    if (!env.USAGE_DB) return json({ error: 'USAGE_DATABASE_NOT_CONFIGURED', message: '使用统计数据库未绑定' }, 503);
    const usage = env.USAGE_DB;
    const [counterRows, people, daily, demoDaily] = await Promise.all([
      usage.prepare("SELECT name, value FROM counters WHERE name IN ('app_opens','apk_downloads','web_visits','web_logins')").all(),
      usage.prepare('SELECT COUNT(*) AS count FROM web_login_users').first('count'),
      usage.prepare("SELECT day, count, 'app_opens' AS kind FROM daily_launches UNION ALL SELECT day, count, 'apk_downloads' AS kind FROM daily_downloads UNION ALL SELECT day, count, 'web_visits' AS kind FROM daily_web_visits UNION ALL SELECT day, count, 'web_logins' AS kind FROM daily_web_logins ORDER BY day DESC").all(),
      usage.prepare('SELECT day, app_opens, apk_downloads, web_visits, web_logins, note FROM demo_daily_usage ORDER BY day DESC').all()
    ]);
    const totals: Record<string, number> = { app_opens: 0, apk_downloads: 0, web_visits: 0, web_logins: 0, login_people: Number(people || 0) };
    for (const row of (counterRows.results || []) as any[]) totals[String(row.name)] = Number(row.value || 0);
    const byDay: Record<string, any> = {};
    for (const row of (daily.results || []) as any[]) { const item = byDay[row.day] || (byDay[row.day] = { day: row.day }); item[row.kind] = Number(row.count || 0); }
    const versions = await usage.prepare("SELECT version, count AS app_opens, 0 AS apk_downloads FROM version_launches UNION ALL SELECT version, 0 AS app_opens, count AS apk_downloads FROM version_downloads ORDER BY version").all();
    const versionMap: Record<string, any> = {};
    for (const row of (versions.results || []) as any[]) { const item = versionMap[row.version] || (versionMap[row.version] = { version: row.version, app_opens: 0, apk_downloads: 0 }); item.app_opens += Number(row.app_opens || 0); item.apk_downloads += Number(row.apk_downloads || 0); }
    const orderedVersions = Object.values(versionMap).sort((a: any, b: any) => String(b.version).localeCompare(String(a.version), 'zh-CN', { numeric: true, sensitivity: 'base' }));
    return json({ totals, daily: Object.values(byDay).sort((a: any, b: any) => String(b.day).localeCompare(String(a.day))), demoDaily: demoDaily.results || [], versions: orderedVersions });
  }
  if (path === '/api/shiyadmin/feedback' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    if (!env.USAGE_DB) return json({ error: 'USAGE_DATABASE_NOT_CONFIGURED', message: '反馈数据库未绑定' }, 503);
    const rows = await env.USAGE_DB.prepare('SELECT id, category, content, contact, app_version, source, diagnostics, created_at, status FROM feedback ORDER BY created_at DESC LIMIT 200').all();
    return json({ feedback: rows.results || [] });
  }
  const feedbackMatch = path.match(/^\/api\/shiyadmin\/feedback\/([^/]+)$/);
  if (feedbackMatch && request.method === 'PATCH') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    if (!env.USAGE_DB) return json({ error: 'USAGE_DATABASE_NOT_CONFIGURED', message: '反馈数据库未绑定' }, 503);
    const body = await request.json() as any;
    const status = ['unread', 'reading', 'resolved', 'ignored', 'archived'].includes(body.status) ? body.status : '';
    if (!status) return json({ error: 'INVALID_STATUS' }, 400);
    const out = await env.USAGE_DB.prepare('UPDATE feedback SET status = ? WHERE id = ?').bind(status, decodeURIComponent(feedbackMatch[1])).run();
    return json({ success: true, updated: Number(out.meta?.changes || 0) });
  }
  if (path === '/api/shiyadmin/cards' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const url = new URL(request.url);
    const status = ['pending','approved','rejected','hidden'].includes(url.searchParams.get('status') || '') ? url.searchParams.get('status') : '';
    const q = cleanText(url.searchParams.get('q'), 60);
    const limit = Math.min(300, Math.max(10, Number(url.searchParams.get('limit') || 100)));
    let sql = 'SELECT id, owner_id, owner_name, title, description, tags, status, created_at, updated_at FROM cards';
    const where: string[] = []; const binds: any[] = [];
    if (status) { where.push('status = ?'); binds.push(status); }
    if (q) { where.push('(title LIKE ? OR owner_name LIKE ? OR description LIKE ?)'); const like = `%${q}%`; binds.push(like, like, like); }
    if (where.length) sql += ' WHERE ' + where.join(' AND ');
    sql += ' ORDER BY created_at DESC LIMIT ?'; binds.push(limit);
    const rows = await env.DB.prepare(sql).bind(...binds).all();
    return json({ cards: rows.results || [] });
  }
  match = path.match(/^\/api\/shiyadmin\/cards\/([^/]+)$/);
  if (match && request.method === 'GET') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const card = await env.DB.prepare('SELECT id, owner_id, owner_name, title, description, tags, card_json, status, created_at, updated_at FROM cards WHERE id = ?').bind(match[1]).first(); if (!card) return json({ error: 'NOT_FOUND' }, 404); return json({ card }); }
  if (match && request.method === 'PATCH') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const body = await request.json() as any; const status = ['pending','approved','rejected','hidden'].includes(body.status) ? body.status : ''; if (!status) return json({ error: 'INVALID_STATUS' }, 400); await env.DB.prepare('UPDATE cards SET status = ?, updated_at = ? WHERE id = ?').bind(status,now(),match[1]).run(); return json({ success: true }); }
  if (match && request.method === 'DELETE') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); await env.DB.prepare('DELETE FROM comments WHERE card_id = ?').bind(match[1]).run(); const out = await env.DB.prepare('DELETE FROM cards WHERE id = ?').bind(match[1]).run(); return json({ success: true, deleted: out.meta.changes || 0 }); }
  if (path === '/api/shiyadmin/users' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const url = new URL(request.url);
    const q = cleanText(url.searchParams.get('q'), 80);
    let sql = 'SELECT user_id, updated_at, LENGTH(payload) AS bytes FROM user_data';
    const binds: any[] = [];
    if (q) { sql += ' WHERE user_id LIKE ?'; binds.push(`%${q}%`); }
    sql += ' ORDER BY updated_at DESC LIMIT 200';
    const rows = await env.DB.prepare(sql).bind(...binds).all();
    return json({ users: rows.results || [] });
  }
  match = path.match(/^\/api\/shiyadmin\/users\/([^/]+)$/);
  if (match && request.method === 'DELETE') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const out = await env.DB.prepare('DELETE FROM user_data WHERE user_id = ?').bind(decodeURIComponent(match[1])).run(); return json({ success: true, deleted: out.meta.changes || 0 }); }
  if (path === '/api/shiyadmin/comments' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    try {
      const rows = await env.DB.prepare('SELECT id, card_id, user_id, user_name, content, created_at FROM comments ORDER BY created_at DESC LIMIT 100').all();
      const comments = (rows.results || []) as any[];
      const cardIds = [...new Set(comments.map(row => String(row.card_id || '')).filter(Boolean))];
      const titles: Record<string, string> = {};
      if (cardIds.length) {
        const placeholders = cardIds.map(() => '?').join(',');
        const cards = await env.DB.prepare(`SELECT id, title FROM cards WHERE id IN (${placeholders})`).bind(...cardIds).all();
        for (const card of (cards.results || []) as any[]) titles[String(card.id)] = String(card.title || '');
      }
      return json({ comments: comments.map(row => ({ ...row, card_title: titles[String(row.card_id)] || '（角色卡已删除）' })) });
    } catch (error) {
      console.error('shiyadmin comments query failed', error);
      return json({ error: 'COMMENTS_QUERY_FAILED', message: '评论数据读取失败，请稍后重试' }, 503);
    }
  }
  match = path.match(/^\/api\/shiyadmin\/comments\/([^/]+)$/);
  if (match && request.method === 'DELETE') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const out = await env.DB.prepare('DELETE FROM comments WHERE id = ?').bind(match[1]).run(); return json({ success: true, deleted: out.meta.changes || 0 }); }
  if (path === '/api/shiyadmin/announcements' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const rows = await env.DB.prepare('SELECT id,title,content,pinned,type,created_at,updated_at FROM announcements ORDER BY pinned DESC, updated_at DESC LIMIT 100').all();
    return json({ announcements: rows.results || [] });
  }
  if (path === '/api/shiyadmin/announcements' && request.method === 'POST') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const body = await request.json() as any;
    const title = cleanText(body.title, 120); const content = cleanText(body.content, 5000);
    if (!title) return json({ error: 'INVALID_TITLE', message: '公告标题不能为空' }, 400);
    const pinned = body.pinned === true ? 1 : 0;
    const type = body.type === 'always' ? 'always' : 'once';
    const announcementId = id();
    await env.DB.prepare('INSERT INTO announcements (id,title,content,pinned,type,created_at,updated_at) VALUES (?,?,?,?,?,?,?)').bind(announcementId,title,content,pinned,type,now(),now()).run();
    return json({ id: announcementId }, 201);
  }
  match = path.match(/^\/api\/shiyadmin\/announcements\/([^/]+)$/);
  if (match && request.method === 'PATCH') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const body = await request.json() as any;
    const title = cleanText(body.title, 120); const content = cleanText(body.content, 5000);
    if (!title) return json({ error: 'INVALID_TITLE', message: '公告标题不能为空' }, 400);
    const pinned = body.pinned === true ? 1 : 0;
    const type = body.type === 'always' ? 'always' : 'once';
    const out = await env.DB.prepare('UPDATE announcements SET title = ?, content = ?, pinned = ?, type = ?, updated_at = ? WHERE id = ?').bind(title,content,pinned,type,now(),match[1]).run();
    if (!out.meta.changes) return json({ error: 'NOT_FOUND' }, 404);
    return json({ success: true });
  }
  if (match && request.method === 'DELETE') { if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401); const out = await env.DB.prepare('DELETE FROM announcements WHERE id = ?').bind(match[1]).run(); return json({ success: true, deleted: out.meta.changes || 0 }); }
  if (path === '/api/shiyadmin/market/submissions' && request.method === 'GET') {
    if (!await admin(request, env)) return json({ error: 'ADMIN_REQUIRED' }, 401);
    const requested = url.searchParams.get('status') || '';
    const status = ['pending','approved','rejected','withdrawn','superseded','hidden'].includes(requested) ? requested : '';
    let sql = 'SELECT id,owner_id,owner_name,asset_type,plugin_id AS asset_id,version,name,summary,description,category,icon,capabilities_json AS capabilities,manifest_json,source_url,source_ref,manifest_sha256,status,reviewer_note,reviewed_by,reviewed_at,created_at,updated_at FROM market_plugin_submissions';
    const binds: any[] = [];
    if (status) { sql += ' WHERE status = ?'; binds.push(status); }
    sql += ' ORDER BY created_at DESC LIMIT 200';
    const rows = await env.DB.prepare(sql).bind(...binds).all();
    return json({ submissions: rows.results || [] });
  }
  const marketReviewMatch = path.match(/^\/api\/shiyadmin\/market\/submissions\/([^/]+)$/);
  if (marketReviewMatch && request.method === 'PATCH') {
    const reviewer = await admin(request, env);
    if (!reviewer) return json({ error: 'ADMIN_REQUIRED' }, 401);
    let body: any;
    try { body = await request.json(); } catch { return json({ error: 'INVALID_JSON' }, 400); }
    const action = ['approve','reject','unpublish'].includes(body.action) ? body.action : '';
    if (!action) return json({ error: 'INVALID_ACTION' }, 400);
    const note = cleanText(body.note, 1000);
    if ((action === 'reject' || action === 'unpublish') && !note) return json({ error: 'NOTE_REQUIRED', message: '拒绝或下架时必须填写审核说明' }, 400);
    const row = await env.DB.prepare('SELECT id,owner_id,owner_name,asset_type,plugin_id AS asset_id,version,manifest_json,source_url,source_ref,manifest_sha256,status FROM market_plugin_submissions WHERE id = ?').bind(marketReviewMatch[1]).first();
    if (!row) return json({ error: 'NOT_FOUND' }, 404);
    const nextStatus = reviewTransition(String(row.status), action);
    if (!nextStatus) return json({ error: 'INVALID_STATUS', message: '当前状态不能执行这个审核操作' }, 409);
    const timestamp = now();
    if (action === 'approve') {
      let manifest: any;
      try { manifest = JSON.parse(String(row.manifest_json || '{}')); } catch { return json({ error: 'INVALID_MANIFEST' }, 409); }
      const validation = validateMarketplaceManifest(manifest);
      if (!validation.ok) return json({ error: 'INVALID_MANIFEST', field: validation.field, message: validation.message }, 409);
      if (String(manifest.id || '') !== String(row.asset_id || '') || String(manifest.version || '') !== String(row.version || '')) return json({ error: 'MANIFEST_MISMATCH' }, 409);
      if (String(manifest.kind || '') !== String(row.asset_type || '')) return json({ error: 'MANIFEST_MISMATCH' }, 409);
      await env.DB.batch([
        env.DB.prepare("UPDATE market_plugin_submissions SET status = 'superseded',updated_at = ? WHERE asset_type = ? AND plugin_id = ? AND status = 'approved' AND id <> ?").bind(timestamp,row.asset_type,row.asset_id,row.id),
        env.DB.prepare('INSERT INTO market_approved_assets (asset_type,asset_id,submission_id,owner_id,owner_name,source_url,source_ref,manifest_sha256,manifest_json,approved_by,approved_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(asset_type,asset_id) DO UPDATE SET submission_id=excluded.submission_id,owner_id=excluded.owner_id,owner_name=excluded.owner_name,source_url=excluded.source_url,source_ref=excluded.source_ref,manifest_sha256=excluded.manifest_sha256,manifest_json=excluded.manifest_json,approved_by=excluded.approved_by,approved_at=excluded.approved_at,updated_at=excluded.updated_at').bind(row.asset_type,row.asset_id,row.id,row.owner_id,row.owner_name,row.source_url,row.source_ref,row.manifest_sha256,JSON.stringify(validation.manifest),reviewer.id,timestamp,timestamp),
        env.DB.prepare("UPDATE market_plugin_submissions SET status = 'approved',reviewer_note = ?,reviewed_by = ?,reviewed_at = ?,updated_at = ? WHERE id = ?").bind(note,reviewer.id,timestamp,timestamp,row.id),
        env.DB.prepare('INSERT INTO market_plugin_review_events (id,submission_id,actor_id,action,note,created_at) VALUES (?,?,?,?,?,?)').bind(id(),row.id,reviewer.id,'approved',note,timestamp)
      ]);
      return json({ success: true, status: 'approved' });
    }
    if (action === 'reject') {
      await env.DB.batch([
        env.DB.prepare("UPDATE market_plugin_submissions SET status = 'rejected',reviewer_note = ?,reviewed_by = ?,reviewed_at = ?,updated_at = ? WHERE id = ?").bind(note,reviewer.id,timestamp,timestamp,row.id),
        env.DB.prepare('INSERT INTO market_plugin_review_events (id,submission_id,actor_id,action,note,created_at) VALUES (?,?,?,?,?,?)').bind(id(),row.id,reviewer.id,'rejected',note,timestamp)
      ]);
      return json({ success: true, status: 'rejected' });
    }
    await env.DB.batch([
      env.DB.prepare('DELETE FROM market_approved_assets WHERE asset_type = ? AND asset_id = ? AND submission_id = ?').bind(row.asset_type,row.asset_id,row.id),
      env.DB.prepare("UPDATE market_plugin_submissions SET status = 'hidden',reviewer_note = ?,reviewed_by = ?,reviewed_at = ?,updated_at = ? WHERE id = ?").bind(note,reviewer.id,timestamp,timestamp,row.id),
      env.DB.prepare('INSERT INTO market_plugin_review_events (id,submission_id,actor_id,action,note,created_at) VALUES (?,?,?,?,?,?)').bind(id(),row.id,reviewer.id,'unpublished',note,timestamp)
    ]);
    return json({ success: true, status: 'hidden' });
  }
  if (request.method === 'GET' || request.method === 'HEAD') return env.ASSETS.fetch(request);
  return json({ error: 'NOT_FOUND' }, 404);
}
export default { async fetch(request: Request, env: Env) { try { return cors(request, await route(request, env), env); } catch (error) { console.error(error); return cors(request, json({ error: 'INTERNAL_ERROR' }, 500), env); } } };
