package com.tniany.shiyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public final class DesktopPetService extends Service {
  private static final String CHANNEL_ID = "desktop_pet";
  private static final int NOTIFICATION_ID = 7031;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Random random = new Random();
  private final ExecutorService replyExecutor = Executors.newSingleThreadExecutor();
  private WindowManager windowManager;
  private SharedPreferences prefs;
  private SpeechEngine speech;
  private View petView;
  private View menuView;
  private View bubbleView;
  private View quickReplyView;
  private View unreadView;
  private WebView live2dWebView;
  private WindowManager.LayoutParams petParams;
  private WindowManager.LayoutParams menuParams;
  private WindowManager.LayoutParams bubbleParams;
  private WindowManager.LayoutParams quickReplyParams;
  private String roleName = "角色";
  private String roleId = "";
  private String chatId = "";
  private volatile boolean replying;
  private boolean bubbleClosedDuringReply;
  private long lastBubbleAt;
  private Runnable dismissBubbleTask;
  private Runnable bubbleDotsTask;
  private final Runnable messagePoll = new Runnable() {
    @Override public void run() {
      CrashProtection.markFeatureActive(DesktopPetService.this, CrashProtection.FEATURE_DESKTOP_PET);
      pollForRoleMessage();
      handler.postDelayed(this, 2500L);
    }
  };

  public static void sync(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    boolean allowed = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
    Intent intent = new Intent(context, DesktopPetService.class);
    if (MainActivity.isDataRescueRequired(context)) {
      context.stopService(intent);
      return;
    }
    if (prefs.getBoolean("experimental_desktop_pet_enabled", false) && allowed) {
      if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
      else context.startService(intent);
    } else {
      context.stopService(intent);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    prefs = getSharedPreferences(MainActivity.PREF, MODE_PRIVATE);
    if (MainActivity.isDataRescueRequired(this)) {
      stopSelf();
      return;
    }
    CrashProtection.markFeatureActive(this, CrashProtection.FEATURE_DESKTOP_PET);
    speech = new SpeechEngine(this, prefs, new SecureStore(this), (speaking, text, error) -> {
      if (!error.isEmpty()) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        if ("voice".equals(prefs.getString("desktop_pet_reply_mode", "both")) && text != null && !text.isEmpty())
          showBubble(text, false);
      }
    });
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    lastBubbleAt = prefs.getLong("desktop_pet_last_bubble_at", System.currentTimeMillis());
    startForeground(NOTIFICATION_ID, notification());
    handler.postDelayed(messagePoll, 2500L);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (MainActivity.isDataRescueRequired(this)
        || !prefs.getBoolean("experimental_desktop_pet_enabled", false)
        || (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))) {
      stopSelf();
      return START_NOT_STICKY;
    }
    rebuildPet();
    return START_STICKY;
  }

  private Notification notification() {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "桌宠模式", NotificationManager.IMPORTANCE_LOW);
      channel.setDescription("保持角色桌宠悬浮显示");
      channel.setShowBadge(false);
      manager.createNotificationChannel(channel);
    }
    Intent open = new Intent(this, ComposeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pending = PendingIntent.getActivity(this, 7031, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.ic_menu_myplaces)
        .setContentTitle("时遇桌宠正在陪伴")
        .setContentText("点击回到时遇，长按桌宠查看更多操作")
        .setContentIntent(pending)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .build();
  }

  private void rebuildPet() {
    removeOverlays();
    JSONObject role = selectedRole();
    roleName = role.optString("name", "角色");
    roleId = role.optString("id");
    chatId = matchingChatId(roleId);
    boolean live2d = "live2d".equals(prefs.getString("experimental_desktop_pet_style", "avatar"))
        && Live2DRepository.ready(this);
    int logicalSize = live2d
        ? Math.max(80, Math.min(320, prefs.getInt("experimental_live2d_pet_size", 180)))
        : Math.max(32, Math.min(160, prefs.getInt("experimental_desktop_pet_size", 76)));
    int width = dp(logicalSize);
    int height = live2d ? dp(Math.round(logicalSize * 1.42f)) : width;
    FrameLayout frame = new FrameLayout(this) {
      @Override public boolean performClick() {
        super.performClick();
        handlePetTap();
        return true;
      }
    };
    frame.setClickable(true);
    if (live2d) {
      frame.setBackgroundColor(Color.TRANSPARENT);
      frame.setPadding(0, 0, 0, 0);
      WebView renderer = createLive2DRenderer();
      frame.addView(renderer, new FrameLayout.LayoutParams(-1, -1));
      live2dWebView = renderer;
    } else {
      GradientDrawable border = new GradientDrawable(
          GradientDrawable.Orientation.TL_BR,
          new int[] {Color.rgb(255, 216, 224), Color.rgb(255, 247, 249), Color.rgb(184, 230, 220)});
      border.setShape(GradientDrawable.OVAL);
      border.setStroke(dp(1), Color.argb(210, 255, 255, 255));
      frame.setBackground(border);
      frame.setElevation(dp(12));
      frame.setPadding(dp(4), dp(4), dp(4), dp(4));
      Bitmap avatar = BitmapFactory.decodeFile(role.optString("avatar"));
      if (avatar != null) {
      ImageView image = new ImageView(this);
      image.setImageBitmap(avatar);
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      image.setClipToOutline(true);
      image.setBackground(rounded(Color.TRANSPARENT, width / 2f));
      image.setOutlineProvider(new android.view.ViewOutlineProvider() {
        @Override public void getOutline(View view, android.graphics.Outline outline) {
          outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
      });
      frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
      } else {
        TextView letter = new TextView(this);
        letter.setText(roleName.isEmpty() ? "时" : roleName.substring(0, 1));
        letter.setTextColor(Color.rgb(111, 45, 61));
        letter.setTextSize(Math.max(18, logicalSize * .34f));
        letter.setGravity(Gravity.CENTER);
        letter.setBackground(rounded(Color.rgb(247, 222, 228), width / 2f));
        frame.addView(letter, new FrameLayout.LayoutParams(-1, -1));
      }
    }
    View status = new View(this);
    GradientDrawable statusBackground = rounded(Color.rgb(53, 190, 132), dp(9));
    statusBackground.setStroke(dp(2), Color.WHITE);
    status.setBackground(statusBackground);
    status.setContentDescription("有未读角色消息");
    status.setClickable(true);
    status.setOnClickListener(view -> { setUnread(false); openApp(chatId); });
    status.setVisibility(prefs.getBoolean("desktop_pet_unread",false)?View.VISIBLE:View.GONE);
    FrameLayout.LayoutParams statusLayout = new FrameLayout.LayoutParams(dp(17), dp(17), Gravity.END | Gravity.BOTTOM);
    statusLayout.setMargins(0, 0, dp(1), dp(1));
    frame.addView(status, statusLayout);
    unreadView=status;
    frame.setContentDescription(roleName + "桌宠，点击按当前状态操作，长按打开菜单");
    frame.setOnTouchListener(new PetTouchListener());
    petView = frame;

    petParams = overlayParams(width, height);
    int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - width);
    int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - height);
    petParams.x = Math.max(0, Math.min(maxX, prefs.getInt("desktop_pet_x", dp(18))));
    petParams.y = Math.max(0, Math.min(maxY, prefs.getInt("desktop_pet_y", dp(150))));
    try {
      windowManager.addView(petView, petParams);
      petView.setScaleX(.88f);
      petView.setScaleY(.88f);
      petView.setAlpha(0f);
      petView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
    } catch (RuntimeException error) {
      prefs.edit().putBoolean("experimental_desktop_pet_enabled", false).apply();
      DiagnosticLog.record(this, "desktop_pet_window", "error",
          MainActivity.diagnosticSummary(error));
      stopSelf();
    }
  }

  private WebView createLive2DRenderer() {
    WebView view = new WebView(this);
    view.setBackgroundColor(Color.TRANSPARENT);
    view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    view.setVerticalScrollBarEnabled(false);
    view.setHorizontalScrollBarEnabled(false);
    view.getSettings().setJavaScriptEnabled(true);
    view.getSettings().setDomStorageEnabled(false);
    view.getSettings().setAllowFileAccess(false);
    view.getSettings().setAllowContentAccess(false);
    view.getSettings().setBlockNetworkLoads(true);
    view.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
    view.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onConsoleMessage(ConsoleMessage message) {
        if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
          prefs.edit().putString("desktop_pet_live2d_last_error", message.message()).apply();
        return true;
      }
    });
    view.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
        return !"shiyu.local".equals(request.getUrl().getHost());
      }

      @Override public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
        if (!"shiyu.local".equals(request.getUrl().getHost())) return emptyResponse();
        String path = request.getUrl().getPath();
        try {
          if ("/runtime/core.js".equals(path)) return response("application/javascript", new FileInputStream(Live2DRepository.runtimeFile(DesktopPetService.this)));
          if ("/assets/pixi.min.js".equals(path)) return response("application/javascript", getAssets().open("live2d/pixi.min.js"));
          if ("/assets/cubism4.min.js".equals(path)) return response("application/javascript", getAssets().open("live2d/cubism4.min.js"));
          if (path != null && path.startsWith("/model/")) {
            String relative = android.net.Uri.decode(path.substring("/model/".length()));
            return response(mimeType(relative), Live2DRepository.openModelFile(DesktopPetService.this, relative));
          }
        } catch (Exception error) {
          prefs.edit().putString("desktop_pet_live2d_last_error", error.getMessage()).apply();
        }
        return emptyResponse();
      }
    });
    String modelUrl = "/model/" + android.net.Uri.encode(Live2DRepository.modelEntry(this), "/");
    String html = "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">"
        + "<style>html,body,canvas{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}canvas{display:block}</style>"
        + "<script src=\"/runtime/core.js\"></script><script src=\"/assets/pixi.min.js\"></script><script src=\"/assets/cubism4.min.js\"></script></head>"
        + "<body><canvas id=\"pet\"></canvas><script>(async()=>{const canvas=document.getElementById('pet');"
        + "const app=new PIXI.Application({view:canvas,transparent:true,antialias:true,autoStart:true,resizeTo:window,resolution:Math.min(devicePixelRatio||1,2)});"
        + "const model=await PIXI.live2d.Live2DModel.from('" + modelUrl + "',{autoInteract:false});app.stage.addChild(model);model.anchor.set(.5,1);"
        + "function fit(){const w=innerWidth,h=innerHeight;const s=Math.min(w/model.internalModel.originalWidth,h/model.internalModel.originalHeight)*.96;model.scale.set(s);model.x=w/2;model.y=h*.99;}"
        + "fit();addEventListener('resize',fit);})().catch(e=>console.error('Live2D: '+(e&&e.message?e.message:e)));</script></body></html>";
    view.loadDataWithBaseURL("https://shiyu.local/index.html", html, "text/html", "UTF-8", null);
    view.setContentDescription("Live2D 桌宠 " + Live2DRepository.modelName(this));
    view.setOnTouchListener(new PetTouchListener());
    return view;
  }

  private WebResourceResponse response(String mime, InputStream input) {
    return new WebResourceResponse(mime, "UTF-8", input);
  }

  private WebResourceResponse emptyResponse() {
    return response("text/plain", new ByteArrayInputStream(new byte[0]));
  }

  private String mimeType(String path) {
    String value = path == null ? "" : path.toLowerCase();
    if (value.endsWith(".json")) return "application/json";
    if (value.endsWith(".png")) return "image/png";
    if (value.endsWith(".jpg") || value.endsWith(".jpeg")) return "image/jpeg";
    if (value.endsWith(".webp")) return "image/webp";
    if (value.endsWith(".wav")) return "audio/wav";
    return "application/octet-stream";
  }

  private JSONObject selectedRole() {
    try (AppDb database = new AppDb(this)) {
      JSONObject state = new JSONObject(database.loadState().isEmpty() ? "{}" : database.loadState());
      JSONArray roles = state.optJSONArray("cards");
      if (roles == null || roles.length() == 0) return new JSONObject().put("name", "时遇");
      String selected = prefs.getString("experimental_desktop_pet_card_id", "");
      for (int i = 0; i < roles.length(); i++) {
        JSONObject role = roles.optJSONObject(i);
        if (role != null && selected.equals(role.optString("id"))) return role;
      }
      JSONObject fallback = roles.optJSONObject(0);
      if (fallback != null) prefs.edit().putString("experimental_desktop_pet_card_id", fallback.optString("id")).apply();
      return fallback == null ? new JSONObject() : fallback;
    } catch (Exception ignored) {
      return new JSONObject();
    }
  }

  private String matchingChatId(String roleId) {
    if (roleId == null || roleId.isEmpty()) return "";
    try (AppDb database = new AppDb(this)) {
      JSONObject state = new JSONObject(database.loadState().isEmpty() ? "{}" : database.loadState());
      JSONArray chats = state.optJSONArray("chats");
      String best = "";
      long newest = Long.MIN_VALUE;
      for (int i = 0; chats != null && i < chats.length(); i++) {
        JSONObject chat = chats.optJSONObject(i);
        if (chat == null || chat.optBoolean("group", false)) continue;
        boolean matches = roleId.equals(chat.optString("cardId"));
        if (matches && chat.optLong("updated") >= newest) {
          newest = chat.optLong("updated");
          best = chat.optString("id");
        }
      }
      return best;
    } catch (Exception ignored) {
      return "";
    }
  }

  private void interact() {
    if (petView == null) return;
    setUnread(false);
    String[] actions = {"我轻轻碰了碰你", "我向你打了个招呼", "我摸了摸你的头", "我凑近看了看你"};
    animatePet();
    requestRoleReply(actions[random.nextInt(actions.length)], "desktop_pet_interaction");
  }

  private void handlePetTap(){
    if(petView==null)return;
    if(replying){animatePet();return;}
    if(prefs.getBoolean("desktop_pet_unread",false)){
      setUnread(false);
      if(bubbleView!=null)showQuickReply();else openApp(chatId);
      return;
    }
    if(bubbleView!=null){showQuickReply();return;}
    interact();
  }

  private void animatePet() {
    if (petView == null) return;
    petView.animate().scaleX(1.14f).scaleY(1.14f).setDuration(110).withEndAction(
        () -> petView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();
  }

  private void showMenu() {
    if (petView == null || menuView != null) return;
    hideBubble();
    LinearLayout menu = new LinearLayout(this);
    menu.setOrientation(LinearLayout.VERTICAL);
    menu.setPadding(dp(9), dp(8), dp(9), dp(9));
    GradientDrawable background = rounded(Color.argb(247, 35, 36, 40), dp(20));
    background.setStroke(dp(1), Color.argb(80, 255, 255, 255));
    menu.setBackground(background);
    menu.setElevation(dp(18));

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(dp(7), 0, dp(2), 0);
    TextView grip = new TextView(this);
    grip.setText("⠿");
    grip.setTextSize(20);
    grip.setTextColor(Color.rgb(135, 205, 192));
    grip.setGravity(Gravity.CENTER);
    header.addView(grip, new LinearLayout.LayoutParams(dp(28), dp(44)));
    LinearLayout heading = new LinearLayout(this);
    heading.setOrientation(LinearLayout.VERTICAL);
    heading.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = new TextView(this);
    title.setText(roleName.isEmpty() ? "桌宠快捷面板" : roleName + " · 桌宠");
    title.setTextSize(14);
    title.setTextColor(Color.WHITE);
    title.setSingleLine(true);
    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
    TextView subtitle = new TextView(this);
    subtitle.setText("快捷操作");
    subtitle.setTextSize(10);
    subtitle.setTextColor(Color.rgb(174, 178, 184));
    heading.addView(title, new LinearLayout.LayoutParams(-1, -2));
    heading.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
    header.addView(heading, new LinearLayout.LayoutParams(0, -1, 1f));
    TextView close = new TextView(this);
    close.setText("×");
    close.setTextSize(25);
    close.setTextColor(Color.rgb(220, 221, 224));
    close.setGravity(Gravity.CENTER);
    close.setContentDescription("关闭桌宠菜单");
    close.setBackground(rounded(Color.argb(30, 255, 255, 255), dp(12)));
    close.setOnClickListener(view -> hideMenu());
    header.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
    header.setContentDescription("拖动桌宠快捷面板");
    header.setOnTouchListener(new MenuDragTouchListener());
    menu.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

    View divider = new View(this);
    divider.setBackgroundColor(Color.argb(42, 255, 255, 255));
    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
    dividerParams.setMargins(dp(5), dp(2), dp(5), dp(7));
    menu.addView(divider, dividerParams);

    LinearLayout firstRow = menuActionRow();
    addMenuAction(firstRow, "🍰", "投喂", () -> {
      hideMenu();
      animatePet();
      requestRoleReply("我给你投喂了一份喜欢的小点心", "desktop_pet_feed");
    });
    addMenuAction(firstRow, "✎", "快捷回复", () -> { hideMenu();showQuickReply(); });
    addMenuAction(firstRow, "◉", "分析屏幕", () -> { hideMenu();analyzeCurrentScreen(); });
    menu.addView(firstRow, new LinearLayout.LayoutParams(-1, dp(76)));

    LinearLayout secondRow = menuActionRow();
    addMenuAction(secondRow, "💬", "打开聊天", () -> { hideMenu();openApp(chatId); });
    addMenuAction(secondRow, "⚙", "桌宠设置", () -> { hideMenu();openPetSettings(); });
    addMenuAction(secondRow, "↺", "重置位置", this::resetPetPosition);
    menu.addView(secondRow, new LinearLayout.LayoutParams(-1, dp(76)));

    TextView stop = new TextView(this);
    stop.setText("关闭桌宠");
    stop.setTextSize(12);
    stop.setTextColor(Color.rgb(255, 188, 193));
    stop.setGravity(Gravity.CENTER);
    stop.setCompoundDrawablePadding(dp(6));
    stop.setBackground(rounded(Color.argb(55, 226, 82, 96), dp(12)));
    stop.setOnClickListener(view -> {
      prefs.edit().putBoolean("experimental_desktop_pet_enabled", false).apply();
      hideMenu();
      stopSelf();
    });
    LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, dp(42));
    stopParams.setMargins(dp(4), dp(7), dp(4), 0);
    menu.addView(stop, stopParams);

    menuView = menu;
    menuParams = overlayParams(dp(304), -2);
    int displayWidth = getResources().getDisplayMetrics().widthPixels;
    int displayHeight = getResources().getDisplayMetrics().heightPixels;
    int defaultX = Math.max(dp(6), Math.min(displayWidth - dp(310), petParams.x - dp(112)));
    int defaultY = Math.max(dp(6), Math.min(displayHeight - dp(270), petParams.y - dp(226)));
    menuParams.x = Math.max(dp(6), Math.min(displayWidth - dp(310), prefs.getInt("desktop_pet_menu_x", defaultX)));
    menuParams.y = Math.max(dp(6), Math.min(displayHeight - dp(270), prefs.getInt("desktop_pet_menu_y", defaultY)));
    try {
      windowManager.addView(menuView, menuParams);
      menuView.setAlpha(0f);
      menuView.setScaleX(.94f);
      menuView.setScaleY(.94f);
      menuView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
    } catch (RuntimeException ignored) {
      menuView = null;
      menuParams = null;
    }
  }

  private LinearLayout menuActionRow() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER);
    return row;
  }

  private void addMenuAction(LinearLayout parent, String symbol, String label, Runnable action) {
    LinearLayout button = new LinearLayout(this);
    button.setOrientation(LinearLayout.VERTICAL);
    button.setGravity(Gravity.CENTER);
    button.setContentDescription(label);
    if (Build.VERSION.SDK_INT >= 26) button.setTooltipText(label);
    button.setBackground(rounded(Color.argb(31, 255, 255, 255), dp(13)));
    TextView icon = new TextView(this);
    icon.setText(symbol);
    icon.setTextSize(21);
    icon.setTextColor(Color.WHITE);
    icon.setGravity(Gravity.CENTER);
    TextView text = new TextView(this);
    text.setText(label);
    text.setTextSize(10);
    text.setTextColor(Color.rgb(220, 222, 225));
    text.setGravity(Gravity.CENTER);
    text.setMaxLines(1);
    button.addView(icon, new LinearLayout.LayoutParams(-1, dp(38)));
    button.addView(text, new LinearLayout.LayoutParams(-1, dp(24)));
    button.setOnClickListener(view -> action.run());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(68), 1f);
    params.setMargins(dp(3), dp(3), dp(3), dp(3));
    parent.addView(button, params);
  }

  private void requestRoleReply(String userText, String messageType) {
    if (replying) {
      if (bubbleView == null) showBubble("", true);
      return;
    }
    if (roleId.isEmpty()) {
      showBubble("请先在“我的 → 桌宠模式”中选择桌宠角色", false);
      return;
    }
    hideQuickReply();
    replying = true;
    bubbleClosedDuringReply = false;
    showBubble("", true);
    String taskId=GenerationTaskTracker.start(this,"pet",roleName+" 的桌宠互动","悬浮窗真实回复",chatId,false);
    replyExecutor.execute(() -> {
      try {
        DesktopPetReplyEngine.Result result =
            DesktopPetReplyEngine.reply(getApplicationContext(), roleId, userText, messageType);
        GenerationTaskTracker.finish(this,taskId,"桌宠回复已显示");
        handler.post(() -> {
          replying = false;
          chatId = result.chatId;
          lastBubbleAt = result.createdAt;
          prefs.edit().putLong("desktop_pet_last_bubble_at", lastBubbleAt)
              .remove("desktop_pet_last_error").apply();
          animatePet();
          String replyMode = prefs.getString("desktop_pet_reply_mode", "both");
          if ("voice".equals(replyMode) || "both".equals(replyMode)) speech.speak(result.text);
          if ("voice".equals(replyMode)) hideBubble();
          else if (bubbleClosedDuringReply) setUnread(true);
          else showBubble(result.text, false);
          bubbleClosedDuringReply = false;
        });
      } catch (Exception error) {
        if (error instanceof InterruptedException || MainActivity.isDataRescueRequired(this)) return;
        GenerationTaskTracker.fail(this,taskId,error);
        String detail = MainActivity.errorDetails(error);
        prefs.edit().putString("desktop_pet_last_error", MainActivity.friendlyError(error))
            .putString("last_api_error", detail).apply();
        DiagnosticLog.record(this, "desktop_pet", "error",
            MainActivity.diagnosticSummary(error));
        String shown = MainActivity.friendlyError(error);
        handler.post(() -> {
          replying = false;
          if (bubbleClosedDuringReply) setUnread(true); else showBubble("回复失败：" + shown, false);
          bubbleClosedDuringReply = false;
        });
      }
    });
  }

  private void analyzeCurrentScreen(){
    if(!prefs.getBoolean("experimental_screen_context_enabled",false)){
      Toast.makeText(this,"请先在“我的 → 桌宠模式”中开启屏幕内容分析",Toast.LENGTH_LONG).show();
      openApp("");
      return;
    }
    if(!ScreenContextAccessibilityService.available()){
      Toast.makeText(this,"请在无障碍设置中开启“时遇屏幕内容分析”",Toast.LENGTH_LONG).show();
      try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException ignored){}
      return;
    }
    String content=ScreenContextAccessibilityService.captureVisibleText().trim();
    if(content.isEmpty()){showBubble("当前页面没有可读取的文字",false);return;}
    requestRoleReply("用户主动授权你查看当前屏幕。请结合你的人设，简洁分析或自然回应屏幕中的内容；不要声称看到了未提供的信息。\n\n当前屏幕文字：\n"+content,"desktop_pet_screen_context");
  }

  private void showBubble(String message, boolean loading) {
    hideBubble();
    if (petView == null || message == null || (!loading && message.trim().isEmpty())) return;
    int widthDp = Math.max(180, Math.min(360, prefs.getInt("desktop_pet_bubble_width", 286)));
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.HORIZONTAL);
    panel.setGravity(Gravity.CENTER_VERTICAL);
    panel.setPadding(dp(14), dp(10), dp(7), dp(10));
    GradientDrawable background = rounded(Color.argb(246, 255, 253, 253), dp(18));
    background.setStroke(dp(1), Color.argb(150, 230, 181, 194));
    panel.setBackground(background);
    panel.setElevation(dp(12));

    LinearLayout copy = new LinearLayout(this);
    copy.setOrientation(LinearLayout.VERTICAL);
    TextView name = new TextView(this);
    name.setText(roleName);
    name.setTextColor(Color.rgb(151, 65, 86));
    name.setTextSize(11);
    name.setTypeface(null, android.graphics.Typeface.BOLD);
    copy.addView(name, new LinearLayout.LayoutParams(-1, -2));
    TextView body = new TextView(this);
    body.setText(loading ? "·" : message.trim());
    body.setTextColor(Color.rgb(42, 43, 47));
    body.setTextSize(14);
    body.setMaxLines(5);
    body.setEllipsize(android.text.TextUtils.TruncateAt.END);
    body.setLineSpacing(0, 1.08f);
    copy.addView(body, new LinearLayout.LayoutParams(-1, -2));
    copy.setOnTouchListener(new BubbleDragTouchListener());
    panel.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

    if (loading) {
      final int[] frame = {0};
      bubbleDotsTask = new Runnable() {
        @Override public void run() {
          if (bubbleView == null || !replying) return;
          frame[0] = (frame[0] + 1) % 3;
          body.setText(frame[0] == 0 ? "·" : frame[0] == 1 ? "··" : "···");
          handler.postDelayed(this, 360L);
        }
      };
      handler.postDelayed(bubbleDotsTask, 280L);
    }

    LinearLayout actions = new LinearLayout(this);
    actions.setOrientation(LinearLayout.VERTICAL);
    actions.setGravity(Gravity.CENTER);
    if (!loading) {
      ImageView reply = bubbleAction(
          android.R.drawable.ic_menu_send,
          "快捷回复",
          Color.rgb(58, 132, 116),
          Color.argb(30, 58, 132, 116));
      reply.setContentDescription("快捷回复");
      reply.setOnClickListener(view -> {setUnread(false);showQuickReply();});
      LinearLayout.LayoutParams replyLayout = new LinearLayout.LayoutParams(dp(34), dp(34));
      actions.addView(reply, replyLayout);
    }
    ImageView close = bubbleAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        "关闭回复气泡",
        Color.rgb(116, 108, 112),
        Color.argb(24, 70, 65, 68));
    close.setOnClickListener(view -> { if (replying) bubbleClosedDuringReply = true; hideBubble(); });
    LinearLayout.LayoutParams closeLayout = new LinearLayout.LayoutParams(dp(34), dp(34));
    closeLayout.setMargins(0, loading ? 0 : dp(5), 0, 0);
    actions.addView(close, closeLayout);
    LinearLayout.LayoutParams actionLayout = new LinearLayout.LayoutParams(dp(34), -2);
    actionLayout.setMargins(dp(8), 0, 0, 0);
    panel.addView(actions, actionLayout);
    if (!loading) panel.setOnClickListener(view -> {setUnread(false);openApp(chatId);});

    TextView resize = new TextView(this);
    resize.setText("⤢");
    resize.setTextColor(Color.argb(150, 116, 108, 112));
    resize.setGravity(Gravity.CENTER);
    resize.setTextSize(18);
    resize.setContentDescription("上下左右拖动调整气泡大小");
    resize.setOnTouchListener(new BubbleResizeTouchListener());
    panel.addView(resize, new LinearLayout.LayoutParams(dp(20), -1));

    bubbleView = panel;
    int width = Math.min(dp(widthDp), getResources().getDisplayMetrics().widthPixels - dp(24));
    int savedHeightDp = prefs.getInt("desktop_pet_bubble_height", 0);
    bubbleParams = overlayParams(width, savedHeightDp > 0 ? dp(savedHeightDp) : -2);
    positionBubble(bubbleParams, width, dp(loading ? 62 : 112));
    try {
      windowManager.addView(bubbleView, bubbleParams);
      bubbleView.post(this::repositionBubble);
      bubbleView.setAlpha(0f);
      bubbleView.setTranslationY(dp(8));
      bubbleView.animate().alpha(1f).translationY(0).setDuration(180).start();
      if (!loading) {
        dismissBubbleTask = this::hideBubble;
        handler.postDelayed(dismissBubbleTask, 12_000L);
      }
    } catch (RuntimeException ignored) {
      bubbleView = null;
    }
  }

  private ImageView bubbleAction(int icon, String description, int tint, int backgroundColor) {
    ImageView button = new ImageView(this);
    button.setImageResource(icon);
    button.setColorFilter(tint);
    button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    button.setPadding(dp(8), dp(8), dp(8), dp(8));
    button.setBackground(rounded(backgroundColor, dp(17)));
    button.setContentDescription(description);
    button.setClickable(true);
    button.setFocusable(true);
    if (Build.VERSION.SDK_INT >= 26) button.setTooltipText(description);
    return button;
  }

  private void showQuickReply() {
    if (petView == null || quickReplyView != null) return;
    hideMenu();
    hideBubble();
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(13), dp(10), dp(13), dp(12));
    GradientDrawable background = rounded(Color.argb(252, 255, 253, 253), dp(20));
    background.setStroke(dp(1), Color.argb(170, 230, 181, 194));
    panel.setBackground(background);
    panel.setElevation(dp(16));

    LinearLayout titleRow = new LinearLayout(this);
    titleRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = new TextView(this);
    title.setText("回复 " + roleName);
    title.setTextSize(14);
    title.setTextColor(Color.rgb(68, 51, 57));
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(34), 1));
    TextView close = new TextView(this);
    close.setText("×");
    close.setTextSize(24);
    close.setGravity(Gravity.CENTER);
    close.setTextColor(Color.rgb(110, 105, 108));
    close.setContentDescription("关闭快捷回复");
    close.setOnClickListener(view -> hideQuickReply());
    titleRow.addView(close, new LinearLayout.LayoutParams(dp(36), dp(34)));
    panel.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(34)));

    LinearLayout phrases = new LinearLayout(this);
    phrases.setGravity(Gravity.CENTER_VERTICAL);
    addQuickPhrase(phrases, "在干嘛");
    addQuickPhrase(phrases, "想你了");
    addQuickPhrase(phrases, "陪陪我");
    LinearLayout.LayoutParams phraseRow = new LinearLayout.LayoutParams(-1, dp(38));
    phraseRow.setMargins(0, dp(5), 0, dp(8));
    panel.addView(phrases, phraseRow);

    LinearLayout inputRow = new LinearLayout(this);
    inputRow.setGravity(Gravity.CENTER_VERTICAL);
    EditText input = new EditText(this);
    input.setSingleLine(true);
    input.setHint("输入消息…");
    input.setTextSize(14);
    input.setTextColor(Color.rgb(35, 36, 39));
    input.setHintTextColor(Color.rgb(145, 139, 142));
    input.setPadding(dp(12), 0, dp(10), 0);
    input.setBackground(rounded(Color.rgb(246, 241, 243), dp(14)));
    input.setImeOptions(EditorInfo.IME_ACTION_SEND);
    inputRow.addView(input, new LinearLayout.LayoutParams(0, dp(44), 1));
    TextView send = new TextView(this);
    send.setText("➤");
    send.setTextSize(20);
    send.setTextColor(Color.WHITE);
    send.setGravity(Gravity.CENTER);
    send.setContentDescription("发送快捷回复");
    send.setBackground(rounded(Color.rgb(215, 91, 119), dp(22)));
    send.setOnClickListener(view -> submitQuickReply(input));
    LinearLayout.LayoutParams sendLayout = new LinearLayout.LayoutParams(dp(44), dp(44));
    sendLayout.setMargins(dp(8), 0, 0, 0);
    inputRow.addView(send, sendLayout);
    panel.addView(inputRow, new LinearLayout.LayoutParams(-1, dp(44)));
    input.setOnEditorActionListener((view, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEND) {
        submitQuickReply(input);
        return true;
      }
      return false;
    });

    quickReplyView = panel;
    int width = Math.min(dp(320), getResources().getDisplayMetrics().widthPixels - dp(20));
    quickReplyParams = focusableOverlayParams(width, -2);
    positionOutsidePet(quickReplyParams, width, dp(150), dp(10));
    try {
      windowManager.addView(quickReplyView, quickReplyParams);
      quickReplyView.post(() -> {
        if (quickReplyView == null || quickReplyParams == null) return;
        positionOutsidePet(quickReplyParams, width, quickReplyView.getHeight(), dp(10));
        try { windowManager.updateViewLayout(quickReplyView, quickReplyParams); } catch (RuntimeException ignored) {}
      });
      quickReplyView.setAlpha(0f);
      quickReplyView.setScaleX(.96f);
      quickReplyView.setScaleY(.96f);
      quickReplyView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170).start();
      input.requestFocus();
      handler.postDelayed(() -> {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null && quickReplyView != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
      }, 180L);
    } catch (RuntimeException ignored) {
      quickReplyView = null;
    }
  }

  private void addQuickPhrase(LinearLayout parent, String phrase) {
    TextView button = new TextView(this);
    button.setText(phrase);
    button.setTextSize(12);
    button.setTextColor(Color.rgb(117, 58, 75));
    button.setGravity(Gravity.CENTER);
    button.setBackground(rounded(Color.rgb(250, 230, 236), dp(13)));
    button.setOnClickListener(view -> {
      hideQuickReply();
      requestRoleReply(phrase, "desktop_pet_quick_reply");
    });
    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, dp(34), 1);
    layout.setMargins(dp(2), 0, dp(2), 0);
    parent.addView(button, layout);
  }

  private void submitQuickReply(EditText input) {
    String value = input.getText().toString().trim();
    if (value.isEmpty()) return;
    hideQuickReply();
    requestRoleReply(value, "desktop_pet_quick_reply");
  }

  private void pollForRoleMessage() {
    if (petView == null || replying || roleId.isEmpty() || quickReplyView != null) return;
    try (AppDb database = new AppDb(this)) {
      String raw = database.loadState();
      if (raw.isEmpty()) return;
      JSONArray chats = new JSONObject(raw).optJSONArray("chats");
      JSONObject best = null;
      long newestChat = Long.MIN_VALUE;
      for (int i = 0; chats != null && i < chats.length(); i++) {
        JSONObject chat = chats.optJSONObject(i);
        if (chat == null || chat.optBoolean("group", false)
            || !roleId.equals(chat.optString("cardId"))) continue;
        if (best == null || chat.optLong("updated", 0) >= newestChat) {
          best = chat;
          newestChat = chat.optLong("updated", 0);
        }
      }
      if (best == null) return;
      chatId = best.optString("id", chatId);
      boolean chatVisible=chatId.equals(prefs.getString("visible_chat_id",""));
      if(chatVisible)setUnread(false);
      JSONArray messages = best.optJSONArray("messages");
      for (int i = messages == null ? -1 : messages.length() - 1; i >= 0; i--) {
        JSONObject message = messages.optJSONObject(i);
        if (message == null || !"assistant".equals(message.optString("role"))) continue;
        String speaker = message.optString("speakerCardId");
        if (!speaker.isEmpty() && !roleId.equals(speaker)) continue;
        long createdAt = message.optLong("createdAt", 0);
        String body = message.optString("body").trim();
        if (createdAt > lastBubbleAt && !body.isEmpty()) {
          lastBubbleAt = createdAt;
          prefs.edit().putLong("desktop_pet_last_bubble_at", createdAt).apply();
          if(!chatVisible){
            String notice=prefs.getString("desktop_pet_message_notice","both");
            if("dot".equals(notice)||"both".equals(notice))setUnread(true);else setUnread(false);
            if("bubble".equals(notice)||"both".equals(notice))showBubble(body, false);
          }
        }
        break;
      }
    } catch (Exception ignored) {}
  }

  private void positionOutsidePet(WindowManager.LayoutParams params, int width, int height, int gap) {
    int displayWidth = getResources().getDisplayMetrics().widthPixels;
    int displayHeight = getResources().getDisplayMetrics().heightPixels;
    params.x = petParams.x + (petParams.width - width) / 2;
    params.x = Math.max(dp(6), Math.min(displayWidth - width - dp(6), params.x));
    int safeHeight = Math.max(dp(48), height);
    boolean above = petParams.y >= safeHeight + gap + dp(6);
    params.y = above ? petParams.y - safeHeight - gap : petParams.y + petParams.height + gap;
    params.y = Math.max(dp(6), Math.min(displayHeight - safeHeight - dp(6), params.y));
  }

  private void repositionBubble() {
    if (bubbleView == null || bubbleParams == null) return;
    positionBubble(bubbleParams, bubbleParams.width, bubbleView.getHeight());
    try { windowManager.updateViewLayout(bubbleView, bubbleParams); } catch (RuntimeException ignored) {}
  }

  private void positionBubble(WindowManager.LayoutParams params, int width, int height) {
    int savedX = prefs.getInt("desktop_pet_bubble_x", Integer.MIN_VALUE);
    int savedY = prefs.getInt("desktop_pet_bubble_y", Integer.MIN_VALUE);
    if (savedX == Integer.MIN_VALUE || savedY == Integer.MIN_VALUE) {
      positionOutsidePet(params, width, height, dp(10));
      return;
    }
    int displayWidth = getResources().getDisplayMetrics().widthPixels;
    int displayHeight = getResources().getDisplayMetrics().heightPixels;
    int safeHeight = Math.max(dp(48), height);
    params.x = Math.max(dp(6), Math.min(displayWidth - width - dp(6), savedX));
    params.y = Math.max(dp(6), Math.min(displayHeight - safeHeight - dp(6), savedY));
  }

  private void openApp(String requestedChatId) {
    setUnread(false);
    Intent intent = new Intent(this, ComposeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    if (requestedChatId != null && !requestedChatId.isEmpty()) intent.putExtra("open_chat_id", requestedChatId);
    startActivity(intent);
  }

  private void openPetSettings() {
    Intent intent = new Intent(this, ComposeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra("open_desktop_pet_settings", true);
    startActivity(intent);
  }

  private void resetPetPosition() {
    if (petParams == null || petView == null) return;
    int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - petParams.width);
    int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - petParams.height);
    petParams.x = Math.max(0, Math.min(maxX, dp(18)));
    petParams.y = Math.max(0, Math.min(maxY, dp(150)));
    prefs.edit().putInt("desktop_pet_x", petParams.x).putInt("desktop_pet_y", petParams.y)
        .remove("desktop_pet_menu_x").remove("desktop_pet_menu_y").apply();
    try { windowManager.updateViewLayout(petView, petParams); } catch (RuntimeException ignored) {}
    hideMenu();
    animatePet();
    Toast.makeText(this, "桌宠位置已重置", Toast.LENGTH_SHORT).show();
  }

  private void setUnread(boolean unread){
    prefs.edit().putBoolean("desktop_pet_unread",unread).apply();
    if(unreadView!=null)unreadView.setVisibility(unread?View.VISIBLE:View.GONE);
  }

  private void hideMenu() {
    if (menuView == null) return;
    try { windowManager.removeView(menuView); } catch (RuntimeException ignored) {}
    menuView = null;
    menuParams = null;
  }

  private void hideBubble() {
    if (dismissBubbleTask != null) handler.removeCallbacks(dismissBubbleTask);
    if (bubbleDotsTask != null) handler.removeCallbacks(bubbleDotsTask);
    dismissBubbleTask = null;
    bubbleDotsTask = null;
    if (bubbleView == null) return;
    try { windowManager.removeView(bubbleView); } catch (RuntimeException ignored) {}
    bubbleView = null;
    bubbleParams = null;
  }

  private void hideQuickReply() {
    if (quickReplyView == null) return;
    InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (keyboard != null) keyboard.hideSoftInputFromWindow(quickReplyView.getWindowToken(), 0);
    try { windowManager.removeView(quickReplyView); } catch (RuntimeException ignored) {}
    quickReplyView = null;
    quickReplyParams = null;
  }

  private WindowManager.LayoutParams overlayParams(int width, int height) {
    int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height, type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.TOP | Gravity.START;
    return params;
  }

  private WindowManager.LayoutParams focusableOverlayParams(int width, int height) {
    int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height, type,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.TOP | Gravity.START;
    params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    return params;
  }

  private GradientDrawable rounded(int color, float radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    return drawable;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private void removeOverlays() {
    hideMenu();
    hideBubble();
    hideQuickReply();
    if (petView == null) return;
    try { windowManager.removeView(petView); } catch (RuntimeException ignored) {}
    if (live2dWebView != null) {
      live2dWebView.stopLoading();
      live2dWebView.loadUrl("about:blank");
      live2dWebView.destroy();
      live2dWebView = null;
    }
    petView = null;
    unreadView = null;
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    removeOverlays();
    if (speech != null) speech.release();
    replyExecutor.shutdownNow();
    CrashProtection.clearFeatureActive(this, CrashProtection.FEATURE_DESKTOP_PET);
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) { return null; }

  private final class MenuDragTouchListener implements View.OnTouchListener {
    private float downRawX, downRawY;
    private int startX, startY;
    private boolean moved;

    @Override public boolean onTouch(View view, MotionEvent event) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          if (menuParams == null || menuView == null) return false;
          menuView.animate().cancel();
          downRawX = event.getRawX();
          downRawY = event.getRawY();
          startX = menuParams.x;
          startY = menuParams.y;
          moved = false;
          return true;
        case MotionEvent.ACTION_MOVE:
          if (menuParams == null || menuView == null) return true;
          float dx = event.getRawX() - downRawX;
          float dy = event.getRawY() - downRawY;
          if (!moved && Math.hypot(dx, dy) > ViewConfiguration.get(DesktopPetService.this).getScaledTouchSlop()) moved = true;
          if (moved) {
            int width = Math.max(dp(304), menuView.getWidth());
            int height = Math.max(dp(248), menuView.getHeight());
            int maxX = Math.max(dp(6), getResources().getDisplayMetrics().widthPixels - width - dp(6));
            int maxY = Math.max(dp(6), getResources().getDisplayMetrics().heightPixels - height - dp(6));
            menuParams.x = Math.max(dp(6), Math.min(maxX, startX + Math.round(dx)));
            menuParams.y = Math.max(dp(6), Math.min(maxY, startY + Math.round(dy)));
            try { windowManager.updateViewLayout(menuView, menuParams); } catch (RuntimeException ignored) {}
          }
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if (moved && menuParams != null) prefs.edit().putInt("desktop_pet_menu_x", menuParams.x).putInt("desktop_pet_menu_y", menuParams.y).apply();
          return true;
        default:
          return false;
      }
    }
  }

  private final class PetTouchListener implements View.OnTouchListener {
    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;
    private boolean moved;
    private boolean longPressed;
    private boolean dismissedPanel;
    private Runnable longPress;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          dismissedPanel=menuView!=null||quickReplyView!=null;
          hideMenu();
          hideQuickReply();
          downRawX = event.getRawX();
          downRawY = event.getRawY();
          startX = petParams.x;
          startY = petParams.y;
          moved = false;
          longPressed = false;
          longPress = dismissedPanel ? null : () -> { if (!moved) { longPressed = true;showMenu(); } };
          if(longPress!=null)handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
          return true;
        case MotionEvent.ACTION_MOVE:
          float dx = event.getRawX() - downRawX;
          float dy = event.getRawY() - downRawY;
          if (Math.hypot(dx, dy) > ViewConfiguration.get(DesktopPetService.this).getScaledTouchSlop()) {
            moved = true;
            if(longPress!=null)handler.removeCallbacks(longPress);
            hideMenu();
          }
          if (moved) {
            int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - petParams.width);
            int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - petParams.height);
            petParams.x = Math.max(0, Math.min(maxX, startX + Math.round(dx)));
            petParams.y = Math.max(0, Math.min(maxY, startY + Math.round(dy)));
            try { windowManager.updateViewLayout(petView, petParams); } catch (RuntimeException ignored) {}
            if (bubbleView != null && bubbleParams != null) {
              repositionBubble();
            }
          }
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if(longPress!=null)handler.removeCallbacks(longPress);
          if (moved) prefs.edit().putInt("desktop_pet_x", petParams.x).putInt("desktop_pet_y", petParams.y).apply();
          else if (!dismissedPanel&&!longPressed && event.getActionMasked() == MotionEvent.ACTION_UP && petView != null) petView.performClick();
          return true;
        default:
          return false;
      }
    }
  }

  private final class BubbleDragTouchListener implements View.OnTouchListener {
    private float downRawX, downRawY;
    private int startX, startY;
    private boolean moved;

    @Override public boolean onTouch(View view, MotionEvent event) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          if (bubbleParams == null) return false;
          downRawX=event.getRawX();downRawY=event.getRawY();startX=bubbleParams.x;startY=bubbleParams.y;moved=false;
          return true;
        case MotionEvent.ACTION_MOVE:
          float dx=event.getRawX()-downRawX,dy=event.getRawY()-downRawY;
          if(!moved&&Math.hypot(dx,dy)>ViewConfiguration.get(DesktopPetService.this).getScaledTouchSlop())moved=true;
          if(moved){
            if (bubbleParams == null || bubbleView == null) return true;
            int maxX=Math.max(0,getResources().getDisplayMetrics().widthPixels-bubbleParams.width);
            int maxY=Math.max(0,getResources().getDisplayMetrics().heightPixels-bubbleView.getHeight());
            bubbleParams.x=Math.max(0,Math.min(maxX,startX+Math.round(dx)));
            bubbleParams.y=Math.max(0,Math.min(maxY,startY+Math.round(dy)));
            try{windowManager.updateViewLayout(bubbleView,bubbleParams);}catch(RuntimeException ignored){}
          }
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if(moved&&bubbleParams!=null)prefs.edit().putInt("desktop_pet_bubble_x",bubbleParams.x).putInt("desktop_pet_bubble_y",bubbleParams.y).apply();
          else if(event.getActionMasked()==MotionEvent.ACTION_UP&&!replying&&bubbleView!=null)bubbleView.performClick();
          return true;
        default:return false;
      }
    }
  }

  private final class BubbleResizeTouchListener implements View.OnTouchListener {
    private float downRawX, downRawY;
    private int startWidth, startHeight;
    private boolean moved;

    @Override public boolean onTouch(View view, MotionEvent event) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          downRawX = event.getRawX();
          downRawY = event.getRawY();
          startWidth = bubbleParams == null ? dp(286) : bubbleParams.width;
          startHeight = bubbleView == null ? dp(112) : Math.max(dp(62), bubbleView.getHeight());
          moved = false;
          return true;
        case MotionEvent.ACTION_MOVE:
          float dx = event.getRawX() - downRawX;
          float dy = event.getRawY() - downRawY;
          if (!moved && Math.hypot(dx, dy) > ViewConfiguration.get(DesktopPetService.this).getScaledTouchSlop()) moved = true;
          if (moved && bubbleParams != null && bubbleView != null) {
            int maxWidth = Math.min(dp(360), getResources().getDisplayMetrics().widthPixels - dp(24));
            int maxHeight = Math.min(dp(320), getResources().getDisplayMetrics().heightPixels - dp(48));
            int width = Math.max(dp(180), Math.min(maxWidth, startWidth + Math.round(dx)));
            int height = Math.max(dp(62), Math.min(maxHeight, startHeight + Math.round(dy)));
            bubbleParams.width = width;
            bubbleParams.height = height;
            bubbleParams.x = Math.max(0, Math.min(getResources().getDisplayMetrics().widthPixels - width, bubbleParams.x));
            bubbleParams.y = Math.max(0, Math.min(getResources().getDisplayMetrics().heightPixels - height, bubbleParams.y));
            try { windowManager.updateViewLayout(bubbleView, bubbleParams); } catch (RuntimeException ignored) {}
          }
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if (moved && bubbleParams != null) prefs.edit()
              .putInt("desktop_pet_bubble_width", Math.round(bubbleParams.width / getResources().getDisplayMetrics().density))
              .putInt("desktop_pet_bubble_height", Math.round(bubbleParams.height / getResources().getDisplayMetrics().density)).apply();
          return true;
        default:return false;
      }
    }
  }
}
