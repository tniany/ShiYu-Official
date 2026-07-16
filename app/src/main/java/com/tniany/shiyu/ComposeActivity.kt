package com.tniany.shiyu

import android.app.Activity
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

private enum class Page { ADULT_CONFIRM, ONBOARDING, COMMUNITY_INVITE, FEEDBACK, TERMS, MAIN, CHAT, CHAT_DETAIL, CHAT_CONTENT, CHAT_CONTEXT, CHAT_TTS, PROACTIVE, YANDERE, MOMENT_SHARE, CHAT_DEFAULTS, GENERATION_TASKS, ROLE_SETTINGS, MEMORY, RENAME_CHAT, CREATE_ROLE, GROUP_SETUP, CARD_CENTER, CARD_EXPORT, IMPORT_PREVIEW, MOMENTS, MOMENT_PERMISSIONS, MOMENT_ROLE_POST, MOMENT_COMMENTS, API, API_ERROR, PROVIDER, ADD_PROVIDER, CONNECTION, MODEL, GENERATION, WEB_SEARCH, EMBEDDING_LIBRARY, IMAGE_GENERATION, TTS, APPEARANCE, CHAT_APPEARANCE, FRAME_RATE, DATA, BACKUP, STICKER_RESOURCES, REALITY_AWARENESS, EXPERIMENTAL, WEIXIN_LINK, ABOUT, SUPPORT, USER_PROFILE, IDENTITIES, APP_BEHAVIOR }

class ComposeActivity : MainActivity() {
  private val adultConsentRevision="2026-07-14"
  private data class ProviderConfig(val id:String,val name:String,val endpoint:String,val modelName:String,val builtIn:Boolean=false)
  private data class IdentityConfig(val id:String,val name:String,val description:String)
  private data class MomentPost(val id:String,val authorId:String,val authorName:String,val content:String,val image:String,val createdAt:Long)
  private data class MomentComment(val id:String,val authorId:String,val authorName:String,val content:String,val createdAt:Long,val parentId:String="",val replyToName:String="")
  private data class DangerAction(val title:String,val message:String,val confirmLabel:String="确认删除",val action:()->Unit)
  private var composePage by mutableStateOf(Page.MAIN)
  private var selectedChat by mutableStateOf<Chat?>(null)
  private var uiRevision by mutableIntStateOf(0)
  private var editingProviderId by mutableStateOf<String?>(null)
  private var providerBackPage by mutableStateOf(Page.PROVIDER)
  private var imageGenerationBackPage by mutableStateOf(Page.API)
  private var embeddingBackPage by mutableStateOf(Page.API)
  private var webSearchBackPage by mutableStateOf(Page.API)
  private var editingCardId by mutableStateOf<String?>(null)
  private var roleBackPage by mutableStateOf(Page.MAIN)
  private var renameBackPage by mutableStateOf(Page.MAIN)
  private var termsBackPage by mutableStateOf(Page.ABOUT)
  private var ttsBackPage by mutableStateOf(Page.MAIN)
  private var stickerResourcesBackPage by mutableStateOf(Page.MAIN)
  private var pendingImportCard by mutableStateOf<Card?>(null)
  private var exportCardId by mutableStateOf<String?>(null)
  private var selectedMomentId by mutableStateOf("")
  private var momentRolePostBackPage by mutableStateOf(Page.MAIN)
  private var availableUpdateVersion by mutableStateOf("")
  private var availableUpdateUrl by mutableStateOf("")
  private var updateNoticeNonce by mutableIntStateOf(0)
  private var requestedMainTab by mutableIntStateOf(0)
  private var mainTabRequestNonce by mutableIntStateOf(0)
  private var pendingDanger by mutableStateOf<DangerAction?>(null)
  private var momentFeedScrollIndex=0
  private var momentFeedScrollOffset=0
  private val bitmapCache=java.util.concurrent.ConcurrentHashMap<String,android.graphics.Bitmap>()
  private val immersiveReplyTasks=java.util.concurrent.ConcurrentHashMap<String,Runnable>()

  private fun clearPersistedImmersiveReply(chatId:String){
    val root=runCatching{JSONObject(prefs.getString("immersive_pending_replies","{}").orEmpty())}.getOrElse{JSONObject()}
    root.remove(chatId);prefs.edit().putString("immersive_pending_replies",root.toString()).apply()
  }

  private fun scheduleImmersiveReply(chatId:String,targetId:String,delayMs:Long=1400L){
    immersiveReplyTasks.remove(chatId)?.let{main.removeCallbacks(it)}
    val root=runCatching{JSONObject(prefs.getString("immersive_pending_replies","{}").orEmpty())}.getOrElse{JSONObject()}
    root.put(chatId,JSONObject().put("targetId",targetId).put("dueAt",System.currentTimeMillis()+delayMs.coerceAtLeast(0L)))
    prefs.edit().putString("immersive_pending_replies",root.toString()).apply()
    lateinit var task:Runnable
    task=Runnable{
      if(immersiveReplyTasks[chatId]!==task)return@Runnable
      immersiveReplyTasks.remove(chatId)
      clearPersistedImmersiveReply(chatId)
      val chat=chats.firstOrNull{it.id==chatId}?:return@Runnable
      val target=find(targetId)?:find(chat.cardId)?:return@Runnable
      when{
        sending&&sendingChatId==chat.id->{restartImmersiveGeneration(chat);request(chat,target)}
        sending->queueCrossChatReply(chat,target)
        else->request(chat,target)
      }
    }
    immersiveReplyTasks[chatId]=task
    main.postDelayed(task,delayMs.coerceAtLeast(0L))
  }

  private fun restoreImmersiveReplies(){
    val root=runCatching{JSONObject(prefs.getString("immersive_pending_replies","{}").orEmpty())}.getOrElse{JSONObject()}
    val now=System.currentTimeMillis();val keys=mutableListOf<String>();root.keys().forEachRemaining{keys.add(it)}
    keys.forEach{chatId->val item=root.optJSONObject(chatId)?:return@forEach;val targetId=item.optString("targetId");if(targetId.isNotBlank())scheduleImmersiveReply(chatId,targetId,(item.optLong("dueAt",now)-now).coerceAtLeast(0L))else clearPersistedImmersiveReply(chatId)}
  }

  override fun onDestroy(){immersiveReplyTasks.values.forEach{main.removeCallbacks(it)};immersiveReplyTasks.clear();super.onDestroy()}

  private fun providers():List<ProviderConfig>{
    val custom=mutableListOf<ProviderConfig>();runCatching{val a=org.json.JSONArray(prefs.getString("custom_providers","[]"));for(i in 0 until a.length()){val j=a.getJSONObject(i);custom.add(ProviderConfig(j.getString("id"),j.getString("name"),j.getString("endpoint"),j.optString("model","gpt-4o-mini")))}}
    return custom
  }
  private fun saveCustomProviders(values:List<ProviderConfig>){val a=org.json.JSONArray();values.filter{!it.builtIn}.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("endpoint",it.endpoint).put("model",it.modelName))};prefs.edit().putString("custom_providers",a.toString()).apply()}
  private fun identities():List<IdentityConfig>{val values=mutableListOf<IdentityConfig>();runCatching{val a=JSONArray(prefs.getString("user_identities","[]"));for(i in 0 until a.length()){val j=a.getJSONObject(i);values.add(IdentityConfig(j.getString("id"),j.getString("name"),j.optString("description")))}};return values}
  private fun saveIdentities(values:List<IdentityConfig>){val a=JSONArray();values.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("description",it.description))};prefs.edit().putString("user_identities",a.toString()).apply()}
  private fun moments():List<MomentPost>{val values=mutableListOf<MomentPost>();runCatching{val a=JSONArray(prefs.getString("moments","[]"));for(i in 0 until a.length()){val j=a.optJSONObject(i)?:continue;val authorId=j.optString("authorId","user");val rawContent=j.optString("content");values.add(MomentPost(j.optString("id"),authorId,j.optString("authorName","我"),if(authorId=="user")rawContent else AiContentSanitizer.moment(rawContent),j.optString("image"),j.optLong("createdAt")))}};return values.sortedByDescending{it.createdAt}}
  private fun saveMoments(values:List<MomentPost>){val a=JSONArray();values.sortedByDescending{it.createdAt}.forEach{a.put(JSONObject().put("id",it.id).put("authorId",it.authorId).put("authorName",it.authorName).put("content",it.content).put("image",it.image).put("createdAt",it.createdAt))};prefs.edit().putString("moments",a.toString()).apply()}
  private fun momentComments(postId:String):List<MomentComment>{val out=mutableListOf<MomentComment>();runCatching{val a=JSONObject(prefs.getString("moment_comments","{}").orEmpty()).optJSONArray(postId)?:JSONArray();for(i in 0 until a.length()){val j=a.optJSONObject(i)?:continue;out.add(MomentComment(j.optString("id"),j.optString("authorId","user"),j.optString("authorName","我"),j.optString("content"),j.optLong("createdAt"),j.optString("parentId"),j.optString("replyToName")))}};return out.sortedBy{it.createdAt}}
  private fun saveMomentComments(postId:String,values:List<MomentComment>){val root=runCatching{JSONObject(prefs.getString("moment_comments","{}").orEmpty())}.getOrElse{JSONObject()};val a=JSONArray();values.sortedBy{it.createdAt}.forEach{a.put(JSONObject().put("id",it.id).put("authorId",it.authorId).put("authorName",it.authorName).put("content",it.content).put("createdAt",it.createdAt).put("parentId",it.parentId).put("replyToName",it.replyToName))};root.put(postId,a);prefs.edit().putString("moment_comments",root.toString()).apply()}
  private fun momentLikes(postId:String):Set<String>{val root=runCatching{JSONObject(prefs.getString("moment_likes","{}").orEmpty())}.getOrElse{JSONObject()};val values=root.optJSONArray(postId)?:JSONArray();return buildSet{for(i in 0 until values.length())values.optString(i).takeIf{it.isNotBlank()}?.let(::add)}}
  private fun saveMomentLikes(postId:String,values:Set<String>){val root=runCatching{JSONObject(prefs.getString("moment_likes","{}").orEmpty())}.getOrElse{JSONObject()};root.put(postId,JSONArray(values.toList()));prefs.edit().putString("moment_likes",root.toString()).apply()}
  private fun toggleMomentLike(postId:String,authorId:String="user"){val values=momentLikes(postId).toMutableSet();if(!values.add(authorId))values.remove(authorId);saveMomentLikes(postId,values);uiRevision++}
  private fun momentRoleIds(key:String)=prefs.getStringSet(key,emptySet())?.toSet().orEmpty()
  private fun saveMomentRoleIds(key:String,values:Set<String>){prefs.edit().putStringSet(key,values.toSet()).apply()}
  private fun askDanger(title:String,message:String,confirmLabel:String="确认删除",action:()->Unit){pendingDanger=DangerAction(title,message,confirmLabel,action)}
  private fun activateIdentity(value:IdentityConfig){prefs.edit().putString("active_identity_id",value.id).putString("active_identity_name",value.name).putString("active_identity_description",value.description).apply();uiRevision++;toast("已切换身份：${value.name}")}
  private fun activeProvider():ProviderConfig?{val id=prefs.getString("active_provider_id","")?:"";return providers().firstOrNull{it.id==id}}
  private fun activateProvider(value:ProviderConfig){prefs.edit().putString("active_provider_id",value.id).putString("provider",value.name).putString("endpoint",value.endpoint).putString("model",value.modelName).apply();runCatching{secure.put(secure.getNamed("provider_key_${value.id}"))};uiRevision++;toast("已切换到 ${value.name}")}
  private fun adultConsentGranted()=prefs.getString("adult_consent_revision","")==adultConsentRevision
  private fun pageDepth(value:Page)=when(value){Page.ADULT_CONFIRM,Page.ONBOARDING,Page.COMMUNITY_INVITE,Page.MAIN->0;Page.FEEDBACK,Page.TERMS,Page.API,Page.TTS,Page.CARD_CENTER,Page.APPEARANCE,Page.DATA,Page.STICKER_RESOURCES,Page.REALITY_AWARENESS,Page.EXPERIMENTAL,Page.ABOUT,Page.SUPPORT,Page.USER_PROFILE,Page.GROUP_SETUP,Page.APP_BEHAVIOR,Page.ROLE_SETTINGS,Page.RENAME_CHAT,Page.CREATE_ROLE,Page.MOMENTS,Page.GENERATION_TASKS->1;Page.WEIXIN_LINK,Page.IDENTITIES,Page.IMPORT_PREVIEW,Page.CARD_EXPORT,Page.CHAT,Page.FRAME_RATE,Page.CHAT_APPEARANCE,Page.BACKUP,Page.API_ERROR,Page.PROVIDER,Page.CONNECTION,Page.MODEL,Page.GENERATION,Page.WEB_SEARCH,Page.EMBEDDING_LIBRARY,Page.IMAGE_GENERATION,Page.MEMORY,Page.MOMENT_PERMISSIONS,Page.MOMENT_ROLE_POST,Page.MOMENT_COMMENTS,Page.CHAT_DEFAULTS->2;Page.CHAT_DETAIL,Page.ADD_PROVIDER->3;Page.CHAT_CONTENT,Page.CHAT_CONTEXT,Page.CHAT_TTS,Page.PROACTIVE,Page.YANDERE,Page.MOMENT_SHARE->4}
  private fun copyText(label:String,value:String){(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText(label,value));toast("$label 已复制")}
  private fun finishCommunityInvite(destination:Page=Page.MAIN){prefs.edit().putString("community_invite_version",BuildConfig.VERSION_NAME).apply();composePage=destination}
  private fun openQqGroup(){runCatching{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=1061590432&card_type=group&source=qrcode")))}.onFailure{copyText("时遇交流群号","1061590432");toast("未检测到 QQ，群号已复制")}}
  private fun openFeedbackWebsite(){runCatching{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(BuildConfig.FEEDBACK_PAGE_URL)))}.onFailure{toast("无法打开官网反馈页")}}
  private fun submitFeedback(category:String,content:String,contact:String,callback:(Boolean,String)->Unit){pool.execute{runCatching{val connection=URL(BuildConfig.FEEDBACK_API_URL).openConnection() as HttpURLConnection;connection.requestMethod="POST";connection.connectTimeout=15000;connection.readTimeout=20000;connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("X-ShiYu-App","android");val payload=JSONObject().put("category",category).put("content",content).put("contact",contact).put("version",BuildConfig.VERSION_NAME).put("source","android").put("website","").toString();connection.outputStream.use{it.write(payload.toByteArray(Charsets.UTF_8))};val code=connection.responseCode;val raw=read(if(code in 200..299)connection.inputStream else connection.errorStream);connection.disconnect();if(code !in 200..299)throw java.io.IOException(JSONObject(raw).optString("message","HTTP $code"));JSONObject(raw).optString("reference").ifBlank{"已提交"}}.onSuccess{reference->main.post{callback(true,reference)}}.onFailure{error->main.post{callback(false,error.message?:"提交失败")}}}}
  private fun parseImportedMessages(raw:String):List<Msg>{
    val clean=raw.removePrefix("\uFEFF").trim()
    val array=if(clean.startsWith("["))JSONArray(clean)else runCatching{val root=JSONObject(clean);root.optJSONArray("messages")?:root.optJSONArray("history")?:root.optJSONArray("chatHistory")?:root.optJSONArray("chat_history")?:root.optJSONArray("items")?:root.optJSONArray("data")?:root.optJSONObject("data")?.optJSONArray("messages")?:root.optJSONObject("conversation")?.optJSONArray("messages")?:root.optJSONObject("chat")?.optJSONArray("messages")?:root.optJSONArray("sessions")?.optJSONObject(0)?.optJSONArray("messages")?:root.optJSONArray("conversations")?.optJSONObject(0)?.optJSONArray("messages")?:root.optJSONArray("chats")?.optJSONObject(0)?.optJSONArray("messages")?:if(root.has("role")||root.has("mes"))JSONArray().put(root)else JSONArray()}.getOrElse{JSONArray().also{out->clean.lineSequence().map{it.trim()}.filter{it.startsWith("{")}.forEach{line->runCatching{out.put(JSONObject(line))}}}}
    fun content(value:Any?):String=when(value){is String->value;is JSONArray->buildList{for(i in 0 until value.length()){val part=value.optJSONObject(i);(part?.optString("text")?.takeIf{it.isNotBlank()}?:part?.optString("content")?.takeIf{it.isNotBlank()})?.let(::add)}}.joinToString("\n");else->""}
    return buildList{for(i in 0 until array.length()){val item=array.optJSONObject(i)?:continue;val rawRole=item.optString("role",item.optString("sender",item.optString("type"))).lowercase();val role=when{item.has("is_user")->if(item.optBoolean("is_user"))"user" else "assistant";rawRole in listOf("user","human","me","person")->"user";rawRole in listOf("assistant","ai","bot","character","char")->"assistant";else->continue};val body=item.optString("body").ifBlank{item.optString("text")}.ifBlank{item.optString("mes")}.ifBlank{content(item.opt("content"))}.trim();if(body.isNotBlank()){val msg=Msg(role,body);msg.image=item.optString("image","");msg.speakerCardId=item.optString("speakerCardId","");msg.displayMode=item.optInt("displayMode",0);add(msg)}}}
  }
  private fun importWeixinHistory(card:Card,raw:String):Int{
    return importWeixinHistory(card,raw,"")
  }
  private fun importWeixinHistory(card:Card,raw:String,chatId:String):Int{
    val parsed=parseImportedMessages(raw).toMutableList()
    if(parsed.isEmpty()){
      val linePattern=Regex("^(?:\\[?\\d{4}[-/.年]\\d{1,2}[-/.月]\\d{1,2}[^\\]]*\\]?\\s*)?([^:：]{1,40})[:：]\\s*(.+)$")
      raw.lineSequence().map(String::trim).filter{it.isNotBlank()}.forEach{line->
        val match=linePattern.find(line);val sender=match?.groupValues?.getOrNull(1).orEmpty().trim();val body=match?.groupValues?.getOrNull(2)?.trim().orEmpty()
        if(body.isNotBlank()){val role=if(sender.contains(card.name,true))"assistant" else "user";parsed.add(Msg(role,body).also{if(role=="assistant")it.speakerCardId=card.id;it.messageType="weixin_import"})}
      }
    }
    if(parsed.isEmpty())return 0
    parsed.filter{it.role=="assistant"&&it.speakerCardId.isBlank()}.forEach{it.speakerCardId=card.id};parsed.forEach{it.messageType="weixin_import"}
    val account=WeixinLinkClient.accounts(this).firstOrNull{it.cardId==card.id};val peer=account?.ownerId?.takeIf{it.isNotBlank()}?:"import:${UUID.randomUUID()}"
    val chat=chats.firstOrNull{chatId.isNotBlank()&&it.id==chatId&&!it.group&&it.cardId==card.id}
      ?:chats.firstOrNull{!it.group&&it.cardId==card.id&&it.externalSource=="weixin"&&it.externalPeer==peer}
      ?:Chat().also{it.id=UUID.randomUUID().toString();it.cardId=card.id;it.title="微信 · ${card.name}";it.updated=System.currentTimeMillis();chats.add(it)}
    if(account!=null&&account.chatId.isNotBlank()&&account.chatId!=chat.id)chats.firstOrNull{it.id==account.chatId}?.let{it.externalSource="";it.externalPeer=""}
    chat.externalSource="weixin";chat.externalPeer=peer
    chat.messages.addAll(parsed);chat.updated=System.currentTimeMillis();save()
    if(account!=null&&account.chatId!=chat.id)WeixinLinkClient.setBinding(this,account.id,card.id,chat.id)
    uiRevision++;return parsed.size
  }
  private fun formatFrequency(minutes:Int)=when{minutes<60->"${minutes} 分钟";minutes%1440==0->"${minutes/1440} 天";minutes%60==0->"${minutes/60} 小时";else->"${minutes/60} 小时 ${minutes%60} 分"}
  private fun batteryWhitelistEnabled():Boolean = android.os.Build.VERSION.SDK_INT < 23 ||
      (getSystemService(POWER_SERVICE) as android.os.PowerManager).isIgnoringBatteryOptimizations(packageName)
  private fun requestBatteryWhitelist(){
    if(android.os.Build.VERSION.SDK_INT<23)return
    runCatching{startActivity(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,android.net.Uri.parse("package:$packageName")))}
      .recoverCatching{startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}
      .onFailure{toast("无法打开电池优化设置")}
  }
  private fun removeChat(chat:Chat){WeixinLinkClient.accounts(this).filter{it.chatId==chat.id}.forEach{WeixinLinkClient.setChatId(this,it.id,"")};immersiveReplyTasks.remove(chat.id)?.let{main.removeCallbacks(it)};clearPersistedImmersiveReply(chat.id);if(sendingChatId==chat.id)stopGeneration();queuedReplyTargets.remove(chat.id);queuedMentionTargets.remove(chat.id);crossChatQueue.removeIf{it==chat.id};db.deleteChatRealtime(chat.id);chats.removeAll{it.id==chat.id};if(selectedChat?.id==chat.id){selectedChat=null;composePage=Page.MAIN};save();YandereMessageService.sync(this);uiRevision++}
  private fun removeCard(card:Card){
    val deleted=mutableListOf<Chat>()
    chats.forEach{chat->if(!chat.group){if(chat.cardId==card.id)deleted.add(chat)}else if(chat.cardIds.remove(card.id)){if(chat.activeCardId==card.id)chat.activeCardId=chat.cardIds.firstOrNull().orEmpty();if(chat.cardId==card.id)chat.cardId=chat.cardIds.firstOrNull().orEmpty();if(chat.cardIds.size<2)deleted.add(chat)}}
    deleted.forEach{immersiveReplyTasks.remove(it.id)?.let{task->main.removeCallbacks(task)};clearPersistedImmersiveReply(it.id);if(sendingChatId==it.id)stopGeneration();queuedReplyTargets.remove(it.id);queuedMentionTargets.remove(it.id);crossChatQueue.removeIf{id->id==it.id};db.deleteChatRealtime(it.id)};chats.removeAll(deleted.toSet());WeixinLinkClient.disconnectCard(this,card.id);cards.removeAll{it.id==card.id};db.deleteCardMemories(card.id);db.deleteCardRealtime(card.id);if(card.avatar.isNotBlank())runCatching{File(card.avatar).delete()};if(selectedChat!=null&&!chats.any{it.id==selectedChat?.id}){selectedChat=null;composePage=Page.MAIN};save();YandereMessageService.sync(this);DesktopPetService.sync(this);uiRevision++
  }
  private fun discardPendingImport(){pendingImportCard?.avatar?.takeIf{it.isNotBlank()}?.let{path->val file=File(path);if(file.parentFile==cacheDir){bitmapCache.remove(path);runCatching{file.delete()}}};pendingImportCard=null;composePage=Page.CARD_CENTER}
  private fun confirmPendingImport(){val card=pendingImportCard?:return;runCatching{card.avatar.takeIf{it.isNotBlank()}?.let{path->val source=File(path);if(source.parentFile==cacheDir&&source.exists()){val target=File(filesDir,"avatar_${card.id}.png");source.copyTo(target,overwrite=true);bitmapCache.remove(path);source.delete();card.avatar=target.absolutePath}};cards.add(card);save()}.onSuccess{pendingImportCard=null;uiRevision++;toast("已导入 ${card.name}");composePage=Page.CARD_CENTER}.onFailure{toast("导入保存失败：${it.message}")}}
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    restoreImmersiveReplies()
    composePage=when{!adultConsentGranted()->Page.ADULT_CONFIRM;!prefs.getBoolean("onboarding_done",false)->Page.ONBOARDING;prefs.getString("community_invite_version","")!=BuildConfig.VERSION_NAME->Page.COMMUNITY_INVITE;else->Page.MAIN}
    if(intent?.getBooleanExtra("open_moments",false)==true)requestedMainTab=2
    if(composePage==Page.MAIN)intent?.getStringExtra("open_chat_id")?.takeIf{it.isNotBlank()}?.let{id->chats.firstOrNull{it.id==id}?.let{selectedChat=it;composePage=Page.CHAT}}
    setContent {
      uiRevision
      var themeMode by remember { mutableIntStateOf(prefs.getInt("compose_theme", 0)) }
      var themeStyle by remember { mutableIntStateOf(prefs.getInt("compose_theme_style", 0)) }
      val systemDark = isSystemInDarkTheme()
      val dark = themeMode == 2 || (themeMode == 0 && systemDark)
      val baseColors = themePalette(themeStyle,dark)
      val colors = if(prefs.getBoolean("experimental_glass_ui",false)) glassPalette(baseColors,prefs.getInt("experimental_glass_opacity",78)) else baseColors
      MaterialTheme(colorScheme = colors, typography = Typography()) {
        val transparentChatStatus=composePage==Page.CHAT&&prefs.getBoolean("experimental_top_bar_transparent",false)
        val statusBackground=storedColor(prefs.getString("chat_background_color","").orEmpty(),MaterialTheme.colorScheme.background)
        val statusImage=prefs.getString("chat_background_image","").orEmpty()
        val statusOverlay=prefs.getInt("chat_background_overlay",20).coerceIn(0,70)/100f
        val darkStatusIcons=remember(transparentChatStatus,statusBackground,statusImage,statusOverlay,dark){if(transparentChatStatus)chatStatusBarDarkIcons(statusBackground,statusImage,statusOverlay)else !dark}
        SyncSystemBars(dark,transparentChatStatus,darkStatusIcons)
        ShiYuApp(themeMode,{ value -> themeMode = value },themeStyle) { value -> themeStyle = value }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if(!adultConsentGranted()){
      selectedChat=null
      composePage=Page.ADULT_CONFIRM
      uiRevision++
      return
    }
    syncBackgroundStateIfNeeded(true)
    intent.getStringExtra("open_chat_id")?.takeIf { it.isNotBlank() }?.let { id ->
      chats.firstOrNull { it.id == id }?.let { selectedChat = it; composePage = Page.CHAT; uiRevision++ }
    }
    if(intent.getBooleanExtra("open_moments",false)){requestedMainTab=2;composePage=Page.MAIN;mainTabRequestNonce++;uiRevision++}
  }

  override fun onUpdateAvailable(version: String, url: String) {
    availableUpdateVersion=version
    availableUpdateUrl=url
    updateNoticeNonce++
    uiRevision++
  }

  override fun onSpeechStateChanged() {
    uiRevision++
  }

  override fun onMomentStateChanged() {
    uiRevision++
  }

  override fun onDesktopPetStateChanged() {
    selectedChat?.id?.let { id -> selectedChat = chats.firstOrNull { it.id == id } }
    uiRevision++
  }

  override fun onResume() {
    super.onResume()
    syncBackgroundStateIfNeeded(false)
    WeixinLinkService.sync(this)
    DesktopPetService.sync(this)
    selectedChat?.id?.let { id -> selectedChat = chats.firstOrNull { it.id == id } }
    if(composePage==Page.CHAT)selectedChat?.id?.let{id->prefs.edit().putString("visible_chat_id",id).apply()}
    uiRevision++
  }

  override fun chat(x: Chat) {
    if(!adultConsentGranted()){
      selectedChat=null
      composePage=Page.ADULT_CONFIRM
      uiRevision++
      return
    }
    selectedChat = x
    composePage = Page.CHAT
    uiRevision++
  }

  override fun refreshChatUi(x: Chat) {
    if (selectedChat?.id == x.id && composePage in listOf(Page.CHAT, Page.CHAT_DETAIL)) uiRevision++
  }

  @Composable
  private fun SyncSystemBars(dark: Boolean, transparentStatus: Boolean, darkStatusIcons: Boolean) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(window,!transparentStatus)
      window.statusBarColor = if(transparentStatus)AndroidColor.TRANSPARENT else background.toArgb()
      window.navigationBarColor = background.toArgb()
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = darkStatusIcons
        isAppearanceLightNavigationBars = !dark
      }
    }
  }

  private fun chatStatusBarDarkIcons(background:Color,imagePath:String,overlay:Float):Boolean{
    if(imagePath.isBlank())return background.luminance()>.5f
    return runCatching{
      val options=BitmapFactory.Options().apply{inSampleSize=8}
      val bitmap=BitmapFactory.decodeFile(imagePath,options)?:return@runCatching background.luminance()>.5f
      var red=0L;var green=0L;var blue=0L;var samples=0L
      val top=(bitmap.height/4).coerceAtLeast(1);val stepX=(bitmap.width/24).coerceAtLeast(1);val stepY=(top/10).coerceAtLeast(1)
      var y=0;while(y<top){var x=0;while(x<bitmap.width){val pixel=bitmap.getPixel(x,y);red+=AndroidColor.red(pixel);green+=AndroidColor.green(pixel);blue+=AndroidColor.blue(pixel);samples++;x+=stepX};y+=stepY}
      bitmap.recycle();if(samples==0L)return@runCatching background.luminance()>.5f
      val alpha=overlay.coerceIn(0f,1f);val r=(red.toFloat()/samples/255f)*(1f-alpha)+background.red*alpha;val g=(green.toFloat()/samples/255f)*(1f-alpha)+background.green*alpha;val b=(blue.toFloat()/samples/255f)*(1f-alpha)+background.blue*alpha
      Color(r,g,b).luminance()>.5f
    }.getOrDefault(background.luminance()>.5f)
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun ShiYuApp(themeMode: Int, setTheme: (Int) -> Unit, themeStyle:Int, setThemeStyle:(Int)->Unit) {
    uiRevision
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage=requestedMainTab,pageCount = { 4 })
    val selectedTab by remember { derivedStateOf { pagerState.currentPage } }
    var showUpdateDialog by rememberSaveable{mutableStateOf(false)}
    LaunchedEffect(updateNoticeNonce) {
      if(updateNoticeNonce>0&&availableUpdateVersion.isNotBlank())showUpdateDialog=true
    }
    LaunchedEffect(mainTabRequestNonce){if(mainTabRequestNonce>0)scope.launch{pagerState.scrollToPage(requestedMainTab)}}

    BackHandler(enabled = composePage != Page.MAIN || selectedTab != 0) {
      when {
        composePage == Page.ADULT_CONFIRM -> finish()
        composePage == Page.ONBOARDING -> finish()
        composePage == Page.COMMUNITY_INVITE -> finishCommunityInvite()
        composePage == Page.FEEDBACK -> composePage = Page.MAIN
        composePage == Page.TERMS -> composePage = termsBackPage
        composePage == Page.CHAT -> { selectedChat = null; composePage = Page.MAIN; scope.launch{pagerState.scrollToPage(0)} }
        composePage == Page.CHAT_DETAIL -> composePage = Page.CHAT
        composePage == Page.CHAT_CONTENT -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_CONTEXT -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_TTS -> composePage = Page.CHAT_DETAIL
        composePage == Page.PROACTIVE -> composePage = Page.CHAT_DETAIL
        composePage == Page.YANDERE -> composePage = Page.CHAT_DETAIL
        composePage == Page.MOMENT_SHARE -> composePage = Page.CHAT
        composePage == Page.CHAT_DEFAULTS -> composePage = Page.APP_BEHAVIOR
        composePage == Page.GENERATION_TASKS -> composePage = Page.MAIN
        composePage == Page.WEB_SEARCH -> composePage = webSearchBackPage
        composePage == Page.IMPORT_PREVIEW -> discardPendingImport()
        composePage == Page.CARD_EXPORT -> { exportCardId=null;composePage=Page.CARD_CENTER }
        composePage == Page.MEMORY -> composePage = Page.ROLE_SETTINGS
        composePage == Page.ROLE_SETTINGS -> composePage = roleBackPage
        composePage == Page.RENAME_CHAT -> composePage = renameBackPage
        composePage == Page.ADD_PROVIDER -> { editingProviderId=null;composePage = providerBackPage }
        composePage == Page.TTS -> composePage = ttsBackPage
        composePage == Page.IDENTITIES -> composePage = Page.USER_PROFILE
        composePage == Page.MOMENT_ROLE_POST -> composePage=momentRolePostBackPage
        composePage in listOf(Page.MOMENTS,Page.MOMENT_PERMISSIONS,Page.MOMENT_COMMENTS) -> composePage = Page.MAIN
        composePage == Page.FRAME_RATE -> composePage = Page.APPEARANCE
        composePage == Page.CHAT_APPEARANCE -> composePage = Page.APPEARANCE
        composePage == Page.STICKER_RESOURCES -> composePage = stickerResourcesBackPage
        composePage == Page.REALITY_AWARENESS -> composePage = Page.MAIN
        composePage == Page.EXPERIMENTAL -> composePage = Page.MAIN
        composePage == Page.WEIXIN_LINK -> composePage = Page.EXPERIMENTAL
        composePage == Page.IMAGE_GENERATION -> composePage = imageGenerationBackPage
        composePage == Page.BACKUP -> composePage = Page.DATA
        composePage == Page.EMBEDDING_LIBRARY -> composePage=embeddingBackPage
        composePage in listOf(Page.API_ERROR,Page.PROVIDER,Page.CONNECTION,Page.MODEL,Page.GENERATION) -> composePage = Page.API
        composePage != Page.MAIN -> composePage = Page.MAIN
        selectedTab != 0 -> scope.launch { pagerState.animateScrollToPage(0, animationSpec = tween(240,easing=FastOutSlowInEasing)) }
      }
    }

    val glassUi = prefs.getBoolean("experimental_glass_ui",false)
    Surface(Modifier.fillMaxSize(),color=if(glassUi)Color.Transparent else MaterialTheme.colorScheme.background,contentColor=MaterialTheme.colorScheme.onBackground) {
      val backdrop = if(glassUi) Modifier.background(MaterialTheme.colorScheme.background) else Modifier
      Box(Modifier.fillMaxSize().then(backdrop)) {
      AnimatedContent(
        targetState = Pair(composePage, selectedChat?.id.takeIf { composePage == Page.CHAT || composePage == Page.CHAT_DETAIL || composePage == Page.CHAT_CONTENT || composePage == Page.CHAT_CONTEXT }),
        transitionSpec = {
          val forward=pageDepth(targetState.first)>pageDepth(initialState.first)
          if(forward)(fadeIn(tween(125))+slideInHorizontally(tween(185)){it/3}) togetherWith (fadeOut(tween(90))+slideOutHorizontally(tween(155)){-it/8})
          else (fadeIn(tween(125))+slideInHorizontally(tween(185)){-it/4}) togetherWith (fadeOut(tween(90))+slideOutHorizontally(tween(165)){it/3})
        },
        modifier = Modifier.fillMaxSize(), label = "shiyu-navigation"
      ) { state ->
        when (state.first) {
          Page.ADULT_CONFIRM -> AdultConfirmPage()
          Page.ONBOARDING -> OnboardingPage()
          Page.COMMUNITY_INVITE -> CommunityInvitePage()
          Page.FEEDBACK -> FeedbackPage()
          Page.TERMS -> TermsPage()
          Page.CHAT -> selectedChat?.let { ChatPage(it) }
          Page.CHAT_DETAIL -> selectedChat?.let { ChatDetailPage(it) }
          Page.CHAT_CONTENT -> selectedChat?.let { ChatContentPage(it) }
          Page.CHAT_CONTEXT -> selectedChat?.let { ChatContextPage(it) }
          Page.CHAT_TTS -> selectedChat?.let { ChatTtsPage(it) }
          Page.PROACTIVE -> selectedChat?.let { ProactiveSettingsPage(it) }
          Page.YANDERE -> selectedChat?.let { YandereSettingsPage(it) }
          Page.MOMENT_SHARE -> selectedChat?.let { MomentSharePage(it) }
          Page.CHAT_DEFAULTS -> ChatDefaultsPage()
          Page.GENERATION_TASKS -> GenerationTasksPage()
          Page.ROLE_SETTINGS -> RoleSettingsPage()
          Page.MEMORY -> MemoryPage()
          Page.RENAME_CHAT -> selectedChat?.let { RenameChatPage(it) }
          Page.CREATE_ROLE -> CreateRolePage()
          Page.GROUP_SETUP -> GroupSetupPage()
          Page.CARD_CENTER -> CardCenterPage()
          Page.CARD_EXPORT -> RoleExportPage()
          Page.IMPORT_PREVIEW -> ImportPreviewPage()
          Page.MOMENTS -> MomentsPage()
          Page.MOMENT_PERMISSIONS -> MomentPermissionsPage()
          Page.MOMENT_ROLE_POST -> MomentRolePostPage()
          Page.MOMENT_COMMENTS -> MomentCommentsPage()
          Page.API -> ApiPage()
          Page.API_ERROR -> ApiErrorPage()
          Page.PROVIDER -> ProviderPage()
          Page.ADD_PROVIDER -> AddProviderPage()
          Page.CONNECTION -> ConnectionPage()
          Page.MODEL -> ModelPage()
          Page.GENERATION -> GenerationPage()
          Page.WEB_SEARCH -> WebSearchPage()
          Page.EMBEDDING_LIBRARY -> EmbeddingLibraryPage()
          Page.IMAGE_GENERATION -> ImageGenerationPage()
          Page.TTS -> TtsPage()
          Page.APPEARANCE -> AppearancePage(themeMode, setTheme,themeStyle,setThemeStyle)
          Page.CHAT_APPEARANCE -> ChatAppearancePage()
          Page.FRAME_RATE -> FrameRatePage()
          Page.DATA -> DataPage()
          Page.BACKUP -> BackupPage()
          Page.STICKER_RESOURCES -> StickerResourcesPage()
          Page.REALITY_AWARENESS -> RealityAwarenessPage()
          Page.EXPERIMENTAL -> ExperimentalPage()
          Page.WEIXIN_LINK -> WeixinLinkPage()
          Page.ABOUT -> AboutPage()
          Page.SUPPORT -> SupportPage()
          Page.USER_PROFILE -> UserProfilePage()
          Page.IDENTITIES -> IdentitiesPage()
          Page.APP_BEHAVIOR -> AppBehaviorPage()
          Page.MAIN -> HorizontalPager(state=pagerState,beyondBoundsPageCount=3,modifier=Modifier.fillMaxSize().padding(bottom=76.dp)){index->when(index){0->ConversationPage{composePage=Page.GROUP_SETUP};1->RolePage();2->MomentsHomePage();else->ProfilePage()}}
        }
      }
      AnimatedVisibility(visible=composePage==Page.MAIN,enter=fadeIn(tween(80)),exit=fadeOut(tween(60)),modifier=Modifier.align(Alignment.BottomCenter)){ShiYuBottomBar(selectedTab){index->snackbar.currentSnackbarData?.dismiss();if(index!=pagerState.currentPage)scope.launch{pagerState.animateScrollToPage(index,animationSpec=tween(260,easing=FastOutSlowInEasing))}}}
      SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).padding(bottom=82.dp))
      if(showUpdateDialog)AlertDialog(onDismissRequest={showUpdateDialog=false},icon={Icon(Icons.Outlined.NewReleases,null)},title={Text("发现时遇 $availableUpdateVersion")},text={Column(Modifier.heightIn(max=320.dp).verticalScroll(rememberScrollState())){Text(prefs.getString("update_notes","新版本已经可以下载。")?:"新版本已经可以下载。",fontSize=14.sp,lineHeight=21.sp);Spacer(Modifier.height(10.dp));Text("更新包来自时遇官网，下载前可在关于页面再次查看版本信息。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}},confirmButton={Button(onClick={showUpdateDialog=false;if(availableUpdateUrl.isNotBlank())runCatching{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(availableUpdateUrl)))}}){Text("立即下载")}},dismissButton={TextButton(onClick={showUpdateDialog=false}){Text("稍后")}})
      pendingDanger?.let{danger->AlertDialog(onDismissRequest={pendingDanger=null},icon={Icon(Icons.Outlined.WarningAmber,null,tint=MaterialTheme.colorScheme.error)},title={Text(danger.title)},text={Text(danger.message,fontSize=14.sp,lineHeight=21.sp)},confirmButton={Button(onClick={pendingDanger=null;danger.action()},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error,contentColor=MaterialTheme.colorScheme.onError)){Text(danger.confirmLabel)}},dismissButton={TextButton(onClick={pendingDanger=null}){Text("取消")}})}
      }
    }
  }

  @Composable
  private fun ShiYuBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val glassUi=prefs.getBoolean("experimental_glass_ui",false)
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
      Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border=if(glassUi)BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.42f))else null, shadowElevation = if(glassUi)0.dp else 4.dp) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
          val labels = listOf("对话", "角色", "朋友圈", "我的")
          val icons = listOf(Icons.Outlined.Forum, Icons.Outlined.Style, Icons.Outlined.Collections, Icons.Outlined.Tune)
          labels.forEachIndexed { index, label ->
            Column(
              Modifier.weight(1f).fillMaxHeight().offset(y=2.dp).clickable { onSelect(index) },
              horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
              Icon(icons[index], label, Modifier.size(20.dp), tint = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(Modifier.height(2.dp))
              Text(label, fontSize = 10.sp, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal, color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }

  @Composable
  private fun AdultConfirmPage(){
    var adult by rememberSaveable{mutableStateOf(false)}
    var accepted by rememberSaveable{mutableStateOf(false)}
    LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding=PaddingValues(horizontal=22.dp,vertical=26.dp),
      horizontalAlignment=Alignment.CenterHorizontally,
      verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
      item{Image(painterResource(R.drawable.ic_launcher_foreground),"时遇",Modifier.size(78.dp).clip(RoundedCornerShape(18.dp)))}
      item{Text("使用前请确认",fontSize=26.sp,fontWeight=FontWeight.Bold)}
      item{Text("时遇仅面向已满 18 周岁的成年人",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
      item{InfoBanner(Icons.Outlined.WarningAmber,"未成年人使用风险","角色扮演模型的输出不可预测，可能涉及情感依赖、成人话题、错误信息或不适宜内容。未成年人请勿使用本应用，也不要通过虚假确认绕过限制。")}
      item{GuideStep("1","AI 不是现实人物","角色的情感、关系与承诺均为模型生成，请勿将其替代现实社交、心理支持或专业意见。")}
      item{GuideStep("2","第三方模型负责内容","聊天会发送到你配置的 API。不同供应商的内容政策、隐私处理和年龄要求可能不同。")}
      item{Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainer){Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)){Row(Modifier.fillMaxWidth().clickable{adult=!adult},verticalAlignment=Alignment.CenterVertically){Checkbox(adult,{adult=it});Text("我确认自己已满 18 周岁",Modifier.weight(1f),fontWeight=FontWeight.Medium)};Row(Modifier.fillMaxWidth().clickable{accepted=!accepted},verticalAlignment=Alignment.CenterVertically){Checkbox(accepted,{accepted=it});Text("我已阅读并同意用户协议与使用条款",Modifier.weight(1f),fontSize=13.sp)}}}}
      item{TextButton(onClick={termsBackPage=Page.ADULT_CONFIRM;composePage=Page.TERMS}){Icon(Icons.Outlined.Gavel,null);Spacer(Modifier.width(7.dp));Text("查看完整协议")}}
      item{Button(onClick={prefs.edit().putString("adult_consent_revision",adultConsentRevision).putBoolean("terms_accepted",true).apply();composePage=when{!prefs.getBoolean("onboarding_done",false)->Page.ONBOARDING;prefs.getString("community_invite_version","")!=BuildConfig.VERSION_NAME->Page.COMMUNITY_INVITE;else->Page.MAIN}},enabled=adult&&accepted,modifier=Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.VerifiedUser,null);Spacer(Modifier.width(8.dp));Text("确认并继续",maxLines=1,softWrap=false)}}
      item{TextButton(onClick={finish()}){Text("不同意并退出")}}
      item{Text("年龄确认仅保存在本机。时遇不会要求或采集身份证、生日、面部信息等身份材料。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun OnboardingPage(){
    val state=rememberPagerState(pageCount={3});val scope=rememberCoroutineScope()
    val pages=listOf(Triple(Icons.Outlined.Style,"导入或创建角色","支持 PNG、JSON 角色卡，也可以填写完整设定创建自己的角色。"),Triple(Icons.Outlined.Hub,"连接你的模型","在模型设置中添加 API 供应商、获取模型列表并自由切换。"),Triple(Icons.Outlined.Forum,"开始属于你的故事","聊天、长期记忆、世界书和本地数据都会保存在这台设备上。"))
    Column(Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=22.dp),horizontalAlignment=Alignment.CenterHorizontally){
      Image(painterResource(R.drawable.ic_launcher_foreground),"时遇",Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)));Text("欢迎来到时遇",fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp));Text("免费、开放的本地角色扮演聊天工具",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      HorizontalPager(state=state,Modifier.weight(1f).fillMaxWidth()){index->val page=pages[index];Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.primaryContainer){Icon(page.first,null,Modifier.padding(24.dp).size(48.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)};Text(page.second,fontSize=22.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=22.dp));Text(page.third,Modifier.padding(horizontal=14.dp).padding(top=10.dp),fontSize=14.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
      Row(Modifier.padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){repeat(3){i->Box(Modifier.size(if(i==state.currentPage)20.dp else 7.dp,7.dp).clip(CircleShape).background(if(i==state.currentPage)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))}}
      Button(onClick={if(state.currentPage<2)scope.launch{state.animateScrollToPage(state.currentPage+1,animationSpec=tween(220,easing=FastOutSlowInEasing))}else{prefs.edit().putBoolean("onboarding_done",true).apply();composePage=Page.COMMUNITY_INVITE}},Modifier.fillMaxWidth().height(50.dp)){Text(if(state.currentPage<2)"下一步" else "开始使用",maxLines=1,softWrap=false)}
    }
  }

  @Composable
  private fun CommunityInvitePage(){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=30.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){
      item{Spacer(Modifier.height(18.dp))}
      item{Image(painterResource(R.drawable.ic_launcher_foreground),"时遇",Modifier.size(92.dp).clip(RoundedCornerShape(20.dp)))}
      item{Text("我们很想听到你的声音",fontSize=26.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(top=8.dp))}
      item{Text("加入时遇交流群，告诉我们你的建议、遇到的问题，以及你希望下一次更新出现的功能。",fontSize=14.sp,lineHeight=22.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(horizontal=10.dp))}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.Groups,null,Modifier.size(34.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer);Spacer(Modifier.height(8.dp));Text("时遇 QQ 交流群",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onPrimaryContainer);Text("1061590432",fontSize=23.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer,modifier=Modifier.padding(top=3.dp))}}}
      item{Button(onClick={finishCommunityInvite();openQqGroup()},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.GroupAdd,null);Spacer(Modifier.width(8.dp));Text("加入 QQ 群",maxLines=1,softWrap=false)}}
      item{OutlinedButton(onClick={finishCommunityInvite(Page.FEEDBACK)},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Feedback,null);Spacer(Modifier.width(8.dp));Text("反馈与建议",maxLines=1,softWrap=false)}}
      item{TextButton(onClick={finishCommunityInvite()}){Text("暂时跳过")}}
      item{Text("此页面只在当前版本首次打开时显示一次。加入群或反馈均为自愿操作。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(horizontal=12.dp))}
    }
  }

  @Composable
  private fun FeedbackPage(){
    val categories=listOf("suggestion" to "功能建议","bug" to "问题反馈","experience" to "体验意见","other" to "其他")
    var category by rememberSaveable{mutableStateOf("suggestion")};var content by rememberSaveable{mutableStateOf("")};var contact by rememberSaveable{mutableStateOf("")};var submitting by remember{mutableStateOf(false)};var result by remember{mutableStateOf("")}
    DetailScaffold("反馈与建议"){
      item{InfoBanner(Icons.Outlined.PrivacyTip,"只发送你填写的内容","提交时仅发送反馈类别、正文、App 版本和可选联系方式。不会附带聊天、角色卡、API Key、设备标识或日志，官网也不保存原始 IP。")}
      item{SectionTitle("反馈类型")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){categories.forEach{entry->FilterChip(selected=category==entry.first,onClick={category=entry.first},label={Text(entry.second,maxLines=1,softWrap=false)})}}}
      item{OutlinedTextField(content,{content=it.take(2000)},Modifier.fillMaxWidth().heightIn(min=170.dp,max=280.dp),label={Text("反馈内容")},placeholder={Text("请描述你的建议、操作步骤或遇到的问题，至少 10 个字。")},supportingText={Text("${content.length} / 2000")},minLines=7,maxLines=12,shape=RoundedCornerShape(13.dp))}
      item{OutlinedTextField(contact,{contact=it.take(100)},Modifier.fillMaxWidth(),label={Text("联系方式（可选）")},placeholder={Text("QQ 或邮箱，仅用于回复本次反馈")},singleLine=true,shape=RoundedCornerShape(13.dp))}
      if(result.isNotBlank())item{InfoBanner(Icons.Outlined.CheckCircle,"反馈已提交","参考编号：$result。感谢你帮助时遇变得更好。")}
      item{Button(onClick={if(content.trim().length<10)toast("反馈内容至少需要 10 个字")else{submitting=true;result="";submitFeedback(category,content.trim(),contact.trim()){ok,message->submitting=false;if(ok){result=message;content="";contact=""}else toast("提交失败：$message")}}},enabled=!submitting,modifier=Modifier.fillMaxWidth().height(50.dp)){if(submitting)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(8.dp));Text(if(submitting)"正在安全提交" else "提交反馈",maxLines=1,softWrap=false)}}
      item{SectionTitle("其他方式")}
      item{SettingButton(Icons.Outlined.Language,"在官网反馈页打开","使用同一安全接口，页面不在官网公开导航中"){openFeedbackWebsite()}}
      item{SettingButton(Icons.Outlined.Groups,"加入时遇 QQ 群","1061590432 · 点击跳转或复制群号"){openQqGroup()}}
      item{Text("请勿在反馈中填写 API Key、密码、身份证件、支付信息、完整聊天记录或其他敏感内容。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(10.dp))}
    }
  }

  @Composable
  private fun TermsPage(){DetailScaffold("用户协议与使用条款",termsBackPage){
    item{InfoBanner(Icons.Outlined.VolunteerActivism,"时遇是免费 App","时遇不收取软件下载或基础功能费用，也不会承诺任何第三方 API 永久免费或持续可用。")}
    item{GuideStep("1","成年用户限制","时遇仅供已满 18 周岁的成年人使用。未成年人不得注册式确认、绕过年龄提示或使用本应用；监护人发现未成年人使用时应立即停止并清除相关数据。")}
    item{GuideStep("2","服务说明","时遇是一款本地角色扮演聊天客户端。用户需要自行配置兼容 API，AI 回复由所选第三方模型生成。")}
    item{GuideStep("3","第三方 API","API 的价格、限额、隐私、年龄要求和可用性由供应商负责。使用前请阅读对应供应商条款，并妥善保管 API Key。")}
    item{GuideStep("4","本地数据","角色卡、聊天、记忆与设置主要保存在设备本地。卸载、清除数据或设备故障可能造成丢失，请自行备份。")}
    item{GuideStep("5","AI 与情感风险","模型可能生成错误、冒犯、成人或其他不适当内容，也可能表现出拟人化关系。AI 不是现实人物，不应替代现实社交、心理支持或医疗、法律、财务等专业服务。")}
    item{GuideStep("6","合规使用","不得利用时遇生成、传播违法内容，不得侵害他人隐私、版权、人格权或用于欺诈、骚扰及其他伤害行为。")}
    item{GuideStep("7","角色卡版权","导入、创建和分享角色卡时，用户应确保拥有必要权利。因第三方素材或角色设定产生的争议由使用者承担。")}
    item{GuideStep("8","隐私与安全","时遇不会运营聊天中转服务器；启动时会匿名向官网记录一次打开次数，不包含设备标识、聊天或 API Key。API Key 使用 Android Keystore 加密保存。")}
    item{GuideStep("9","协议变更","功能、兼容性和条款可能随版本更新调整。涉及年龄或重要风险的条款更新后，应用会再次要求明确确认。")}
    item{Text("生效日期：2026年7月14日",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(12.dp))}
  }}

  @Composable
  private fun BrandHeader(title: String, subtitle: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      actions()
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun ConversationPage(onGroupClick: () -> Unit) {
    uiRevision
    var filter by remember { mutableIntStateOf(0) }
    var menuChat by remember { mutableStateOf<Chat?>(null) }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize()) {
      BrandHeader("时遇", "每一次对话，都在靠近") {
        CircleAction(Icons.Outlined.GroupAdd, "群聊", onGroupClick)
        Spacer(Modifier.width(8.dp))
        CircleAction(Icons.Outlined.AddComment, "新对话") { composePage = Page.CARD_CENTER }
      }
      Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)) {
        listOf("全部", "私聊", "群聊").forEachIndexed { index, label ->
          Box(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
              .background(if (filter == index) MaterialTheme.colorScheme.surface else Color.Transparent)
              .clickable { filter = index }
              .padding(vertical = 9.dp), contentAlignment = Alignment.Center
          ) { Text(label, fontSize = 13.sp, fontWeight = if (filter == index) FontWeight.SemiBold else FontWeight.Normal) }
        }
      }
      SectionTitle("最近对话")
      val visibleChats=remember(uiRevision,filter){chats.filter{filter==0||(filter==1&&!it.group)||(filter==2&&it.group)}.sortedWith(compareByDescending<Chat>{it.pinned}.thenByDescending{if(it.pinned)it.pinnedAt else it.updated})}
      LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (visibleChats.isEmpty()) item { EmptyState(if(filter==2)Icons.Outlined.Groups else Icons.Outlined.Forum, if(filter==2)"还没有群聊" else if(filter==1)"还没有私聊" else "还没有对话", if(filter==2)"点击右上角创建一个多角色群聊" else "前往角色页，选择一张角色卡开始") }
        items(visibleChats, key = { it.id }) { item ->
          val card = find(item.cardId) ?: return@items
          Surface(
            modifier = Modifier.combinedClickable(onClick = { chat(item) }, onLongClick = { menuChat = item }), shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f))
          ) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
              RoleAvatar(card, 48)
              Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title.ifBlank{card.name}, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically){Text(item.messages.lastOrNull()?.body ?: "新的故事正等待开始",Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant);if(item.externalSource=="weixin")Surface(shape=RoundedCornerShape(6.dp),color=MaterialTheme.colorScheme.secondaryContainer){Text("微信",Modifier.padding(horizontal=5.dp,vertical=2.dp),fontSize=10.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.onSecondaryContainer)}}
              }
              if(item.pinned)Icon(Icons.Filled.PushPin,"已置顶",Modifier.size(15.dp),tint=MaterialTheme.colorScheme.primary)
              Spacer(Modifier.width(5.dp))
              Text(time(item.updated), fontSize = 10.sp, maxLines=1,softWrap=false,color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
    menuChat?.let { target ->
      AlertDialog(onDismissRequest={menuChat=null},title={Text(target.title)},text={Column{
        TextButton(onClick={menuChat=null;chat(target)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.OpenInNew,null);Spacer(Modifier.width(8.dp));Text("打开对话",Modifier.weight(1f))}
        TextButton(onClick={target.pinned=!target.pinned;target.pinnedAt=if(target.pinned)System.currentTimeMillis() else 0;save();uiRevision++;menuChat=null;toast(if(target.pinned)"已置顶对话" else "已取消置顶")},Modifier.fillMaxWidth()){Icon(if(target.pinned)Icons.Outlined.PushPin else Icons.Filled.PushPin,null);Spacer(Modifier.width(8.dp));Text(if(target.pinned)"取消置顶" else "置顶对话",Modifier.weight(1f))}
        TextButton(onClick={val value=target.messages.lastOrNull()?.body.orEmpty();if(value.isBlank())toast("暂无可复制内容")else{clipboard.setText(AnnotatedString(value));toast("已复制最后一条消息")};menuChat=null},Modifier.fillMaxWidth()){Icon(Icons.Outlined.ContentCopy,null);Spacer(Modifier.width(8.dp));Text("复制最后一条消息",Modifier.weight(1f))}
        TextButton(onClick={menuChat=null;selectedChat=target;renameBackPage=Page.MAIN;composePage=Page.RENAME_CHAT},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Edit,null);Spacer(Modifier.width(8.dp));Text("重命名",Modifier.weight(1f))}
        TextButton(onClick={menuChat=null;askDanger("删除对话","将永久删除“${target.title}”及全部聊天记录，此操作无法撤销。"){removeChat(target);toast("对话已删除")}},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Delete,null);Spacer(Modifier.width(8.dp));Text("删除对话",Modifier.weight(1f),color=MaterialTheme.colorScheme.error)}
      }},confirmButton={})
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun RolePage() {
    val roleItems = remember(uiRevision) { cards.toList() }
    var menuCard by remember { mutableStateOf<Card?>(null) }
    Column(Modifier.fillMaxSize()) {
      BrandHeader("角色卡", "${roleItems.size} 张角色卡，收藏不同的世界") {
        CircleAction(Icons.Outlined.PersonAdd, "创建角色") { composePage = Page.CREATE_ROLE }
        Spacer(Modifier.width(8.dp))
        CircleAction(Icons.Outlined.Inventory2, "角色卡中心") { composePage = Page.CARD_CENTER }
      }
      SectionTitle("我的角色")
      LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (roleItems.isEmpty()) item { EmptyState(Icons.Outlined.Style, "还没有角色卡", "导入 PNG 或 JSON 角色卡，开启第一场相遇") }
        items(roleItems, key = { it.id }) { card ->
          Surface(
            modifier = Modifier.combinedClickable(onClick = { newChat(card) }, onLongClick = { menuCard = card }),
            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface
          ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              RoleAvatar(card, 52)
              Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(card.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(card.desc.ifBlank { "等待与你相遇" }, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Icon(Icons.AutoMirrored.Filled.Send, "开始对话", tint = MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }
    }
    menuCard?.let { target ->
      AlertDialog(onDismissRequest={menuCard=null},title={Text(target.name)},text={Column{
        TextButton(onClick={menuCard=null;newChat(target)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Forum,null);Spacer(Modifier.width(8.dp));Text("开始新对话",Modifier.weight(1f))}
        TextButton(onClick={menuCard=null;editingCardId=target.id;roleBackPage=Page.MAIN;composePage=Page.ROLE_SETTINGS},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Edit,null);Spacer(Modifier.width(8.dp));Text("编辑角色卡",Modifier.weight(1f))}
        TextButton(onClick={menuCard=null;askDanger("删除角色","将永久删除“${target.name}”、相关单聊、角色记忆，并从群聊中移除该角色。"){if(cards.any{it.id==target.id}){removeCard(target);toast("角色已删除")}}},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Delete,null,tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(8.dp));Text("删除角色",Modifier.weight(1f),color=MaterialTheme.colorScheme.error)}
      }},confirmButton={})
    }
  }

  @Composable
  private fun CreateRolePage() {
    val draftKey="role_draft_new";val draft=remember{runCatching{JSONObject(prefs.getString(draftKey,"{}").orEmpty())}.getOrElse{JSONObject()}}
    var name by remember{mutableStateOf(draft.optString("name"))};var desc by remember{mutableStateOf(draft.optString("desc"))};var persona by remember{mutableStateOf(draft.optString("persona"))};var greeting by remember{mutableStateOf(draft.optString("greeting"))};var frontend by remember{mutableStateOf(draft.optString("frontend"))};var worldBook by remember{mutableStateOf(draft.optString("worldBook"))};var regex by remember{mutableStateOf(draft.optString("regex"))};var preset by remember{mutableStateOf(draft.optString("preset"))};var avatarUri by remember{mutableStateOf(draft.optString("avatar"))};var patSelf by remember{mutableStateOf(draft.optString("patSelf","肩膀"))};var patUser by remember{mutableStateOf(draft.optString("patUser","肩膀"))}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};avatarUri=uri.toString()}}
    var draftResolved by remember{mutableStateOf(false)}
    fun persistDraft(){if(listOf(name,desc,persona,greeting,frontend,worldBook,regex,preset,avatarUri).any{it.isNotBlank()}){val value=JSONObject().put("name",name).put("desc",desc).put("persona",persona).put("greeting",greeting).put("frontend",frontend).put("worldBook",worldBook).put("regex",regex).put("preset",preset).put("avatar",avatarUri).put("patSelf",patSelf).put("patUser",patUser);prefs.edit().putString(draftKey,value.toString()).apply()}else prefs.edit().remove(draftKey).apply()}
    LaunchedEffect(name,desc,persona,greeting,frontend,worldBook,regex,preset,avatarUri,patSelf,patUser){delay(650);persistDraft()}
    DisposableEffect(draftKey){onDispose{if(!draftResolved)persistDraft()}}
    DetailScaffold("创建角色"){
      item{InfoBanner(Icons.Outlined.PersonAdd,if(draft.length()>0)"已恢复本地草稿" else "创建属于你的角色","输入内容会自动保存为本地草稿；创建成功后草稿自动清除。")}
      item{Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){if(avatarUri.isNotBlank())MessageImage(avatarUri,Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)));OutlinedButton(onClick={picker.launch(arrayOf("image/*"))},Modifier.padding(top=8.dp)){Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(7.dp));Text(if(avatarUri.isBlank())"选择角色头像" else "更换角色头像")}}}
      item{StyledField(name,{name=it},"角色名称")}
      item{OutlinedTextField(desc,{desc=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=180.dp),label={Text("角色简介")},minLines=3,maxLines=7,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(persona,{persona=it},Modifier.fillMaxWidth().heightIn(min=150.dp,max=260.dp),label={Text("角色人格与设定")},minLines=6,maxLines=11,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(greeting,{greeting=it},Modifier.fillMaxWidth().heightIn(min=100.dp,max=200.dp),label={Text("开场白")},minLines=4,maxLines=8,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("高级内容")}
      item{OutlinedTextField(frontend,{frontend=it},Modifier.fillMaxWidth().heightIn(min=120.dp,max=260.dp),label={Text("酒馆前端 HTML（可选）")},minLines=5,maxLines=11,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(worldBook,{worldBook=it},Modifier.fillMaxWidth().heightIn(min=110.dp,max=210.dp),label={Text("世界书（可选）")},minLines=4,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(regex,{regex=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=210.dp),label={Text("正则规则（可选）")},minLines=3,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(preset,{preset=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=190.dp),label={Text("对话预设（可选）")},minLines=3,maxLines=8,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("拍一拍")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(patSelf,{patSelf=it.take(40)},"用户拍角色的部位")};Box(Modifier.weight(1f)){StyledField(patUser,{patUser=it.take(40)},"角色拍用户的部位")}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={name="";desc="";persona="";greeting="";frontend="";worldBook="";regex="";preset="";avatarUri="";patSelf="肩膀";patUser="肩膀";prefs.edit().remove(draftKey).apply();toast("草稿已清除")},Modifier.weight(1f).height(52.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(7.dp));Text("清除草稿",maxLines=1)};Button(onClick={if(name.isBlank()||persona.isBlank())toast("请填写角色名称和角色设定")else{val card=Card();card.id=UUID.randomUUID().toString();card.name=name.trim();card.desc=desc.trim();card.persona=persona.trim();card.greeting=greeting.trim();card.avatar="";card.frontend=frontend.trim();card.worldBook=worldBook;card.regexRules=regex;card.preset=preset;card.patSelfText=patSelf.trim().ifBlank{"肩膀"};card.patUserText=patUser.trim().ifBlank{"肩膀"};if(avatarUri.isNotBlank())runCatching{val file=File(filesDir,"avatar_${card.id}.png");contentResolver.openInputStream(android.net.Uri.parse(avatarUri))?.use{input->FileOutputStream(file).use{output->input.copyTo(output)}};card.avatar=file.absolutePath};cards.add(card);save();DesktopPetService.sync(this@ComposeActivity);draftResolved=true;prefs.edit().remove(draftKey).apply();uiRevision++;toast("角色 ${card.name} 已创建");composePage=Page.MAIN}},Modifier.weight(1f).height(52.dp)){Icon(Icons.Outlined.PersonAdd,null);Spacer(Modifier.width(8.dp));Text("创建角色",maxLines=1,softWrap=false)}}}
    }
  }

  @Composable
  private fun GroupSetupPage() {
    val availableCards = remember(uiRevision) { cards.toList() }
    val selected = remember { mutableStateListOf<String>() }
    var title by remember { mutableStateOf("") }
    DetailScaffold("创建群聊") {
      item { InfoBanner(Icons.Outlined.Groups, "让角色在同一个故事里相遇", "至少选择两名角色。聊天中可以随时指定下一位回复角色。") }
      item { StyledField(title, { title = it }, "群聊名称（可选）") }
      item { SectionTitle("选择角色") }
      if (availableCards.size < 2) item { EmptyState(Icons.Outlined.GroupAdd, "需要至少两张角色卡", "先从角色卡中心导入更多角色") }
      items(availableCards, key = { it.id }) { card ->
        val checked = card.id in selected
        Surface(onClick={if(checked)selected.remove(card.id)else selected.add(card.id)},shape=RoundedCornerShape(14.dp),color=if(checked)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
          Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,44);Text(card.name,Modifier.weight(1f).padding(horizontal=12.dp),fontWeight=FontWeight.Medium);Checkbox(checked=checked,onCheckedChange=null)}
        }
      }
      item {
        Button(onClick={
          val members=availableCards.filter{it.id in selected}
          if(members.size<2){toast("请至少选择两名角色");return@Button}
          val x=Chat();x.id=UUID.randomUUID().toString();x.group=true;x.cardIds.addAll(members.map{it.id});x.cardId=members.first().id;x.activeCardId=members.first().id;x.title=title.trim().ifBlank{members.joinToString("、"){it.name}};x.updated=System.currentTimeMillis();x.groupAutoRelay=prefs.getBoolean("default_group_relay",true);x.groupAutonomous=prefs.getBoolean("default_group_autonomous",true);x.groupActiveTopics=prefs.getBoolean("default_group_topics",true);x.groupReplyDecision=prefs.getBoolean("default_group_reply_decision",true);x.groupRelayRounds=prefs.getInt("default_group_rounds",6).coerceIn(1,9999);x.groupReplyProbability=prefs.getInt("default_group_probability",70).coerceIn(0,100);x.proactiveMinMinutes=prefs.getInt("default_group_proactive_min",180);x.proactiveMaxMinutes=prefs.getInt("default_group_proactive_max",180);x.proactiveBurstCount=prefs.getInt("default_group_burst",1)
          members.forEach{member->if(member.frontend.isNotBlank()){val front=Msg("assistant",member.frontend);front.speakerCardId=member.id;front.messageType="tavern_frontend";x.messages.add(front)};if(member.greeting.isNotBlank()){val m=Msg("assistant",member.greeting);m.speakerCardId=member.id;x.messages.add(m)}}
          chats.add(0,x);save();selectedChat=x;composePage=Page.CHAT;uiRevision++
        },Modifier.fillMaxWidth().height(50.dp),enabled=availableCards.size>=2){Icon(Icons.Outlined.GroupAdd,null);Spacer(Modifier.width(8.dp));Text("创建群聊",maxLines=1,softWrap=false)}
      }
    }
  }

  @Composable
  private fun CardCenterPage() {
    val availableCards = remember(uiRevision) { cards.toList() }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pool.execute {
        try {
          val bytes = bytes(contentResolver.openInputStream(uri)); val name = fileName(uri); val card = parse(bytes, name)
          if (isPng(bytes)) { val file = File(cacheDir, "import_preview_${card.id}.png"); FileOutputStream(file).use { it.write(bytes) }; card.avatar = file.absolutePath }
          main.post { pendingImportCard=card;composePage=Page.IMPORT_PREVIEW }
        } catch (e: Exception) { main.post { toast("导入失败：${e.message}") } }
      }
    }
    DetailScaffold("角色卡中心") {
      item { InfoBanner(Icons.Outlined.AutoStories, "把角色带进时遇", "支持 Tavo、TavernAI、SillyTavern JSON/PNG，以及角色世界书和正则脚本。导入前会显示完整预览。") }
      item {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { import.launch(arrayOf("application/json", "image/png", "*/*")) }, Modifier.fillMaxWidth().height(46.dp)) { Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(7.dp)); Text("导入角色卡",maxLines=1,softWrap=false) }
          OutlinedButton(onClick = { if (availableCards.isEmpty()) toast("暂无可导出的角色卡") }, Modifier.fillMaxWidth().height(46.dp)) { Icon(Icons.Outlined.IosShare, null); Spacer(Modifier.width(7.dp)); Text("选择下方角色进行导出",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis) }
        }
      }
      item { SectionTitle("导入指引") }
      item { GuideStep("1", "准备文件", "选择角色卡 PNG 或 JSON 文件。") }
      item { GuideStep("2", "确认内容", "预览角色资料、世界书和正则，确认无误后再导入。") }
      item { GuideStep("3", "开始聊天", "点击角色卡创建独立会话，历史记录互不影响。") }
      item { SectionTitle("导出角色卡") }
      if (availableCards.isEmpty()) item { EmptyState(Icons.Outlined.Inventory2, "没有可导出的角色", "先导入一张角色卡") }
      items(availableCards, key = { it.id }) { card ->
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
          Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoleAvatar(card, 42); Text(card.name, Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.Medium)
            IconButton(onClick = { exportCardId=card.id;composePage=Page.CARD_EXPORT }) { Icon(Icons.Outlined.Download, "选择导出格式") }
          }
        }
      }
    }
  }

  @Composable
  private fun RoleExportPage(){
    val card=find(exportCardId?:return)?:return
    var pendingFormat by remember(card.id){mutableStateOf<CharacterCardExporter.Format?>(null)}
    var exporting by remember(card.id){mutableStateOf(false)}
    val createFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")){uri->
      val format=pendingFormat
      if(uri!=null&&format!=null){exporting=true;pool.execute{runCatching{CharacterCardExporter.export(card,format)}.onSuccess{file->runCatching{contentResolver.openOutputStream(uri,"w")?.use{it.write(file.bytes)}?:error("无法写入所选位置")}.onSuccess{main.post{exporting=false;toast("${card.name} 已导出为 ${formatLabel(format)}")}}.onFailure{error->main.post{exporting=false;toast("导出写入失败：${error.message}")}}}.onFailure{error->main.post{exporting=false;toast("导出失败：${error.message}")}}}}
      else exporting=false
    }
    fun launch(format:CharacterCardExporter.Format){pendingFormat=format;val suffix=when(format){CharacterCardExporter.Format.JSON_V2->"ccv2.json";CharacterCardExporter.Format.JSON_V3->"ccv3.json";CharacterCardExporter.Format.PNG_V2->"ccv2.png";CharacterCardExporter.Format.PNG_V3->"ccv3.png"};createFile.launch("${card.name.replace(Regex("[\\\\/:*?\"<>|]"),"_")}.$suffix")}
    DetailScaffold("导出角色卡",Page.CARD_CENTER){
      item{Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,62);Column(Modifier.weight(1f).padding(start=14.dp)){Text(card.name,fontSize=20.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("选择接收端支持的角色卡格式",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
      item{InfoBanner(Icons.Outlined.Inventory2,"完整保留角色资料","导出会包含头像、世界书、正则、预设、前端、备用问候、作者信息与时遇拍一拍设定。")}
      item{SectionTitle("JSON 格式")}
      item{SettingButton(Icons.Outlined.DataObject,"Character Card V3 JSON","字段更完整，适合新版 SillyTavern、Tavo 与兼容工具"){launch(CharacterCardExporter.Format.JSON_V3)}}
      item{SettingButton(Icons.Outlined.Code,"Character Card V2 JSON","兼容较早版本的酒馆与角色卡工具"){launch(CharacterCardExporter.Format.JSON_V2)}}
      item{SectionTitle("PNG 角色卡")}
      item{SettingButton(Icons.Outlined.Image,"Character Card V3 PNG","头像本身即角色卡，内嵌 V3 与兼容 V2 数据"){launch(CharacterCardExporter.Format.PNG_V3)}}
      item{SettingButton(Icons.Outlined.Photo,"Character Card V2 PNG","适合只识别传统 chara 元数据的工具"){launch(CharacterCardExporter.Format.PNG_V2)}}
      if(exporting)item{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Spacer(Modifier.width(9.dp));Text("正在生成角色卡…",fontSize=13.sp)}}
    }
  }

  private fun formatLabel(format:CharacterCardExporter.Format)=when(format){CharacterCardExporter.Format.JSON_V2->"CCv2 JSON";CharacterCardExporter.Format.JSON_V3->"CCv3 JSON";CharacterCardExporter.Format.PNG_V2->"CCv2 PNG";CharacterCardExporter.Format.PNG_V3->"CCv3 PNG"}

  @Composable
  private fun ImportPreviewPage(){
    val card=pendingImportCard?:return
    val detected=buildList{if(card.worldBook.isNotBlank())add("世界书");if(card.regexRules.isNotBlank())add("正则");if(card.preset.isNotBlank())add("预设");if(card.frontend.isNotBlank()||looksLikeTavernFrontend(card.greeting))add("酒馆前端")}.joinToString("、").ifBlank{"未包含扩展内容"}
    DetailScaffold("确认导入",Page.CARD_CENTER,onBack={discardPendingImport()}){
      item{InfoBanner(Icons.Outlined.FactCheck,"已解析角色卡","请核对下方内容。已识别：$detected")}
      item{Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,64);Column(Modifier.weight(1f).padding(start=14.dp)){Text(card.name,fontSize=21.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("Tavo / Tavern 兼容角色卡",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={discardPendingImport()},modifier=Modifier.weight(1f).height(48.dp)){Text("取消")};Button(onClick={confirmPendingImport()},modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.FileDownloadDone,null);Spacer(Modifier.width(7.dp));Text("确认导入")}}}
      item{SectionTitle("角色卡信息")}
      item{PreviewText("角色简介",card.desc,3)}
      item{PreviewText("角色设定",card.persona,6)}
      item{PreviewText("开场白",card.greeting,4)}
      if(card.frontend.isNotBlank()){item{SectionTitle("酒馆前端")};item{PreviewText("前端 HTML / CSS",card.frontend,8)}}
      item{SectionTitle("世界书信息")}
      if(card.worldBook.isBlank())item{InfoBanner(Icons.Outlined.MenuBook,"未识别到世界书","该文件没有可识别的世界书字段。")}
      else item{PreviewText("世界书 JSON / 文本",card.worldBook,8)}
      item{SectionTitle("正则信息")}
      if(card.regexRules.isBlank())item{InfoBanner(Icons.Outlined.CodeOff,"未识别到正则","该文件没有可识别的正则脚本字段。")}
      else item{PreviewText("正则脚本 JSON / 文本",card.regexRules,7)}
      if(card.preset.isNotBlank()){item{SectionTitle("预设信息")};item{PreviewText("角色预设",card.preset,5)}}
      item{Spacer(Modifier.height(20.dp))}
    }
  }

  @Composable
  private fun ProfilePage() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      item { BrandHeader("我的时遇", "连接、外观与数据都由你掌控") }
      item { ProfileCard { composePage = Page.USER_PROFILE } }
      item { MenuGroup("模型与偏好", listOf(
        MenuEntry(Icons.Outlined.AccountCircle, "用户资料", "昵称与头像") { composePage = Page.USER_PROFILE },
        MenuEntry(Icons.Outlined.Hub, "模型设置", "添加、切换模型供应商与生成参数") { composePage = Page.API },
        MenuEntry(Icons.Outlined.PendingActions, "生成任务", "查看正在生成的回复与最近记录") { composePage = Page.GENERATION_TASKS },
        MenuEntry(Icons.Outlined.RecordVoiceOver, "语音模型", "本地与在线 TTS、音色和语速") { ttsBackPage=Page.MAIN;composePage = Page.TTS },
        MenuEntry(Icons.Outlined.Palette, "外观与动画", "主题模式、系统栏与页面过渡") { composePage = Page.APPEARANCE },
        MenuEntry(Icons.Outlined.Public, "现实感知增强", "让角色自然了解当前日期、时间与时区") { composePage = Page.REALITY_AWARENESS },
        MenuEntry(Icons.Outlined.Science, "实验性功能", "玻璃界面、透明栏与桌宠模式") { composePage = Page.EXPERIMENTAL },
        MenuEntry(Icons.Outlined.NotificationsActive, "应用通知", "主动消息与通知设置") { composePage = Page.APP_BEHAVIOR }
      )) }
      item { MenuGroup("内容管理", listOf(
        MenuEntry(Icons.Outlined.Collections, "朋友圈设置", "角色查看、发布、自动动态、评论与点赞授权") { composePage = Page.MOMENT_PERMISSIONS },
        MenuEntry(Icons.Outlined.Style, "角色卡中心", "导入、导出和格式指引") { composePage = Page.CARD_CENTER },
        MenuEntry(Icons.Outlined.EmojiEmotions, "表情资源", "下载、导入、收藏并授权角色使用") { stickerResourcesBackPage=Page.MAIN;composePage = Page.STICKER_RESOURCES },
        MenuEntry(Icons.Outlined.Storage, "本地数据", "数据库、聊天记录和安全存储") { composePage = Page.DATA },
        MenuEntry(Icons.Outlined.Backup, "备份与恢复", "完整备份角色、聊天、记忆、图片与设置") { composePage = Page.BACKUP }
      )) }
      item { MenuGroup("帮助与关于", listOf(MenuEntry(Icons.Outlined.Feedback, "反馈与建议", "提交建议、报告问题或加入交流群") { composePage = Page.FEEDBACK },MenuEntry(Icons.Outlined.FavoriteBorder, "支持我们", "赞助时遇或联系作者") { composePage = Page.SUPPORT },MenuEntry(Icons.Outlined.Info, "关于时遇", "版本、交流群与隐私说明") { composePage = Page.ABOUT })) }
    }
  }

  @Composable
  private fun MomentsHomePage() {
    val posts=remember(uiRevision){moments()}
    var forwardingPost by remember{mutableStateOf<MomentPost?>(null)}
    val feedState=rememberLazyListState(momentFeedScrollIndex.coerceAtMost(posts.lastIndex.coerceAtLeast(0)),momentFeedScrollOffset)
    DisposableEffect(Unit){onDispose{momentFeedScrollIndex=feedState.firstVisibleItemIndex;momentFeedScrollOffset=feedState.firstVisibleItemScrollOffset}}
    Column(Modifier.fillMaxSize()) {
      BrandHeader("朋友圈","记录你和角色共同生活的片刻") {
        CircleAction(Icons.Outlined.AdminPanelSettings,"角色授权"){composePage=Page.MOMENT_PERMISSIONS}
        Spacer(Modifier.width(8.dp))
        CircleAction(Icons.Outlined.PostAdd,"发布朋友圈"){composePage=Page.MOMENTS}
      }
      if(momentRoleIds("moments_post_roles").isNotEmpty())OutlinedButton(onClick={momentRolePostBackPage=Page.MAIN;composePage=Page.MOMENT_ROLE_POST},modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp).height(44.dp)){Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(7.dp));Text("邀请角色发布",maxLines=1,softWrap=false)}
      LazyColumn(state=feedState,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(posts.isEmpty())item{EmptyState(Icons.Outlined.Collections,"还没有动态","点击右上角发布第一条朋友圈")}
        items(posts,key={it.id}){post->val role=cards.firstOrNull{it.id==post.authorId};MomentPostCard(post,role){forwardingPost=post}}
      }
    }
    forwardingPost?.let { post ->
      val targets = chats.sortedByDescending { it.updated }
      AlertDialog(
        onDismissRequest = { forwardingPost = null },
        icon = { Icon(Icons.Outlined.Share, null) },
        title = { Text("转发朋友圈") },
        text = {
          Column {
            Text("选择要发送到的角色或群聊", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (targets.isEmpty()) {
              Text("还没有可用对话", Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 360.dp).padding(top = 9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
              ) {
                items(targets, key = { it.id }) { target ->
                  val role = find(target.activeCardId.takeIf { it.isNotBlank() } ?: target.cardId)
                  Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                      if (role == null) {
                        toast("该对话的角色已不存在")
                        return@clickable
                      }
                      val meta = JSONObject()
                        .put("id", post.id)
                        .put("authorId", post.authorId)
                        .put("authorName", post.authorName)
                        .put("content", post.content)
                        .put("createdAt", post.createdAt)
                        .toString()
                      val message = Msg("user", post.content.ifBlank { "[图片动态]" }).apply {
                        messageType = "moment_share"
                        this.meta = meta
                        image = post.image
                      }
                      target.messages.add(message)
                      target.updated = System.currentTimeMillis()
                      save()
                      forwardingPost = null
                      selectedChat = target
                      composePage = Page.CHAT
                      uiRevision++
                      when {
                        sending && sendingChatId == target.id -> queueReply(target, role)
                        sending -> queueCrossChatReply(target, role)
                        else -> request(target, role)
                      }
                      toast("已转发给 ${if (target.group) target.title else role.name}")
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                  ) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                      if (role != null) RoleAvatar(role, 38) else Avatar(target.title.ifBlank { "对话" }, 38)
                      Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                          if (target.group) target.title else role?.name.orEmpty(),
                          fontWeight = FontWeight.Medium,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          if (target.group) "群聊" else target.title,
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                      Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                  }
                }
              }
            }
          }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { forwardingPost = null }) { Text("取消") } }
      )
    }
  }

  @Composable
  private fun MomentsPage() {
    var content by rememberSaveable { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf("") }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};pendingImage=uri.toString()}}
    DetailScaffold("发布朋友圈") {
      item { InfoBanner(Icons.Outlined.Collections,"只属于你的朋友圈","动态保存在本机。角色只有在你明确授权后才能在聊天中看到最近动态或代表自己发布内容。") }
      item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){UserAvatar(42);Text(prefs.getString("user_name","我")?:"我",Modifier.padding(start=11.dp),fontWeight=FontWeight.SemiBold)};OutlinedTextField(content,{content=it.take(1000)},Modifier.fillMaxWidth().padding(top=10.dp).heightIn(min=130.dp,max=240.dp),placeholder={Text("分享此刻的想法…")},supportingText={Text("${content.length} / 1000")},minLines=5,maxLines=10,shape=RoundedCornerShape(13.dp));if(pendingImage.isNotBlank()){MomentImage(pendingImage,Modifier.fillMaxWidth().padding(top=10.dp).aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)));TextButton(onClick={pendingImage=""},Modifier.align(Alignment.End)){Icon(Icons.Outlined.Delete,null);Spacer(Modifier.width(5.dp));Text("移除图片")}};OutlinedButton(onClick={picker.launch(arrayOf("image/*"))},Modifier.fillMaxWidth().padding(top=8.dp).height(46.dp)){Icon(Icons.Outlined.AddPhotoAlternate,null);Spacer(Modifier.width(6.dp));Text(if(pendingImage.isBlank())"添加图片" else "更换图片",maxLines=1,softWrap=false)}}} }
      item { Button(onClick={if(content.isBlank()&&pendingImage.isBlank())toast("请输入内容或选择图片")else{var savedImage="";if(pendingImage.isNotBlank())runCatching{val target=File(filesDir,"moment_${UUID.randomUUID()}.img");contentResolver.openInputStream(android.net.Uri.parse(pendingImage))?.use{input->FileOutputStream(target).use{output->input.copyTo(output)}};savedImage=target.absolutePath}.onFailure{toast("图片保存失败：${it.message}");return@Button};val post=MomentPost(UUID.randomUUID().toString(),"user",prefs.getString("user_name","我")?:"我",content.trim(),savedImage,System.currentTimeMillis());saveMoments(listOf(post)+moments());content="";pendingImage="";uiRevision++;toast("已发布到朋友圈");composePage=Page.MAIN}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text("发布朋友圈",maxLines=1,softWrap=false)} }
    }
  }

  @Composable
  private fun MomentPermissionsPage() {
    val availableCards = remember(uiRevision) { cards.toList() }
    var viewIds by remember(uiRevision){mutableStateOf(momentRoleIds("moments_view_roles"))}
    var postIds by remember(uiRevision){mutableStateOf(momentRoleIds("moments_post_roles"))}
    var autoIds by remember(uiRevision){mutableStateOf(momentRoleIds("moments_auto_roles"))}
    var commentIds by remember(uiRevision){mutableStateOf(momentRoleIds("moments_comment_roles"))}
    var minHours by remember{mutableStateOf(prefs.getInt("moments_auto_min_minutes",prefs.getInt("moments_auto_min_hours",4)*60).toString())}
    var maxHours by remember{mutableStateOf(prefs.getInt("moments_auto_max_minutes",prefs.getInt("moments_auto_max_hours",12)*60).toString())}
    var viewProbability by remember{mutableFloatStateOf(prefs.getInt("moments_view_probability",80).toFloat())}
    var likeProbability by remember{mutableFloatStateOf(prefs.getInt("moments_like_probability",65).toFloat())}
    var commentProbability by remember{mutableFloatStateOf(prefs.getInt("moments_comment_probability",35).toFloat())}
    val lastError=prefs.getString("moment_auto_last_error","").orEmpty()
    DetailScaffold("朋友圈角色授权",Page.MAIN) {
      item { InfoBanner(Icons.Outlined.PrivacyTip,"默认全部拒绝","所有角色使用同一个本地朋友圈。授权后，角色可在后台按概率浏览、点赞、参与已有评论，并在自己的随机时间发布动态。") }
      item { SectionTitle("后台活动时机") }
      item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(minHours,{minHours=it},"最短分钟")};Box(Modifier.weight(1f)){StyledField(maxHours,{maxHours=it},"最长分钟")}} }
      item { Button(onClick={val min=minHours.toIntOrNull();val max=maxHours.toIntOrNull();if(min==null||max==null||min<1||max<min||max>525600)toast("请输入 1 到 525600 分钟的有效范围")else{prefs.edit().putInt("moments_auto_min_minutes",min).putInt("moments_auto_max_minutes",max).apply();(autoIds+commentIds).forEach{id->prefs.edit().remove("moment_auto_next_$id").remove("moment_comment_next_$id").apply()};MomentAutoPostWorker.sync(this@ComposeActivity);toast("后台活动时间已保存")}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Schedule,null);Spacer(Modifier.width(7.dp));Text("保存随机时间范围",maxLines=1,softWrap=false)} }
      item { SectionTitle("后台互动概率") }
      item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("查看概率",fontWeight=FontWeight.Medium);Text("${viewProbability.toInt()}%",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)};Slider(viewProbability,{viewProbability=it;prefs.edit().putInt("moments_view_probability",it.toInt()).apply()},valueRange=0f..100f,steps=19);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("点赞概率",fontWeight=FontWeight.Medium);Text("${likeProbability.toInt()}%",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)};Slider(likeProbability,{likeProbability=it;prefs.edit().putInt("moments_like_probability",it.toInt()).apply()},valueRange=0f..100f,steps=19);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("评论概率",fontWeight=FontWeight.Medium);Text("${commentProbability.toInt()}%",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)};Slider(commentProbability,{commentProbability=it;prefs.edit().putInt("moments_comment_probability",it.toInt()).apply()},valueRange=0f..100f,steps=19);Text("每次后台活动分别判断，可能不查看、只查看、点赞、评论或同时互动。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}} }
      if(availableCards.isEmpty())item { EmptyState(Icons.Outlined.PersonOff,"还没有角色","先创建或导入角色卡后再配置授权") }
      items(availableCards,key={it.id}){card->Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,44);Text(card.name,Modifier.weight(1f).padding(start=11.dp),fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)};Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许查看",fontSize=14.sp,fontWeight=FontWeight.Medium);Text("聊天时可读取最近朋友圈",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=card.id in viewIds,onCheckedChange={checked->viewIds=if(checked)viewIds+card.id else viewIds-card.id;saveMomentRoleIds("moments_view_roles",viewIds)})};Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许发布",fontSize=14.sp,fontWeight=FontWeight.Medium);Text("可在角色发布页生成动态",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=card.id in postIds,onCheckedChange={checked->postIds=if(checked)postIds+card.id else postIds-card.id;saveMomentRoleIds("moments_post_roles",postIds)})};Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("合适时自动发布",fontSize=14.sp,fontWeight=FontWeight.Medium);Text("后台按随机时间生成并通知",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=card.id in autoIds,onCheckedChange={checked->autoIds=if(checked)autoIds+card.id else autoIds-card.id;if(checked){postIds=postIds+card.id;saveMomentRoleIds("moments_post_roles",postIds);ensureNotificationPermission()}else prefs.edit().remove("moment_auto_next_${card.id}").apply();saveMomentRoleIds("moments_auto_roles",autoIds);MomentAutoPostWorker.sync(this@ComposeActivity)})}}}}
      if(availableCards.isNotEmpty())item{SectionTitle("角色后台朋友圈")}
      items(availableCards,key={"comment_${it.id}"}){card->Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,40);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(card.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("随机查看共享动态，并按概率点赞或回复",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=card.id in commentIds,onCheckedChange={checked->commentIds=if(checked)commentIds+card.id else commentIds-card.id;if(checked){viewIds=viewIds+card.id;saveMomentRoleIds("moments_view_roles",viewIds);ensureNotificationPermission()}else prefs.edit().remove("moment_comment_next_${card.id}").apply();saveMomentRoleIds("moments_comment_roles",commentIds);MomentAutoPostWorker.sync(this@ComposeActivity)})}}}
      if(lastError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"最近一次自动发布失败",lastError)}
    }
  }

  @Composable
  private fun MomentRolePostPage() {
    val allowed=remember(uiRevision){cards.filter{it.id in momentRoleIds("moments_post_roles")}}
    var selectedId by remember(allowed){mutableStateOf(allowed.firstOrNull()?.id.orEmpty())}
    var direction by rememberSaveable{mutableStateOf("")}
    var generating by remember{mutableStateOf(false)}
    DetailScaffold("角色发布朋友圈",momentRolePostBackPage) {
      item { InfoBanner(Icons.Outlined.AutoAwesome,"以角色自己的口吻发布","选择已授权角色后，时遇会调用当前模型生成一条简短动态。内容生成后直接保存在本机朋友圈。") }
      if(allowed.isEmpty())item { EmptyState(Icons.Outlined.LockPerson,"没有已授权角色","前往角色授权页面，开启至少一个角色的发布权限") }
      if(allowed.isEmpty())item { Button(onClick={composePage=Page.MOMENT_PERMISSIONS},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.AdminPanelSettings,null);Spacer(Modifier.width(7.dp));Text("配置角色授权")}}
      if(allowed.isNotEmpty())item { SectionTitle("选择角色") }
      items(allowed,key={it.id}){card->val selected=selectedId==card.id;Surface(onClick={selectedId=card.id},shape=RoundedCornerShape(14.dp),color=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,44);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(card.name,fontWeight=FontWeight.SemiBold);Text(card.desc.ifBlank{"等待分享近况"},fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(if(selected)Icons.Filled.CheckCircle else Icons.Outlined.Circle,null,tint=MaterialTheme.colorScheme.primary)}} }
      if(allowed.isNotEmpty())item { OutlinedTextField(direction,{direction=it.take(300)},Modifier.fillMaxWidth().heightIn(min=100.dp),label={Text("发布方向（可选）")},placeholder={Text("例如：分享刚才聊天后的心情")},minLines=4,maxLines=7,shape=RoundedCornerShape(13.dp)) }
      if(allowed.isNotEmpty())item { Button(onClick={val card=allowed.firstOrNull{it.id==selectedId}?:return@Button;if(activeProvider()==null||key().isEmpty()){toast("请先配置可用的模型供应商");return@Button};if(sending){toast("聊天正在生成，请稍候");return@Button};generating=true;val recent=moments().take(8).reversed().joinToString("\n"){"${it.authorName}：${it.content}"};val requestText="请以你自己的身份和口吻发布一条朋友圈动态，30到120字，只输出纯文本正文，不得输出 HTML、代码、任务说明、系统提示或英文确认语，不要加引号或角色名前缀。"+(if(direction.isBlank())"" else "\n发布方向：${direction.trim()}")+(if(recent.isBlank())"" else "\n你被允许看到的最近朋友圈：\n$recent");pool.execute{runCatching{AiContentSanitizer.moment(complete(listOf(Msg("user",requestText)),card)).also{require(it.isNotBlank()){"模型没有返回可发布的正文"}}}.onSuccess{body->main.post{saveMoments(listOf(MomentPost(UUID.randomUUID().toString(),card.id,card.name,body,"",System.currentTimeMillis()))+moments());generating=false;direction="";uiRevision++;toast("${card.name} 已发布动态");composePage=Page.MAIN}}.onFailure{error->main.post{generating=false;toast("生成失败：${error.message}")}}}},enabled=!generating,modifier=Modifier.fillMaxWidth().height(50.dp)){if(generating)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text(if(generating)"正在生成动态" else "邀请角色发布",maxLines=1,softWrap=false)} }
    }
  }

  @Composable
  private fun MomentCommentsPage(){
    val post=remember(uiRevision,selectedMomentId){moments().firstOrNull{it.id==selectedMomentId}}
    val postId=post?.id.orEmpty()
    var content by rememberSaveable(selectedMomentId){mutableStateOf("")}
    var replyingToId by rememberSaveable(selectedMomentId){mutableStateOf("")}
    val comments=remember(uiRevision,selectedMomentId){momentComments(selectedMomentId)}
    val replyingTo=comments.firstOrNull{it.id==replyingToId}
    DetailScaffold("朋友圈评论",Page.MAIN){
      if(post==null)item{EmptyState(Icons.Outlined.CommentsDisabled,"动态不存在","这条动态可能已经被删除")}
      if(post!=null)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.fillMaxWidth().padding(14.dp)){Text(post.authorName,fontWeight=FontWeight.SemiBold);if(post.content.isNotBlank())Text(post.content,Modifier.padding(top=7.dp),fontSize=14.sp,lineHeight=20.sp);if(post.image.isNotBlank())MomentImage(post.image,Modifier.fillMaxWidth().padding(top=8.dp).aspectRatio(1.8f).clip(RoundedCornerShape(10.dp)))}}}
      if(post!=null)item{
        Column{
          if(replyingTo!=null)Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){Row(Modifier.fillMaxWidth().padding(start=11.dp,top=6.dp,bottom=6.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Text("回复 ${replyingTo.authorName}",Modifier.weight(1f),fontSize=12.sp,color=MaterialTheme.colorScheme.onSecondaryContainer,maxLines=1,overflow=TextOverflow.Ellipsis);IconButton(onClick={replyingToId=""},Modifier.size(30.dp)){Icon(Icons.Outlined.Close,"取消回复",Modifier.size(16.dp))}}}
          OutlinedTextField(content,{content=it.take(500)},Modifier.fillMaxWidth().padding(top=if(replyingTo==null)0.dp else 7.dp).heightIn(min=92.dp),label={Text(if(replyingTo==null)"写下评论" else "回复 ${replyingTo.authorName}")},supportingText={Text("${content.length} / 500")},minLines=3,maxLines=6,shape=RoundedCornerShape(13.dp))
        }
      }
      if(post!=null)item{Button(onClick={if(content.isBlank())toast("请输入评论内容")else{val value=MomentComment(UUID.randomUUID().toString(),"user",prefs.getString("user_name","我")?:"我",content.trim(),System.currentTimeMillis(),replyingTo?.id.orEmpty(),replyingTo?.authorName.orEmpty());saveMomentComments(post.id,comments+value);content="";replyingToId="";uiRevision++;toast(if(replyingTo==null)"评论已发布" else "回复已发布")}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text(if(replyingTo==null)"发表评论" else "回复评论",maxLines=1,softWrap=false)}}
      if(post!=null)item{SectionTitle("评论 ${comments.size} 条")}
      items(comments,key={it.id}){comment->
        val role=cards.firstOrNull{it.id==comment.authorId}
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.padding(start=if(comment.parentId.isBlank())0.dp else 30.dp)){
          Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.Top){
            if(role!=null)RoleAvatar(role,36)else UserAvatar(36)
            Column(Modifier.weight(1f).padding(horizontal=10.dp)){
              Row(verticalAlignment=Alignment.CenterVertically){Text(comment.authorName,fontWeight=FontWeight.SemiBold,fontSize=13.sp);if(comment.replyToName.isNotBlank()){Text(" 回复 ",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(comment.replyToName,fontSize=12.sp,fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.secondary)};Spacer(Modifier.width(7.dp));Text(time(comment.createdAt),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
              Text(comment.content,Modifier.padding(top=4.dp),fontSize=14.sp,lineHeight=20.sp)
            }
            IconButton(onClick={replyingToId=comment.id},Modifier.size(34.dp)){Icon(Icons.Outlined.Reply,"回复评论",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.secondary)}
            IconButton(onClick={val removeIds=mutableSetOf(comment.id);var changed=true;while(changed){changed=false;comments.filter{it.parentId in removeIds}.forEach{if(removeIds.add(it.id))changed=true}};saveMomentComments(postId,comments.filterNot{it.id in removeIds});if(replyingToId in removeIds)replyingToId="";uiRevision++;toast("评论已删除")},Modifier.size(34.dp)){Icon(Icons.Outlined.Delete,"删除评论",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}
          }
        }
      }
    }
  }

  @Composable
  private fun UserProfilePage() {
    var nickname by remember { mutableStateOf(prefs.getString("user_name","我") ?: "我") }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};prefs.edit().putString("user_avatar",uri.toString()).apply();uiRevision++;toast("头像已更新")}}
    DetailScaffold("用户资料") {
      item { Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){UserAvatar(88);Spacer(Modifier.height(12.dp));OutlinedButton(onClick={picker.launch(arrayOf("image/*"))}){Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(8.dp));Text("选择头像")}} }
      item { StyledField(nickname,{nickname=it},"用户昵称") }
      item { SettingButton(Icons.Outlined.Badge,"身份设定",prefs.getString("active_identity_name","")?.takeIf{it.isNotBlank()}?.let{"当前：$it"}?:"创建并切换多个用户身份"){composePage=Page.IDENTITIES} }
      item { Button(onClick={if(nickname.isBlank())toast("昵称不能为空")else{prefs.edit().putString("user_name",nickname.trim()).apply();uiRevision++;toast("用户资料已保存");composePage=Page.MAIN}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存用户资料")}}
    }
  }

  @Composable
  private fun IdentitiesPage(){
    var values by remember(uiRevision){mutableStateOf(identities())}
    var editingId by remember{mutableStateOf<String?>(null)}
    var name by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    val activeId=prefs.getString("active_identity_id","")?:""
    DetailScaffold("身份设定",Page.USER_PROFILE){
      item{InfoBanner(Icons.Outlined.Badge,"告诉角色你是谁","当前身份描述会随聊天请求发送给模型，只保存在本机；昵称和头像不会随身份切换改变。")}
      item{StyledField(name,{name=it},"身份名称")}
      item{OutlinedTextField(description,{description=it},Modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),label={Text("身份描述")},placeholder={Text("例如：我是住在海边的插画师，性格慢热，和角色是多年好友。")},minLines=5,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){if(editingId!=null)OutlinedButton(onClick={editingId=null;name="";description=""},Modifier.weight(1f).height(48.dp)){Text("取消编辑")};Button(onClick={if(name.isBlank()||description.isBlank())toast("请填写身份名称和描述")else{val value=IdentityConfig(editingId?:UUID.randomUUID().toString(),name.trim(),description.trim());val updated=if(editingId==null)values+value else values.map{if(it.id==value.id)value else it};saveIdentities(updated);if(activeId.isBlank()||activeId==value.id)activateIdentity(value);values=updated;editingId=null;name="";description="";uiRevision++;toast("身份已保存")}},Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text(if(editingId==null)"保存身份" else "保存修改",maxLines=1)}}}
      item{SectionTitle("本地身份 ${values.size} 个")}
      if(values.isEmpty())item{EmptyState(Icons.Outlined.Badge,"还没有身份设定","填写名称和描述后保存")}
      items(values,key={it.id}){value->Surface(onClick={activateIdentity(value)},shape=RoundedCornerShape(14.dp),color=if(value.id==activeId)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(start=15.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(value.id==activeId)Icons.Filled.CheckCircle else Icons.Outlined.Badge,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(value.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(value.description,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={editingId=value.id;name=value.name;description=value.description}){Icon(Icons.Outlined.Edit,"编辑")};IconButton(onClick={val updated=values.filterNot{it.id==value.id};saveIdentities(updated);if(value.id==activeId){prefs.edit().remove("active_identity_id").remove("active_identity_name").remove("active_identity_description").apply()};values=updated;uiRevision++;toast("身份已删除")}){Icon(Icons.Outlined.Delete,"删除",tint=MaterialTheme.colorScheme.error)}}}}
    }
  }

  @Composable
  private fun RoleSettingsPage() {
    val card=find(editingCardId?:return)?:return
    val draftKey="role_draft_${card.id}";val draft=remember(card.id){runCatching{JSONObject(prefs.getString(draftKey,"{}").orEmpty())}.getOrElse{JSONObject()}}
    var name by remember(card.id){mutableStateOf(draft.optString("name",card.name))};var desc by remember(card.id){mutableStateOf(draft.optString("desc",card.desc))};var persona by remember(card.id){mutableStateOf(draft.optString("persona",card.persona))};var greeting by remember(card.id){mutableStateOf(draft.optString("greeting",card.greeting))};var frontend by remember(card.id){mutableStateOf(draft.optString("frontend",card.frontend))};var worldBook by remember(card.id){mutableStateOf(draft.optString("worldBook",card.worldBook))};var regex by remember(card.id){mutableStateOf(draft.optString("regex",card.regexRules))};var preset by remember(card.id){mutableStateOf(draft.optString("preset",card.preset))};var avatarSource by remember(card.id){mutableStateOf(draft.optString("avatar",card.avatar))};var patSelf by remember(card.id){mutableStateOf(draft.optString("patSelf",card.patSelfText))};var patUser by remember(card.id){mutableStateOf(draft.optString("patUser",card.patUserText))}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};avatarSource=uri.toString()}}
    var draftResolved by remember(card.id){mutableStateOf(false)}
    fun persistDraft(){val changed=name!=card.name||desc!=card.desc||persona!=card.persona||greeting!=card.greeting||frontend!=card.frontend||worldBook!=card.worldBook||regex!=card.regexRules||preset!=card.preset||avatarSource!=card.avatar||patSelf!=card.patSelfText||patUser!=card.patUserText;if(changed){val value=JSONObject().put("name",name).put("desc",desc).put("persona",persona).put("greeting",greeting).put("frontend",frontend).put("worldBook",worldBook).put("regex",regex).put("preset",preset).put("avatar",avatarSource).put("patSelf",patSelf).put("patUser",patUser);prefs.edit().putString(draftKey,value.toString()).apply()}else prefs.edit().remove(draftKey).apply()}
    LaunchedEffect(name,desc,persona,greeting,frontend,worldBook,regex,preset,avatarSource,patSelf,patUser){delay(650);persistDraft()}
    DisposableEffect(draftKey){onDispose{if(!draftResolved)persistDraft()}}
    val memoryCount=remember(uiRevision,card.id){db.memories(card.id).size}
    DetailScaffold("角色设置",roleBackPage){
      if(draft.length()>0)item{InfoBanner(Icons.Outlined.EditNote,"已恢复未保存草稿","修改会自动保存在本机；点击保存后才会正式写入角色卡。")}
      item{Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){if(avatarSource==card.avatar)RoleAvatar(card,78)else MessageImage(avatarSource,Modifier.size(78.dp).clip(RoundedCornerShape(16.dp)));OutlinedButton(onClick={picker.launch(arrayOf("image/*"))},Modifier.padding(top=9.dp)){Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(7.dp));Text("更换角色头像")}}}
      item{StyledField(name,{name=it},"角色名称")}
      item{OutlinedTextField(desc,{desc=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=180.dp),label={Text("角色简介")},minLines=3,maxLines=7,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(persona,{persona=it},Modifier.fillMaxWidth().heightIn(min=140.dp,max=260.dp),label={Text("角色设定")},minLines=5,maxLines=11,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(greeting,{greeting=it},Modifier.fillMaxWidth().heightIn(min=100.dp,max=200.dp),label={Text("开场白")},minLines=4,maxLines=8,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(frontend,{frontend=it},Modifier.fillMaxWidth().heightIn(min=120.dp,max=260.dp),label={Text("酒馆前端 HTML（可选）")},minLines=5,maxLines=11,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("角色卡扩展")}
      item{OutlinedTextField(worldBook,{worldBook=it},Modifier.fillMaxWidth().heightIn(min=110.dp,max=210.dp),label={Text("世界书")},minLines=4,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(regex,{regex=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=210.dp),label={Text("正则规则")},minLines=3,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(preset,{preset=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=190.dp),label={Text("对话预设")},minLines=3,maxLines=8,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("拍一拍")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(patSelf,{patSelf=it.take(40)},"用户拍角色")};Box(Modifier.weight(1f)){StyledField(patUser,{patUser=it.take(40)},"角色拍用户")}}}
      item{SettingButton(Icons.Outlined.Psychology,"长期记忆","系统已记录 $memoryCount 条，点击查看与管理"){composePage=Page.MEMORY}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={name=card.name;desc=card.desc;persona=card.persona;greeting=card.greeting;frontend=card.frontend;worldBook=card.worldBook;regex=card.regexRules;preset=card.preset;avatarSource=card.avatar;patSelf=card.patSelfText;patUser=card.patUserText;prefs.edit().remove(draftKey).apply();toast("草稿已丢弃")},Modifier.weight(1f).height(50.dp)){Text("丢弃草稿",maxLines=1)};Button(onClick={if(name.isBlank())toast("角色名称不能为空")else{var savedAvatar=avatarSource;if(avatarSource.startsWith("content:")){runCatching{val file=File(filesDir,"avatar_${card.id}.png");contentResolver.openInputStream(android.net.Uri.parse(avatarSource))?.use{input->FileOutputStream(file).use{output->input.copyTo(output)}}?:error("无法读取头像");savedAvatar=file.absolutePath;bitmapCache.remove(file.absolutePath)}.onFailure{toast("头像保存失败：${it.message}");return@Button}};card.name=name.trim();card.desc=desc;card.persona=persona;card.greeting=greeting;card.frontend=frontend.trim();card.worldBook=worldBook;card.regexRules=regex;card.preset=preset;card.avatar=savedAvatar;card.patSelfText=patSelf.trim().ifBlank{"肩膀"};card.patUserText=patUser.trim().ifBlank{"肩膀"};save();DesktopPetService.sync(this@ComposeActivity);draftResolved=true;prefs.edit().remove(draftKey).apply();uiRevision++;toast("角色设置已保存");composePage=roleBackPage}},Modifier.weight(1f).height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存",maxLines=1)}}}
    }
  }

  @Composable
  private fun MemoryPage() {
    val card=find(editingCardId?:return)?:return
    var revision by remember{mutableIntStateOf(0)};var newMemory by remember{mutableStateOf("")};val memories=remember(card.id,revision){db.memories(card.id)}
    DetailScaffold("${card.name}的长期记忆",Page.ROLE_SETTINGS){
      item{InfoBanner(Icons.Outlined.Psychology,"系统长期记忆","聊天请求会自动携带这些记录。你可以在这里查看、修改或删除。")}
      item{OutlinedTextField(newMemory,{newMemory=it},Modifier.fillMaxWidth().heightIn(min=92.dp),label={Text("添加一条长期记忆")},minLines=3,shape=RoundedCornerShape(14.dp))}
      item{Button(onClick={if(newMemory.isBlank())toast("请输入记忆内容")else{db.addMemory(card.id,newMemory.trim());newMemory="";revision++;uiRevision++;toast("长期记忆已添加")}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text("添加记忆",maxLines=1,softWrap=false)}}
      item{SectionTitle("已记录 ${memories.size} 条")}
      if(memories.isEmpty())item{EmptyState(Icons.Outlined.Memory,"暂无长期记忆","系统记录的内容会显示在这里")}
      items(memories,key={it.id}){memory->var value by remember(memory.id,revision){mutableStateOf(memory.content)};Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(13.dp)){OutlinedTextField(value,{value=it},Modifier.fillMaxWidth(),minLines=2,shape=RoundedCornerShape(12.dp));Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.End){TextButton(onClick={db.deleteMemory(memory.id);revision++;uiRevision++;toast("记忆已删除")}){Icon(Icons.Outlined.Delete,null,tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(5.dp));Text("删除",color=MaterialTheme.colorScheme.error)};Button(onClick={db.updateMemory(memory.id,value.trim());revision++;uiRevision++;toast("记忆已保存")}){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(5.dp));Text("保存")}}}}}
    }
  }

  @Composable
  private fun RenameChatPage(chat:Chat){var title by remember(chat.id){mutableStateOf(chat.title)};DetailScaffold("重命名对话",renameBackPage){item{InfoBanner(Icons.Outlined.Edit,"对话名称","名称只用于本地列表显示，不会发送给模型。")};item{StyledField(title,{title=it},"对话名称")};item{Button(onClick={if(title.isBlank())toast("名称不能为空")else{chat.title=title.trim();save();uiRevision++;toast("对话已重命名");composePage=renameBackPage}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存名称",maxLines=1,softWrap=false)}}}}

  @Composable
  private fun ApiPage() {
    val active=activeProvider()
    DetailScaffold("模型设置") {
      item { SettingButton(Icons.Outlined.Business, "模型供应商", active?.let{"${it.name} · ${it.modelName}"} ?: "暂未配置模型供应商") { composePage = Page.PROVIDER } }
      item { SettingButton(Icons.Outlined.Tune, "生成参数", "温度与上下文消息数量") { composePage = Page.GENERATION } }
      item { SettingButton(Icons.Outlined.TravelExplore, "联网搜索 API", if(prefs.getBoolean("web_search_enabled",false))"已开启 · ${prefs.getString("web_search_provider","tavily")}" else "未开启 · Tavily / Serper / 通用接口") { webSearchBackPage=Page.API;composePage = Page.WEB_SEARCH } }
      item { SettingButton(Icons.Outlined.ManageSearch, "嵌入模型实时库", if(prefs.getBoolean("embedding_enabled",false))"已开启 · ${db.realtimeCount("")} 条语义片段" else "未开启 · 实时索引与相关记忆检索") { embeddingBackPage=Page.API;composePage = Page.EMBEDDING_LIBRARY } }
      item { SettingButton(Icons.Outlined.Image, "生图模型", if(prefs.getBoolean("image_generation_enabled",false))prefs.getString("image_generation_model","已开启")?:"已开启" else "未开启 · OpenAI / NAI 兼容接口") { imageGenerationBackPage=Page.API;composePage = Page.IMAGE_GENERATION } }
      item { SettingButton(Icons.Outlined.BugReport, "API 错误详情", if(lastApiError.isBlank())"暂无错误记录" else lastApiError) { composePage = Page.API_ERROR } }
    }
  }

  @Composable
  private fun ApiErrorPage() {
    DetailScaffold("API 错误详情", Page.API) {
      if(lastApiError.isBlank()) item { EmptyState(Icons.Outlined.CheckCircle,"暂无 API 错误","请求失败时会在这里保留最近一次详细信息") }
      else {
        item { InfoBanner(Icons.Outlined.ErrorOutline,"最近一次错误",lastApiError) }
        item { OutlinedButton(onClick={lastApiError="";uiRevision++;toast("错误记录已清除")},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(8.dp));Text("清除错误记录",maxLines=1,softWrap=false)} }
      }
    }
  }

  @Composable
  private fun ProviderPage() {
    var allProviders by remember(uiRevision) { mutableStateOf(providers()) }
    DetailScaffold("模型提供商", Page.API) {
      item { InfoBanner(Icons.Outlined.Verified, "已配置供应商", if(allProviders.isEmpty())"暂未配置模型供应商，请点击下方按钮添加。" else "切换时会同时切换 API 地址、Key 和已选择模型。") }
      item { Button(onClick={editingProviderId=null;providerBackPage=Page.PROVIDER;composePage=Page.ADD_PROVIDER},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text("添加模型供应商",maxLines=1,softWrap=false)} }
      items(allProviders,key={it.id}) { value ->
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(start=4.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){SettingButton(if(activeProvider()?.id==value.id)Icons.Filled.CheckCircle else Icons.Outlined.Cloud,value.name,if(activeProvider()?.id==value.id)"当前使用 · ${value.modelName}" else value.modelName){activateProvider(value)}};IconButton(onClick={editingProviderId=value.id;providerBackPage=Page.PROVIDER;composePage=Page.ADD_PROVIDER}){Icon(Icons.Outlined.Edit,"编辑")};IconButton(onClick={val wasActive=activeProvider()?.id==value.id;val updated=allProviders.filterNot{it.id==value.id};saveCustomProviders(updated);if(wasActive){prefs.edit().remove("active_provider_id").remove("provider").remove("endpoint").remove("model").apply();runCatching{secure.put("")}};allProviders=updated;uiRevision++;toast("供应商已删除")}){Icon(Icons.Outlined.Delete,"删除",tint=MaterialTheme.colorScheme.error)}}}
      }
    }
  }

  @Composable
  private fun AddProviderPage(){
    val editing=providers().firstOrNull{it.id==editingProviderId}
    var name by remember(editingProviderId){mutableStateOf(editing?.name.orEmpty())};var url by remember(editingProviderId){mutableStateOf(editing?.endpoint.orEmpty())};var providerKey by remember(editingProviderId){mutableStateOf(editing?.let{secure.getNamed("provider_key_${it.id}")}.orEmpty())};var selectedModel by remember(editingProviderId){mutableStateOf(editing?.modelName.orEmpty())};var models by remember{mutableStateOf<List<String>>(emptyList())};var loading by remember{mutableStateOf(false)}
    DetailScaffold(if(editing==null)"添加模型供应商" else "编辑模型供应商",providerBackPage){
      item{InfoBanner(Icons.Outlined.AddLink,"完整供应商配置","在一个页面中保存名称、API 地址、Key 和模型。保存后可在供应商列表自由切换。")}
      if(editing==null)item{SectionTitle("快速填充")}
      if(editing==null)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.fillMaxWidth().heightIn(max=310.dp).verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){listOf(ProviderConfig("","小老鼠的奶酪公益工坊","https://api.tniay.top/v","gpt-4o-mini"),ProviderConfig("","OpenAI","https://api.openai.com/v1","gpt-4o-mini"),ProviderConfig("","DeepSeek","https://api.deepseek.com/v1","deepseek-chat"),ProviderConfig("","OpenRouter","https://openrouter.ai/api/v1","openai/gpt-4o-mini"),ProviderConfig("","Google Gemini","https://generativelanguage.googleapis.com/v1beta/openai","gemini-2.5-flash"),ProviderConfig("","通义千问 DashScope","https://dashscope.aliyuncs.com/compatible-mode/v1","qwen-plus"),ProviderConfig("","硅基流动","https://api.siliconflow.cn/v1","deepseek-ai/DeepSeek-V3"),ProviderConfig("","Groq","https://api.groq.com/openai/v1","llama-3.3-70b-versatile"),ProviderConfig("","Together AI","https://api.together.xyz/v1","meta-llama/Llama-3.3-70B-Instruct-Turbo"),ProviderConfig("","Moonshot","https://api.moonshot.cn/v1","moonshot-v1-8k"),ProviderConfig("","智谱 BigModel","https://open.bigmodel.cn/api/paas/v4","glm-4-flash"),ProviderConfig("","NVIDIA NIM","https://integrate.api.nvidia.com/v1","meta/llama-3.1-70b-instruct")).forEach{preset->OutlinedButton(onClick={name=preset.name;url=preset.endpoint;selectedModel=preset.modelName},Modifier.fillMaxWidth().height(42.dp)){Text(preset.name,Modifier.weight(1f),maxLines=1,softWrap=false);Icon(Icons.Outlined.ChevronRight,null)}}}}}
      item{SectionTitle("连接配置")};item{StyledField(name,{name=it},"供应商名称")};item{StyledField(url,{url=it},"API 基础地址")};item{StyledField(providerKey,{providerKey=it},"API Key")};item{StyledField(selectedModel,{selectedModel=it},"模型")}
      item{Button(onClick={if(url.isBlank()||providerKey.isBlank())toast("请先填写 API 地址和 Key")else{loading=true;pool.execute{try{val base=url.trim().trimEnd('/').removeSuffix("/chat/completions");val c=URL("$base/models").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Authorization","Bearer "+providerKey.trim());val code=c.responseCode;val raw=read(if(code<300)c.inputStream else c.errorStream);if(code !in 200..299)throw java.io.IOException("HTTP $code");val data=JSONObject(raw).optJSONArray("data")?:JSONArray();val result=mutableListOf<String>();for(i in 0 until data.length())data.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()}?.let(result::add);result.sort();main.post{models=result;loading=false}}catch(e:Exception){main.post{loading=false;toast("模型获取失败：${e.message}")}}}}},Modifier.fillMaxWidth().height(46.dp)){if(loading)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(if(loading)"正在获取模型" else "调用 API 获取模型",maxLines=1,softWrap=false)}}
      if(models.isNotEmpty())item{ModelListCard(models,selectedModel){selectedModel=it}}
      item{Button(onClick={if(name.isBlank()||url.isBlank()||providerKey.isBlank()||selectedModel.isBlank())toast("请完整填写供应商、地址、Key 和模型")else{val value=ProviderConfig(editing?.id?:UUID.randomUUID().toString(),name.trim(),url.trim().trimEnd('/').removeSuffix("/chat/completions"),selectedModel.trim());val updated=if(editing==null)providers()+value else providers().map{if(it.id==value.id)value else it};saveCustomProviders(updated);runCatching{secure.putNamed("provider_key_${value.id}",providerKey.trim())};if(activeProvider()?.id==value.id||editing==null)activateProvider(value);toast(if(editing==null)"供应商已添加并启用" else "供应商配置已保存");editingProviderId=null;composePage=providerBackPage}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text(if(editing==null)"保存并启用" else "保存修改",maxLines=1,softWrap=false)}}
    }
  }

  @Composable
  private fun ConnectionPage() {
    var endpoint by remember { mutableStateOf(baseEndpoint()) }; var apiKey by remember { mutableStateOf(key()) }
    DetailScaffold("接口与密钥", Page.API) {
      item { StyledField(endpoint,{endpoint=it},"API 基础地址") }
      item { StyledField(apiKey,{apiKey=it},"API Key（Keystore 加密）") }
      item { Button(onClick={prefs.edit().putString("endpoint",endpoint.trim()).apply();runCatching{secure.put(apiKey.trim());val active=prefs.getString("active_provider_id","")?:"";if(active.isNotBlank())secure.putNamed("provider_key_$active",apiKey.trim())}.onSuccess{toast("连接已保存");composePage=Page.API}.onFailure{toast("密钥保存失败")}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存连接")}}
    }
  }

  @Composable
  private fun ModelPage() {
    var modelName by remember { mutableStateOf(model()) }; var models by remember { mutableStateOf<List<String>>(emptyList()) }; var loading by remember { mutableStateOf(false) }
    DetailScaffold("模型", Page.API) {
      item { StyledField(modelName,{modelName=it},"模型名称") }
      item { Button(onClick={if(key().isEmpty()){toast("请先设置 API Key")}else{loading=true;pool.execute{try{val c=URL(modelsEndpoint()).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Authorization","Bearer "+key());val code=c.responseCode;val raw=read(if(code<300)c.inputStream else c.errorStream);if(code !in 200..299)throw java.io.IOException("HTTP $code");val data=JSONObject(raw).optJSONArray("data")?:JSONArray();val list=mutableListOf<String>();for(i in 0 until data.length()){data.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()}?.let(list::add)};list.sort();main.post{models=list;loading=false}}catch(e:Exception){main.post{loading=false;toast("获取失败：${e.message}")}}}}},Modifier.fillMaxWidth().height(48.dp)){if(loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(if(loading)"正在获取…" else "自动获取模型列表")}}
      if(models.isNotEmpty()) item { SectionTitle("可用模型") }
      if(models.isNotEmpty())item{ModelListCard(models,modelName){modelName=it}}
      item { Button(onClick={if(modelName.isBlank())toast("请输入模型名称")else{prefs.edit().putString("model",modelName.trim()).apply();toast("模型已保存");composePage=Page.API}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存模型")}}
    }
  }

  @Composable
  private fun GenerationPage() {
    var temperature by remember { mutableStateOf(String.format(Locale.ROOT,"%.2f",java.lang.Double.longBitsToDouble(prefs.getLong("temp",java.lang.Double.doubleToLongBits(.85))))) };var context by remember{mutableStateOf(prefs.getInt("context",30).toString())};var autoRetry by remember{mutableStateOf(prefs.getBoolean("generation_auto_retry",true))};var retryCount by remember{mutableFloatStateOf(prefs.getInt("generation_auto_retry_count",2).toFloat())}
    DetailScaffold("生成参数",Page.API){
      item{StyledField(temperature,{temperature=it},"温度（0 - 2）")};item{StyledField(context,{context=it},"上下文消息数（2 - 9999）")}
      item{SectionTitle("错误恢复")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){ChatSettingSwitch("生成错误自动重试","网络、限流、超时或服务端异常时自动再次请求",autoRetry){autoRetry=it};AnimatedVisibility(autoRetry){Column{Text("最多重试 ${retryCount.toInt()} 次",Modifier.padding(top=8.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(retryCount,{retryCount=it},valueRange=1f..5f,steps=3);Text("认证、参数配置错误和手动停止不会重试。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
      item{Button(onClick={val t=temperature.toDoubleOrNull();val count=context.toIntOrNull();if(t==null||count==null)toast("请输入有效数字")else{prefs.edit().putLong("temp",java.lang.Double.doubleToLongBits(t.coerceIn(0.0,2.0))).putInt("context",count.coerceIn(2,9999)).putBoolean("generation_auto_retry",autoRetry).putInt("generation_auto_retry_count",retryCount.toInt().coerceIn(1,5)).apply();toast("生成参数已保存");composePage=Page.API}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存生成参数")}}
    }
  }

  @Composable
  private fun WebSearchPage(){
    var enabled by remember{mutableStateOf(prefs.getBoolean("web_search_enabled",false))}
    var auto by remember{mutableStateOf(prefs.getBoolean("web_search_auto",false))}
    var provider by remember{mutableStateOf(prefs.getString("web_search_provider","tavily")?:"tavily")}
    var endpoint by remember{mutableStateOf(prefs.getString("web_search_endpoint","").orEmpty().ifBlank{if(provider=="serper")"https://google.serper.dev/search" else if(provider=="tavily")"https://api.tavily.com/search" else ""})}
    var searchKey by remember{mutableStateOf(secure.getNamed("web_search_api_key"))}
    var maxResults by remember{mutableFloatStateOf(prefs.getInt("web_search_max_results",5).toFloat().coerceIn(1f,10f))}
    var testing by remember{mutableStateOf(false)}
    fun select(value:String){provider=value;endpoint=when(value){"tavily"->"https://api.tavily.com/search";"serper"->"https://google.serper.dev/search";else->""}}
    fun persist(showToast:Boolean=true):Boolean{
      if(enabled&&(endpoint.isBlank()||searchKey.isBlank())){toast("请填写搜索 API 地址和 Key");return false}
      prefs.edit().putBoolean("web_search_enabled",enabled).putBoolean("web_search_auto",auto).putString("web_search_provider",provider).putString("web_search_endpoint",endpoint.trim()).putInt("web_search_max_results",maxResults.toInt()).apply()
      runCatching{secure.putNamed("web_search_api_key",searchKey.trim())}.onFailure{toast("搜索 Key 保存失败");return false}
      if(showToast)toast("联网搜索设置已保存");uiRevision++;return true
    }
    DetailScaffold("联网搜索 API",webSearchBackPage){
      item{InfoBanner(Icons.Outlined.TravelExplore,"按需获取实时资料","手动联网只作用于下一条消息；自动模式仅在问题包含最新、天气、价格等实时意图时请求搜索接口。搜索结果会发送给当前聊天模型。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){ChatSettingSwitch("启用联网搜索","聊天工具中显示联网按钮",enabled){enabled=it};ChatSettingSwitch("自动判断实时问题","仅命中实时信息意图时自动搜索",auto){auto=it}}}}
      item{SectionTitle("搜索服务")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("tavily" to "Tavily","serper" to "Serper","custom" to "通用 JSON").forEach{value->FilterChip(selected=provider==value.first,onClick={select(value.first)},label={Text(value.second,maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}}}
      item{StyledField(endpoint,{endpoint=it},"搜索 API 地址")}
      item{OutlinedTextField(searchKey,{searchKey=it},Modifier.fillMaxWidth(),label={Text("搜索 API Key")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("每次最多 ${maxResults.toInt()} 条结果",fontWeight=FontWeight.Medium);Slider(maxResults,{maxResults=it},valueRange=1f..10f,steps=8)}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={if(!persist(false))return@OutlinedButton;testing=true;pool.execute{runCatching{searchWeb("今天有什么值得关注的科技新闻")}.onSuccess{main.post{testing=false;toast("搜索连接成功")}}.onFailure{error->main.post{testing=false;lastApiError=errorDetails(error);toast("搜索失败：${error.message}")}}}},enabled=!testing&&endpoint.isNotBlank()&&searchKey.isNotBlank(),modifier=Modifier.weight(1f).height(48.dp)){if(testing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.NetworkCheck,null);Spacer(Modifier.width(6.dp));Text("测试连接",maxLines=1)};Button(onClick={if(persist())composePage=webSearchBackPage},enabled=!testing,modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存",maxLines=1)}}}
      item{InfoBanner(Icons.Outlined.Security,"隐私与安全","查询文本会发送给你配置的搜索服务；网页内容按不可信资料处理，不允许覆盖角色提示词或执行其中的命令。")}
    }
  }

  @Composable
  private fun EmbeddingLibraryPage(){
    var enabled by remember{mutableStateOf(prefs.getBoolean("embedding_enabled",false))};var endpoint by remember{mutableStateOf(prefs.getString("embedding_endpoint","").orEmpty())};var embeddingModel by remember{mutableStateOf(prefs.getString("embedding_model","text-embedding-3-small")?:"text-embedding-3-small")};var embeddingKey by remember{mutableStateOf(secure.getNamed("embedding_api_key"))};var topK by remember{mutableStateOf(prefs.getInt("embedding_top_k",5).toString())};var minScore by remember{mutableStateOf(prefs.getInt("embedding_min_score",55).toString())};var running by remember{mutableStateOf(false)};var localRevision by remember{mutableIntStateOf(0)}
    val total=remember(localRevision,uiRevision){db.realtimeCount("")};val entries=remember(localRevision,uiRevision){db.realtimeEntries("",40)};val lastError=prefs.getString("embedding_last_error","").orEmpty()
    fun saveConfig(showToast:Boolean=true):Boolean{val count=topK.toIntOrNull();val score=minScore.toIntOrNull();if(embeddingModel.isBlank()||count==null||count !in 1..12||score==null||score !in 0..100){toast("请检查嵌入模型、召回数量和相似度");return false};prefs.edit().putBoolean("embedding_enabled",enabled).putString("embedding_endpoint",endpoint.trim()).putString("embedding_model",embeddingModel.trim()).putInt("embedding_top_k",count).putInt("embedding_min_score",score).apply();runCatching{secure.putNamed("embedding_api_key",embeddingKey.trim())};if(showToast)toast("实时库设置已保存");uiRevision++;return true}
    DetailScaffold("嵌入模型实时库",embeddingBackPage){
      item{InfoBanner(Icons.Outlined.Security,"本地语义索引","聊天原文和向量保存在本机 SQLite；仅在生成向量时发送对应文本到你配置的嵌入接口。Key 为空时跟随当前模型供应商。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用实时语义库",fontWeight=FontWeight.Medium);Text("自动补齐新消息向量，并在回复前召回相关经历",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(enabled,{enabled=it})}}}
      item{StyledField(endpoint,{endpoint=it},"嵌入接口（留空跟随供应商 /embeddings）")};item{StyledField(embeddingModel,{embeddingModel=it},"嵌入模型")};item{OutlinedTextField(embeddingKey,{embeddingKey=it},Modifier.fillMaxWidth(),label={Text("嵌入 API Key（留空跟随供应商）")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(topK,{topK=it},"召回片段 1-12")};Box(Modifier.weight(1f)){StyledField(minScore,{minScore=it},"最低相似度 %")}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={if(saveConfig(false)){running=true;pool.execute{runCatching{embedTexts(listOf("时遇实时语义库连接测试"))}.onSuccess{main.post{running=false;prefs.edit().remove("embedding_last_error").apply();toast("嵌入接口连接成功，向量维度 ${it.firstOrNull()?.length()?:0}")}}.onFailure{error->main.post{running=false;prefs.edit().putString("embedding_last_error",error.message?:error.toString()).apply();localRevision++;toast("连接失败：${error.message}")}}}}},enabled=!running,modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.NetworkCheck,null);Spacer(Modifier.width(6.dp));Text("测试连接",maxLines=1)};Button(onClick={saveConfig()},enabled=!running,modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存设置",maxLines=1)}}}
      item{SectionTitle("实时库状态")};item{InfoBanner(Icons.Outlined.ManageSearch,"$total 条语义片段","每次对话只批量索引尚未处理的消息；检索按角色隔离并依据余弦相似度排序。")}
      item{Button(onClick={if(!saveConfig(false))return@Button;running=true;pool.execute{runCatching{rebuildRealtimeLibrary()}.onSuccess{count->main.post{running=false;localRevision++;toast("实时库已重建，共 $count 条")}}.onFailure{error->main.post{running=false;prefs.edit().putString("embedding_last_error",error.message?:error.toString()).apply();localRevision++;toast("重建失败：${error.message}")}}}},enabled=!running&&chats.isNotEmpty(),modifier=Modifier.fillMaxWidth().height(48.dp)){if(running)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(7.dp));Text(if(running)"正在处理语义片段" else "重建全部聊天实时库",maxLines=1)}}
      item{OutlinedButton(onClick={askDanger("清空嵌入实时库","只删除本机向量索引，不会删除聊天、角色或长期记忆。","确认清空"){db.clearRealtimeLibrary();localRevision++;toast("实时库已清空")}},enabled=!running&&total>0,modifier=Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(7.dp));Text("清空向量索引",maxLines=1)}}
      if(lastError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"最近一次嵌入错误",lastError)}
      if(entries.isNotEmpty())item{SectionTitle("最近索引")};items(entries,key={it.sourceKey}){entry->val role=cards.firstOrNull{it.id==entry.cardId};Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(13.dp)){Text(role?.name?:"已删除角色",fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(entry.content,Modifier.padding(top=4.dp),maxLines=3,overflow=TextOverflow.Ellipsis,fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
    }
  }

  @Composable
  private fun ImageGenerationPage(){
    var enabled by remember{mutableStateOf(prefs.getBoolean("image_generation_enabled",false))}
    var protocol by remember{mutableStateOf(prefs.getString("image_generation_protocol","openai")?:"openai")}
    var endpoint by remember{mutableStateOf(prefs.getString("image_generation_endpoint","")?.takeIf{it.isNotBlank()}?:baseEndpoint().trimEnd('/').removeSuffix("/chat/completions")+"/images/generations")}
    var model by remember{mutableStateOf(prefs.getString("image_generation_model","dall-e-3")?:"dall-e-3")}
    var size by remember{mutableStateOf(prefs.getString("image_generation_size","1024x1024")?:"1024x1024")}
    var negative by remember{mutableStateOf(prefs.getString("image_generation_negative","lowres, bad anatomy, blurry")?:"")}
    var steps by remember{mutableFloatStateOf(prefs.getInt("image_generation_nai_steps",28).toFloat())}
    var scale by remember{mutableFloatStateOf(prefs.getInt("image_generation_nai_scale",7).toFloat())}
    var imageKey by remember{mutableStateOf(secure.getNamed("image_generation_key"))}
    var confirmEach by remember{mutableStateOf(prefs.getBoolean("image_generation_confirm_each",true))}
    DetailScaffold("生图模型",imageGenerationBackPage){
      item{InfoBanner(Icons.Outlined.Image,"兼容 OpenAI 与 NAI","支持 OpenAI Images 和第三方 NovelAI/NAI 兼容接口。角色只在对话单独授权且剧情适合时调用，图片保存本机并纳入备份。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用生图功能",fontWeight=FontWeight.Medium);Text("每个对话仍需单独允许角色调用",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=enabled,onCheckedChange={enabled=it})}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("生成前逐次确认",fontWeight=FontWeight.Medium);Text("角色提出生图请求后，由你点击确认才会调用接口",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=confirmEach,onCheckedChange={confirmEach=it})}}}
      item{SectionTitle("接口协议")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=protocol=="openai",onClick={protocol="openai";endpoint=baseEndpoint().trimEnd('/').removeSuffix("/chat/completions")+"/images/generations";model="dall-e-3";size="1024x1024"},label={Text("OpenAI Images",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f));FilterChip(selected=protocol=="nai",onClick={protocol="nai";endpoint="https://image.novelai.net/ai/generate-image";model="nai-diffusion-3";size="832x1216"},label={Text("NovelAI / NAI",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}}
      item{StyledField(endpoint,{endpoint=it},"Images 接口地址")}
      item{StyledField(model,{model=it},"生图模型")}
      item{StyledField(imageKey,{imageKey=it},"专用 API Key（留空复用当前供应商）")}
      item{SectionTitle("图片尺寸")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){(if(protocol=="nai")listOf("832x1216","1216x832","1024x1024","512x768")else listOf("1024x1024","1024x1536","1536x1024","512x512")).forEach{value->FilterChip(selected=size==value,onClick={size=value},label={Text(value,maxLines=1,softWrap=false)})}}}
      if(protocol=="nai")item{OutlinedTextField(negative,{negative=it.take(1000)},Modifier.fillMaxWidth().heightIn(min=96.dp),label={Text("负面提示词")},minLines=3,maxLines=6,shape=RoundedCornerShape(13.dp))}
      if(protocol=="nai")item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("采样步数 ${steps.toInt()}",fontWeight=FontWeight.Medium);Slider(steps,{steps=it},valueRange=10f..50f,steps=39);Text("提示词引导 ${scale.toInt()}",fontWeight=FontWeight.Medium);Slider(scale,{scale=it},valueRange=1f..15f,steps=13)}}}
      item{Button(onClick={if(enabled&&(endpoint.isBlank()||model.isBlank()))toast("请填写生图接口和模型")else{prefs.edit().putBoolean("image_generation_enabled",enabled).putBoolean("image_generation_confirm_each",confirmEach).putString("image_generation_protocol",protocol).putString("image_generation_endpoint",endpoint.trim()).putString("image_generation_model",model.trim()).putString("image_generation_size",size).putString("image_generation_negative",negative.trim()).putInt("image_generation_nai_steps",steps.toInt()).putInt("image_generation_nai_scale",scale.toInt()).apply();runCatching{secure.putNamed("image_generation_key",imageKey.trim())}.onSuccess{uiRevision++;toast("生图模型设置已保存");composePage=imageGenerationBackPage}.onFailure{toast("生图密钥保存失败")}}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存生图设置",maxLines=1,softWrap=false)}}
    }
  }

  @Composable
  private fun TtsPage() {
    var provider by remember { mutableStateOf(prefs.getString("tts_provider","local")?:"local") }
    var endpoint by remember { mutableStateOf(prefs.getString("tts_endpoint","https://api.openai.com/v1/audio/speech")?:"") }
    var apiKey by remember { mutableStateOf(secure.getNamed("tts_api_key_encrypted")) }
    var model by remember { mutableStateOf(prefs.getString("tts_model","gpt-4o-mini-tts")?:"") }
    var voice by remember { mutableStateOf(prefs.getString("tts_voice","alloy")?:"") }
    var locale by remember { mutableStateOf(prefs.getString("tts_locale","zh-CN")?:"zh-CN") }
    var localEngine by remember { mutableStateOf((prefs.getString("tts_engine","")?:"").takeUnless{it=="auto_xiaomi"}.orEmpty()) }
    val installedEngines=remember{packageManager.queryIntentServices(Intent("android.intent.action.TTS_SERVICE"),0).mapNotNull{info->info.serviceInfo?.packageName?.let{pkg->pkg to (info.loadLabel(packageManager)?.toString()?.ifBlank{pkg}?:pkg)}}.distinctBy{it.first}}
    var rate by remember { mutableFloatStateOf(java.lang.Float.intBitsToFloat(prefs.getInt("tts_rate",java.lang.Float.floatToIntBits(1f)))) }
    var pitch by remember { mutableFloatStateOf(java.lang.Float.intBitsToFloat(prefs.getInt("tts_pitch",java.lang.Float.floatToIntBits(1f)))) }
    var quoteOnly by remember { mutableStateOf(prefs.getBoolean("tts_quote_only",false)) }
    var autoPlay by remember { mutableStateOf(prefs.getBoolean("tts_auto_play",true)) }
    var sttLocalFirst by remember { mutableStateOf(prefs.getBoolean("stt_local_first",true)) }
    var sttFallback by remember { mutableStateOf(prefs.getBoolean("stt_online_fallback",true)) }
    var sttEndpoint by remember { mutableStateOf(prefs.getString("stt_endpoint","")?:"") }
    var sttModel by remember { mutableStateOf(prefs.getString("stt_model","whisper-1")?:"whisper-1") }
    var sttKey by remember { mutableStateOf(secure.getNamed("stt_api_key_encrypted")) }
    var sttDownloading by remember{mutableStateOf(OfflineSpeechDownloadService.running(this@ComposeActivity))}
    var sttProgress by remember{mutableIntStateOf(prefs.getInt("offline_stt_download_progress",0))}
    var sttProgressText by remember{mutableStateOf(prefs.getString("offline_stt_download_label","").orEmpty())}
    val systemRecognitionAvailable=remember{android.speech.SpeechRecognizer.isRecognitionAvailable(this@ComposeActivity)}
    val offlineModelInstalled=remember(uiRevision){OfflineSpeechRepository.isInstalled(this@ComposeActivity)}
    DisposableEffect(Unit){val receiver=object:android.content.BroadcastReceiver(){override fun onReceive(context:android.content.Context?,intent:Intent?){sttDownloading=OfflineSpeechDownloadService.running(this@ComposeActivity);sttProgress=prefs.getInt("offline_stt_download_progress",0);sttProgressText=prefs.getString("offline_stt_download_label","").orEmpty();uiRevision++}};androidx.core.content.ContextCompat.registerReceiver(this@ComposeActivity,receiver,android.content.IntentFilter(OfflineSpeechDownloadService.ACTION_STATE_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);onDispose{runCatching{unregisterReceiver(receiver)}}}
    fun saveSettings():Boolean=runCatching{secure.putNamed("tts_api_key_encrypted",apiKey.trim());secure.putNamed("stt_api_key_encrypted",sttKey.trim());prefs.edit().putString("tts_provider",provider).putString("tts_endpoint",endpoint.trim()).putString("tts_model",model.trim()).putString("tts_voice",voice.trim()).putString("tts_locale",locale.trim()).putString("tts_engine",localEngine).putInt("tts_rate",java.lang.Float.floatToIntBits(rate)).putInt("tts_pitch",java.lang.Float.floatToIntBits(pitch)).putBoolean("tts_quote_only",quoteOnly).putBoolean("tts_auto_play",autoPlay).putBoolean("stt_local_first",sttLocalFirst).putBoolean("stt_online_fallback",sttFallback).putString("stt_endpoint",sttEndpoint.trim()).putString("stt_model",sttModel.trim().ifBlank{"whisper-1"}).apply();if(provider=="local")resetLocalSpeechEngine();true}.getOrElse{toast("语音配置保存失败");false}
    DetailScaffold("语音模型",ttsBackPage){
      item{InfoBanner(Icons.Outlined.RecordVoiceOver,"本地与在线 TTS","本地模式使用手机已安装的语音引擎；在线模式兼容 OpenAI /v1/audio/speech 接口。")}
      item{SectionTitle("提供方式")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=provider=="local",onClick={provider="local"},label={Text("本地语音")},leadingIcon={Icon(Icons.Outlined.PhoneAndroid,null)},modifier=Modifier.weight(1f));FilterChip(selected=provider=="online",onClick={provider="online"},label={Text("在线模型")},leadingIcon={Icon(Icons.Outlined.Cloud,null)},modifier=Modifier.weight(1f))}}
      if(provider=="local"){
        item{StyledField(locale,{locale=it},"语言区域（如 zh-CN）")}
        item{SectionTitle("本地语音引擎")}
        item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.fillMaxWidth().heightIn(max=260.dp).verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onClick={localEngine=""},Modifier.fillMaxWidth().height(42.dp)){Text("系统默认",Modifier.weight(1f),maxLines=1);if(localEngine.isBlank())Icon(Icons.Filled.Check,null)};installedEngines.forEach{entry->OutlinedButton(onClick={localEngine=entry.first},Modifier.fillMaxWidth().height(42.dp)){Text(entry.second,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);if(localEngine==entry.first)Icon(Icons.Filled.Check,null)}}}}}
        if(installedEngines.isEmpty())item{InfoBanner(Icons.Outlined.WarningAmber,"未检测到本地引擎","请进入系统设置安装语音引擎和中文语音包，或切换到在线模型。")}
        item{SettingButton(Icons.Outlined.SettingsVoice,"系统 TTS 引擎与语音包","安装、选择或试听本地语音"){runCatching{startActivity(Intent("com.android.settings.TTS_SETTINGS"))}.recoverCatching{startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))}.onFailure{toast("无法打开系统语音设置")}}}
      }else{
        item{OutlinedButton(onClick={endpoint="https://api.xiaomimimo.com/v1/audio/speech";model="mimo-v2.5-tts";voice=""},Modifier.fillMaxWidth().height(52.dp)){Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f),horizontalAlignment=Alignment.Start){Text("MiMo V2.5 TTS 预设",fontWeight=FontWeight.SemiBold,maxLines=1);Text("小米在线模型 · 音色按开放平台填写",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)}}}
        item{StyledField(endpoint,{endpoint=it},"TTS 接口地址")}
        item{OutlinedTextField(apiKey,{apiKey=it},Modifier.fillMaxWidth(),label={Text("TTS API Key")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}
        item{StyledField(model,{model=it},"模型（如 gpt-4o-mini-tts）")}
        item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("gpt-4o-mini-tts","tts-1","mimo-v2.5-tts","FunAudioLLM/CosyVoice2-0.5B").forEach{value->AssistChip(onClick={model=value},label={Text(value,maxLines=1)})}}}
        item{StyledField(voice,{voice=it},"音色 / voice_id")}
      }
      item{SectionTitle("声音参数")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("语速 ${String.format(Locale.ROOT,"%.2f",rate)}",fontWeight=FontWeight.Medium);Slider(rate,{rate=it},valueRange=.5f..2f,steps=14);if(provider=="local"){Text("音调 ${String.format(Locale.ROOT,"%.2f",pitch)}",fontWeight=FontWeight.Medium);Slider(pitch,{pitch=it},valueRange=.5f..2f,steps=14)}}}}
      item{SectionTitle("朗读行为")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TTS 仅朗读引用内容",fontWeight=FontWeight.Medium);Text("仅朗读双引号、「」、『』或《》中的内容",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=quoteOnly,onCheckedChange={quoteOnly=it})};HorizontalDivider(Modifier.padding(horizontal=15.dp));Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("生成后自动播放 TTS",fontWeight=FontWeight.Medium);Text("AI 消息生成完成后自动开始朗读",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=autoPlay,onCheckedChange={autoPlay=it})}}}}
      item{SectionTitle("语音识别")}
      item{InfoBanner(if(offlineModelInstalled||systemRecognitionAvailable)Icons.Outlined.CheckCircle else Icons.Outlined.CloudSync,if(offlineModelInstalled)"App 离线语音模型可用" else if(systemRecognitionAvailable)"系统语音识别可用" else "未检测到本地识别服务",if(offlineModelInstalled)"聊天麦克风会优先使用 Vosk 小模型，录音不离开设备。" else if(systemRecognitionAvailable)"可优先调用设备本地识别；是否完全离线取决于手机安装的语音服务与语言包。" else "可下载 App 小模型，或启用在线识别回退。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.GraphicEq,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(OfflineSpeechRepository.MODEL_LABEL,fontWeight=FontWeight.Medium);Text(if(offlineModelInstalled)"已安装 · ${String.format(Locale.getDefault(),"%.1f MB",OfflineSpeechRepository.installedBytes(this@ComposeActivity)/1048576.0)}" else "约 42 MB · 中文离线语音转文字",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(offlineModelInstalled)Icon(Icons.Filled.CheckCircle,null,tint=MaterialTheme.colorScheme.secondary)};if(sttDownloading){LinearProgressIndicator(progress={sttProgress/100f},Modifier.fillMaxWidth().padding(top=11.dp));Text(sttProgressText,Modifier.padding(top=4.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Column(Modifier.fillMaxWidth().padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(OfflineSpeechRepository.SOURCE_URL)))},Modifier.fillMaxWidth(),contentPadding=PaddingValues(horizontal=8.dp)){Icon(Icons.Outlined.OpenInNew,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("GitHub 项目",maxLines=1)};Button(onClick={if(offlineModelInstalled)askDanger("删除离线语音模型","删除后可继续使用系统或在线语音识别。","确认删除"){cancelOfflineVoiceRecognition();OfflineSpeechRepository.delete(this@ComposeActivity);uiRevision++}else{sttDownloading=true;sttProgress=1;sttProgressText="准备下载";OfflineSpeechDownloadService.start(this@ComposeActivity)}},enabled=!sttDownloading,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(horizontal=8.dp)){Icon(if(offlineModelInstalled)Icons.Outlined.Delete else Icons.Outlined.Download,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(if(offlineModelInstalled)"删除模型" else if(sttDownloading)"后台下载中" else "下载模型",maxLines=1)}}}}
      }
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("设备本地识别优先",fontWeight=FontWeight.Medium);Text(if(offlineModelInstalled)"优先使用 App 内 Vosk 模型" else "请求系统优先使用已下载的离线语言包",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=sttLocalFirst,onCheckedChange={sttLocalFirst=it},enabled=systemRecognitionAvailable||offlineModelInstalled)}}}
      item{SettingButton(Icons.Outlined.DownloadForOffline,"系统离线语音包","打开手机语音输入设置，安装或管理本地识别模型"){runCatching{startActivity(Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS))}.recoverCatching{startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))}.onFailure{toast("无法打开系统语音输入设置")}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("在线识别回退",fontWeight=FontWeight.Medium);Text("接口和 Key 留空时跟随当前模型供应商",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=sttFallback,onCheckedChange={sttFallback=it})}}}
      if(sttFallback){item{StyledField(sttEndpoint,{sttEndpoint=it},"识别接口（留空自动使用当前供应商）")};item{OutlinedTextField(sttKey,{sttKey=it},Modifier.fillMaxWidth(),label={Text("识别 API Key（留空跟随当前供应商）")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))};item{StyledField(sttModel,{sttModel=it},"识别模型（如 whisper-1）")}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={if(saveSettings())toggleSpeech("“你好，我是时遇。”这是一段语音模型试听。")},Modifier.weight(1f).height(48.dp)){Icon(if(speechSpeaking)Icons.Filled.Stop else Icons.Outlined.PlayArrow,null);Spacer(Modifier.width(7.dp));Text(if(speechSpeaking)"停止试听" else "试听",maxLines=1)};Button(onClick={if(saveSettings()){toast("语音模型设置已保存");composePage=ttsBackPage}},Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存",maxLines=1)}}}
    }
  }

  @Composable
  private fun ChatTtsPage(chat:Chat) {
    var enabled by remember(chat.id){mutableStateOf(chat.ttsEnabled)}
    var mode by remember(chat.id){mutableIntStateOf(chat.ttsReplyMode)}
    val modes=listOf(1 to ("仅语音" to "只显示可播放的语音气泡"),2 to ("文字与语音" to "显示文字和语音播放入口"),3 to ("随机回复" to "每条回复随机选择文字或语音"))
    DetailScaffold("语音回复",Page.CHAT_DETAIL){
      item{InfoBanner(Icons.Outlined.GraphicEq,"当前对话单独配置","AI 完整回复后才开始朗读，流式生成过程中不会重复播报。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用语音回复",fontWeight=FontWeight.Medium);Text("关闭时保持普通文字聊天",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=enabled,onCheckedChange={enabled=it})}}}
      item{SectionTitle("AI 回复方式")}
      items(modes){entry->val value=entry.second;Surface(onClick={mode=entry.first},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(mode==entry.first)Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.padding(start=12.dp)){Text(value.first,fontWeight=FontWeight.Medium);Text(value.second,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
      item{SettingButton(Icons.Outlined.SettingsVoice,"配置语音模型",if((prefs.getString("tts_provider","local")?:"local")=="local")"当前使用本地 Android TTS" else "${prefs.getString("tts_model","在线模型")} · ${prefs.getString("tts_voice","")}"){ttsBackPage=Page.CHAT_TTS;composePage=Page.TTS}}
      item{Button(onClick={chat.ttsEnabled=enabled;chat.ttsReplyMode=mode;save();uiRevision++;toast("语音回复设置已保存");composePage=Page.CHAT_DETAIL},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存语音回复设置",maxLines=1)}}
    }
  }

  @Composable
  private fun ExperimentalPage(){
    var inputGlass by remember{mutableStateOf(prefs.getBoolean("experimental_input_glass",false))}
    var transparentTop by remember{mutableStateOf(prefs.getBoolean("experimental_top_bar_transparent",false))}
    var globalGlass by remember{mutableStateOf(prefs.getBoolean("experimental_glass_ui",false))}
    var glassOpacity by remember{mutableFloatStateOf(prefs.getInt("experimental_glass_opacity",78).toFloat())}
    var petEnabled by remember{mutableStateOf(prefs.getBoolean("experimental_desktop_pet_enabled",false))}
    var petStyle by remember{mutableStateOf(prefs.getString("experimental_desktop_pet_style","avatar")?:"avatar")}
    var petCardId by remember{mutableStateOf(prefs.getString("experimental_desktop_pet_card_id","").orEmpty())}
    var petMessageNotice by remember{mutableStateOf(prefs.getString("desktop_pet_message_notice","both")?:"both")}
    var petSize by remember{mutableFloatStateOf(if(petStyle=="live2d")prefs.getInt("experimental_live2d_pet_size",180).toFloat().coerceIn(80f,320f) else prefs.getInt("experimental_desktop_pet_size",76).toFloat().coerceIn(32f,160f))}
    var petSizeInput by remember{mutableStateOf(petSize.toInt().toString())}
    var live2dBusy by remember{mutableStateOf(false)}
    var petGuideExpanded by remember{mutableStateOf(false)}
    var screenContextEnabled by remember{mutableStateOf(prefs.getBoolean("experimental_screen_context_enabled",false))}
    var accessibilityGranted by remember{mutableStateOf(ScreenContextAccessibilityService.available())}
    val petLastError=prefs.getString("desktop_pet_last_error","").orEmpty()
    val overlayGranted=android.os.Build.VERSION.SDK_INT<23||android.provider.Settings.canDrawOverlays(this@ComposeActivity)
    val overlayPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
      val granted=android.os.Build.VERSION.SDK_INT<23||android.provider.Settings.canDrawOverlays(this@ComposeActivity)
      if(granted&&cards.isNotEmpty()){if(cards.none{it.id==petCardId})petCardId=cards.first().id;petEnabled=true;prefs.edit().putString("experimental_desktop_pet_card_id",petCardId).putBoolean("experimental_desktop_pet_enabled",true).apply();DesktopPetService.sync(this@ComposeActivity);toast("桌宠模式已开启")}else if(granted)toast("请先创建或导入一个角色")else toast("需要悬浮窗权限才能显示桌宠")
      uiRevision++
    }
    val accessibilityPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
      accessibilityGranted=ScreenContextAccessibilityService.available();uiRevision++
    }
    val live2dModelImporter=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){live2dBusy=true;pool.execute{runCatching{Live2DRepository.importModelZip(this@ComposeActivity,uri)}.onSuccess{main.post{live2dBusy=false;uiRevision++;DesktopPetService.sync(this@ComposeActivity);toast("Live2D 模型已导入")}}.onFailure{error->main.post{live2dBusy=false;toast("模型导入失败：${error.message}")}}}}}
    DisposableEffect(Unit){val receiver=object:android.content.BroadcastReceiver(){override fun onReceive(context:android.content.Context?,intent:Intent?){uiRevision++}};androidx.core.content.ContextCompat.registerReceiver(this@ComposeActivity,receiver,android.content.IntentFilter(Live2DCoreDownloadService.ACTION_STATE_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);onDispose{runCatching{unregisterReceiver(receiver)}}}
    fun persistVisuals(){prefs.edit().putBoolean("experimental_input_glass",inputGlass).putBoolean("experimental_top_bar_transparent",transparentTop).putBoolean("experimental_glass_ui",globalGlass).putInt("experimental_glass_opacity",glassOpacity.toInt()).apply();uiRevision++}
    fun persistPetSize(value:Float){val range=if(petStyle=="live2d")80f..320f else 32f..160f;petSize=value.coerceIn(range);petSizeInput=petSize.toInt().toString();prefs.edit().putInt(if(petStyle=="live2d")"experimental_live2d_pet_size" else "experimental_desktop_pet_size",petSize.toInt()).apply();DesktopPetService.sync(this@ComposeActivity);uiRevision++}
    DetailScaffold("实验性功能"){
      item{InfoBanner(Icons.Outlined.Science,"仍在打磨中的体验","实验性外观和桌宠可随时关闭，不会改变聊天记录、角色卡或模型配置。")}
      item{SectionTitle("聊天玻璃效果")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("底部输入栏美化","使用克制的半透明层次与细描边",inputGlass){inputGlass=it;persistVisuals()}
        ChatSettingSwitch("聊天顶部栏透明","背景图延伸到角色名称和模型区域",transparentTop){transparentTop=it;persistVisuals()}
      }}}
      item{SectionTitle("全局玻璃材质")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("全 App 玻璃材质 UI","让页面、面板与导航使用平静的半透明材质",globalGlass){globalGlass=it;persistVisuals()}
        AnimatedVisibility(globalGlass){Column{Text("材质不透明度 ${glassOpacity.toInt()}%",Modifier.padding(top=8.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(glassOpacity,{glassOpacity=it},valueRange=48f..96f,steps=11,onValueChangeFinished={persistVisuals()})}}
      }}}
      item{InfoBanner(Icons.Outlined.TipsAndUpdates,"显示说明","玻璃效果使用纯色半透明面板和细描边，不添加发光或彩色渐变；较低不透明度可能降低文字可读性。")}
      item{SectionTitle("桌宠模式")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("显示角色桌宠",if(overlayGranted)"悬浮陪伴、真实互动回复与快捷消息" else "需要先授予悬浮窗权限",petEnabled){enabled->
          if(enabled&&cards.isEmpty())toast("请先创建或导入一个角色")
          else if(enabled&&!overlayGranted){runCatching{overlayPermission.launch(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:$packageName")))}.onFailure{toast("无法打开悬浮窗权限设置")}}
          else{if(enabled&&cards.none{it.id==petCardId})petCardId=cards.first().id;petEnabled=enabled;prefs.edit().putString("experimental_desktop_pet_card_id",petCardId).putBoolean("experimental_desktop_pet_enabled",enabled).apply();DesktopPetService.sync(this@ComposeActivity);uiRevision++}
        }
        Text("桌宠样式",Modifier.padding(top=10.dp,bottom=5.dp),fontSize=13.sp,fontWeight=FontWeight.Medium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=petStyle=="avatar",onClick={petStyle="avatar";petSize=prefs.getInt("experimental_desktop_pet_size",76).toFloat().coerceIn(32f,160f);petSizeInput=petSize.toInt().toString();prefs.edit().putString("experimental_desktop_pet_style",petStyle).apply();DesktopPetService.sync(this@ComposeActivity);uiRevision++},label={Text("角色头像")},leadingIcon={Icon(Icons.Outlined.AccountCircle,null,Modifier.size(17.dp))},modifier=Modifier.weight(1f));FilterChip(selected=petStyle=="live2d",onClick={petStyle="live2d";petSize=prefs.getInt("experimental_live2d_pet_size",180).toFloat().coerceIn(80f,320f);petSizeInput=petSize.toInt().toString();prefs.edit().putString("experimental_desktop_pet_style",petStyle).apply();DesktopPetService.sync(this@ComposeActivity);uiRevision++},label={Text("Live2D")},leadingIcon={Icon(Icons.Outlined.Animation,null,Modifier.size(17.dp))},modifier=Modifier.weight(1f))}
        if(petStyle=="live2d"){
          val hasCore=Live2DRepository.hasRuntime(this@ComposeActivity);val coreDownloading=Live2DCoreDownloadService.running(this@ComposeActivity);val coreProgress=prefs.getInt("live2d_core_download_progress",0);val coreLabel=prefs.getString("live2d_core_download_label","").orEmpty();val coreError=prefs.getString("live2d_core_download_error","").orEmpty();val models=Live2DRepository.listModels(this@ComposeActivity);val selectedModelId=Live2DRepository.selectedModelId(this@ComposeActivity);val hasModel=models.isNotEmpty()
          Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Column(Modifier.fillMaxWidth().padding(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(if(hasCore&&hasModel)Icons.Outlined.CheckCircle else Icons.Outlined.Animation,null,tint=if(hasCore&&hasModel)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant);Column(Modifier.weight(1f).padding(start=9.dp)){Text(if(hasModel)"Live2D 模型库 · ${models.size}" else "尚未导入模型",fontSize=13.sp,fontWeight=FontWeight.Medium);Text("Cubism Core ${if(hasCore)"已就绪" else "未导入"} · ${if(hasModel)"当前 ${Live2DRepository.modelName(this@ComposeActivity)}" else "模型未导入"}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            Row(Modifier.fillMaxWidth().padding(top=9.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedButton(onClick={Live2DCoreDownloadService.start(this@ComposeActivity);uiRevision++},enabled=!coreDownloading,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=8.dp)){if(coreDownloading)CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)else Icon(if(hasCore)Icons.Outlined.Refresh else Icons.Outlined.Download,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(if(coreDownloading)"下载中" else if(hasCore)"重新下载 Core" else "一键下载 Core",maxLines=1)};Button(onClick={live2dModelImporter.launch(arrayOf("application/zip","application/octet-stream","*/*"))},enabled=!live2dBusy,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=8.dp)){Icon(Icons.Outlined.Add,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("添加模型",maxLines=1)}}
            if(coreDownloading){LinearProgressIndicator(progress={coreProgress.coerceIn(0,100)/100f},Modifier.fillMaxWidth().padding(top=9.dp));Text(coreLabel,Modifier.padding(top=4.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}else if(coreError.isNotBlank()&&!hasCore)Text(coreError,Modifier.padding(top=7.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.error)
            if(models.isNotEmpty()){HorizontalDivider(Modifier.padding(vertical=10.dp));Text("选择模型",fontSize=12.sp,fontWeight=FontWeight.Medium);models.forEach{model->Row(Modifier.fillMaxWidth().clickable{if(Live2DRepository.selectModel(this@ComposeActivity,model.id)){uiRevision++;DesktopPetService.sync(this@ComposeActivity)}}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=model.id==selectedModelId,onClick=null);Text(model.name,Modifier.weight(1f).padding(start=5.dp),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis);IconButton(onClick={askDanger("删除 Live2D 模型","将删除“${model.name}”的模型文件，不影响其他模型、角色卡或聊天记录。","确认删除"){Live2DRepository.deleteModel(this@ComposeActivity,model.id);uiRevision++;DesktopPetService.sync(this@ComposeActivity)}},modifier=Modifier.size(38.dp)){Icon(Icons.Outlined.Delete,"删除 ${model.name}",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}
            }
          }}
        }
        if(cards.isNotEmpty()){
          Text("桌宠角色",Modifier.padding(top=10.dp,bottom=7.dp),fontSize=13.sp,fontWeight=FontWeight.Medium)
          Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){cards.forEach{card->FilterChip(selected=petCardId==card.id,onClick={petCardId=card.id;prefs.edit().putString("experimental_desktop_pet_card_id",card.id).apply();DesktopPetService.sync(this@ComposeActivity)},label={Text(card.name,maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={if(petCardId==card.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})}}
          val sizeLabel=if(petStyle=="live2d")when{petSize<155f->"小巧";petSize<205f->"标准";petSize<250f->"醒目";else->"大型"}else when{petSize<64f->"小巧";petSize<90f->"标准";petSize<116f->"醒目";else->"大型"}
          Text("悬浮${if(petStyle=="live2d")"模型" else "头像"} ${petSize.toInt()} dp · $sizeLabel",Modifier.padding(top=12.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={persistPetSize(petSize-if(petStyle=="live2d")8f else 4f)},modifier=Modifier.size(40.dp)){Icon(Icons.Outlined.Remove,"缩小桌宠")};Slider(petSize,{petSize=it;petSizeInput=it.toInt().toString()},Modifier.weight(1f),valueRange=if(petStyle=="live2d")80f..320f else 32f..160f,steps=if(petStyle=="live2d")29 else 31,onValueChangeFinished={persistPetSize(petSize)});IconButton(onClick={persistPetSize(petSize+if(petStyle=="live2d")8f else 4f)},modifier=Modifier.size(40.dp)){Icon(Icons.Outlined.Add,"放大桌宠")}}
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(petSizeInput,{petSizeInput=it.filter(Char::isDigit).take(3)},Modifier.weight(1f),label={Text("输入 dp")},singleLine=true,shape=RoundedCornerShape(12.dp));Button(onClick={val value=petSizeInput.toIntOrNull();if(value==null)toast("请输入桌宠 dp 大小")else persistPetSize(value.toFloat())},Modifier.height(54.dp),contentPadding=PaddingValues(horizontal=14.dp)){Text("应用",maxLines=1,softWrap=false)}}
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){(if(petStyle=="live2d")listOf(96 to "小",180 to "中",260 to "大",320 to "特大")else listOf(36 to "微",56 to "小",96 to "中",144 to "大")).forEach{(size,label)->FilterChip(selected=kotlin.math.abs(petSize-size)<4,onClick={persistPetSize(size.toFloat())},label={Text(label)},modifier=Modifier.weight(1f))}}
          Text("新消息提示",Modifier.padding(top=12.dp,bottom=6.dp),fontSize=13.sp,fontWeight=FontWeight.Medium)
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("dot" to "绿色提示","bubble" to "气泡提示","both" to "同时显示").forEach{(value,label)->FilterChip(selected=petMessageNotice==value,onClick={petMessageNotice=value;prefs.edit().putString("desktop_pet_message_notice",value).apply();DesktopPetService.sync(this@ComposeActivity)},label={Text(label,maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}
          }
        }else Text("创建或导入角色后即可选择桌宠。",Modifier.padding(top=9.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().clickable{petGuideExpanded=!petGuideExpanded}.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.MenuBook,null,tint=MaterialTheme.colorScheme.secondary);Text("桌宠使用指南",Modifier.weight(1f).padding(start=10.dp),fontWeight=FontWeight.Medium);Icon(if(petGuideExpanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)};AnimatedVisibility(petGuideExpanded){Column(Modifier.fillMaxWidth().padding(start=15.dp,end=15.dp,bottom=15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("点击：空闲时互动；绿色提示或回复气泡可直接进入对应聊天。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("拖动：移动桌宠；气泡左侧手柄支持上下左右移动，右侧手柄调整宽度。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("长按：投喂、快捷回复、分析屏幕、返回聊天或关闭桌宠。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("气泡：箭头进入快捷回复，关闭图标只收起当前气泡。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("Live2D：导入官方 Cubism Web Core 与包含 model3.json、moc3、纹理的 ZIP。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
      item{SectionTitle("屏幕内容分析")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("允许桌宠分析当前屏幕","仅在长按桌宠并点击眼睛时读取可见文字",screenContextEnabled){value->screenContextEnabled=value;prefs.edit().putBoolean("experimental_screen_context_enabled",value).apply();uiRevision++}
        Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(accessibilityGranted)Icons.Outlined.VerifiedUser else Icons.Outlined.AdminPanelSettings,null,tint=if(accessibilityGranted)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(if(accessibilityGranted)"系统权限已开启" else "尚未开启系统无障碍权限",fontWeight=FontWeight.Medium,fontSize=13.sp);Text("密码输入框会自动跳过，内容只在主动点击后发送",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};OutlinedButton(onClick={runCatching{accessibilityPermission.launch(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))}.onFailure{toast("无法打开无障碍设置")}},contentPadding=PaddingValues(horizontal=11.dp)){Text(if(accessibilityGranted)"权限设置" else "去授权",maxLines=1,softWrap=false)}}
      }}}
      item{InfoBanner(Icons.Outlined.PrivacyTip,"按需读取","时遇不会后台持续记录屏幕。关闭应用内开关后，眼睛按钮不读取内容；系统权限可随时在无障碍设置中撤销。")}
      item{InfoBanner(Icons.Outlined.AutoAwesome,"角色真实互动","点击头像或投喂会注入所选角色的人设、系统提示、世界书、预设、长期记忆和最近聊天，并使用当前模型生成回复。")}
      item{InfoBanner(Icons.Outlined.TouchApp,"悬浮消息与快捷回复","角色回复会显示在头像外侧；绿色圆点仅表示有未读角色消息，打开对应聊天或点击气泡后自动消失。")}
      if(petLastError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"最近一次桌宠回复失败",petLastError.take(260))}
      item{SectionTitle("外部聊天连接")}
      item{SettingButton(Icons.Outlined.Link,"将聊天角色接入微信",if(WeixinLinkClient.connected(this@ComposeActivity))"${WeixinLinkClient.accounts(this@ComposeActivity).size} 个角色 Bot 已连接" else "选择角色并扫码连接"){composePage=Page.WEIXIN_LINK}}
    }
  }

  @Composable
  private fun WeixinLinkPage(){
    var localRevision by remember{mutableIntStateOf(0)}
    var busy by remember{mutableStateOf(false)}
    var verifyCode by remember{mutableStateOf("")}
    var selectedCardId by remember{mutableStateOf(prefs.getString("weixin_link_login_card_id","").orEmpty().ifBlank{cards.firstOrNull()?.id.orEmpty()})}
    var selectedChatId by remember{mutableStateOf("")}
    val accounts=WeixinLinkClient.accounts(this@ComposeActivity)
    val status=prefs.getString("weixin_link_status","disconnected").orEmpty()
    val statusText=prefs.getString("weixin_link_status_text","尚未连接微信").orEmpty()
    val qrUrl=prefs.getString("weixin_link_login_qr_url","").orEmpty()
    val verifyPending=prefs.getString("weixin_link_verify_code","").orEmpty()
    val qrBitmap=remember(qrUrl){if(qrUrl.isBlank())null else runCatching{WeixinLinkClient.qrBitmap(qrUrl,720)}.getOrNull()}
    var historyCardId by remember{mutableStateOf(selectedCardId)}
    var historyChatId by remember{mutableStateOf("")}
    val selectedChats=remember(uiRevision,selectedCardId){chats.filter{!it.group&&it.cardId==selectedCardId}.sortedByDescending{it.updated}}
    val historyChats=remember(uiRevision,historyCardId){chats.filter{!it.group&&it.cardId==historyCardId}.sortedByDescending{it.updated}}
    LaunchedEffect(selectedCardId,uiRevision){
      if(selectedChatId.isBlank()||selectedChats.none{it.id==selectedChatId})selectedChatId=selectedChats.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(historyCardId,uiRevision){
      if(historyChatId.isBlank()||historyChats.none{it.id==historyChatId})historyChatId=historyChats.firstOrNull()?.id.orEmpty()
    }
    val historyImporter=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
      if(uri!=null){
        val card=cards.firstOrNull{it.id==historyCardId}
        if(card==null)toast("请先选择角色") else pool.execute{
          runCatching{contentResolver.openInputStream(uri)?.use{read(it)}?:""}
            .onSuccess{raw->main.post{runCatching{importWeixinHistory(card,raw,historyChatId)}.onSuccess{count->toast(if(count>0)"已导入 $count 条微信消息" else "没有识别到聊天消息")}.onFailure{error->toast("导入失败：${error.message}")}}}
            .onFailure{error->main.post{toast("导入失败：${error.message}")}}
        }
      }
    }
    DisposableEffect(Unit){
      val receiver=object:android.content.BroadcastReceiver(){override fun onReceive(context:android.content.Context?,intent:Intent?){localRevision++}}
      androidx.core.content.ContextCompat.registerReceiver(this@ComposeActivity,receiver,android.content.IntentFilter(WeixinLinkClient.ACTION_STATE_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
      onDispose{runCatching{unregisterReceiver(receiver)}}
    }
    LaunchedEffect(status,localRevision){
      if(qrUrl.isNotBlank()&&(status in listOf("wait","scaned","scaned_but_redirect")||(status=="need_verifycode"&&verifyPending.isNotBlank()))){
        delay(900);busy=true
        withContext(Dispatchers.IO){runCatching{WeixinLinkClient.pollLogin(this@ComposeActivity)}}
          .onSuccess{localRevision++}
          .onFailure{prefs.edit().putString("weixin_link_status_text","连接失败：${it.message}").apply()}
        busy=false
      }
    }
    fun updateBinding(account:WeixinLinkClient.Account,cardId:String,chatId:String){
      val sharedOld=accounts.any{it.id!=account.id&&it.chatId.isNotBlank()&&it.chatId==account.chatId}
      if(!sharedOld){
        if(account.chatId.isNotBlank())chats.firstOrNull{it.id==account.chatId}?.let{it.externalSource="";it.externalPeer=""}
        else chats.filter{it.externalSource=="weixin"&&it.cardId==account.cardId&&(account.ownerId.isBlank()||it.externalPeer==account.ownerId)}.forEach{it.externalSource="";it.externalPeer=""}
      }
      chats.firstOrNull{it.id==chatId&&it.cardId==cardId&&!it.group}?.let{it.externalSource="weixin";it.externalPeer=account.ownerId}
      save();WeixinLinkClient.setBinding(this@ComposeActivity,account.id,cardId,chatId);localRevision++;uiRevision++
    }
    fun disconnectAccount(account:WeixinLinkClient.Account){
      val shared=accounts.any{it.id!=account.id&&it.chatId.isNotBlank()&&it.chatId==account.chatId}
      if(!shared){
        if(account.chatId.isNotBlank())chats.firstOrNull{it.id==account.chatId}?.let{it.externalSource="";it.externalPeer=""}
        else chats.filter{it.externalSource=="weixin"&&it.cardId==account.cardId&&(account.ownerId.isBlank()||it.externalPeer==account.ownerId)}.forEach{it.externalSource="";it.externalPeer=""}
      }
      save();WeixinLinkClient.disconnect(this@ComposeActivity,account.id);localRevision++;uiRevision++
    }
    DetailScaffold("将聊天角色接入微信"){
      item{SectionTitle("添加角色 Bot")}
      item{
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
          Column(Modifier.fillMaxWidth().padding(15.dp)){
            if(cards.isEmpty())Text("请先创建或导入角色。",color=MaterialTheme.colorScheme.error)
            else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
              cards.forEach{card->
                FilterChip(selected=selectedCardId==card.id,onClick={selectedCardId=card.id;historyCardId=card.id;selectedChatId=""},label={Text(card.name,maxLines=1)},leadingIcon={if(selectedCardId==card.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})
              }
            }
            if(selectedChats.isNotEmpty()){
              Text("选择聊天记录",Modifier.padding(top=10.dp,bottom=6.dp),fontSize=13.sp,fontWeight=FontWeight.Medium)
              Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                selectedChats.forEach{chat->
                  FilterChip(selected=selectedChatId==chat.id,onClick={selectedChatId=chat.id},label={Text(chat.title.ifBlank{"未命名聊天"},maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)},leadingIcon={if(selectedChatId==chat.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})
                }
              }
            }else if(selectedCardId.isNotBlank())Text("这个角色还没有主页聊天记录，微信消息会自动新建一条对话。",Modifier.padding(top=9.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(qrBitmap!=null){
              Text(statusText,Modifier.padding(top=10.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
              Image(qrBitmap.asImageBitmap(),"微信登录二维码",Modifier.align(Alignment.CenterHorizontally).padding(top=10.dp).size(220.dp).clip(RoundedCornerShape(10.dp)))
            }
            if(status=="need_verifycode"){
              OutlinedTextField(verifyCode,{verifyCode=it.filter(Char::isDigit).take(8)},Modifier.fillMaxWidth().padding(top=10.dp),label={Text("微信验证码")},singleLine=true,shape=RoundedCornerShape(12.dp))
              Button(onClick={if(verifyCode.isBlank())toast("请输入验证码")else{prefs.edit().putString("weixin_link_verify_code",verifyCode).apply();localRevision++}},Modifier.fillMaxWidth().padding(top=7.dp)){Text("提交验证码")}
            }
            Button(onClick={if(selectedCardId.isBlank())toast("请先选择角色")else{busy=true;pool.execute{runCatching{WeixinLinkClient.startLogin(this@ComposeActivity,selectedCardId,selectedChatId)}.onSuccess{main.post{busy=false;localRevision++}}.onFailure{error->main.post{busy=false;toast("连接失败：${error.message}")}}}}},enabled=!busy&&cards.isNotEmpty(),modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(46.dp)){
              if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.QrCode2,null)
              Spacer(Modifier.width(7.dp));Text(if(qrUrl.isBlank())"生成连接二维码" else "刷新二维码")
            }
          }
        }
      }
      if(accounts.isNotEmpty())item{SectionTitle("已连接角色")}
      items(accounts,key={it.id}){account->
        val card=cards.firstOrNull{it.id==account.cardId}
        val bindChats=chats.filter{!it.group&&it.cardId==account.cardId}.sortedByDescending{it.updated}
        val bound=bindChats.firstOrNull{it.id==account.chatId}
        val availableCards=cards.filter{candidate->accounts.none{it.id!=account.id&&it.cardId==candidate.id}}
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
          Column(Modifier.fillMaxWidth().padding(13.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
              if(card!=null)RoleAvatar(card,44)else Avatar("角",44)
              Column(Modifier.weight(1f).padding(start=10.dp)){
                Text(card?.name?:"角色已删除",fontWeight=FontWeight.Medium,maxLines=1)
                Text((prefs.getString("weixin_link_status_${account.id}",if(account.enabled)"微信已连接" else "已暂停").orEmpty())+" · "+(bound?.title?.ifBlank{"未命名聊天"}?:"未绑定聊天记录"),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
              }
              Switch(account.enabled,{WeixinLinkClient.setEnabled(this@ComposeActivity,account.id,it);localRevision++})
              IconButton(onClick={askDanger("断开 ${card?.name?:"角色"}","清除这个角色的微信 Bot 连接，App 中已同步的聊天记录会保留。","确认断开"){disconnectAccount(account)}},Modifier.size(38.dp)){Icon(Icons.Outlined.LinkOff,"断开")}
            }
            Text("快速切换角色",Modifier.padding(top=9.dp,bottom=5.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
              availableCards.forEach{candidate->
                FilterChip(selected=account.cardId==candidate.id,onClick={val target=chats.filter{!it.group&&it.cardId==candidate.id}.maxByOrNull{it.updated};updateBinding(account,candidate.id,target?.id.orEmpty())},label={Text(candidate.name,maxLines=1,softWrap=false)},leadingIcon={if(account.cardId==candidate.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})
              }
            }
            if(bindChats.isNotEmpty()){
              Text("快速切换聊天",Modifier.padding(top=9.dp,bottom=5.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
              Row(Modifier.fillMaxWidth().padding(top=9.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                bindChats.forEach{chat->
                  FilterChip(selected=account.chatId==chat.id,onClick={updateBinding(account,account.cardId,chat.id);toast("已切换微信聊天")},label={Text(chat.title.ifBlank{"未命名聊天"},maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)},leadingIcon={if(account.chatId==chat.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})
                }
              }
            }
          }
        }
      }
      item{SectionTitle("聊天记录")}
      item{
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
          Column(Modifier.fillMaxWidth().padding(15.dp)){
            if(cards.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
              cards.forEach{card->FilterChip(selected=historyCardId==card.id,onClick={historyCardId=card.id;historyChatId=""},label={Text(card.name,maxLines=1)})}
            }
            if(historyChats.isNotEmpty()){
              Text("导入到",Modifier.padding(top=10.dp,bottom=6.dp),fontSize=13.sp,fontWeight=FontWeight.Medium)
              Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                historyChats.forEach{chat->FilterChip(selected=historyChatId==chat.id,onClick={historyChatId=chat.id},label={Text(chat.title.ifBlank{"未命名聊天"},maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)},leadingIcon={if(historyChatId==chat.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})}
              }
            }
            OutlinedButton(onClick={if(historyCardId.isBlank())toast("请先选择角色")else historyImporter.launch(arrayOf("application/json","text/plain","*/*"))},enabled=historyCardId.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(44.dp)){
              Icon(Icons.Outlined.UploadFile,null);Spacer(Modifier.width(7.dp));Text("导入现有聊天记录")
            }
          }
        }
      }
    }
  }
  @Composable
  private fun AppearancePage(themeMode: Int, setTheme: (Int) -> Unit,themeStyle:Int,setThemeStyle:(Int)->Unit) {
    var bubbleFont by remember { mutableFloatStateOf(prefs.getInt("chat_font_size",15).toFloat()) }
    var bubbleSpacing by remember { mutableFloatStateOf(prefs.getInt("chat_bubble_spacing",12).toFloat()) }
    var lineHeight by remember { mutableFloatStateOf(prefs.getInt("chat_line_height",22).toFloat()) }
    DetailScaffold("外观与动画") {
      item { InfoBanner(Icons.Outlined.ColorLens, "时遇风格", "珊瑚色表达情感，青绿色强调操作；深浅模式会同步系统状态栏。") }
      item { SectionTitle("主题模式") }
      items(listOf("跟随系统" to "根据手机深浅模式自动切换", "明亮" to "清爽、柔和的浅色界面", "夜间" to "低亮度深色界面").withIndex().toList()) { indexed ->
        val index=indexed.index; val value=indexed.value
        Surface(onClick = { prefs.edit().putInt("compose_theme", index).apply(); setTheme(index) }, shape=RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.surface) {
          Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(themeMode==index)Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.padding(start=12.dp)){Text(value.first,fontWeight=FontWeight.Medium);Text(value.second,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        }
      }
      item { SectionTitle("主题风格") }
      items(listOf("时遇珊瑚" to "温暖珊瑚与青绿色，保持时遇默认气质","青岚" to "清透青绿、蓝灰与柔和冷白","樱雾" to "低饱和樱粉、莓红与中性灰","简墨" to "黑白灰为主，使用少量青色强调").withIndex().toList()){indexed->val index=indexed.index;val value=indexed.value;Surface(onClick={prefs.edit().putInt("compose_theme_style",index).apply();setThemeStyle(index);uiRevision++},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=themePalette(index,false).primary,modifier=Modifier.size(28.dp)){Box{}};Column(Modifier.weight(1f).padding(start=12.dp)){Text(value.first,fontWeight=FontWeight.Medium);Text(value.second,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(if(themeStyle==index)Icons.Filled.CheckCircle else Icons.Outlined.Circle,null,tint=MaterialTheme.colorScheme.primary)}}}
      item { SectionTitle("显示性能") }
      item { SettingButton(Icons.Outlined.Speed,"屏幕刷新率",prefs.getInt("refresh_rate",0).takeIf{it>0}?.let{"优先 ${it}Hz"}?:"跟随系统"){composePage=Page.FRAME_RATE} }
      item { SettingButton(Icons.Outlined.ChatBubbleOutline,"聊天页面样式","背景图片、背景色、气泡颜色、宽度与高度"){composePage=Page.CHAT_APPEARANCE} }
      item { SectionTitle("聊天气泡") }
      item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){Text("字体大小 ${bubbleFont.toInt()} sp",fontWeight=FontWeight.Medium);Slider(bubbleFont,{bubbleFont=it},valueRange=12f..20f,steps=7);Text("气泡间距 ${bubbleSpacing.toInt()} dp",fontWeight=FontWeight.Medium);Slider(bubbleSpacing,{bubbleSpacing=it},valueRange=6f..22f,steps=15);Text("正文行距 ${lineHeight.toInt()} sp",fontWeight=FontWeight.Medium);Slider(lineHeight,{lineHeight=it},valueRange=16f..32f,steps=15);Surface(Modifier.fillMaxWidth().padding(top=8.dp),RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primary){Text("这是聊天气泡预览，调整后会应用到所有对话。",Modifier.padding(14.dp),fontSize=bubbleFont.toInt().sp,lineHeight=lineHeight.toInt().coerceAtLeast(bubbleFont.toInt()+2).sp,color=MaterialTheme.colorScheme.onPrimary)}}} }
      item { Button(onClick={prefs.edit().putInt("chat_font_size",bubbleFont.toInt()).putInt("chat_bubble_spacing",bubbleSpacing.toInt()).putInt("chat_line_height",lineHeight.toInt().coerceAtLeast(bubbleFont.toInt()+2)).apply();uiRevision++;toast("聊天气泡样式已保存")},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存气泡样式",maxLines=1,softWrap=false)} }
    }
  }

  @Composable
  private fun ChatAppearancePage(){
    var background by remember{mutableStateOf(prefs.getString("chat_background_color","").orEmpty())}
    var userBubble by remember{mutableStateOf(prefs.getString("chat_user_bubble_color","").orEmpty())}
    var aiBubble by remember{mutableStateOf(prefs.getString("chat_ai_bubble_color","").orEmpty())}
    var noBubbles by remember{mutableStateOf(prefs.getBoolean("chat_no_bubbles",false))}
    var hideAvatars by remember{mutableStateOf(prefs.getBoolean("chat_hide_avatars",prefs.getBoolean("chat_no_bubbles",false)))}
    var userBubbleOpacity by remember{mutableFloatStateOf(prefs.getInt("chat_user_bubble_opacity",100).toFloat())}
    var aiBubbleOpacity by remember{mutableFloatStateOf(prefs.getInt("chat_ai_bubble_opacity",100).toFloat())}
    var bubbleWidth by remember{mutableFloatStateOf(prefs.getInt("chat_bubble_max_width",292).toFloat())}
    var bubblePadding by remember{mutableFloatStateOf(prefs.getInt("chat_bubble_vertical_padding",10).toFloat())}
    var inputHeight by remember{mutableFloatStateOf(prefs.getInt("chat_input_min_height",42).toFloat())}
    var suggestionPosition by remember{mutableStateOf(prefs.getString("reply_suggestion_position","bubble")?:"bubble")}
    var overlay by remember{mutableFloatStateOf(prefs.getInt("chat_background_overlay",20).toFloat())}
    var imagePath by remember{mutableStateOf(prefs.getString("chat_background_image","").orEmpty())}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{val target=File(filesDir,"chat_background.img");contentResolver.openInputStream(uri)?.use{input->FileOutputStream(target).use{output->input.copyTo(output)}};prefs.edit().putString("chat_background_image",target.absolutePath).apply();imagePath=target.absolutePath;bitmapCache.remove(target.absolutePath)}.onFailure{toast("背景图片保存失败：${it.message}")}}}
    val palette=listOf("" to "跟随主题","#FFF7F8F4" to "雾白","#FFF1EDEF" to "浅粉","#FFE8F1EF" to "青灰","#FF1A1A1C" to "灰黑","#FF26302E" to "墨绿","#FF30272A" to "暗红")
    val bubblePalette=listOf("" to "跟随主题","#FFC85C6F" to "珊瑚","#FF2F8073" to "青绿","#FF394A5A" to "蓝灰","#FFF2E6E9" to "浅粉","#FFE4ECEA" to "浅青","#FF252527" to "深灰")
    DetailScaffold("聊天页面样式",Page.APPEARANCE){
      item{InfoBanner(Icons.Outlined.FormatPaint,"每处细节都可调整","背景图仅保存在本机；颜色和尺寸应用到全部对话。清除自定义值后自动跟随当前主题。")}
      item{SectionTitle("聊天背景")}
      item{ColorSwatchSelector("背景颜色",background,palette){background=it}}
      item{ColorHexInput("自定义背景颜色",background){background=it}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={picker.launch(arrayOf("image/*"))},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Wallpaper,null);Spacer(Modifier.width(6.dp));Text(if(imagePath.isBlank())"选择背景图" else "更换背景图",maxLines=1,softWrap=false)};OutlinedButton(onClick={imagePath.takeIf{it.isNotBlank()}?.let{runCatching{File(it).delete()}};imagePath="";prefs.edit().remove("chat_background_image").apply()},Modifier.weight(1f).height(46.dp),enabled=imagePath.isNotBlank()){Icon(Icons.Outlined.HideImage,null);Spacer(Modifier.width(6.dp));Text("清除背景图",maxLines=1,softWrap=false)}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("背景遮罩 ${overlay.toInt()}%",fontWeight=FontWeight.Medium);Text("提高文字和气泡在图片上的可读性",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(overlay,{overlay=it},valueRange=0f..70f,steps=13)}}}
      item{SectionTitle("消息布局")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("无气泡模式",fontWeight=FontWeight.Medium);Text("聊天只显示文字和媒体，适合搭配背景图",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(noBubbles,{noBubbles=it})}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("隐藏消息头像",fontWeight=FontWeight.Medium);Text("只隐藏聊天记录两侧头像，不影响顶部角色头像",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(hideAvatars,{hideAvatars=it})}}}
      item{SectionTitle("推荐回复位置")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=suggestionPosition=="bubble",onClick={suggestionPosition="bubble"},label={Text("角色气泡下方",maxLines=1,softWrap=false)},leadingIcon={Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(16.dp))},modifier=Modifier.weight(1f));FilterChip(selected=suggestionPosition=="input",onClick={suggestionPosition="input"},label={Text("输入框上方",maxLines=1,softWrap=false)},leadingIcon={Icon(Icons.Outlined.Keyboard,null,Modifier.size(16.dp))},modifier=Modifier.weight(1f))}}
      item{SectionTitle("气泡颜色")}
      item{ColorSwatchSelector("我的气泡",userBubble,bubblePalette){userBubble=it}}
      item{ColorHexInput("自定义我的气泡颜色",userBubble){userBubble=it}}
      item{ColorSwatchSelector("角色气泡",aiBubble,bubblePalette){aiBubble=it}}
      item{ColorHexInput("自定义角色气泡颜色",aiBubble){aiBubble=it}}
      if(!noBubbles)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("我的气泡透明度 ${userBubbleOpacity.toInt()}%",fontWeight=FontWeight.Medium);Slider(userBubbleOpacity,{userBubbleOpacity=it},valueRange=10f..100f,steps=17);Text("角色气泡透明度 ${aiBubbleOpacity.toInt()}%",fontWeight=FontWeight.Medium);Slider(aiBubbleOpacity,{aiBubbleOpacity=it},valueRange=10f..100f,steps=17)}}}
      item{SectionTitle("尺寸")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("气泡最大宽度 ${bubbleWidth.toInt()} dp",fontWeight=FontWeight.Medium);Slider(bubbleWidth,{bubbleWidth=it},valueRange=240f..360f,steps=11);Text("气泡上下内边距 ${bubblePadding.toInt()} dp",fontWeight=FontWeight.Medium);Slider(bubblePadding,{bubblePadding=it},valueRange=6f..20f,steps=13);Text("输入框最小高度 ${inputHeight.toInt()} dp",fontWeight=FontWeight.Medium);Slider(inputHeight,{inputHeight=it},valueRange=40f..64f,steps=5)}}}
      item{Button(onClick={val normalizedBackground=normalizeStoredColor(background);val normalizedUser=normalizeStoredColor(userBubble);val normalizedAi=normalizeStoredColor(aiBubble);if(normalizedBackground==null||normalizedUser==null||normalizedAi==null)toast("颜色格式无效，请使用 6 位 RGB 或 8 位 ARGB")else{prefs.edit().putString("chat_background_color",normalizedBackground).putString("chat_user_bubble_color",normalizedUser).putString("chat_ai_bubble_color",normalizedAi).putBoolean("chat_no_bubbles",noBubbles).putBoolean("chat_hide_avatars",hideAvatars).putString("reply_suggestion_position",suggestionPosition).putInt("chat_user_bubble_opacity",userBubbleOpacity.toInt()).putInt("chat_ai_bubble_opacity",aiBubbleOpacity.toInt()).putInt("chat_bubble_max_width",bubbleWidth.toInt()).putInt("chat_bubble_vertical_padding",bubblePadding.toInt()).putInt("chat_input_min_height",inputHeight.toInt()).putInt("chat_background_overlay",overlay.toInt()).apply();background=normalizedBackground;userBubble=normalizedUser;aiBubble=normalizedAi;uiRevision++;toast("聊天页面样式已保存");composePage=Page.APPEARANCE}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存聊天页面样式",maxLines=1,softWrap=false)}}
    }
  }

  @Composable
  private fun FrameRatePage(){
    val supported=remember{if(android.os.Build.VERSION.SDK_INT>=23){val display=if(android.os.Build.VERSION.SDK_INT>=30)this@ComposeActivity.display else windowManager.defaultDisplay;display?.supportedModes?.map{it.refreshRate.toInt()}?.distinct()?.sorted().orEmpty()}else listOf(60)}
    var selected by remember{mutableIntStateOf(prefs.getInt("refresh_rate",0))}
    val options=listOf(0,60,90,120,144,165)
    DetailScaffold("屏幕刷新率",Page.APPEARANCE){
      item{InfoBanner(Icons.Outlined.Speed,"优先匹配设备显示模式","时遇会选择最接近目标帧率的系统显示模式；最终刷新率仍由屏幕、系统和省电策略决定。")}
      items(options){rate->val available=rate==0||supported.any{kotlin.math.abs(it-rate)<=1};Surface(onClick={selected=rate;prefs.edit().putInt("refresh_rate",rate).apply();applyPreferredRefreshRate();toast(if(rate==0)"已跟随系统刷新率" else "已请求 ${rate}Hz")},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(selected==rate)Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=12.dp)){Text(if(rate==0)"跟随系统" else "${rate}Hz",fontWeight=FontWeight.Medium,maxLines=1,softWrap=false);Text(if(rate==0)"由 Android 自动选择" else if(available)"设备已报告支持" else "设备未报告，将匹配最接近档位",fontSize=12.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
      item{Text("设备显示模式：${supported.joinToString(" / "){"${it}Hz"}.ifBlank{"未知"}}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(12.dp))}
    }
  }

  @Composable
  private fun DataPage() {
    val promptTokens=chats.sumOf{it.promptTokens};val completionTokens=chats.sumOf{it.completionTokens};val messageCount=chats.sumOf{it.messages.size};val memoryCount=cards.sumOf{db.memories(it.id).size}
    DetailScaffold("本地数据") {
      item { InfoBanner(Icons.Outlined.Security, "数据留在本机", "API Key 使用 Android Keystore 加密；角色、记忆与聊天存储在 SQLite 数据库。") }
      item { StatCard("角色卡", cards.size.toString(), Icons.Outlined.Style) }
      item { StatCard("对话", chats.size.toString(), Icons.Outlined.Forum) }
      item { StatCard("累计 Token",(promptTokens+completionTokens).toString(),Icons.Outlined.DataUsage) }
      item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){TokenStatCard("输入 Token",promptTokens,Icons.Outlined.ArrowUpward)};Box(Modifier.weight(1f)){TokenStatCard("输出 Token",completionTokens,Icons.Outlined.ArrowDownward)}} }
      item { StatCard("消息数量",messageCount.toString(),Icons.Outlined.ChatBubbleOutline) }
      item { StatCard("长期记忆",memoryCount.toString(),Icons.Outlined.Psychology) }
      item { SettingButton(Icons.Outlined.Backup,"备份与恢复","导出或恢复角色、聊天、长期记忆、图片和全部设置"){composePage=Page.BACKUP} }
      item { OutlinedButton(onClick = { askDanger("清除全部聊天记录","所有单聊、群聊、摘要和 Token 统计都会被永久删除，角色卡不会删除。","确认清除"){chats.clear();save();YandereMessageService.sync(this@ComposeActivity);uiRevision++;toast("聊天记录已清除")} }, Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(8.dp));Text("清除全部聊天记录") } }
    }
  }

  @Composable
  private fun BackupPage() {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmOverwrite by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
      if(uri!=null){running=true;pool.execute{runCatching{BackupManager.create(this@ComposeActivity,db,prefs,secure,password)}.onSuccess{data->runCatching{contentResolver.openOutputStream(uri,"w")!!.use{it.write(data)}}.onSuccess{main.post{running=false;toast("完整备份已创建")}}.onFailure{error->main.post{running=false;toast("备份写入失败：${error.message}")}}}.onFailure{error->main.post{running=false;toast("创建备份失败：${error.message}")}}}}
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
      if(uri!=null){running=true;pool.execute{runCatching{bytes(contentResolver.openInputStream(uri))}.mapCatching{data->BackupManager.restore(this@ComposeActivity,db,prefs,secure,data,password)}.onSuccess{main.post{running=false;cards.clear();chats.clear();load();selectedChat=null;ActiveReplyWorker.sync(this@ComposeActivity);YandereMessageService.sync(this@ComposeActivity);MomentAutoPostWorker.sync(this@ComposeActivity);toast("备份恢复完成，正在重新载入");recreate()}}.onFailure{error->main.post{running=false;toast("恢复失败：${error.message}")}}}}
    }
    DetailScaffold("备份与恢复",Page.DATA){
      item{InfoBanner(Icons.Outlined.Inventory,"完整本地备份","备份包含角色卡、聊天、长期记忆、角色图片、聊天图片、用户资料、模型供应商、API Key 与全部应用设置。")}
      item{OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("备份加密密码")},supportingText={Text("创建备份至少 6 位；使用 AES-256-GCM 加密全部聊天、设置和 API Key")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}
      item{Button(onClick={if(password.length<6)toast("请设置至少 6 位备份密码")else{val stamp=java.text.SimpleDateFormat("yyyyMMdd-HHmm",java.util.Locale.getDefault()).format(java.util.Date());export.launch("shiyu-backup-$stamp.sybak")}},enabled=!running,modifier=Modifier.fillMaxWidth().height(50.dp)){if(running)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Backup,null);Spacer(Modifier.width(8.dp));Text(if(running)"正在处理" else "创建加密备份",maxLines=1,softWrap=false)}}
      item{SectionTitle("从备份恢复")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.errorContainer){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("我已了解恢复会覆盖本地数据",fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.onErrorContainer);Text("导入前建议先创建当前数据的备份",fontSize=12.sp,color=MaterialTheme.colorScheme.onErrorContainer)};Switch(checked=confirmOverwrite,onCheckedChange={confirmOverwrite=it})}}}
      item{OutlinedButton(onClick={restore.launch(arrayOf("application/json","application/octet-stream","*/*"))},enabled=confirmOverwrite&&!running,modifier=Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Restore,null);Spacer(Modifier.width(8.dp));Text("选择备份并恢复",maxLines=1,softWrap=false)}}
      item{GuideStep("1","创建备份","设置至少 6 位密码后导出，聊天、角色、API Key 和本地资源会统一加密。")} 
      item{GuideStep("2","妥善保存","备份文件只保存在你选择的位置，时遇服务器不会接收备份内容。")}
      item{GuideStep("3","恢复数据","恢复会覆盖当前设备内的角色、聊天、记忆与设置，完成后自动重新载入。")}
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun StickerResourcesPage() {
    uiRevision
    var downloading by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(0) }
    var progressText by remember { mutableStateOf("") }
    var aiEnabled by remember { mutableStateOf(prefs.getBoolean("stickers_ai_enabled", false)) }
    val all = remember(uiRevision) { StickerRepository.all(this) }
    val favorites = remember(uiRevision) { StickerRepository.favorites(prefs) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      if (uris.isNotEmpty()) pool.execute {
        val count = StickerRepository.import(this@ComposeActivity, prefs, uris)
        main.post { uiRevision++;toast(if(count>0)"已收藏 $count 张表情" else "没有可导入的图片") }
      }
    }
    DetailScaffold("表情资源", stickerResourcesBackPage) {
      item { InfoBanner(Icons.Outlined.EmojiEmotions,"本地表情库","下载或导入的表情只保存在本机。聊天中长按表情可收藏；角色使用表情需单独授权。") }
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色使用表情",fontWeight=FontWeight.Medium);Text("角色可根据情绪从本地资源中选择表情",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(aiEnabled,{aiEnabled=it;prefs.edit().putBoolean("stickers_ai_enabled",it).apply()})}} }
      item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){OutlinedButton(onClick={importer.launch(arrayOf("image/*"))},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.AddPhotoAlternate,null);Spacer(Modifier.width(6.dp));Text("导入本地表情",maxLines=1,softWrap=false)};FilledTonalButton(onClick={},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Favorite,null);Spacer(Modifier.width(6.dp));Text("已收藏 ${favorites.size}",maxLines=1,softWrap=false)}} }
      if(all.isNotEmpty()) item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(12.dp)){Text("资源预览 · 共 ${all.size} 张",fontSize=13.sp,fontWeight=FontWeight.Medium);Row(Modifier.fillMaxWidth().padding(top=10.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){all.take(18).forEach{path->MessageImage(path,Modifier.size(66.dp).clip(RoundedCornerShape(10.dp)).combinedClickable(onClick={},onLongClick={val added=StickerRepository.toggleFavorite(this@ComposeActivity,prefs,path);uiRevision++;toast(if(added)"已收藏" else "已取消收藏")}),ContentScale.Fit)}}}} }
      item { SectionTitle("在线资源包") }
      StickerRepository.packs.forEach { pack ->
        item(key=pack.id) {
          val installed=StickerRepository.isInstalled(this@ComposeActivity,pack.id)
          val count=StickerRepository.count(this@ComposeActivity,pack.id)
          Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Collections,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(start=10.dp)){Text(pack.name,fontWeight=FontWeight.SemiBold);Text(if(installed)"已安装 $count 张" else pack.estimatedSize,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            Text(pack.description,Modifier.padding(top=9.dp),fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(pack.sourceUrl)))},contentPadding=PaddingValues(0.dp)){Icon(Icons.Outlined.OpenInNew,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("查看项目来源",fontSize=12.sp)}
            if(downloading==pack.id){LinearProgressIndicator(progress={progress/100f},Modifier.fillMaxWidth().padding(vertical=8.dp));Text(progressText,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            if(installed){val packImages=remember(uiRevision,pack.id){StickerRepository.imagesForPack(this@ComposeActivity,pack.id)};Row(Modifier.fillMaxWidth().padding(vertical=9.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){packImages.take(24).forEach{path->MessageImage(path,Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)).combinedClickable(onClick={},onLongClick={val added=StickerRepository.toggleFavorite(this@ComposeActivity,prefs,path);uiRevision++;toast(if(added)"已收藏" else "已取消收藏")}),ContentScale.Fit)}}}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
              if(installed)OutlinedButton(onClick={askDanger("删除 ${pack.name}","将删除本机下载的 $count 张资源，聊天中已经发送的图片不会被删除。","确认删除"){StickerRepository.deletePack(this@ComposeActivity,pack.id);bitmapCache.clear();uiRevision++;toast("资源包已删除")}},Modifier.weight(1f).height(43.dp)){Icon(Icons.Outlined.Delete,null);Spacer(Modifier.width(5.dp));Text("删除",maxLines=1)}
              Button(onClick={if(downloading.isBlank()){downloading=pack.id;progress=1;progressText="准备下载";Thread({runCatching{StickerRepository.download(this@ComposeActivity,pack.id){value,label->main.post{progress=value;progressText=label}}}.onSuccess{main.post{downloading="";bitmapCache.clear();uiRevision++;toast("${pack.name} 已安装")}}.onFailure{error->main.post{downloading="";toast("下载失败：${error.message}")}}},"shiyu-sticker-download").start()}else toast("${StickerRepository.packs.firstOrNull{it.id==downloading}?.name?:"资源包"}正在下载，请稍候")},Modifier.weight(1f).height(43.dp),enabled=downloading!=pack.id){Icon(if(installed)Icons.Outlined.Refresh else Icons.Outlined.Download,null);Spacer(Modifier.width(5.dp));Text(if(downloading==pack.id)"下载中" else if(installed)"重新下载" else "下载",maxLines=1)}
            }
      }}
        }
      }
      item { InfoBanner(Icons.Outlined.Copyright,"第三方资源说明","资源由对应 GitHub 项目提供，版权和使用规则归原作者及素材权利人所有。时遇不售卖、不内置转载这些资源。") }
    }
  }

  @Composable
  private fun RealityAwarenessPage() {
    var enabled by remember { mutableStateOf(prefs.getBoolean("reality_awareness_enabled", true)) }
    var includeTimezone by remember { mutableStateOf(prefs.getBoolean("reality_include_timezone", true)) }
    val preview=remember{java.text.SimpleDateFormat("yyyy年M月d日 EEEE HH:mm",java.util.Locale.CHINA).format(java.util.Date())}
    DetailScaffold("现实感知增强") {
      item { InfoBanner(Icons.Outlined.Public,"只提供当前时间信息","开启后，角色会在每次回复时获得当前日期、星期、时间段和可选时区，并在情境需要时自然使用。") }
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用现实感知",fontWeight=FontWeight.Medium);Text("例如知道今天日期、现在是早晨还是夜晚",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(enabled,{enabled=it;prefs.edit().putBoolean("reality_awareness_enabled",it).apply()})}} }
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("包含设备时区",fontWeight=FontWeight.Medium);Text(java.util.TimeZone.getDefault().id,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(includeTimezone,{includeTimezone=it;prefs.edit().putBoolean("reality_include_timezone",it).apply()},enabled=enabled)}} }
      item { GuideStep("当前","时间预览",preview) }
      item { InfoBanner(Icons.Outlined.PrivacyTip,"隐私边界","此功能不会读取定位、天气、日历、联系人或其他 App 数据；角色也会被要求不要假装知道这些信息。") }
    }
  }

  @Composable
  private fun AboutPage() { var checking by remember{mutableStateOf(false)};val updateVersion=prefs.getString("update_version","")?:"";val updateUrl=prefs.getString("update_url","")?:"";DetailScaffold("关于时遇") { item { Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Image(painterResource(R.drawable.ic_launcher_foreground),"时遇图标",Modifier.size(82.dp).clip(RoundedCornerShape(18.dp)));Spacer(Modifier.height(14.dp));Text("时遇",fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer);Text("在故事里，遇见彼此",fontSize=13.sp,color=MaterialTheme.colorScheme.onPrimaryContainer)}}};item{SectionTitle("应用信息")};item{SettingButton(Icons.Outlined.Info,"当前版本","${BuildConfig.VERSION_NAME} · Android Compose"){} };item{SettingButton(Icons.Outlined.Language,"时遇官网","shiy.tniay.top"){startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://shiy.tniay.top/")))}};item{Button(onClick={checking=true;checkForUpdates{checking=false}},Modifier.fillMaxWidth().height(48.dp)){if(checking)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.SystemUpdateAlt,null);Spacer(Modifier.width(8.dp));Text(if(checking)"正在检查更新" else "检查更新",maxLines=1,softWrap=false)}};if(updateVersion.isNotBlank())item{SettingButton(Icons.Outlined.NewReleases,"发现新版本 $updateVersion",updateUrl.ifBlank{"时遇官网"}){startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(updateUrl)))}};item{SettingButton(Icons.Outlined.Gavel,"用户协议与使用条款","免费 App、第三方 API、隐私与内容规范"){termsBackPage=Page.ABOUT;composePage=Page.TERMS}};item{SettingButton(Icons.Outlined.Groups,"时遇交流群","1061590432 · 点击复制"){copyText("交流群号","1061590432")}};item{SettingButton(Icons.Outlined.AccountCircle,"作者 QQ","3581378884 · 点击复制"){copyText("作者 QQ","3581378884")}};item{GuideStep("隐私","本地优先","聊天内容不会上传到时遇服务器，仅发送给你配置的 API。")} ;item{GuideStep("开放","兼容接口","支持 OpenAI Chat Completions、模型列表和流式输出。")} ;item{GuideStep("设计","时遇风格","珊瑚色代表情感，青绿色代表连接，灰黑夜间模式降低视觉压力。")} } }

  @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
  @Composable
  private fun ChatPage(chat: Chat) {
    val card = find(chat.activeCardId.takeIf { it.isNotBlank() } ?: chat.cardId) ?: return
    val thisSending=sending&&sendingChatId==chat.id
    var text by remember { mutableStateOf("") }
    var replySuggestions by remember(chat.id){mutableStateOf<List<String>>(emptyList())}
    var suggestionsLoading by remember(chat.id){mutableStateOf(false)}
    var webSearchNext by rememberSaveable(chat.id){mutableStateOf(false)}
    var pendingImage by rememberSaveable(chat.id) { mutableStateOf("") }
    var pendingSticker by rememberSaveable(chat.id) { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showRedPacket by remember { mutableStateOf(false) }
    var redAmount by remember { mutableStateOf("") }
    var redGreeting by remember { mutableStateOf("恭喜发财，万事如意") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var menuMessage by remember { mutableStateOf<Msg?>(null) }
    var editMessage by remember { mutableStateOf<Msg?>(null) }
    var errorMessage by remember { mutableStateOf<Msg?>(null) }
    var editText by remember { mutableStateOf("") }
    var smoothScrollRequest by remember(chat.id) { mutableIntStateOf(0) }
    var followStreamingAtBottom by remember(chat.id) { mutableStateOf(true) }
    val clipboard=LocalClipboardManager.current
    val chatBackground=storedColor(prefs.getString("chat_background_color","").orEmpty(),MaterialTheme.colorScheme.background)
    val chatBackgroundImage=prefs.getString("chat_background_image","").orEmpty()
    val chatBackgroundOverlay=prefs.getInt("chat_background_overlay",20).coerceIn(0,70)/100f
    val transparentTop=prefs.getBoolean("experimental_top_bar_transparent",false)
    val topBarDarkContent=remember(chatBackground,chatBackgroundImage,chatBackgroundOverlay){chatStatusBarDarkIcons(chatBackground,chatBackgroundImage,chatBackgroundOverlay)}
    val topBarContentColor=if(topBarDarkContent)Color(0xFF252326)else Color.White
    val glassInput=prefs.getBoolean("experimental_input_glass",false)
    val suggestionPosition=prefs.getString("reply_suggestion_position","bubble")?:"bubble"
    val visibleMessages=chat.messages.filterNot{chat.immersiveMode&&!chat.group&&it.loading}
    val suggestionAnchorId=visibleMessages.lastOrNull{it.role=="assistant"&&!it.loading&&!it.failed&&(it.body.isNotBlank()||it.image.isNotBlank())}?.id
    val listState = rememberLazyListState(initialFirstVisibleItemIndex=visibleMessages.lastIndex.coerceAtLeast(0))
    DisposableEffect(chat.id){prefs.edit().putString("visible_chat_id",chat.id).apply();onDispose{if(prefs.getString("visible_chat_id","")==chat.id)prefs.edit().remove("visible_chat_id").apply();if(voiceRecording)cancelOnlineVoiceRecording();if(offlineVoiceBusy())cancelOfflineVoiceRecognition()}}
    val userDragging by listState.interactionSource.collectIsDraggedAsState()
    var navigationSettled by remember(chat.id){mutableStateOf(false)}
    val lastMessageLength = chat.messages.lastOrNull()?.body?.length ?: 0
    val latestConversationKey=chat.messages.lastOrNull()?.let{"${it.id}:${it.body.length}:${it.loading}"}.orEmpty()
    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};pendingImage=uri.toString();pendingSticker=false}}
    val voiceInput=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->if(result.resultCode==Activity.RESULT_OK){val words=result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);words?.firstOrNull()?.let{text=if(text.isBlank())it else "$text $it"}}}
    fun appendRecognized(value:String){if(value.isNotBlank())text=if(text.isBlank())value.trim() else "$text ${value.trim()}"}
    fun startOfflineInput(){val started=startOfflineVoiceRecognition(object:OfflineSpeechCallback{override fun onStateChanged(){uiRevision++};override fun onResult(value:String){appendRecognized(value);uiRevision++};override fun onError(error:String){uiRevision++;toast("语音识别失败：$error")}});if(!started)toast("无法启动本地语音识别")}
    val audioPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted){if(prefs.getBoolean("stt_local_first",true)&&OfflineSpeechRepository.isInstalled(this@ComposeActivity))startOfflineInput()else if(startOnlineVoiceRecording()){uiRevision++;toast("正在录音，再次点击麦克风完成识别")}}else toast("需要麦克风权限才能使用语音识别")}
    fun startOnlineInput(){if(!prefs.getBoolean("stt_online_fallback",true)){toast("请在语音设置中开启在线识别回退");return};if(secure.getNamed("stt_api_key_encrypted").isBlank()&&key().isBlank()){toast("请先配置语音识别 API Key");return};if(android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED){if(startOnlineVoiceRecording())uiRevision++}else audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)}
    fun handleVoiceInput(){if(offlineVoiceBusy()){if(!offlineVoiceLoading)finishOfflineVoiceRecognition();return};if(voiceRecording){finishOnlineVoiceRecording{appendRecognized(it);uiRevision++};uiRevision++;return};val localFirst=prefs.getBoolean("stt_local_first",true);if(localFirst&&OfflineSpeechRepository.isInstalled(this@ComposeActivity)){if(android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)startOfflineInput()else audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)}else if(localFirst&&android.speech.SpeechRecognizer.isRecognitionAvailable(this@ComposeActivity)){runCatching{voiceInput.launch(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,"zh-CN").putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,1).putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE,true).putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"说出要发送的内容"))}.onFailure{startOnlineInput()}}else startOnlineInput()}
    fun loadReplySuggestions(){if(suggestionsLoading)return;if(sending){toast("请等待当前回复生成完成");return};if(activeProvider()==null||key().isBlank()){toast("请先配置模型供应商");return};suggestionsLoading=true;val taskId=GenerationTaskTracker.start(this@ComposeActivity,"suggestion","正在生成推荐回复",chat.title,chat.id,false);pool.execute{runCatching{generateReplySuggestions(chat,card)}.onSuccess{values->GenerationTaskTracker.finish(this@ComposeActivity,taskId,"已生成 ${values.size} 条建议");main.post{replySuggestions=values;suggestionsLoading=false}}.onFailure{error->GenerationTaskTracker.fail(this@ComposeActivity,taskId,error);main.post{suggestionsLoading=false;lastApiError=errorDetails(error);toast("推荐回复生成失败：${error.message?:"未知错误"}")}}}}
    fun sendMessage(body:String,image:String="",type:String="text",meta:String=""):Boolean{
      if(body.isBlank()&&image.isBlank())return false
      if(activeProvider()==null){toast("请先在模型设置中添加供应商");return false}
      val otherChatSending=sending&&sendingChatId!=chat.id
      val immersive=chat.immersiveMode&&!chat.group
      val wasSending=sending&&sendingChatId==chat.id
      if(immersive&&wasSending)restartImmersiveGeneration(chat)
      val messageBody=body.trim()
      val members=if(chat.group)chat.cardIds.mapNotNull{find(it)}else emptyList()
      val mentioned=members.filter{member->messageBody.contains("@${member.name}")}
      val target=mentioned.firstOrNull()?:if(chat.group&&chat.groupAutonomous)members.randomOrNull()?:card else card
      if(mentioned.isNotEmpty()||chat.group){chat.activeCardId=target.id;chat.cardId=target.id}
      val message=Msg("user",messageBody);message.messageType=type;message.meta=meta;message.image=image
      chat.messages.add(message);chat.updated=System.currentTimeMillis();chat.proactiveSequence=0;replySuggestions=emptyList();save()
      followStreamingAtBottom=false;if(chat.autoScrollAfterSend)smoothScrollRequest++
      when{
        otherChatSending->{if(mentioned.size>1)queueCrossChatMentions(chat,mentioned)else queueCrossChatReply(chat,target);toast("消息已发送，将在当前回复完成后生成");uiRevision++}
        immersive->{scheduleImmersiveReply(chat.id,target.id);uiRevision++}
        wasSending->{if(mentioned.size>1)queueMentionedReplies(chat,mentioned)else queueReply(chat,target);toast("消息已加入下一轮回复");uiRevision++}
        mentioned.size>1->requestMentioned(chat,mentioned)
        else->request(chat,target)
      }
      return true
    }
    fun patRole(target:Card){
      val user=prefs.getString("user_name","我").orEmpty().ifBlank{"我"}
      sendMessage("$user 拍了拍 ${target.name} 的 ${target.patSelfText.ifBlank{"肩膀"}}","","pat",target.id)
    }
    fun sendCurrent(){val type=if(pendingSticker)"sticker" else if(webSearchNext)"web_search" else "text";if(sendMessage(text,pendingImage,type)){text="";pendingImage="";pendingSticker=false;webSearchNext=false}}
    fun generateConfirmedImage(msg:Msg){if(msg.loading)return;val prompt=msg.meta.trim();if(prompt.isBlank()){toast("没有可用的生图提示词");return};msg.loading=true;msg.failed=false;msg.errorDetail="";uiRevision++;pool.execute{runCatching{generateImage(prompt)}.onSuccess{path->main.post{msg.image=path;msg.messageType="text";msg.body="分享了一张刚刚生成的图片。";msg.meta="";msg.loading=false;msg.failed=false;msg.errorDetail="";save();uiRevision++;toast("图片已生成")}}.onFailure{error->main.post{msg.loading=false;msg.failed=true;msg.errorDetail=errorDetails(error);lastApiError=msg.errorDetail;save();uiRevision++;toast("生图失败：${error.message?:error}")}}}}
    fun claimRedPacket(msg:Msg){val data=runCatching{JSONObject(msg.meta)}.getOrElse{JSONObject()};if(data.optBoolean("claimed",false)){toast("红包已经领取");return};val amount=data.optString("amount","0.00");val sender=find(msg.speakerCardId)?:card;if(sendMessage("你领取了 ${sender.name} 的 ¥$amount 红包","","red_packet_receipt",msg.id)){data.put("claimed",true).put("claimedAt",System.currentTimeMillis());msg.meta=data.toString();save();uiRevision++}}
    LaunchedEffect(chat.id){delay(210);navigationSettled=true;if(visibleMessages.isNotEmpty())listState.scrollToItem(visibleMessages.lastIndex)}
    LaunchedEffect(smoothScrollRequest){if(smoothScrollRequest>0&&navigationSettled){delay(260);if(visibleMessages.isNotEmpty())listState.animateScrollToItem(visibleMessages.lastIndex);followStreamingAtBottom=true}}
    LaunchedEffect(userDragging){if(userDragging)followStreamingAtBottom=false else{delay(90);if(!listState.canScrollForward)followStreamingAtBottom=true}}
    LaunchedEffect(latestConversationKey){if(!suggestionsLoading)replySuggestions=emptyList()}
    LaunchedEffect(lastMessageLength){if(navigationSettled&&thisSending&&followStreamingAtBottom&&visibleMessages.isNotEmpty()){delay(35);if(followStreamingAtBottom&&!userDragging)listState.scrollToItem(visibleMessages.lastIndex,Int.MAX_VALUE)}}
    Box(Modifier.fillMaxSize().background(chatBackground)){
    if(chatBackgroundImage.isNotBlank())MomentImage(chatBackgroundImage,Modifier.fillMaxSize())
    if(chatBackgroundImage.isNotBlank())Box(Modifier.fillMaxSize().background(chatBackground.copy(alpha=chatBackgroundOverlay)))
    Scaffold(containerColor = Color.Transparent,
      topBar = {
        Surface(color=if(transparentTop)Color.Transparent else MaterialTheme.colorScheme.background.copy(alpha=if(glassInput).92f else 1f),contentColor=if(transparentTop)topBarContentColor else MaterialTheme.colorScheme.onBackground) { Column(Modifier.then(if(transparentTop)Modifier.statusBarsPadding()else Modifier)) {
          Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={selectedChat=null;composePage=Page.MAIN}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")};Box(Modifier.combinedClickable(onClick={},onDoubleClick={patRole(card)})){RoleAvatar(card,36)};Column(Modifier.weight(1f).padding(start=9.dp),verticalArrangement=Arrangement.spacedBy(0.dp)){Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){Text(if(thisSending&&chat.immersiveMode&&!chat.group)"正在输入中....." else if(chat.group)chat.title else card.name,fontSize=16.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1,softWrap=false)};Text(activeProvider()?.let{"${it.name} · ${model()}"} ?: "暂未配置模型供应商",fontSize=10.sp,lineHeight=12.sp,color=if(transparentTop)topBarContentColor.copy(alpha=.72f) else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};IconButton(onClick={composePage=Page.CHAT_DETAIL}){Icon(Icons.Outlined.MoreHoriz,"更多")}
          }
          if(chat.group){Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=58.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){chat.cardIds.mapNotNull{find(it)}.forEach{member->val token="@${member.name}";val selected=text.contains(token);FilterChip(selected=selected,onClick={if(selected){text=text.replace(token,"").replace(Regex(" {2,}")," ").trimStart()}else{text=if(text.isBlank())"$token " else "$text $token "};uiRevision++},label={Text(member.name,maxLines=1,softWrap=false)})}}}
        }}
      },
      bottomBar = {
        Surface(color=Color.Transparent,modifier=Modifier.then(if(transparentTop)Modifier.navigationBarsPadding()else Modifier)){Column(Modifier.fillMaxWidth().padding(horizontal=if(glassInput)8.dp else 0.dp,vertical=if(glassInput)7.dp else 0.dp).then(if(glassInput)Modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=.86f)).border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.42f),RoundedCornerShape(22.dp)).padding(horizontal=10.dp,vertical=7.dp)else Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal=10.dp,vertical=6.dp))){
          if(suggestionPosition=="input")Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom=5.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
            if(suggestionsLoading)SuggestionChip(onClick={},enabled=false,label={Text("正在生成推荐回复…",maxLines=1)},icon={CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp)})
            else if(replySuggestions.isEmpty())SuggestionChip(onClick={loadReplySuggestions()},label={Text("推荐回复",maxLines=1)},icon={Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(16.dp))})
            else{IconButton(onClick={loadReplySuggestions()},modifier=Modifier.size(34.dp)){Icon(Icons.Outlined.Refresh,"重新生成推荐回复",Modifier.size(18.dp))};replySuggestions.forEach{suggestion->SuggestionChip(onClick={text=suggestion},label={Text(suggestion,maxLines=1,overflow=TextOverflow.Ellipsis)})}}
          }
          if(webSearchNext)AssistChip(onClick={webSearchNext=false},label={Text("下一条联网搜索",maxLines=1)},leadingIcon={Icon(Icons.Outlined.TravelExplore,null,Modifier.size(16.dp))},trailingIcon={Icon(Icons.Outlined.Close,"取消联网",Modifier.size(15.dp))},modifier=Modifier.padding(bottom=5.dp))
          if(pendingImage.isNotBlank())Row(Modifier.fillMaxWidth().padding(bottom=5.dp),verticalAlignment=Alignment.CenterVertically){MessageImage(pendingImage,Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),if(pendingSticker)ContentScale.Fit else ContentScale.Crop);Text(if(pendingSticker)"表情可与文字一起发送" else "图片将与文字一起发送",Modifier.weight(1f).padding(start=9.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);IconButton(onClick={pendingImage="";pendingSticker=false},Modifier.size(36.dp)){Icon(Icons.Outlined.Close,"移除图片")}}
          if(text.length>=3500)Text("${text.length} / 4000",Modifier.fillMaxWidth().padding(end=54.dp,bottom=3.dp),textAlign=androidx.compose.ui.text.style.TextAlign.End,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){FilledTonalIconButton(onClick={showTools=true},modifier=Modifier.size(40.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=if(glassInput).66f else 1f))){Icon(Icons.Outlined.Add,"添加更多内容",Modifier.size(21.dp))};Spacer(Modifier.width(7.dp));Box(Modifier.weight(1f).heightIn(min=prefs.getInt("chat_input_min_height",42).coerceIn(40,64).dp,max=92.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=if(glassInput).62f else 1f)).then(if(glassInput)Modifier.border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.28f),RoundedCornerShape(20.dp))else Modifier).padding(horizontal=14.dp,vertical=8.dp),contentAlignment=Alignment.CenterStart){BasicTextField(value=text,onValueChange={text=it.take(4000)},modifier=Modifier.fillMaxWidth().onPreviewKeyEvent{event->if(chat.enterToSend&&event.type==KeyEventType.KeyDown&&event.key==Key.Enter&&!event.isShiftPressed){sendCurrent();true}else false},maxLines=4,textStyle=LocalTextStyle.current.copy(color=MaterialTheme.colorScheme.onSurface,fontSize=15.sp,lineHeight=20.sp),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),keyboardOptions=KeyboardOptions(imeAction=if(chat.enterToSend)ImeAction.Send else ImeAction.Default),keyboardActions=KeyboardActions(onSend={sendCurrent()}),decorationBox={inner->if(text.isEmpty())Text(if(voiceRecording||offlineVoiceBusy())"正在录音…" else if(thisSending)"继续发送，角色稍后回复…" else "写下此刻想说的话…",fontSize=14.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);inner()})};if(thisSending){Spacer(Modifier.width(4.dp));FilledTonalIconButton(onClick={stopGeneration()},modifier=Modifier.size(36.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.errorContainer.copy(alpha=if(glassInput).78f else 1f))){Icon(Icons.Filled.Stop,"停止生成",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onErrorContainer)}};Spacer(Modifier.width(4.dp));FilledIconButton(onClick={sendCurrent()},modifier=Modifier.size(42.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.primary.copy(alpha=if(glassInput).88f else 1f))){Icon(Icons.AutoMirrored.Filled.Send,"发送",Modifier.size(19.dp))}}
        }}
      }
    ){padding->
      val listModifier=if(glassInput)Modifier.fillMaxSize().padding(top=padding.calculateTopPadding()) else Modifier.fillMaxSize().padding(padding)
      LazyColumn(state=listState,modifier=listModifier,contentPadding=PaddingValues(start=14.dp,end=14.dp,top=12.dp,bottom=if(glassInput)padding.calculateBottomPadding()+28.dp else 28.dp),verticalArrangement=Arrangement.spacedBy(prefs.getInt("chat_bubble_spacing",12).coerceIn(6,22).dp)){
        itemsIndexed(visibleMessages,key={_,msg->msg.id}){index,msg->Column(Modifier.fillMaxWidth()){
          val previous=visibleMessages.getOrNull(index-1)
          if(shouldShowMessageTime(previous,msg))MessageTimeDivider(msg.createdAt)
          val suggestionAnchor=suggestionPosition=="bubble"&&msg.id==suggestionAnchorId
          MessageBubble(card,chat,msg,onLongPress={menuMessage=it},onCopy={clipboard.setText(AnnotatedString(msg.body));toast("已复制")},onRetry={if(sending){toast("请等待当前生成结束")}else if(activeProvider()==null){toast("请先在模型设置中添加供应商")}else{val i=chat.messages.indexOf(msg);if(i>=0){while(chat.messages.size>i)chat.messages.removeAt(chat.messages.lastIndex);save();request(chat,find(msg.speakerCardId)?:card)}}},onErrorDetails={errorMessage=msg},onImageRequest={generateConfirmedImage(msg)},onCancelImageRequest={chat.messages.remove(msg);save();uiRevision++;toast("已取消生图请求")},onClaimRedPacket={claimRedPacket(msg)},onPatRole={patRole(it)},suggestionsLoading=suggestionAnchor&&suggestionsLoading,onSuggestions=if(suggestionAnchor){{loadReplySuggestions()}}else null)
          if(suggestionAnchor&&replySuggestions.isNotEmpty())ReplySuggestionBar(replySuggestions){text=it}
        }}
      }
    }
    }
    if(showTools){
      ModalBottomSheet(onDismissRequest={showTools=false},dragHandle={BottomSheetDefaults.DragHandle()}){
        Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=24.dp)){
          Text("添加到对话",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
          Text("图片和表情可以继续搭配文字发送",Modifier.padding(top=3.dp,bottom=16.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          LazyVerticalGrid(
            columns=GridCells.Fixed(4),
            modifier=Modifier.fillMaxWidth().height(176.dp),
            userScrollEnabled=false,
            horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)
          ){
            item{ChatToolAction(Icons.Outlined.AddPhotoAlternate,"图片"){showTools=false;imagePicker.launch(arrayOf("image/*"))}}
            item{val recording=voiceRecording||offlineVoiceBusy();ChatToolAction(if(recording)Icons.Filled.StopCircle else Icons.Outlined.Mic,if(recording)"结束录音" else "语音"){showTools=false;handleVoiceInput()}}
            item{ChatToolAction(Icons.Outlined.EmojiEmotions,"表情"){showTools=false;showStickers=true}}
            item{ChatToolAction(Icons.Outlined.CardGiftcard,"红包"){showTools=false;showRedPacket=true}}
            item{ChatToolAction(Icons.Outlined.Collections,"朋友圈"){showTools=false;composePage=Page.MOMENT_SHARE}}
            item{ChatToolAction(Icons.Outlined.TouchApp,"拍一拍"){showTools=false;patRole(card)}}
            item{ChatToolAction(Icons.Outlined.TravelExplore,"联网"){showTools=false;if(prefs.getBoolean("web_search_enabled",false))webSearchNext=true else{toast("请先配置联网搜索 API");webSearchBackPage=Page.CHAT;composePage=Page.WEB_SEARCH}}}
            if(card.id in momentRoleIds("moments_post_roles"))item{ChatToolAction(Icons.Outlined.AutoAwesome,"角色动态"){showTools=false;momentRolePostBackPage=Page.CHAT;composePage=Page.MOMENT_ROLE_POST}}
          }
        }
      }
    }
    if(showStickers)ModalBottomSheet(onDismissRequest={showStickers=false},dragHandle={BottomSheetDefaults.DragHandle()}){
      val allStickers=StickerRepository.all(this@ComposeActivity)
      val favoriteSet=StickerRepository.favorites(prefs)
      val shown=if(favoritesOnly)allStickers.filter{favoriteSet.contains(it)}else allStickers
      Column(Modifier.fillMaxWidth().heightIn(min=300.dp,max=560.dp).padding(horizontal=14.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("选择表情",Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold);FilterChip(favoritesOnly,{favoritesOnly=!favoritesOnly},label={Text("收藏 ${favoriteSet.size}")},leadingIcon={Icon(if(favoritesOnly)Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,null,Modifier.size(17.dp))})}
        Text("点击选择，长按收藏或取消收藏",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(shown.isEmpty())Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Outlined.EmojiEmotions,null,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.secondary);Text(if(favoritesOnly)"还没有收藏表情" else "还没有表情资源",Modifier.padding(top=9.dp),fontWeight=FontWeight.Medium);Button(onClick={showStickers=false;stickerResourcesBackPage=Page.CHAT;composePage=Page.STICKER_RESOURCES},Modifier.padding(top=12.dp)){Text("前往表情资源")}}
        else LazyVerticalGrid(columns=GridCells.Fixed(4),modifier=Modifier.fillMaxWidth().weight(1f).padding(top=10.dp),contentPadding=PaddingValues(bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){gridItems(shown.take(240),key={it}){path->Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.aspectRatio(1f).combinedClickable(onClick={pendingImage=path;pendingSticker=true;showStickers=false},onLongClick={val added=StickerRepository.toggleFavorite(this@ComposeActivity,prefs,path);uiRevision++;toast(if(added)"已收藏表情" else "已取消收藏")})){MessageImage(path,Modifier.fillMaxSize().padding(5.dp),ContentScale.Fit)}}}
      }
    }
    if(showRedPacket)ModalBottomSheet(onDismissRequest={showRedPacket=false},dragHandle={BottomSheetDefaults.DragHandle()}){Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=28.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFE9634F)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.CardGiftcard,null,tint=Color.White)};Text("发送红包",Modifier.padding(start=11.dp),fontSize=18.sp,fontWeight=FontWeight.SemiBold)};OutlinedTextField(redAmount,{value->redAmount=value.filter{it.isDigit()||it=='.'}.take(8)},Modifier.fillMaxWidth().padding(top=16.dp),label={Text("金额")},prefix={Text("¥ ")},singleLine=true,shape=RoundedCornerShape(13.dp));OutlinedTextField(redGreeting,{redGreeting=it.take(40)},Modifier.fillMaxWidth().padding(top=10.dp),label={Text("祝福语")},singleLine=true,shape=RoundedCornerShape(13.dp));Button(onClick={val amount=redAmount.toDoubleOrNull();if(amount==null||amount<=0||amount>99999)toast("请输入 0.01 至 99999 元的红包金额")else{val greeting=redGreeting.trim().ifBlank{"恭喜发财，万事如意"};val meta=JSONObject().put("amount",String.format(java.util.Locale.US,"%.2f",amount)).put("greeting",greeting).toString();if(sendMessage(greeting,"","red_packet",meta)){showRedPacket=false;redAmount="";redGreeting="恭喜发财，万事如意"}}},Modifier.fillMaxWidth().padding(top=16.dp).height(48.dp)){Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text("发送红包")}}}
    menuMessage?.let{msg->AlertDialog(onDismissRequest={menuMessage=null},title={Text("消息操作")},text={Column{if(msg.image.isNotBlank())TextButton(onClick={val saved=StickerRepository.addFavorite(this@ComposeActivity,prefs,msg.image);menuMessage=null;if(saved!=null){uiRevision++;toast("已收藏到表情库")}else toast("收藏失败，图片无法读取")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.EmojiEmotions,null);Spacer(Modifier.width(8.dp));Text("收藏为表情",Modifier.weight(1f))};TextButton(onClick={clipboard.setText(AnnotatedString(msg.body));menuMessage=null;toast("已复制")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.ContentCopy,null);Spacer(Modifier.width(8.dp));Text("复制",Modifier.weight(1f))};TextButton(onClick={editMessage=msg;editText=msg.body;menuMessage=null},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Edit,null);Spacer(Modifier.width(8.dp));Text("编辑",Modifier.weight(1f))};if(msg.role=="assistant"&&msg.body.isNotBlank()&&msg.messageType in listOf("text","desktop_pet_reply"))TextButton(onClick={menuMessage=null;requestAlternative(chat,msg,find(msg.speakerCardId)?:card)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.CallSplit,null);Spacer(Modifier.width(8.dp));Text("生成备选回复",Modifier.weight(1f))};TextButton(onClick={chat.messages.remove(msg);save();uiRevision++;menuMessage=null;toast("消息已删除")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Delete,null,tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(8.dp));Text("删除",Modifier.weight(1f),color=MaterialTheme.colorScheme.error)};if(msg.role=="assistant")TextButton(onClick={val i=chat.messages.indexOf(msg);if(i>=0){while(chat.messages.size>i)chat.messages.removeAt(chat.messages.lastIndex);menuMessage=null;request(chat,find(msg.speakerCardId)?:card)}},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text("从此处重新生成（删除后续）",Modifier.weight(1f))}}},confirmButton={})}
    editMessage?.let{msg->AlertDialog(onDismissRequest={editMessage=null},title={Text("编辑消息")},text={OutlinedTextField(editText,{editText=it},Modifier.fillMaxWidth(),minLines=3)},confirmButton={Button(onClick={msg.body=editText;msg.failed=false;msg.loading=false;msg.errorDetail="";msg.keepCurrentAlternative();editMessage=null;save();uiRevision++}){Text("保存")}},dismissButton={TextButton(onClick={editMessage=null}){Text("取消")}})}
    errorMessage?.let{msg->val detail=msg.errorDetail.ifBlank{lastApiError.ifBlank{"未记录详细错误"}};AlertDialog(onDismissRequest={errorMessage=null},icon={Icon(Icons.Outlined.BugReport,null)},title={Text("生成错误详情")},text={OutlinedTextField(value=detail,onValueChange={},modifier=Modifier.fillMaxWidth().heightIn(min=180.dp,max=420.dp),readOnly=true,minLines=8,maxLines=18,textStyle=LocalTextStyle.current.copy(fontFamily=FontFamily.Monospace,fontSize=12.sp,lineHeight=17.sp))},confirmButton={Button(onClick={clipboard.setText(AnnotatedString(detail));toast("错误详情已复制")}){Icon(Icons.Outlined.ContentCopy,null);Spacer(Modifier.width(6.dp));Text("复制")}},dismissButton={TextButton(onClick={errorMessage=null}){Text("关闭")}})}
  }

  @Composable
  private fun ChatDetailPage(chat:Chat){
    val card=find(chat.activeCardId.takeIf{it.isNotBlank()}?:chat.cardId)?:return;var quickModels by remember{mutableStateOf<List<String>>(emptyList())};var loading by remember{mutableStateOf(false)};var longMemory by remember(chat.id){mutableStateOf(chat.longMemory)};var threshold by remember(chat.id){mutableStateOf(chat.compressionThreshold.toString())};var richContent by remember(chat.id){mutableStateOf(chat.richContentEnabled)};var tavernFrontend by remember(chat.id){mutableStateOf(chat.tavernFrontendEnabled)};var customCss by remember(chat.id){mutableStateOf(chat.customCss)};var segmented by remember(chat.id){mutableStateOf(chat.segmentedRepliesEnabled)};var segmentParts by remember(chat.id){mutableIntStateOf(chat.segmentedReplyMaxParts.coerceIn(2,6))};var imageGeneration by remember(chat.id){mutableStateOf(chat.imageGenerationEnabled)}
    var relayRoundsText by remember(chat.id){mutableStateOf(chat.groupRelayRounds.toString())}
    val chatImporter=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)pool.execute{runCatching{parseImportedMessages(String(bytes(contentResolver.openInputStream(uri)),Charsets.UTF_8))}.onSuccess{values->main.post{if(values.isEmpty())toast("没有识别到聊天消息")else{chat.messages.addAll(values);chat.updated=System.currentTimeMillis();save();uiRevision++;toast("已导入 ${values.size} 条聊天记录")}}}.onFailure{main.post{toast("聊天记录导入失败：${it.message}")}}}}
    DetailScaffold("对话设置",Page.CHAT){
      item{InfoBanner(Icons.Outlined.Bolt,"快速切换","在这里切换供应商或模型，下一条消息立即使用新配置。")}
      item{SectionTitle("模型供应商")}
      if(providers().isEmpty())item{SettingButton(Icons.Outlined.Add,"添加模型供应商","当前暂未配置模型供应商"){providerBackPage=Page.CHAT_DETAIL;composePage=Page.ADD_PROVIDER}}
      items(providers(),key={it.id}){value->SettingButton(if(activeProvider()?.id==value.id)Icons.Filled.CheckCircle else Icons.Outlined.Cloud,value.name,if(activeProvider()?.id==value.id)"当前使用 · ${value.modelName}" else value.modelName){activateProvider(value)}}
      item{Button(onClick={if(activeProvider()==null)toast("请先添加模型供应商")else{loading=true;pool.execute{try{val c=URL(modelsEndpoint()).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Authorization","Bearer "+key());val code=c.responseCode;val raw=read(if(code<300)c.inputStream else c.errorStream);if(code !in 200..299)throw java.io.IOException("HTTP $code：$raw");val data=JSONObject(raw).optJSONArray("data")?:JSONArray();val result=mutableListOf<String>();for(i in 0 until data.length())data.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()}?.let(result::add);result.sort();main.post{lastApiError="";quickModels=result;loading=false}}catch(e:Exception){lastApiError=e.message?:e.toString();main.post{loading=false;toast("模型获取失败：${e.message}")}}}}},Modifier.fillMaxWidth().height(46.dp)){if(loading)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(if(loading)"正在获取模型" else "获取当前供应商模型",maxLines=1,softWrap=false)}}
      if(quickModels.isNotEmpty())item{ModelListCard(quickModels,model()){name->prefs.edit().putString("model",name).apply();activeProvider()?.let{active->val updated=providers().map{if(it.id==active.id)it.copy(modelName=name)else it};saveCustomProviders(updated)};uiRevision++;toast("已切换模型 $name")}}
      item{SectionTitle("Token 使用")};item{ChatTokenSummary(chat.promptTokens,chat.completionTokens)}
      item{SettingButton(Icons.Outlined.DataUsage,"Token 消耗检查器",if(chat.lastContextAt>0)"最近统计 ${formatTaskTime(chat.lastContextAt)}" else "查看输入、输出与上下文来源估算"){composePage=Page.CHAT_CONTEXT}}
      if(lastApiError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"最近一次 API 报错",lastApiError)}
      item{SectionTitle("对话行为")}
      if(chat.group)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("角色接力交流",fontWeight=FontWeight.Medium);Text("成员可在一轮回复后继续自然交流",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupAutoRelay,onCheckedChange={chat.groupAutoRelay=it;save();uiRevision++})};if(chat.groupAutoRelay){Spacer(Modifier.height(10.dp));OutlinedTextField(relayRoundsText,{value->relayRoundsText=value.filter(Char::isDigit).take(4);relayRoundsText.toIntOrNull()?.takeIf{it in 1..9999}?.let{chat.groupRelayRounds=it;save()}},Modifier.fillMaxWidth(),label={Text("每次最多接力轮数（1-9999）")},singleLine=true,shape=RoundedCornerShape(12.dp))}}}}
      if(chat.group)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("全部角色自主交流",fontWeight=FontWeight.Medium);Text("未指定 @ 时随机选择成员，并允许成员间接力",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupAutonomous,onCheckedChange={chat.groupAutonomous=it;save();uiRevision++})};Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("自行决定是否接话",fontWeight=FontWeight.Medium);Text("角色可根据人格和话题选择保持沉默",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupReplyDecision,onCheckedChange={chat.groupReplyDecision=it;save();uiRevision++})};if(chat.groupReplyDecision){Text("接话倾向 ${chat.groupReplyProbability}%",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(chat.groupReplyProbability.toFloat(),{chat.groupReplyProbability=it.toInt();save()},valueRange=0f..100f,steps=19)};Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许主动开启话题",fontWeight=FontWeight.Medium);Text("冷场时角色可按自身经历自然发起讨论",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupActiveTopics,onCheckedChange={chat.groupActiveTopics=it;save();uiRevision++})}}}}
      if(!chat.group)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("沉浸式私聊",fontWeight=FontWeight.Medium);Text("回复前合并连续消息，角色读完后自行决定发送几条",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.immersiveMode,onCheckedChange={chat.immersiveMode=it;save();uiRevision++})};if(chat.immersiveMode)Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色决定是否回复",fontWeight=FontWeight.Medium);Text("角色可根据关系和语境选择暂不回复",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.privateReplyDecision,onCheckedChange={chat.privateReplyDecision=it;save();uiRevision++})}}}}
      if(chat.group)item{InfoBanner(Icons.Outlined.AlternateEmail,"指定角色回答","在消息中输入 @角色名，或点击聊天页顶部成员，即可指定首位回复角色。")}
      item{SettingButton(Icons.Outlined.RecordVoiceOver,"语音回复",if(chat.ttsEnabled)listOf("","仅语音","文字与语音","随机回复").getOrElse(chat.ttsReplyMode){"已启用"} else "未启用 · 配置本地或在线 TTS"){composePage=Page.CHAT_TTS}}
      item{SettingButton(Icons.Outlined.NotificationsActive,"主动消息",if(chat.proactiveEnabled)"${chat.proactiveMinMinutes}-${chat.proactiveMaxMinutes} 分钟 · 每次 ${chat.proactiveBurstCount} 条" else "未开启 · 设置频率、连发与免打扰"){composePage=Page.PROACTIVE}}
      if(!chat.group)item{SettingButton(Icons.Outlined.Favorite,"病娇模式",if(chat.yandereEnabled)"App 退到后台后每分钟主动联系" else "未开启 · 独立配置后台持续联系"){composePage=Page.YANDERE}}
      item{SettingButton(Icons.Outlined.Share,"分享朋友圈给角色","选择一条动态发送到当前对话"){composePage=Page.MOMENT_SHARE}}
      if(card.id in momentRoleIds("moments_post_roles"))item{SettingButton(Icons.Outlined.Collections,"邀请角色发朋友圈","${card.name} 已获得发布授权"){momentRolePostBackPage=Page.CHAT_DETAIL;composePage=Page.MOMENT_ROLE_POST}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("发送后平滑滚动到底部",fontWeight=FontWeight.Medium);Text("稍作停顿后滑到底部等待生成内容",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.autoScrollAfterSend,onCheckedChange={chat.autoScrollAfterSend=it;save();uiRevision++})}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("角色分段回复",fontWeight=FontWeight.Medium);Text("将长回复拆成多个连续消息气泡",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=segmented,onCheckedChange={segmented=it;chat.segmentedRepliesEnabled=it;save();uiRevision++})};if(segmented){Text("最多 $segmentParts 段",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(value=segmentParts.toFloat(),onValueChange={segmentParts=it.toInt().coerceIn(2,6);chat.segmentedReplyMaxParts=segmentParts;save()},valueRange=2f..6f,steps=3)}}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色调用生图",fontWeight=FontWeight.Medium);Text(if(prefs.getBoolean("image_generation_enabled",false))"角色可按剧情需要生成图片" else "全局生图尚未开启",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=imageGeneration,onCheckedChange={imageGeneration=it;chat.imageGenerationEnabled=it;save();uiRevision++},enabled=prefs.getBoolean("image_generation_enabled",false))}}}
      item{SettingButton(Icons.Outlined.Image,"生图模型配置",prefs.getString("image_generation_model","")?.takeIf{it.isNotBlank()}?:"未配置"){imageGenerationBackPage=Page.CHAT_DETAIL;composePage=Page.IMAGE_GENERATION}}
      item{SectionTitle("角色扮演内容")};item{SettingButton(Icons.Outlined.AutoStories,"世界书、正则与预设",if(chat.worldBookOverride||chat.regexRulesOverride||chat.presetOverride)"当前对话使用自定义覆盖" else if(card.worldBook.isNotBlank()||card.regexRules.isNotBlank()||card.preset.isNotBlank())"正在使用角色卡内置内容" else "角色卡没有内置内容"){composePage=Page.CHAT_CONTENT}}
      item{SectionTitle("消息显示与输入")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){ChatSettingSwitch("流式输出","逐步显示角色正在生成的内容",chat.streamEnabled){chat.streamEnabled=it;save();uiRevision++};ChatSettingSwitch("Markdown","显示标题、强调、列表和代码样式",chat.markdownEnabled){chat.markdownEnabled=it;save();uiRevision++};ChatSettingSwitch("显示模型思考","模型返回思考字段时可展开查看",chat.showThinking){chat.showThinking=it;save();uiRevision++};ChatSettingSwitch("回车发送","软键盘发送键和外接键盘回车直接发送",chat.enterToSend){chat.enterToSend=it;save();uiRevision++}}}}
      item{SectionTitle("长记忆")};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("长记忆模式",fontWeight=FontWeight.Medium);Text("达到阈值后自动生成摘要压缩上下文",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=longMemory,onCheckedChange={longMemory=it})}}};item{StyledField(threshold,{threshold=it.filter(Char::isDigit).take(4)},"压缩阈值（10-9999 条消息）")};item{Button(onClick={chat.longMemory=longMemory;chat.compressionThreshold=(threshold.toIntOrNull()?:40).coerceIn(10,9999);save();uiRevision++;toast("长记忆设置已保存")},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存长记忆设置",maxLines=1,softWrap=false)}};if(chat.summary.isNotBlank())item{InfoBanner(Icons.Outlined.Summarize,"当前记忆摘要",chat.summary)}
      item{SettingButton(Icons.Outlined.ManageSearch,"嵌入模型实时库",if(prefs.getBoolean("embedding_enabled",false))"当前角色已索引 ${db.realtimeCount(card.id)} 条相关片段" else "未开启 · 使用语义检索补充上下文"){embeddingBackPage=Page.CHAT_DETAIL;composePage=Page.EMBEDDING_LIBRARY}}
      item{SectionTitle("富内容渲染")};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("显示酒馆卡片前端",fontWeight=FontWeight.Medium);Text("自动识别角色卡 HTML / CSS 页面，默认开启",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=tavernFrontend,onCheckedChange={tavernFrontend=it})}}};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用 HTML / CSS / JavaScript",fontWeight=FontWeight.Medium);Text("允许普通 AI 回复和酒馆前端运行内联脚本",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=richContent,onCheckedChange={richContent=it})}}};item{OutlinedTextField(customCss,{customCss=it},Modifier.fillMaxWidth().heightIn(min=110.dp),label={Text("自定义 CSS（可选）")},shape=RoundedCornerShape(13.dp),minLines=4)};item{Button(onClick={chat.tavernFrontendEnabled=tavernFrontend;chat.richContentEnabled=richContent;chat.customCss=customCss;save();uiRevision++;toast("富内容设置已保存")},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Code,null);Spacer(Modifier.width(8.dp));Text("保存渲染设置",maxLines=1,softWrap=false)}}
      item{SectionTitle("对话管理")}
      item{SettingButton(Icons.Outlined.FileUpload,"导入 JSON 聊天记录","支持时遇、Tavo、OpenAI role/content 和常见 messages 数组"){chatImporter.launch(arrayOf("application/json","text/plain","*/*"))}}
      item{SettingButton(Icons.Outlined.Badge,"角色设置",card.name){editingCardId=card.id;roleBackPage=Page.CHAT_DETAIL;composePage=Page.ROLE_SETTINGS}}
      item{SettingButton(Icons.Outlined.Edit,"重命名对话",chat.title){renameBackPage=Page.CHAT_DETAIL;composePage=Page.RENAME_CHAT}}
      item{OutlinedButton(onClick={askDanger("清空当前对话","将清空当前对话的全部消息和上下文摘要，但保留对话入口。","确认清空"){chat.messages.clear();chat.summary="";save();uiRevision++;toast("对话内容已清空")}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(8.dp));Text("清空当前对话",maxLines=1,softWrap=false)}}
      item{OutlinedButton(onClick={askDanger("删除当前对话","将永久删除当前对话及全部消息，此操作无法撤销。"){removeChat(chat);toast("对话已删除")}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.DeleteForever,null,tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(8.dp));Text("删除当前对话",maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.error)}}
    }
  }

  @Composable
  private fun ChatContextPage(chat:Chat) {
    val snapshot=remember(chat.id,chat.lastContextSnapshot){runCatching{JSONObject(chat.lastContextSnapshot)}.getOrNull()}
    val sources=remember(chat.id,chat.lastContextSnapshot){
      val value=snapshot?.optJSONObject("sources")?:JSONObject()
      buildList { val keys=value.keys();while(keys.hasNext()){val key=keys.next();add(key to value.optInt(key,0))} }
    }
    val lastActual=chat.lastPromptTokens+chat.lastCompletionTokens
    val estimated=snapshot?.optLong("estimatedTokens",0)?:0L
    DetailScaffold("Token 消耗检查器",Page.CHAT_DETAIL){
      item{InfoBanner(Icons.Outlined.DataUsage,"API 用量与本地估算","累计和最近实际用量来自模型供应商返回的 usage；供应商未返回 usage 时，最近请求按文本字符数进行本地估算。检查器只保存统计数据，不保存请求正文。")}
      item{SectionTitle("当前对话累计")}
      item{ChatTokenSummary(chat.promptTokens,chat.completionTokens)}
      item{SectionTitle("最近一次请求")}
      if(snapshot==null)item{EmptyState(Icons.Outlined.QueryStats,"还没有请求统计","在当前对话发送一次消息后即可查看")}
      else {
        item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
          Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Memory,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(start=10.dp)){Text(snapshot.optString("model","未记录模型"),fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(snapshot.optString("provider","未记录供应商"),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};Text(formatTaskTime(snapshot.optLong("createdAt",chat.lastContextAt)),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
          HorizontalDivider()
          if(lastActual>0)ChatTokenSummary(chat.lastPromptTokens,chat.lastCompletionTokens)else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("供应商未返回 usage",fontWeight=FontWeight.Medium);Text("以下显示本地估算值",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(String.format(Locale.getDefault(),"约 %,d Token",estimated),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
        }}}
        item{SectionTitle("输入来源估算")}
        if(sources.isEmpty())item{InfoBanner(Icons.Outlined.Info,"没有来源统计","当前记录未包含输入来源明细")}
        else items(sources,key={it.first}){source->val tokens=kotlin.math.ceil(source.second/2.2).toLong();Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Text(source.first,Modifier.weight(1f),fontSize=14.sp);Column(horizontalAlignment=Alignment.End){Text(String.format(Locale.getDefault(),"约 %,d Token",tokens),fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text(String.format(Locale.getDefault(),"%,d 字符",source.second),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
      }
    }
  }

  @Composable
  private fun GenerationTasksPage() {
    var tasks by remember{mutableStateOf<List<GenerationTaskTracker.Task>>(GenerationTaskTracker.list(this@ComposeActivity))}
    var tab by rememberSaveable{mutableIntStateOf(0)}
    LaunchedEffect(Unit){while(true){tasks=GenerationTaskTracker.list(this@ComposeActivity);delay(1000)}}
    val visible=tasks.filter{if(tab==0)it.active() else !it.active()}
    DetailScaffold("生成任务"){
      item{TabRow(selectedTabIndex=tab,containerColor=Color.Transparent,divider={}){listOf("进行中","历史").forEachIndexed{index,label->Tab(selected=tab==index,onClick={tab=index},text={Text(if(index==0&&tasks.count{it.active()}>0)"$label ${tasks.count{it.active()}}" else label,maxLines=1,softWrap=false)})}}}
      if(tab==1&&visible.isNotEmpty())item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={GenerationTaskTracker.clearHistory(this@ComposeActivity);tasks=GenerationTaskTracker.list(this@ComposeActivity)}){Icon(Icons.Outlined.DeleteSweep,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("清理历史")}}}
      if(visible.isEmpty())item{EmptyState(if(tab==0)Icons.Outlined.HourglassEmpty else Icons.Outlined.History,if(tab==0)"当前没有生成任务" else "还没有历史记录",if(tab==0)"聊天、备选回复和桌宠任务会显示在这里" else "完成或失败的生成任务会保留在这里")}
      items(visible,key={it.id}){task->
        val statusLabel=when(task.status){"active"->"生成中";"queued"->"排队中";"success"->"已完成";"failed"->"失败";"cancelled"->"已停止";else->task.status}
        val statusColor=when(task.status){"active","queued"->MaterialTheme.colorScheme.primary;"success"->Color(0xFF2E7D32);"failed"->MaterialTheme.colorScheme.error;else->MaterialTheme.colorScheme.onSurfaceVariant}
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
          Row(verticalAlignment=Alignment.Top){Box(Modifier.size(38.dp).clip(CircleShape).background(statusColor.copy(alpha=.12f)),contentAlignment=Alignment.Center){if(task.active())CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=statusColor)else Icon(when(task.status){"success"->Icons.Outlined.CheckCircle;"failed"->Icons.Outlined.ErrorOutline;else->Icons.Outlined.StopCircle},null,Modifier.size(21.dp),tint=statusColor)};Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(task.title.ifBlank{"生成任务"},fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(task.detail,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)};Text(statusLabel,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=statusColor)}
          Row(verticalAlignment=Alignment.CenterVertically){Text("${taskTypeLabel(task.type)} · ${formatTaskTime(task.updatedAt)}",Modifier.weight(1f),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(task.sourceId.isNotBlank()&&chats.any{it.id==task.sourceId})IconButton(onClick={selectedChat=chats.firstOrNull{it.id==task.sourceId};composePage=Page.CHAT},Modifier.size(36.dp)){Icon(Icons.Outlined.OpenInNew,"打开对话",Modifier.size(18.dp))};if(task.active()&&task.cancellable&&sending&&task.sourceId==sendingChatId)IconButton(onClick={stopGeneration();tasks=GenerationTaskTracker.list(this@ComposeActivity)},Modifier.size(36.dp)){Icon(Icons.Filled.Stop,"停止生成",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.error)}}
          if(task.error.isNotBlank())Text(task.error,fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.error,maxLines=4,overflow=TextOverflow.Ellipsis)
        }}
      }
    }
  }

  private fun taskTypeLabel(type:String)=when(type){"chat"->"聊天回复";"branch"->"备选回复";"pet"->"桌宠互动";"suggestion"->"推荐回复";else->"模型生成"}
  private fun formatTaskTime(timestamp:Long):String=if(timestamp<=0L)"未记录" else java.text.SimpleDateFormat("M月d日 HH:mm:ss",Locale.CHINA).format(Date(timestamp))

  @Composable
  private fun ProactiveSettingsPage(chat: Chat) {
    var enabled by remember(chat.id) { mutableStateOf(chat.proactiveEnabled) }
    var minMinutes by remember(chat.id) { mutableStateOf(chat.proactiveMinMinutes.toString()) }
    var maxMinutes by remember(chat.id) { mutableStateOf(chat.proactiveMaxMinutes.toString()) }
    var burstCount by remember(chat.id) { mutableIntStateOf(chat.proactiveBurstCount.coerceIn(1,20)) }
    var followUps by remember(chat.id) { mutableStateOf(chat.proactiveFollowUps) }
    var maxFollowUps by remember(chat.id) { mutableIntStateOf(chat.proactiveMaxFollowUps.coerceIn(1, 30)) }
    var dnd by remember(chat.id) { mutableStateOf(chat.proactiveDnd) }
    var dndStart by remember(chat.id) { mutableStateOf(chat.proactiveDndStart.toString()) }
    var dndEnd by remember(chat.id) { mutableStateOf(chat.proactiveDndEnd.toString()) }
    val lastError=prefs.getString("proactive_last_error","").orEmpty()
    DetailScaffold("主动消息", Page.CHAT_DETAIL) {
      item { InfoBanner(Icons.Outlined.AutoAwesome, "让角色自然想起你", "应用进入后台后，系统会根据当前对话和角色记忆生成消息；频率是最短间隔，实际时间会受系统调度影响。") }
      item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许当前角色主动发消息",fontWeight=FontWeight.Medium);Text("消息会保存到对话并发送系统通知",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=enabled,onCheckedChange={enabled=it})}} }
      item { SectionTitle("触发频率") }
      item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(minMinutes,{minMinutes=it},"最短分钟（≥1）")};Box(Modifier.weight(1f)){StyledField(maxMinutes,{maxMinutes=it},"最长分钟")}} }
      item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("每次连续发送 $burstCount 条",fontWeight=FontWeight.Medium);Text("同一次触发可随机连续生成 1 到 20 条自然消息",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(value=burstCount.toFloat(),onValueChange={burstCount=it.toInt().coerceIn(1,20)},valueRange=1f..20f,steps=18)}} }
      item { SectionTitle("连续追问") }
      item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("没有回复时继续关心",fontWeight=FontWeight.Medium);Text("收到你的新消息后会重新计数",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=followUps,onCheckedChange={followUps=it})};if(followUps){Spacer(Modifier.height(10.dp));Text("最多连续 $maxFollowUps 次",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(value=maxFollowUps.toFloat(),onValueChange={maxFollowUps=it.toInt().coerceIn(1,30)},valueRange=1f..30f,steps=28)}}} }
      item { SectionTitle("免打扰") }
      item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用免打扰时段",fontWeight=FontWeight.Medium);Text("时段内不生成也不推送主动消息",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=dnd,onCheckedChange={dnd=it})}} }
      if(dnd)item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(dndStart,{dndStart=it},"开始（0-23 时）")};Box(Modifier.weight(1f)){StyledField(dndEnd,{dndEnd=it},"结束（0-23 时）")}} }
      if(lastError.isNotBlank())item { InfoBanner(Icons.Outlined.ErrorOutline,"最近一次后台生成失败",lastError) }
      item { Button(onClick={
        val start=dndStart.toIntOrNull();val end=dndEnd.toIntOrNull();val min=minMinutes.toIntOrNull();val max=maxMinutes.toIntOrNull()
        if(dnd&&(start==null||end==null||start !in 0..23||end !in 0..23))toast("请输入 0 到 23 的整点") else if(min==null||max==null||min<1||max<min||max>525600)toast("请输入 1 到 525600 分钟的有效范围") else {
          val safeMin=min.coerceIn(1,525600);val safeMax=max.coerceIn(safeMin,525600)
          chat.proactiveEnabled=enabled;chat.proactiveFrequencyMinutes=safeMin;chat.proactiveMinMinutes=safeMin;chat.proactiveMaxMinutes=safeMax;chat.proactiveBurstCount=burstCount;chat.proactiveFollowUps=followUps;chat.proactiveMaxFollowUps=if(followUps)maxFollowUps.coerceIn(1,30) else 1;chat.proactiveDnd=dnd;chat.proactiveDndStart=start?:23;chat.proactiveDndEnd=end?:8
          if((enabled||chat.yandereEnabled)&&chat.lastProactiveAt==0L)chat.lastProactiveAt=System.currentTimeMillis()
          chat.nextProactiveAt=System.currentTimeMillis()+kotlin.random.Random.nextLong(safeMin.toLong(),safeMax.toLong()+1)*60_000L
          if(!enabled&&!chat.yandereEnabled)chat.proactiveSequence=0
          if(enabled||chat.yandereEnabled){ensureNotificationPermission();prefs.edit().putBoolean("active_replies",true).apply()}
          save();ActiveReplyWorker.sync(this@ComposeActivity);YandereMessageService.sync(this@ComposeActivity);uiRevision++;toast("主动消息设置已保存");composePage=Page.CHAT_DETAIL
        }
      },Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存主动消息设置",maxLines=1,softWrap=false)} }
    }
  }

  @Composable
  private fun YandereSettingsPage(chat:Chat){
    var enabled by remember(chat.id){mutableStateOf(chat.yandereEnabled)}
    var screenTime by remember(chat.id){mutableStateOf(chat.yandereScreenTimeEnabled)}
    var appUsage by remember(chat.id){mutableStateOf(chat.yandereAppUsageEnabled)}
    val access=hasUsageStatsAccess();val usage=if(access)screenTimeLast24Hours() else -1L
    DetailScaffold("病娇模式",Page.CHAT_DETAIL){
      item{InfoBanner(Icons.Outlined.Favorite,"只在 App 后台触发","离开时遇后，当前角色会每分钟尝试生成一条主动消息并通过通知发送；回到 App 后立即暂停。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("持续主动联系",fontWeight=FontWeight.Medium);Text("可能持续消耗所选模型的 Token 或余额",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(enabled,{value->if(value&&!enabled)askDanger("开启病娇模式","角色会在 App 后台每分钟请求模型并发送通知，可能持续消耗 API 额度。","确认开启"){enabled=true}else enabled=false})}}}
      item{SectionTitle("屏幕使用时长授权")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色了解过去 24 小时总时长",fontWeight=FontWeight.Medium);Text(if(access)"当前约 ${formatDuration(usage)}，具体应用需下方单独授权" else "尚未获得系统使用情况访问权限",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(screenTime,{value->if(value&&!access){runCatching{startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))};toast("请为时遇开启使用情况访问权限")}else{screenTime=value;if(!value)appUsage=false}})}}}
      if(screenTime&&access)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("发送常用应用列表",fontWeight=FontWeight.Medium);Text("向当前角色提供过去 24 小时使用最多的 6 个应用及各自时长",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(appUsage,{value->if(value&&!appUsage)askDanger("授权具体应用列表","应用名称与使用时长会随聊天请求发送给你配置的模型供应商。","确认授权"){appUsage=true}else appUsage=false})}}}
      if(!access)item{OutlinedButton(onClick={runCatching{startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))}.onFailure{toast("无法打开系统授权页面")}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.AdminPanelSettings,null);Spacer(Modifier.width(7.dp));Text("前往系统授权",maxLines=1)}}
      item{InfoBanner(Icons.Outlined.PrivacyTip,"分级授权","总时长和具体应用列表分别控制；关闭具体应用列表后，角色只能获得汇总时长。")}
      item{Button(onClick={chat.yandereEnabled=enabled;chat.yandereScreenTimeEnabled=screenTime&&hasUsageStatsAccess();chat.yandereAppUsageEnabled=chat.yandereScreenTimeEnabled&&appUsage;if(enabled){ensureNotificationPermission();prefs.edit().putBoolean("active_replies",true).apply();if(chat.lastProactiveAt==0L)chat.lastProactiveAt=System.currentTimeMillis()};save();ActiveReplyWorker.sync(this@ComposeActivity);YandereMessageService.sync(this@ComposeActivity);uiRevision++;toast("病娇模式设置已保存");composePage=Page.CHAT_DETAIL},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存设置",maxLines=1)}}
    }
  }

  @Composable
  private fun MomentSharePage(chat:Chat){
    val card=find(chat.activeCardId.takeIf{it.isNotBlank()}?:chat.cardId)?:return
    val posts=remember(uiRevision){moments()}
    fun share(post:MomentPost){
      if(activeProvider()==null){toast("请先配置模型供应商");return}
      if(sending&&sendingChatId!=chat.id){toast("另一会话正在生成，请稍候");return}
      val meta=JSONObject().put("id",post.id).put("authorId",post.authorId).put("authorName",post.authorName).put("content",post.content).put("createdAt",post.createdAt).toString()
      val message=Msg("user",post.content.ifBlank{"[图片动态]"});message.messageType="moment_share";message.meta=meta;message.image=post.image;chat.messages.add(message);chat.updated=System.currentTimeMillis();save();composePage=Page.CHAT;uiRevision++
      if(sending&&sendingChatId==chat.id)queueReply(chat,card)else request(chat,card)
    }
    DetailScaffold("分享朋友圈",Page.CHAT){
      item{InfoBanner(Icons.Outlined.Share,"发送给当前角色","选择后会作为一条消息出现在对话中，角色可以看到动态正文和图片并自然回应。")}
      if(posts.isEmpty())item{EmptyState(Icons.Outlined.Collections,"还没有朋友圈动态","先到朋友圈发布一条内容")}
      items(posts,key={it.id}){post->Surface(onClick={share(post)},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){val role=find(post.authorId);if(role!=null)RoleAvatar(role,42)else UserAvatar(42);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(post.authorName,fontWeight=FontWeight.SemiBold,maxLines=1);Text(post.content.ifBlank{"[图片动态]"},fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.Send,"分享",tint=MaterialTheme.colorScheme.primary)}}}
    }
  }

  private fun formatDuration(value:Long):String{if(value<0)return "未知";val minutes=value/60_000L;return if(minutes<60)"${minutes} 分钟" else "${minutes/60} 小时 ${minutes%60} 分钟"}

  @Composable
  private fun ChatContentPage(chat: Chat) {
    val card = find(chat.cardId) ?: return
    var worldBookOverride by remember(chat.id) { mutableStateOf(chat.worldBookOverride) }
    var regexRulesOverride by remember(chat.id) { mutableStateOf(chat.regexRulesOverride) }
    var presetOverride by remember(chat.id) { mutableStateOf(chat.presetOverride) }
    var worldBook by remember(chat.id) { mutableStateOf(if(chat.worldBookOverride)chat.worldBook else card.worldBook) }
    var regexRules by remember(chat.id) { mutableStateOf(if(chat.regexRulesOverride)chat.regexRules else card.regexRules) }
    var preset by remember(chat.id) { mutableStateOf(if(chat.presetOverride)chat.preset else card.preset) }
    var importingPreset by remember { mutableStateOf(false) }
    var importingWorldBook by remember { mutableStateOf(false) }
    val worldBookImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pool.execute {
        runCatching { extractWorldBook(String(bytes(contentResolver.openInputStream(uri)), Charsets.UTF_8)) }
          .onSuccess { value -> main.post { worldBook=value;worldBookOverride=true;importingWorldBook=false;toast("世界书已导入") } }
          .onFailure { main.post { importingWorldBook=false;toast("世界书导入失败：${it.message}") } }
      } else importingWorldBook=false
    }
    val presetImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pool.execute {
        runCatching { String(bytes(contentResolver.openInputStream(uri)), Charsets.UTF_8) }
          .onSuccess { raw -> main.post { preset = runCatching { val j=JSONObject(raw.removePrefix("\uFEFF"));j.optString("content",j.optString("prompt",j.optString("system",raw))) }.getOrDefault(raw);presetOverride=true;importingPreset=false;toast("预设已导入") } }
          .onFailure { main.post { importingPreset=false;toast("预设导入失败：${it.message}") } }
      } else importingPreset=false
    }
    DetailScaffold("世界书与预设", Page.CHAT_DETAIL) {
      item { InfoBanner(Icons.Outlined.AutoStories, "按当前对话单独编辑", "关闭覆盖时继承角色卡内容；开启后可修改或清空，且只影响当前对话。") }
      item { SectionTitle("世界书") }
      item { ContentOverrideSwitch("覆盖角色卡世界书",worldBookOverride){worldBookOverride=it;worldBook=if(it)(chat.worldBook.takeIf{value->value.isNotEmpty()}?:card.worldBook)else card.worldBook} }
      item { OutlinedTextField(worldBook,{worldBook=it;worldBookOverride=true},Modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),label={Text("世界书 JSON 或文本")},minLines=5,maxLines=10,shape=RoundedCornerShape(14.dp)) }
      item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){OutlinedButton(onClick={importingWorldBook=true;worldBookImporter.launch(arrayOf("application/json","text/plain","*/*"))},modifier=Modifier.weight(1f).height(42.dp)){Icon(Icons.Outlined.FileOpen,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(if(importingWorldBook)"导入中" else "导入世界书",maxLines=1,softWrap=false)};OutlinedButton(onClick={worldBook=card.worldBook;worldBookOverride=true},modifier=Modifier.weight(1f).height(42.dp)){Icon(Icons.Outlined.Restore,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("载入卡片原版",maxLines=1,softWrap=false)}} }
      item { TextButton(onClick={runCatching{val value=worldBook.trim();worldBook=if(value.startsWith("["))JSONArray(value).toString(2)else JSONObject(value).toString(2);worldBookOverride=true}.onFailure{toast("世界书不是有效 JSON")}},modifier=Modifier.fillMaxWidth().height(38.dp)){Icon(Icons.Outlined.FormatAlignLeft,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("格式化世界书 JSON")} }
      item { SectionTitle("正则规则") }
      item { ContentOverrideSwitch("覆盖角色卡正则",regexRulesOverride){regexRulesOverride=it;regexRules=if(it)(chat.regexRules.takeIf{value->value.isNotEmpty()}?:card.regexRules)else card.regexRules} }
      item { OutlinedTextField(regexRules,{regexRules=it;regexRulesOverride=true},Modifier.fillMaxWidth().heightIn(min=110.dp,max=200.dp),label={Text("正则脚本 JSON 或文本")},minLines=4,maxLines=9,shape=RoundedCornerShape(14.dp)) }
      item { TextButton(onClick={regexRules=card.regexRules;regexRulesOverride=true},modifier=Modifier.fillMaxWidth().height(38.dp)){Icon(Icons.Outlined.Restore,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("重新载入角色卡原版正则")} }
      item { SectionTitle("预设") }
      item { ContentOverrideSwitch("覆盖角色卡预设",presetOverride){presetOverride=it;preset=if(it)(chat.preset.takeIf{value->value.isNotEmpty()}?:card.preset)else card.preset} }
      item { OutlinedTextField(preset,{preset=it;presetOverride=true},Modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),label={Text("对话预设文本")},minLines=5,maxLines=10,shape=RoundedCornerShape(14.dp)) }
      item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){OutlinedButton(onClick={importingPreset=true;presetImporter.launch(arrayOf("application/json","text/plain","*/*"))},modifier=Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.FileOpen,null);Spacer(Modifier.width(6.dp));Text(if(importingPreset)"导入中" else "导入预设",maxLines=1,softWrap=false)};Button(onClick={chat.worldBook=if(worldBookOverride)worldBook else "";chat.regexRules=if(regexRulesOverride)regexRules else "";chat.preset=if(presetOverride)preset else "";chat.worldBookOverride=worldBookOverride;chat.regexRulesOverride=regexRulesOverride;chat.presetOverride=presetOverride;save();uiRevision++;toast("内容规则已保存");composePage=Page.CHAT_DETAIL},modifier=Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存",maxLines=1,softWrap=false)}} }
    }
  }

  @Composable
  private fun AppBehaviorPage() {
    var active by remember { mutableStateOf(prefs.getBoolean("active_replies", false)) }
    var keepAlive by remember { mutableStateOf(prefs.getBoolean("notification_keep_alive", false)) }
    val batteryAllowed=batteryWhitelistEnabled()
    DetailScaffold("应用通知") {
      item { InfoBanner(Icons.Outlined.NotificationsActive,"后台消息推送","普通 AI 回复和角色主动消息在后台完成后都会通知；点击通知可直接回到对应对话。") }
      item { SettingButton(Icons.Outlined.Tune,"通用聊天默认设置","新建私聊与群聊自动使用，可在单个对话中覆盖") { composePage=Page.CHAT_DEFAULTS } }
      item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("角色主动消息总开关",fontWeight=FontWeight.Medium);Text("每个对话仍可单独设置频率和免打扰",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=active,onCheckedChange={active=it;if(it)ensureNotificationPermission();prefs.edit().putBoolean("active_replies",it).apply();ActiveReplyWorker.sync(this@ComposeActivity);YandereMessageService.sync(this@ComposeActivity);toast(if(it)"已开启主动消息" else "已关闭主动消息")})}} }
      item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("通知保活",fontWeight=FontWeight.Medium);Text("显示低优先级常驻通知，提高后台消息送达稳定性",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=keepAlive,onCheckedChange={keepAlive=it;if(it)ensureNotificationPermission();prefs.edit().putBoolean("notification_keep_alive",it).apply();NotificationKeepAliveService.sync(this@ComposeActivity);toast(if(it)"通知保活已开启" else "通知保活已关闭")})}} }
      item { SettingButton(Icons.Outlined.BatterySaver, "电池优化白名单", if(batteryAllowed)"已允许后台运行" else "未加入 · 点击前往系统授权") { requestBatteryWhitelist() } }
      item { SettingButton(Icons.Outlined.Settings, "系统通知设置", "管理通知权限、声音与锁屏显示") { runCatching{startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))}.onFailure{toast("无法打开通知设置")} } }
      item { InfoBanner(Icons.Outlined.Security, "系统调度说明", "通知保活不会绕过系统权限，也不能保证进程永不被清理。配合电池优化白名单、自启动和后台运行权限可减少延迟。") }
    }
  }

  @Composable
  private fun ChatDefaultsPage(){
    var immersive by remember{mutableStateOf(prefs.getBoolean("default_private_immersive",false))};var privateDecision by remember{mutableStateOf(prefs.getBoolean("default_private_reply_decision",false))};var privateMin by remember{mutableStateOf(prefs.getInt("default_private_proactive_min",180).toString())};var privateMax by remember{mutableStateOf(prefs.getInt("default_private_proactive_max",180).toString())};var privateBurst by remember{mutableStateOf(prefs.getInt("default_private_burst",1).toString())}
    var relay by remember{mutableStateOf(prefs.getBoolean("default_group_relay",true))};var autonomous by remember{mutableStateOf(prefs.getBoolean("default_group_autonomous",true))};var topics by remember{mutableStateOf(prefs.getBoolean("default_group_topics",true))};var groupDecision by remember{mutableStateOf(prefs.getBoolean("default_group_reply_decision",true))};var rounds by remember{mutableStateOf(prefs.getInt("default_group_rounds",6).toString())};var probability by remember{mutableStateOf(prefs.getInt("default_group_probability",70).toString())};var groupMin by remember{mutableStateOf(prefs.getInt("default_group_proactive_min",180).toString())};var groupMax by remember{mutableStateOf(prefs.getInt("default_group_proactive_max",180).toString())};var groupBurst by remember{mutableStateOf(prefs.getInt("default_group_burst",1).toString())}
    DetailScaffold("通用聊天默认设置",Page.APP_BEHAVIOR){
      item{InfoBanner(Icons.Outlined.Tune,"只影响新建对话","已有对话保留各自设置，可在对应对话设置中单独修改。")}
      item{SectionTitle("私聊默认")};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("沉浸式私聊",Modifier.weight(1f));Switch(immersive,{immersive=it})};Row(verticalAlignment=Alignment.CenterVertically){Text("角色自行决定是否回复",Modifier.weight(1f));Switch(privateDecision,{privateDecision=it})}}}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(privateMin,{privateMin=it},"主动消息最短分钟")};Box(Modifier.weight(1f)){StyledField(privateMax,{privateMax=it},"最长分钟")}}};item{StyledField(privateBurst,{privateBurst=it},"随机连发上限（1-20）")}
      item{SectionTitle("群聊默认")};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("角色接力",Modifier.weight(1f));Switch(relay,{relay=it})};Row(verticalAlignment=Alignment.CenterVertically){Text("全部角色自主交流",Modifier.weight(1f));Switch(autonomous,{autonomous=it})};Row(verticalAlignment=Alignment.CenterVertically){Text("角色自行决定是否接话",Modifier.weight(1f));Switch(groupDecision,{groupDecision=it})};Row(verticalAlignment=Alignment.CenterVertically){Text("允许主动开启话题",Modifier.weight(1f));Switch(topics,{topics=it})}}}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(rounds,{rounds=it},"接力轮数")};Box(Modifier.weight(1f)){StyledField(probability,{probability=it},"接话倾向 %")}}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(groupMin,{groupMin=it},"主动消息最短分钟")};Box(Modifier.weight(1f)){StyledField(groupMax,{groupMax=it},"最长分钟")}}};item{StyledField(groupBurst,{groupBurst=it},"随机连发上限（1-20）")}
      item{Button(onClick={val pMin=privateMin.toIntOrNull();val pMax=privateMax.toIntOrNull();val pBurst=privateBurst.toIntOrNull();val gMin=groupMin.toIntOrNull();val gMax=groupMax.toIntOrNull();val gBurst=groupBurst.toIntOrNull();val r=rounds.toIntOrNull();val chance=probability.toIntOrNull();if(pMin==null||pMax==null||pBurst==null||gMin==null||gMax==null||gBurst==null||r==null||chance==null||pMin<1||pMax<pMin||pBurst !in 1..20||gMin<1||gMax<gMin||gBurst !in 1..20||r !in 1..9999||chance !in 0..100)toast("请检查分钟范围、连发上限、接力轮数和概率")else{prefs.edit().putBoolean("default_private_immersive",immersive).putBoolean("default_private_reply_decision",privateDecision).putInt("default_private_proactive_min",pMin).putInt("default_private_proactive_max",pMax).putInt("default_private_burst",pBurst).putBoolean("default_group_relay",relay).putBoolean("default_group_autonomous",autonomous).putBoolean("default_group_topics",topics).putBoolean("default_group_reply_decision",groupDecision).putInt("default_group_rounds",r).putInt("default_group_probability",chance).putInt("default_group_proactive_min",gMin).putInt("default_group_proactive_max",gMax).putInt("default_group_burst",gBurst).apply();toast("通用默认设置已保存");composePage=Page.APP_BEHAVIOR}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存默认设置")}}
    }
  }

  @Composable
  private fun SupportPage(){
    DetailScaffold("支持我们"){
      item{InfoBanner(Icons.Outlined.FavoriteBorder,"感谢支持时遇","时遇是一款免费 App。赞助将用于维护、测试设备与后续功能开发，不影响任何功能使用。")}
      item{Button(onClick={runCatching{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://docx.134506.xyz/shiyuzan.html")))}.onFailure{toast("无法打开赞助页面")}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.VolunteerActivism,null);Spacer(Modifier.width(8.dp));Text("前往赞助页面",maxLines=1,softWrap=false)}}
      item{OutlinedButton(onClick={copyText("作者 QQ","3581378884");runCatching{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=3581378884")))}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.PersonAdd,null);Spacer(Modifier.width(8.dp));Text("添加作者 QQ：3581378884",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}
      item{SettingButton(Icons.Outlined.Groups,"时遇交流群","1061590432 · 点击复制"){copyText("交流群号","1061590432")}}
      item{InfoBanner(Icons.Outlined.Info,"自愿支持","赞助完全自愿，不构成购买、投资或服务承诺。未赞助用户可正常使用全部免费功能。")}
    }
  }

  @Composable
  private fun ContentOverrideSwitch(title:String,checked:Boolean,onCheckedChange:(Boolean)->Unit){
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){
      Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
        Text(title,Modifier.weight(1f),fontSize=14.sp,color=MaterialTheme.colorScheme.onSurface)
        Switch(checked=checked,onCheckedChange=onCheckedChange)
      }
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun MessageBubble(card:Card,chat:Chat,msg:Msg,onLongPress:(Msg)->Unit,onCopy:()->Unit,onRetry:()->Unit,onErrorDetails:()->Unit,onImageRequest:()->Unit,onCancelImageRequest:()->Unit,onClaimRedPacket:()->Unit,onPatRole:(Card)->Unit,suggestionsLoading:Boolean=false,onSuggestions:(()->Unit)?=null){
    val mine=msg.role=="user";val speaker=find(msg.speakerCardId)?.takeIf{!mine}?:card
    if(msg.messageType in listOf("pat","red_packet_receipt","moment_post","screen_context_share","desktop_pet_screen_context")){
      val screenShare=msg.messageType in listOf("screen_context_share","desktop_pet_screen_context")
      val systemText=when{screenShare->"我分享了当前屏幕给角色";msg.messageType=="pat"&&!mine->"${speaker.name} 拍了拍我的 ${msg.body}";msg.messageType=="moment_post"->"${speaker.name} 发布了朋友圈：${msg.body}";else->msg.body}
      val systemIcon=when{screenShare->Icons.Outlined.Visibility;msg.messageType=="pat"->Icons.Outlined.TouchApp;msg.messageType=="moment_post"->Icons.Outlined.Collections;else->Icons.Outlined.Redeem}
      Box(Modifier.fillMaxWidth().padding(vertical=3.dp),contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.82f),modifier=Modifier.combinedClickable(onClick={},onLongClick={onLongPress(msg)})){Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(systemIcon,null,Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.width(6.dp));Text(systemText,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)}}}
      return
    }
    val frontendContent=!mine&&chat.tavernFrontendEnabled&&(msg.messageType=="tavern_frontend"||looksLikeTavernFrontend(msg.body))
    val noBubbles=prefs.getBoolean("chat_no_bubbles",false)
    val hideAvatars=prefs.getBoolean("chat_hide_avatars",noBubbles)
    var reasoningExpanded by remember(msg.id){mutableStateOf(false)}
    BoxWithConstraints(Modifier.fillMaxWidth()){
      val avatarSpace=if(hideAvatars)8.dp else 54.dp
      val bubbleMax=if(frontendContent||noBubbles)(maxWidth-avatarSpace).coerceAtLeast(180.dp) else minOf(maxWidth-avatarSpace,prefs.getInt("chat_bubble_max_width",292).coerceIn(220,380).dp).coerceAtLeast(140.dp)
      val mineBase=storedColor(prefs.getString("chat_user_bubble_color","").orEmpty(),MaterialTheme.colorScheme.primary)
      val aiBase=storedColor(prefs.getString("chat_ai_bubble_color","").orEmpty(),MaterialTheme.colorScheme.surface)
      val mineColor=mineBase.copy(alpha=(mineBase.alpha*prefs.getInt("chat_user_bubble_opacity",100).coerceIn(10,100)/100f).coerceIn(0f,1f))
      val aiColor=aiBase.copy(alpha=(aiBase.alpha*prefs.getInt("chat_ai_bubble_opacity",100).coerceIn(10,100)/100f).coerceIn(0f,1f))
      val mineText=if(noBubbles)MaterialTheme.colorScheme.onBackground else if(mineBase.luminance()>.55f)Color(0xFF202322) else Color.White
      val aiText=if(noBubbles)MaterialTheme.colorScheme.onBackground else if(aiBase.luminance()>.55f)Color(0xFF202322) else Color.White
      val verticalPadding=prefs.getInt("chat_bubble_vertical_padding",10).coerceIn(6,20).dp
      Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start,verticalAlignment=Alignment.Top){
        if(!mine&&!hideAvatars){Box(Modifier.padding(top=2.dp).combinedClickable(onClick={},onDoubleClick={onPatRole(speaker)})){RoleAvatar(speaker,38)};Spacer(Modifier.width(8.dp))}
        Column(Modifier.widthIn(max=bubbleMax),horizontalAlignment=if(mine)Alignment.End else Alignment.Start){
          Surface(modifier=Modifier.widthIn(max=bubbleMax).combinedClickable(onClick={if(!mine&&msg.messageType=="red_packet")onClaimRedPacket()},onLongClick={onLongPress(msg)}),shape=RoundedCornerShape(if(mine)18.dp else 14.dp),color=if(noBubbles&&!msg.failed||msg.messageType=="red_packet"||(msg.messageType=="sticker"&&msg.body.isBlank())||(!mine&&msg.loading&&chat.immersiveMode))Color.Transparent else if(msg.failed)MaterialTheme.colorScheme.errorContainer else if(mine)mineColor else aiColor){
          Column(horizontalAlignment=if(mine)Alignment.End else Alignment.Start){
            if(msg.loading&&msg.messageType!="image_request")if(chat.immersiveMode&&!chat.group)Spacer(Modifier.size(1.dp))else Row(Modifier.padding(horizontal=16.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.secondary);repeat(3){index->Box(Modifier.size((5+index).dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha=.45f+index*.2f)))}}
            if(!mine&&chat.group&&msg.speakerCardId.isNotBlank())Text(speaker.name,Modifier.padding(start=14.dp,top=8.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.secondary)
            if(!mine&&chat.showThinking&&msg.thinking.isNotBlank()){TextButton(onClick={reasoningExpanded=!reasoningExpanded},contentPadding=PaddingValues(horizontal=12.dp,vertical=2.dp),modifier=Modifier.height(34.dp)){Icon(Icons.Outlined.Psychology,null,Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text(if(reasoningExpanded)"收起思考" else if(msg.loading)"正在思考" else "查看思考",fontSize=11.sp)};AnimatedVisibility(reasoningExpanded){Column(Modifier.padding(horizontal=13.dp)){Text(msg.thinking,fontSize=11.sp,lineHeight=17.sp,color=if(mine)mineText.copy(alpha=.76f)else aiText.copy(alpha=.76f));HorizontalDivider(Modifier.padding(top=8.dp),color=(if(mine)mineText else aiText).copy(alpha=.15f))}}}
            if(msg.messageType=="red_packet")RedPacketBubble(msg.meta,msg.body)
            else if(msg.messageType=="image_request")ImageRequestBubble(msg.meta,msg.loading,onImageRequest,onCancelImageRequest)
            else if(msg.messageType=="moment_share")MomentShareBubble(msg)
            else {
              if(msg.image.isNotBlank())MessageImage(msg.image,modifier=if(msg.messageType=="sticker"&&msg.body.isBlank())Modifier.size(112.dp).padding(2.dp)else if(msg.messageType=="sticker")Modifier.padding(start=14.dp,top=10.dp,end=14.dp).size(72.dp)else Modifier.widthIn(max=260.dp).heightIn(max=300.dp).padding(5.dp).clip(RoundedCornerShape(12.dp)),contentScale=if(msg.messageType=="sticker")ContentScale.Fit else ContentScale.Crop)
              if(!mine&&msg.displayMode==1&&msg.body.isNotBlank())VoiceReplyBubble(msg.body,isSpeakingText(msg.body)){toggleSpeech(msg.body)}
              if(msg.body.isNotBlank()&&msg.displayMode!=1){if(frontendContent&&!msg.failed&&!msg.loading)HtmlMessage(renderTavernFrontend(msg.body,speaker,chat),chat.customCss,chat.richContentEnabled)else if(!mine&&chat.richContentEnabled&&!msg.failed&&!msg.loading)HtmlMessage(msg.body,chat.customCss,true)else {val font=prefs.getInt("chat_font_size",15).coerceIn(12,20);val lines=prefs.getInt("chat_line_height",22).coerceIn(font+2,32);val color=if(msg.failed)MaterialTheme.colorScheme.onErrorContainer else if(mine)mineText else aiText;if(chat.markdownEnabled)MarkdownMessage(msg.body,Modifier.padding(horizontal=if(noBubbles)4.dp else 14.dp,vertical=verticalPadding),font,lines,color)else Text(msg.body,Modifier.padding(horizontal=if(noBubbles)4.dp else 14.dp,vertical=verticalPadding),fontSize=font.sp,lineHeight=lines.sp,color=color)}}
            }
          }
        }
          if(!mine&&!msg.loading&&!msg.failed&&(msg.body.isNotBlank()||msg.image.isNotBlank()))Text("内容由 AI 生成",Modifier.padding(start=4.dp,top=2.dp,bottom=1.dp),fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.72f))
          if(!mine&&(msg.branchLoading||msg.alternatives.size>1))Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.82f),modifier=Modifier.padding(top=3.dp)){if(msg.branchLoading)Row(Modifier.height(32.dp).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp);Text("正在生成备选版本",Modifier.padding(start=7.dp),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}else Row(Modifier.height(32.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={msg.selectAlternative(msg.activeAlternative-1);save();uiRevision++},enabled=msg.activeAlternative>0,modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.ChevronLeft,"上一个回复版本",Modifier.size(17.dp))};Text("${msg.activeAlternative+1} / ${msg.alternatives.size}",Modifier.width(38.dp),fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant);IconButton(onClick={msg.selectAlternative(msg.activeAlternative+1);save();uiRevision++},enabled=msg.activeAlternative<msg.alternatives.lastIndex,modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.ChevronRight,"下一个回复版本",Modifier.size(17.dp))};IconButton(onClick={msg.keepCurrentAlternative();save();uiRevision++;toast("已保留当前回复版本")},modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.Done,"只保留当前版本",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary)}}}
          if(!mine&&msg.failed&&msg.errorDetail.isNotBlank())TextButton(onClick=onErrorDetails,modifier=Modifier.height(30.dp),contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)){Icon(Icons.Outlined.BugReport,null,Modifier.size(14.dp));Spacer(Modifier.width(4.dp));Text("查看详情",fontSize=11.sp)}
          if(!mine){Row(Modifier.widthIn(max=bubbleMax).height(30.dp),verticalAlignment=Alignment.CenterVertically){if(msg.body.isNotBlank()){IconButton(onClick={toggleSpeech(msg.body)},modifier=Modifier.size(30.dp)){Icon(if(isSpeakingText(msg.body))Icons.Filled.Stop else Icons.Outlined.VolumeUp,if(isSpeakingText(msg.body))"停止朗读" else "朗读",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.secondary)};IconButton(onClick=onCopy,modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.ContentCopy,"复制",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}};if(onSuggestions!=null)IconButton(onClick=onSuggestions,enabled=!suggestionsLoading,modifier=Modifier.size(30.dp)){if(suggestionsLoading)CircularProgressIndicator(Modifier.size(15.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.AutoAwesome,"推荐回复",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.secondary)};if(msg.failed)IconButton(onClick=onRetry,modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Refresh,"重试",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.error)};IconButton(onClick={onLongPress(msg)},modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.MoreHoriz,"更多操作",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        }
        if(mine&&!hideAvatars){Spacer(Modifier.width(8.dp));Box(Modifier.padding(top=2.dp)){UserAvatar(38)}}
      }
    }
  }
  private fun shouldShowMessageTime(previous:Msg?,current:Msg):Boolean {
    if(current.createdAt<=0)return false
    return previous==null||previous.createdAt<=0||current.createdAt-previous.createdAt>=5*60*1000L
  }
  private fun isSameDay(first:Calendar,second:Calendar)=first.get(Calendar.ERA)==second.get(Calendar.ERA)&&first.get(Calendar.YEAR)==second.get(Calendar.YEAR)&&first.get(Calendar.DAY_OF_YEAR)==second.get(Calendar.DAY_OF_YEAR)
  private fun formatMessageTime(timestamp:Long):String {
    val value=Calendar.getInstance().apply{timeInMillis=timestamp}
    val now=Calendar.getInstance()
    val date=Date(timestamp)
    val clock=java.text.SimpleDateFormat("HH:mm",Locale.CHINA).format(date)
    if(isSameDay(value,now))return clock
    val yesterday=(now.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,-1)}
    if(isSameDay(value,yesterday))return "昨天 $clock"
    val sixDaysAgo=(now.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,-6)}
    if(timestamp>=sixDaysAgo.timeInMillis)return java.text.SimpleDateFormat("EEEE HH:mm",Locale.CHINA).format(date)
    return java.text.SimpleDateFormat(if(value.get(Calendar.YEAR)==now.get(Calendar.YEAR))"M月d日 HH:mm" else "yyyy年M月d日 HH:mm",Locale.CHINA).format(date)
  }
  @Composable private fun ReplySuggestionBar(suggestions:List<String>,onSelect:(String)->Unit){Row(Modifier.fillMaxWidth().padding(start=46.dp,top=1.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){suggestions.forEach{suggestion->SuggestionChip(onClick={onSelect(suggestion)},label={Text(suggestion,maxLines=1,overflow=TextOverflow.Ellipsis)})}}}
  @Composable private fun MessageTimeDivider(timestamp:Long){Box(Modifier.fillMaxWidth().padding(top=2.dp,bottom=9.dp),contentAlignment=Alignment.Center){Text(formatMessageTime(timestamp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.78f))}}
  @Composable private fun RedPacketBubble(meta:String,fallback:String){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};val amount=data.optString("amount","0.00");val greeting=data.optString("greeting",fallback).ifBlank{"恭喜发财，万事如意"};val claimed=data.optBoolean("claimed",false);Surface(shape=RoundedCornerShape(15.dp),color=if(claimed)Color(0xFFB66B60) else Color(0xFFE9634F),modifier=Modifier.widthIn(min=210.dp,max=250.dp)){Column{Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha=.2f)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.CardGiftcard,null,tint=Color.White)};Column(Modifier.padding(start=10.dp)){Text(greeting,color=Color.White,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("¥ $amount",color=Color.White.copy(alpha=.9f),fontSize=12.sp)}};HorizontalDivider(color=Color.White.copy(alpha=.22f));Text(if(claimed)"红包已领取" else "点击领取红包",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontSize=10.sp,color=Color.White.copy(alpha=.84f))}}}
  @Composable private fun ImageRequestBubble(prompt:String,running:Boolean,onConfirm:()->Unit,onCancel:()->Unit){Column(Modifier.widthIn(min=220.dp,max=280.dp).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.AutoFixHigh,null,tint=MaterialTheme.colorScheme.secondary);Text("角色想生成一张图片",Modifier.padding(start=8.dp),fontWeight=FontWeight.SemiBold,fontSize=14.sp)};Text(prompt,Modifier.padding(top=8.dp),fontSize=12.sp,lineHeight=18.sp,maxLines=4,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onCancel,enabled=!running,modifier=Modifier.weight(1f).height(40.dp),contentPadding=PaddingValues(horizontal=8.dp)){Text("取消",maxLines=1)};Button(onClick=onConfirm,enabled=!running,modifier=Modifier.weight(1f).height(40.dp),contentPadding=PaddingValues(horizontal=8.dp)){if(running)CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Image,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(if(running)"生成中" else "确认",maxLines=1)}}}}
  @Composable private fun MomentShareBubble(msg:Msg){val data=remember(msg.meta){runCatching{JSONObject(msg.meta)}.getOrElse{JSONObject()}};Column(Modifier.widthIn(min=220.dp,max=300.dp).padding(13.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Collections,null,Modifier.size(18.dp));Text("分享了 ${data.optString("authorName","一条朋友圈")}",Modifier.padding(start=7.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold)};if(msg.body.isNotBlank())Text(msg.body,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=20.sp,maxLines=6,overflow=TextOverflow.Ellipsis);if(msg.image.isNotBlank())MessageImage(msg.image,Modifier.fillMaxWidth().padding(top=9.dp).aspectRatio(1.5f).clip(RoundedCornerShape(10.dp)))}}
  @Composable private fun ChatToolAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit){Column(Modifier.fillMaxWidth().height(80.dp).clickable(onClick=onClick),horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.size(58.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(icon,label,Modifier.size(25.dp),tint=MaterialTheme.colorScheme.secondary)}};Box(Modifier.fillMaxWidth().height(22.dp),contentAlignment=Alignment.BottomCenter){Text(label,fontSize=12.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}}
  @Composable private fun ChatSettingSwitch(title:String,subtitle:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=14.sp,fontWeight=FontWeight.Medium);Text(subtitle,fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked,onChange)}}
  @Composable private fun MarkdownMessage(source:String,modifier:Modifier,font:Int,lineHeight:Int,color:Color){Text(markdownAnnotated(source,color,font),modifier,fontSize=font.sp,lineHeight=lineHeight.sp,color=color)}
  private fun markdownAnnotated(source:String,color:Color,font:Int):AnnotatedString=buildAnnotatedString{
    fun inline(value:String){var index=0;while(index<value.length){when{value.startsWith("**",index)->{val end=value.indexOf("**",index+2);if(end>index+2){withStyle(SpanStyle(fontWeight=FontWeight.Bold)){append(value.substring(index+2,end))};index=end+2}else{append(value[index]);index++}};value[index]=='`'->{val end=value.indexOf('`',index+1);if(end>index+1){withStyle(SpanStyle(fontFamily=FontFamily.Monospace,background=color.copy(alpha=.1f))){append(value.substring(index+1,end))};index=end+1}else{append(value[index]);index++}};value.startsWith("~~",index)->{val end=value.indexOf("~~",index+2);if(end>index+2){withStyle(SpanStyle(textDecoration=TextDecoration.LineThrough)){append(value.substring(index+2,end))};index=end+2}else{append(value[index]);index++}};value[index]=='*'->{val end=value.indexOf('*',index+1);if(end>index+1){withStyle(SpanStyle(fontStyle=FontStyle.Italic)){append(value.substring(index+1,end))};index=end+1}else{append(value[index]);index++}};value[index]=='['->{val close=value.indexOf(']',index+1);val open=if(close>0&&close+1<value.length&&value[close+1]=='(')close+1 else -1;val end=if(open>0)value.indexOf(')',open+1)else -1;if(close>index&&end>open){withStyle(SpanStyle(textDecoration=TextDecoration.Underline)){append(value.substring(index+1,close))};index=end+1}else{append(value[index]);index++}};else->{append(value[index]);index++}}}}
    source.lines().forEachIndexed{lineIndex,raw->if(lineIndex>0)append('\n');val trimmed=raw.trimStart();when{trimmed.startsWith("### ")->{withStyle(SpanStyle(fontWeight=FontWeight.SemiBold,fontSize=(font+1).sp)){inline(trimmed.removePrefix("### "))}};trimmed.startsWith("## ")->{withStyle(SpanStyle(fontWeight=FontWeight.Bold,fontSize=(font+2).sp)){inline(trimmed.removePrefix("## "))}};trimmed.startsWith("# ")->{withStyle(SpanStyle(fontWeight=FontWeight.Bold,fontSize=(font+4).sp)){inline(trimmed.removePrefix("# "))}};trimmed.startsWith("- ")||trimmed.startsWith("* ")->{append("• ");inline(trimmed.drop(2))};else->inline(raw)}}
  }
  @Composable private fun VoiceReplyBubble(text:String,playing:Boolean,onClick:()->Unit){Row(Modifier.widthIn(min=150.dp,max=245.dp).height(54.dp).clickable(onClick=onClick).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),contentAlignment=Alignment.Center){Icon(if(playing)Icons.Filled.Stop else Icons.Filled.PlayArrow,if(playing)"停止" else "播放",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSecondaryContainer)};Row(Modifier.weight(1f).padding(horizontal=11.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){listOf(12,22,16,27,19).forEachIndexed{i,h->Box(Modifier.width(3.dp).height((if(playing&&i%2==0)h+4 else h).dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))}};Text("${(text.length/4).coerceIn(1,60)}\"",fontSize=11.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  @Composable private fun MessageImage(uri:String,modifier:Modifier=Modifier.widthIn(max=260.dp).heightIn(max=300.dp).clip(RoundedCornerShape(12.dp)),contentScale:ContentScale=ContentScale.Crop){val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[uri],uri){if(uri.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[uri]?:runCatching{if(uri.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(uri))?.use{BitmapFactory.decodeStream(it)}else BitmapFactory.decodeFile(uri)}.getOrNull()?.also{bitmapCache[uri]=it}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"聊天图片",modifier,contentScale=contentScale)}
  @Composable private fun MomentImage(reference:String,modifier:Modifier=Modifier){val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[reference],reference){if(reference.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[reference]?:runCatching{if(reference.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(reference))?.use{BitmapFactory.decodeStream(it)}else BitmapFactory.decodeFile(reference)}.getOrNull()?.also{bitmapCache[reference]=it}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"朋友圈图片",modifier,contentScale=ContentScale.Crop)}
  @OptIn(ExperimentalFoundationApi::class)
  @Composable private fun MomentPostCard(post:MomentPost,role:Card?,onForward:()->Unit={}){
    val commentCount=momentComments(post.id).size
    val likes=momentLikes(post.id)
    val likedByUser=likes.contains("user")
    val likeNames=likes.mapNotNull{id->if(id=="user")prefs.getString("user_name","我")?:"我" else find(id)?.name}.joinToString("、")
    Surface(modifier=Modifier.combinedClickable(onClick={},onLongClick=onForward),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){if(role!=null)RoleAvatar(role,42)else UserAvatar(42);Column(Modifier.weight(1f).padding(start=11.dp)){Text(post.authorName,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(time(post.createdAt),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={askDanger("删除朋友圈动态","动态、图片、点赞和它下面的全部评论都会被永久删除。"){saveMoments(moments().filterNot{it.id==post.id});saveMomentComments(post.id,emptyList());saveMomentLikes(post.id,emptySet());post.image.takeIf{it.isNotBlank()}?.let{path->runCatching{File(path).delete()}};uiRevision++;toast("动态已删除")}}){Icon(Icons.Outlined.Delete,"删除动态",tint=MaterialTheme.colorScheme.onSurfaceVariant)}}
      if(post.content.isNotBlank())Text(post.content,Modifier.padding(top=10.dp),fontSize=15.sp,lineHeight=22.sp)
      if(post.image.isNotBlank())MomentImage(post.image,Modifier.fillMaxWidth().padding(top=10.dp).aspectRatio(1.45f).clip(RoundedCornerShape(12.dp)))
      Row(Modifier.fillMaxWidth().padding(top=4.dp),horizontalArrangement=Arrangement.End){TextButton(onClick={toggleMomentLike(post.id)},contentPadding=PaddingValues(horizontal=10.dp)){Icon(if(likedByUser)Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,null,Modifier.size(17.dp),tint=if(likedByUser)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.width(5.dp));Text(if(likes.isEmpty())"点赞" else "${likes.size}",maxLines=1)};TextButton(onClick={selectedMomentId=post.id;composePage=Page.MOMENT_COMMENTS},contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Outlined.ChatBubbleOutline,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(if(commentCount==0)"评论" else "$commentCount",maxLines=1)}}
      if(likeNames.isNotBlank())Surface(shape=RoundedCornerShape(9.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.Favorite,null,Modifier.size(14.dp),tint=MaterialTheme.colorScheme.primary);Text(likeNames,Modifier.padding(start=6.dp),fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
    }}
  }
  private fun looksLikeTavernFrontend(source:String):Boolean{val value=source.lowercase();return value.contains("<html")||value.contains("<style")||value.contains("<script")||value.contains("<div")&&value.contains("</div>")||value.contains("```html")}
  private fun renderTavernFrontend(source:String,card:Card,chat:Chat):String{
    val user=prefs.getString("user_name","我").orEmpty().ifBlank{"我"}
    val lastUser=chat.messages.asReversed().firstOrNull{it.role=="user"&&!it.loading}?.body.orEmpty()
    val values=linkedMapOf(
      "char" to card.name,"character" to card.name,"user" to user,
      "description" to card.desc,"personality" to card.persona,"persona" to card.persona,
      "scenario" to card.persona,"mesExamples" to card.exampleMessages,
      "system" to card.systemPrompt,"lastMessage" to lastUser
    )
    var rendered=source
    values.forEach{(key,value)->rendered=rendered.replace(Regex("\\{\\{\\s*${Regex.escape(key)}\\s*\\}\\}",RegexOption.IGNORE_CASE),TextUtils.htmlEncode(value))}
    rendered=rendered.replace("<USER>",TextUtils.htmlEncode(user),ignoreCase=true).replace("<BOT>",TextUtils.htmlEncode(card.name),ignoreCase=true)
    val context=JSONObject().put("character",card.name).put("user",user).put("description",card.desc).put("personality",card.persona).put("scenario",card.persona).put("lastMessage",lastUser).toString().replace("</","<\\/")
    val bridge="""<script>(function(){const c=Object.freeze($context);window.SHIYU_CONTEXT=c;window.TAVO_CONTEXT=c;window.SillyTavern=window.SillyTavern||{};if(typeof window.SillyTavern.getContext!=='function')window.SillyTavern.getContext=function(){return c};if(typeof window.getContext!=='function')window.getContext=function(){return c};})();</script>"""
    val headTag=Regex("<head[^>]*>",RegexOption.IGNORE_CASE).find(rendered)?.value
    if(headTag!=null)return rendered.replaceFirst(headTag,headTag+bridge)
    val htmlTag=Regex("<html[^>]*>",RegexOption.IGNORE_CASE).find(rendered)?.value
    return if(htmlTag!=null)rendered.replaceFirst(htmlTag,htmlTag+"<head>$bridge</head>") else bridge+rendered
  }
  @Composable private fun HtmlMessage(source:String,css:String,allowJavaScript:Boolean=false){
    var webHeight by remember{mutableIntStateOf(64)}
    val dark=MaterialTheme.colorScheme.background.luminance()<0.5f;val font=prefs.getInt("chat_font_size",15).coerceIn(12,20);val lines=prefs.getInt("chat_line_height",22).coerceIn(font+2,32)
    val raw=source.trim().replace(Regex("^```(?:html|css)?\\s*",RegexOption.IGNORE_CASE),"").replace(Regex("\\s*```$"),"").trim()
    val csp=if(allowJavaScript)"default-src 'none'; img-src data: blob:; media-src data: blob:; style-src 'unsafe-inline'; script-src 'unsafe-inline'" else "default-src 'none'; img-src data: blob:; media-src data: blob:; style-src 'unsafe-inline'; script-src 'none'"
    val baseStyle="""html,body{margin:0;padding:0;background:transparent;color:${if(dark)"#e8e8e8" else "#242825"};font-family:sans-serif;font-size:${font}px;line-height:${lines.toFloat()/font};overflow-wrap:anywhere;max-width:100%}img,video{max-width:100%;height:auto;border-radius:8px}$css"""
    val content=if(raw.contains("<",ignoreCase=true))raw else TextUtils.htmlEncode(raw).replace("\n","<br>")
    val head="""<meta http-equiv="Content-Security-Policy" content="$csp"><meta name="viewport" content="width=device-width,initial-scale=1"><style>$baseStyle</style>"""
    val html=if(raw.contains("<html",ignoreCase=true)){
      when{
        raw.contains("</head>",ignoreCase=true)->raw.replaceFirst(Regex("</head>",RegexOption.IGNORE_CASE),"$head</head>")
        raw.contains("<body",ignoreCase=true)->raw.replaceFirst(Regex("<body",RegexOption.IGNORE_CASE),"<head>$head</head><body")
        else->{val tag=Regex("<html[^>]*>",RegexOption.IGNORE_CASE).find(raw)?.value.orEmpty();if(tag.isEmpty())"<html><head>$head</head><body>$raw</body></html>" else raw.replaceFirst(tag,tag+"<head>$head</head>")}
      }
    }else """<!doctype html><html><head>$head</head><body>$content</body></html>"""
    val signature=remember(html,allowJavaScript){31*html.hashCode()+allowJavaScript.hashCode()}
    var activeWebView by remember{mutableStateOf<WebView?>(null)}
    DisposableEffect(Unit){onDispose{activeWebView?.let{view->view.stopLoading();view.loadUrl("about:blank");view.removeAllViews();view.destroy()};activeWebView=null}}
    AndroidView(factory={context->WebView(context).apply{
      activeWebView=this
      setBackgroundColor(AndroidColor.TRANSPARENT)
      settings.allowFileAccess=false;settings.allowContentAccess=false;settings.blockNetworkLoads=true;settings.domStorageEnabled=false;settings.databaseEnabled=false;settings.javaScriptEnabled=allowJavaScript;settings.javaScriptCanOpenWindowsAutomatically=false;settings.setSupportMultipleWindows(false);isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false;isNestedScrollingEnabled=true
      if(android.os.Build.VERSION.SDK_INT>=16){settings.allowFileAccessFromFileURLs=false;settings.allowUniversalAccessFromFileURLs=false}
      if(android.os.Build.VERSION.SDK_INT>=21)settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
      if(android.os.Build.VERSION.SDK_INT>=26)settings.safeBrowsingEnabled=true
      webViewClient=object:WebViewClient(){
        override fun shouldOverrideUrlLoading(view:WebView,request:android.webkit.WebResourceRequest)=true
        override fun shouldInterceptRequest(view:WebView,request:android.webkit.WebResourceRequest):android.webkit.WebResourceResponse?{val scheme=request.url.scheme?.lowercase();return if(scheme !in listOf("data","about","blob"))android.webkit.WebResourceResponse("text/plain","UTF-8",java.io.ByteArrayInputStream(ByteArray(0)))else null}
        override fun onPageFinished(view:WebView,url:String?){listOf(40L,220L,700L,1500L,3000L).forEach{wait->view.postDelayed({if(activeWebView===view){
          if(allowJavaScript)view.evaluateJavascript("Math.max(document.body?.scrollHeight||0,document.documentElement?.scrollHeight||0)"){value->if(activeWebView===view){val measured=(value.trim('"').toDoubleOrNull()?:0.0).toInt().coerceIn(40,12000);if(measured>40&&measured!=webHeight)webHeight=measured}}
          else {val density=view.resources.displayMetrics.density;val measured=(view.contentHeight*view.scale/density).toInt().coerceIn(40,12000);if(measured!=webHeight)webHeight=measured}
        }},wait)}}
      }
    }},modifier=Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp).height(webHeight.dp),update={view->view.settings.javaScriptEnabled=allowJavaScript;if(view.tag!=signature){view.tag=signature;view.loadDataWithBaseURL("about:blank",html,"text/html","UTF-8",null)}})
  }

  @Composable private fun DetailScaffold(title:String,back:Page=Page.MAIN,onBack:(()->Unit)?=null,content:LazyListScope.()->Unit){Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={onBack?.invoke()?:run{composePage=back}}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")};Text(title,fontSize=19.sp,fontWeight=FontWeight.SemiBold)};LazyColumn(contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
  @Composable private fun CircleAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit){FilledIconButton(onClick=onClick,modifier=Modifier.size(40.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Icon(icon,label,Modifier.size(20.dp))}}
  @Composable private fun SectionTitle(text:String){Text(text,fontSize=14.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(start=20.dp,top=18.dp,bottom=6.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  @Composable private fun Avatar(name:String,size:Int){Box(Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text(name.take(1),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)}}
  @Composable private fun RoleAvatar(card:Card,size:Int){val path=card.avatar.orEmpty();val name=card.name.orEmpty().ifBlank{"角色"};val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[path],path){if(path.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[path]?:BitmapFactory.decodeFile(path)?.also{bitmapCache[path]=it}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),name,Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)else Avatar(name,size)}
  @Composable private fun UserAvatar(size:Int){val path=prefs.getString("user_avatar","") ?: "";val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[path],path){if(path.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[path]?:runCatching{if(path.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(path))?.use{BitmapFactory.decodeStream(it)}else BitmapFactory.decodeFile(path)}.getOrNull()?.also{bitmapCache[path]=it}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"用户头像",Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)else Avatar(prefs.getString("user_name","我") ?: "我",size)}
  @Composable private fun EmptyState(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String){Column(Modifier.fillMaxWidth().padding(top=70.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.SemiBold,fontSize=17.sp);Spacer(Modifier.height(3.dp));Text(subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  @Composable private fun InfoBanner(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,text:String){Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.secondaryContainer){Row(Modifier.fillMaxWidth().padding(16.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.onSecondaryContainer);Column(Modifier.padding(start=12.dp)){Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(3.dp));Text(text,fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSecondaryContainer)}}}}
  @Composable private fun GuideStep(number:String,title:String,text:String){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text(number,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)};Column(Modifier.padding(start=12.dp)){Text(title,fontWeight=FontWeight.Medium);Text(text,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
  private data class MenuEntry(val icon:androidx.compose.ui.graphics.vector.ImageVector,val title:String,val subtitle:String,val action:()->Unit)
  @Composable private fun MenuGroup(title:String,entries:List<MenuEntry>){Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=4.dp,bottom=2.dp));entries.forEach{e->SettingButton(e.icon,e.title,e.subtitle,e.action)}}}
  @Composable private fun ProfileCard(onClick:()->Unit){Surface(onClick=onClick,modifier=Modifier.padding(horizontal=16.dp),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){UserAvatar(58);Column(Modifier.weight(1f).padding(start=14.dp)){Text(prefs.getString("user_name","我")?:"我",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("你的角色、记忆与故事",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}
  @Composable private fun StyledField(value:String,onChange:(String)->Unit,label:String){OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,shape=RoundedCornerShape(13.dp))}
  @Composable private fun PreviewText(label:String,value:String,minLines:Int){Column(Modifier.fillMaxWidth()){Text(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=12.dp,bottom=5.dp));Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Box(Modifier.fillMaxWidth().heightIn(min=(minLines.coerceAtMost(4)*22+24).dp,max=170.dp).verticalScroll(rememberScrollState()).padding(12.dp)){Text(value.ifBlank{"（空）"},fontSize=14.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurface)}}}}
  @Composable private fun ModelListCard(models:List<String>,selected:String,onSelect:(String)->Unit){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.fillMaxWidth().heightIn(max=260.dp).verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){models.forEach{name->OutlinedButton(onClick={onSelect(name)},Modifier.fillMaxWidth().height(42.dp)){Text(name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);if(selected==name)Icon(Icons.Filled.Check,null)}}}}}
  private fun storedColor(value:String,fallback:Color)=if(value.isBlank())fallback else runCatching{Color(AndroidColor.parseColor(value))}.getOrDefault(fallback)
  private fun normalizeStoredColor(value:String):String?{
    val raw=value.trim()
    if(raw.isEmpty())return ""
    val prefixed=if(raw.startsWith('#'))raw else "#$raw"
    if(!Regex("^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$").matches(prefixed))return null
    return if(prefixed.length==7)"#FF${prefixed.substring(1).uppercase(Locale.ROOT)}" else "#${prefixed.substring(1).uppercase(Locale.ROOT)}"
  }
  @Composable private fun ColorHexInput(label:String,value:String,onChange:(String)->Unit){
    val normalized=normalizeStoredColor(value)
    OutlinedTextField(value=value,onValueChange={onChange(it.trim().take(9))},modifier=Modifier.fillMaxWidth(),label={Text(label)},placeholder={Text("#RRGGBB")},singleLine=true,isError=normalized==null,leadingIcon={Box(Modifier.size(22.dp).clip(CircleShape).background(normalized?.takeIf{it.isNotBlank()}?.let{storedColor(it,MaterialTheme.colorScheme.surfaceVariant)}?:MaterialTheme.colorScheme.surfaceVariant).border(1.dp,MaterialTheme.colorScheme.outline,CircleShape))},supportingText=if(normalized==null){{Text("输入 6 位 RGB 或 8 位 ARGB")}}else null,shape=RoundedCornerShape(13.dp))
  }
  @Composable private fun ColorSwatchSelector(title:String,selected:String,options:List<Pair<String,String>>,onSelect:(String)->Unit){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Text(title,fontWeight=FontWeight.Medium);Row(Modifier.fillMaxWidth().padding(top=10.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){options.forEach{option->Column(Modifier.width(54.dp).clickable{onSelect(option.first)},horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=CircleShape,color=storedColor(option.first,MaterialTheme.colorScheme.surfaceVariant),border=androidx.compose.foundation.BorderStroke(if(selected==option.first)3.dp else 1.dp,if(selected==option.first)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),modifier=Modifier.size(38.dp)){};Text(option.second,fontSize=10.sp,maxLines=1,softWrap=false,modifier=Modifier.padding(top=4.dp),color=if(selected==option.first)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}
  @Composable private fun SettingButton(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){FilledTonalButton(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=15.dp,vertical=10.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(horizontal=12.dp),horizontalAlignment=Alignment.Start){Text(title,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis);Text(subtitle,fontSize=12.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}
  @Composable private fun StatCard(label:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=MaterialTheme.colorScheme.secondary);Text(label,Modifier.weight(1f).padding(start=12.dp));Text(value,fontSize=22.sp,fontWeight=FontWeight.Bold)}}}
  private fun compactToken(value:Long)=when{value>=1_000_000->String.format(Locale.ROOT,"%.1fM",value/1_000_000.0);value>=1_000->String.format(Locale.ROOT,"%.1fK",value/1_000.0);else->value.toString()}
  @Composable private fun ChatTokenSummary(input:Long,output:Long){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().height(78.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){listOf("输入" to input,"输出" to output,"累计" to (input+output)).forEachIndexed{index,value->if(index>0)VerticalDivider(Modifier.height(34.dp));Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text(value.first,fontSize=11.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(compactToken(value.second),fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}}}}
  @Composable private fun TokenStatCard(label:String,value:Long,icon:androidx.compose.ui.graphics.vector.ImageVector){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().height(92.dp).padding(13.dp),verticalArrangement=Arrangement.SpaceBetween){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.secondary);Text(label,Modifier.padding(start=7.dp),fontSize=12.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(String.format(Locale.getDefault(),"%,d",value),Modifier.fillMaxWidth(),fontSize=20.sp,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}}
}

private val ShiYuLight=lightColorScheme(primary=Color(0xFFC85C6F),onPrimary=Color.White,primaryContainer=Color(0xFFFFD9DE),onPrimaryContainer=Color(0xFF6C2634),secondary=Color(0xFF2F8073),onSecondary=Color.White,secondaryContainer=Color(0xFFD4EFE9),onSecondaryContainer=Color(0xFF164B43),background=Color(0xFFF7F8F4),surface=Color(0xFFFFFFFF),surfaceVariant=Color(0xFFEDF1EC),onSurface=Color(0xFF242825),onSurfaceVariant=Color(0xFF68706A),outline=Color(0xFFD8DED8))
private val ShiYuDark=darkColorScheme(primary=Color(0xFFD98290),onPrimary=Color(0xFF2B1015),primaryContainer=Color(0xFF4A3035),onPrimaryContainer=Color(0xFFF4D8DC),secondary=Color(0xFFA9BDB8),onSecondary=Color(0xFF101715),secondaryContainer=Color(0xFF343B39),onSecondaryContainer=Color(0xFFE3EAE8),background=Color(0xFF121212),onBackground=Color(0xFFF0F0F1),surface=Color(0xFF1D1D1F),onSurface=Color(0xFFF0F0F1),surfaceVariant=Color(0xFF29292C),onSurfaceVariant=Color(0xFFC2C2C6),outline=Color(0xFF57575C),error=Color(0xFFFFB4AB),onError=Color(0xFF690005))
private val QingLanLight=lightColorScheme(primary=Color(0xFF24776F),onPrimary=Color.White,primaryContainer=Color(0xFFCBECE7),onPrimaryContainer=Color(0xFF0D403B),secondary=Color(0xFF526B82),onSecondary=Color.White,secondaryContainer=Color(0xFFDCE7F0),onSecondaryContainer=Color(0xFF293E52),background=Color(0xFFF5F8F7),surface=Color.White,surfaceVariant=Color(0xFFE8F0EE),onSurface=Color(0xFF202826),onSurfaceVariant=Color(0xFF5D6E6A),outline=Color(0xFFCAD7D4))
private val QingLanDark=darkColorScheme(primary=Color(0xFF7FCBC0),onPrimary=Color(0xFF073A35),primaryContainer=Color(0xFF24534E),onPrimaryContainer=Color(0xFFD0F2ED),secondary=Color(0xFFAFC8DE),onSecondary=Color(0xFF1D3447),secondaryContainer=Color(0xFF354A5B),onSecondaryContainer=Color(0xFFDCEAF4),background=Color(0xFF101514),surface=Color(0xFF1A201F),surfaceVariant=Color(0xFF26302E),onSurface=Color(0xFFE8EFED),onSurfaceVariant=Color(0xFFBCC9C6),outline=Color(0xFF53625F))
private val YingWuLight=lightColorScheme(primary=Color(0xFFB65370),onPrimary=Color.White,primaryContainer=Color(0xFFFFD9E3),onPrimaryContainer=Color(0xFF682338),secondary=Color(0xFF68758A),onSecondary=Color.White,secondaryContainer=Color(0xFFE4E8F0),onSecondaryContainer=Color(0xFF354154),background=Color(0xFFFAF6F7),surface=Color.White,surfaceVariant=Color(0xFFF2E9EC),onSurface=Color(0xFF2A2426),onSurfaceVariant=Color(0xFF71666A),outline=Color(0xFFE1D4D8))
private val YingWuDark=darkColorScheme(primary=Color(0xFFE18AA2),onPrimary=Color(0xFF421321),primaryContainer=Color(0xFF57313C),onPrimaryContainer=Color(0xFFFFD9E3),secondary=Color(0xFFBBC6D8),onSecondary=Color(0xFF273345),secondaryContainer=Color(0xFF3A4352),onSecondaryContainer=Color(0xFFE5EAF2),background=Color(0xFF141112),surface=Color(0xFF201B1D),surfaceVariant=Color(0xFF30272A),onSurface=Color(0xFFF3ECEE),onSurfaceVariant=Color(0xFFCBBFC3),outline=Color(0xFF62565A))
private val JianMoLight=lightColorScheme(primary=Color(0xFF27766C),onPrimary=Color.White,primaryContainer=Color(0xFFD5ECE8),onPrimaryContainer=Color(0xFF173E39),secondary=Color(0xFF5D6268),onSecondary=Color.White,secondaryContainer=Color(0xFFE5E7E9),onSecondaryContainer=Color(0xFF33373B),background=Color(0xFFF6F6F5),surface=Color.White,surfaceVariant=Color(0xFFECEDEC),onSurface=Color(0xFF202120),onSurfaceVariant=Color(0xFF646764),outline=Color(0xFFD2D4D2))
private val JianMoDark=darkColorScheme(primary=Color(0xFF82C9BF),onPrimary=Color(0xFF063B34),primaryContainer=Color(0xFF294B47),onPrimaryContainer=Color(0xFFD5EFEB),secondary=Color(0xFFC1C4C7),onSecondary=Color(0xFF2C3033),secondaryContainer=Color(0xFF3B3E41),onSecondaryContainer=Color(0xFFE7E8E9),background=Color(0xFF101010),surface=Color(0xFF1B1B1C),surfaceVariant=Color(0xFF28282A),onSurface=Color(0xFFEDEDEE),onSurfaceVariant=Color(0xFFC1C1C4),outline=Color(0xFF565659))
private fun themePalette(style:Int,dark:Boolean)=when(style){1->if(dark)QingLanDark else QingLanLight;2->if(dark)YingWuDark else YingWuLight;3->if(dark)JianMoDark else JianMoLight;else->if(dark)ShiYuDark else ShiYuLight}
private fun glassPalette(base:ColorScheme,opacity:Int):ColorScheme{
  val alpha=opacity.coerceIn(35,95)/100f
  return base.copy(
    surface=base.surface.copy(alpha=alpha),
    surfaceVariant=base.surfaceVariant.copy(alpha=(alpha*.9f).coerceIn(.3f,.9f)),
    primaryContainer=base.primaryContainer.copy(alpha=(alpha*.88f).coerceIn(.3f,.9f)),
    secondaryContainer=base.secondaryContainer.copy(alpha=(alpha*.86f).coerceIn(.3f,.9f))
  )
}

