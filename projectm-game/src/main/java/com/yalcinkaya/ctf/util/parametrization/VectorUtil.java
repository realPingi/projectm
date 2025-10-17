package com.yalcinkaya.ctf.util.parametrization;

import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.function.Function;

import static java.lang.Math.acos;

public class VectorUtil {

    public static Vector grow(Vector vector, Vector origin, double growth) {
        return vector.clone().subtract(origin).multiply(growth).add(origin);
    }

    public static Function<Vector, Vector> getRotation(Vector from, Vector to) {
        Vector normalFrom = from.normalize();
        Vector normalTo = to.normalize();
        double angle = acos(normalFrom.dot(normalTo));
        Vector axis = normalFrom.crossProduct(normalTo);
        return vector -> rotateAboutAxis(vector, axis, angle);
    }

    public static Quaterniond getQuaternion(Vector from, Vector to) {
        Quaterniond rotation = from.toVector3d().rotationTo(to.toVector3d(), new Quaterniond());
        return rotation;
    }

    public static Vector rotateVector(Vector vector, Quaterniond rotation) {
        Vector3d vector3d = vector.toVector3d();
        vector3d.rotate(rotation);
        return new Vector(vector3d.x, vector3d.y, vector3d.z);
    }

    public static Vector rotateAboutAxis(Vector vector, Vector axis, Vector origin, double theta) {
        return rotateAboutAxis(vector.clone().subtract(origin), axis, theta).add(origin);
    }

    public static Vector rotateAboutAxis(Vector vector, Vector axis, double theta) {
        axis.normalize();

        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();

        double ux = axis.getX();
        double uy = axis.getY();
        double uz = axis.getZ();

        double c = Math.cos(theta);
        double s = Math.sin(theta);
        double t = 1 - Math.cos(theta);

        double gammaX =
                (x * (t * Math.pow(ux, 2) + c)) + (y * (t * ux * uy - s * uz)) + (z * (t * ux * uz
                        + s * uy));
        double gammaY =
                (x * (t * ux * uy + s * uz)) + (y * (t * Math.pow(uy, 2) + c)) + (z * (t * uy * uz
                        - s * ux));
        double gammaZ =
                (x * (t * ux * uz - s * uy)) + (y * (t * uy * uz + s * ux)) + (z * (t * Math.pow(uz, 2)
                        + c));

        return new Vector(gammaX, gammaY, gammaZ);
    }

    public static Vector getXAxis() {
        return new Vector(1, 0, 0);
    }

    public static Vector getYAxis() {
        return new Vector(0, 1, 0);
    }

    public static Vector getZAxis() {
        return new Vector(0, 0, 1);
    }
}
