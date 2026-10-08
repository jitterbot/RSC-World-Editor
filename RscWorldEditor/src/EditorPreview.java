package orsc.graphics.three;
import orsc.*;
import orsc.graphics.three.*;
import orsc.graphics.two.*;
import com.openrsc.client.entityhandling.EntityHandler;
import java.lang.reflect.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.json.*;
public class EditorPreview {
  static Object get(Object obj,Class<?> cls,String n)throws Exception {Field f=cls.getDeclaredField(n);f.setAccessible(true);return f.get(obj);}
  public static void main(String[] args)throws Exception {
    System.setProperty("rscworldeditor.objects",Paths.get(args[0],"preview","WorkshopObjects.properties").toAbsolutePath().toString());
    OpenRSC.main(new String[0]); mudclient m=null;
    long deadline=System.currentTimeMillis()+45000;
    while(System.currentTimeMillis()<deadline) {
      m=(mudclient)get(null,ORSCApplet.class,"mudclient");
      if(m!=null && (Boolean)get(m,mudclient.class,"worldComponentsLoaded")) break;
      Thread.sleep(250);
    }
    if(m==null || !(Boolean)get(m,mudclient.class,"worldComponentsLoaded")) throw new IllegalStateException("Client assets did not finish loading");
                                                                                  
    m.clientBaseThread.suspend();
    Scene scene=(Scene)get(m,mudclient.class,"scene");
    World world=m.getWorld(); GraphicsController surf=m.getSurface();
    JSONObject view=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0],"preview","view.json")),"UTF-8"));
    Field archive=World.class.getDeclaredField("tileArchive");archive.setAccessible(true);
    ((java.util.zip.ZipFile)archive.get(world)).close();
    archive.set(world,new java.util.zip.ZipFile(Paths.get(args[0],"preview","video","Custom_Landscape.orsc").toFile()));
    RSModel[] models=(RSModel[])get(m,mudclient.class,"modelCache");
    byte[] sceneryArchive=Files.readAllBytes(Paths.get(args[0],"preview","models.orsc"));
    byte[] sceneryData=java.util.Arrays.copyOfRange(sceneryArchive,6,sceneryArchive.length);
    for(int i=0;i<EntityHandler.getModelCount();i++)if(EntityHandler.getModelName(i).startsWith("wsobj_")){
      int offset=com.openrsc.data.DataOperations.getDataFileOffset(EntityHandler.getModelName(i)+".ob3",sceneryData);
      if(offset==0)throw new IllegalStateException("Missing custom scenery model");
      models[i]=new RSModel(sceneryData,offset,true);
    }
    surf.resize(1024,768); scene.setMidpoints(384,true,1024,512,384,9,512);
    scene.fogLandscapeDistance=20000;scene.fogEntityDistance=20000;scene.fogSmoothingStartDistance=20000;scene.fogZFalloff=1;
    JSONObject locs=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0],"SceneryLocs.json")),"UTF-8"));
    int plane=view.getInt("floor");
    {
      int gx=view.getInt("x")+2304,gy=view.getInt("y")+1776;
      int baseX=((gx+24)/48-1)*48,baseY=((gy+24)/48-1)*48;
      world.loadSections(gx,gy,plane);
      for(int h=plane==0?1:4;h<=2;h++) for(int j=0;j<64;j++) {
        if(world.modelWallGrid[h][j]!=null) scene.addModel(world.modelWallGrid[h][j]);
        if(world.modelRoofGrid[h][j]!=null) scene.addModel(world.modelRoofGrid[h][j]);
      }
      JSONArray objects=locs.getJSONArray("sceneries");
      for(int i=0;i<objects.length();i++) {
        JSONObject o=objects.getJSONObject(i);JSONObject p=o.getJSONObject("pos");
        if(p.getInt("Y")/944!=plane)continue;
        int x=p.getInt("X")+2304-baseX,y=p.getInt("Y")%944+1776-baseY;
        if(x<1||y<1||x>90||y>90)continue;
        int id=o.getInt("id"),dir=o.getInt("direction");
        com.openrsc.client.entityhandling.defs.GameObjectDef def=EntityHandler.getObjectDef(id);
        int w=def.getWidth(),h=def.getHeight();if(dir!=0 && dir!=4){int t=w;w=h;h=t;}
        RSModel model=models[def.modelID].copyModel(false,-120,false,false,true);
        int px=(x*2+w)*64,pz=(y*2+h)*64;
        model.translate2(px,-world.getElevation(px,pz),pz);model.setRot256(0,dir*32,0);
        model.setDiffuseLight(48,48,-10,-122,-50,-50);scene.addModel(model);
      }
      if(view.optBoolean("hideRoofs"))for(int j=0;j<64;j++) {
        if(world.modelRoofGrid[plane][j]!=null)scene.removeModel(world.modelRoofGrid[plane][j]);
        if(plane==0)for(int h=1;h<=2;h++) {
          if(world.modelRoofGrid[h][j]!=null)scene.removeModel(world.modelRoofGrid[h][j]);
          if(world.modelWallGrid[h][j]!=null)scene.removeModel(world.modelWallGrid[h][j]);
        }
      }
      surf.blackScreen(true);
      int cx=(gx-baseX)*128+64,cy=(gy-baseY)*128+64;
      scene.setCamera(cx,-world.getElevation(cx,cy),cy,912,view.getInt("angle"),0,3600);scene.endScene(-124);
      BufferedImage im=new BufferedImage(1024,768,BufferedImage.TYPE_INT_RGB);
      im.setRGB(0,0,1024,768,surf.pixelData,0,1024);
      ImageIO.write(im,"png",Paths.get(args[0],"preview","scene.png").toFile());
      System.out.println("Draft preview rendered successfully");
    }
    System.exit(0);
  }
}
