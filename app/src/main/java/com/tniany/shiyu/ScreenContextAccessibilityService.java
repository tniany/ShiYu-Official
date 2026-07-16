package com.tniany.shiyu;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.HashSet;

/** Provides an on-demand, user-triggered snapshot of visible accessibility text. */
public final class ScreenContextAccessibilityService extends AccessibilityService {
  private static volatile ScreenContextAccessibilityService active;

  static boolean available() { return active != null; }

  static String captureVisibleText() {
    ScreenContextAccessibilityService service=active;
    if(service==null)return "";
    AccessibilityNodeInfo root=service.getRootInActiveWindow();
    if(root==null)return "";
    StringBuilder result=new StringBuilder();
    ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();
    HashSet<String> seen=new HashSet<>();
    queue.add(root);
    try{
      while(!queue.isEmpty()&&result.length()<6000){
        AccessibilityNodeInfo node=queue.removeFirst();
        if(node.isVisibleToUser()){
          String text=node.isPassword()?"[密码输入框]":clean(node.getText());
          String description=node.isPassword()?"":clean(node.getContentDescription());
          appendUnique(result,seen,text);appendUnique(result,seen,description);
          for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.addLast(child);}
        }
        if(node!=root)node.recycle();
      }
    }finally{root.recycle();}
    return result.length()>6000?result.substring(0,6000):result.toString();
  }

  private static String clean(CharSequence value){return value==null?"":value.toString().replaceAll("\\s+"," ").trim();}
  private static void appendUnique(StringBuilder out,HashSet<String> seen,String value){
    if(value.isEmpty()||value.length()>1000||!seen.add(value))return;
    if(out.length()>0)out.append('\n');out.append(value);
  }

  @Override protected void onServiceConnected(){super.onServiceConnected();active=this;}
  @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
  @Override public void onInterrupt() {}
  @Override public void onDestroy(){if(active==this)active=null;super.onDestroy();}
}
