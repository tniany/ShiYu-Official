export function developerMarketPage() {
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="index,follow"><title>时遇开发者中心</title><style>
:root{color-scheme:light;--bg:#eef4f1;--surface:#fbfdfc;--surface-strong:#ffffff;--ink:#142822;--muted:#5e706a;--line:#cfddd7;--accent:#176b56;--accent-strong:#0e5846;--accent-soft:#deeee8;--danger:#a93f4d;--danger-soft:#fbeaed;--warn:#8a621b;--warn-soft:#fbefd9;--shadow:0 18px 50px rgba(30,68,57,.09)}
@media(prefers-color-scheme:dark){:root{color-scheme:dark;--bg:#101b18;--surface:#16231f;--surface-strong:#1a2a25;--ink:#edf6f2;--muted:#9cafaa;--line:#31463f;--accent:#68c5a4;--accent-strong:#8bd9bb;--accent-soft:#1f3a32;--danger:#ff9ba7;--danger-soft:#41252b;--warn:#f1c675;--warn-soft:#3d3220;--shadow:0 20px 56px rgba(0,0,0,.22)}}
*{box-sizing:border-box}html{background:var(--bg)}body{margin:0;min-height:100vh;background:radial-gradient(circle at 8% 0,rgba(60,136,108,.16),transparent 32rem),radial-gradient(circle at 95% 18%,rgba(103,143,130,.1),transparent 28rem),var(--bg);color:var(--ink);font:15px/1.65 'Segoe UI','Microsoft YaHei',system-ui,sans-serif}
a{color:var(--accent);text-underline-offset:3px}button,input{font:inherit}.site-header{display:flex;align-items:center;justify-content:space-between;gap:20px;max-width:1120px;margin:auto;padding:20px 22px}.brand{display:flex;align-items:center;gap:11px;color:var(--ink);text-decoration:none;font-weight:750;letter-spacing:-.01em}.brand-mark{display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:var(--accent);color:var(--surface-strong);font-size:15px;font-weight:800}.nav{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.nav a{min-height:38px;display:inline-flex;align-items:center;padding:0 13px;border:1px solid var(--line);border-radius:999px;background:rgba(255,255,255,.35);color:var(--ink);text-decoration:none;font-size:13px;font-weight:650}
main{max-width:1120px;margin:0 auto 72px;padding:0 22px}.hero{display:grid;grid-template-columns:minmax(0,1.15fr) minmax(280px,.85fr);gap:18px;align-items:stretch;margin:12px 0 18px}.panel{border:1px solid var(--line);border-radius:16px;background:var(--surface);box-shadow:var(--shadow)}.hero-copy{padding:34px;background:linear-gradient(135deg,var(--surface),var(--accent-soft))}.hero-copy h1{max-width:12ch;margin:0 0 14px;font-size:clamp(30px,5vw,46px);line-height:1.08;letter-spacing:-.045em}.hero-copy p{max-width:60ch;margin:0;color:var(--muted)}.flow{padding:20px;display:grid;gap:9px}.flow h2{margin:0 0 5px;font-size:16px}.flow-item{display:grid;grid-template-columns:30px 1fr;gap:10px;align-items:start;padding:11px;border-radius:12px;background:var(--surface-strong);border:1px solid var(--line);font-size:13px}.flow-item b{display:grid;place-items:center;width:28px;height:28px;border-radius:9px;background:var(--accent-soft);color:var(--accent);font-size:12px}
.workspace{display:grid;gap:18px}.workspace[hidden],.gate[hidden],.result[hidden],.notice[hidden],.preview-list[hidden]{display:none}.gate{max-width:560px;margin:48px auto;padding:34px;text-align:center}.gate h2{margin:0 0 8px;font-size:24px}.gate p{max-width:52ch;margin:0 auto 20px;color:var(--muted)}.primary,.secondary,.danger-button{display:inline-flex;align-items:center;justify-content:center;min-height:42px;padding:0 16px;border-radius:10px;border:1px solid var(--line);font-weight:700;font-size:13px;cursor:pointer;text-decoration:none}.primary{border-color:var(--accent);background:var(--accent);color:var(--surface-strong)}.primary:hover{background:var(--accent-strong)}.secondary{background:var(--surface-strong);color:var(--ink)}.danger-button{background:transparent;color:var(--danger);border-color:var(--danger)}button:active,.nav a:active{transform:translateY(1px)}button:disabled{opacity:.55;cursor:not-allowed}
.section{padding:24px}.section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:16px}.section-head h2{margin:0;font-size:20px;letter-spacing:-.02em}.section-head p{margin:4px 0 0;color:var(--muted);font-size:13px}.source-form{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px;align-items:end}.field{display:grid;gap:7px}.field label{color:var(--ink);font-size:13px;font-weight:650}.field small,.muted{color:var(--muted);font-size:12px}input{width:100%;min-height:45px;border:1px solid var(--line);border-radius:10px;background:var(--surface-strong);color:var(--ink);padding:10px 12px;outline:none}input::placeholder{color:var(--muted);opacity:.9}input:focus{border-color:var(--accent);box-shadow:0 0 0 3px color-mix(in srgb,var(--accent) 22%,transparent)}.notice{margin-top:14px;padding:11px 13px;border:1px solid var(--line);border-radius:10px;background:var(--accent-soft);color:var(--ink);font-size:13px}.notice.error{background:var(--danger-soft);border-color:var(--danger);color:var(--danger)}.notice.warn{background:var(--warn-soft);border-color:var(--warn);color:var(--warn)}
.result{margin-top:16px;border-top:1px solid var(--line);padding-top:16px}.result-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.result h3{margin:0;font-size:18px}.kind{display:inline-flex;align-items:center;min-height:26px;padding:0 9px;border-radius:999px;background:var(--accent-soft);color:var(--accent);font-size:12px;font-weight:750}.facts{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin:14px 0}.fact{padding:11px;border:1px solid var(--line);border-radius:10px;background:var(--surface-strong)}.fact span{display:block;color:var(--muted);font-size:11px}.fact b{display:block;margin-top:2px;overflow-wrap:anywhere;font-size:13px}.description{margin:12px 0;color:var(--muted);white-space:pre-wrap}.chips{display:flex;flex-wrap:wrap;gap:7px}.chip{padding:4px 9px;border-radius:999px;border:1px solid var(--line);background:var(--surface-strong);font-size:12px}.palette{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}.swatch{display:flex;align-items:center;gap:7px;padding:5px 8px;border:1px solid var(--line);border-radius:8px;font:11px ui-monospace,Consolas,monospace}.swatch i{width:18px;height:18px;border-radius:4px;border:1px solid rgba(0,0,0,.18)}.submit-row{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:16px}.submit-row span{max-width:65ch;color:var(--muted);font-size:12px}
.history{display:grid;gap:0}.submission{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:14px;padding:15px 0;border-top:1px solid var(--line)}.submission:first-child{border-top:0;padding-top:0}.submission:last-child{padding-bottom:0}.submission-title{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.submission-title b{font-size:15px}.meta{color:var(--muted);font-size:12px;overflow-wrap:anywhere}.meta a{overflow-wrap:anywhere}.status{display:inline-flex;align-items:center;min-height:25px;padding:0 9px;border-radius:999px;background:var(--surface-strong);border:1px solid var(--line);color:var(--muted);font-size:11px;font-weight:700;white-space:nowrap}.status.pending{background:var(--warn-soft);border-color:var(--warn);color:var(--warn)}.status.approved{background:var(--accent-soft);border-color:var(--accent);color:var(--accent)}.status.rejected{background:var(--danger-soft);border-color:var(--danger);color:var(--danger)}.review-note{margin-top:8px;padding:9px 11px;border-radius:9px;background:var(--warn-soft);color:var(--warn);font-size:12px;white-space:pre-wrap}.empty{padding:28px 14px;text-align:center;color:var(--muted)}
@media(max-width:760px){.site-header{align-items:flex-start;flex-direction:column;padding:18px 16px}.hero{grid-template-columns:1fr}.hero-copy{padding:25px}.flow{padding:16px}main{padding:0 15px}.source-form{grid-template-columns:1fr}.source-form button{width:100%}.facts{grid-template-columns:1fr}.result-head,.submit-row{align-items:flex-start;flex-direction:column}.submission{grid-template-columns:1fr}.section{padding:18px}}
@media(prefers-reduced-transparency:reduce){.panel,.hero-copy{background:var(--surface)}}
@media(prefers-reduced-motion:reduce){*,*::before,*::after{scroll-behavior:auto!important;transition:none!important;animation:none!important}}
</style></head><body>
<header class="site-header"><a class="brand" href="/"><span class="brand-mark">遇</span><span>时遇开发者中心</span></a><nav class="nav"><a href="/market/docs">开发文档</a><a href="/market">返回市场</a><span id="who" class="muted">正在确认账号</span></nav></header>
<main>
<section class="hero"><div class="panel hero-copy"><h1>从一个开源仓库开始发布</h1><p>时遇欢迎开源社区。仓库根目录提供 shiyu.json，系统会读取默认分支、识别插件或主题并生成审核资料。页面上展示的内容仅用于确认，最终提交会重新读取仓库。</p></div><aside class="panel flow"><h2>审核流程</h2><div class="flow-item"><b>1</b><span>在公开 GitHub 仓库的默认分支根目录提交强制配置文件 shiyu.json</span></div><div class="flow-item"><b>2</b><span>粘贴仓库地址，由时遇自动识别作品类型和所调用的 App 接口</span></div><div class="flow-item"><b>3</b><span>提交后由管理员检查兼容性、数据范围与配置变更</span></div><div class="flow-item"><b>4</b><span>通过后进入市场并展示开源仓库，新版本审核期间旧版本继续可用</span></div></aside></section>
<section class="panel gate" id="loginGate" hidden><h2>登录后提交作品</h2><p>投稿会绑定到时遇账号，用于查看审核状态、撤回待审版本和追踪发布记录。</p><a class="primary" href="/auth/login?return_to=%2Fmarket%2Fsubmit">使用时遇账号登录</a></section>
<div class="workspace" id="workspace" hidden>
<section class="panel section"><div class="section-head"><div><h2>识别 GitHub 仓库</h2><p>只接受公开仓库根地址，并读取默认分支根目录的 shiyu.json。</p></div><a class="secondary" href="/market/docs">查看配置规范</a></div>
<form id="inspectForm"><div class="source-form"><div class="field"><label for="repositoryUrl">GitHub 仓库地址</label><input id="repositoryUrl" type="url" required autocomplete="url" spellcheck="false" placeholder="https://github.com/owner/repository"><small>不要粘贴 /tree、/blob 或具体文件地址。配置文件最大 64KB。</small></div><button class="primary" id="inspectButton" type="submit">识别仓库</button></div></form>
<div class="notice error" id="inspectNotice" role="alert" hidden></div>
<div class="result" id="recognized" hidden><div class="result-head"><div><h3 id="assetName"></h3><div class="muted" id="assetIdentity"></div></div><span class="kind" id="assetKind"></span></div><div class="facts" id="assetFacts"></div><p class="description" id="assetDescription"></p><div id="assetDetails"></div><div class="submit-row"><span>提交时后端会再次读取 GitHub。如果 shiyu.json 已变化，会停止提交并要求重新识别。</span><button class="primary" id="submitButton" type="button">提交审核</button></div></div></section>
<section class="panel section"><div class="section-head"><div><h2>我的投稿</h2><p>展示插件与主题的审核状态、仓库和版本记录。</p></div><button class="secondary" id="reloadHistory" type="button">刷新状态</button></div><div class="notice error" id="historyNotice" role="alert" hidden></div><div class="history" id="history"><div class="empty">正在读取投稿记录</div></div></section>
</div>
</main><script>
const $=function(selector){return document.querySelector(selector)};
const esc=function(value){return String(value==null?'':value).replace(/[&<>"']/g,function(character){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]})};
const statusName={pending:'等待审核',approved:'已发布',rejected:'未通过',withdrawn:'已撤回',superseded:'旧版本',hidden:'已下架'};
const kindName={plugin:'插件',theme:'主题'};
const capabilityName={time:'日期与时间接口',music:'网易云点歌接口',web_search:'联网搜索接口',travel_guidance:'出行引导接口'};
let recognized=null;
function showNotice(element,message,kind){element.textContent=message||'';element.className='notice'+(kind==='error'?' error':kind==='warn'?' warn':'');element.hidden=!message}
async function api(path,options){const request=Object.assign({},options||{});if(request.body)request.headers=Object.assign({'content-type':'application/json'},request.headers||{});const response=await fetch(path,request);let data={};try{data=await response.json()}catch{}if(!response.ok){const error=new Error(data.message||data.error||'HTTP '+response.status);error.code=data.error;throw error}return data}
function clearRecognized(){recognized=null;document.querySelector('#recognized').hidden=true;showNotice(document.querySelector('#inspectNotice'),'')}
function renderRecognized(data){
  recognized=data;const manifest=data.manifest;document.querySelector('#assetName').textContent=manifest.name;document.querySelector('#assetKind').textContent=kindName[manifest.kind]||manifest.kind;document.querySelector('#assetIdentity').textContent=manifest.id+'  /  v'+manifest.version+'  /  '+manifest.author;
  document.querySelector('#assetFacts').innerHTML='<div class="fact"><span>作品类型</span><b>'+esc(kindName[manifest.kind]||manifest.kind)+'</b></div><div class="fact"><span>默认分支</span><b>'+esc(data.repository.defaultBranch)+'</b></div><div class="fact"><span>仓库</span><b>'+esc(data.repository.fullName)+'</b></div>';
  document.querySelector('#assetDescription').textContent=manifest.description;
  let details='';
  if(manifest.kind==='plugin'){details='<div class="chips">'+(manifest.capabilities||[]).map(function(value){return '<span class="chip">'+esc(capabilityName[value]||value)+'</span>'}).join('')+'</div><p class="muted">分类：'+esc(manifest.category)+'</p>'}
  else{details='<div class="chips">'+(manifest.tags||[]).map(function(value){return '<span class="chip">'+esc(value)+'</span>'}).join('')+'</div><div class="palette">'+(manifest.palette||[]).map(function(value){const color=/^#[0-9A-F]{6,8}$/i.test(value)?value:'transparent';return '<span class="swatch"><i style="background:'+esc(color)+'"></i>'+esc(value)+'</span>'}).join('')+'</div><p class="muted">外观字段：'+String(Object.keys(manifest.values||{}).length)+' 项</p>'}
  if((manifest.settings||[]).length)details+='<p class="muted">声明设置：'+String(manifest.settings.length)+' 项</p>'
  document.querySelector('#assetDetails').innerHTML=details;document.querySelector('#recognized').hidden=false;showNotice(document.querySelector('#inspectNotice'),'仓库识别成功。确认信息无误后提交审核。')
}
async function loadHistory(){const box=document.querySelector('#history');box.innerHTML='<div class="empty">正在读取投稿记录</div>';try{const data=await api('/api/market/submissions/mine');const rows=(data.submissions||[]).map(function(item){const action=item.status==='pending'?'<button class="danger-button" type="button" data-withdraw="'+esc(item.id)+'">撤回</button>':'';return '<article class="submission"><div><div class="submission-title"><b>'+esc(item.name)+'</b><span class="status '+esc(item.status)+'">'+esc(statusName[item.status]||item.status)+'</span><span class="chip">'+esc(kindName[item.asset_type]||item.asset_type)+'</span></div><div class="meta">'+esc(item.asset_id)+'  /  v'+esc(item.version)+'  /  提交于 '+esc(new Date(Number(item.created_at)*1000).toLocaleString('zh-CN'))+'</div><div class="meta"><a href="'+esc(item.source_url)+'" target="_blank" rel="noopener noreferrer">'+esc(item.source_url)+'</a></div>'+(item.reviewer_note?'<div class="review-note">审核说明：'+esc(item.reviewer_note)+'</div>':'')+'</div><div>'+action+'</div></article>'});box.innerHTML=rows.join('')||'<div class="empty">还没有投稿。识别一个 GitHub 仓库后开始提交。</div>';showNotice(document.querySelector('#historyNotice'),'')}catch(error){box.innerHTML='<div class="empty">暂时无法读取投稿记录</div>';showNotice(document.querySelector('#historyNotice'),error.message,'error')}}
async function init(){try{const data=await fetch('/auth/me').then(function(response){return response.json()});if(!data.loggedIn){document.querySelector('#loginGate').hidden=false;document.querySelector('#who').textContent='未登录';return}document.querySelector('#who').textContent=data.user.name+(data.user.qqId?'  /  '+data.user.qqId:'');document.querySelector('#workspace').hidden=false;await loadHistory()}catch(error){document.querySelector('#loginGate').hidden=false;document.querySelector('#who').textContent='登录不可用';showNotice(document.querySelector('#inspectNotice'),error.message,'error')}}
document.querySelector('#repositoryUrl').addEventListener('input',clearRecognized);
document.querySelector('#inspectForm').addEventListener('submit',async function(event){event.preventDefault();const button=document.querySelector('#inspectButton');button.disabled=true;button.textContent='正在读取';showNotice(document.querySelector('#inspectNotice'),'');clearRecognized();try{const data=await api('/api/market/repository/inspect',{method:'POST',body:JSON.stringify({repositoryUrl:document.querySelector('#repositoryUrl').value.trim()})});renderRecognized(data)}catch(error){showNotice(document.querySelector('#inspectNotice'),error.message,'error')}finally{button.disabled=false;button.textContent='识别仓库'}});
document.querySelector('#submitButton').addEventListener('click',async function(){if(!recognized)return;const button=document.querySelector('#submitButton');button.disabled=true;button.textContent='正在提交';try{await api('/api/market/submissions',{method:'POST',body:JSON.stringify({repositoryUrl:document.querySelector('#repositoryUrl').value.trim(),expectedManifestSha256:recognized.manifestSha256})});showNotice(document.querySelector('#inspectNotice'),'已提交，等待管理员审核。');await loadHistory();clearRecognized()}catch(error){showNotice(document.querySelector('#inspectNotice'),error.message,'error')}finally{button.disabled=false;button.textContent='提交审核'}});
document.querySelector('#history').addEventListener('click',async function(event){const button=event.target.closest('[data-withdraw]');if(!button)return;if(!confirm('确定撤回这条待审投稿？'))return;button.disabled=true;try{await api('/api/market/submissions/'+encodeURIComponent(button.dataset.withdraw)+'/withdraw',{method:'POST',body:'{}'});await loadHistory()}catch(error){showNotice(document.querySelector('#historyNotice'),error.message,'error');button.disabled=false}});
document.querySelector('#reloadHistory').addEventListener('click',loadHistory);
init();
</script></body></html>`;
}

export function marketplaceDocsPage() {
  const pluginExample = `{
  "schema": 1,
  "kind": "plugin",
  "id": "community.city-radio",
  "version": "1.0.0",
  "name": "城市电台",
  "summary": "按城市整理适合通勤时收听的公开歌单。",
  "description": "调用时遇已实现并审核的音乐搜索与日期时间接口整理公开歌单。作品不包含可执行代码、脚本、DEX 或原生模块。",
  "author": "你的作者名称",
  "category": "娱乐内容",
  "icon": "music",
  "capabilities": ["music", "time"],
  "permissions": ["歌单内容来自第三方音乐服务"],
  "settings": [
    {
      "key": "music_enabled",
      "type": "switch",
      "label": "允许点歌",
      "description": "关闭后不再调用网易云点歌接口。",
      "target": "agent_music_enabled",
      "default": true
    }
  ]
}`;
  const themeExample = `{
  "schema": 1,
  "kind": "theme",
  "id": "community.sea-breeze",
  "version": "1.0.0",
  "name": "海风",
  "summary": "低饱和青绿色主题，适合长时间阅读和夜间聊天。",
  "description": "使用 App 已审核的外观字段调整聊天背景、气泡颜色、玻璃透明度和文字间距，不包含脚本或远程资源。",
  "author": "你的作者名称",
  "tags": ["青绿", "玻璃", "夜间"],
  "palette": ["#FF12201D", "#FF68C5A4", "#FF21352F"],
  "values": {
    "compose_theme": 2,
    "compose_theme_style": 1,
    "experimental_glass_ui": true,
    "experimental_glass_opacity": 82,
    "experimental_input_glass": true,
    "experimental_top_bar_transparent": true,
    "chat_background_color": "#FF12201D",
    "chat_user_bubble_color": "#FF367961",
    "chat_ai_bubble_color": "#FF223A33",
    "chat_user_bubble_opacity": 92,
    "chat_ai_bubble_opacity": 94,
    "chat_font_size": 15,
    "chat_bubble_spacing": 9,
    "chat_line_height": 23
  },
  "settings": [
    {
      "key": "glass_opacity",
      "type": "range",
      "label": "玻璃强度",
      "description": "调整主题玻璃材质的可见程度。",
      "target": "experimental_glass_opacity",
      "min": 20,
      "max": 100,
      "step": 1,
      "default": 82
    },
    {
      "key": "font_size",
      "type": "range",
      "label": "聊天字号",
      "description": "调整聊天正文的字号。",
      "target": "chat_font_size",
      "min": 12,
      "max": 20,
      "step": 1,
      "default": 15
    },
    {
      "key": "glass_ui",
      "type": "switch",
      "label": "玻璃界面",
      "description": "控制主题是否使用玻璃材质。",
      "target": "experimental_glass_ui",
      "default": true
    }
  ]
}`;
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="index,follow"><title>时遇插件与主题开发规范</title><style>
:root{color-scheme:light;--bg:#eef4f1;--surface:#fbfdfc;--surface-strong:#fff;--ink:#142822;--muted:#5e706a;--line:#cfddd7;--accent:#176b56;--accent-strong:#0e5846;--accent-soft:#deeee8;--code:#11201c;--code-ink:#dcece6;--shadow:0 18px 50px rgba(30,68,57,.08)}
@media(prefers-color-scheme:dark){:root{color-scheme:dark;--bg:#0e1815;--surface:#15231f;--surface-strong:#1a2a25;--ink:#edf6f2;--muted:#9cafaa;--line:#31463f;--accent:#68c5a4;--accent-strong:#8bd9bb;--accent-soft:#1f3a32;--code:#09100e;--code-ink:#dcece6;--shadow:0 20px 56px rgba(0,0,0,.22)}}
*{box-sizing:border-box}html{scroll-behavior:smooth;background:var(--bg)}body{margin:0;min-height:100vh;background:radial-gradient(circle at 4% 0,rgba(60,136,108,.15),transparent 30rem),var(--bg);color:var(--ink);font:15px/1.75 'Segoe UI','Microsoft YaHei',system-ui,sans-serif}a{color:var(--accent);text-underline-offset:3px}.site-header{position:sticky;top:0;z-index:10;display:flex;align-items:center;justify-content:space-between;gap:16px;max-width:1280px;margin:auto;padding:15px 22px;background:color-mix(in srgb,var(--bg) 88%,transparent);backdrop-filter:blur(18px);border-bottom:1px solid var(--line)}.brand{display:flex;align-items:center;gap:11px;color:var(--ink);text-decoration:none;font-weight:750}.brand-mark{display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:var(--accent);color:var(--surface-strong);font-size:15px;font-weight:800}.nav{display:flex;gap:8px;flex-wrap:wrap}.nav a{min-height:36px;display:inline-flex;align-items:center;padding:0 12px;border:1px solid var(--line);border-radius:999px;color:var(--ink);text-decoration:none;font-size:13px;font-weight:650}
.layout{display:grid;grid-template-columns:230px minmax(0,850px);gap:42px;max-width:1180px;margin:0 auto;padding:34px 22px 86px}.toc{position:sticky;top:88px;align-self:start;display:grid;gap:4px;padding:16px;border:1px solid var(--line);border-radius:14px;background:var(--surface)}.toc b{margin:0 7px 5px}.toc a{padding:6px 7px;border-radius:7px;color:var(--muted);text-decoration:none;font-size:13px}.toc a:hover{background:var(--accent-soft);color:var(--ink)}.doc{min-width:0;padding:34px;border:1px solid var(--line);border-radius:18px;background:var(--surface);box-shadow:var(--shadow)}.lead{max-width:70ch;margin:0 0 28px;color:var(--muted);font-size:17px}.doc h1{max-width:15ch;margin:0 0 14px;font-size:clamp(32px,6vw,52px);line-height:1.02;letter-spacing:-.05em}.doc h2{margin:48px 0 13px;font-size:24px;letter-spacing:-.025em}.doc h3{margin:28px 0 10px;font-size:17px}.doc p,.doc li{max-width:72ch}.doc li+li{margin-top:6px}.rule{padding:15px 17px;margin:16px 0;border-left:3px solid var(--accent);border-radius:0 10px 10px 0;background:var(--accent-soft)}.rule b{display:block;margin-bottom:2px}.steps{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.step{padding:14px;border:1px solid var(--line);border-radius:12px;background:var(--surface-strong)}.step b{display:block;margin-bottom:3px}.step span{color:var(--muted);font-size:13px}.table-wrap{overflow:auto;border:1px solid var(--line);border-radius:12px;background:var(--surface-strong)}table{width:100%;border-collapse:collapse;font-size:13px}th,td{padding:10px 12px;text-align:left;vertical-align:top;border-bottom:1px solid var(--line)}tr:last-child td{border-bottom:0}th{background:var(--accent-soft);font-size:12px}.code-block{position:relative;margin:14px 0 22px;border:1px solid #2e4940;border-radius:13px;background:var(--code);box-shadow:0 10px 30px rgba(0,0,0,.12)}.code-head{display:flex;justify-content:space-between;align-items:center;padding:8px 12px;color:#aac1b9;border-bottom:1px solid #2e4940;font-size:12px}.code-head button{min-height:30px;padding:0 10px;border:1px solid #456158;border-radius:7px;background:transparent;color:#dcece6;cursor:pointer}pre{max-height:520px;margin:0;overflow:auto;padding:17px;color:var(--code-ink);font:12.5px/1.7 ui-monospace,SFMono-Regular,Consolas,monospace;white-space:pre}.inline{padding:2px 5px;border:1px solid var(--line);border-radius:5px;background:var(--surface-strong);font:12px ui-monospace,Consolas,monospace}.downloads{display:flex;gap:9px;flex-wrap:wrap;margin:14px 0 0}.downloads a{display:inline-flex;min-height:40px;align-items:center;padding:0 13px;border:1px solid var(--line);border-radius:9px;background:var(--surface-strong);color:var(--ink);text-decoration:none;font-weight:650;font-size:13px}.warning{padding:15px 17px;border:1px solid var(--accent);border-radius:12px;background:var(--accent-soft)}
@media(max-width:900px){.layout{grid-template-columns:1fr;padding-top:20px}.toc{position:static;grid-template-columns:repeat(2,minmax(0,1fr))}.toc b{grid-column:1/-1}.doc{padding:24px}.steps{grid-template-columns:1fr}.site-header{align-items:flex-start;flex-direction:column}}
@media(max-width:560px){.site-header{padding:14px 15px}.layout{padding:14px 12px 64px}.doc{padding:19px}.toc{grid-template-columns:1fr}.doc h2{margin-top:38px}.code-head span{max-width:190px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}}
@media(prefers-reduced-motion:reduce){html{scroll-behavior:auto}*,*::before,*::after{transition:none!important;animation:none!important}}
</style></head><body>
<header class="site-header"><a class="brand" href="/"><span class="brand-mark">遇</span><span>时遇开发者文档</span></a><nav class="nav"><a href="/market/submit">提交作品</a><a href="/market">返回市场</a></nav></header>
<div class="layout"><nav class="toc"><b>本页内容</b><a href="#source">仓库规则</a><a href="#manifest">强制配置</a><a href="#plugin">插件配置</a><a href="#theme">主题配置</a><a href="#review">审核流程</a><a href="#safety">安全与限制</a><a href="#schema">JSON Schema</a></nav><article class="doc">
<h1>插件与主题开发规范</h1><p class="lead">时遇欢迎开源社区。开发者中心只接收公开 GitHub 仓库，并读取默认分支根目录的 shiyu.json，自动识别作品类型、名称、版本和所调用的 App 接口。没有配置文件或配置不合法时不能提交审核。</p>
<div class="rule"><b>强制配置文件</b>仓库根目录必须存在 <span class="inline">shiyu.json</span>，文件名区分大小写，最大 64KB，必须使用 UTF-8 编码。</div>
<section id="source"><h2>提交仓库规则</h2><div class="steps"><div class="step"><b>公开仓库</b><span>必须能从 GitHub 公开访问，私有仓库不会读取。</span></div><div class="step"><b>仓库根地址</b><span>格式为 https://github.com/owner/repository。</span></div><div class="step"><b>默认分支</b><span>只读取仓库 default branch 根目录，不跟随子目录地址。</span></div></div><p>不要提交 <span class="inline">/tree</span>、<span class="inline">/blob</span>、文件地址、查询参数、账号密码或 .git 后缀。时遇只访问 GitHub 固定 API，不接受任意网页代理。</p><p>识别成功后，提交审核时后端会再次读取同一个仓库。如果 shiyu.json 的 SHA-256 已在识别后变化，提交会被拒绝，需要重新识别。</p></section>
<section id="manifest"><h2>公共字段</h2><div class="table-wrap"><table><thead><tr><th>字段</th><th>要求</th><th>说明</th></tr></thead><tbody><tr><td>schema</td><td>必填整数</td><td>当前固定为 1</td></tr><tr><td>kind</td><td>必填字符串</td><td>plugin 或 theme，后端据此自动分类</td></tr><tr><td>id</td><td>必填字符串</td><td>community. 开头，小写字母、数字和连字符</td></tr><tr><td>version</td><td>必填字符串</td><td>例如 1.0.0 或 1.0.0-beta</td></tr><tr><td>name</td><td>必填，2 至 60 字</td><td>市场中显示的作品名称</td></tr><tr><td>summary</td><td>必填，至少 8 字</td><td>一句话简介，最多 300 字</td></tr><tr><td>description</td><td>必填，至少 20 字</td><td>完整说明，最多 2000 字</td></tr><tr><td>author</td><td>必填，2 至 80 字</td><td>展示作者名称，审核记录同时绑定登录账号</td></tr></tbody></table></div><p>official. 开头的 ID 保留给时遇官方。同一作品 ID 首次通过后，只能由原投稿账号继续提交新版本。</p></section>
<section id="plugin"><h2>插件配置</h2><p>插件不加载仓库代码，只能声明调用 App 已实现并审核的接口。接口兼容性由时遇版本决定；runtimePrefs 由后端根据 capabilities 生成，开发者不能手写或覆盖。</p><div class="table-wrap"><table><thead><tr><th>字段</th><th>要求</th></tr></thead><tbody><tr><td>category</td><td>必填分类，最多 30 字</td></tr><tr><td>icon</td><td>schedule、music、search、travel、extension 之一</td></tr><tr><td>capabilities</td><td>至少声明一项 App 已审核接口：time、music、web_search、travel_guidance</td></tr><tr><td>permissions</td><td>可选的数据流向补充说明，最多 8 条。已审核接口的标准说明会自动加入</td></tr></tbody></table></div><div class="code-block"><div class="code-head"><span>shiyu.json  /  插件示例</span><button type="button" data-copy="plugin">复制</button></div><pre id="pluginCode">${pluginExample.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre></div></section>
<section id="theme"><h2>主题配置</h2><p>主题只能使用 App 已审核的外观字段白名单。颜色使用 #RRGGBB 或 #AARRGGBB，数值与布尔值会在后端重新校验。</p><div class="table-wrap"><table><thead><tr><th>字段</th><th>要求</th><th>范围或可用键</th></tr></thead><tbody><tr><td>tags</td><td>1 至 8 个标签</td><td>每个标签最多 16 字</td></tr><tr><td>palette</td><td>1 至 6 个颜色</td><td>#RRGGBB 或 #AARRGGBB</td></tr><tr><td>values</td><td>至少一项，只接受白名单字段</td><td>见下方范围</td></tr><tr><td>整数字段</td><td>必须是整数且在范围内</td><td>compose_theme 0-2；compose_theme_style 0-4；refresh_rate 0-165；chat_font_size 12-20；chat_bubble_spacing 2-16；chat_line_height 16-32；气泡透明度 10-100；气泡最大宽度 240-360；气泡上下内边距 6-20；输入框最小高度 40-64；背景遮罩 0-70；experimental_glass_opacity 20-100</td></tr><tr><td>颜色字段</td><td>颜色字符串或空字符串</td><td>chat_background_color、chat_user_bubble_color、chat_ai_bubble_color</td></tr><tr><td>布尔字段</td><td>true 或 false</td><td>chat_no_bubbles、chat_hide_avatars、chat_half_screen、experimental_input_glass、experimental_top_bar_transparent、experimental_glass_ui</td></tr></tbody></table></div><div class="code-block"><div class="code-head"><span>shiyu.json  /  主题示例</span><button type="button" data-copy="theme">复制</button></div><pre id="themeCode">${themeExample.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre></div></section>
<section id="review"><h2>审核与发布</h2><p>提交成功后，管理员会查看规范化清单、GitHub 仓库、账号投稿记录和审核材料。通过后可识别的开源仓库地址会展示在市场中，方便用户查看源码与项目说明。以下操作由管理员执行：通过、拒绝、下架。</p><ul><li>待审版本可以撤回，撤回后可以重新提交。</li><li>每个账号最多同时保留 3 个待审作品。</li><li>同一作品同一时间只能有一个待审版本。</li><li>新版本审核期间，已经发布的旧版本继续可用。</li><li>新版本通过后，旧版本记录标记为已替换。</li><li>审核说明会显示在开发者中心的投稿记录中。</li></ul></section>
<section id="safety"><h2>安全与内容限制</h2><div class="warning"><p>当前 Android 客户端不会动态执行 GitHub 仓库中的代码。社区插件只能调用 App 已实现并审核的接口；JavaScript、DEX、JAR、原生共享库、远程脚本、任意网络代理和自动安装器都不会由时遇加载。</p></div><p>插件声明项只接受白名单中的 App 接口。主题只能修改白名单中的外观值。配置文件中的未知字段、未知接口、越界数值、非法颜色和手动 runtimePrefs 都会被拒绝。</p><p>时遇拥抱开源，但审核通过只代表该作品符合当前客户端接口与市场规则，不代表第三方仓库中的所有代码都被时遇执行或认可。</p><p>审核会检查实际效果、隐私说明、第三方服务依赖、版权与内容合规。格式通过只代表可以进入人工审核。</p></section>
<section id="schema"><h2>JSON Schema 下载</h2><p>编辑器可使用以下 Schema 检查字段、枚举与数值范围。GitHub 中的 shiyu.json 仍以时遇后端校验结果为准。</p><div class="downloads"><a href="/market/schema/plugin.shiyu.schema.json" download>插件 Schema</a><a href="/market/schema/theme.shiyu.schema.json" download>主题 Schema</a><a href="/market/submit">开始提交</a></div></section>
</article></div><script>
const examples={plugin:${JSON.stringify(pluginExample)},theme:${JSON.stringify(themeExample)}};
document.querySelectorAll('[data-copy]').forEach(function(button){button.addEventListener('click',async function(){const previous=button.textContent;try{await navigator.clipboard.writeText(examples[button.dataset.copy]);button.textContent='已复制';setTimeout(function(){button.textContent=previous},1400)}catch{button.textContent='请手动复制';setTimeout(function(){button.textContent=previous},1400)}})});
</script></body></html>`;
}
