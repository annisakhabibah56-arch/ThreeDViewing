package com.grafkom.viewing3d;
import org.junit.jupiter.api.Test;
import org.joml.*;
import static org.junit.jupiter.api.Assertions.*;
class ViewingMathTest {
 private void matrix(Matrix4f expected, Matrix4f actual){assertTrue(expected.equals(actual,1E-5f),"Matriks berbeda");}
 @Test void lookAtMatchesReference(){
  Vector3f e=new Vector3f(4,3,5),t=new Vector3f(1,-2,.5f),u=new Vector3f(0,1,0);
  Camera3D c=new Camera3D(e,t,u);
  matrix(new Matrix4f().lookAt(e,t,u),c.calculateLookAtMatrix());
  assertEquals(0,c.calculateLookAtMatrix().transformPosition(e,new Vector3f()).length(),1E-5);
  c.getEye().zero();assertEquals(new Vector3f(4,3,5),c.getEye());
 }
 @Test void orbitUsesTargetAsCenter(){
  Vector3f target=new Vector3f(2,3,4);
  Camera3D c=new Camera3D(new Vector3f(2,3,9),target,new Vector3f(0,1,0));
  c.updateOrbitalPosition(5,90,0);
  assertEquals(0,c.getEye().distance(new Vector3f(7,3,4)),1E-5);
 }
 @Test void rejectsDegenerateCamera(){
  assertThrows(IllegalArgumentException.class,()->new Camera3D(new Vector3f(),new Vector3f(),new Vector3f(0,1,0)));
  assertThrows(IllegalArgumentException.class,()->new Camera3D(new Vector3f(0,1,0),new Vector3f(),new Vector3f(0,1,0)));
 }
 @Test void perspectiveAndClipping(){
  PerspectiveProjection p=new PerspectiveProjection(60,1.6f,.1f,50);
  matrix(new Matrix4f().perspective((float)java.lang.Math.toRadians(60),1.6f,.1f,50),p.calculateProjectionMatrix());
  Matrix4f m=p.calculateProjectionMatrix();
  assertEquals(-1,m.transformProject(new Vector3f(0,0,-.1f)).z,1E-5);
  assertEquals(1,m.transformProject(new Vector3f(0,0,-50)).z,1E-5);
  assertThrows(IllegalArgumentException.class,()->p.setClipping(2,1));
  assertThrows(IllegalArgumentException.class,()->p.setFovDegree(180));
 }
 private float projectedX(StereoAnaglyphRenderer s,PerspectiveProjection p,Camera3D c,float eye,Vector3f point){
  return s.eyeProjection(p,eye).mul(s.eyeView(c,eye)).transformProject(new Vector3f(point)).x;
 }
 @Test void stereoConvergenceAndDisparity(){
  Camera3D c=new Camera3D(new Vector3f(0,0,7),new Vector3f(),new Vector3f(0,1,0));
  PerspectiveProjection p=new PerspectiveProjection(50,1.5f,.1f,50);
  StereoAnaglyphRenderer s=new StereoAnaglyphRenderer(); s.setFocalLength(7);
  assertEquals(projectedX(s,p,c,-.1f,new Vector3f()),projectedX(s,p,c,.1f,new Vector3f()),1E-6);
  float front=projectedX(s,p,c,-.1f,new Vector3f(0,0,2))-projectedX(s,p,c,.1f,new Vector3f(0,0,2));
  float back=projectedX(s,p,c,-.1f,new Vector3f(0,0,-2))-projectedX(s,p,c,.1f,new Vector3f(0,0,-2));
  assertTrue(front>0);assertTrue(back<0);
  matrix(p.calculateProjectionMatrix(),s.eyeProjection(p,0));
 }
 @Test void isometricForeshorteningIsEqual(){
  Camera3D c=new Camera3D(new Vector3f(0,0,7),new Vector3f(),new Vector3f(0,1,0));
  c.updateOrbitalPosition(7,45,35.26439f);
  Matrix4f v=c.calculateLookAtMatrix();
  Vector3f x=v.transformDirection(new Vector3f(1,0,0)),y=v.transformDirection(new Vector3f(0,1,0)),z=v.transformDirection(new Vector3f(0,0,1));
  assertEquals(x.x*x.x+x.y*x.y,y.x*y.x+y.y*y.y,1E-5);
  assertEquals(x.x*x.x+x.y*x.y,z.x*z.x+z.y*z.y,1E-5);
 }
 @Test void cavalierPreservesFullDepthLengthAt45Degrees(){
  ProjectionTaxonomyManager t=new ProjectionTaxonomyManager();
  PerspectiveProjection p=new PerspectiveProjection(50,1,.1f,50);
  t.select(ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER);
  Matrix4f m=t.calculateProjection(p,7);
  Vector3f origin=m.transformProject(new Vector3f(0,0,-7));
  Vector3f depth=m.transformProject(new Vector3f(0,0,-8)).sub(origin);
  Vector3f horizontal=m.transformProject(new Vector3f(1,0,-7)).sub(origin);
  assertEquals(horizontal.x,java.lang.Math.hypot(depth.x,depth.y),1E-6);
  assertEquals(depth.x,depth.y,1E-6);
  assertEquals(6,ProjectionTaxonomyManager.ProjectionType.values().length);
 }
 @Test void perspectivePresetsHaveCorrectVanishingDirections(){
  ProjectionTaxonomyManager t=new ProjectionTaxonomyManager();
  for(int index=3;index<=5;index++){
   t.select(ProjectionTaxonomyManager.ProjectionType.values()[index]);
   Camera3D c=new Camera3D(new Vector3f(0,0,7),new Vector3f(),new Vector3f(0,1,0));
   c.updateOrbitalPosition(7,t.defaultAzimuth(),t.defaultElevation());
   Matrix4f v=c.calculateLookAtMatrix();
   int count=0;
   for(Vector3f axis:new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)})
    if(java.lang.Math.abs(v.transformDirection(axis).z)>1E-5)count++;
   assertEquals(index-2,count);
  }
 }

 @Test void aspectRatioPreservesEqualPixelScale(){
  for(int[] size:new int[][]{{1200,600},{600,900},{800,800}}){
   PerspectiveProjection p=new PerspectiveProjection(50,(float)size[0]/size[1],.1f,50);
   Matrix4f m=p.calculateProjectionMatrix();
   Vector3f x=m.transformProject(new Vector3f(1,0,-5));
   Vector3f y=m.transformProject(new Vector3f(0,1,-5));
   assertEquals(x.x*size[0]/2,y.y*size[1]/2,1E-4);
  }
 }
 @Test void fovAndClipBoundariesArePrecise(){
  for(float fov:new float[]{15,50,110}){
   PerspectiveProjection p=new PerspectiveProjection(fov,1.6f,.5f,20);
   float top=.5f*(float)java.lang.Math.tan(java.lang.Math.toRadians(fov)/2);
   Matrix4f m=p.calculateProjectionMatrix();
   assertEquals(1,m.transformProject(new Vector3f(0,top,-.5f)).y,1E-5);
   assertEquals(1,m.transformProject(new Vector3f(top*1.6f,0,-.5f)).x,1E-5);
   assertTrue(m.transformProject(new Vector3f(0,0,-.49f)).z < -1);
   assertTrue(m.transformProject(new Vector3f(0,0,-20.1f)).z > 1);
  }
 }
 @Test void stereoWorksAfterOrbitAndWithDynamicIpd(){
  Camera3D c=new Camera3D(new Vector3f(4,3,5),new Vector3f(),new Vector3f(0,1,0));
  c.updateOrbitalPosition(9,63,31);
  StereoAnaglyphRenderer s=new StereoAnaglyphRenderer();s.setFocalLength(9);
  PerspectiveProjection p=new PerspectiveProjection(50,1.6f,.1f,50);
  Vector3f forward=new Vector3f(c.getTarget()).sub(c.getEye()).normalize();
  Vector3f nearer=new Vector3f(c.getEye()).fma(5,forward);
  float previous=0;
  for(float ipd:new float[]{0,.1f,.2f,.4f}){
   s.setIpd(ipd);
   assertEquals(projectedX(s,p,c,-ipd/2,new Vector3f()),projectedX(s,p,c,ipd/2,new Vector3f()),1E-5);
   float disparity=projectedX(s,p,c,-ipd/2,nearer)-projectedX(s,p,c,ipd/2,nearer);
   if(ipd==0)assertEquals(0,disparity,1E-6);else assertTrue(disparity>previous);
   previous=disparity;
  }
 }
 @Test void cameraOrbitRemainsFiniteAtPolesAndReturnsCopies(){
  Vector3f e=new Vector3f(0,0,9),t=new Vector3f(),u=new Vector3f(0,1,0);
  Camera3D c=new Camera3D(e,t,u);e.zero();u.zero();t.set(10);
  c.getTarget().set(10);c.getUp().zero();
  for(float elevation:new float[]{-90,0,90}){
   c.updateOrbitalPosition(9,45,elevation);
   assertEquals(9,c.getEye().distance(c.getTarget()),1E-5);
   for(float entry:c.calculateLookAtMatrix().get(new float[16]))assertTrue(Float.isFinite(entry));
  }
 }
}
