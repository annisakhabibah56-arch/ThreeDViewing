package com.grafkom.viewing3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;
/** Kamera paralel dengan off-axis frustum, tanpa toe-in. */
public final class StereoAnaglyphRenderer{
 /** Callback render satu mata: matriks proyeksi, matriks view, dan posisi mata di world space. */
 @FunctionalInterface
 public interface EyePass{ void render(Matrix4f projection,Matrix4f view,Vector3f eyePosition); }
 private float ipd=.2f,focalLength=9f;
 public float getIpd(){return ipd;}
 public float getFocalLength(){return focalLength;}
 public void setIpd(float v){
  if(!Float.isFinite(v)||v<0||v>1)throw new IllegalArgumentException("IPD harus 0..1");ipd=v;
 }
 public void setFocalLength(float v){
  if(!Float.isFinite(v)||v<=0)throw new IllegalArgumentException("Fokus harus positif");focalLength=v;
 }
 public Matrix4f eyeView(Camera3D camera,float offset){
  return new Matrix4f().translation(-offset,0,0).mul(camera.calculateLookAtMatrix());
 }
 public Matrix4f eyeProjection(PerspectiveProjection p,float offset){
  float top=p.getNear()*(float)Math.tan(Math.toRadians(p.getFovDegree())/2);
  float right=top*p.getAspectRatio(),shift=-offset*p.getNear()/focalLength;
  return new Matrix4f().frustum(-right+shift,right+shift,-top,top,p.getNear(),p.getFar());
 }
 public void renderAnaglyph(Camera3D camera,PerspectiveProjection p,EyePass scene){
  glColorMask(true,true,true,true);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
  try{
   glColorMask(true,false,false,true);renderEye(camera,p,-ipd/2,scene);
   glClear(GL_DEPTH_BUFFER_BIT);
   glColorMask(false,true,true,true);renderEye(camera,p,ipd/2,scene);
  }finally{glColorMask(true,true,true,true);}
 }
 private void renderEye(Camera3D camera,PerspectiveProjection p,float offset,EyePass scene){
  Matrix4f view=eyeView(camera,offset);
  Vector3f eyePosition=view.invert(new Matrix4f()).transformPosition(new Vector3f());
  scene.render(eyeProjection(p,offset),view,eyePosition);
 }
}
