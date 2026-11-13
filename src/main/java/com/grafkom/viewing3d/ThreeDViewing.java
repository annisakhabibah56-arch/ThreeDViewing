package com.grafkom.viewing3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Entry point: 3D Visual Realism Suite (lanjutan 3D Viewing Minggu 10).
 * Modern OpenGL 3.3 core profile + GLSL shader.
 */
public final class ThreeDViewing {
 private long window;
 private int width=1100,height=760;
 private float radius=9,azimuth=45,elevation=25;
 private boolean stereo,dragging,freeOrbit;
 private double lastX,lastY;
 private final Camera3D camera=new Camera3D(new Vector3f(4,3,5),new Vector3f(0,.8f,0),new Vector3f(0,1,0));
 private final PerspectiveProjection perspective=new PerspectiveProjection(50,1100f/760,.1f,50);
 private final ProjectionTaxonomyManager taxonomy=new ProjectionTaxonomyManager();
 private final StereoAnaglyphRenderer anaglyph=new StereoAnaglyphRenderer();
 private SceneRenderer scene;
 private int frames;
 private double fpsClock;
 private int fps;

 public static void main(String[] args){
  new ThreeDViewing().run();
 }

 private void run(){
  GLFWErrorCallback error=GLFWErrorCallback.createPrint(System.err);error.set();
  try{
   if(!glfwInit())throw new IllegalStateException("GLFW gagal. Periksa driver grafis.");
   glfwDefaultWindowHints();
   glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
   glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
   glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE);
   glfwWindowHint(GLFW_DEPTH_BITS,24);glfwWindowHint(GLFW_SAMPLES,4);
   window=glfwCreateWindow(width,height,"3D Visual Realism Suite",NULL,NULL);
   if(window==NULL)throw new IllegalStateException("Window OpenGL 3.3 gagal dibuat. Perbarui driver grafis.");
   glfwMakeContextCurrent(window);GL.createCapabilities();glfwSwapInterval(1);
   int[] w={0},h={0};glfwGetFramebufferSize(window,w,h);width=w[0];height=h[0];
   glEnable(GL_MULTISAMPLE);glClearColor(.065f,.08f,.115f,1);
   scene=new SceneRenderer();
   glfwSetFramebufferSizeCallback(window,(win,x,y)->{width=x;height=y;});
   glfwSetKeyCallback(window,(win,key,sc,action,mods)->{if(action==GLFW_PRESS)onKey(key);});
   glfwSetMouseButtonCallback(window,(win,b,a,m)->{
    if(b==GLFW_MOUSE_BUTTON_LEFT){
     dragging=a==GLFW_PRESS;
     double[] x={0},y={0};
     glfwGetCursorPos(win,x,y);lastX=x[0];lastY=y[0];
    }
   });
   glfwSetCursorPosCallback(window,(win,x,y)->{
    if(dragging)orbit((float)(x-lastX)*.25f,(float)(y-lastY)*.25f);
    lastX=x;lastY=y;
   });
   glfwSetScrollCallback(window,(win,x,y)->radius=clamp(radius-(float)y*.4f,3,18));
   printControls();
   double previous=glfwGetTime();fpsClock=previous;
   while(!glfwWindowShouldClose(window)){
    glfwPollEvents();double now=glfwGetTime();float dt=(float)Math.min(.05,now-previous);previous=now;
    input(dt);
    scene.update(dt);
    if(width>0&&height>0){draw(now);glfwSwapBuffers(window);}else glfwWaitEventsTimeout(.05);
   }
   scene.dispose();
  }finally{
   if(window!=NULL){org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(window);glfwDestroyWindow(window);}
   glfwTerminate();glfwSetErrorCallback(null);error.free();
  }
 }

 private static void printControls(){
  System.out.println("=== 3D Visual Realism Suite (Minggu 11) ===");
  System.out.println("B: Phong/Blinn-Phong | F: Flat/Smooth | Z: Z-Buffer | C: Back-Face Culling");
  System.out.println("T: filter tekstur Nearest/Bilinear+Mipmap | X: tekstur ON/OFF | H: bayangan ON/OFF");
  System.out.println("G: ganti mesh (Sphere/Torus/Rumah) | P: gerak cahaya ON/OFF");
  System.out.println("--- kontrol 3D Viewing (Minggu 10) ---");
  System.out.println("1-6: proyeksi | Panah/drag: orbit | Scroll/W/S: jarak | A: stereo | R: reset");
  System.out.println("Q/E: FOV | [ ]: IPD | - =: fokus | N/M: near | J/K: far | Esc: keluar");
 }

 private static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
 private boolean down(int k){return glfwGetKey(window,k)==GLFW_PRESS;}
 private void select(int index){
  taxonomy.select(ProjectionTaxonomyManager.ProjectionType.values()[index]);
  azimuth=taxonomy.defaultAzimuth();elevation=taxonomy.defaultElevation();freeOrbit=false;
  if(!taxonomy.getType().isPerspective())stereo=false;
 }
 private void onKey(int key){
  if(key>=GLFW_KEY_1&&key<=GLFW_KEY_6)select(key-GLFW_KEY_1);
  switch(key){
   case GLFW_KEY_ESCAPE->glfwSetWindowShouldClose(window,true);
   case GLFW_KEY_A->{if(!taxonomy.getType().isPerspective())select(5);stereo=!stereo;}
   case GLFW_KEY_R->{
    radius=9;perspective.setFovDegree(50);perspective.setClipping(.1f,50);
    anaglyph.setIpd(.2f);anaglyph.setFocalLength(9);stereo=false;select(5);scene.resetDefaults();
   }
   // Praktikum Minggu 11
   case GLFW_KEY_B->scene.toggleBlinnPhong();
   case GLFW_KEY_F->scene.toggleSmooth();
   case GLFW_KEY_Z->scene.toggleDepthTest();
   case GLFW_KEY_C->scene.toggleCulling();
   case GLFW_KEY_T->scene.toggleTextureFilter();
   case GLFW_KEY_X->scene.toggleTexture();
   case GLFW_KEY_H->scene.toggleShadows();
   case GLFW_KEY_G->scene.nextMesh();
   case GLFW_KEY_P->scene.toggleLightAnimation();
   default->{}
  }
 }
 private void orbit(float dx,float dy){
  if(dx==0&&dy==0)return;
  azimuth+=dx;elevation=clamp(elevation+dy,-85,85);freeOrbit=true;
 }
 private void input(float dt){
  orbit(((down(GLFW_KEY_RIGHT)?1:0)-(down(GLFW_KEY_LEFT)?1:0))*65*dt,
        ((down(GLFW_KEY_UP)?1:0)-(down(GLFW_KEY_DOWN)?1:0))*65*dt);
  radius=clamp(radius+((down(GLFW_KEY_S)?1:0)-(down(GLFW_KEY_W)?1:0))*4*dt,3,18);
  perspective.setFovDegree(clamp(perspective.getFovDegree()+((down(GLFW_KEY_E)?1:0)-(down(GLFW_KEY_Q)?1:0))*35*dt,15,110));
  anaglyph.setIpd(clamp(anaglyph.getIpd()+((down(GLFW_KEY_RIGHT_BRACKET)?1:0)-(down(GLFW_KEY_LEFT_BRACKET)?1:0))*.25f*dt,0,.8f));
  anaglyph.setFocalLength(clamp(anaglyph.getFocalLength()+((down(GLFW_KEY_EQUAL)?1:0)-(down(GLFW_KEY_MINUS)?1:0))*3*dt,1,30));
  float near=clamp(perspective.getNear()+((down(GLFW_KEY_M)?1:0)-(down(GLFW_KEY_N)?1:0))*3*dt,.05f,20);
  float far=clamp(perspective.getFar()+((down(GLFW_KEY_K)?1:0)-(down(GLFW_KEY_J)?1:0))*10*dt,near+.1f,100);
  perspective.setClipping(near,far);
 }
 private void draw(double now){
  perspective.setAspectRatio((float)width/height);
  camera.updateOrbitalPosition(radius,azimuth,elevation);

  scene.renderShadowPass(width,height);          // Pass 1: depth dari sudut pandang cahaya
  glViewport(0,0,width,height);                  // Pass 2: render normal dari kamera
  if(stereo)anaglyph.renderAnaglyph(camera,perspective,
   (projection,view,eyePosition)->scene.renderScene(projection,view,eyePosition,true));
  else{
   glColorMask(true,true,true,true);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
   scene.renderScene(taxonomy.calculateProjection(perspective,radius),camera.calculateLookAtMatrix(),
    camera.getEye(),false);
  }

  frames++;
  if(now-fpsClock>=.5){
   fps=(int)Math.round(frames/(now-fpsClock));frames=0;fpsClock=now;
   glfwSetWindowTitle(window,String.format(java.util.Locale.ROOT,
    "%s | %s | %s%s | FPS %d | FOV %.0f | 1-6 proyeksi, A stereo, R reset",
    scene.status(),taxonomy.getType(),stereo?"ANAGLYPH":"MONO",freeOrbit?" (orbit bebas)":"",
    fps,perspective.getFovDegree()));
  }
 }
}
