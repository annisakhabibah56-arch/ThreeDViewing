package com.grafkom.viewing3d;
import org.joml.Matrix4f;
public final class PerspectiveProjection {
 private float fovDegree,aspectRatio,zNear,zFar;
 public PerspectiveProjection(float fov,float aspect,float near,float far){
  setFovDegree(fov);setAspectRatio(aspect);setClipping(near,far);
 }
 public void setFovDegree(float v){
  if(!Float.isFinite(v)||v<=0||v>=180)throw new IllegalArgumentException("FOV harus 0 < FOV < 180");
  fovDegree=v;
 }
 public void setAspectRatio(float v){
  if(!Float.isFinite(v)||v<=0)throw new IllegalArgumentException("Aspect harus positif");aspectRatio=v;
 }
 public void setClipping(float n,float f){
  if(!Float.isFinite(n)||!Float.isFinite(f)||n<=0||f<=n)throw new IllegalArgumentException("Harus 0 < near < far");
  zNear=n;zFar=f;
 }
 public float getFovDegree(){return fovDegree;}
 public float getAspectRatio(){return aspectRatio;}
 public float getNear(){return zNear;}
 public float getFar(){return zFar;}
 public Matrix4f calculateProjectionMatrix(){
  float f=1f/(float)Math.tan(Math.toRadians(fovDegree)/2);
  return new Matrix4f().zero().m00(f/aspectRatio).m11(f)
   .m22(-(zFar+zNear)/(zFar-zNear)).m32(-2*zFar*zNear/(zFar-zNear)).m23(-1);
 }
}
