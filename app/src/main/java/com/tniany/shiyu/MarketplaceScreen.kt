package com.tniany.shiyu
import android.content.Intent
import android.net.Uri

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class MarketSettingsRequest(
  val assetId: String,
  val name: String,
  val settings: List<MarketSetting>,
  val theme: MarketTheme? = null,
  val plugin: MarketPlugin? = null
)



@Composable
internal fun MarketplaceScreen(
  repository: MarketplaceRepository,
  onApplyTheme: (MarketTheme) -> String?,
  onResetTheme: () -> String?,
  onRollbackTheme: () -> String?,
  onBack: () -> Unit,
  onChanged: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val initial = remember { runCatching { repository.cachedOrBuiltInCatalog() } }
  var catalog by remember { mutableStateOf(initial.getOrNull()) }
  var refreshing by remember { mutableStateOf(false) }
  var notice by remember { mutableStateOf(initial.exceptionOrNull()?.let { "内置目录读取失败：${it.message ?: "未知错误"}" }.orEmpty()) }
  var noticeIsError by remember { mutableStateOf(initial.isFailure) }
  var query by rememberSaveable { mutableStateOf("") }
  var section by rememberSaveable { mutableStateOf("themes") }
  var localRevision by remember { mutableIntStateOf(0) }
  var importPrompt by remember { mutableStateOf(false) }
  var settingsRequest by remember { mutableStateOf<MarketSettingsRequest?>(null) }
  var importCountdown by remember { mutableIntStateOf(5) }

  val activeTheme = remember(localRevision) { repository.activeThemeId() }
  val installedThemes = remember(localRevision) { repository.installedThemeIds() }
  val installedPlugins = remember(localRevision) { repository.installedPluginIds() }
  val enabledPlugins = remember(localRevision) { repository.enabledPluginIds() }
  val localThemes = remember(localRevision, catalog) { repository.localThemes() }
  val localThemeIds = localThemes.mapTo(mutableSetOf()) { it.id }
  val canRollback = remember(localRevision) { repository.previousAppearance() != null }
  val allThemes = (catalog?.themes.orEmpty() + localThemes).distinctBy { it.id }
  val allPlugins = catalog?.plugins.orEmpty().distinctBy { it.id }
  val sourceStatus = catalog?.source?.let {
    if (it.builtIn) "精选外观与实用工具，随时可用" else "已同步到最新精选内容"
  }.orEmpty()
  val normalizedQuery = query.trim().lowercase(Locale.getDefault())

  LaunchedEffect(importPrompt) {
    if (!importPrompt) return@LaunchedEffect
    importCountdown = 5
    while (importCountdown > 0) {
      delay(1_000)
      importCountdown--
    }
  }

  val themes = allThemes.filter { theme ->
    normalizedQuery.isBlank() || listOf(theme.name, theme.summary, theme.author)
      .plus(theme.tags)
      .any { it.lowercase(Locale.getDefault()).contains(normalizedQuery) }
  }
  val plugins = allPlugins.filter { plugin ->
    normalizedQuery.isBlank() || listOf(
      plugin.name,
      plugin.summary,
      plugin.description,
      plugin.author,
      plugin.category
    ).any { it.lowercase(Locale.getDefault()).contains(normalizedQuery) }
  }

  fun report(message: String, error: Boolean = false) {
    notice = message
    noticeIsError = error
  }

  fun refreshCatalog() {
    if (refreshing) return
    refreshing = true
    notice = ""
    scope.launch {
      runCatching { withContext(Dispatchers.IO) { repository.refreshOfficialCatalog() } }
        .onSuccess {
          catalog = it
          report("已刷新官方源：${it.themes.size} 个主题，${it.plugins.size} 个插件")
        }
        .onFailure { report("在线刷新失败，继续使用当前目录：${it.message ?: "未知错误"}", true) }
      refreshing = false
    }
  }

  fun openSource(url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
      .onFailure { report("无法打开开源仓库：${it.message ?: "未知错误"}", true) }
  }

  fun saveAssetSettings(request: MarketSettingsRequest, values: Map<String, Any>) {
    runCatching {
      repository.saveSettingValues(request.assetId, request.settings, values)
      val theme = request.theme
      val plugin = request.plugin
      if (theme != null && repository.activeThemeId() == theme.id) {
        onApplyTheme(repository.resolvedTheme(theme))?.let { error(it) }
      } else if (plugin != null && plugin.id in repository.enabledPluginIds()) {
        repository.setPluginEnabled(repository.resolvedPlugin(plugin), true)
      }
      localRevision++
      onChanged()
      report("${request.name}设置已保存")
      settingsRequest = null
    }.onFailure { report("保存设置失败：${it.message ?: "未知错误"}", true) }
  }

  fun importChosenTheme(uri: Uri?) {
    if (uri == null) return
    scope.launch {
      runCatching {
        val raw = withContext(Dispatchers.IO) {
          val reader = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
            ?: error("无法读取导入文件")
          reader.use {
            val output = StringBuilder()
            val buffer = CharArray(8192)
            var total = 0
            while (true) {
              val count = it.read(buffer)
              if (count < 0) break
              total += count
              require(total <= MarketplaceRepository.PAGE_SIZE_LIMIT) { "导入文件超过 2MB 限制" }
              output.append(buffer, 0, count)
            }
            output.toString()
          }
        }
        withContext(Dispatchers.IO) { repository.importLocalTheme(raw) }
      }.onSuccess { result ->
        localRevision++
        onChanged()
        report("已导入 ${result.themes.size} 个本地主题。本地主题未经官方审核，请确认来源可信")
      }.onFailure {
        report("主题导入失败：${it.message ?: "文件格式无效"}", true)
      }
    }
  }

  val themeImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    importChosenTheme(uri)
  }

  Column(Modifier.fillMaxSize()) {
    Row(
      Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
      }
      Text("时遇市场", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(48.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Storefront, null, tint = MaterialTheme.colorScheme.onPrimary)
                  }
                }
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                  Text(
                    "为时遇换一种心情",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                  Text(
                    sourceStatus.ifBlank { "市场目录暂不可用" },
                    Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f),
                    maxLines = 2
                  )
                }
                IconButton(onClick = { refreshCatalog() }, enabled = !refreshing) {
                  Icon(if (refreshing) Icons.Outlined.Sync else Icons.Outlined.Refresh, "刷新官方源")
                }
              }
              Text(
                "挑一套喜欢的外观，再为聊天、创作和日常加入顺手的小工具。",
                Modifier.padding(top = 12.dp),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .82f)
              )
            }
          }
        }

        if (notice.isNotBlank()) {
          item {
            MarketNotice(
              if (noticeIsError) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
              if (noticeIsError) "市场提示" else "操作完成",
              notice
            )
          }
        }

        item {
          OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索主题和插件") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = if (query.isBlank()) null else {
              {
                IconButton(onClick = { query = "" }) {
                  Icon(Icons.Outlined.Close, "清除搜索")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
          )
        }

        item {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FilterChip(
              selected = section == "themes",
              onClick = { section = "themes" },
              label = { Text("主题 ${allThemes.size}") },
              leadingIcon = { Icon(Icons.Outlined.Palette, null, Modifier.size(17.dp)) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = section == "plugins",
              onClick = { section = "plugins" },
              label = { Text("插件 ${allPlugins.size}") },
              leadingIcon = { Icon(Icons.Outlined.Extension, null, Modifier.size(17.dp)) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        if (section == "themes") {

          item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
              OutlinedButton(
                onClick = { importPrompt = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 7.dp)
              ) {
                Icon(Icons.Outlined.FileOpen, null, Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("导入主题", maxLines = 1)
              }
              OutlinedButton(
                onClick = { onResetTheme()?.let { report("恢复默认失败：$it", true) } },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 7.dp)
              ) {
                Icon(Icons.Outlined.RestartAlt, null, Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("恢复默认", maxLines = 1)
              }
              OutlinedButton(
                onClick = { onRollbackTheme()?.let { report("回滚失败：$it", true) } },
                enabled = canRollback,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 7.dp)
              ) {
                Icon(Icons.Outlined.Undo, null, Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("回滚", maxLines = 1)
              }
            }
          }
          if (activeTheme.isNotBlank()) {
            item {
              Text(
                "当前市场主题：${allThemes.firstOrNull { it.id == activeTheme }?.name ?: activeTheme}",
                Modifier.padding(horizontal = 4.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
          if (themes.isEmpty()) {
            item {
              MarketEmpty(
                Icons.Outlined.Palette,
                "没有匹配的主题",
                if (normalizedQuery.isBlank()) "请刷新官方源或导入本地主题" else "换个关键词试试"
              )
            }
          }
          items(themes, key = { it.id }) { theme ->
            val installed = theme.id in installedThemes
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
              Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                  MarketThemePreview(theme)
                  Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        theme.name,
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      if (theme.id == activeTheme) {
                        Surface(
                          shape = RoundedCornerShape(7.dp),
                          color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                          Text(
                            "使用中",
                            Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                          )
                        }
                      }
                    }
                    Text(
                      "${theme.author} · v${theme.version}",
                      Modifier.padding(top = 3.dp),
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )
                    Text(
                      theme.summary,
                      Modifier.padding(top = 7.dp),
                      fontSize = 12.sp,
                      lineHeight = 17.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
                if (theme.tags.isNotEmpty()) {
                  Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    theme.tags.forEach { tag ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                      ) {
                        Text(
                          tag,
                          Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          fontSize = 10.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
                if (theme.sourceUrl.isNotBlank()) {
                  TextButton(onClick = { openSource(theme.sourceUrl) }) {
                    Text("查看开源仓库")
                  }
                }
                if (theme.settings.isNotEmpty()) {
                  AssistChip(
                    onClick = {
                      settingsRequest = MarketSettingsRequest(theme.id, theme.name, theme.settings, theme = theme)
                    },
                    label = { Text("设置 ${theme.settings.size}") },
                    leadingIcon = { Icon(Icons.Outlined.Tune, null, Modifier.size(16.dp)) }
                  )
                }
                HorizontalDivider(
                  Modifier.padding(vertical = 11.dp),
                  color = MaterialTheme.colorScheme.outline.copy(alpha = .2f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (theme.id in localThemeIds) {
                    IconButton(
                      onClick = {
                        runCatching {
                          repository.deleteLocalTheme(theme.id)
                          localRevision++
                          onChanged()
                          report("已删除本地主题：${theme.name}")
                        }.onFailure { report("删除失败：${it.message ?: "未知错误"}", true) }
                      },
                      modifier = Modifier.size(42.dp)
                    ) {
                      Icon(Icons.Outlined.Delete, "删除本地主题", tint = MaterialTheme.colorScheme.error)
                    }
                  } else {
                    OutlinedButton(
                      onClick = {
                        runCatching {
                          repository.installTheme(theme)
                          localRevision++
                          onChanged()
                          report("已安装主题：${theme.name}")
                        }.onFailure { report("安装失败：${it.message ?: "未知错误"}", true) }
                      },
                      enabled = !installed,
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(
                        if (installed) Icons.Filled.Check else Icons.Outlined.Download,
                        null,
                        Modifier.size(17.dp)
                      )
                      Spacer(Modifier.width(5.dp))
                      Text(if (installed) "已安装" else "安装", maxLines = 1)
                    }
                  }
                  Button(
                    onClick = {
                      runCatching {
                        if (!installed) repository.installTheme(theme)
                        onApplyTheme(repository.resolvedTheme(theme))?.let { error(it) }
                        localRevision++
                        onChanged()
                      }.onFailure { report("应用失败：${it.message ?: "未知错误"}", true) }
                    },
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Outlined.Brush, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("应用", maxLines = 1)
                  }
                }
              }
            }
          }
        } else {
          if (plugins.isEmpty()) {
            item {
              MarketEmpty(Icons.Outlined.Extension, "没有匹配的插件", "检查关键词或刷新官方源")
            }
          }
          items(plugins, key = { it.id }) { plugin ->
            val installed = plugin.id in installedPlugins
            val enabled = plugin.id in enabledPlugins
            val community = plugin.id.startsWith("community.")
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
              Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(46.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        MarketPluginIcon(plugin.icon),
                        null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                      )
                    }
                  }
                  Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        plugin.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      if (plugin.experimental) {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = MaterialTheme.colorScheme.tertiaryContainer,
                          modifier = Modifier.padding(start = 6.dp)
                        ) {
                          Text(
                            "Beta",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                          )
                        }
                      }
                    }
                    Text(
                      "${if (community) "社区审核 · " else ""}${plugin.category} · ${plugin.author} · v${plugin.version}",
                      Modifier.padding(top = 2.dp),
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  if (installed) {
                    Switch(
                      checked = enabled,
                      onCheckedChange = { next ->
                        runCatching {
                          repository.setPluginEnabled(plugin, next)
                          localRevision++
                          onChanged()
                          report("${plugin.name}${if (next) "已启用" else "已暂停"}")
                        }.onFailure { report("插件设置失败：${it.message ?: "未知错误"}", true) }
                      }
                    )
                  }
                }
                Text(
                  plugin.summary,
                  Modifier.padding(top = 11.dp),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  plugin.description,
                  Modifier.padding(top = 5.dp),
                  fontSize = 12.sp,
                  lineHeight = 18.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plugin.sourceUrl.isNotBlank()) {
                  TextButton(onClick = { openSource(plugin.sourceUrl) }) {
                    Text("查看开源仓库")
                  }
                }
                if (plugin.settings.isNotEmpty()) {
                  AssistChip(
                    onClick = {
                      settingsRequest = MarketSettingsRequest(plugin.id, plugin.name, plugin.settings, plugin = plugin)
                    },
                    label = { Text("设置 ${plugin.settings.size}") },
                    leadingIcon = { Icon(Icons.Outlined.Tune, null, Modifier.size(16.dp)) }
                  )
                }
                if (plugin.permissions.isNotEmpty()) {
                  Text(
                    "权限与数据范围",
                    Modifier.padding(top = 11.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                plugin.permissions.forEach { permission ->
                  Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Icon(
                      Icons.Outlined.CheckCircle,
                      null,
                      Modifier.padding(top = 1.dp).size(15.dp),
                      tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                      permission,
                      Modifier.padding(start = 7.dp),
                      fontSize = 12.sp,
                      lineHeight = 17.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                Row(
                  Modifier.fillMaxWidth().padding(top = 12.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  if (!installed) {
                    Button(
                      onClick = {
                        runCatching {
                          repository.installPlugin(plugin)
                          localRevision++
                          onChanged()
                          report("已安装并启用：${plugin.name}")
                        }.onFailure { report("安装失败：${it.message ?: "未知错误"}", true) }
                      },
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Outlined.Add, null, Modifier.size(17.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("安装并启用", maxLines = 1)
                    }
                  } else {
                    OutlinedButton(
                      onClick = {
                        runCatching {
                          repository.uninstallPlugin(plugin)
                          localRevision++
                          onChanged()
                          report("已卸载：${plugin.name}，原设置已恢复")
                        }.onFailure { report("卸载失败：${it.message ?: "未知错误"}", true) }
                      },
                      modifier = Modifier.weight(1f),
                      colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                      )
                    ) {
                      Icon(Icons.Outlined.Delete, null, Modifier.size(17.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("卸载并恢复原设置", maxLines = 1)
                    }
                  }
                }
              }
            }
          }
        }

        item {
          Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp)) {
            Text(
              "更多主题和经审核的社区插件会持续更新。安装主题后，可随时恢复默认外观或回滚上一套外观。",
              fontSize = 11.sp,
              lineHeight = 17.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }

  if (importPrompt) {
    AlertDialog(
      onDismissRequest = {},
      properties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
      ),
      icon = { Icon(Icons.Outlined.Security, null) },
      title = {
        Text("导入本地主题")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "本地包没有经过时遇官方审核，只应从可信来源导入。",
            fontWeight = FontWeight.Medium
          )
          Text(
            "本地主题只会修改 App 已实现并审核的外观字段，不加载脚本、远程代码或插件。",
            fontSize = 13.sp,
            lineHeight = 20.sp
          )
          Text(
            if (importCountdown > 0) {
              "请阅读以上说明，${importCountdown} 秒后才能继续。"
            } else {
              "已阅读安全说明，现在可以选择文件。"
            },
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      },
      confirmButton = {
        TextButton(
          enabled = importCountdown == 0,
          onClick = {
            importPrompt = false
            themeImporter.launch(arrayOf("application/json", "text/plain", "*/*"))
          }
        ) {
          Text(if (importCountdown > 0) "${importCountdown} 秒后可继续" else "选择文件")
        }
      }
    )
  }

  settingsRequest?.let { request ->
    MarketSettingsDialog(
      request = request,
      initialValues = repository.settingValues(request.assetId, request.settings),
      onDismiss = { settingsRequest = null },
      onSave = { values -> saveAssetSettings(request, values) },
      onReset = {
        runCatching {
          repository.clearSettingValues(request.assetId)
          request.theme?.takeIf { repository.activeThemeId() == it.id }?.let {
            onApplyTheme(repository.resolvedTheme(it))?.let { message -> error(message) }
          }
          request.plugin?.takeIf { it.id in repository.enabledPluginIds() }?.let {
            repository.setPluginEnabled(repository.resolvedPlugin(it), true)
          }
          localRevision++
          onChanged()
          settingsRequest = null
          report("${request.name}已恢复默认设置")
        }.onFailure { report("恢复默认失败：${it.message ?: "未知错误"}", true) }
      }
    )
  }
}


@Composable
private fun MarketSettingsDialog(
  request: MarketSettingsRequest,
  initialValues: Map<String, Any>,
  onDismiss: () -> Unit,
  onSave: (Map<String, Any>) -> Unit,
  onReset: () -> Unit
) {
  var values by remember(request.assetId) { mutableStateOf(initialValues) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${request.name}设置") },
    text = {
      Column(
        Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        request.settings.forEach { setting ->
          when (setting.type) {
            MarketSettingType.SWITCH -> {
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(setting.label, fontWeight = FontWeight.SemiBold)
                  if (setting.description.isNotBlank()) Text(
                    setting.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Switch(
                  checked = values[setting.key] as? Boolean ?: false,
                  onCheckedChange = { value -> values = values + (setting.key to value) }
                )
              }
            }
            MarketSettingType.RANGE -> {
              val current = (values[setting.key] as? Number)?.toInt() ?: setting.defaultValue as Int
              Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Text(setting.label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                  Text(current.toString(), color = MaterialTheme.colorScheme.primary)
                }
                if (setting.description.isNotBlank()) Text(setting.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                  value = current.toFloat(),
                  onValueChange = { raw -> values = values + (setting.key to quantize(raw, setting)) },
                  valueRange = setting.min.toFloat()..setting.max.toFloat(),
                  steps = ((setting.max - setting.min) / setting.step - 1).coerceAtLeast(0)
                )
              }
            }
            MarketSettingType.SELECT -> {
              Text(setting.label, fontWeight = FontWeight.SemiBold)
              if (setting.description.isNotBlank()) Text(setting.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                setting.options.forEach { option ->
                  FilterChip(
                    selected = values[setting.key] == option.value,
                    onClick = { values = values + (setting.key to option.value) },
                    label = { Text(option.label) },
                    leadingIcon = { Box(Modifier.size(14.dp).clip(CircleShape).background(runCatching { Color(AndroidColor.parseColor(option.value)) }.getOrDefault(Color.Gray))) }
                  )
                }
              }
            }
          }
        }
        Text(
          if (request.plugin != null) "设置只作用于 App 已实现并审核的运行接口。" else "设置只修改主题已审核的外观字段。",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    dismissButton = { TextButton(onClick = onReset) { Text("恢复默认") } },
    confirmButton = { Button(onClick = { onSave(values.toMap()) }) { Text("保存") } }
  )
}

private fun quantize(value: Float, setting: MarketSetting): Int {
  val snapped = setting.min + (((value - setting.min) / setting.step).roundToInt() * setting.step)
  return snapped.coerceIn(setting.min, setting.max)
}

@Composable
private fun MarketNotice(icon: ImageVector, title: String, text: String) {
  Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
      Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
      Column(Modifier.padding(start = 11.dp)) {
        Text(
          title,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
          text,
          Modifier.padding(top = 3.dp),
          fontSize = 12.sp,
          lineHeight = 18.sp,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .84f)
        )
      }
    }
  }
}

@Composable
private fun MarketEmpty(icon: ImageVector, title: String, subtitle: String) {
  Column(
    Modifier.fillMaxWidth().padding(vertical = 38.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(icon, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.secondary)
    Text(title, Modifier.padding(top = 11.dp), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Text(
      subtitle,
      Modifier.padding(top = 3.dp),
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun MarketThemePreview(theme: MarketTheme) {
  val fallback = listOf(
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer
  )
  val colors = theme.palette.map { value ->
    runCatching { Color(AndroidColor.parseColor(value)) }.getOrDefault(fallback.first())
  }.ifEmpty { fallback }
  Box(
    Modifier
      .size(width = 76.dp, height = 82.dp)
      .clip(RoundedCornerShape(15.dp))
      .background(colors.first())
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .3f), RoundedCornerShape(15.dp))
  ) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors + colors.first())))
    Column(
      Modifier.fillMaxSize().padding(9.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
          Box(
            Modifier
              .size(9.dp)
              .clip(CircleShape)
              .background(colors.getOrElse(index) { colors.last() }.copy(alpha = .9f))
          )
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
          Modifier.fillMaxWidth(.75f).height(7.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = .62f))
        )
        Box(
          Modifier.fillMaxWidth(.52f).height(7.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = .48f))
        )
      }
    }
  }
}

private fun MarketPluginIcon(name: String): ImageVector = when (name) {
  "schedule" -> Icons.Outlined.Schedule
  "music" -> Icons.Outlined.MusicNote
  "search" -> Icons.Outlined.Search
  "travel" -> Icons.Outlined.TravelExplore
  "groups" -> Icons.Outlined.Groups
  else -> Icons.Outlined.Extension
}
