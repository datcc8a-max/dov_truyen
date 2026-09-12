(function(){
  if(window.DocReader)return;
  const R={sentences:[],nodes:[],starts:[],manual:false,clickMode:true};
  const ignored='script,style,noscript,svg,canvas,nav,header,footer,aside,button,input,textarea,select,[hidden],[aria-hidden="true"]';
  const visible=e=>!!e&&(e.offsetWidth||e.offsetHeight||e.getClientRects().length)&&getComputedStyle(e).visibility!=='hidden';
  const stv=()=>/(^|\.)sangtacviet\.(app|com|vip)$/i.test(location.hostname);
  function root(){
    if(stv())return document.querySelector('#content-container .contentbox[id^="cld-"]')||document.querySelector('#content-container');
    return [...document.querySelectorAll('article,main,[role="main"],.chapter-content,.reading-content,.entry-content,.post-content,.chapter-c')]
      .filter(visible).sort((a,b)=>(b.innerText||'').length-(a.innerText||'').length)[0]||document.body;
  }
  function collect(){
    const base=root();if(!base)return 0;
    R.nodes=[];R.starts=[];let text='',last=null;
    const w=document.createTreeWalker(base,NodeFilter.SHOW_TEXT,{acceptNode(n){const p=n.parentElement,t=n.nodeValue;if(!p||!t.trim()||p.closest(ignored)||!visible(p))return NodeFilter.FILTER_REJECT;return NodeFilter.FILTER_ACCEPT}});
    let n;while(n=w.nextNode()){
      const block=n.parentElement.closest('p,li,blockquote,h1,h2,h3,h4,h5,h6,section,div')||n.parentElement;
      if(R.nodes.length)text+=block!==last&&!/[.!?…]\s*$/.test(text)?'. ':' ';
      R.starts.push(text.length);R.nodes.push(n);text+=n.nodeValue;last=block;
    }
    const seg=Intl.Segmenter?[...new Intl.Segmenter('vi',{granularity:'sentence'}).segment(text)].map(x=>({text:x.segment,a:x.index,b:x.index+x.segment.length})):[...text.matchAll(/[^.!?…\n]+(?:[.!?…]+|$)/g)].map(x=>({text:x[0],a:x.index,b:x.index+x[0].length}));
    R.sentences=seg.filter(x=>x.text.trim()).map(x=>{const ranges=[];R.nodes.forEach((node,i)=>{const ns=R.starts[i],ne=ns+node.nodeValue.length,a=Math.max(x.a,ns),b=Math.min(x.b,ne);if(a<b){const q=document.createRange();q.setStart(node,a-ns);q.setEnd(node,b-ns);ranges.push(q)}});return{text:x.text.trim(),ranges}}).filter(x=>x.ranges.length);
    return R.sentences.length;
  }
  function clear(){
    if(CSS.highlights){CSS.highlights.delete('doc-app-sentence');CSS.highlights.delete('doc-app-word')}
    document.querySelectorAll('.doc-app-active').forEach(e=>e.classList.remove('doc-app-active'));
  }
  function mark(i,start,len){
    clear();const s=R.sentences[i];if(!s)return;
    if(CSS.highlights)CSS.highlights.set('doc-app-sentence',new Highlight(...s.ranges));
    else s.ranges.forEach(r=>{const e=r.startContainer.parentElement;e&&e.classList.add('doc-app-active')});
    let seen=0,target=null;
    if(start>=0)for(const r of s.ranges){const l=r.toString().length,a=Math.max(0,start-seen),b=Math.min(l,start+Math.max(1,len)-seen);if(a<b){target=document.createRange();target.setStart(r.startContainer,r.startOffset+a);target.setEnd(r.endContainer,r.startOffset+b);break}seen+=l}
    if(target&&CSS.highlights)CSS.highlights.set('doc-app-word',new Highlight(target));
    const r=target||s.ranges[0],rect=r.getBoundingClientRect();
    if(!R.manual&&rect.bottom>innerHeight*.76)scrollBy({top:Math.max(40,rect.bottom-innerHeight*.62),behavior:'smooth'});
    else if(!R.manual&&rect.top<innerHeight*.12)scrollBy({top:rect.top-innerHeight*.25,behavior:'smooth'});
  }
  function text(i){if(!R.sentences.length)collect();return R.sentences[i]?R.sentences[i].text:''}
  function count(){return collect()}
  function next(){
    const n=stv()?document.querySelector('#navnexttop,#navnextbot'):document.querySelector('a[rel~="next"]')||[...document.querySelectorAll('a,button')].find(e=>/^(chương|chuong)\s*(sau|tiếp|tiep)|next\s*chapter/i.test((e.innerText||'').trim()));
    if(!n)return false;
    const before=(root()?.innerText||'').trim();n.click();
    let tries=0;const timer=setInterval(()=>{tries++;const now=(root()?.innerText||'').trim();if(now&&now!==before){clearInterval(timer);setTimeout(()=>{collect();AndroidReader.chapterReady()},700)}else if(tries>80)clearInterval(timer)},250);
    return true;
  }
  addEventListener('wheel',()=>R.manual=true,{passive:true});addEventListener('touchmove',()=>R.manual=true,{passive:true});
  document.addEventListener('click',e=>{
    if(!R.clickMode||e.target.closest('a,button,input,textarea,select'))return;
    const base=root();if(!base||!base.contains(e.target))return;
    if(!R.sentences.length)collect();
    const p=document.caretRangeFromPoint&&document.caretRangeFromPoint(e.clientX,e.clientY);if(!p)return;
    for(let i=0;i<R.sentences.length;i++)for(const q of R.sentences[i].ranges){try{if(q.comparePoint(p.startContainer,p.startOffset)===0){R.manual=false;AndroidReader.startFrom(i);return}}catch(_){}}
  },true);
  const css=document.createElement('style');css.textContent='::highlight(doc-app-sentence){background:#ffe36e;color:#111}::highlight(doc-app-word){background:#ff9f43;color:#111}.doc-app-active{background:#ffe36e!important;color:#111!important}';document.documentElement.appendChild(css);
  R.collect=collect;R.count=count;R.text=text;R.mark=mark;R.clear=clear;R.next=next;R.follow=()=>{R.manual=false};R.setClick=v=>R.clickMode=!!v;window.DocReader=R;
})();
