package com.tniany.shiyu

import android.app.Activity
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
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
import kotlinx.coroutines.isActive

private enum class Page { ADULT_CONFIRM, ONBOARDING, STARTUP_LOADING, COMMUNITY_INVITE, FEEDBACK, TERMS, DATA_RESCUE, MAIN, CHAT, CHAT_SEARCH, CHAT_DETAIL, CHAT_MODEL, CHAT_IDENTITY, CHAT_CONTENT, CHAT_CONTEXT, CHAT_TTS, CHAT_BACKGROUND, MEMORY_SUMMARY, PLOT_NODES, DIARIES, PROACTIVE, YANDERE, MOMENT_SHARE, CHAT_DEFAULTS, COMMON_RULES, STORIES, STORY_EDIT, GENERATION_TASKS, ROLE_SETTINGS, ROLE_PROFILE, ROLE_SESSIONS, MUSIC, TOGETHER_LISTEN, WORLD_GAMES, WORLD_ACTIVITY, WORLD_HOME_3D, WORLD_WARDROBE, MEMORY, RENAME_CHAT, CREATE_ROLE, GROUP_SETUP, CARD_CENTER, CARD_EXPORT, IMPORT_PREVIEW, MOMENTS, MOMENT_PERMISSIONS, MOMENT_ROLE_POST, MOMENT_COMMENTS, API, API_ERROR, PROVIDER, ADD_PROVIDER, CONNECTION, MODEL, LOCAL_MODELS, GENERATION, AUXILIARY_MODELS, MCP_TOOLS, WEB_SEARCH, EMBEDDING_LIBRARY, VIDEO_GENERATION, IMAGE_GENERATION, TTS, APPEARANCE, CHAT_APPEARANCE, FRAME_RATE, DATA, BACKUP, STICKER_RESOURCES, REALITY_AWARENESS, EXPERIMENTAL, DESKTOP_PET, WEIXIN_LINK, QQ_BOT, SETTINGS_SEARCH, GUIDE, LANGUAGE, ABOUT, UPDATE_HISTORY, SUPPORT, USER_PROFILE, IDENTITIES, APP_BEHAVIOR }
private enum class VoiceCallPhase { IDLE, READY, LISTENING, TRANSCRIBING, THINKING, SPEAKING, ERROR }
private val mainBottomBarClearance = 88.dp

class ComposeActivity : MainActivity() {
  private val adultConsentRevision="2026-07-14"
  private data class ProviderConfig(val id:String,val name:String,val endpoint:String,val modelName:String,val builtIn:Boolean=false)
  private data class ImageProviderConfig(val id:String,val name:String,val protocol:String,val endpoint:String,val model:String,val size:String,val negative:String="",val steps:Int=28,val scale:Int=7,val runtime:String="cpu")
  private data class IdentityConfig(val id:String,val name:String,val description:String,val birthday:String="",val pronouns:String="",val location:String="",val occupation:String="",val interests:String="",val bio:String="")
  private data class ImagePromptConfig(val positive:String,val negative:String,val style:String="natural")
  private data class WorldBookEntryView(val index:Int,val mapKey:String,val id:String,val title:String,val keywords:String,val content:String,val enabled:Boolean,val constant:Boolean,val order:Int)
  private data class WorldBookDocument(val root:Any,val entries:JSONArray,val mapKeys:List<String>?)
  private data class MomentPost(val id:String,val authorId:String,val authorName:String,val content:String,val image:String,val createdAt:Long)
  private data class MomentComment(val id:String,val authorId:String,val authorName:String,val content:String,val createdAt:Long,val parentId:String="",val replyToName:String="")
  private data class NeteaseSong(val id:Long,val title:String,val artist:String,val album:String,val durationMs:Long,val coverUrl:String="")
  private data class LyricLine(val timeMs:Long,val text:String)
  private data class NeteaseAccount(val userId:Long,val nickname:String,val avatarUrl:String)
  private data class NeteasePlaylist(val id:Long,val name:String,val coverUrl:String,val trackCount:Int)
  private data class RoleLlmState(val affection:Int?,val trust:Int?,val energy:Int?,val fatigue:Int?,val mood:Int?,val hobbies:List<String>,val summary:String,val generatedAt:Long)
  private data class WorldOutfit(val id:String,val name:String,val description:String,val primary:String,val secondary:String)
  private data class DangerAction(val title:String,val message:String,val confirmLabel:String="确认删除",val action:()->Unit)
  private data class NewbieGuideStep(val icon:androidx.compose.ui.graphics.vector.ImageVector,val title:String,val content:String,val points:List<String>,val actionLabel:String?=null,val page:Page?=null)
  private var composePage by mutableStateOf(Page.MAIN)
  private var selectedChat by mutableStateOf<Chat?>(null)
  private var uiRevision by mutableIntStateOf(0)
  private var chatUiRevision by mutableIntStateOf(0)
  private var editingProviderId by mutableStateOf<String?>(null)
  private var providerBackPage by mutableStateOf(Page.PROVIDER)
  private var imageGenerationBackPage by mutableStateOf(Page.API)
  private var videoGenerationBackPage by mutableStateOf(Page.API)
  private var embeddingBackPage by mutableStateOf(Page.API)
  private var webSearchBackPage by mutableStateOf(Page.API)
  private var apiErrorBackPage by mutableStateOf(Page.API)
  private var editingCardId by mutableStateOf<String?>(null)
  private var roleBackPage by mutableStateOf(Page.MAIN)
  private var roleProfileBackPage by mutableStateOf(Page.MAIN)
  private var roleProfileChatId by mutableStateOf<String?>(null)
  private var musicBackPage by mutableStateOf(Page.MAIN)
  private var togetherBackPage by mutableStateOf(Page.MUSIC)
  private var renameBackPage by mutableStateOf(Page.MAIN)
  private var termsBackPage by mutableStateOf(Page.ABOUT)
  private var ttsBackPage by mutableStateOf(Page.MAIN)
  private var stickerResourcesBackPage by mutableStateOf(Page.MAIN)
  private var pendingImportCard by mutableStateOf<Card?>(null)
  private val sharedCardImportQueue = java.util.ArrayDeque<android.net.Uri>()
  private var sharedCardImportRunning = false
  private var exportCardId by mutableStateOf<String?>(null)
  private var selectedMomentId by mutableStateOf("")
  private var momentRolePostBackPage by mutableStateOf(Page.MAIN)
  private var availableUpdateVersion by mutableStateOf("")
  private var availableUpdateUrl by mutableStateOf("")
  private var updateNoticeNonce by mutableIntStateOf(0)
  private var remoteAnnouncementJson by mutableStateOf("")
  private var storyTts:android.speech.tts.TextToSpeech?=null
  private var storyTtsReady=false
  private var storyGeneration=0
  private var storySegments=emptyList<String>()
  private var storySegmentIndex=0
  private var storyPlaybackId by mutableStateOf("")
  private var storyPlaybackTitle by mutableStateOf("")
  private var storyPlaying by mutableStateOf(false)
  private var storyEditingId by mutableStateOf<String?>(null)
  private var remoteAnnouncementNonce by mutableIntStateOf(0)
  private val remoteAnnouncementSessionSeen = mutableSetOf<String>()
  private var requestedMainTab by mutableIntStateOf(0)
  private var mainTabRequestNonce by mutableIntStateOf(0)
  private var pendingDanger by mutableStateOf<DangerAction?>(null)
  private var providerListBackPage by mutableStateOf(Page.API)
  private var pendingChatMessageId by mutableStateOf("")
  private var voiceCallSessionId by mutableStateOf("")
  private var voiceCallChatId by mutableStateOf("")
  private var voiceCallCardId by mutableStateOf("")
  private var voiceCallPhase by mutableStateOf(VoiceCallPhase.IDLE)
  private var voiceCallStatus by mutableStateOf("")
  private var voiceCallLastUserText by mutableStateOf("")
  private var voiceCallLastReplyText by mutableStateOf("")
  private var voiceCallStartedAt by mutableLongStateOf(0L)
  private var voiceCallMicMuted by mutableStateOf(false)
  private var voiceCallSpeakerEnabled by mutableStateOf(true)
  private var voiceCallFloating by mutableStateOf(false)
  private var voiceCallScreenSharing by mutableStateOf(false)
  private var voiceCallScreenImagePath by mutableStateOf("")
  private var voiceCallPendingCommand by mutableStateOf("")
  private lateinit var voiceCallScreenPermission:ActivityResultLauncher<Intent>
  private lateinit var gallerySavePermission:ActivityResultLauncher<String>
  private var gallerySavePendingReference=""
  private var voiceCallReceiver:android.content.BroadcastReceiver?=null
  private var musicReceiver:android.content.BroadcastReceiver?=null
  private var voiceCallAudioConfigured=false
  private var voiceCallPreviousAudioMode=AudioManager.MODE_NORMAL
  private var voiceCallPreviousSpeaker=false
  private var chatMusicPlayer:MediaPlayer?=null
  private var chatMusicGeneration=0
  private var musicLoadingUrl by mutableStateOf("")
  private var musicPlayingUrl by mutableStateOf("")
  private var musicPausedUrl by mutableStateOf("")
  private var musicCoverUrl by mutableStateOf("")
  private var musicResolvingSongId by mutableLongStateOf(0L)
  private var musicPositionMs by mutableLongStateOf(0L)
  private var musicDurationMs by mutableLongStateOf(0L)
  private var togetherChatId by mutableStateOf<String?>(null)
  private var togetherSong by mutableStateOf<NeteaseSong?>(null)
  private var togetherQueue by mutableStateOf<List<NeteaseSong>>(emptyList())
  private var musicOwnerChatId by mutableStateOf("")
  private var musicOwnerRoleId by mutableStateOf("")
  private var musicPlaybackId=""
  private var musicRequestGeneration=0
  private var autoAdvanceInFlight=false
  private val bitmapCache=object:android.util.LruCache<String,android.graphics.Bitmap>(64*1024){
    override fun sizeOf(key:String,value:android.graphics.Bitmap)=(value.byteCount/1024).coerceAtLeast(1)
  }
  private val animatedImageCache=java.util.concurrent.ConcurrentHashMap<String,Boolean>()
  private val immersiveReplyTasks=java.util.concurrent.ConcurrentHashMap<String,Runnable>()
  private val imageGenerationTaskIds=java.util.concurrent.ConcurrentHashMap<String,String>()

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
        isChatSending(chat.id)->{restartImmersiveGeneration(chat);request(chat,target)}
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

  private fun applyMusicState(intent:Intent?):Boolean{
    if(intent?.getStringExtra(NeteaseMusicService.EXTRA_PLAYBACK_ID).orEmpty()!=musicPlaybackId)return false
    val status=intent?.getStringExtra(NeteaseMusicService.EXTRA_STATUS).orEmpty();val url=intent?.getStringExtra(NeteaseMusicService.EXTRA_URL).orEmpty()
    musicOwnerChatId=intent?.getStringExtra(NeteaseMusicService.EXTRA_CHAT_ID).orEmpty()
    musicOwnerRoleId=intent?.getStringExtra(NeteaseMusicService.EXTRA_ROLE_ID).orEmpty()
    musicCoverUrl=intent?.getStringExtra(NeteaseMusicService.EXTRA_COVER_URL).orEmpty()
    musicPositionMs=intent?.getLongExtra(NeteaseMusicService.EXTRA_POSITION,0L)?:0L
    musicDurationMs=intent?.getLongExtra(NeteaseMusicService.EXTRA_DURATION,0L)?:0L
    when(status){NeteaseMusicService.STATUS_LOADING->{musicLoadingUrl=url;musicPlayingUrl="";musicPausedUrl=""};NeteaseMusicService.STATUS_PLAYING->{musicLoadingUrl="";musicPlayingUrl=url;musicPausedUrl=""};NeteaseMusicService.STATUS_PAUSED->{musicLoadingUrl="";musicPlayingUrl="";musicPausedUrl=url};else->{musicLoadingUrl="";musicPlayingUrl="";musicPausedUrl=""}}
    uiRevision++
    return true
  }
  private fun musicPlaybackState()=runCatching{JSONObject(prefs.getString("music_playback_state","{}").orEmpty())}.getOrElse{JSONObject()}
  private fun syncMusicStateFromPrefs(){
    val root=musicPlaybackState()
    musicPlaybackId=prefs.getString("music_requested_playback_id",root.optString(NeteaseMusicService.EXTRA_PLAYBACK_ID)).orEmpty()
    applyMusicState(Intent().putExtra(NeteaseMusicService.EXTRA_STATUS,root.optString(NeteaseMusicService.EXTRA_STATUS)).putExtra(NeteaseMusicService.EXTRA_URL,root.optString(NeteaseMusicService.EXTRA_URL)).putExtra(NeteaseMusicService.EXTRA_COVER_URL,root.optString(NeteaseMusicService.EXTRA_COVER_URL)).putExtra(NeteaseMusicService.EXTRA_POSITION,root.optLong(NeteaseMusicService.EXTRA_POSITION)).putExtra(NeteaseMusicService.EXTRA_DURATION,root.optLong(NeteaseMusicService.EXTRA_DURATION)).putExtra(NeteaseMusicService.EXTRA_CHAT_ID,root.optString(NeteaseMusicService.EXTRA_CHAT_ID)).putExtra(NeteaseMusicService.EXTRA_ROLE_ID,root.optString(NeteaseMusicService.EXTRA_ROLE_ID)).putExtra(NeteaseMusicService.EXTRA_PLAYBACK_ID,root.optString(NeteaseMusicService.EXTRA_PLAYBACK_ID)))
    listeningChat(musicOwnerChatId)?.takeIf{it.cardId==musicOwnerRoleId}?.let{loadTogetherSession(it)}
  }
  private fun stopChatMusic(){
    musicRequestGeneration++;autoAdvanceInFlight=false;musicPlaybackId=UUID.randomUUID().toString()
    prefs.edit().putString("music_requested_playback_id",musicPlaybackId).putString("music_playback_state","{}").apply()
    NeteaseMusicService.stop(this)
    musicOwnerChatId="";musicOwnerRoleId="";musicLoadingUrl="";musicPlayingUrl="";musicPausedUrl="";musicCoverUrl="";musicResolvingSongId=0L;musicPositionMs=0L;musicDurationMs=0L;uiRevision++
  }

  private fun appRecentSongs():List<NeteaseSong>{
    val values=runCatching{JSONArray(prefs.getString("music_recent_history","[]").orEmpty())}.getOrElse{JSONArray()}
    return buildList{for(i in 0 until values.length()){val item=values.optJSONObject(i)?:continue;val song=NeteaseSong(item.optLong("id"),item.optString("title"),item.optString("artist"),item.optString("album"),item.optLong("durationMs"),item.optString("coverUrl"));if(song.id>0)add(song)}}
  }

  private fun rememberAppRecent(song:NeteaseSong){
    if(song.id<=0)return
    val values=(listOf(song)+appRecentSongs()).distinctBy{it.id}.take(50)
    val array=JSONArray();values.forEach{array.put(JSONObject().put("id",it.id).put("title",it.title).put("artist",it.artist).put("album",it.album).put("durationMs",it.durationMs).put("coverUrl",it.coverUrl))}
    prefs.edit().putString("music_recent_history",array.toString()).apply()
  }

  private fun listeningChat(chatId:String?)=chats.firstOrNull{it.id==chatId&&!it.group&&find(it.cardId)!=null}
  private fun activeTogetherChat()=listeningChat(togetherChatId)
  private fun togetherSession(chatId:String)=runCatching{JSONObject(prefs.getString(TogetherListening.sessionKey(chatId),"{}").orEmpty())}.getOrElse{JSONObject()}
  private fun savedTogetherSong(value:JSONObject?):NeteaseSong?{
    value?:return null
    return value.optLong("id").takeIf{it>0}?.let{NeteaseSong(it,value.optString("title"),value.optString("artist"),value.optString("album"),value.optLong("durationMs"),value.optString("coverUrl"))}
  }
  private fun NeteaseSong.savedTogetherJson()=JSONObject().put("id",id).put("title",title).put("artist",artist).put("album",album).put("durationMs",durationMs).put("coverUrl",coverUrl)
  private fun savedTogetherQueue(state:JSONObject):List<NeteaseSong>{
    val values=state.optJSONArray("queue")?:JSONArray()
    return buildList{for(i in 0 until values.length())savedTogetherSong(values.optJSONObject(i))?.let(::add)}.distinctBy{it.id}
  }
  private fun loadTogetherSession(chat:Chat){
    val state=togetherSession(chat.id)
    togetherChatId=chat.id;togetherSong=savedTogetherSong(state.optJSONObject("song"));togetherQueue=savedTogetherQueue(state)
  }
  private fun selectTogetherChat(chat:Chat){
    if(togetherChatId==chat.id)return
    saveTogetherSession()
    stopChatMusic()
    loadTogetherSession(chat)
  }
  private fun saveTogetherSession(){
    val chat=activeTogetherChat()?:return
    val state=togetherSession(chat.id).put("song",togetherSong?.savedTogetherJson()?:JSONObject.NULL)
      .put("queue",JSONArray(togetherQueue.map{it.savedTogetherJson()}))
    prefs.edit().putString(TogetherListening.sessionKey(chat.id),state.toString()).apply()
  }
  private fun endTogetherListening(){
    val chat=activeTogetherChat()?:return
    saveTogetherSession()
    if(musicOwnerChatId==chat.id)stopChatMusic()else{musicRequestGeneration++;autoAdvanceInFlight=false}
    togetherChatId=null;togetherSong=null;togetherQueue=emptyList()
    selectedChat=chat;composePage=Page.CHAT;uiRevision++
  }
  private fun clearTogetherSession(chatId:String){
    if(musicOwnerChatId==chatId)stopChatMusic()
    if(togetherChatId==chatId){musicRequestGeneration++;autoAdvanceInFlight=false;togetherChatId=null;togetherSong=null;togetherQueue=emptyList();if(composePage==Page.TOGETHER_LISTEN)composePage=Page.MAIN}
    prefs.edit().remove(TogetherListening.sessionKey(chatId)).remove(TogetherListening.sessionKey(chatId)+"_background").apply()
  }

  private fun playTogetherSong(song:NeteaseSong){
    val chat=activeTogetherChat()?:return
    val card=find(chat.cardId)?:return
    togetherSong=song;addToTogetherQueue(song,chat)
    toggleChatMusic(neteaseSongUrl(song.id),song.title,song.artist,song.album,song.id,
      card.id,card.name,song.coverUrl,playOnly=true,chatId=chat.id)
  }

  private fun addToTogetherQueue(song:NeteaseSong,chat:Chat):Boolean{
    val state=togetherSession(chat.id)
    val queue=if(togetherChatId==chat.id)togetherQueue else savedTogetherQueue(state)
    val added=queue.none{it.id==song.id}
    val updated=(queue+song).distinctBy{it.id}
    if(togetherChatId==chat.id){togetherQueue=updated;saveTogetherSession()}
    else prefs.edit().putString(TogetherListening.sessionKey(chat.id),state.put("queue",JSONArray(updated.map{it.savedTogetherJson()})).toString()).apply()
    uiRevision++
    return added
  }

  private fun musicPlayMode()=togetherChatId?.let{togetherSession(it).optString("playMode")}.orEmpty().takeIf{it in listOf("list","repeat","shuffle")}?:"list"

  private fun cycleMusicPlayMode(){
    val chat=activeTogetherChat()?:return
    val next=when(musicPlayMode()){"list"->"repeat";"repeat"->"shuffle";else->"list"}
    prefs.edit().putString(TogetherListening.sessionKey(chat.id),togetherSession(chat.id).put("playMode",next).toString()).apply();uiRevision++
  }

  private fun autoAdvanceTogether(){
    val chat=activeTogetherChat()?:return
    if(autoAdvanceInFlight||musicResolvingSongId>0)return
    val current=togetherSong?:return
    val index=togetherQueue.indexOfFirst{it.id==current.id}
    val queued=when(musicPlayMode()){
      "repeat"->current
      "shuffle"->togetherQueue.filter{it.id!=current.id}.randomOrNull()
      else->if(index in 0 until togetherQueue.lastIndex)togetherQueue[index+1] else null
    }
    if(queued!=null){playTogetherSong(queued);return}
    autoAdvanceInFlight=true
    val requestGeneration=musicRequestGeneration
    pool.execute{
      val result=runCatching{searchNeteaseSongs(current.artist.ifBlank{current.title}).firstOrNull{it.id!=current.id}}
      main.post{
        if(requestGeneration!=musicRequestGeneration||togetherChatId!=chat.id||togetherSong?.id!=current.id)return@post
        autoAdvanceInFlight=false
        result.onSuccess{recommended->
          if(recommended!=null){playTogetherSong(recommended)}
          else toast("暂时没有找到下一首推荐")
        }.onFailure{toast("自动推荐下一首失败：${it.message?:"网络不可用"}")}
      }
    }
  }

  private fun toggleChatMusic(rawUrl:String,title:String="分享的音乐",artist:String="未知歌手",album:String="",songId:Long=0L,roleId:String="",roleName:String="",coverUrl:String="",playOnly:Boolean=false,chatId:String=""){
    val url=rawUrl.trim()
    if(url.isBlank()){toast("这条音乐没有可播放的链接");return}
    if(!(url.startsWith("https://",true)||url.startsWith("http://",true))){toast("音乐链接必须以 http:// 或 https:// 开头");return}
    if(chatId.isNotBlank()&&listeningChat(chatId)?.cardId!=roleId)return
    val boundRoleId=if(chatId.isBlank())"" else roleId
    val sameOwner=musicOwnerChatId==chatId&&musicOwnerRoleId==boundRoleId
    if(sameOwner&&musicLoadingUrl==url)return
    if(sameOwner&&(musicPlayingUrl==url||musicPausedUrl==url)){if(!playOnly||musicPausedUrl==url){musicRequestGeneration++;autoAdvanceInFlight=false;musicResolvingSongId=0L;musicLoadingUrl="";NeteaseMusicService.toggle(this)};return}
    stopChatMusic()
    val requestGeneration=++musicRequestGeneration
    val playbackId=UUID.randomUUID().toString()
    musicPlaybackId=playbackId;musicOwnerChatId=chatId;musicOwnerRoleId=boundRoleId
    prefs.edit().putString("music_requested_playback_id",playbackId).apply()
    musicResolvingSongId=0L
    val resolvedSongId=if(songId>0)songId else runCatching{android.net.Uri.parse(url).getQueryParameter("id")?.substringBefore('.')?.toLongOrNull()?:0L}.getOrDefault(0L)
    val cleanTitle=title.trim().ifBlank{"分享的音乐"};val cleanArtist=artist.trim().ifBlank{"未知歌手"};val cleanAlbum=album.trim()
    if(resolvedSongId>0&&url.contains("music.163.com",true)){
      if(musicResolvingSongId==resolvedSongId)return
      musicResolvingSongId=resolvedSongId;musicLoadingUrl=url;musicCoverUrl=coverUrl;uiRevision++
      pool.execute{
        runCatching{neteasePlayableUrl(resolvedSongId)}.onSuccess{stream->main.post{if(requestGeneration!=musicRequestGeneration)return@post;musicResolvingSongId=0L;rememberAppRecent(NeteaseSong(resolvedSongId,cleanTitle,cleanArtist,cleanAlbum,0L,coverUrl));NeteaseMusicService.play(this,url,cleanTitle,cleanArtist,cleanAlbum,resolvedSongId,boundRoleId,roleName,coverUrl,stream,chatId,playbackId);uiRevision++}}.onFailure{error->main.post{if(requestGeneration!=musicRequestGeneration)return@post;musicResolvingSongId=0L;musicLoadingUrl="";toast("网易云没有返回完整音源：${error.message?:"请登录有权限的账号"}");uiRevision++}}
      }
    }else { musicLoadingUrl=url;if(resolvedSongId>0)rememberAppRecent(NeteaseSong(resolvedSongId,cleanTitle,cleanArtist,cleanAlbum,0L,coverUrl)); NeteaseMusicService.play(this,url,cleanTitle,cleanArtist,cleanAlbum,resolvedSongId,boundRoleId,roleName,coverUrl,"",chatId,playbackId) }
  }

  private fun beginVoiceCallSession(chat:Chat,card:Card,note:String):Boolean{
    if(isChatSending(chat.id)){toast("请等待当前回复完成后再发起通话");return false}
    if(activeProvider()==null){toast("请先在模型设置中添加供应商");return false}
    if(chat.userBlockedRole){toast("你已拉黑 ${card.name}，无法发起通话");return false}
    if(chat.roleBlockedUser){toast("${card.name} 尚未解除拉黑，暂时无法接通");return false}
    if(voiceCallSessionId.isNotBlank())endVoiceCallSession(true)
    voiceCallSessionId=UUID.randomUUID().toString()
    voiceCallChatId=chat.id
    voiceCallCardId=card.id
    voiceCallPhase=VoiceCallPhase.READY
    voiceCallStatus="准备就绪"
    voiceCallLastUserText=""
    voiceCallLastReplyText=""
    voiceCallStartedAt=SystemClock.elapsedRealtime()
    voiceCallMicMuted=false
    voiceCallFloating=false
    voiceCallScreenSharing=false
    voiceCallScreenImagePath=""
    configureVoiceCallAudio(true)
    val event=Msg("user",note.trim().ifBlank{"已接通语音通话"})
    event.messageType="voice_call"
    event.meta=JSONObject().put("state","通话中").put("note",note.trim())
      .put("callId",voiceCallSessionId).toString()
    event.delivered=true
    chat.messages.add(event)
    chat.updated=System.currentTimeMillis()
    save();uiRevision++
    return true
  }

  private fun voiceCallVisualReady()=prefs.getBoolean("visual_aux_enabled",false)&&prefs.getString("visual_aux_model","").orEmpty().isNotBlank()

  private fun syncVoiceCallOverlay(){
    if(!voiceCallFloating||voiceCallSessionId.isBlank())return
    val role=find(voiceCallCardId)?.name.orEmpty().ifBlank{"AI"}
    VoiceCallOverlayService.show(this,role,voiceCallStatus,voiceCallMicMuted,voiceCallSpeakerEnabled,
      voiceCallScreenSharing,voiceCallVisualReady(),voiceCallChatId)
  }

  private fun minimizeVoiceCall(){
    if(voiceCallSessionId.isBlank())return
    if(android.os.Build.VERSION.SDK_INT>=23&&!android.provider.Settings.canDrawOverlays(this)){
      toast("需要悬浮窗权限才能缩小通话")
      runCatching{startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:$packageName")))}
      return
    }
    voiceCallFloating=true
    syncVoiceCallOverlay()
    uiRevision++
  }

  private fun requestVoiceCallScreenShare(){
    if(voiceCallSessionId.isBlank())return
    if(voiceCallScreenSharing){ScreenShareService.stop(this);return}
    val manager=getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as? android.media.projection.MediaProjectionManager
    if(manager==null){toast("当前系统不支持屏幕共享");return}
    runCatching{voiceCallScreenPermission.launch(manager.createScreenCaptureIntent())}
      .onFailure{toast("无法打开屏幕共享授权：${it.message}")}
  }

  private fun handleVoiceCallCommand(command:String){
    if(command.isBlank()||voiceCallSessionId.isBlank())return
    when(command){
      VoiceCallOverlayService.COMMAND_REOPEN->{
        chats.firstOrNull{it.id==voiceCallChatId}?.let{selectedChat=it;composePage=Page.CHAT}
        voiceCallFloating=false;VoiceCallOverlayService.hide(this);uiRevision++
      }
      VoiceCallOverlayService.COMMAND_MIC->{voiceCallPendingCommand="mic:${SystemClock.elapsedRealtimeNanos()}";uiRevision++}
      VoiceCallOverlayService.COMMAND_SPEAKER->{configureVoiceCallAudio(!voiceCallSpeakerEnabled);syncVoiceCallOverlay();uiRevision++}
      VoiceCallOverlayService.COMMAND_SCREEN->requestVoiceCallScreenShare()
      VoiceCallOverlayService.COMMAND_END->endVoiceCallSession(true)
    }
  }

  private fun handleVoiceCallIntent(intent:Intent?){
    handleVoiceCallCommand(intent?.getStringExtra(VoiceCallOverlayService.EXTRA_COMMAND).orEmpty())
  }

  private fun configureVoiceCallAudio(speaker:Boolean){
    val audio=getSystemService(AUDIO_SERVICE) as? AudioManager?:return
    runCatching{
      if(!voiceCallAudioConfigured){
        voiceCallPreviousAudioMode=audio.mode
        voiceCallPreviousSpeaker=audio.isSpeakerphoneOn
        voiceCallAudioConfigured=true
      }
      audio.mode=AudioManager.MODE_IN_COMMUNICATION
      audio.isSpeakerphoneOn=speaker
      voiceCallSpeakerEnabled=speaker
    }
    syncVoiceCallOverlay()
  }

  private fun restoreVoiceCallAudio(){
    if(!voiceCallAudioConfigured)return
    val audio=getSystemService(AUDIO_SERVICE) as? AudioManager
    runCatching{if(audio!=null){audio.isSpeakerphoneOn=voiceCallPreviousSpeaker;audio.mode=voiceCallPreviousAudioMode}}
    voiceCallAudioConfigured=false
  }

  private fun endVoiceCallSession(recordEvent:Boolean){
    val callId=voiceCallSessionId
    if(callId.isBlank())return
    val chatId=voiceCallChatId
    val wasThinking=voiceCallPhase==VoiceCallPhase.THINKING
    val duration=((SystemClock.elapsedRealtime()-voiceCallStartedAt).coerceAtLeast(0L)/1000L)
    voiceCallSessionId=""
    voiceCallPhase=VoiceCallPhase.IDLE
    voiceCallStatus=""
    if(voiceRecording)cancelOnlineVoiceRecording()
    if(offlineVoiceBusy())cancelOfflineVoiceRecognition()
    stopSpeech()
    if(wasThinking&&isChatSending(chatId))stopGeneration(chatId)
    restoreVoiceCallAudio()
    ScreenShareService.stop(this)
    VoiceCallOverlayService.hide(this)
    if(recordEvent){
      chats.firstOrNull{it.id==chatId}?.let{chat->
        val event=Msg("user","语音通话已结束")
        event.messageType="voice_call"
        event.meta=JSONObject().put("state","结束").put("callId",callId)
          .put("durationSeconds",duration).toString()
        event.delivered=true
        chat.messages.add(event);chat.updated=System.currentTimeMillis();save()
      }
    }
    voiceCallChatId="";voiceCallCardId="";voiceCallStartedAt=0L
    voiceCallLastUserText="";voiceCallLastReplyText="";voiceCallMicMuted=false
    voiceCallFloating=false;voiceCallScreenSharing=false;voiceCallScreenImagePath="";voiceCallPendingCommand=""
    uiRevision++
  }

  override fun onDestroy(){voiceCallReceiver?.let{runCatching{unregisterReceiver(it)}};voiceCallReceiver=null;musicReceiver?.let{runCatching{unregisterReceiver(it)}};musicReceiver=null;endVoiceCallSession(false);immersiveReplyTasks.values.forEach{main.removeCallbacks(it)};immersiveReplyTasks.clear();super.onDestroy()}

  override fun onStop(){if(voiceCallSessionId.isNotBlank()&&(android.os.Build.VERSION.SDK_INT<23||android.provider.Settings.canDrawOverlays(this))){voiceCallFloating=true;syncVoiceCallOverlay()};super.onStop()}

  private fun providers():List<ProviderConfig>{
    val custom=mutableListOf<ProviderConfig>();runCatching{val a=org.json.JSONArray(prefs.getString("custom_providers","[]"));for(i in 0 until a.length()){val j=a.getJSONObject(i);custom.add(ProviderConfig(j.getString("id"),j.getString("name"),j.getString("endpoint"),j.optString("model","gpt-4o-mini")))}}
    return custom
  }
  private fun saveCustomProviders(values:List<ProviderConfig>){val a=org.json.JSONArray();values.filter{!it.builtIn}.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("endpoint",it.endpoint).put("model",it.modelName))};prefs.edit().putString("custom_providers",a.toString()).apply()}
  private fun imageProviders():List<ImageProviderConfig>{
    val values=mutableListOf<ImageProviderConfig>()
    runCatching{val a=JSONArray(prefs.getString("image_generation_providers","[]"));for(i in 0 until a.length()){val j=a.optJSONObject(i)?:continue;values.add(ImageProviderConfig(j.optString("id"),j.optString("name","生图供应商"),j.optString("protocol","openai"),j.optString("endpoint"),j.optString("model"),j.optString("size","1024x1024"),j.optString("negative"),j.optInt("steps",28),j.optInt("scale",7),if(j.optString("runtime","cpu")=="npu")"npu" else "cpu"))}}
    if(values.isEmpty()&&prefs.contains("image_generation_endpoint"))values.add(ImageProviderConfig("legacy-image-provider",prefs.getString("image_generation_provider_name","默认生图供应商")?:"默认生图供应商",prefs.getString("image_generation_protocol","openai")?:"openai",prefs.getString("image_generation_endpoint","").orEmpty(),prefs.getString("image_generation_model","dall-e-3")?:"dall-e-3",prefs.getString("image_generation_size","1024x1024")?:"1024x1024",prefs.getString("image_generation_negative","").orEmpty(),prefs.getInt("image_generation_nai_steps",28),prefs.getInt("image_generation_nai_scale",7),if(prefs.getString("image_generation_local_runtime","cpu")=="npu")"npu" else "cpu"))
    return values.filter{it.id.isNotBlank()}
  }
  private fun saveImageProviders(values:List<ImageProviderConfig>){val a=JSONArray();values.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("protocol",it.protocol).put("endpoint",it.endpoint).put("model",it.model).put("size",it.size).put("negative",it.negative).put("steps",it.steps).put("scale",it.scale).put("runtime",it.runtime))};prefs.edit().putString("image_generation_providers",a.toString()).apply()}
  private fun activateImageProvider(value:ImageProviderConfig){
    val editor=prefs.edit().putString("active_image_provider_id",value.id).putString("image_generation_provider_name",value.name).putString("image_generation_protocol",value.protocol).putString("image_generation_endpoint",value.endpoint).putString("image_generation_model",value.model).putString("image_generation_size",value.size).putString("image_generation_negative",value.negative).putInt("image_generation_nai_steps",value.steps).putInt("image_generation_nai_scale",value.scale)
    if(value.protocol=="local")editor.putInt("image_generation_local_steps",value.steps).putLong("image_generation_local_cfg",java.lang.Double.doubleToLongBits(value.scale.toDouble())).putString("image_generation_local_runtime",if(value.runtime=="npu"&&LocalImageEngine.npuSupported(this))"npu" else "cpu")
    editor.apply();runCatching{val providerKey=secure.getNamed("image_provider_key_${value.id}");val legacyKey=if(value.id=="legacy-image-provider")secure.getNamed("image_generation_key")else providerKey;secure.putNamed("image_generation_key",if(providerKey.isBlank())legacyKey else providerKey)};uiRevision++
  }
  private fun activeImageProvider():ImageProviderConfig?{val values=imageProviders();val id=prefs.getString("active_image_provider_id","").orEmpty();return values.firstOrNull{it.id==id}?:values.firstOrNull()}
  private fun imagePromptConfig(raw:String):ImagePromptConfig=runCatching{val value=JSONObject(raw);ImagePromptConfig(value.optString("positive"),value.optString("negative"),value.optString("style","natural"))}.getOrElse{ImagePromptConfig(raw,prefs.getString("image_generation_negative","lowres, bad anatomy, blurry").orEmpty())}
  private fun imagePromptJson(positive:String,negative:String,style:String)=JSONObject().put("positive",positive.trim()).put("negative",negative.trim()).put("style",style).toString()
  private val appearanceConfigDefaults=linkedMapOf<String,Any>("compose_theme" to 0,"compose_theme_style" to 0,"refresh_rate" to 0,"chat_font_size" to 15,"chat_bubble_spacing" to 8,"chat_line_height" to 22,"chat_background_color" to "","chat_user_bubble_color" to "","chat_ai_bubble_color" to "","chat_no_bubbles" to false,"chat_hide_avatars" to false,"reply_suggestion_position" to "bubble","chat_half_screen" to false,"chat_bubble_actions" to "latest","chat_user_bubble_opacity" to 100,"chat_ai_bubble_opacity" to 100,"chat_bubble_max_width" to 292,"chat_bubble_vertical_padding" to 10,"chat_input_min_height" to 42,"chat_background_overlay" to 20,"experimental_input_glass" to false,"experimental_top_bar_transparent" to false,"experimental_glass_ui" to false,"experimental_glass_opacity" to 78)
  private fun appearanceConfigJson():JSONObject{
    val values=JSONObject();appearanceConfigDefaults.forEach{(key,default)->values.put(key,prefs.all[key]?:default)}
    return JSONObject().put("format","shiyu_appearance").put("version",1).put("values",values)
  }
  private fun importAppearanceConfig(raw:String){
    val root=JSONObject(raw.removePrefix("\uFEFF"));require(root.optString("format")=="shiyu_appearance"){"不是时遇外观配置文件"};val values=root.getJSONObject("values")
    val allowed=appearanceConfigDefaults.keys;val editor=prefs.edit()
    values.keys().forEach{key->if(key in allowed)when(val value=values.get(key)){is Boolean->editor.putBoolean(key,value);is Int->editor.putInt(key,value);is Long->editor.putLong(key,value);is Double->editor.putFloat(key,value.toFloat());is String->editor.putString(key,value)}}
    editor.apply();applyPreferredRefreshRate();uiRevision++
  }
  private fun shareAppearanceConfig(){
    runCatching{val directory=File(cacheDir,"shared").apply{mkdirs()};val file=File(directory,"shiyu-appearance.json");FileOutputStream(file).use{it.write(appearanceConfigJson().toString(2).toByteArray(Charsets.UTF_8))};val uri=androidx.core.content.FileProvider.getUriForFile(this,"$packageName.files",file);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"分享外观配置"))}.onFailure{toast("分享外观配置失败：${it.message}")}
  }
  private fun testProviderConnection(rawEndpoint:String,apiKey:String,modelName:String,testContent:String):String{
    if(rawEndpoint.trim().startsWith("local://in-app",true)){
      val modelFile=File(LocalModelRepository.directory(this@ComposeActivity),File(modelName).name)
      return LocalLlamaEngine.complete(
        modelFile.absolutePath,
        "你是本地模型连接测试助手。",
        testContent.trim().ifBlank{"请只回复：连接成功"},
        prefs.getInt("local_llm_context",4096),
        prefs.getInt("local_llm_threads",minOf(4,Runtime.getRuntime().availableProcessors().coerceAtLeast(1))),
        .2f,
        128
      )
    }
    var url=rawEndpoint.trim().trimEnd('/').removeSuffix("/chat/completions");url+="/chat/completions"
    val connection=URL(url).openConnection() as HttpURLConnection
    try{
      connection.requestMethod="POST";connection.connectTimeout=20_000;connection.readTimeout=90_000;connection.doOutput=true
      connection.setRequestProperty("Content-Type","application/json; charset=utf-8");if(apiKey.isNotBlank())connection.setRequestProperty("Authorization","Bearer ${apiKey.trim()}")
      val request=JSONObject().put("model",modelName.trim()).put("stream",false).put("temperature",0.2)
        .put("messages",JSONArray().put(JSONObject().put("role","user").put("content",testContent.trim().ifBlank{"请只回复：连接成功"})))
      connection.outputStream.use{it.write(request.toString().toByteArray(Charsets.UTF_8))}
      val code=connection.responseCode;val raw=read(if(code in 200..299)connection.inputStream else connection.errorStream)
      if(code !in 200..299)throw java.io.IOException("HTTP $code：$raw")
      val message=JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?:throw java.io.IOException("接口没有返回 message")
      return jsonText(message,"content").ifBlank{reasoningText(message)}.ifBlank{"连接成功，但模型返回空内容"}.take(2000)
    }finally{connection.disconnect()}
  }
  private fun identities():List<IdentityConfig>{val values=mutableListOf<IdentityConfig>();runCatching{val a=JSONArray(prefs.getString("user_identities","[]"));for(i in 0 until a.length()){val j=a.getJSONObject(i);values.add(IdentityConfig(j.getString("id"),j.getString("name"),j.optString("description"),j.optString("birthday"),j.optString("pronouns"),j.optString("location"),j.optString("occupation"),j.optString("interests"),j.optString("bio")))}};return values}
  private fun saveIdentities(values:List<IdentityConfig>){val a=JSONArray();values.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("description",it.description).put("birthday",it.birthday).put("pronouns",it.pronouns).put("location",it.location).put("occupation",it.occupation).put("interests",it.interests).put("bio",it.bio))};prefs.edit().putString("user_identities",a.toString()).apply()}
  private fun worldBookDocument(raw:String):WorldBookDocument?=runCatching{
    val clean=raw.trim();if(clean.isEmpty())return null
    val root:Any=if(clean.startsWith("["))JSONArray(clean)else JSONObject(clean)
    if(root is JSONArray)return@runCatching WorldBookDocument(root,root,null)
    root as JSONObject
    root.optJSONArray("entries")?.let{return@runCatching WorldBookDocument(root,it,null)}
    val map=root.optJSONObject("entries")?:return null;val entries=JSONArray();val keys=mutableListOf<String>();val iterator=map.keys();while(iterator.hasNext()){val key=iterator.next();map.optJSONObject(key)?.let{entries.put(it);keys.add(key)}}
    WorldBookDocument(root,entries,keys)
  }.getOrNull()
  private fun worldBookKeywords(value:Any?):String=when(value){is JSONArray->buildList{for(i in 0 until value.length())value.optString(i).trim().takeIf{it.isNotBlank()}?.let(::add)}.joinToString("，");is String->value;else->""}
  private fun worldBookEntries(raw:String):List<WorldBookEntryView>?{
    val document=worldBookDocument(raw)?:return null
    return buildList{for(i in 0 until document.entries.length()){val item=document.entries.optJSONObject(i)?:continue;val mapKey=document.mapKeys?.getOrNull(i).orEmpty();val title=item.optString("comment",item.optString("name",item.optString("title",mapKey.ifBlank{"条目 ${i+1}"}))).ifBlank{"条目 ${i+1}"};val keys=worldBookKeywords(if(item.has("keys"))item.opt("keys")else item.opt("key"));val content=item.optString("content",item.optString("text",item.optString("entry","")));val enabled=if(item.has("enabled"))item.optBoolean("enabled",true)else !item.optBoolean("disable",false);val stableId=mapKey.ifBlank{item.optString("uid",item.optString("id",i.toString()))};add(WorldBookEntryView(i,mapKey,"$i:$stableId",title,keys,content,enabled,item.optBoolean("constant",false),item.optInt("insertion_order",item.optInt("order",100))))}}
  }
  private fun serializeWorldBook(document:WorldBookDocument):String=when(val root=document.root){is JSONArray->root.toString(2);is JSONObject->root.toString(2);else->root.toString()}
  private fun mutateWorldBookEntry(raw:String,entry:WorldBookEntryView,change:(JSONObject)->Unit):String{
    val document=worldBookDocument(raw)?:return raw;val item=if(document.mapKeys!=null){val map=(document.root as JSONObject).optJSONObject("entries")?:return raw;map.optJSONObject(entry.mapKey)}else document.entries.optJSONObject(entry.index);if(item==null)return raw;change(item);return serializeWorldBook(document)
  }
  private fun updateWorldBookEntry(raw:String,entry:WorldBookEntryView):String=mutateWorldBookEntry(raw,entry){item->
    when{item.has("comment")->item.put("comment",entry.title);item.has("name")->item.put("name",entry.title);else->item.put("comment",entry.title)}
    val keys=JSONArray();entry.keywords.split(Regex("[,，|\\n]")).map(String::trim).filter(String::isNotBlank).forEach(keys::put);if(item.has("key")&&!item.has("keys"))item.put("key",keys)else item.put("keys",keys)
    when{item.has("text")&&!item.has("content")->item.put("text",entry.content);item.has("entry")&&!item.has("content")->item.put("entry",entry.content);else->item.put("content",entry.content)}
    if(item.has("disable")&&!item.has("enabled"))item.put("disable",!entry.enabled)else item.put("enabled",entry.enabled);item.put("constant",entry.constant);if(item.has("insertion_order"))item.put("insertion_order",entry.order)else item.put("order",entry.order)
  }
  private fun addWorldBookEntry(raw:String):String{
    val document=worldBookDocument(raw)?:return JSONObject().put("entries",JSONArray().put(JSONObject().put("comment","新条目").put("keys",JSONArray()).put("content","").put("enabled",true).put("order",100))).toString(2)
    val item=JSONObject().put("comment","新条目").put("keys",JSONArray()).put("content","").put("enabled",true).put("constant",false).put("order",100)
    if(document.mapKeys!=null)(document.root as JSONObject).getJSONObject("entries").put(UUID.randomUUID().toString(),item)else document.entries.put(item)
    return serializeWorldBook(document)
  }
  private fun removeWorldBookEntry(raw:String,entry:WorldBookEntryView):String{
    val document=worldBookDocument(raw)?:return raw;if(document.mapKeys!=null)(document.root as JSONObject).optJSONObject("entries")?.remove(entry.mapKey)else document.entries.remove(entry.index);return serializeWorldBook(document)
  }
  private fun moments():List<MomentPost>{val values=mutableListOf<MomentPost>();runCatching{val a=JSONArray(prefs.getString("moments","[]"));for(i in 0 until a.length()){val j=a.optJSONObject(i)?:continue;val authorId=j.optString("authorId","user");val rawContent=j.optString("content");values.add(MomentPost(j.optString("id"),authorId,j.optString("authorName","我"),if(authorId=="user")rawContent else AiContentSanitizer.moment(rawContent),j.optString("image"),j.optLong("createdAt")))}};return values.sortedByDescending{it.createdAt}}
  private fun saveMoments(values:List<MomentPost>){val a=JSONArray();values.sortedByDescending{it.createdAt}.forEach{a.put(JSONObject().put("id",it.id).put("authorId",it.authorId).put("authorName",it.authorName).put("content",it.content).put("image",it.image).put("createdAt",it.createdAt))};prefs.edit().putString("moments",a.toString()).apply()}
  private fun momentComments(postId:String):List<MomentComment>{val out=mutableListOf<MomentComment>();runCatching{val a=JSONObject(prefs.getString("moment_comments","{}").orEmpty()).optJSONArray(postId)?:JSONArray();for(i in 0 until a.length()){val j=a.optJSONObject(i)?:continue;out.add(MomentComment(j.optString("id"),j.optString("authorId","user"),j.optString("authorName","我"),j.optString("content"),j.optLong("createdAt"),j.optString("parentId"),j.optString("replyToName")))}};return out.sortedBy{it.createdAt}}
  private fun saveMomentComments(postId:String,values:List<MomentComment>){val root=runCatching{JSONObject(prefs.getString("moment_comments","{}").orEmpty())}.getOrElse{JSONObject()};val a=JSONArray();values.sortedBy{it.createdAt}.forEach{a.put(JSONObject().put("id",it.id).put("authorId",it.authorId).put("authorName",it.authorName).put("content",it.content).put("createdAt",it.createdAt).put("parentId",it.parentId).put("replyToName",it.replyToName))};root.put(postId,a);prefs.edit().putString("moment_comments",root.toString()).apply()}
  private fun momentLikes(postId:String):Set<String>{val root=runCatching{JSONObject(prefs.getString("moment_likes","{}").orEmpty())}.getOrElse{JSONObject()};val values=root.optJSONArray(postId)?:JSONArray();return buildSet{for(i in 0 until values.length())values.optString(i).takeIf{it.isNotBlank()}?.let(::add)}}
  private fun saveMomentLikes(postId:String,values:Set<String>){val root=runCatching{JSONObject(prefs.getString("moment_likes","{}").orEmpty())}.getOrElse{JSONObject()};root.put(postId,JSONArray(values.toList()));prefs.edit().putString("moment_likes",root.toString()).apply()}
  private fun toggleMomentLike(postId:String,authorId:String="user"){val values=momentLikes(postId).toMutableSet();if(!values.add(authorId))values.remove(authorId);saveMomentLikes(postId,values);uiRevision++}
  private fun momentRoleIds(key:String)=prefs.getStringSet(key,emptySet())?.toSet().orEmpty()
  private fun saveMomentRoleIds(key:String,values:Set<String>){prefs.edit().putStringSet(key,values.toSet()).apply()}
  private fun worldRoleIds():Set<String>{val available=cards.map{it.id}.toSet();val saved=prefs.getStringSet("world_role_ids",emptySet()).orEmpty().filterTo(linkedSetOf()){it in available};return if(saved.isNotEmpty())saved else cards.firstOrNull()?.let{setOf(it.id)}.orEmpty()}
  private fun saveWorldRoleIds(values:Set<String>){prefs.edit().putStringSet("world_role_ids",values.toSet()).apply();uiRevision++}
  private fun worldOutfits()=listOf(
    WorldOutfit("home","居家针织","柔软针织与浅色长裤，适合在家休息","#DF7861","#F4D8CD"),
    WorldOutfit("campus","学院制服","利落的深色外套与明亮领结","#284B63","#F4C95D"),
    WorldOutfit("date","约会礼服","克制的正式剪裁与酒红配色","#7A2841","#F0C7B5"),
    WorldOutfit("travel","旅行轻装","耐磨外套、短靴与醒目的随身包","#397367","#E8B86D"),
    WorldOutfit("night","星夜睡衣","深夜蓝睡衣与柔软浅灰饰边","#303A52","#B8C4CE")
  )
  private fun worldOutfitId(cardId:String)=runCatching{JSONObject(prefs.getString("world_wardrobe","{}").orEmpty()).optString(cardId,"home")}.getOrDefault("home")
  private fun saveWorldOutfit(cardId:String,outfitId:String){val root=runCatching{JSONObject(prefs.getString("world_wardrobe","{}").orEmpty())}.getOrElse{JSONObject()};root.put(cardId,outfitId);prefs.edit().putString("world_wardrobe",root.toString()).apply();uiRevision++}
  private fun askDanger(title:String,message:String,confirmLabel:String="确认删除",action:()->Unit){pendingDanger=DangerAction(title,message,confirmLabel,action)}
  private fun activateIdentity(value:IdentityConfig){prefs.edit().putString("active_identity_id",value.id).putString("active_identity_name",value.name).putString("active_identity_description",value.description).apply();uiRevision++;toast("已切换身份：${value.name}")}
  private fun relationshipKey(cardId:String,chatId:String?=null)=chatId?.takeIf{it.isNotBlank()}?.let{"role_relationship_${cardId}_chat_$it"}?:"role_relationship_$cardId"
  private fun relationshipRoot(cardId:String,chatId:String?=null):JSONObject{
    val key=relationshipKey(cardId,chatId);val saved=prefs.getString(key,null)
    if(!saved.isNullOrBlank())return runCatching{JSONObject(saved)}.getOrElse{JSONObject()}
    if(!chatId.isNullOrBlank()){
      val legacyKey=relationshipKey(cardId);val legacy=prefs.getString(legacyKey,null)
      if(!legacy.isNullOrBlank()){
        prefs.edit().putString(key,legacy).remove(legacyKey).apply()
        return runCatching{JSONObject(legacy)}.getOrElse{JSONObject()}
      }
    }
    return JSONObject()
  }
  private fun saveRelationshipRoot(cardId:String,value:JSONObject,chatId:String?=null){prefs.edit().putString(relationshipKey(cardId,chatId),value.toString()).apply()}
  private fun relationshipSessionId(cardId:String,preferred:String?=null):String?{
    fun valid(id:String?)=id?.takeIf{candidate->chats.any{chat->chat.id==candidate&&if(chat.group)cardId in chat.cardIds else chat.cardId==cardId}}
    return valid(preferred)?:valid(selectedChat?.id)?:valid(roleProfileChatId)?:chatsForCard(cardId).maxByOrNull{it.updated}?.id
  }
  private fun roleLlmState(card:Card,chatId:String?=null):RoleLlmState{
    val root=relationshipRoot(card.id,chatId);val state=root.optJSONObject("llmState")?:JSONObject()
    val hobbies=buildList{val a=state.optJSONArray("hobbies")?:root.optJSONArray("interests")?:JSONArray();for(i in 0 until a.length())a.optString(i).trim().takeIf{it.isNotBlank()}?.let(::add)}.distinct().take(10)
    fun score(key:String):Int?=if(state.has(key)&&!state.isNull(key))state.optInt(key).coerceIn(0,100) else null
    return RoleLlmState(score("affection"),score("trust"),score("energy"),score("fatigue"),score("mood"),hobbies,state.optString("summary"),state.optLong("generatedAt"))
  }
  private fun roleScore(value:Int?,label:String):String= value?.let{"$it"}?:"未生成"
  private fun relationshipState(card:Card,chatId:String?=null):RoleLlmState=roleLlmState(card,chatId)
  private fun relationshipEvents(cardId:String,chatId:String?=null):List<JSONObject>{
    val values=relationshipRoot(cardId,chatId).optJSONArray("events")?:JSONArray()
    return buildList{for(i in 0 until values.length())values.optJSONObject(i)?.let(::add)}.sortedByDescending{it.optLong("at")}.take(30)
  }
  private fun addRelationshipEvent(card:Card,title:String,detail:String,affectionDelta:Int=0,trustDelta:Int=0,chatId:String?=null){
    val resolvedChatId=relationshipSessionId(card.id,chatId);val root=relationshipRoot(card.id,resolvedChatId);val events=root.optJSONArray("events")?:JSONArray()
    val item=JSONObject().put("id",UUID.randomUUID().toString()).put("title",title).put("detail",detail).put("at",System.currentTimeMillis())
    val next=JSONArray().put(item);for(i in 0 until minOf(events.length(),49))next.put(events.opt(i));root.put("events",next)
    if(affectionDelta!=0||trustDelta!=0)root.put("pendingLlmHint", "互动事件：$title（好感变化建议 $affectionDelta，信任变化建议 $trustDelta；请由角色 LLM 确认，不在本地直接修改数值）")
    saveRelationshipRoot(card.id,root,resolvedChatId);uiRevision++
  }
  private fun relationshipLevel(value:Int)=when(value){in 0..14->"初次相遇";in 15..29->"渐渐熟悉";in 30..49->"值得信赖";in 50..69->"默契相伴";in 70..84->"特别在意";else->"灵魂共鸣"}
  private fun neteaseCookie()=runCatching{secure.getNamed("netease_music_cookie")}.getOrDefault("").trim()
  private fun neteaseAccount(cookie:String):NeteaseAccount{
    val connection=(URL("https://music.163.com/api/nuser/account/get").openConnection() as HttpURLConnection).apply{
      connectTimeout=15_000;readTimeout=20_000;instanceFollowRedirects=true
      setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 ShiYu/${BuildConfig.VERSION_NAME}")
      setRequestProperty("Referer","https://music.163.com/");setRequestProperty("Accept","application/json,text/plain,*/*");setRequestProperty("Cookie",cookie)
    }
    try{
      val code=connection.responseCode;if(code !in 200..299)throw java.io.IOException("网易云账户验证 HTTP $code")
      val root=JSONObject(read(connection.inputStream));val profile=root.optJSONObject("profile")?:throw java.io.IOException("网易云登录状态已失效")
      return NeteaseAccount(profile.optLong("userId"),profile.optString("nickname").trim().ifBlank{"网易云用户"},profile.optString("avatarUrl"))
    }finally{connection.disconnect()}
  }
  private fun neteaseAccountName(cookie:String)=neteaseAccount(cookie).nickname
  private fun neteaseSongUrl(id:Long)="https://music.163.com/song/media/outer/url?id=$id.mp3"
  private fun neteasePlayableUrl(id:Long):String{
    val ids=java.net.URLEncoder.encode("[$id]","UTF-8")
    val root=neteaseJson("https://music.163.com/api/song/enhance/player/url/v1?ids=$ids&level=exhigh&encodeType=mp3",neteaseCookie())
    val item=root.optJSONArray("data")?.optJSONObject(0)?:throw java.io.IOException("网易云没有返回歌曲音源")
    val code=item.optInt("code",200);val stream=item.optString("url").trim()
    if(stream.isBlank()||(code!=0&&code!=200))throw java.io.IOException(if(code==-110||code==404)"当前账号或这首歌没有完整播放权限" else "网易云没有返回歌曲音源")
    return stream
  }
  private fun parseNeteaseSong(item:JSONObject):NeteaseSong?{
    val artists=item.optJSONArray("artists")?:item.optJSONArray("ar")?:JSONArray();val artist=buildList{for(j in 0 until artists.length())artists.optJSONObject(j)?.optString("name")?.takeIf{it.isNotBlank()}?.let(::add)}.joinToString(" / ").ifBlank{"未知歌手"}
    val album=item.optJSONObject("album")?:item.optJSONObject("al")?:JSONObject();val id=item.optLong("id");if(id<=0)return null
    return NeteaseSong(id,item.optString("name","未知歌曲"),artist,album.optString("name"),item.optLong("duration",item.optLong("dt")),album.optString("picUrl"))
  }
  private fun neteaseSongDetails(ids:List<Long>):Map<Long,NeteaseSong>{
    val unique=ids.filter{it>0}.distinct().take(100);if(unique.isEmpty())return emptyMap()
    val values=JSONArray();unique.forEach{values.put(JSONObject().put("id",it))}
    val encoded=java.net.URLEncoder.encode(values.toString(),"UTF-8")
    val songs=neteaseJson("https://music.163.com/api/v3/song/detail?c=$encoded",neteaseCookie()).optJSONArray("songs")?:JSONArray()
    return buildMap{for(i in 0 until songs.length())parseNeteaseSong(songs.optJSONObject(i)?:continue)?.let{put(it.id,it)}}
  }
  private fun enrichNeteaseSongs(values:List<NeteaseSong>):List<NeteaseSong>{
    val missing=values.filter{it.coverUrl.isBlank()}.map{it.id};if(missing.isEmpty())return values
    val details=runCatching{neteaseSongDetails(missing)}.getOrDefault(emptyMap())
    return values.map{song->details[song.id]?.let{detail->song.copy(title=detail.title.ifBlank{song.title},artist=detail.artist.ifBlank{song.artist},album=detail.album.ifBlank{song.album},durationMs=detail.durationMs.takeIf{it>0}?:song.durationMs,coverUrl=detail.coverUrl)}?:song}
  }
  private fun parseNeteaseLyrics(raw:String):List<LyricLine>{
    val stamp=Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    return buildList{raw.lineSequence().forEach{line->val matches=stamp.findAll(line).toList();if(matches.isEmpty())return@forEach;val text=stamp.replace(line,"").trim();if(text.isBlank())return@forEach;matches.forEach{match->val minute=match.groupValues[1].toLongOrNull()?:0L;val second=match.groupValues[2].toLongOrNull()?:0L;val fraction=match.groupValues[3];val millis=when(fraction.length){1->fraction.toLongOrNull()?.times(100)?:0L;2->fraction.toLongOrNull()?.times(10)?:0L;else->fraction.take(3).padEnd(3,'0').toLongOrNull()?:0L};add(LyricLine(minute*60_000L+second*1_000L+millis,text))}}}.distinctBy{it.timeMs to it.text}.sortedBy{it.timeMs}
  }
  private fun neteaseLyrics(id:Long):List<LyricLine>{
    val root=neteaseJson("https://music.163.com/api/song/lyric?id=$id&lv=1&kv=1&tv=-1",neteaseCookie())
    val original=parseNeteaseLyrics(root.optJSONObject("lrc")?.optString("lyric").orEmpty())
    val translated=parseNeteaseLyrics(root.optJSONObject("tlyric")?.optString("lyric").orEmpty()).associateBy{it.timeMs}
    return original.map{line->translated[line.timeMs]?.text?.takeIf{it.isNotBlank()&&it!=line.text}?.let{line.copy(text="${line.text}\n$it") }?:line}
  }
  private fun neteaseJson(url:String,cookie:String=neteaseCookie()):JSONObject{
    val connection=(URL(url).openConnection() as HttpURLConnection).apply{connectTimeout=15_000;readTimeout=25_000;instanceFollowRedirects=true;setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 ShiYu/${BuildConfig.VERSION_NAME}");setRequestProperty("Referer","https://music.163.com/");setRequestProperty("Accept","application/json,text/plain,*/*");cookie.takeIf{it.isNotBlank()}?.let{setRequestProperty("Cookie",it)}}
    try{val code=connection.responseCode;if(code !in 200..299)throw java.io.IOException("网易云请求 HTTP $code");return JSONObject(read(connection.inputStream))}finally{connection.disconnect()}
  }
  private fun neteasePlaylists(account:NeteaseAccount):List<NeteasePlaylist>{
    val values=neteaseJson("https://music.163.com/api/user/playlist?uid=${account.userId}&limit=50&offset=0",neteaseCookie()).optJSONArray("playlist")?:JSONArray()
    val result=mutableListOf<NeteasePlaylist>()
    for(i in 0 until values.length()){
      val item=values.optJSONObject(i) ?: continue
      val id=item.optLong("id")
      if(id>0)result.add(NeteasePlaylist(id,item.optString("name","未命名歌单"),item.optString("coverImgUrl"),item.optInt("trackCount")))
    }
    return result
  }
  private fun neteasePlaylistSongs(id:Long):List<NeteaseSong>{
    val values=neteaseJson("https://music.163.com/api/v6/playlist/detail?id=$id&n=1000&s=8").optJSONObject("playlist")?.optJSONArray("tracks")?:JSONArray()
    val result=mutableListOf<NeteaseSong>()
    for(i in 0 until values.length()){val item=values.optJSONObject(i) ?: continue;parseNeteaseSong(item)?.let(result::add)}
    return enrichNeteaseSongs(result)
  }
  private fun neteaseRecentSongs(account:NeteaseAccount):List<NeteaseSong>{
    val values=neteaseJson("https://music.163.com/api/v1/play/record?uid=${account.userId}&type=0",neteaseCookie()).optJSONArray("allData")?:JSONArray()
    val result=mutableListOf<NeteaseSong>()
    for(i in 0 until values.length()){val item=values.optJSONObject(i) ?: continue;val song=item.optJSONObject("song") ?: continue;parseNeteaseSong(song)?.let(result::add)}
    return enrichNeteaseSongs(result)
  }
  private fun searchNeteaseSongs(keyword:String):List<NeteaseSong>{
    val query=java.net.URLEncoder.encode(keyword.trim(),"UTF-8")
    val connection=(URL("https://music.163.com/api/search/get/web?csrf_token=&s=$query&type=1&offset=0&total=true&limit=20").openConnection() as HttpURLConnection).apply{
      connectTimeout=15_000;readTimeout=25_000;instanceFollowRedirects=true
      setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 ShiYu/${BuildConfig.VERSION_NAME}")
      setRequestProperty("Referer","https://music.163.com/")
      setRequestProperty("Accept","application/json,text/plain,*/*")
      neteaseCookie().takeIf{it.isNotBlank()}?.let{setRequestProperty("Cookie",it)}
    }
    try{
      val code=connection.responseCode;if(code !in 200..299)throw java.io.IOException("网易云搜索 HTTP $code")
      val root=JSONObject(read(connection.inputStream));val songs=root.optJSONObject("result")?.optJSONArray("songs")?:JSONArray()
      return enrichNeteaseSongs(buildList{for(i in 0 until songs.length())parseNeteaseSong(songs.optJSONObject(i)?:continue)?.let(::add)})
    }finally{connection.disconnect()}
  }
  private fun listenWithRole(chat:Chat,song:NeteaseSong,announce:Boolean=false){
    if(chat.id!=togetherChatId||listeningChat(chat.id)==null)return
    val card=find(chat.cardId)?:return
    playTogetherSong(song)
    if(!announce)return
    val url=neteaseSongUrl(song.id)
    if(!TogetherListening.shouldAnnounce(chat.messages,song.id))return
    val chatId=chat.id
    val root=relationshipRoot(card.id,chatId);root.put("listens",root.optInt("listens",0)+1).put("lastSong",song.title).put("lastSongArtist",song.artist).put("lastListenedAt",System.currentTimeMillis())
    saveRelationshipRoot(card.id,root,chatId);addRelationshipEvent(card,"一起听了《${song.title}》","${song.artist} · 来自网易云音乐",1,1,chatId)
    val message=Msg("user","${song.title} · ${song.artist}");message.messageType="music";message.delivered=!chat.userBlockedRole;message.meta=JSONObject().put("title",song.title).put("artist",song.artist).put("album",song.album).put("url",url).put("source","netease").put("songId",song.id).put("coverUrl",song.coverUrl).put("chatId",chat.id).put("roleId",card.id).put("roleName",card.name).put("playMode",musicPlayMode()).put("scene","together_listen").toString();chat.messages.add(message);chat.updated=System.currentTimeMillis();save()
    toast("正在和 ${card.name} 一起听《${song.title}》")
  }
  private fun openTogetherListen(chat:Chat,song:NeteaseSong?=null,queue:List<NeteaseSong> = emptyList(),backPage:Page=Page.MUSIC){
    if(listeningChat(chat.id)==null){toast("请选择一个私聊会话");return}
    selectTogetherChat(chat);roleProfileChatId=chat.id;editingCardId=chat.cardId;togetherBackPage=backPage
    if(queue.isNotEmpty()){togetherQueue=(queue+listOfNotNull(song)).distinctBy{it.id};saveTogetherSession()}
    if(song!=null)listenWithRole(chat,song,announce=true)
    composePage=Page.TOGETHER_LISTEN;uiRevision++
  }
  private fun openMusicNotification(intent:Intent){
    val chat=listeningChat(intent.getStringExtra(NeteaseMusicService.EXTRA_CHAT_ID))
    if(chat!=null)openTogetherListen(chat,backPage=Page.MAIN)
    else{musicBackPage=Page.MAIN;composePage=Page.MUSIC;uiRevision++}
  }
  private fun activeProvider():ProviderConfig?{val id=prefs.getString("active_provider_id","")?:"";return providers().firstOrNull{it.id==id}}
  private fun isLocalProviderEndpoint(raw:String):Boolean{val value=raw.trim();if(value.startsWith("local://in-app",true))return true;return runCatching{when(android.net.Uri.parse(value).host?.lowercase(Locale.ROOT)){"localhost","127.0.0.1","0.0.0.0","::1"->true;else->false}}.getOrDefault(false)}
  private fun activateProvider(value:ProviderConfig){prefs.edit().putString("active_provider_id",value.id).putString("provider",value.name).putString("endpoint",value.endpoint).putString("model",value.modelName).apply();runCatching{secure.put(if(value.endpoint.startsWith("local://in-app",true))"local-in-app" else secure.getNamed("provider_key_${value.id}"))};uiRevision++;toast("已切换到 ${value.name}")}
  private fun adultConsentGranted()=prefs.getString("adult_consent_revision","")==adultConsentRevision
  private fun pageDepth(value:Page)=when(value){Page.ADULT_CONFIRM,Page.ONBOARDING,Page.STARTUP_LOADING,Page.COMMUNITY_INVITE,Page.DATA_RESCUE,Page.MAIN->0;Page.FEEDBACK,Page.TERMS,Page.API,Page.TTS,Page.CARD_CENTER,Page.APPEARANCE,Page.DATA,Page.STICKER_RESOURCES,Page.REALITY_AWARENESS,Page.EXPERIMENTAL,Page.DESKTOP_PET,Page.STORIES,Page.STORY_EDIT,Page.SETTINGS_SEARCH,Page.GUIDE,Page.LANGUAGE,Page.ABOUT,Page.SUPPORT,Page.USER_PROFILE,Page.GROUP_SETUP,Page.APP_BEHAVIOR,Page.ROLE_SETTINGS,Page.ROLE_SESSIONS,Page.MUSIC,Page.RENAME_CHAT,Page.CREATE_ROLE,Page.MOMENTS,Page.GENERATION_TASKS,Page.WORLD_GAMES,Page.WORLD_ACTIVITY,Page.WORLD_HOME_3D,Page.WORLD_WARDROBE->1;Page.UPDATE_HISTORY,Page.WEIXIN_LINK,Page.QQ_BOT,Page.IDENTITIES,Page.IMPORT_PREVIEW,Page.CARD_EXPORT,Page.CHAT,Page.ROLE_PROFILE,Page.TOGETHER_LISTEN,Page.FRAME_RATE,Page.CHAT_APPEARANCE,Page.BACKUP,Page.API_ERROR,Page.PROVIDER,Page.CONNECTION,Page.MODEL,Page.LOCAL_MODELS,Page.GENERATION,Page.AUXILIARY_MODELS,Page.MCP_TOOLS,Page.WEB_SEARCH,Page.EMBEDDING_LIBRARY,Page.VIDEO_GENERATION,Page.IMAGE_GENERATION,Page.MEMORY,Page.MOMENT_PERMISSIONS,Page.MOMENT_ROLE_POST,Page.MOMENT_COMMENTS,Page.CHAT_DEFAULTS,Page.COMMON_RULES->2;Page.CHAT_SEARCH,Page.CHAT_DETAIL->3;Page.CHAT_MODEL,Page.CHAT_IDENTITY,Page.CHAT_CONTENT,Page.CHAT_CONTEXT,Page.CHAT_TTS,Page.CHAT_BACKGROUND,Page.MEMORY_SUMMARY,Page.PLOT_NODES,Page.DIARIES,Page.PROACTIVE,Page.YANDERE,Page.MOMENT_SHARE->4;Page.ADD_PROVIDER->5}
  private fun chatStateId(page:Page)=if(page in listOf(Page.CHAT,Page.CHAT_SEARCH,Page.CHAT_DETAIL,Page.CHAT_MODEL,Page.CHAT_IDENTITY,Page.CHAT_CONTENT,Page.CHAT_CONTEXT,Page.CHAT_TTS,Page.CHAT_BACKGROUND,Page.MEMORY_SUMMARY,Page.PLOT_NODES,Page.DIARIES,Page.PROACTIVE,Page.YANDERE,Page.MOMENT_SHARE,Page.RENAME_CHAT))selectedChat?.id else null
  private fun englishUi()=androidx.core.os.ConfigurationCompat.getLocales(resources.configuration)[0]?.language=="en"
  private fun ui(zh:String,en:String)=if(englishUi())en else zh
  private fun safePrefInt(key:String,default:Int)=runCatching{prefs.getInt(key,default)}.getOrDefault(default)
  private fun safePrefBoolean(key:String,default:Boolean)=runCatching{prefs.getBoolean(key,default)}.getOrDefault(default)
  private fun safePrefString(key:String,default:String="")=runCatching{prefs.getString(key,default).orEmpty()}.getOrDefault(default)
  private fun localizedPageTitle(value:String)=if(!englishUi())value else when(value){
    "搜索设置"->"Search settings";"语言"->"Language";"用户资料"->"User profile";"身份设定"->"Identities";"模型设置"->"Model settings";"模型提供商"->"Model providers";"添加模型供应商"->"Add model provider";"编辑模型供应商"->"Edit model provider";"模型"->"Model";"生成参数"->"Generation";"联网搜索 API"->"Web search API";"嵌入模型实时库"->"Embedding library";"生图模型"->"Image generation";"语音模型"->"Voice models";"外观与动画"->"Appearance & motion";"聊天页面样式"->"Chat appearance";"帧率"->"Frame rate";"本地数据"->"Local data";"备份与恢复"->"Backup & restore";"表情资源"->"Sticker resources";"现实感知增强"->"Reality awareness";"实验性功能"->"Experimental";"桌宠模式"->"Desktop pet";"将角色接入微信"->"Connect character to WeChat";"官方 QQ Bot"->"Official QQ Bot";"应用通知"->"App notifications";"朋友圈设置"->"Moments settings";"角色卡中心"->"Character cards";"反馈与建议"->"Feedback";"支持我们"->"Support us";"关于时遇"->"About ShiYu";"更新历史"->"Update history";"对话设置"->"Chat settings";"对话模型"->"Chat model";else->value}
  private fun safeToast(message:String){runCatching{android.widget.Toast.makeText(applicationContext,message,android.widget.Toast.LENGTH_SHORT).show()}}
  private fun copyText(label:String,value:String){
    val copied=runCatching{
      val clipboard=applicationContext.getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager?:error("剪贴板不可用")
      clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label,value))
    }.isSuccess
    safeToast(if(copied)"$label 已复制" else "$label 复制失败")
  }
  private fun tryOpenWebLink(rawUrl:String):Boolean{
    if(isFinishing||isDestroyed)return false
    val uri=runCatching{android.net.Uri.parse(rawUrl.trim())}.getOrNull()?:return false
    val scheme=uri.scheme?.lowercase(Locale.ROOT)
    if((scheme!="http"&&scheme!="https")||uri.host.isNullOrBlank())return false
    val intent=Intent(Intent.ACTION_VIEW,uri).apply{addCategory(Intent.CATEGORY_BROWSABLE)}
    return try{startActivity(intent);true}catch(_:Exception){false}
  }
  private fun openWebLink(rawUrl:String,failureMessage:String):Boolean{
    val opened=tryOpenWebLink(rawUrl)
    if(!opened)safeToast(failureMessage)
    return opened
  }
  private fun openImageStream(reference:String):java.io.InputStream?=runCatching{
    if(reference.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(reference))
    else java.io.FileInputStream(java.io.File(reference))
  }.getOrNull()
  private fun guessImageMime(reference:String):String{
    if(reference.startsWith("content:"))runCatching{contentResolver.getType(android.net.Uri.parse(reference))}.getOrNull()?.takeIf{it!=null&&it.startsWith("image/")}?.let{return it}
    val lower=reference.lowercase(Locale.ROOT)
    return when{lower.endsWith(".gif")->"image/gif";lower.endsWith(".webp")->"image/webp";lower.endsWith(".jpg")||lower.endsWith(".jpeg")->"image/jpeg";else->"image/png"}
  }
  fun saveImageToGallery(reference:String){
    if(reference.isBlank()){safeToast("图片不存在");return}
    if(android.os.Build.VERSION.SDK_INT<29&&checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
      gallerySavePendingReference=reference
      gallerySavePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
      return
    }
    writeImageToGallery(reference)
  }
  private fun writeImageToGallery(reference:String){
    pool.execute{
      runCatching{
        val mime=guessImageMime(reference)
        val stamp=java.text.SimpleDateFormat("yyyyMMdd_HHmmss",java.util.Locale.US).format(java.util.Date())
        val ext=when(mime){"image/gif"->"gif";"image/webp"->"webp";"image/jpeg"->"jpg";else->"png"}
        val values=android.content.ContentValues().apply{
          put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"ShiYu_${stamp}.$ext")
          put(android.provider.MediaStore.Images.Media.MIME_TYPE,mime)
          if(android.os.Build.VERSION.SDK_INT>=29)put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ShiYu")
        }
        val target=contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)?:error("无法创建相册文件")
        val copied=contentResolver.openOutputStream(target)?.use{out->openImageStream(reference)?.use{it.copyTo(out)}!=null}?:false
        if(!copied)error("图片读取失败")
      }.onSuccess{main.post{safeToast("已保存到相册 Pictures/ShiYu")}}.onFailure{main.post{safeToast("保存失败：${it.message}")}}
    }
  }
  fun shareImage(reference:String){
    pool.execute{
      runCatching{
        val shared=java.io.File(cacheDir,"shared").apply{mkdirs()}
        val mime=guessImageMime(reference)
        val ext=when(mime){"image/gif"->"gif";"image/webp"->"webp";"image/jpeg"->"jpg";else->"png"}
        val target=java.io.File(shared,"shiyu_image_${System.currentTimeMillis()}.$ext")
        openImageStream(reference)?.use{input->target.outputStream().use{out->input.copyTo(out)}}?:error("图片读取失败")
        val uri=androidx.core.content.FileProvider.getUriForFile(this,"${packageName}.files",target)
        main.post{
          val intent=Intent(android.content.Intent.ACTION_SEND).setType(mime).putExtra(android.content.Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          startActivity(Intent.createChooser(intent,"分享图片"))
        }
      }.onFailure{main.post{safeToast("分享失败：${it.message}")}}
    }
  }
  private fun finishCommunityInvite(destination:Page=Page.MAIN){prefs.edit().putString("community_invite_version",BuildConfig.VERSION_NAME).apply();composePage=destination}
  private fun nextEntryPage():Page=when{!prefs.getBoolean("onboarding_done",false)->Page.ONBOARDING;prefs.getString("startup_loading_version","")!=BuildConfig.VERSION_NAME->Page.STARTUP_LOADING;prefs.getString("community_invite_version","")!=BuildConfig.VERSION_NAME->Page.COMMUNITY_INVITE;else->Page.MAIN}
  private fun finishStartupLoading(){prefs.edit().putString("startup_loading_version",BuildConfig.VERSION_NAME).apply();composePage=if(prefs.getString("community_invite_version","")!=BuildConfig.VERSION_NAME)Page.COMMUNITY_INVITE else Page.MAIN}
  private fun openQqGroup():Boolean{
    if(isFinishing||isDestroyed)return false
    val url="https://qm.qq.com/q/rfu5wNZqca"
    if(tryOpenWebLink(url))return true
    copyText("QQ 群链接",url)
    return false
  }
  private fun feedbackDiagnostics():String{
    val root=JSONObject().put("appVersion",BuildConfig.VERSION_NAME).put("versionCode",BuildConfig.VERSION_CODE)
      .put("androidSdk",android.os.Build.VERSION.SDK_INT).put("androidRelease",android.os.Build.VERSION.RELEASE)
      .put("device",("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}").trim().take(160))
    val logs=JSONArray();val seen=mutableSetOf<String>()
    fun appendLog(time:Long,source:String,level:String,message:String){
      if(logs.length()>=DiagnosticLog.MAX_ENTRIES)return
      val privacySafe=if(message.contains("API 原始返回："))feedbackErrorSummary(message) else message
      val clean=DiagnosticLog.sanitize(privacySafe,600);if(clean.isBlank())return
      val signature="${source.take(80)}|$clean";if(!seen.add(signature))return
      logs.put(JSONObject().put("time",time).put("source",source.take(80)).put("level",level.take(24)).put("message",clean))
    }
    val stored=DiagnosticLog.recent(this);for(i in 0 until stored.length()){val item=stored.optJSONObject(i)?:continue;appendLog(item.optLong("time"),item.optString("source"),item.optString("level","error"),item.optString("message"))}
    if(lastApiError.isNotBlank())appendLog(System.currentTimeMillis(),"api","error",lastApiError)
    CrashProtection.records(this).forEach{record->appendLog(record.timestamp,"crash_protection","error","${record.featureId}: ${record.reason}${if(record.location.isBlank())"" else " @ ${record.location}"}")}
    listOf("weixin_link_service_error","proactive_last_error","desktop_pet_last_error","desktop_pet_live2d_last_error","qqbot_service_error","moment_auto_last_error","embedding_last_error","offline_stt_download_error","live2d_core_download_error").forEach{key->prefs.getString(key,"")?.takeIf{it.isNotBlank()}?.let{appendLog(System.currentTimeMillis(),key,"error",it)}}
    root.put("logs",logs)
    while(root.toString().length>9_500&&logs.length()>0){var reduced=false;for(i in logs.length()-1 downTo 0){val item=logs.optJSONObject(i)?:continue;val message=item.optString("message");if(message.length<=120)continue;val excess=(root.toString().length-9_400).coerceAtLeast(40);val target=(message.length-excess).coerceAtLeast(120);item.put("message",if(message.length<=target)message else message.take((target-3).coerceAtLeast(1)).trimEnd()+"...");reduced=true;break};if(!reduced)logs.remove(logs.length()-1)}
    return root.toString()
  }
  private fun submitFeedback(category:String,content:String,contact:String,includeLogs:Boolean,callback:(Boolean,String)->Unit){
    if(isFinishing||isDestroyed)return
    runCatching{pool.execute{
      val result=runCatching{
        val connection=URL(BuildConfig.FEEDBACK_API_URL).openConnection() as HttpURLConnection
        try{
          connection.requestMethod="POST";connection.connectTimeout=15000;connection.readTimeout=20000;connection.doOutput=true
          connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("X-ShiYu-App","android")
          val payload=JSONObject().put("category",category).put("content",content).put("contact",contact).put("version",BuildConfig.VERSION_NAME).put("source","android").put("website","").put("diagnostics",if(includeLogs)feedbackDiagnostics() else "").toString()
          connection.outputStream.use{it.write(payload.toByteArray(Charsets.UTF_8))}
          val code=connection.responseCode
          val raw=read(if(code in 200..299)connection.inputStream else connection.errorStream)
          if(code !in 200..299)throw java.io.IOException(runCatching{JSONObject(raw).optString("message","HTTP $code")}.getOrDefault("HTTP $code"))
          JSONObject(raw).optString("reference").ifBlank{"已提交"}
        }finally{connection.disconnect()}
      }
      main.post{
        if(!isFinishing&&!isDestroyed)result.fold(
          onSuccess={reference->callback(true,reference)},
          onFailure={error->callback(false,error.message?:"提交失败")}
        )
      }
    }}.onFailure{error->if(!isFinishing&&!isDestroyed)callback(false,error.message?:"提交任务启动失败")}
  }
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
  private fun removeChat(chat:Chat){clearTogetherSession(chat.id);WeixinLinkClient.accounts(this).filter{it.chatId==chat.id}.forEach{WeixinLinkClient.setChatId(this,it.id,"")};immersiveReplyTasks.remove(chat.id)?.let{main.removeCallbacks(it)};clearPersistedImmersiveReply(chat.id);if(isChatSending(chat.id))stopGeneration(chat.id);queuedReplyTargets.remove(chat.id);queuedMentionTargets.remove(chat.id);crossChatQueue.removeIf{it==chat.id};db.deleteChatRealtime(chat.id);val relationshipCards=if(chat.group)chat.cardIds else listOf(chat.cardId);val relationshipEditor=prefs.edit();relationshipCards.filter{it.isNotBlank()}.forEach{relationshipEditor.remove(relationshipKey(it,chat.id))};relationshipEditor.apply();chats.removeAll{it.id==chat.id};if(roleProfileChatId==chat.id)roleProfileChatId=null;if(selectedChat?.id==chat.id){selectedChat=null;composePage=Page.MAIN};save();YandereMessageService.sync(this);uiRevision++}
  private fun removeCard(card:Card){
    val deleted=mutableListOf<Chat>()
    chats.forEach{chat->if(!chat.group){if(chat.cardId==card.id)deleted.add(chat)}else if(chat.cardIds.remove(card.id)){if(chat.activeCardId==card.id)chat.activeCardId=chat.cardIds.firstOrNull().orEmpty();if(chat.cardId==card.id)chat.cardId=chat.cardIds.firstOrNull().orEmpty();if(chat.cardIds.size<2)deleted.add(chat)}}
    deleted.forEach{clearTogetherSession(it.id)}
    deleted.forEach{immersiveReplyTasks.remove(it.id)?.let{task->main.removeCallbacks(task)};clearPersistedImmersiveReply(it.id);if(isChatSending(it.id))stopGeneration(it.id);queuedReplyTargets.remove(it.id);queuedMentionTargets.remove(it.id);crossChatQueue.removeIf{id->id==it.id};db.deleteChatRealtime(it.id)};val relationshipEditor=prefs.edit();prefs.all.keys.filter{it==relationshipKey(card.id)||it.startsWith("role_relationship_${card.id}_chat_")}.forEach{relationshipEditor.remove(it)};relationshipEditor.apply();chats.removeAll(deleted.toSet());WeixinLinkClient.disconnectCard(this,card.id);cards.removeAll{it.id==card.id};db.deleteCardMemories(card.id);db.deleteCardRealtime(card.id);if(card.avatar.isNotBlank())runCatching{File(card.avatar).delete()};if(selectedChat!=null&&!chats.any{it.id==selectedChat?.id}){selectedChat=null;composePage=Page.MAIN};save();YandereMessageService.sync(this);DesktopPetService.sync(this);uiRevision++
  }
  private fun discardPendingImport(){pendingImportCard?.avatar?.takeIf{it.isNotBlank()}?.let{path->val file=File(path);if(file.parentFile==cacheDir){bitmapCache.remove(path);runCatching{file.delete()}}};pendingImportCard=null;composePage=Page.CARD_CENTER}
  private fun confirmPendingImport(){val card=pendingImportCard?:return;runCatching{card.avatar.takeIf{it.isNotBlank()}?.let{path->val source=File(path);if(source.parentFile==cacheDir&&source.exists()){val target=File(filesDir,"avatar_${card.id}.png");source.copyTo(target,overwrite=true);bitmapCache.remove(path);source.delete();card.avatar=target.absolutePath}};cards.add(card);save()}.onSuccess{pendingImportCard=null;uiRevision++;toast("已导入 ${card.name}");composePage=Page.CARD_CENTER}.onFailure{toast("导入保存失败：${it.message}")}}
  private fun handleIncomingCardIntent(incoming:Intent?){
    if(incoming==null)return
    val action=incoming.action
    if(action !in listOf(Intent.ACTION_SEND,Intent.ACTION_SEND_MULTIPLE,Intent.ACTION_VIEW))return
    val uris=runCatching{
      buildList {
        if(action==Intent.ACTION_SEND_MULTIPLE){
          androidx.core.content.IntentCompat.getParcelableArrayListExtra(incoming,Intent.EXTRA_STREAM,android.net.Uri::class.java)?.let{addAll(it)}
        }else{
          androidx.core.content.IntentCompat.getParcelableExtra(incoming,Intent.EXTRA_STREAM,android.net.Uri::class.java)?.let{add(it)}
        }
        incoming.data?.let{add(it)}
        incoming.clipData?.let{clip->repeat(clip.itemCount){index->clip.getItemAt(index).uri?.let{add(it)}}}
      }.filter{it.scheme in listOf("content","file")}.distinct()
    }.getOrElse{error->toast("无法读取分享文件：${friendlyError(error)}");return}
    if(uris.isEmpty()){toast("未收到可读取的文件，请使用文件的分享或其他应用打开功能");return}
    uris.filterNot{sharedCardImportQueue.contains(it)}.forEach{sharedCardImportQueue.addLast(it)}
    openNextSharedCard()
  }

  private fun openNextSharedCard(){
    if(sharedCardImportRunning||pendingImportCard!=null||sharedCardImportQueue.isEmpty())return
    if(hasDataLoadFailure()||!adultConsentGranted())return
    if(composePage in listOf(Page.ADULT_CONFIRM,Page.ONBOARDING,Page.STARTUP_LOADING,Page.COMMUNITY_INVITE,Page.DATA_RESCUE))return
    val uri=sharedCardImportQueue.removeFirst()
    sharedCardImportRunning=true
    pool.execute{
      runCatching{
        val name=fileName(uri).ifBlank{uri.lastPathSegment?.substringAfterLast('/')?:"shared-card"}
        val data=contentResolver.openInputStream(uri)?.use{input->
          val out=ByteArrayOutputStream();val buffer=ByteArray(16*1024);var total=0L
          while(true){val count=input.read(buffer);if(count<0)break;total+=count;if(total>20L*1024*1024)error("角色卡文件超过 20 MB");out.write(buffer,0,count)}
          out.toByteArray()
        }?:error("无法读取分享文件")
        parseCardClean(data,name).also{card->if(isPng(data)){val file=File(cacheDir,"shared_import_${card.id}.png");FileOutputStream(file).use{it.write(data)};card.avatar=file.absolutePath}}
      }.onSuccess{card->main.post{sharedCardImportRunning=false;pendingImportCard=card;composePage=Page.IMPORT_PREVIEW;uiRevision++;toast("已识别角色卡：${card.name}")}}.onFailure{error->main.post{sharedCardImportRunning=false;DiagnosticLog.record(this,"shared_import","error",diagnosticSummary(error));toast("分享导入失败：${friendlyError(error)}");openNextSharedCard()}}
    }
  }
  private fun downloadCharacterCard(rawUrl:String):Card{
    val parsed=android.net.Uri.parse(rawUrl.trim());require(parsed.scheme in listOf("http","https")){"链接必须使用 http 或 https"}
    val connection=(URL(rawUrl.trim()).openConnection() as HttpURLConnection).apply{connectTimeout=20_000;readTimeout=60_000;instanceFollowRedirects=true;setRequestProperty("User-Agent","ShiYu-Android/${BuildConfig.VERSION_NAME}");setRequestProperty("Accept","application/json,image/png,application/octet-stream,*/*")}
    try{
      val code=connection.responseCode;if(code !in 200..299)throw java.io.IOException("角色卡服务器 HTTP $code")
      val max=12L*1024*1024;val declared=connection.contentLengthLong;if(declared>max)throw java.io.IOException("角色卡超过 12 MB 安全限制")
      val output=ByteArrayOutputStream();connection.inputStream.use{input->val buffer=ByteArray(16*1024);var total=0L;while(true){val count=input.read(buffer);if(count<=0)break;total+=count;if(total>max)throw java.io.IOException("角色卡超过 12 MB 安全限制");output.write(buffer,0,count)}}
      val data=output.toByteArray();val disposition=connection.getHeaderField("Content-Disposition").orEmpty();val headerName=Regex("filename\\*?=(?:UTF-8''|\\\")?([^;\\\"]+)",RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.getOrNull(1)?.let{java.net.URLDecoder.decode(it,"UTF-8")};val fallback=parsed.lastPathSegment?.substringBefore('?').orEmpty().ifBlank{"character-card.json"};val card=parseCardClean(data,headerName?:fallback)
      if(isPng(data)){val file=File(cacheDir,"import_preview_${card.id}.png");FileOutputStream(file).use{it.write(data)};card.avatar=file.absolutePath}
      return card
    }finally{connection.disconnect()}
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    gallerySavePermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
      if(granted)writeImageToGallery(gallerySavePendingReference)else safeToast("需要存储权限才能保存到相册")
    }
    voiceCallScreenPermission=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
      val data=result.data
      if(result.resultCode==Activity.RESULT_OK&&data!=null&&voiceCallSessionId.isNotBlank()){
        ScreenShareService.start(this,result.resultCode,data)
      }else if(voiceCallSessionId.isNotBlank())toast("已取消屏幕共享")
    }
    voiceCallReceiver=object:android.content.BroadcastReceiver(){
      override fun onReceive(context:android.content.Context?,intent:Intent?){
        when(intent?.action){
          VoiceCallOverlayService.ACTION_COMMAND->handleVoiceCallCommand(intent.getStringExtra(VoiceCallOverlayService.EXTRA_COMMAND).orEmpty())
          ScreenShareService.ACTION_STATE->{
            voiceCallScreenSharing=intent.getBooleanExtra(ScreenShareService.EXTRA_ACTIVE,false)
            if(voiceCallScreenSharing&&!voiceCallVisualReady())toast("屏幕已共享；配置视觉辅助模型后 AI 才能看懂画面")
            syncVoiceCallOverlay();uiRevision++
          }
          ScreenShareService.ACTION_FRAME->{
            voiceCallScreenImagePath=intent.getStringExtra(ScreenShareService.EXTRA_PATH).orEmpty()
            syncVoiceCallOverlay();uiRevision++
          }
        }
      }
    }
    val voiceFilter=android.content.IntentFilter().apply{addAction(VoiceCallOverlayService.ACTION_COMMAND);addAction(ScreenShareService.ACTION_STATE);addAction(ScreenShareService.ACTION_FRAME)}
    androidx.core.content.ContextCompat.registerReceiver(this,voiceCallReceiver,voiceFilter,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    musicReceiver=object:android.content.BroadcastReceiver(){
      override fun onReceive(context:android.content.Context?,intent:Intent?){
        if(intent?.action!=NeteaseMusicService.ACTION_STATE||!applyMusicState(intent))return
        intent.getStringExtra(NeteaseMusicService.EXTRA_ERROR)?.takeIf{it.isNotBlank()}?.let{toast("音乐播放失败：$it")}
        val chat=activeTogetherChat()?:return
        val completion=JSONObject().put("chatId",intent.getStringExtra(NeteaseMusicService.EXTRA_CHAT_ID))
          .put("roleId",intent.getStringExtra(NeteaseMusicService.EXTRA_ROLE_ID))
          .put("playbackId",intent.getStringExtra(NeteaseMusicService.EXTRA_PLAYBACK_ID))
          .put("status",intent.getStringExtra(NeteaseMusicService.EXTRA_STATUS))
          .put("songId",intent.getLongExtra(NeteaseMusicService.EXTRA_SONG_ID,0L))
          .put("completed",intent.getBooleanExtra(NeteaseMusicService.EXTRA_COMPLETED,false))
        if(TogetherListening.canAdvance(completion,chat.id,chat.cardId,musicPlaybackId,togetherSong?.id?:0L))autoAdvanceTogether()
      }
    }
    androidx.core.content.ContextCompat.registerReceiver(this,musicReceiver,android.content.IntentFilter(NeteaseMusicService.ACTION_STATE),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    syncMusicStateFromPrefs()
    WindowCompat.setDecorFitsSystemWindows(window,false)
    restoreImmersiveReplies()
    composePage=when{hasDataLoadFailure()->Page.DATA_RESCUE;!adultConsentGranted()->Page.ADULT_CONFIRM;else->nextEntryPage()}
    handleIncomingCardIntent(intent)
    if(intent?.getBooleanExtra("open_moments",false)==true)requestedMainTab=2
    if(composePage==Page.MAIN&&prefs.getBoolean("reopen_language_settings",false)){prefs.edit().remove("reopen_language_settings").apply();requestedMainTab=3;composePage=Page.LANGUAGE}
    if(composePage==Page.MAIN&&intent?.getBooleanExtra("open_desktop_pet_settings",false)==true)composePage=Page.DESKTOP_PET
    if(composePage==Page.MAIN)intent?.getStringExtra("open_chat_id")?.takeIf{it.isNotBlank()}?.let{id->chats.firstOrNull{it.id==id}?.let{selectedChat=it;composePage=Page.CHAT}}
    if(intent?.getBooleanExtra("open_music",false)==true)openMusicNotification(intent)
    handleVoiceCallIntent(intent)
    setContent {
      uiRevision
      var themeMode by remember { mutableIntStateOf(safePrefInt("compose_theme", 0)) }
      var themeStyle by remember { mutableIntStateOf(safePrefInt("compose_theme_style", 0)) }
      val systemDark = isSystemInDarkTheme()
      val dark = themeMode == 2 || (themeMode == 0 && systemDark)
      val baseColors = themePalette(themeStyle,dark)
      val colors = if(safePrefBoolean("experimental_glass_ui",false)) glassPalette(baseColors,safePrefInt("experimental_glass_opacity",78)) else baseColors
      MaterialTheme(colorScheme = colors, typography = Typography()) {
        val transparentChatStatus=composePage==Page.CHAT&&safePrefBoolean("experimental_top_bar_transparent",false)
        val activeChat=selectedChat.takeIf{composePage==Page.CHAT}
        val statusBackground=storedColor(activeChat?.backgroundColor?.ifBlank{safePrefString("chat_background_color")}?:safePrefString("chat_background_color"),MaterialTheme.colorScheme.background)
        val statusImage=activeChat?.backgroundImage?.ifBlank{safePrefString("chat_background_image")}?:safePrefString("chat_background_image")
        val statusOverlay=(activeChat?.backgroundOverlay?.takeIf{it>=0}?:safePrefInt("chat_background_overlay",20)).coerceIn(0,70)/100f
        val darkStatusIcons=remember(transparentChatStatus,statusBackground,statusImage,statusOverlay,dark){if(transparentChatStatus)chatStatusBarDarkIcons(statusBackground,statusImage,statusOverlay)else !dark}
        val togetherBars=composePage==Page.TOGETHER_LISTEN
        SyncSystemBars(dark,transparentChatStatus||togetherBars,true,if(togetherBars)false else darkStatusIcons,null)
        ShiYuApp(themeMode,{ value -> themeMode = value },themeStyle) { value -> themeStyle = value }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingCardIntent(intent)
    handleVoiceCallIntent(intent)
    if(hasDataLoadFailure()){
      selectedChat=null
      composePage=Page.DATA_RESCUE
      uiRevision++
      return
    }
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
    if(intent.getBooleanExtra("open_desktop_pet_settings",false)){selectedChat=null;composePage=Page.DESKTOP_PET;uiRevision++}
    if(intent.getBooleanExtra("open_moments",false)){requestedMainTab=2;composePage=Page.MAIN;mainTabRequestNonce++;uiRevision++}
    if(intent.getBooleanExtra("open_music",false))openMusicNotification(intent)
  }

  override fun onUpdateAvailable(version: String, url: String) {
    if(isFinishing||isDestroyed)return
    availableUpdateVersion=version
    availableUpdateUrl=url
    updateNoticeNonce++
    uiRevision++
  }

  override fun onRemoteAnnouncement(announcement: JSONObject) {
    if(isFinishing||isDestroyed)return
    val id=announcement.optString("id","").trim()
    if(id.isBlank())return
    if(remoteAnnouncementSessionSeen.contains(id))return
    remoteAnnouncementSessionSeen.add(id)
    remoteAnnouncementJson=announcement.toString()
    remoteAnnouncementNonce++
    uiRevision++
  }

  override fun onSpeechStateChanged() {
    if(voiceCallSessionId.isNotBlank()&&voiceCallPhase==VoiceCallPhase.SPEAKING&&!speechSpeaking){
      if(speechError.isNotBlank()){voiceCallPhase=VoiceCallPhase.ERROR;voiceCallStatus=speechError}
      else{
        voiceCallPhase=VoiceCallPhase.READY
        voiceCallStatus=if(voiceCallMicMuted)"麦克风已关闭" else "准备继续聆听"
        if(!voiceCallMicMuted)voiceCallPendingCommand="listen:${SystemClock.elapsedRealtimeNanos()}"
      }
    }
    syncVoiceCallOverlay()
    uiRevision++
  }

  override fun onVoiceCallReplyCompleted(callId:String,chatId:String,cardId:String,text:String){
    if(callId!=voiceCallSessionId||chatId!=voiceCallChatId||cardId!=voiceCallCardId
      ||voiceCallPhase!=VoiceCallPhase.THINKING)return
    val spoken=text.trim()
    if(spoken.isBlank()){
      voiceCallPhase=VoiceCallPhase.ERROR;voiceCallStatus="角色没有返回可播放的语音内容";uiRevision++;return
    }
    voiceCallLastReplyText=spoken
    voiceCallPhase=VoiceCallPhase.SPEAKING
    voiceCallStatus="角色正在说话"
    speakVoiceCall(spoken)
    syncVoiceCallOverlay()
    uiRevision++
  }

  override fun onVoiceCallReplyFailed(callId:String,chatId:String,cardId:String,error:String){
    if(callId!=voiceCallSessionId||chatId!=voiceCallChatId||cardId!=voiceCallCardId)return
    voiceCallPhase=VoiceCallPhase.ERROR
    voiceCallStatus=error.ifBlank{"角色回复失败"}
    syncVoiceCallOverlay()
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
    if(voiceCallSessionId.isNotBlank()&&voiceCallFloating){voiceCallFloating=false;VoiceCallOverlayService.hide(this)}
    runCatching{CrashProtection.consumeNotice(this)}.getOrDefault("").takeIf{it.isNotBlank()}?.let{toast(it)}
    if(hasDataLoadFailure()){selectedChat=null;composePage=Page.DATA_RESCUE;uiRevision++;return}
    syncBackgroundStateIfNeeded(false)
    WeixinLinkService.sync(this)
    QqBotService.sync(this)
    DesktopPetService.sync(this)
    selectedChat?.id?.let { id -> selectedChat = chats.firstOrNull { it.id == id } }
    if(composePage==Page.CHAT)selectedChat?.id?.let{id->prefs.edit().putString("visible_chat_id",id).apply()}
    uiRevision++
  }

  override fun onDataLoadStateChanged(failed:Boolean){
    super.onDataLoadStateChanged(failed)
    if(!failed)return
    val showRescue:()->Unit={
      listOf(WeixinLinkService::class.java,QqBotService::class.java,DesktopPetService::class.java,YandereMessageService::class.java,NotificationKeepAliveService::class.java).forEach{service->runCatching{stopService(Intent(this,service))}}
      selectedChat=null;composePage=Page.DATA_RESCUE;uiRevision++
    }
    if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())showRescue() else main.post{showRescue()}
  }

  override fun chat(x: Chat) {
    if(!adultConsentGranted()){
      selectedChat=null
      composePage=Page.ADULT_CONFIRM
      uiRevision++
      return
    }
    selectedChat = x
    if(!x.group)roleProfileChatId=x.id
    composePage = Page.CHAT
    uiRevision++
  }

  override fun refreshChatUi(x: Chat) {
    if (selectedChat?.id == x.id && composePage in listOf(Page.CHAT, Page.CHAT_DETAIL)) chatUiRevision++
    if (composePage == Page.TOGETHER_LISTEN && togetherChatId == x.id) uiRevision++
  }

  @Composable
  private fun SyncSystemBars(dark: Boolean, transparentStatus: Boolean, transparentNavigation: Boolean, darkStatusIcons: Boolean, barColor:Color?=null) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background
    val resolvedBarColor=barColor?:background
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(window,false)
      window.statusBarColor = if(transparentStatus)AndroidColor.TRANSPARENT else resolvedBarColor.toArgb()
      window.navigationBarColor = if(transparentNavigation)AndroidColor.TRANSPARENT else resolvedBarColor.toArgb()
      if(android.os.Build.VERSION.SDK_INT>=28)window.navigationBarDividerColor=AndroidColor.TRANSPARENT
      if(android.os.Build.VERSION.SDK_INT>=29)window.isNavigationBarContrastEnforced=!transparentNavigation
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = darkStatusIcons
        isAppearanceLightNavigationBars = if(transparentNavigation)darkStatusIcons else !dark
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
    LaunchedEffect(composePage){openNextSharedCard()}
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage=requestedMainTab.coerceIn(0,3),pageCount = { 4 })
    val mainListStates = List(4) { rememberLazyListState() }
    val pageStateHolder = rememberSaveableStateHolder()
    val selectedTab by remember { derivedStateOf { pagerState.currentPage } }
    val bottomBarHidden by remember {
      derivedStateOf {
        val listState = mainListStates[pagerState.currentPage]
        listState.canScrollBackward && !listState.canScrollForward
      }
    }
    var showUpdateDialog by rememberSaveable{mutableStateOf(false)}
    LaunchedEffect(updateNoticeNonce) {
      val mutedUntil=prefs.getLong("update_mute_until_$availableUpdateVersion",0L)
      if(updateNoticeNonce>0&&availableUpdateVersion.isNotBlank()&&System.currentTimeMillis()>=mutedUntil)showUpdateDialog=true
    }
    var showAnnouncementDialog by rememberSaveable{mutableStateOf(false)}
    LaunchedEffect(remoteAnnouncementNonce) {
      if(remoteAnnouncementNonce>0&&remoteAnnouncementJson.isNotBlank())showAnnouncementDialog=true
    }
    LaunchedEffect(mainTabRequestNonce){if(mainTabRequestNonce>0)scope.launch{pagerState.scrollToPage(requestedMainTab)}}

    BackHandler(enabled = composePage != Page.MAIN || selectedTab != 0) {
      when {
        composePage == Page.ADULT_CONFIRM -> finish()
        composePage == Page.ONBOARDING -> finish()
        composePage == Page.STARTUP_LOADING -> finishStartupLoading()
        composePage == Page.COMMUNITY_INVITE -> finishCommunityInvite()
        composePage == Page.FEEDBACK -> composePage = Page.MAIN
        composePage == Page.TERMS -> composePage = termsBackPage
        composePage == Page.DATA_RESCUE -> finish()
        composePage == Page.CHAT -> { selectedChat = null; composePage = Page.MAIN; scope.launch{pagerState.scrollToPage(0)} }
        composePage == Page.CHAT_SEARCH -> composePage = Page.CHAT
        composePage == Page.CHAT_DETAIL -> composePage = Page.CHAT
        composePage == Page.CHAT_MODEL -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_IDENTITY -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_CONTENT -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_CONTEXT -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_TTS -> composePage = Page.CHAT_DETAIL
        composePage == Page.CHAT_BACKGROUND -> composePage = Page.CHAT_DETAIL
        composePage == Page.MEMORY_SUMMARY -> composePage = Page.CHAT_DETAIL
        composePage == Page.PLOT_NODES -> composePage = Page.CHAT
        composePage == Page.DIARIES -> composePage = Page.CHAT
        composePage == Page.PROACTIVE -> composePage = Page.CHAT_DETAIL
        composePage == Page.YANDERE -> composePage = Page.CHAT_DETAIL
        composePage == Page.MOMENT_SHARE -> composePage = Page.CHAT
        composePage == Page.CHAT_DEFAULTS -> composePage = Page.APP_BEHAVIOR
        composePage == Page.COMMON_RULES -> composePage = Page.APP_BEHAVIOR
        composePage == Page.GENERATION_TASKS -> composePage = Page.MAIN
        composePage == Page.WEB_SEARCH -> composePage = webSearchBackPage
        composePage == Page.IMPORT_PREVIEW -> discardPendingImport()
        composePage == Page.CARD_EXPORT -> { exportCardId=null;composePage=Page.CARD_CENTER }
        composePage == Page.MEMORY -> composePage = Page.ROLE_SETTINGS
        composePage == Page.ROLE_PROFILE -> composePage = roleProfileBackPage
        composePage == Page.ROLE_SESSIONS -> composePage = Page.ROLE_PROFILE
        composePage == Page.MUSIC -> composePage = musicBackPage
        composePage == Page.TOGETHER_LISTEN -> composePage = togetherBackPage
        composePage in listOf(Page.WORLD_GAMES,Page.WORLD_ACTIVITY,Page.WORLD_HOME_3D,Page.WORLD_WARDROBE) -> composePage = Page.MAIN
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
        composePage == Page.DESKTOP_PET -> composePage = Page.MAIN
        composePage == Page.REALITY_AWARENESS -> composePage = Page.MAIN
        composePage == Page.EXPERIMENTAL -> composePage = Page.MAIN
        composePage == Page.WEIXIN_LINK -> composePage = Page.EXPERIMENTAL
        composePage == Page.QQ_BOT -> composePage = Page.EXPERIMENTAL
        composePage == Page.SETTINGS_SEARCH -> composePage = Page.MAIN
        composePage == Page.LANGUAGE -> composePage = Page.MAIN
        composePage == Page.UPDATE_HISTORY -> composePage = Page.ABOUT
        composePage == Page.IMAGE_GENERATION -> composePage = imageGenerationBackPage
        composePage == Page.VIDEO_GENERATION -> composePage = videoGenerationBackPage
        composePage == Page.BACKUP -> composePage = Page.DATA
        composePage == Page.EMBEDDING_LIBRARY -> composePage=embeddingBackPage
        composePage == Page.PROVIDER -> composePage = providerListBackPage
        composePage == Page.API_ERROR -> composePage = apiErrorBackPage
        composePage in listOf(Page.CONNECTION,Page.MODEL,Page.LOCAL_MODELS,Page.GENERATION,Page.AUXILIARY_MODELS,Page.MCP_TOOLS) -> composePage = Page.API
        composePage != Page.MAIN -> composePage = Page.MAIN
        selectedTab != 0 -> scope.launch { pagerState.animateScrollToPage(0, animationSpec = tween(240,easing=FastOutSlowInEasing)) }
      }
    }

    val glassUi = safePrefBoolean("experimental_glass_ui",false)
    Surface(Modifier.fillMaxSize(),color=if(glassUi)Color.Transparent else MaterialTheme.colorScheme.background,contentColor=MaterialTheme.colorScheme.onBackground) {
      val backdrop = if(glassUi) Modifier.background(MaterialTheme.colorScheme.background) else Modifier
      Box(Modifier.fillMaxSize().then(backdrop)) {
      AnimatedContent(
        targetState = Pair(composePage, chatStateId(composePage)),
        transitionSpec = {
          val forward=pageDepth(targetState.first)>pageDepth(initialState.first)
          if(forward)(fadeIn(tween(125))+slideInHorizontally(tween(185)){it/3}) togetherWith (fadeOut(tween(90))+slideOutHorizontally(tween(155)){-it/8})
          else (fadeIn(tween(125))+slideInHorizontally(tween(185)){-it/4}) togetherWith (fadeOut(tween(90))+slideOutHorizontally(tween(165)){it/3})
        },
        modifier = Modifier.fillMaxSize(), label = "shiyu-navigation"
      ) { state -> pageStateHolder.SaveableStateProvider("${state.first.name}:${state.second.orEmpty()}") { Box(Modifier.fillMaxSize().then(if(state.first in listOf(Page.CHAT,Page.WORLD_HOME_3D,Page.TOGETHER_LISTEN))Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing))){
        when (state.first) {
          Page.ADULT_CONFIRM -> AdultConfirmPage()
          Page.ONBOARDING -> OnboardingPage()
          Page.STARTUP_LOADING -> StartupLoadingPage()
          Page.COMMUNITY_INVITE -> CommunityInvitePage()
          Page.FEEDBACK -> FeedbackPage()
          Page.TERMS -> TermsPage()
          Page.DATA_RESCUE -> DataRescuePage()
          Page.CHAT -> selectedChat?.let { ChatPage(it) }
          Page.CHAT_SEARCH -> selectedChat?.let { ChatSearchPage(it) }
          Page.CHAT_DETAIL -> selectedChat?.let { ChatDetailPage(it) }
          Page.CHAT_MODEL -> selectedChat?.let { ChatModelPage(it) }
          Page.CHAT_IDENTITY -> selectedChat?.let { ChatIdentityPage(it) }
          Page.CHAT_CONTENT -> selectedChat?.let { ChatContentPage(it) }
          Page.CHAT_CONTEXT -> selectedChat?.let { ChatContextPage(it) }
          Page.CHAT_TTS -> selectedChat?.let { ChatTtsPage(it) }
          Page.CHAT_BACKGROUND -> selectedChat?.let { ChatBackgroundPage(it) }
          Page.MEMORY_SUMMARY -> selectedChat?.let { MemorySummaryPage(it) }
          Page.PLOT_NODES -> selectedChat?.let { PlotNodesPage(it) }
          Page.DIARIES -> selectedChat?.let { DiaryPage(it) }
          Page.PROACTIVE -> selectedChat?.let { ProactiveSettingsPage(it) }
          Page.YANDERE -> selectedChat?.let { YandereSettingsPage(it) }
          Page.MOMENT_SHARE -> selectedChat?.let { MomentSharePage(it) }
          Page.CHAT_DEFAULTS -> ChatDefaultsPage()
          Page.COMMON_RULES -> CommonRulesPage()
          Page.STORIES -> StoriesPage()
          Page.STORY_EDIT -> StoryEditPage()
          Page.GENERATION_TASKS -> GenerationTasksPage()
          Page.ROLE_SETTINGS -> RoleSettingsPage()
          Page.ROLE_PROFILE -> RoleProfilePage()
          Page.ROLE_SESSIONS -> RoleSessionsPage()
          Page.MUSIC -> NeteaseMusicPage()
          Page.TOGETHER_LISTEN -> TogetherListenPage()
          Page.WORLD_GAMES -> WorldGamesPage()
          Page.WORLD_ACTIVITY -> WorldActivityPage()
          Page.WORLD_HOME_3D -> WorldHome3dPage()
          Page.WORLD_WARDROBE -> WorldWardrobePage()
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
          Page.LOCAL_MODELS -> LocalModelsPage()
          Page.GENERATION -> GenerationPage()
          Page.AUXILIARY_MODELS -> AuxiliaryModelsPage()
          Page.MCP_TOOLS -> McpToolsPage()
          Page.WEB_SEARCH -> WebSearchPage()
          Page.EMBEDDING_LIBRARY -> EmbeddingLibraryPage()
          Page.IMAGE_GENERATION -> ImageGenerationPage()
          Page.VIDEO_GENERATION -> VideoStudio(this@ComposeActivity,onBack={composePage=videoGenerationBackPage},onSend=if(videoGenerationBackPage==Page.CHAT&&selectedChat!=null){{task->
            val chat=selectedChat
            if(chat!=null&&!sending&&!hasDataLoadFailure()){
              val message=Msg("user","AI 生成视频：${task.prompt}").apply{messageType="video";meta=JSONObject().put("file",task.file).put("prompt",task.prompt).toString()}
              chat.messages.add(message);chat.updated=System.currentTimeMillis();save();uiRevision++;composePage=Page.CHAT;toast("视频已添加到当前会话")
            }else toast("请等待当前回复完成后再添加视频")
          }}else null)
          Page.TTS -> TtsPage()
          Page.APPEARANCE -> AppearancePage(themeMode, setTheme,themeStyle,setThemeStyle)
          Page.CHAT_APPEARANCE -> ChatAppearancePage()
          Page.FRAME_RATE -> FrameRatePage()
          Page.DATA -> DataPage()
          Page.BACKUP -> BackupPage()
          Page.STICKER_RESOURCES -> StickerResourcesPage()
          Page.REALITY_AWARENESS -> RealityAwarenessPage()
          Page.EXPERIMENTAL -> ExperimentalPage()
          Page.DESKTOP_PET -> DesktopPetPage()
          Page.WEIXIN_LINK -> WeixinLinkPage()
          Page.QQ_BOT -> QqBotPage()
          Page.SETTINGS_SEARCH -> SettingsSearchPage()
          Page.GUIDE -> GuidePage()
          Page.LANGUAGE -> LanguagePage()
          Page.ABOUT -> AboutPage()
          Page.UPDATE_HISTORY -> UpdateHistoryPage()
          Page.SUPPORT -> SupportPage()
          Page.USER_PROFILE -> UserProfilePage()
          Page.IDENTITIES -> IdentitiesPage()
          Page.APP_BEHAVIOR -> AppBehaviorPage()
          Page.MAIN -> HorizontalPager(state=pagerState,beyondBoundsPageCount=3,modifier=Modifier.fillMaxSize()){index->when(index){0->ConversationPage(mainListStates[index]){composePage=Page.GROUP_SETUP};1->RolePage(mainListStates[index]);2->MomentsHomePage(mainListStates[index]);else->ProfilePage(mainListStates[index])}}
        }
      }}}
      BottomBarDissolve(
        visible=composePage==Page.MAIN&&!bottomBarHidden,
        modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
      ){enabled->ShiYuBottomBar(selectedTab,enabled){index->snackbar.currentSnackbarData?.dismiss();if(index!=pagerState.currentPage)scope.launch{pagerState.animateScrollToPage(index,animationSpec=tween(260,easing=FastOutSlowInEasing))}}}
      SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=82.dp))
      if(showUpdateDialog)AlertDialog(onDismissRequest={showUpdateDialog=false},icon={Icon(Icons.Outlined.NewReleases,null)},title={Text("发现时遇 $availableUpdateVersion")},text={Column(Modifier.heightIn(max=320.dp).verticalScroll(rememberScrollState())){Text(prefs.getString("update_notes","新版本已经可以下载。")?:"新版本已经可以下载。",fontSize=14.sp,lineHeight=21.sp);Spacer(Modifier.height(10.dp));Text("更新包来自时遇官网，下载前可在关于页面再次查看版本信息。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}},confirmButton={Button(onClick={showUpdateDialog=false;openWebLink(availableUpdateUrl,"更新链接无效或无法打开")}){Text("立即下载")}},dismissButton={Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={showUpdateDialog=false}){Text("稍后")};TextButton(onClick={prefs.edit().putLong("update_mute_until_$availableUpdateVersion",System.currentTimeMillis()+24L*60L*60L*1000L).apply();showUpdateDialog=false}){Text("24 小时内不再显示")}}})
      if(showAnnouncementDialog){
        val announcement=runCatching{JSONObject(remoteAnnouncementJson)}.getOrNull()
        if(announcement==null){showAnnouncementDialog=false}else{
          val annId=announcement.optString("id","")
          val annType=announcement.optString("type","once")
          val annTitle=announcement.optString("title","时遇公告").ifBlank{"时遇公告"}
          val annContent=announcement.optString("content","")
          val dismissAnnouncement={showAnnouncementDialog=false;if(annType!="always")markRemoteAnnouncementSeen(annId)}
          Dialog(onDismissRequest={dismissAnnouncement()}){
            Surface(shape=RoundedCornerShape(18.dp),tonalElevation=6.dp,shadowElevation=16.dp,modifier=Modifier.fillMaxWidth()){
              Column(Modifier.padding(20.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                  Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Campaign,null,tint=MaterialTheme.colorScheme.primary)}
                  Spacer(Modifier.width(12.dp))
                  Column{Text("时遇公告",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(annTitle,fontSize=17.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)}
                }
                Spacer(Modifier.height(12.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.26f));Spacer(Modifier.height(12.dp))
                Text(annContent,fontSize=14.sp,lineHeight=22.sp,modifier=Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState()))
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
                  if(annType=="always"){OutlinedButton(onClick={showAnnouncementDialog=false},modifier=Modifier.weight(1f)){Text("关闭")}}
                  Button(onClick={dismissAnnouncement()},modifier=Modifier.weight(1f)){Icon(Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("我知道了",maxLines=1,softWrap=false)}
                }
              }
            }
          }
        }
      }
      pendingDanger?.let{danger->AlertDialog(onDismissRequest={pendingDanger=null},icon={Icon(Icons.Outlined.WarningAmber,null,tint=MaterialTheme.colorScheme.error)},title={Text(danger.title)},text={Text(danger.message,fontSize=14.sp,lineHeight=21.sp)},confirmButton={Button(onClick={pendingDanger=null;danger.action()},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error,contentColor=MaterialTheme.colorScheme.onError)){Text(danger.confirmLabel)}},dismissButton={TextButton(onClick={pendingDanger=null}){Text("取消")}})}
      }
    }
  }

  @Composable
  private fun ShiYuBottomBar(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val glassUi=prefs.getBoolean("experimental_glass_ui",false)
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
      Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha=if(glassUi).56f else .64f), border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=if(glassUi).48f else .34f)), shadowElevation = if(glassUi)0.dp else 5.dp) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
          val labels = if(englishUi())listOf("Chats","Characters","Moments","Me")else listOf("对话", "角色", "朋友圈", "我的")
          val icons = listOf(Icons.Outlined.Forum, Icons.Outlined.Style, Icons.Outlined.Collections, Icons.Outlined.Tune)
          labels.forEachIndexed { index, label ->
            Column(
              Modifier.weight(1f).fillMaxHeight().offset(y=2.dp).clickable(enabled=enabled) { onSelect(index) },
              horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
              Icon(icons[index], label, Modifier.size(20.dp), tint = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(Modifier.height(1.dp))
              Text(label, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, softWrap = false, style = LocalTextStyle.current.copy(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)), fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal, color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
      item{Button(onClick={prefs.edit().putString("adult_consent_revision",adultConsentRevision).putBoolean("terms_accepted",true).apply();composePage=nextEntryPage()},enabled=adult&&accepted,modifier=Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.VerifiedUser,null);Spacer(Modifier.width(8.dp));Text("确认并继续",maxLines=1,softWrap=false)}}
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
      Button(onClick={if(state.currentPage<2)scope.launch{state.animateScrollToPage(state.currentPage+1,animationSpec=tween(220,easing=FastOutSlowInEasing))}else{prefs.edit().putBoolean("onboarding_done",true).apply();composePage=nextEntryPage()}},Modifier.fillMaxWidth().height(50.dp)){Text(if(state.currentPage<2)"下一步" else "开始使用",maxLines=1,softWrap=false)}
    }
  }

  @Composable
  private fun StartupLoadingPage(){
    var ready by remember{mutableStateOf(false)}
    LaunchedEffect(BuildConfig.VERSION_NAME){delay(900);ready=true}
    Column(Modifier.fillMaxSize().padding(horizontal=28.dp,vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
      CircularProgressIndicator(Modifier.size(42.dp),strokeWidth=3.dp)
      Text("正在加载…",Modifier.padding(top=22.dp),fontSize=23.sp,fontWeight=FontWeight.SemiBold,maxLines=1,softWrap=false)
      Text("更新后的首次进入需要初始化本地数据和功能，可能出现短暂卡顿，请稍候。",Modifier.padding(top=12.dp),fontSize=14.sp,lineHeight=21.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Button(onClick={finishStartupLoading()},enabled=ready,modifier=Modifier.fillMaxWidth().padding(top=28.dp).height(48.dp)){Text(if(ready)"进入时遇" else "正在准备",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}
      Text("此提示仅在当前版本首次打开时显示一次。",Modifier.padding(top=12.dp),fontSize=11.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
      item{Button(onClick={if(openQqGroup())finishCommunityInvite()},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.GroupAdd,null);Spacer(Modifier.width(8.dp));Text("加入 QQ 群",maxLines=1,softWrap=false)}}
      item{OutlinedButton(onClick={finishCommunityInvite(Page.FEEDBACK)},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Feedback,null);Spacer(Modifier.width(8.dp));Text("反馈与建议",maxLines=1,softWrap=false)}}
      item{TextButton(onClick={finishCommunityInvite()}){Text("暂时跳过")}}
      item{Text("此页面只在当前版本首次打开时显示一次。加入群或反馈均为自愿操作。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(horizontal=12.dp))}
    }
  }

  @Composable
  private fun FeedbackPage(){
    val categories=listOf("suggestion" to "功能建议","bug" to "问题反馈","experience" to "体验意见","other" to "其他")
    var category by rememberSaveable{mutableStateOf("suggestion")};var content by rememberSaveable{mutableStateOf("")};var contact by rememberSaveable{mutableStateOf("")};var includeLogs by rememberSaveable{mutableStateOf(true)};var submitting by remember{mutableStateOf(false)};var result by remember{mutableStateOf("")}
    DetailScaffold("反馈与建议"){
      item{InfoBanner(Icons.Outlined.PrivacyTip,"由你决定是否附带日志",if(includeLogs)"将附带 App 与 Android 版本、设备型号、最近错误和闪退保护记录；日志会先脱敏，不会读取或附加本地聊天、角色卡、API Key、账号或设备唯一 ID。" else "当前仅发送反馈类别、正文、App 版本和可选联系方式；不会附带错误日志。")}
      item{SectionTitle("反馈类型")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){categories.forEach{entry->FilterChip(selected=category==entry.first,onClick={category=entry.first},label={Text(entry.second,maxLines=1,softWrap=false)})}}}
      item{OutlinedTextField(content,{content=it.take(2000)},Modifier.fillMaxWidth().heightIn(min=170.dp,max=280.dp),label={Text("反馈内容")},placeholder={Text("请描述你的建议、操作步骤或遇到的问题，至少 10 个字。")},supportingText={Text("${content.length} / 2000")},minLines=7,maxLines=12,shape=RoundedCornerShape(13.dp))}
      item{OutlinedTextField(contact,{contact=it.take(100)},Modifier.fillMaxWidth(),label={Text("联系方式（可选）")},placeholder={Text("QQ 或邮箱，仅用于回复本次反馈")},singleLine=true,shape=RoundedCornerShape(13.dp))}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().clickable{includeLogs=!includeLogs}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(includeLogs,{includeLogs=it});Column(Modifier.padding(start=8.dp)){Text("上传报错日志",fontWeight=FontWeight.Medium);Text("App、Android 与设备型号，最近 API/后台错误及闪退记录",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
      if(result.isNotBlank())item{InfoBanner(Icons.Outlined.CheckCircle,"反馈已提交","参考编号：$result。感谢你帮助时遇变得更好。")}
      item{Button(onClick={if(content.trim().length<10)toast("反馈内容至少需要 10 个字")else{submitting=true;result="";submitFeedback(category,content.trim(),contact.trim(),includeLogs){ok,message->submitting=false;if(ok){result=message;content="";contact="";includeLogs=true}else toast("提交失败：$message")}}},enabled=!submitting,modifier=Modifier.fillMaxWidth().height(50.dp)){if(submitting)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(8.dp));Text(if(submitting)"正在安全提交" else "提交反馈",maxLines=1,softWrap=false)}}
      item{SectionTitle("其他方式")}
      item{SettingButton(Icons.Outlined.Groups,"加入时遇 QQ 群","1061590432 · 点击用浏览器打开"){openQqGroup()}}
      item{Text("请勿在反馈中填写 API Key、密码、身份证件、支付信息、完整聊天记录或其他敏感内容。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(10.dp))}
    }
  }

  @Composable
  private fun DataRescuePage(){
    var running by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var password by rememberSaveable{mutableStateOf("")}
    var confirmRestore by rememberSaveable{mutableStateOf(false)}
    val exportRaw=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->
      if(uri!=null){running=true;status="正在整理可抢救的数据";pool.execute{
        runCatching{contentResolver.openOutputStream(uri,"w")?.use{output->DataRescueManager.writeRescuePackage(this@ComposeActivity,output,dataLoadFailureDetails())}?:error("无法写入所选文件")}
          .onSuccess{main.post{running=false;status="抢救包已保存，请妥善保管"}}
          .onFailure{error->main.post{running=false;status="";toast("抢救包导出失败：${error.message?:"未知错误"}")}}
      }}
    }
    val restoreBackup=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
      if(uri!=null){running=true;status="正在验证并恢复备份";pool.execute{
        runCatching{contentResolver.openInputStream(uri)?.use{BackupManager.restore(this@ComposeActivity,db,prefs,secure,it,password)}?:error("系统无法读取该备份");if(!load())error("备份已写入，但重新读取失败")}
          .onSuccess{main.post{running=false;status="恢复完成，正在重新载入";selectedChat=null;recreate()}}
          .onFailure{error->main.post{running=false;status="";toast("恢复失败：${error.message?:"备份不可用"}")}}
      }}
    }
    fun retryLoad(){
      if(running)return
      running=true;status="正在重新检查本地数据";pool.execute{
        val recovered=load()
        main.post{running=false;if(recovered){status="数据已恢复，正在重新载入";selectedChat=null;recreate()}else status="仍然无法读取，请先导出抢救包"}
      }
    }
    DetailScaffold("数据抢救",Page.DATA_RESCUE,onBack={finish()}){
      item{InfoBanner(Icons.Outlined.HealthAndSafety,"本地数据暂时无法载入","时遇已停止继续加载，现有数据库不会被自动清空。请先导出抢救包，再尝试重试或恢复备份。")}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.errorContainer){Column(Modifier.fillMaxWidth().padding(14.dp)){Text("检测到的问题",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onErrorContainer);Text(dataLoadFailureDetails().lineSequence().firstOrNull().orEmpty().ifBlank{"本地数据库或状态文件无法解析"}.take(600),Modifier.padding(top=6.dp),fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onErrorContainer)}}}
      if(status.isNotBlank())item{InfoBanner(if(status.contains("完成")||status.contains("保存"))Icons.Outlined.CheckCircle else Icons.Outlined.Pending,"抢救状态",status)}
      item{SectionTitle("先保存原始数据")}
      item{Button(onClick={val stamp=java.text.SimpleDateFormat("yyyyMMdd-HHmm",Locale.getDefault()).format(Date());exportRaw.launch("shiyu-rescue-$stamp.zip")},enabled=!running,modifier=Modifier.fillMaxWidth().height(50.dp)){if(running)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.SaveAlt,null);Spacer(Modifier.width(8.dp));Text(if(running)"正在处理" else "导出数据抢救包",maxLines=1,softWrap=false)}}
      item{OutlinedButton(onClick={running=true;status="正在筛选并修复可识别数据";pool.execute{val recovered=repairCurrentStateForRescue();main.post{running=false;if(recovered){status="可识别数据已修复，正在重新载入";selectedChat=null;recreate()}else status="没有找到可直接修复的数据"}}},enabled=!running,modifier=Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Build,null);Spacer(Modifier.width(8.dp));Text("自动修复可识别数据",maxLines=1,softWrap=false)}}
      item{OutlinedButton(onClick={retryLoad()},enabled=!running,modifier=Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text("重新尝试载入",maxLines=1,softWrap=false)}}
      if(hasLegacyStateForRescue())item{OutlinedButton(onClick={askDanger("尝试旧版数据副本","将把设备中保留的旧版状态副本写回当前数据库。建议先导出抢救包。","开始抢救"){running=true;status="正在读取旧版数据副本";pool.execute{val recovered=restoreLegacyStateForRescue();main.post{running=false;if(recovered){status="旧版数据已恢复，正在重新载入";recreate()}else status="旧版副本也无法读取"}}}},enabled=!running,modifier=Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.History,null);Spacer(Modifier.width(8.dp));Text("尝试旧版数据副本",maxLines=1,softWrap=false)}}
      item{SectionTitle("从已有备份恢复")}
      item{OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("备份密码")},supportingText={Text("未加密备份可留空")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许备份覆盖损坏的数据",fontWeight=FontWeight.Medium);Text("恢复前建议先导出抢救包",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(confirmRestore,{confirmRestore=it})}}}
      item{OutlinedButton(onClick={restoreBackup.launch(arrayOf("application/json","application/octet-stream","*/*"))},enabled=confirmRestore&&!running,modifier=Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Restore,null);Spacer(Modifier.width(8.dp));Text("选择时遇备份并恢复",maxLines=1,softWrap=false)}}
      item{Text("抢救包包含原始数据库、应用设置和可识别的用户资源，不会上传到服务器。文件中可能含有聊天等隐私内容，请只保存在可信位置。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(10.dp))}
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
  private fun ConversationPage(listState: LazyListState, onGroupClick: () -> Unit) {
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
      LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = mainBottomBarClearance), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                Text(item.messages.lastOrNull()?.body ?: "新的故事正等待开始",fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(Modifier.width(72.dp),horizontalAlignment=Alignment.End){
                Row(Modifier.height(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.End){if(item.pinned){Icon(Icons.Filled.PushPin,"已置顶",Modifier.size(14.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(4.dp))};Text(time(item.updated), fontSize = 10.sp, maxLines=1,softWrap=false,color = MaterialTheme.colorScheme.onSurfaceVariant)}
                Spacer(Modifier.height(4.dp))
                if(item.externalSource=="weixin"||item.externalSource=="qqbot")Surface(Modifier.width(34.dp).height(18.dp),shape=RoundedCornerShape(6.dp),color=MaterialTheme.colorScheme.secondaryContainer){Box(contentAlignment=Alignment.Center){Text(if(item.externalSource=="qqbot")"QQ" else "微信",fontSize=10.sp,lineHeight=12.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.onSecondaryContainer)}}else Spacer(Modifier.height(18.dp))
              }
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
  private fun RolePage(listState: LazyListState) {
    val roleItems = remember(uiRevision) { cards.toList() }
    var menuCard by remember { mutableStateOf<Card?>(null) }
    Column(Modifier.fillMaxSize()) {
      BrandHeader("角色卡", "${roleItems.size} 张角色卡，收藏不同的世界") {
        CircleAction(Icons.Outlined.PersonAdd, "创建角色") { composePage = Page.CREATE_ROLE }
        Spacer(Modifier.width(8.dp))
        CircleAction(Icons.Outlined.Inventory2, "角色卡中心") { composePage = Page.CARD_CENTER }
      }
      SectionTitle("我的角色")
      LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = mainBottomBarClearance), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (roleItems.isEmpty()) item { EmptyState(Icons.Outlined.Style, "还没有角色卡", "导入 PNG 或 JSON 角色卡，开启第一场相遇") }
        items(roleItems, key = { it.id }) { card ->
          Surface(
            modifier = Modifier.combinedClickable(onClick = { editingCardId=card.id;roleProfileChatId=null;roleProfileBackPage=Page.MAIN;composePage=Page.ROLE_PROFILE }, onLongClick = { menuCard = card }),
            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface
          ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              RoleAvatar(card, 52)
              Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(card.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
              Text(card.desc.ifBlank { "等待与你相遇" }, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
              Column(horizontalAlignment=Alignment.End){Icon(Icons.Outlined.ChevronRight, "查看角色信息", tint = MaterialTheme.colorScheme.secondary)}
            }
          }
        }
      }
    }
    menuCard?.let { target ->
      AlertDialog(onDismissRequest={menuCard=null},title={Text(target.name)},text={Column{
        TextButton(onClick={menuCard=null;newChat(target)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Forum,null);Spacer(Modifier.width(8.dp));Text("开始新对话",Modifier.weight(1f))}
              TextButton(onClick={menuCard=null;editingCardId=target.id;roleProfileChatId=null;roleProfileBackPage=Page.MAIN;composePage=Page.ROLE_PROFILE},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Badge,null);Spacer(Modifier.width(8.dp));Text("查看角色信息",Modifier.weight(1f))}
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
      item{InfoBanner(Icons.Outlined.AutoStories,"留空将使用公共世界书/正则","角色没有配置自己的世界书或正则时，会自动使用设置里的「公共世界书与正则」兜底内容。")}
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
    var cardUrl by rememberSaveable{mutableStateOf("")}
    var importingUrl by remember{mutableStateOf(false)}
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pool.execute {
        try {
          val bytes = bytes(contentResolver.openInputStream(uri)); val name = fileName(uri); val card = parseCardClean(bytes, name)
          normalizedPngBytes(bytes)?.let { png -> val file = File(cacheDir, "import_preview_${card.id}.png"); FileOutputStream(file).use { it.write(png) }; card.avatar = file.absolutePath }
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
      item{SectionTitle("链接导入")}
      item{
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
          Column(Modifier.fillMaxWidth().padding(14.dp)){
            OutlinedTextField(cardUrl,{cardUrl=it.take(2000)},Modifier.fillMaxWidth(),label={Text("角色卡直链")},placeholder={Text("https://example.com/character.png")},singleLine=true,shape=RoundedCornerShape(12.dp))
            Button(onClick={
              val value=cardUrl.trim()
              if(value.isBlank())toast("请粘贴角色卡链接") else {
                importingUrl=true
                pool.execute{
                  runCatching{downloadCharacterCard(value)}
                    .onSuccess{card->main.post{importingUrl=false;pendingImportCard=card;composePage=Page.IMPORT_PREVIEW}}
                    .onFailure{error->main.post{importingUrl=false;toast("链接导入失败：${friendlyError(error)}")}}
                }
              }
            },enabled=!importingUrl,modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(46.dp)){
              if(importingUrl)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Link,null)
              Spacer(Modifier.width(7.dp));Text(if(importingUrl)"正在下载解析" else "从链接导入")
            }
          }
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
    fun launch(format:CharacterCardExporter.Format){pendingFormat=format;val suffix=when(format){CharacterCardExporter.Format.SHIY->"shiy";CharacterCardExporter.Format.JSON_V2->"ccv2.json";CharacterCardExporter.Format.JSON_V3->"ccv3.json";CharacterCardExporter.Format.PNG_V2->"ccv2.png";CharacterCardExporter.Format.PNG_V3->"ccv3.png"};createFile.launch("${card.name.replace(Regex("[\\\\/:*?\"<>|]"),"_")}.$suffix")}
    DetailScaffold("导出角色卡",Page.CARD_CENTER){
      item{Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,62);Column(Modifier.weight(1f).padding(start=14.dp)){Text(card.name,fontSize=20.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("选择接收端支持的角色卡格式",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
      item{InfoBanner(Icons.Outlined.Inventory2,"完整保留角色资料","导出会包含头像、世界书、正则、预设、前端、备用问候、作者信息与时遇拍一拍设定。")}
      item{SettingButton(Icons.Outlined.Inventory2,"时遇 .shiy","保留时遇角色设定、世界书、预设和扩展字段"){launch(CharacterCardExporter.Format.SHIY)}}
      item{SectionTitle("JSON 格式")}
      item{SettingButton(Icons.Outlined.DataObject,"Character Card V3 JSON","字段更完整，适合新版 SillyTavern、Tavo 与兼容工具"){launch(CharacterCardExporter.Format.JSON_V3)}}
      item{SettingButton(Icons.Outlined.Code,"Character Card V2 JSON","兼容较早版本的酒馆与角色卡工具"){launch(CharacterCardExporter.Format.JSON_V2)}}
      item{SectionTitle("PNG 角色卡")}
      item{SettingButton(Icons.Outlined.Image,"Character Card V3 PNG","头像本身即角色卡，内嵌 V3 与兼容 V2 数据"){launch(CharacterCardExporter.Format.PNG_V3)}}
      item{SettingButton(Icons.Outlined.Photo,"Character Card V2 PNG","适合只识别传统 chara 元数据的工具"){launch(CharacterCardExporter.Format.PNG_V2)}}
      if(exporting)item{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Spacer(Modifier.width(9.dp));Text("正在生成角色卡…",fontSize=13.sp)}}
    }
  }

  private fun formatLabel(format:CharacterCardExporter.Format)=when(format){CharacterCardExporter.Format.SHIY->"时遇 .shiy";CharacterCardExporter.Format.JSON_V2->"CCv2 JSON";CharacterCardExporter.Format.JSON_V3->"CCv3 JSON";CharacterCardExporter.Format.PNG_V2->"CCv2 PNG";CharacterCardExporter.Format.PNG_V3->"CCv3 PNG"}

  @Composable
  private fun ImportPreviewPage(){
    val card=pendingImportCard?:return
    val detected=buildList{if(card.worldBook.isNotBlank())add("世界书");if(card.regexRules.isNotBlank())add("正则");if(card.preset.isNotBlank())add("预设");if(card.frontend.isNotBlank()||looksLikeTavernFrontend(card.greeting))add("酒馆前端")}.joinToString("、").ifBlank{"未包含扩展内容"}
    val previewEntries=remember(card.id){worldBookEntries(card.worldBook)}
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
      else if(previewEntries!=null){if(previewEntries.isEmpty())item{InfoBanner(Icons.Outlined.MenuBook,"世界书为空","已识别到 JSON 结构，但其中没有条目。")};items(previewEntries,key={it.id}){entry->WorldBookEntryPreview(entry)}}
      else item{PreviewText("世界书 JSON / 文本",card.worldBook,8)}
      item{SectionTitle("正则信息")}
      if(card.regexRules.isBlank())item{InfoBanner(Icons.Outlined.CodeOff,"未识别到正则","该文件没有可识别的正则脚本字段。")}
      else item{PreviewText("正则脚本 JSON / 文本",card.regexRules,7)}
      if(card.preset.isNotBlank()){item{SectionTitle("预设信息")};item{PreviewText("角色预设",card.preset,5)}}
      item{Spacer(Modifier.height(20.dp))}
    }
  }

  @Composable
  private fun ProfilePage(listState: LazyListState) {
    val languageLabel=when(prefs.getString("app_language","system")){"zh-CN"->"简体中文";"en"->"English";else->ui("跟随系统","System default")}
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = mainBottomBarClearance), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      item { BrandHeader(ui("我的时遇","My ShiYu"), ui("连接、外观与数据都由你掌控","Connections, appearance and data, in your control")) { CircleAction(Icons.Outlined.Search,ui("搜索设置","Search settings")){composePage=Page.SETTINGS_SEARCH} } }
      item { ProfileCard { composePage = Page.USER_PROFILE } }
      item { RecommendCard() }
      item { MenuGroup(ui("常用功能","Quick access"), listOf(
        MenuEntry(Icons.Outlined.Nightlight,ui("哄睡故事","Bedtime stories"),ui("编辑故事并让角色循环读给你听","Edit stories with looped narration")){composePage=Page.STORIES},
        MenuEntry(Icons.Outlined.QueueMusic,ui("网易云音乐","NetEase Cloud Music"),ui("搜索歌曲、选择私聊会话并和当前角色一起听","Search songs, choose a private chat, and listen with its character")){musicBackPage=Page.MAIN;composePage=Page.MUSIC},
        MenuEntry(Icons.Outlined.SportsEsports,ui("游戏房","Game room"),ui("五子棋、井字棋和抽鬼牌","Gomoku, tic-tac-toe and cards")){composePage=Page.WORLD_GAMES},
        MenuEntry(Icons.Outlined.Backup,ui("备份与恢复","Backup & restore"),ui("立即导出或恢复全部本地数据","Export or restore local data")){composePage=Page.BACKUP},
        MenuEntry(Icons.Outlined.Feedback,ui("反馈与建议","Feedback"),ui("提交问题并可附带脱敏日志","Report issues with optional diagnostics")){composePage=Page.FEEDBACK}
      )) }
      item { MenuGroup(ui("入门","Getting started"), listOf(MenuEntry(Icons.Outlined.School, ui("新手指南","Beginner's guide"), ui("一步一步了解时遇的核心功能","Step-by-step tour of ShiYu's core features")) { composePage = Page.GUIDE })) }
      item { MenuGroup(ui("模型与偏好","Models & preferences"), listOf(
        MenuEntry(Icons.Outlined.AccountCircle, ui("用户资料","User profile"), ui("昵称、头像与个人信息","Name, avatar and personal information")) { composePage = Page.USER_PROFILE },
        MenuEntry(Icons.Outlined.Hub, ui("模型设置","Model settings"), ui("添加、切换模型供应商与生成参数","Providers, models and generation parameters")) { composePage = Page.API },
        MenuEntry(Icons.Outlined.Extension, ui("MCP 工具","MCP tools"), ui("管理内置工具与远程 MCP 服务","Manage built-in tools and remote MCP services")) { composePage = Page.MCP_TOOLS },
        MenuEntry(Icons.Outlined.PendingActions, ui("生成任务","Generation tasks"), ui("查看正在生成的回复与最近记录","Active generations and recent history")) { composePage = Page.GENERATION_TASKS },
        MenuEntry(Icons.Outlined.RecordVoiceOver, ui("语音模型","Voice models"), ui("本地与在线 TTS、音色和语速","Local and online TTS, voice and speed")) { ttsBackPage=Page.MAIN;composePage = Page.TTS },
        MenuEntry(Icons.Outlined.Palette, ui("外观与动画","Appearance & motion"), ui("主题模式、系统栏与页面过渡","Theme, system bars and transitions")) { composePage = Page.APPEARANCE },
        MenuEntry(Icons.Outlined.Language, ui("语言","Language"), languageLabel) { composePage = Page.LANGUAGE },
        MenuEntry(Icons.Outlined.Public, ui("现实感知增强","Reality awareness"), ui("让角色自然了解当前日期、时间与时区","Share the current date, time and time zone")) { composePage = Page.REALITY_AWARENESS },
        MenuEntry(Icons.Outlined.Pets, ui("桌宠模式","Desktop pet"), ui("悬浮角色、语音回复、Live2D 与屏幕互动","Floating character, voice, Live2D and screen actions")) { composePage = Page.DESKTOP_PET },
        MenuEntry(Icons.Outlined.Science, ui("实验性功能","Experimental"), ui("玻璃界面与透明栏","Glass UI and transparent bars")) { composePage = Page.EXPERIMENTAL },
        MenuEntry(Icons.Outlined.NotificationsActive, ui("应用通知","App notifications"), ui("主动消息与通知设置","Proactive messages and notification settings")) { composePage = Page.APP_BEHAVIOR }
      )) }
      item { MenuGroup(ui("外部连接","External connections"), listOf(
        MenuEntry(Icons.Outlined.Link, ui("将角色接入微信","Connect character to WeChat"), ui("扫码连接并同步私聊消息","Scan to connect and sync private messages")) { composePage = Page.WEIXIN_LINK },
        MenuEntry(Icons.Outlined.SmartToy, ui("官方 QQ Bot","Official QQ Bot"), ui("配置 AppID、密钥与角色绑定","Configure AppID, secret and role binding")) { composePage = Page.QQ_BOT }
      )) }
      item { MenuGroup(ui("内容管理","Content"), listOf(
       MenuEntry(Icons.Outlined.Collections, ui("朋友圈设置","Moments settings"), ui("角色查看、发布、自动动态、评论与点赞授权","Viewing, posting, comments and permissions")) { composePage = Page.MOMENT_PERMISSIONS },
        MenuEntry(Icons.Outlined.QueueMusic, ui("网易云音乐","NetEase Cloud Music"), ui("登录、歌单、搜索与一起听歌","Login, playlists, search and listen together")) { musicBackPage=Page.MAIN;composePage=Page.MUSIC },
        MenuEntry(Icons.Outlined.Style, ui("角色卡中心","Character cards"), ui("导入、导出和格式指引","Import, export and format guides")) { composePage = Page.CARD_CENTER },
        MenuEntry(Icons.Outlined.EmojiEmotions, ui("表情资源","Sticker resources"), ui("下载、导入、收藏并授权角色使用","Download, import, favorite and allow character use")) { stickerResourcesBackPage=Page.MAIN;composePage = Page.STICKER_RESOURCES },
        MenuEntry(Icons.Outlined.Storage, ui("本地数据","Local data"), ui("数据库、聊天记录和安全存储","Database, chats and secure storage")) { composePage = Page.DATA },
        MenuEntry(Icons.Outlined.Backup, ui("备份与恢复","Backup & restore"), ui("完整备份角色、聊天、记忆、图片与设置","Back up characters, chats, memories, images and settings")) { composePage = Page.BACKUP }
      )) }
      item { MenuGroup(ui("帮助与关于","Help & about"), listOf(MenuEntry(Icons.Outlined.Feedback, ui("反馈与建议","Feedback"), ui("提交建议、报告问题或加入交流群","Send suggestions or report a problem")) { composePage = Page.FEEDBACK },MenuEntry(Icons.Outlined.FavoriteBorder, ui("支持我们","Support us"), ui("赞助时遇或联系作者","Sponsor ShiYu or contact the author")) { composePage = Page.SUPPORT },MenuEntry(Icons.Outlined.Info, ui("关于时遇","About ShiYu"), ui("版本、交流群与隐私说明","Version, community and privacy")) { composePage = Page.ABOUT })) }
    }
  }

  @Composable
  private fun WorldHomePage(){
    val selectedIds=remember(uiRevision){worldRoleIds()}
    val selectedRoles=remember(uiRevision){cards.filter{it.id in selectedIds}}
    val session=remember(uiRevision){runCatching{JSONObject(prefs.getString("world_activity_session","{}").orEmpty())}.getOrElse{JSONObject()}}
    val petEnergy=remember(uiRevision){prefs.getInt("world_pet_energy",72).coerceIn(0,100)}
    val petJoy=remember(uiRevision){prefs.getInt("world_pet_joy",68).coerceIn(0,100)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      item{BrandHeader("小世界",if(selectedRoles.isEmpty())"先邀请一位角色来到这里" else selectedRoles.joinToString("、"){it.name}+" 正在这里"){CircleAction(Icons.Outlined.Home,"进入 3D 家园"){composePage=Page.WORLD_HOME_3D}}}
      item{WorldRoleSelector(selectedIds){saveWorldRoleIds(it)}}
      if(session.length()>0)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.padding(horizontal=16.dp)){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Timer,null,tint=MaterialTheme.colorScheme.onPrimaryContainer);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text("正在一起${session.optString("title","活动")}",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onPrimaryContainer);Text("${session.optInt("duration",30)} 分钟 · ${session.optJSONArray("roleIds")?.length()?:0} 位角色",fontSize=12.sp,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=.78f))};TextButton(onClick={composePage=Page.WORLD_ACTIVITY}){Text("查看")}}}}
      item{Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){WorldFeatureAction(Modifier.weight(1f),Icons.Outlined.SportsEsports,"游戏房","五子棋、井字棋与抽鬼牌"){composePage=Page.WORLD_GAMES};WorldFeatureAction(Modifier.weight(1f),Icons.Outlined.Diversity3,"共同活动","学习、家务、约会与旅行"){composePage=Page.WORLD_ACTIVITY}}}
      item{Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){WorldFeatureAction(Modifier.weight(1f),Icons.Outlined.HolidayVillage,"3D 家园","动态小家、昼夜灯光与角色陪伴"){composePage=Page.WORLD_HOME_3D};WorldFeatureAction(Modifier.weight(1f),Icons.Outlined.Checkroom,"换装间","为角色选择服装并听取真实反馈"){composePage=Page.WORLD_WARDROBE}}}
      item{SectionTitle("一起养崽")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.padding(horizontal=16.dp)){Column(Modifier.fillMaxWidth().padding(15.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.size(54.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Pets,"家园幼崽",Modifier.size(28.dp),tint=MaterialTheme.colorScheme.onSecondaryContainer)}};Column(Modifier.weight(1f).padding(start=12.dp)){Text(prefs.getString("world_pet_name","团团")?:"团团",fontWeight=FontWeight.SemiBold,fontSize=17.sp);Text("家园共同成员",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        Text("精力 $petEnergy",Modifier.padding(top=14.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LinearProgressIndicator({petEnergy/100f},Modifier.fillMaxWidth().padding(top=5.dp));Text("愉悦 $petJoy",Modifier.padding(top=10.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LinearProgressIndicator({petJoy/100f},Modifier.fillMaxWidth().padding(top=5.dp),color=MaterialTheme.colorScheme.tertiary)
        Row(Modifier.fillMaxWidth().padding(top=13.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={prefs.edit().putInt("world_pet_energy",(petEnergy+12).coerceAtMost(100)).apply();uiRevision++},modifier=Modifier.weight(1f)){Icon(Icons.Outlined.Restaurant,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("喂食")};OutlinedButton(onClick={prefs.edit().putInt("world_pet_joy",(petJoy+12).coerceAtMost(100)).putInt("world_pet_energy",(petEnergy-5).coerceAtLeast(0)).apply();uiRevision++},modifier=Modifier.weight(1f)){Icon(Icons.Outlined.Toys,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("陪玩")}}
      }}}
    }
  }

  @Composable
  private fun WorldRoleSelector(selected:Set<String>,onChange:(Set<String>)->Unit){
    Column(Modifier.fillMaxWidth()){
      Text("同行角色",Modifier.padding(horizontal=20.dp,vertical=5.dp),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
      if(cards.isEmpty())Text("还没有角色，先在角色页导入或创建角色卡",Modifier.padding(horizontal=20.dp,vertical=12.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      else LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(cards,key={it.id}){card->FilterChip(selected=card.id in selected,onClick={val next=selected.toMutableSet();if(!next.add(card.id))next.remove(card.id);onChange(next)},label={Text(card.name,maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={RoleAvatar(card,24)})}}
    }
  }

  @Composable
  private fun WorldFeatureAction(modifier:Modifier,icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){
    Surface(onClick=onClick,modifier=modifier.height(132.dp),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxSize().padding(14.dp)){Icon(icon,title,Modifier.size(25.dp),tint=MaterialTheme.colorScheme.primary);Text(title,Modifier.padding(top=11.dp),fontWeight=FontWeight.SemiBold,maxLines=1);Text(subtitle,Modifier.padding(top=4.dp),fontSize=11.sp,lineHeight=16.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  }

  @Composable
  private fun WorldGamesPage(){
    var mode by rememberSaveable{mutableStateOf("gomoku")}
    var pickedRoleId by rememberSaveable{mutableStateOf("")}
    val selectedIds=remember(uiRevision){worldRoleIds()}
    val latestWorldId=selectedIds.lastOrNull{worldId->cards.any{card->card.id==worldId}}
    val role=cards.firstOrNull{card->card.id==pickedRoleId}
      ?:cards.firstOrNull{card->card.id==latestWorldId}
      ?:cards.firstOrNull()
    DetailScaffold("游戏房"){
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("gomoku" to "五子棋","tic" to "井字棋","ghost" to "抽鬼牌").forEach{entry->FilterChip(selected=mode==entry.first,onClick={mode=entry.first},label={Text(entry.second,maxLines=1)},leadingIcon={Icon(when(entry.first){"gomoku"->Icons.Outlined.GridOn;"tic"->Icons.Outlined.Grid3x3;else->Icons.Outlined.Style},null,Modifier.size(17.dp))})}}}
      if(cards.isNotEmpty())item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){cards.forEach{candidate->FilterChip(selected=role!=null&&candidate.id==role.id,onClick={pickedRoleId=candidate.id;if(!worldRoleIds().contains(candidate.id))saveWorldRoleIds(worldRoleIds()+candidate.id)},label={Text(candidate.name,maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={RoleAvatar(candidate,24)})}}}
      if(role==null)item{EmptyState(Icons.Outlined.PersonAdd,"还没有同行角色","先创建或导入角色卡")}
      else item{key(role.id){when(mode){"gomoku"->GomokuGame(role);"tic"->TicTacToeGame(role);else->GhostCardGame(role)}}}
    }
  }

  @Composable
  private fun TicTacToeGame(role:Card){
    var board by rememberSaveable(role.id){mutableStateOf(".........")}
    var turn by rememberSaveable(role.id){mutableStateOf("user")}
    fun winner(value:String):Char?{val lines=listOf(0,1,2,3,4,5,6,7,8,0,3,6,1,4,7,2,5,8,0,4,8,2,4,6);for(i in lines.indices step 3){val mark=value[lines[i]];if(mark!='.'&&mark==value[lines[i+1]]&&mark==value[lines[i+2]])return mark};return if('.' !in value)'D' else null}
    fun chooseMove(value:String):Int{
      fun tactical(mark:Char):Int?{for(index in value.indices.filter{value[it]=='.'}){val chars=value.toCharArray();chars[index]=mark;if(winner(chars.concatToString())==mark)return index};return null}
      return tactical('O')?:tactical('X')?:listOf(4,0,2,6,8,1,3,5,7).first{value[it]=='.'}
    }
    val result=winner(board)
    LaunchedEffect(turn,board,role.id){if(turn=="role"&&winner(board)==null){delay(520);val index=chooseMove(board);val chars=board.toCharArray();chars[index]='O';board=chars.concatToString();turn="user"}}
    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){UserAvatar(38);Text("你 · X",Modifier.weight(1f).padding(start=8.dp),fontSize=13.sp);Text(if(result==null&&turn=="role")"${role.name} 思考中" else when(result){'X'->"你赢了";'O'->"${role.name} 赢了";'D'->"平局";else->"轮到你"},fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary);Text("O · ${role.name}",Modifier.weight(1f).padding(end=8.dp),fontSize=13.sp,textAlign=androidx.compose.ui.text.style.TextAlign.End);RoleAvatar(role,38)}
      Column(Modifier.fillMaxWidth().padding(top=18.dp).aspectRatio(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){repeat(3){row->Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp)){repeat(3){column->val index=row*3+column;Surface(onClick={if(board[index]=='.'&&turn=="user"&&result==null){val chars=board.toCharArray();chars[index]='X';board=chars.concatToString();if(winner(board)==null)turn="role"}},enabled=board[index]=='.'&&turn=="user"&&result==null,modifier=Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(9.dp),color=MaterialTheme.colorScheme.surfaceVariant){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(board[index].takeIf{it!='.'}?.toString().orEmpty(),fontSize=42.sp,fontWeight=FontWeight.Bold,color=if(board[index]=='X')MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)}}}}}}
      OutlinedButton(onClick={board=".........";turn="user"},Modifier.fillMaxWidth().padding(top=15.dp)){Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(6.dp));Text("重新开局")}
    }}
  }

  @Composable
  private fun GomokuGame(role:Card){
    val size = 15
    var boardRaw by rememberSaveable(role.id) { mutableStateOf(".".repeat(size * size)) }
    var turn by rememberSaveable(role.id) { mutableStateOf("user") }
    var delegateRemaining by rememberSaveable(role.id) { mutableIntStateOf(0) }
    var moveThinking by rememberSaveable(role.id) { mutableStateOf(false) }
    var chatThinking by rememberSaveable(role.id) { mutableStateOf(false) }
    var gameEpoch by rememberSaveable(role.id) { mutableIntStateOf(0) }
    var input by rememberSaveable(role.id) { mutableStateOf("") }
    val log = remember(role.id) { mutableStateListOf<String>() }

    fun winner(value: String): Char? {
      for (row in 0 until size) for (column in 0 until size) {
        val mark = value[row * size + column]
        if (mark == '.') continue
        for ((dr, dc) in listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)) {
          var count = 1
          var r = row + dr
          var c = column + dc
          while (r in 0 until size && c in 0 until size && value[r * size + c] == mark) {
            count++
            r += dr
            c += dc
          }
          if (count >= 5) return mark
        }
      }
      return if ('.' !in value) 'D' else null
    }

    fun delegatedMove(value: String): Pair<Int, Int>? {
      val center = size / 2
      for (radius in 0 until size) {
        for (row in center - radius..center + radius) {
          for (column in center - radius..center + radius) {
            if (row in 0 until size && column in 0 until size && value[row * size + column] == '.') {
              return row to column
            }
          }
        }
      }
      return null
    }

    fun addLog(text: String) {
      if (text.isNotBlank()) log.add(text)
    }

    fun parseRoleMove(raw: String, snapshot: String): Triple<Int, Int, Pair<Int, String>> {
      val normalized = raw.replace("```json", "", ignoreCase = true).replace("```", "").trim()
      val start = normalized.indexOf('{')
      val end = normalized.lastIndexOf('}')
      require(start >= 0 && end > start) { "角色没有返回有效落子 JSON" }
      val json = JSONObject(normalized.substring(start, end + 1))
      val row = json.optInt("row", -1)
      val column = json.optInt("column", -1)
      require(row in 0 until size && column in 0 until size && snapshot[row * size + column] == '.') { "角色选择了无效位置" }
      return Triple(row, column, json.optInt("delegate", 0).coerceIn(0, 3) to AiContentSanitizer.comment(json.optString("speech")))
    }

    LaunchedEffect(role.id) {
      if (log.isEmpty()) addLog("${role.name}：来下一局五子棋吧。")
    }
    LaunchedEffect(boardRaw, turn, delegateRemaining, role.id) {
      val currentWinner = winner(boardRaw)
      if (turn != "role" || currentWinner != null || moveThinking) return@LaunchedEffect
      if (delegateRemaining > 0) {
        delay(520)
        val move = delegatedMove(boardRaw) ?: return@LaunchedEffect
        val chars = boardRaw.toCharArray()
        chars[move.first * size + move.second] = 'O'
        boardRaw = chars.concatToString()
        delegateRemaining--
        turn = if (winner(boardRaw) == null) "user" else "done"
        return@LaunchedEffect
      }
      moveThinking = true
      val snapshot = boardRaw
      val requestEpoch = gameEpoch
      gamePool.execute {
        val result = runCatching {
          var parsed: Triple<Int, Int, Pair<Int, String>>? = null
          var lastError: Exception? = null
          for (attempt in 0..1) {
            try {
              val retryHint = if (attempt == 0) "" else "上一次返回无法解析或坐标无效。请重新检查棋盘，只返回一个有效 JSON，不要使用 Markdown 代码块或任何解释。"
              val prompt = "我们正在玩15x15五子棋。你执白O，我执黑X。棋盘共225个字符，.为空。请根据角色设定认真决定下一步，只输出严格JSON：{\"row\":0,\"column\":0,\"delegate\":0,\"speech\":\"不超过50字\"}。row和column范围0到14，delegate范围0到3。$retryHint 棋盘：$snapshot"
              parsed = parseRoleMove(complete(listOf(Msg("user", prompt)), role), snapshot)
              break
            } catch (error: Exception) {
              lastError = error
            }
          }
          parsed ?: throw (lastError ?: IllegalStateException("角色没有返回有效落子"))
        }
        result.onSuccess { move ->
          main.post {
            if (requestEpoch != gameEpoch || boardRaw != snapshot || turn != "role") {
              moveThinking = false
              return@post
            }
            val chars = boardRaw.toCharArray()
            val index = move.first * size + move.second
            if (index in chars.indices && chars[index] == '.') {
              chars[index] = 'O'
              boardRaw = chars.concatToString()
              delegateRemaining = move.third.first
              addLog("${role.name}：${move.third.second.ifBlank { "轮到你了。" }}")
              turn = if (winner(boardRaw) == null) "user" else "done"
            }
            moveThinking = false
          }
        }.onFailure { error ->
          main.post {
            if (requestEpoch != gameEpoch || boardRaw != snapshot || turn != "role") {
              moveThinking = false
              return@post
            }
            moveThinking = false
            addLog("${role.name} 暂时无法落子：${friendlyError(error)}")
            turn = "user"
          }
        }
      }
    }

    val board = boardRaw
    val result = winner(board)
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
      Column(Modifier.fillMaxWidth().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          RoleAvatar(role, 40)
          Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("${role.name} · 五子棋", fontWeight = FontWeight.SemiBold)
            Text(when (result) {
              'X' -> "你赢了"
              'O' -> "${role.name} 赢了"
              'D' -> "和棋"
              else -> if (moveThinking) "${role.name} 正在思考" else if (delegateRemaining > 0) "系统托管中 · 剩余 $delegateRemaining 步" else if (turn == "user") "轮到你落子" else "等待 ${role.name}"
            }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
          }
          Icon(Icons.Outlined.GridOn, null, tint = MaterialTheme.colorScheme.primary)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
          val boardSide = maxWidth.coerceAtMost(360.dp)
          LazyVerticalGrid(columns = GridCells.Fixed(size), modifier = Modifier.size(boardSide), userScrollEnabled = false) {
            items(size * size) { index ->
              val mark = board[index]
              Box(Modifier.padding(1.dp).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).background(if ((index / size + index % size) % 2 == 0) Color(0xFFF4D9A6) else Color(0xFFEBC98C)).clickable(enabled = mark == '.' && turn == "user" && result == null && !moveThinking) {
                val chars = boardRaw.toCharArray()
                chars[index] = 'X'
                boardRaw = chars.concatToString()
                turn = if (winner(boardRaw) == null) "role" else "done"
              }, contentAlignment = Alignment.Center) {
                Text(if (mark == '.') "" else if (mark == 'X') "●" else "○", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (mark == 'X') Color(0xFF263238) else Color.White)
              }
            }
          }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { gameEpoch++; boardRaw = ".".repeat(size * size); turn = "user"; delegateRemaining = 0; moveThinking = false; chatThinking = false; log.clear(); addLog("${role.name}：再来一局。") }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Refresh, null)
            Spacer(Modifier.width(5.dp))
            Text("重新开局")
          }
          if (delegateRemaining > 0) Text("系统代下中", Modifier.align(Alignment.CenterVertically), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        }
        if (log.isNotEmpty()) {
          HorizontalDivider(Modifier.padding(vertical = 10.dp))
          log.takeLast(5).forEach { line -> Text(line, Modifier.padding(vertical = 2.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(value = input, onValueChange = { input = it.take(120) }, modifier = Modifier.weight(1f), placeholder = { Text("在游戏中和角色说点什么") }, singleLine = true)
          IconButton(onClick = {
            val text = input.trim()
            if (text.isNotBlank() && !chatThinking) {
              input = ""
              addLog("我：$text")
              chatThinking = true
              gamePool.execute {
                val reply = runCatching { complete(listOf(Msg("user", "你正在和我下五子棋。请自然回应这句话，不要落子，只输出角色要说的话：$text")), role).trim() }
                main.post { reply.onSuccess { addLog("${role.name}：${AiContentSanitizer.comment(it)}") }.onFailure { addLog("${role.name}：${friendlyError(it)}") }; chatThinking = false }
              }
            }
          }) { Icon(Icons.AutoMirrored.Filled.Send, "发送") }
        }
      }
    }
  }

  private fun ghostStripPairs(values:List<Int>):List<Int>{val counts=values.groupingBy{it}.eachCount();return values.filter{it==0||(counts[it]?:0)%2==1}.distinct()}
  private fun ghostEncode(values:List<Int>)=values.joinToString(",")
  private fun ghostDecode(value:String)=value.split(',').mapNotNull{it.toIntOrNull()}
  private fun ghostOutcome(user:List<Int>,role:List<Int>,roleName:String):String?=when{user.isEmpty()&&role==listOf(0)->"你赢了，鬼牌留在了 $roleName 手里";role.isEmpty()&&user==listOf(0)->"$roleName 赢了，鬼牌留在了你手里";else->null}

  @Composable
  private fun GhostCardGame(role:Card){
    var userRaw by rememberSaveable(role.id){mutableStateOf("")}
    var roleRaw by rememberSaveable(role.id){mutableStateOf("")}
    var turn by rememberSaveable(role.id){mutableStateOf("user")}
    var status by rememberSaveable(role.id){mutableStateOf("从对方手里抽一张牌")}
    fun reset(){val deck=(listOf(0)+(1..7).flatMap{listOf(it,it)}).shuffled();userRaw=ghostEncode(ghostStripPairs(deck.filterIndexed{index,_->index%2==0}));roleRaw=ghostEncode(ghostStripPairs(deck.filterIndexed{index,_->index%2==1}));turn="user";status="从 ${role.name} 手里抽一张牌"}
    LaunchedEffect(role.id){if(userRaw.isBlank()&&roleRaw.isBlank())reset()}
    val user=ghostDecode(userRaw);val roleHand=ghostDecode(roleRaw);val outcome=ghostOutcome(user,roleHand,role.name)
    LaunchedEffect(turn,userRaw,roleRaw){if(turn=="role"&&outcome==null&&user.isNotEmpty()){delay(650);val index=((role.id.hashCode()+user.size*17+roleHand.size*31)and Int.MAX_VALUE)%user.size;val drawn=user[index];val nextUser=user.toMutableList().also{it.removeAt(index)};val nextRole=ghostStripPairs(roleHand+drawn);userRaw=ghostEncode(nextUser);roleRaw=ghostEncode(nextRole);val done=ghostOutcome(nextUser,nextRole,role.name);status=done?:"轮到你从 ${role.name} 手里抽牌";turn="user"}}
    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){RoleAvatar(role,40);Column(Modifier.padding(start=10.dp)){Text(role.name,fontWeight=FontWeight.SemiBold);Text("手牌 ${roleHand.size} 张",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
      LazyRow(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){itemsIndexed(roleHand,key={index,_->"role_$index"}){index,_->GhostPlayingCard("?",true,enabled=turn=="user"&&outcome==null){val currentRole=ghostDecode(roleRaw);if(index>=currentRole.size)return@GhostPlayingCard;val drawn=currentRole[index];val nextRole=currentRole.toMutableList().also{it.removeAt(index)};val nextUser=ghostStripPairs(ghostDecode(userRaw)+drawn);roleRaw=ghostEncode(nextRole);userRaw=ghostEncode(nextUser);val done=ghostOutcome(nextUser,nextRole,role.name);status=done?:"${role.name} 正在抽牌";if(done==null)turn="role"}}}
      HorizontalDivider(Modifier.padding(vertical=16.dp))
      Text(status,fontSize=13.sp,fontWeight=FontWeight.Medium,color=if(outcome==null)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
      Text("你的手牌",Modifier.padding(top=12.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      LazyRow(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){itemsIndexed(user,key={index,value->"user_${index}_$value"}){_,value->GhostPlayingCard(if(value==0)"鬼" else value.toString(),value==0,false){}}}
      OutlinedButton(onClick={reset()},Modifier.fillMaxWidth().padding(top=15.dp)){Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(6.dp));Text("重新洗牌")}
    }}
  }

  @Composable
  private fun GhostPlayingCard(label:String,back:Boolean,enabled:Boolean,onClick:()->Unit){Surface(onClick=onClick,enabled=enabled,modifier=Modifier.size(width=54.dp,height=76.dp),shape=RoundedCornerShape(8.dp),color=if(back)MaterialTheme.colorScheme.primary else if(label=="鬼")MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.55f))){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){if(back)Icon(Icons.Outlined.AutoAwesome,"牌背",tint=MaterialTheme.colorScheme.onPrimary)else Text(label,fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(label=="鬼")MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)}}}

  @Composable
  private fun WorldActivityPage(){
    val activities=listOf(
      Triple("listen","一起听",Icons.Outlined.Headphones),Triple("study","学习",Icons.Outlined.School),Triple("craft","做手工",Icons.Outlined.Handyman),Triple("chores","做家务",Icons.Outlined.CleaningServices),Triple("tv","看电视",Icons.Outlined.Tv),Triple("picnic","野餐",Icons.Outlined.Park),Triple("date","约会",Icons.Outlined.FavoriteBorder),Triple("shopping","逛街",Icons.Outlined.ShoppingBag),Triple("travel","旅行",Icons.Outlined.FlightTakeoff),Triple("garden","照料花草宠物",Icons.Outlined.Yard)
    )
    val selectedIds=remember(uiRevision){worldRoleIds()};val selectedRoles=remember(uiRevision){cards.filter{it.id in selectedIds}}
    var chosen by rememberSaveable{mutableStateOf("study")};var duration by rememberSaveable{mutableIntStateOf(30)};var sessionRaw by remember(uiRevision){mutableStateOf(prefs.getString("world_activity_session","{}").orEmpty())};var generating by remember{mutableStateOf(false)};var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    val session=remember(sessionRaw){runCatching{JSONObject(sessionRaw)}.getOrElse{JSONObject()}};val active=session.length()>0;val endAt=session.optLong("endAt",0L);val startedAt=session.optLong("startedAt",0L);val remaining=(endAt-now).coerceAtLeast(0L);val total=(endAt-startedAt).coerceAtLeast(1L);val progress=(1f-remaining.toFloat()/total).coerceIn(0f,1f)
    LaunchedEffect(sessionRaw){while(session.length()>0){now=System.currentTimeMillis();delay(1000)}}
    val history=remember(uiRevision){runCatching{JSONArray(prefs.getString("world_activity_history","[]").orEmpty())}.getOrElse{JSONArray()}}
    DetailScaffold("共同活动"){
      item{WorldRoleSelector(selectedIds){saveWorldRoleIds(it)}}
      if(active){
        item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){
          Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Timer,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=10.dp)){Text("一起${session.optString("title","活动")}",fontSize=19.sp,fontWeight=FontWeight.SemiBold);Text(if(remaining>0)"剩余 ${remaining/60000} 分 ${(remaining/1000)%60} 秒" else "本次活动可以完成了",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
          LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().padding(top=15.dp).height(7.dp).clip(CircleShape))
          val participants=session.optJSONArray("roleIds")?:JSONArray();Row(Modifier.fillMaxWidth().padding(top=13.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){for(i in 0 until participants.length()){find(participants.optString(i))?.let{card->Row(verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,30);Text(card.name,Modifier.padding(start=5.dp),fontSize=12.sp)}}}}
          if(session.optString("kind")=="listen")OutlinedButton(onClick={composePage=Page.MUSIC},Modifier.fillMaxWidth().padding(top=13.dp)){Icon(Icons.Outlined.QueueMusic,null);Spacer(Modifier.width(6.dp));Text("打开网易云音乐")}
          Button(onClick={if(activeProvider()==null)toast("请先配置可用的模型供应商")else if(sending)toast("聊天正在生成，请稍候")else{val roleIds=session.optJSONArray("roleIds")?:JSONArray();val roles=buildList{for(i in 0 until roleIds.length())find(roleIds.optString(i))?.let(::add)};if(roles.isEmpty()){toast("活动中的角色已不存在");return@Button};generating=true;val title=session.optString("title");val elapsed=((System.currentTimeMillis()-session.optLong("startedAt"))/60000).coerceAtLeast(1);pool.execute{val responses=JSONArray();roles.forEach{card->val item=JSONObject().put("cardId",card.id).put("name",card.name);runCatching{val raw=complete(listOf(Msg("user","我们刚刚一起完成了$title，持续约 $elapsed 分钟。请完全依据你的角色设定，用你自己的口吻说说此刻最真实的一句感受或反馈，20到80字。只输出角色说的话，不要角色名前缀、说明、JSON 或系统提示。")),card);AiContentSanitizer.moment(raw).also{require(it.isNotBlank()){ "模型没有返回反馈" }}}.onSuccess{item.put("text",it)}.onFailure{item.put("error",friendlyError(it))};responses.put(item)};val record=JSONObject().put("id",UUID.randomUUID().toString()).put("title",title).put("kind",session.optString("kind")).put("startedAt",session.optLong("startedAt")).put("completedAt",System.currentTimeMillis()).put("roleIds",roleIds).put("responses",responses);val old=runCatching{JSONArray(prefs.getString("world_activity_history","[]").orEmpty())}.getOrElse{JSONArray()};val next=JSONArray().put(record);for(i in 0 until minOf(old.length(),19))next.put(old.opt(i));prefs.edit().remove("world_activity_session").putString("world_activity_history",next.toString()).apply();main.post{roles.forEach{addRelationshipEvent(it,"共同活动：$title","共同完成 $title，角色反馈由 LLM 生成")};sessionRaw="{}";generating=false;uiRevision++;toast("活动已完成，角色反馈已保存")}}}},enabled=!generating,modifier=Modifier.fillMaxWidth().padding(top=9.dp)){if(generating)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.DoneAll,null);Spacer(Modifier.width(7.dp));Text(if(generating)"正在生成角色反馈" else if(remaining>0)"提前完成并生成反馈" else "完成并生成角色反馈")}
          TextButton(onClick={prefs.edit().remove("world_activity_session").apply();sessionRaw="{}";uiRevision++},Modifier.fillMaxWidth()){Text("取消本次活动")}
        }}}
      }else{
        item{InfoBanner(Icons.Outlined.Diversity3,"把陪伴变成共同经历","活动会持续计时并保留历史；完成时由每位参与角色的 LLM 生成独立反馈。")}
        item{SectionTitle("选择活动")}
        activities.chunked(2).forEach{row->item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){row.forEach{activity->val selected=chosen==activity.first;Surface(onClick={chosen=activity.first},modifier=Modifier.weight(1f).height(92.dp),shape=RoundedCornerShape(12.dp),color=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=.35f))){Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.SpaceBetween){Icon(activity.third,activity.second,Modifier.size(23.dp),tint=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant);Text(activity.second,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}}
        item{SectionTitle("活动时长")}
        item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(15,30,60,120).forEach{minutes->FilterChip(duration==minutes,{duration=minutes},{Text("$minutes 分钟")})}}}
        item{Button(onClick={if(selectedRoles.isEmpty())toast("至少选择一位同行角色")else{val activity=activities.first{it.first==chosen};val start=System.currentTimeMillis();val value=JSONObject().put("kind",activity.first).put("title",activity.second).put("duration",duration).put("startedAt",start).put("endAt",start+duration*60_000L).put("roleIds",JSONArray(selectedRoles.map{it.id}));sessionRaw=value.toString();prefs.edit().putString("world_activity_session",sessionRaw).apply();now=start;uiRevision++;toast("已开始一起${activity.second}")}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.PlayArrow,null);Spacer(Modifier.width(7.dp));Text("开始共同活动")}}
      }
      if(history.length()>0){item{SectionTitle("最近共同经历")};items(minOf(history.length(),6)){index->val record=history.optJSONObject(index)?:return@items;Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.History,null,Modifier.size(19.dp),tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(start=9.dp)){Text("一起${record.optString("title")}",fontWeight=FontWeight.SemiBold);Text(java.text.SimpleDateFormat("M月d日 HH:mm",Locale.CHINA).format(Date(record.optLong("completedAt"))),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}};val responses=record.optJSONArray("responses")?:JSONArray();for(i in 0 until responses.length()){val response=responses.optJSONObject(i)?:continue;Text("${response.optString("name")}：${if(response.has("error"))"生成失败（${response.optString("error")}）" else response.optString("text")}",Modifier.padding(top=8.dp),fontSize=12.sp,lineHeight=18.sp,color=if(response.has("error"))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
    }
  }

  @Composable
  private fun WorldWardrobePage(){
    val defaultId=worldRoleIds().firstOrNull()?:cards.firstOrNull()?.id.orEmpty();var cardId by rememberSaveable{mutableStateOf(defaultId)};val card=find(cardId);var generating by remember{mutableStateOf(false)};var feedback by rememberSaveable(cardId){mutableStateOf("")}
    val selectedOutfit=remember(uiRevision,cardId){worldOutfits().firstOrNull{it.id==worldOutfitId(cardId)}?:worldOutfits().first()}
    DetailScaffold("换装间"){
      if(cards.isEmpty())item{EmptyState(Icons.Outlined.Checkroom,"还没有可换装的角色","先在角色页导入或创建角色卡")}
      else item{LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(cards,key={it.id}){role->FilterChip(selected=role.id==cardId,onClick={cardId=role.id;feedback=""},label={Text(role.name,maxLines=1)},leadingIcon={RoleAvatar(role,24)})}}}
      if(card!=null){
        item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().height(126.dp).padding(15.dp),verticalAlignment=Alignment.CenterVertically){Box(contentAlignment=Alignment.BottomCenter){Box(Modifier.size(width=82.dp,height=94.dp).clip(RoundedCornerShape(13.dp)).background(storedColor(selectedOutfit.primary,MaterialTheme.colorScheme.primaryContainer)));RoleAvatar(card,68)};Column(Modifier.weight(1f).padding(start=14.dp)){Text(card.name,fontSize=19.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(selectedOutfit.name,Modifier.padding(top=5.dp),fontSize=14.sp,fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.primary);Text(selectedOutfit.description,Modifier.padding(top=4.dp),fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)}}}}
        item{SectionTitle("衣橱")}
        items(worldOutfits(),key={it.id}){outfit->val selected=outfit.id==selectedOutfit.id;Surface(onClick={saveWorldOutfit(card.id,outfit.id);feedback=""},shape=RoundedCornerShape(12.dp),color=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=.28f))){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){Row(horizontalArrangement=Arrangement.spacedBy(0.dp)){Box(Modifier.size(width=23.dp,height=38.dp).clip(RoundedCornerShape(topStart=9.dp,bottomStart=9.dp)).background(storedColor(outfit.primary,Color.Gray)));Box(Modifier.size(width=23.dp,height=38.dp).clip(RoundedCornerShape(topEnd=9.dp,bottomEnd=9.dp)).background(storedColor(outfit.secondary,Color.LightGray)))};Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(outfit.name,fontWeight=FontWeight.SemiBold);Text(outfit.description,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)};if(selected)Icon(Icons.Filled.CheckCircle,"已穿着",tint=MaterialTheme.colorScheme.primary)}}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){OutlinedButton(onClick={composePage=Page.WORLD_HOME_3D},modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.ViewInAr,null);Spacer(Modifier.width(6.dp));Text("3D 查看")};Button(onClick={if(activeProvider()==null)toast("请先配置可用的模型供应商")else if(sending)toast("聊天正在生成，请稍候")else{generating=true;feedback="";val outfit=selectedOutfit;pool.execute{runCatching{val raw=complete(listOf(Msg("user","你现在换上了“${outfit.name}”：${outfit.description}。请完全依据你的角色设定，用自己的口吻对这身穿搭和我说一句真实反馈，20到80字。只输出你说的话，不要角色名前缀、说明、JSON 或系统提示。")),card);AiContentSanitizer.moment(raw).also{require(it.isNotBlank()){ "模型没有返回反馈" }}}.onSuccess{value->main.post{feedback=value;generating=false;addRelationshipEvent(card,"换装：${outfit.name}",value)}}.onFailure{error->main.post{generating=false;toast("角色反馈生成失败：${friendlyError(error)}")}}}}},enabled=!generating,modifier=Modifier.weight(1f).height(48.dp)){if(generating)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.RecordVoiceOver,null);Spacer(Modifier.width(6.dp));Text(if(generating)"回应中" else "问问角色")}}}
        if(feedback.isNotBlank())item{Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.secondaryContainer){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.Top){RoleAvatar(card,38);Column(Modifier.padding(start=10.dp)){Text(card.name,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSecondaryContainer);Text(feedback,Modifier.padding(top=4.dp),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSecondaryContainer)}}}}
      }
    }
  }

  @android.annotation.SuppressLint("SetJavaScriptEnabled")
  @Composable
  private fun WorldHome3dPage(){
    val card=remember(uiRevision){cards.firstOrNull{it.id in worldRoleIds()}?:cards.firstOrNull()};val outfit=remember(uiRevision,card?.id){worldOutfits().firstOrNull{it.id==worldOutfitId(card?.id.orEmpty())}?:worldOutfits().first()};var webView by remember{mutableStateOf<WebView?>(null)}
    val encodedName=java.net.URLEncoder.encode(card?.name?:"角色","UTF-8");val url="file:///android_asset/world/home3d.html?name=$encodedName&primary=%23${outfit.primary.removePrefix("#")}&secondary=%23${outfit.secondary.removePrefix("#")}"
    DisposableEffect(Unit){onDispose{webView?.stopLoading();webView?.destroy();webView=null}}
    Box(Modifier.fillMaxSize().background(Color(0xFF9FC6CF))){
      AndroidView(factory={context->WebView(context).apply{setBackgroundColor(AndroidColor.TRANSPARENT);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.allowFileAccess=true;settings.allowFileAccessFromFileURLs=true;settings.allowUniversalAccessFromFileURLs=true;webViewClient=WebViewClient();webChromeClient=android.webkit.WebChromeClient();tag=url;loadUrl(url);webView=this}},update={view->if(view.tag!=url){view.tag=url;view.loadUrl(url)}},modifier=Modifier.fillMaxSize())
      Surface(color=Color(0xB8223137),contentColor=Color.White,modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth()){Row(Modifier.statusBarsPadding().height(58.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={composePage=Page.MAIN}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")};Column(Modifier.weight(1f)){Text("3D 家园",fontWeight=FontWeight.SemiBold);Text("${card?.name?:"等待角色"} · ${outfit.name}",fontSize=11.sp,color=Color.White.copy(alpha=.72f),maxLines=1,overflow=TextOverflow.Ellipsis)};IconButton(onClick={composePage=Page.WORLD_WARDROBE}){Icon(Icons.Outlined.Checkroom,"换装")}}}
      Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=18.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){FilledTonalButton(onClick={webView?.evaluateJavascript("window.toggleHomeLight&&window.toggleHomeLight()",null)},contentPadding=PaddingValues(horizontal=13.dp)){Icon(Icons.Outlined.LightMode,"切换灯光",Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("灯光")};FilledTonalButton(onClick={webView?.evaluateJavascript("window.resetCamera&&window.resetCamera()",null)},contentPadding=PaddingValues(horizontal=13.dp)){Icon(Icons.Outlined.CenterFocusStrong,"复位视角",Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("复位")};FilledTonalButton(onClick={composePage=Page.WORLD_WARDROBE},contentPadding=PaddingValues(horizontal=13.dp)){Icon(Icons.Outlined.Checkroom,"换装",Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("换装")}}
    }
  }

  @Composable
  private fun MomentsHomePage(feedState: LazyListState) {
    val posts=remember(uiRevision){moments()}
    var forwardingPost by remember{mutableStateOf<MomentPost?>(null)}
    Column(Modifier.fillMaxSize()) {
      BrandHeader("朋友圈","记录你和角色共同生活的片刻") {
        CircleAction(Icons.Outlined.AdminPanelSettings,"角色授权"){composePage=Page.MOMENT_PERMISSIONS}
        Spacer(Modifier.width(8.dp))
        CircleAction(Icons.Outlined.PostAdd,"发布朋友圈"){composePage=Page.MOMENTS}
      }
      if(momentRoleIds("moments_post_roles").isNotEmpty())OutlinedButton(onClick={momentRolePostBackPage=Page.MAIN;composePage=Page.MOMENT_ROLE_POST},modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp).height(44.dp)){Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(7.dp));Text("邀请角色发布",maxLines=1,softWrap=false)}
      LazyColumn(state=feedState,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,top=10.dp,end=16.dp,bottom=mainBottomBarClearance),verticalArrangement=Arrangement.spacedBy(10.dp)){
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
                        isChatSending(target.id) -> queueReply(target, role)
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
  private fun GuidePage(){
    val steps=remember{listOf(
      NewbieGuideStep(Icons.Outlined.Favorite,"认识时遇","时遇是只属于你的角色扮演陪伴应用：角色、对话和记忆都保存在本机。",listOf("导入角色卡或从零创建角色","每个角色拥有独立的对话","数据默认不离开你的设备"),null,null),
      NewbieGuideStep(Icons.Outlined.Style,"创建或导入角色","从底部“角色”页进入角色卡中心，可以新建角色，也可以导入 JSON 或 SillyTavern PNG 角色卡。",listOf("支持 JSON / PNG 角色卡","可编辑角色设定、开场白与世界书"),"前往角色卡中心",Page.CARD_CENTER),
      NewbieGuideStep(Icons.Outlined.Hub,"配置模型供应商","在“模型设置”中添加 OpenAI 兼容 API 地址、Key 与模型，也可以使用本地模型。",listOf("支持多个供应商与一键切换","本地模型无需联网"),"前往模型设置",Page.API),
      NewbieGuideStep(Icons.Outlined.Forum,"开始第一次聊天","回到“对话”页新建对话并选择角色，发送文字、图片、语音或表情。",listOf("支持多角色、多会话","消息可重新生成与编辑"),null,null),
      NewbieGuideStep(Icons.Outlined.AutoAwesome,"主动生成图片","在聊天页点击角色气泡下方的操作按钮，选择“生图”，填写画面描述即可生成并发送到当前对话。",listOf("生成前可填写正负向提示词","生成结果直接出现在聊天里"),"前往生图设置",Page.IMAGE_GENERATION),
      NewbieGuideStep(Icons.Outlined.Pets,"让陪伴走进生活","桌宠、语音通话、朋友圈、微信/QQ 联动等能力都在“我的时遇”里，按需开启。",listOf("桌面宠物可悬浮陪伴","朋友圈与自动互动由角色授权"),"前往桌宠设置",Page.DESKTOP_PET),
      NewbieGuideStep(Icons.Outlined.Storage,"数据与隐私","角色与聊天默认只存在本机；在“本地数据”可查看，“备份与恢复”可完整备份。",listOf("API Key 仅发往你配置的接口","建议定期备份"),"前往本地数据",Page.DATA)
    )}
    var index by rememberSaveable{mutableIntStateOf(0)}
    val step=steps[index.coerceIn(steps.indices)]
    DetailScaffold("新手指南",Page.MAIN){
      item{
        Column(Modifier.fillMaxWidth().padding(top=4.dp)){
          LinearProgressIndicator(progress={ (index+1).toFloat()/steps.size },modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
          Text("第 ${index+1} / ${steps.size} 步",Modifier.fillMaxWidth().padding(top=7.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
      }
      item{
        Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
          Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Icon(step.icon,null,Modifier.size(30.dp),tint=MaterialTheme.colorScheme.primary)}
            Text(step.title,Modifier.padding(top=14.dp),fontSize=19.sp,fontWeight=FontWeight.SemiBold)
            Text(step.content,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            step.points.forEach{point->Row(Modifier.fillMaxWidth().padding(top=9.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.CheckCircle,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary);Text(point,Modifier.padding(start=8.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurface)}}
          }
        }
      }
      step.page?.let{target->item{Button(onClick={composePage=target},modifier=Modifier.fillMaxWidth().height(46.dp)){Text(step.actionLabel.orEmpty(),maxLines=1,softWrap=false)}}}
      item{
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
          OutlinedButton(onClick={index=(index-1).coerceAtLeast(0)},modifier=Modifier.weight(1f).height(46.dp),enabled=index>0){Text("上一步",maxLines=1,softWrap=false)}
          Button(onClick={if(index==steps.lastIndex){prefs.edit().putBoolean("guide_completed",true).apply();toast("新手指南已完成");composePage=Page.MAIN}else index++},modifier=Modifier.weight(1f).height(46.dp)){Text(if(index==steps.lastIndex)"完成" else "下一步",maxLines=1,softWrap=false)}
        }
      }
      item{Spacer(Modifier.height(12.dp))}
    }
  }

  @Composable
  private fun SettingsSearchPage(){
    var query by rememberSaveable{mutableStateOf("")}
    val entries=listOf(
      MenuEntry(Icons.Outlined.AccountCircle,ui("用户资料","User profile"),ui("昵称、头像与身份设定","Name, avatar and identities")){composePage=Page.USER_PROFILE},
      MenuEntry(Icons.Outlined.School,ui("新手指南","Beginner's guide"),ui("一步一步了解时遇的核心功能","Step-by-step tour of ShiYu's core features")){composePage=Page.GUIDE},
      MenuEntry(Icons.Outlined.Hub,ui("模型设置","Model settings"),ui("供应商、API Key、模型与生成参数","Providers, API keys, models and generation")){composePage=Page.API},
      MenuEntry(Icons.Outlined.PendingActions,ui("生成任务","Generation tasks"),ui("正在生成的回复与最近记录","Active generations and recent history")){composePage=Page.GENERATION_TASKS},
      MenuEntry(Icons.Outlined.RecordVoiceOver,ui("语音模型","Voice models"),ui("TTS、语音识别、音色与语速","TTS, speech recognition, voice and speed")){ttsBackPage=Page.SETTINGS_SEARCH;composePage=Page.TTS},
      MenuEntry(Icons.Outlined.Palette,ui("外观与动画","Appearance & motion"),ui("主题、聊天背景、气泡、帧率与页面过渡","Theme, chat background, bubbles and transitions")){composePage=Page.APPEARANCE},
      MenuEntry(Icons.Outlined.Language,ui("语言","Language"),ui("跟随系统、简体中文或 English","System default, Simplified Chinese or English")){composePage=Page.LANGUAGE},
      MenuEntry(Icons.Outlined.Public,ui("现实感知增强","Reality awareness"),ui("当前日期、时间与时区","Current date, time and time zone")){composePage=Page.REALITY_AWARENESS},
      MenuEntry(Icons.Outlined.Pets,ui("桌宠模式","Desktop pet"),ui("悬浮角色、语音回复、Live2D 与屏幕互动","Floating character, voice, Live2D and screen actions")){composePage=Page.DESKTOP_PET},
      MenuEntry(Icons.Outlined.Science,ui("实验性功能","Experimental"),ui("闪退保护、玻璃界面与外部连接","Crash protection, glass UI and external links")){composePage=Page.EXPERIMENTAL},
      MenuEntry(Icons.Outlined.NotificationsActive,ui("应用通知","App notifications"),ui("主动消息、通知权限与后台行为","Proactive messages, permissions and background behavior")){composePage=Page.APP_BEHAVIOR},
      MenuEntry(Icons.Outlined.Collections,ui("朋友圈设置","Moments settings"),ui("角色查看、发布、评论、点赞与授权","Viewing, posting, comments, likes and permissions")){composePage=Page.MOMENT_PERMISSIONS},
      MenuEntry(Icons.Outlined.QueueMusic,ui("网易云音乐","NetEase Cloud Music"),ui("登录、歌单、搜索、播放与一起听歌","Login, playlists, search and listen together")){musicBackPage=Page.SETTINGS_SEARCH;composePage=Page.MUSIC},
      MenuEntry(Icons.Outlined.Style,ui("角色卡中心","Character cards"),ui("创建、链接导入、导出与格式指引","Create, link import, export and format guides")){composePage=Page.CARD_CENTER},
      MenuEntry(Icons.Outlined.EmojiEmotions,ui("表情资源","Sticker resources"),ui("资源包、本地导入、收藏与角色授权","Packs, local imports, favorites and permissions")){stickerResourcesBackPage=Page.SETTINGS_SEARCH;composePage=Page.STICKER_RESOURCES},
      MenuEntry(Icons.Outlined.Storage,ui("本地数据","Local data"),ui("数据库、聊天记录与安全存储","Database, chats and secure storage")){composePage=Page.DATA},
      MenuEntry(Icons.Outlined.Backup,ui("备份与恢复","Backup & restore"),ui("角色、聊天、记忆、图片与设置备份","Characters, chats, memories, images and settings")){composePage=Page.BACKUP},
      MenuEntry(Icons.Outlined.Link,ui("将角色接入微信","Connect character to WeChat"),ui("扫码连接角色 Bot 与聊天记录绑定","Connect a character bot and bind chat history")){composePage=Page.WEIXIN_LINK},
      MenuEntry(Icons.Outlined.SmartToy,ui("官方 QQ Bot","Official QQ Bot"),ui("配置多个官方机器人并绑定角色","Configure official bots and bind characters")){composePage=Page.QQ_BOT},
      MenuEntry(Icons.Outlined.Feedback,ui("反馈与建议","Feedback"),ui("提交建议、问题和体验意见","Suggestions, problems and experience feedback")){composePage=Page.FEEDBACK},
      MenuEntry(Icons.Outlined.FavoriteBorder,ui("支持我们","Support us"),ui("赞助时遇或联系作者","Sponsor ShiYu or contact the author")){composePage=Page.SUPPORT},
      MenuEntry(Icons.Outlined.Info,ui("关于时遇","About ShiYu"),ui("版本、更新、交流群、协议与隐私","Version, updates, community, terms and privacy")){composePage=Page.ABOUT}
    )
    val normalized=query.trim()
    val results=if(normalized.isBlank())entries else entries.filter{"${it.title} ${it.subtitle}".contains(normalized,true)}
    DetailScaffold("搜索设置"){
      item{OutlinedTextField(query,{query=it.take(80)},Modifier.fillMaxWidth(),placeholder={Text(ui("搜索设置内容","Search settings"))},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon=if(query.isNotBlank()){{IconButton(onClick={query=""}){Icon(Icons.Outlined.Close,ui("清空搜索","Clear search"))}}}else null,singleLine=true,shape=RoundedCornerShape(13.dp))}
      if(results.isEmpty())item{EmptyState(Icons.Outlined.SearchOff,ui("没有找到相关设置","No matching settings"),ui("换一个更简短的关键词试试","Try a shorter search term"))}
      else items(results,key={it.title}){entry->SettingButton(entry.icon,entry.title,entry.subtitle,entry.action)}
    }
  }

  @Composable
  private fun LanguagePage(){
    val selected=prefs.getString("app_language","system")?:"system"
    val options=listOf(Triple("system",ui("跟随系统","System default"),ui("使用设备当前语言；暂不支持的语言回退为简体中文","Uses the device language; unsupported languages fall back to Chinese")),Triple("zh-CN","简体中文","简体中文"),Triple("en","English",ui("英文界面（持续补充翻译）","English UI (translation coverage is expanding)")))
    DetailScaffold("语言"){
      item{InfoBanner(Icons.Outlined.Language,ui("应用语言","App language"),ui("切换后立即重建界面，不影响聊天、角色卡或模型配置。","The interface reloads immediately. Chats, character cards and model settings are unchanged."))}
      items(options,key={it.first}){option->Surface(onClick={if(selected!=option.first){prefs.edit().putString("app_language",option.first).putBoolean("reopen_language_settings",true).commit();recreate()}},shape=RoundedCornerShape(14.dp),color=if(selected==option.first)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(selected==option.first)Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=12.dp)){Text(option.second,fontWeight=FontWeight.SemiBold);Text(option.third,fontSize=12.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
    }
  }

  @Composable
  private fun UserProfilePage() {
    var nickname by remember { mutableStateOf(prefs.getString("user_name","我") ?: "我") }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};prefs.edit().putString("user_avatar",uri.toString()).apply();uiRevision++;toast("头像已更新")}}
    DetailScaffold("用户资料") {
      item { Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){UserAvatar(88);Spacer(Modifier.height(12.dp));OutlinedButton(onClick={picker.launch(arrayOf("image/*"))}){Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(8.dp));Text("选择头像")}} }
      item { StyledField(nickname,{nickname=it},"用户昵称") }
      item { InfoBanner(Icons.Outlined.Badge,"个人信息按身份保存","生日、称呼、所在地、职业、兴趣和简介已移动到身份设定，不会再对所有角色全局注入。") }
      item { SettingButton(Icons.Outlined.Badge,"身份设定",prefs.getString("active_identity_name","")?.takeIf{it.isNotBlank()}?.let{"当前：$it"}?:"创建并切换多个用户身份"){composePage=Page.IDENTITIES} }
      item { Button(onClick={if(nickname.isBlank())toast("昵称不能为空")else{prefs.edit().putString("user_name",nickname.trim()).apply();uiRevision++;toast("用户资料已保存");composePage=Page.MAIN}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存用户资料",maxLines=1,softWrap=false)}}
      item { RecommendCard() }
    }
  }

  @Composable
  private fun IdentitiesPage(){
    var values by remember(uiRevision){mutableStateOf(identities())}
    var editingId by remember{mutableStateOf<String?>(null)}
    var name by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var birthday by remember{mutableStateOf("")};var pronouns by remember{mutableStateOf("")}
    var location by remember{mutableStateOf("")};var occupation by remember{mutableStateOf("")}
    var interests by remember{mutableStateOf("")};var bio by remember{mutableStateOf("")}
    val activeId=prefs.getString("active_identity_id","")?:""
    fun clearForm(){editingId=null;name="";description="";birthday="";pronouns="";location="";occupation="";interests="";bio=""}
    fun edit(value:IdentityConfig){editingId=value.id;name=value.name;description=value.description;birthday=value.birthday;pronouns=value.pronouns;location=value.location;occupation=value.occupation;interests=value.interests;bio=value.bio}
    DetailScaffold("身份设定",Page.USER_PROFILE){
      item{InfoBanner(Icons.Outlined.Badge,"告诉角色你是谁","每个身份的资料只在选择该身份的角色或对话中注入；所有附加条目都可以留空。")}
      item{StyledField(name,{name=it},"身份名称")}
      item{OutlinedTextField(description,{description=it},Modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),label={Text("身份描述")},placeholder={Text("例如：我是住在海边的插画师，性格慢热，和角色是多年好友。")},minLines=5,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("个人信息（可选）")}
      item{StyledField(birthday,{birthday=it.take(40)},"生日（可选）")}
      item{StyledField(pronouns,{pronouns=it.take(40)},"性别 / 称呼（可选）")}
      item{StyledField(location,{location=it.take(80)},"所在地（可选）")}
      item{StyledField(occupation,{occupation=it.take(80)},"职业（可选）")}
      item{OutlinedTextField(interests,{interests=it.take(300)},Modifier.fillMaxWidth().heightIn(min=86.dp,max=150.dp),label={Text("兴趣爱好（可选）")},minLines=3,maxLines=6,shape=RoundedCornerShape(13.dp))}
      item{OutlinedTextField(bio,{bio=it.take(500)},Modifier.fillMaxWidth().heightIn(min=100.dp,max=190.dp),label={Text("个人简介 / 补充信息（可选）")},minLines=4,maxLines=8,shape=RoundedCornerShape(13.dp))}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){if(editingId!=null)OutlinedButton(onClick={clearForm()},Modifier.weight(1f).height(48.dp)){Text("取消编辑",maxLines=1,softWrap=false)};Button(onClick={if(name.isBlank()||description.isBlank())toast("请填写身份名称和描述")else{val value=IdentityConfig(editingId?:UUID.randomUUID().toString(),name.trim(),description.trim(),birthday.trim(),pronouns.trim(),location.trim(),occupation.trim(),interests.trim(),bio.trim());val updated=if(editingId==null)values+value else values.map{if(it.id==value.id)value else it};saveIdentities(updated);if(activeId.isBlank()||activeId==value.id)activateIdentity(value);values=updated;clearForm();uiRevision++;toast("身份已保存")}},Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text(if(editingId==null)"保存身份" else "保存修改",maxLines=1,softWrap=false)}}}
      item{SectionTitle("本地身份 ${values.size} 个")}
      if(values.isEmpty())item{EmptyState(Icons.Outlined.Badge,"还没有身份设定","填写名称和描述后保存")}
      items(values,key={it.id}){value->Surface(onClick={activateIdentity(value)},shape=RoundedCornerShape(14.dp),color=if(value.id==activeId)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(start=15.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(value.id==activeId)Icons.Filled.CheckCircle else Icons.Outlined.Badge,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(value.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(value.description,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);val optional=listOf(value.birthday,value.pronouns,value.location,value.occupation,value.interests,value.bio).count{it.isNotBlank()};if(optional>0)Text("已填写 $optional 项个人信息",fontSize=11.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.primary)};IconButton(onClick={edit(value)}){Icon(Icons.Outlined.Edit,"编辑")};IconButton(onClick={val updated=values.filterNot{it.id==value.id};saveIdentities(updated);if(value.id==activeId){prefs.edit().remove("active_identity_id").remove("active_identity_name").remove("active_identity_description").apply()};values=updated;uiRevision++;toast("身份已删除")}){Icon(Icons.Outlined.Delete,"删除",tint=MaterialTheme.colorScheme.error)}}}}
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
    val memoryCount=remember(uiRevision,card.id){db.memories(card.id).size+db.memoryNodes(card.id).size}
    DetailScaffold("角色设置",roleBackPage){
      if(draft.length()>0)item{InfoBanner(Icons.Outlined.EditNote,"已恢复未保存草稿","修改会自动保存在本机；点击保存后才会正式写入角色卡。")}
      item{Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){if(avatarSource==card.avatar)RoleAvatar(card,78)else MessageImage(avatarSource,Modifier.size(78.dp).clip(RoundedCornerShape(16.dp)));OutlinedButton(onClick={picker.launch(arrayOf("image/*"))},Modifier.padding(top=9.dp)){Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(7.dp));Text("更换角色头像")}}}
      item{StyledField(name,{name=it},"角色名称")}
      item{OutlinedTextField(desc,{desc=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=180.dp),label={Text("角色简介")},minLines=3,maxLines=7,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(persona,{persona=it},Modifier.fillMaxWidth().heightIn(min=140.dp,max=260.dp),label={Text("角色设定")},minLines=5,maxLines=11,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(greeting,{greeting=it},Modifier.fillMaxWidth().heightIn(min=100.dp,max=200.dp),label={Text("开场白")},minLines=4,maxLines=8,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(frontend,{frontend=it},Modifier.fillMaxWidth().heightIn(min=120.dp,max=260.dp),label={Text("酒馆前端 HTML（可选）")},minLines=5,maxLines=11,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("角色卡扩展")}
      item{InfoBanner(Icons.Outlined.AutoStories,"留空将使用公共世界书/正则","角色没有配置自己的世界书或正则时，会自动使用设置里的「公共世界书与正则」兜底内容。")}
      item{OutlinedTextField(worldBook,{worldBook=it},Modifier.fillMaxWidth().heightIn(min=110.dp,max=210.dp),label={Text("世界书")},minLines=4,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(regex,{regex=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=210.dp),label={Text("正则规则")},minLines=3,maxLines=9,shape=RoundedCornerShape(14.dp))}
      item{OutlinedTextField(preset,{preset=it},Modifier.fillMaxWidth().heightIn(min=90.dp,max=190.dp),label={Text("对话预设")},minLines=3,maxLines=8,shape=RoundedCornerShape(14.dp))}
      item{SectionTitle("拍一拍")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(patSelf,{patSelf=it.take(40)},"用户拍角色")};Box(Modifier.weight(1f)){StyledField(patUser,{patUser=it.take(40)},"角色拍用户")}}}
      item{SettingButton(Icons.Outlined.Badge,"角色信息","角色设定、LLM 状态、历史会话与一起听歌"){roleProfileChatId=null;roleProfileBackPage=Page.ROLE_SETTINGS;composePage=Page.ROLE_PROFILE}}
      item{SettingButton(Icons.Outlined.Psychology,"长期记忆","系统已记录 $memoryCount 条，点击查看与管理"){composePage=Page.MEMORY}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={name=card.name;desc=card.desc;persona=card.persona;greeting=card.greeting;frontend=card.frontend;worldBook=card.worldBook;regex=card.regexRules;preset=card.preset;avatarSource=card.avatar;patSelf=card.patSelfText;patUser=card.patUserText;prefs.edit().remove(draftKey).apply();toast("草稿已丢弃")},Modifier.weight(1f).height(50.dp)){Text("丢弃草稿",maxLines=1)};Button(onClick={if(name.isBlank())toast("角色名称不能为空")else{var savedAvatar=avatarSource;if(avatarSource.startsWith("content:")){runCatching{val file=File(filesDir,"avatar_${card.id}.png");contentResolver.openInputStream(android.net.Uri.parse(avatarSource))?.use{input->FileOutputStream(file).use{output->input.copyTo(output)}}?:error("无法读取头像");savedAvatar=file.absolutePath;bitmapCache.remove(file.absolutePath)}.onFailure{toast("头像保存失败：${it.message}");return@Button}};card.name=name.trim();card.desc=desc;card.persona=persona;card.greeting=greeting;card.frontend=frontend.trim();card.worldBook=worldBook;card.regexRules=regex;card.preset=preset;card.avatar=savedAvatar;card.patSelfText=patSelf.trim().ifBlank{"肩膀"};card.patUserText=patUser.trim().ifBlank{"肩膀"};save();DesktopPetService.sync(this@ComposeActivity);draftResolved=true;prefs.edit().remove(draftKey).apply();uiRevision++;toast("角色设置已保存");composePage=roleBackPage}},Modifier.weight(1f).height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存",maxLines=1)}}}
    }
  }

  @Composable
  private fun MemoryPage() {
    val card=find(editingCardId?:return)?:return
    var revision by remember{mutableIntStateOf(0)};var newMemory by remember{mutableStateOf("")}
    var editingNodeId by remember{mutableStateOf<String?>(null)};var nodeType by remember{mutableStateOf("anniversary")}
    var nodeTitle by remember{mutableStateOf("")};var nodeDetail by remember{mutableStateOf("")}
    var nodeYear by remember{mutableStateOf("")};var nodeMonth by remember{mutableStateOf("")};var nodeDay by remember{mutableStateOf("")}
    var nodeRepeat by remember{mutableStateOf(true)};var nodeRemindDays by remember{mutableStateOf("7")}
    val memories=remember(card.id,revision){db.memories(card.id)};val nodes=remember(card.id,revision){db.memoryNodes(card.id)}
    fun clearNode(){editingNodeId=null;nodeType="anniversary";nodeTitle="";nodeDetail="";nodeYear="";nodeMonth="";nodeDay="";nodeRepeat=true;nodeRemindDays="7"}
    fun editNode(value:AppDb.MemoryNode){editingNodeId=value.id;nodeType=value.type;nodeTitle=value.title;nodeDetail=value.detail;nodeYear=value.year.takeIf{it>0}?.toString().orEmpty();nodeMonth=value.month.toString();nodeDay=value.day.toString();nodeRepeat=value.repeatYearly;nodeRemindDays=value.remindDays.toString()}
    fun saveNode(){val month=nodeMonth.toIntOrNull();val day=nodeDay.toIntOrNull();val year=nodeYear.toIntOrNull()?:0;val remind=nodeRemindDays.toIntOrNull();if(nodeTitle.isBlank()){toast("请填写节点名称");return};if(month==null||month !in 1..12||day==null||day !in 1..31){toast("请填写有效日期");return};val valid=runCatching{Calendar.getInstance().apply{isLenient=false;clear();set(if(year>0)year else 2024,month-1,day);time}}.isSuccess;if(!valid){toast("请填写有效日期");return};if(!nodeRepeat&&year<=0){toast("不重复的经历需要填写年份");return};if(remind==null||remind !in 0..60){toast("提前关注天数应为 0 到 60");return};val id=editingNodeId;if(id==null)db.addMemoryNode(card.id,nodeType,nodeTitle.trim(),nodeDetail.trim(),year,month,day,nodeRepeat,remind)else db.updateMemoryNode(id,nodeType,nodeTitle.trim(),nodeDetail.trim(),year,month,day,nodeRepeat,remind);clearNode();revision++;uiRevision++;toast(if(id==null)"记忆节点已添加" else "记忆节点已保存")}
    DetailScaffold("${card.name}的长期记忆",Page.ROLE_SETTINGS){
      item{InfoBanner(Icons.Outlined.EventNote,"先添加有意义的记忆节点","可以从角色或你的生日、第一次相遇、一起过的节日开始。临近日期时，角色会在聊天中自然想起，不会机械报日历。")}
      item{SectionTitle("纪念日与共同经历")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("birthday" to "生日","anniversary" to "纪念日","festival" to "节日","shared_event" to "共同经历").forEach{(key,label)->FilterChip(selected=nodeType==key,onClick={nodeType=key},label={Text(label)})}}}
      item{StyledField(nodeTitle,{nodeTitle=it.take(80)},"节点名称")}
      item{OutlinedTextField(nodeDetail,{nodeDetail=it.take(500)},Modifier.fillMaxWidth().heightIn(min=88.dp),label={Text("这件事对你们的意义（可选）")},minLines=3,maxLines=6,shape=RoundedCornerShape(14.dp))}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){Box(Modifier.weight(1.2f)){StyledField(nodeYear,{nodeYear=it.filter(Char::isDigit).take(4)},"年份（可选）")};Box(Modifier.weight(1f)){StyledField(nodeMonth,{nodeMonth=it.filter(Char::isDigit).take(2)},"月")};Box(Modifier.weight(1f)){StyledField(nodeDay,{nodeDay=it.filter(Char::isDigit).take(2)},"日")}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("每年重复",fontWeight=FontWeight.Medium);Text("生日、节日和纪念日建议开启",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(nodeRepeat,{nodeRepeat=it})};Spacer(Modifier.height(7.dp));StyledField(nodeRemindDays,{nodeRemindDays=it.filter(Char::isDigit).take(2)},"提前关注天数（0-60）")}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){if(editingNodeId!=null)OutlinedButton(onClick={clearNode()},Modifier.weight(1f).height(46.dp)){Text("取消编辑")};Button(onClick={saveNode()},Modifier.weight(1f).height(46.dp)){Icon(if(editingNodeId==null)Icons.Outlined.Add else Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text(if(editingNodeId==null)"添加节点" else "保存修改",maxLines=1)}}}
      if(nodes.isEmpty())item{EmptyState(Icons.Outlined.Event,"还没有记忆节点","添加一个你们共同在意的日子")}
      items(nodes,key={it.id}){node->Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(start=14.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.EventAvailable,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(node.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${node.typeLabel()} · ${node.dateLabel()} · 提前 ${node.remindDays} 天",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(node.detail.isNotBlank())Text(node.detail,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={editNode(node)}){Icon(Icons.Outlined.Edit,"编辑节点")};IconButton(onClick={db.deleteMemoryNode(node.id);revision++;uiRevision++;toast("记忆节点已删除")}){Icon(Icons.Outlined.Delete,"删除节点",tint=MaterialTheme.colorScheme.error)}}}}
      item{SectionTitle("普通长期记忆")}
      item{InfoBanner(Icons.Outlined.Psychology,"对话会自动携带","适合记录称呼、喜好、约定和已经发生的事；不要把临时猜测写成事实。")}
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
      item { SettingButton(Icons.Outlined.Download, "本地模型下载", "中文小聊天模型与本地生图模型") { composePage = Page.LOCAL_MODELS } }
      item { SettingButton(Icons.Outlined.Tune, "生成参数", "温度、思考程度与最大上下文") { composePage = Page.GENERATION } }
      item { SettingButton(Icons.Outlined.Visibility, "视觉与听觉辅助模型", when{prefs.getBoolean("visual_aux_enabled",false)&&prefs.getBoolean("audio_aux_enabled",false)->"视觉与听觉均已开启";prefs.getBoolean("visual_aux_enabled",false)->"视觉辅助已开启";prefs.getBoolean("audio_aux_enabled",false)->"听觉辅助已开启";else->"未开启 · 可独立配置模型"}) { composePage = Page.AUXILIARY_MODELS } }
      item { SettingButton(Icons.Outlined.Extension, "角色工具与 MCP", if(prefs.getBoolean("character_tools_enabled",true))"已开启 · ${McpToolClient.servers(this@ComposeActivity).count{it.enabled}} 个远程服务" else "未开启 · 本地工具与远程 MCP") { composePage = Page.MCP_TOOLS } }
      item { SettingButton(Icons.Outlined.TravelExplore, "联网搜索", if(prefs.getBoolean("web_search_enabled",true))"已开启 · 内置必应搜索" else "未开启 · 角色无法搜索网页") { webSearchBackPage=Page.API;composePage = Page.WEB_SEARCH } }
      item { SettingButton(Icons.Outlined.ManageSearch, "嵌入模型实时库", if(prefs.getBoolean("embedding_enabled",false))"已开启 · ${db.realtimeCount("")} 条语义片段" else "未开启 · 实时索引与相关记忆检索") { embeddingBackPage=Page.API;composePage = Page.EMBEDDING_LIBRARY } }
      item { SettingButton(Icons.Outlined.Image, "生图模型", if(prefs.getBoolean("image_generation_enabled",false))prefs.getString("image_generation_model","已开启")?:"已开启" else "未开启 · OpenAI / NAI 兼容接口") { imageGenerationBackPage=Page.API;composePage = Page.IMAGE_GENERATION } }
      item { SettingButton(Icons.Outlined.Movie,"视频生成模型","独立供应商 · 通义万相 / 火山方舟 · 视频任务与结果"){videoGenerationBackPage=Page.API;composePage=Page.VIDEO_GENERATION} }
      item { SettingButton(Icons.Outlined.BugReport, "API 错误详情", if(lastApiError.isBlank())"暂无错误记录" else friendlyErrorText(lastApiError)) { apiErrorBackPage=Page.API;composePage = Page.API_ERROR } }
    }
  }

  @Composable
  private fun ApiErrorPage() {
    DetailScaffold("API 错误详情", apiErrorBackPage) {
      if(lastApiError.isBlank()) item { EmptyState(Icons.Outlined.CheckCircle,"暂无 API 错误","请求失败时会在这里保留最近一次友好提示与受限诊断信息") }
      else {
        item { InfoBanner(Icons.Outlined.ErrorOutline,"最近一次错误",friendlyErrorText(lastApiError)) }
        item { SectionTitle("API 原始返回") }
        item { OutlinedTextField(apiRawErrorText(lastApiError),{ },Modifier.fillMaxWidth().heightIn(min=112.dp,max=300.dp),readOnly=true,minLines=4,maxLines=13,textStyle=LocalTextStyle.current.copy(fontFamily=FontFamily.Monospace,fontSize=11.sp,lineHeight=16.sp),shape=RoundedCornerShape(12.dp)) }
        item { SectionTitle("诊断建议") }
        item { Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.Top){Icon(Icons.Outlined.BuildCircle,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.secondary);Text(diagnosticAdviceText(lastApiError),Modifier.padding(start=10.dp),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}} }
        item { OutlinedButton(onClick={clearApiError();uiRevision++;toast("错误记录已清除")},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(8.dp));Text("清除错误记录",maxLines=1,softWrap=false)} }
      }
    }
  }

  @Composable
  private fun ProviderPage() {
    var allProviders by remember(uiRevision) { mutableStateOf(providers()) }
    DetailScaffold("模型提供商", Page.API) {
      item { InfoBanner(Icons.Outlined.Verified, "已配置供应商", if(allProviders.isEmpty())"暂未配置模型供应商，可添加在线供应商或本地模型服务。" else "切换时会同时切换 API 地址、Key 和已选择模型。") }
      item { Button(onClick={editingProviderId=null;providerBackPage=Page.PROVIDER;composePage=Page.ADD_PROVIDER},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text("添加模型供应商（含本地）",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)} }
      items(allProviders,key={it.id}) { value ->
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(start=4.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){SettingButton(if(activeProvider()?.id==value.id)Icons.Filled.CheckCircle else if(isLocalProviderEndpoint(value.endpoint))Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,value.name,if(activeProvider()?.id==value.id)"当前使用 · ${value.modelName}" else value.modelName){activateProvider(value)}};IconButton(onClick={editingProviderId=value.id;providerBackPage=Page.PROVIDER;composePage=Page.ADD_PROVIDER}){Icon(Icons.Outlined.Edit,"编辑")};IconButton(onClick={val wasActive=activeProvider()?.id==value.id;val updated=allProviders.filterNot{it.id==value.id};saveCustomProviders(updated);if(wasActive){prefs.edit().remove("active_provider_id").remove("provider").remove("endpoint").remove("model").apply();runCatching{secure.put("")}};allProviders=updated;uiRevision++;toast("供应商已删除")}){Icon(Icons.Outlined.Delete,"删除",tint=MaterialTheme.colorScheme.error)}}}
      }
    }
  }

  @Composable
  private fun AddProviderPage(){
    val editing=providers().firstOrNull{it.id==editingProviderId}
    var name by remember(editingProviderId){mutableStateOf(editing?.name.orEmpty())};var url by remember(editingProviderId){mutableStateOf(editing?.endpoint.orEmpty())};var providerKey by remember(editingProviderId){mutableStateOf(editing?.let{secure.getNamed("provider_key_${it.id}")}.orEmpty())};var selectedModel by remember(editingProviderId){mutableStateOf(editing?.modelName.orEmpty())};var models by remember{mutableStateOf<List<String>>(emptyList())};var loading by remember{mutableStateOf(false)};var testing by remember{mutableStateOf(false)};var testContent by rememberSaveable(editingProviderId){mutableStateOf("请只回复：连接成功")};var testResult by remember{mutableStateOf("")};var savingProvider by remember(editingProviderId){mutableStateOf(false)}
    val localInstalled=LocalModelRepository.models(this@ComposeActivity).filter{it.kind=="chat"&&LocalModelRepository.installed(this@ComposeActivity,it)&&it.fileName.endsWith(".gguf",true)}.map{it.fileName}
    DetailScaffold(if(editing==null)"添加模型供应商" else "编辑模型供应商",providerBackPage){
      item{InfoBanner(Icons.Outlined.AddLink,"完整供应商配置","在一个页面中保存名称、API 地址、Key 和模型。保存后可在供应商列表自由切换。")}
      if(editing==null)item{SectionTitle("快速填充")}
      if(editing==null)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.fillMaxWidth().heightIn(max=310.dp).verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){listOf(ProviderConfig("","App 内本地模型","local://in-app","qwen2.5-0.5b-instruct-q4_k_m.gguf"),ProviderConfig("","OpenAI","https://api.openai.com/v1","gpt-4o-mini"),ProviderConfig("","DeepSeek","https://api.deepseek.com/v1","deepseek-chat"),ProviderConfig("","OpenRouter","https://openrouter.ai/api/v1","openai/gpt-4o-mini"),ProviderConfig("","Google Gemini","https://generativelanguage.googleapis.com/v1beta/openai","gemini-2.5-flash"),ProviderConfig("","通义千问 DashScope","https://dashscope.aliyuncs.com/compatible-mode/v1","qwen-plus"),ProviderConfig("","硅基流动","https://api.siliconflow.cn/v1","deepseek-ai/DeepSeek-V3"),ProviderConfig("","Groq","https://api.groq.com/openai/v1","llama-3.3-70b-versatile"),ProviderConfig("","Together AI","https://api.together.xyz/v1","meta-llama/Llama-3.3-70B-Instruct-Turbo"),ProviderConfig("","Moonshot","https://api.moonshot.cn/v1","moonshot-v1-8k"),ProviderConfig("","智谱 BigModel","https://open.bigmodel.cn/api/paas/v4","glm-4-flash"),ProviderConfig("","NVIDIA NIM","https://integrate.api.nvidia.com/v1","meta/llama-3.1-70b-instruct")).forEach{preset->OutlinedButton(onClick={name=preset.name;url=preset.endpoint;selectedModel=preset.modelName;providerKey=""},Modifier.fillMaxWidth().height(42.dp)){Icon(if(isLocalProviderEndpoint(preset.endpoint))Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(preset.name,Modifier.weight(1f),maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis);Icon(Icons.Outlined.ChevronRight,null)}}}}}
      item{SectionTitle("连接配置")};item{StyledField(name,{name=it},"供应商名称")}
      if(isLocalProviderEndpoint(url)){
        item{InfoBanner(Icons.Outlined.PhoneAndroid,"App 内本地推理","无需填写地址或 API Key，直接选择已安装的 GGUF 模型。")}
        if(localInstalled.isEmpty())item{SettingButton(Icons.Outlined.Download,"还没有可用模型","前往下载或导入 GGUF 对话模型"){composePage=Page.LOCAL_MODELS}}
        else item{ModelListCard(localInstalled,selectedModel){selectedModel=it}}
      }else{
        item{StyledField(url,{url=it},"API 基础地址")};item{StyledField(providerKey,{providerKey=it},"API Key")};item{StyledField(selectedModel,{selectedModel=it},"模型")}
        item{Button(onClick={if(url.isBlank()||providerKey.isBlank())toast("请先填写 API 地址和 Key")else{loading=true;pool.execute{try{val base=url.trim().trimEnd('/').removeSuffix("/chat/completions");val c=URL("$base/models").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;if(providerKey.isNotBlank())c.setRequestProperty("Authorization","Bearer "+providerKey.trim());val code=c.responseCode;val raw=read(if(code<300)c.inputStream else c.errorStream);if(code !in 200..299)throw java.io.IOException("HTTP $code：$raw");val data=JSONObject(raw).optJSONArray("data")?:JSONArray();val result=mutableListOf<String>();for(i in 0 until data.length())data.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()}?.let(result::add);result.sort();main.post{models=result;loading=false}}catch(e:Exception){main.post{loading=false;recordApiError(e);toast("模型获取失败：${friendlyError(e)}")}}}}},Modifier.fillMaxWidth().height(46.dp)){if(loading)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(if(loading)"正在获取模型" else "调用 API 获取模型",maxLines=1,softWrap=false)}}
        if(models.isNotEmpty())item{ModelListCard(models,selectedModel){selectedModel=it}}
      }
      item{SectionTitle("连接测试")}
      item{OutlinedTextField(testContent,{testContent=it.take(1000)},Modifier.fillMaxWidth().heightIn(min=90.dp,max=180.dp),label={Text("测试内容")},supportingText={Text("可自定义发送给模型的测试内容")},minLines=3,maxLines=7,shape=RoundedCornerShape(13.dp))}
      item{OutlinedButton(onClick={if(url.isBlank()||selectedModel.isBlank()||(!isLocalProviderEndpoint(url)&&providerKey.isBlank()))toast("请先完整填写地址、模型和所需密钥")else{testing=true;testResult="";pool.execute{runCatching{testProviderConnection(url,providerKey,selectedModel,testContent)}.onSuccess{answer->main.post{testing=false;testResult=answer;clearApiError()}}.onFailure{error->main.post{testing=false;recordApiError(error);testResult="失败：${friendlyError(error)}"}}}}},enabled=!testing,modifier=Modifier.fillMaxWidth().height(46.dp)){if(testing)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.NetworkCheck,null);Spacer(Modifier.width(7.dp));Text(if(testing)"正在测试" else "测试模型连接",maxLines=1,softWrap=false)}}
      if(testResult.isNotBlank())item{InfoBanner(if(testResult.startsWith("失败"))Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,if(testResult.startsWith("失败"))"连接测试失败" else "模型已响应",testResult)}
      item{Button(onClick={if(!savingProvider){if(name.isBlank()||url.isBlank()||selectedModel.isBlank()||(!isLocalProviderEndpoint(url)&&providerKey.isBlank()))toast("请完整填写供应商、地址、模型和所需密钥")else{savingProvider=true;val value=ProviderConfig(editing?.id?:UUID.randomUUID().toString(),name.trim(),url.trim().trimEnd('/').removeSuffix("/chat/completions"),selectedModel.trim());val duplicate=if(editing==null)providers().firstOrNull{it.name==value.name&&it.endpoint==value.endpoint&&it.modelName==value.modelName} else null;val target=if(duplicate!=null)value.copy(id=duplicate.id) else value;val updated=if(editing==null){if(duplicate==null)providers()+target else providers().map{if(it.id==duplicate.id)target else it}}else providers().map{if(it.id==value.id)value else it};saveCustomProviders(updated);runCatching{secure.putNamed("provider_key_${target.id}",providerKey.trim())};if(activeProvider()?.id==target.id||editing==null)activateProvider(target);toast(if(editing==null)"供应商已添加并启用" else "供应商配置已保存");editingProviderId=null;composePage=providerBackPage}}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text(if(editing==null)"保存并启用" else "保存修改",maxLines=1,softWrap=false)}}
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
  private fun RoleProfilePage(){
    val card=find(editingCardId?:return)?:return
    val sessions=remember(card.id,uiRevision){chatsForCard(card.id)}
    val profileChat=sessions.firstOrNull{it.id==roleProfileChatId}?:sessions.maxByOrNull{it.updated}
    val profileChatId=profileChat?.id
    val state=remember(card.id,profileChatId,uiRevision){roleLlmState(card,profileChatId)}
    val events=remember(card.id,profileChatId,uiRevision){relationshipEvents(card.id,profileChatId)}
    var llmBusy by remember(card.id,profileChatId){mutableStateOf(false)}
    var llmError by remember(card.id,profileChatId){mutableStateOf("")}
    fun updateLlmState(){
      if(llmBusy||sending){toast(if(sending)"请等待当前回复完成后再更新状态" else "角色状态正在更新");return}
      val provider=activeProvider();if(provider==null){toast("请先配置模型供应商");return}
      llmBusy=true;llmError=""
      val history=profileChat?.messages?.filter{!it.loading&&it.body.isNotBlank()}?.takeLast(48).orEmpty()
      val prompt="""请根据你的角色设定、长期记忆和你与用户的真实互动，更新你自己的关系状态。
只返回一个 JSON 对象，不要 Markdown、解释或代码围栏；没有足够证据的数值必须返回 null，禁止根据消息数量、时间或本地公式猜测。
字段：affection 0-100 或 null；trust 0-100 或 null；energy 0-100 或 null；fatigue 0-100 或 null；mood 0-100 或 null；hobbies 字符串数组；summary 字符串（不超过 160 字）。
以下是最近真实互动记录：
${history.joinToString("\n"){"${if(it.role=="user")"用户" else "角色"}：${it.body.take(500)}"}}
""".trim()
      pool.execute{
        runCatching{
          val temp=Chat().apply{id="role-state-${UUID.randomUUID()}";cardId=card.id;title="角色状态更新";streamEnabled=false}
          beginChatGeneration(temp.id)
          try{
            val result=completeStreamOnce(temp,arrayListOf(Msg("user",prompt)),card){_,_->}
            val raw=result.text.trim();val start=raw.indexOf('{');val end=raw.lastIndexOf('}');require(start>=0&&end>start){"角色没有返回 JSON"}
            val obj=JSONObject(raw.substring(start,end+1));val clean=JSONObject()
            listOf("affection","trust","energy","fatigue","mood").forEach{key->if(obj.has(key)&&!obj.isNull(key)){val value=obj.optInt(key,-1);if(value in 0..100)clean.put(key,value)}else clean.put(key,JSONObject.NULL)}
            val hobbies=JSONArray();(obj.optJSONArray("hobbies")?:JSONArray()).let{a->for(i in 0 until a.length())a.optString(i).trim().takeIf{it.isNotBlank()}?.let{hobbies.put(it.take(40))}}
            clean.put("hobbies",hobbies).put("summary",obj.optString("summary").trim().take(160)).put("generatedAt",System.currentTimeMillis())
            clean
          }finally{endChatGeneration(temp.id)}
        }.onSuccess{value->main.post{val root=relationshipRoot(card.id,profileChatId);root.put("llmState",value).remove("pendingLlmHint");saveRelationshipRoot(card.id,root,profileChatId);llmBusy=false;uiRevision++;toast("${card.name} 已更新当前会话的状态")}}.onFailure{error->main.post{llmBusy=false;llmError=friendlyError(error);toast("角色状态更新失败：$llmError")}}
      }
    }
    DetailScaffold("${card.name} · 角色信息",roleProfileBackPage){
      item{Surface(shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,76);Column(Modifier.weight(1f).padding(start=15.dp)){Text(card.name,fontSize=24.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(card.desc.ifBlank{"等待与你相遇"},fontSize=13.sp,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=.76f),maxLines=3,overflow=TextOverflow.Ellipsis)}};Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={newChat(card)},modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.AddComment,null);Spacer(Modifier.width(6.dp));Text("新会话",maxLines=1,softWrap=false)};OutlinedButton(onClick={composePage=Page.ROLE_SESSIONS},modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.History,null);Spacer(Modifier.width(6.dp));Text("历史会话",maxLines=1,softWrap=false)}}}}}
      if(sessions.isNotEmpty())item{Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Text("状态所属会话",fontWeight=FontWeight.SemiBold);Text("好感、精力、疲惫、兴趣和关系足迹都按聊天会话独立保存。",Modifier.padding(top=4.dp),fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.fillMaxWidth().padding(top=9.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){sessions.sortedByDescending{it.updated}.take(12).forEach{session->FilterChip(selected=session.id==profileChatId,onClick={roleProfileChatId=session.id;llmError="";uiRevision++},label={Text(session.title.ifBlank{"与 ${card.name} 的会话"},maxLines=1,overflow=TextOverflow.Ellipsis)})}}}}}
      item{SectionTitle("角色卡信息")}
      item{Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){RoleInfoLine("创作者",card.creator);RoleInfoLine("版本",card.characterVersion);RoleInfoLine("标签",runCatching{val a=JSONArray(card.tags);buildList{for(i in 0 until a.length())add(a.optString(i))}.joinToString("、")}.getOrDefault(card.tags).ifBlank{"未填写"});Text("角色设定",fontWeight=FontWeight.SemiBold);Text(card.persona.ifBlank{"未填写角色设定"},fontSize=13.sp,lineHeight=20.sp,maxLines=8,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
      item{SectionTitle("角色状态")}
      item{Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){NullableRelationshipMetric("好感",state.affection,Icons.Outlined.Favorite,Color(0xFFE05B73),Modifier.weight(1f));NullableRelationshipMetric("信任",state.trust,Icons.Outlined.VerifiedUser,MaterialTheme.colorScheme.secondary,Modifier.weight(1f));NullableRelationshipMetric("心情",state.mood,Icons.Outlined.Mood,Color(0xFFDB6C91),Modifier.weight(1f))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){NullableRelationshipMetric("精力",state.energy,Icons.Outlined.Bolt,Color(0xFFE1A92F),Modifier.weight(1f));NullableRelationshipMetric("疲惫",state.fatigue,Icons.Outlined.Bedtime,Color(0xFF7B75C9),Modifier.weight(1f));NullableRelationshipMetric("听歌足迹",relationshipRoot(card.id,profileChatId).optInt("listens",0).takeIf{it>0},Icons.Outlined.Headphones,Color(0xFFE43D3D),Modifier.weight(1f))};if(state.summary.isNotBlank())Text("“${state.summary}”",fontSize=13.sp,lineHeight=20.sp,fontStyle=FontStyle.Italic);if(state.generatedAt>0)Text("最近生成：${time(state.generatedAt)}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(llmError.isNotBlank())Text(llmError,fontSize=11.sp,color=MaterialTheme.colorScheme.error);Button(onClick={updateLlmState()},enabled=!llmBusy,modifier=Modifier.fillMaxWidth().height(46.dp)){if(llmBusy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(7.dp));Text(if(llmBusy)"角色正在更新…" else "让角色更新当前会话状态",maxLines=1,softWrap=false)}}}}
      item{SectionTitle("兴趣星球")}
      item{if(state.hobbies.isEmpty())EmptyState(Icons.Outlined.Interests,"尚未生成兴趣","点击“让角色更新状态”即可生成兴趣")else Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){InterestPlanet(state.hobbies);Text("兴趣会随当前会话的角色状态更新。",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
      item{SectionTitle("网易云与一起听")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={roleProfileChatId=profileChatId;musicBackPage=Page.ROLE_PROFILE;composePage=Page.MUSIC},modifier=Modifier.weight(1f).height(46.dp)){Icon(Icons.Filled.MusicNote,null);Spacer(Modifier.width(6.dp));Text("网易云音乐",maxLines=1,softWrap=false)};OutlinedButton(onClick={val target=profileChat?:run{newChat(card);selectedChat};target?.let{openTogetherListen(it,backPage=Page.ROLE_PROFILE)}},modifier=Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Headphones,null);Spacer(Modifier.width(6.dp));Text("一起听歌",maxLines=1,softWrap=false)}}}
      item{SectionTitle("最近会话")}
      if(sessions.isEmpty())item{EmptyState(Icons.Outlined.History,"还没有历史会话","点击上方“新会话”开始")}
      else items(sessions.take(5),key={it.id}){session->RoleSessionRow(session,card){chat(session)}}
      item{TextButton(onClick={composePage=Page.ROLE_SESSIONS},modifier=Modifier.fillMaxWidth().height(44.dp)){Text("查看全部历史会话",maxLines=1,softWrap=false);Spacer(Modifier.width(5.dp));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp))}}
      item{SectionTitle("关系足迹")}
      if(events.isEmpty())item{EmptyState(Icons.Outlined.AutoStories,"还没有关系足迹","聊天和一起听歌会留下属于你们的记录")}else items(events.take(8),key={it.optString("id")}){event->Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.FavoriteBorder,null,Modifier.size(19.dp),tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(start=10.dp)){Text(event.optString("title"),fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(event.optString("detail"),fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(time(event.optLong("at")),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
    }
  }

  @Composable private fun RoleInfoLine(label:String,value:String){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){Text(label,Modifier.width(58.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value.ifBlank{"未填写"},Modifier.weight(1f),fontSize=13.sp,maxLines=3,overflow=TextOverflow.Ellipsis)}}
  @Composable private fun NullableRelationshipMetric(label:String,value:Int?,icon:androidx.compose.ui.graphics.vector.ImageVector,color:Color,modifier:Modifier=Modifier){Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)){Column(Modifier.fillMaxWidth().padding(11.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,Modifier.size(16.dp),tint=color);Text(label,Modifier.padding(start=5.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(value?.toString()?:"未生成",Modifier.padding(top=5.dp),fontSize=17.sp,fontWeight=FontWeight.Bold);if(value!=null)LinearProgressIndicator(progress={value.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().padding(top=5.dp).height(5.dp).clip(CircleShape),color=color,trackColor=color.copy(alpha=.13f))}}}
  @Composable private fun RoleSessionRow(session:Chat,card:Card,onClick:()->Unit){Surface(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).clickable(onClick=onClick),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){RoleAvatar(card,42);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(session.title.ifBlank{"与 ${card.name} 的会话"},fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(session.messages.lastOrNull{!it.loading}?.body?.ifBlank{"空会话"}?:"空会话",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)};Column(horizontalAlignment=Alignment.End){Text(time(session.updated),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("${session.messages.count{!it.loading}} 条",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}

  @Composable private fun RoleSessionsPage(){
    val card=find(editingCardId?:return)?:return;val sessions=remember(card.id,uiRevision){chatsForCard(card.id)}
    DetailScaffold("${card.name} · 历史会话",Page.ROLE_PROFILE){item{Button(onClick={newChat(card)},modifier=Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.AddComment,null);Spacer(Modifier.width(7.dp));Text("新会话")}};if(sessions.isEmpty())item{EmptyState(Icons.Outlined.History,"还没有历史会话","新的故事会出现在这里")}else items(sessions,key={it.id}){session->RoleSessionRow(session,card){chat(session)}}}
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable private fun NeteaseMusicPage(){
    var tab by rememberSaveable{mutableIntStateOf(0)}
    var query by rememberSaveable{mutableStateOf("")}
    var songs by remember{mutableStateOf<List<NeteaseSong>>(emptyList())}
    var playlists by remember{mutableStateOf<List<NeteasePlaylist>>(emptyList())}
    var recent by remember{mutableStateOf<List<NeteaseSong>>(emptyList())}
    var appRecent by remember{mutableStateOf<List<NeteaseSong>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    var showLogin by remember{mutableStateOf(false)}
    var loginBusy by remember{mutableStateOf(false)}
    var loginWebView by remember{mutableStateOf<WebView?>(null)}
    var roleMenu by remember{mutableStateOf(false)}
    var listeningChatId by rememberSaveable{mutableStateOf(when(musicBackPage){Page.TOGETHER_LISTEN->togetherChatId;Page.ROLE_PROFILE->roleProfileChatId;else->null})}
    val loggedIn=remember(uiRevision){neteaseCookie().isNotBlank()}
    val accountName=remember(uiRevision){prefs.getString("netease_account_name","").orEmpty().ifBlank{"网易云用户"}}
    val targetChat=listeningChat(listeningChatId)
    val role=targetChat?.let{find(it.cardId)}
    fun startTogether(song:NeteaseSong?=null,queue:List<NeteaseSong> = emptyList()){
      val target=targetChat
      if(target==null){toast("请先选择一个私聊会话");return}
      openTogetherListen(target,song,queue,if(musicBackPage==Page.TOGETHER_LISTEN)togetherBackPage else Page.MUSIC)
    }
    fun playSong(song:NeteaseSong){
      if(musicBackPage==Page.TOGETHER_LISTEN)startTogether(song)
      else toggleChatMusic(neteaseSongUrl(song.id),song.title,song.artist,song.album,song.id,coverUrl=song.coverUrl)
    }
    fun enqueueSong(song:NeteaseSong){
      val target=targetChat?:run{toast("请先选择一个私聊会话");return}
      val added=addToTogetherQueue(song,target)
      toast(if(added)"已加入播放列表：${song.title}" else "歌曲已在播放列表")
    }
    fun search(){val value=query.trim();if(value.isBlank()){toast("请输入歌曲或歌手");return};loading=true;error="";pool.execute{runCatching{searchNeteaseSongs(value)}.onSuccess{result->main.post{songs=result;tab=0;loading=false;if(result.isEmpty())error="没有找到歌曲，换个关键词试试"}}.onFailure{e->main.post{loading=false;error=e.message?:"网易云搜索失败"}}}}
    fun loadPrivate(target:Int){if(!loggedIn){toast("请先登录网易云账号");showLogin=true;return};loading=true;error="";pool.execute{runCatching{val account=neteaseAccount(neteaseCookie());prefs.edit().putLong("netease_user_id",account.userId).putString("netease_account_name",account.nickname).apply();when(target){1->playlists=neteasePlaylists(account);2->recent=neteaseRecentSongs(account)};target}.onSuccess{value->main.post{tab=value;loading=false;uiRevision++}}.onFailure{e->main.post{loading=false;error=e.message?:"网易云数据加载失败"}}}}
    fun loadAppRecent(){appRecent=appRecentSongs();error="";tab=3}
    fun closeLogin(){runCatching{loginWebView?.stopLoading();loginWebView?.destroy()};loginWebView=null;showLogin=false}
    fun openLogin(){val manager=android.webkit.CookieManager.getInstance();manager.setAcceptCookie(true);neteaseCookie().takeIf{it.isNotBlank()}?.split(';')?.map(String::trim)?.filter(String::isNotBlank)?.forEach{manager.setCookie("https://music.163.com",it)};manager.flush();showLogin=true}
    fun finishLogin(){val manager=android.webkit.CookieManager.getInstance();manager.flush();val cookie=manager.getCookie("https://music.163.com").orEmpty().trim();if(!cookie.contains("MUSIC_U=")&&!cookie.contains("MUSIC_A=")){toast("还没有检测到网易云登录");return};loginBusy=true;pool.execute{runCatching{val account=neteaseAccount(cookie);secure.putNamed("netease_music_cookie",cookie);account}.onSuccess{account->main.post{prefs.edit().putLong("netease_user_id",account.userId).putString("netease_account_name",account.nickname).apply();loginBusy=false;closeLogin();uiRevision++;toast("网易云账号已登录：${account.nickname}")}}.onFailure{e->main.post{loginBusy=false;toast("网易云登录保存失败：${e.message?:"未知错误"}")}}}}
    fun logout(){runCatching{secure.putNamed("netease_music_cookie","")};prefs.edit().remove("netease_user_id").remove("netease_account_name").apply();android.webkit.CookieManager.getInstance().removeAllCookies(null);android.webkit.CookieManager.getInstance().flush();uiRevision++;toast("已退出网易云账号")}
    DisposableEffect(Unit){onDispose{runCatching{loginWebView?.stopLoading();loginWebView?.destroy()};loginWebView=null}}
    DetailScaffold("网易云音乐",musicBackPage){
      item{Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFF211C20)){Column(Modifier.fillMaxWidth().padding(17.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE43D3D)),contentAlignment=Alignment.Center){Icon(Icons.Filled.MusicNote,null,tint=Color.White)};Column(Modifier.weight(1f).padding(start=12.dp)){Text("网易云音乐",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(if(loggedIn)"已登录 · $accountName" else "登录后查看自己的歌单和最近播放",fontSize=12.sp,color=Color.White.copy(alpha=.68f),maxLines=1,overflow=TextOverflow.Ellipsis)};if(loggedIn)IconButton(onClick={logout()}){Icon(Icons.Outlined.Logout,"退出登录",tint=Color.White.copy(alpha=.8f))}else TextButton(onClick={openLogin()},colors=ButtonDefaults.textButtonColors(contentColor=Color.White)){Text("登录",maxLines=1,softWrap=false)}};OutlinedTextField(query,{query=it.take(80)},Modifier.fillMaxWidth().padding(top=12.dp),label={Text("搜索歌曲、歌手或专辑")},singleLine=true,trailingIcon={IconButton(onClick={search()}){Icon(Icons.Outlined.Search,"搜索")}},shape=RoundedCornerShape(14.dp),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=Color(0xFFE43D3D),unfocusedBorderColor=Color.White.copy(alpha=.3f),focusedLabelColor=Color(0xFFFFB4AB),unfocusedLabelColor=Color.White.copy(alpha=.65f),cursorColor=Color.White));Row(Modifier.fillMaxWidth().padding(top=12.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("搜索","我的歌单","网易云最近","应用最近").forEachIndexed{i,label->FilterChip(selected=tab==i,onClick={when(i){0->tab=0;1,2->loadPrivate(i);else->loadAppRecent()}},label={Text(label,maxLines=1,softWrap=false)})}}}}}
      item{Box(Modifier.fillMaxWidth().padding(top=8.dp)){
        OutlinedButton(onClick={roleMenu=true},enabled=musicBackPage!=Page.TOGETHER_LISTEN,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
          Icon(Icons.Outlined.Headphones,null);Spacer(Modifier.width(6.dp))
          Text(if(targetChat==null)"选择一起听的会话" else "${role?.name.orEmpty()} · ${targetChat.title}",Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis)
          if(musicBackPage!=Page.TOGETHER_LISTEN)Icon(Icons.Outlined.ExpandMore,null)
        }
        DropdownMenu(expanded=roleMenu,onDismissRequest={roleMenu=false}){
          val candidates=chats.filter{!it.group&&find(it.cardId)!=null}.sortedByDescending{it.updated}
          if(candidates.isEmpty())DropdownMenuItem(text={Text("暂无私聊会话")},onClick={},enabled=false)
          candidates.forEach{candidate->DropdownMenuItem(text={Column{
            Text("${find(candidate.cardId)?.name.orEmpty()} · ${candidate.title}",maxLines=2,overflow=TextOverflow.Ellipsis)
            Text("${time(candidate.updated)} · ${candidate.id.takeLast(6)}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          }},onClick={listeningChatId=candidate.id;roleMenu=false})}
        }
      }}
      item{OutlinedButton(onClick={startTogether()},modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(44.dp)){Icon(Icons.Outlined.Headphones,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text("进入一起听歌",maxLines=1,softWrap=false);Spacer(Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp))}}
      if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp),color=Color(0xFFE43D3D))}
      if(error.isNotBlank())item{Text(error,Modifier.fillMaxWidth().padding(top=8.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.error)}
      when(tab){0->{if(songs.isEmpty()&&!loading)item{EmptyState(Icons.Outlined.Search,"搜索网易云歌曲","歌曲、歌手和专辑都可以搜索")};songs.take(30).forEach{song->item(key="song-${song.id}"){NeteaseSongRow(song,musicPlayingUrl==neteaseSongUrl(song.id),musicLoadingUrl==neteaseSongUrl(song.id),onPlay={playSong(song)},onTogether={startTogether(song,songs)},onAddToQueue={enqueueSong(song)})}}};1->{if(playlists.isEmpty()&&!loading)item{EmptyState(Icons.Outlined.QueueMusic,"还没有歌单","登录网易云后加载自己的歌单")};playlists.forEach{playlist->item(key="playlist-${playlist.id}"){Surface(Modifier.fillMaxWidth().padding(vertical=3.dp).clip(RoundedCornerShape(15.dp)).clickable{loading=true;pool.execute{runCatching{neteasePlaylistSongs(playlist.id)}.onSuccess{value->main.post{songs=value;tab=0;loading=false}}.onFailure{e->main.post{loading=false;error=e.message?:"歌单加载失败"}}}},color=MaterialTheme.colorScheme.surface){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){RemoteArtwork(playlist.coverUrl,Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)));Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(playlist.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${playlist.trackCount} 首歌曲",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.ChevronRight,"打开歌单")}}}}};2->{if(recent.isEmpty()&&!loading)item{EmptyState(Icons.Outlined.History,"暂无网易云最近播放","登录网易云后加载最近播放")};recent.forEach{song->item(key="recent-${song.id}"){NeteaseSongRow(song,musicPlayingUrl==neteaseSongUrl(song.id),musicLoadingUrl==neteaseSongUrl(song.id),onPlay={playSong(song)},onTogether={startTogether(song,recent)},onAddToQueue={enqueueSong(song)})}}};else->{if(appRecent.isEmpty())item{EmptyState(Icons.Outlined.History,"暂无应用播放记录","在时遇播放过的歌曲会显示在这里")};appRecent.forEach{song->item(key="app-recent-${song.id}"){NeteaseSongRow(song,musicPlayingUrl==neteaseSongUrl(song.id),musicLoadingUrl==neteaseSongUrl(song.id),onPlay={playSong(song);appRecent=appRecentSongs()},onTogether={startTogether(song,appRecent)},onAddToQueue={enqueueSong(song)})}}}}
      item{Text("歌曲播放使用网易云公开外链。版权、地区或会员限制可能导致部分歌曲无法播放；时遇不会绕过付费版权。",Modifier.fillMaxWidth().padding(top=8.dp),fontSize=10.sp,lineHeight=15.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    if(showLogin)Dialog(onDismissRequest={if(!loginBusy)closeLogin()},properties=DialogProperties(usePlatformDefaultWidth=false)){Surface(Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal=10.dp,vertical=18.dp),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={if(!loginBusy)closeLogin()},enabled=!loginBusy){Icon(Icons.Outlined.Close,"关闭")};Column(Modifier.weight(1f)){Text("登录网易云音乐",fontWeight=FontWeight.SemiBold);Text("登录过程由网易云官方网页完成",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Filled.MusicNote,null,tint=Color(0xFFE43D3D))};HorizontalDivider();AndroidView(factory={context->val manager=android.webkit.CookieManager.getInstance();manager.setAcceptCookie(true);WebView(context).apply{loginWebView=this;settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.databaseEnabled=true;settings.allowFileAccess=false;settings.allowContentAccess=false;if(android.os.Build.VERSION.SDK_INT>=21){settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;manager.setAcceptThirdPartyCookies(this,true)};webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView,request:android.webkit.WebResourceRequest)=false};loadUrl("https://music.163.com/m/login")}},Modifier.fillMaxWidth().weight(1f));HorizontalDivider();Column(Modifier.fillMaxWidth().padding(14.dp)){Text("使用网易云官方页面登录，时遇只保存加密 Cookie，不读取密码。",fontSize=11.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){OutlinedButton(onClick={closeLogin()},enabled=!loginBusy,modifier=Modifier.weight(1f).height(46.dp)){Text("稍后")};Button(onClick={finishLogin()},enabled=!loginBusy,modifier=Modifier.weight(1f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE43D3D),contentColor=Color.White)){if(loginBusy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Color.White)else Icon(Icons.Outlined.CheckCircle,null);Spacer(Modifier.width(6.dp));Text(if(loginBusy)"验证中" else "完成登录")}}}}}}
  }

  @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
  @Composable private fun TogetherListenPage(){
    val chat=activeTogetherChat()
    if(chat==null){DetailScaffold("一起听歌",Page.MUSIC){item{Text("当前会话已结束",Modifier.padding(16.dp))}};return}
    val card=find(chat.cardId)?:return
    val song=togetherSong
    val url=song?.let{neteaseSongUrl(it.id)}.orEmpty()
    val ownsPlayback=musicOwnerChatId==chat.id&&musicOwnerRoleId==card.id&&url.isNotBlank()&&url in listOf(musicPlayingUrl,musicPausedUrl,musicLoadingUrl)
    val cover=song?.coverUrl.orEmpty().ifBlank{if(ownsPlayback)musicCoverUrl else ""}
    val playing=ownsPlayback&&musicPlayingUrl==url
    val loading=ownsPlayback&&musicLoadingUrl==url
    val duration=((if(ownsPlayback)musicDurationMs.takeIf{it>0}else null)?:song?.durationMs?:0L).coerceAtLeast(0L)
    val position=(if(ownsPlayback)musicPositionMs else 0L).coerceIn(0L,duration.takeIf{it>0}?:Long.MAX_VALUE)
    var seeking by remember(chat.id,song?.id){mutableStateOf(false)}
    var seekPosition by remember(chat.id,song?.id){mutableFloatStateOf(position.toFloat())}
    LaunchedEffect(position,duration,seeking){if(!seeking)seekPosition=position.coerceAtMost(duration.takeIf{it>0}?:position).toFloat()}
    val displayedPosition=if(seeking)seekPosition.toLong()else position
    val backgroundKey=TogetherListening.sessionKey(chat.id)+"_background"
    var backgroundPath by remember(chat.id,uiRevision){mutableStateOf(prefs.getString(backgroundKey,"").orEmpty())}
     var chatInput by rememberSaveable(card?.id,chat?.id){mutableStateOf("")}
     var historyExpanded by rememberSaveable(card?.id,chat?.id){mutableStateOf(false)}
     val playMode=musicPlayMode()
    val enteredAt=remember(card?.id,chat?.id){System.currentTimeMillis()}
    var transientReplyId by remember(card?.id,chat?.id){mutableStateOf("")}
    var lyrics by remember(song?.id){mutableStateOf<List<LyricLine>>(emptyList())}
    var lyricsLoading by remember(song?.id){mutableStateOf(false)}
    var lyricsError by remember(song?.id){mutableStateOf("")}
    val playerPager=rememberPagerState(initialPage=0,pageCount={2})
    val historyListState=rememberLazyListState()
    val allHistory=chat?.messages?.filter{!it.loading&&it.body.isNotBlank()&&it.messageType!="tool_event"}.orEmpty()
    val shownHistory=if(historyExpanded)allHistory.takeLast(60)else allHistory.takeLast(8)
    val newestRoleReply=allHistory.lastOrNull{it.role=="assistant"&&it.createdAt>=enteredAt}
    val transientReply=newestRoleReply?.takeIf{it.id==transientReplyId}
    LaunchedEffect(newestRoleReply?.id){val value=newestRoleReply?:return@LaunchedEffect;transientReplyId=value.id;delay(6500);if(transientReplyId==value.id)transientReplyId=""}
    LaunchedEffect(song?.id){val id=song?.id?:0L;if(id<=0){lyrics=emptyList();lyricsError="";return@LaunchedEffect};lyricsLoading=true;lyricsError="";val result=withContext(Dispatchers.IO){runCatching{neteaseLyrics(id)}};result.onSuccess{lyrics=it;if(it.isEmpty())lyricsError="这首歌暂无滚动歌词"}.onFailure{lyrics=emptyList();lyricsError=it.message?:"歌词加载失败"};lyricsLoading=false}
    LaunchedEffect(shownHistory.size,historyExpanded){if(shownHistory.isNotEmpty())historyListState.scrollToItem(shownHistory.lastIndex)}
    val backgroundPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
      if(uri!=null)runCatching{
        val target=File(filesDir,"together_bg_chat_${UUID.nameUUIDFromBytes(chat.id.toByteArray(Charsets.UTF_8))}.img")
        contentResolver.openInputStream(uri)?.use{input->FileOutputStream(target).use{output->input.copyTo(output)}}?:error("无法读取背景图")
        bitmapCache.remove(target.absolutePath);prefs.edit().putString(backgroundKey,target.absolutePath).apply();backgroundPath=target.absolutePath;uiRevision++
      }.onFailure{toast("一起听背景保存失败：${it.message}")}
    }
    val spin=rememberInfiniteTransition(label="together-disc")
    val rotation by spin.animateFloat(0f,360f,infiniteRepeatable(tween(12000,easing=androidx.compose.animation.core.LinearEasing)),label="disc-rotation")
    fun sendTogetherMessage(){
      val target=card?:return;val body=chatInput.trim();if(body.isBlank())return
      if(activeProvider()==null){toast("请先配置模型供应商");return}
      val targetChat=listeningChat(chat.id)?:return
      if(targetChat.userBlockedRole){toast("请先解除拉黑后再与角色聊天");return}
      val wasSending=isChatSending(targetChat.id)
      if(targetChat.immersiveMode&&wasSending)restartImmersiveGeneration(targetChat)
      val message=Msg("user",body);message.messageType="text";message.meta=JSONObject().put("scene","together_listen").put("songTitle",song?.title.orEmpty()).put("songArtist",song?.artist.orEmpty()).toString();targetChat.messages.add(message);targetChat.updated=System.currentTimeMillis();targetChat.proactiveSequence=0;save();chatInput="";uiRevision++
      when{targetChat.immersiveMode->scheduleImmersiveReply(targetChat.id,target.id);wasSending->queueReply(targetChat,target);else->request(targetChat,target)}
    }
    fun playPrevious(){
      val index=togetherQueue.indexOfFirst{it.id==song?.id}
      val previous=when{musicPlayMode()=="repeat"->song;index>0->togetherQueue[index-1];togetherQueue.size>1->togetherQueue.lastOrNull();else->null}
      if(previous!=null)playTogetherSong(previous)else toast("播放列表里没有上一首")
    }
    fun playNext(){
      autoAdvanceTogether()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF160E15),Color(0xFF3B1923),Color(0xFF100D14))))){
      when{backgroundPath.isNotBlank()->MomentImage(backgroundPath,Modifier.fillMaxSize().graphicsLayer{scaleX=1.12f;scaleY=1.12f}.blur(24.dp));cover.isNotBlank()->RemoteArtwork(cover,Modifier.fillMaxSize().graphicsLayer{scaleX=1.18f;scaleY=1.18f}.blur(30.dp))}
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF100C12).copy(alpha=.58f),Color(0xFF28101A).copy(alpha=.70f),Color(0xFF09070B).copy(alpha=.88f)))))
      Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()){
      DetailScaffold(card?.let{"和 ${it.name} 一起听"}?:"听歌",togetherBackPage,contentColor=Color.White){
        item{Surface(shape=RoundedCornerShape(28.dp),color=Color.White.copy(alpha=.10f),border=BorderStroke(1.dp,Color.White.copy(alpha=.18f)),shadowElevation=0.dp){Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally){
           Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){Column(Modifier.width(64.dp),horizontalAlignment=Alignment.CenterHorizontally){RoleAvatar(card,48);Text(card.name,Modifier.padding(top=4.dp),fontSize=10.sp,color=Color.White.copy(alpha=.72f),maxLines=1,overflow=TextOverflow.Ellipsis)};Box(Modifier.padding(horizontal=12.dp).size(34.dp).clip(CircleShape).background(Color(0xFFE43D3D).copy(alpha=.92f)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Favorite,null,tint=Color.White)};Column(Modifier.width(64.dp),horizontalAlignment=Alignment.CenterHorizontally){UserAvatar(48);Text("我",Modifier.padding(top=4.dp),fontSize=10.sp,color=Color.White.copy(alpha=.72f))}}
           Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
             Text(chat.title.ifBlank{"与 ${card.name} 的会话"},Modifier.weight(1f),fontSize=11.sp,color=Color.White.copy(alpha=.72f),maxLines=2,overflow=TextOverflow.Ellipsis)
             TextButton(onClick={endTogetherListening()},colors=ButtonDefaults.textButtonColors(contentColor=Color.White.copy(alpha=.82f))){Icon(Icons.Outlined.PersonRemove,null,Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("结束一起听",maxLines=1)}
           }
          transientReply?.body?.takeIf{it.isNotBlank()}?.let{latest->Surface(Modifier.padding(top=10.dp).widthIn(max=280.dp).heightIn(max=78.dp),shape=RoundedCornerShape(18.dp,18.dp,18.dp,5.dp),color=Color.White.copy(alpha=.14f)){Text(latest.take(160),Modifier.padding(horizontal=13.dp,vertical=9.dp),fontSize=12.sp,lineHeight=17.sp,color=Color.White,maxLines=3,overflow=TextOverflow.Ellipsis)}}
          HorizontalPager(state=playerPager,modifier=Modifier.fillMaxWidth().padding(top=8.dp)){page->
            if(page==0)Column(Modifier.fillMaxWidth().height(330.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.padding(top=10.dp).size(238.dp).graphicsLayer{rotationZ=if(playing)rotation else 0f}.clip(CircleShape).background(Color(0xFF171217).copy(alpha=.92f)),contentAlignment=Alignment.Center){if(cover.isNotBlank())RemoteArtwork(cover,Modifier.fillMaxSize().clip(CircleShape))else Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFE43D3D)),contentAlignment=Alignment.Center){Icon(Icons.Filled.MusicNote,null,tint=Color.White,modifier=Modifier.size(60.dp))}};Text(song?.title?:"选择一首歌开始一起听",Modifier.padding(top=18.dp),color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song?.artist?:"网易云音乐",Modifier.padding(top=4.dp),color=Color.White.copy(alpha=.70f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
            else TogetherLyricsPanel(song?.title.orEmpty(),lyrics,lyricsLoading,lyricsError,position)
          }
          Row(Modifier.padding(top=3.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){repeat(2){index->Box(Modifier.size(if(playerPager.currentPage==index)18.dp else 6.dp,6.dp).clip(CircleShape).background(Color.White.copy(alpha=if(playerPager.currentPage==index).88f else .30f)))};Text(if(playerPager.currentPage==0)"左滑查看歌词" else "右滑返回唱片",Modifier.padding(start=3.dp),fontSize=10.sp,color=Color.White.copy(alpha=.58f))}
          Slider(value=seekPosition.coerceIn(0f,duration.coerceAtLeast(1L).toFloat()),onValueChange={seeking=true;seekPosition=it},onValueChangeFinished={if(duration>0&&musicOwnerChatId==chat.id&&ownsPlayback&&!loading){musicPositionMs=seekPosition.toLong();NeteaseMusicService.seek(this@ComposeActivity,musicPositionMs)};seeking=false},enabled=ownsPlayback&&!loading&&duration>0,modifier=Modifier.fillMaxWidth().padding(top=11.dp),valueRange=0f..duration.coerceAtLeast(1L).toFloat(),colors=SliderDefaults.colors(thumbColor=Color(0xFFE85A65),activeTrackColor=Color(0xFFE85A65),inactiveTrackColor=Color.White.copy(alpha=.18f)))
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(formatMusicTime(displayedPosition),fontSize=10.sp,color=Color.White.copy(alpha=.64f));Text(formatMusicTime(duration),fontSize=10.sp,color=Color.White.copy(alpha=.64f))}
           Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){IconButton(onClick={cycleMusicPlayMode()},modifier=Modifier.size(38.dp)){Icon(when(playMode){"repeat"->Icons.Outlined.RepeatOne;"shuffle"->Icons.Outlined.Shuffle;else->Icons.Outlined.FormatListBulleted},"播放模式",tint=Color.White)};IconButton(onClick={playPrevious()},enabled=song!=null){Icon(Icons.Filled.SkipPrevious,"上一首",tint=Color.White)};Surface(onClick={if(song==null){musicBackPage=Page.TOGETHER_LISTEN;composePage=Page.MUSIC}else toggleChatMusic(url,song.title,song.artist,song.album,song.id,card.id,card.name,cover,chatId=chat.id)},shape=CircleShape,color=Color(0xFFE43D3D),modifier=Modifier.size(66.dp)){Box(contentAlignment=Alignment.Center){if(loading)CircularProgressIndicator(Modifier.size(25.dp),strokeWidth=3.dp,color=Color.White)else Icon(if(playing)Icons.Filled.Pause else Icons.Filled.PlayArrow,if(playing)"暂停" else "播放",Modifier.size(34.dp),tint=Color.White)}};IconButton(onClick={playNext()},enabled=song!=null){Icon(Icons.Filled.SkipNext,"下一首",tint=Color.White)}}
          Row(Modifier.fillMaxWidth().padding(top=7.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={musicBackPage=Page.TOGETHER_LISTEN;composePage=Page.MUSIC},modifier=Modifier.weight(1f),colors=ButtonDefaults.textButtonColors(contentColor=Color.White)){Icon(Icons.Outlined.QueueMusic,null);Spacer(Modifier.width(5.dp));Text("添加歌曲 · ${togetherQueue.size} 首",maxLines=1,softWrap=false)};TextButton(onClick={backgroundPicker.launch(arrayOf("image/*"))},modifier=Modifier.weight(1f),colors=ButtonDefaults.textButtonColors(contentColor=Color.White)){Icon(Icons.Outlined.Wallpaper,null,Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text(if(backgroundPath.isBlank())"自定义背景" else "更换背景",maxLines=1,softWrap=false)};if(backgroundPath.isNotBlank())IconButton(onClick={backgroundPath.takeIf{it.startsWith(filesDir.absolutePath)}?.let{runCatching{File(it).delete()}};prefs.edit().remove(backgroundKey).apply();backgroundPath="";uiRevision++}){Icon(Icons.Outlined.Restore,"恢复专辑封面",tint=Color.White.copy(alpha=.82f))}}
        }}}
        if(card!=null)item{Surface(shape=RoundedCornerShape(24.dp),color=Color.White.copy(alpha=.10f),border=BorderStroke(1.dp,Color.White.copy(alpha=.16f)),shadowElevation=0.dp){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Forum,null,Modifier.size(19.dp),tint=Color.White);Text("边听边聊",Modifier.weight(1f).padding(start=7.dp),fontWeight=FontWeight.SemiBold);if(allHistory.size>8)TextButton(onClick={historyExpanded=!historyExpanded},colors=ButtonDefaults.textButtonColors(contentColor=Color.White.copy(alpha=.82f))){Text(if(historyExpanded)"收起" else "查看历史 ${allHistory.size}")}}
          if(shownHistory.isEmpty())Text("还没有聊天记录，音乐开始后说点什么吧。",Modifier.fillMaxWidth().padding(vertical=18.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontSize=12.sp,color=Color.White.copy(alpha=.62f))
          else LazyColumn(state=historyListState,modifier=Modifier.fillMaxWidth().heightIn(min=112.dp,max=if(historyExpanded)320.dp else 228.dp),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=4.dp)){items(shownHistory,key={it.id}){message->val mine=message.role=="user";Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start,verticalAlignment=Alignment.Bottom){if(!mine&&card!=null){RoleAvatar(card,28);Spacer(Modifier.width(6.dp))};Surface(modifier=Modifier.widthIn(max=270.dp),shape=if(mine)RoundedCornerShape(17.dp,17.dp,5.dp,17.dp)else RoundedCornerShape(17.dp,17.dp,17.dp,5.dp),color=if(mine)Color(0xFFE15662).copy(alpha=.84f)else Color.White.copy(alpha=.14f)){Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)){Text(message.body,fontSize=13.sp,lineHeight=18.sp,color=Color.White,maxLines=if(historyExpanded)12 else 4,overflow=TextOverflow.Ellipsis);Text(time(message.createdAt),Modifier.align(Alignment.End).padding(top=3.dp),fontSize=8.sp,color=Color.White.copy(alpha=.52f))}};if(mine){Spacer(Modifier.width(6.dp));UserAvatar(28)}}}}
          if(chat!=null&&isChatSending(chat.id))Row(Modifier.padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){if(card!=null)RoleAvatar(card,28);Surface(Modifier.padding(start=6.dp),shape=RoundedCornerShape(16.dp),color=Color.White.copy(alpha=.14f)){Row(Modifier.padding(horizontal=11.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(13.dp),strokeWidth=2.dp,color=Color.White);Text("${card?.name?:"角色"} 正在回复",Modifier.padding(start=7.dp),fontSize=11.sp,color=Color.White.copy(alpha=.78f))}}}
        }}}
        if(card!=null)item{Surface(modifier=Modifier.fillMaxWidth().imePadding(),shape=RoundedCornerShape(22.dp),color=Color.White.copy(alpha=.13f),border=BorderStroke(1.dp,Color.White.copy(alpha=.18f)),shadowElevation=0.dp){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.Bottom){Box(Modifier.weight(1f).heightIn(min=42.dp,max=100.dp).padding(horizontal=10.dp,vertical=8.dp),contentAlignment=Alignment.CenterStart){BasicTextField(value=chatInput,onValueChange={chatInput=it.take(2000)},modifier=Modifier.fillMaxWidth(),maxLines=4,textStyle=LocalTextStyle.current.copy(color=Color.White,fontSize=14.sp,lineHeight=20.sp),cursorBrush=SolidColor(Color.White),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={sendTogetherMessage()}),decorationBox={inner->if(chatInput.isBlank())Text("在这里和角色聊天…",fontSize=14.sp,color=Color.White.copy(alpha=.58f));inner()})};FilledIconButton(onClick={sendTogetherMessage()},enabled=chatInput.isNotBlank(),modifier=Modifier.size(42.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFE43D3D),contentColor=Color.White)){Icon(Icons.Outlined.ArrowUpward,"发送")}}}}
        item{Text("进度每 0.5 秒从后台播放服务同步；页面切换、锁屏和普通 LLM 回复不会结束当前歌曲。版权、会员和地区限制仍由网易云决定。",Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=12.dp),fontSize=10.sp,lineHeight=15.sp,color=Color.White.copy(alpha=.58f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
      }
      }
    }
  }

  @Composable private fun TogetherLyricsPanel(title:String,lyrics:List<LyricLine>,loading:Boolean,error:String,position:Long){
    val lyricListState=rememberLazyListState();val activeIndex=remember(lyrics,position){lyrics.indexOfLast{it.timeMs<=position}.coerceAtLeast(0)}
     LaunchedEffect(activeIndex,lyrics.size){
       if(lyrics.isNotEmpty())runCatching{lyricListState.animateScrollToItem(activeIndex,scrollOffset=-141)}
    }
    Box(Modifier.fillMaxWidth().height(330.dp),contentAlignment=Alignment.Center){
       when{loading->Column(horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator(Modifier.size(28.dp),strokeWidth=2.dp,color=Color.White);Text("正在加载歌词",Modifier.padding(top=10.dp),fontSize=12.sp,color=Color.White.copy(alpha=.68f))};lyrics.isEmpty()->Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.Lyrics,null,Modifier.size(42.dp),tint=Color.White.copy(alpha=.62f));Text(error.ifBlank{"这首歌暂无滚动歌词"},Modifier.padding(top=10.dp),fontSize=12.sp,color=Color.White.copy(alpha=.68f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)};else->LazyColumn(state=lyricListState,modifier=Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(9.dp)){itemsIndexed(lyrics,key={index,line->"${line.timeMs}-$index"}){index,line->val active=index==activeIndex;Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal=12.dp),contentAlignment=Alignment.Center){Text(line.text,fontSize=if(active)18.sp else 14.sp,lineHeight=if(active)27.sp else 22.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal,color=Color.White.copy(alpha=if(active)1f else .48f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis)}}}
      }
      if(title.isNotBlank())Text(title,Modifier.align(Alignment.TopCenter).padding(top=8.dp),fontSize=11.sp,color=Color.White.copy(alpha=.48f),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
  }

  private fun formatMusicTime(value:Long):String=if(value<=0)"0:00" else String.format(Locale.ROOT,"%d:%02d",value/60000,(value/1000)%60)
  @Composable private fun RemoteArtwork(url:String,modifier:Modifier=Modifier.size(54.dp)){val bitmap by produceState<android.graphics.Bitmap?>(null,url){if(url.startsWith("http",true))value=withContext(Dispatchers.IO){runCatching{val c=URL(url).openConnection() as HttpURLConnection;c.connectTimeout=10_000;c.readTimeout=15_000;c.inputStream.use{BitmapFactory.decodeStream(it)}.also{c.disconnect()}}.getOrNull()}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"专辑封面",modifier,contentScale=ContentScale.Crop)else Box(modifier.background(Color(0xFF3A2730)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.MusicNote,null,tint=Color.White.copy(alpha=.72f),modifier=Modifier.size(22.dp))}}

  @Composable private fun RelationshipMetric(label:String,value:Int,icon:androidx.compose.ui.graphics.vector.ImageVector,color:Color,modifier:Modifier=Modifier){Surface(modifier=modifier,shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,Modifier.size(17.dp),tint=color);Text(label,Modifier.padding(start=6.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("$value",Modifier.padding(top=5.dp),fontSize=21.sp,fontWeight=FontWeight.Bold);LinearProgressIndicator(progress={value.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().padding(top=5.dp).height(5.dp).clip(CircleShape),color=color,trackColor=color.copy(alpha=.13f))}}}
  @Composable private fun InterestPlanet(values:List<String>){BoxWithConstraints(Modifier.fillMaxWidth().height(220.dp)){val colors=listOf(Color(0xFF77B8A4),Color(0xFFE28191),Color(0xFFF2B55E),Color(0xFF7895D1),Color(0xFFA77CC2),Color(0xFF65AFC2),Color(0xFFDD846D),Color(0xFF7EAF72));values.take(8).forEachIndexed{index,label->val sizes=listOf(88,68,60,74,54,64,50,58);val positions=listOf(.34f to .42f,.70f to .25f,.12f to .18f,.72f to .69f,.15f to .70f,.50f to .08f,.45f to .78f,.88f to .46f);val size=sizes[index%sizes.size].dp;val position=positions[index%positions.size];Surface(shape=CircleShape,color=colors[index%colors.size].copy(alpha=.92f),shadowElevation=5.dp,modifier=Modifier.size(size).offset(x=(maxWidth*position.first-size/2),y=(220.dp*position.second-size/2))){Box(contentAlignment=Alignment.Center){Text(label,color=Color.White,fontSize=if(sizes[index%sizes.size]>65)13.sp else 11.sp,fontWeight=FontWeight.SemiBold,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(6.dp))}}}}}
  @Composable private fun NeteaseSongRow(song:NeteaseSong,playing:Boolean,loading:Boolean,onPlay:()->Unit,onTogether:()->Unit,onAddToQueue:(()->Unit)?=null){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical=7.dp,horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick=onPlay),contentAlignment=Alignment.Center){RemoteArtwork(song.coverUrl,Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)));Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.28f)),contentAlignment=Alignment.Center){if(loading)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Color.White)else Icon(if(playing)Icons.Filled.Pause else Icons.Outlined.PlayArrow,null,tint=Color.White)}};Column(Modifier.weight(1f).padding(horizontal=10.dp).clickable(onClick=onPlay)){Text(song.title,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${song.artist}${song.album.takeIf{it.isNotBlank()}?.let{" · $it"}.orEmpty()}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};Text(if(song.durationMs>0)formatMusicTime(song.durationMs) else "",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(onAddToQueue!=null)IconButton(onClick=onAddToQueue,modifier=Modifier.size(38.dp)){Icon(Icons.Outlined.QueueMusic,"加入播放列表",Modifier.size(19.dp),tint=MaterialTheme.colorScheme.primary)};IconButton(onClick=onTogether,modifier=Modifier.size(38.dp)){Icon(Icons.Outlined.Headphones,"一起听",Modifier.size(19.dp),tint=Color(0xFFE43D3D))}}}

  @Composable
  private fun LocalModelsPage(){
    LaunchedEffect(Unit){while(isActive){delay(1000);uiRevision++}}
    var mirror by remember{mutableStateOf(prefs.getString("local_model_download_mirror","").orEmpty())}
    var importKind by remember{mutableStateOf("chat")}
    var importing by remember{mutableStateOf(false)}
    var localContext by remember{mutableFloatStateOf(prefs.getInt("local_llm_context",4096).toFloat())}
    var localThreads by remember{mutableFloatStateOf(prefs.getInt("local_llm_threads",minOf(4,Runtime.getRuntime().availableProcessors().coerceAtLeast(1))).toFloat())}
    var localMaxTokens by remember{mutableFloatStateOf(prefs.getInt("local_llm_max_tokens",512).toFloat())}
    fun useInApp(spec:LocalModelRepository.ModelSpec){
      if(spec.kind!="chat"||!spec.fileName.endsWith(".gguf",true)){toast("App 内聊天推理仅支持 GGUF 模型");return}
      val value=ProviderConfig("local-in-app-${spec.id}","本地 · ${spec.name}","local://in-app",spec.fileName)
      val updated=providers().filterNot{it.id==value.id}+value
      saveCustomProviders(updated);runCatching{secure.putNamed("provider_key_${value.id}","local-in-app")};activateProvider(value)
      toast("已启用 App 内推理：${spec.name}")
    }
    fun useLocalImage(spec:LocalModelRepository.ModelSpec){
      if(spec.kind!="image"||!(spec.fileName.endsWith(".gguf",true)||spec.fileName.endsWith(".safetensors",true)||spec.fileName.endsWith(".ckpt",true))){toast("App 内生图仅支持 GGUF、SafeTensors 和 CKPT 模型");return}
      val value=ImageProviderConfig("local-image-${spec.id}","本地 · ${spec.name}","local","local://in-app",spec.fileName,"384x384","lowres, bad anatomy, blurry",15,7)
      val updated=imageProviders().filterNot{it.id==value.id}+value
      saveImageProviders(updated);activateImageProvider(value);prefs.edit().putBoolean("image_generation_enabled",true).apply()
      toast("已启用 App 内生图：${spec.name}")
    }
    fun deleteLocalModel(spec:LocalModelRepository.ModelSpec){
      LocalLlamaEngine.release();LocalImageEngine.release();LocalModelRepository.delete(this@ComposeActivity,spec)
      val chatValues=providers();val removedChat=chatValues.filter{it.endpoint.startsWith("local://in-app",true)&&it.modelName==spec.fileName};val remainingChat=chatValues.filterNot{it in removedChat}
      if(removedChat.isNotEmpty()){
        saveCustomProviders(remainingChat);removedChat.forEach{runCatching{secure.putNamed("provider_key_${it.id}","")}}
        if(removedChat.any{it.id==prefs.getString("active_provider_id","")}){prefs.edit().remove("active_provider_id").remove("provider").remove("endpoint").remove("model").apply();runCatching{secure.put("")}}
      }
      val imageValues=imageProviders();val removedImages=imageValues.filter{it.protocol=="local"&&it.model==spec.fileName};val remainingImages=imageValues.filterNot{it in removedImages}
      if(removedImages.isNotEmpty()){
        saveImageProviders(remainingImages);removedImages.forEach{runCatching{secure.putNamed("image_provider_key_${it.id}","")}}
        if(removedImages.any{it.id==prefs.getString("active_image_provider_id","")}){
          remainingImages.firstOrNull()?.let(::activateImageProvider)?:prefs.edit().remove("active_image_provider_id").remove("image_generation_provider_name").remove("image_generation_protocol").remove("image_generation_endpoint").remove("image_generation_model").putBoolean("image_generation_enabled",false).apply()
        }
      }
      uiRevision++;toast(if(spec.imported)"导入模型已删除" else "本地模型已删除")
    }
    val running=LocalModelDownloadService.running(this@ComposeActivity)
    val modelImporter=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
      if(uri==null){importing=false;return@rememberLauncherForActivityResult}
      val kind=importKind
      try{
        pool.execute{
          runCatching{LocalModelRepository.importModel(this@ComposeActivity,uri,kind)}
            .onSuccess{spec->main.post{importing=false;uiRevision++;toast("已导入 ${spec.name}")}}
            .onFailure{error->main.post{importing=false;toast("模型导入失败：${error.message?:"无法读取文件"}")}}
        }
      }catch(error:Throwable){importing=false;toast("模型导入失败：${error.message?:"无法开始导入"}")}
    }
    fun launchImport(kind:String,types:Array<String>){
      if(importing||running)return
      importKind=kind;importing=true
      runCatching{modelImporter.launch(types)}.onFailure{importing=false;toast("无法打开文件选择器")}
    }
    val activeId=prefs.getString("local_model_download_id","").orEmpty()
    val progress=prefs.getInt("local_model_download_progress",0)
    val progressLabel=prefs.getString("local_model_download_label","").orEmpty()
    val error=prefs.getString("local_model_download_error","").orEmpty()
    val localModels=LocalModelRepository.models(this@ComposeActivity)
    DetailScaffold("本地模型",Page.API){
      item{InfoBanner(Icons.Outlined.PhoneAndroid,"模型在 App 内直接推理","对话使用 llama.cpp，生图使用 stable-diffusion.cpp；均不需要 API、网络或外部服务。要求 Android 8.0 以上设备。")}
      item{SectionTitle("本地模型能力")}
      item{InfoBanner(Icons.Outlined.Chat,"本地对话","GGUF 对话模型可在设备上离线完成角色回复、身份设定和上下文对话；小模型更省内存，大模型通常有更好的理解与连贯性。")}
      item{InfoBanner(Icons.Outlined.Image,"本地生图","SD1.5 系列模型适合不同画风：Anything V5 偏二次元，QteaMix 偏Q版，Absolute Reality 与 ChilloutMix 偏写实人像。支持正向提示词、负向提示词、尺寸、步数和随机种子。")}
      item{SectionTitle("App 内推理参数")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("上下文 ${localContext.toInt()} Token",fontWeight=FontWeight.Medium);Slider(localContext,{localContext=it},valueRange=512f..8192f,steps=14);Text("CPU 线程 ${localThreads.toInt()}",fontWeight=FontWeight.Medium);Slider(localThreads,{localThreads=it},valueRange=1f..8f,steps=6);Text("单次最多生成 ${localMaxTokens.toInt()} Token",fontWeight=FontWeight.Medium);Slider(localMaxTokens,{localMaxTokens=it},valueRange=128f..1024f,steps=6)}}}
      item{Button(onClick={prefs.edit().putInt("local_llm_context",(localContext.toInt()/512*512).coerceIn(512,8192)).putInt("local_llm_threads",localThreads.toInt().coerceIn(1,8)).putInt("local_llm_max_tokens",(localMaxTokens.toInt()/128*128).coerceIn(128,1024)).apply();LocalLlamaEngine.release();toast("本地推理参数已保存")},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存推理参数",maxLines=1,softWrap=false)}}
      item{SectionTitle("导入本地文件")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedButton(onClick={launchImport("chat",arrayOf("application/octet-stream","application/x-gguf","*/*"))},Modifier.weight(1f).height(46.dp),enabled=!importing&&!running){Icon(Icons.Outlined.Chat,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("导入对话模型",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}
        OutlinedButton(onClick={launchImport("image",arrayOf("application/octet-stream","*/*"))},Modifier.weight(1f).height(46.dp),enabled=!importing&&!running){Icon(Icons.Outlined.Image,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("导入生图模型",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}
      }}
      if(importing)item{Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Text("正在复制模型到应用目录…",Modifier.padding(start=10.dp),maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}}
      item{InfoBanner(Icons.Outlined.Info,"支持的导入格式","对话模型支持 GGUF；App 内生图支持 Stable Diffusion 的 GGUF、SafeTensors 和 CKPT 文件。")}
      item{SectionTitle("下载加速")}
      item{StyledField(mirror,{mirror=it.take(500)},"镜像地址（留空使用官方；支持 {url} 占位符）")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){AssistChip(onClick={mirror="https://hf-mirror.com"},label={Text("hf-mirror",maxLines=1,softWrap=false)});AssistChip(onClick={mirror=""},label={Text("官方源",maxLines=1,softWrap=false)})}}
      item{Button(onClick={prefs.edit().putString("local_model_download_mirror",mirror.trim()).apply();toast("下载地址已保存")},modifier=Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Save,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text("保存下载地址",maxLines=1,softWrap=false)}}
      item{InfoBanner(Icons.Outlined.Speed,"镜像规则","填写镜像根地址会替换 Hugging Face 域名，例如 https://hf-mirror.com；也可以使用 {url} 作为完整原始地址占位符。")}
      item{SectionTitle("可用模型 ${localModels.size} 个")}
      items(localModels,key={it.id}){spec->
        val installed=LocalModelRepository.installed(this@ComposeActivity,spec)
        val bytes=LocalModelRepository.downloadedBytes(this@ComposeActivity,spec)
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
          Row(verticalAlignment=Alignment.Top){Icon(if(spec.kind=="chat")Icons.Outlined.Chat else Icons.Outlined.Image,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=11.dp)){Text(spec.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(spec.description,fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis);Text("${spec.sizeLabel} · ${if(installed)if(spec.imported)"已导入" else "已安装" else if(bytes>0)"已下载 ${LocalModelRepository.formatBytes(bytes)}" else "未下载"}",fontSize=12.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,color=if(installed)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}
          if(running&&activeId==spec.id){LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth().padding(top=12.dp));Text(progressLabel,Modifier.padding(top=6.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
          if(!running&&activeId==spec.id&&error.isNotBlank())Text(error,Modifier.padding(top=7.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.error,maxLines=3,overflow=TextOverflow.Ellipsis)
          Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            if(spec.imported||installed||bytes>0)OutlinedButton(onClick={askDanger(if(spec.imported)"删除导入模型" else "删除 ${spec.name}",if(spec.imported)"将删除导入文件、记录和引用它的本地供应商。" else "将删除本机模型文件和未完成的下载进度。",if(spec.imported)"删除导入" else "确认删除"){runCatching{deleteLocalModel(spec)}.onFailure{toast("删除失败：${it.message?:"无法删除模型"}")}}},Modifier.weight(1f).height(43.dp),enabled=!running&&!importing){Icon(Icons.Outlined.Delete,null);Spacer(Modifier.width(6.dp));Text(if(spec.imported)"删除导入" else "删除",maxLines=1,softWrap=false)}
            if(!spec.imported)Button(onClick={LocalModelDownloadService.start(this@ComposeActivity,spec.id);uiRevision++;toast("已开始后台下载")},Modifier.weight(1f).height(43.dp),enabled=!running&&!importing&&!installed){Icon(if(bytes>0)Icons.Outlined.Refresh else Icons.Outlined.Download,null);Spacer(Modifier.width(6.dp));Text(if(installed)"已安装" else if(bytes>0)"继续下载" else "下载",maxLines=1,softWrap=false)}
          }
          if(installed&&spec.kind=="chat"&&spec.fileName.endsWith(".gguf",true)){val using=activeProvider()?.let{it.endpoint.startsWith("local://in-app",true)&&it.modelName==spec.fileName}==true;Button(onClick={useInApp(spec)},enabled=!running&&!importing&&!using,modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(43.dp)){Icon(if(using)Icons.Filled.CheckCircle else Icons.Outlined.PlayArrow,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(if(using)"当前正在 App 内使用" else "在 App 内使用",maxLines=1,softWrap=false)}}
          if(installed&&spec.kind=="image"){val using=prefs.getString("image_generation_protocol","")=="local"&&prefs.getString("image_generation_model","")==spec.fileName;Button(onClick={useLocalImage(spec)},enabled=!running&&!importing&&!using,modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(43.dp)){Icon(if(using)Icons.Filled.CheckCircle else Icons.Outlined.PlayArrow,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(if(using)"当前正在 App 内使用" else "在 App 内使用",maxLines=1,softWrap=false)}}
        }}
      }
      item{InfoBanner(Icons.Outlined.Info,"设备与授权","大模型下载可能消耗较多流量和存储空间。模型由对应开源仓库提供，使用前请确认其许可证与你的用途相符。")}
    }
  }

  @Composable
  private fun ModelPage() {
    var modelName by remember { mutableStateOf(model()) }; var models by remember { mutableStateOf<List<String>>(emptyList()) }; var loading by remember { mutableStateOf(false) }
    DetailScaffold("模型", Page.API) {
      item { StyledField(modelName,{modelName=it},"模型名称") }
      item { Button(onClick={if(key().isEmpty()){toast("请先设置 API Key")}else{loading=true;pool.execute{try{val c=URL(modelsEndpoint()).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Authorization","Bearer "+key());val code=c.responseCode;val raw=read(if(code<300)c.inputStream else c.errorStream);if(code !in 200..299)throw java.io.IOException("HTTP $code：$raw");val data=JSONObject(raw).optJSONArray("data")?:JSONArray();val list=mutableListOf<String>();for(i in 0 until data.length()){data.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()}?.let(list::add)};list.sort();main.post{models=list;loading=false}}catch(e:Exception){main.post{loading=false;recordApiError(e);toast("获取失败：${friendlyError(e)}")}}}}},Modifier.fillMaxWidth().height(48.dp)){if(loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(if(loading)"正在获取…" else "自动获取模型列表")}}
      if(models.isNotEmpty()) item { SectionTitle("可用模型") }
      if(models.isNotEmpty())item{ModelListCard(models,modelName){modelName=it}}
      item { Button(onClick={if(modelName.isBlank())toast("请输入模型名称")else{prefs.edit().putString("model",modelName.trim()).apply();toast("模型已保存");composePage=Page.API}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存模型")}}
    }
  }

  @Composable
  private fun GenerationPage() {
    var temperature by remember { mutableStateOf(String.format(Locale.ROOT,"%.2f",java.lang.Double.longBitsToDouble(prefs.getLong("temp",java.lang.Double.doubleToLongBits(.85))))) };var context by remember{mutableStateOf(prefs.getInt("context",30).toString())};var maxContext by remember{mutableStateOf(prefs.getInt("model_max_context",32768).toString())};var reasoningEffort by remember{mutableStateOf(prefs.getString("reasoning_effort","auto")?:"auto")};var autoRetry by remember{mutableStateOf(prefs.getBoolean("generation_auto_retry",true))};var retryCount by remember{mutableFloatStateOf(prefs.getInt("generation_auto_retry_count",2).toFloat())}
    DetailScaffold("生成参数",Page.API){
      item{StyledField(temperature,{temperature=it},"温度（0 - 2）")};item{StyledField(context,{context=it.filter(Char::isDigit).take(4)},"上下文消息数（2 - 9999）")};item{StyledField(maxContext,{maxContext=it.filter(Char::isDigit).take(7)},"模型最大上下文 Token（1024 - 2000000）")}
      item{SectionTitle("思考程度")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("auto" to "自动","none" to "不思考","low" to "较少","medium" to "中等","high" to "较多").forEach{entry->FilterChip(selected=reasoningEffort==entry.first,onClick={reasoningEffort=entry.first},label={Text(entry.second,maxLines=1,softWrap=false)})}}}
      item{SectionTitle("错误恢复")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){ChatSettingSwitch("生成错误自动重试","网络、限流、超时或服务端异常时自动再次请求",autoRetry){autoRetry=it};AnimatedVisibility(autoRetry){Column{Text("最多重试 ${retryCount.toInt()} 次",Modifier.padding(top=8.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(retryCount,{retryCount=it},valueRange=1f..5f,steps=3);Text("认证、参数配置错误和手动停止不会重试。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
      item{Button(onClick={val t=temperature.toDoubleOrNull();val count=context.toIntOrNull();val maxTokens=maxContext.toIntOrNull();if(t==null||count==null||maxTokens==null)toast("请输入有效数字")else{prefs.edit().putLong("temp",java.lang.Double.doubleToLongBits(t.coerceIn(0.0,2.0))).putInt("context",count.coerceIn(2,9999)).putInt("model_max_context",maxTokens.coerceIn(1024,2_000_000)).putString("reasoning_effort",reasoningEffort).putBoolean("generation_auto_retry",autoRetry).putInt("generation_auto_retry_count",retryCount.toInt().coerceIn(1,5)).apply();toast("生成参数已保存");composePage=Page.API}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存生成参数")}}
    }
  }

  @Composable
  private fun AuxiliaryModelsPage(){
    var visualEnabled by remember{mutableStateOf(prefs.getBoolean("visual_aux_enabled",false))}
    var visualEndpoint by remember{mutableStateOf(prefs.getString("visual_aux_endpoint","").orEmpty())}
    var visualModel by remember{mutableStateOf(prefs.getString("visual_aux_model","").orEmpty())}
    var visualKey by remember{mutableStateOf(secure.getNamed("visual_aux_key"))}
    var audioEnabled by remember{mutableStateOf(prefs.getBoolean("audio_aux_enabled",false))}
    var audioEndpoint by remember{mutableStateOf(prefs.getString("audio_aux_endpoint","").orEmpty())}
    var audioModel by remember{mutableStateOf(prefs.getString("audio_aux_model","whisper-1")?:"whisper-1")}
    var audioKey by remember{mutableStateOf(secure.getNamed("audio_aux_key"))}
    fun saveAuxiliary(){
      if(visualEnabled&&visualModel.trim().isBlank()){toast("请填写视觉辅助模型名称");return}
      if(audioEnabled&&audioModel.trim().isBlank()){toast("请填写听觉辅助模型名称");return}
      runCatching{
        secure.putNamed("visual_aux_key",visualKey.trim());secure.putNamed("audio_aux_key",audioKey.trim())
        prefs.edit().putBoolean("visual_aux_enabled",visualEnabled).putString("visual_aux_endpoint",visualEndpoint.trim().trimEnd('/').removeSuffix("/chat/completions")).putString("visual_aux_model",visualModel.trim()).putBoolean("audio_aux_enabled",audioEnabled).putString("audio_aux_endpoint",audioEndpoint.trim()).putString("audio_aux_model",audioModel.trim().ifBlank{"whisper-1"}).apply()
      }.onSuccess{toast("辅助模型设置已保存");composePage=Page.API}.onFailure{toast("辅助模型密钥保存失败")}
    }
    DetailScaffold("视觉与听觉辅助模型",Page.API){
      item{InfoBanner(Icons.Outlined.Security,"独立且可选","辅助模型的 Key 使用 Android Keystore 加密；对应 Key 留空时跟随当前模型供应商。")}
      item{SectionTitle("视觉辅助")}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Visibility,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text("图片理解辅助",fontWeight=FontWeight.Medium);Text("先描述图片，再把描述交给对话模型",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(visualEnabled,{visualEnabled=it})}}}
      if(visualEnabled){item{StyledField(visualEndpoint,{visualEndpoint=it},"API 基础地址（留空跟随当前供应商）")};item{StyledField(visualModel,{visualModel=it},"视觉模型")};item{OutlinedTextField(visualKey,{visualKey=it},Modifier.fillMaxWidth(),label={Text("视觉模型 API Key（可留空）")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}}
      item{SectionTitle("听觉辅助")}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Hearing,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text("语音理解辅助",fontWeight=FontWeight.Medium);Text("聊天录音优先交给独立转写模型",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(audioEnabled,{audioEnabled=it})}}}
      if(audioEnabled){item{StyledField(audioEndpoint,{audioEndpoint=it},"转写接口（留空跟随当前供应商）")};item{StyledField(audioModel,{audioModel=it},"听觉模型")};item{OutlinedTextField(audioKey,{audioKey=it},Modifier.fillMaxWidth(),label={Text("听觉模型 API Key（可留空）")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}}
      item{Button(onClick={saveAuxiliary()},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存辅助模型设置")}}
    }
  }

  @Composable
  private fun McpToolsPage(){
    var enabled by remember{mutableStateOf(prefs.getBoolean("character_tools_enabled",true))}
    var revision by remember{mutableIntStateOf(0)}
    var serverName by rememberSaveable{mutableStateOf("")};var endpoint by rememberSaveable{mutableStateOf("")};var token by remember{mutableStateOf("")};var githubRequiredEndpoint by rememberSaveable{mutableStateOf("")}
    var testingId by remember{mutableStateOf("")};var testStatus by remember{mutableStateOf("")}
    var editingMcpId by rememberSaveable{mutableStateOf("")}
    var musicAgent by remember{mutableStateOf(prefs.getBoolean("agent_music_enabled",true))}
    var timeAgent by remember{mutableStateOf(prefs.getBoolean("agent_time_enabled",true))}
    var travelAgent by remember{mutableStateOf(prefs.getBoolean("agent_travel_enabled",true))}
    var githubUrl by rememberSaveable{mutableStateOf("")};var githubBusy by remember{mutableStateOf(false)};var githubStatus by remember{mutableStateOf("")}
    var githubCandidates by remember{mutableStateOf<List<McpGitHubImporter.Candidate>>(emptyList())}
    val servers=remember(revision,uiRevision){McpToolClient.servers(this@ComposeActivity)}
    fun setServerEnabled(id:String,value:Boolean){val updated=java.util.ArrayList(servers.map{server->if(server.id==id)McpToolClient.Server(server.id,server.name,server.endpoint,value)else server});McpToolClient.saveServers(this@ComposeActivity,updated);revision++;uiRevision++}
    DetailScaffold("角色工具与 MCP",Page.API){
      item{InfoBanner(Icons.Outlined.Extension,"工具调用","角色可使用关系、日记、定位等本地工具，也可连接支持 Streamable HTTP 的 HTTPS MCP 服务。")}
      item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色调用工具",fontWeight=FontWeight.Medium);Text("关闭后不会读取或调用远程 MCP 服务",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(enabled,{enabled=it;prefs.edit().putBoolean("character_tools_enabled",it).apply();uiRevision++})}}}
      item{SectionTitle("默认 Agent 与内置工具")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("网易云音乐助手","按歌名和歌手查询真实歌曲，配合分享音乐与一起听歌；不是官方远程 MCP",musicAgent){musicAgent=it;prefs.edit().putBoolean("agent_music_enabled",it).apply();uiRevision++}
        ChatSettingSwitch("日期时间工具","按需读取当前日期、星期和时区，无需联网",timeAgent){timeAgent=it;prefs.edit().putBoolean("agent_time_enabled",it).apply();uiRevision++}
        ChatSettingSwitch("出行助手","先确认地点与时间，再使用已配置的天气、地图与打车工具",travelAgent){travelAgent=it;prefs.edit().putBoolean("agent_travel_enabled",it).apply();uiRevision++}
      }}}
      item{SettingButton(Icons.Outlined.MusicNote,"网易云登录与一起听歌","复用已有音乐账号和播放能力"){composePage=Page.MUSIC}}
      item{SettingButton(Icons.Outlined.TravelExplore,"联网搜索与天气","搜索默认可用；天气需在现实感知中配置"){webSearchBackPage=Page.MCP_TOOLS;composePage=Page.WEB_SEARCH}}
      item{SectionTitle("推荐 MCP 服务")}
      item{InfoBanner(Icons.Outlined.Security,"远程服务权限","仅启用的服务会提供给角色；工具参数会发送到对应 MCP 服务，密钥加密保存在本机。涉及订单、费用或隐私时，请先在角色设定中明确授权范围。")}
      item{SettingButton(Icons.Outlined.LocalTaxi,"滴滴官方 MCP","填写滴滴开发者平台申请的 MCP Key，支持真实出行工具"){editingMcpId="";serverName="滴滴出行";endpoint="https://mcp.didichuxing.com/mcp-servers";token="";githubRequiredEndpoint=endpoint;toast("已填入滴滴官方端点，请在下方填写 MCP Key")}}
      item{TextButton(onClick={openWebLink("https://mcp.didichuxing.com/api","无法打开滴滴接入文档")}){Text("滴滴官方接入说明 / 申请密钥")}}
      item{SettingButton(Icons.Outlined.QueueMusic,"网易云远程 MCP（自建）","已有内置音乐助手；更多能力可接入你信任的 HTTPS MCP 服务"){editingMcpId="";serverName="网易云 MCP";endpoint="";token="";githubRequiredEndpoint="";toast("请填写实际部署的 HTTPS 端点，不支持直接运行本地 stdio 项目")}}
      item{SectionTitle("从 GitHub 项目导入")}
      item{InfoBanner(Icons.Outlined.Security,"只读取远程配置","仅发现项目声明的 HTTPS MCP 端点，不下载或运行 GitHub 项目代码。导入前可检查名称和地址。")}
      item{StyledField(githubUrl,{githubUrl=it.take(500)},"GitHub 项目或配置文件链接")}
      item{Button(onClick={if(githubUrl.isBlank())toast("请粘贴 GitHub 项目链接")else{githubBusy=true;githubStatus="";githubCandidates=emptyList();pool.execute{runCatching{McpGitHubImporter.discover(githubUrl.trim())}.onSuccess{found->main.post{githubBusy=false;githubCandidates=found;githubStatus="发现 ${found.size} 个可导入服务"}}.onFailure{error->DiagnosticLog.record(this@ComposeActivity,"mcp_github","error","${error.javaClass.simpleName}: ${error.message.orEmpty()}");main.post{githubBusy=false;githubStatus="";toast("GitHub 导入失败：${error.message?:friendlyError(error)}")}}}}},enabled=!githubBusy,modifier=Modifier.fillMaxWidth().height(48.dp)){if(githubBusy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.CloudDownload,null);Spacer(Modifier.width(7.dp));Text(if(githubBusy)"正在读取 GitHub 配置" else "读取项目 MCP 配置",maxLines=1,softWrap=false)}}
      if(githubStatus.isNotBlank())item{InfoBanner(Icons.Outlined.CheckCircle,"已读取配置",githubStatus)}
      if(githubCandidates.isNotEmpty())items(githubCandidates,key={it.endpoint}){candidate->Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(candidate.name,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis);Text(candidate.endpoint,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis);if(candidate.needsToken)Text("添加前需要填写 Bearer Token",fontSize=11.sp,color=MaterialTheme.colorScheme.tertiary)};OutlinedButton(onClick={serverName=candidate.name;endpoint=candidate.endpoint;token="";githubRequiredEndpoint=if(candidate.needsToken)candidate.endpoint else "";toast("配置已填入，请确认后添加")},contentPadding=PaddingValues(horizontal=12.dp)){Text("使用")}}}}
      item{SectionTitle("MCP 服务")}
      if(servers.isEmpty())item{EmptyState(Icons.Outlined.ExtensionOff,"还没有 MCP 服务","可在下方添加 HTTPS Streamable HTTP 端点")}
      else items(servers,key={it.id}){server->
        Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){
          Column(Modifier.fillMaxWidth().padding(14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
              Icon(Icons.Outlined.Dns,null,tint=MaterialTheme.colorScheme.secondary)
              Column(Modifier.weight(1f).padding(horizontal=10.dp)){
                Text(server.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(server.endpoint,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
              }
              Switch(server.enabled,{setServerEnabled(server.id,it)})
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
              TextButton(onClick={editingMcpId=server.id;serverName=server.name;endpoint=server.endpoint;token=secure.getNamed(McpToolClient.tokenKey(server.id));githubRequiredEndpoint=if(server.endpoint.startsWith("https://mcp.didichuxing.com/"))server.endpoint else "";toast("已载入下方编辑表单")}){Text("编辑")}
              TextButton(
                onClick={
                  testingId=server.id
                  testStatus=""
                  pool.execute{
                    runCatching{McpToolClient.test(this@ComposeActivity,server,secure.getNamed(McpToolClient.tokenKey(server.id)))}
                      .onSuccess{tools->main.post{testingId="";testStatus="${server.name}：已读取 ${tools.length()} 个工具"}}
                      .onFailure{error->DiagnosticLog.record(this@ComposeActivity,"mcp","error","test ${server.id} | ${diagnosticSummary(error)}");main.post{testingId="";testStatus="${server.name}：${friendlyError(error)}"}}
                  }
                },
                enabled=testingId.isBlank()
              ){
                if(testingId==server.id)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp)
                else Icon(Icons.Outlined.NetworkCheck,null,Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp));Text("测试")
              }
              TextButton(onClick={McpToolClient.deleteServer(this@ComposeActivity,server.id);revision++;uiRevision++}){
                Icon(Icons.Outlined.Delete,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(5.dp));Text("删除",color=MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
      if(testStatus.isNotBlank())item{InfoBanner(if(testStatus.contains("已读取"))Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,"连接结果",testStatus)}
      item{SectionTitle(if(editingMcpId.isBlank())"添加服务" else "编辑服务")}
      item{StyledField(serverName,{serverName=it.take(80)},"服务名称")};item{StyledField(endpoint,{endpoint=it.take(500)},"HTTPS MCP 端点（不要在地址中填写密钥）")};item{OutlinedTextField(token,{token=it},Modifier.fillMaxWidth(),label={Text(if(endpoint.startsWith("https://mcp.didichuxing.com/"))"滴滴 MCP Key（必填）" else if(githubRequiredEndpoint.isNotBlank()&&endpoint.trim()==githubRequiredEndpoint)"Bearer Token（必填）" else "Bearer Token（可选）")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}
      item{Button(onClick={
        if(serverName.trim().isBlank()){toast("请填写服务名称");return@Button}
        val cleanEndpoint=runCatching{McpToolClient.validatedEndpoint(endpoint.trim())}.getOrElse{toast("请填写有效的 HTTPS MCP 端点");return@Button}
        if(githubRequiredEndpoint.isNotBlank()&&cleanEndpoint==githubRequiredEndpoint&&token.trim().isBlank()){toast("这个服务需要 Bearer Token");return@Button}
        if(cleanEndpoint.startsWith("https://mcp.didichuxing.com/")&&android.net.Uri.parse(cleanEndpoint).query!=null){toast("请移除地址中的 key，将 MCP Key 填入独立密钥框");return@Button}
        if(servers.any{it.endpoint==cleanEndpoint&&it.id!=editingMcpId}){toast("这个 MCP 服务已经存在");return@Button}
        val id=editingMcpId.ifBlank{UUID.randomUUID().toString()};val previous=servers.firstOrNull{it.id==id};val updated=java.util.ArrayList(servers.filter{it.id!=id});updated.add(McpToolClient.Server(id,serverName.trim(),cleanEndpoint,previous?.enabled?:true))
        runCatching{secure.putNamed(McpToolClient.tokenKey(id),token.trim());McpToolClient.saveServers(this@ComposeActivity,updated)}.onSuccess{editingMcpId="";serverName="";endpoint="";token="";githubRequiredEndpoint="";revision++;uiRevision++;toast("MCP 服务已保存")}.onFailure{toast("MCP 服务保存失败")}
      },Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text(if(editingMcpId.isBlank())"添加 MCP 服务" else "保存 MCP 服务")}}
      if(editingMcpId.isNotBlank())item{TextButton(onClick={editingMcpId="";serverName="";endpoint="";token="";githubRequiredEndpoint=""}){Text("取消编辑")}}
    }
  }

  @Composable
  private fun WebSearchPage(){
    var enabled by remember{mutableStateOf(prefs.getBoolean("web_search_enabled",true))}
    var auto by remember{mutableStateOf(prefs.getBoolean("web_search_auto",false))}
    var provider by remember{mutableStateOf(prefs.getString("web_search_provider","bing")?:"bing")}
    var endpoint by remember{mutableStateOf(prefs.getString("web_search_endpoint","").orEmpty().ifBlank{if(provider=="serper")"https://google.serper.dev/search" else if(provider=="tavily")"https://api.tavily.com/search" else ""})}
    var searchKey by remember{mutableStateOf(secure.getNamed("web_search_api_key"))}
    var maxResults by remember{mutableFloatStateOf(prefs.getInt("web_search_max_results",5).toFloat().coerceIn(1f,10f))}
    var testing by remember{mutableStateOf(false)}
    fun select(value:String){provider=value;endpoint=when(value){"tavily"->"https://api.tavily.com/search";"serper"->"https://google.serper.dev/search";else->""}}
    fun persist(showToast:Boolean=true):Boolean{
      if(enabled&&provider!="bing"&&(endpoint.isBlank()||searchKey.isBlank())){toast("请填写搜索 API 地址和 Key");return false}
      prefs.edit().putBoolean("web_search_enabled",enabled).putBoolean("web_search_auto",auto).putString("web_search_provider",provider).putString("web_search_endpoint",endpoint.trim()).putInt("web_search_max_results",maxResults.toInt()).apply()
      runCatching{secure.putNamed("web_search_api_key",searchKey.trim())}.onFailure{toast("搜索 Key 保存失败");return false}
      if(showToast)toast("联网搜索设置已保存");uiRevision++;return true
    }
    DetailScaffold("联网搜索",webSearchBackPage){
      item{InfoBanner(Icons.Outlined.TravelExplore,"内置必应搜索","用户可以手动联网，角色也能在需要最新资料或不确定答案时自行搜索，再根据真实结果继续回复。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){ChatSettingSwitch("启用联网搜索","允许用户和角色使用必应搜索",enabled){enabled=it};ChatSettingSwitch("自动判断实时问题","用户提问命中实时信息时自动搜索",auto){auto=it}}}}
      item{SectionTitle("搜索服务")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("bing" to "必应","tavily" to "Tavily","serper" to "Serper").forEach{value->FilterChip(selected=provider==value.first,onClick={select(value.first)},label={Text(value.second,maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}}}
      if(provider!="bing"){item{StyledField(endpoint,{endpoint=it},"搜索 API 地址")};item{OutlinedTextField(searchKey,{searchKey=it},Modifier.fillMaxWidth(),label={Text("搜索 API Key")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}}else item{InfoBanner(Icons.Outlined.CheckCircle,"无需 API Key","必应搜索已内置，启用后即可直接使用。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("每次最多 ${maxResults.toInt()} 条结果",fontWeight=FontWeight.Medium);Slider(maxResults,{maxResults=it},valueRange=1f..10f,steps=8)}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={if(!persist(false))return@OutlinedButton;testing=true;pool.execute{runCatching{searchWeb("今天有什么值得关注的科技新闻")}.onSuccess{main.post{testing=false;toast("搜索连接成功")}}.onFailure{error->main.post{testing=false;recordApiError(error);toast("搜索失败：${friendlyError(error)}")}}}},enabled=!testing&&(provider=="bing"||(endpoint.isNotBlank()&&searchKey.isNotBlank())),modifier=Modifier.weight(1f).height(48.dp)){if(testing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.NetworkCheck,null);Spacer(Modifier.width(6.dp));Text("测试搜索",maxLines=1)};Button(onClick={if(persist())composePage=webSearchBackPage},enabled=!testing,modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存",maxLines=1)}}}
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
    fun availableRuntime(value:String?)=if(value=="npu"&&LocalImageEngine.npuSupported(this@ComposeActivity))"npu" else "cpu"
    var enabled by remember{mutableStateOf(prefs.getBoolean("image_generation_enabled",false))}
    var confirmEach by remember{mutableStateOf(prefs.getBoolean("image_generation_confirm_each",true))}
    var providers by remember(uiRevision){mutableStateOf(imageProviders())}
    var editingId by remember{mutableStateOf<String?>(activeImageProvider()?.id)}
    var name by remember{mutableStateOf(activeImageProvider()?.name.orEmpty())}
    var protocol by remember{mutableStateOf(activeImageProvider()?.protocol?:"openai")}
    var endpoint by remember{mutableStateOf(activeImageProvider()?.endpoint?.ifBlank{baseEndpoint().trimEnd('/').removeSuffix("/chat/completions")+"/images/generations"}.orEmpty())}
    var model by remember{mutableStateOf(activeImageProvider()?.model?.ifBlank{"dall-e-3"}?:"dall-e-3")}
    var size by remember{mutableStateOf(activeImageProvider()?.size?:"1024x1024")}
    var negative by remember{mutableStateOf(activeImageProvider()?.negative?:"lowres, bad anatomy, blurry")}
    var steps by remember{mutableFloatStateOf((activeImageProvider()?.steps?:28).toFloat())}
    var scale by remember{mutableFloatStateOf((activeImageProvider()?.scale?:7).toFloat())}
    var runtime by remember{mutableStateOf(availableRuntime(activeImageProvider()?.runtime))}
    var seed by remember{mutableStateOf(prefs.getString("image_generation_local_seed","").orEmpty())}
    var imageKey by remember{mutableStateOf(activeImageProvider()?.let{secure.getNamed("image_provider_key_${it.id}").ifBlank{secure.getNamed("image_generation_key")}}?:secure.getNamed("image_generation_key"))}
    fun load(value:ImageProviderConfig?){editingId=value?.id;name=value?.name.orEmpty();protocol=value?.protocol?:"openai";endpoint=value?.endpoint?.ifBlank{baseEndpoint().trimEnd('/').removeSuffix("/chat/completions")+"/images/generations"}.orEmpty();model=value?.model?.ifBlank{"dall-e-3"}?:"dall-e-3";size=value?.size?:"1024x1024";negative=value?.negative?:"lowres, bad anatomy, blurry";steps=(value?.steps?:28).toFloat();scale=(value?.scale?:7).toFloat();runtime=availableRuntime(value?.runtime);imageKey=value?.let{secure.getNamed("image_provider_key_${it.id}").ifBlank{secure.getNamed("image_generation_key")}}?:""}
    fun clearEditor(){load(null);name="";endpoint="";model="dall-e-3";editingId=null}
    DetailScaffold("生图模型",imageGenerationBackPage){
      item{InfoBanner(Icons.Outlined.Image,"保存多个生图供应商","支持 OpenAI Images、NovelAI/NAI，以及直接使用已下载或导入模型的 App 内生图。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用生图功能",fontWeight=FontWeight.Medium);Text("每个对话仍需单独允许角色调用",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=enabled,onCheckedChange={enabled=it})}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("生成前逐次确认",fontWeight=FontWeight.Medium);Text("角色提出生图请求后，由你点击确认才会调用接口",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=confirmEach,onCheckedChange={confirmEach=it})}}}
      item{SectionTitle("已保存供应商 ${providers.size} 个")}
      if(providers.isEmpty())item{EmptyState(Icons.Outlined.Image,"还没有生图供应商","填写下面的配置并保存")}
      items(providers,key={it.id}){value->Surface(onClick={load(value);activateImageProvider(value)},shape=RoundedCornerShape(10.dp),color=if(value.id==editingId)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(value.id==editingId)Icons.Filled.CheckCircle else Icons.Outlined.Image,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(value.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${value.protocol} · ${value.model}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};IconButton(onClick={load(value)}){Icon(Icons.Outlined.Edit,"编辑生图供应商")};IconButton(onClick={val updated=providers.filterNot{it.id==value.id};saveImageProviders(updated);if(updated.isEmpty())prefs.edit().remove("image_generation_endpoint").remove("image_generation_model").remove("active_image_provider_id").apply();providers=updated;if(value.id==editingId)load(updated.firstOrNull());toast("生图供应商已删除")}){Icon(Icons.Outlined.Delete,"删除生图供应商",tint=MaterialTheme.colorScheme.error)}}}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={clearEditor()}){Icon(Icons.Outlined.Add,null,Modifier.size(17.dp));Spacer(Modifier.width(4.dp));Text("添加供应商")}}}
      item{SectionTitle(if(editingId==null)"新增供应商" else "编辑供应商")}
      item{StyledField(name,{name=it.take(80)},"供应商名称")}
      item{SectionTitle("接口协议")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=protocol=="openai",onClick={protocol="openai";endpoint=baseEndpoint().trimEnd('/').removeSuffix("/chat/completions")+"/images/generations";model="dall-e-3";size="1024x1024"},label={Text("OpenAI",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f));FilterChip(selected=protocol=="nai",onClick={protocol="nai";endpoint="https://image.novelai.net/ai/generate-image";model="nai-diffusion-3";size="832x1216"},label={Text("NovelAI",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f));FilterChip(selected=protocol=="local",onClick={protocol="local";endpoint="local://in-app";model=LocalModelRepository.findInstalled(this@ComposeActivity,model,"image")?.fileName.orEmpty();size="384x384";if(name.isBlank())name="App 内本地生图"},label={Text("本地",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}}
      if(protocol!="local")item{StyledField(endpoint,{endpoint=it},"Images 接口地址")}
      if(protocol!="local")item{StyledField(model,{model=it},"生图模型")}
      if(protocol=="local"){
        val installed=LocalModelRepository.models(this@ComposeActivity).filter{it.kind=="image"&&LocalModelRepository.installed(this@ComposeActivity,it)}
        item{InfoBanner(Icons.Outlined.PhoneAndroid,"App 内直接生成","模型在当前 App 进程中推理，不连接 127.0.0.1，也不需要 API Key。")}
        item{SectionTitle("运行设备")}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
          FilterChip(selected=runtime=="cpu",onClick={runtime="cpu"},label={Text("CPU",maxLines=1,softWrap=false)},leadingIcon={Icon(Icons.Outlined.Memory,null,Modifier.size(17.dp))},modifier=Modifier.weight(1f))
          FilterChip(selected=runtime=="npu",onClick={if(LocalImageEngine.npuSupported(this@ComposeActivity))runtime="npu" else{runtime="cpu";toast("当前设备或安装包不支持 NPU 生图")}},label={Text("NPU",maxLines=1,softWrap=false)},leadingIcon={Icon(Icons.Outlined.Speed,null,Modifier.size(17.dp))},modifier=Modifier.weight(1f))
        }}
        if(installed.isEmpty())item{SettingButton(Icons.Outlined.Download,"还没有可用生图模型","前往下载或导入本地生图模型"){imageGenerationBackPage=Page.IMAGE_GENERATION;composePage=Page.LOCAL_MODELS}}
        else item{Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(7.dp)){installed.forEach{spec->OutlinedButton(onClick={model=spec.fileName},Modifier.fillMaxWidth().height(44.dp)){Icon(if(model==spec.fileName)Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(spec.name,Modifier.weight(1f),maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}}}
      }
      if(protocol!="local")item{StyledField(imageKey,{imageKey=it},"专用 API Key（留空复用当前供应商）")}
      item{SectionTitle("图片尺寸")}
      item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){(when(protocol){"nai"->listOf("832x1216","1216x832","1024x1024","512x768");"local"->listOf("256x256","384x384","512x512");else->listOf("1024x1024","1024x1536","1536x1024","512x512")}).forEach{value->FilterChip(selected=size==value,onClick={size=value},label={Text(value,maxLines=1,softWrap=false)})}}}
      if(protocol in listOf("nai","local"))item{OutlinedTextField(negative,{negative=it.take(1000)},Modifier.fillMaxWidth().heightIn(min=96.dp),label={Text("默认负向提示词",maxLines=1,softWrap=false)},minLines=3,maxLines=6,shape=RoundedCornerShape(13.dp))}
      if(protocol in listOf("nai","local"))item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("生成步数 ${steps.toInt()}",fontWeight=FontWeight.Medium);Slider(steps,{steps=it},valueRange=5f..50f,steps=44);Text("CFG Scale ${String.format(Locale.ROOT,"%.1f",scale)}",fontWeight=FontWeight.Medium);Slider(scale,{scale=it},valueRange=1f..20f,steps=37)}}}
      if(protocol=="local"){
        item{SectionTitle("本地生成参数")}
        item{StyledField(seed,{seed=it.filter(Char::isDigit).take(19)},"随机种子（可选）")}
      }
      item{Button(onClick={if(name.trim().isBlank()||model.trim().isBlank()||(protocol!="local"&&endpoint.trim().isBlank()))toast(if(protocol=="local")"请填写名称并选择已安装模型" else "请填写供应商名称、接口和模型")else{val finalEndpoint=if(protocol=="local")"local://in-app" else endpoint.trim();val value=ImageProviderConfig(editingId?:UUID.randomUUID().toString(),name.trim(),protocol,finalEndpoint,model.trim(),size,negative.trim(),steps.toInt(),scale.toInt(),runtime);val updated=if(providers.any{it.id==value.id})providers.map{if(it.id==value.id)value else it}else providers+value;runCatching{saveImageProviders(updated);secure.putNamed("image_provider_key_${value.id}",if(protocol=="local")"" else imageKey.trim());prefs.edit().putBoolean("image_generation_enabled",enabled).putBoolean("image_generation_confirm_each",confirmEach).putInt("image_generation_local_steps",steps.toInt()).putLong("image_generation_local_cfg",java.lang.Double.doubleToLongBits(scale.toDouble())).putString("image_generation_local_seed",seed.trim()).apply();activateImageProvider(value)}.onSuccess{providers=updated;editingId=value.id;toast("生图供应商已保存");composePage=imageGenerationBackPage}.onFailure{toast("生图供应商保存失败")}}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存生图供应商",maxLines=1,softWrap=false)}}
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
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.GraphicEq,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(OfflineSpeechRepository.MODEL_LABEL,fontWeight=FontWeight.Medium);Text(if(offlineModelInstalled)"已安装 · ${String.format(Locale.getDefault(),"%.1f MB",OfflineSpeechRepository.installedBytes(this@ComposeActivity)/1048576.0)}" else "约 42 MB · 中文离线语音转文字",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(offlineModelInstalled)Icon(Icons.Filled.CheckCircle,null,tint=MaterialTheme.colorScheme.secondary)};if(sttDownloading){LinearProgressIndicator(progress={sttProgress/100f},Modifier.fillMaxWidth().padding(top=11.dp));Text(sttProgressText,Modifier.padding(top=4.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Column(Modifier.fillMaxWidth().padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={openWebLink(OfflineSpeechRepository.SOURCE_URL,"无法打开 GitHub 项目页面")},Modifier.fillMaxWidth(),contentPadding=PaddingValues(horizontal=8.dp)){Icon(Icons.Outlined.OpenInNew,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("GitHub 项目",maxLines=1)};Button(onClick={if(offlineModelInstalled)askDanger("删除离线语音模型","删除后可继续使用系统或在线语音识别。","确认删除"){cancelOfflineVoiceRecognition();OfflineSpeechRepository.delete(this@ComposeActivity);uiRevision++}else{sttDownloading=true;sttProgress=1;sttProgressText="准备下载";OfflineSpeechDownloadService.start(this@ComposeActivity)}},enabled=!sttDownloading,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(horizontal=8.dp)){Icon(if(offlineModelInstalled)Icons.Outlined.Delete else Icons.Outlined.Download,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(if(offlineModelInstalled)"删除模型" else if(sttDownloading)"后台下载中" else "下载模型",maxLines=1)}}}}
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
    var crashProtection by remember{mutableStateOf(CrashProtection.isEnabled(this@ComposeActivity))}
    var inputGlass by remember{mutableStateOf(prefs.getBoolean("experimental_input_glass",false))}
    var transparentTop by remember{mutableStateOf(prefs.getBoolean("experimental_top_bar_transparent",false))}
    var globalGlass by remember{mutableStateOf(prefs.getBoolean("experimental_glass_ui",false))}
    var glassOpacity by remember{mutableFloatStateOf(prefs.getInt("experimental_glass_opacity",78).toFloat())}
    val crashRecords=remember(uiRevision){CrashProtection.records(this@ComposeActivity)}
    fun persistVisuals(){prefs.edit().putBoolean("experimental_input_glass",inputGlass).putBoolean("experimental_top_bar_transparent",transparentTop).putBoolean("experimental_glass_ui",globalGlass).putInt("experimental_glass_opacity",glassOpacity.toInt()).apply();uiRevision++}
    DetailScaffold("实验性功能"){
      item{InfoBanner(Icons.Outlined.Science,"仍在打磨中的体验","实验功能可随时关闭，不会改变聊天记录、角色卡或模型配置。")}
      item{SectionTitle("稳定性保护")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("闪退保护","识别到后台功能异常时自动暂停该功能，并保留近期记录",crashProtection){enabled->crashProtection=enabled;CrashProtection.setEnabled(this@ComposeActivity,enabled);uiRevision++;toast(if(enabled)"闪退保护已开启" else "闪退保护已关闭")}
      }}}
      if(crashRecords.isEmpty())item{InfoBanner(Icons.Outlined.HealthAndSafety,"暂无闪退记录","保护功能默认开启；正常退出、系统清理后台和手动停止不会被记为闪退。")}
      else{
        item{SectionTitle("保护记录")}
        crashRecords.take(3).forEach{record->item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.SecurityUpdateWarning,null,tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(20.dp));Text(record.featureName,Modifier.weight(1f).padding(start=9.dp),fontWeight=FontWeight.SemiBold);Text(java.text.SimpleDateFormat("M月d日 HH:mm",Locale.CHINA).format(Date(record.timestamp)),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
          Text(record.reason,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)
          if(record.location.isNotBlank())Text("位置：${record.location}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
          Text("处理：${record.action}",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
        }}}}
        item{OutlinedButton(onClick={CrashProtection.clearRecords(this@ComposeActivity);uiRevision++;toast("闪退记录已清除")},Modifier.fillMaxWidth().height(44.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(7.dp));Text("清除 ${crashRecords.size} 条记录")}}
      }
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
      item{InfoBanner(Icons.Outlined.TipsAndUpdates,"显示说明","玻璃效果使用纯色半透明面板和细描边；较低不透明度可能降低文字可读性。")}
    }
  }

  @Composable
  private fun ChatSearchPage(chat:Chat){
    var query by rememberSaveable(chat.id){mutableStateOf("")}
    val results=remember(chat.messages.size,query){if(query.isBlank())emptyList() else chat.messages.filter{it.body.contains(query.trim(),ignoreCase=true)}}
    DetailScaffold("搜索对话",Page.CHAT){
      item{OutlinedTextField(query,{query=it.take(200)},Modifier.fillMaxWidth(),label={Text("关键词")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,shape=RoundedCornerShape(13.dp))}
      if(query.isBlank())item{EmptyState(Icons.Outlined.Search,"搜索当前对话","输入角色名、事件或任意消息片段")}
      else if(results.isEmpty())item{EmptyState(Icons.Outlined.SearchOff,"没有匹配消息","换一个关键词试试")}
      else items(results,key={it.id}){msg->Surface(onClick={pendingChatMessageId=msg.id;composePage=Page.CHAT},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Text(if(msg.role=="user")prefs.getString("user_name","我").orEmpty().ifBlank{"我"} else find(msg.speakerCardId)?.name?:"角色",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.secondary);Text(msg.body.ifBlank{if(msg.image.isNotBlank())"[图片]" else "[非文字消息]"},Modifier.padding(top=5.dp),maxLines=4,overflow=TextOverflow.Ellipsis,fontSize=14.sp,lineHeight=20.sp);Text(formatMessageTime(msg.createdAt),Modifier.padding(top=7.dp),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
    }
  }

  @Composable
  private fun DiaryPage(chat:Chat){
    val card=find(chat.activeCardId.ifBlank{chat.cardId})?:return
    var tab by rememberSaveable(chat.id){mutableIntStateOf(0)}
    var editorOpen by rememberSaveable(chat.id){mutableStateOf(false)}
    var diaryTitle by rememberSaveable(chat.id){mutableStateOf("")}
    var diaryContent by rememberSaveable(chat.id){mutableStateOf("")}
    var viewing by remember{mutableStateOf<AppDb.Diary?>(null)}
    val all=remember(uiRevision,card.id){db.diaries(card.id)}
    val visible=if(tab==0)all.filter{it.ownerType=="user"} else all.filter{it.ownerType=="character"&&it.ownerId==card.id&&it.sharedWith.split(',').contains("user")}

    fun deliver(body:String,type:String="text",meta:String=""):Boolean{
      if(activeProvider()==null&&!chat.userBlockedRole){toast("请先配置模型供应商");return false}
      val message=Msg("user",body);message.messageType=type;message.meta=meta;message.delivered=!chat.userBlockedRole
      chat.messages.add(message);chat.updated=System.currentTimeMillis();chat.proactiveSequence=0;save();uiRevision++
      if(chat.userBlockedRole){toast("内容已保存在本机，但角色无法收到");return true}
      val target=card
      when{
        chat.immersiveMode&&!chat.group->{scheduleImmersiveReply(chat.id,target.id)}
        isChatSending(chat.id)->{queueReply(chat,target);toast("已加入下一轮回复")}
        else->request(chat,target)
      }
      return true
    }

    fun share(entry:AppDb.Diary,exchange:Boolean){
      val meta=diaryMeta(entry).put("exchange",exchange).toString()
      val prompt=if(exchange)"我想和你交换这篇日记：《${entry.title}》\n${entry.content}" else "我把日记《${entry.title}》分享给你：\n${entry.content}"
      if(deliver(prompt,"diary_share",meta)){
        db.markDiaryShared(entry.id,card.id)
        composePage=Page.CHAT
      }
    }

    DetailScaffold("日记",Page.CHAT){
      item{TabRow(selectedTabIndex=tab,containerColor=Color.Transparent,divider={}){Tab(selected=tab==0,onClick={tab=0},text={Text("我的日记")});Tab(selected=tab==1,onClick={tab=1},text={Text(card.name,maxLines=1,overflow=TextOverflow.Ellipsis)})}}
      if(tab==0)item{Button(onClick={diaryTitle="";diaryContent="";editorOpen=true},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.EditNote,null);Spacer(Modifier.width(7.dp));Text("写日记")}}
      else item{OutlinedButton(onClick={if(deliver("今天有什么想写进日记的吗？你可以自己决定是否写，也可以选择和我交换一篇日记。"))composePage=Page.CHAT},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.AutoStories,null);Spacer(Modifier.width(7.dp));Text("邀请 ${card.name} 写日记",maxLines=1,overflow=TextOverflow.Ellipsis)}}
      if(visible.isEmpty())item{EmptyState(Icons.Outlined.MenuBook,if(tab==0)"还没有日记" else "还没有收到日记",if(tab==0)"写下只保存在本机的此刻" else "角色主动分享或交换后会显示在这里")}
      else items(visible,key={it.id}){entry->Surface(onClick={viewing=entry},shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(entry.ownerType=="user")Icons.Outlined.Person else Icons.Outlined.AutoStories,null,Modifier.size(19.dp),tint=MaterialTheme.colorScheme.secondary);Text(entry.title.ifBlank{"无题日记"},Modifier.weight(1f).padding(start=8.dp),fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(java.text.SimpleDateFormat("M月d日",Locale.CHINA).format(Date(entry.createdAt)),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(entry.content,fontSize=14.sp,lineHeight=20.sp,maxLines=5,overflow=TextOverflow.Ellipsis);if(entry.ownerType=="user")Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={share(entry,false)}){Icon(Icons.Outlined.Share,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("分享")};TextButton(onClick={share(entry,true)}){Icon(Icons.Outlined.SwapHoriz,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("交换")}}}}}
    }
    if(editorOpen)AlertDialog(onDismissRequest={editorOpen=false},icon={Icon(Icons.Outlined.EditNote,null)},title={Text("写日记")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(diaryTitle,{diaryTitle=it.take(80)},Modifier.fillMaxWidth(),label={Text("标题")},singleLine=true);OutlinedTextField(diaryContent,{diaryContent=it.take(4000)},Modifier.fillMaxWidth().heightIn(min=180.dp,max=360.dp),label={Text("正文")},supportingText={Text("${diaryContent.length} / 4000")},minLines=8,maxLines=18)}},confirmButton={Button(onClick={if(diaryContent.trim().isBlank())toast("请先写下日记内容")else{db.addDiary("user",prefs.getString("active_identity_id","local")?:"local",diaryTitle.trim().ifBlank{java.text.SimpleDateFormat("M月d日的日记",Locale.CHINA).format(Date())},diaryContent.trim());editorOpen=false;diaryTitle="";diaryContent="";uiRevision++;toast("日记已保存")}}){Text("保存")}},dismissButton={TextButton(onClick={editorOpen=false}){Text("取消")}})
    viewing?.let{entry->AlertDialog(onDismissRequest={viewing=null},title={Text(entry.title.ifBlank{"无题日记"})},text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState())){Text(entry.content,fontSize=15.sp,lineHeight=23.sp);Text(java.text.SimpleDateFormat("yyyy年M月d日 HH:mm",Locale.CHINA).format(Date(entry.createdAt)),Modifier.padding(top=18.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}},confirmButton={TextButton(onClick={viewing=null}){Text("关闭")}},dismissButton=if(entry.ownerType=="user"){{TextButton(onClick={viewing=null;db.deleteDiary(entry.id);uiRevision++;toast("日记已删除")}){Text("删除",color=MaterialTheme.colorScheme.error)}}}else null)}
  }

  @Composable
  private fun MemorySummaryPage(chat:Chat){
    var summary by rememberSaveable(chat.id){mutableStateOf(chat.summary)}
    DetailScaffold("记忆概要",Page.CHAT_DETAIL){
      item{InfoBanner(Icons.Outlined.Summarize,"受限的对话摘要","自动摘要建议保持在 300–800 字；手动编辑最多 4000 字，仅用于当前对话的后续上下文。")}
      item{OutlinedTextField(summary,{summary=it.take(4000)},Modifier.fillMaxWidth().heightIn(min=220.dp,max=520.dp),label={Text("当前摘要")},supportingText={Text("${summary.length} / 4000")},minLines=10,maxLines=24,shape=RoundedCornerShape(13.dp))}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={summary=""},Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(6.dp));Text("清空")};Button(onClick={chat.summary=constrainSummary(summary);save();uiRevision++;toast("记忆概要已保存");composePage=Page.CHAT_DETAIL},Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存")}}}
    }
  }

  @Composable
  private fun PlotNodesPage(chat:Chat){
    var title by remember(chat.id){mutableStateOf("")}
    var revision by remember{mutableIntStateOf(0)}
    val nodes=remember(chat.id,revision,uiRevision){chat.plotNodes.sortedByDescending{it.createdAt}}
    fun snapshot(messages:List<Msg>):String{val values=JSONArray();messages.forEach{values.put(it.json())};return values.toString()}
    LaunchedEffect(chat.id){
      var upgraded=false
      chat.plotNodes.filter{it.snapshotVersion<1&&it.messageCount<=chat.messages.size}.forEach{node->
        node.messagesJson=snapshot(chat.messages.take(node.messageCount));node.summarySnapshot="";node.contextSnapshot="";node.contextAt=0L;node.snapshotVersion=1;upgraded=true
      }
      if(upgraded){save();revision++;uiRevision++}
    }
    DetailScaffold("剧情节点",Page.CHAT){
      item{InfoBanner(Icons.Outlined.BookmarkAdded,"独立剧情存档","每个节点保存完整消息与上下文快照。读取旧节点不会删除更晚的节点，可随时在不同剧情分支间切换。")}
      item{StyledField(title,{title=it.take(80)},"节点名称（如：雨夜重逢）")}
      item{Button(onClick={val value=title.trim().ifBlank{"剧情节点 ${chat.plotNodes.size+1}"};val node=PlotNode();node.title=value;node.messageCount=chat.messages.size;node.messagesJson=snapshot(chat.messages);node.summarySnapshot=chat.summary;node.contextSnapshot=chat.lastContextSnapshot;node.contextAt=chat.lastContextAt;node.snapshotVersion=1;node.createdAt=System.currentTimeMillis();chat.plotNodes.add(node);chat.updated=System.currentTimeMillis();save();title="";revision++;uiRevision++;toast("独立剧情存档已保存")},enabled=chat.messages.isNotEmpty()&&!isChatSending(chat.id),modifier=Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.BookmarkAdd,null);Spacer(Modifier.width(7.dp));Text("保存当前剧情存档",maxLines=1,softWrap=false)}}
      item{SectionTitle("已保存节点 ${nodes.size} 个")}
      if(nodes.isEmpty())item{EmptyState(Icons.Outlined.Bookmarks,"还没有剧情节点","在重要剧情发生后保存一个节点")}
      items(nodes,key={it.id}){node->Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Bookmark,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(node.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("第 ${node.messageCount} 条消息 · ${java.text.SimpleDateFormat("M月d日 HH:mm",Locale.CHINA).format(Date(node.createdAt))}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)};IconButton(onClick={chat.plotNodes.removeAll{it.id==node.id};save();revision++;uiRevision++;toast("节点已删除")}){Icon(Icons.Outlined.Delete,"删除节点",tint=MaterialTheme.colorScheme.error)}}
        val canRestore=node.snapshotVersion>=1&&node.messagesJson.isNotBlank()
        Button(onClick={askDanger("读取 ${node.title}","当前对话将切换到该独立存档；其他剧情节点会完整保留。","读取存档"){runCatching{val values=JSONArray(node.messagesJson);val restored=ArrayList<Msg>();for(i in 0 until values.length()){values.optJSONObject(i)?.let{restored.add(Msg.from(it))}};require(restored.size==values.length()){ "存档消息不完整" };chat.messages.clear();chat.messages.addAll(restored);chat.summary=node.summarySnapshot;chat.lastContextSnapshot=node.contextSnapshot;chat.lastContextAt=node.contextAt;chat.updated=System.currentTimeMillis();save();chatUiRevision++;uiRevision++;toast("已读取 ${node.title}");composePage=Page.CHAT}.onFailure{toast("读取存档失败：${friendlyError(it)}")}}},enabled=canRestore&&!isChatSending(chat.id),modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(42.dp)){Icon(Icons.Outlined.History,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(if(canRestore)"读取此独立存档" else "旧节点暂不可读取",maxLines=1,softWrap=false)}
      }}}
    }
  }

  @Composable
  private fun ChatBackgroundPage(chat:Chat){
    var custom by remember(chat.id){mutableStateOf(chat.backgroundImage.isNotBlank()||chat.backgroundColor.isNotBlank()||chat.backgroundOverlay>=0)}
    var color by remember(chat.id){mutableStateOf(chat.backgroundColor)}
    var image by remember(chat.id){mutableStateOf(chat.backgroundImage)}
    var overlay by remember(chat.id){mutableFloatStateOf((if(chat.backgroundOverlay>=0)chat.backgroundOverlay else prefs.getInt("chat_background_overlay",20)).toFloat())}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{val target=File(filesDir,"chat_bg_${chat.id}.img");contentResolver.openInputStream(uri)?.use{input->FileOutputStream(target).use{output->input.copyTo(output)}}?:error("无法读取背景图");image=target.absolutePath;custom=true;bitmapCache.remove(target.absolutePath)}.onFailure{toast("背景图保存失败：${it.message}")}}}
    val palette=listOf("" to "跟随主题","#FFF7F8F4" to "雾白","#FFF1EDEF" to "浅粉","#FFE8F1EF" to "青灰","#FF1A1A1C" to "灰黑","#FF26302E" to "墨绿")
    DetailScaffold("当前对话背景",Page.CHAT_DETAIL){
      item{ChatSettingSwitch("覆盖全局聊天背景","关闭后恢复“外观与动画”中的全局设置",custom){custom=it}}
      if(custom){item{ColorSwatchSelector("背景颜色",color,palette){color=it}};item{ColorHexInput("自定义背景颜色",color){color=it}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={picker.launch(arrayOf("image/*"))},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Wallpaper,null);Spacer(Modifier.width(6.dp));Text(if(image.isBlank())"选择背景图" else "更换背景图")};OutlinedButton(onClick={image=""},enabled=image.isNotBlank(),modifier=Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.HideImage,null);Spacer(Modifier.width(6.dp));Text("清除图片")}}};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("背景遮罩 ${overlay.toInt()}%",fontWeight=FontWeight.Medium);Slider(overlay,{overlay=it},valueRange=0f..70f,steps=13)}}}}
      item{Button(onClick={if(!custom){chat.backgroundImage.takeIf{it.startsWith(filesDir.absolutePath)}?.let{runCatching{File(it).delete()}};chat.backgroundImage="";chat.backgroundColor="";chat.backgroundOverlay=-1}else{val normalized=normalizeStoredColor(color);if(normalized==null){toast("颜色格式无效");return@Button};chat.backgroundImage=image;chat.backgroundColor=normalized;chat.backgroundOverlay=overlay.toInt()};save();uiRevision++;toast("对话背景已保存");composePage=Page.CHAT_DETAIL},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存对话背景")}}
    }
  }

  @Composable
  private fun DesktopPetPage(){
    var petEnabled by remember{mutableStateOf(prefs.getBoolean("experimental_desktop_pet_enabled",false))}
    var petStyle by remember{mutableStateOf(prefs.getString("experimental_desktop_pet_style","avatar")?:"avatar")}
    var petCardId by remember{mutableStateOf(prefs.getString("experimental_desktop_pet_card_id","").orEmpty())}
    var petMessageNotice by remember{mutableStateOf(prefs.getString("desktop_pet_message_notice","both")?:"both")}
    var petReplyMode by remember{mutableStateOf(prefs.getString("desktop_pet_reply_mode","both")?:"both")}
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
    fun persistPetSize(value:Float){val range=if(petStyle=="live2d")80f..320f else 32f..160f;petSize=value.coerceIn(range);petSizeInput=petSize.toInt().toString();prefs.edit().putInt(if(petStyle=="live2d")"experimental_live2d_pet_size" else "experimental_desktop_pet_size",petSize.toInt()).apply();DesktopPetService.sync(this@ComposeActivity);uiRevision++}
    DetailScaffold("桌宠模式"){
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
          Text("桌宠回复方式",Modifier.padding(top=12.dp,bottom=6.dp),fontSize=13.sp,fontWeight=FontWeight.Medium)
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("voice" to "仅语音","both" to "文字+语音","text" to "仅文字").forEach{(value,label)->FilterChip(selected=petReplyMode==value,onClick={petReplyMode=value;prefs.edit().putString("desktop_pet_reply_mode",value).apply()},label={Text(label,maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}
          }
          Text("语音使用“我的 → 语音模型”中的本地或在线 TTS 配置。",Modifier.padding(top=5.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }else Text("创建或导入角色后即可选择桌宠。",Modifier.padding(top=9.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().clickable{petGuideExpanded=!petGuideExpanded}.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.MenuBook,null,tint=MaterialTheme.colorScheme.secondary);Text("桌宠使用指南",Modifier.weight(1f).padding(start=10.dp),fontWeight=FontWeight.Medium);Icon(if(petGuideExpanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)};AnimatedVisibility(petGuideExpanded){Column(Modifier.fillMaxWidth().padding(start=15.dp,end=15.dp,bottom=15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("点击：空闲时互动；绿色提示或回复气泡可直接进入对应聊天。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("拖动：拖动桌宠或气泡正文可移动；右侧手柄可上下左右调整气泡大小。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("长按：打开可拖动快捷面板，可投喂、回复、分析屏幕、打开设置或重置位置。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("气泡：箭头进入快捷回复，关闭图标只收起当前气泡。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("Live2D：导入官方 Cubism Web Core 与包含 model3.json、moc3、纹理的 ZIP。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
      item{SectionTitle("屏幕内容分析")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("允许桌宠分析当前屏幕","仅在长按桌宠并点击眼睛时读取可见文字",screenContextEnabled){value->screenContextEnabled=value;prefs.edit().putBoolean("experimental_screen_context_enabled",value).apply();uiRevision++}
        Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(accessibilityGranted)Icons.Outlined.VerifiedUser else Icons.Outlined.AdminPanelSettings,null,tint=if(accessibilityGranted)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(if(accessibilityGranted)"系统权限已开启" else "尚未开启系统无障碍权限",fontWeight=FontWeight.Medium,fontSize=13.sp);Text("密码输入框会自动跳过，内容只在主动点击后发送",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};OutlinedButton(onClick={runCatching{accessibilityPermission.launch(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))}.onFailure{toast("无法打开无障碍设置")}},contentPadding=PaddingValues(horizontal=11.dp)){Text(if(accessibilityGranted)"权限设置" else "去授权",maxLines=1,softWrap=false)}}
      }}}
      if(petLastError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"最近一次桌宠回复失败",friendlyErrorText(petLastError))}
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
    val serviceError=prefs.getString("weixin_link_service_error","").orEmpty()
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
    DetailScaffold("将角色接入微信",Page.EXPERIMENTAL){
      if(serviceError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"微信后台连接未启动",serviceError)}
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
  private fun QqBotPage(){
    var localRevision by remember{mutableIntStateOf(0)}
    var editingId by rememberSaveable{mutableStateOf("")}
    var botName by rememberSaveable{mutableStateOf("")}
    var appId by rememberSaveable{mutableStateOf("")}
    var appSecret by remember{mutableStateOf("")}
    var selectedCardId by rememberSaveable{mutableStateOf(cards.firstOrNull()?.id.orEmpty())}
    var selectedChatId by rememberSaveable{mutableStateOf("")}
    var enabled by rememberSaveable{mutableStateOf(true)}
    val accounts=remember(localRevision,uiRevision){QqBotClient.accounts(this@ComposeActivity)}
    val selectedChats=remember(selectedCardId,uiRevision){chats.filter{!it.group&&it.cardId==selectedCardId}.sortedByDescending{it.updated}}
    val serviceError=prefs.getString("qqbot_service_error","").orEmpty()

    DisposableEffect(Unit){
      val receiver=object:android.content.BroadcastReceiver(){override fun onReceive(context:android.content.Context?,intent:Intent?){localRevision++}}
      androidx.core.content.ContextCompat.registerReceiver(this@ComposeActivity,receiver,android.content.IntentFilter(QqBotClient.ACTION_STATE_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
      onDispose{runCatching{unregisterReceiver(receiver)}}
    }

    fun clearEditor(){
      editingId="";botName="";appId="";appSecret="";selectedCardId=cards.firstOrNull()?.id.orEmpty();selectedChatId="";enabled=true
    }
    fun edit(account:QqBotClient.Account){
      editingId=account.id;botName=account.name;appId=account.appId
      appSecret=secure.getNamed(QqBotClient.secretKey(account.id));selectedCardId=account.cardId
      selectedChatId=account.chatId;enabled=account.enabled
    }
    fun persist(){
      val name=botName.trim();val idValue=appId.trim();val secretValue=appSecret.trim()
      when{
        name.isBlank()->toast("请填写机器人名称")
        idValue.isBlank()->toast("请填写 AppID")
        secretValue.isBlank()->toast("请填写 AppSecret")
        selectedCardId.isBlank()->toast("请先选择角色")
        else->runCatching{
          val id=editingId.ifBlank{UUID.randomUUID().toString()}
          secure.putNamed(QqBotClient.secretKey(id),secretValue)
          val updated=accounts.toMutableList();val value=QqBotClient.Account(id,name,idValue,selectedCardId,selectedChatId,enabled)
          val index=updated.indexOfFirst{it.id==id};if(index>=0)updated[index]=value else updated.add(value)
          QqBotClient.saveAccounts(this@ComposeActivity,updated)
        }.onSuccess{clearEditor();localRevision++;uiRevision++;toast("QQ Bot 配置已保存")}
          .onFailure{toast("保存失败：${it.message?:"未知错误"}")}
      }
    }

    DetailScaffold("官方 QQ Bot",Page.EXPERIMENTAL){
      item{InfoBanner(Icons.Outlined.Science,"实验性连接","使用 QQ 开放平台官方机器人接口；每个 Bot 独立连接并绑定一个角色，AppSecret 仅加密保存在本机。")}
      if(serviceError.isNotBlank())item{InfoBanner(Icons.Outlined.ErrorOutline,"最近一次后台错误",serviceError)}
      item{SectionTitle(if(editingId.isBlank())"添加 QQ Bot" else "编辑 QQ Bot")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        StyledField(botName,{botName=it.take(80)},"机器人名称")
        StyledField(appId,{appId=it.filter(Char::isDigit).take(64)},"AppID")
        OutlinedTextField(appSecret,{appSecret=it.take(300)},Modifier.fillMaxWidth(),label={Text("AppSecret")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))
        Text("绑定角色",fontSize=13.sp,fontWeight=FontWeight.Medium)
        if(cards.isEmpty())Text("请先创建或导入角色",fontSize=12.sp,color=MaterialTheme.colorScheme.error)
        else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){cards.forEach{card->FilterChip(selected=selectedCardId==card.id,onClick={selectedCardId=card.id;selectedChatId=""},label={Text(card.name,maxLines=1)},leadingIcon={if(selectedCardId==card.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})}}
        if(selectedCardId.isNotBlank()){
          Text("聊天记录",fontSize=13.sp,fontWeight=FontWeight.Medium)
          Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=selectedChatId.isBlank(),onClick={selectedChatId=""},label={Text("按联系人自动新建")})
            selectedChats.forEach{chat->FilterChip(selected=selectedChatId==chat.id,onClick={selectedChatId=chat.id},label={Text(chat.title.ifBlank{"未命名聊天"},maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={if(selectedChatId==chat.id)Icon(Icons.Filled.Check,null,Modifier.size(16.dp))})}
          }
        }
        ChatSettingSwitch("保存后立即启用","后台连接私聊和群聊消息",enabled){enabled=it}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
          if(editingId.isNotBlank())OutlinedButton(onClick={clearEditor()},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Close,null);Spacer(Modifier.width(6.dp));Text("取消")}
          Button(onClick={persist()},enabled=cards.isNotEmpty(),modifier=Modifier.weight(1f).height(46.dp)){Icon(if(editingId.isBlank())Icons.Outlined.Add else Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text(if(editingId.isBlank())"添加 Bot" else "保存修改",maxLines=1)}
        }
      }}}
      if(accounts.isNotEmpty())item{SectionTitle("已配置 Bot")}
      items(accounts,key={it.id}){account->
        val card=cards.firstOrNull{it.id==account.cardId};val bound=chats.firstOrNull{it.id==account.chatId}
        val status=prefs.getString("qqbot_status_${account.id}",if(account.enabled)"等待连接" else "已暂停").orEmpty()
        val error=prefs.getString("qqbot_error_${account.id}","").orEmpty()
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(13.dp)){
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            if(card!=null)RoleAvatar(card,44)else Avatar("Q",44)
            Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(account.name.ifBlank{"QQ Bot"},fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("$status · ${card?.name?:"角色已删除"} · ${bound?.title?:"自动建聊天"}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)}
            Switch(account.enabled,{QqBotClient.setEnabled(this@ComposeActivity,account.id,it);localRevision++})
            IconButton(onClick={edit(account)},Modifier.size(38.dp)){Icon(Icons.Outlined.Edit,"编辑")}
            IconButton(onClick={askDanger("删除 ${account.name}","删除这个 QQ Bot 配置与本机加密密钥，已同步的聊天记录会保留。","确认删除"){chats.filter{it.externalSource=="qqbot"&&it.externalPeer.startsWith(account.id+":")}.forEach{it.externalSource="";it.externalPeer=""};save();QqBotClient.delete(this@ComposeActivity,account.id);if(editingId==account.id)clearEditor();localRevision++;uiRevision++}},Modifier.size(38.dp)){Icon(Icons.Outlined.Delete,"删除",tint=MaterialTheme.colorScheme.error)}
          }
          if(error.isNotBlank())Text(error,Modifier.padding(top=8.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.error,maxLines=3,overflow=TextOverflow.Ellipsis)
        }}
      }
      item{InfoBanner(Icons.Outlined.Security,"权限范围","连接仅申请 C2C 私聊与群聊 @ 消息事件，不申请未使用的高权限 Intent。")}
    }
  }

  @Composable
  private fun AppearancePage(themeMode: Int, setTheme: (Int) -> Unit,themeStyle:Int,setThemeStyle:(Int)->Unit) {
    var bubbleFont by remember { mutableFloatStateOf(prefs.getInt("chat_font_size",15).toFloat()) }
    var bubbleSpacing by remember { mutableFloatStateOf(prefs.getInt("chat_bubble_spacing",8).toFloat()) }
    var lineHeight by remember { mutableFloatStateOf(prefs.getInt("chat_line_height",22).toFloat()) }
    var pendingExport by remember{mutableStateOf("")}
    val exportAppearance=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)runCatching{contentResolver.openOutputStream(uri,"w")?.use{it.write(pendingExport.toByteArray(Charsets.UTF_8))}?:error("无法写入文件")}.onSuccess{toast("外观配置已导出")}.onFailure{toast("导出失败：${it.message}")}}
    val importAppearance=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)runCatching{String(bytes(contentResolver.openInputStream(uri)),Charsets.UTF_8)}.onSuccess{raw->runCatching{importAppearanceConfig(raw)}.onSuccess{toast("外观配置已导入，正在应用");recreate()}.onFailure{toast("导入失败：${it.message}")}}.onFailure{toast("读取失败：${it.message}")}}
    DetailScaffold("外观与动画") {
      item { InfoBanner(Icons.Outlined.ColorLens, "时遇风格", "珊瑚色表达情感，青绿色强调操作；深浅模式会同步系统状态栏。") }
      item{SectionTitle("分享与迁移")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(14.dp)){Text("我的外观配置",fontWeight=FontWeight.Medium);Text("包含主题、聊天背景颜色、气泡、字号、布局、玻璃效果与刷新率；不包含背景图片和聊天数据。",Modifier.padding(top=3.dp,bottom=11.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedButton(onClick={pendingExport=appearanceConfigJson().toString(2);exportAppearance.launch("shiyu-appearance.json")},Modifier.weight(1f),contentPadding=PaddingValues(horizontal=7.dp)){Icon(Icons.Outlined.FileDownload,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("导出")};OutlinedButton(onClick={importAppearance.launch(arrayOf("application/json","text/plain","*/*"))},Modifier.weight(1f),contentPadding=PaddingValues(horizontal=7.dp)){Icon(Icons.Outlined.FileOpen,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("导入")};Button(onClick={shareAppearanceConfig()},Modifier.weight(1f),contentPadding=PaddingValues(horizontal=7.dp)){Icon(Icons.Outlined.Share,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("分享")}}}}}
      item { SectionTitle("主题模式") }
      items(listOf("跟随系统" to "根据手机深浅模式自动切换", "明亮" to "清爽、柔和的浅色界面", "夜间" to "低亮度深色界面").withIndex().toList()) { indexed ->
        val index=indexed.index; val value=indexed.value
        Surface(onClick = { prefs.edit().putInt("compose_theme", index).apply(); setTheme(index) }, shape=RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.surface) {
          Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(themeMode==index)Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.padding(start=12.dp)){Text(value.first,fontWeight=FontWeight.Medium);Text(value.second,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        }
      }
      item { SectionTitle("主题风格") }
      items(listOf("时遇珊瑚" to "温暖珊瑚与青绿色，保持时遇默认气质","青岚" to "清透青绿、蓝灰与柔和冷白","樱雾" to "低饱和樱粉、莓红与中性灰","简墨" to "黑白灰为主，使用少量青色强调","微信绿" to "仿微信聊天 App 的清新绿色、灰白与克制气泡").withIndex().toList()){indexed->val index=indexed.index;val value=indexed.value;Surface(onClick={prefs.edit().putInt("compose_theme_style",index).apply();setThemeStyle(index);uiRevision++},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=themePalette(index,false).primary,modifier=Modifier.size(28.dp)){Box{}};Column(Modifier.weight(1f).padding(start=12.dp)){Text(value.first,fontWeight=FontWeight.Medium);Text(value.second,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(if(themeStyle==index)Icons.Filled.CheckCircle else Icons.Outlined.Circle,null,tint=MaterialTheme.colorScheme.primary)}}}
      item { SectionTitle("显示性能") }
      item { SettingButton(Icons.Outlined.Speed,"屏幕刷新率",prefs.getInt("refresh_rate",0).takeIf{it>0}?.let{"优先 ${it}Hz"}?:"跟随系统"){composePage=Page.FRAME_RATE} }
      item { SettingButton(Icons.Outlined.ChatBubbleOutline,"聊天页面样式","背景图片、背景色、气泡颜色、宽度与高度"){composePage=Page.CHAT_APPEARANCE} }
      item { SectionTitle("聊天气泡") }
      item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){Text("字体大小 ${bubbleFont.toInt()} sp",fontWeight=FontWeight.Medium);Slider(bubbleFont,{bubbleFont=it},valueRange=12f..20f,steps=7);Text("气泡间距 ${bubbleSpacing.toInt()} dp",fontWeight=FontWeight.Medium);Slider(bubbleSpacing,{bubbleSpacing=it},valueRange=2f..16f,steps=13);Text("正文行距 ${lineHeight.toInt()} sp",fontWeight=FontWeight.Medium);Slider(lineHeight,{lineHeight=it},valueRange=16f..32f,steps=15);Surface(Modifier.fillMaxWidth().padding(top=8.dp),RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primary){Text("这是聊天气泡预览，调整后会应用到所有对话。",Modifier.padding(14.dp),fontSize=bubbleFont.toInt().sp,lineHeight=lineHeight.toInt().coerceAtLeast(bubbleFont.toInt()+2).sp,color=MaterialTheme.colorScheme.onPrimary)}}} }
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
    var bubbleActions by remember{mutableStateOf(prefs.getString("chat_bubble_actions","latest")?:"latest")}
    var overlay by remember{mutableFloatStateOf(prefs.getInt("chat_background_overlay",20).toFloat())}
    var halfScreenMode by remember{mutableStateOf(prefs.getBoolean("chat_half_screen",false))}
    var backgroundFollowKeyboard by remember{mutableStateOf(prefs.getBoolean("chat_background_follow_ime",false))}
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
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("半屏聊天模式",fontWeight=FontWeight.Medium);Text("有背景图的消息列表缩到下半屏，上半部分完整露出背景人像",fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(halfScreenMode,{halfScreenMode=it})}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("聊天背景跟随键盘上移",fontWeight=FontWeight.Medium);Text("输入时背景图片随键盘向上移动，默认关闭",fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(backgroundFollowKeyboard,{backgroundFollowKeyboard=it})}}}
      item{SectionTitle("推荐回复位置")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=suggestionPosition=="bubble",onClick={suggestionPosition="bubble"},label={Text("角色气泡下方",maxLines=1,softWrap=false)},leadingIcon={Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(16.dp))},modifier=Modifier.weight(1f));FilterChip(selected=suggestionPosition=="input",onClick={suggestionPosition="input"},label={Text("输入框上方",maxLines=1,softWrap=false)},leadingIcon={Icon(Icons.Outlined.Keyboard,null,Modifier.size(16.dp))},modifier=Modifier.weight(1f))}}
      item{SectionTitle("气泡功能按钮")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=bubbleActions=="latest",onClick={bubbleActions="latest"},label={Text("仅最新回复",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f));FilterChip(selected=bubbleActions=="hidden",onClick={bubbleActions="hidden"},label={Text("全部隐藏",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}}
      item{SectionTitle("气泡颜色")}
      item{ColorSwatchSelector("我的气泡",userBubble,bubblePalette){userBubble=it}}
      item{ColorHexInput("自定义我的气泡颜色",userBubble){userBubble=it}}
      item{ColorSwatchSelector("角色气泡",aiBubble,bubblePalette){aiBubble=it}}
      item{ColorHexInput("自定义角色气泡颜色",aiBubble){aiBubble=it}}
      if(!noBubbles)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("我的气泡透明度 ${userBubbleOpacity.toInt()}%",fontWeight=FontWeight.Medium);Slider(userBubbleOpacity,{userBubbleOpacity=it},valueRange=10f..100f,steps=17);Text("角色气泡透明度 ${aiBubbleOpacity.toInt()}%",fontWeight=FontWeight.Medium);Slider(aiBubbleOpacity,{aiBubbleOpacity=it},valueRange=10f..100f,steps=17)}}}
      item{SectionTitle("尺寸")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Text("气泡最大宽度 ${bubbleWidth.toInt()} dp",fontWeight=FontWeight.Medium);Slider(bubbleWidth,{bubbleWidth=it},valueRange=240f..360f,steps=11);Text("气泡上下内边距 ${bubblePadding.toInt()} dp",fontWeight=FontWeight.Medium);Slider(bubblePadding,{bubblePadding=it},valueRange=6f..20f,steps=13);Text("输入框最小高度 ${inputHeight.toInt()} dp",fontWeight=FontWeight.Medium);Slider(inputHeight,{inputHeight=it},valueRange=40f..64f,steps=5)}}}
      item{Button(onClick={val normalizedBackground=normalizeStoredColor(background);val normalizedUser=normalizeStoredColor(userBubble);val normalizedAi=normalizeStoredColor(aiBubble);if(normalizedBackground==null||normalizedUser==null||normalizedAi==null)toast("颜色格式无效，请使用 6 位 RGB 或 8 位 ARGB")else{prefs.edit().putString("chat_background_color",normalizedBackground).putString("chat_user_bubble_color",normalizedUser).putString("chat_ai_bubble_color",normalizedAi).putBoolean("chat_no_bubbles",noBubbles).putBoolean("chat_hide_avatars",hideAvatars).putBoolean("chat_half_screen",halfScreenMode).putBoolean("chat_background_follow_ime",backgroundFollowKeyboard).putString("reply_suggestion_position",suggestionPosition).putString("chat_bubble_actions",bubbleActions).putInt("chat_user_bubble_opacity",userBubbleOpacity.toInt()).putInt("chat_ai_bubble_opacity",aiBubbleOpacity.toInt()).putInt("chat_bubble_max_width",bubbleWidth.toInt()).putInt("chat_bubble_vertical_padding",bubblePadding.toInt()).putInt("chat_input_min_height",inputHeight.toInt()).putInt("chat_background_overlay",overlay.toInt()).apply();background=normalizedBackground;userBubble=normalizedUser;aiBubble=normalizedAi;uiRevision++;toast("聊天页面样式已保存");composePage=Page.APPEARANCE}},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(7.dp));Text("保存聊天页面样式",maxLines=1,softWrap=false)}}
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
    val promptTokens=chats.sumOf{it.promptTokens};val completionTokens=chats.sumOf{it.completionTokens};val messageCount=chats.sumOf{it.messages.size};val memoryCount=cards.sumOf{db.memories(it.id).size+db.memoryNodes(it.id).size}
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
    var password by remember { mutableStateOf("") }
    var confirmOverwrite by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var includeRoles by rememberSaveable { mutableStateOf(true) }
    var includeChats by rememberSaveable { mutableStateOf(true) }
    var includeMemories by rememberSaveable { mutableStateOf(true) }
    var includeDiaries by rememberSaveable { mutableStateOf(true) }
    var includeMoments by rememberSaveable { mutableStateOf(true) }
    var includeSettings by rememberSaveable { mutableStateOf(true) }
    var includeCredentials by rememberSaveable { mutableStateOf(true) }
    var includeAssets by rememberSaveable { mutableStateOf(true) }
    var exportSelection by remember { mutableStateOf(BackupSelection.all()) }
    var exportPassword by remember { mutableStateOf("") }
    val rolesRequired=includeChats||includeMemories||includeDiaries||includeMoments
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->
      if(uri!=null){running=true;val selection=exportSelection;val secret=exportPassword;pool.execute{runCatching{contentResolver.openOutputStream(uri,"w")?.use{BackupManager.write(this@ComposeActivity,db,prefs,secure,secret,it,selection);it.flush()}?:error("系统未提供可写入的文件位置")}.onSuccess{main.post{running=false;exportPassword="";toast(if(selection.isFull)"完整备份已创建" else "所选内容已加密备份")}}.onFailure{error->DiagnosticLog.record(this@ComposeActivity,"backup","error",diagnosticSummary(error));main.post{running=false;exportPassword="";toast("创建备份失败：${error.message?:friendlyError(error)}")}}}}
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
      if(uri!=null){running=true;pool.execute{runCatching{contentResolver.openInputStream(uri)?.use{BackupManager.restore(this@ComposeActivity,db,prefs,secure,it,password)}?:error("系统无法读取该备份")}.onSuccess{main.post{running=false;if(load()){selectedChat=null;ActiveReplyWorker.sync(this@ComposeActivity);YandereMessageService.sync(this@ComposeActivity);MomentAutoPostWorker.sync(this@ComposeActivity);toast("备份恢复完成，正在重新载入");recreate()}else{composePage=Page.DATA_RESCUE;toast("备份已写入，但数据仍无法正常载入")}}}.onFailure{error->DiagnosticLog.record(this@ComposeActivity,"backup_restore","error",diagnosticSummary(error));val readable=load();main.post{running=false;if(!readable){selectedChat=null;composePage=Page.DATA_RESCUE};toast(if(readable)"恢复失败：${error.message?:friendlyError(error)}" else "恢复未完成，本地数据需要抢救")}}}}
    }
    DetailScaffold("备份与恢复",Page.DATA){
      item{InfoBanner(Icons.Outlined.Inventory,"选择备份内容","按需勾选导出；聊天、记忆、日记和朋友圈会自动包含关联角色卡。部分备份恢复时按 ID 合并，未选择的本地内容不会被清空；同 ID 内容以备份为准。部分备份需使用 0.9.3 本次修订或更新版本恢复。")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        ChatSettingSwitch("角色卡",if(rolesRequired)"已自动包含，取消关联内容后可取消" else "角色设定、世界书、预设",includeRoles||rolesRequired){if(!running&&!rolesRequired)includeRoles=it}
        ChatSettingSwitch("聊天记录","会话、消息和对话摘要",includeChats){if(!running)includeChats=it}
        ChatSettingSwitch("长期记忆","记忆、纪念节点与语义库",includeMemories){if(!running)includeMemories=it}
        ChatSettingSwitch("日记","用户及角色日记",includeDiaries){if(!running)includeDiaries=it}
        ChatSettingSwitch("朋友圈","动态、评论及相关设置",includeMoments){if(!running)includeMoments=it}
        ChatSettingSwitch("应用设置与供应商","外观、用户资料和模型配置",includeSettings){if(!running)includeSettings=it}
        ChatSettingSwitch("密钥与登录凭据","包含 API Key 与已保存登录凭据，请勿分享给他人",includeCredentials){if(!running)includeCredentials=it}
        ChatSettingSwitch("本地图片资源","包含已选内容引用的头像、背景和消息图片",includeAssets){if(!running)includeAssets=it}
      }}}
      item{OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("备份加密密码")},supportingText={Text("创建备份至少 6 位；使用 AES-256-GCM 加密全部聊天、设置和 API Key")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))}
      item{Button(onClick={val selection=BackupSelection(includeRoles,includeChats,includeMemories,includeDiaries,includeMoments,includeSettings,includeCredentials,includeAssets);if(password.length<6)toast("请设置至少 6 位备份密码")else if(selection.isEmpty)toast("请至少选择一项备份内容")else{exportSelection=selection;exportPassword=password;val stamp=java.text.SimpleDateFormat("yyyyMMdd-HHmm",java.util.Locale.getDefault()).format(java.util.Date());export.launch("shiyu-backup-$stamp.sybak")}},enabled=!running,modifier=Modifier.fillMaxWidth().height(50.dp)){if(running)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Backup,null);Spacer(Modifier.width(8.dp));Text(if(running)"正在处理" else "备份所选内容",maxLines=1,softWrap=false)}}
      item{SectionTitle("从备份恢复")}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.errorContainer){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("我已了解恢复会覆盖本地数据",fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.onErrorContainer);Text("导入前建议先创建当前数据的备份",fontSize=12.sp,color=MaterialTheme.colorScheme.onErrorContainer)};Switch(checked=confirmOverwrite,onCheckedChange={confirmOverwrite=it})}}}
      item{OutlinedButton(onClick={restore.launch(arrayOf("application/json","application/octet-stream","*/*"))},enabled=confirmOverwrite&&!running,modifier=Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.Restore,null);Spacer(Modifier.width(8.dp));Text("选择备份并恢复",maxLines=1,softWrap=false)}}
      item{GuideStep("1","创建备份","设置至少 6 位密码后导出，聊天、角色、API Key 和本地资源会统一加密。")} 
      item{GuideStep("2","妥善保存","备份文件只保存在你选择的位置，时遇服务器不会接收备份内容。")}
      item{GuideStep("3","恢复数据","完整备份覆盖本地数据；部分备份合并所选内容，保留未选择的内容，完成后自动重新载入。")}
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun StickerResourcesPage() {
    uiRevision
    var downloading by remember { mutableStateOf("") }
    var progressText by remember { mutableStateOf("") }
    var aiEnabled by remember { mutableStateOf(prefs.getBoolean("stickers_ai_enabled", false)) }
    var stickerPreview by remember { mutableStateOf("") }
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
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色使用表情",fontWeight=FontWeight.Medium);Text("角色可根据情绪从本地资源中选择表情",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(aiEnabled&&all.isEmpty())Text("本机还没有表情资源，先在下方导入或下载表情包，角色才能发表情",fontSize=12.sp,color=MaterialTheme.colorScheme.error)};Switch(aiEnabled,{aiEnabled=it;prefs.edit().putBoolean("stickers_ai_enabled",it).apply()})}} }
      item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){OutlinedButton(onClick={importer.launch(arrayOf("image/*"))},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.AddPhotoAlternate,null);Spacer(Modifier.width(6.dp));Text("导入本地表情",maxLines=1,softWrap=false)};FilledTonalButton(onClick={},Modifier.weight(1f).height(46.dp)){Icon(Icons.Outlined.Favorite,null);Spacer(Modifier.width(6.dp));Text("已收藏 ${favorites.size}",maxLines=1,softWrap=false)}} }
      if(all.isNotEmpty()) item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(12.dp)){Text("资源预览 · 共 ${all.size} 张",fontSize=13.sp,fontWeight=FontWeight.Medium);LazyRow(Modifier.fillMaxWidth().height(76.dp),contentPadding=PaddingValues(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(all,key={it}){path->MessageImage(path,Modifier.size(66.dp).clip(RoundedCornerShape(10.dp)).combinedClickable(onClick={stickerPreview=path},onLongClick={stickerPreview=path}),ContentScale.Fit)}}}} }
      item { SectionTitle("在线资源包") }
      StickerRepository.packs.forEach { pack ->
        item(key=pack.id) {
          val installed=StickerRepository.isInstalled(this@ComposeActivity,pack.id)
          val count=StickerRepository.count(this@ComposeActivity,pack.id)
          Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Collections,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(start=10.dp)){Text(pack.name,fontWeight=FontWeight.SemiBold);Text(if(installed)"已安装 $count 张" else pack.estimatedSize,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            Text(pack.description,Modifier.padding(top=9.dp),fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={openWebLink(pack.sourceUrl,"无法打开项目来源页面")},contentPadding=PaddingValues(0.dp)){Icon(Icons.Outlined.OpenInNew,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("查看项目来源",fontSize=12.sp)}
            if(downloading==pack.id){LinearProgressIndicator(modifier=Modifier.fillMaxWidth().padding(vertical=8.dp));Text(progressText,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            if(installed){val packImages=remember(uiRevision,pack.id){StickerRepository.imagesForPack(this@ComposeActivity,pack.id)};LazyRow(Modifier.fillMaxWidth().height(76.dp),contentPadding=PaddingValues(vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){items(packImages,key={it}){path->MessageImage(path,Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)).combinedClickable(onClick={stickerPreview=path},onLongClick={stickerPreview=path}),ContentScale.Fit)}}}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
              if(installed)OutlinedButton(onClick={askDanger("删除 ${pack.name}","将删除本机下载的 $count 张资源，聊天中已经发送的图片不会被删除。","确认删除"){StickerRepository.deletePack(this@ComposeActivity,pack.id);bitmapCache.evictAll();uiRevision++;toast("资源包已删除")}},Modifier.weight(1f).height(43.dp)){Icon(Icons.Outlined.Delete,null);Spacer(Modifier.width(5.dp));Text("删除",maxLines=1)}
              Button(onClick={if(downloading.isBlank()){downloading=pack.id;progressText="准备下载";Thread({runCatching{StickerRepository.download(this@ComposeActivity,pack.id){label->main.post{progressText=label}}}.onSuccess{main.post{downloading="";bitmapCache.evictAll();uiRevision++;toast("${pack.name} 已安装")}}.onFailure{error->main.post{downloading="";toast("下载失败：${error.message}")}}},"shiyu-sticker-download").start()}else toast("${StickerRepository.packs.firstOrNull{it.id==downloading}?.name?:"资源包"}正在下载，请稍候")},Modifier.weight(1f).height(43.dp),enabled=downloading!=pack.id){Icon(if(installed)Icons.Outlined.Refresh else Icons.Outlined.Download,null);Spacer(Modifier.width(5.dp));Text(if(downloading==pack.id)"下载中" else if(installed)"重新下载" else "下载",maxLines=1)}
            }
      }}
        }
      }
      item { InfoBanner(Icons.Outlined.Copyright,"第三方资源说明","资源由对应 GitHub 项目提供，版权和使用规则归原作者及素材权利人所有。时遇不售卖、不内置转载这些资源。") }
    }
    if(stickerPreview.isNotBlank())StickerPreviewDialog(stickerPreview){stickerPreview=""}
  }

  @Composable
  private fun RealityAwarenessPage() {
    var enabled by remember { mutableStateOf(prefs.getBoolean("reality_awareness_enabled", true)) }
    var includeTimezone by remember { mutableStateOf(prefs.getBoolean("reality_include_timezone", true)) }
    var weatherEnabled by remember { mutableStateOf(prefs.getBoolean("moji_weather_enabled", false)) }
    var weatherCityId by remember { mutableStateOf(prefs.getString("moji_weather_city_id", "").orEmpty()) }
    var weatherCityName by remember { mutableStateOf(prefs.getString("moji_weather_city_name", "").orEmpty()) }
    var weatherAppCode by remember { mutableStateOf(secure.getNamed(MojiWeatherClient.APP_CODE_KEY)) }
    var weatherEndpoint by remember { mutableStateOf(prefs.getString("moji_weather_endpoint", MojiWeatherClient.DEFAULT_CURRENT_ENDPOINT).orEmpty()) }
    var weatherForecastEndpoint by remember { mutableStateOf(prefs.getString("moji_weather_forecast_endpoint", MojiWeatherClient.DEFAULT_FORECAST_ENDPOINT).orEmpty()) }
    var weatherBusy by remember { mutableStateOf(false) }
    val preview=remember{java.text.SimpleDateFormat("yyyy年M月d日 EEEE HH:mm",java.util.Locale.CHINA).format(java.util.Date())}
    DetailScaffold("现实感知增强") {
      item { InfoBanner(Icons.Outlined.Public,"现实信息与天气","角色会获得当前时间；开启墨迹天气后，角色在需要时可以主动查询你配置城市的实时天气和预报。") }
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("启用现实感知",fontWeight=FontWeight.Medium);Text("例如知道今天日期、现在是早晨还是夜晚",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(enabled,{enabled=it;prefs.edit().putBoolean("reality_awareness_enabled",it).apply()})}} }
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("包含设备时区",fontWeight=FontWeight.Medium);Text(java.util.TimeZone.getDefault().id,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(includeTimezone,{includeTimezone=it;prefs.edit().putBoolean("reality_include_timezone",it).apply()},enabled=enabled)}} }
      item { GuideStep("当前","时间预览",preview) }
      item { SectionTitle("墨迹天气 API") }
      item { Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色查询天气",fontWeight=FontWeight.Medium);Text("通过阿里云市场墨迹天气接口获取实时结果，不会自动定位",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(weatherEnabled,{weatherEnabled=it;if(!it)prefs.edit().putBoolean("moji_weather_enabled",false).apply()})}
        if(weatherEnabled){
          StyledField(weatherCityName,{weatherCityName=it.take(60)},"城市名称（用于提示）")
          StyledField(weatherCityId,{weatherCityId=it.filter{ch->ch.isDigit()}.take(12)},"墨迹城市 ID")
          OutlinedTextField(weatherAppCode,{weatherAppCode=it.take(180)},Modifier.fillMaxWidth(),label={Text("阿里云市场 AppCode")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),shape=RoundedCornerShape(13.dp))
          StyledField(weatherEndpoint,{weatherEndpoint=it.take(500)},"实时天气接口（可用默认地址）")
          StyledField(weatherForecastEndpoint,{weatherForecastEndpoint=it.take(500)},"预报接口（可用默认地址）")
          Text("在阿里云市场开通墨迹天气后，将 AppCode 和城市 ID 填入此处。聊天中角色会按需输出 [查询天气] 或 [查询天气：预报]。",fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick={if(weatherCityId.isBlank()||weatherAppCode.isBlank())toast("请先填写城市 ID 和 AppCode")else{runCatching{secure.putNamed(MojiWeatherClient.APP_CODE_KEY,weatherAppCode.trim())}.onFailure{toast("AppCode 临时保存失败");return@OutlinedButton};prefs.edit().putBoolean("moji_weather_enabled",true).putString("moji_weather_city_id",weatherCityId.trim()).putString("moji_weather_city_name",weatherCityName.trim()).putString("moji_weather_endpoint",weatherEndpoint.trim().ifBlank{MojiWeatherClient.DEFAULT_CURRENT_ENDPOINT}).putString("moji_weather_forecast_endpoint",weatherForecastEndpoint.trim().ifBlank{MojiWeatherClient.DEFAULT_FORECAST_ENDPOINT}).apply();weatherBusy=true;pool.execute{runCatching{MojiWeatherClient.queryConfigured(this@ComposeActivity,false)}.onSuccess{value->main.post{weatherBusy=false;toast("天气查询成功：${value.lineSequence().firstOrNull().orEmpty()}")}}.onFailure{error->main.post{weatherBusy=false;toast("天气查询失败：${error.message?:friendlyError(error)}")}}}}},enabled=!weatherBusy,modifier=Modifier.weight(1f).height(44.dp)){if(weatherBusy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Cloud,null);Spacer(Modifier.width(6.dp));Text(if(weatherBusy)"查询中" else "测试天气",maxLines=1,softWrap=false)}
            Button(onClick={if(weatherCityId.isBlank()||weatherAppCode.isBlank())toast("请填写城市 ID 和 AppCode")else{runCatching{secure.putNamed(MojiWeatherClient.APP_CODE_KEY,weatherAppCode.trim())}.onFailure{toast("AppCode 保存失败");return@Button};prefs.edit().putBoolean("moji_weather_enabled",true).putString("moji_weather_city_id",weatherCityId.trim()).putString("moji_weather_city_name",weatherCityName.trim()).putString("moji_weather_endpoint",weatherEndpoint.trim().ifBlank{MojiWeatherClient.DEFAULT_CURRENT_ENDPOINT}).putString("moji_weather_forecast_endpoint",weatherForecastEndpoint.trim().ifBlank{MojiWeatherClient.DEFAULT_FORECAST_ENDPOINT}).apply();toast("墨迹天气设置已保存")}},modifier=Modifier.weight(1f).height(44.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存天气",maxLines=1,softWrap=false)}
          }
        } else Text("关闭后角色不会调用天气接口。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }}}
      item { InfoBanner(Icons.Outlined.PrivacyTip,"隐私边界","当前时间只来自设备时钟；天气仅发送城市 ID 到你配置的墨迹天气接口，应用不会读取定位、日历、联系人或其他 App 数据。") }
    }
  }

  // ===== 哄睡故事 =====
  data class StoryItem(val id:String,val title:String,val content:String,val rate:Float,val pitch:Float,val voice:String,val createdAt:Long)

  private fun stories():List<StoryItem>{
    val raw=prefs.getString("bedtime_stories","").orEmpty()
    if(raw.isBlank())return emptyList()
    return runCatching{
      val array=JSONArray(raw)
      buildList{for(i in 0 until array.length()){val item=array.optJSONObject(i)?:continue;add(StoryItem(item.optString("id"),item.optString("title","未命名故事"),item.optString("content"),item.optDouble("rate",0.8).toFloat(),item.optDouble("pitch",1.0).toFloat(),item.optString("voice"),item.optLong("createdAt",0L)))}}
    }.getOrElse{emptyList()}
  }
  private fun saveStories(values:List<StoryItem>){
    val array=JSONArray()
    values.forEach{array.put(JSONObject().put("id",it.id).put("title",it.title).put("content",it.content).put("rate",it.rate.toDouble()).put("pitch",it.pitch.toDouble()).put("voice",it.voice).put("createdAt",it.createdAt))}
    prefs.edit().putString("bedtime_stories",array.toString()).apply()
  }
  private fun splitStorySegments(content:String):List<String>{
    val parts=content.replace("\r","").split(Regex("(?<=[。！？!?…\n])"))
    val out=ArrayList<String>();val buffer=StringBuilder()
    for(part in parts){val piece=part.trim();if(piece.isEmpty())continue
      if(buffer.isNotEmpty()&&buffer.length+piece.length>200){out.add(buffer.toString());buffer.setLength(0)}
      buffer.append(piece)
      if(buffer.length>=200){out.add(buffer.toString());buffer.setLength(0)}
    }
    if(buffer.isNotEmpty())out.add(buffer.toString())
    return out.ifEmpty{listOf(content)}
  }
  private fun ensureStoryTts(){
    if(storyTts!=null)return
    storyTts=android.speech.tts.TextToSpeech(applicationContext){status->
      storyTtsReady=status==android.speech.tts.TextToSpeech.SUCCESS
      if(storyTtsReady){runCatching{storyTts?.language=java.util.Locale.SIMPLIFIED_CHINESE}}
      else{storyTts=null;main.post{if(storyPlaying){storyPlaying=false;prefs.edit().putBoolean("story_playing",false).apply()};toast("语音引擎初始化失败")}}
    }
    storyTts?.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){
      override fun onStart(utteranceId:String?){}
      override fun onDone(utteranceId:String?){main.post{onStorySegmentDone(utteranceId.orEmpty())}}
      override fun onError(utteranceId:String?){}
      @Deprecated("Deprecated in Java") override fun onError(utteranceId:String?,errorCode:Int){main.post{onStorySegmentDone(utteranceId.orEmpty())}}
    })
  }
  private fun startStoryPlayback(story:StoryItem){
    speech.stop()
    storyGeneration++
    storyPlaybackId=story.id;storyPlaybackTitle=story.title
    storySegments=splitStorySegments(story.content);storySegmentIndex=0
    storyPlaying=true;prefs.edit().putBoolean("story_playing",true).apply()
    runCatching{NeteaseMusicService.stop(this)}
    ensureStoryTts();uiRevision++
    if(storyTtsReady)speakStorySegment()
  }
  private fun stopStoryPlayback(){
    storyGeneration++
    runCatching{storyTts?.stop()}
    storyPlaying=false;storyPlaybackId="";storyPlaybackTitle="";storySegments=emptyList();storySegmentIndex=0
    prefs.edit().putBoolean("story_playing",false).apply()
    uiRevision++
  }
  private fun speakStorySegment(){
    if(!storyPlaying)return
    val engine=storyTts
    if(engine==null||!storyTtsReady)return
    if(storySegmentIndex>=storySegments.size){onStoryFinished();return}
    val story=stories().firstOrNull{it.id==storyPlaybackId}?:run{stopStoryPlayback();return}
    runCatching{
      engine.setSpeechRate(story.rate.coerceIn(0.5f,2f))
      engine.setPitch(story.pitch.coerceIn(0.5f,2f))
      if(story.voice.isNotBlank())engine.voices?.firstOrNull{it.name==story.voice}?.let{engine.voice=it}
      engine.speak(storySegments[storySegmentIndex],android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"story_${storyGeneration}_$storySegmentIndex")
      storySegmentIndex++
    }.onFailure{stopStoryPlayback()}
  }
  private fun onStorySegmentDone(utteranceId:String){
    if(!storyPlaying||!utteranceId.startsWith("story_${storyGeneration}_"))return
    if(storySegmentIndex>=storySegments.size)onStoryFinished()else speakStorySegment()
  }
  private fun onStoryFinished(){
    val mode=prefs.getString("story_play_mode","off").orEmpty().ifBlank{"off"}
    if(mode=="one"&&storyPlaybackId.isNotBlank()){storyGeneration++;storySegmentIndex=0;speakStorySegment();return}
    val list=stories()
    if(mode=="all"&&list.size>1){
      val index=list.indexOfFirst{it.id==storyPlaybackId}
      startStoryPlayback(list[(index+1).mod(list.size)]);return
    }
    stopStoryPlayback()
  }
  private fun previewStory(rate:Float,pitch:Float,content:String){
    speech.stop();storyGeneration++
    storyPlaying=false;prefs.edit().putBoolean("story_playing",false).apply()
    ensureStoryTts()
    pool.execute{
      var waited=0
      while((storyTts==null||!storyTtsReady)&&waited<3000){Thread.sleep(120);waited+=120}
      main.post{
        val engine=storyTts
        if(engine==null||!storyTtsReady)toast("语音引擎初始化失败")
        else runCatching{engine.setSpeechRate(rate.coerceIn(0.5f,2f));engine.setPitch(pitch.coerceIn(0.5f,2f));engine.speak(content.take(90).ifBlank{"这是试听示例。"},android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"story_preview_${System.currentTimeMillis()}")}
      }
    }
  }

  @Composable
  private fun StoriesPage(){
    val list=remember(uiRevision){stories()}
    var playMode by remember{mutableStateOf(prefs.getString("story_play_mode","off").orEmpty().ifBlank{"off"})}
    DetailScaffold("哄睡故事",Page.MAIN){
      item{InfoBanner(Icons.Outlined.Nightlight,"让角色读故事哄你入睡","编辑故事并调试语速音色，保存后可单曲或列表循环播放；长时间播放建议开启通知保活并保持时遇在后台运行。")}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FilterChip(selected=playMode=="off",onClick={playMode="off";prefs.edit().putString("story_play_mode","off").apply()},label={Text("播完停止",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))
        FilterChip(selected=playMode=="one",onClick={playMode="one";prefs.edit().putString("story_play_mode","one").apply()},label={Text("单曲循环",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))
        FilterChip(selected=playMode=="all",onClick={playMode="all";prefs.edit().putString("story_play_mode","all").apply()},label={Text("列表循环",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))
      }}
      if(storyPlaying)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.secondaryContainer){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Filled.GraphicEq,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.secondary)
        Column(Modifier.weight(1f).padding(start=10.dp)){Text(storyPlaybackTitle.ifBlank{"正在播放"},fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("第 ${storySegmentIndex.coerceAtLeast(1)} / ${storySegments.size} 段",fontSize=11.sp,color=MaterialTheme.colorScheme.onSecondaryContainer)}
        TextButton(onClick={stopStoryPlayback()}){Text("停止")}
      }}}
      if(list.isEmpty())item{EmptyState(Icons.Outlined.Nightlight,"还没有保存的故事","点击下方按钮，写一个让角色读给你听的故事")}
      items(list,key={it.id}){story->
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
          Row(Modifier.fillMaxWidth().padding(start=8.dp,end=12.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick={if(storyPlaying&&storyPlaybackId==story.id)stopStoryPlayback()else startStoryPlayback(story)},modifier=Modifier.size(44.dp)){Icon(if(storyPlaying&&storyPlaybackId==story.id)Icons.Filled.Stop else Icons.Filled.PlayArrow,if(storyPlaying&&storyPlaybackId==story.id)"停止" else "播放",tint=MaterialTheme.colorScheme.primary)}
            Column(Modifier.weight(1f)){Text(story.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(story.content.replace("\n"," ").take(60),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("语速 ${String.format(java.util.Locale.US,"%.1f",story.rate)}x · 音调 ${String.format(java.util.Locale.US,"%.1f",story.pitch)} · ${splitStorySegments(story.content).size} 段",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            TextButton(onClick={storyEditingId=story.id;composePage=Page.STORY_EDIT}){Text("编辑")}
            IconButton(onClick={askDanger("删除故事","确定删除《${story.title}》吗？删除后不可恢复。","删除"){saveStories(stories().filterNot{it.id==story.id});uiRevision++;toast("故事已删除")}}){Icon(Icons.Outlined.Delete,null,tint=MaterialTheme.colorScheme.error)}
          }
        }
      }
      item{Button(onClick={storyEditingId=null;composePage=Page.STORY_EDIT},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(7.dp));Text("新建故事")}}
    }
  }

  @Composable
  private fun StoryEditPage(){
    val existing=remember(storyEditingId){storyEditingId?.let{id->stories().firstOrNull{it.id==id}}}
    var title by remember(existing){mutableStateOf(existing?.title.orEmpty())}
    var content by remember(existing){mutableStateOf(existing?.content.orEmpty())}
    var rate by remember(existing){mutableFloatStateOf(existing?.rate?:0.8f)}
    var pitch by remember(existing){mutableFloatStateOf(existing?.pitch?:1f)}
    var generating by remember(existing){mutableStateOf(false)}
    var roleMenu by remember{mutableStateOf(false)}
    var selectedRoleId by remember(existing){mutableStateOf(cards.firstOrNull()?.id.orEmpty())}
    val selectedRole=cards.firstOrNull{it.id==selectedRoleId}?:cards.firstOrNull()
    fun generateStory(){
      val target=selectedRole
      if(target==null){toast("请先创建或导入一个角色");return}
      if(activeProvider()==null){toast("请先在模型设置中配置可用的模型供应商");return}
      if(generating)return
      generating=true
      pool.execute{
        runCatching{
          val prompt="你是角色“${target.name}”。请完全依据你的角色设定，为我创作一篇适合睡前聆听的中文哄睡故事。故事要温柔、安静、节奏舒缓，避免惊吓、暴力和强烈冲突，约 800 到 1600 字。可以自然使用第二人称“你”，让听故事的人感到被陪伴。只输出故事正文，不要标题、角色名前缀、解释、Markdown、JSON 或系统提示。"
          complete(listOf(Msg("user",prompt)),target).trim().replace(Regex("^```(?:text|markdown)?\\s*|\\s*```$"),"").trim().also{require(it.isNotBlank()){ "角色没有生成故事内容" }}
        }.onSuccess{value->main.post{content=value.take(20000);if(title.isBlank())title="${target.name}的哄睡故事";generating=false;toast("${target.name} 已生成哄睡故事，请试听或保存")}}.onFailure{error->main.post{generating=false;recordApiError(error);toast("故事生成失败：${friendlyError(error)}")}}
      }
    }
    DetailScaffold("编辑哄睡故事",Page.STORIES){
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.secondaryContainer){Column(Modifier.fillMaxWidth().padding(14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.AutoStories,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.weight(1f).padding(start=9.dp)){Text("让角色生成故事",fontWeight=FontWeight.SemiBold);Text(if(selectedRole!=null)"当前角色：${selectedRole.name}" else "请先创建或导入角色",fontSize=11.sp,color=MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=.78f))}}
        if(cards.isNotEmpty()){OutlinedButton(onClick={roleMenu=true},modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(42.dp)){Icon(Icons.Outlined.Person,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("生成角色：${selectedRole?.name?:"请选择"}",Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Icon(Icons.Outlined.ExpandMore,null,Modifier.size(18.dp))};DropdownMenu(expanded=roleMenu,onDismissRequest={roleMenu=false}){cards.forEach{candidate->DropdownMenuItem(text={Text(candidate.name)},onClick={selectedRoleId=candidate.id;roleMenu=false})}}}
        Button(onClick={generateStory()},enabled=!generating&&selectedRole!=null,modifier=Modifier.fillMaxWidth().padding(top=8.dp).height(44.dp)){if(generating)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(if(generating)"角色正在写故事" else "让角色生成哄睡故事",maxLines=1,softWrap=false)}
      }}}
      item{StyledField(title,{title=it.take(60)},"故事标题")}
      item{OutlinedTextField(content,{content=it.take(20000)},Modifier.fillMaxWidth().heightIn(min=200.dp,max=420.dp),label={Text("故事正文")},placeholder={Text("写下想让角色读的内容，会按句子自动分段朗读")},minLines=6,maxLines=14,shape=RoundedCornerShape(14.dp))}
      item{Text("${content.length} / 20000 字 · 约 ${splitStorySegments(content).size} 段",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=12.dp))}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){
        Text("语速 ${String.format(java.util.Locale.US,"%.1f",rate)}x",fontWeight=FontWeight.Medium);Text("哄睡建议 0.6 - 0.9，越慢越安静",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(rate,{rate=it},valueRange=0.5f..1.5f,steps=4)
        Spacer(Modifier.height(10.dp))
        Text("音调 ${String.format(java.util.Locale.US,"%.1f",pitch)}",fontWeight=FontWeight.Medium);Text("偏低更沉稳，适合睡前",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(pitch,{pitch=it},valueRange=0.5f..1.5f,steps=4)
      }}}
      item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
        OutlinedButton(onClick={if(content.isBlank())toast("请先填写故事正文")else previewStory(rate,pitch,content)},modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.PlayCircle,null);Spacer(Modifier.width(6.dp));Text("试听开头",maxLines=1,softWrap=false)}
        Button(onClick={if(title.isBlank())toast("请填写故事标题")else if(content.isBlank())toast("请填写故事正文")else{val values=stories().toMutableList();val id=existing?.id?:java.util.UUID.randomUUID().toString();values.removeAll{it.id==id};values.add(0,StoryItem(id,title.trim(),content,rate,pitch,existing?.voice.orEmpty(),existing?.createdAt?:System.currentTimeMillis()));saveStories(values);uiRevision++;toast("故事已保存");composePage=Page.STORIES}},modifier=Modifier.weight(1f).height(48.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存故事",maxLines=1,softWrap=false)}
      }}
      if(existing!=null)item{OutlinedButton(onClick={askDanger("删除故事","确定删除《${existing.title}》吗？","删除"){saveStories(stories().filterNot{it.id==existing.id});uiRevision++;toast("故事已删除");composePage=Page.STORIES}},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Delete,null,tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(6.dp));Text("删除这个故事",color=MaterialTheme.colorScheme.error)}}
    }
  }

  @Composable
  private fun RecommendCard(){
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f),modifier=Modifier.fillMaxWidth()){
      Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
        Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary.copy(alpha=.14f),modifier=Modifier.size(34.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Outlined.Favorite,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)}}
        Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text("如果你喜欢时遇",fontWeight=FontWeight.SemiBold,maxLines=1,softWrap=false);Text("不妨推荐给朋友吧",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,softWrap=false)}
        IconButton(onClick={shareApp()},modifier=Modifier.size(36.dp)){Icon(Icons.Outlined.Share,"分享给朋友",Modifier.size(19.dp),tint=MaterialTheme.colorScheme.primary)}
        IconButton(onClick={copyText("时遇链接","https://shiy.tniay.top/")},modifier=Modifier.size(36.dp)){Icon(Icons.Outlined.Link,"复制链接",Modifier.size(19.dp),tint=MaterialTheme.colorScheme.primary)}
      }
    }
  }
  private fun extractFileText(file:java.io.File,mime:String):String{
    val name=file.name.lowercase()
    val textLike=mime.startsWith("text/")||mime.contains("json")||mime.contains("xml")||mime.contains("javascript")||mime.contains("sql")
      ||listOf("txt","md","markdown","csv","log","json","xml","html","htm","kt","java","py","js","ts","css","yml","yaml","ini","conf","sql","sh","c","cpp","h","hpp","cs","go","rs","rb","php","swift","toml","vtt","srt").any{suffix->name.endsWith("."+suffix)}
    if(!textLike)return ""
    return runCatching{
      val bytes=file.readBytes()
      val limit=100*1024
      if(bytes.size>limit)String(bytes,0,limit,Charsets.UTF_8)+"\n…（内容过长已截断）" else String(bytes,Charsets.UTF_8)
    }.getOrElse{""}
  }
  private fun openChatFile(path:String){
    runCatching{
      val uri=if(path.startsWith("content:"))android.net.Uri.parse(path)else androidx.core.content.FileProvider.getUriForFile(this,"${packageName}.files",java.io.File(path))
      val intent=Intent(Intent.ACTION_VIEW).setDataAndType(uri,guessFileMime(path).ifBlank{"*/*"}).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      startActivity(intent)
    }.onFailure{safeToast("无法打开文件（可能缺少支持的应用）")}
  }
  private fun guessFileMime(path:String):String{
    if(path.startsWith("content:"))return ""
    val name=path.lowercase()
    return when{
      name.endsWith(".pdf")->"application/pdf";name.endsWith(".txt")->"text/plain";name.endsWith(".md")->"text/markdown"
      name.endsWith(".html")||name.endsWith(".htm")->"text/html";name.endsWith(".json")->"application/json"
      name.endsWith(".xml")->"application/xml";name.endsWith(".csv")->"text/csv"
      name.endsWith(".mp3")->"audio/mpeg";name.endsWith(".wav")->"audio/wav";name.endsWith(".mp4")->"video/mp4"
      name.endsWith(".png")->"image/png";name.endsWith(".jpg")||name.endsWith(".jpeg")->"image/jpeg"
      name.endsWith(".gif")->"image/gif";name.endsWith(".webp")->"image/webp";name.endsWith(".zip")->"application/zip"
      name.endsWith(".doc")->"application/msword"
      name.endsWith(".docx")->"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      else->"*/*"}
  }
  private fun shareApp(){
    val text="我在用「时遇」和角色聊天、一起生活，免费又好玩的 AI 陪伴应用，推荐给你试试：https://shiy.tniay.top/"
    runCatching{startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"分享时遇"))}.onFailure{safeToast("分享失败")}
  }

  @Composable
  private fun AboutPage() {
    var checking by remember{mutableStateOf(false)}
    val updateVersion=prefs.getString("update_version","")?:""
    val updateUrl=prefs.getString("update_url","")?:""
    DetailScaffold("关于时遇") {
      item { Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Image(painterResource(R.drawable.ic_launcher_foreground),"时遇图标",Modifier.size(82.dp).clip(RoundedCornerShape(18.dp)));Spacer(Modifier.height(14.dp));Text("时遇",fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer);Text("在故事里，遇见彼此",fontSize=13.sp,color=MaterialTheme.colorScheme.onPrimaryContainer)}} }
      item { SectionTitle("应用信息") }
      item { SettingButton(Icons.Outlined.Info,"当前版本","${BuildConfig.VERSION_NAME} · Android Compose"){} }
      item { SettingButton(Icons.Outlined.History,"更新历史","查看不同版本的新增内容与问题修复"){composePage=Page.UPDATE_HISTORY} }
      item { SettingButton(Icons.Outlined.Language,"时遇官网","shiy.tniay.top"){openWebLink("https://shiy.tniay.top/","无法打开时遇官网")} }
      item { Button(onClick={checking=true;checkForUpdates{checking=false}},Modifier.fillMaxWidth().height(48.dp)){if(checking)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.SystemUpdateAlt,null);Spacer(Modifier.width(8.dp));Text(if(checking)"正在检查更新" else "检查更新",maxLines=1,softWrap=false)} }
      if(updateVersion.isNotBlank())item { SettingButton(Icons.Outlined.NewReleases,"发现新版本 $updateVersion",updateUrl.ifBlank{"时遇官网"}){openWebLink(updateUrl.ifBlank{"https://shiy.tniay.top/"},"无法打开更新页面")} }
      item { SettingButton(Icons.Outlined.Gavel,"用户协议与使用条款","免费 App、第三方 API、隐私与内容规范"){termsBackPage=Page.ABOUT;composePage=Page.TERMS} }
      item { SettingButton(Icons.Outlined.Groups,"时遇交流群","1061590432 · 点击复制"){copyText("交流群号","1061590432")} }
      item { SettingButton(Icons.Outlined.Person,"作者","浅月tniay"){} }
      item { SettingButton(Icons.Outlined.AccountCircle,"作者 QQ","3581378884 · 点击复制"){copyText("作者 QQ","3581378884")} }
      item { RecommendCard() }
      item { SectionTitle("时遇功能大全") }
      item { GuideStep("聊天","角色对话","私聊、群聊、主动消息、长期记忆、语音回复与多媒体消息。") }
      item { GuideStep("角色","角色状态","按会话查看好感、信任、心情、精力、疲惫、兴趣与关系足迹。") }
      item { GuideStep("音乐","一起听歌","登录网易云后搜索歌曲和歌单，在一个私聊会话中和当前角色边听边聊。") }
      item { GuideStep("世界","游戏与活动","邀请角色玩五子棋、井字棋、抽鬼牌，或一起学习与旅行。") }
      item { GuideStep("内容","朋友圈与日记","角色可以查看、发布、评论动态，也能记录和分享日记。") }
      item { GuideStep("扩展","桌宠与外部连接","使用桌宠、语音通话、屏幕分享，并可接入微信或 QQ Bot。") }
      item { GuideStep("数据","本地与备份","数据默认保存在设备，可加密备份、恢复，并在异常时导出抢救包。") }
      item { GuideStep("隐私","本地优先","聊天内容不会上传到时遇服务器，仅发送给你配置的 API。") }
      item { GuideStep("开放","兼容接口","支持 OpenAI Chat Completions、模型列表和流式输出。") }
      item { GuideStep("设计","时遇风格","珊瑚色代表情感，青绿色代表连接，灰黑夜间模式降低视觉压力。") }
    }
  }

  @Composable
  private fun UpdateHistoryPage() {
    var revision by remember{mutableIntStateOf(0)}
    var refreshing by remember{mutableStateOf(false)}
    val cached=remember(revision){ReleaseHistory.fromJson(prefs.getString("release_history_cache","").orEmpty())}
    val releases=if(cached.isEmpty())ReleaseHistory.bundled() else cached
    val historyError=remember(revision){prefs.getString("release_history_error","").orEmpty()}
    LaunchedEffect(Unit){if(cached.isEmpty()){refreshing=true;checkForUpdates{refreshing=false;revision++}}}
    DetailScaffold("更新历史",Page.ABOUT) {
      item { InfoBanner(Icons.Outlined.Language,"官网版本记录","历史版本和后续新版本均从时遇官网获取，并缓存在本机供离线查看。") }
      item{OutlinedButton(onClick={refreshing=true;checkForUpdates{refreshing=false;revision++}},enabled=!refreshing,modifier=Modifier.fillMaxWidth().height(46.dp)){if(refreshing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(7.dp));Text(if(refreshing)"正在同步版本记录" else "刷新版本记录",maxLines=1,softWrap=false)}}
      if(historyError.isNotBlank()&&cached.isEmpty())item{InfoBanner(Icons.Outlined.ErrorOutline,"官网记录加载失败",historyError)}
      releases.forEach{release->
        val comparison=ReleaseHistory.compare(release.version,BuildConfig.VERSION_NAME)
        val status=when{comparison>0->"可更新";comparison==0->"当前版本";else->"历史版本"}
        item { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(if(comparison>=0)Icons.Outlined.NewReleases else Icons.Outlined.History,null,tint=if(comparison>=0)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant);Column(Modifier.weight(1f).padding(start=10.dp)){Text("时遇 ${release.version}",fontWeight=FontWeight.SemiBold);Text(release.publishedAt,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(status,fontSize=11.sp,color=if(comparison>=0)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,softWrap=false)}
          release.notes.forEach{note->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){Text("•",color=MaterialTheme.colorScheme.primary);Text(note,Modifier.padding(start=8.dp),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        }} }
      }
    }
  }

  @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
  @Composable
  private fun ChatPage(chat: Chat) {
    chatUiRevision
    val card = find(chat.activeCardId.takeIf { it.isNotBlank() } ?: chat.cardId) ?: return
    val thisSending=isChatSending(chat.id)
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
    var showLocation by remember{mutableStateOf(false)}
    var locationName by remember{mutableStateOf("")};var locationAddress by remember{mutableStateOf("")};var locationLat by remember{mutableStateOf("")};var locationLng by remember{mutableStateOf("")}
    var showTransfer by remember{mutableStateOf(false)};var transferAmount by remember{mutableStateOf("")};var transferNote by remember{mutableStateOf("")}
    var showMusic by remember{mutableStateOf(false)};var musicTitle by remember{mutableStateOf("")};var musicArtist by remember{mutableStateOf("")};var musicUrl by remember{mutableStateOf("")};var musicSearchQuery by remember{mutableStateOf("")};var musicSearchBusy by remember{mutableStateOf(false)};var musicSearchError by remember{mutableStateOf("")};var musicSearchSongs by remember{mutableStateOf<List<NeteaseSong>>(emptyList())}
    var showGift by remember{mutableStateOf(false)};var giftName by remember{mutableStateOf("")};var giftWish by remember{mutableStateOf("")};var giftImage by remember{mutableStateOf("")}
    var showShareChat by remember{mutableStateOf(false)}
    var showVoiceCall by remember{mutableStateOf(false)};var voiceCallNote by remember{mutableStateOf("")}
    var showImageGen by remember{mutableStateOf(false)}
    var imageGenPositive by rememberSaveable(chat.id){mutableStateOf("")}
    var imageGenNegative by rememberSaveable(chat.id){mutableStateOf("")}
    var selectedStickerGroup by rememberSaveable(chat.id) { mutableStateOf("") }
    var menuMessage by remember { mutableStateOf<Msg?>(null) }
    var editMessage by remember { mutableStateOf<Msg?>(null) }
    var editImagePrompt by remember { mutableStateOf<Msg?>(null) }
    var imagePreview by remember { mutableStateOf("") }
    var stickerPreview by remember { mutableStateOf("") }
    var editText by remember { mutableStateOf("") }
    var smoothScrollRequest by remember(chat.id) { mutableIntStateOf(0) }
    var followStreamingAtBottom by remember(chat.id) { mutableStateOf(true) }
    val clipboard=LocalClipboardManager.current
    val chatBackground=storedColor(chat.backgroundColor.ifBlank{prefs.getString("chat_background_color","").orEmpty()},MaterialTheme.colorScheme.background)
    val chatBackgroundImage=chat.backgroundImage.ifBlank{prefs.getString("chat_background_image","").orEmpty()}
    val chatBackgroundOverlay=(if(chat.backgroundOverlay>=0)chat.backgroundOverlay else prefs.getInt("chat_background_overlay",20)).coerceIn(0,70)/100f
    val backgroundFollowKeyboard=prefs.getBoolean("chat_background_follow_ime",false)
    val imeBottom=WindowInsets.ime.getBottom(LocalDensity.current)
    val backgroundKeyboardOffset=if(backgroundFollowKeyboard)with(LocalDensity.current){imeBottom.toDp()}else 0.dp
    val backgroundLayerModifier=Modifier.fillMaxSize().graphicsLayer{translationY=-backgroundKeyboardOffset.toPx()}
    val transparentTop=prefs.getBoolean("experimental_top_bar_transparent",false)
    val topBarDarkContent=remember(chatBackground,chatBackgroundImage,chatBackgroundOverlay){chatStatusBarDarkIcons(chatBackground,chatBackgroundImage,chatBackgroundOverlay)}
    val topBarContentColor=if(topBarDarkContent)Color(0xFF252326)else Color.White
    val glassInput=prefs.getBoolean("experimental_input_glass",false)
    val suggestionPosition=prefs.getString("reply_suggestion_position","bubble")?:"bubble"
    val bubbleActionMode=prefs.getString("chat_bubble_actions","latest")?:"latest"
    val activeVoiceCall=voiceCallSessionId.isNotBlank()&&voiceCallChatId==chat.id&&!voiceCallFloating
    val visibleMessages=chat.messages.filterNot{chat.immersiveMode&&!chat.group&&it.loading&&it.messageType!="image_request"}
    val suggestionAnchorId=visibleMessages.lastOrNull{it.role=="assistant"&&!it.loading&&!it.failed&&(it.body.isNotBlank()||it.image.isNotBlank())}?.id
    val actionAnchorId=visibleMessages.lastOrNull{it.role=="assistant"&&!it.loading}?.id
    val listState = rememberLazyListState(initialFirstVisibleItemIndex=visibleMessages.lastIndex.coerceAtLeast(0))
    DisposableEffect(chat.id){prefs.edit().putString("visible_chat_id",chat.id).apply();onDispose{if(prefs.getString("visible_chat_id","")==chat.id)prefs.edit().remove("visible_chat_id").apply();if(voiceCallChatId!=chat.id){if(voiceRecording)cancelOnlineVoiceRecording();if(offlineVoiceBusy())cancelOfflineVoiceRecognition()}}}
    val userDragging by listState.interactionSource.collectIsDraggedAsState()
    var navigationSettled by rememberSaveable(chat.id){mutableStateOf(false)}
    var initialPositionApplied by rememberSaveable(chat.id){mutableStateOf(false)}
    val lastMessageLength = chat.messages.lastOrNull()?.body?.length ?: 0
    val latestConversationKey=chat.messages.lastOrNull()?.let{"${it.id}:${it.body.length}:${it.loading}"}.orEmpty()
    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};pendingImage=uri.toString();pendingSticker=false}}
    var pendingFile by remember(chat.id){mutableStateOf<JSONObject?>(null)}
    var pendingFileBusy by remember(chat.id){mutableStateOf(false)}
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){
      pendingFileBusy=true
      pool.execute{runCatching{
        val resolver=contentResolver
        var name=uri.lastPathSegment?:"文件"
        runCatching{resolver.query(uri,null,null,null,null)?.use{cursor->val idx=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(cursor.moveToFirst()&&idx>=0)name=cursor.getString(idx)?:"文件"}}
        var size=-1L
        runCatching{resolver.query(uri,null,null,null,null)?.use{cursor->val idx=cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);if(cursor.moveToFirst()&&idx>=0)size=cursor.getLong(idx)}}
        val mime=runCatching{resolver.getType(uri)}.getOrNull().orEmpty()
        require(size<0||size<=20*1024*1024L){"文件超过 20MB 上限"}
        val directory=File(filesDir,"chat_files");if(!directory.exists()&&!directory.mkdirs())error("无法创建附件目录")
        val safeName=name.replace(Regex("[\\/:*?\"<>|]"),"_").take(80)
        val target=File(directory,""+System.currentTimeMillis()+"_"+safeName)
        resolver.openInputStream(uri)?.use{input->target.outputStream().use{output->input.copyTo(output)}}?:error("无法读取所选文件")
        val extracted=extractFileText(target,mime)
        main.post{pendingFile=JSONObject().put("path",target.absolutePath).put("name",safeName).put("size",if(size>=0)size else target.length()).put("mime",mime).put("text",extracted);pendingFileBusy=false;toast("附件已添加，可直接发送")}
      }.onFailure{main.post{pendingFileBusy=false;toast("附件添加失败："+(it.message?:"未知错误"))}}}
    }}
    val giftImagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};giftImage=uri.toString()}}
    var pendingCameraPath by remember(chat.id){mutableStateOf("")}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){saved->val path=pendingCameraPath;pendingCameraPath="";if(saved&&path.isNotBlank()&&File(path).isFile){pendingImage=path;pendingSticker=false;uiRevision++;toast("照片已添加，可直接发送给 AI 识别")}else{path.takeIf{it.isNotBlank()}?.let{runCatching{File(it).delete()}};if(!saved)toast("已取消拍照")}}
    fun takePhoto(){runCatching{val directory=File(filesDir,"chat_photos");if(!directory.exists()&&!directory.mkdirs())error("无法创建照片目录");val photo=File(directory,"photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg");pendingCameraPath=photo.absolutePath;val uri=androidx.core.content.FileProvider.getUriForFile(this@ComposeActivity,"$packageName.files",photo);camera.launch(uri)}.onFailure{pendingCameraPath="";toast("无法打开相机：${it.message}")}}
    val voiceInput=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->if(result.resultCode==Activity.RESULT_OK){val words=result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);words?.firstOrNull()?.let{text=if(text.isBlank())it else "$text $it"}}}
    fun appendRecognized(value:String){if(value.isNotBlank())text=if(text.isBlank())value.trim() else "$text ${value.trim()}"}
    fun startOfflineInput(){val started=startOfflineVoiceRecognition(object:OfflineSpeechCallback{override fun onStateChanged(){uiRevision++};override fun onResult(value:String){appendRecognized(value);uiRevision++};override fun onError(error:String){uiRevision++;toast("语音识别失败：$error")}});if(!started)toast("无法启动本地语音识别")}
    val audioPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted){if(prefs.getBoolean("stt_local_first",true)&&OfflineSpeechRepository.isInstalled(this@ComposeActivity))startOfflineInput()else if(startOnlineVoiceRecording()){uiRevision++;toast("正在录音，再次点击麦克风完成识别")}}else toast("需要麦克风权限才能使用语音识别")}
    fun startOnlineInput(){if(!prefs.getBoolean("stt_online_fallback",true)){toast("请在语音设置中开启在线识别回退");return};if(secure.getNamed("stt_api_key_encrypted").isBlank()&&key().isBlank()){toast("请先配置语音识别 API Key");return};if(android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED){if(startOnlineVoiceRecording())uiRevision++}else audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)}
    fun handleVoiceInput(){if(offlineVoiceBusy()){if(!offlineVoiceLoading)finishOfflineVoiceRecognition();return};if(voiceRecording){finishOnlineVoiceRecording{appendRecognized(it);uiRevision++};uiRevision++;return};val localFirst=prefs.getBoolean("stt_local_first",true);if(localFirst&&OfflineSpeechRepository.isInstalled(this@ComposeActivity)){if(android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)startOfflineInput()else audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)}else if(localFirst&&android.speech.SpeechRecognizer.isRecognitionAvailable(this@ComposeActivity)){runCatching{voiceInput.launch(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,"zh-CN").putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,1).putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE,true).putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"说出要发送的内容"))}.onFailure{startOnlineInput()}}else startOnlineInput()}
    fun loadReplySuggestions(){if(suggestionsLoading)return;if(isChatSending(chat.id)){toast("请等待当前回复生成完成");return};if(activeProvider()==null||key().isBlank()){toast("请先配置模型供应商");return};suggestionsLoading=true;val taskId=GenerationTaskTracker.start(this@ComposeActivity,"suggestion","正在生成推荐回复",chat.title,chat.id,false);pool.execute{runCatching{generateReplySuggestions(chat,card)}.onSuccess{values->GenerationTaskTracker.finish(this@ComposeActivity,taskId,"已生成 ${values.size} 条建议");main.post{replySuggestions=values;suggestionsLoading=false}}.onFailure{error->GenerationTaskTracker.fail(this@ComposeActivity,taskId,error);main.post{suggestionsLoading=false;recordApiError(error);toast("推荐回复生成失败：${friendlyError(error)}")}}}}
    fun sendMessage(body:String,image:String="",type:String="text",meta:String="",file:String=""):Boolean{
      if(body.isBlank()&&image.isBlank()&&file.isBlank())return false
      if(activeProvider()==null&&!chat.userBlockedRole){toast("请先在模型设置中添加供应商");return false}
      val immersive=chat.immersiveMode&&!chat.group
      val wasSending=isChatSending(chat.id)
      if(immersive&&wasSending)restartImmersiveGeneration(chat)
      val messageBody=body.trim()
      val members=if(chat.group)chat.cardIds.mapNotNull{find(it)}else emptyList()
      val mentioned=ChatReplyOrdering.mentioned(messageBody,members)
      val target=mentioned.firstOrNull()?:if(chat.group&&chat.groupAutonomous)members.randomOrNull()?:card else card
      if(mentioned.isNotEmpty()||chat.group){chat.activeCardId=target.id;chat.cardId=target.id}
      val message=Msg("user",messageBody);message.messageType=type;message.meta=meta;message.image=image;message.file=file;message.delivered=!chat.userBlockedRole
      chat.messages.add(message);chat.updated=System.currentTimeMillis();chat.proactiveSequence=0;replySuggestions=emptyList();save()
      followStreamingAtBottom=false;if(chat.autoScrollAfterSend)smoothScrollRequest++
      if(chat.userBlockedRole){toast("消息已保存在本机，但角色无法收到");uiRevision++;return true}
      when{
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
    fun shareChatMessage(source:Chat,message:Msg){
      val sourceRole=if(message.role=="assistant")find(message.speakerCardId)?.name?:"角色" else "我"
      val meta=JSONObject().put("sourceChatId",source.id).put("sourceChatTitle",source.title).put("sourceRole",sourceRole).put("originalType",message.messageType).toString()
      if(sendMessage(message.body,message.image,"chat_share",meta)){showShareChat=false;toast("已分享给 ${card.name}")}
    }
    fun sendCurrent(){
      val fileData=pendingFile
      val type=if(pendingSticker)"sticker" else if(webSearchNext)"web_search" else if(fileData!=null)"file" else if(pendingImage.isNotBlank())"image" else "text"
      val fileMeta=if(fileData==null)"" else JSONObject().put("name",fileData.optString("name")).put("size",fileData.optLong("size")).put("mime",fileData.optString("mime")).put("text",fileData.optString("text")).toString()
      if(sendMessage(text,pendingImage,type,fileMeta,fileData?.optString("path").orEmpty())){text="";pendingImage="";pendingSticker=false;webSearchNext=false;pendingFile=null}}
    fun searchMusicForShare(){val value=musicSearchQuery.trim();if(value.isBlank()){toast("请输入歌曲或歌手");return};musicSearchBusy=true;musicSearchError="";pool.execute{runCatching{searchNeteaseSongs(value)}.onSuccess{result->main.post{musicSearchSongs=result;musicSearchBusy=false;if(result.isEmpty())musicSearchError="没有找到歌曲"}}.onFailure{e->main.post{musicSearchBusy=false;musicSearchError=e.message?:"网易云搜索失败"}}}}
    fun sendNeteaseSong(song:NeteaseSong){val url=neteaseSongUrl(song.id);val meta=JSONObject().put("title",song.title).put("artist",song.artist).put("album",song.album).put("url",url).put("source","netease").put("songId",song.id).put("coverUrl",song.coverUrl).toString();if(sendMessage("${song.title} · ${song.artist}","","music",meta)){showMusic=false;musicSearchQuery="";musicSearchSongs=emptyList();toast("已分享网易云歌曲")}}
    fun failVoiceCall(session:String,error:String){
      if(session!=voiceCallSessionId)return
      voiceCallPhase=VoiceCallPhase.ERROR
      voiceCallStatus=error.ifBlank{"语音识别失败"}
      syncVoiceCallOverlay()
      uiRevision++
    }
    fun submitVoiceCallTranscript(session:String,value:String){
      if(session!=voiceCallSessionId||voiceCallChatId!=chat.id)return
      val transcript=value.trim()
      if(transcript.isBlank()){failVoiceCall(session,"没有识别到语音");return}
      if(chat.roleBlockedUser){failVoiceCall(session,"${card.name} 尚未解除拉黑，无法收到回复");return}
      if(isChatSending(chat.id)){failVoiceCall(session,"当前通话回复仍在生成");return}
      voiceCallLastUserText=transcript
      voiceCallLastReplyText=""
      voiceCallPhase=VoiceCallPhase.THINKING
      voiceCallStatus="角色正在思考"
      val message=Msg("user",transcript)
      message.messageType="voice_call_transcript"
      val sharedFrame=voiceCallScreenImagePath.takeIf{voiceCallScreenSharing&&voiceCallVisualReady()&&File(it).isFile}.orEmpty()
      message.image=sharedFrame
      message.meta=JSONObject().put("callId",session).put("screenShared",voiceCallScreenSharing)
        .put("screenFrameAttached",sharedFrame.isNotBlank()).toString()
      message.delivered=true
      chat.messages.add(message);chat.updated=System.currentTimeMillis();chat.proactiveSequence=0
      save();uiRevision++
      syncVoiceCallOverlay()
      requestVoiceCall(chat,card,session)
    }
    fun startVoiceCallCapture(){
      val session=voiceCallSessionId
      if(session.isBlank()||voiceCallChatId!=chat.id||voiceCallMicMuted)return
      if(chat.roleBlockedUser){failVoiceCall(session,"${card.name} 尚未解除拉黑，无法继续通话");return}
      val localFirst=prefs.getBoolean("stt_local_first",true)
      if(localFirst&&OfflineSpeechRepository.isInstalled(this@ComposeActivity)){
        voiceCallPhase=VoiceCallPhase.LISTENING;voiceCallStatus="正在准备语音识别"
        val started=startOfflineVoiceRecognition(object:OfflineSpeechCallback{
          override fun onStateChanged(){
            if(session!=voiceCallSessionId)return
            if(voiceCallPhase==VoiceCallPhase.LISTENING)
              voiceCallStatus=if(offlineVoiceLoading)"正在准备语音识别" else if(offlineVoiceListening)"正在聆听，说完会自动发送" else "正在识别"
            uiRevision++
          }
          override fun onResult(value:String){submitVoiceCallTranscript(session,value)}
          override fun onError(error:String){failVoiceCall(session,error)}
        },true)
        if(!started)failVoiceCall(session,"无法启动本地语音识别")
        return
      }
      if(!prefs.getBoolean("stt_online_fallback",true)){
        failVoiceCall(session,"请先安装本地语音模型，或在语音设置中开启在线识别")
        return
      }
      val auxiliary=prefs.getBoolean("audio_aux_enabled",false)
      val voiceKey=secure.getNamed(if(auxiliary)"audio_aux_key" else "stt_api_key_encrypted")
      if(voiceKey.isBlank()&&key().isBlank()){
        failVoiceCall(session,"请先在语音设置中配置语音识别 API Key")
        return
      }
      if(startOnlineVoiceRecording()){
        voiceCallPhase=VoiceCallPhase.LISTENING;voiceCallStatus="正在聆听，说完会自动发送";syncVoiceCallOverlay();uiRevision++
        startOnlineVoiceAutoStop{
          if(session==voiceCallSessionId&&voiceCallPhase==VoiceCallPhase.LISTENING&&voiceRecording){
            voiceCallPhase=VoiceCallPhase.TRANSCRIBING;voiceCallStatus="正在识别";syncVoiceCallOverlay();uiRevision++
            finishOnlineVoiceRecording({value->submitVoiceCallTranscript(session,value)},{error->failVoiceCall(session,error)})
          }
        }
      }else failVoiceCall(session,"无法启动录音")
    }
    val voiceCallAudioPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
      if(granted)startVoiceCallCapture() else failVoiceCall(voiceCallSessionId,"需要麦克风权限才能进行语音通话")
    }
    fun handleVoiceCallMic(){
      val session=voiceCallSessionId
      if(session.isBlank()||voiceCallChatId!=chat.id)return
      when(voiceCallPhase){
        VoiceCallPhase.LISTENING->{
          voiceCallPhase=VoiceCallPhase.TRANSCRIBING;voiceCallStatus="正在识别";uiRevision++
          when{
            offlineVoiceBusy()->finishOfflineVoiceRecognition()
            voiceRecording->finishOnlineVoiceRecording(
              {value->submitVoiceCallTranscript(session,value)},
              {error->failVoiceCall(session,error)})
            else->failVoiceCall(session,"录音已中断，请重试")
          }
        }
        VoiceCallPhase.SPEAKING->{
          voiceCallPhase=VoiceCallPhase.READY;voiceCallStatus="正在切换到聆听";uiRevision++
          stopSpeech()
          main.postDelayed({if(session==voiceCallSessionId&&voiceCallPhase==VoiceCallPhase.READY&&!voiceCallMicMuted)startVoiceCallCapture()},220L)
        }
        VoiceCallPhase.READY,VoiceCallPhase.ERROR->{
          if(voiceCallMicMuted){toast("请先开启麦克风");return}
          if(android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)startVoiceCallCapture()
          else voiceCallAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }
        else->Unit
      }
      syncVoiceCallOverlay()
    }
    fun toggleVoiceCallMic(){
      voiceCallMicMuted=!voiceCallMicMuted
      if(voiceCallMicMuted){
        if(voiceRecording)cancelOnlineVoiceRecording()
        if(offlineVoiceBusy())cancelOfflineVoiceRecognition()
        if(voiceCallPhase==VoiceCallPhase.LISTENING||voiceCallPhase==VoiceCallPhase.TRANSCRIBING){voiceCallPhase=VoiceCallPhase.READY;voiceCallStatus="麦克风已关闭"}
      }else if(voiceCallPhase==VoiceCallPhase.READY)voiceCallStatus="准备就绪"
      syncVoiceCallOverlay()
      uiRevision++
    }
    LaunchedEffect(voiceCallPendingCommand,voiceCallSessionId,chat.id){
      val command=voiceCallPendingCommand.substringBefore(':')
      if(command.isNotBlank()&&voiceCallChatId==chat.id){
        voiceCallPendingCommand=""
        when(command){
          "mic"->handleVoiceCallMic()
          "listen"->{delay(260);if(voiceCallSessionId.isNotBlank()&&voiceCallChatId==chat.id&&voiceCallPhase==VoiceCallPhase.READY&&!voiceCallMicMuted)startVoiceCallCapture()}
        }
      }
    }
    fun acceptVoiceCall(invitation:Msg,speaker:Card){
      val data=runCatching{JSONObject(invitation.meta)}.getOrElse{JSONObject()}
      if(data.optString("state","邀请")!="邀请")return
      val note=data.optString("note",invitation.body).takeIf{it!="语音通话"}.orEmpty()
      if(beginVoiceCallSession(chat,speaker,note)){
        data.put("state","接听").put("callId",voiceCallSessionId)
        invitation.meta=data.toString();save();uiRevision++
        handleVoiceCallMic()
      }
    }
    fun generateConfirmedImage(msg:Msg){
      if(msg.loading)return
      val prompt=imagePromptConfig(msg.meta)
      if(prompt.positive.isBlank()){toast("没有可用的生图提示词");return}
      msg.loading=true;msg.body="正在生成图片";msg.failed=false;msg.errorDetail=""
      val taskId=GenerationTaskTracker.start(this@ComposeActivity,"image","正在生成图片",chat.title,chat.id,true)
      imageGenerationTaskIds[msg.id]=taskId
      val handle=beginImageGeneration(msg.id)
      uiRevision++
      pool.execute{
        handle.worker=Thread.currentThread()
        try {
          val path=generateImage(prompt.positive,prompt.negative,handle)
          if(isImageGenerationCancelled(handle)){
            runCatching{File(path).delete()}
            GenerationTaskTracker.cancel(this@ComposeActivity,taskId)
            imageGenerationTaskIds.remove(msg.id,taskId)
          } else {
            main.post{
              if(isImageGenerationCancelled(handle)||!chat.messages.contains(msg)||!msg.loading){runCatching{File(path).delete()};GenerationTaskTracker.cancel(this@ComposeActivity,taskId);imageGenerationTaskIds.remove(msg.id,taskId);return@post}
              GenerationTaskTracker.finish(this@ComposeActivity,taskId,"图片已生成")
              imageGenerationTaskIds.remove(msg.id,taskId)
              msg.image=path;msg.messageType="text";msg.body="分享了一张刚刚生成的图片。";msg.meta="";msg.loading=false;msg.failed=false;msg.errorDetail="";save();uiRevision++;toast("图片已生成")
            }
          }
        } catch(error:Throwable) {
          if(isImageGenerationCancelled(handle)||error is InterruptedIOException){
            GenerationTaskTracker.cancel(this@ComposeActivity,taskId)
            imageGenerationTaskIds.remove(msg.id,taskId)
          } else {
            GenerationTaskTracker.fail(this@ComposeActivity,taskId,error)
            imageGenerationTaskIds.remove(msg.id,taskId)
            main.post{if(!chat.messages.contains(msg))return@post;msg.loading=false;msg.failed=true;msg.errorDetail=friendlyError(error);recordApiError(error);save();uiRevision++;toast("生图失败：${friendlyError(error)}")}
          }
        } finally { finishImageGeneration(msg.id,handle) }
      }
    }
    fun startUserImageGeneration(){
      val positive=imageGenPositive.trim()
      if(positive.isBlank()){toast("请先填写画面描述");return}
      if(!prefs.getBoolean("image_generation_enabled",false)||activeImageProvider()==null){toast("请先开启并配置生图功能");imageGenerationBackPage=Page.CHAT;composePage=Page.IMAGE_GENERATION;showImageGen=false;return}
      if(isChatSending(chat.id)){toast("请等待当前回复生成完成");return}
      val message=Msg("user",positive)
      message.messageType="image_request"
      message.meta=imagePromptJson(positive,imageGenNegative.trim(),"natural")
      message.delivered=true
      chat.messages.add(message);chat.updated=System.currentTimeMillis();chat.proactiveSequence=0;save();uiRevision++
      showImageGen=false;imageGenPositive="";imageGenNegative=""
      generateConfirmedImage(message)
    }
    fun claimRedPacket(msg:Msg){val data=runCatching{JSONObject(msg.meta)}.getOrElse{JSONObject()};if(data.optBoolean("claimed",false)){toast("红包已经领取");return};val amount=data.optString("amount","0.00");val sender=find(msg.speakerCardId)?:card;if(sendMessage("你领取了 ${sender.name} 的 ¥$amount 红包","","red_packet_receipt",msg.id)){data.put("claimed",true).put("claimedAt",System.currentTimeMillis());msg.meta=data.toString();save();uiRevision++}}
    LaunchedEffect(chat.id){if(!initialPositionApplied&&visibleMessages.isNotEmpty())listState.scrollToItem(visibleMessages.lastIndex,Int.MAX_VALUE);initialPositionApplied=true;navigationSettled=true}
    LaunchedEffect(pendingChatMessageId,visibleMessages.size){val target=pendingChatMessageId.takeIf{it.isNotBlank()}?.let{id->visibleMessages.indexOfFirst{it.id==id}}?:-1;if(target>=0){delay(180);listState.animateScrollToItem(target);pendingChatMessageId=""}}
    LaunchedEffect(smoothScrollRequest){if(smoothScrollRequest>0&&navigationSettled){delay(260);if(visibleMessages.isNotEmpty())listState.animateScrollToItem(visibleMessages.lastIndex);followStreamingAtBottom=true}}
    LaunchedEffect(userDragging){if(userDragging)followStreamingAtBottom=false else{delay(90);if(!listState.canScrollForward)followStreamingAtBottom=true}}
    LaunchedEffect(latestConversationKey){if(!suggestionsLoading)replySuggestions=emptyList()}
    LaunchedEffect(lastMessageLength){if(navigationSettled&&thisSending&&followStreamingAtBottom&&visibleMessages.isNotEmpty()){delay(35);if(followStreamingAtBottom&&!userDragging)listState.scrollToItem(visibleMessages.lastIndex,Int.MAX_VALUE)}}
    val halfScreenAvailable=prefs.getBoolean("chat_half_screen",false)&&chatBackgroundImage.isNotBlank()
    var halfScreen by remember(chat.id){mutableStateOf(halfScreenAvailable)}
    Box(Modifier.fillMaxSize().background(chatBackground)){
    if(chatBackgroundImage.isNotBlank())MomentImage(chatBackgroundImage,backgroundLayerModifier)
    if(chatBackgroundImage.isNotBlank())Box(backgroundLayerModifier.background(chatBackground.copy(alpha=chatBackgroundOverlay)))
    Scaffold(containerColor = Color.Transparent,
      modifier=if(halfScreen)Modifier.fillMaxWidth().fillMaxHeight(0.58f).align(Alignment.BottomCenter) else Modifier.fillMaxSize(),
      topBar = {
        Surface(color=if(transparentTop)Color.Transparent else MaterialTheme.colorScheme.background.copy(alpha=if(glassInput).92f else 1f),contentColor=if(transparentTop)topBarContentColor else MaterialTheme.colorScheme.onBackground) { Column(Modifier.statusBarsPadding()) {
          Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={selectedChat=null;composePage=Page.MAIN}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")};Box(Modifier.combinedClickable(onClick={},onDoubleClick={patRole(card)})){RoleAvatar(card,36)};Column(Modifier.weight(1f).padding(start=9.dp),verticalArrangement=Arrangement.spacedBy(0.dp)){Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){Text(if(thisSending&&chat.immersiveMode&&!chat.group)"正在输入中....." else if(chat.group)chat.title else card.name,fontSize=16.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1,softWrap=false)};Text((activeProvider()?.let{"${it.name} · ${model()}"} ?: "暂未配置模型供应商")+(if(chat.immersiveMode)" · 内容由 AI 生成" else ""),fontSize=10.sp,lineHeight=12.sp,color=if(transparentTop)topBarContentColor.copy(alpha=.72f) else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};if(!chat.group)IconButton(onClick={editingCardId=card.id;roleProfileChatId=chat.id;roleProfileBackPage=Page.CHAT;composePage=Page.ROLE_PROFILE}){Icon(Icons.Outlined.Insights,"角色小手机")};IconButton(onClick={composePage=Page.CHAT_SEARCH}){Icon(Icons.Outlined.Search,"搜索对话")};IconButton(onClick={composePage=Page.CHAT_DETAIL}){Icon(Icons.Outlined.MoreHoriz,"更多")}
          }
          if(chat.group){Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=58.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){chat.cardIds.mapNotNull{find(it)}.forEach{member->val token="@${member.name}";val selected=text.contains(token);FilterChip(selected=selected,onClick={if(selected){text=text.replace(token,"").replace(Regex(" {2,}")," ").trimStart()}else{text=if(text.isBlank())"$token " else "$text $token "};uiRevision++},label={Text(member.name,maxLines=1,softWrap=false)})}}}
        }}
      },
      bottomBar = {
        Surface(color=Color.Transparent,modifier=Modifier.navigationBarsPadding().imePadding()){Column(Modifier.fillMaxWidth().padding(horizontal=if(glassInput)8.dp else 0.dp,vertical=if(glassInput)7.dp else 0.dp).then(if(glassInput)Modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=.86f)).border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.42f),RoundedCornerShape(22.dp)).padding(horizontal=10.dp,vertical=7.dp)else Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal=10.dp,vertical=6.dp))){
          if(suggestionPosition=="input")Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom=5.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
            if(suggestionsLoading)SuggestionChip(onClick={},enabled=false,label={Text("正在生成推荐回复…",maxLines=1)},icon={CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp)})
            else if(replySuggestions.isEmpty())SuggestionChip(onClick={loadReplySuggestions()},label={Text("推荐回复",maxLines=1)},icon={Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(16.dp))})
            else{IconButton(onClick={loadReplySuggestions()},modifier=Modifier.size(34.dp)){Icon(Icons.Outlined.Refresh,"重新生成推荐回复",Modifier.size(18.dp))};replySuggestions.forEach{suggestion->SuggestionChip(onClick={text=suggestion},label={Text(suggestion,maxLines=1,overflow=TextOverflow.Ellipsis)})}}
          }
          if(webSearchNext)AssistChip(onClick={webSearchNext=false},label={Text("下一条联网搜索",maxLines=1)},leadingIcon={Icon(Icons.Outlined.TravelExplore,null,Modifier.size(16.dp))},trailingIcon={Icon(Icons.Outlined.Close,"取消联网",Modifier.size(15.dp))},modifier=Modifier.padding(bottom=5.dp))
          if(pendingImage.isNotBlank())Row(Modifier.fillMaxWidth().padding(bottom=5.dp),verticalAlignment=Alignment.CenterVertically){MessageImage(pendingImage,Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),if(pendingSticker)ContentScale.Fit else ContentScale.Crop);Text(if(pendingSticker)"表情可与文字一起发送" else "照片将随消息发送给 AI 进行识别",Modifier.weight(1f).padding(start=9.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);IconButton(onClick={pendingImage="";pendingSticker=false},Modifier.size(36.dp)){Icon(Icons.Outlined.Close,"移除图片")}}
          if(pendingFileBusy)Row(Modifier.fillMaxWidth().padding(bottom=5.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp);Text("正在读取附件…",Modifier.padding(start=9.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
          pendingFile?.let{fileData->Row(Modifier.fillMaxWidth().padding(bottom=5.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.85f)){Row(Modifier.padding(9.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.InsertDriveFile,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary);Column(Modifier.padding(start=9.dp)){Text(fileData.optString("name","附件"),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(if(fileData.optString("text").isNotBlank())"已提取文本内容" else "将作为附件发送",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}};IconButton(onClick={pendingFile=null}){Icon(Icons.Outlined.Close,"移除附件",Modifier.size(16.dp))}}}}
          if(text.length>=3500)Text("${text.length} / 4000",Modifier.fillMaxWidth().padding(end=54.dp,bottom=3.dp),textAlign=androidx.compose.ui.text.style.TextAlign.End,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){FilledTonalIconButton(onClick={showTools=true},modifier=Modifier.size(40.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=if(glassInput).66f else 1f))){Icon(Icons.Outlined.Add,"添加更多内容",Modifier.size(21.dp))};Spacer(Modifier.width(7.dp));Box(Modifier.weight(1f).heightIn(min=prefs.getInt("chat_input_min_height",42).coerceIn(40,64).dp,max=92.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=if(glassInput).62f else 1f)).then(if(glassInput)Modifier.border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.28f),RoundedCornerShape(20.dp))else Modifier).padding(horizontal=14.dp,vertical=8.dp),contentAlignment=Alignment.CenterStart){BasicTextField(value=text,onValueChange={text=it.take(4000)},modifier=Modifier.fillMaxWidth().onPreviewKeyEvent{event->if(chat.enterToSend&&event.type==KeyEventType.KeyDown&&event.key==Key.Enter&&!event.isShiftPressed){sendCurrent();true}else false},maxLines=4,textStyle=LocalTextStyle.current.copy(color=MaterialTheme.colorScheme.onSurface,fontSize=15.sp,lineHeight=20.sp),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),keyboardOptions=KeyboardOptions(imeAction=if(chat.enterToSend)ImeAction.Send else ImeAction.Default),keyboardActions=KeyboardActions(onSend={sendCurrent()}),decorationBox={inner->if(text.isEmpty())Text(if(voiceRecording||offlineVoiceBusy())"正在录音…" else if(thisSending)"继续发送，角色稍后回复…" else "写下此刻想说的话…",fontSize=14.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);inner()})};if(thisSending){Spacer(Modifier.width(4.dp));FilledTonalIconButton(onClick={stopGeneration(chat.id)},modifier=Modifier.size(36.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.errorContainer.copy(alpha=if(glassInput).78f else 1f))){Icon(Icons.Filled.Stop,"停止生成",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onErrorContainer)}};Spacer(Modifier.width(4.dp));FilledIconButton(onClick={sendCurrent()},modifier=Modifier.size(42.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.primary.copy(alpha=if(glassInput).88f else 1f))){Icon(Icons.Outlined.ArrowUpward,"发送",Modifier.size(20.dp))}}
        }}
      }
    ){padding->
      val listModifier=if(glassInput)Modifier.fillMaxSize().padding(top=padding.calculateTopPadding()) else Modifier.fillMaxSize().padding(padding)
      LazyColumn(state=listState,modifier=listModifier,contentPadding=PaddingValues(start=14.dp,end=14.dp,top=12.dp,bottom=if(glassInput)padding.calculateBottomPadding()+28.dp else 28.dp),verticalArrangement=Arrangement.spacedBy(prefs.getInt("chat_bubble_spacing",8).coerceIn(2,16).dp)){
        itemsIndexed(visibleMessages,key={_,msg->msg.id}){index,msg->Column(Modifier.fillMaxWidth()){
          val previous=visibleMessages.getOrNull(index-1)
          if(shouldShowMessageTime(previous,msg))MessageTimeDivider(msg.createdAt)
          val suggestionAnchor=suggestionPosition=="bubble"&&msg.id==suggestionAnchorId
          MessageBubble(card,chat,msg,onLongPress={menuMessage=it},onCopy={clipboard.setText(AnnotatedString(msg.body));toast("已复制")},onRetry={if(isChatSending(chat.id)){toast("请等待当前生成")}else if(activeProvider()==null){toast("请先在模型设置中添加供应商")}else{val i=chat.messages.indexOf(msg);if(i>=0){while(chat.messages.size>i)chat.messages.removeAt(chat.messages.lastIndex);save();request(chat,find(msg.speakerCardId)?:card)}}},onErrorDetails={if(lastApiError.isBlank()&&msg.errorDetail.isNotBlank())recordApiErrorDetails(msg.errorDetail);apiErrorBackPage=Page.CHAT;composePage=Page.API_ERROR},onConfigureVision={composePage=Page.AUXILIARY_MODELS},onImageClick={if(msg.messageType=="sticker")stickerPreview=msg.image else imagePreview=msg.image},showActions=bubbleActionMode!="hidden"&&msg.id==actionAnchorId,onImageRequest={generateConfirmedImage(msg)},onEditImageRequest={editImagePrompt=msg},onCancelImageRequest={
            if(msg.messageType=="image_request"){
              val running=msg.loading
              cancelImageGeneration(msg.id)
              if(running&&isChatSending(chat.id))stopGeneration(chat.id)
              chat.messages.remove(msg)
              imageGenerationTaskIds.remove(msg.id)?.let{GenerationTaskTracker.cancel(this@ComposeActivity,it)}
              save();uiRevision++;toast(if(running)"已取消图片生成" else "已取消生图请求")
            }
          },onClaimRedPacket={claimRedPacket(msg)},onVoiceCall={message,speaker->acceptVoiceCall(message,speaker)},onPatRole={patRole(it)},onImageGenRequest={showImageGen=true},suggestionsLoading=suggestionAnchor&&suggestionsLoading,onSuggestions=if(suggestionAnchor){{loadReplySuggestions()}}else null)
          if(suggestionAnchor&&replySuggestions.isNotEmpty())ReplySuggestionBar(replySuggestions){text=it}
        }}
      }
    }
      if(halfScreen)Surface(onClick={halfScreen=false},shape=RoundedCornerShape(999.dp),color=Color.Black.copy(alpha=.42f),modifier=Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top=10.dp)){Row(Modifier.padding(horizontal=13.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.KeyboardArrowUp,null,Modifier.size(15.dp),tint=Color.White);Text("展开完整聊天",Modifier.padding(start=4.dp),fontSize=11.sp,color=Color.White)}}
      else Surface(onClick={halfScreen=true},shape=RoundedCornerShape(999.dp),color=Color.Black.copy(alpha=.34f),modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=68.dp,end=10.dp)){Row(Modifier.padding(horizontal=11.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.KeyboardArrowDown,null,Modifier.size(14.dp),tint=Color.White);Text("半屏",Modifier.padding(start=3.dp),fontSize=11.sp,color=Color.White)}}
    }
    if(showTools){
      ModalBottomSheet(onDismissRequest={showTools=false},dragHandle={BottomSheetDefaults.DragHandle()}){
        Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=24.dp)){
          Text("添加到对话",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
          Text("图片和表情可以继续搭配文字发送",Modifier.padding(top=3.dp,bottom=16.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
          LazyVerticalGrid(
            columns=GridCells.Fixed(4),
            modifier=Modifier.fillMaxWidth().height(340.dp),
            userScrollEnabled=true,
            horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)
          ){
            item{ChatToolAction(Icons.Outlined.AddPhotoAlternate,"图片"){showTools=false;imagePicker.launch(arrayOf("image/*"))}}
            item{ChatToolAction(Icons.Outlined.PhotoCamera,"拍照"){showTools=false;takePhoto()}}
            item{ChatToolAction(Icons.Outlined.InsertDriveFile,"文件"){showTools=false;if(pendingFileBusy)toast("正在处理上一个附件")else filePicker.launch(arrayOf("*/*"))}}
            item{val recording=voiceRecording||offlineVoiceBusy();ChatToolAction(if(recording)Icons.Filled.StopCircle else Icons.Outlined.Mic,if(recording)"结束录音" else "语音"){showTools=false;handleVoiceInput()}}
            item{ChatToolAction(Icons.Outlined.EmojiEmotions,"表情"){showTools=false;showStickers=true}}
            item{ChatToolAction(Icons.Outlined.Nightlight,"哄睡故事"){showTools=false;composePage=Page.STORIES}}
            item{ChatToolAction(Icons.Outlined.CardGiftcard,"红包"){showTools=false;showRedPacket=true}}
            item{ChatToolAction(Icons.Outlined.Redeem,"礼物"){showTools=false;showGift=true}}
            item{ChatToolAction(Icons.Outlined.LocationOn,"定位"){showTools=false;showLocation=true}}
            item{ChatToolAction(Icons.Outlined.Payments,"转账"){showTools=false;showTransfer=true}}
            item{ChatToolAction(Icons.Outlined.MusicNote,"音乐"){showTools=false;showMusic=true}}
            item{ChatToolAction(Icons.Outlined.SportsEsports,"游戏"){showTools=false;saveWorldRoleIds(worldRoleIds()+card.id);composePage=Page.WORLD_GAMES}}
            item{ChatToolAction(Icons.Outlined.Call,"语音通话"){showTools=false;showVoiceCall=true}}
             item{ChatToolAction(Icons.Outlined.Brush,"生图"){showTools=false;showImageGen=true}}
            item{ChatToolAction(Icons.Outlined.Movie,"视频"){showTools=false;videoGenerationBackPage=Page.CHAT;composePage=Page.VIDEO_GENERATION}}
            item{ChatToolAction(Icons.Outlined.MenuBook,"日记"){showTools=false;composePage=Page.DIARIES}}
            item{ChatToolAction(Icons.Outlined.Bookmarks,"剧情节点"){showTools=false;composePage=Page.PLOT_NODES}}
            item{ChatToolAction(Icons.Outlined.Collections,"朋友圈"){showTools=false;composePage=Page.MOMENT_SHARE}}
            item{ChatToolAction(Icons.Outlined.ForwardToInbox,"分享聊天"){showTools=false;showShareChat=true}}
            item{ChatToolAction(Icons.Outlined.TouchApp,"拍一拍"){showTools=false;patRole(card)}}
            item{ChatToolAction(Icons.Outlined.TravelExplore,"联网"){showTools=false;if(prefs.getBoolean("web_search_enabled",true))webSearchNext=true else{toast("请先开启联网搜索");webSearchBackPage=Page.CHAT;composePage=Page.WEB_SEARCH}}}
            if(card.id in momentRoleIds("moments_post_roles"))item{ChatToolAction(Icons.Outlined.AutoAwesome,"角色动态"){showTools=false;momentRolePostBackPage=Page.CHAT;composePage=Page.MOMENT_ROLE_POST}}
          }
        }
      }
    }
    if(showImageGen)AlertDialog(
      onDismissRequest={showImageGen=false},
       icon={Icon(Icons.Outlined.Brush,null)},
      title={Text("主动生成图片")},
      text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("输入画面描述，图片生成后会直接出现在当前对话中。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(imageGenPositive,{imageGenPositive=it.take(1200)},Modifier.fillMaxWidth().heightIn(min=96.dp),label={Text("画面描述")},placeholder={Text("例如：黄昏的海边，她回头对我微笑")},minLines=3,maxLines=6,shape=RoundedCornerShape(13.dp))
        OutlinedTextField(imageGenNegative,{imageGenNegative=it.take(500)},Modifier.fillMaxWidth(),label={Text("负向提示词（可选）")},placeholder={Text("例如：lowres, bad anatomy")},singleLine=true,shape=RoundedCornerShape(13.dp))
      }},
      confirmButton={Button(onClick={startUserImageGeneration()}){Text("开始生成")}},
      dismissButton={TextButton(onClick={showImageGen=false}){Text("取消")}}
    )
    if(showStickers)ModalBottomSheet(onDismissRequest={showStickers=false},dragHandle={BottomSheetDefaults.DragHandle()}){
      val groups=remember(uiRevision){StickerRepository.groups(this@ComposeActivity,prefs)}
      val activeId=selectedStickerGroup.takeIf{id->groups.any{it.id==id}}?:groups.firstOrNull()?.id.orEmpty()
      val activeGroup=groups.firstOrNull{it.id==activeId}
      val sourceImages=activeGroup?.images.orEmpty()
      var shown by remember(activeId,sourceImages){mutableStateOf(sourceImages)}
      var organizing by remember{mutableStateOf(false)}
      var selectedMovePath by remember(activeId){mutableStateOf("")}
      var stickerActionPath by remember{mutableStateOf("")}
      val stickerGridState=rememberLazyGridState()
      LaunchedEffect(activeId){if(sourceImages.isNotEmpty())stickerGridState.scrollToItem(0)}
      Column(Modifier.fillMaxWidth().heightIn(min=300.dp,max=560.dp).padding(horizontal=14.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
          Text(
            if(organizing&&selectedMovePath.isNotBlank())"选择目标位置" else if(organizing)"整理表情顺序" else "选择表情",
            Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold
          )
          if(shown.isNotEmpty())TextButton(onClick={organizing=!organizing;selectedMovePath=""}){
            Icon(if(organizing)Icons.Filled.Check else Icons.Outlined.SwapVert,null,Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text(if(organizing)"完成" else "整理")
          }
        }
        if(groups.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){groups.forEach{group->FilterChip(selected=group.id==activeId,onClick={selectedStickerGroup=group.id},label={Text("${group.name} ${group.images.size}",maxLines=1,softWrap=false)})}}
        if(shown.isEmpty())Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Outlined.EmojiEmotions,null,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.secondary);Text("还没有表情资源",Modifier.padding(top=9.dp),fontWeight=FontWeight.Medium);Button(onClick={showStickers=false;stickerResourcesBackPage=Page.CHAT;composePage=Page.STICKER_RESOURCES},Modifier.padding(top=12.dp)){Text("前往表情资源")}}
        else LazyVerticalGrid(
          columns=GridCells.Fixed(4),
          state=stickerGridState,
          modifier=Modifier.fillMaxWidth().weight(1f).padding(top=10.dp),
          contentPadding=PaddingValues(bottom=24.dp),
          horizontalArrangement=Arrangement.spacedBy(8.dp),
          verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
          gridItemsIndexed(shown,key={_,path->path}){_,path->
            StickerTile(
              path=path,
              selected=organizing&&selectedMovePath==path,
              organizing=organizing,
              modifier=Modifier.animateItemPlacement(animationSpec=tween(220,easing=FastOutSlowInEasing)),
              onClick={
                if(!organizing){pendingImage=path;pendingSticker=true;showStickers=false}
                else when{
                  selectedMovePath.isBlank()->selectedMovePath=path
                  selectedMovePath==path->selectedMovePath=""
                  else->{
                    val from=shown.indexOf(selectedMovePath)
                    val target=shown.indexOf(path)
                    if(activeId.isNotBlank()&&from in shown.indices&&target in shown.indices){
                      shown=StickerRepository.move(prefs,activeId,shown,from,target)
                      uiRevision++
                    }
                    selectedMovePath=""
                  }
                }
              },
              onPreview={stickerActionPath=path}
            )
          }
        }
      }
      if(stickerActionPath.isNotBlank()){
        val isFavorite=StickerRepository.favorites(prefs).contains(stickerActionPath)
        AlertDialog(onDismissRequest={stickerActionPath=""},
          icon={Icon(Icons.Outlined.EmojiEmotions,null)},
          title={Text("表情操作")},
          text={Text(if(isFavorite)"可以预览这张表情，或从收藏中删除（删除后不可恢复）。" else "这是表情资源包里的图片，可以预览；如需移除请在表情资源页删除整个资源包。",fontSize=14.sp,lineHeight=21.sp)},
          confirmButton={Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={stickerPreview=stickerActionPath;stickerActionPath=""}){Text("预览")}
            if(isFavorite)Button(onClick={val target=stickerActionPath;stickerActionPath="";askDanger("删除收藏表情","删除后不可恢复，确定要删除这个表情吗？","删除"){if(StickerRepository.removeFavorite(this@ComposeActivity,prefs,target))uiRevision++else toast("删除失败")}}){Text("删除")}
          }})
      }
    }
    if(showRedPacket)ModalBottomSheet(onDismissRequest={showRedPacket=false},dragHandle={BottomSheetDefaults.DragHandle()}){Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=28.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFE9634F)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.CardGiftcard,null,tint=Color.White)};Text("发送红包",Modifier.padding(start=11.dp),fontSize=18.sp,fontWeight=FontWeight.SemiBold)};OutlinedTextField(redAmount,{value->redAmount=value.filter{it.isDigit()||it=='.'}.take(8)},Modifier.fillMaxWidth().padding(top=16.dp),label={Text("金额")},prefix={Text("¥ ")},singleLine=true,shape=RoundedCornerShape(13.dp));OutlinedTextField(redGreeting,{redGreeting=it.take(40)},Modifier.fillMaxWidth().padding(top=10.dp),label={Text("祝福语")},singleLine=true,shape=RoundedCornerShape(13.dp));Button(onClick={val amount=redAmount.toDoubleOrNull();if(amount==null||amount<=0||amount>99999)toast("请输入 0.01 至 99999 元的红包金额")else{val greeting=redGreeting.trim().ifBlank{"恭喜发财，万事如意"};val meta=JSONObject().put("amount",String.format(java.util.Locale.US,"%.2f",amount)).put("greeting",greeting).toString();if(sendMessage(greeting,"","red_packet",meta)){showRedPacket=false;redAmount="";redGreeting="恭喜发财，万事如意"}}},Modifier.fillMaxWidth().padding(top=16.dp).height(48.dp)){Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text("发送红包")}}}
    if(showGift)ModalBottomSheet(onDismissRequest={showGift=false},dragHandle={BottomSheetDefaults.DragHandle()}){Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.tertiaryContainer),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Redeem,null,tint=MaterialTheme.colorScheme.onTertiaryContainer)};Text("送礼物给 ${card.name}",Modifier.padding(start=11.dp),fontSize=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)};StyledField(giftName,{giftName=it.take(80)},"礼物名称");StyledField(giftWish,{giftWish=it.take(160)},"想说的话（可选）");OutlinedButton(onClick={giftImagePicker.launch(arrayOf("image/*"))},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.AddPhotoAlternate,null);Spacer(Modifier.width(7.dp));Text(if(giftImage.isBlank())"添加礼物图片（可选）" else "更换礼物图片",maxLines=1,overflow=TextOverflow.Ellipsis)};if(giftImage.isNotBlank())Row(verticalAlignment=Alignment.CenterVertically){MessageImage(giftImage,Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),ContentScale.Crop);Text("将随礼物一起发送",Modifier.weight(1f).padding(start=10.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);IconButton(onClick={giftImage=""}){Icon(Icons.Outlined.Close,"移除图片")}};Button(onClick={val name=giftName.trim();if(name.isBlank())toast("请填写礼物名称")else{val wish=giftWish.trim();val meta=JSONObject().put("name",name).put("wish",wish).toString();if(sendMessage(wish.ifBlank{"送给你一份 $name"},giftImage,"gift",meta)){showGift=false;giftName="";giftWish="";giftImage=""}}},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Redeem,null);Spacer(Modifier.width(7.dp));Text("送出礼物")}}}
    if(showShareChat)ModalBottomSheet(onDismissRequest={showShareChat=false},dragHandle={BottomSheetDefaults.DragHandle()}){val sources=chats.filter{it.id!=chat.id&&it.messages.any{m->!m.loading&&(m.body.isNotBlank()||m.image.isNotBlank())}};Column(Modifier.fillMaxWidth().heightIn(min=320.dp,max=620.dp).padding(horizontal=16.dp)){Text("分享其他聊天内容",fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("选择一条内容发送给当前角色，来源会显示在分享卡片中。",Modifier.padding(top=3.dp,bottom=10.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(sources.isEmpty())EmptyState(Icons.Outlined.Forum,"没有可分享的其他聊天","先在其他会话中产生聊天内容")else LazyColumn(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=24.dp)){sources.forEach{source->item(key="title-${source.id}"){Text(source.title,Modifier.padding(top=5.dp),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.secondary,maxLines=1,overflow=TextOverflow.Ellipsis)};items(source.messages.filter{!it.loading&&(it.body.isNotBlank()||it.image.isNotBlank())}.takeLast(12),key={"${source.id}-${it.id}"}){message->Surface(onClick={shareChatMessage(source,message)},shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){if(message.image.isNotBlank())MessageImage(message.image,Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),if(message.messageType=="sticker")ContentScale.Fit else ContentScale.Crop);Column(Modifier.weight(1f).padding(start=if(message.image.isNotBlank())10.dp else 0.dp)){Text(if(message.role=="assistant")find(message.speakerCardId)?.name?:"角色" else "我",fontSize=11.sp,color=MaterialTheme.colorScheme.secondary);Text(message.body.ifBlank{"[图片]"},fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)};Icon(Icons.Outlined.Send,"分享",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)}}}}}}}
    if(showLocation)ModalBottomSheet(onDismissRequest={showLocation=false},dragHandle={BottomSheetDefaults.DragHandle()}){Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("发送定位",fontSize=18.sp,fontWeight=FontWeight.SemiBold);StyledField(locationName,{locationName=it.take(80)},"地点名称");StyledField(locationAddress,{locationAddress=it.take(200)},"地址");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StyledField(locationLat,{locationLat=it.filter{ch->ch.isDigit()||ch in ".-"}.take(20)},"纬度（可选）")};Box(Modifier.weight(1f)){StyledField(locationLng,{locationLng=it.filter{ch->ch.isDigit()||ch in ".-"}.take(20)},"经度（可选）")}};Button(onClick={if(locationName.trim().isBlank()||locationAddress.trim().isBlank())toast("请填写地点名称和地址")else{val meta=JSONObject().put("name",locationName.trim()).put("address",locationAddress.trim()).put("latitude",locationLat.trim()).put("longitude",locationLng.trim()).toString();if(sendMessage(locationName.trim(),"","location",meta)){showLocation=false;locationName="";locationAddress="";locationLat="";locationLng=""}}},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text("发送定位")}}}
    if(showTransfer)ModalBottomSheet(onDismissRequest={showTransfer=false},dragHandle={BottomSheetDefaults.DragHandle()}){Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("发送转账",fontSize=18.sp,fontWeight=FontWeight.SemiBold);OutlinedTextField(transferAmount,{transferAmount=it.filter{ch->ch.isDigit()||ch=='.'}.take(10)},Modifier.fillMaxWidth(),label={Text("金额")},prefix={Text("¥ ")},singleLine=true);StyledField(transferNote,{transferNote=it.take(80)},"备注（可选）");Button(onClick={val amount=transferAmount.toDoubleOrNull();if(amount==null||amount<=0||amount>999999)toast("请输入有效金额")else{val note=transferNote.trim().ifBlank{"转账"};val meta=JSONObject().put("amount",String.format(Locale.US,"%.2f",amount)).put("note",note).toString();if(sendMessage(note,"","transfer",meta)){showTransfer=false;transferAmount="";transferNote=""}}},Modifier.fillMaxWidth().height(48.dp)){Icon(Icons.Outlined.Send,null);Spacer(Modifier.width(7.dp));Text("发送转账")}}}
    if(showMusic)ModalBottomSheet(onDismissRequest={showMusic=false},dragHandle={BottomSheetDefaults.DragHandle()}){Column(Modifier.fillMaxWidth().heightIn(min=360.dp,max=650.dp).padding(start=18.dp,end=18.dp,bottom=22.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text("分享网易云音乐",fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("搜索歌曲后直接发送，聊天中的播放会交给后台音乐服务。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedTextField(musicSearchQuery,{musicSearchQuery=it.take(80)},Modifier.fillMaxWidth(),label={Text("歌曲 / 歌手")},singleLine=true,trailingIcon={IconButton(onClick={searchMusicForShare()}){Icon(Icons.Outlined.Search,"搜索")}},shape=RoundedCornerShape(13.dp));if(musicSearchBusy)LinearProgressIndicator(Modifier.fillMaxWidth());if(musicSearchError.isNotBlank())Text(musicSearchError,fontSize=12.sp,color=MaterialTheme.colorScheme.error);if(musicSearchSongs.isEmpty()&&!musicSearchBusy)EmptyState(Icons.Outlined.Search,"搜索网易云歌曲","例如：周杰伦、陈奕迅或歌曲名")else LazyColumn(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp),contentPadding=PaddingValues(vertical=5.dp)){items(musicSearchSongs,key={it.id}){song->NeteaseSongRow(song,musicPlayingUrl==neteaseSongUrl(song.id),musicLoadingUrl==neteaseSongUrl(song.id),onPlay={toggleChatMusic(neteaseSongUrl(song.id),song.title,song.artist,song.album,song.id)},onTogether={sendNeteaseSong(song)})}}}}
    if(showVoiceCall)AlertDialog(onDismissRequest={showVoiceCall=false},icon={Icon(Icons.Outlined.Call,null)},title={Text("发起语音通话")},text={OutlinedTextField(voiceCallNote,{voiceCallNote=it.take(100)},Modifier.fillMaxWidth(),label={Text("附言（可选）")},minLines=2,maxLines=4)},confirmButton={Button(onClick={val note=voiceCallNote.trim();if(beginVoiceCallSession(chat,card,note)){showVoiceCall=false;voiceCallNote="";handleVoiceCallMic()}}){Text("呼叫")}},dismissButton={TextButton(onClick={showVoiceCall=false}){Text("取消")}})
    if(activeVoiceCall)VoiceCallDialog(
      card=card,
      phase=voiceCallPhase,
      status=voiceCallStatus,
      startedAt=voiceCallStartedAt,
      lastUserText=voiceCallLastUserText,
      lastReplyText=voiceCallLastReplyText,
      micMuted=voiceCallMicMuted,
      speakerEnabled=voiceCallSpeakerEnabled,
      screenSharing=voiceCallScreenSharing,
      screenReady=voiceCallVisualReady(),
      backgroundImagePath=chat.backgroundImage.ifBlank{prefs.getString("chat_background_image","").orEmpty()},
      onMic={handleVoiceCallMic()},
      onToggleMic={toggleVoiceCallMic()},
      onToggleSpeaker={configureVoiceCallAudio(!voiceCallSpeakerEnabled);uiRevision++},
      onToggleScreen={requestVoiceCallScreenShare()},
      onMinimize={minimizeVoiceCall()},
      onEnd={endVoiceCallSession(true)})
    menuMessage?.let{msg->AlertDialog(onDismissRequest={menuMessage=null},title={Text(if(msg.messageType=="sticker")"表情消息操作" else "消息操作")},text={Column{if(msg.image.isNotBlank())TextButton(onClick={val saved=StickerRepository.addFavorite(this@ComposeActivity,prefs,msg.image);menuMessage=null;if(saved!=null){uiRevision++;toast("已收藏到表情库")}else toast("收藏失败，图片无法读取")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.EmojiEmotions,null);Spacer(Modifier.width(8.dp));Text("收藏为表情",Modifier.weight(1f))};if(msg.image.isNotBlank())TextButton(onClick={saveImageToGallery(msg.image);menuMessage=null},Modifier.fillMaxWidth()){Icon(Icons.Outlined.SaveAlt,null);Spacer(Modifier.width(8.dp));Text("保存到相册",Modifier.weight(1f))};if(msg.image.isNotBlank())TextButton(onClick={shareImage(msg.image);menuMessage=null},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Share,null);Spacer(Modifier.width(8.dp));Text("分享图片",Modifier.weight(1f))};if(msg.messageType=="sticker")Text("长按菜单可删除这条表情聊天记录。",Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=2.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick={clipboard.setText(AnnotatedString(msg.body));menuMessage=null;toast("已复制")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.ContentCopy,null);Spacer(Modifier.width(8.dp));Text("复制",Modifier.weight(1f))};if(msg.body.isNotBlank())TextButton(onClick={editMessage=msg;editText=msg.body;menuMessage=null},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Edit,null);Spacer(Modifier.width(8.dp));Text("编辑",Modifier.weight(1f))};if(msg.role=="assistant"&&msg.body.isNotBlank()&&msg.messageType in listOf("text","desktop_pet_reply"))TextButton(onClick={menuMessage=null;requestAlternative(chat,msg,find(msg.speakerCardId)?:card)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.CallSplit,null);Spacer(Modifier.width(8.dp));Text("生成备选回复",Modifier.weight(1f))};TextButton(onClick={chat.messages.remove(msg);save();uiRevision++;menuMessage=null;toast(if(msg.messageType=="sticker")"表情聊天记录已删除" else "消息已删除")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Delete,null,tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(8.dp));Text(if(msg.messageType=="sticker")"删除表情记录" else "删除",Modifier.weight(1f),color=MaterialTheme.colorScheme.error)};if(msg.role=="assistant")TextButton(onClick={val i=chat.messages.indexOf(msg);if(i>=0){while(chat.messages.size>i)chat.messages.removeAt(chat.messages.lastIndex);menuMessage=null;request(chat,find(msg.speakerCardId)?:card)}},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text("从此处重新生成（删除后续）",Modifier.weight(1f))}}},confirmButton={})}
    editImagePrompt?.let{msg->
      val initial=remember(msg.id){imagePromptConfig(msg.meta)}
      var positive by remember(msg.id){mutableStateOf(initial.positive)}
      var negative by remember(msg.id){mutableStateOf(initial.negative)}
      var style by remember(msg.id){mutableStateOf(initial.style)}
      var converting by remember(msg.id){mutableStateOf(false)}
      AlertDialog(onDismissRequest={if(!converting)editImagePrompt=null},icon={Icon(Icons.Outlined.AutoFixHigh,null)},title={Text("编辑生图提示词")},text={Column(Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){FilterChip(selected=style=="natural",onClick={style="natural"},label={Text("自然语言",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f));FilterChip(selected=style=="english",onClick={style="english"},label={Text("英文提示词",maxLines=1,softWrap=false)},modifier=Modifier.weight(1f))}
        OutlinedTextField(positive,{positive=it.take(4000)},Modifier.fillMaxWidth().heightIn(min=120.dp),label={Text("正向提示词",maxLines=1,softWrap=false)},minLines=4,maxLines=10,shape=RoundedCornerShape(12.dp))
        OutlinedTextField(negative,{negative=it.take(2000)},Modifier.fillMaxWidth().heightIn(min=90.dp),label={Text("负向提示词",maxLines=1,softWrap=false)},minLines=3,maxLines=7,shape=RoundedCornerShape(12.dp))
        OutlinedButton(onClick={if(positive.isBlank())toast("请先填写正向提示词")else if(activeProvider()==null)toast("请先配置对话模型供应商")else{converting=true;val instruction=if(style=="english")"将下面的生图提示词优化并转换成英文。正向提示词需包含主体、外观、动作、环境、构图、镜头、光线和风格；负向提示词排除低清、错误肢体、文字水印和冲突元素。" else "将下面的生图提示词优化为自然、具体、连贯的中文画面描述。正向提示词需包含主体、外观、动作、环境、构图、镜头、光线和风格；负向提示词排除低清、错误肢体、文字水印和冲突元素。";pool.execute{runCatching{val raw=complete(listOf(Msg("user","$instruction\n只输出 JSON：{\"positive\":\"...\",\"negative\":\"...\"}\n原正向：$positive\n原负向：$negative")),card);val start=raw.indexOf('{');val end=raw.lastIndexOf('}');require(start>=0&&end>start){"模型未返回有效 JSON"};val value=JSONObject(raw.substring(start,end+1));ImagePromptConfig(value.optString("positive").ifBlank{positive},value.optString("negative").ifBlank{negative},style)}.onSuccess{value->main.post{positive=value.positive;negative=value.negative;converting=false}}.onFailure{error->main.post{converting=false;toast("提示词转换失败：${friendlyError(error)}")}}}}},enabled=!converting,modifier=Modifier.fillMaxWidth().height(44.dp)){if(converting)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Translate,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(if(converting)"正在转换" else "转换并优化提示词",maxLines=1,softWrap=false)}
      }},confirmButton={Button(onClick={if(positive.isBlank())toast("正向提示词不能为空")else{msg.meta=imagePromptJson(positive,negative,style);msg.failed=false;msg.errorDetail="";save();uiRevision++;editImagePrompt=null}},enabled=!converting){Text("保存",maxLines=1,softWrap=false)}},dismissButton={TextButton(onClick={editImagePrompt=null},enabled=!converting){Text("取消",maxLines=1,softWrap=false)}})
    }
    editMessage?.let{msg->AlertDialog(onDismissRequest={editMessage=null},title={Text("编辑消息")},text={OutlinedTextField(editText,{editText=it},Modifier.fillMaxWidth(),minLines=3)},confirmButton={Button(onClick={msg.body=editText;msg.failed=false;msg.loading=false;msg.errorDetail="";msg.keepCurrentAlternative();editMessage=null;save();uiRevision++}){Text("保存")}},dismissButton={TextButton(onClick={editMessage=null}){Text("取消")}})}
    if(imagePreview.isNotBlank())FullScreenImagePreview(imagePreview){imagePreview=""}
    if(stickerPreview.isNotBlank())StickerPreviewDialog(stickerPreview){stickerPreview=""}
  }

  @Composable
  private fun ChatModelPage(chat:Chat){
    val active=activeProvider()
    val configuredProviders=providers()
    var models by remember(chat.id,active?.id,active?.endpoint){mutableStateOf<List<String>>(emptyList())}
    var loading by remember(chat.id,active?.id,active?.endpoint){mutableStateOf(false)}
    val localActive=active?.let{isLocalProviderEndpoint(it.endpoint)}==true
    val installedLocalModels=if(localActive)LocalModelRepository.models(this).filter{it.kind=="chat"&&LocalModelRepository.installed(this,it)}.map{it.fileName}.distinct()else emptyList()
    val visibleModels=if(localActive)installedLocalModels else models
    DetailScaffold("对话模型",Page.CHAT_DETAIL){
      item{InfoBanner(Icons.Outlined.Bolt,"${chat.title} 的下一条回复","供应商和模型切换会立即生效，也会同步成为其他对话的当前模型。")}
      item{SectionTitle("模型供应商")}
      item{SettingButton(Icons.Outlined.Add,"添加模型供应商",if(configuredProviders.isEmpty())"配置 API 地址、密钥与模型后即可开始对话" else "新增在线供应商或本地模型服务"){editingProviderId=null;providerBackPage=Page.CHAT_MODEL;composePage=Page.ADD_PROVIDER}}
      items(configuredProviders,key={it.id}){value->
        Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.weight(1f)){
              SettingButton(if(active?.id==value.id)Icons.Filled.CheckCircle else if(isLocalProviderEndpoint(value.endpoint))Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,value.name,if(active?.id==value.id)"当前使用 · ${value.modelName}" else value.modelName){activateProvider(value);models=emptyList()}
            }
            IconButton(onClick={editingProviderId=value.id;providerBackPage=Page.CHAT_MODEL;composePage=Page.ADD_PROVIDER}){Icon(Icons.Outlined.Edit,"配置 ${value.name}")}
          }
        }
      }
      if(active!=null)item{SectionTitle("模型")}
      if(active!=null&&!localActive)item{Button(onClick={loading=true;pool.execute{try{val c=URL(modelsEndpoint()).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Authorization","Bearer "+key());val code=c.responseCode;val raw=read(if(code<300)c.inputStream else c.errorStream);if(code !in 200..299)throw java.io.IOException("HTTP $code：$raw");val data=JSONObject(raw).optJSONArray("data")?:JSONArray();val result=mutableListOf<String>();for(i in 0 until data.length())data.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()}?.let(result::add);result.sort();main.post{clearApiError();models=result.distinct();loading=false;if(result.isEmpty())toast("供应商没有返回可用模型")}}catch(error:Exception){main.post{recordApiError(error);loading=false;toast("模型获取失败：${friendlyError(error)}")}}}},enabled=!loading,modifier=Modifier.fillMaxWidth().height(46.dp)){if(loading)CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(if(loading)"正在获取模型" else "获取当前供应商模型",maxLines=1,softWrap=false)}}
      if(visibleModels.isNotEmpty())item{ModelListCard(visibleModels,model()){name->prefs.edit().putString("model",name).apply();activeProvider()?.let{provider->saveCustomProviders(providers().map{if(it.id==provider.id)it.copy(modelName=name)else it})};uiRevision++;toast("已切换模型 $name")}}
      if(localActive&&visibleModels.isEmpty())item{SettingButton(Icons.Outlined.Download,"还没有可用的本地聊天模型","前往本地模型页面下载或导入 GGUF 模型"){composePage=Page.LOCAL_MODELS}}
      if(active!=null&&!localActive&&models.isEmpty()&&!loading)item{InfoBanner(Icons.Outlined.Search,"当前模型 · ${model()}","获取模型列表后可搜索并切换，也可以在供应商配置中手动填写模型名称。")}
    }
  }

  @Composable
  private fun ChatDetailPage(chat:Chat){
    chatUiRevision
    val card=find(chat.activeCardId.takeIf{it.isNotBlank()}?:chat.cardId)?:return;var longMemory by remember(chat.id){mutableStateOf(chat.longMemory)};var threshold by remember(chat.id){mutableStateOf(chat.compressionThreshold.toString())};var richContent by remember(chat.id){mutableStateOf(chat.richContentEnabled)};var tavernFrontend by remember(chat.id){mutableStateOf(chat.tavernFrontendEnabled)};var customCss by remember(chat.id){mutableStateOf(chat.customCss)};var segmented by remember(chat.id){mutableStateOf(chat.segmentedRepliesEnabled)};var segmentParts by remember(chat.id){mutableIntStateOf(chat.segmentedReplyMaxParts.coerceAtLeast(2))};var imageGeneration by remember(chat.id){mutableStateOf(chat.imageGenerationEnabled)}
    var relayRoundsText by remember(chat.id){mutableStateOf(chat.groupRelayRounds.toString())}
    val chatImporter=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)pool.execute{runCatching{parseImportedMessages(String(bytes(contentResolver.openInputStream(uri)),Charsets.UTF_8))}.onSuccess{values->main.post{if(values.isEmpty())toast("没有识别到聊天消息")else{chat.messages.addAll(values);chat.updated=System.currentTimeMillis();save();uiRevision++;toast("已导入 ${values.size} 条聊天记录")}}}.onFailure{main.post{toast("聊天记录导入失败：${it.message}")}}}}
    DetailScaffold("对话设置",Page.CHAT){
      item{SettingButton(Icons.Outlined.Hub,"对话模型",activeProvider()?.let{"${it.name} · ${model()}"}?:"暂未配置模型供应商"){composePage=Page.CHAT_MODEL}}
      item{SettingButton(Icons.Outlined.Badge,"本对话用户身份",runCatching{identityFor(prefs,chat)}.getOrNull()?.let{if(it.name.isBlank()&&it.description.isBlank())"跟随全局身份" else it.name}.orEmpty().ifBlank{"跟随全局身份"}){composePage=Page.CHAT_IDENTITY}}
      item{SectionTitle("Token 使用")};item{ChatTokenSummary(chat.promptTokens,chat.completionTokens)}
      item{SettingButton(Icons.Outlined.DataUsage,"Token 消耗检查器",if(chat.lastContextAt>0)"最近统计 ${formatTaskTime(chat.lastContextAt)}" else "查看输入、输出与上下文来源估算"){composePage=Page.CHAT_CONTEXT}}
      if(lastApiError.isNotBlank())item{SettingButton(Icons.Outlined.ErrorOutline,"最近一次 API 报错",friendlyErrorText(lastApiError)){apiErrorBackPage=Page.CHAT_DETAIL;composePage=Page.API_ERROR}}
      item{SectionTitle("对话行为")}
      if(chat.group)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("角色接力交流",fontWeight=FontWeight.Medium);Text("成员可在一轮回复后继续自然交流",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupAutoRelay,onCheckedChange={chat.groupAutoRelay=it;save();uiRevision++})};if(chat.groupAutoRelay){Spacer(Modifier.height(10.dp));OutlinedTextField(relayRoundsText,{value->relayRoundsText=value.filter(Char::isDigit).take(4);relayRoundsText.toIntOrNull()?.takeIf{it in 1..9999}?.let{chat.groupRelayRounds=it;save()}},Modifier.fillMaxWidth(),label={Text("每次最多接力轮数（1-9999）")},singleLine=true,shape=RoundedCornerShape(12.dp))}}}}
      if(chat.group)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("全部角色自主交流",fontWeight=FontWeight.Medium);Text("未指定 @ 时随机选择成员，并允许成员间接力",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupAutonomous,onCheckedChange={chat.groupAutonomous=it;save();uiRevision++})};Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("自行决定是否接话",fontWeight=FontWeight.Medium);Text("角色可根据人格和话题选择保持沉默",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupReplyDecision,onCheckedChange={chat.groupReplyDecision=it;save();uiRevision++})};if(chat.groupReplyDecision){Text("接话倾向 ${chat.groupReplyProbability}%",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Slider(chat.groupReplyProbability.toFloat(),{chat.groupReplyProbability=it.toInt();save()},valueRange=0f..100f,steps=19)};Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许主动开启话题",fontWeight=FontWeight.Medium);Text("冷场时角色可按自身经历自然发起讨论",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.groupActiveTopics,onCheckedChange={chat.groupActiveTopics=it;save();uiRevision++})}}}}
      if(!chat.group)item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("沉浸式私聊",fontWeight=FontWeight.Medium);Text("回复前合并连续消息，角色读完后自行决定发送几条",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.immersiveMode,onCheckedChange={chat.immersiveMode=it;save();uiRevision++})};if(chat.immersiveMode)Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色决定是否回复",fontWeight=FontWeight.Medium);Text("角色可根据关系和语境选择暂不回复",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.privateReplyDecision,onCheckedChange={chat.privateReplyDecision=it;save();uiRevision++})}}}}
      if(!chat.group)item{Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("拉黑 ${card.name}",fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("开启后消息只保存在本机，不会交付给角色",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.userBlockedRole,onCheckedChange={blocked->if(blocked&&isChatSending(chat.id))stopGeneration(chat.id);chat.userBlockedRole=blocked;val event=Msg("system",if(blocked)"你已将 ${card.name} 拉黑" else "你已解除对 ${card.name} 的拉黑");event.messageType="relationship_event";event.speakerCardId=card.id;chat.messages.add(event);chat.updated=System.currentTimeMillis();save();ActiveReplyWorker.sync(this@ComposeActivity);YandereMessageService.sync(this@ComposeActivity);uiRevision++})};if(chat.roleBlockedUser){HorizontalDivider(Modifier.padding(vertical=11.dp));Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Block,null,Modifier.size(19.dp),tint=MaterialTheme.colorScheme.error);Column(Modifier.padding(start=9.dp)){Text("你已被 ${card.name} 拉黑",fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.error);Text("仍可发送消息；只有角色主动解除后才会收到回复",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}
      if(chat.group)item{InfoBanner(Icons.Outlined.AlternateEmail,"指定角色回答","在消息中输入 @角色名，或点击聊天页顶部成员，即可指定首位回复角色。")}
      item{SettingButton(Icons.Outlined.MenuBook,"日记",if(db.diaries(card.id).isEmpty())"写日记、阅读或与角色交换" else "本机已有 ${db.diaries(card.id).size} 篇"){composePage=Page.DIARIES}}
      item{SettingButton(Icons.Outlined.RecordVoiceOver,"语音回复",if(chat.ttsEnabled)listOf("","仅语音","文字与语音","随机回复").getOrElse(chat.ttsReplyMode){"已启用"} else "未启用 · 配置本地或在线 TTS"){composePage=Page.CHAT_TTS}}
      item{SettingButton(Icons.Outlined.NotificationsActive,"主动消息",if(chat.proactiveEnabled)"${chat.proactiveMinMinutes}-${chat.proactiveMaxMinutes} 分钟 · 每次 ${chat.proactiveBurstCount} 条" else "未开启 · 设置频率、连发与免打扰"){composePage=Page.PROACTIVE}}
      if(!chat.group)item{SettingButton(Icons.Outlined.Favorite,"病娇模式",if(chat.yandereEnabled)"App 退到后台后每分钟主动联系" else "未开启 · 独立配置后台持续联系"){composePage=Page.YANDERE}}
      item{SettingButton(Icons.Outlined.Share,"分享朋友圈给角色","选择一条动态发送到当前对话"){composePage=Page.MOMENT_SHARE}}
      if(card.id in momentRoleIds("moments_post_roles"))item{SettingButton(Icons.Outlined.Collections,"邀请角色发朋友圈","${card.name} 已获得发布授权"){momentRolePostBackPage=Page.CHAT_DETAIL;composePage=Page.MOMENT_ROLE_POST}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("发送后平滑滚动到底部",fontWeight=FontWeight.Medium);Text("稍作停顿后滑到底部等待生成内容",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=chat.autoScrollAfterSend,onCheckedChange={chat.autoScrollAfterSend=it;save();uiRevision++})}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("角色分段回复",fontWeight=FontWeight.Medium);Text("将长回复拆成多个连续消息气泡",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=segmented,onCheckedChange={segmented=it;chat.segmentedRepliesEnabled=it;save();uiRevision++})};if(segmented){var segmentPartsText by remember(chat.id){mutableStateOf(segmentParts.toString())};OutlinedTextField(value=segmentPartsText,onValueChange={raw->val digits=raw.filter(Char::isDigit).take(3);segmentPartsText=digits;digits.toIntOrNull()?.takeIf{it>=2}?.let{segmentParts=it;chat.segmentedReplyMaxParts=it;save()}},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("最多分段数量")},supportingText={Text("可自由填写，最少 2 段")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(12.dp))}}}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("允许角色调用生图",fontWeight=FontWeight.Medium);Text(if(prefs.getBoolean("image_generation_enabled",false))"角色可按剧情需要生成图片" else "全局生图尚未开启",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=imageGeneration,onCheckedChange={imageGeneration=it;chat.imageGenerationEnabled=it;save();uiRevision++},enabled=prefs.getBoolean("image_generation_enabled",false))}}}
      item{SettingButton(Icons.Outlined.Image,"生图模型配置",prefs.getString("image_generation_model","")?.takeIf{it.isNotBlank()}?:"未配置"){imageGenerationBackPage=Page.CHAT_DETAIL;composePage=Page.IMAGE_GENERATION}}
      item{SettingButton(Icons.Outlined.Bookmarks,"剧情节点","已保存 ${chat.plotNodes.size} 个 · 可快速回溯对话"){composePage=Page.PLOT_NODES}}
      item{SectionTitle("角色扮演内容")};item{SettingButton(Icons.Outlined.AutoStories,"世界书、正则与预设",if(chat.worldBookOverride||chat.regexRulesOverride||chat.presetOverride)"当前对话使用自定义覆盖" else if(card.worldBook.isNotBlank()||card.regexRules.isNotBlank()||card.preset.isNotBlank())"正在使用角色卡内置内容" else if(commonWorldBook.isBlank()&&commonRegexRules.isBlank())"角色卡没有内置内容" else "角色卡没有内置内容 · 未覆盖时回退公共内容"){composePage=Page.CHAT_CONTENT}}
      item{ChatSettingSwitch("允许角色调用 MCP 工具","当前对话",prefs.getBoolean("chat_mcp_enabled_${chat.id}",true)){enabled->prefs.edit().putBoolean("chat_mcp_enabled_${chat.id}",enabled).apply();uiRevision++}}
      item{SectionTitle("消息显示与输入")}
      item{SettingButton(Icons.Outlined.Wallpaper,"当前对话背景",if(chat.backgroundImage.isNotBlank()||chat.backgroundColor.isNotBlank())"已覆盖全局背景" else "跟随全局聊天背景"){composePage=Page.CHAT_BACKGROUND}}
      item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(15.dp)){ChatSettingSwitch("流式输出","逐步显示角色正在生成的内容",chat.streamEnabled){chat.streamEnabled=it;save();uiRevision++};ChatSettingSwitch("Markdown","显示标题、强调、列表和代码样式",chat.markdownEnabled){chat.markdownEnabled=it;save();uiRevision++};ChatSettingSwitch("显示模型思考","模型返回思考字段时可展开查看",chat.showThinking){chat.showThinking=it;save();uiRevision++};ChatSettingSwitch("回车发送","软键盘发送键和外接键盘回车直接发送",chat.enterToSend){chat.enterToSend=it;save();uiRevision++}}}}
      item{SectionTitle("长记忆")};item{Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("长记忆模式",fontWeight=FontWeight.Medium);Text("达到阈值后自动生成摘要压缩上下文",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=longMemory,onCheckedChange={longMemory=it})}}};item{StyledField(threshold,{threshold=it.filter(Char::isDigit).take(4)},"压缩阈值（10-9999 条消息）")};item{Button(onClick={chat.longMemory=longMemory;chat.compressionThreshold=(threshold.toIntOrNull()?:40).coerceIn(10,9999);save();uiRevision++;toast("长记忆设置已保存")},Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(8.dp));Text("保存长记忆设置",maxLines=1,softWrap=false)}};item{SettingButton(Icons.Outlined.Summarize,"记忆概要",if(chat.summary.isBlank())"暂无摘要" else "${chat.summary.take(90)}${if(chat.summary.length>90)"…" else ""}"){composePage=Page.MEMORY_SUMMARY}}
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
          Row(verticalAlignment=Alignment.CenterVertically){Text("${taskTypeLabel(task.type)} · ${formatTaskTime(task.updatedAt)}",Modifier.weight(1f),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(task.sourceId.isNotBlank()&&chats.any{it.id==task.sourceId})IconButton(onClick={selectedChat=chats.firstOrNull{it.id==task.sourceId};composePage=Page.CHAT},Modifier.size(36.dp)){Icon(Icons.Outlined.OpenInNew,"打开对话",Modifier.size(18.dp))};if(task.active()&&task.cancellable&&isChatSending(task.sourceId))IconButton(onClick={stopGeneration(task.sourceId);tasks=GenerationTaskTracker.list(this@ComposeActivity)},Modifier.size(36.dp)){Icon(Icons.Filled.Stop,"停止生成",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.error)}}
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
      val meta=JSONObject().put("id",post.id).put("authorId",post.authorId).put("authorName",post.authorName).put("content",post.content).put("createdAt",post.createdAt).toString()
      val message=Msg("user",post.content.ifBlank{"[图片动态]"});message.messageType="moment_share";message.meta=meta;message.image=post.image;chat.messages.add(message);chat.updated=System.currentTimeMillis();save();composePage=Page.CHAT;uiRevision++
      if(isChatSending(chat.id))queueReply(chat,card)else request(chat,card)
    }
    DetailScaffold("分享朋友圈",Page.CHAT){
      item{InfoBanner(Icons.Outlined.Share,"发送给当前角色","选择后会作为一条消息出现在对话中，角色可以看到动态正文和图片并自然回应。")}
      if(posts.isEmpty())item{EmptyState(Icons.Outlined.Collections,"还没有朋友圈动态","先到朋友圈发布一条内容")}
      items(posts,key={it.id}){post->Surface(onClick={share(post)},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){val role=find(post.authorId);if(role!=null)RoleAvatar(role,42)else UserAvatar(42);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(post.authorName,fontWeight=FontWeight.SemiBold,maxLines=1);Text(post.content.ifBlank{"[图片动态]"},fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.Send,"分享",tint=MaterialTheme.colorScheme.primary)}}}
    }
  }

  private fun formatDuration(value:Long):String{if(value<0)return "未知";val minutes=value/60_000L;return if(minutes<60)"${minutes} 分钟" else "${minutes/60} 小时 ${minutes%60} 分钟"}

  @Composable
  private fun WorldBookEntryCard(entry:WorldBookEntryView,onChange:(WorldBookEntryView)->Unit,onDelete:()->Unit){
    var expanded by remember(entry.id){mutableStateOf(false)}
    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(12.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){Icon(if(entry.constant)Icons.Outlined.PushPin else Icons.Outlined.MenuBook,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(horizontal=9.dp)){Text(entry.title.ifBlank{"未命名条目"},fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(if(entry.keywords.isBlank())"无关键词" else entry.keywords,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=entry.enabled,onCheckedChange={onChange(entry.copy(enabled=it))});IconButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,"展开条目")}}
      if(expanded){
        OutlinedTextField(entry.title,{onChange(entry.copy(title=it))},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("条目名称")},singleLine=true,shape=RoundedCornerShape(10.dp))
        OutlinedTextField(entry.keywords,{onChange(entry.copy(keywords=it))},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("关键词，用逗号分隔")},singleLine=false,maxLines=3,shape=RoundedCornerShape(10.dp))
        OutlinedTextField(entry.content,{onChange(entry.copy(content=it))},Modifier.fillMaxWidth().padding(top=8.dp).heightIn(min=96.dp,max=220.dp),label={Text("条目内容")},minLines=4,maxLines=10,shape=RoundedCornerShape(10.dp))
        Row(Modifier.fillMaxWidth().padding(top=5.dp),verticalAlignment=Alignment.CenterVertically){Text("常驻注入",Modifier.weight(1f),fontSize=12.sp);Switch(checked=entry.constant,onCheckedChange={onChange(entry.copy(constant=it))});TextButton(onClick=onDelete){Icon(Icons.Outlined.Delete,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(4.dp));Text("删除",color=MaterialTheme.colorScheme.error)}}
      }else if(entry.content.isNotBlank())Text(entry.content,Modifier.padding(start=33.dp,top=6.dp),fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
  }

  @Composable
  private fun WorldBookEntryPreview(entry:WorldBookEntryView){
    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surface){
      Column(Modifier.fillMaxWidth().padding(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
          Icon(if(entry.constant)Icons.Outlined.PushPin else Icons.Outlined.MenuBook,null,tint=MaterialTheme.colorScheme.primary)
          Column(Modifier.weight(1f).padding(start=9.dp)){
            Text(entry.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(if(entry.keywords.isBlank())"无关键词" else entry.keywords,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
          }
          Text(if(entry.enabled)"启用" else "停用",fontSize=11.sp,color=if(entry.enabled)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        if(entry.content.isNotBlank())Text(entry.content,Modifier.padding(start=33.dp,top=7.dp),fontSize=12.sp,lineHeight=18.sp,maxLines=5,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }

  @Composable
  private fun ChatIdentityPage(chat:Chat){
    var values by remember(uiRevision){mutableStateOf(identities())}
    val selectedId=chat.identityId
    val globalName=prefs.getString("active_identity_name","未设置").orEmpty().ifBlank{"未设置"}
    DetailScaffold("本对话用户身份",Page.CHAT_DETAIL){
      item{InfoBanner(Icons.Outlined.Badge,"每个角色对话可单独选择","这个身份只会用于当前对话；选择跟随全局后，切换全局身份会自动影响未单独指定的对话。")}
      item{Surface(onClick={chat.identityId="";chat.identityName="";chat.identityDescription="";save();uiRevision++;toast("已改为跟随全局身份")},shape=RoundedCornerShape(12.dp),color=if(selectedId.isBlank())MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(selectedId.isBlank())Icons.Filled.CheckCircle else Icons.Outlined.Badge,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=11.dp)){Text("跟随全局身份",fontWeight=FontWeight.SemiBold);Text("当前全局：$globalName",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
      item{SectionTitle("已保存身份")}
      if(values.isEmpty())item{EmptyState(Icons.Outlined.Badge,"还没有身份设定","请先在用户资料中创建身份设定")}
      items(values,key={it.id}){value->Surface(onClick={chat.identityId=value.id;chat.identityName=value.name;chat.identityDescription=value.description;save();uiRevision++;toast("当前对话已使用：${value.name}")},shape=RoundedCornerShape(12.dp),color=if(value.id==selectedId)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(value.id==selectedId)Icons.Filled.CheckCircle else Icons.Outlined.Badge,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=11.dp)){Text(value.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(value.description,fontSize=12.sp,maxLines=3,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
    }
  }

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
    val visualEntries=remember(worldBook){worldBookEntries(worldBook)}
    var showWorldBookRaw by remember(chat.id){mutableStateOf(visualEntries==null)}
    DetailScaffold("世界书与预设", Page.CHAT_DETAIL) {
      item { InfoBanner(Icons.Outlined.AutoStories, "按当前对话单独编辑", "关闭覆盖时继承角色卡内容（角色卡为空时使用公共世界书/正则）；开启后可修改或清空，且只影响当前对话。") }
      item { SectionTitle("世界书") }
      item { ContentOverrideSwitch("覆盖角色卡世界书",worldBookOverride){worldBookOverride=it;worldBook=if(it)(chat.worldBook.takeIf{value->value.isNotEmpty()}?:card.worldBook)else card.worldBook} }
      item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){OutlinedButton(onClick={importingWorldBook=true;worldBookImporter.launch(arrayOf("application/json","text/plain","*/*"))},modifier=Modifier.weight(1f).height(42.dp)){Icon(Icons.Outlined.FileOpen,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(if(importingWorldBook)"导入中" else "导入世界书",maxLines=1,softWrap=false)};OutlinedButton(onClick={worldBook=card.worldBook;worldBookOverride=true},modifier=Modifier.weight(1f).height(42.dp)){Icon(Icons.Outlined.Restore,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("载入卡片原版",maxLines=1,softWrap=false)}} }
      if(visualEntries!=null&&!showWorldBookRaw){
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("可视化条目 ${visualEntries.size} 个",fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));TextButton(onClick={worldBook=addWorldBookEntry(worldBook);worldBookOverride=true}){Icon(Icons.Outlined.Add,null,Modifier.size(17.dp));Spacer(Modifier.width(4.dp));Text("添加条目")};TextButton(onClick={showWorldBookRaw=true}){Text("查看 JSON")}}}
        if(visualEntries.isEmpty())item{InfoBanner(Icons.Outlined.MenuBook,"世界书暂无条目","可以点击添加条目，或切换到 JSON 视图导入完整结构。")}
        itemsIndexed(visualEntries,key={_,entry->entry.id}){_,entry->WorldBookEntryCard(entry,{updated->worldBook=updateWorldBookEntry(worldBook,updated);worldBookOverride=true},{askDanger("删除世界书条目","删除后需要保存内容规则才会写入对话。","确认删除"){worldBook=removeWorldBookEntry(worldBook,entry);worldBookOverride=true}})}
      }else{
        item{OutlinedTextField(worldBook,{worldBook=it;worldBookOverride=true},Modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),label={Text("世界书 JSON 或文本")},minLines=5,maxLines=10,shape=RoundedCornerShape(14.dp))}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={runCatching{val value=worldBook.trim();worldBook=if(value.startsWith("["))JSONArray(value).toString(2)else JSONObject(value).toString(2);worldBookOverride=true;showWorldBookRaw=false}.onFailure{toast("世界书不是有效 JSON")}}){Icon(Icons.Outlined.FormatAlignLeft,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("格式化并显示条目")}}}
      }
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

  private fun commonWorldBookEntryCount():Int=worldBookEntries(commonWorldBook)?.size?:if(commonWorldBook.isBlank())0 else 1

  @Composable
  private fun CommonRulesPage() {
    var worldBook by remember(uiRevision) { mutableStateOf(commonWorldBook) }
    var regexRules by remember(uiRevision) { mutableStateOf(commonRegexRules) }
    val visualEntries = remember(worldBook) { worldBookEntries(worldBook) }
    var showWorldBookRaw by remember(uiRevision) { mutableStateOf(visualEntries == null && worldBook.isNotBlank()) }
    DetailScaffold("公共世界书与正则", Page.APP_BEHAVIOR) {
      item { InfoBanner(Icons.Outlined.AutoStories, "全局兜底内容", "角色卡和当前会话都没有配置世界书或正则时，自动使用这里的内容；任何一方配置了自己的内容就优先使用自己的。") }
      item { SectionTitle("公共世界书") }
      if (visualEntries != null && !showWorldBookRaw) {
        item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("可视化条目 ${visualEntries.size} 个",fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));TextButton(onClick={worldBook=addWorldBookEntry(worldBook)}){Icon(Icons.Outlined.Add,null,Modifier.size(17.dp));Spacer(Modifier.width(4.dp));Text("添加条目")};TextButton(onClick={showWorldBookRaw=true}){Text("查看 JSON")}} }
        if (visualEntries.isEmpty()) item { InfoBanner(Icons.Outlined.MenuBook,"公共世界书暂无条目","点击添加条目创建全局兜底设定，或切换到 JSON 视图导入完整结构。") }
        itemsIndexed(visualEntries,key={_,entry->entry.id}){_,entry->WorldBookEntryCard(entry,{updated->worldBook=updateWorldBookEntry(worldBook,updated)},{askDanger("删除公共世界书条目","删除后需要点击保存才会写入公共内容。","确认删除"){worldBook=removeWorldBookEntry(worldBook,entry)}})}
      } else {
        item { OutlinedTextField(worldBook,{worldBook=it},Modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),label={Text("公共世界书 JSON 或文本")},minLines=5,maxLines=10,shape=RoundedCornerShape(14.dp)) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={runCatching{val value=worldBook.trim();worldBook=if(value.startsWith("["))JSONArray(value).toString(2)else JSONObject(value).toString(2);showWorldBookRaw=false}.onFailure{toast("公共世界书不是有效 JSON")}}){Icon(Icons.Outlined.FormatAlignLeft,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("格式化并显示条目")}} }
      }
      item { SectionTitle("公共正则") }
      item { OutlinedTextField(regexRules,{regexRules=it},Modifier.fillMaxWidth().heightIn(min=110.dp,max=200.dp),label={Text("公共正则脚本 JSON 或文本")},minLines=4,maxLines=9,shape=RoundedCornerShape(14.dp)) }
      item { Button(onClick={commonWorldBook=worldBook.trim();commonRegexRules=regexRules.trim();save();uiRevision++;toast("公共世界书与正则已保存")},modifier=Modifier.fillMaxWidth().height(46.dp)){Icon(Icons.Outlined.Save,null);Spacer(Modifier.width(6.dp));Text("保存公共规则",maxLines=1,softWrap=false)} }
    }
  }

  @Composable
  private fun AppBehaviorPage() {
    var active by remember { mutableStateOf(prefs.getBoolean("active_replies", false)) }
    var keepAlive by remember { mutableStateOf(prefs.getBoolean("notification_keep_alive", false)) }
    var relationshipCare by remember { mutableStateOf(prefs.getBoolean("relationship_care_enabled", true)) }
    val batteryAllowed=batteryWhitelistEnabled()
    DetailScaffold("应用通知") {
      item { InfoBanner(Icons.Outlined.NotificationsActive,"后台消息推送","普通 AI 回复和角色主动消息在后台完成后都会通知；点击通知可直接回到对应对话。") }
      item { SettingButton(Icons.Outlined.Tune,"通用聊天默认设置","新建私聊与群聊自动使用，可在单个对话中覆盖") { composePage=Page.CHAT_DEFAULTS } }
      item { SettingButton(Icons.Outlined.AutoStories,"公共世界书与正则",if(commonWorldBook.isBlank()&&commonRegexRules.isBlank())"未配置 · 角色与会话都缺失时作为兜底内容" else "世界书 ${commonWorldBookEntryCount()} 条 · 正则 ${if(commonRegexRules.isBlank())"未配置" else "已配置"}") { composePage=Page.COMMON_RULES } }
      item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("深夜聊天关怀",fontWeight=FontWeight.Medium);Text("深夜连续聊天一段时间后，角色会自然关心休息与身体",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=relationshipCare,onCheckedChange={relationshipCare=it;prefs.edit().putBoolean("relationship_care_enabled",it).apply();toast(if(it)"深夜关怀已开启" else "深夜关怀已关闭")})}} }
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
      item{Button(onClick={openWebLink("https://docx.134506.xyz/shiyuzan.html","无法打开赞助页面")},Modifier.fillMaxWidth().height(50.dp)){Icon(Icons.Outlined.VolunteerActivism,null);Spacer(Modifier.width(8.dp));Text("前往赞助页面",maxLines=1,softWrap=false)}}
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
  private fun FileBubble(msg:Msg){
    val data=runCatching{JSONObject(if(msg.meta.isBlank())"{}" else msg.meta)}.getOrElse{JSONObject()}
    val name=data.optString("name",msg.body.ifBlank{"附件"})
    val size=data.optLong("size",-1L)
    Surface(onClick={openChatFile(msg.file)},shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.85f),modifier=Modifier.widthIn(max=260.dp)){
      Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Icon(Icons.Outlined.InsertDriveFile,null,tint=MaterialTheme.colorScheme.primary)}
        Column(Modifier.weight(1f).padding(start=10.dp)){Text(name,fontSize=13.sp,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis);Text(if(size>=0)"点击打开 · "+android.text.format.Formatter.formatShortFileSize(this@ComposeActivity,size) else "点击打开",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Icon(Icons.Outlined.OpenInNew,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
  @Composable private fun LinkPreviewCard(url:String){
    val meta by produceState<UrlMetadataFetcher.Metadata?>(null,url){value=withContext(Dispatchers.IO){UrlMetadataFetcher.fetch(url)}}
    val cover=meta?.image.orEmpty()
    Surface(onClick={tryOpenWebLink(url)},shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.6f),modifier=Modifier.fillMaxWidth().padding(top=6.dp)){
      Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
        if(cover.isNotBlank())UrlImage(cover,Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f).padding(start=if(cover.isNotBlank())10.dp else 0.dp)){
          Text(meta?.title?.takeIf{it.isNotBlank()}?:url,fontSize=13.sp,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
          Text(meta?.description?.takeIf{it.isNotBlank()}?:"网页链接 · 点击打开",fontSize=11.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.OpenInNew,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary)
      }
    }
  }
  @Composable private fun UrlImage(url:String,modifier:Modifier){
    val bitmap by produceState<android.graphics.Bitmap?>(null,url){value=withContext(Dispatchers.IO){runCatching{val connection=java.net.URL(url).openConnection() as java.net.HttpURLConnection;connection.connectTimeout=8000;connection.readTimeout=8000;connection.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13)");connection.inputStream.use{android.graphics.BitmapFactory.decodeStream(it)}}.getOrNull()}}
    if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"网页封面",modifier,contentScale=ContentScale.Crop)
  }
  @OptIn(ExperimentalFoundationApi::class)
  @Composable private fun MessageBubble(card:Card,chat:Chat,msg:Msg,onLongPress:(Msg)->Unit,onCopy:()->Unit,onRetry:()->Unit,onErrorDetails:()->Unit,onConfigureVision:()->Unit={},onImageClick:()->Unit,showActions:Boolean,onImageRequest:()->Unit,onEditImageRequest:()->Unit,onCancelImageRequest:()->Unit,onClaimRedPacket:()->Unit,onVoiceCall:(Msg,Card)->Unit,onPatRole:(Card)->Unit,onImageGenRequest:()->Unit={},suggestionsLoading:Boolean=false,onSuggestions:(()->Unit)?=null){
    val mine=msg.role=="user";val speaker=find(msg.speakerCardId)?.takeIf{!mine}?:card
    if(msg.messageType in listOf("pat","red_packet_receipt","moment_post","screen_context_share","desktop_pet_screen_context","relationship_event","tool_event")){
      val screenShare=msg.messageType in listOf("screen_context_share","desktop_pet_screen_context")
      val systemText=when{screenShare->"我分享了当前屏幕给角色";msg.messageType=="pat"&&!mine->"${speaker.name} 拍了拍我的 ${msg.body}";msg.messageType=="moment_post"->"${speaker.name} 发布了朋友圈：${msg.body}";else->msg.body}
      val systemIcon=when{msg.messageType=="tool_event"->Icons.Outlined.Build;screenShare->Icons.Outlined.Visibility;msg.messageType=="pat"->Icons.Outlined.TouchApp;msg.messageType=="moment_post"->Icons.Outlined.Collections;msg.messageType=="relationship_event"->Icons.Outlined.Block;else->Icons.Outlined.Redeem}
      Box(Modifier.fillMaxWidth().padding(vertical=3.dp),contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.82f),modifier=Modifier.combinedClickable(onClick={},onLongClick={onLongPress(msg)})){Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(systemIcon,null,Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.width(6.dp));Text(systemText,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)}}}
      return
    }
    val frontendContent=!mine&&chat.tavernFrontendEnabled&&(msg.messageType=="tavern_frontend"||looksLikeTavernFrontend(msg.body))
    val noBubbles=prefs.getBoolean("chat_no_bubbles",false)
    val hideAvatars=prefs.getBoolean("chat_hide_avatars",noBubbles)
    val weixinStyle=prefs.getInt("compose_theme_style",0)==4
    val bareUrl=if(msg.messageType=="text"&&msg.image.isBlank()&&msg.file.isBlank())msg.body.trim().takeIf{value->value.length in 4..2048&&android.util.Patterns.WEB_URL.matcher(value).matches()}else null
    var reasoningExpanded by remember(msg.id){mutableStateOf(false)}
    fun messageClick(){
      val data=runCatching{JSONObject(msg.meta)}.getOrElse{JSONObject()}
      when{
        !mine&&msg.messageType=="red_packet"->onClaimRedPacket()
        msg.messageType=="location"->{val lat=data.optString("latitude");val lng=data.optString("longitude");val query=if(lat.isNotBlank()&&lng.isNotBlank())"$lat,$lng" else data.optString("address",data.optString("name"));openWebLink("https://uri.amap.com/marker?position=${java.net.URLEncoder.encode(if(lat.isNotBlank()&&lng.isNotBlank())"$lng,$lat" else "","UTF-8")}&name=${java.net.URLEncoder.encode(data.optString("name",query),"UTF-8")}&src=shiyu","无法打开地图")}
        msg.messageType=="music"->toggleChatMusic(data.optString("url"),data.optString("title",msg.body),data.optString("artist","未知歌手"),data.optString("album"),data.optLong("songId"),data.optString("roleId"),data.optString("roleName"),data.optString("coverUrl"))
        msg.messageType=="video"->{runCatching{startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(videoUri(this@ComposeActivity,data.optString("file")),"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{toast("无法播放视频，请确认文件存在且已安装播放器")}}
        !mine&&msg.messageType=="voice_call"&&data.optString("state","邀请") in listOf("邀请","接听")->{
          if(data.optString("state","邀请")=="邀请")onVoiceCall(msg,speaker)
        }
      }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()){
      val avatarSpace=if(hideAvatars)8.dp else 54.dp
      val bubbleMax=if(frontendContent||noBubbles)(maxWidth-avatarSpace).coerceAtLeast(180.dp) else minOf(maxWidth-avatarSpace,prefs.getInt("chat_bubble_max_width",292).coerceIn(220,380).dp).coerceAtLeast(140.dp)
      val mineBase=storedColor(prefs.getString("chat_user_bubble_color","").orEmpty(),if(weixinStyle)Color(0xFF95EC69)else MaterialTheme.colorScheme.primary)
      val aiBase=storedColor(prefs.getString("chat_ai_bubble_color","").orEmpty(),if(weixinStyle)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface)
      val mineColor=mineBase.copy(alpha=(mineBase.alpha*prefs.getInt("chat_user_bubble_opacity",100).coerceIn(10,100)/100f).coerceIn(0f,1f))
      val aiColor=aiBase.copy(alpha=(aiBase.alpha*prefs.getInt("chat_ai_bubble_opacity",100).coerceIn(10,100)/100f).coerceIn(0f,1f))
      val mineText=if(noBubbles)MaterialTheme.colorScheme.onBackground else if(mineBase.luminance()>.55f)Color(0xFF202322) else Color.White
      val aiText=if(noBubbles)MaterialTheme.colorScheme.onBackground else if(aiBase.luminance()>.55f)Color(0xFF202322) else Color.White
      val verticalPadding=prefs.getInt("chat_bubble_vertical_padding",10).coerceIn(6,20).dp
      Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start,verticalAlignment=Alignment.Top){
        if(!mine&&!hideAvatars){Box(Modifier.padding(top=2.dp).combinedClickable(onClick={},onDoubleClick={onPatRole(speaker)})){RoleAvatar(speaker,38)};Spacer(Modifier.width(8.dp))}
        Column(Modifier.widthIn(max=bubbleMax),horizontalAlignment=if(mine)Alignment.End else Alignment.Start){
          Surface(modifier=Modifier.widthIn(max=bubbleMax).combinedClickable(onClick={messageClick()},onLongClick={onLongPress(msg)}),shape=RoundedCornerShape(if(weixinStyle)6.dp else if(mine)18.dp else 14.dp),color=if(noBubbles&&!msg.failed||msg.messageType=="red_packet"||(msg.messageType=="sticker"&&msg.body.isBlank())||(!mine&&msg.loading&&chat.immersiveMode))Color.Transparent else if(msg.failed)MaterialTheme.colorScheme.errorContainer else if(mine)mineColor else aiColor){
          Column(horizontalAlignment=if(mine)Alignment.End else Alignment.Start){
            if(msg.loading&&msg.messageType!="image_request")if(chat.immersiveMode&&!chat.group)Spacer(Modifier.size(1.dp))else Row(Modifier.padding(horizontal=16.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.secondary);repeat(3){index->Box(Modifier.size((5+index).dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha=.45f+index*.2f)))}}
            if(!mine&&chat.group&&msg.speakerCardId.isNotBlank())Text(speaker.name,Modifier.padding(start=14.dp,top=8.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.secondary)
            if(!mine&&chat.showThinking&&msg.thinking.isNotBlank()){TextButton(onClick={reasoningExpanded=!reasoningExpanded},contentPadding=PaddingValues(horizontal=12.dp,vertical=2.dp),modifier=Modifier.height(34.dp)){Icon(Icons.Outlined.Psychology,null,Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text(if(reasoningExpanded)"收起思考" else if(msg.loading)"正在思考" else "查看思考",fontSize=11.sp)};AnimatedVisibility(reasoningExpanded){Column(Modifier.padding(horizontal=13.dp)){Text(msg.thinking,fontSize=11.sp,lineHeight=17.sp,color=if(mine)mineText.copy(alpha=.76f)else aiText.copy(alpha=.76f));HorizontalDivider(Modifier.padding(top=8.dp),color=(if(mine)mineText else aiText).copy(alpha=.15f))}}}
            if(msg.messageType=="red_packet")RedPacketBubble(msg.meta,msg.body,mine)
            else if(msg.messageType=="image_request")ImageRequestBubble(msg.meta,msg.loading,mine,onImageRequest,onEditImageRequest,onCancelImageRequest)
            else if(msg.messageType=="moment_share")MomentShareBubble(msg,onImageClick)
            else if(msg.messageType=="location")LocationBubble(msg.meta,msg.body)
            else if(msg.messageType=="transfer")TransferBubble(msg.meta,msg.body)
            else if(msg.messageType=="music")MusicBubble(msg.meta,msg.body)
            else if(msg.messageType=="video"){val videoData=runCatching{JSONObject(msg.meta)}.getOrElse{JSONObject()};Row(Modifier.widthIn(min=230.dp,max=310.dp).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.PlayCircle,"播放视频",Modifier.size(32.dp));Column(Modifier.padding(start=10.dp)){Text("AI 生成视频",fontWeight=FontWeight.SemiBold);Text(videoData.optString("prompt",msg.body),fontSize=12.sp,maxLines=3,overflow=TextOverflow.Ellipsis);Text("点击播放 · 本地 MP4",fontSize=10.sp)}}}
            else if(msg.messageType=="voice_call")VoiceCallBubble(msg.meta,msg.body,mine)
             else if(msg.messageType=="diary_share")DiaryShareBubble(msg.meta,msg.body,speaker.name,mine)
             else if(msg.messageType=="gift")GiftBubble(msg.meta,msg.body,msg.image,mine,onImageClick)
             else if(msg.messageType=="chat_share")ChatShareBubble(msg.meta,msg.body,msg.image,onImageClick)
             else if(msg.messageType=="file")FileBubble(msg)
            else {
              if(bareUrl!=null)LinkPreviewCard(bareUrl)
              if(msg.image.isNotBlank())MessageImage(msg.image,modifier=(if(msg.messageType=="sticker"&&msg.body.isBlank())Modifier.size(112.dp).padding(2.dp)else if(msg.messageType=="sticker")Modifier.padding(start=14.dp,top=10.dp,end=14.dp).size(72.dp)else Modifier.widthIn(max=260.dp).heightIn(max=300.dp).padding(5.dp).clip(RoundedCornerShape(12.dp))).combinedClickable(onClick=onImageClick,onLongClick={onLongPress(msg)}),contentScale=if(msg.messageType=="sticker")ContentScale.Fit else ContentScale.Crop)
              if(!mine&&msg.displayMode==1&&msg.body.isNotBlank())VoiceReplyBubble(msg.body,isSpeakingText(msg.body)){toggleSpeech(msg.body)}
              if(msg.body.isNotBlank()&&msg.displayMode!=1){if(frontendContent&&!msg.failed&&!msg.loading)HtmlMessage(renderTavernFrontend(msg.body,speaker,chat),chat.customCss,chat.richContentEnabled)else if(!mine&&chat.richContentEnabled&&!msg.failed&&!msg.loading)HtmlMessage(msg.body,chat.customCss,true)else {val font=prefs.getInt("chat_font_size",15).coerceIn(12,20);val lines=prefs.getInt("chat_line_height",22).coerceIn(font+2,32);val color=if(msg.failed)MaterialTheme.colorScheme.onErrorContainer else if(mine)mineText else aiText;if(chat.markdownEnabled)MarkdownMessage(msg.body,Modifier.padding(horizontal=if(noBubbles)4.dp else 14.dp,vertical=verticalPadding),font,lines,color)else Text(msg.body,Modifier.padding(horizontal=if(noBubbles)4.dp else 14.dp,vertical=verticalPadding),fontSize=font.sp,lineHeight=lines.sp,color=color)}}
            }
          }
        }
          if(mine&&!msg.delivered)Text("未送达",Modifier.padding(end=4.dp,top=2.dp),fontSize=10.sp,color=MaterialTheme.colorScheme.error)
          if(!mine&&!chat.immersiveMode&&!msg.loading&&!msg.failed&&(msg.body.isNotBlank()||msg.image.isNotBlank()))Text("内容由 AI 生成",Modifier.padding(start=4.dp,top=1.dp),fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.72f))
          if(!mine&&(msg.branchLoading||msg.alternatives.size>1))Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.82f),modifier=Modifier.padding(top=3.dp)){if(msg.branchLoading)Row(Modifier.height(32.dp).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp);Text("正在生成备选版本",Modifier.padding(start=7.dp),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}else Row(Modifier.height(32.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={msg.selectAlternative(msg.activeAlternative-1);save();uiRevision++},enabled=msg.activeAlternative>0,modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.ChevronLeft,"上一个回复版本",Modifier.size(17.dp))};Text("${msg.activeAlternative+1} / ${msg.alternatives.size}",Modifier.width(38.dp),fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant);IconButton(onClick={msg.selectAlternative(msg.activeAlternative+1);save();uiRevision++},enabled=msg.activeAlternative<msg.alternatives.lastIndex,modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.ChevronRight,"下一个回复版本",Modifier.size(17.dp))};IconButton(onClick={msg.keepCurrentAlternative();save();uiRevision++;toast("已保留当前回复版本")},modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.Done,"只保留当前版本",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary)}}}
          if(!mine&&msg.failed&&msg.errorDetail.startsWith(MainActivity.VISION_MODEL_REQUIRED_MARKER)){TextButton(onClick=onConfigureVision,modifier=Modifier.height(32.dp),contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)){Icon(Icons.Outlined.Visibility,null,Modifier.size(14.dp));Spacer(Modifier.width(4.dp));Text("配置识图模型",fontSize=11.sp)};TextButton(onClick=onRetry,modifier=Modifier.height(32.dp),contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)){Icon(Icons.Outlined.Refresh,null,Modifier.size(14.dp));Spacer(Modifier.width(4.dp));Text("重试",fontSize=11.sp)}}
          if(!mine&&msg.failed)TextButton(onClick=onErrorDetails,modifier=Modifier.height(30.dp),contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)){Icon(Icons.Outlined.BugReport,null,Modifier.size(14.dp));Spacer(Modifier.width(4.dp));Text("查看 API 报错",fontSize=11.sp)}
          if(!mine&&showActions){Row(Modifier.widthIn(max=bubbleMax).height(28.dp),verticalAlignment=Alignment.CenterVertically){if(msg.body.isNotBlank()){IconButton(onClick={toggleSpeech(msg.body)},modifier=Modifier.size(28.dp)){Icon(if(isSpeakingText(msg.body))Icons.Filled.Stop else Icons.Outlined.VolumeUp,if(isSpeakingText(msg.body))"停止朗读" else "朗读",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.secondary)};IconButton(onClick=onCopy,modifier=Modifier.size(28.dp)){Icon(Icons.Outlined.ContentCopy,"复制",Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}};if(onSuggestions!=null)IconButton(onClick=onSuggestions,enabled=!suggestionsLoading,modifier=Modifier.size(28.dp)){if(suggestionsLoading)CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp)else Icon(Icons.Outlined.AutoAwesome,"推荐回复",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.secondary)};if(msg.failed)IconButton(onClick=onRetry,modifier=Modifier.size(28.dp)){Icon(Icons.Outlined.Refresh,"重试",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.error)};IconButton(onClick={onLongPress(msg)},modifier=Modifier.size(28.dp)){Icon(Icons.Outlined.MoreHoriz,"更多操作",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}
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
  @Composable private fun RedPacketBubble(meta:String,fallback:String,mine:Boolean){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};val amount=data.optString("amount","0.00");val greeting=data.optString("greeting",fallback).ifBlank{"恭喜发财，万事如意"};val claimed=data.optBoolean("claimed",false);Surface(shape=RoundedCornerShape(15.dp),color=if(claimed)Color(0xFFB66B60) else Color(0xFFE9634F),modifier=Modifier.widthIn(min=210.dp,max=250.dp)){Column{Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha=.2f)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.CardGiftcard,null,tint=Color.White)};Column(Modifier.padding(start=10.dp)){Text(greeting,color=Color.White,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("¥ $amount",color=Color.White.copy(alpha=.9f),fontSize=12.sp)}};HorizontalDivider(color=Color.White.copy(alpha=.22f));Text(if(claimed)"红包已领取" else if(mine)"等待对方领取" else "点击领取红包",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontSize=10.sp,color=Color.White.copy(alpha=.84f))}}}
  @Composable private fun LocationBubble(meta:String,fallback:String){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};Row(Modifier.widthIn(min=210.dp,max=280.dp).padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer),contentAlignment=Alignment.Center){Icon(Icons.Outlined.LocationOn,null,tint=MaterialTheme.colorScheme.onSecondaryContainer)};Column(Modifier.padding(start=11.dp)){Text(data.optString("name",fallback),fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(data.optString("address","点击查看位置"),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}}}
  @Composable private fun TransferBubble(meta:String,fallback:String){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};Column(Modifier.widthIn(min=210.dp,max=260.dp).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Payments,null,tint=MaterialTheme.colorScheme.secondary);Text("转账",Modifier.padding(start=7.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("¥ ${data.optString("amount","0.00")}",Modifier.padding(top=7.dp),fontSize=24.sp,fontWeight=FontWeight.Bold);Text(data.optString("note",fallback),Modifier.padding(top=4.dp),fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}}
  @Composable private fun MusicBubble(meta:String,fallback:String){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};val url=data.optString("url").trim();val cover=data.optString("coverUrl").trim();val loading=url.isNotBlank()&&musicLoadingUrl==url;val playing=url.isNotBlank()&&musicPlayingUrl==url;val paused=url.isNotBlank()&&musicPausedUrl==url;Row(Modifier.widthIn(min=232.dp,max=310.dp).padding(11.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.tertiaryContainer),contentAlignment=Alignment.Center){if(cover.isNotBlank())RemoteArtwork(cover,Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))else Icon(if(playing)Icons.Filled.GraphicEq else Icons.Outlined.MusicNote,null,tint=MaterialTheme.colorScheme.onTertiaryContainer);if(playing)Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.20f)),contentAlignment=Alignment.Center){Icon(Icons.Filled.GraphicEq,null,Modifier.size(22.dp),tint=Color.White)}};Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(data.optString("title",fallback).ifBlank{"分享的音乐"},fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(when{loading->"正在加载";playing->"正在播放";paused->"已暂停";data.optBoolean("unavailable")->"网易云未找到可播放版本";url.isBlank()->"没有播放链接";else->data.optString("artist","未知歌手").ifBlank{"未知歌手"}},fontSize=12.sp,color=if(playing||paused)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};if(loading)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Icon(if(playing)Icons.Filled.PauseCircle else Icons.Outlined.PlayCircle,if(playing)"暂停" else "播放",Modifier.size(27.dp),tint=if(url.isBlank())MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.45f)else MaterialTheme.colorScheme.secondary)}}
  @Composable private fun VoiceCallBubble(meta:String,fallback:String,mine:Boolean){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};val state=data.optString("state","邀请");Row(Modifier.widthIn(min=210.dp,max=280.dp).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(CircleShape).background(if(state=="结束"||state=="拒绝")MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Icon(if(state=="结束"||state=="拒绝")Icons.Outlined.CallEnd else Icons.Outlined.Call,null,tint=if(state=="结束"||state=="拒绝")MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer)};Column(Modifier.padding(start=11.dp)){Text(when(state){"接听","通话中"->"语音通话中";"结束"->"语音通话已结束";"拒绝"->"语音通话已拒绝";"未接听"->"语音通话未接听";else->if(mine)"已发起语音通话" else "邀请你进行语音通话"},fontWeight=FontWeight.SemiBold);data.optString("note",fallback).takeIf{it.isNotBlank()&&it!="语音通话"}?.let{Text(it,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}}}}
  @Composable private fun DiaryShareBubble(meta:String,fallback:String,speakerName:String,mine:Boolean){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};Column(Modifier.widthIn(min=230.dp,max=300.dp).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.MenuBook,null,tint=MaterialTheme.colorScheme.secondary);Text(if(mine)"分享我的日记" else "$speakerName 分享了日记",Modifier.padding(start=8.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold)};Text(data.optString("title","无题日记"),Modifier.padding(top=9.dp),fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(data.optString("content",fallback),Modifier.padding(top=5.dp),fontSize=13.sp,lineHeight=19.sp,maxLines=6,overflow=TextOverflow.Ellipsis)}}
  @Composable private fun GiftBubble(meta:String,fallback:String,image:String,mine:Boolean,onImageClick:()->Unit){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};Column(Modifier.widthIn(min=220.dp,max=300.dp).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.tertiaryContainer),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Redeem,null,tint=MaterialTheme.colorScheme.onTertiaryContainer)};Column(Modifier.padding(start=10.dp)){Text(if(mine)"送出了一份礼物" else "收到了一份礼物",fontWeight=FontWeight.SemiBold);Text(data.optString("name",fallback).ifBlank{"神秘礼物"},fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)}};data.optString("wish",fallback).takeIf{it.isNotBlank()}?.let{Text(it,Modifier.padding(top=9.dp),fontSize=13.sp,lineHeight=19.sp,maxLines=4,overflow=TextOverflow.Ellipsis)};if(image.isNotBlank())MessageImage(image,Modifier.fillMaxWidth().padding(top=9.dp).heightIn(max=190.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick=onImageClick),ContentScale.Crop)}}
  @Composable private fun ChatShareBubble(meta:String,fallback:String,image:String,onImageClick:()->Unit){val data=remember(meta){runCatching{JSONObject(meta)}.getOrElse{JSONObject()}};Column(Modifier.widthIn(min=230.dp,max=310.dp).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.ForwardToInbox,null,tint=MaterialTheme.colorScheme.secondary);Column(Modifier.padding(start=8.dp)){Text("分享了一段聊天",fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text("来自“${data.optString("sourceChatTitle","其他聊天")}” · ${data.optString("sourceRole","未知")}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)}};Text(fallback.ifBlank{"[图片]"},Modifier.padding(top=9.dp),fontSize=13.sp,lineHeight=19.sp,maxLines=6,overflow=TextOverflow.Ellipsis);if(image.isNotBlank())MessageImage(image,Modifier.fillMaxWidth().padding(top=9.dp).heightIn(max=190.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick=onImageClick),ContentScale.Crop)}}
  @Composable private fun ImageRequestBubble(raw:String,running:Boolean,mine:Boolean,onConfirm:()->Unit,onEdit:()->Unit,onCancel:()->Unit){val prompt=remember(raw){imagePromptConfig(raw)};Column(Modifier.widthIn(min=240.dp,max=310.dp).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Brush,null,tint=MaterialTheme.colorScheme.secondary);Text(if(running)"正在生成图片" else if(mine)"你想生成一张图片" else "角色想生成一张图片",Modifier.padding(start=8.dp).weight(1f),fontWeight=FontWeight.SemiBold,fontSize=14.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis);if(!running)IconButton(onClick=onEdit,Modifier.size(32.dp)){Icon(Icons.Outlined.Edit,"编辑提示词",Modifier.size(18.dp))}};Text("正向 · ${prompt.positive}",Modifier.padding(top=8.dp),fontSize=12.sp,lineHeight=18.sp,maxLines=4,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);if(prompt.negative.isNotBlank())Text("负向 · ${prompt.negative}",Modifier.padding(top=5.dp),fontSize=11.sp,lineHeight=16.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.78f));if(running){LinearProgressIndicator(modifier=Modifier.fillMaxWidth().padding(top=12.dp));Text("生成时间取决于模型与设备性能",Modifier.padding(top=6.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis);OutlinedButton(onClick=onCancel,modifier=Modifier.fillMaxWidth().padding(top=9.dp).height(40.dp),contentPadding=PaddingValues(horizontal=8.dp)){Icon(Icons.Outlined.Close,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("取消生成",maxLines=1,softWrap=false)}}else Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onCancel,modifier=Modifier.weight(1f).height(40.dp),contentPadding=PaddingValues(horizontal=8.dp)){Text("取消",maxLines=1,softWrap=false)};Button(onClick=onConfirm,modifier=Modifier.weight(1f).height(40.dp),contentPadding=PaddingValues(horizontal=8.dp)){Icon(Icons.Outlined.Image,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("确认生成",maxLines=1,softWrap=false)}}}}
  @Composable private fun MomentShareBubble(msg:Msg,onImageClick:()->Unit){val data=remember(msg.meta){runCatching{JSONObject(msg.meta)}.getOrElse{JSONObject()}};Column(Modifier.widthIn(min=220.dp,max=300.dp).padding(13.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Collections,null,Modifier.size(18.dp));Text("分享了 ${data.optString("authorName","一条朋友圈")}",Modifier.padding(start=7.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold)};if(msg.body.isNotBlank())Text(msg.body,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=20.sp,maxLines=6,overflow=TextOverflow.Ellipsis);if(msg.image.isNotBlank())MessageImage(msg.image,Modifier.fillMaxWidth().padding(top=9.dp).aspectRatio(1.5f).clip(RoundedCornerShape(10.dp)).clickable(onClick=onImageClick))}}
  @Composable private fun ChatToolAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit){Column(Modifier.fillMaxWidth().height(80.dp).clickable(onClick=onClick),horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.size(58.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(icon,label,Modifier.size(25.dp),tint=MaterialTheme.colorScheme.secondary)}};Box(Modifier.fillMaxWidth().height(22.dp),contentAlignment=Alignment.BottomCenter){Text(label,fontSize=12.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}}}
  @OptIn(ExperimentalFoundationApi::class)
  @Composable private fun StickerTile(path:String,selected:Boolean,organizing:Boolean,modifier:Modifier=Modifier,onClick:()->Unit,onPreview:()->Unit){
    val scale by animateFloatAsState(if(selected).95f else 1f,tween(150),label="sticker-selection-scale")
    Surface(
      shape=RoundedCornerShape(12.dp),
      color=if(selected)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
      border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha=.32f)),
      shadowElevation=if(selected)3.dp else 0.dp,
      modifier=modifier.aspectRatio(1f).graphicsLayer{scaleX=scale;scaleY=scale}.combinedClickable(onClick=onClick,onLongClick=onPreview)
    ){
      Box{
        MessageImage(path,Modifier.fillMaxSize().padding(6.dp),ContentScale.Fit)
        if(selected)Surface(
          shape=CircleShape,
          color=MaterialTheme.colorScheme.secondary,
          modifier=Modifier.align(Alignment.TopEnd).padding(5.dp).size(22.dp)
        ){Box(contentAlignment=Alignment.Center){Icon(Icons.Filled.Check,"已选择",Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSecondary)}}
        else if(organizing)Icon(Icons.Outlined.RadioButtonUnchecked,"可选择",Modifier.align(Alignment.TopEnd).padding(7.dp).size(17.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.62f))
      }
    }
  }
  @Composable private fun ChatSettingSwitch(title:String,subtitle:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=14.sp,fontWeight=FontWeight.Medium);Text(subtitle,fontSize=11.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked,onChange)}}
  @Composable private fun MarkdownMessage(source:String,modifier:Modifier,font:Int,lineHeight:Int,color:Color){Text(markdownAnnotated(source,color,font),modifier,fontSize=font.sp,lineHeight=lineHeight.sp,color=color)}
  private fun markdownAnnotated(source:String,color:Color,font:Int):AnnotatedString=buildAnnotatedString{
    fun inline(value:String){var index=0;while(index<value.length){when{value.startsWith("**",index)->{val end=value.indexOf("**",index+2);if(end>index+2){withStyle(SpanStyle(fontWeight=FontWeight.Bold)){append(value.substring(index+2,end))};index=end+2}else{append(value[index]);index++}};value[index]=='`'->{val end=value.indexOf('`',index+1);if(end>index+1){withStyle(SpanStyle(fontFamily=FontFamily.Monospace,background=color.copy(alpha=.1f))){append(value.substring(index+1,end))};index=end+1}else{append(value[index]);index++}};value.startsWith("~~",index)->{val end=value.indexOf("~~",index+2);if(end>index+2){withStyle(SpanStyle(textDecoration=TextDecoration.LineThrough)){append(value.substring(index+2,end))};index=end+2}else{append(value[index]);index++}};value[index]=='*'->{val end=value.indexOf('*',index+1);if(end>index+1){withStyle(SpanStyle(fontStyle=FontStyle.Italic)){append(value.substring(index+1,end))};index=end+1}else{append(value[index]);index++}};value[index]=='['->{val close=value.indexOf(']',index+1);val open=if(close>0&&close+1<value.length&&value[close+1]=='(')close+1 else -1;val end=if(open>0)value.indexOf(')',open+1)else -1;if(close>index&&end>open){withStyle(SpanStyle(textDecoration=TextDecoration.Underline)){append(value.substring(index+1,close))};index=end+1}else{append(value[index]);index++}};else->{append(value[index]);index++}}}}
    source.lines().forEachIndexed{lineIndex,raw->if(lineIndex>0)append('\n');val trimmed=raw.trimStart();when{trimmed.startsWith("### ")->{withStyle(SpanStyle(fontWeight=FontWeight.SemiBold,fontSize=(font+1).sp)){inline(trimmed.removePrefix("### "))}};trimmed.startsWith("## ")->{withStyle(SpanStyle(fontWeight=FontWeight.Bold,fontSize=(font+2).sp)){inline(trimmed.removePrefix("## "))}};trimmed.startsWith("# ")->{withStyle(SpanStyle(fontWeight=FontWeight.Bold,fontSize=(font+4).sp)){inline(trimmed.removePrefix("# "))}};trimmed.startsWith("- ")||trimmed.startsWith("* ")->{append("• ");inline(trimmed.drop(2))};else->inline(raw)}}
  }
  @Composable private fun VoiceReplyBubble(text:String,playing:Boolean,onClick:()->Unit){Row(Modifier.widthIn(min=150.dp,max=245.dp).height(54.dp).clickable(onClick=onClick).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),contentAlignment=Alignment.Center){Icon(if(playing)Icons.Filled.Stop else Icons.Filled.PlayArrow,if(playing)"停止" else "播放",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSecondaryContainer)};Row(Modifier.weight(1f).padding(horizontal=11.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){listOf(12,22,16,27,19).forEachIndexed{i,h->Box(Modifier.width(3.dp).height((if(playing&&i%2==0)h+4 else h).dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))}};Text("${(text.length/4).coerceIn(1,60)}\"",fontSize=11.sp,maxLines=1,softWrap=false,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  private fun isGifReference(reference:String):Boolean=animatedImageCache[reference]?:run{
    val clean=reference.substringBefore('?').lowercase()
    val detected=clean.endsWith(".gif")
      ||runCatching{reference.startsWith("content:")&&contentResolver.getType(android.net.Uri.parse(reference))?.equals("image/gif",true)==true}.getOrDefault(false)
      ||runCatching{
        val file=File(reference)
        if(!file.isFile||!file.name.startsWith("favorite_"))false
        else file.inputStream().use{input->
          val header=ByteArray(6)
          input.read(header)==6&&String(header,Charsets.US_ASCII) in setOf("GIF87a","GIF89a")
        }
      }.getOrDefault(false)
    animatedImageCache[reference]=detected;detected
  }
  @Composable private fun MessageImage(uri:String,modifier:Modifier=Modifier.widthIn(max=260.dp).heightIn(max=300.dp).clip(RoundedCornerShape(12.dp)),contentScale:ContentScale=ContentScale.Crop){
    val animated=remember(uri){isGifReference(uri)}
    if(animated){
      if(android.os.Build.VERSION.SDK_INT>=28){
        val drawable by produceState<android.graphics.drawable.Drawable?>(null,uri){value=withContext(Dispatchers.IO){runCatching{val source=if(uri.startsWith("content:"))android.graphics.ImageDecoder.createSource(contentResolver,android.net.Uri.parse(uri))else android.graphics.ImageDecoder.createSource(File(uri));android.graphics.ImageDecoder.decodeDrawable(source)}.getOrNull()}}
        AndroidView(factory={android.widget.ImageView(it).apply{adjustViewBounds=true;setBackgroundColor(AndroidColor.TRANSPARENT)}},modifier=modifier,update={view->view.scaleType=when(contentScale){ContentScale.Fit->android.widget.ImageView.ScaleType.FIT_CENTER;ContentScale.FillBounds->android.widget.ImageView.ScaleType.FIT_XY;else->android.widget.ImageView.ScaleType.CENTER_CROP};if(view.drawable!==drawable)view.setImageDrawable(drawable);(drawable as? android.graphics.drawable.AnimatedImageDrawable)?.start()})
        DisposableEffect(drawable){onDispose{(drawable as? android.graphics.drawable.AnimatedImageDrawable)?.stop()}}
      }else{
        val data by produceState<ByteArray?>(null,uri){value=withContext(Dispatchers.IO){runCatching{if(uri.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(uri))?.use{it.readBytes()}else File(uri).readBytes()}.getOrNull()}}
        AndroidView(factory={LegacyGifView(it).apply{setBackgroundColor(AndroidColor.TRANSPARENT)}},modifier=modifier,update={view->if(view.tag!=uri&&data!=null){view.tag=uri;view.setGif(data,contentScale==ContentScale.Fit)}})
      }
    }else{
      val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[uri],uri){if(uri.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[uri]?:runCatching{if(uri.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(uri))?.use{BitmapFactory.decodeStream(it)}else BitmapFactory.decodeFile(uri)}.getOrNull()?.also{bitmapCache.put(uri,it)}}}
      if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"聊天图片",modifier,contentScale=contentScale)
    }
  }
  private fun Modifier.transparencyGrid():Modifier=drawBehind{
    val cell=12.dp.toPx()
    drawRect(Color(0xFFE9ECEE))
    var row=0
    var y=0f
    while(y<size.height){
      var column=0
      var x=0f
      while(x<size.width){
        if((row+column)%2==0)drawRect(Color(0xFFD2D7DA),topLeft=Offset(x,y),size=androidx.compose.ui.geometry.Size((size.width-x).coerceAtMost(cell),(size.height-y).coerceAtMost(cell)))
        column++;x+=cell
      }
      row++;y+=cell
    }
  }
  @Composable
  private fun VoiceCallDialog(card:Card,phase:VoiceCallPhase,status:String,startedAt:Long,
    lastUserText:String,lastReplyText:String,micMuted:Boolean,speakerEnabled:Boolean,
    screenSharing:Boolean,screenReady:Boolean,onMic:()->Unit,onToggleMic:()->Unit,
    onToggleSpeaker:()->Unit,onToggleScreen:()->Unit,onMinimize:()->Unit,onEnd:()->Unit,backgroundImagePath:String=""){
    var elapsedSeconds by remember(startedAt){mutableLongStateOf(((SystemClock.elapsedRealtime()-startedAt).coerceAtLeast(0L))/1000L)}
    LaunchedEffect(startedAt){while(isActive){elapsedSeconds=((SystemClock.elapsedRealtime()-startedAt).coerceAtLeast(0L))/1000L;delay(1000)}}
    var fullBackground by remember(backgroundImagePath){mutableStateOf(prefs.getBoolean("voice_call_background_mode",false)&&backgroundImagePath.isNotBlank())}
    val avatarPath=card.avatar.orEmpty()
    val backdrop by produceState<android.graphics.Bitmap?>(bitmapCache[avatarPath],avatarPath){if(avatarPath.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[avatarPath]?:BitmapFactory.decodeFile(avatarPath)?.also{bitmapCache.put(avatarPath,it)}}}
    val activePulse=phase==VoiceCallPhase.LISTENING||phase==VoiceCallPhase.SPEAKING
    val pulse=rememberInfiniteTransition(label="voice-call-pulse")
    val pulseScale by pulse.animateFloat(initialValue=1f,targetValue=if(activePulse)1.07f else 1f,animationSpec=infiniteRepeatable(animation=tween(780),repeatMode=RepeatMode.Reverse),label="voice-call-scale")
    val pulseAlpha by pulse.animateFloat(initialValue=.16f,targetValue=if(activePulse).5f else .16f,animationSpec=infiniteRepeatable(animation=tween(780),repeatMode=RepeatMode.Reverse),label="voice-call-alpha")
    val accent=when(phase){VoiceCallPhase.LISTENING->MaterialTheme.colorScheme.primary;VoiceCallPhase.SPEAKING->MaterialTheme.colorScheme.secondary;VoiceCallPhase.ERROR->MaterialTheme.colorScheme.error;else->Color.White}
    val statusText=when(phase){VoiceCallPhase.THINKING->"${card.name} 正在思考";VoiceCallPhase.SPEAKING->"${card.name} 正在说话";else->status.ifBlank{"准备就绪"}}
    val caption=when{phase==VoiceCallPhase.SPEAKING&&lastReplyText.isNotBlank()->lastReplyText;lastUserText.isNotBlank()->lastUserText;lastReplyText.isNotBlank()->lastReplyText;else->""}
    Dialog(onDismissRequest=onMinimize,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
      BackHandler(onBack=onMinimize)
      val dialogView=LocalView.current
      SideEffect{(dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let{window->window.statusBarColor=AndroidColor.TRANSPARENT;window.navigationBarColor=AndroidColor.TRANSPARENT;WindowCompat.getInsetsController(window,dialogView).isAppearanceLightStatusBars=false;WindowCompat.getInsetsController(window,dialogView).isAppearanceLightNavigationBars=false}}
      Box(Modifier.fillMaxSize().background(Color(0xFF292927))){
        if(fullBackground&&backgroundImagePath.isNotBlank()){
          MomentImage(backgroundImagePath,Modifier.fillMaxSize())
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)))
        }else{
          if(backdrop!=null)Image(backdrop!!.asImageBitmap(),null,Modifier.fillMaxSize().graphicsLayer(scaleX=1.14f,scaleY=1.14f).blur(24.dp),contentScale=ContentScale.Crop)
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(backdrop==null).36f else .62f)))
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding()){
          Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onMinimize,modifier=Modifier.size(48.dp)){Icon(Icons.Outlined.PictureInPictureAlt,"缩小为悬浮窗",Modifier.size(24.dp),tint=Color.White)}
            Text("AI 语音通话",Modifier.weight(1f),color=Color.White.copy(alpha=.88f),fontSize=14.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick={val next=!fullBackground;fullBackground=next;prefs.edit().putBoolean("voice_call_background_mode",next).apply()},enabled=backgroundImagePath.isNotBlank(),modifier=Modifier.size(48.dp)){Icon(if(fullBackground)Icons.Filled.Person else Icons.Outlined.Portrait,if(fullBackground)"切换为头像模式" else "切换为全屏背景模式",Modifier.size(24.dp),tint=if(fullBackground)accent else Color.White.copy(alpha=.88f))}
          }
          BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)){
            val compact=maxHeight<390.dp
            val avatarSize=if(compact)82 else 108
            Column(Modifier.fillMaxWidth().heightIn(min=maxHeight).verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=if(compact)8.dp else 16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
              if(!fullBackground)Box(Modifier.size((avatarSize+22).dp).graphicsLayer(scaleX=pulseScale,scaleY=pulseScale).clip(CircleShape).background(accent.copy(alpha=pulseAlpha)).border(1.dp,Color.White.copy(alpha=.28f),CircleShape),contentAlignment=Alignment.Center){Box(Modifier.size(avatarSize.dp).clip(CircleShape)){RoleAvatar(card,avatarSize)}}
              Text(card.name,Modifier.fillMaxWidth().padding(top=if(compact)12.dp else 20.dp),color=Color.White,fontSize=if(compact)20.sp else 24.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
              Text(String.format(Locale.US,"%02d:%02d",elapsedSeconds/60,elapsedSeconds%60),Modifier.padding(top=6.dp),color=Color.White.copy(alpha=.72f),fontSize=13.sp,fontFamily=FontFamily.Monospace)
              Row(Modifier.fillMaxWidth().heightIn(min=34.dp).padding(top=8.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
                when(phase){VoiceCallPhase.TRANSCRIBING,VoiceCallPhase.THINKING->CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp,color=Color.White);VoiceCallPhase.LISTENING,VoiceCallPhase.SPEAKING->Icon(Icons.Filled.GraphicEq,null,Modifier.size(18.dp),tint=accent);VoiceCallPhase.ERROR->Icon(Icons.Outlined.ErrorOutline,null,Modifier.size(18.dp),tint=accent);else->Icon(Icons.Outlined.PhoneInTalk,null,Modifier.size(17.dp),tint=Color.White.copy(alpha=.8f))}
                Text(statusText,Modifier.padding(start=7.dp).widthIn(max=260.dp),color=if(phase==VoiceCallPhase.ERROR)MaterialTheme.colorScheme.error else Color.White.copy(alpha=.86f),fontSize=14.sp,lineHeight=19.sp,maxLines=if(phase==VoiceCallPhase.ERROR)2 else 1,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
              }
              if(screenSharing)Row(Modifier.padding(top=5.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.ScreenShare,null,Modifier.size(15.dp),tint=if(screenReady)Color(0xFF6EE7A8)else Color(0xFFFFC46B));Text(if(screenReady)"正在将屏幕画面共享给视觉模型" else "屏幕共享中 · 尚未配置视觉模型",Modifier.padding(start=6.dp),color=Color.White.copy(alpha=.78f),fontSize=11.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)}
              if(caption.isNotBlank())Surface(Modifier.fillMaxWidth().widthIn(max=340.dp).padding(top=if(compact)10.dp else 20.dp),shape=RoundedCornerShape(8.dp),color=Color.Black.copy(alpha=.28f)){Text(caption,Modifier.padding(horizontal=14.dp,vertical=10.dp),color=Color.White.copy(alpha=.9f),fontSize=14.sp,lineHeight=20.sp,maxLines=if(compact)2 else 3,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
            }
          }
          val micEnabled=phase in listOf(VoiceCallPhase.READY,VoiceCallPhase.ERROR,VoiceCallPhase.LISTENING,VoiceCallPhase.SPEAKING)
          Column(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,top=8.dp,bottom=18.dp),horizontalAlignment=Alignment.CenterHorizontally){
            FilledIconButton(onClick=onMic,enabled=micEnabled,modifier=Modifier.size(70.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=when(phase){VoiceCallPhase.LISTENING->MaterialTheme.colorScheme.primary;VoiceCallPhase.SPEAKING->MaterialTheme.colorScheme.secondary;else->Color.White.copy(alpha=.94f)},contentColor=when(phase){VoiceCallPhase.LISTENING->MaterialTheme.colorScheme.onPrimary;VoiceCallPhase.SPEAKING->MaterialTheme.colorScheme.onSecondary;else->Color(0xFF252624)},disabledContainerColor=Color.White.copy(alpha=.18f),disabledContentColor=Color.White.copy(alpha=.45f))){Icon(when(phase){VoiceCallPhase.LISTENING->Icons.Filled.Stop;VoiceCallPhase.SPEAKING->Icons.Filled.Mic;else->if(micMuted)Icons.Filled.MicOff else Icons.Filled.Mic},null,Modifier.size(30.dp))}
            Text(when(phase){VoiceCallPhase.LISTENING->"结束说话";VoiceCallPhase.SPEAKING->"打断并说话";VoiceCallPhase.TRANSCRIBING->"正在识别";VoiceCallPhase.THINKING->"等待回复";else->"开始说话"},Modifier.padding(top=6.dp),color=Color.White.copy(alpha=.82f),fontSize=11.sp)
            Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.SpaceEvenly){
              VoiceCallControl(if(micMuted)Icons.Filled.MicOff else Icons.Outlined.Mic,if(micMuted)"已静音" else "静音",micMuted,false,onToggleMic)
              VoiceCallControl(if(speakerEnabled)Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,"扬声器",speakerEnabled,false,onToggleSpeaker)
              VoiceCallControl(if(screenSharing)Icons.Filled.StopScreenShare else Icons.Filled.ScreenShare,if(screenSharing)"停止共享" else "共享屏幕",screenSharing,false,onToggleScreen)
              VoiceCallControl(Icons.Filled.CallEnd,"挂断",true,true,onEnd)
            }
          }
        }
      }
    }
  }

  @Composable private fun VoiceCallControl(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,selected:Boolean,danger:Boolean,onClick:()->Unit){Column(Modifier.width(66.dp),horizontalAlignment=Alignment.CenterHorizontally){FilledIconButton(onClick=onClick,modifier=Modifier.size(52.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=if(danger)Color(0xFFE24D4D)else if(selected)Color.White.copy(alpha=.94f)else Color.White.copy(alpha=.16f),contentColor=if(danger)Color.White else if(selected)Color(0xFF252624)else Color.White)){Icon(icon,label,Modifier.size(22.dp))};Text(label,Modifier.padding(top=5.dp),color=Color.White.copy(alpha=.84f),fontSize=11.sp,maxLines=1,softWrap=false)}}

  @Composable private fun FullScreenImagePreview(reference:String,onDismiss:()->Unit){Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha=.38f)).clickable(onClick=onDismiss).padding(18.dp).clip(RoundedCornerShape(12.dp)).transparencyGrid(),contentAlignment=Alignment.Center){MessageImage(reference,Modifier.fillMaxSize(),ContentScale.Fit);IconButton(onClick={saveImageToGallery(reference)},modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end=10.dp,top=10.dp).background(Color.Black.copy(alpha=.42f),CircleShape)){Icon(Icons.Outlined.SaveAlt,"保存到相册",tint=Color.White)};IconButton(onClick=onDismiss,modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=74.dp,end=10.dp).background(Color.Black.copy(alpha=.42f),CircleShape)){Icon(Icons.Outlined.Close,"关闭预览",tint=Color.White)}}}}
  @Composable private fun StickerPreviewDialog(reference:String,onDismiss:()->Unit){
    val animated=remember(reference){isGifReference(reference)}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
      Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha=.34f)).clickable(onClick=onDismiss).systemBarsPadding().padding(22.dp),contentAlignment=Alignment.Center){
        Surface(onClick={},modifier=Modifier.fillMaxWidth().widthIn(max=360.dp),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=6.dp,shadowElevation=16.dp){Column{
          Row(Modifier.fillMaxWidth().height(54.dp).padding(start=17.dp,end=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.EmojiEmotions,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.secondary);Text(ui("表情预览","Sticker preview"),Modifier.weight(1f).padding(start=9.dp),fontWeight=FontWeight.SemiBold);if(animated)Surface(shape=RoundedCornerShape(6.dp),color=MaterialTheme.colorScheme.secondaryContainer){Text("GIF",Modifier.padding(horizontal=7.dp,vertical=3.dp),fontSize=10.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSecondaryContainer)};IconButton(onClick={saveImageToGallery(reference)}){Icon(Icons.Outlined.SaveAlt,ui("保存到相册","Save to gallery"))};IconButton(onClick=onDismiss){Icon(Icons.Outlined.Close,ui("关闭预览","Close preview"))}}
          HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.26f))
          Box(Modifier.fillMaxWidth().aspectRatio(1f).transparencyGrid().padding(22.dp),contentAlignment=Alignment.Center){MessageImage(reference,Modifier.fillMaxSize(),ContentScale.Fit)}
        }}
      }
    }
  }
  @Composable private fun MomentImage(reference:String,modifier:Modifier=Modifier){val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[reference],reference){if(reference.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[reference]?:runCatching{if(reference.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(reference))?.use{BitmapFactory.decodeStream(it)}else BitmapFactory.decodeFile(reference)}.getOrNull()?.also{bitmapCache.put(reference,it)}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"朋友圈图片",modifier,contentScale=ContentScale.Crop)}
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

  @Composable private fun DetailScaffold(title:String,back:Page=Page.MAIN,onBack:(()->Unit)?=null,contentColor:Color?=null,content:LazyListScope.()->Unit){val listState=rememberLazyListState();CompositionLocalProvider(LocalContentColor provides (contentColor?:MaterialTheme.colorScheme.onBackground)){Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={onBack?.invoke()?:run{composePage=back}}){Icon(Icons.AutoMirrored.Filled.ArrowBack,ui("返回","Back"))};Text(localizedPageTitle(title),fontSize=19.sp,fontWeight=FontWeight.SemiBold)};LazyColumn(state=listState,contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}}
  @Composable private fun CircleAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit){FilledIconButton(onClick=onClick,modifier=Modifier.size(40.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Icon(icon,label,Modifier.size(20.dp))}}
  @Composable private fun SectionTitle(text:String){Text(text,fontSize=14.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(start=20.dp,top=18.dp,bottom=6.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  @Composable private fun Avatar(name:String,size:Int){Box(Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text(name.take(1),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)}}
  @Composable private fun RoleAvatar(card:Card,size:Int){val path=card.avatar.orEmpty();val name=card.name.orEmpty().ifBlank{"角色"};val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[path],path){if(path.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[path]?:BitmapFactory.decodeFile(path)?.also{bitmapCache.put(path,it)}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),name,Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)else Avatar(name,size)}
  @Composable private fun UserAvatar(size:Int){val path=prefs.getString("user_avatar","") ?: "";val bitmap by produceState<android.graphics.Bitmap?>(bitmapCache[path],path){if(path.isNotBlank()&&value==null)value=withContext(Dispatchers.IO){bitmapCache[path]?:runCatching{if(path.startsWith("content:"))contentResolver.openInputStream(android.net.Uri.parse(path))?.use{BitmapFactory.decodeStream(it)}else BitmapFactory.decodeFile(path)}.getOrNull()?.also{bitmapCache.put(path,it)}}};if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"用户头像",Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)else Avatar(prefs.getString("user_name","我") ?: "我",size)}
  @Composable private fun EmptyState(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String){Column(Modifier.fillMaxWidth().padding(top=70.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.SemiBold,fontSize=17.sp);Spacer(Modifier.height(3.dp));Text(subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  @Composable private fun InfoBanner(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,text:String){Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.secondaryContainer){Row(Modifier.fillMaxWidth().padding(16.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.onSecondaryContainer);Column(Modifier.padding(start=12.dp)){Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(3.dp));Text(text,fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSecondaryContainer)}}}}
  @Composable private fun GuideStep(number:String,title:String,text:String){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text(number,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)};Column(Modifier.padding(start=12.dp)){Text(title,fontWeight=FontWeight.Medium);Text(text,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
  private data class MenuEntry(val icon:androidx.compose.ui.graphics.vector.ImageVector,val title:String,val subtitle:String,val action:()->Unit)
  @Composable private fun MenuGroup(title:String,entries:List<MenuEntry>){Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=4.dp,bottom=2.dp));entries.forEach{e->SettingButton(e.icon,e.title,e.subtitle,e.action)}}}
  @Composable private fun ProfileCard(onClick:()->Unit){Surface(onClick=onClick,modifier=Modifier.padding(horizontal=16.dp),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){UserAvatar(58);Column(Modifier.weight(1f).padding(start=14.dp)){Text(prefs.getString("user_name",ui("我","Me"))?:ui("我","Me"),fontSize=20.sp,fontWeight=FontWeight.Bold);Text(ui("你的角色、记忆与故事","Your characters, memories and stories"),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}
  @Composable private fun StyledField(value:String,onChange:(String)->Unit,label:String){OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)},singleLine=true,shape=RoundedCornerShape(13.dp))}
  @Composable private fun PreviewText(label:String,value:String,minLines:Int){Column(Modifier.fillMaxWidth()){Text(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=12.dp,bottom=5.dp));Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Box(Modifier.fillMaxWidth().heightIn(min=(minLines.coerceAtMost(4)*22+24).dp,max=170.dp).verticalScroll(rememberScrollState()).padding(12.dp)){Text(value.ifBlank{"（空）"},fontSize=14.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurface)}}}}
  @Composable private fun ModelListCard(models:List<String>,selected:String,onSelect:(String)->Unit){
    var query by remember(models){mutableStateOf("")}
    val visible=remember(models,query){val value=query.trim();if(value.isBlank())models else models.filter{it.contains(value,true)}}
    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.fillMaxWidth().padding(8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
      OutlinedTextField(query,{query=it.take(120)},Modifier.fillMaxWidth(),placeholder={Text("搜索 ${models.size} 个模型")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon=if(query.isNotBlank()){{IconButton(onClick={query=""}){Icon(Icons.Outlined.Close,"清空模型搜索")}}}else null,singleLine=true,shape=RoundedCornerShape(11.dp))
      if(visible.isEmpty())Box(Modifier.fillMaxWidth().height(82.dp),contentAlignment=Alignment.Center){Text("没有匹配的模型",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
      else Column(Modifier.fillMaxWidth().heightIn(max=260.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){visible.forEach{name->OutlinedButton(onClick={onSelect(name)},Modifier.fillMaxWidth().height(42.dp)){Text(name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);if(selected==name)Icon(Icons.Filled.Check,null)}}}
    }}
  }
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
private val WeiXinLight=lightColorScheme(primary=Color(0xFF07C160),onPrimary=Color.White,primaryContainer=Color(0xFFDDF7E8),onPrimaryContainer=Color(0xFF075C31),secondary=Color(0xFF576B62),onSecondary=Color.White,secondaryContainer=Color(0xFFE5EAE7),onSecondaryContainer=Color(0xFF26352E),background=Color(0xFFEDEDED),onBackground=Color(0xFF191919),surface=Color(0xFFFFFFFF),onSurface=Color(0xFF191919),surfaceVariant=Color(0xFFF5F5F5),onSurfaceVariant=Color(0xFF7A7A7A),outline=Color(0xFFD9D9D9),error=Color(0xFFFA5151),onError=Color.White)
private val WeiXinDark=darkColorScheme(primary=Color(0xFF15C36A),onPrimary=Color(0xFF052E18),primaryContainer=Color(0xFF173C29),onPrimaryContainer=Color(0xFFC9F2DA),secondary=Color(0xFFA8B4AE),onSecondary=Color(0xFF243029),secondaryContainer=Color(0xFF303A35),onSecondaryContainer=Color(0xFFE0E7E3),background=Color(0xFF111111),onBackground=Color(0xFFE7E7E7),surface=Color(0xFF1E1E1E),onSurface=Color(0xFFE7E7E7),surfaceVariant=Color(0xFF2A2A2A),onSurfaceVariant=Color(0xFFA8A8A8),outline=Color(0xFF444444),error=Color(0xFFFF6B6B),onError=Color(0xFF3D0000))
private fun themePalette(style:Int,dark:Boolean)=when(style){1->if(dark)QingLanDark else QingLanLight;2->if(dark)YingWuDark else YingWuLight;3->if(dark)JianMoDark else JianMoLight;4->if(dark)WeiXinDark else WeiXinLight;else->if(dark)ShiYuDark else ShiYuLight}
private fun glassPalette(base:ColorScheme,opacity:Int):ColorScheme{
  val alpha=opacity.coerceIn(35,95)/100f
  return base.copy(
    surface=base.surface.copy(alpha=alpha),
    surfaceVariant=base.surfaceVariant.copy(alpha=(alpha*.9f).coerceIn(.3f,.9f)),
    primaryContainer=base.primaryContainer.copy(alpha=(alpha*.88f).coerceIn(.3f,.9f)),
    secondaryContainer=base.secondaryContainer.copy(alpha=(alpha*.86f).coerceIn(.3f,.9f))
  )
}
