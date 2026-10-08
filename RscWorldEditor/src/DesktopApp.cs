using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Runtime.InteropServices;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

static class Program {
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode)] static extern bool SetDllDirectory(string path);
    [STAThread] static void Main(string[] args) {
        string root=AppDomain.CurrentDomain.BaseDirectory;
        SetDllDirectory(Path.Combine(root,"RscWorldEditor","desktop"));
        Application.EnableVisualStyles(); Application.SetCompatibleTextRenderingDefault(false);
        string key; using(var sha=SHA256.Create()) key=BitConverter.ToString(sha.ComputeHash(Encoding.UTF8.GetBytes(root.ToLowerInvariant()))).Replace("-","");
        bool first; using(var mutex=new Mutex(true,"Local\\Rsc World Editor-"+key,out first)) {
            if(!first){MessageBox.Show("Rsc World Editor is already open. Switch to its existing window.","RscWorldEditor");return;}
            try { Application.Run(new EditorWindow(root,Array.IndexOf(args,"--self-test")>=0)); }
            catch(Exception ex){File.AppendAllText(Path.Combine(root,"RscWorldEditor","desktop.log"),ex+Environment.NewLine);MessageBox.Show("Rsc World Editor could not start. Details are in RscWorldEditor/desktop.log.\n\n"+ex.Message,"RscWorldEditor",MessageBoxButtons.OK,MessageBoxIcon.Error);}
        }
    }
}

