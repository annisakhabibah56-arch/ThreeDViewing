package com.grafkom.viewing3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Kamera right-handed dengan LookAt manual. Getter mengembalikan salinan. */
public final class Camera3D {
 private final Vector3f eye, target, up;
 public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
  this.eye=new Vector3f(eye); this.target=new Vector3f(target); this.up=new Vector3f(up);
  calculateLookAtMatrix();
 }
 public Vector3f getEye(){return new Vector3f(eye);}
 public Vector3f getTarget(){return new Vector3f(target);}
 public Vector3f getUp(){return new Vector3f(up);}
 public Matrix4f calculateLookAtMatrix(){
  if(!eye.isFinite()||!target.isFinite()||!up.isFinite()) throw new IllegalArgumentException("Vektor harus finite");
  Vector3f n=new Vector3f(eye).sub(target);
  if(n.lengthSquared()<1E-10f) throw new IllegalArgumentException("Eye sama dengan target");
  n.normalize(); Vector3f u=new Vector3f(up).cross(n);
  if(u.lengthSquared()<1E-10f) throw new IllegalArgumentException("Up sejajar arah pandang");
  u.normalize(); Vector3f v=new Vector3f(n).cross(u);
  // Indeks JOML: m(kolom)(baris).
  return new Matrix4f().m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye))
   .m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye))
   .m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
 }
 public void updateOrbitalPosition(float radius,float azimuthDeg,float elevationDeg){
  if(!Float.isFinite(radius)||radius<=0||!Float.isFinite(azimuthDeg)||!Float.isFinite(elevationDeg))
   throw new IllegalArgumentException("Parameter orbit tidak valid");
  double az=Math.toRadians(azimuthDeg), el=Math.toRadians(Math.max(-89,Math.min(89,elevationDeg)));
  eye.set((float)(radius*Math.cos(el)*Math.sin(az)),(float)(radius*Math.sin(el)),
   (float)(radius*Math.cos(el)*Math.cos(az))).add(target);
 }
}
