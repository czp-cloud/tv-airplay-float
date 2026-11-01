#!/usr/bin/env node
// Browser interaction tests against a fixture API. Real TV/API checks are separate.
const {chromium, webkit} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.join(__dirname, '../app/src/main/assets/remote');
const out = path.resolve(process.env.QA_OUT || 'artifacts/web-smoke');
fs.mkdirSync(out, {recursive:true});
let state, offline = false, commands = 0;
function reset() {
  state = {version:'0.3.0',receiver:'电视投屏小窗',server:'RUNNING',autoStart:true,mirroring:false,
    overlay:{visible:false,screenWidth:1920,screenHeight:1080,minWidth:240,maxWidth:1536,x:1584,y:16,width:320,height:180},
    audio:{source:'tv',volume:100,gain:1,outputActive:false,focusHeld:false,message:'正在播放电视原声'}};
  commands = 0; offline = false;
}
reset();
const server = http.createServer(async (req,res) => {
  if (offline) {res.destroy(); return;}
  const files={'/':'index.html','/remote.js':'remote.js','/remote.css':'remote.css'};
  if (files[req.url]) {
    const file=files[req.url];
    res.setHeader('Content-Type', file.endsWith('js') ? 'application/javascript' : file.endsWith('css') ? 'text/css' : 'text/html');
    res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'");
    return res.end(fs.readFileSync(path.join(root,file)));
  }
  let raw=''; for await(const chunk of req) raw+=chunk;
  const data=raw?JSON.parse(raw):{};
  res.setHeader('Content-Type','application/json');
  if(req.url==='/api/move') {commands++;state.overlay.x+=data.dx;state.overlay.y+=data.dy;}
  if(req.url==='/api/size') {commands++;state.overlay.width=Math.max(240,Math.min(1536,data.width));state.overlay.height=Math.floor(state.overlay.width*9/16);}
  if(req.url==='/api/volume') {commands++;state.audio.volume=data.volume;state.audio.gain=data.volume/100;}
  if(req.url==='/api/audio') {commands++;state.audio.source=data.source;state.audio.message=data.source==='tv'?'正在播放电视原声':data.source==='mixed'?'连接投屏后混合播放':'连接投屏后切换声音';}
  if(req.url==='/api/autostart') {commands++;state.autoStart=data.enabled;}
  if(req.url==='/api/reset') {commands++;Object.assign(state.overlay,{x:1584,y:16,width:320,height:180});}
  const o=state.overlay;o.x=Math.max(0,Math.min(1920-o.width,o.x));o.y=Math.max(0,Math.min(1080-o.height,o.y));
  res.end(JSON.stringify(state));
});
const checks=[];
function check(name, value){assert.ok(value,name);checks.push(name);console.log('PASS',name);}
async function until(condition) {for(let i=0;i<50;i++){if(condition())return;await new Promise(r=>setTimeout(r,50));}throw new Error('Fixture condition timeout');}
(async()=>{
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const url=`http://127.0.0.1:${server.address().port}`;
  for(const engine of ['chromium','webkit']) {
    reset();
    const browser=await (engine==='webkit'?webkit:chromium).launch(engine==='chromium'?{headless:true,args:['--disable-gpu'],executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}:{headless:true});
    try {
      const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:2,isMobile:true,hasTouch:true,reducedMotion:'reduce'});
      const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
      await page.goto(url);await page.getByText('已连接电视',{exact:true}).waitFor();
      check(engine+': opens directly without pairing',await page.locator('#pairCode').count()===0 && await page.locator('#dashboard').isVisible());
      check(engine+': TV mode hides mirror volume',await page.locator('#mirrorVolumeControl').isHidden());
      for(const width of [320,375,390,430,768,1280]) {
        const sized=await browser.newContext({viewport:{width,height:900},isMobile:width<700,hasTouch:width<700,reducedMotion:'reduce'});
        const sizedPage=await sized.newPage();await sizedPage.goto(url);await sizedPage.getByText('已连接电视',{exact:true}).waitFor();
        check(engine+': no horizontal overflow '+width,await sizedPage.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
        if(width===1280) await sizedPage.screenshot({path:path.join(out,engine+'-desktop.png'),fullPage:true});
        await sized.close();
      }
      await page.locator('#toast').waitFor({state:'hidden'});
      await page.screenshot({path:path.join(out,engine+'-phone.png'),fullPage:true});
      const left=page.locator('.direction.left');await left.scrollIntoViewIfNeeded();
      const box=await left.boundingBox();
      await page.mouse.move(box.x+box.width/2,box.y+box.height/2);const before=state.overlay.x;
      await page.mouse.down();await page.waitForTimeout(650);await page.mouse.up();await page.waitForTimeout(180);
      check(engine+': long press moves repeatedly',state.overlay.x<=before-48);
      const stopped=state.overlay.x;await page.waitForTimeout(350);check(engine+': releasing stops movement',state.overlay.x===stopped);
      // Touch tap must work without waiting for mouse events.
      await page.locator('.direction.down').scrollIntoViewIfNeeded();
      const b=await page.locator('.direction.down').boundingBox();const y=state.overlay.y;
      await page.touchscreen.tap(b.x+b.width/2,b.y+b.height/2);await until(()=>state.overlay.y>y);
      check(engine+': touch direction',state.overlay.y>y);
      // Pointer cancellation cannot leave a repeating input running.
      const startCancel=state.overlay.x;
      await left.scrollIntoViewIfNeeded();const cancelBox=await left.boundingBox();
      await page.mouse.move(cancelBox.x+cancelBox.width/2,cancelBox.y+cancelBox.height/2);
      await page.mouse.down();
      await page.dispatchEvent('.direction.left','pointercancel',{pointerId:1,pointerType:'mouse'});
      await page.mouse.up();
      await page.waitForTimeout(500);check(engine+': cancellation releases input',state.overlay.x>=startCancel-24);
      await page.locator('#stick').scrollIntoViewIfNeeded();const stickBox=await page.locator('#stick').boundingBox();
      const stickBefore={x:state.overlay.x,y:state.overlay.y};
      await page.mouse.move(stickBox.x+stickBox.width/2,stickBox.y+stickBox.height/2);await page.mouse.down();
      await page.mouse.move(stickBox.x+stickBox.width/2+24,stickBox.y+stickBox.height/2+14);await page.waitForTimeout(400);await page.mouse.up();await page.waitForTimeout(120);
      check(engine+': analog joystick moves diagonally',state.overlay.x>stickBefore.x&&state.overlay.y>stickBefore.y);
      const afterStick=state.overlay.x;await page.waitForTimeout(220);check(engine+': analog release stops movement',state.overlay.x===afterStick);
      const slider=page.locator('#sizeSlider');await slider.scrollIntoViewIfNeeded();
      await slider.evaluate(el=>{el.value='780';el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));});await until(()=>state.overlay.width===780);
      check(engine+': slider sends absolute size',state.overlay.width===780);
      await page.getByRole('radio',{name:'仅投屏',exact:true}).tap();await until(()=>state.audio.source==='mirror');
      check(engine+': mirror option selects mirror',state.audio.source==='mirror');
      await page.locator('#mirrorVolumeControl').waitFor({state:'visible'});
      const volume=page.locator('#volumeSlider');await volume.scrollIntoViewIfNeeded();
      await volume.evaluate(el=>{el.value='35';el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));});
      await until(()=>state.audio.volume===35);check(engine+': volume maps to own gain',Math.abs(state.audio.gain-.35)<.00001);
      await volume.evaluate(el=>{el.value='0';el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));});
      await until(()=>state.audio.volume===0);check(engine+': zero mutes receiver',state.audio.gain===0);
      await volume.evaluate(el=>{el.value='70';el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));});
      await until(()=>state.audio.volume===70);
      await page.screenshot({path:path.join(out,engine+'-phone-volume.png'),fullPage:true});
      for(const width of [320,430]) {
        const sized=await browser.newContext({viewport:{width,height:900},isMobile:true,hasTouch:true,reducedMotion:'reduce'});
        const sizedPage=await sized.newPage();await sizedPage.goto(url);await sizedPage.locator('#mirrorVolumeControl').waitFor({state:'visible'});
        check(engine+': volume layout no overflow '+width,await sizedPage.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await sized.close();
      }
      await page.getByRole('radio',{name:'混合声音',exact:true}).tap();await until(()=>state.audio.source==='mixed');
      check(engine+': mixed option selects mixed',state.audio.source==='mixed');
      check(engine+': mixed mode exposes receiver volume',await page.locator('#mirrorVolumeControl').isVisible());
      await page.screenshot({path:path.join(out,engine+'-phone-mixed.png'),fullPage:true});
      await page.getByRole('radio',{name:'仅电视',exact:true}).tap();await until(()=>state.audio.source==='tv');
      check(engine+': TV option selects TV',state.audio.source==='tv');
      await page.getByRole('radio',{name:'仅电视',exact:true}).focus();await page.keyboard.press('ArrowRight');await until(()=>state.audio.source==='mirror');
      check(engine+': mode keyboard navigation',await page.getByRole('radio',{name:'仅投屏',exact:true}).getAttribute('aria-checked')==='true');
      await page.getByRole('radio',{name:'仅电视',exact:true}).click();await until(()=>state.audio.source==='tv');
      await page.getByRole('switch',{name:'开机自动接收',exact:true}).uncheck();await until(()=>state.autoStart===false);
      check(engine+': autostart can be disabled',state.autoStart===false);
      await page.getByRole('switch',{name:'开机自动接收',exact:true}).check();await until(()=>state.autoStart===true);
      check(engine+': autostart can be enabled',state.autoStart===true);
      await page.locator('#mirrorVolumeControl').waitFor({state:'hidden'});check(engine+': changing source preserves volume',state.audio.volume===70);
      await page.reload();await page.getByText('已连接电视',{exact:true}).waitFor();check(engine+': reload needs no code',await page.locator('#pairCode').count()===0);

      await page.locator('#resetButton').click();await until(()=>state.overlay.width===320&&state.overlay.x===1584);
      check(engine+': reset corner and size',state.overlay.width===320&&state.overlay.x===1584);
      state.mirroring=true;state.overlay.visible=true;await page.getByText('正在投屏',{exact:true}).waitFor();
      check(engine+': live state updates',await page.locator('#miniWindow').evaluate(el=>!el.classList.contains('idle')));

      offline=true;await page.getByText('连接中断 · 正在重连',{exact:true}).waitFor();
      check(engine+': offline controls disabled',await page.locator('#dashboard').evaluate(el=>el.inert));
      offline=false;await page.getByText('已连接电视',{exact:true}).waitFor();
      check(engine+': reconnects automatically',await page.locator('#dashboard').evaluate(el=>!el.inert));
      await page.waitForTimeout(200);check(engine+': no uncaught JavaScript errors',errors.length===0);
      await context.close();
    } finally {await browser.close();}
  }
  fs.writeFileSync(path.join(out,'report.json'),JSON.stringify({kind:'browser tests with fixture API; not actual TV audio verification',checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>server.close());
