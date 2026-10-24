package com.tniany.shiyu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun videoUri(context:Context,path:String):Uri {
  val file=File(path)
  require(file.isFile&&file.canonicalPath.startsWith(File(context.filesDir,"generated_videos").canonicalPath+File.separator)){"视频文件不存在，请重新下载"}
  return FileProvider.getUriForFile(context,"${context.packageName}.files",file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoStudio(context:Context,onBack:()->Unit,onSend:((VideoTask)->Unit)?=null) {
  val scope=rememberCoroutineScope()
  var providers by remember{mutableStateOf(VideoGeneration.providers(context))}
  var tasks by remember{mutableStateOf(VideoGeneration.tasks(context))}
  var selected by rememberSaveable{mutableStateOf(VideoGeneration.activeId(context))}
  var editing by rememberSaveable{mutableStateOf(false)}
  var editingId by rememberSaveable{mutableStateOf("")}
  var name by rememberSaveable{mutableStateOf("")}
  var protocol by rememberSaveable{mutableStateOf("dashscope")}
  var endpoint by rememberSaveable{mutableStateOf("")}
  var model by rememberSaveable{mutableStateOf("")}
  var secret by remember{mutableStateOf("")}
  var prompt by rememberSaveable{mutableStateOf("")}
  var seconds by rememberSaveable{mutableStateOf("5")}
  var resolution by rememberSaveable{mutableStateOf("720P")}
  var ratio by rememberSaveable{mutableStateOf("16:9")}
  var extra by rememberSaveable{mutableStateOf("{}")}
  var message by remember{mutableStateOf("")}
  var busy by remember{mutableStateOf(false)}
  var autoPoll by rememberSaveable{mutableStateOf(true)}
  var confirmSubmit by remember{mutableStateOf(false)}
  var deleteProvider by remember{mutableStateOf<VideoProvider?>(null)}
  var removeTask by remember{mutableStateOf<VideoTask?>(null)}
  var preview by remember{mutableStateOf<VideoTask?>(null)}
  var exportFile by remember{mutableStateOf("")}
  fun reload(){providers=VideoGeneration.providers(context);tasks=VideoGeneration.tasks(context)}
  fun edit(provider:VideoProvider?){
    editingId=provider?.id.orEmpty();name=provider?.name.orEmpty();protocol=provider?.protocol?:"dashscope"
    endpoint=provider?.endpoint.orEmpty();model=provider?.model.orEmpty();secret=provider?.let{VideoGeneration.key(context,it.id)}.orEmpty();editing=true
  }
  fun action(block:()->Unit){
    if(busy)return
    busy=true;message=""
    scope.launch{try{withContext(Dispatchers.IO){block()};reload()}catch(error:Exception){message=VideoGeneration.safeError(error)}finally{busy=false}}
  }
  val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")){uri->
    if(uri!=null){val source=exportFile;action{videoUri(context,source);context.contentResolver.openOutputStream(uri,"w")?.use{output->File(source).inputStream().use{it.copyTo(output)}}?:error("无法写入保存位置")}}
  }
  val pendingIds=tasks.filter{it.pending&&it.remoteId.isNotBlank()}.map{it.id}
  LaunchedEffect(pendingIds,autoPoll){
    if(!autoPoll)return@LaunchedEffect
    while(true){
      delay(15_000)
      val pending=VideoGeneration.tasks(context).filter{it.id in pendingIds&&it.pending&&System.currentTimeMillis()-it.createdAt<10*60_000}
      if(pending.isEmpty())break
      try{withContext(Dispatchers.IO){pending.forEach{VideoGeneration.refresh(context,it)}};tasks=VideoGeneration.tasks(context)}catch(error:Exception){message="自动查询暂停：${VideoGeneration.safeError(error)}；可手动查询，不要重新提交。";break}
    }
  }
  Scaffold(topBar={TopAppBar(title={Text("视频生成")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Outlined.ArrowBack,"返回")}})}){padding->
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      item{Text("视频供应商独立于聊天与生图配置。提交会消耗供应商额度；生成通常需要数分钟，退出页面不会取消云端任务。",style=MaterialTheme.typography.bodySmall)}
      item{Text("视频模型供应商",fontWeight=FontWeight.SemiBold)}
      items(providers,key={it.id}){provider->
        Card(Modifier.fillMaxWidth()){
          Row(Modifier.fillMaxWidth().padding(12.dp)){
            RadioButton(selected==provider.id,onClick={selected=provider.id;VideoGeneration.select(context,provider.id)},enabled=!busy)
            Column(Modifier.weight(1f).padding(top=10.dp)){Text(provider.name,fontWeight=FontWeight.Medium);Text(provider.model,style=MaterialTheme.typography.bodySmall);Text(if(provider.protocol=="ark")"火山方舟" else "通义万相",style=MaterialTheme.typography.labelSmall)}
            IconButton(onClick={edit(provider)},enabled=!busy){Icon(Icons.Outlined.Edit,"编辑视频供应商")}
            IconButton(onClick={deleteProvider=provider},enabled=!busy){Icon(Icons.Outlined.Delete,"删除视频供应商")}
          }
        }
      }
      item{OutlinedButton(onClick={edit(null)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text("新增视频供应商")}}
      if(editing){
        item{Text(if(editingId.isBlank())"新增配置" else "编辑配置",fontWeight=FontWeight.SemiBold)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
          FilterChip(protocol=="dashscope",onClick={protocol="dashscope";endpoint="";model="wan2.7-t2v"},label={Text("通义万相")},enabled=!busy)
          FilterChip(protocol=="ark",onClick={protocol="ark";endpoint="https://ark.cn-beijing.volces.com/api/v3";model=""},label={Text("火山方舟")},enabled=!busy)
        }}
        item{OutlinedTextField(name,{name=it.take(80)},Modifier.fillMaxWidth(),label={Text("供应商名称")},enabled=!busy,singleLine=true)}
        item{OutlinedTextField(endpoint,{endpoint=it.take(500)},Modifier.fillMaxWidth(),label={Text("HTTPS Base URL")},supportingText={Text(if(protocol=="ark")"例如 https://ark.cn-beijing.volces.com/api/v3" else "百炼控制台业务空间地址，例如 https://业务空间ID.cn-beijing.maas.aliyuncs.com/api/v1")},enabled=!busy,singleLine=true)}
        item{OutlinedTextField(model,{model=it.take(150)},Modifier.fillMaxWidth(),label={Text(if(protocol=="ark")"模型 ID / 推理接入点 ID" else "模型 ID，例如 wan2.7-t2v")},enabled=!busy,singleLine=true)}
        item{OutlinedTextField(secret,{secret=it},Modifier.fillMaxWidth(),label={Text("视频 API Key")},visualTransformation=PasswordVisualTransformation(),enabled=!busy,singleLine=true)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
          Button(onClick={runCatching{val provider=VideoProvider(editingId.ifBlank{UUID.randomUUID().toString()},name.trim(),protocol,endpoint.trim(),model.trim());VideoGeneration.saveProvider(context,provider,secret);selected=provider.id;editing=false;secret="";reload();message="视频供应商已保存"}.onFailure{message=VideoGeneration.safeError(it)}},enabled=!busy){Text("保存配置")}
          TextButton(onClick={editing=false;secret=""},enabled=!busy){Text("取消")}
        }}
      }
      item{HorizontalDivider();Text("创建视频",Modifier.padding(top=12.dp),fontWeight=FontWeight.SemiBold)}
      item{OutlinedTextField(prompt,{prompt=it.take(4000)},Modifier.fillMaxWidth(),label={Text("描述画面、动作、镜头和风格")},minLines=3,enabled=!busy)}
      item{OutlinedTextField(seconds,{seconds=it.filter(Char::isDigit).take(2)},Modifier.fillMaxWidth(),label={Text("视频时长（秒）")},supportingText={Text("以具体模型支持范围为准，通常 5 或 10 秒")},singleLine=true,enabled=!busy)}
      item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("480P","720P","1080P").forEach{value->FilterChip(resolution==value,onClick={resolution=value},label={Text(value)},enabled=!busy)}}}
      item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("16:9","9:16","1:1").forEach{value->FilterChip(ratio==value,onClick={ratio=value},label={Text(value)},enabled=!busy)}}}
      item{OutlinedTextField(extra,{extra=it.take(4000)},Modifier.fillMaxWidth(),label={Text("可选模型参数 JSON")},supportingText={Text("如 {\"watermark\":true}。时长、分辨率、比例和提示词以上方为准；万相使用新版 resolution/ratio 参数。")},enabled=!busy,minLines=2)}
      item{Button(onClick={val provider=providers.firstOrNull{it.id==selected};if(provider==null)message="请先新增并选择视频供应商" else runCatching{VideoGeneration.requestBody(provider,prompt,seconds.toIntOrNull()?:0,if(provider.protocol=="ark")resolution.lowercase() else resolution,ratio,extra)}.onSuccess{confirmSubmit=true}.onFailure{message=VideoGeneration.safeError(it)}},enabled=!busy&&prompt.isNotBlank()&&providers.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Movie,null);Spacer(Modifier.width(8.dp));Text("生成视频（消耗供应商额度）")}}
      if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
      if(message.isNotBlank())item{Text(message,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)}
      item{HorizontalDivider();Text("视频任务与结果",Modifier.padding(top=12.dp),fontWeight=FontWeight.SemiBold)}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("自动查询（15 秒 / 最长 10 分钟）",style=MaterialTheme.typography.bodySmall);Switch(autoPoll,{autoPoll=it})}}
      if(tasks.isEmpty())item{Text("还没有视频任务。",style=MaterialTheme.typography.bodySmall)}
      items(tasks,key={it.id}){task->
        Card(Modifier.fillMaxWidth()){
          Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(task.prompt,maxLines=3,fontWeight=FontWeight.Medium)
            Text("${task.provider.name} · ${videoStatus(task.status)}",style=MaterialTheme.typography.bodySmall)
            if(task.remoteId.isNotBlank())Text("任务 ID：${task.remoteId}",style=MaterialTheme.typography.labelSmall)
            if(task.error.isNotBlank())Text(task.error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
            if(task.status=="submitting"&&task.remoteId.isBlank())Text("若长时间停留在提交中，请检查供应商后台，不要重复提交。",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
              if(task.remoteId.isNotBlank())TextButton(onClick={action{VideoGeneration.refresh(context,task)}},enabled=!busy){Text("查询状态")}
              if(task.succeeded&&task.url.isNotBlank())TextButton(onClick={action{VideoGeneration.download(context,task)}},enabled=!busy){Text(if(task.file.isBlank())"下载视频" else "重新下载")}
              TextButton(onClick={removeTask=task},enabled=!busy){Text("移除记录")}
            }
            if(task.file.isNotBlank()&&File(task.file).isFile){
              Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                TextButton(onClick={preview=task},enabled=!busy){Text("播放")}
                TextButton(onClick={exportFile=task.file;export.launch("shiyu-video-${task.id.take(8)}.mp4")},enabled=!busy){Text("另存为")}
                TextButton(onClick={runCatching{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM,videoUri(context,task.file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"分享 AI 生成视频"))}.onFailure{message="无法打开分享面板"}},enabled=!busy){Text("分享")}
              }
              if(onSend!=null)OutlinedButton(onClick={onSend(task)},enabled=!busy){Text("添加到当前会话")}
            }
            if(task.succeeded&&task.file.isBlank())Text("结果链接可能过期，请及时下载到本机。",style=MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
  if(confirmSubmit)AlertDialog(onDismissRequest={confirmSubmit=false},title={Text("确认生成视频？")},text={Text("将向 ${providers.firstOrNull{it.id==selected}?.name.orEmpty()} 发送画面描述，生成 ${seconds} 秒视频并产生费用。仅提交一次；超时后请查询任务，不要重复下单。")},confirmButton={TextButton(onClick={confirmSubmit=false;val provider=providers.firstOrNull{it.id==selected}?:return@TextButton;val description=prompt;val duration=seconds.toIntOrNull()?:5;val chosenResolution=if(provider.protocol=="ark")resolution.lowercase() else resolution;val chosenRatio=ratio;val parameters=extra;action{VideoGeneration.submit(context,provider,description,duration,chosenResolution,chosenRatio,parameters)}}){Text("确认并提交")}},dismissButton={TextButton(onClick={confirmSubmit=false}){Text("取消")}})
  deleteProvider?.let{provider->AlertDialog(onDismissRequest={deleteProvider=null},title={Text("删除视频供应商？")},text={Text("删除 ${provider.name} 的配置及本地密钥。")},confirmButton={TextButton(onClick={deleteProvider=null;action{VideoGeneration.deleteProvider(context,provider.id)}}){Text("删除")}},dismissButton={TextButton(onClick={deleteProvider=null}){Text("取消")}})}
  removeTask?.let{task->AlertDialog(onDismissRequest={removeTask=null},title={Text("仅移除本地记录？")},text={Text("此操作不会取消云端生成，也不会退费；已下载的视频文件保留。未完成任务移除后需到供应商后台查询。")},confirmButton={TextButton(onClick={VideoGeneration.remove(context,task.id);removeTask=null;reload()}){Text("移除记录")}},dismissButton={TextButton(onClick={removeTask=null}){Text("保留")}})}
  preview?.let{task->AlertDialog(onDismissRequest={preview=null},title={Text("AI 生成视频")},text={VideoPreview(context,task.file)},confirmButton={TextButton(onClick={preview=null}){Text("关闭")}})}
}

private fun videoStatus(status:String)=when(status){
  "submitting"->"提交中";"submit_unknown"->"提交状态待核实";"pending","queued"->"排队中";"running","processing"->"生成中";"succeeded","completed","success"->"已完成";"failed"->"生成失败";"canceled","cancelled"->"已取消";"expired","unknown"->"任务过期或不存在";else->status
}

@Composable
private fun VideoPreview(context:Context,path:String){
  var player by remember{mutableStateOf<VideoView?>(null)}
  DisposableEffect(path){onDispose{player?.stopPlayback()}}
  AndroidView(factory={VideoView(it).apply{player=this;setMediaController(MediaController(it).also{control->control.setAnchorView(this)});setVideoURI(videoUri(context,path));setOnPreparedListener{start()}}},modifier=Modifier.fillMaxWidth().height(280.dp))
}
