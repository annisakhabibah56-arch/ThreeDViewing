package com.grafkom.viewing3d;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Menguji geometri, vektor normal (flat/smooth), dan koordinat UV tanpa konteks OpenGL. */
class MeshBuilderTest {
 private static Vector3f p(float[] a,int vertex){return new Vector3f(a[vertex*3],a[vertex*3+1],a[vertex*3+2]);}
 private static MeshBuilder.Geometry[] meshes(){
  return new MeshBuilder.Geometry[]{MeshBuilder.sphere(1,10,16,2),MeshBuilder.torus(1.1f,.45f,24,12,3,1),
   MeshBuilder.house(),MeshBuilder.ground(10,16)};
 }
 @Test void vertexCountIsMultipleOfThree(){
  for(MeshBuilder.Geometry g:meshes()){
   assertTrue(g.vertexCount()>0&&g.vertexCount()%3==0,"Jumlah vertex harus kelipatan 3");
   assertEquals(g.vertexCount()*2,g.uvs.length);
  }
 }
 @Test void sphereTrianglesWindCounterClockwiseOutward(){
  MeshBuilder.Geometry g=MeshBuilder.sphere(1,10,16,2);
  for(int t=0;t<g.vertexCount();t+=3){
   Vector3f a=p(g.positions,t),b=p(g.positions,t+1),c=p(g.positions,t+2);
   Vector3f n=new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
   Vector3f centroid=new Vector3f(a).add(b).add(c).div(3);
   assertTrue(n.dot(centroid)>0,"Face bola harus CCW menghadap keluar (segitiga "+t/3+")");
  }
 }
 @Test void houseTrianglesWindCounterClockwiseOutward(){
  MeshBuilder.Geometry g=MeshBuilder.house();
  Vector3f inside=new Vector3f(0,.9f,0);
  for(int t=0;t<g.vertexCount();t+=3){
   Vector3f a=p(g.positions,t),b=p(g.positions,t+1),c=p(g.positions,t+2);
   Vector3f n=new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
   Vector3f centroid=new Vector3f(a).add(b).add(c).div(3).sub(inside);
   assertTrue(n.dot(centroid)>0,"Face rumah harus CCW menghadap keluar");
  }
 }
 @Test void groundFacesUp(){
  MeshBuilder.Geometry g=MeshBuilder.ground(10,16);
  float[] flat=MeshBuilder.flatNormals(g.positions);
  for(int v=0;v<g.vertexCount();v++){
   assertEquals(0,flat[v*3],1E-6);assertEquals(1,flat[v*3+1],1E-6);assertEquals(0,flat[v*3+2],1E-6);
  }
 }
 @Test void flatNormalsAreUnitAndEqualWithinFace(){
  for(MeshBuilder.Geometry g:meshes()){
   float[] n=MeshBuilder.flatNormals(g.positions);
   for(int t=0;t<g.vertexCount();t+=3){
    Vector3f n0=p(n,t);
    assertEquals(1,n0.length(),1E-4);
    assertTrue(n0.equals(p(n,t+1),1E-6f)&&n0.equals(p(n,t+2),1E-6f),"Flat: tiga vertex harus berbagi normal face");
   }
  }
 }
 @Test void smoothNormalsAreUnitAndFinite(){
  for(MeshBuilder.Geometry g:meshes()){
   float[] n=MeshBuilder.smoothNormals(g.positions);
   for(int v=0;v<g.vertexCount();v++){
    Vector3f nv=p(n,v);
    assertTrue(nv.isFinite());assertEquals(1,nv.length(),1E-4);
   }
  }
 }
 @Test void smoothNormalsOnSphereAreNearlyRadial(){
  MeshBuilder.Geometry g=MeshBuilder.sphere(1,10,16,2);
  float[] n=MeshBuilder.smoothNormals(g.positions);
  for(int v=0;v<g.vertexCount();v++)
   assertTrue(p(n,v).dot(p(g.positions,v).normalize())>.95f,"Normal smooth harus mendekati arah radial");
 }
 @Test void smoothNormalsAreContinuousAcrossUvSeam(){
  for(MeshBuilder.Geometry g:new MeshBuilder.Geometry[]{MeshBuilder.sphere(1,10,16,2),MeshBuilder.torus(1.1f,.45f,24,12,3,1)}){
   float[] n=MeshBuilder.smoothNormals(g.positions);
   Map<String,Vector3f> seen=new HashMap<>();
   for(int v=0;v<g.vertexCount();v++){
    Vector3f q=p(g.positions,v);
    String key=Math.round(q.x*1E4f)+","+Math.round(q.y*1E4f)+","+Math.round(q.z*1E4f);
    Vector3f nv=p(n,v),old=seen.putIfAbsent(key,nv);
    if(old!=null)assertTrue(old.equals(nv,1E-5f),"Posisi sama harus bernormal sama (seam)");
   }
  }
 }
 @Test void flatAndSmoothDifferOnCurvedMeshes(){
  MeshBuilder.Geometry g=MeshBuilder.sphere(1,10,16,2);
  float[] flat=MeshBuilder.flatNormals(g.positions),smooth=MeshBuilder.smoothNormals(g.positions);
  int different=0;
  for(int v=0;v<g.vertexCount();v++)if(!p(flat,v).equals(p(smooth,v),1E-3f))different++;
  assertTrue(different>g.vertexCount()/2,"Flat dan Smooth harus menghasilkan normal berbeda");
 }
 @Test void uvCoordinatesAreFiniteAndNonNegative(){
  for(MeshBuilder.Geometry g:meshes())
   for(float t:g.uvs){assertTrue(Float.isFinite(t));assertTrue(t>=-1E-5f,"UV negatif");}
 }
}