sealed class EditorWindow : Form {
    readonly string root,editor; readonly bool selfTest;
    WebView2 view; Process engine; Uri address; bool closing,closePending,starting;
    ClassicLoadingPanel loading; Button retry; System.Windows.Forms.Timer monitor;
    public EditorWindow(string path,bool test) {
        root=path;editor=Path.Combine(root,"RscWorldEditor");selfTest=test;
        Text="RuneScape Classic World Editor";MinimumSize=new Size(1000,700);StartPosition=FormStartPosition.Manual;var screen=Screen.FromPoint(Cursor.Position);var area=screen.WorkingArea;Size=new Size(Math.Min(1400,area.Width),Math.Min(900,area.Height));Location=new Point(area.Left+(area.Width-Width)/2,area.Top+(area.Height-Height)/2);WindowState=FormWindowState.Maximized;
        BackColor=Color.Black;Icon=Icon.ExtractAssociatedIcon(Application.ExecutablePath);
        view=new WebView2{Dock=DockStyle.Fill,DefaultBackgroundColor=BackColor};Controls.Add(view);
        loading=new ClassicLoadingPanel{Dock=DockStyle.Fill};Controls.Add(loading);loading.BringToFront();
        retry=new Button{Text="Try again",Dock=DockStyle.Bottom,Height=48,Visible=false};retry.Click+=async(s,e)=>await StartAsync();loading.Controls.Add(retry);
        Shown+=async(s,e)=>await StartAsync();FormClosing+=OnClosing;
        monitor=new System.Windows.Forms.Timer{Interval=3000};monitor.Tick+=(s,e)=>{if(!closing&&!starting&&engine!=null&&engine.HasExited){monitor.Stop();ShowError("The local editor engine stopped. Click Try again to reconnect. Your current canvas stays in this window.");}};
    }
    void Log(string text){try{File.AppendAllText(Path.Combine(editor,"desktop.log"),DateTime.Now.ToString("s")+" "+text+Environment.NewLine);}catch{}}
    void ShowError(string text){Log(text);loading.Failed=true;loading.StatusText=text;retry.Visible=true;loading.Visible=true;loading.BringToFront();}
    async Task<Uri> StartEngineAsync(int port) {
        var ready=new TaskCompletionSource<Uri>();
        var info=new ProcessStartInfo(Path.Combine(editor,"runtime","python.exe"),"-I -u \""+Path.Combine(editor,"server.py")+"\" --port "+port+" --parent-pid "+Process.GetCurrentProcess().Id){WorkingDirectory=editor,UseShellExecute=false,CreateNoWindow=true,RedirectStandardOutput=true,RedirectStandardError=true};
        var proc=new Process{StartInfo=info,EnableRaisingEvents=true};engine=proc;
        proc.OutputDataReceived+=(s,e)=>{if(e.Data==null)return;Log(e.Data);const string prefix="Rsc World Editor ready: ";if(e.Data.StartsWith(prefix)){Uri url;if(Uri.TryCreate(e.Data.Substring(prefix.Length),UriKind.Absolute,out url)&&url.Host=="127.0.0.1")ready.TrySetResult(url);}};
        proc.ErrorDataReceived+=(s,e)=>{if(e.Data!=null)Log(e.Data);};proc.Exited+=(s,e)=>ready.TrySetException(new Exception("The local editor could not start. See RscWorldEditor/desktop.log."));
        proc.Start();proc.BeginOutputReadLine();proc.BeginErrorReadLine();
        if(await Task.WhenAny(ready.Task,Task.Delay(30000))!=ready.Task){if(!proc.HasExited)proc.Kill();throw new Exception("The local editor took too long to start. See RscWorldEditor/desktop.log.");}
        return await ready.Task;
    }
    async Task StartAsync() {
        if(starting)return;starting=true;retry.Visible=false;loading.Failed=false;loading.SetProgress(10,"Loading world map...");loading.Visible=true;loading.BringToFront();
        try {
            bool reconnect=address!=null&&view.CoreWebView2!=null;
            if(engine==null||engine.HasExited) address=await StartEngineAsync(reconnect?address.Port:0);
            loading.SetProgress(55,"Loading editor... ");
            if(view.CoreWebView2==null){
                Log("Creating WebView2 environment: "+CoreWebView2Environment.GetAvailableBrowserVersionString()); var env=await CoreWebView2Environment.CreateAsync(null,Path.Combine(editor,"desktop-data")); Log("Environment ready; creating window");
                await view.EnsureCoreWebView2Async(env);
                loading.SetProgress(80,"Preparing your world...");
                view.CoreWebView2.Settings.AreDefaultContextMenusEnabled=false;
                view.CoreWebView2.Settings.AreBrowserAcceleratorKeysEnabled=false;
                view.CoreWebView2.Settings.IsStatusBarEnabled=false;
                view.CoreWebView2.Settings.IsZoomControlEnabled=false;
                view.CoreWebView2.Settings.IsPasswordAutosaveEnabled=false;
                view.CoreWebView2.Settings.IsGeneralAutofillEnabled=false;
                view.CoreWebView2.NewWindowRequested+=(s,e)=>e.Handled=true;
                view.CoreWebView2.NavigationStarting+=(s,e)=>{Uri uri;if(!Uri.TryCreate(e.Uri,UriKind.Absolute,out uri)||uri.Scheme!="http"||uri.Host!="127.0.0.1"||uri.Port!=address.Port)e.Cancel=true;};
                view.CoreWebView2.AddWebResourceRequestedFilter("*",CoreWebView2WebResourceContext.All);
                view.CoreWebView2.WebResourceRequested+=(s,e)=>{Uri uri;if(Uri.TryCreate(e.Request.Uri,UriKind.Absolute,out uri)&&uri.Scheme.StartsWith("http")&&(uri.Host!="127.0.0.1"||uri.Port!=address.Port))e.Response=env.CreateWebResourceResponse(new MemoryStream(),403,"Offline application","");};
                view.CoreWebView2.WebMessageReceived+=(s,e)=>{string msg=e.TryGetWebMessageAsString();Log("UI: "+msg);if(msg=="close-saved"){closing=true;Close();}else if(msg.StartsWith("save-error:")){closePending=false;MessageBox.Show(this,msg.Substring(11),"Could not save");}};
                view.CoreWebView2.NavigationCompleted+=async(s,e)=>{if(!e.IsSuccess){ShowError("The editor window could not load ("+e.WebErrorStatus+"). Click Try again.");return;}loading.SetProgress(100,"Ready");loading.Visible=false;Log("Desktop window loaded");if(selfTest)await RunSelfTest();};
            }
            if(reconnect){await view.CoreWebView2.ExecuteScriptAsync("fetch('/api/assets').then(r=>r.json()).then(a=>{token=a.token;status('Reconnected. Your canvas is preserved')})");loading.Visible=false;}
            else view.CoreWebView2.Navigate(address.ToString());
            monitor.Start();
        }catch(Exception ex){Log(ex.ToString());ShowError(ex.Message);if(selfTest){File.WriteAllText(Path.Combine(editor,"desktop-self-test.json"),"{\"passed\":false,\"reason\":\"Desktop startup failed; see desktop.log\"}");closing=true;Close();}}
        finally{starting=false;}
    }
    async Task RunSelfTest(){
        try{
            string result="null";
            for(int i=0;i<100;i++){
                result=await view.CoreWebView2.ExecuteScriptAsync("(()=>{if(typeof state==='undefined'||!state||!modelIndex||!Object.keys(modelIndex).length||document.getElementById('busy').hidden===false)return null;return {ready:true,title:document.title,tiles:state.tiles.length*state.tiles[0].length,models:Object.keys(modelIndex).length,revision:state.revision,heightTool:tools.some(t=>t[0]==='height')}})()");
                if(result!="null")break;await Task.Delay(200);
            }
            using(var shot=File.Create(Path.Combine(editor,"desktop-self-test.png")))await view.CoreWebView2.CapturePreviewAsync(CoreWebView2CapturePreviewImageFormat.Png,shot);
            File.WriteAllText(Path.Combine(editor,"desktop-self-test.json"),result);Log("Self-test: "+result);
        }catch(Exception ex){Log(ex.ToString());File.WriteAllText(Path.Combine(editor,"desktop-self-test.json"),"{\"passed\":false}");}
        closing=true;Close();
    }
    async void OnClosing(object sender,FormClosingEventArgs e){
        if(closing){Cleanup();return;}e.Cancel=true;if(closePending)return;closePending=true;
        try{
            if(view.CoreWebView2!=null){
                string busy=await view.CoreWebView2.ExecuteScriptAsync("!!document.getElementById('busy')&&!document.getElementById('busy').hidden");
                if(busy=="true"&&!loading.Visible){MessageBox.Show(this,"Please let the current save or preview finish, then close the window.","RscWorldEditor");closePending=false;return;}
                string dirty=await view.CoreWebView2.ExecuteScriptAsync("typeof dirty !== 'undefined' && dirty");
                if(dirty=="true"){
                    var choice=MessageBox.Show(this,"Save your map changes before closing?","RscWorldEditor",MessageBoxButtons.YesNoCancel,MessageBoxIcon.Question);
                    if(choice==DialogResult.Cancel){closePending=false;return;}
                    if(choice==DialogResult.Yes){await view.CoreWebView2.ExecuteScriptAsync("save().then(()=>chrome.webview.postMessage('close-saved')).catch(e=>chrome.webview.postMessage('save-error:'+e.message))");return;}
                }
            }
            closing=true;Close();
        }catch(Exception ex){Log(ex.ToString());closePending=false;MessageBox.Show(this,"Rsc World Editor could not check your draft. Please save from the editor before closing.\n"+ex.Message,"RscWorldEditor");}
    }
    void Cleanup(){monitor.Stop();try{if(engine!=null&&!engine.HasExited)engine.Kill();}catch(Exception ex){Log(ex.Message);}view.Dispose();}
}



                                                                                   
sealed class ClassicLoadingPanel : Panel {
    string statusText="Loading world map...";
    int progress=10;
    public bool Failed { get; set; }
    public string StatusText { get { return statusText; } set { statusText=value;Invalidate(); } }
    public ClassicLoadingPanel(){BackColor=Color.Black;DoubleBuffered=true;ResizeRedraw=true;}
    public void SetProgress(int value,string text){progress=Math.Max(0,Math.Min(100,value));StatusText=text;}
    protected override void OnPaint(PaintEventArgs e){
        base.OnPaint(e);
        var g=e.Graphics;
        g.TextRenderingHint=System.Drawing.Text.TextRenderingHint.SingleBitPerPixelGridFit;
        int width=Math.Min(300,Math.Max(120,ClientSize.Width-40));
        int left=(ClientSize.Width-width)/2,top=ClientSize.Height/2-22;
        using(var textFont=new Font("Times New Roman",10,FontStyle.Regular))
        using(var creditFont=new Font("Times New Roman",10,FontStyle.Bold))
        using(var format=new StringFormat{Alignment=StringAlignment.Center,LineAlignment=StringAlignment.Center}){
            if(Failed){
                g.DrawString(statusText,textFont,Brushes.White,new RectangleF(Math.Max(10,(ClientSize.Width-540)/2),top-70,Math.Min(540,ClientSize.Width-20),80),format);
            }else{
                g.FillRectangle(Brushes.Gray,left,top,width*progress/100,18);
                g.DrawString(statusText,textFont,Brushes.White,new RectangleF(left,top,width,18),format);
            }
            g.DrawString("RuneScape Classic World Editor",creditFont,Brushes.White,new RectangleF(left-30,top+20,width+60,17),format);
            g.DrawString("Created by JitterBot",creditFont,Brushes.White,new RectangleF(left-30,top+36,width+60,17),format);
        }
    }
}
