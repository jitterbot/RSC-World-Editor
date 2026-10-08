package orsc.graphics.three;
import orsc.*;
import orsc.graphics.two.*;
import com.openrsc.client.entityhandling.EntityHandler;
import java.lang.reflect.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Polygon;
import javax.imageio.ImageIO;
import java.util.*;
import org.json.*;

                                                                              
public class ModelThumbnails {
  static Object get(Object o,Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}
  static void topView(BufferedImage sheet,RSModel m,int direction,Scene scene)throws Exception{
    final int[] ys=(int[])get(m,RSModel.class,"vertY");Integer[] faces=new Integer[m.faceHead];
    for(int i=0;i<faces.length;i++)faces[i]=i;
    Arrays.sort(faces,(a,b)->{double aa=0,bb=0;for(int v:m.faceIndices[a])aa+=ys[v];for(int v:m.faceIndices[b])bb+=ys[v];return Double.compare(bb/m.faceIndices[b].length,aa/m.faceIndices[a].length);});
    double extent=32;for(int i=0;i<m.vertHead;i++)extent=Math.max(extent,Math.hypot(m.vertX[i],m.vertZ[i]));
    double scale=53/extent,angle=direction*Math.PI/4,sin=Math.sin(angle),cos=Math.cos(angle);
    Graphics2D g=sheet.createGraphics();g.setClip(direction*128,128,128,128);
    int[][] palette=(int[][])get(scene,Scene.class,"m_L");byte[][] pixels=(byte[][])get(scene,Scene.class,"m_g");
    for(int face:faces){int col=m.faceTextureFront[face];if(col==12345678)col=m.faceTextureBack[face];if(col==12345678)continue;int rgb;
      if(col<0){int c=-1-col;rgb=((c>>10&31)*8<<16)|((c>>5&31)*8<<8)|(c&31)*8;}
      else if(col<palette.length&&palette[col]!=null&&pixels[col]!=null){long r=0,green=0,b=0;int count=0;for(byte value:pixels[col]){int c=palette[col][value&255];if(c==0xff00ff)continue;r+=c>>16&255;green+=c>>8&255;b+=c&255;count++;}rgb=count==0?0x9b8767:((int)(r/count)<<16)|((int)(green/count)<<8)|(int)(b/count);}
      else rgb=0x929b96;
      Polygon shape=new Polygon();for(int v:m.faceIndices[face]){double x=m.vertX[v]*cos+m.vertZ[v]*sin,z=m.vertZ[v]*cos-m.vertX[v]*sin;shape.addPoint(direction*128+64-(int)Math.round(x*scale),192+(int)Math.round(z*scale));}
      g.setColor(new Color(rgb));g.fillPolygon(shape);g.setColor(new Color(0,0,0,45));g.drawPolygon(shape);
    }g.dispose();
  }
  public static void main(String[] args)throws Exception{
    OpenRSC.main(new String[0]);mudclient client=null;
    long deadline=System.currentTimeMillis()+45000;
    while(System.currentTimeMillis()<deadline){client=(mudclient)get(null,ORSCApplet.class,"mudclient");if(client!=null&&(Boolean)get(client,mudclient.class,"worldComponentsLoaded"))break;Thread.sleep(200);}
    if(client==null||!(Boolean)get(client,mudclient.class,"worldComponentsLoaded"))throw new IllegalStateException("Client assets did not load");
    client.clientBaseThread.suspend();
    Scene scene=(Scene)get(client,mudclient.class,"scene");GraphicsController surface=client.getSurface();
    RSModel[] models=(RSModel[])get(client,mudclient.class,"modelCache");
    scene.removeAllGameObjects(false);surface.resize(128,128);scene.setMidpoints(64,true,128,64,64,8,64);
    scene.fogLandscapeDistance=100000;scene.fogEntityDistance=100000;scene.fogSmoothingStartDistance=100000;scene.fogZFalloff=1;
    Path out=Paths.get(args[0]);Files.createDirectories(out);JSONObject mapping=new JSONObject();Set<Integer> done=new HashSet<Integer>();
    for(int id=0;id<EntityHandler.objectCount();id++){
      int key=EntityHandler.getObjectDef(id).modelID;mapping.put(Integer.toString(id),key);
      if(done.contains(key))continue;done.add(key);
      RSModel source=models[key];int[] ys=(int[])get(source,RSModel.class,"vertY");
      int minY=0,maxY=0;double radius=32;
      for(int i=0;i<source.vertHead;i++){minY=Math.min(minY,ys[i]);maxY=Math.max(maxY,ys[i]);radius=Math.max(radius,Math.hypot(source.vertX[i],source.vertZ[i]));}
      int cy=(minY+maxY)/2;radius=Math.max(radius,(maxY-minY)/2.0);
      int distance=(int)Math.ceil(radius*6.5)+128;
      BufferedImage sheet=new BufferedImage(1024,256,BufferedImage.TYPE_INT_ARGB);
      for(int direction=0;direction<8;direction++){
        RSModel model=source.copyModel(false,-120,false,false,true);int textureCount=((int[])get(scene,Scene.class,"m_Hb")).length; for(int f=0;f<model.faceHead;f++){if(model.faceTextureFront[f]>=textureCount && model.faceTextureFront[f]!=12345678)model.faceTextureFront[f]=-16913;if(model.faceTextureBack[f]>=textureCount && model.faceTextureBack[f]!=12345678)model.faceTextureBack[f]=-16913;} model.setRot256(0,direction*32,0);model.setDiffuseLight(48,48,-10,-122,-50,-50);scene.addModel(model);
        for(int view=0;view<1;view++){
          Arrays.fill(surface.pixelData,0x233a41);
          scene.setCamera(0,cy,0,view==0?880:800,view==0?640:513,0,distance);scene.endScene(-124);
          int[] pixels=new int[16384];for(int i=0;i<pixels.length;i++)pixels[i]=surface.pixelData[i]==0x233a41?0:0xff000000|surface.pixelData[i];
          sheet.setRGB(direction*128,view*128,128,128,pixels,0,128);
        }
        scene.removeModel(model);
        topView(sheet,source,direction,scene);
      }
      ImageIO.write(sheet,"png",out.resolve(key+".png").toFile());
      if(done.size()%50==0)System.out.println("Rendered "+done.size()+" models");
    }
    Files.write(out.resolve("index.json"),mapping.toString().getBytes("UTF-8"));
    System.out.println("Complete: "+done.size()+" models, "+mapping.length()+" scenery entries");System.exit(0);
  }
}



