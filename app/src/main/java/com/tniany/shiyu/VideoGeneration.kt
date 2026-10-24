package com.tniany.shiyu

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

internal data class VideoProvider(
  val id:String,
  val name:String,
  val protocol:String,
  val endpoint:String,
  val model:String
) {
  fun json()=JSONObject().put("id",id).put("name",name).put("protocol",protocol).put("endpoint",endpoint).put("model",model)
  companion object {
    fun from(value:JSONObject)=VideoProvider(value.getString("id"),value.getString("name"),value.getString("protocol"),value.getString("endpoint"),value.getString("model"))
  }
}

internal data class VideoTask(
  val id:String,
  val provider:VideoProvider,
  val prompt:String,
  val createdAt:Long,
  val remoteId:String="",
  val status:String="submitting",
  val url:String="",
  val file:String="",
  val error:String=""
) {
  val pending get()=status in listOf("submitting","pending","queued","running","processing")
  val succeeded get()=status in listOf("succeeded","completed","success")
  fun json()=JSONObject().put("id",id).put("provider",provider.json()).put("prompt",prompt).put("createdAt",createdAt)
    .put("remoteId",remoteId).put("status",status).put("url",url).put("file",file).put("error",error)
  companion object {
    fun from(value:JSONObject)=VideoTask(value.getString("id"),VideoProvider.from(value.getJSONObject("provider")),value.getString("prompt"),value.getLong("createdAt"),value.optString("remoteId"),value.optString("status"),value.optString("url"),value.optString("file"),value.optString("error"))
  }
}

