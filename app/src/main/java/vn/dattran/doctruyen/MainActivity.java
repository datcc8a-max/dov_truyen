package vn.dattran.doctruyen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
  WebView web; TextToSpeech tts; EditText address; Spinner speed;
  CheckBox autoNext, clickRead; int current=0,total=0; boolean reading=false,ready=false,loadingNext=false;
  String readerJs="";

  @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
  @Override public void onCreate(Bundle b){super.onCreate(b);
    readerJs=asset("reader.js"); tts=new TextToSpeech(this,this);
    LinearLayout all=new LinearLayout(this);all.setOrientation(LinearLayout.VERTICAL);all.setBackgroundColor(Color.WHITE);
    LinearLayout nav=row();address=new EditText(this);address.setSingleLine();address.setText("https://sangtacviet.app/");nav.addView(address,new LinearLayout.LayoutParams(0,-2,1));
    Button go=button("Mở");nav.addView(go);all.addView(nav);
    LinearLayout tools=row();Button play=button("Đọc");Button pause=button("Dừng");Button back=button("◀");Button next=button("▶");Button follow=button("Bám theo");
    tools.addView(play);tools.addView(pause);tools.addView(back);tools.addView(next);tools.addView(follow);all.addView(tools);
    LinearLayout opts=row();speed=new Spinner(this);String[] rates={"1x","1.5x","2x","2.5x","3x","3.5x","4x","4.5x","5x","5.5x","6x","6.5x","7x","7.5x"};speed.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,rates));
    autoNext=new CheckBox(this);autoNext.setText("Tự sang chương");clickRead=new CheckBox(this);clickRead.setText("Chạm để đọc");clickRead.setChecked(true);opts.addView(speed);opts.addView(autoNext);opts.addView(clickRead);all.addView(opts);
    web=new WebView(this);all.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(all);
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setUserAgentString(s.getUserAgentString()+" DocTruyenAndroid/1.0");
    web.addJavascriptInterface(new Bridge(),"AndroidReader");web.setWebChromeClient(new WebChromeClient());web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String u){address.setText(u);v.evaluateJavascript(readerJs+";DocReader.collect()",x->{});if(reading&&loadingNext){loadingNext=false;v.postDelayed(()->start(0),900);}}});
    go.setOnClickListener(v->{String u=address.getText().toString().trim();if(!u.startsWith("http"))u="https://"+u;web.loadUrl(u);});
    play.setOnClickListener(v->start(current));pause.setOnClickListener(v->stopRead());back.setOnClickListener(v->start(Math.max(0,current-1)));next.setOnClickListener(v->start(Math.min(total-1,current+1)));follow.setOnClickListener(v->web.evaluateJavascript("DocReader.follow();DocReader.mark("+current+",-1,0)",null));
    clickRead.setOnCheckedChangeListener((x,on)->web.evaluateJavascript("DocReader.setClick("+on+")",null));
    web.loadUrl("https://sangtacviet.app/");
  }
  LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);x.setGravity(17);return x;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  String asset(String name){try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] q=new byte[4096];int n;while((n=in.read(q))>0)out.write(q,0,n);return out.toString("UTF-8");}catch(Exception e){return "";}}
  float rate(){String s=(String)speed.getSelectedItem();return Float.parseFloat(s.replace("x",""));}
  void start(int i){if(!ready){toast("Đang khởi tạo giọng đọc");return;}reading=true;current=Math.max(0,i);web.evaluateJavascript("DocReader.count()",v->{try{total=Integer.parseInt(v);}catch(Exception e){total=0;}speakCurrent();});}
  void speakCurrent(){if(!reading||current>=total){finishChapter();return;}web.evaluateJavascript("DocReader.text("+current+")",v->{String text=unquote(v);if(text.isEmpty()){current++;speakCurrent();return;}tts.stop();tts.setSpeechRate(rate());Bundle p=new Bundle();tts.speak(text,TextToSpeech.QUEUE_FLUSH,p,"sentence-"+current);});}
  void finishChapter(){tts.stop();web.evaluateJavascript("DocReader.clear()",null);if(reading&&autoNext.isChecked()){loadingNext=true;web.evaluateJavascript("DocReader.next()",v->{if(!"true".equals(v)){loadingNext=false;reading=false;toast("Đã đọc hết chương");}});}else{reading=false;toast("Đã đọc hết chương");}}
  void stopRead(){reading=false;tts.stop();web.evaluateJavascript("DocReader.clear()",null);}
  String unquote(String s){if(s==null||"null".equals(s))return "";if(s.length()>1&&s.charAt(0)=='\"')s=s.substring(1,s.length()-1);return s.replace("\\n","\n").replace("\\\"","\"").replace("\\\\","\\");}
  void toast(String s){runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}
  @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS){int r=tts.setLanguage(new Locale("vi","VN"));ready=r!=TextToSpeech.LANG_MISSING_DATA&&r!=TextToSpeech.LANG_NOT_SUPPORTED;tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onError(String id){}public void onDone(String id){runOnUiThread(()->{if(reading){current++;speakCurrent();}});}public void onRangeStart(String id,int start,int end,int frame){runOnUiThread(()->web.evaluateJavascript("DocReader.mark("+current+","+start+","+(end-start)+")",null));}});}if(!ready)toast("Điện thoại chưa có giọng tiếng Việt");}
  public class Bridge{
    @JavascriptInterface public void startFrom(int i){runOnUiThread(()->start(i));}
    @JavascriptInterface public void chapterReady(){runOnUiThread(()->{if(reading&&loadingNext){loadingNext=false;start(0);}});}
  }
  @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