internal object VideoGeneration {
  private const val PROVIDERS="video_providers"
  private const val TASKS="video_tasks"
  private fun prefs(context:Context)=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE)
  private fun values(raw:String)=runCatching{JSONArray(raw)}.getOrElse{JSONArray()}
  fun providers(context:Context):List<VideoProvider> {
    val values=values(prefs(context).getString(PROVIDERS,"[]").orEmpty())
    return (0 until values.length()).mapNotNull{runCatching{VideoProvider.from(values.getJSONObject(it))}.getOrNull()}
  }
  fun tasks(context:Context):List<VideoTask> {
    val values=values(prefs(context).getString(TASKS,"[]").orEmpty())
    return (0 until values.length()).mapNotNull{runCatching{VideoTask.from(values.getJSONObject(it))}.getOrNull()}.sortedByDescending{it.createdAt}
  }
  fun key(context:Context,id:String)=SecureStore(context).getNamed("video_key_$id")
  fun activeId(context:Context)=prefs(context).getString("video_active_provider","").orEmpty()
  fun select(context:Context,id:String){prefs(context).edit().putString("video_active_provider",id).apply()}

  fun endpoint(raw:String):String {
    val value=raw.trim().trimEnd('/')
    val uri=URI(value)
    require(uri.scheme.equals("https",true)&&!uri.host.isNullOrBlank()&&uri.userInfo==null&&uri.query==null&&uri.fragment==null){"请填写不含密钥或查询参数的 HTTPS Base URL"}
    return value
  }

  @Synchronized fun saveProvider(context:Context,provider:VideoProvider,secret:String) {
    require(provider.name.isNotBlank()&&provider.model.isNotBlank()&&secret.isNotBlank()){ "请填写名称、模型和 API Key" }
    require(provider.protocol in listOf("dashscope","ark")){"不支持的视频协议"}
    val saved=provider.copy(endpoint=endpoint(provider.endpoint))
    SecureStore(context).putNamed("video_key_${saved.id}",secret.trim())
    val updated=providers(context).filterNot{it.id==saved.id}+saved
    check(prefs(context).edit().putString(PROVIDERS,JSONArray(updated.map{it.json()}).toString()).putString("video_active_provider",saved.id).commit()){ "无法保存视频供应商" }
  }

  @Synchronized fun deleteProvider(context:Context,id:String) {
    require(tasks(context).none{it.provider.id==id&&(it.pending||it.status=="submit_unknown")}){"仍有未完成的视频任务，请先处理或移除任务记录"}
    val updated=providers(context).filterNot{it.id==id}
    prefs(context).edit().putString(PROVIDERS,JSONArray(updated.map{it.json()}).toString()).apply()
    SecureStore(context).putNamed("video_key_$id","")
    if(activeId(context)==id)select(context,updated.firstOrNull()?.id.orEmpty())
  }

  @Synchronized private fun store(context:Context,task:VideoTask):VideoTask {
    val updated=(tasks(context).filterNot{it.id==task.id}+task).sortedByDescending{it.createdAt}
    check(prefs(context).edit().putString(TASKS,JSONArray(updated.map{it.json()}).toString()).commit()){ "无法保存视频任务" }
    return task
  }

  @Synchronized fun remove(context:Context,id:String) {
    prefs(context).edit().putString(TASKS,JSONArray(tasks(context).filterNot{it.id==id}.map{it.json()}).toString()).apply()
  }

  fun requestBody(provider:VideoProvider,prompt:String,seconds:Int,resolution:String,ratio:String,extra:String):JSONObject {
    require(prompt.isNotBlank()&&prompt.length<=4000){"视频描述应为 1–4000 字"}
    require(seconds in 2..30){"时长应为 2–30 秒，具体范围由模型决定"}
    val parameters=JSONObject(extra.ifBlank{"{}"})
    parameters.put("duration",seconds).put("resolution",resolution).put("ratio",ratio)
    return if(provider.protocol=="dashscope") {
      JSONObject().put("model",provider.model).put("input",JSONObject().put("prompt",prompt))
        .put("parameters",parameters)
    } else {
      parameters.put("model",provider.model).put("content",JSONArray().put(JSONObject().put("type","text").put("text",prompt)))
    }
  }

  fun submit(context:Context,provider:VideoProvider,prompt:String,seconds:Int,resolution:String,ratio:String,extra:String):VideoTask {
    val body=requestBody(provider,prompt.trim(),seconds,resolution,ratio,extra)
    endpoint(provider.endpoint)
    val secret=key(context,provider.id)
    require(secret.isNotBlank()){"请配置视频供应商 API Key"}
    var task=store(context,VideoTask(UUID.randomUUID().toString(),provider,prompt.trim(),System.currentTimeMillis()))
    try {
      val route=if(provider.protocol=="dashscope")"/services/aigc/video-generation/video-synthesis" else "/contents/generations/tasks"
      val result=request(provider.endpoint+route,secret,body,provider.protocol=="dashscope")
      val remoteId=if(provider.protocol=="dashscope")result.optJSONObject("output")?.optString("task_id").orEmpty() else result.optString("id")
      require(remoteId.matches(Regex("[A-Za-z0-9_.:-]{1,200}"))){"接口未返回有效任务 ID；请在供应商后台确认是否已创建，勿重复提交"}
      task=task.copy(remoteId=remoteId,status="pending")
    } catch(error:Exception) {
      task=task.copy(status="submit_unknown",error="提交未确认：${safeError(error)}。请先检查供应商后台，重复提交可能重复计费。")
    }
    return store(context,task)
  }

  fun parseResult(task:VideoTask,result:JSONObject):VideoTask {
    val output=if(task.provider.protocol=="dashscope")result.optJSONObject("output")?:result else result
    val status=output.optString(if(task.provider.protocol=="dashscope")"task_status" else "status").lowercase()
    require(status.isNotBlank()){"任务查询未返回状态"}
    val remoteError=output.optJSONObject("error")?:result.optJSONObject("error")
    val message=remoteError?.optString("message").orEmpty().ifBlank{output.optString("message")}.take(600)
    val url=if(task.provider.protocol=="dashscope")output.optString("video_url") else output.optJSONObject("content")?.optString("video_url").orEmpty()
    if(url.isNotBlank())endpointForDownload(url)
    return task.copy(status=status,url=url.ifBlank{task.url},error=message)
  }

  fun refresh(context:Context,task:VideoTask):VideoTask {
    require(task.remoteId.isNotBlank()){"没有任务 ID，不能查询；请先检查供应商后台"}
    val provider=providers(context).firstOrNull{it.id==task.provider.id}?:error("原视频供应商已删除")
    require(provider.endpoint==task.provider.endpoint&&provider.protocol==task.provider.protocol){"原任务的供应商地址或协议已更改，请恢复后查询"}
    val secret=key(context,provider.id)
    require(secret.isNotBlank()){"视频供应商 API Key 为空"}
    val route=if(provider.protocol=="dashscope")"/tasks/" else "/contents/generations/tasks/"
    return store(context,parseResult(task,request(provider.endpoint+route+task.remoteId,secret,null,false)))
  }

  fun download(context:Context,task:VideoTask):VideoTask {
    require(task.succeeded&&task.url.isNotBlank()){"任务尚未返回可下载的视频"}
    var address=endpointForDownload(task.url)
    val directory=File(context.filesDir,"generated_videos").apply{mkdirs()}
    val target=File(directory,"${task.id}.mp4")
    val temporary=File(directory,"${task.id}.part")
    try {
      for(redirect in 0..4) {
        val connection=URL(address).openConnection() as HttpURLConnection
        try {
          connection.connectTimeout=20_000;connection.readTimeout=60_000;connection.instanceFollowRedirects=false
          val code=connection.responseCode
          if(code in listOf(301,302,303,307,308)) {
            require(redirect<4){"视频下载重定向过多"}
            address=endpointForDownload(URI(address).resolve(connection.getHeaderField("Location")?:error("下载跳转地址为空")).toString())
            continue
          }
          check(code in 200..299){"视频下载 HTTP $code；链接可能已过期"}
          connection.inputStream.use{input->temporary.outputStream().use{output->
            val buffer=ByteArray(64*1024);var total=0L
            while(true){val count=input.read(buffer);if(count<0)break;total+=count;require(total<=256L*1024*1024){"视频超过 256 MB 下载限制"};output.write(buffer,0,count)}
          }}
          require(temporary.length()>12){"下载的视频为空"}
          val header=temporary.inputStream().use{input->ByteArray(12).also{input.read(it)}}
          require(String(header,4,4,Charsets.US_ASCII)=="ftyp"){"服务未返回有效 MP4 视频"}
          check(temporary.renameTo(target)){"无法保存视频文件"}
          return store(context,task.copy(file=target.absolutePath))
        } finally {connection.disconnect()}
      }
      error("无法下载视频")
    } finally {temporary.delete()}
  }

  private fun endpointForDownload(raw:String):String {
    val uri=URI(raw)
    require(uri.scheme.equals("https",true)&&!uri.host.isNullOrBlank()&&uri.userInfo==null){"视频结果必须为 HTTPS 地址"}
    return raw
  }

  private fun request(address:String,secret:String,body:JSONObject?,async:Boolean):JSONObject {
    val connection=URL(address).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout=20_000;connection.readTimeout=60_000;connection.instanceFollowRedirects=false
      connection.setRequestProperty("Authorization","Bearer $secret")
      connection.setRequestProperty("Accept","application/json")
      if(body!=null) {
        connection.requestMethod="POST";connection.doOutput=true
        connection.setRequestProperty("Content-Type","application/json; charset=utf-8")
        if(async)connection.setRequestProperty("X-DashScope-Async","enable")
        connection.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
      }
      val code=connection.responseCode
      if(code !in 200..299)throw IOException("视频接口 HTTP $code（请检查额度、模型、地址和权限）")
      val raw=connection.inputStream.bufferedReader().use{reader->
        val output=StringBuilder();val buffer=CharArray(8192)
        while(true){val count=reader.read(buffer);if(count<0)break;output.append(buffer,0,count);require(output.length<=1_048_576){"视频接口响应过大"}}
        output.toString()
      }
      val result=JSONObject(raw)
      if(result.has("code")&&!result.has("output"))error("视频接口错误：${result.optString("code").take(80)}")
      if(result.has("error"))error("视频接口拒绝请求，请检查模型和参数")
      return result
    } finally {connection.disconnect()}
  }

  fun safeError(error:Throwable)=if(error is IOException)error.message.orEmpty().substringBefore("https://").take(180) else error.message.orEmpty().take(240)
}
